package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de recompensas por tempo/login, Event Manager, Siege Reward, Clan Skills por item,
 * RaidBoss Info paginado, Custom Cancel Task e tabelas customizadas
 * (mods.properties, add-on.properties, custom.properties).
 */
@Service
public class ModRewardsService {

	private static final Logger log = LoggerFactory.getLogger(ModRewardsService.class);

	public record ItemRewardEntry(int itemId, int count) {}

	public record CancelSnapshot(int playerId, Map<Integer, Integer> cancelledSkills, long restoreAtMillis) {}

	private final Set<Integer> firstLoginRewardedPlayers = ConcurrentHashMap.newKeySet();
	private final Map<Integer, Long> lastOnlineItemRewardTime = new ConcurrentHashMap<>();
	private final Map<Integer, Long> lastOnlineSkillRewardTime = new ConcurrentHashMap<>();
	private final Map<Integer, List<CancelSnapshot>> pendingCancelRestores = new ConcurrentHashMap<>();

	// ---- Event Manager (mods.properties) ----

	public boolean isEventManagerEnabled() {
		return Config.ENABLE_EVENT_MANAGER;
	}

	public int getEventManagerNpcId() {
		return Config.EVENT_MANAGER_NPC_ID;
	}

	public boolean isEventManagerAutoSpawnEnabled() {
		return Config.ENABLE_EVENT_MANAGER && Config.ENABLE_AUTO_SPAWN;
	}

	public boolean canRegisterInEvent(PlayerCharacter player, String playerClassName) {
		if (player == null || !Config.ENABLE_EVENT_MANAGER || player.isDead() || player.isOlympiadMode()) {
			return false;
		}
		if (Config.EVENT_BLOCK_CLASS_ID != null && Config.EVENT_BLOCK_CLASS_ID.contains(player.classId())) {
			log.info("EventManager: classe ID {} bloqueada para {}", player.classId(), player.name());
			return false;
		}
		if (playerClassName != null && Config.EVENT_BLOCK_CLASS_NAMES != null && !Config.EVENT_BLOCK_CLASS_NAMES.isBlank()) {
			for (String blocked : Config.EVENT_BLOCK_CLASS_NAMES.split(",")) {
				if (blocked.trim().equalsIgnoreCase(playerClassName.trim())) {
					log.info("EventManager: classe '{}' bloqueada para {}", playerClassName, player.name());
					return false;
				}
			}
		}
		return true;
	}

	public boolean chargeEventParticipationFee(PlayerCharacter player) {
		if (!canRegisterInEvent(player, null)) {
			return false;
		}
		int feeId = Config.EVENT_PARTICIPATION_FEE_ID;
		long feeQnt = Config.EVENT_PARTICIPATION_FEE_QNT;
		if (feeId <= 0 || feeQnt <= 0) {
			return true;
		}
		if (player.inventory() == null) {
			return false;
		}
		var item = player.inventory().byItemId(feeId).orElse(null);
		if (item == null || item.count() < feeQnt) {
			return false;
		}
		return player.inventory().destroyItemByItemId(feeId, (int) Math.min(Integer.MAX_VALUE, feeQnt));
	}

	// ---- Siege Reward System & Siege Schedule (mods.properties / add-on.properties) ----

	public boolean isSiegeRewardActive() {
		return Config.ACTIVATE_SIEGE_REWARD_SYSTEM;
	}

	public boolean isSiegeRewardOnlineOnly() {
		return Config.REWARD_ONLINE_ONLY;
	}

	public List<ItemRewardEntry> getSiegeMemberRewards() {
		return parseRewardPairs(Config.REWARD_INFO, ";", ",");
	}

	public List<ItemRewardEntry> getSiegeClanLeaderRewards() {
		return parseRewardPairs(Config.REWARD_CL_INFO, ";", ",");
	}

