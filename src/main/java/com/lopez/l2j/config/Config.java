package com.lopez.l2j.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acesso centralizado e fortemente tipado para todas as configuracoes carregadas
 * a partir dos arquivos .properties de config/ (game e login).
 * Permite tanto leitura via campos estaticos de alta performance quanto metodos
 * dinamicos (Config.getInt, Config.getBoolean, etc.) e recarregamento a quente via Config.reload().
 */
public final class Config {

	private static final Logger log = LoggerFactory.getLogger(Config.class);

	// =========================================================================
	// RATES (config/game/main/rates.properties)
	// =========================================================================
	public static float RATE_XP = 1.0f;
	public static float RATE_SP = 1.0f;
	public static float RATE_PARTY_XP = 1.0f;
	public static float RATE_PARTY_SP = 1.0f;
	public static float RATE_DROP_ADENA = 1.0f;
	public static float RATE_DROP_ITEMS = 1.0f;
	public static float RATE_DROP_SPOIL = 1.0f;
	public static float RATE_DROP_SEAL_STONES = 1.0f;
	public static float RATE_DROP_QUEST = 1.0f;
	public static float RATE_DROP_MANOR = 1.0f;
	public static float RATE_RAID_DROP_ITEMS = 1.0f;
	public static int RATE_EXTR_FISH = 1;

	public static boolean ALLOW_VIP_XPSP = false;
	public static float VIP_XP = 1.0f;
	public static float VIP_SP = 1.0f;
	public static float VIP_DROP_RATE = 1.0f;
	public static float VIP_SPOIL_RATE = 1.0f;

	// =========================================================================
	// CUSTOM & STARTER (config/game/custom/custom.properties)
	// =========================================================================
	public static int STARTING_ADENA = 0;
	public static int STARTING_AA = 0;
	public static boolean ENABLE_STARTUP_LVL = false;
	public static int STARTUP_LVL = 1;
	public static boolean ALT_SPAWN_NEW_CHAR = false;
	public static int ALT_SPAWN_X = 0;
	public static int ALT_SPAWN_Y = 0;
	public static int ALT_SPAWN_Z = 0;
	public static boolean ALLOW_MANA_POTIONS = true;
	public static int MANA_POTION_POWER = 200;
	public static boolean SHOW_HTML_WELCOME = false;
	public static boolean ALT_RECOMMEND = false;

	// =========================================================================
	// MODS & OFFLINE SHOP (config/game/custom/mods.properties)
	// =========================================================================
	public static boolean ALLOW_OFFLINE_TRADE = false;
	public static boolean ALLOW_OFFLINE_TRADE_CRAFT = false;
	public static boolean ALLOW_OFFLINE_TRADE_COLOR_NAME = true;
	public static String OFFLINE_TRADE_COLOR_NAME = "999999";
	public static boolean ALLOW_OFFLINE_TRADE_PROTECTION = true;
	public static boolean RESTORE_OFFLINE_TRADERS = true;
	public static int OFFLINE_MAX_DAYS = 72;

	public static boolean ENABLE_CAPTCHA = false;
	public static int CAPTCHA_KILLS_COUNTER = 60;
	public static int CAPTCHA_KILLS_COUNTER_RANDOMIZATION = 50;
	public static int CAPTCHA_VALIDATION_TIME = 60;
	public static int CAPTCHA_PUNISHMENT = 0;
	public static int CAPTCHA_PUNISHMENT_TIME = 60;

	public static boolean BANKING_SYSTEM_ENABLED = false;
	public static int BANKING_SYSTEM_GOLDBARS = 1;
	public static int BANKING_SYSTEM_ADENA = 500000000;

	public static boolean CHAMPION_ENABLE = false;
	public static int CHAMPION_FREQUENCY = 5;
	public static int CHAMPION_MIN_LVL = 20;
	public static int CHAMPION_MAX_LVL = 70;
	public static int CHAMPION_HP = 8;
	public static int CHAMPION_REWARDS = 8;

	public static boolean ANNOUNCE_PK_PVP = false;
	public static boolean ANNOUNCE_PK_PVP_NORMAL_MESSAGE = true;
	public static boolean ANNOUNCE_PK_PVP_PK_MESSAGE = true;

