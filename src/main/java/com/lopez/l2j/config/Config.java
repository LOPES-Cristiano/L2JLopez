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
	public static boolean ANNOUNCE_CLASS_CHANGE = false;
	public static boolean ANNOUNCE_CLASS_CHANGE_AROUND = false;
	public static boolean SHOW_CLASS_CHANGE_MESSAGE = true;
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
	public static boolean ENABLE_GEODATA = false;

	// =========================================================================
	// CHAT & SOCIAL (config/game/main/options.properties)
	// =========================================================================
	public static String GLOBAL_CHAT = "REGION";
	public static String TRADE_CHAT = "REGION";
	public static boolean USE_CHAT_FILTER = false;
	public static String CHAT_FILTER_CHARS = "...";
	public static boolean ALLOW_MULTILINE_CHAT = false;
	public static int CHAT_LENGTH = 120;
	public static int CHAT_FILTER_KARMA = 0;
	public static boolean LOG_CHAT_ON_FILE = true;
	public static int SHOUT_CHAT_REUSE_DELAY = 1;
	public static int TRADE_CHAT_REUSE_DELAY = 1;
	public static int HERO_CHAT_REUSE_DELAY = 10;
	public static boolean REGION_CHAT_ALSO_BLOCKED = false;
	public static int SHOUT_CHAT_LEVEL = 1;
	public static int TRADE_CHAT_LEVEL = 1;

	// Petitions
	public static boolean PETITIONING_ALLOWED = true;
	public static int MAX_PETITIONS_PER_PLAYER = 5;
	public static int MAX_PETITIONS_PENDING = 25;
	public static boolean SEND_PAGE_ON_PETITION = false;
	public static boolean PETITIONING_NEED_GM_ONLINE = true;
	public static boolean MAIL_STORE_DELETED_LETTERS = false;

	// =========================================================================
	// COMBAT, ZONES & GEODATA (config/game/main/options.properties)
	// =========================================================================
	public static int ZONE_TOWN = 0;
	public static boolean USE_BOW_DISTANCE_PENALTY = false;
	public static float MAX_BOW_DISTANCE_PENALTY = 0.6f;
	public static boolean FALL_DOWN_ON_DEATH = true;
	public static int COORD_SYNCHRONIZE = -1;
	public static boolean AUTO_DELETE_INVALID_QUEST_DATA = true;
	public static int CHARACTER_DATA_STORE_INTERVAL = 15;
	public static boolean LAZY_ITEMS_UPDATE = false;
	public static boolean UPDATE_ITEMS_ON_CHAR_STORE = false;
	public static boolean RESTORE_PLAYER_INSTANCE = false;
	public static boolean ALLOW_SUMMON_TO_INSTANCE = false;
	public static boolean ALLOW_GUARDS = true;
	public static boolean ALLOW_NPC_WALKERS = true;
	public static boolean ALLOW_WEAR = true;
	public static boolean ENABLE_PATH_FINDING = true;
	public static String PATH_FINDING_MODE = "Pathnode";
	public static String GEO_CORRECT_Z = "All";
	public static int MAX_PATH_LENGTH = 800;
	public static int Z_AXIS_DENSITY = 12;
	public static boolean RESET_TO_BASE_CLASS_IF_FAIL = true;
	public static boolean ENABLE_RESTART = false;
	public static String RESTART_TIME = "06:20:00";
	public static int RESTART_WARN_TIME = 10;

	// =========================================================================
	// PARTY (config/game/main/options.properties)
	// =========================================================================
	public static String PARTY_XP_CUTOFF_METHOD = "auto";
	public static double PARTY_XP_CUTOFF_PERCENT = 3.0;
	public static int PARTY_XP_CUTOFF_LEVEL = 30;
	public static double ALT_PARTY_RANGE = 1600.0;
	public static double ALT_PARTY_RANGE2 = 1400.0;
	public static boolean PARTY_LEVEL_LIMIT = true;
	public static int PARTY_MAX_LEVEL_DIFFERENCE = 10;

	// =========================================================================
	// CLAN & ALLIANCE (config/game/main/options.properties)
	// =========================================================================
	public static int LVL_FOR_USE_AUCTION = 2;
	public static int DAYS_BEFORE_JOIN_A_CLAN = 1;
	public static int DAYS_BEFORE_CREATE_A_CLAN = 1;
	public static int ALT_CLAN_MEMBERS_FOR_WAR = 15;
	public static int REPUTATION_SCORE_PER_KILL = 1;
	public static int DAYS_TO_PASS_TO_DISSOLVE_A_CLAN = 7;
	public static int DAYS_BEFORE_JOIN_ALLY_WHEN_LEAVED = 1;
	public static int DAYS_BEFORE_JOIN_ALLY_WHEN_DISMISSED = 1;
	public static int DAYS_BEFORE_ACCEPT_NEW_CLAN_WHEN_DISMISSED = 1;
	public static int DAYS_BEFORE_CREATE_NEW_ALLY_WHEN_DISSOLVED = 1;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY = 3;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLAN_WH = false;
	public static int ALT_CLAN_WAR_PENALTY_WHEN_ENDED = 5;
	public static int MAX_MEMBERS_CLAN_0 = 10;
	public static int MAX_MEMBERS_CLAN_1 = 15;
	public static int MAX_MEMBERS_CLAN_2 = 20;
	public static int MAX_MEMBERS_CLAN_3 = 30;
	public static int MAX_MEMBERS_CLAN_4 = 40;
	public static int MAX_MEMBERS_CLAN_5 = 40;
	public static int MAX_MEMBERS_CLAN_6 = 40;
	public static int MAX_MEMBERS_CLAN_7 = 40;
	public static int MAX_MEMBERS_CLAN_8 = 40;
	public static int MAX_MEMBERS_ROYALS = 20;
	public static int MAX_MEMBERS_KNIGHTS = 10;
	public static boolean REMOVE_CASTLE_CIRCULAR = true;
	public static int MIN_LEVEL_TO_CREATE_PLEDGE = 10;
	public static int ALT_CLAN_LEADER_DATE_CHANGE = 3;
	public static String ALT_CLAN_LEADER_HOUR_CHANGE = "00:00:00";
	public static boolean ALT_CLAN_LEADER_INSTANT_ACTIVATION = false;

	// =========================================================================
	// DROPS FINE ADJUSTS & ECONOMY (config/game/main/options.properties)
	// =========================================================================
	public static boolean MULTIPLE_ITEM_DROP = true;
	public static boolean PRECISE_DROP_CALCULATION = false;
	public static boolean USE_DEEP_BLUE_DROP_RULES = true;
	public static String PICKUP_FULL_INVENTORY = "drop";
	public static boolean L2OFF_ADENA_PROTECTION = false;
	public static boolean SET_MAX_ETC_ITEM_SELL = false;
	public static int SET_MAX_ETC_ITEM_SELL_QNT = 10000;

	// =========================================================================
	// COMMUNITY BOARD (config/game/main/options.properties)
	// =========================================================================
	public static String COMMUNITY_TYPE = "Full";
	public static String BBS_DEFAULT = "_bbshome";
	public static boolean SHOW_LEVEL_ON_COMMUNITY_BOARD = false;
	public static boolean SHOW_STATUS_ON_COMMUNITY_BOARD = true;
	public static int NAME_PAGE_SIZE_ON_COMMUNITY_BOARD = 50;
	public static int NAME_PER_ROW_ON_COMMUNITY_BOARD = 5;
	public static boolean CUSTOM_COMMUNITY_BOARD = true;
	public static String COMMUNITY_BUFFER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
	public static String GATEKEEPER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
	public static String RESTRICT_CB_WHEN = "JAIL";
	public static boolean ONLINE_COMMUNITY_BOARD = true;
	public static boolean COLOR_COMMUNITY_BOARD = false;
	public static boolean SHOW_CURSED_WEAPON_OWNER = false;
	public static boolean SHOW_KARMA_PLAYERS = false;
	public static boolean SHOW_JAILED_PLAYERS = false;
	public static boolean SHOW_LEGEND = false;
	public static boolean SHOW_CLAN_LEADER = false;
	public static int SHOW_CLAN_LEADER_AT_CLAN_LEVEL = 3;
	public static String DISABLED_PAGES = "";

	// =========================================================================
	// RATES EXTENDED (config/game/main/rates.properties)
	// =========================================================================
	public static float RATE_QUESTS_REWARD_EXP_SP = 1.0f;
	public static float RATE_QUESTS_REWARD_ADENA = 1.0f;
	public static float RATE_QUESTS_REWARD_ITEMS = 1.0f;
	public static float RATE_CRAFT_COST = 1.0f;
	public static float RATE_CONSUMABLE_COST = 1.0f;
	public static float RATE_SIEGE_GUARDS_PRICE = 1.0f;
	public static float RATE_RUN_SPEED = 1.0f;
	public static float SIN_EATER_XP_RATE = 1.0f;
	public static float PET_XP_RATE = 1.0f;
	public static float PET_FOOD_RATE = 1.0f;
	public static int KARMA_DROP_LIMIT = 10;
	public static int KARMA_RATE_DROP = 70;
	public static int KARMA_RATE_DROP_ITEM = 50;
	public static int KARMA_RATE_DROP_EQUIP = 40;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON = 10;
	public static float RATE_KARMA_EXP_LOST = 1.0f;
	public static float RATE_COMMON_HERBS = 15.0f;
	public static float RATE_HP_MP_HERBS = 10.0f;
	public static float RATE_GREATER_HERBS = 4.0f;
	public static float RATE_SUPERIOR_HERBS = 0.8f;
	public static float RATE_SPECIAL_HERBS = 0.2f;

	// Boss Rates (config/game/main/rates.properties)
	public static float ADENA_BOSS = 1.0f;
	public static float ADENA_RAID = 1.0f;
	public static float ADENA_MINON = 1.0f;
	public static float JEWEL_BOSS = 1.0f;
	public static float ITEMS_BOSS = 1.0f;
	public static float ITEMS_RAID = 1.0f;
	public static float ITEMS_MINON = 1.0f;
	public static float SPOIL_BOSS = 1.0f;
	public static float SPOIL_RAID = 1.0f;
	public static float SPOIL_MINON = 1.0f;

	// =========================================================================
	// INVENTORY & SLOTS (config/game/main/player.properties)
	// =========================================================================
	public static int MAX_INVENTORY_SLOTS_FOR_OTHER = 80;
	public static int MAX_INVENTORY_SLOTS_FOR_DWARF = 100;
	public static int MAX_INVENTORY_SLOTS_FOR_GM = 250;
	public static int MAXIMUM_SLOTS_FOR_PET = 12;
	public static boolean DESTROY_PLAYER_INVENTORY_DROP = false;
	public static boolean ALLOW_DISCARD_ITEM = true;

	// Karma & PK (config/game/main/player.properties)
	public static int MIN_KARMA = 240;
	public static int MAX_KARMA = 10000;
	public static float KARMA_RATE = 1.0f;
	public static int XP_DIVIDER = 260;
	public static boolean CAN_GM_DROP_EQUIPMENT = false;
	public static int MINIMUM_PK_REQUIRED_TO_DROP = 5;
	public static int BASE_KARMA_LOST = 0;
	public static boolean AWARD_PK_KILL_PVP_POINT = true;

	// =========================================================================
	// ENCHANT SYSTEM (config/game/main/rates.properties)
	// =========================================================================
	public static boolean ALLOW_CRYSTAL_SCROLL = false;
	public static String NORMAL_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
	public static String NORMAL_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
	public static String NORMAL_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,95;5,90;6,85;7,80;8,75;9,70;10,65;11,60;12,55;13,50;14,45;15,40;16,35;";
	public static String BLESS_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
	public static String BLESS_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
	public static String BLESS_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
	public static String CRYSTAL_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;";
	public static String CRYSTAL_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;";
	public static String CRYSTAL_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
	public static int ENCHANT_MAX_WEAPON_NORMAL = 0;
	public static int ENCHANT_MAX_ARMOR_NORMAL = 0;
	public static int ENCHANT_MAX_JEWELRY_NORMAL = 0;
	public static int ENCHANT_MAX_WEAPON_BLESSED = 0;
	public static int ENCHANT_MAX_ARMOR_BLESSED = 0;
	public static int ENCHANT_MAX_JEWELRY_BLESSED = 0;
	public static int ENCHANT_MAX_WEAPON_CRYSTAL = 0;
	public static int ENCHANT_MAX_ARMOR_CRYSTAL = 0;
	public static int ENCHANT_MAX_JEWELRY_CRYSTAL = 0;
	public static int ENCHANT_OVER_CHANT_CHECK = 0;
	public static boolean CHECK_ENCHANT_LEVEL_EQUIP = true;
	public static int ENCHANT_SAFE_MAX = 3;
	public static int ENCHANT_SAFE_MAX_FULL = 4;
	public static boolean ALT_ENC_LVL_AFTER_FAIL = false;
	public static boolean ENCHANT_ROLL_BACK = false;
	public static int ENCHANT_ROLL_BACK_VALUE = 0;
	public static boolean ENCHANT_DWARF_SYSTEM = false;
	public static int ENCHANT_DWARF_1_ENCHANT_LEVEL = 8;
	public static int ENCHANT_DWARF_2_ENCHANT_LEVEL = 10;
	public static int ENCHANT_DWARF_3_ENCHANT_LEVEL = 12;
	public static int ENCHANT_DWARF_1_CHANCE = 15;
	public static int ENCHANT_DWARF_2_CHANCE = 15;
	public static int ENCHANT_DWARF_3_CHANCE = 15;

	// =========================================================================
	// EQUIPMENT RESTRICTIONS (config/game/custom/equipments.properties)
	// =========================================================================
	public static boolean ALLOW_LIGHT_USE_HEAVY = true;
	public static List<Integer> NOT_ALLOWED_USE_HEAVY = List.of(8, 9, 23, 24, 36, 37, 92, 93, 101, 102, 108, 109);
	public static boolean ALLOW_HEAVY_USE_LIGHT = true;
	public static List<Integer> NOT_ALLOWED_USE_LIGHT = List.of(3, 4, 5, 6, 19, 20, 21, 32, 33, 34, 90, 91, 99, 100, 106, 107, 112);
	public static boolean ALT_DISABLE_BOW = false;
	public static List<Integer> DISABLE_BOW_FOR_CLASSES = List.of(88, 89);
	public static boolean ALT_DISABLE_DAGGER = false;
	public static List<Integer> DISABLE_DAGGER_FOR_CLASSES = List.of(90, 91);
	public static boolean ALT_DISABLE_SWORD = false;
	public static List<Integer> DISABLE_SWORD_FOR_CLASSES = List.of(92, 93);
	public static boolean ALT_DISABLE_BLUNT = false;
	public static List<Integer> DISABLE_BLUNT_FOR_CLASSES = List.of(94, 95);
	public static boolean ALT_DISABLE_DUAL = false;
	public static List<Integer> DISABLE_DUAL_FOR_CLASSES = List.of(96, 97);
	public static boolean ALT_DISABLE_POLLE = false;
	public static List<Integer> DISABLE_POLLE_FOR_CLASSES = List.of(98, 99);
	public static boolean ALT_DISABLE_BIG_SWORD = false;
	public static List<Integer> DISABLE_BIG_SWORD_FOR_CLASSES = List.of(100, 101);

	// Custom Private Store & PvP Mods (config/game/custom/add-on.properties)
	public static boolean SELL_BY_ITEM = false;
	public static int SELL_ITEM = 57;
	public static String COIN_TEXT = "Adena";
	public static boolean ALLOW_QUAKE_SYSTEM = false;
	public static boolean QUAKE_RESET_ON_DIE = true;
	public static boolean WAR_LEGEND_AURA = false;
	public static int KILLS_TO_GET_WAR_LEGEND_AURA = 30;
	public static boolean ALLOW_HERO_SKILL_ON_SUB = false;
	public static boolean ALLOW_CUSTOM_CANCEL_TASK = false;
	public static int CUSTOM_CANCEL_SECONDS = 5;

	// =========================================================================
	// STATS CAPS, HEROES & CRAFTING (config/game/main/player.properties)
	// =========================================================================
	public static int MAX_P_ATK_SPEED = 9999;
	public static int MAX_M_ATK_SPEED = 9999;
	public static int MAX_RUN_SPEED = 9999;
	public static int MAX_EVASION = 200;
	public static int MINIMUM_HIT_TIME = 330;
	public static int ALT_P_CRITICAL_CAP = 500;
	public static int ALT_M_CRITICAL_CAP = 200;
	public static boolean STRICT_HERO_SYSTEM = true;
	public static boolean HERO_WEAPONS_CAN_BE_ENCHANTED = true;
	public static boolean LOG_IS_HERO_NO_CLAN = false;
	public static boolean CRAFTING_ENABLED = true;
	public static boolean ALT_GAME_CREATION = false;
	public static int ALT_GAME_CREATION_SPEED = 1;
	public static int ALT_GAME_CREATION_RATE_XP = 1;
	public static int ALT_GAME_CREATION_RATE_SP = 1;
	public static int DWARF_RECIPE_LIMIT = 50;
	public static int COMMON_RECIPE_LIMIT = 50;
	public static boolean SUBCLASS_WITH_ITEM_AND_NO_QUEST = false;
	public static boolean SUBCLASS_WITH_CUSTOM_ITEM = false;
	public static int SUBCLASS_WITH_CUSTOM_ITEM_ID = 57;
	public static int SUBCLASS_WITH_CUSTOM_ITEM_COUNT = 1000000;
	public static boolean ALT_SUBCLASS_SKILLS = false;
	public static boolean INCREASE_WEIGHT_LIMIT_BY_LEVEL = true;
	public static int PLAYER_SPAWN_PROTECTION = 0;
	public static int PLAYER_FAKE_DEATH_UP_PROTECTION = 0;
	public static int DEATH_PENALTY_CHANCE = 20;

	// Custom properties (config/game/custom/custom.properties)
	public static int WEAR_DELAY = 5;
	public static int WEAR_PRICE = 10;
	public static boolean PLAYER_CAN_DROP_ADENA = false;
	public static int PLAYER_RATE_DROP_ADENA = 0;
	public static int RATE_DROP_ANCIENT_ADENA = 0;
	public static boolean ALT_BLACKSMITH_USE_RECIPES = true;
	public static int PET_TICKET_ID = 13273;
	public static int SPECIAL_PET_TICKET_ID = 0;
	public static int AUCTION_BID_ITEM_ID = 57;
	public static int MIN_BOSS_MANA_TO_CAST = 100;
	public static int GOLD_BAR_PRICE = 250000000;
	public static int GOLD_BAR_ID = 3470;



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

	// =========================================================================
	// ONDA C20 / ADD-ONS: BARAKIEL NOBLESSE, NOBLESSE ITEM & PVP/PK REWARDS (config/game/custom/add-on.properties)
	// =========================================================================
	public static boolean KILL_BARAKIEL_SET_NOBLESS = false;
	public static int NOBLESSE_ITEM_ID = 9229;
	public static boolean LEAVE_BUFFS_ON_DIE = true;
	public static boolean ALLOW_PVP_REWARD_SYSTEM = false;
	public static String PVP_REWARD_ITEM = "57,100;6392,100;";
	public static boolean ALLOW_PK_REWARD_SYSTEM = false;
	public static String PK_REWARD_ITEM = "57,100;6393,100;";
	public static String ANNOUNCE_PK_MSG = "Player $killer has slaughtered $target .";
	public static String ANNOUNCE_PVP_MSG = "Player $killer has defeated $target .";
	public static boolean PVP_CONGRATULATIONS_MSG = true;

	// =========================================================================
	// ONDA 9: NPCS, RAID BOSSES & GRAND BOSSES (bosses.properties, npc.properties, altgame.properties)
	// =========================================================================
	public static boolean ANNOUNCE_RAID_SPAWN = false;
	public static int ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = -1;
	public static int MAX_DRIFT_RANGE = 120;
	public static boolean DISABLE_RAID_BOSS_FOSSILIZATION = false;
	public static int MAX_LEVEL_RAID_BOSS_CURSE = 87;

	// =========================================================================
	// ONDA 10: OLIMPIADAS & CICLO DOS HEROIS (olympiad.properties)
	// =========================================================================
	public static boolean OLYMPIAD_ENABLED = true;
	public static int ALT_OLY_START_TIME = 18;
	public static int ALT_OLY_MIN = 0;
	public static int ALT_OLY_CPERIOD = 6;
	public static int ALT_OLY_WPERIOD = 7;
	public static int ALT_OLY_VPERIOD = 24;
	public static long ALT_OLY_BATTLE = 360000L;
	public static int ALT_OLY_CLASSED_PARTICIPANTS = 5;
	public static int ALT_OLY_CLASSED_REW_ITEM_COUNT = 50;
	public static int ALT_OLY_NON_CLASSED_PARTICIPANTS = 9;
	public static int ALT_OLY_NON_CLASSED_REW_ITEM_COUNT = 30;
	public static boolean ALT_OLY_HEAL_ON_TELEPORT = true;
	public static boolean ALT_OLY_HEAL_ON_FIGHT_START = true;
	public static boolean ALT_OLY_RESET_SKILL_TIME = true;
	public static boolean ALT_OLY_SAME_IP = true;
	public static boolean ALT_OLY_REMOVE_CUBICS = true;
	public static int ALT_OLY_ENCHANT_LIMIT = -1;
	public static int ALT_OLY_REWARD_ITEM = 6651;
	public static int ALT_OLY_HERO_POINTS = 300;
	public static int ALT_OLY_MIN_POINT_FOR_EXCHANGE = 50;
	public static int ALT_OLY_GP_PER_POINT = 1000;
	public static int ALT_OLY_START_POINTS_COUNT = 18;
	public static int ALT_OLY_WEEKLY_POINTS_COUNT = 3;
	public static int ALT_OLY_MIN_MATCHES = 5;
	public static boolean OLYMPIAD_REMOVE_POINTS_ON_TIE = true;

	// =========================================================================
	// ONDA 11: EVENTOS AUTOMATIZADOS (events_start.properties, tvtevent.properties, ctfevent.properties, dmevent.properties)
	// =========================================================================
	public static boolean TVT_AUTO_START = false;
	public static boolean CTF_AUTO_START = false;
	public static boolean DM_AUTO_START = false;
	public static int TVT_DELAY_ON_BOOT = 10;
	public static int CTF_DELAY_ON_BOOT = 10;
	public static int DM_DELAY_ON_BOOT = 10;
	public static int TVT_NEXT_DELAY = 60;
	public static int CTF_NEXT_DELAY = 60;
	public static int DM_NEXT_DELAY = 60;
	public static boolean TVT_ENABLED = true;
	public static boolean CTF_ENABLED = true;
	public static boolean DM_ENABLED = true;
	public static int TVT_MIN_LEVEL = 1;
	public static int TVT_MAX_LEVEL = 85;
	public static int TVT_MIN_PLAYERS = 8;
	public static int TVT_MAX_PLAYERS = 60;
	public static int TVT_REWARD_ID = 57;
	public static int TVT_REWARD_AMOUNT = 100000;
	public static int CTF_MIN_LEVEL = 1;
	public static int CTF_MAX_LEVEL = 85;
	public static int CTF_MIN_PLAYERS = 8;
	public static int CTF_MAX_PLAYERS = 60;
	public static int CTF_REWARD_ID = 57;
	public static int CTF_REWARD_AMOUNT = 100000;
	public static int DM_MIN_LEVEL = 1;
	public static int DM_MAX_LEVEL = 80;
	public static int DM_MIN_PLAYERS = 8;
	public static int DM_MAX_PLAYERS = 60;
	public static int DM_REWARD_ID = 57;
	public static int DM_REWARD_AMOUNT = 50000;

	// =========================================================================
	// ONDA 12: CERCOS A CASTELOS (siege.properties)
	// =========================================================================
	public static int SIEGE_LENGTH = 120;
	public static int COUNTDOWN_LENGTH = 10;
	public static int MAX_FLAGS = 1;
	public static int SIEGE_CLAN_MIN_LEVEL = 5;
	public static int SIEGE_CLAN_MIN_MEMBERS_COUNT = 1;
	public static int ATTACKER_MAX_CLANS = 500;
	public static int DEFENDER_MAX_CLANS = 500;
	public static int ATTACKER_RESPAWN = 0;
	public static int BLOOD_ALLIANCE_REWARD = 1;
	public static boolean SPAWN_SIEGE_GUARD = true;
	public static int MAX_GUARD_COUNT = 400;
	public static boolean ONLY_REGISTERED = false;
	public static boolean ALT_FLYING_WYVERN_IN_SIEGE = false;
	public static boolean ALLOW_GATE_CONTROL = false;
	public static boolean DISABLE_CHANGE_SIEGE_TIME = false;

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
		ANNOUNCE_CLASS_CHANGE = ConfigLoader.getBoolean("AnnounceClassChange", false);
		ANNOUNCE_CLASS_CHANGE_AROUND = ConfigLoader.getBoolean("AnnounceClassChangeAround", false);
		SHOW_CLASS_CHANGE_MESSAGE = ConfigLoader.getBoolean("ShowClassChangeMessage", true);
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
		ENABLE_GEODATA = ConfigLoader.getBoolean("EnableGeoData", false);

		// Chat & Social
		GLOBAL_CHAT = ConfigLoader.getProperty("GlobalChat", "REGION");
		TRADE_CHAT = ConfigLoader.getProperty("TradeChat", "REGION");
		USE_CHAT_FILTER = ConfigLoader.getBoolean("UseChatFilter", ConfigLoader.getBoolean("UseSayFilter", false));
		CHAT_FILTER_CHARS = ConfigLoader.getProperty("ChatFilterChars", "...");
		ALLOW_MULTILINE_CHAT = ConfigLoader.getBoolean("AllowMultiLineChat", false);
		CHAT_LENGTH = ConfigLoader.getInt("ChatLength", 120);
		CHAT_FILTER_KARMA = ConfigLoader.getInt("ChatFilterKarma", 0);
		LOG_CHAT_ON_FILE = ConfigLoader.getBoolean("LogChatOnFile", true);
		SHOUT_CHAT_REUSE_DELAY = ConfigLoader.getInt("ShoutChatReuseDelay", 1);
		TRADE_CHAT_REUSE_DELAY = ConfigLoader.getInt("TradeChatReuseDelay", 1);
		HERO_CHAT_REUSE_DELAY = ConfigLoader.getInt("HeroChatReuseDelay", 10);
		REGION_CHAT_ALSO_BLOCKED = ConfigLoader.getBoolean("RegionChatAlsoBlocked", false);
		SHOUT_CHAT_LEVEL = ConfigLoader.getInt("ShoutChatLevel", 1);
		TRADE_CHAT_LEVEL = ConfigLoader.getInt("TradeChatLevel", 1);

		// Petitions
		PETITIONING_ALLOWED = ConfigLoader.getBoolean("PetitioningAllowed", true);
		MAX_PETITIONS_PER_PLAYER = ConfigLoader.getInt("MaxPetitionsPerPlayer", 5);
		MAX_PETITIONS_PENDING = ConfigLoader.getInt("MaxPetitionsPending", 25);
		SEND_PAGE_ON_PETITION = ConfigLoader.getBoolean("SendPageOnPetition", false);
		PETITIONING_NEED_GM_ONLINE = ConfigLoader.getBoolean("PetitioningNeedGmOnline", true);
		MAIL_STORE_DELETED_LETTERS = ConfigLoader.getBoolean("MailStoreDeletedLetters", false);

		// Combat, Zones & Geodata
		ZONE_TOWN = ConfigLoader.getInt("ZoneTown", 0);
		USE_BOW_DISTANCE_PENALTY = ConfigLoader.getBoolean("UseBowDistancePenalty", false);
		MAX_BOW_DISTANCE_PENALTY = ConfigLoader.getFloat("MaxBowDistancePenalty", 0.6f);
		FALL_DOWN_ON_DEATH = ConfigLoader.getBoolean("FallDownOnDeath", true);
		COORD_SYNCHRONIZE = ConfigLoader.getInt("CoordSynchronize", -1);
		AUTO_DELETE_INVALID_QUEST_DATA = ConfigLoader.getBoolean("AutoDeleteInvalidQuestData", true);
		CHARACTER_DATA_STORE_INTERVAL = ConfigLoader.getInt("CharacterDataStoreInterval", 15);
		LAZY_ITEMS_UPDATE = ConfigLoader.getBoolean("LazyItemsUpdate", false);
		UPDATE_ITEMS_ON_CHAR_STORE = ConfigLoader.getBoolean("UpdateItemsOnCharStore", false);
		RESTORE_PLAYER_INSTANCE = ConfigLoader.getBoolean("RestorePlayerInstance", false);
		ALLOW_SUMMON_TO_INSTANCE = ConfigLoader.getBoolean("AllowSummonToInstance", false);
		ALLOW_GUARDS = ConfigLoader.getBoolean("AllowGuards", true);
		ALLOW_NPC_WALKERS = ConfigLoader.getBoolean("AllowNpcWalkers", true);
		ALLOW_WEAR = ConfigLoader.getBoolean("AllowWear", true);
		ENABLE_PATH_FINDING = ConfigLoader.getBoolean("EnablePathFinding", true);
		PATH_FINDING_MODE = ConfigLoader.getProperty("PathFindingMode", "Pathnode");
		GEO_CORRECT_Z = ConfigLoader.getProperty("GeoCorrectZ", "All");
		MAX_PATH_LENGTH = ConfigLoader.getInt("MaxPathLength", 800);
		Z_AXIS_DENSITY = ConfigLoader.getInt("ZAxisDensity", 12);
		RESET_TO_BASE_CLASS_IF_FAIL = ConfigLoader.getBoolean("ResetToBaseClassIfFail", true);
		ENABLE_RESTART = ConfigLoader.getBoolean("EnableRestart", false);
		RESTART_TIME = ConfigLoader.getProperty("RestartTime", "06:20:00");
		RESTART_WARN_TIME = ConfigLoader.getInt("RestartWarnTime", 10);

		// Party
		PARTY_XP_CUTOFF_METHOD = ConfigLoader.getProperty("PartyXpCutoffMethod", "auto");
		PARTY_XP_CUTOFF_PERCENT = ConfigLoader.getDouble("PartyXpCutoffPercent", 3.0);
		PARTY_XP_CUTOFF_LEVEL = ConfigLoader.getInt("PartyXpCutoffLevel", 30);
		ALT_PARTY_RANGE = ConfigLoader.getDouble("AltPartyRange", 1600.0);
		ALT_PARTY_RANGE2 = ConfigLoader.getDouble("AltPartyRange2", 1400.0);
		PARTY_LEVEL_LIMIT = ConfigLoader.getBoolean("PartLevelLimit", true);
		PARTY_MAX_LEVEL_DIFFERENCE = ConfigLoader.getInt("PartyMaxLevelDifference", 10);

		// Clan & Alliance
		LVL_FOR_USE_AUCTION = ConfigLoader.getInt("LvlForUseAuction", 2);
		DAYS_BEFORE_JOIN_A_CLAN = ConfigLoader.getInt("DaysBeforeJoinAClan", 1);
		DAYS_BEFORE_CREATE_A_CLAN = ConfigLoader.getInt("DaysBeforeCreateAClan", 1);
		ALT_CLAN_MEMBERS_FOR_WAR = ConfigLoader.getInt("AltClanMembersForWar", 15);
		REPUTATION_SCORE_PER_KILL = ConfigLoader.getInt("ReputationScorePerKill", 1);
		DAYS_TO_PASS_TO_DISSOLVE_A_CLAN = ConfigLoader.getInt("DaysToPassToDissolveAClan", 7);
		DAYS_BEFORE_JOIN_ALLY_WHEN_LEAVED = ConfigLoader.getInt("DaysBeforeJoinAllyWhenLeaved", 1);
		DAYS_BEFORE_JOIN_ALLY_WHEN_DISMISSED = ConfigLoader.getInt("DaysBeforeJoinAllyWhenDismissed", 1);
		DAYS_BEFORE_ACCEPT_NEW_CLAN_WHEN_DISMISSED = ConfigLoader.getInt("DaysBeforeAcceptNewClanWhenDismissed", 1);
		DAYS_BEFORE_CREATE_NEW_ALLY_WHEN_DISSOLVED = ConfigLoader.getInt("DaysBeforeCreateNewAllyWhenDissolved", 1);
		ALT_MAX_NUM_OF_CLANS_IN_ALLY = ConfigLoader.getInt("AltMaxNumOfClansInAlly", 3);
		ALT_MEMBERS_CAN_WITHDRAW_FROM_CLAN_WH = ConfigLoader.getBoolean("AltMembersCanWithdrawFromClanWH", false);
		ALT_CLAN_WAR_PENALTY_WHEN_ENDED = ConfigLoader.getInt("AltClanWarPenaltyWhenEnded", 5);
		MAX_MEMBERS_CLAN_0 = ConfigLoader.getInt("MaxMembersClan0", 10);
		MAX_MEMBERS_CLAN_1 = ConfigLoader.getInt("MaxMembersClan1", 15);
		MAX_MEMBERS_CLAN_2 = ConfigLoader.getInt("MaxMembersClan2", 20);
		MAX_MEMBERS_CLAN_3 = ConfigLoader.getInt("MaxMembersClan3", 30);
		MAX_MEMBERS_CLAN_4 = ConfigLoader.getInt("MaxMembersClan4", 40);
		MAX_MEMBERS_CLAN_5 = ConfigLoader.getInt("MaxMembersClan5", 40);
		MAX_MEMBERS_CLAN_6 = ConfigLoader.getInt("MaxMembersClan6", 40);
		MAX_MEMBERS_CLAN_7 = ConfigLoader.getInt("MaxMembersClan7", 40);
		MAX_MEMBERS_CLAN_8 = ConfigLoader.getInt("MaxMembersClan8", 40);
		MAX_MEMBERS_ROYALS = ConfigLoader.getInt("MaxMembersRoyals", 20);
		MAX_MEMBERS_KNIGHTS = ConfigLoader.getInt("MaxMembersKnights", 10);
		REMOVE_CASTLE_CIRCULAR = ConfigLoader.getBoolean("RemoveCastleCirclets", true);
		MIN_LEVEL_TO_CREATE_PLEDGE = ConfigLoader.getInt("MinLevelToCreatePledge", 10);
		ALT_CLAN_LEADER_DATE_CHANGE = ConfigLoader.getInt("AltClanLeaderDateChange", 3);
		ALT_CLAN_LEADER_HOUR_CHANGE = ConfigLoader.getProperty("AltClanLeaderHourChange", "00:00:00");
		ALT_CLAN_LEADER_INSTANT_ACTIVATION = ConfigLoader.getBoolean("AltClanLeaderInstantActivation", false);

		// Drops Fine & Economy
		MULTIPLE_ITEM_DROP = ConfigLoader.getBoolean("MultipleItemDrop", true);
		PRECISE_DROP_CALCULATION = ConfigLoader.getBoolean("PreciseDropCalculation", false);
		USE_DEEP_BLUE_DROP_RULES = ConfigLoader.getBoolean("UseDeepBlueDropRules", true);
		PICKUP_FULL_INVENTORY = ConfigLoader.getProperty("PickupFullInventory", "drop");
		L2OFF_ADENA_PROTECTION = ConfigLoader.getBoolean("L2OFFAdenaProtection", false);
		SET_MAX_ETC_ITEM_SELL = ConfigLoader.getBoolean("SetMaxEtcItemSell", false);
		SET_MAX_ETC_ITEM_SELL_QNT = ConfigLoader.getInt("SetMaxEtcItemSellQnt", 10000);

		// Community Board
		COMMUNITY_TYPE = ConfigLoader.getProperty("CommunityType", "Full");
		BBS_DEFAULT = ConfigLoader.getProperty("BBSDefault", "_bbshome");
		SHOW_LEVEL_ON_COMMUNITY_BOARD = ConfigLoader.getBoolean("ShowLevelOnCommunityBoard", false);
		SHOW_STATUS_ON_COMMUNITY_BOARD = ConfigLoader.getBoolean("ShowStatusOnCommunityBoard", true);
		NAME_PAGE_SIZE_ON_COMMUNITY_BOARD = ConfigLoader.getInt("NamePageSizeOnCommunityBoard", 50);
		NAME_PER_ROW_ON_COMMUNITY_BOARD = ConfigLoader.getInt("NamePerRowOnCommunityBoard", 5);
		CUSTOM_COMMUNITY_BOARD = ConfigLoader.getBoolean("CustomCommunityBoard", true);
		COMMUNITY_BUFFER_EXCLUDE_ON = ConfigLoader.getProperty("CommunityBufferExcludeOn", "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE");
		GATEKEEPER_EXCLUDE_ON = ConfigLoader.getProperty("GatekeeperExcludeOn", "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE");
		RESTRICT_CB_WHEN = ConfigLoader.getProperty("RestrictCBWhen", "JAIL");
		ONLINE_COMMUNITY_BOARD = ConfigLoader.getBoolean("OnlineCommunityBoard", true);
		COLOR_COMMUNITY_BOARD = ConfigLoader.getBoolean("ColorCommunityBoard", false);
		SHOW_CURSED_WEAPON_OWNER = ConfigLoader.getBoolean("ShowCursedWeaponOwner", false);
		SHOW_KARMA_PLAYERS = ConfigLoader.getBoolean("ShowKarmaPlayers", false);
		SHOW_JAILED_PLAYERS = ConfigLoader.getBoolean("ShowJailedPlayers", false);
		SHOW_LEGEND = ConfigLoader.getBoolean("ShowLegend", false);
		SHOW_CLAN_LEADER = ConfigLoader.getBoolean("ShowClanLeader", false);
		SHOW_CLAN_LEADER_AT_CLAN_LEVEL = ConfigLoader.getInt("ShowClanLeaderAtClanLevel", 3);
		DISABLED_PAGES = ConfigLoader.getProperty("DisabledPages", "");

		// Rates Extended
		RATE_QUESTS_REWARD_EXP_SP = ConfigLoader.getFloat("RateQuestsRewardExpSp", 1.0f);
		RATE_QUESTS_REWARD_ADENA = ConfigLoader.getFloat("RateQuestsRewardAdena", 1.0f);
		RATE_QUESTS_REWARD_ITEMS = ConfigLoader.getFloat("RateQuestsRewardItems", 1.0f);
		RATE_CRAFT_COST = ConfigLoader.getFloat("RateCraftCost", 1.0f);
		RATE_CONSUMABLE_COST = ConfigLoader.getFloat("RateConsumableCost", 1.0f);
		RATE_SIEGE_GUARDS_PRICE = ConfigLoader.getFloat("RateSiegeGuardsPrice", 1.0f);
		RATE_RUN_SPEED = ConfigLoader.getFloat("RateRunSpeed", 1.0f);
		SIN_EATER_XP_RATE = ConfigLoader.getFloat("SinEaterXpRate", 1.0f);
		PET_XP_RATE = ConfigLoader.getFloat("PetXpRate", 1.0f);
		PET_FOOD_RATE = ConfigLoader.getFloat("PetFoodRate", 1.0f);
		KARMA_DROP_LIMIT = ConfigLoader.getInt("KarmaDropLimit", 10);
		KARMA_RATE_DROP = ConfigLoader.getInt("KarmaRateDrop", 70);
		KARMA_RATE_DROP_ITEM = ConfigLoader.getInt("KarmaRateDropItem", 50);
		KARMA_RATE_DROP_EQUIP = ConfigLoader.getInt("KarmaRateDropEquip", 40);
		KARMA_RATE_DROP_EQUIP_WEAPON = ConfigLoader.getInt("KarmaRateDropEquipWeapon", 10);
		RATE_KARMA_EXP_LOST = ConfigLoader.getFloat("RateKarmaExpLost", 1.0f);
		RATE_COMMON_HERBS = ConfigLoader.getFloat("RateCommonHerbs", 15.0f);
		RATE_HP_MP_HERBS = ConfigLoader.getFloat("RateHpMpHerbs", 10.0f);
		RATE_GREATER_HERBS = ConfigLoader.getFloat("RateGreaterHerbs", 4.0f);
		RATE_SUPERIOR_HERBS = ConfigLoader.getFloat("RateSuperiorHerbs", 0.8f);
		RATE_SPECIAL_HERBS = ConfigLoader.getFloat("RateSpecialHerbs", 0.2f);

		// Boss Rates
		ADENA_BOSS = ConfigLoader.getFloat("AdenaBoss", 1.0f);
		ADENA_RAID = ConfigLoader.getFloat("AdenaRaid", 1.0f);
		ADENA_MINON = ConfigLoader.getFloat("AdenaMinon", 1.0f);
		JEWEL_BOSS = ConfigLoader.getFloat("JewelBoss", 1.0f);
		ITEMS_BOSS = ConfigLoader.getFloat("ItemsBoss", 1.0f);
		ITEMS_RAID = ConfigLoader.getFloat("ItemsRaid", 1.0f);
		ITEMS_MINON = ConfigLoader.getFloat("ItemsMinon", 1.0f);
		SPOIL_BOSS = ConfigLoader.getFloat("SpoilBoss", 1.0f);
		SPOIL_RAID = ConfigLoader.getFloat("SpoilRaid", 1.0f);
		SPOIL_MINON = ConfigLoader.getFloat("SpoilMinon", 1.0f);

		// Inventory Slots & Player Karma
		MAX_INVENTORY_SLOTS_FOR_OTHER = ConfigLoader.getInt("MaxInventorySlotsForOther", 80);
		MAX_INVENTORY_SLOTS_FOR_DWARF = ConfigLoader.getInt("MaxInventorySlotsForDwarf", 100);
		MAX_INVENTORY_SLOTS_FOR_GM = ConfigLoader.getInt("MaxInventorySlotsForGameMaster", 250);
		MAXIMUM_SLOTS_FOR_PET = ConfigLoader.getInt("MaximumSlotsForPet", 12);
		DESTROY_PLAYER_INVENTORY_DROP = ConfigLoader.getBoolean("DestroyPlayerInventoryDrop", false);
		ALLOW_DISCARD_ITEM = ConfigLoader.getBoolean("AllowDiscardItem", true);

		MIN_KARMA = ConfigLoader.getInt("MinKarma", 240);
		MAX_KARMA = ConfigLoader.getInt("MaxKarma", 10000);
		KARMA_RATE = ConfigLoader.getFloat("KarmaRate", 1.0f);
		XP_DIVIDER = ConfigLoader.getInt("XpDivider", 260);
		CAN_GM_DROP_EQUIPMENT = ConfigLoader.getBoolean("CanGMDropEquipment", false);
		MINIMUM_PK_REQUIRED_TO_DROP = ConfigLoader.getInt("MinimumPKRequiredToDrop", 5);
		BASE_KARMA_LOST = ConfigLoader.getInt("BaseKarmaLost", 0);
		AWARD_PK_KILL_PVP_POINT = ConfigLoader.getBoolean("AwardPKKillPVPPoint", true);

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

		// Add-on & PvP Rewards
		KILL_BARAKIEL_SET_NOBLESS = ConfigLoader.getBoolean("KillBarakielSetNobless", false);
		NOBLESSE_ITEM_ID = ConfigLoader.getInt("NoblesseItem", 9229);
		LEAVE_BUFFS_ON_DIE = ConfigLoader.getBoolean("LeaveBuffsOnDie", true);
		ALLOW_PVP_REWARD_SYSTEM = ConfigLoader.getBoolean("AllowPvpRewardSystem", false);
		PVP_REWARD_ITEM = ConfigLoader.getProperty("PvpRewardItem", "57,100;6392,100;");
		ALLOW_PK_REWARD_SYSTEM = ConfigLoader.getBoolean("AllowPkRewardSystem", false);
		PK_REWARD_ITEM = ConfigLoader.getProperty("PkRewardItem", "57,100;6393,100;");
		ANNOUNCE_PK_PVP = ConfigLoader.getBoolean("AnnouncePkPvP", false);
		ANNOUNCE_PK_PVP_NORMAL_MESSAGE = ConfigLoader.getBoolean("AnnouncePkPvPNormalMessage", true);
		ANNOUNCE_PK_MSG = ConfigLoader.getProperty("AnnouncePkMsg", "Player $killer has slaughtered $target .");
		ANNOUNCE_PVP_MSG = ConfigLoader.getProperty("AnnouncePvpMsg", "Player $killer has defeated $target .");
		PVP_CONGRATULATIONS_MSG = ConfigLoader.getBoolean("PvPCongratulationsMsg", true);

		// Custom Private Store & PvP Mods
		SELL_BY_ITEM = ConfigLoader.getBoolean("SellByItem", false);
		SELL_ITEM = ConfigLoader.getInt("SellItem", 57);
		COIN_TEXT = ConfigLoader.getProperty("CoinText", "Adena");
		ALLOW_QUAKE_SYSTEM = ConfigLoader.getBoolean("AllowQuakeSystem", false);
		QUAKE_RESET_ON_DIE = ConfigLoader.getBoolean("QuakeResetOnDie", true);
		WAR_LEGEND_AURA = ConfigLoader.getBoolean("WarLegendAura", false);
		KILLS_TO_GET_WAR_LEGEND_AURA = ConfigLoader.getInt("KillsToGetWarLegendAura", 30);
		ALLOW_HERO_SKILL_ON_SUB = ConfigLoader.getBoolean("AllowHeroSkillOnSub", false);
		ALLOW_CUSTOM_CANCEL_TASK = ConfigLoader.getBoolean("AllowCustomCancelTask", false);
		CUSTOM_CANCEL_SECONDS = ConfigLoader.getInt("CustomCancelSeconds", 5);

		// Enchant System
		ALLOW_CRYSTAL_SCROLL = ConfigLoader.getBoolean("AllowCrystalScroll", false);
		NORMAL_WEAPON_ENCHANT_LEVEL = ConfigLoader.getProperty("NormalWeaponEnchantLevel", "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;");
		NORMAL_ARMOR_ENCHANT_LEVEL = ConfigLoader.getProperty("NormalArmorEnchantLevel", "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;");
		NORMAL_JEWELRY_ENCHANT_LEVEL = ConfigLoader.getProperty("NormalJewelryEnchantLevel", "1,100;2,100;3,100;4,95;5,90;6,85;7,80;8,75;9,70;10,65;11,60;12,55;13,50;14,45;15,40;16,35;");
		BLESS_WEAPON_ENCHANT_LEVEL = ConfigLoader.getProperty("BlessWeaponEnchantLevel", "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;");
		BLESS_ARMOR_ENCHANT_LEVEL = ConfigLoader.getProperty("BlessArmorEnchantLevel", "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;");
		BLESS_JEWELRY_ENCHANT_LEVEL = ConfigLoader.getProperty("BlessJewelryEnchantLevel", "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;");
		CRYSTAL_WEAPON_ENCHANT_LEVEL = ConfigLoader.getProperty("CrystalWeaponEnchantLevel", "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;");
		CRYSTAL_ARMOR_ENCHANT_LEVEL = ConfigLoader.getProperty("CrystalArmorEnchantLevel", "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;");
		CRYSTAL_JEWELRY_ENCHANT_LEVEL = ConfigLoader.getProperty("CrystalJewelryEnchantLevel", "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;");
		ENCHANT_MAX_WEAPON_NORMAL = ConfigLoader.getInt("EnchantMaxWeaponNormal", 0);
		ENCHANT_MAX_ARMOR_NORMAL = ConfigLoader.getInt("EnchantMaxArmorNormal", 0);
		ENCHANT_MAX_JEWELRY_NORMAL = ConfigLoader.getInt("EnchantMaxJewelryNormal", 0);
		ENCHANT_MAX_WEAPON_BLESSED = ConfigLoader.getInt("EnchantMaxWeaponBlessed", 0);
		ENCHANT_MAX_ARMOR_BLESSED = ConfigLoader.getInt("EnchantMaxArmorBlessed", 0);
		ENCHANT_MAX_JEWELRY_BLESSED = ConfigLoader.getInt("EnchantMaxJewelryBlessed", 0);
		ENCHANT_MAX_WEAPON_CRYSTAL = ConfigLoader.getInt("EnchantMaxWeaponCrystal", 0);
		ENCHANT_MAX_ARMOR_CRYSTAL = ConfigLoader.getInt("EnchantMaxArmorCrystal", 0);
		ENCHANT_MAX_JEWELRY_CRYSTAL = ConfigLoader.getInt("EnchantMaxJewelryCrystal", 0);
		ENCHANT_OVER_CHANT_CHECK = ConfigLoader.getInt("EnchantOverChantCheck", 0);
		CHECK_ENCHANT_LEVEL_EQUIP = ConfigLoader.getBoolean("CheckEnchantLevelEquip", true);
		ENCHANT_SAFE_MAX = ConfigLoader.getInt("EnchantSafeMax", 3);
		ENCHANT_SAFE_MAX_FULL = ConfigLoader.getInt("EnchantSafeMaxFull", 4);
		ALT_ENC_LVL_AFTER_FAIL = ConfigLoader.getBoolean("AltEncLvlAfterFail", false);
		ENCHANT_ROLL_BACK = ConfigLoader.getBoolean("EnchantRollBack", false);
		ENCHANT_ROLL_BACK_VALUE = ConfigLoader.getInt("EnchantRollBackValue", 0);
		ENCHANT_DWARF_SYSTEM = ConfigLoader.getBoolean("EnchantDwarfSystem", false);
		ENCHANT_DWARF_1_ENCHANT_LEVEL = ConfigLoader.getInt("EnchantDwarf1Enchantlevel", 8);
		ENCHANT_DWARF_2_ENCHANT_LEVEL = ConfigLoader.getInt("EnchantDwarf2Enchantlevel", 10);
		ENCHANT_DWARF_3_ENCHANT_LEVEL = ConfigLoader.getInt("EnchantDwarf3Enchantlevel", 12);
		ENCHANT_DWARF_1_CHANCE = ConfigLoader.getInt("EnchantDwarf1Chance", 15);
		ENCHANT_DWARF_2_CHANCE = ConfigLoader.getInt("EnchantDwarf2Chance", 15);
		ENCHANT_DWARF_3_CHANCE = ConfigLoader.getInt("EnchantDwarf3Chance", 15);

		// Equipment Restrictions
		ALLOW_LIGHT_USE_HEAVY = ConfigLoader.getBoolean("AllowLightUseHeavy", true);
		NOT_ALLOWED_USE_HEAVY = getIntList("NotAllowedUseHeavy", List.of(8, 9, 23, 24, 36, 37, 92, 93, 101, 102, 108, 109));
		ALLOW_HEAVY_USE_LIGHT = ConfigLoader.getBoolean("AllowHeavyUseLight", true);
		NOT_ALLOWED_USE_LIGHT = getIntList("NotAllowedUseLight", List.of(3, 4, 5, 6, 19, 20, 21, 32, 33, 34, 90, 91, 99, 100, 106, 107, 112));
		ALT_DISABLE_BOW = ConfigLoader.getBoolean("AltDisableBow", false);
		DISABLE_BOW_FOR_CLASSES = getIntList("DisableBowForClasses", List.of(88, 89));
		ALT_DISABLE_DAGGER = ConfigLoader.getBoolean("AltDisableDagger", false);
		DISABLE_DAGGER_FOR_CLASSES = getIntList("DisableDaggerForClasses", List.of(90, 91));
		ALT_DISABLE_SWORD = ConfigLoader.getBoolean("AltDisableSword", false);
		DISABLE_SWORD_FOR_CLASSES = getIntList("DisableSwordForClasses", List.of(92, 93));
		ALT_DISABLE_BLUNT = ConfigLoader.getBoolean("AltDisableBlunt", false);
		DISABLE_BLUNT_FOR_CLASSES = getIntList("DisableBluntForClasses", List.of(94, 95));
		ALT_DISABLE_DUAL = ConfigLoader.getBoolean("AltDisableDual", false);
		DISABLE_DUAL_FOR_CLASSES = getIntList("DisableDualForClasses", List.of(96, 97));
		ALT_DISABLE_POLLE = ConfigLoader.getBoolean("AltDisablePolle", false);
		DISABLE_POLLE_FOR_CLASSES = getIntList("DisablePolleForClasses", List.of(98, 99));
		ALT_DISABLE_BIG_SWORD = ConfigLoader.getBoolean("AltDisableBigSword", false);
		DISABLE_BIG_SWORD_FOR_CLASSES = getIntList("DisableBigSwordForClasses", List.of(100, 101));

		// Stats Caps, Heroes & Crafting (player.properties)
		MAX_P_ATK_SPEED = ConfigLoader.getInt("MaxPAtkSpeed", 9999);
		MAX_M_ATK_SPEED = ConfigLoader.getInt("MaxMAtkSpeed", 9999);
		MAX_RUN_SPEED = ConfigLoader.getInt("MaxRunSpeed", 9999);
		MAX_EVASION = ConfigLoader.getInt("MaxEvasion", 200);
		MINIMUM_HIT_TIME = ConfigLoader.getInt("MinimumHitTime", 330);
		ALT_P_CRITICAL_CAP = ConfigLoader.getInt("AltPCriticalCap", 500);
		ALT_M_CRITICAL_CAP = ConfigLoader.getInt("AltMCriticalCap", 200);
		STRICT_HERO_SYSTEM = ConfigLoader.getBoolean("StrictHeroSystem", true);
		HERO_WEAPONS_CAN_BE_ENCHANTED = ConfigLoader.getBoolean("HeroWeaponsCanBeEnchanted", true);
		LOG_IS_HERO_NO_CLAN = ConfigLoader.getBoolean("LogIsHeroNoClan", false);
		CRAFTING_ENABLED = ConfigLoader.getBoolean("CraftingEnabled", true);
		ALT_GAME_CREATION = ConfigLoader.getBoolean("AltGameCreation", false);
		ALT_GAME_CREATION_SPEED = ConfigLoader.getInt("AltGameCreationSpeed", 1);
		ALT_GAME_CREATION_RATE_XP = ConfigLoader.getInt("AltGameCreationRateXp", 1);
		ALT_GAME_CREATION_RATE_SP = ConfigLoader.getInt("AltGameCreationRateSp", 1);
		DWARF_RECIPE_LIMIT = ConfigLoader.getInt("DwarfRecipeLimit", 50);
		COMMON_RECIPE_LIMIT = ConfigLoader.getInt("CommonRecipeLimit", 50);
		SUBCLASS_WITH_ITEM_AND_NO_QUEST = ConfigLoader.getBoolean("SubclassWithItemAndNoQuest", false);
		SUBCLASS_WITH_CUSTOM_ITEM = ConfigLoader.getBoolean("SubclassWithCustomItem", false);
		SUBCLASS_WITH_CUSTOM_ITEM_ID = ConfigLoader.getInt("SubclassWithCustomItemID", 57);
		SUBCLASS_WITH_CUSTOM_ITEM_COUNT = ConfigLoader.getInt("SubclassWithCustomItemCount", 1000000);
		ALT_SUBCLASS_SKILLS = ConfigLoader.getBoolean("AltSubClassSkills", false);
		INCREASE_WEIGHT_LIMIT_BY_LEVEL = ConfigLoader.getBoolean("IncreaseWeightLimitByLevel", true);
		PLAYER_SPAWN_PROTECTION = ConfigLoader.getInt("PlayerSpawnProtection", 0);
		PLAYER_FAKE_DEATH_UP_PROTECTION = ConfigLoader.getInt("PlayerFakeDeathUpProtection", 0);
		DEATH_PENALTY_CHANCE = ConfigLoader.getInt("DeathPenaltyChance", 20);

		// Custom properties (custom.properties)
		WEAR_DELAY = ConfigLoader.getInt("WearDelay", 5);
		WEAR_PRICE = ConfigLoader.getInt("WearPrice", 10);
		PLAYER_CAN_DROP_ADENA = ConfigLoader.getBoolean("PlayerCanDropAdena", false);
		PLAYER_RATE_DROP_ADENA = ConfigLoader.getInt("PlayerRateDropAdena", 0);
		RATE_DROP_ANCIENT_ADENA = ConfigLoader.getInt("RateDropAncientAdena", 0);
		ALT_BLACKSMITH_USE_RECIPES = ConfigLoader.getBoolean("AltBlacksmithUseRecipes", true);
		PET_TICKET_ID = ConfigLoader.getInt("PetTicketID", 13273);
		SPECIAL_PET_TICKET_ID = ConfigLoader.getInt("SpecialPetTicketID", 0);
		AUCTION_BID_ITEM_ID = ConfigLoader.getInt("AuctionBidItemId", 57);
		MIN_BOSS_MANA_TO_CAST = ConfigLoader.getInt("MinBossManaToCast", 100);
		GOLD_BAR_PRICE = ConfigLoader.getInt("GoldBarPrice", 250000000);
		GOLD_BAR_ID = ConfigLoader.getInt("GoldBarId", 3470);

		// Onda 9
		ANNOUNCE_RAID_SPAWN = ConfigLoader.getBoolean("AnnounceRaidSpawn", false);
		ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = ConfigLoader.getInt("AltMobNoAttackWithLevelDifference", -1);
		MAX_DRIFT_RANGE = ConfigLoader.getInt("MaxDriftRange", 120);
		DISABLE_RAID_BOSS_FOSSILIZATION = ConfigLoader.getBoolean("DisableRaidBossFossilization", false);
		MAX_LEVEL_RAID_BOSS_CURSE = ConfigLoader.getInt("MaxLevelRaidBossCurse", 87);

		// Onda 10
		OLYMPIAD_ENABLED = ConfigLoader.getBoolean("OlympiadEnabled", true);
		ALT_OLY_START_TIME = ConfigLoader.getInt("AltOlyStartTime", 18);
		ALT_OLY_MIN = ConfigLoader.getInt("AltOlyMin", 0);
		ALT_OLY_CPERIOD = ConfigLoader.getInt("AltOlyCPeriod", 6);
		ALT_OLY_WPERIOD = ConfigLoader.getInt("AltOlyWperiod", 7);
		ALT_OLY_VPERIOD = ConfigLoader.getInt("AltOlyVperiod", 24);
		ALT_OLY_BATTLE = ConfigLoader.getLong("AltOlyBattle", 360000L);
		ALT_OLY_CLASSED_PARTICIPANTS = ConfigLoader.getInt("AltOlyClassedParticipants", 5);
		ALT_OLY_CLASSED_REW_ITEM_COUNT = ConfigLoader.getInt("AltOlyClassedRewItemCount", 50);
		ALT_OLY_NON_CLASSED_PARTICIPANTS = ConfigLoader.getInt("AltOlyNonClassedParticipants", 9);
		ALT_OLY_NON_CLASSED_REW_ITEM_COUNT = ConfigLoader.getInt("AltOlyNonClassedRewItemCount", 30);
		ALT_OLY_HEAL_ON_TELEPORT = ConfigLoader.getBoolean("AltOlyHealOnTeleport", true);
		ALT_OLY_HEAL_ON_FIGHT_START = ConfigLoader.getBoolean("AltOlyHealOnFightStart", true);
		ALT_OLY_RESET_SKILL_TIME = ConfigLoader.getBoolean("AltOlyResetSkillTime", true);
		ALT_OLY_SAME_IP = ConfigLoader.getBoolean("AltOlySameIp", true);
		ALT_OLY_REMOVE_CUBICS = ConfigLoader.getBoolean("AltOlyRemoveCubics", true);
		ALT_OLY_ENCHANT_LIMIT = ConfigLoader.getInt("AltOlyEnchantLimit", -1);
		ALT_OLY_REWARD_ITEM = ConfigLoader.getInt("AltOlyRewardItem", 6651);
		ALT_OLY_HERO_POINTS = ConfigLoader.getInt("AltOlyHeroPoints", 300);
		ALT_OLY_MIN_POINT_FOR_EXCHANGE = ConfigLoader.getInt("AltOlyMinPointForExchange", 50);
		ALT_OLY_GP_PER_POINT = ConfigLoader.getInt("AltOlyGPPerPoint", 1000);
		ALT_OLY_START_POINTS_COUNT = ConfigLoader.getInt("AltOlyStartPointsCount", 18);
		ALT_OLY_WEEKLY_POINTS_COUNT = ConfigLoader.getInt("AltOlyWeeklyPointsCount", 3);
		ALT_OLY_MIN_MATCHES = ConfigLoader.getInt("AltOlyMinMatches", 5);
		OLYMPIAD_REMOVE_POINTS_ON_TIE = ConfigLoader.getBoolean("OlympiadRemovePointsOnTie", true);

		// Onda 11
		TVT_AUTO_START = ConfigLoader.getBoolean("TvT.AutoStart", false);
		CTF_AUTO_START = ConfigLoader.getBoolean("CTF.AutoStart", false);
		DM_AUTO_START = ConfigLoader.getBoolean("DeathMatch.AutoStart", false);
		TVT_DELAY_ON_BOOT = ConfigLoader.getInt("TvT.DelayOnBoot", 10);
		CTF_DELAY_ON_BOOT = ConfigLoader.getInt("CTF.DelayOnBoot", 10);
		DM_DELAY_ON_BOOT = ConfigLoader.getInt("DeathMatch.DelayOnBoot", 10);
		TVT_NEXT_DELAY = ConfigLoader.getInt("TvT.NextDelay", 60);
		CTF_NEXT_DELAY = ConfigLoader.getInt("CTF.NextDelay", 60);
		DM_NEXT_DELAY = ConfigLoader.getInt("DeathMatch.NextDelay", 60);
		TVT_ENABLED = ConfigLoader.getBoolean("TvTEnabled", true);
		CTF_ENABLED = ConfigLoader.getBoolean("CTFEnabled", true);
		DM_ENABLED = ConfigLoader.getBoolean("DMEnabled", true);
		TVT_MIN_LEVEL = ConfigLoader.getInt("TvTMinLevel", 1);
		TVT_MAX_LEVEL = ConfigLoader.getInt("TvTMaxLevel", 85);
		TVT_MIN_PLAYERS = ConfigLoader.getInt("TvTMinPlayers", 8);
		TVT_MAX_PLAYERS = ConfigLoader.getInt("TvTMaxPlayers", 60);
		TVT_REWARD_ID = ConfigLoader.getInt("TvTRewardId", 57);
		TVT_REWARD_AMOUNT = ConfigLoader.getInt("TvTRewardAmount", 100000);
		CTF_MIN_LEVEL = ConfigLoader.getInt("CTFMinLevel", 1);
		CTF_MAX_LEVEL = ConfigLoader.getInt("CTFMaxLevel", 85);
		CTF_MIN_PLAYERS = ConfigLoader.getInt("CTFMinPlayers", 8);
		CTF_MAX_PLAYERS = ConfigLoader.getInt("CTFMaxPlayers", 60);
		CTF_REWARD_ID = ConfigLoader.getInt("CTFRewardId", 57);
		CTF_REWARD_AMOUNT = ConfigLoader.getInt("CTFRewardAmount", 100000);
		DM_MIN_LEVEL = ConfigLoader.getInt("MinLevel", 1);
		DM_MAX_LEVEL = ConfigLoader.getInt("MaxLevel", 80);
		DM_MIN_PLAYERS = ConfigLoader.getInt("MinPlayers", 8);
		DM_MAX_PLAYERS = ConfigLoader.getInt("MaxPlayers", 60);
		DM_REWARD_ID = ConfigLoader.getInt("RewardItem", 57);
		DM_REWARD_AMOUNT = ConfigLoader.getInt("RewardItemCount", 50000);

		// Onda 12
		SIEGE_LENGTH = ConfigLoader.getInt("SiegeLength", 120);
		COUNTDOWN_LENGTH = ConfigLoader.getInt("CountDownLength", 10);
		MAX_FLAGS = ConfigLoader.getInt("MaxFlags", 1);
		SIEGE_CLAN_MIN_LEVEL = ConfigLoader.getInt("SiegeClanMinLevel", 5);
		SIEGE_CLAN_MIN_MEMBERS_COUNT = ConfigLoader.getInt("SiegeClanMinMembersCount", 1);
		ATTACKER_MAX_CLANS = ConfigLoader.getInt("AttackerMaxClans", 500);
		DEFENDER_MAX_CLANS = ConfigLoader.getInt("DefenderMaxClans", 500);
		ATTACKER_RESPAWN = ConfigLoader.getInt("AttackerRespawn", 0);
		BLOOD_ALLIANCE_REWARD = ConfigLoader.getInt("BloodAllianceReward", 1);
		SPAWN_SIEGE_GUARD = ConfigLoader.getBoolean("SpawnSiegeGuard", true);
		MAX_GUARD_COUNT = ConfigLoader.getInt("MaxGuardCount", 400);
		ONLY_REGISTERED = ConfigLoader.getBoolean("OnlyRegistered", false);
		ALT_FLYING_WYVERN_IN_SIEGE = ConfigLoader.getBoolean("AltFlyingWyvernInSiege", false);
		ALLOW_GATE_CONTROL = ConfigLoader.getBoolean("AllowGateControl", false);
		DISABLE_CHANGE_SIEGE_TIME = ConfigLoader.getBoolean("DisableChangeSiegeTime", false);
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