	public int distributeSiegeRewards(List<PlayerCharacter> clanMembers, Set<Integer> onlineObjectIds) {
		if (!Config.ACTIVATE_SIEGE_REWARD_SYSTEM || clanMembers == null || clanMembers.isEmpty()) {
			return 0;
		}
		List<ItemRewardEntry> memberRewards = getSiegeMemberRewards();
		List<ItemRewardEntry> leaderRewards = getSiegeClanLeaderRewards();
		int rewardedCount = 0;
		for (PlayerCharacter member : clanMembers) {
			if (member == null) continue;
			if (Config.REWARD_ONLINE_ONLY && onlineObjectIds != null && !onlineObjectIds.contains(member.objectId())) {
				continue;
			}
			List<ItemRewardEntry> targetList = member.isClanLeader() ? leaderRewards : memberRewards;
			for (ItemRewardEntry entry : targetList) {
				grantItem(member, entry.itemId(), entry.count());
			}
			rewardedCount++;
		}
		return rewardedCount;
	}

	public long calculateNextSiegeTimeMillis(long currentMillis) {
		int days = Math.max(1, Config.DAY_TO_SIEGE);
		int hour = Math.clamp(Config.HOUR_TO_SIEGE, 0, 23);
		long basePlusDays = currentMillis + Duration.ofDays(days).toMillis();
		return basePlusDays + (hour * 3_600_000L);
	}

	// ---- First Login Rewards & Buffs & Time Rewards (mods.properties) ----

	public boolean processFirstLogin(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		boolean anyApplied = false;
		if (Config.ENABLE_FIRST_LOGIN_REWARD && firstLoginRewardedPlayers.add(player.objectId())) {
			for (ItemRewardEntry entry : parseRewardPairs(Config.FIRST_LOGIN_REWARD_TIME_ITEM, ";", "-")) {
				grantItem(player, entry.itemId(), entry.count());
				anyApplied = true;
			}
		}
		if (Config.FIRST_LOGIN_BUFFS) {
			List<Integer> buffList = player.isMage()
					? Config.FIRST_LOGIN_MAGE_BUFF_LIST
					: Config.FIRST_LOGIN_FIGHTER_BUFF_LIST;
			if (buffList != null) {
				for (Integer skillId : buffList) {
					if (skillId != null && skillId > 0) {
						player.skills().putIfAbsent(skillId, 1);
						anyApplied = true;
					}
				}
			}
			player.currentHp(player.maxHp());
			player.currentMp(player.maxMp());
			player.currentCp(player.maxCp());
		}
		return anyApplied;
	}

	public boolean tickOnlineTimeRewards(PlayerCharacter player, long nowMillis) {
		if (player == null) {
			return false;
		}
		boolean rewarded = false;
		if (Config.ENABLE_REWARD_BY_TIME && Config.REWARD_ITEM_BY_TIME_MIN > 0) {
			long intervalMs = Config.REWARD_ITEM_BY_TIME_MIN * 60_000L;
			long last = lastOnlineItemRewardTime.computeIfAbsent(player.objectId(), k -> nowMillis);
			if (nowMillis - last >= intervalMs) {
				lastOnlineItemRewardTime.put(player.objectId(), nowMillis);
				for (ItemRewardEntry entry : parseRewardPairs(Config.REWARD_ITEM_BY_TIME_ITEM, ";", "-")) {
					grantItem(player, entry.itemId(), entry.count());
					rewarded = true;
				}
			}
		}
		if (Config.ENABLE_SKILL_REWARD_BY_TIME && Config.REWARD_SKILL_TIME > 0 && Config.REWARD_SKILL_ID > 0) {
			long intervalMs = Config.REWARD_SKILL_TIME * 60_000L;
			long last = lastOnlineSkillRewardTime.computeIfAbsent(player.objectId(), k -> nowMillis);
			if (nowMillis - last >= intervalMs) {
				lastOnlineSkillRewardTime.put(player.objectId(), nowMillis);
				int curLvl = player.skillLevel(Config.REWARD_SKILL_ID);
				if (curLvl < Config.REWARD_SKILL_MAX_LVL) {
					player.skills().put(Config.REWARD_SKILL_ID, curLvl + 1);
					rewarded = true;
				}
			}
		}
		return rewarded;
	}

