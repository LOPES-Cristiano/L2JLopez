package com.lopez.l2j.game.reset;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico moderno de Reset / Rebirth de Personagens (Portado de ResetManager/ResetData).
 * Permite que jogadores de nivel maximo (80) com 3a classe reiniciem seu nivel para 1,
 * aumentando seu contador de resets, registrando no ranking diario/mensal (reset_rankings)
 * e concedendo recompensas e bonus de rebirth.
 */
@Service
public class CharacterResetService {

	private static final Logger log = LoggerFactory.getLogger(CharacterResetService.class);

	private final Map<Integer, Integer> totalResets = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> dailyResets = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> monthlyResets = new ConcurrentHashMap<>();

	private CharacterResetRequirement requirement;
	private final DataSource dataSource;
	private final InventoryService inventoryService;
	private final CharacterService characterService;
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	public CharacterResetService(DataSource dataSource, ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this(dataSource, null, null, itemRepository, itemTable, idFactory);
	}

	@Autowired
	public CharacterResetService(
			@Autowired(required = false) DataSource dataSource,
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) CharacterService characterService,
			@Autowired(required = false) ItemRepository itemRepository,
			@Autowired(required = false) ItemTemplateTable itemTable,
			@Autowired(required = false) ObjectIdFactory idFactory) {
		this.dataSource = dataSource;
		this.inventoryService = inventoryService;
		this.characterService = characterService;
		this.itemRepository = itemRepository;
		this.itemTable = itemTable != null ? itemTable : (inventoryService != null ? inventoryService.templates() : null);
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x65000000);
		initializeDefaultRequirement();
		loadRankingsFromDatabase();
	}

	public CharacterResetService() {
		this(null, null, null, null, null, null);
	}

	private void initializeDefaultRequirement() {
		// Level 80, 10 PvP kills, 3rd class, 50,000,000 Adena
		this.requirement = new CharacterResetRequirement(80, 10, true, Map.of(57, 50_000_000L));
	}

	public CharacterResetRequirement getRequirement() {
		return requirement;
	}

	public void setRequirement(CharacterResetRequirement requirement) {
		this.requirement = requirement;
	}

	public boolean canReset(PlayerCharacter player) {
		return canReset(player, buildInventoryMap(player));
	}

	public boolean canReset(PlayerCharacter player, Map<Integer, Long> inventoryCounts) {
		if (player == null || player.isDead()) {
			return false;
		}
		if (player.level() < requirement.minLevel()) {
			return false;
		}
		if (player.pvpKills() < requirement.minPvP()) {
			return false;
		}
		// 3rd class check: classId >= 88 or third tier
		if (requirement.requireThirdClass() && player.classId() < 88) {
			return false;
		}
		// Required items
		for (Map.Entry<Integer, Long> req : requirement.requiredItems().entrySet()) {
			long ownedMap = inventoryCounts != null ? inventoryCounts.getOrDefault(req.getKey(), 0L) : 0L;
			long ownedInv = player.inventory() != null
					? player.inventory().byItemId(req.getKey()).map(i -> (long) i.count()).orElse(0L)
					: 0L;
			if (Math.max(ownedMap, ownedInv) < req.getValue()) {
				return false;
			}
		}
		return true;
	}

	public synchronized boolean performReset(PlayerCharacter player) {
		return performReset(player, buildInventoryMap(player));
	}

	public synchronized boolean performReset(PlayerCharacter player, Map<Integer, Long> inventoryCounts) {
		if (!canReset(player, inventoryCounts)) {
			return false;
		}

		// Deduct cost items in map and real inventory/DB
		for (Map.Entry<Integer, Long> req : requirement.requiredItems().entrySet()) {
			int itemId = req.getKey();
			long reqCount = req.getValue();
			if (inventoryCounts != null && inventoryCounts.containsKey(itemId)) {
				long owned = inventoryCounts.getOrDefault(itemId, 0L);
				inventoryCounts.put(itemId, Math.max(0L, owned - reqCount));
			}
			consumeFromInventoryAndDb(player, itemId, (int) Math.min(Integer.MAX_VALUE, reqCount));
		}

		// Apply reset to player
		player.level(1);
		player.exp(0L);
		player.sp(0);

		if (characterService != null) {
			try {
				characterService.save(player, true);
			} catch (Exception e) {
				log.warn("Failed to persist character state after reset for player {}: {}", player.objectId(), e.getMessage());
			}
		}

		// Increment counts
		int playerId = player.objectId();
		int total = totalResets.merge(playerId, 1, Integer::sum);
		int daily = dailyResets.merge(playerId, 1, Integer::sum);
		int monthly = monthlyResets.merge(playerId, 1, Integer::sum);

		// Award Rebirth reward: 5 Coin of Luck (4037) in both inventory and DB
		awardRewardToInventoryAndDb(player, 4037, 5);

		saveRankingToDatabase(playerId, daily, monthly);

		log.info("Player {} (ID: {}) successfully performed Reset #{}. Daily: {}, Monthly: {}",
				player.name(), playerId, total, daily, monthly);
		return true;
	}

	private Map<Integer, Long> buildInventoryMap(PlayerCharacter player) {
		Map<Integer, Long> counts = new HashMap<>();
		if (player != null && player.inventory() != null) {
			for (ItemInstance item : player.inventory().items()) {
				counts.merge(item.itemId(), (long) item.count(), Long::sum);
			}
		}
		return counts;
	}

	private void consumeFromInventoryAndDb(PlayerCharacter player, int itemId, int count) {
		if (count <= 0 || player == null || player.inventory() == null) {
			return;
		}
		ItemInstance existing = player.inventory().byItemId(itemId).orElse(null);
		if (existing == null) {
			return;
		}
		if (inventoryService != null && existing.count() >= count) {
			inventoryService.consumeItem(player.inventory(), itemId, count, "ResetCost");
			return;
		}
		if (existing.count() > count) {
			existing.count(existing.count() - count);
			if (itemRepository != null) {
				itemRepository.update(existing);
			}
		} else {
			player.inventory().remove(existing);
			existing.count(0);
			if (itemRepository != null) {
				itemRepository.delete(existing.objectId());
			}
		}
	}

	private void awardRewardToInventoryAndDb(PlayerCharacter player, int itemId, int count) {
		if (count <= 0 || player == null) {
			return;
		}
		if (inventoryService != null && player.inventory() != null) {
			try {
				var res = inventoryService.addItem(player.inventory(), itemId, count, "ResetReward");
				if (res != null && res.item() != null) {
					return;
				}
			} catch (Exception e) {
				log.debug("InventoryService.addItem fallback for reset reward {}: {}", itemId, e.getMessage());
			}
		}
		ItemTemplateTable templates = this.itemTable != null ? this.itemTable
				: (inventoryService != null ? inventoryService.templates() : null);
		if (templates != null) {
			var tmplOpt = templates.get(itemId);
			if (tmplOpt.isPresent()) {
				var tmpl = tmplOpt.get();
				if (player.inventory() != null && tmpl.isStackable()) {
					ItemInstance existing = player.inventory().byItemId(itemId).orElse(null);
					if (existing != null) {
						existing.count((int) Math.min(Integer.MAX_VALUE, (long) existing.count() + count));
						if (itemRepository != null) {
							itemRepository.update(existing);
						}
						return;
					}
				}
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
				if (player.inventory() != null) {
					player.inventory().add(item);
				}
				if (itemRepository != null) {
					itemRepository.insert(item, "ResetReward");
				}
				return;
			}
		}
		if (player.inventory() != null) {
			player.inventory().addItem(itemId, count);
		}
	}

	public int getTotalResets(int playerId) {
		return totalResets.getOrDefault(playerId, 0);
	}

	public int getDailyResets(int playerId) {
		return dailyResets.getOrDefault(playerId, 0);
	}

	public int getMonthlyResets(int playerId) {
		return monthlyResets.getOrDefault(playerId, 0);
	}

	public List<ResetRankingEntry> getTopRankings(int limit) {
		List<ResetRankingEntry> list = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : totalResets.entrySet()) {
			int id = entry.getKey();
			list.add(new ResetRankingEntry(id, "Player_" + id,
					dailyResets.getOrDefault(id, 0),
					monthlyResets.getOrDefault(id, 0),
					entry.getValue()));
		}
		list.sort((r1, r2) -> Integer.compare(r2.totalCount(), r1.totalCount()));
		return list.size() > limit ? list.subList(0, limit) : list;
	}

	public String generateHtml(PlayerCharacter player) {
		return generateHtml(player, buildInventoryMap(player));
	}

	public String generateHtml(PlayerCharacter player, Map<Integer, Long> inventoryCounts) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Character Rebirth System</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Character Reset / Rebirth ===</font><br>");
		sb.append("Current Resets: <font color=\"8FA888\">").append(getTotalResets(player.objectId())).append("</font><br><br>");

		sb.append("<table width=280 bgcolor=000000>");
		sb.append("<tr><td><font color=\"LEVEL\">Requirements:</font></td></tr>");
		sb.append("<tr><td>Level: <font color=\"").append(player.level() >= requirement.minLevel() ? "8FA888" : "B86B6B")
				.append("\">").append(player.level()).append(" / ").append(requirement.minLevel()).append("</font></td></tr>");
		sb.append("<tr><td>PvP Kills: <font color=\"").append(player.pvpKills() >= requirement.minPvP() ? "8FA888" : "B86B6B")
				.append("\">").append(player.pvpKills()).append(" / ").append(requirement.minPvP()).append("</font></td></tr>");

		for (Map.Entry<Integer, Long> req : requirement.requiredItems().entrySet()) {
			long ownedMap = inventoryCounts != null ? inventoryCounts.getOrDefault(req.getKey(), 0L) : 0L;
			long ownedInv = player.inventory() != null
					? player.inventory().byItemId(req.getKey()).map(i -> (long) i.count()).orElse(0L)
					: 0L;
			long owned = Math.max(ownedMap, ownedInv);
			sb.append("<tr><td>Cost (Item ").append(req.getKey()).append("): <font color=\"")
					.append(owned >= req.getValue() ? "8FA888" : "B86B6B").append("\">")
					.append(String.format("%,d", owned)).append(" / ").append(String.format("%,d", req.getValue())).append("</font></td></tr>");
		}
		sb.append("</table><br>");

		if (canReset(player, inventoryCounts)) {
			sb.append("<button value=\"PERFORM RESET\" action=\"bypass -h voiced_reset do\" width=120 height=26 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
		} else {
			sb.append("<font color=\"808080\">Requirements not satisfied.</font><br>");
		}

		sb.append("</center></body></html>");
		return sb.toString();
	}

	private void loadRankingsFromDatabase() {
		if (dataSource == null) return;
		String sql = "SELECT player_id, daily_count, monthly_count FROM reset_rankings";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int playerId = rs.getInt("player_id");
				int daily = rs.getInt("daily_count");
				int monthly = rs.getInt("monthly_count");
				dailyResets.put(playerId, daily);
				monthlyResets.put(playerId, monthly);
				totalResets.put(playerId, monthly);
			}
		} catch (SQLException e) {
			log.warn("Failed to load reset rankings: {}", e.getMessage());
		}
	}

	private void saveRankingToDatabase(int playerId, int daily, int monthly) {
		if (dataSource == null) return;
		String sql = "INSERT INTO reset_rankings (player_id, daily_count, monthly_count) VALUES (?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE daily_count = VALUES(daily_count), monthly_count = VALUES(monthly_count)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, playerId);
			ps.setInt(2, daily);
			ps.setInt(3, monthly);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.warn("Failed to save reset ranking for player {}: {}", playerId, e.getMessage());
		}
	}
}
