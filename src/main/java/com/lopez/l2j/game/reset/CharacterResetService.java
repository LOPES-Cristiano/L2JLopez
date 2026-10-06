package com.lopez.l2j.game.reset;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CharacterResetService {

	private static final Logger log = LoggerFactory.getLogger(CharacterResetService.class);

	private final Map<Integer, Integer> totalResets = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> dailyResets = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> monthlyResets = new ConcurrentHashMap<>();

	private CharacterResetRequirement requirement;
	private final DataSource dataSource;
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	public CharacterResetService(DataSource dataSource, ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this.dataSource = dataSource;
		this.itemRepository = itemRepository;
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x65000000);
		initializeDefaultRequirement();
		loadRankingsFromDatabase();
	}

	public CharacterResetService() {
		this(null, null, null, null);
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
			long owned = inventoryCounts.getOrDefault(req.getKey(), 0L);
			if (owned < req.getValue()) {
				return false;
			}
		}
		return true;
	}

	public synchronized boolean performReset(PlayerCharacter player, Map<Integer, Long> inventoryCounts) {
		if (!canReset(player, inventoryCounts)) {
			return false;
		}

		// Deduct cost items
		for (Map.Entry<Integer, Long> req : requirement.requiredItems().entrySet()) {
			long owned = inventoryCounts.getOrDefault(req.getKey(), 0L);
			inventoryCounts.put(req.getKey(), owned - req.getValue());
		}

		// Apply reset to player
		player.level(1);
		player.exp(0L);
		player.sp(0);

		// Increment counts
		int playerId = player.objectId();
		int total = totalResets.merge(playerId, 1, Integer::sum);
		int daily = dailyResets.merge(playerId, 1, Integer::sum);
		int monthly = monthlyResets.merge(playerId, 1, Integer::sum);

		// Award Rebirth reward: 5 Coin of Luck (4037)
		if (itemRepository != null && itemTable != null) {
			itemTable.get(4037).ifPresent(tmpl -> {
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, playerId, 5);
				itemRepository.insert(item, "ResetReward");
			});
		}

		saveRankingToDatabase(playerId, daily, monthly);

		log.info("Player {} (ID: {}) successfully performed Reset #{}. Daily: {}, Monthly: {}",
				player.name(), playerId, total, daily, monthly);
		return true;
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

	public String generateHtml(PlayerCharacter player, Map<Integer, Long> inventoryCounts) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Character Rebirth System</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Character Reset / Rebirth ===</font><br>");
		sb.append("Current Resets: <font color=\"00FF00\">").append(getTotalResets(player.objectId())).append("</font><br><br>");

		sb.append("<table width=280 bgcolor=000000>");
		sb.append("<tr><td><font color=\"LEVEL\">Requirements:</font></td></tr>");
		sb.append("<tr><td>Level: <font color=\"").append(player.level() >= requirement.minLevel() ? "00FF00" : "FF0000")
				.append("\">").append(player.level()).append(" / ").append(requirement.minLevel()).append("</font></td></tr>");
		sb.append("<tr><td>PvP Kills: <font color=\"").append(player.pvpKills() >= requirement.minPvP() ? "00FF00" : "FF0000")
				.append("\">").append(player.pvpKills()).append(" / ").append(requirement.minPvP()).append("</font></td></tr>");

		for (Map.Entry<Integer, Long> req : requirement.requiredItems().entrySet()) {
			long owned = inventoryCounts.getOrDefault(req.getKey(), 0L);
			sb.append("<tr><td>Cost (Item ").append(req.getKey()).append("): <font color=\"")
					.append(owned >= req.getValue() ? "00FF00" : "FF0000").append("\">")
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