	// ---- Clan Skills by Item (add-on.properties) ----

	public boolean grantClanSkillsByItem(PlayerCharacter player) {
		if (player == null || !player.isClanLeader()) {
			return false;
		}
		int itemId = Config.CLAN_SKILL_BY_ITEM;
		List<Integer> skillIds = Config.CLAN_SKILL_ID;
		if (itemId <= 0 || skillIds == null || skillIds.isEmpty()) {
			return false;
		}
		if (player.inventory() != null && !player.inventory().items().isEmpty()) {
			if (!player.inventory().destroyItemByItemId(itemId, 1)) {
				return false;
			}
		}
		Clan clan = player.getClan();
		for (Integer skillId : skillIds) {
			if (skillId != null && skillId > 0) {
				player.skills().put(skillId, 1);
				if (clan != null) {
					clan.skills().put(skillId, 1);
				}
			}
		}
		log.info("ClanSkillByItem: {} skills concedidas ao cla de {}", skillIds.size(), player.name());
		return true;
	}

	// ---- Raid Boss Info & Drop Page Limits (add-on.properties) ----

	public List<Integer> getConfiguredRaidBossIds() {
		if (Config.RAID_BOSS_IDS == null || Config.RAID_BOSS_IDS.isBlank()) {
			return List.of();
		}
		List<Integer> ids = new ArrayList<>();
		for (String token : Config.RAID_BOSS_IDS.split(",")) {
			String clean = token.trim();
			if (!clean.isEmpty()) {
				try {
					ids.add(Integer.parseInt(clean));
				} catch (NumberFormatException ignored) {}
			}
		}
		return ids;
	}

	public int getRaidBossInfoPageLimit() {
		return Math.max(1, Config.RAID_BOSS_INFO_PAGE_LIMIT);
	}

	public int getRaidBossDropPageLimit() {
		return Math.max(1, Config.RAID_BOSS_DROP_PAGE_LIMIT);
	}