	// =========================================================================
	// PLAYER CONFIGURATION (config/game/main/player.properties)
	// =========================================================================
	public static int PLAYER_MAX_LEVEL = 81;
	public static int MAX_SUBCLASSES = 3;
	public static int SUBCLASS_MAX_LEVEL = 80;
	public static int SUBCLASS_INIT_LEVEL = 40;
	public static boolean ALT_SUBCLASS_WITHOUT_QUESTS = false;
	public static boolean ALT_SUBCLASS_EVERYWHERE = true;
	public static int INVENTORY_MAXIMUM_NO_DWARF = 80;
	public static int INVENTORY_MAXIMUM_DWARF = 100;
	public static int WAREHOUSE_SLOT_NO_DWARF = 100;
	public static int WAREHOUSE_SLOT_DWARF = 120;
	public static float ALT_WEIGHT_LIMIT = 1.0f;
	public static boolean AUTO_LEARN_SKILLS = true;
	public static int AUTO_LEARN_MAX_LEVEL = 0;
	public static boolean DELEVEL = true;
	public static boolean MAGIC_FAILURES = true;
	public static boolean SHOW_SUCCESS_CHANCE = true;
	public static boolean SHOW_DEBUFF_ONLY = true;

	public static boolean AUTO_LOOT = true;
	public static boolean AUTO_LOOT_DEFAULT = true;
	public static boolean AUTO_LOOT_RAID = false;
	public static boolean AUTO_LOOT_HERBS = false;
	public static boolean AUTO_LOOT_ADENA = true;

	public static int MAX_BUFFS_AMOUNT = 20;
	public static int MAX_DANCES_AMOUNT = 12;

	// =========================================================================
	// ALT GAME CONFIGURATION (config/game/main/altgame.properties)
	// =========================================================================
	public static boolean ALT_GAME_TIREDNESS = false;
	public static boolean ALT_GAME_FREIGHT = false;
	public static String ANNOUNCE_MODE = "l2j";
	public static boolean ANNOUNCE_GM_NAME = true;
	public static int CHANCE_TO_LEVEL_SA = 32;
	public static boolean DESTROY_PLAYER_DROPPED_ITEM = false;
	public static int AUTO_DESTROY_DROPPED_ITEM_AFTER = 300;

	// =========================================================================
	// OPTIONS (config/game/main/options.properties)
	// =========================================================================
	public static boolean ALLOW_CURSED_WEAPONS = true;
	public static boolean ALLOW_MANOR = true;
	public static boolean ALLOW_SEVEN_SIGNS = true;
	public static boolean ALLOW_OLYMPIAD = true;
	public static boolean ALLOW_WATER = true;
	public static boolean ALLOW_BOAT = true;
	public static boolean ALLOW_FISHING = true;
	public static boolean ALLOW_PET_RENT = true;
	public static int DELETE_CHAR_AFTER_DAYS = 7;

	// =========================================================================
	// NETWORK & GAMESERVER (config/game/main/gameserver.properties, network.properties)
	// =========================================================================
	public static String SERVER_NAME = "L2JLopez";
	public static int GAMESERVER_PORT = 7777;
	public static String GAMESERVER_HOSTNAME = "127.0.0.1";
	public static String EXTERNAL_HOSTNAME = "127.0.0.1";
	public static String INTERNAL_HOSTNAME = "127.0.0.1";
	public static int LOGIN_PORT = 9014;
	public static String LOGIN_HOST = "127.0.0.1";
	public static int MAXIMUM_ONLINE_USERS = 500;
	public static int CHAR_MAX_NUMBER = 7;
	public static String DATAPACK_ROOT = ".";

	// =========================================================================
	// LOGIN SERVER (config/login/authserver.properties, network.properties)
	// =========================================================================
	public static int AUTH_SERVER_PORT = 2106;
	public static String AUTH_SERVER_HOSTNAME = "0.0.0.0";
	public static boolean AUTO_CREATE_ACCOUNTS = true;
	public static int MAX_ACCOUNT_CREATIONS_PER_IP = 2;
	public static boolean SHOW_LICENCE = true;
	public static boolean DDOS_PROTECTION = true;
	public static int LOGIN_TRY_BEFORE_BAN = 5;
	public static int LOGIN_BLOCK_AFTER_BAN = 600;

	static {
		load();
	}

	private Config() {
	}

	public static synchronized void load() {
		ConfigLoader.load();
		init();
	}

	public static synchronized void reload() {
		log.info("Recarregando configuracoes a partir dos arquivos .properties...");
		ConfigLoader.load();
		init();
		log.info("Configuracoes recarregadas com sucesso!");
	}

