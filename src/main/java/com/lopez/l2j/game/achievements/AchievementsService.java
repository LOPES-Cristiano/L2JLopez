package com.lopez.l2j.game.achievements;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema moderno de Conquistas (Achievements) do Lineage II.
 * Avalia progresso de niveis, PvP, PK, adena, encantamentos de armas e raid bosses,
 * concedendo recompensas e persistindo na tabela player_achievement.
 */
public class AchievementsService {

	private static final Logger log = LoggerFactory.getLogger(AchievementsService.class);

	private final Map<Integer, AchievementDef> achievements = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, PlayerAchievement>> playerProgress = new ConcurrentHashMap<>();
	private final DataSource dataSource;
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	public AchievementsService(DataSource dataSource, ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this.dataSource = dataSource;
		this.itemRepository = itemRepository;
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x40000000);
		registerDefaultAchievements();
	}

	public AchievementsService() {
		this(null, null, null, null);
	}

	private void registerDefaultAchievements() {
		registerAchievement(new AchievementDef(1, "First Steps", "Reach character level 20", AchievementType.LEVEL, 20, false,
				List.of(new AchievementReward(57, 50_000L))));
		registerAchievement(new AchievementDef(2, "Experienced Adventurer", "Reach character level 40", AchievementType.LEVEL, 40, false,
				List.of(new AchievementReward(57, 500_000L))));
		registerAchievement(new AchievementDef(3, "Master of Power", "Reach character level 75", AchievementType.LEVEL, 75, false,
				List.of(new AchievementReward(57, 5_000_000L), new AchievementReward(6673, 10L))));
		registerAchievement(new AchievementDef(4, "Grand Champion", "Reach maximum level 80", AchievementType.LEVEL, 80, false,
				List.of(new AchievementReward(57, 20_000_000L), new AchievementReward(4037, 5L))));
		registerAchievement(new AchievementDef(5, "First Blood", "Eliminate your first opponent in PvP", AchievementType.PVP_KILLS, 1, false,
				List.of(new AchievementReward(57, 100_000L))));
		registerAchievement(new AchievementDef(6, "Gladiator", "Achieve 50 PvP kills", AchievementType.PVP_KILLS, 50, false,
				List.of(new AchievementReward(57, 5_000_000L), new AchievementReward(6673, 20L))));
		registerAchievement(new AchievementDef(7, "War Legend", "Achieve 200 PvP kills", AchievementType.PVP_KILLS, 200, false,
				List.of(new AchievementReward(57, 20_000_000L), new AchievementReward(4037, 10L))));
		registerAchievement(new AchievementDef(8, "Outlaw", "Achieve 10 PK kills", AchievementType.PK_KILLS, 10, false,
				List.of(new AchievementReward(57, 2_000_000L))));
		registerAchievement(new AchievementDef(9, "Millionaire", "Accumulate at least 10,000,000 Adena", AchievementType.ADENA, 10_000_000L, false,
				List.of(new AchievementReward(6673, 5L))));
		registerAchievement(new AchievementDef(10, "Billionaire", "Accumulate at least 500,000,000 Adena", AchievementType.ADENA, 500_000_000L, false,
				List.of(new AchievementReward(4037, 25L))));
		registerAchievement(new AchievementDef(11, "Enchanted Arsenal", "Enchant a weapon to +10 or higher", AchievementType.WEAPON_ENCHANT, 10, false,
				List.of(new AchievementReward(6577, 5L))));
		registerAchievement(new AchievementDef(12, "Raid Slayer", "Participate in defeating at least 5 Raid Bosses", AchievementType.RAID_KILLS, 5, false,
				List.of(new AchievementReward(4037, 10L))));
	}

	public void registerAchievement(AchievementDef def) {
		achievements.put(def.id(), def);
	}

	public Map<Integer, AchievementDef> getAchievements() {
		return Collections.unmodifiableMap(achievements);
	}

	public AchievementDef getAchievement(int id) {
		return achievements.get(id);
	}

	public Map<Integer, PlayerAchievement> getPlayerAchievements(int playerId) {
		Map<Integer, PlayerAchievement> map = playerProgress.get(playerId);
		if (map == null) {
			map = loadFromDatabase(playerId);
			playerProgress.put(playerId, map);
		}
		return Collections.unmodifiableMap(map);
	}

	public boolean hasCompleted(int playerId, int achievementId) {
		Map<Integer, PlayerAchievement> completed = getPlayerAchievements(playerId);
		return completed.containsKey(achievementId);
	}

	public boolean meetsRequirement(AchievementDef def, PlayerCharacter player, long currentAdena, int raidKills, int weaponEnchant, long onlineHours) {
		return switch (def.type()) {
			case LEVEL -> player.level() >= def.requiredValue();
			case PVP_KILLS -> player.pvpKills() >= def.requiredValue();
			case PK_KILLS -> player.pkKills() >= def.requiredValue();
			case ADENA -> currentAdena >= def.requiredValue();
			case WEAPON_ENCHANT -> weaponEnchant >= def.requiredValue();
			case RAID_KILLS -> raidKills >= def.requiredValue();
			case NOBLESSE -> player.title() != null && player.title().toLowerCase().contains("noble");
			case HERO -> player.title() != null && player.title().toLowerCase().contains("hero");
			case CLAN_LEVEL -> player.clanId() > 0;
			case ONLINE_TIME -> onlineHours >= def.requiredValue();
		};
	}

	public List<AchievementDef> checkAvailableAchievements(PlayerCharacter player, long currentAdena, int raidKills, int weaponEnchant, long onlineHours) {
		List<AchievementDef> ready = new ArrayList<>();
		Map<Integer, PlayerAchievement> completed = getPlayerAchievements(player.objectId());

		for (AchievementDef def : achievements.values()) {
			if (!def.repeatable() && completed.containsKey(def.id())) {
				continue;
			}
			if (meetsRequirement(def, player, currentAdena, raidKills, weaponEnchant, onlineHours)) {
				ready.add(def);
			}
		}
		return ready;
	}

	public synchronized boolean claimAchievement(PlayerCharacter player, int achievementId, long currentAdena, int raidKills, int weaponEnchant, long onlineHours) {
		AchievementDef def = achievements.get(achievementId);
		if (def == null) {
			return false;
		}

		Map<Integer, PlayerAchievement> completed = playerProgress.computeIfAbsent(player.objectId(), this::loadFromDatabase);
		PlayerAchievement existing = completed.get(achievementId);
		if (!def.repeatable() && existing != null) {
			return false; // already claimed
		}

		if (!meetsRequirement(def, player, currentAdena, raidKills, weaponEnchant, onlineHours)) {
			return false; // requirements not satisfied
		}

		// Award rewards
		if (itemRepository != null && itemTable != null) {
			for (AchievementReward reward : def.rewards()) {
				itemTable.get(reward.itemId()).ifPresent(tmpl -> {
					int objectId = idFactory.nextId();
					ItemInstance item = new ItemInstance(objectId, tmpl, player.objectId(), (int) reward.count());
					if (reward.enchantLevel() > 0) {
						item.enchant(reward.enchantLevel());
					}
					itemRepository.insert(item, "AchievementReward");
				});
			}
		}

		int newTimes = (existing == null ? 1 : existing.timesCompleted() + 1);
		PlayerAchievement pa = new PlayerAchievement(player.objectId(), achievementId, newTimes, Instant.now());
		completed.put(achievementId, pa);

		saveToDatabase(pa);
		log.info("Player {} (ID: {}) claimed achievement: {} (ID: {})", player.name(), player.objectId(), def.name(), achievementId);
		return true;
	}

	public String generateHtml(PlayerCharacter player, long currentAdena, int raidKills, int weaponEnchant, long onlineHours) {
		Map<Integer, PlayerAchievement> completed = getPlayerAchievements(player.objectId());
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Achievements System</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Lineage II Achievements ===</font><br>");
		sb.append("Progress: <font color=\"00FF00\">").append(completed.size()).append("</font> / ")
				.append(achievements.size()).append(" Completed<br><br>");

		sb.append("<table width=290 border=0>");
		for (AchievementDef def : achievements.values()) {
			boolean isDone = completed.containsKey(def.id());
			boolean canClaim = !isDone && meetsRequirement(def, player, currentAdena, raidKills, weaponEnchant, onlineHours);

			sb.append("<tr>");
			sb.append("<td width=190>");
			sb.append("<font color=\"").append(isDone ? "00FF00" : (canClaim ? "LEVEL" : "FFFFFF")).append("\">")
					.append(def.name()).append("</font><br1>");
			sb.append("<font color=\"B09878\">").append(def.description()).append("</font>");
			sb.append("</td>");
			sb.append("<td width=100 align=center>");
			if (isDone) {
				sb.append("<font color=\"00FF00\">Completed</font>");
			} else if (canClaim) {
				sb.append("<button value=\"Claim\" action=\"bypass -h achieve_claim ").append(def.id())
						.append("\" width=60 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			} else {
				sb.append("<font color=\"808080\">In Progress</font>");
			}
			sb.append("</td>");
			sb.append("</tr>");
			sb.append("<tr><td colspan=2><font color=\"555555\">------------------------------------</font></td></tr>");
		}
		sb.append("</table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	private Map<Integer, PlayerAchievement> loadFromDatabase(int playerId) {
		Map<Integer, PlayerAchievement> map = new ConcurrentHashMap<>();
		if (dataSource == null) {
			return map;
		}
		String sql = "SELECT achievement_id, times_completed, last_completed_at FROM player_achievement WHERE owner_id = ?";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int achId = rs.getInt("achievement_id");
					int times = rs.getInt("times_completed");
					Timestamp ts = rs.getTimestamp("last_completed_at");
					Instant instant = ts != null ? ts.toInstant() : Instant.now();
					map.put(achId, new PlayerAchievement(playerId, achId, times, instant));
				}
			}
		} catch (SQLException e) {
			log.warn("Failed to load player achievements for ownerId {}: {}", playerId, e.getMessage());
		}
		return map;
	}

	private void saveToDatabase(PlayerAchievement pa) {
		if (dataSource == null) {
			return;
		}
		String sql = "INSERT INTO player_achievement (owner_id, achievement_id, times_completed, last_completed_at) " +
				"VALUES (?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE times_completed = VALUES(times_completed), last_completed_at = VALUES(last_completed_at)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, pa.ownerId());
			ps.setInt(2, pa.achievementId());
			ps.setInt(3, pa.timesCompleted());
			ps.setTimestamp(4, Timestamp.from(pa.lastCompletedAt()));
			ps.executeUpdate();
		} catch (SQLException e) {
			log.warn("Failed to save achievement progress for ownerId {}: {}", pa.ownerId(), e.getMessage());
		}
	}
}