	public String formatRaidBossDate(long respawnTimeMillis) {
		String pattern = (Config.RAID_BOSS_DATE_FORMAT != null && !Config.RAID_BOSS_DATE_FORMAT.isBlank())
				? Config.RAID_BOSS_DATE_FORMAT
				: "dd/MM/yyyy HH:mm";
		try {
			return new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(respawnTimeMillis));
		} catch (Exception e) {
			return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(new Date(respawnTimeMillis));
		}
	}

	public List<Integer> getRaidBossIdsPage(int page) {
		List<Integer> all = getConfiguredRaidBossIds();
		if (all.isEmpty()) {
			return List.of();
		}
		int limit = getRaidBossInfoPageLimit();
		int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / limit));
		int curPage = Math.clamp(page, 1, totalPages);
		int from = (curPage - 1) * limit;
		int to = Math.min(from + limit, all.size());
		return all.subList(from, to);
	}

	// ---- Custom Cancel Task & Hero Subclass Skill (add-on.properties) ----

	public boolean isCustomCancelTaskEnabled() {
		return Config.ALLOW_CUSTOM_CANCEL_TASK && Config.CUSTOM_CANCEL_SECONDS > 0;
	}

	public int getCustomCancelSeconds() {
		return Math.max(1, Config.CUSTOM_CANCEL_SECONDS);
	}

	public void scheduleCancelRestore(PlayerCharacter target, Map<Integer, Integer> removedBuffs) {
		if (target == null || removedBuffs == null || removedBuffs.isEmpty() || !isCustomCancelTaskEnabled()) {
			return;
		}
		long restoreAt = System.currentTimeMillis() + (getCustomCancelSeconds() * 1000L);
		pendingCancelRestores
				.computeIfAbsent(target.objectId(), k -> new ArrayList<>())
				.add(new CancelSnapshot(target.objectId(), Map.copyOf(removedBuffs), restoreAt));
	}

	public int restoreExpiredCancels(PlayerCharacter target, long nowMillis) {
		if (target == null || !Config.ALLOW_CUSTOM_CANCEL_TASK) {
			return 0;
		}
		List<CancelSnapshot> list = pendingCancelRestores.get(target.objectId());
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int restored = 0;
		var it = list.iterator();
		while (it.hasNext()) {
			CancelSnapshot snap = it.next();
			if (nowMillis >= snap.restoreAtMillis()) {
				for (var entry : snap.cancelledSkills().entrySet()) {
					target.skills().putIfAbsent(entry.getKey(), entry.getValue());
					restored++;
				}
				it.remove();
			}
		}
		return restored;
	}

	public boolean canKeepHeroSkillsOnSubClass(PlayerCharacter player) {
		if (player == null || !player.isHero()) {
			return false;
		}
		return !player.isSubClassActive() || Config.ALLOW_HERO_SKILL_ON_SUB;
	}

	// ---- Custom Rates, Boss Mana, Custom Tables & Tickets (custom.properties) ----

	public long calculateAncientAdenaDrop(long baseAmount) {
		int rate = Math.max(1, Config.RATE_DROP_ANCIENT_ADENA);
		return Math.max(0L, baseAmount * rate);
	}

	public boolean hasBossEnoughManaToCast(double currentMp, double skillMpConsume) {
		int minMana = Math.max(0, Config.MIN_BOSS_MANA_TO_CAST);
		return currentMp >= Math.max(minMana, skillMpConsume);
	}

	public Map<String, Boolean> getCustomTablesLoadState() {
		return Map.of(
				"custom_teleport", Config.LOAD_CUSTOM_TELEPORTS,
				"custom_etcitem_armor_weapon", Config.LOAD_CUSTOM_ITEM_TABLES,
				"custom_npc", Config.LOAD_CUSTOM_NPC_TABLE,
				"custom_droplist", Config.LOAD_CUSTOM_DROPLIST_TABLE,
				"custom_armorsets", Config.LOAD_CUSTOM_ARMOR_SET_TABLE,
				"custom_spawnlist", Config.LOAD_CUSTOM_SPAWNLIST_TABLE);
	}

	public int getPetTicketId() {
		return Config.PET_TICKET_ID;
	}

	public int getSpecialPetTicketId() {
		return Config.SPECIAL_PET_TICKET_ID;
	}

	public int getAuctionBidItemId() {
		return Config.AUCTION_BID_ITEM_ID;
	}

	// ---- Helpers ----

	public static List<ItemRewardEntry> parseRewardPairs(String raw, String entryDelimiter, String kvDelimiter) {
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		List<ItemRewardEntry> list = new ArrayList<>();
		for (String token : raw.split(entryDelimiter)) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) continue;
			String[] kv = trimmed.split(kvDelimiter);
			if (kv.length >= 2) {
				try {
					int itemId = Integer.parseInt(kv[0].trim());
					int count = Integer.parseInt(kv[1].trim());
					if (itemId > 0 && count > 0) {
						list.add(new ItemRewardEntry(itemId, count));
					}
				} catch (NumberFormatException ignored) {}
			}
		}
		return list;
	}

	private void grantItem(PlayerCharacter player, int itemId, int count) {
		if (player == null || player.inventory() == null || itemId <= 0 || count <= 0) {
			return;
		}
		var existing = player.inventory().byItemId(itemId).orElse(null);
		if (existing != null && existing.stackable()) {
			existing.count(existing.count() + count);
		} else {
			int nextObjId = (player.objectId() * 1000) + player.inventory().items().size() + 1;
			player.inventory().add(new com.lopez.l2j.game.item.ItemInstance(
					nextObjId, player.objectId(), itemId, count, 0,
					com.lopez.l2j.game.item.ItemInstance.Location.INVENTORY, 0, true));
		}
	}
}