	private static void init() {
		// Rates
		RATE_XP = ConfigLoader.getFloat("RateXp", 1.0f);
		RATE_SP = ConfigLoader.getFloat("RateSp", 1.0f);
		RATE_PARTY_XP = ConfigLoader.getFloat("RatePartyXp", 1.0f);
		RATE_PARTY_SP = ConfigLoader.getFloat("RatePartySp", 1.0f);
		RATE_DROP_ADENA = ConfigLoader.getFloat("RateDropAdena", 1.0f);
		RATE_DROP_ITEMS = ConfigLoader.getFloat("RateDropItems", 1.0f);
		RATE_DROP_SPOIL = ConfigLoader.getFloat("RateDropSpoil", 1.0f);
		RATE_DROP_SEAL_STONES = ConfigLoader.getFloat("RateDropSealStones", 1.0f);
		RATE_DROP_QUEST = ConfigLoader.getFloat("RateDropQuest", 1.0f);
		RATE_DROP_MANOR = ConfigLoader.getFloat("RateDropManor", 1.0f);
		RATE_RAID_DROP_ITEMS = ConfigLoader.getFloat("RateRaidDropItems", 1.0f);
		RATE_EXTR_FISH = ConfigLoader.getInt("RateExtractFish", 1);

		ALLOW_VIP_XPSP = ConfigLoader.getBoolean("AllowVipMulXpSp", false);
		VIP_XP = ConfigLoader.getFloat("VipMulXp", 1.0f);
		VIP_SP = ConfigLoader.getFloat("VipMulSp", 1.0f);
		VIP_DROP_RATE = ConfigLoader.getFloat("VipDropRate", 1.0f);
		VIP_SPOIL_RATE = ConfigLoader.getFloat("VipSpoilRate", 1.0f);

		// Custom & Starter
		STARTING_ADENA = ConfigLoader.getInt("StartingAdena", 0);
		STARTING_AA = ConfigLoader.getInt("StartingAA", 0);
		ENABLE_STARTUP_LVL = ConfigLoader.getBoolean("EnableStartupLvl", false);
		STARTUP_LVL = ConfigLoader.getInt("StartupLvl", 1);
		ALT_SPAWN_NEW_CHAR = ConfigLoader.getBoolean("AltSpawnNewChar", false);
		ALT_SPAWN_X = ConfigLoader.getInt("AltSpawnX", 0);
		ALT_SPAWN_Y = ConfigLoader.getInt("AltSpawnY", 0);
		ALT_SPAWN_Z = ConfigLoader.getInt("AltSpawnZ", 0);
		ALLOW_MANA_POTIONS = ConfigLoader.getBoolean("AllowManaPotions", true);
		MANA_POTION_POWER = ConfigLoader.getInt("ManaPotionPower", 200);
		SHOW_HTML_WELCOME = ConfigLoader.getBoolean("ShowHTMLWelcome", false);
		ALT_RECOMMEND = ConfigLoader.getBoolean("AltRecommend", false);

		// Mods & Offline
		ALLOW_OFFLINE_TRADE = ConfigLoader.getBoolean("AllowOfflineTrade", false);
		ALLOW_OFFLINE_TRADE_CRAFT = ConfigLoader.getBoolean("AllowOfflineTradeCraft", false);
		ALLOW_OFFLINE_TRADE_COLOR_NAME = ConfigLoader.getBoolean("AllowOfflineTradeColorName", true);
		OFFLINE_TRADE_COLOR_NAME = ConfigLoader.getProperty("OfflineTradeColorName", "999999");
		ALLOW_OFFLINE_TRADE_PROTECTION = ConfigLoader.getBoolean("AllowOfflineTradeProtection", true);
		RESTORE_OFFLINE_TRADERS = ConfigLoader.getBoolean("RestoreOfflineTraders", true);
		OFFLINE_MAX_DAYS = ConfigLoader.getInt("OfflineMaxDays", 72);

		ENABLE_CAPTCHA = ConfigLoader.getBoolean("EnableCaptcha", false);
		CAPTCHA_KILLS_COUNTER = ConfigLoader.getInt("KillsCounter", 60);
		CAPTCHA_KILLS_COUNTER_RANDOMIZATION = ConfigLoader.getInt("KillsCounterRandomization", 50);
		CAPTCHA_VALIDATION_TIME = ConfigLoader.getInt("ValidationTime", 60);
		CAPTCHA_PUNISHMENT = ConfigLoader.getInt("Punishment", 0);
		CAPTCHA_PUNISHMENT_TIME = ConfigLoader.getInt("PunishmentTime", 60);

		BANKING_SYSTEM_ENABLED = ConfigLoader.getBoolean("BankingEnabled", false);
		BANKING_SYSTEM_GOLDBARS = ConfigLoader.getInt("BankingGoldBarItemCount", 1);
		BANKING_SYSTEM_ADENA = ConfigLoader.getInt("BankingAdenaCount", 500000000);

		CHAMPION_ENABLE = ConfigLoader.getBoolean("ChampionEnable", false);
		CHAMPION_FREQUENCY = ConfigLoader.getInt("ChampionFrequency", 5);
		CHAMPION_MIN_LVL = ConfigLoader.getInt("ChampionMinLevel", 20);
		CHAMPION_MAX_LVL = ConfigLoader.getInt("ChampionMaxLevel", 70);
		CHAMPION_HP = ConfigLoader.getInt("ChampionHp", 8);
		CHAMPION_REWARDS = ConfigLoader.getInt("ChampionRewards", 8);

		ANNOUNCE_PK_PVP = ConfigLoader.getBoolean("AnnouncePkPvP", false);
		ANNOUNCE_PK_PVP_NORMAL_MESSAGE = ConfigLoader.getBoolean("AnnouncePkPvPNormalMessage", true);
		ANNOUNCE_PK_PVP_PK_MESSAGE = ConfigLoader.getBoolean("AnnouncePkPvPPkMessage", true);

		// Player
		PLAYER_MAX_LEVEL = ConfigLoader.getInt("PlayerMaxLevel", 81);
		MAX_SUBCLASSES = ConfigLoader.getInt("MaxSubClass", 3);
		SUBCLASS_MAX_LEVEL = ConfigLoader.getInt("SubclassMaxLevel", 80);
		SUBCLASS_INIT_LEVEL = ConfigLoader.getInt("SublcassInitLevel", 40);
		ALT_SUBCLASS_WITHOUT_QUESTS = ConfigLoader.getBoolean("AltSubClassWithoutQuests", false);
		ALT_SUBCLASS_EVERYWHERE = ConfigLoader.getBoolean("AltSubclassEverywhere", true);
		INVENTORY_MAXIMUM_NO_DWARF = ConfigLoader.getInt("MaxInventorySlotsForOther", 80);
		INVENTORY_MAXIMUM_DWARF = ConfigLoader.getInt("MaxInventorySlotsForDwarf", 100);
		WAREHOUSE_SLOT_NO_DWARF = ConfigLoader.getInt("WarehouseSlotLimitNoDwarf", 100);
		WAREHOUSE_SLOT_DWARF = ConfigLoader.getInt("WarehouseSlotLimitDwarf", 120);
		ALT_WEIGHT_LIMIT = ConfigLoader.getFloat("AltWeightLimit", 1.0f);
		AUTO_LEARN_SKILLS = ConfigLoader.getBoolean("AutoLearnSkills", true);
		AUTO_LEARN_MAX_LEVEL = ConfigLoader.getInt("AutoLearnMaxLevel", 0);
		DELEVEL = ConfigLoader.getBoolean("Delevel", true);
		MAGIC_FAILURES = ConfigLoader.getBoolean("MagicFailures", true);
		SHOW_SUCCESS_CHANCE = ConfigLoader.getBoolean("ShowSuccessChance", true);
		SHOW_DEBUFF_ONLY = ConfigLoader.getBoolean("ShowDebuffOnly", true);

		AUTO_LOOT = ConfigLoader.getBoolean("AlowAutoLoot", true);
		AUTO_LOOT_DEFAULT = ConfigLoader.getBoolean("AutoLootDefault", true);
		AUTO_LOOT_RAID = ConfigLoader.getBoolean("AutoLootRaid", false);
		AUTO_LOOT_HERBS = ConfigLoader.getBoolean("AutoLootHerbs", false);
		AUTO_LOOT_ADENA = ConfigLoader.getBoolean("AutoLootAdena", true);

		MAX_BUFFS_AMOUNT = ConfigLoader.getInt("MaxBuffsAmount", 20);
		MAX_DANCES_AMOUNT = ConfigLoader.getInt("MaxDancesAmount", 12);

		// Alt Game
		ALT_GAME_TIREDNESS = ConfigLoader.getBoolean("AltGameTiredness", false);
		ALT_GAME_FREIGHT = ConfigLoader.getBoolean("AltGameFreight", false);
		ANNOUNCE_MODE = ConfigLoader.getProperty("AnnounceMode", "l2j");
		ANNOUNCE_GM_NAME = ConfigLoader.getBoolean("AnnounceGMName", true);
		CHANCE_TO_LEVEL_SA = ConfigLoader.getInt("ChanceToLevel", 32);
		DESTROY_PLAYER_DROPPED_ITEM = ConfigLoader.getBoolean("DestroyPlayerDroppedItem", false);
		AUTO_DESTROY_DROPPED_ITEM_AFTER = ConfigLoader.getInt("AutoDestroyDroppedItemAfter", 300);

		// Options
		ALLOW_CURSED_WEAPONS = ConfigLoader.getBoolean("AllowCursedWeapons", true);
		ALLOW_MANOR = ConfigLoader.getBoolean("AllowManor", true);
		ALLOW_SEVEN_SIGNS = ConfigLoader.getBoolean("AllowSevenSigns", true);
		ALLOW_OLYMPIAD = ConfigLoader.getBoolean("AllowOlympiad", true);
		ALLOW_WATER = ConfigLoader.getBoolean("AllowWater", true);
		ALLOW_BOAT = ConfigLoader.getBoolean("AllowBoat", true);
		ALLOW_FISHING = ConfigLoader.getBoolean("AllowFishing", true);
		ALLOW_PET_RENT = ConfigLoader.getBoolean("AllowPetRent", true);
		DELETE_CHAR_AFTER_DAYS = ConfigLoader.getInt("DeleteCharAfterDays", 7);

		// Network & Gameserver
		SERVER_NAME = ConfigLoader.getProperty("ServerName", "L2JLopez");
		GAMESERVER_PORT = ConfigLoader.getInt("GameServerPort", 7777);
		GAMESERVER_HOSTNAME = ConfigLoader.getProperty("GameServerHostName", "127.0.0.1");
		EXTERNAL_HOSTNAME = ConfigLoader.getProperty("ExternalHostname", "127.0.0.1");
		INTERNAL_HOSTNAME = ConfigLoader.getProperty("InternalHostname", "127.0.0.1");
		LOGIN_PORT = ConfigLoader.getInt("LoginPort", 9014);
		LOGIN_HOST = ConfigLoader.getProperty("LoginHost", "127.0.0.1");
		MAXIMUM_ONLINE_USERS = ConfigLoader.getInt("MaximumOnlineUsers", 500);
		CHAR_MAX_NUMBER = ConfigLoader.getInt("CharMaxNumber", 7);
		DATAPACK_ROOT = ConfigLoader.getProperty("DatapackRoot", ".");

		// Login
		AUTH_SERVER_PORT = ConfigLoader.getInt("AuthServerPort", 2106);
		AUTH_SERVER_HOSTNAME = ConfigLoader.getProperty("AuthServerHostName", "0.0.0.0");
		AUTO_CREATE_ACCOUNTS = ConfigLoader.getBoolean("AutoCreateAccounts", true);
		MAX_ACCOUNT_CREATIONS_PER_IP = ConfigLoader.getInt("MaxAccountCreationsPerIP", 2);
		SHOW_LICENCE = ConfigLoader.getBoolean("ShowLicence", true);
		DDOS_PROTECTION = ConfigLoader.getBoolean("DDoSProtection", true);
		LOGIN_TRY_BEFORE_BAN = ConfigLoader.getInt("LoginTryBeforeBan", 5);
		LOGIN_BLOCK_AFTER_BAN = ConfigLoader.getInt("LoginBlockAfterBan", 600);
	}

	// =========================================================================
	// HELPER GETTERS
	// =========================================================================
	public static String getProperty(String key, String defaultValue) {
		return ConfigLoader.getProperty(key, defaultValue);
	}

	public static String getString(String key, String defaultValue) {
		return ConfigLoader.getProperty(key, defaultValue);
	}

	public static String getProperty(String key) {
		return ConfigLoader.getProperty(key);
	}

	public static String getString(String key) {
		return ConfigLoader.getProperty(key);
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		return ConfigLoader.getBoolean(key, defaultValue);
	}

	public static int getInt(String key, int defaultValue) {
		return ConfigLoader.getInt(key, defaultValue);
	}

	public static long getLong(String key, long defaultValue) {
		return ConfigLoader.getLong(key, defaultValue);
	}

	public static float getFloat(String key, float defaultValue) {
		return ConfigLoader.getFloat(key, defaultValue);
	}

	public static double getDouble(String key, double defaultValue) {
		return ConfigLoader.getDouble(key, defaultValue);
	}

	public static List<Integer> getIntList(String key, List<Integer> defaultValue) {
		String val = ConfigLoader.getProperty(key);
		if (val == null || val.isBlank()) {
			return defaultValue;
		}
		try {
			return Arrays.stream(val.split("[,;\\s]+"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Integer::parseInt)
					.toList();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int[] getIntArray(String key, int[] defaultValue) {
		List<Integer> list = getIntList(key, null);
		if (list == null) {
			return defaultValue;
		}
		return list.stream().mapToInt(Integer::intValue).toArray();
	}

	public static void setProperty(String key, String value) {
		ConfigLoader.setProperty(key, value);
		init();
	}

	public static Map<String, String> getAllProperties() {
		return ConfigLoader.getAllRawProperties();
	}
}
