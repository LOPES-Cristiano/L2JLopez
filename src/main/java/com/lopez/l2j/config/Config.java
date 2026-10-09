package com.lopez.l2j.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
	public static int MAX_ONLINE_USERS = 1000;
	public static int GEODATA = 0;

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
	public static int ALLOW_OFFLINE_HOUR = 72;
	public static int OFFLINE_TRADE_PRICE_ID = 57;
	public static long OFFLINE_TRADE_PRICE_COUNT = 500000;
	public static int OFFLINE_TRADE_PRICE_ID_TIME = 1;
	public static int OFFLINE_CRAFT_PRICE_ID = 57;
	public static long OFFLINE_CRAFT_PRICE_COUNT = 500000;
	public static int OFFLINE_CRAFT_PRICE_ID_TIME = 1;
	public static boolean SHOW_NPC_CREST = false;
	public static boolean CUSTOM_START_TITLE = true;
	public static String CUSTOM_TITLE_TEXT = "L2JDream Project";
	public static String TITLE_COLOR = "00FF00";

	public static boolean PVP_COLOR_SYSTEM = false;
	public static String PVP_COLOR_MODE = "Title";
	public static int PVP_AMMOUNT_1 = 50;
	public static int PVP_AMMOUNT_2 = 100;
	public static int PVP_AMMOUNT_3 = 150;
	public static int PVP_AMMOUNT_4 = 250;
	public static int PVP_AMMOUNT_5 = 500;
	public static String COLOR_FOR_AMMOUNT_1 = "00FF00";
	public static String COLOR_FOR_AMMOUNT_2 = "00FF00";
	public static String COLOR_FOR_AMMOUNT_3 = "00FF00";
	public static String COLOR_FOR_AMMOUNT_4 = "00FF00";
	public static String COLOR_FOR_AMMOUNT_5 = "00FF00";
	public static String TITLE_FOR_AMMOUNT_1 = "00FF00";
	public static String TITLE_FOR_AMMOUNT_2 = "00FF00";
	public static String TITLE_FOR_AMMOUNT_3 = "00FF00";
	public static String TITLE_FOR_AMMOUNT_4 = "00FF00";
	public static String TITLE_FOR_AMMOUNT_5 = "00FF00";

	public static boolean LOAD_VOICED_HELP = true;
	public static boolean LOAD_VOICED_OFFLINE = true;
	public static boolean LOAD_VOICED_WEDDING = true;
	public static boolean LOAD_VOICED_BANK = true;
	public static boolean LOAD_VOICED_CONFIGURATOR = true;
	public static boolean LOAD_VOICED_CLASS_MASTER = true;
	public static boolean LOAD_VOICED_AIOX_COMMAND = true;
	public static boolean LOAD_VOICED_AUTOFARM_COMMAND = true;
	public static boolean LOAD_VOICED_ROULETTE_COMMAND = true;
	public static boolean LOAD_VOICED_RESET_COMMAND = true;

	public static boolean ENABLE_EVENT_MANAGER = false;
	public static int EVENT_MANAGER_NPC_ID = 50004;
	public static boolean ENABLE_AUTO_SPAWN = false;
	public static int EVENT_PARTICIPATION_FEE_ID = 57;
	public static long EVENT_PARTICIPATION_FEE_QNT = 10000;
	public static List<Integer> EVENT_BLOCK_CLASS_ID = List.of(15, 16, 97);
	public static String EVENT_BLOCK_CLASS_NAMES = "Cleric,Bishop,Cardinal";

	public static boolean ACTIVATE_SIEGE_REWARD_SYSTEM = false;
	public static boolean REWARD_ONLINE_ONLY = false;
	public static String REWARD_INFO = "57,2000;5575,2000000";
	public static String REWARD_CL_INFO = "57,2000;5575,200000";
	public static boolean ENABLE_SKILL_REWARD_BY_TIME = false;
	public static int REWARD_SKILL_TIME = 0;
	public static int REWARD_SKILL_ID = 0;
	public static int REWARD_SKILL_MAX_LVL = 0;
	public static boolean ENABLE_FIRST_LOGIN_REWARD = false;
	public static String FIRST_LOGIN_REWARD_TIME_ITEM = "57,1";
	public static boolean ENABLE_REWARD_BY_TIME = false;
	public static int REWARD_ITEM_BY_TIME_MIN = 5;
	public static String REWARD_ITEM_BY_TIME_ITEM = "57,1";
	public static boolean FIRST_LOGIN_BUFFS = false;
	public static List<Integer> FIRST_LOGIN_FIGHTER_BUFF_LIST = List.of();
	public static List<Integer> FIRST_LOGIN_MAGE_BUFF_LIST = List.of();
	public static boolean ENABLE_STARTUP_SYSTEM = false;
	public static boolean DISABLE_NEWBIE_TUTORIAL = false;
	public static boolean STARTUP_SYSTEM_CLASS = false;
	public static boolean STARTUP_SYSTEM_ARMOR = false;
	public static boolean STARTUP_SYSTEM_WEAPON = false;
	public static boolean STARTUP_SYSTEM_BUFF_FIGHT = true;
	public static List<Integer> FIGHTER_BUFF_LIST = List.of();
	public static boolean STARTUP_SYSTEM_BUFF_MAGE = true;
	public static List<Integer> MAGE_BUFF_LIST = List.of();
	public static String HTM_ROBE = "avadon_robe";
	public static String SET_ROBE = "2406,1;2415,1;5716,1;5732,1;895,1;895,1;864,1;864,1;926,1;";
	public static String HTM_LIGHT = "doom_light";
	public static String SET_LIGHT = "2392,1;2417,1;5723,1;5739,1;895,1;895,1;864,1;864,1;926,1;";
	public static String HTM_HEAVY = "doom_heavy";
	public static String SET_HEAVY = "2381,1;2417,1;5722,1;5738,1;895,1;895,1;864,1;864,1;926,1;";
	public static int SHIELD_ID = 110;
	public static int ARROW_ID = 1343;

	public static String BP_WEAPON_01 = "damascus_damage";
	public static int WP_01_ID = 4718;
	public static String BP_WEAPON_02 = "damascus_haste";
	public static int WP_02_ID = 4719;
	public static String BP_WEAPON_03 = "damascus_focus";
	public static int WP_03_ID = 4717;
	public static String BP_WEAPON_04 = "lance_anger";
	public static int WP_04_ID = 4858;
	public static String BP_WEAPON_05 = "lance_stun";
	public static int WP_05_ID = 4859;
	public static String BP_WEAPON_06 = "lance_blow";
	public static int WP_06_ID = 4860;
	public static String BP_WEAPON_07 = "wizard_acumen";
	public static int WP_07_ID = 8117;
	public static String BP_WEAPON_08 = "wizard_power";
	public static int WP_08_ID = 8118;
	public static String BP_WEAPON_09 = "wizard_conversion";
	public static int WP_09_ID = 8119;
	public static String BP_WEAPON_10 = "tear_acumen";
	public static int WP_10_ID = 8117;
	public static String BP_WEAPON_11 = "tear_power";
	public static int WP_11_ID = 8118;
	public static String BP_WEAPON_12 = "tear_conversion";
	public static int WP_12_ID = 8119;
	public static String BP_WEAPON_13 = "demons_damage";
	public static int WP_13_ID = 6359;
	public static String BP_WEAPON_14 = "demons_bleed";
	public static int WP_14_ID = 4780;
	public static String BP_WEAPON_15 = "demons_strike";
	public static int WP_15_ID = 4782;
	public static String BP_WEAPON_16 = "bow_guidance";
	public static int WP_16_ID = 4828;
	public static String BP_WEAPON_17 = "bow_shot";
	public static int WP_17_ID = 4830;
	public static String BP_WEAPON_18 = "bow_recovery";
	public static int WP_18_ID = 4829;
	public static String BP_WEAPON_19 = "great_damage";
	public static int WP_19_ID = 4724;
	public static String BP_WEAPON_20 = "great_focus";
	public static int WP_20_ID = 4725;
	public static String BP_WEAPON_21 = "great_health";
	public static int WP_21_ID = 4723;
	public static String BP_WEAPON_22 = "axe_health";
	public static int WP_22_ID = 4753;
	public static String BP_WEAPON_23 = "axe_focus";
	public static int WP_23_ID = 4754;
	public static String BP_WEAPON_24 = "axe_haste";
	public static int WP_24_ID = 4755;
	public static String BP_WEAPON_25 = "bellion_drain";
	public static int WP_25_ID = 4804;
	public static String BP_WEAPON_26 = "bellion_poison";
	public static int WP_26_ID = 4805;
	public static String BP_WEAPON_27 = "bellion_haste";
	public static int WP_27_ID = 4806;
	public static String BP_WEAPON_28 = "samurai";
	public static int WP_28_ID = 2626;
	public static String BP_WEAPON_29 = "0";
	public static int WP_29_ID = 0;
	public static String BP_WEAPON_30 = "0";
	public static int WP_30_ID = 0;
	public static String BP_WEAPON_31 = "0";
	public static int WP_31_ID = 0;

	public static boolean LOAD_CUSTOM_TELEPORTS = false;
	public static boolean LOAD_CUSTOM_ITEM_TABLES = false;
	public static boolean LOAD_CUSTOM_NPC_TABLE = false;
	public static boolean LOAD_CUSTOM_DROPLIST_TABLE = false;
	public static boolean LOAD_CUSTOM_ARMOR_SET_TABLE = false;
	public static boolean LOAD_CUSTOM_SPAWNLIST_TABLE = false;
	public static boolean GOLDBAR_ENABLED = true;

	public static boolean ENABLE_CAPTCHA = false;
	public static int CAPTCHA_KILLS_COUNTER = 60;
	public static int CAPTCHA_KILLS_COUNTER_RANDOMIZATION = 50;
	public static int CAPTCHA_VALIDATION_TIME = 60;
	public static int CAPTCHA_PUNISHMENT = 0;
	public static int CAPTCHA_PUNISHMENT_TIME = 60;

	public static boolean BANKING_SYSTEM_ENABLED = false;
	public static int BANKING_SYSTEM_GOLDBARS = 1;
	public static int BANKING_SYSTEM_ADENA = 500000000;


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

	// Blow & Lethal rates (config/game/main/player.properties)
	public static int BLOW_FRONT = 50;
	public static int BLOW_SIDE = 60;
	public static int BLOW_BEHIND = 70;
	public static double ALT_LETHAL_RATE_DAGGER = 1.0;
	public static double ALT_LETHAL_RATE_ARCHERY = 2.0;
	public static double ALT_LETHAL_RATE_OTHER = 2.0;

	// Consumables & Combat restrictions (config/game/main/player.properties)
	public static boolean CONSUME_SOUL_SHOT = true;
	public static boolean CONSUME_ARROWS = true;
	public static boolean BLOCK_PARTY_INVITE_ON_COMBAT = false;
	public static boolean BLOCK_CHANGE_WEAPON_WHILE_ATTACKING = false;

	// PK & Karma Rules (config/game/main/player.properties)
	public static boolean ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE = false;
	public static boolean ALT_KARMA_PLAYER_CAN_SHOP = false;
	public static boolean ALT_KARMA_PLAYER_CAN_TELEPORT = true;
	public static boolean ALT_KARMA_PLAYER_CAN_USE_GK = false;
	public static boolean ALT_KARMA_PLAYER_CAN_TRADE = true;
	public static boolean ALT_KARMA_PLAYER_CAN_USE_WAREHOUSE = true;
	public static int PVP_VS_NORMAL_TIME = 10000;
	public static int PVP_VS_PVP_TIME = 40000;
	public static boolean CURSED_WEAPON_NPC_INTERACT = false;
	public static List<Integer> LIST_OF_PET_ITEMS = List.of(2375, 3500, 3501, 3502, 4422, 4423, 4424, 4425, 6648, 6649, 6650, 9882);
	public static List<Integer> LIST_OF_NON_DROPPABLE_ITEMS = List.of(57, 1147, 425, 1146, 461, 10, 2368, 7, 6, 2370, 2369, 6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 8181, 5575, 7694, 9388, 9389, 9390);

	// Augmentation Rates (config/game/main/rates.properties)
	public static boolean AUGMENT_EXCLUDE_NOTDONE = false;
	public static int AUGMENTATION_NG_SKILL_CHANCE = 15;
	public static int AUGMENTATION_MID_SKILL_CHANCE = 30;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE = 45;
	public static int AUGMENTATION_TOP_SKILL_CHANCE = 60;
	public static int AUGMENTATION_BASE_STAT_CHANCE = 1;
	public static int AUGMENTATION_NG_GLOW_CHANCE = 0;
	public static int AUGMENTATION_MID_GLOW_CHANCE = 40;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE = 70;
	public static int AUGMENTATION_TOP_GLOW_CHANCE = 100;

	// Regeneration & Multipliers (config/game/main/rates.properties)
	public static int PLAYER_CP_REGEN_MULTIPLIER = 100;
	public static int PLAYER_HP_REGEN_MULTIPLIER = 100;
	public static int PLAYER_MP_REGEN_MULTIPLIER = 100;
	public static int NPC_HP_REGEN_MULTIPLIER = 100;
	public static int NPC_MP_REGEN_MULTIPLIER = 100;
	public static int PET_HP_REGEN_MULTIPLIER = 100;
	public static int PET_MP_REGEN_MULTIPLIER = 100;
	public static int RAID_HP_REGEN_MULTIPLIER = 100;
	public static int RAID_MP_REGEN_MULTIPLIER = 100;
	public static int RAID_P_DEFENCE_MULTIPLIER = 100;
	public static int RAID_M_DEFENCE_MULTIPLIER = 100;

	// Warehouse Slots & Freight (config/game/main/rates.properties)
	public static int MAX_WAREHOUSE_SLOTS_FOR_OTHER = 100;
	public static int MAX_WAREHOUSE_SLOTS_FOR_DWARF = 120;
	public static int MAX_WAREHOUSE_SLOTS_FOR_CLAN = 200;
	public static int MAX_WAREHOUSE_FREIGHT_SLOTS = 100;
	public static boolean ENABLE_WAREHOUSE_SORTING_CLAN = false;
	public static boolean ENABLE_WAREHOUSE_SORTING_PRIVATE = false;
	public static boolean ENABLE_WAREHOUSE_SORTING_FREIGHT = false;
	public static boolean ALT_GAME_FREIGHTS = false;
	public static int ALT_GAME_FREIGHT_PRICE = 1000;
	public static boolean ALLOW_WAREHOUSE = true;
	public static boolean ALLOW_FREIGHT = true;
	public static boolean WAREHOUSE_CACHE = false;
	public static int WAREHOUSE_CACHE_TIME = 15;

	// Private Store (config/game/main/rates.properties)
	public static int MAX_PVT_STORE_SELL_SLOTS_DWARF = 6;
	public static int MAX_PVT_STORE_SELL_SLOTS_OTHER = 4;
	public static int MAX_PVT_STORE_BUY_SLOTS_DWARF = 6;
	public static int MAX_PVT_STORE_BUY_SLOTS_OTHER = 4;
	public static boolean CHECK_ZONE_ON_PVT = false;

	// Manor (config/game/main/rates.properties)
	public static int ALT_MANOR_REFRESH_TIME = 20;
	public static int ALT_MANOR_REFRESH_MIN = 0;
	public static int ALT_MANOR_APPROVE_TIME = 6;
	public static int ALT_MANOR_APPROVE_MIN = 0;
	public static int ALT_MANOR_MAINTENANCE_PERIOD = 360000;
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS = false;
	public static int ALT_MANOR_SAVE_PERIOD_RATE = 2;

	// Bosses & Grand Bosses timers & parameters (config/game/main/bosses.properties)
	public static boolean QUEEN_ANT_ENABLED = true;
	public static boolean CORE_ENABLED = true;
	public static boolean ZAKEN_ENABLED = true;
	public static boolean SAILREN_ENABLED = true;
	public static boolean ANTHARAS_ENABLED = true;
	public static boolean VALAKAS_ENABLED = true;
	public static boolean BAIUM_ENABLED = true;
	public static boolean FRINTEZZA_ENABLED = true;
	public static double RATE_RAID_BOSS = 1.0;
	public static String ANTHARAS_RESPAWN_TIME_PATTERN = "";
	public static String VALAKAS_RESPAWN_TIME_PATTERN = "";
	public static String BAIUM_RESPAWN_TIME_PATTERN = "";
	public static String SAILREN_RESPAWN_TIME_PATTERN = "";
	public static String FRINTEZZA_RESPAWN_TIME_PATTERN = "";

	public static int ANTHARAS_ARRIVED_TIME = 15;
	public static int ANTHARAS_ACTIVE_TIME = 240;
	public static int ANTHARAS_MIN_RESPAWN = 11520;
	public static int ANTHARAS_MAX_RESPAWN = 15840;
	public static int ANTHARAS_WEAK_PLAYERS = 50;
	public static int ANTHARAS_MIDDLE_PLAYERS = 80;
	public static int ANTHARAS_MIN_SLEEP_TIME = 45;
	public static int ANTHARAS_MAX_SLEEP_TIME = 60;
	public static int ANTHARAS_INTERVAL_OF_BEHEMOTH = 4;

	public static int VALAKAS_ARRIVED_TIME = 1;
	public static int VALAKAS_ACTIVE_TIME = 240;
	public static int VALAKAS_MIN_RESPAWN = 11520;
	public static int VALAKAS_MAX_RESPAWN = 15840;
	public static int VALAKAS_LAIR_CAPACITY = 500;
	public static int VALAKAS_MIN_SLEEP_TIME = 45;
	public static int VALAKAS_MAX_SLEEP_TIME = 60;

	public static int BAIUM_ACTIVE_TIME = 240;
	public static int BAIUM_NO_ATTACK_TIME = 20;
	public static int BAIUM_MIN_RESPAWN = 7200;
	public static int BAIUM_MAX_RESPAWN = 10080;
	public static int BAIUM_UNSPAWN_CUBE = 300;

	public static int VAN_HALTER_ACTIVE_TIME = 240;
	public static int VAN_HALTER_TIME_OF_LOCK_UP_DOOR_OF_ALTAR = 3;
	public static int VAN_HALTER_INTERVAL_OF_DOOR_OF_ALTER = 360;
	public static int VAN_HALTER_APPEARANCE_TIME = 1;
	public static int VAN_HALTER_FIGHT_TIME = 240;
	public static int VAN_HALTER_MIN_RESPAWN = 720;
	public static int VAN_HALTER_MAX_RESPAWN = 2160;

	public static int FRINTEZZA_ACTIVE_TIME = 150;
	public static int FRINTEZZA_MIN_RESPAWN = 2400;
	public static int FRINTEZZA_MAX_RESPAWN = 3600;
	public static int FRINTEZZA_TOMB_PASS_TIME = 35;
	public static int FRINTEZZA_MIN_PARTY_IN_CC = 1;
	public static int FRINTEZZA_MAX_PARTY_IN_CC = 222;
	public static int FRINTEZZA_MIN_DISTANCE_FOR_ENTRANCE = 300;

	public static int SAILREN_ACTIVITY_TIME = 60;
	public static boolean SAILREN_ENABLE_SINGLE_PLAYER = true;
	public static int SAILREN_MIN_RESPAWN = 720;
	public static int SAILREN_MAX_RESPAWN = 2160;
	public static int SAILREN_INTERVAL_OF_MONSTERS = 3;

	public static int QUEEN_ANT_MIN_RESPAWN = 1140;
	public static int QUEEN_ANT_MAX_RESPAWN = 2160;
	public static int QUEEN_ANT_NUMBER_OF_GUARDS = 8;
	public static int QUEEN_ANT_NUMBER_OF_NURSES = 6;

	public static int CORE_MIN_RESPAWN = 2220;
	public static int CORE_MAX_RESPAWN = 3600;
	public static int CORE_NUMBER_OF_GUARDS = 4;

	public static boolean ZAKEN_DOOR_CLOSED_DEFAULT = true;
	public static String ZAKEN_DOOR_OPEN_HOUR = "0";
	public static int ZAKEN_DOOR_OPEN_TIME = 5;
	public static int ZAKEN_MAX_LEVEL_IN_ZONE = 80;
	public static int ZAKEN_MIN_RESPAWN = 2400;
	public static int ZAKEN_MAX_RESPAWN = 3600;

	public static int ORFEN_MIN_RESPAWN = 1680;
	public static int ORFEN_MAX_RESPAWN = 2880;

	public static int ALL_MINIONS_RESPAWN_INTERVAL = 5;
	public static boolean RETURN_TO_HOME_BOSSES_FROM_PVP_ZONES = false;
	public static boolean RETURN_TO_HOME_BOSSES_FROM_TOWN_ZONES = true;
	public static double RAID_BOSS_P_ATK_MODIFIER = 1.0;
	public static double RAID_BOSS_M_ATK_MODIFIER = 1.0;
	public static double RAID_BOSS_MAX_HP_MODIFIER = 1.0;
	public static double RAID_BOSS_MAX_MP_MODIFIER = 1.0;
	public static double RAID_BOSS_P_DEF_MODIFIER = 1.0;
	public static double RAID_BOSS_M_DEF_MODIFIER = 1.0;

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
	public static String DONATOR_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;";
	public static String DONATOR_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;";
	public static String DONATOR_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;";
	public static int ENCHANT_MAX_WEAPON_NORMAL = 0;
	public static int ENCHANT_MAX_ARMOR_NORMAL = 0;
	public static int ENCHANT_MAX_JEWELRY_NORMAL = 0;
	public static int ENCHANT_MAX_WEAPON_BLESSED = 0;
	public static int ENCHANT_MAX_ARMOR_BLESSED = 0;
	public static int ENCHANT_MAX_JEWELRY_BLESSED = 0;
	public static int ENCHANT_MAX_WEAPON_CRYSTAL = 0;
	public static int ENCHANT_MAX_ARMOR_CRYSTAL = 0;
	public static int ENCHANT_MAX_JEWELRY_CRYSTAL = 0;
	public static int ENCHANT_MAX_WEAPON_DONATOR = 0;
	public static int ENCHANT_MAX_ARMOR_DONATOR = 0;
	public static int ENCHANT_MAX_JEWELRY_DONATOR = 0;
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

	// Skill Enchant Cost Settings
	public static boolean ENCHANT_SKILL_SP_BOOK_NEEDED = true;
	public static boolean ENCH_SKILL_SP_NEEDED = true;
	public static boolean ENCH_SKILL_XP_NEEDED = true;

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

	public static boolean ALLOW_AUTO_FARM = true;
	public static boolean ALLOW_DRESS_ME = true;
	public static boolean ALLOW_PVP_RANK = true;
	public static boolean ALLOW_RESET = true;
	public static boolean ALLOW_ROULETTE = true;
	public static boolean ALLOW_AIO = true;
	public static boolean ALLOW_VIP = true;
	public static boolean ALLOW_ACHIEVEMENTS = true;
	public static boolean ALLOW_VOTE = false;
	public static boolean ALLOW_QUAKE = true;

	// =========================================================================
	// ONDA 11: EVENTOS PVP (tvtevent.properties, ctfevent.properties, dmevent.properties)
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

	// PvP Event Rules & Team Locs
	public static boolean TVT_AURA = true;
	public static boolean TVT_JOIN_WITH_CURSED_WEAPON = false;
	public static boolean TVT_ON_START_REMOVE_ALL_EFFECTS = true;
	public static boolean TVT_ON_START_UNSUMMON_PET = true;
	public static boolean TVT_CLOSE_COLISEUM_DOORS = true;
	public static boolean TVT_PRICE_NO_KILLS = false;
	public static int TVT_JOIN_TIME = 5;
	public static int TVT_EVENT_TIME = 15;
	public static boolean TVT_ALLOW_INTERFERENCE = false;
	public static boolean TVT_ALLOW_POTIONS = false;
	public static boolean TVT_ALLOW_SUMMON = true;
	public static boolean TVT_REVIVE_RECOVERY = true;
	public static int TVT_REVIVE_DELAY = 10;
	public static boolean TVT_ORIGINAL_POSITION = false;
	public static boolean TVT_ALLOW_ENEMY_HEALING = false;
	public static boolean TVT_ALLOW_TEAM_CASTING = false;
	public static boolean TVT_ALLOW_TEAM_ATTACKING = false;
	public static String TVT_BLUE_TEAM_LOC = "150545,46734,-3415";
	public static String TVT_RED_TEAM_LOC = "148386,46747,-3415";

	public static boolean CTF_AURA = true;
	public static boolean CTF_IN_INSTANCE = true;
	public static boolean CTF_ORIGINAL_POSITION = false;
	public static boolean CTF_JOIN_WITH_CURSED_WEAPON = false;
	public static boolean CTF_CLOSE_COLISEUM_DOORS = true;
	public static int CTF_JOIN_TIME = 5;
	public static int CTF_EVENT_TIME = 15;
	public static boolean CTF_ALLOW_INTERFERENCE = false;
	public static boolean CTF_ALLOW_TEAM_CASTING = false;
	public static boolean CTF_ALLOW_POTIONS = false;
	public static boolean CTF_ALLOW_SUMMON = true;
	public static boolean CTF_REVIVE_RECOVERY = true;
	public static boolean CTF_ON_START_REMOVE_ALL_EFFECTS = true;
	public static boolean CTF_ON_START_UNSUMMON_PET = true;
	public static int CTF_REVIVE_DELAY = 10;
	public static String CTF_BLUE_TEAM_LOC = "150545,46734,-3415";
	public static String CTF_RED_TEAM_LOC = "148386,46747,-3415";
	public static String CTF_BLUE_FLAG_LOC = "150545,46734,-3415";
	public static String CTF_RED_FLAG_LOC = "148386,46747,-3415";

	public static boolean DM_AURA = true;
	public static boolean DM_ORIGINAL_POSITION = false;
	public static int DM_REG_TIME = 5;
	public static int DM_EVENT_TIME = 15;
	public static boolean DM_ON_START_REMOVE_ALL_EFFECTS = true;
	public static boolean DM_ON_START_UNSUMMON_PET = true;
	public static boolean DM_ON_START_RESTORE_HP_MP_CP = true;
	public static boolean DM_ALLOW_POTION = true;
	public static boolean DM_ALLOW_SUMMON = true;
	public static boolean DM_CURSED_WEAPON = false;
	public static boolean DM_ALLOW_INTERFERENCE = false;
	public static boolean DM_RESET_ALL_SKILL = false;
	public static int DM_REVIVE_DELAY = 10;
	public static String DM_EVENT_LOCATION = "149800,46800,-3412";

	public static String ARENA_DUEL_LOC = "149800,46800,-3412";
	public static String TOURNAMENT_1X1_LOC = "149800,46800,-3412";



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
	// HENNA & DYES (formulas.properties / altgame.properties)
	// =========================================================================
	public static int LIMIT_HENNA_INT = 5;
	public static int LIMIT_HENNA_STR = 5;
	public static int LIMIT_HENNA_MEN = 5;
	public static int LIMIT_HENNA_CON = 5;
	public static int LIMIT_HENNA_WIT = 5;
	public static int LIMIT_HENNA_DEX = 5;

	// =========================================================================
	// ONDA 9: NPCS, RAID BOSSES & GRAND BOSSES (bosses.properties, npc.properties, altgame.properties)
	// =========================================================================
	public static boolean ANNOUNCE_RAID_SPAWN = false;
	public static boolean ANNOUNCE_RAID_DEATH = true;
	public static int ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = -1;
	public static int MAX_DRIFT_RANGE = 120;
	public static boolean DISABLE_RAID_BOSS_FOSSILIZATION = false;
	public static int MAX_LEVEL_RAID_BOSS_CURSE = 87;

	// Raid Boss Penalties & Curse (Dream / Lucera)
	public static int RAID_MAX_LEVEL_DIFF = 8;
	public static boolean PARALIZE_ON_RAID_LEVEL_DIFF = true;
	public static int DEEP_BLUE_DROP_RAID_MAX_DIFF = 2;

	// Raid Boss Rate & Stat Modifiers
	public static float RAID_MIN_RESPAWN_MULTIPLIER = 1.0f;
	public static float RAID_MAX_RESPAWN_MULTIPLIER = 1.0f;
	public static float RATE_RAID_EXP = 1.0f;
	public static float RATE_RAID_SP = 1.0f;
	public static float RATE_RAID_REGEN = 1.0f;
	public static float RATE_RAID_DEFENSE = 1.0f;
	public static float RATE_RAID_ATTACK = 1.0f;

	// Raid AI & Home behavior
	public static boolean RETURN_HOME_BOSSES_FROM_PVP = false;
	public static boolean RETURN_HOME_BOSSES_FROM_TOWN = true;

	// Grand Boss Entry & Mechanics (Dream / Lucera)
	public static boolean QUEST_REQUIRED_FOR_BOSS = true;
	public static boolean AUTO_LOOT_GRAND = false;
	public static boolean CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC = false;

	public static int QUEEN_ANT_MAX_SAFE_LEVEL = 48;
	public static boolean ZAKEN_USE_TELEPORT = true;
	public static boolean ORFEN_USE_TELEPORT = true;
	public static boolean BAIUM_CHECK_QUEST_FOR_AWAKE = true;


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
	public static List<Integer> ALT_OLY_RESTRICTED_ITEMS = List.of();
	public static List<Integer> ALT_OLY_RESTRICTED_SKILLS = List.of();
	public static boolean ALT_OLY_MATCH_HEAL_COUNTS = false;
	public static boolean ALT_OLY_SUMMON_DAMAGE_COUNTS = false;
	public static boolean OLYMPIAD_ALLOW_BSS = false;
	public static boolean ALT_OLY_SHOW_MONTHLY_WINNERS = true;
	public static boolean ALY_OLY_LOG_FIGHTS = true;
	public static String OLYMPIAD_DURATION_TYPE = "Month";
	public static int OLYMPIAD_DURATION = 1;
	public static boolean INCLUDE_SUMMON_DAMAGE = true;


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

	// SIEGE TOWERS, ARTEFACTS & FORTRESSES (siege.properties)
	public static boolean CORRECT_DATE_BY_7S = true;
	public static String CL_SET_SIEGE_TIME_LIST = "hour";
	public static String SIEGE_HOUR_LIST = "16,20";
	public static int REWARD_ID = 0;
	public static int REWARD_COUNT = 0;
	public static String GLUDIO_CONTROL_TOWER_1 = "-18133,109474,-2657,13002,8000";
	public static String GLUDIO_CONTROL_TOWER_2 = "-18137,108583,-2379,13002,20000";
	public static String GLUDIO_CONTROL_TOWER_3 = "-18061,107294,-2409,13002,8000";
	public static String GLUDIO_CONTROL_TOWER_4 = "-18359,112879,-2409,13002,8000";
	public static String GLUDIO_ARTEFACT_1 = "-18120,107984,-2483,16384,35063";
	public static String GLUDIO_FLAME_TOWER_1 = "-18154,107591,-2560,13004,70009,71009";
	public static String GLUDIO_FLAME_TOWER_2 = "-19329,108154,-2384,13004,70010,71010";
	public static String GIRAN_CONTROL_TOWER_1 = "118623,145150,-2476,13002,10000";
	public static String GIRAN_CONTROL_TOWER_2 = "117339,145051,-2446,13002,30000";
	public static String GIRAN_CONTROL_TOWER_3 = "116116,145016,-2750,13002,10000";
	public static String GIRAN_CONTROL_TOWER_4 = "113049,144849,-2476,13002,10000";
	public static String GIRAN_ARTEFACT_1 = "117939,145090,-2550,32768,35147";
	public static String GIRAN_FLAME_TOWER_1 = "118331,145055,-2627,13004,70013,71013";
	public static String GIRAN_FLAME_TOWER_2 = "117768,143880,-2451,13004,70014,71014";
	public static String DION_CONTROL_TOWER_1 = "22158,161167,-2573,13002,8000";
	public static String DION_CONTROL_TOWER_2 = "22138,159901,-2877,13002,20000";
	public static String DION_CONTROL_TOWER_3 = "22027,162449,-2603,13002,80000";
	public static String DION_CONTROL_TOWER_4 = "22319,156863,-2603,13002,8000";
	public static String DION_ARTEFACT_1 = "22081,161771,-2677,49017,35105";
	public static String DION_FLAME_TOWER_1 = "22114,162159,-2754,13004,70011,71011";
	public static String DION_FLAME_TOWER_2 = "23289,161596,-2578,13004,70012,71012";
	public static String OREN_CONTROL_TOWER_1 = "83416,37164,-2173,13002,10000";
	public static String OREN_CONTROL_TOWER_2 = "82129,37131,-2477,13002,30000";
	public static String OREN_CONTROL_TOWER_3 = "84709,37234,-2203,13002,10000";
	public static String OREN_CONTROL_TOWER_4 = "79103,36942,-2203,13002,10000";
	public static String OREN_ARTEFACT_1 = "84014,37184,-2277,32768,35189";
	public static String OREN_FLAME_TOWER_1 = "84407,37150,-2354,13004,70015,71015";
	public static String OREN_FLAME_TOWER_2 = "83844,35975,-2178,13004,70016,71016";
	public static String ADEN_CONTROL_TOWER_1 = "147455,5624,-911,13002,6000";
	public static String ADEN_CONTROL_TOWER_2 = "147460,1303,-176,13002,6000";
	public static String ADEN_CONTROL_TOWER_3 = "146158,6929,-426,13002,6000";
	public static String ADEN_CONTROL_TOWER_4 = "148755,6930,-426,13002,6000";
	public static String ADEN_CONTROL_TOWER_5 = "148775,2351,-426,13002,6000";
	public static String ADEN_CONTROL_TOWER_6 = "146137,2351,-426,13002,6000";
	public static String ADEN_CONTROL_TOWER_7 = "144954,1603,-494,13002,6000";
	public static String ADEN_CONTROL_TOWER_8 = "149976,1585,-494,13002,6000";
	public static String ADEN_ARTEFACT_1 = "147465,1537,-373,16384,35233";
	public static String ADEN_FLAME_TOWER_1 = "149976,1583,-450,13004,70001,71001,70002,71002,70003,71003,70004,71004";
	public static String ADEN_FLAME_TOWER_2 = "144955,1603,-450,13004,70005,71005,70006,71006,70007,71007,70008,71008";
	public static String INNADRIL_CONTROL_TOWER_1 = "116062,248649,-973,13002,6000";
	public static String INNADRIL_CONTROL_TOWER_2 = "116037,249948,-669,13002,6000";
	public static String INNADRIL_CONTROL_TOWER_3 = "115977,251223,-699,13002,6000";
	public static String INNADRIL_CONTROL_TOWER_4 = "116261,245621,-699,13002,6000";
	public static String INNADRIL_ARTEFACT_1 = "116031,250555,-798,49200,35279";
	public static String INNADRIL_FLAME_TOWER_1 = "116065,250938,-850,13004,70017,71017";
	public static String INNADRIL_FLAME_TOWER_2 = "117240,250375,-674,13004,70018,71018";
	public static String GODDARD_CONTROL_TOWER_1 = "147456,-46029,-1360,13002,6000";
	public static String GODDARD_CONTROL_TOWER_2 = "150183,-48201,-1744,13002,6000";
	public static String GODDARD_CONTROL_TOWER_3 = "144741,-48188,-1744,13002,6000";
	public static String GODDARD_CONTROL_TOWER_4 = "147477,-48516,-505,13002,6000";
	public static String GODDARD_ARTEFACT_1 = "146601,-50439,-1505,32768,35322";
	public static String GODDARD_ARTEFACT_2 = "148350,-50457,-1505,0,35323";
	public static String GODDARD_FLAME_TOWER_1 = "148144,-46992,-1609,13004,70019,71019";
	public static String GODDARD_FLAME_TOWER_2 = "146784,-46992,-1609,13004,70020,71020";
	public static String RUNE_CONTROL_TOWER_1 = "18260,-49161,-571,13002,6000";
	public static String RUNE_CONTROL_TOWER_2 = "16690,-50330,-641,13002,6000";
	public static String RUNE_CONTROL_TOWER_3 = "16727,-47952,-641,13002,6000";
	public static String RUNE_CONTROL_TOWER_4 = "14796,-47041,1027,13002,6000";
	public static String RUNE_CONTROL_TOWER_5 = "14822,-51282,1027,13002,6000";
	public static String RUNE_CONTROL_TOWER_6 = "12259,-47510,1295,13002,6000";
	public static String RUNE_ARTEFACT_1 = "9132,-49152,1094,64270,35469";
	public static String RUNE_FLAME_TOWER_1 = "12864,-47440,-1087,13004,70021,71021";
	public static String RUNE_FLAME_TOWER_2 = "12225,-50767,1248,13004,70022,71022";
	public static String SCHUTTGART_CONTROL_TOWER_1 = "77561,-150087,371,13002,6000";
	public static String SCHUTTGART_CONTROL_TOWER_2 = "80306,-152257,-12,13002,6000";
	public static String SCHUTTGART_CONTROL_TOWER_3 = "74862,-152162,-12,13002,6000";
	public static String SCHUTTGART_CONTROL_TOWER_4 = "77568,-152541,1226,13002,6000";
	public static String SCHUTTGART_ARTEFACT_1 = "76668,-154520,226,0,35515";
	public static String SCHUTTGART_ARTEFACT_2 = "78446,-154524,227,0,35514";
	public static String SCHUTTGART_FLAME_TOWER_1 = "76872,-151043,120,13004,70023,71023";
	public static String SCHUTTGART_FLAME_TOWER_2 = "78233,-151037,120,13004,70024,71024";
	public static int FORT_SIEGE_LENGTH = 120;
	public static int FORT_COUNT_DOWN_LENGTH = 10;
	public static int SUSPICIOUS_MERCHANT_RESPAWN_DELAY = 180;
	public static int FORT_MAX_FLAGS = 1;
	public static int FORT_SIEGE_CLAN_MIN_LEVEL = 4;
	public static int FORT_ATTACKER_MAX_CLANS = 500;
	public static int FORT_REWARD_ID = 0;
	public static int FORT_REWARD_COUNT = 0;
	public static int COMBAT_FLAG_ID = 6718;
	public static String SHANTY_COMMANDER_1 = "-52435,155188,-1768,20000,35683";
	public static String SHANTY_COMMANDER_2 = "-52128,157752,-2024,29864,35677";
	public static String SHANTY_COMMANDER_3 = "-53944,155433,-2024,7304,35680";
	public static String SHANTY_FLAG_1 = "-53086,156493,-1896";
	public static String SHANTY_FLAG_2 = "-53054,156605,-1896";
	public static String SHANTY_FLAG_3 = "-53032,156689,-1896";
	public static String SOUTHERN_COMMANDER_1 = "-21328,218864,-2952,0,35719";
	public static String SOUTHERN_COMMANDER_2 = "-22992,218160,-3208,0,35713";
	public static String SOUTHERN_COMMANDER_3 = "-21520,221504,-3208,45328,35716";
	public static String SOUTHERN_COMMANDER_4 = "-22728,221746,-3200,33168,35721";
	public static String SOUTHERN_FLAG_1 = "-22386,219917,-3079";
	public static String SOUTHERN_FLAG_2 = "-22386,219798,-3079";
	public static String SOUTHERN_FLAG_3 = "-22386,219679,-3079";
	public static String HIVE_COMMANDER_1 = "15152,188128,-2640,0,35752";
	public static String HIVE_COMMANDER_2 = "17984,187536,-2896,45056,35746";
	public static String HIVE_COMMANDER_3 = "16016,189520,-2888,0,35749";
	public static String HIVE_FLAG_1 = "16685,188358,-2770";
	public static String HIVE_FLAG_2 = "16761,188306,-2770";
	public static String HIVE_FLAG_3 = "16847,188257,-2770";
	public static String VALLEY_COMMANDER_1 = "124768,121856,-2296,0,35788";
	public static String VALLEY_COMMANDER_2 = "124299,123614,-2552,49192,35782";
	public static String VALLEY_COMMANDER_3 = "124768,124640,-2552,54480,35785";
	public static String VALLEY_COMMANDER_4 = "128048,123344,-2536,35028,35790";
	public static String VALLEY_FLAG_1 = "125970,123653,-2429";
	public static String VALLEY_FLAG_2 = "126092,123650,-2429";
	public static String VALLEY_FLAG_3 = "126205,123648,-2429";
	public static String IVORY_COMMANDER_1 = "72400,2896,-2760,0,35821";
	public static String IVORY_COMMANDER_2 = "73788,5479,-3016,55136,35815";
	public static String IVORY_COMMANDER_3 = "71264,4144,-3008,0,35818";
	public static String IVORY_FLAG_1 = "72565,4436,-2888";
	public static String IVORY_FLAG_2 = "72660,4512,-2888";
	public static String IVORY_FLAG_3 = "72759,4594,-2888";
	public static String NARSELL_COMMANDER_1 = "154704,53856,-2968,0,35852";
	public static String NARSELL_COMMANDER_2 = "155576,56592,-3224,59224,35846";
	public static String NARSELL_COMMANDER_3 = "153328,54848,-3216,5512,35849";
	public static String NARSELL_FLAG_1 = "154567,55397,-3097";
	public static String NARSELL_FLAG_2 = "154650,55493,-3097";
	public static String NARSELL_FLAG_3 = "154715,55587,-3097";
	public static String BAYOU_COMMANDER_1 = "188624,38240,-3128,0,35888";
	public static String BAYOU_COMMANDER_2 = "188160,39920,-3376,49284,35882";
	public static String BAYOU_COMMANDER_3 = "188626,41066,-3376,57140,35885";
	public static String BAYOU_COMMANDER_4 = "191846,39764,-3368,33020,35890";
	public static String BAYOU_FLAG_1 = "189838,40063,-3253";
	public static String BAYOU_FLAG_2 = "189931,40060,-3253";
	public static String BAYOU_FLAG_3 = "190052,40062,-3253";
	public static String WHITE_SANDS_COMMANDER_1 = "117216,205648,-3048,0,35921";
	public static String WHITE_SANDS_COMMANDER_2 = "118880,203568,-3304,5396,35915";
	public static String WHITE_SANDS_COMMANDER_3 = "118560,206560,-3304,48872,35918";
	public static String WHITE_SANDS_FLAG_1 = "118640,205151,-3176";
	public static String WHITE_SANDS_FLAG_2 = "118690,205062,-3176";
	public static String WHITE_SANDS_FLAG_3 = "118742,204968,-3176";
	public static String BORDERLAND_COMMANDER_1 = "159664,-72224,-2584,0,35957";
	public static String BORDERLAND_COMMANDER_2 = "157968,-71659,-2832,59020,35951";
	public static String BORDERLAND_COMMANDER_3 = "157312,-70640,-2832,0,35954";
	public static String BORDERLAND_COMMANDER_4 = "160194,-68688,-2824,43272,35959";
	public static String BORDERLAND_FLAG_1 = "158817,-70229,-2708";
	public static String BORDERLAND_FLAG_2 = "158883,-70145,-2708";
	public static String BORDERLAND_FLAG_3 = "158946,-70045,-2708";
	public static String SWAMP_COMMANDER_1 = "71264,-60512,-2504,0,35995";
	public static String SWAMP_COMMANDER_2 = "71248,-62352,-2752,12388,35989";
	public static String SWAMP_COMMANDER_3 = "68688,-59648,-2752,56012,35992";
	public static String SWAMP_COMMANDER_4 = "68005,-60866,-2744,5424,35997";
	public static String SWAMP_FLAG_1 = "69829,-61087,-2629";
	public static String SWAMP_FLAG_2 = "69979,-61144,-2632";
	public static String SWAMP_FLAG_3 = "70069,-61182,-2629";
	public static String ARCHAIC_COMMANDER_1 = "109856,-142640,-2672,0,36028";
	public static String ARCHAIC_COMMANDER_2 = "109600,-139735,-2928,62612,36022";
	public static String ARCHAIC_COMMANDER_3 = "108223,-142209,-2920,8524,36025";
	public static String ARCHAIC_FLAG_1 = "109142,-141243,-2801";
	public static String ARCHAIC_FLAG_2 = "109184,-141129,-2801";
	public static String ARCHAIC_FLAG_3 = "109214,-141016,-2801";
	public static String FLORAN_COMMANDER_1 = "6528,151872,-2608,0,36064";
	public static String FLORAN_COMMANDER_2 = "7006,148242,-2856,32768,36058";
	public static String FLORAN_COMMANDER_3 = "4384,150992,-2856,0,36061";
	public static String FLORAN_COMMANDER_4 = "5246,152319,-2848,49151,36066";
	public static String FLORAN_FLAG_1 = "5293,149624,-2732";
	public static String FLORAN_FLAG_2 = "5306,149743,-2732";
	public static String FLORAN_FLAG_3 = "5299,149870,-2732";
	public static String CLOUD_MOUNTAIN_COMMANDER_1 = "-55248,90496,-2536,0,36102";
	public static String CLOUD_MOUNTAIN_COMMANDER_2 = "-55791,91856,-2792,0,36096";
	public static String CLOUD_MOUNTAIN_COMMANDER_3 = "-54168,92604,-2784,49196,36099";
	public static String CLOUD_MOUNTAIN_COMMANDER_4 = "-50913,92259,-2776,41188,36104";
	public static String CLOUD_MOUNTAIN_FLAG_1 = "-53354,91537,-2664";
	public static String CLOUD_MOUNTAIN_FLAG_2 = "-53237,91537,-2664";
	public static String CLOUD_MOUNTAIN_FLAG_3 = "-53112,91537,-2664";
	public static String TANOR_COMMANDER_1 = "58480,139648,-1464,0,36135";
	public static String TANOR_COMMANDER_2 = "61864,139257,-1728,46896,36129";
	public static String TANOR_COMMANDER_3 = "59436,140834,-1720,47296,36132";
	public static String TANOR_FLAG_1 = "60225,139771,-1597";
	public static String TANOR_FLAG_2 = "60362,139742,-1597";
	public static String TANOR_FLAG_3 = "60467,139727,-1597";
	public static String DRAGONSPINE_COMMANDER_1 = "13184,94928,-3144,0,36166";
	public static String DRAGONSPINE_COMMANDER_2 = "9472,94992,-3392,0,36160";
	public static String DRAGONSPINE_COMMANDER_3 = "12829,96214,-3392,49152,36163";
	public static String DRAGONSPINE_FLAG_1 = "11459,95308,-3264";
	public static String DRAGONSPINE_FLAG_2 = "11527,95301,-3264";
	public static String DRAGONSPINE_FLAG_3 = "11623,95311,-3264";
	public static String ANTHARAS_COMMANDER_1 = "79440,88752,-2600,0,36202";
	public static String ANTHARAS_COMMANDER_2 = "77262,91704,-2856,5112,36196";
	public static String ANTHARAS_COMMANDER_3 = "80929,90510,-2856,40192,36199";
	public static String ANTHARAS_COMMANDER_4 = "80755,89002,-2848,21984,36204";
	public static String ANTHARAS_FLAG_1 = "79470,91299,-2728";
	public static String ANTHARAS_FLAG_2 = "79528,91187,-2728";
	public static String ANTHARAS_FLAG_3 = "79580,91095,-2728";
	public static String WESTERN_COMMANDER_1 = "113481,-16058,-712,0,36240";
	public static String WESTERN_COMMANDER_2 = "109872,-16624,-968,16384,36234";
	public static String WESTERN_COMMANDER_3 = "112601,-13933,-960,49152,36237";
	public static String WESTERN_COMMANDER_4 = "113929,-14801,-960,32768,36242";
	public static String WESTERN_FLAG_1 = "111280,-14820,-839";
	public static String WESTERN_FLAG_2 = "111380,-14820,-839";
	public static String WESTERN_FLAG_3 = "111480,-14820,-839";
	public static String HUNTERS_COMMANDER_1 = "123232,94400,-1856,0,36278";
	public static String HUNTERS_COMMANDER_2 = "122688,95760,-2112,0,36272";
	public static String HUNTERS_COMMANDER_3 = "124305,96528,-2104,49151,36275";
	public static String HUNTERS_COMMANDER_4 = "127632,96240,-2096,40892,36280";
	public static String HUNTERS_FLAG_1 = "125155,95455,-1984";
	public static String HUNTERS_FLAG_2 = "125255,95455,-1984";
	public static String HUNTERS_FLAG_3 = "125355,95455,-1984";
	public static String AARU_COMMANDER_1 = "74288,186912,-2296,0,36311";
	public static String AARU_COMMANDER_2 = "71392,184720,-2552,5528,36305";
	public static String AARU_COMMANDER_3 = "71542,186410,-2552,55088,36308";
	public static String AARU_FLAG_1 = "73029,186303,-2424";
	public static String AARU_FLAG_2 = "73923,186247,-2424";
	public static String AARU_FLAG_3 = "72833,186178,-2424";
	public static String DEMON_COMMANDER_1 = "100752,-53664,-360,0,36347";
	public static String DEMON_COMMANDER_2 = "100688,-57440,-616,16384,36341";
	public static String DEMON_COMMANDER_3 = "99484,-54027,-616,0,36344";
	public static String DEMON_FLAG_1 = "100400,-55401,-488";
	public static String DEMON_FLAG_2 = "100400,-55301,-488";
	public static String DEMON_FLAG_3 = "100400,-55201,-488";
	public static String MONASTIC_COMMANDER_1 = "73680,-95456,-1144,0,36385";
	public static String MONASTIC_COMMANDER_2 = "70189,-93935,-1400,61576,36379";
	public static String MONASTIC_COMMANDER_3 = "73831,-94119,-1400,45536,36382";
	public static String MONASTIC_FLAG_1 = "72174,-94437,-1271";
	public static String MONASTIC_FLAG_2 = "72294,-94481,-1271";
	public static String MONASTIC_FLAG_3 = "72401,-94526,-1271";


	// =========================================================================
	// ONDA 13: SISTEMA AIOX & BUFF SHOP (aiox.properties)
	// =========================================================================
	public static boolean ENABLE_AIO_SYSTEM = true;
	public static boolean ENABLE_AIO_DELEVEL = false;
	public static int AIO_SET_DELEVEL = 1;
	public static boolean ALLOW_AIO_SPEAK_NPC = false;
	public static boolean ALLOW_AIO_LEAVE_TOWN = false;
	public static boolean ALLOW_AIO_TELEPORT = false;
	public static boolean ALLOW_AIO_NAME_COLOR = true;
	public static String AIO_NAME_COLOR = "88AA88";
	public static boolean ALLOW_AIO_TITLE_COLOR = true;
	public static String AIO_TITLE_COLOR = "88AA88";
	public static boolean ALLOW_AIO_DUAL = true;
	public static int AIO_DIAS = 30;
	public static int AIO_DIAS_2 = 60;
	public static int AIO_DIAS_3 = 90;
	public static List<Integer> AIO_ALLOWED_CLASS_IDS = List.of(10, 25, 38);
	public static String AIO_CLASSES_NAME = "Human Mystic, Elf Mystic or Dark Elf Mystic";
	public static int AIO_ITEM_ID = 9225;
	public static int AIO_ITEM_COUNT = 15;
	public static int AIO_ITEM_COUNT_2 = 30;
	public static int AIO_ITEM_COUNT_3 = 45;
	public static String AIO_COIN_TEXT = "Donate Coin";
	public static boolean BUFF_SHOP_ENABLE = true;
	public static int BUFF_SHOP_MAX_DAYS = 14;
	public static boolean ALLOW_OFFLINE_BUFF = true;
	public static int DEFAULT_BUFF_SHOP_SLOTS = 24;
	public static String BUFF_SHOP_NAME_COLOR = "808080";
	public static String BUFF_SHOP_TITLE_COLOR = "00DD00";
	public static int BUFF_SHOP_EFFECT = 80;
	public static String AIO_SKILLS = "";
	public static String VALID_BUFF_SHOP_SKILLS = "";

	// =========================================================================
	// ONDA 14: SISTEMA VIP, ITENS DE CLÃ, START CUSTOM & VOTOS (add-on.properties, vote.properties)
	// =========================================================================
	public static boolean ALLOW_VIP_NAME_COLOR = true;
	public static String VIP_NAME_COLOR = "0088FF";
	public static boolean ALLOW_VIP_TITLE_COLOR = true;
	public static String VIP_TITLE_COLOR = "0088FF";
	public static int VIP_DIAS = 30;
	public static int VIP_DIAS_2 = 60;
	public static int VIP_DIAS_3 = 90;
	public static int CLAN_SKILL_BY_ITEM = 0;
	public static List<Integer> CLAN_SKILL_ID = List.of(0, 0);
	public static int RAID_BOSS_INFO_PAGE_LIMIT = 12;
	public static int RAID_BOSS_DROP_PAGE_LIMIT = 12;
	public static String RAID_BOSS_DATE_FORMAT = "(MMM dd, HH:mm)";
	public static String RAID_BOSS_IDS = "29019,29020,29022,29028,29045,29062,29065,29099,29001,29006,29014,25325,25299,25309,25514,25302,25312,25527,25305,25315,25487,25490";
	public static int DAY_TO_SIEGE = 14;
	public static int HOUR_TO_SIEGE = 14;
	public static boolean CUSTOM_STARTER_ITEMS_ENABLED = false;
	public static String STARTING_CUSTOM_ITEMS_FIGHTER = "57,1000;";
	public static String STARTING_CUSTOM_ITEMS_MAGE = "57,1000;";
	public static boolean AUTO_RESTART_ENABLED = false;
	public static String AUTO_RESTART_TIME = "05:00";
	public static int AUTO_RESTART_COUNTDOWN = 300;
	public static boolean SERVER_GM_ONLY = false;
	public static boolean ANNOUNCE_HERO_LOGIN = false;
	public static boolean ANNOUNCE_AIOX_LOGIN = false;
	public static boolean ANNOUNCE_VIP_LOGIN = false;
	public static boolean ANNOUNCE_LORD_LOGIN = false;
	public static boolean ANNOUNCE_NEWBIE_LOGIN = false;
	public static String API_KEY_TOPZONE = "e2ec0d41791613092ac03b6243ec6b87";
	public static String SERVER_ID_KEY_TOPZONE = "14093";
	public static String API_KEY_HOPZONE = "0vocH6Te6bpQ89H8";
	public static String SERVER_ID_NETWORK = "l2nightmare";
	public static int VOTE_SYSTEM_REWARD_ID = 3470;
	public static int VOTE_SYSTEM_REWARD_COUNT = 5;

	// =========================================================================
	// ONDA 15: REGRAS ALTERNATIVAS, CANCEL, ESCUDO & DURAÇÃO (altgame.properties)
	// =========================================================================
	public static boolean CANCEL_AUGMENTATION_EFFECT = true;
	public static boolean ALT_DANCE_MP_CONSUME = true;
	public static int ALT_BUFFER_TIME = 100;
	public static int ALT_5MIN_TIME = 100;
	public static int ALT_DANCE_TIME = 100;
	public static int ALT_SONG_TIME = 100;
	public static int ALT_HERO_TIME = 100;
	public static int ALT_CH_TIME = 1;
	public static boolean ENABLE_MODIFY_SKILL_DURATION = true;
	public static Map<Integer, Integer> SKILL_DURATION_LIST = Collections.emptyMap();
	public static int MAX_BUFF_AMOUNT = 50;
	public static boolean CANCEL_LESSER_EFFECT = true;
	public static boolean STORE_SKILL_COOLTIME = true;
	public static boolean GRADE_PENALTY = true;
	public static String ALT_GAME_CANCEL_BY_HIT = "all";
	public static boolean ALT_SHIELD_BLOCKS = false;
	public static int ALT_PERFECT_SHIELD_BLOCK_RATE = 5;
	public static boolean CONSUME_ON_SUCCESS = true;
	public static boolean ENABLE_STATIC_REUSE = false;
	public static boolean OLY_USE_STATIC_REUSE = false;
	public static int SKILL_REUSE_DELAY = 70;
	public static boolean USE_LEVEL_PENALTY = true;
	public static int M_CRIT_RATE = 2;
	public static boolean DISABLE_SKILLS_ON_LEVEL_LOST = false;
	public static boolean USE_CHAR_LEVEL_MODIFIER = true;
	public static String CANCEL_MODE = "new";
	public static boolean JAIL_IS_PVP_ZONE = false;
	public static List<String> FORBIDDEN_NAMES = List.of("admin", "gm", "gamemaster", "annoucements");

	public static boolean GRIDS_ALWAYS_ON = false;
	public static int GRID_NEIGHBOR_TURN_ON_TIME = 1;
	public static int GRID_NEIGHBOR_TURN_OFF_TIME = 90;
	public static boolean DESTROY_EQUIPABLE_ITEM = false;
	public static int AUTO_DESTROY_HERB_TIME = 60;
	public static List<Integer> LIST_OF_PROTECTED_ITEMS = List.of(57, 5575, 6673);
	public static boolean SAVE_DROPPED_ITEM = false;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = false;
	public static int SAVE_DROPPED_ITEM_INTERVAL = 60;
	public static boolean CLEAR_DROPPED_ITEM_TABLE = false;
	public static boolean LOAD_AUTO_ANNOUNCE_AT_STARTUP = false;
	public static boolean FAIL_FAKE_DEATH = false;
	public static int ALT_MINIMUM_FALL_HEIGHT = 400;
	public static double ALT_ATTACK_DELAY = 1.0;
	public static int BUFFER_HATE = 1;
	public static String UNAFFECTED_SKILLS = "0";
	public static String ALLOWED_SKILLS = "0";

	// =========================================================================
	// ONDA 16: DUELOS EM ARENA & TORNEIOS (ArenaDuel.properties, tournament.properties)
	// =========================================================================
	public static boolean ARENA_DUEL_ENABLE = true;
	public static int ARENA_DUEL_CHECK_INTERVAL = 15;
	public static int ARENA_DUEL_CALL_INTERVAL = 60;
	public static int ARENA_DUEL_WAIT_INTERVAL = 20;
	public static List<Integer> ARENA_DUEL_ITEMS_RESTRICTION = List.of(1538, 5858);
	public static boolean ARENA_ALLOW_S = true;
	public static String ARENA_DUEL_REWARD = "3470,5";
	public static boolean TOURNAMENT_1X1_ENABLE = false;
	public static int TOURNAMENT_CHECK_INTERVAL = 15;
	public static int TOURNAMENT_CALL_INTERVAL = 60;
	public static int TOURNAMENT_WAIT_INTERVAL = 20;
	public static List<Integer> TOURNAMENT_ITEMS_RESTRICTION = List.of(1538, 5858);
	public static String TOURNAMENT_1X1_REWARD = "3470,5";
	public static boolean TOURNAMENT_1X1_HWID_BLOCK = false;

	// =========================================================================
	// ONDA 17: ADMINISTRACAO & ACESSO GM (access.properties)
	// =========================================================================
	public static boolean GM_STARTUP_INVISIBLE = false;
	public static boolean GM_STARTUP_INVULNERABLE = false;
	public static boolean GM_STARTUP_SILENCE = false;
	public static boolean GM_STARTUP_AUTO_LIST = false;
	public static boolean SHOW_GM_LOGIN = false;
	public static boolean EVERYONE_HAS_ADMIN_RIGHTS = false;
	public static boolean GM_ITEM_RESTRICTION = false;
	public static int GM_MAX_ENCHANT = 65535;
	public static int STANDARD_RESPAWN_DELAY = 60;
	public static boolean GM_AUDIT = true;
	public static boolean SHOW_HTML_CHAT = true;
	public static String GM_NAME_COLOR = "FF9900";
	public static String GM_TITLE_COLOR = "0099FF";
	public static List<String> SUPERUSERS = new ArrayList<>();

	// =========================================================================
	// ONDA 18: SERVICOS DE NPC, TELEPORTES & CLASS MASTER (npc.properties)
	// =========================================================================
	public static boolean FREE_TELEPORTING = false;
	public static int FREE_TELEPORTING_MIN_LVL = 1;
	public static int FREE_TELEPORTING_MAX_LVL = 99;
	public static boolean NOBLE_PASS_FREE_TP = false;
	public static int NOBLE_PASS_FREE_TP_MIN_LVL = 1;
	public static int NOBLE_PASS_FREE_TP_MAX_LVL = 99;
	public static boolean CLASS_MASTER = true;
	public static boolean ALT_CLASS_MASTER = true;
	public static boolean CLASS_MASTER_UPDATE_STRIDER = false;
	public static boolean CLASS_MASTER_ENTIRE_TREE = false;
	public static boolean ALLOW_RENT_PET = false;
	public static boolean ALLOW_WYVERN_UPGRADER = false;
	public static boolean ALT_MOB_AGGRO_IN_PEACE_ZONE = true;
	public static boolean ALT_ATTACKABLE_NPCS = false;
	public static boolean ALLOW_PET_WALKER = true;
	public static int MANAGER_CRYSTAL_COUNT = 25;
	public static boolean ALLOW_LETHAL_PROTECTION_MOBS = false;
	public static List<Integer> LETHAL_PROTECTED_MOBS = List.of(35062);

	// =========================================================================
	// ONDA 19: FUN EVENTS, MINIGAMES & CASAMENTO (fun_events.properties)
	// =========================================================================
	public static boolean ALT_CASTLE_FOR_DAWN = true;
	public static boolean ALT_CASTLE_FOR_DUSK = true;
	public static boolean ALT_REQUIRE_CLAN_CASTLE = false;
	public static int ALT_JOIN_DAWN_COST = 50000;
	public static boolean ANNOUNCE_MAMMON_SPAWN = false;
	public static boolean STRICT_SEVEN_SIGNS = true;
	public static boolean ANNOUNCE_7S = true;
	public static int ALT_FESTIVAL_MIN_PLAYER = 5;
	public static int ALT_MAX_PLAYER_CONTRIB = 1000000;
	public static int ALT_FESTIVAL_MANAGER_START = 120000;
	public static int ALT_FESTIVAL_LENGTH = 1080000;
	public static int ALT_FESTIVAL_CYCLE_LENGTH = 2280000;
	public static double ALT_DAWN_GATES_PDEF_MULT = 1.1;
	public static double ALT_DUSK_GATES_PDEF_MULT = 0.8;
	public static double ALT_DAWN_GATES_MDEF_MULT = 1.1;
	public static double ALT_DUSK_GATES_MDEF_MULT = 0.8;
	public static boolean PC_CAFFE_ENABLED = false;
	public static int PC_CAFE_INTERVAL = 10;
	public static int PC_CAFE_MIN_LEVEL = 20;
	public static int PC_CAFE_MAX_LEVEL = 80;
	public static int PC_CAFE_MIN_SCORE = 0;
	public static int PC_CAFE_MAX_SCORE = 10;
	public static int ALT_LOTTERY_PRIZE = 50000;
	public static int ALT_LOTTERY_TICKET_PRICE = 2000;
	public static double ALT_LOTTERY_5_NUMBER_RATE = 0.6;
	public static double ALT_LOTTERY_4_NUMBER_RATE = 0.2;
	public static double ALT_LOTTERY_3_NUMBER_RATE = 0.2;
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = 200;
	public static boolean CHAMPION_ENABLE = false;
	public static int CHAMPION_FREQUENCY = 0;
	public static int CHAMPION_MIN_LVL = 20;
	public static int CHAMPION_MAX_LVL = 60;
	public static int CHAMPION_HP = 7;
	public static int CHAMPION_REWARDS = 8;
	public static boolean CHAMPION_PASSIVE = false;
	public static String CHAMPION_TITLE = "Champion";
	public static double CHAMPION_HP_REGEN = 1.0;
	public static double CHAMPION_ATK = 1.0;
	public static double CHAMPION_SPD_ATK = 1.0;
	public static int CHAMPION_ADENAS_REWARDS = 1;
	public static int CHAMPION_EXP_SP = 8;
	public static boolean CHAMPION_BOSS = false;
	public static boolean CHAMPION_MINIONS = false;
	public static int CHAMPION_SPECIAL_ITEM_LEVEL_DIFF = 0;
	public static int CHAMPION_SPECIAL_ITEM_CHANCE = 0;
	public static int CHAMPION_SPECIAL_ITEM_ID = 6393;
	public static int CHAMPION_SPECIAL_ITEM_AMOUNT = 1;
	public static boolean ALLOW_WEDDING = true;
	public static boolean SPAWN_WEDDING_NPC = false;
	public static int WEDDING_PRICE = 500000;
	public static boolean WEDDING_PUNISH_INFIDELITY = true;
	public static boolean WEDDING_TELEPORT = true;
	public static int WEDDING_TELEPORT_PRICE = 500;
	public static int WEDDING_TELEPORT_INTERVAL = 120;
	public static boolean WEDDING_ALLOW_SAME_SEX = false;
	public static boolean WEDDING_FORMAL_WEAR = true;
	public static int WEDDING_DIVORCE_COSTS = 20;
	public static boolean WEDDING_GIVE_BOW = true;
	public static boolean WEDDING_HONEYMOON = false;
	public static boolean WEDDING_USE_NICK_COLOR = true;
	public static String WEDDING_NORMAL_PAIR_NICK_COLOR = "BF0000";
	public static String WEDDING_GAY_PAIR_NICK_COLOR = "0000BF";
	public static String WEDDING_LESBI_PAIR_NICK_COLOR = "BF00BF";
	public static boolean MEDAL_ADD_DROP = false;
	public static int MEDAL_1_DROP_CHANCE = 10;
	public static int MEDAL_2_DROP_CHANCE = 2;
	public static boolean CRISTMAS_ADD_DROP = false;
	public static int CRISTMAS_DROP_CHANCE = 10;
	public static int CRISTMAS_TREE_LIFE_TIME = 5;
	public static boolean L2_DAY_ADD_DROP = false;
	public static int L2_DAY_DROP_CHANCE = 10;
	public static boolean BIG_SQUASH_ADD_DROP = false;
	public static int BIG_SQUASH_DROP_CHANCE = 10;
	public static int ALT_FESTIVAL_FIRST_SPAWN = 120000;
	public static int ALT_FESTIVAL_FIRST_SWARM = 300000;
	public static int ALT_FESTIVAL_SECOND_SPAWN = 540000;
	public static int ALT_FESTIVAL_SECOND_SWARM = 720000;
	public static int ALT_FESTIVAL_CHEST_SPAWN = 900000;
	public static int ALT_FESTIVAL_ARCHER_AGGRO = 200;
	public static int ALT_FESTIVAL_CHEST_AGGRO = 0;
	public static int ALT_FESTIVAL_MONSTER_AGGRO = 200;
	public static boolean ENABLE_EVENT_MESSAGE = false;
	public static boolean STAR_ADD_DROP = false;
	public static int STAR_1_DROP_CHANCE = 10;
	public static int STAR_2_DROP_CHANCE = 5;
	public static int STAR_3_DROP_CHANCE = 2;
	public static boolean STAR_SPAWN_MANAGER = false;
	public static boolean MEDAL_SPAWN_MENEGER = false;
	public static boolean CRISTMAS_SPAWN_SANTA = false;
	public static boolean L2_DAY_SPAWN_MANAGER = false;
	public static int L2_DAY_SCROLL_CHS = 300;
	public static int L2_DAY_ENCH_SCROLL_CHS = 100;
	public static int L2_DAY_ACC_SCROLL_CHS = 10;
	public static String L2_DAY_REWARDS = "3931,3927,3928,3929,3926,3930,3933,3932,3935,3934";
	public static String L2_DAY_REWARDS_ACCESSORIE = "6662,6660";
	public static String L2_DAY_REWARDS_SCROLL = "3958,3959";
	public static boolean L2_DROP_DAY_ADD_DROP = false;
	public static int L2_DROP_DAY_ITEM_1 = 9500;
	public static int L2_DROP_DAY_CHANCE_1 = 1;
	public static int L2_DROP_DAY_ITEM_2 = 9501;
	public static int L2_DROP_DAY_CHANCE_2 = 1;
	public static int L2_DROP_DAY_ITEM_3 = 9502;
	public static int L2_DROP_DAY_CHANCE_3 = 1;
	public static int L2_DROP_DAY_ITEM_4 = 9503;
	public static int L2_DROP_DAY_CHANCE_4 = 1;
	public static int L2_DROP_DAY_ITEM_5 = 9504;
	public static int L2_DROP_DAY_CHANCE_5 = 1;
	public static int L2_DROP_DAY_ITEM_6 = 9505;
	public static int L2_DROP_DAY_CHANCE_6 = 1;
	public static int L2_DROP_DAY_ITEM_7 = 9506;
	public static int L2_DROP_DAY_CHANCE_7 = 1;
	public static int L2_DROP_DAY_ITEM_8 = 9507;
	public static int L2_DROP_DAY_CHANCE_8 = 1;
	public static int L2_DROP_DAY_ITEM_9 = 9508;
	public static int L2_DROP_DAY_CHANCE_9 = 1;
	public static int L2_DROP_DAY_ITEM_10 = 9509;
	public static int L2_DROP_DAY_CHANCE_10 = 1;
	public static int L2_DROP_DAY_ITEM_11 = 9510;
	public static int L2_DROP_DAY_CHANCE_11 = 1;
	public static int L2_DROP_DAY_ITEM_12 = 9511;
	public static int L2_DROP_DAY_CHANCE_12 = 1;
	public static int L2_DROP_DAY_ITEM_13 = 9512;
	public static int L2_DROP_DAY_CHANCE_13 = 1;
	public static int L2_DROP_DAY_ITEM_14 = 9513;
	public static int L2_DROP_DAY_CHANCE_14 = 1;
	public static int L2_DROP_DAY_ITEM_15 = 9514;
	public static int L2_DROP_DAY_CHANCE_15 = 1;
	public static int L2_DROP_DAY_ITEM_16 = 9515;
	public static int L2_DROP_DAY_CHANCE_16 = 1;
	public static int L2_DROP_DAY_ITEM_17 = 9516;
	public static int L2_DROP_DAY_CHANCE_17 = 1;
	public static int L2_DROP_DAY_ITEM_18 = 9517;
	public static int L2_DROP_DAY_CHANCE_18 = 1;
	public static int L2_DROP_DAY_ITEM_19 = 9518;
	public static int L2_DROP_DAY_CHANCE_19 = 1;
	public static int L2_DROP_DAY_ITEM_20 = 9519;
	public static int L2_DROP_DAY_CHANCE_20 = 1;
	public static int L2_DROP_DAY_ITEM_21 = 9520;
	public static int L2_DROP_DAY_CHANCE_21 = 1;
	public static int L2_DROP_DAY_ITEM_22 = 9521;
	public static int L2_DROP_DAY_CHANCE_22 = 1;
	public static boolean BIG_SQUASH_SPAWN_MANAGER = false;
	public static boolean BIG_SQUASH_USE_SEEDS = false;
	public static boolean ARENA_ENABLED = false;
	public static int ARENA_INTERVAL = 60;
	public static int ARENA_REWARD_ID = 57;
	public static int ARENA_REWARD_COUNT = 0;
	public static boolean FISHERMAN_ENABLED = false;
	public static int FISHERMAN_INTERVAL = 60;
	public static int FISHERMAN_REWARD_ID = 57;
	public static int FISHERMAN_REWARD_COUNT = 0;
	public static int WEDDING_TELEPORT_X = 0;
	public static int WEDDING_TELEPORT_Y = 0;
	public static int WEDDING_TELEPORT_Z = 0;

	// =========================================================================
	// ONDA 20: GAMESERVER, SAFE REBOOT, DIMENSIONAL RIFT & RESPAWN (gameserver.properties)
	// =========================================================================
	public static int REQUEST_SERVER_ID = 1;
	public static boolean ACCEPT_ALTERNATE_ID = true;
	public static String TIME_ZONE = "America/Sao_Paulo";
	public static boolean BAN_CHAT_LOG = true;
	public static boolean BAN_ACCOUNT_LOG = true;
	public static boolean JAIL_LOG = true;
	public static boolean PLAYER_BAN_LOG = true;
	public static boolean CLASSIC_ANNOUNCE_MODE = true;
	public static boolean ANNOUNCE_BAN_CHAT = false;
	public static boolean ANNOUNCE_UNBAN_CHAT = false;
	public static boolean ANNOUNCE_BAN_ACCOUNT = false;
	public static boolean ANNOUNCE_UNBAN_ACCOUNT = false;
	public static boolean ANNOUNCE_JAIL = false;
	public static boolean ANNOUNCE_UNJAIL = false;
	public static boolean ANNOUNCE_BAN_CHAR = false;
	public static boolean ANNOUNCE_UNBAN_CHAR = false;
	public static int GLOBAL_BAN_TIME = 15;
	public static boolean SAFE_REBOOT = true;
	public static int SAFE_REBOOT_TIME = 30;
	public static boolean SAFE_REBOOT_DISABLE_ENCHANT = true;
	public static boolean SAFE_REBOOT_DISABLE_TELEPORT = true;
	public static boolean SAFE_REBOOT_DISABLE_CREATE_ITEM = true;
	public static boolean SAFE_REBOOT_DISABLE_TRANSACTION = true;
	public static boolean SAFE_REBOOT_DISABLE_PC_INTERACTION = true;
	public static boolean SAFE_REBOOT_DISABLE_NPC_INTERACTION = true;
	public static boolean ONLY_CLAN_LEADER_CAN_SIT_ON_THRONE = true;
	public static int RIFT_MIN_PARTY_SIZE = 2;
	public static int MAX_RIFT_JUMPS = 4;
	public static int RIFT_SPAWN_DELAY = 10000;
	public static int AUTO_JUMPS_DELAY_MIN = 480;
	public static int AUTO_JUMPS_DELAY_MAX = 600;
	public static double BOSS_ROOM_TIME_MULTIPLY = 1.5;
	public static int RECRUIT_COST = 18;
	public static int SOLDIER_COST = 21;
	public static int OFFICER_COST = 24;
	public static int CAPTAIN_COST = 27;
	public static int COMMANDER_COST = 30;
	public static int HERO_COST = 33;
	public static boolean RESPAWN_RANDOM_IN_TOWN = true;
	public static int RESPAWN_RANDOM_MAX_OFFSET = 20;
	public static int RESPAWN_RESTORE_CP = 30;
	public static int RESPAWN_RESTORE_HP = 70;
	public static int RESPAWN_RESTORE_MP = 40;
	public static int RAID_MINION_RESPAWN_TIME = 300000;
	public static int ALT_DEFAULT_RESTART_TOWN = 0;
	public static boolean USE_MONSTER_RND_SPAWN = true;
	public static int RND_SPAWN_ZONE = 300;
	public static boolean SPAWN_CLASS_MASTER = true;
	public static String CONFIG_CLASS_MASTER = "1;[57(100000)];[];2;[57(1000000)];[];3;[57(10000000)],[5575(1000000)];[6622(1)]";
	public static boolean ALLOW_DIALOG_CLASS_MASTER = true;
	public static boolean CLASS_MASTER_POPUP_WINDOW = true;

	public static boolean GAME_SERVER_LISTEN = true;
	public static boolean BAD_ID_CHECKING = true;
	public static String ID_FACTORY = "BitSet";
	public static boolean CLEAN_BAD_IDS = true;
	public static int REGISTRATION_MODE = 2;
	public static int REGISTRATION_TIME = 5;
	public static int MIN_PARTY_COUNT = 2;
	public static int MAX_PARTY_COUNT = 5;
	public static int MIN_PLAYER_COUNT = 1;
	public static int MAX_PLAYER_COUNT = 45;
	public static int TIME_LIMIT = 35;
	public static int HOT_SPRING_DEBUFF_CHANCE = 15;
	public static boolean PRIMAVEL_HAS_FLYING_MONSTERS = true;
	public static int PRIMAVEL_FLYING_MONSTERS_COUNT = 5;
	public static int FOG_MOBS_CLONE_CHANCE = 10;

	// Clan Hall Fees & Ratios
	public static int CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_1 = 7000;
	public static int CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_2 = 14000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_1 = 2500;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_2 = 5000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_3 = 7000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_4 = 11000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_5 = 21000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_6 = 36000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_7 = 37000;
	public static int CLAN_HALL_SUPPORT_FEE_LVL_8 = 52000;
	public static int CLAN_HALL_MP_REGENERATION_FEE_LVL_1 = 2000;
	public static int CLAN_HALL_MP_REGENERATION_FEE_LVL_2 = 3750;
	public static int CLAN_HALL_MP_REGENERATION_FEE_LVL_3 = 6500;
	public static int CLAN_HALL_MP_REGENERATION_FEE_LVL_4 = 13750;
	public static int CLAN_HALL_MP_REGENERATION_FEE_LVL_5 = 20000;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_1 = 700;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_2 = 800;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_3 = 1000;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_4 = 1166;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_5 = 1500;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_6 = 1750;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_7 = 2000;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_8 = 2250;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_9 = 2500;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_10 = 3250;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_11 = 3750;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_12 = 4250;
	public static int CLAN_HALL_HP_REGENERATION_FEE_LVL_13 = 5166;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_1 = 3000;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_2 = 6000;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_3 = 9000;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_4 = 15000;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_5 = 21000;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_6 = 23330;
	public static int CLAN_HALL_EXP_REGENERATION_FEE_LVL_7 = 30000;
	public static int CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_1 = 30000;
	public static int CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_2 = 70000;
	public static int CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_3 = 140000;
	public static int CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_1 = 2000;
	public static int CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_2 = 2500;
	public static int CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_1 = 1300;
	public static int CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_2 = 4000;
	public static long CLAN_HALL_TELEPORT_FUNCTION_FEE_RATIO = 604800000L;
	public static long CLAN_HALL_SUPPORT_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_MP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_HP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_EXP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_CURTAIN_FUNCTION_FEE_RATIO = 86400000L;
	public static long CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_RATIO = 259200000L;

	// Castle Fees & Ratios
	public static int CASTLE_TELEPORT_FUNCTION_FEE_LVL_1 = 7000;
	public static int CASTLE_TELEPORT_FUNCTION_FEE_LVL_2 = 14000;
	public static int CASTLE_SUPPORT_FEE_LVL_1 = 7000;
	public static int CASTLE_SUPPORT_FEE_LVL_2 = 21000;
	public static int CASTLE_SUPPORT_FEE_LVL_3 = 37000;
	public static int CASTLE_SUPPORT_FEE_LVL_4 = 52000;
	public static int CASTLE_MP_REGENERATION_FEE_LVL_1 = 2000;
	public static int CASTLE_MP_REGENERATION_FEE_LVL_2 = 6500;
	public static int CASTLE_MP_REGENERATION_FEE_LVL_3 = 13750;
	public static int CASTLE_MP_REGENERATION_FEE_LVL_4 = 20000;
	public static int CASTLE_HP_REGENERATION_FEE_LVL_1 = 1000;
	public static int CASTLE_HP_REGENERATION_FEE_LVL_2 = 1500;
	public static int CASTLE_HP_REGENERATION_FEE_LVL_3 = 2250;
	public static int CASTLE_HP_REGENERATION_FEE_LVL_4 = 3270;
	public static int CASTLE_HP_REGENERATION_FEE_LVL_5 = 5166;
	public static int CASTLE_EXP_REGENERATION_FEE_LVL_1 = 9000;
	public static int CASTLE_EXP_REGENERATION_FEE_LVL_2 = 15000;
	public static int CASTLE_EXP_REGENERATION_FEE_LVL_3 = 21000;
	public static int CASTLE_EXP_REGENERATION_FEE_LVL_4 = 30000;
	public static long CASTLE_TELEPORT_FUNCTION_FEE_RATIO = 604800000L;
	public static long CASTLE_SUPPORT_FUNCTION_FEE_RATIO = 86400000L;
	public static long CASTLE_MP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CASTLE_HP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long CASTLE_EXP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;

	// Fort Fees & Ratios
	public static int FORT_TELEPORT_FUNCTION_FEE_LVL_1 = 1000;
	public static int FORT_TELEPORT_FUNCTION_FEE_LVL_2 = 10000;
	public static int FORT_SUPPORT_FEE_LVL_1 = 7000;
	public static int FORT_SUPPORT_FEE_LVL_2 = 17000;
	public static int FORT_MP_REGENERATION_FEE_LVL_1 = 6500;
	public static int FORT_MP_REGENERATION_FEE_LVL_2 = 9300;
	public static int FORT_HP_REGENERATION_FEE_LVL_1 = 2000;
	public static int FORT_HP_REGENERATION_FEE_LVL_2 = 3500;
	public static int FORT_EXP_REGENERATION_FEE_LVL_1 = 9000;
	public static int FORT_EXP_REGENERATION_FEE_LVL_2 = 10000;
	public static long FORT_TELEPORT_FUNCTION_FEE_RATIO = 604800000L;
	public static long FORT_SUPPORT_FUNCTION_FEE_RATIO = 86400000L;
	public static long FORT_MP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long FORT_HP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;
	public static long FORT_EXP_REGENERATION_FUNCTION_FEE_RATIO = 86400000L;

	// =========================================================================
	// ONDA 21: NETWORK & LOGIN PROTOCOLS (network.properties, authserver.properties)
	// =========================================================================
	public static int MIN_PROTOCOL_VERSION = 730;
	public static int MAX_PROTOCOL_VERSION = 746;
	public static int MAXIMUM_DB_CONNECTIONS = 500;
	public static boolean ACCEPT_NEW_GAME_SERVER = false;
	public static int GM_MIN_LEVEL = 1;
	public static boolean BRUT_PROTECTION = true;
	public static int SESSION_TTL = 15;
	public static int MAX_SESSIONS = 100;
	public static int MAX_ACCOUNT_REGISTRATION = 3000;
	public static boolean ENABLE_FLOOD_PROTECTION = true;
	public static int FAST_CONNECTION_LIMIT = 15;
	public static int NORMAL_CONNECTION_TIME = 500;
	public static int FAST_CONNECTION_TIME = 250;
	public static int MAX_CONNECTION_PER_IP = 5;
	public static int INACTIVE_TIMEOUT = 3;
	public static int LOGIN_AUTH_PORT = 9014;
	public static String LOGIN_AUTH_HOSTNAME = "127.0.0.1";
	public static int IP_UPDATE_TIME = 10;
	public static int LOGIN_MAX_DB_CONNECTIONS = 1000;

	// REVISION (revision.properties)
	public static int REVISION_VERSION = 236;
	public static String REVISION_BUILD_DATE = "2025-05-28 22:20:05";

	// AUTHSERVER (login/authserver.properties)
	public static String ON_SELECT_SERVER = "notify";
	public static String ON_SELECT_SERVER_COMMAND = "";
	public static boolean LOGIN_SERVER_LISTEN = true;

	// DATABASE / NETWORK CREDENTIALS (network.properties)
	public static String DB_DRIVER = "org.mariadb.jdbc.Driver";
	public static String DB_URL = "jdbc:mariadb://localhost:3306/l2jdb";
	public static String DB_LOGIN = "root";
	public static String DB_PASSWORD = "";

	// OPTIONS (options.properties)
	public static boolean SERVER_LIST_BRACKETS = false;
	public static boolean SERVER_LIST_CLOCK = false;
	public static boolean LOG_ITEMS = true;
	public static String IGNORE_LOG_ITEMS = "CONSUME RESET";
	public static boolean ALLOW_LOTTERY = true;
	public static String GEO_DATA_ROOT = "./data";
	public static String GEO_ENGINE = "geodata";

	// NPC & CREATURES (npc.properties)
	public static boolean SHOW_NPC_LEVEL = true;
	public static int MIN_NPC_ANIMATION = 10;
	public static int MAX_NPC_ANIMATION = 20;
	public static int MIN_MONSTER_ANIMATION = 5;
	public static int MAX_MONSTER_ANIMATION = 20;
	public static int MIN_NPC_WALK_ANIMATION = 10;
	public static int MAX_NPC_WALK_ANIMATION = 20;
	public static int WYVERN_SPEED = 100;
	public static int STRIDER_SPEED = 80;
	public static int URN_TEMP_FAIL = 10;
	public static boolean FORCE_UPDATE_RAID_BOSS_ON_DB = false;
	public static int NO_RESTART_KICK_TIME = 5;

	// PLAYER & GAMEPLAY (player.properties)
	public static boolean ALT_GAME_VIEW_NPC = false;
	public static boolean ALT_GAME_VIEW_NPC_DROP = true;
	public static boolean ALLOW_EXCHANGE = false;
	public static int CRUMA_TOWER_LEVEL_RESTRICT = 56;
	public static int ALT_GAME_EXPONENT_XP = 0;
	public static int ALT_GAME_EXPONENT_SP = 0;
	public static double ALT_SUMMON_PENALTY_RATE = 1.0;
	public static boolean ALT_GAME_SKILL_LEARN = false;
	public static boolean AUTO_LEARN_DIVINE_INSPIRATION = false;
	public static boolean SP_BOOK_NEEDED = false;
	public static boolean LIFE_CRYSTAL_NEEDED = true;
	public static boolean DIVINE_INSPIRATION_SP_BOOK_NEEDED = true;
	public static boolean ALT_ITEM_SKILLS_NOT_INFLUENCED = false;
	public static boolean CHECK_SKILLS_ON_ENTER = false;
	public static boolean CHECK_ADDITIONAL_SKILLS = false;
	public static int SEND_NOT_DONE_SKILLS = 2;
	public static boolean ALLOW_USER_MENU = true;
	public static boolean ALLOW_USE_EXP_SET = true;
	public static List<Integer> LIST_PET_RENT_NPC = List.of(30827, 30828);
	public static boolean ALLOW_KEYBOARD_MOVEMENT = true;
	public static boolean CHECK_PLAYER_MACRO = true;
	public static List<String> MACRO_RESTRICTED_COMMAND_LIST = List.of("exit", "fly", "hero", "[gm]", "[adm]");

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
		ALLOW_OFFLINE_HOUR = ConfigLoader.getInt("AllowOfflineHour", 72);
		OFFLINE_TRADE_PRICE_ID = ConfigLoader.getInt("OfflineTradePriceID", 57);
		OFFLINE_TRADE_PRICE_COUNT = ConfigLoader.getLong("OfflineTradePriceCount", 500000L);
		OFFLINE_TRADE_PRICE_ID_TIME = ConfigLoader.getInt("OfflineTradePriceIDTime", 1);
		OFFLINE_CRAFT_PRICE_ID = ConfigLoader.getInt("OfflineCraftPriceID", 57);
		OFFLINE_CRAFT_PRICE_COUNT = ConfigLoader.getLong("OfflineCraftPriceCount", 500000L);
		OFFLINE_CRAFT_PRICE_ID_TIME = ConfigLoader.getInt("OfflineCraftPriceIDTime", 1);
		SHOW_NPC_CREST = ConfigLoader.getBoolean("ShowNpcCrest", false);
		CUSTOM_START_TITLE = ConfigLoader.getBoolean("CustomStartTitle", true);
		CUSTOM_TITLE_TEXT = ConfigLoader.getProperty("CustomTitleText", "L2JDream Project");
		TITLE_COLOR = ConfigLoader.getProperty("TitleColor", "00FF00");

		PVP_COLOR_SYSTEM = ConfigLoader.getBoolean("PvPColorSystem", false);
		PVP_COLOR_MODE = ConfigLoader.getProperty("PvPColorMode", "Title");
		PVP_AMMOUNT_1 = ConfigLoader.getInt("PvpAmmount1", 50);
		PVP_AMMOUNT_2 = ConfigLoader.getInt("PvpAmmount2", 100);
		PVP_AMMOUNT_3 = ConfigLoader.getInt("PvpAmmount3", 150);
		PVP_AMMOUNT_4 = ConfigLoader.getInt("PvpAmmount4", 250);
		PVP_AMMOUNT_5 = ConfigLoader.getInt("PvpAmmount5", 500);
		COLOR_FOR_AMMOUNT_1 = ConfigLoader.getProperty("ColorForAmmount1", "00FF00");
		COLOR_FOR_AMMOUNT_2 = ConfigLoader.getProperty("ColorForAmmount2", "00FF00");
		COLOR_FOR_AMMOUNT_3 = ConfigLoader.getProperty("ColorForAmmount3", "00FF00");
		COLOR_FOR_AMMOUNT_4 = ConfigLoader.getProperty("ColorForAmmount4", "00FF00");
		COLOR_FOR_AMMOUNT_5 = ConfigLoader.getProperty("ColorForAmmount5", "00FF00");
		TITLE_FOR_AMMOUNT_1 = ConfigLoader.getProperty("TitleForAmmount1", "00FF00");
		TITLE_FOR_AMMOUNT_2 = ConfigLoader.getProperty("TitleForAmmount2", "00FF00");
		TITLE_FOR_AMMOUNT_3 = ConfigLoader.getProperty("TitleForAmmount3", "00FF00");
		TITLE_FOR_AMMOUNT_4 = ConfigLoader.getProperty("TitleForAmmount4", "00FF00");
		TITLE_FOR_AMMOUNT_5 = ConfigLoader.getProperty("TitleForAmmount5", "00FF00");

		LOAD_VOICED_HELP = ConfigLoader.getBoolean("LoadVoicedHelp", true);
		LOAD_VOICED_OFFLINE = ConfigLoader.getBoolean("LoadVoicedOffline", true);
		LOAD_VOICED_WEDDING = ConfigLoader.getBoolean("LoadVoicedWedding", true);
		LOAD_VOICED_BANK = ConfigLoader.getBoolean("LoadVoicedBank", true);
		LOAD_VOICED_CONFIGURATOR = ConfigLoader.getBoolean("LoadVoicedConfigurator", true);
		LOAD_VOICED_CLASS_MASTER = ConfigLoader.getBoolean("LoadVoicedClassMaster", true);
		LOAD_VOICED_AIOX_COMMAND = ConfigLoader.getBoolean("LoadVoicedAioxCommand", true);
		LOAD_VOICED_AUTOFARM_COMMAND = ConfigLoader.getBoolean("LoadVoicedAutofarmCommand", true);
		LOAD_VOICED_ROULETTE_COMMAND = ConfigLoader.getBoolean("LoadVoicedRouletteCommand", true);
		LOAD_VOICED_RESET_COMMAND = ConfigLoader.getBoolean("LoadVoicedResetCommand", true);

		ENABLE_EVENT_MANAGER = ConfigLoader.getBoolean("EnableEventManager", false);
		EVENT_MANAGER_NPC_ID = ConfigLoader.getInt("EventManagerNpcId", 50004);
		ENABLE_AUTO_SPAWN = ConfigLoader.getBoolean("EnableAutoSpawn", false);
		EVENT_PARTICIPATION_FEE_ID = ConfigLoader.getInt("EventParticipationFeeId", 57);
		EVENT_PARTICIPATION_FEE_QNT = ConfigLoader.getLong("EventParticipationFeeQnt", 10000L);
		EVENT_BLOCK_CLASS_ID = getIntList("EventBlockClassId", List.of(15, 16, 97));
		EVENT_BLOCK_CLASS_NAMES = ConfigLoader.getProperty("EventBlockClassNames", "Cleric,Bishop,Cardinal");

		ACTIVATE_SIEGE_REWARD_SYSTEM = ConfigLoader.getBoolean("ActivateSystem", false);
		REWARD_ONLINE_ONLY = ConfigLoader.getBoolean("RewardOnlineOnly", false);
		REWARD_INFO = ConfigLoader.getProperty("RewardInfo", "57,2000;5575,2000000");
		REWARD_CL_INFO = ConfigLoader.getProperty("RewardClInfo", "57,2000;5575,200000");
		ENABLE_SKILL_REWARD_BY_TIME = ConfigLoader.getBoolean("EnableSkillRewardByTime", false);
		REWARD_SKILL_TIME = ConfigLoader.getInt("RewardSkillTime", 0);
		REWARD_SKILL_ID = ConfigLoader.getInt("RewardSkillID", 0);
		REWARD_SKILL_MAX_LVL = ConfigLoader.getInt("RewardSkillMaxLvl", 0);
		ENABLE_FIRST_LOGIN_REWARD = ConfigLoader.getBoolean("EnableFirstLoginReward", false);
		FIRST_LOGIN_REWARD_TIME_ITEM = ConfigLoader.getProperty("FirstLoginRewardTimeItem", "57,1");
		ENABLE_REWARD_BY_TIME = ConfigLoader.getBoolean("EnableRewardByTime", false);
		REWARD_ITEM_BY_TIME_MIN = ConfigLoader.getInt("RewardItemByTimeMin", 5);
		REWARD_ITEM_BY_TIME_ITEM = ConfigLoader.getProperty("RewardItemByTimeItem", "57,1");
		FIRST_LOGIN_BUFFS = ConfigLoader.getBoolean("FirstLoginBuffs", false);
		FIRST_LOGIN_FIGHTER_BUFF_LIST = getIntList("FirstLoginFighterBuffList", List.of());
		FIRST_LOGIN_MAGE_BUFF_LIST = getIntList("FirstLoginMageBuffList", List.of());
		ENABLE_STARTUP_SYSTEM = ConfigLoader.getBoolean("EnableStartupSystem", false);
		DISABLE_NEWBIE_TUTORIAL = ConfigLoader.getBoolean("DisableNewbieTutorial", false);
		STARTUP_SYSTEM_CLASS = ConfigLoader.getBoolean("StartupSystemClass", false);
		STARTUP_SYSTEM_ARMOR = ConfigLoader.getBoolean("StartupSystemArmor", false);
		STARTUP_SYSTEM_WEAPON = ConfigLoader.getBoolean("StartupSystemWeapon", false);
		STARTUP_SYSTEM_BUFF_FIGHT = ConfigLoader.getBoolean("StartupSystemBuffFight", true);
		FIGHTER_BUFF_LIST = getIntList("FighterBuffList", List.of());
		STARTUP_SYSTEM_BUFF_MAGE = ConfigLoader.getBoolean("StartupSystemBuffMage", true);
		MAGE_BUFF_LIST = getIntList("MageBuffList", List.of());
		HTM_ROBE = ConfigLoader.getProperty("htm_robe", "avadon_robe");
		SET_ROBE = ConfigLoader.getProperty("SetRobe", "2406,1;2415,1;5716,1;5732,1;895,1;895,1;864,1;864,1;926,1;");
		HTM_LIGHT = ConfigLoader.getProperty("htm_light", "doom_light");
		SET_LIGHT = ConfigLoader.getProperty("SetLight", "2392,1;2417,1;5723,1;5739,1;895,1;895,1;864,1;864,1;926,1;");
		HTM_HEAVY = ConfigLoader.getProperty("htm_heavy", "doom_heavy");
		SET_HEAVY = ConfigLoader.getProperty("SetHeavy", "2381,1;2417,1;5722,1;5738,1;895,1;895,1;864,1;864,1;926,1;");
		SHIELD_ID = ConfigLoader.getInt("Shield_ID", 110);
		ARROW_ID = ConfigLoader.getInt("Arrow_ID", 1343);

		BP_WEAPON_01 = ConfigLoader.getProperty("BpWeapon_01", "damascus_damage");
		WP_01_ID = ConfigLoader.getInt("Wp_01_ID", 4718);
		BP_WEAPON_02 = ConfigLoader.getProperty("BpWeapon_02", "damascus_haste");
		WP_02_ID = ConfigLoader.getInt("Wp_02_ID", 4719);
		BP_WEAPON_03 = ConfigLoader.getProperty("BpWeapon_03", "damascus_focus");
		WP_03_ID = ConfigLoader.getInt("Wp_03_ID", 4717);
		BP_WEAPON_04 = ConfigLoader.getProperty("BpWeapon_04", "lance_anger");
		WP_04_ID = ConfigLoader.getInt("Wp_04_ID", 4858);
		BP_WEAPON_05 = ConfigLoader.getProperty("BpWeapon_05", "lance_stun");
		WP_05_ID = ConfigLoader.getInt("Wp_05_ID", 4859);
		BP_WEAPON_06 = ConfigLoader.getProperty("BpWeapon_06", "lance_blow");
		WP_06_ID = ConfigLoader.getInt("Wp_06_ID", 4860);
		BP_WEAPON_07 = ConfigLoader.getProperty("BpWeapon_07", "wizard_acumen");
		WP_07_ID = ConfigLoader.getInt("Wp_07_ID", 8117);
		BP_WEAPON_08 = ConfigLoader.getProperty("BpWeapon_08", "wizard_power");
		WP_08_ID = ConfigLoader.getInt("Wp_08_ID", 8118);
		BP_WEAPON_09 = ConfigLoader.getProperty("BpWeapon_09", "wizard_conversion");
		WP_09_ID = ConfigLoader.getInt("Wp_09_ID", 8119);
		BP_WEAPON_10 = ConfigLoader.getProperty("BpWeapon_10", "tear_acumen");
		WP_10_ID = ConfigLoader.getInt("Wp_10_ID", 8117);
		BP_WEAPON_11 = ConfigLoader.getProperty("BpWeapon_11", "tear_power");
		WP_11_ID = ConfigLoader.getInt("Wp_11_ID", 8118);
		BP_WEAPON_12 = ConfigLoader.getProperty("BpWeapon_12", "tear_conversion");
		WP_12_ID = ConfigLoader.getInt("Wp_12_ID", 8119);
		BP_WEAPON_13 = ConfigLoader.getProperty("BpWeapon_13", "demons_damage");
		WP_13_ID = ConfigLoader.getInt("Wp_13_ID", 6359);
		BP_WEAPON_14 = ConfigLoader.getProperty("BpWeapon_14", "demons_bleed");
		WP_14_ID = ConfigLoader.getInt("Wp_14_ID", 4780);
		BP_WEAPON_15 = ConfigLoader.getProperty("BpWeapon_15", "demons_strike");
		WP_15_ID = ConfigLoader.getInt("Wp_15_ID", 4782);
		BP_WEAPON_16 = ConfigLoader.getProperty("BpWeapon_16", "bow_guidance");
		WP_16_ID = ConfigLoader.getInt("Wp_16_ID", 4828);
		BP_WEAPON_17 = ConfigLoader.getProperty("BpWeapon_17", "bow_shot");
		WP_17_ID = ConfigLoader.getInt("Wp_17_ID", 4830);
		BP_WEAPON_18 = ConfigLoader.getProperty("BpWeapon_18", "bow_recovery");
		WP_18_ID = ConfigLoader.getInt("Wp_18_ID", 4829);
		BP_WEAPON_19 = ConfigLoader.getProperty("BpWeapon_19", "great_damage");
		WP_19_ID = ConfigLoader.getInt("Wp_19_ID", 4724);
		BP_WEAPON_20 = ConfigLoader.getProperty("BpWeapon_20", "great_focus");
		WP_20_ID = ConfigLoader.getInt("Wp_20_ID", 4725);
		BP_WEAPON_21 = ConfigLoader.getProperty("BpWeapon_21", "great_health");
		WP_21_ID = ConfigLoader.getInt("Wp_21_ID", 4723);
		BP_WEAPON_22 = ConfigLoader.getProperty("BpWeapon_22", "axe_health");
		WP_22_ID = ConfigLoader.getInt("Wp_22_ID", 4753);
		BP_WEAPON_23 = ConfigLoader.getProperty("BpWeapon_23", "axe_focus");
		WP_23_ID = ConfigLoader.getInt("Wp_23_ID", 4754);
		BP_WEAPON_24 = ConfigLoader.getProperty("BpWeapon_24", "axe_haste");
		WP_24_ID = ConfigLoader.getInt("Wp_24_ID", 4755);
		BP_WEAPON_25 = ConfigLoader.getProperty("BpWeapon_25", "bellion_drain");
		WP_25_ID = ConfigLoader.getInt("Wp_25_ID", 4804);
		BP_WEAPON_26 = ConfigLoader.getProperty("BpWeapon_26", "bellion_poison");
		WP_26_ID = ConfigLoader.getInt("Wp_26_ID", 4805);
		BP_WEAPON_27 = ConfigLoader.getProperty("BpWeapon_27", "bellion_haste");
		WP_27_ID = ConfigLoader.getInt("Wp_27_ID", 4806);
		BP_WEAPON_28 = ConfigLoader.getProperty("BpWeapon_28", "samurai");
		WP_28_ID = ConfigLoader.getInt("Wp_28_ID", 2626);
		BP_WEAPON_29 = ConfigLoader.getProperty("BpWeapon_29", "0");
		WP_29_ID = ConfigLoader.getInt("Wp_29_ID", 0);
		BP_WEAPON_30 = ConfigLoader.getProperty("BpWeapon_30", "0");
		WP_30_ID = ConfigLoader.getInt("Wp_30_ID", 0);
		BP_WEAPON_31 = ConfigLoader.getProperty("BpWeapon_31", "0");
		WP_31_ID = ConfigLoader.getInt("Wp_31_ID", 0);

		LOAD_CUSTOM_TELEPORTS = ConfigLoader.getBoolean("LoadCustomTeleports", false);
		LOAD_CUSTOM_ITEM_TABLES = ConfigLoader.getBoolean("LoadCustomItemTables", false);
		LOAD_CUSTOM_NPC_TABLE = ConfigLoader.getBoolean("LoadCustomNpcTable", false);
		LOAD_CUSTOM_DROPLIST_TABLE = ConfigLoader.getBoolean("LoadCustomDroplistTable", false);
		LOAD_CUSTOM_ARMOR_SET_TABLE = ConfigLoader.getBoolean("LoadCustomArmorSetTable", false);
		LOAD_CUSTOM_SPAWNLIST_TABLE = ConfigLoader.getBoolean("LoadCustomSpawnlistTable", false);
		GOLDBAR_ENABLED = ConfigLoader.getBoolean("Enabled", true);

		ENABLE_CAPTCHA = ConfigLoader.getBoolean("EnableCaptcha", false);
		CAPTCHA_KILLS_COUNTER = ConfigLoader.getInt("KillsCounter", 60);
		CAPTCHA_KILLS_COUNTER_RANDOMIZATION = ConfigLoader.getInt("KillsCounterRandomization", 50);
		CAPTCHA_VALIDATION_TIME = ConfigLoader.getInt("ValidationTime", 60);
		CAPTCHA_PUNISHMENT = ConfigLoader.getInt("Punishment", 0);
		CAPTCHA_PUNISHMENT_TIME = ConfigLoader.getInt("PunishmentTime", 60);

		BANKING_SYSTEM_ENABLED = ConfigLoader.getBoolean("BankingEnabled", false);
		BANKING_SYSTEM_GOLDBARS = ConfigLoader.getInt("BankingGoldBarItemCount", 1);
		BANKING_SYSTEM_ADENA = ConfigLoader.getInt("BankingAdenaCount", 500000000);

		// Player
		// Fase A: Blow, Lethal, Consumables, PK
		BLOW_FRONT = ConfigLoader.getInt("BlowFront", 50);
		BLOW_SIDE = ConfigLoader.getInt("BlowSide", 60);
		BLOW_BEHIND = ConfigLoader.getInt("BlowBehind", 70);
		ALT_LETHAL_RATE_DAGGER = ConfigLoader.getDouble("AltLethalRateDagger", 1.0);
		ALT_LETHAL_RATE_ARCHERY = ConfigLoader.getDouble("AltLethalRateArchery", 2.0);
		ALT_LETHAL_RATE_OTHER = ConfigLoader.getDouble("AltLethalRateOther", 2.0);
		CONSUME_SOUL_SHOT = ConfigLoader.getBoolean("ConsumeSoulShot", true);
		CONSUME_ARROWS = ConfigLoader.getBoolean("ConsumeArrows", true);
		BLOCK_PARTY_INVITE_ON_COMBAT = ConfigLoader.getBoolean("BlockPartyInviteOnCombat", false);
		BLOCK_CHANGE_WEAPON_WHILE_ATTACKING = ConfigLoader.getBoolean("BlockChangeWeaponWhileAttacking", false);
		ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE = ConfigLoader.getBoolean("AltKarmaPlayerCanBeKilledInPeaceZone", false);
		ALT_KARMA_PLAYER_CAN_SHOP = ConfigLoader.getBoolean("AltKarmaPlayerCanShop", false);
		ALT_KARMA_PLAYER_CAN_TELEPORT = ConfigLoader.getBoolean("AltKarmaPlayerCanTeleport", true);
		ALT_KARMA_PLAYER_CAN_USE_GK = ConfigLoader.getBoolean("AltKarmaPlayerCanUseGK", false);
		ALT_KARMA_PLAYER_CAN_TRADE = ConfigLoader.getBoolean("AltKarmaPlayerCanTrade", true);
		ALT_KARMA_PLAYER_CAN_USE_WAREHOUSE = ConfigLoader.getBoolean("AltKarmaPlayerCanUseWarehouse", true);
		PVP_VS_NORMAL_TIME = ConfigLoader.getInt("PvPVsNormalTime", 10000);
		PVP_VS_PVP_TIME = ConfigLoader.getInt("PvPVsPvPTime", 40000);

		// Fase B: Augmentations, Rates & Regen
		AUGMENTATION_NG_SKILL_CHANCE = ConfigLoader.getInt("AugmentationNGSkillChance", 15);
		AUGMENTATION_MID_SKILL_CHANCE = ConfigLoader.getInt("AugmentationMidSkillChance", 30);
		AUGMENTATION_HIGH_SKILL_CHANCE = ConfigLoader.getInt("AugmentationHighSkillChance", 45);
		AUGMENTATION_TOP_SKILL_CHANCE = ConfigLoader.getInt("AugmentationTopSkillChance", 60);
		AUGMENTATION_BASE_STAT_CHANCE = ConfigLoader.getInt("AugmentationBaseStatChance", 1);
		AUGMENTATION_NG_GLOW_CHANCE = ConfigLoader.getInt("AugmentationNGGlowChance", 0);
		AUGMENTATION_MID_GLOW_CHANCE = ConfigLoader.getInt("AugmentationMidGlowChance", 40);
		AUGMENTATION_HIGH_GLOW_CHANCE = ConfigLoader.getInt("AugmentationHighGlowChance", 70);
		AUGMENTATION_TOP_GLOW_CHANCE = ConfigLoader.getInt("AugmentationTopGlowChance", 100);
		PLAYER_CP_REGEN_MULTIPLIER = ConfigLoader.getInt("PlayerCpRegenMultiplier", 100);
		PLAYER_HP_REGEN_MULTIPLIER = ConfigLoader.getInt("PlayerHpRegenMultiplier", 100);
		PLAYER_MP_REGEN_MULTIPLIER = ConfigLoader.getInt("PlayerMpRegenMultiplier", 100);
		MAX_WAREHOUSE_SLOTS_FOR_OTHER = ConfigLoader.getInt("MaxWarehouseSlotsForOther", 100);
		ALLOW_WAREHOUSE = ConfigLoader.getBoolean("AllowWarehouse", true);

		// Fase C: Bosses & Grand Bosses
		ANTHARAS_ARRIVED_TIME = ConfigLoader.getInt("AntharasArrivedTime", 15);
		ANTHARAS_ACTIVE_TIME = ConfigLoader.getInt("AntharasActiveTime", 240);
		ANTHARAS_MIN_RESPAWN = ConfigLoader.getInt("AntharasMinRespawn", 11520);
		ANTHARAS_MAX_RESPAWN = ConfigLoader.getInt("AntharasMaxRespawn", 15840);
		ANTHARAS_WEAK_PLAYERS = ConfigLoader.getInt("AntharasWeakPlayers", 50);
		ANTHARAS_MIDDLE_PLAYERS = ConfigLoader.getInt("AntharasMiddlePlayers", 80);
		ANTHARAS_INTERVAL_OF_BEHEMOTH = ConfigLoader.getInt("AntharasIntervalOfBehemoth", 4);
		VALAKAS_ARRIVED_TIME = ConfigLoader.getInt("ValakasArrivedTime", 1);
		VALAKAS_ACTIVE_TIME = ConfigLoader.getInt("ValakasActiveTime", 240);
		VALAKAS_MIN_RESPAWN = ConfigLoader.getInt("ValakasMinRespawn", 11520);
		VALAKAS_MAX_RESPAWN = ConfigLoader.getInt("ValakasMaxRespawn", 15840);
		BAIUM_ACTIVE_TIME = ConfigLoader.getInt("BaiumActiveTime", 240);
		BAIUM_NO_ATTACK_TIME = ConfigLoader.getInt("BaiumNoAttackTime", 20);
		BAIUM_MIN_RESPAWN = ConfigLoader.getInt("BaiumMinRespawn", 7200);
		BAIUM_MAX_RESPAWN = ConfigLoader.getInt("BaiumMaxRespawn", 10080);
		QUEEN_ANT_MIN_RESPAWN = ConfigLoader.getInt("QueenAntMinRespawn", 1140);
		QUEEN_ANT_MAX_RESPAWN = ConfigLoader.getInt("QueenAntMaxRespawn", 2160);
		QUEEN_ANT_NUMBER_OF_GUARDS = ConfigLoader.getInt("QueenAntNumberOfGuards", 8);
		QUEEN_ANT_NUMBER_OF_NURSES = ConfigLoader.getInt("QueenAntNumberOfNurses", 6);
		CORE_MIN_RESPAWN = ConfigLoader.getInt("CoreMinRespawn", 2220);
		CORE_MAX_RESPAWN = ConfigLoader.getInt("CoreMaxRespawn", 3600);
		CORE_NUMBER_OF_GUARDS = ConfigLoader.getInt("CoreNumberOfGuards", 4);
		ZAKEN_MIN_RESPAWN = ConfigLoader.getInt("ZakenMinRespawn", 2400);
		ZAKEN_MAX_RESPAWN = ConfigLoader.getInt("ZakenMaxRespawn", 3600);
		ORFEN_MIN_RESPAWN = ConfigLoader.getInt("OrfenMinRespawn", 1680);
		ORFEN_MAX_RESPAWN = ConfigLoader.getInt("OrfenMaxRespawn", 2880);

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
		ANNOUNCE_PK_PVP_PK_MESSAGE = ConfigLoader.getBoolean("AnnouncePkPvPPkMessage", true);
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
		DONATOR_WEAPON_ENCHANT_LEVEL = ConfigLoader.getProperty("DonatorWeaponEnchantLevel", "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;");
		DONATOR_ARMOR_ENCHANT_LEVEL = ConfigLoader.getProperty("DonatorArmorEnchantLevel", "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;");
		DONATOR_JEWELRY_ENCHANT_LEVEL = ConfigLoader.getProperty("DonatorJewelryEnchantLevel", "1,100;2,100;3,100;4,99;5,98;6,97;7,96;8,95;9,94;10,93;11,92;12,91;13,90;14,89;15,88;16,87;");
		ENCHANT_MAX_WEAPON_NORMAL = ConfigLoader.getInt("EnchantMaxWeaponNormal", 0);
		ENCHANT_MAX_ARMOR_NORMAL = ConfigLoader.getInt("EnchantMaxArmorNormal", 0);
		ENCHANT_MAX_JEWELRY_NORMAL = ConfigLoader.getInt("EnchantMaxJewelryNormal", 0);
		ENCHANT_MAX_WEAPON_BLESSED = ConfigLoader.getInt("EnchantMaxWeaponBlessed", 0);
		ENCHANT_MAX_ARMOR_BLESSED = ConfigLoader.getInt("EnchantMaxArmorBlessed", 0);
		ENCHANT_MAX_JEWELRY_BLESSED = ConfigLoader.getInt("EnchantMaxJewelryBlessed", 0);
		ENCHANT_MAX_WEAPON_CRYSTAL = ConfigLoader.getInt("EnchantMaxWeaponCrystal", 0);
		ENCHANT_MAX_ARMOR_CRYSTAL = ConfigLoader.getInt("EnchantMaxArmorCrystal", 0);
		ENCHANT_MAX_JEWELRY_CRYSTAL = ConfigLoader.getInt("EnchantMaxJewelryCrystal", 0);
		ENCHANT_MAX_WEAPON_DONATOR = ConfigLoader.getInt("EnchantMaxWeaponDonator", 0);
		ENCHANT_MAX_ARMOR_DONATOR = ConfigLoader.getInt("EnchantMaxArmorDonator", 0);
		ENCHANT_MAX_JEWELRY_DONATOR = ConfigLoader.getInt("EnchantMaxJewelryDonator", 0);
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
		DEATH_PENALTY_CHANCE = ConfigLoader.getInt("DeathPenaltyChance", 20);

		// Henna limits (formulas.properties / altgame.properties)
		LIMIT_HENNA_INT = ConfigLoader.getInt("HennaLimitINT", 5);
		LIMIT_HENNA_STR = ConfigLoader.getInt("HennaLimitSTR", 5);
		LIMIT_HENNA_MEN = ConfigLoader.getInt("HennaLimitMEN", 5);
		LIMIT_HENNA_CON = ConfigLoader.getInt("HennaLimitCON", 5);
		LIMIT_HENNA_WIT = ConfigLoader.getInt("HennaLimitWIT", 5);
		LIMIT_HENNA_DEX = ConfigLoader.getInt("HennaLimitDEX", 5);

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
		ALLOW_AUTO_FARM = ConfigLoader.getBoolean("AllowAutoFarm", true);
		ALLOW_DRESS_ME = ConfigLoader.getBoolean("AllowDressMe", true);
		ALLOW_PVP_RANK = ConfigLoader.getBoolean("AllowPvpRank", true);
		ALLOW_RESET = ConfigLoader.getBoolean("AllowReset", true);
		ALLOW_ROULETTE = ConfigLoader.getBoolean("AllowRoulette", true);
		ALLOW_AIO = ConfigLoader.getBoolean("AllowAio", true);
		ALLOW_VIP = ConfigLoader.getBoolean("AllowVip", true);
		ALLOW_ACHIEVEMENTS = ConfigLoader.getBoolean("AllowAchievements", true);
		ALLOW_VOTE = ConfigLoader.getBoolean("AllowVote", false);
		ALLOW_QUAKE = ConfigLoader.getBoolean("AllowQuake", true);

		// Onda 9: Bosses, Raids e Grand Bosses
		ANNOUNCE_RAID_SPAWN = ConfigLoader.getBoolean("AnnounceRaidSpawn", false);
		ANNOUNCE_RAID_DEATH = ConfigLoader.getBoolean("AnnounceRaidDeath", true);
		ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE = ConfigLoader.getInt("AltMobNoAttackWithLevelDifference", -1);
		MAX_DRIFT_RANGE = ConfigLoader.getInt("MaxDriftRange", 120);
		DISABLE_RAID_BOSS_FOSSILIZATION = ConfigLoader.getBoolean("DisableRaidBossFossilization", false);
		MAX_LEVEL_RAID_BOSS_CURSE = ConfigLoader.getInt("MaxLevelRaidBossCurse", 87);

		RAID_MAX_LEVEL_DIFF = ConfigLoader.getInt("RaidMaxLevelDiff", 8);
		PARALIZE_ON_RAID_LEVEL_DIFF = ConfigLoader.getBoolean("ParalizeOnRaidLevelDiff", true);
		DEEP_BLUE_DROP_RAID_MAX_DIFF = ConfigLoader.getInt("DeepBlueDropRaidMaxDiff", 2);

		RAID_MIN_RESPAWN_MULTIPLIER = ConfigLoader.getFloat("RaidMinRespawnMultiplier", 1.0f);
		RAID_MAX_RESPAWN_MULTIPLIER = ConfigLoader.getFloat("RaidMaxRespawnMultiplier", 1.0f);
		RATE_RAID_EXP = ConfigLoader.getFloat("RateExpRaidBoss", 1.0f);
		RATE_RAID_SP = ConfigLoader.getFloat("RateSpRaidBoss", 1.0f);
		RATE_RAID_REGEN = ConfigLoader.getFloat("RateRaidRegen", 1.0f);
		RATE_RAID_DEFENSE = ConfigLoader.getFloat("RateRaidDefense", 1.0f);
		RATE_RAID_ATTACK = ConfigLoader.getFloat("RateRaidAttack", 1.0f);
		RAID_BOSS_P_ATK_MODIFIER = ConfigLoader.getFloat("RaidBossPAtkModifier", 1.0f);
		RAID_BOSS_M_ATK_MODIFIER = ConfigLoader.getFloat("RaidBossMAtkModifier", 1.0f);
		RAID_BOSS_MAX_HP_MODIFIER = ConfigLoader.getFloat("RaidBossMaxHpModifier", 1.0f);
		RAID_BOSS_MAX_MP_MODIFIER = ConfigLoader.getFloat("RaidBossMaxMpModifier", 1.0f);
		RAID_BOSS_P_DEF_MODIFIER = ConfigLoader.getFloat("RaidBossPDefModifier", 1.0f);
		RAID_BOSS_M_DEF_MODIFIER = ConfigLoader.getFloat("RaidBossMDefModifier", 1.0f);

		RETURN_HOME_BOSSES_FROM_PVP = ConfigLoader.getBoolean("ReturnToHomeBossesFromPvPZones", false);
		RETURN_HOME_BOSSES_FROM_TOWN = ConfigLoader.getBoolean("ReturnToHomeBossesFromTownZones", true);
		ALL_MINIONS_RESPAWN_INTERVAL = ConfigLoader.getInt("AllMinionsRespawnInterval", 5);

		QUEST_REQUIRED_FOR_BOSS = ConfigLoader.getBoolean("QuestRequired", true);
		AUTO_LOOT_GRAND = ConfigLoader.getBoolean("AutoLootGrand", false);
		CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC = ConfigLoader.getBoolean("CanAttackFromAnotherZoneToEpic", false);

		QUEEN_ANT_MAX_SAFE_LEVEL = ConfigLoader.getInt("QueenAntMaxSafeLevel", 48);
		ZAKEN_DOOR_CLOSED_DEFAULT = ConfigLoader.getBoolean("ZakenDoorClosedDefault", true);
		ZAKEN_DOOR_OPEN_HOUR = ConfigLoader.getProperty("ZakenDoorOpenHour", "0");
		ZAKEN_DOOR_OPEN_TIME = ConfigLoader.getInt("ZakenDoorOpenTime", 5);
		ZAKEN_MAX_LEVEL_IN_ZONE = ConfigLoader.getInt("ZakenMaxLevelInZone", 80);
		ZAKEN_USE_TELEPORT = ConfigLoader.getBoolean("ZakenUseTeleport", true);
		ORFEN_USE_TELEPORT = ConfigLoader.getBoolean("OrfenUseTeleport", true);
		VALAKAS_LAIR_CAPACITY = ConfigLoader.getInt("ValakasLairCapacity", 500);
		BAIUM_CHECK_QUEST_FOR_AWAKE = ConfigLoader.getBoolean("BaiumCheckQuestForAwake", true);
		BAIUM_UNSPAWN_CUBE = ConfigLoader.getInt("BaiumUnspawnCube", 300);
		FRINTEZZA_MIN_PARTY_IN_CC = ConfigLoader.getInt("FrintezzaMinPartyInCC", 1);
		FRINTEZZA_MAX_PARTY_IN_CC = ConfigLoader.getInt("FrintezzaMaxPartyInCC", 222);
		FRINTEZZA_TOMB_PASS_TIME = ConfigLoader.getInt("FrintezzaTombPassTime", 35);
		QUEEN_ANT_ENABLED = ConfigLoader.getBoolean("QueenAntEnabled", true);
		CORE_ENABLED = ConfigLoader.getBoolean("CoreEnabled", true);
		ZAKEN_ENABLED = ConfigLoader.getBoolean("ZakenEnabled", true);
		SAILREN_ENABLED = ConfigLoader.getBoolean("SailrenEnabled", true);
		SAILREN_ACTIVITY_TIME = ConfigLoader.getInt("SailrenActivityTime", 60);
		SAILREN_ENABLE_SINGLE_PLAYER = ConfigLoader.getBoolean("SailrenEnableSinglePlayer", true);
		SAILREN_MIN_RESPAWN = ConfigLoader.getInt("SailrenMinRespawn", 720);
		SAILREN_MAX_RESPAWN = ConfigLoader.getInt("SailrenMaxRespawn", 2160);
		SAILREN_INTERVAL_OF_MONSTERS = ConfigLoader.getInt("SailrenIntervalOfMonsters", 3);
		ANTHARAS_ENABLED = ConfigLoader.getBoolean("AntharasEnabled", true);
		ANTHARAS_MIN_SLEEP_TIME = ConfigLoader.getInt("AntharasMinSleepTime", 45);
		ANTHARAS_MAX_SLEEP_TIME = ConfigLoader.getInt("AntharasMaxSleepTime", 60);
		VALAKAS_ENABLED = ConfigLoader.getBoolean("ValakasEnabled", true);
		VALAKAS_MIN_SLEEP_TIME = ConfigLoader.getInt("ValakasMinSleepTime", 45);
		VALAKAS_MAX_SLEEP_TIME = ConfigLoader.getInt("ValakasMaxSleepTime", 60);
		BAIUM_ENABLED = ConfigLoader.getBoolean("BaiumEnabled", true);
		VAN_HALTER_ACTIVE_TIME = ConfigLoader.getInt("VanHalterActiveTime", 240);
		VAN_HALTER_TIME_OF_LOCK_UP_DOOR_OF_ALTAR = ConfigLoader.getInt("VanHalterTimeOfLockUpDoorOfAltar", 3);
		VAN_HALTER_INTERVAL_OF_DOOR_OF_ALTER = ConfigLoader.getInt("VanHalterIntervalOfDoorOfAlter", 360);
		VAN_HALTER_APPEARANCE_TIME = ConfigLoader.getInt("VanHalterAppearanceTime", 1);
		VAN_HALTER_FIGHT_TIME = ConfigLoader.getInt("VanHalterFightTime", 240);
		VAN_HALTER_MIN_RESPAWN = ConfigLoader.getInt("VanHalterMinRespawn", 720);
		VAN_HALTER_MAX_RESPAWN = ConfigLoader.getInt("VanHalterMaxRespawn", 2160);
		FRINTEZZA_ENABLED = ConfigLoader.getBoolean("FrintezzaEnabled", true);
		FRINTEZZA_ACTIVE_TIME = ConfigLoader.getInt("FrintezzaActiveTime", 150);
		FRINTEZZA_MIN_RESPAWN = ConfigLoader.getInt("FrintezzaMinRespawn", 2400);
		FRINTEZZA_MAX_RESPAWN = ConfigLoader.getInt("FrintezzaMaxRespawn", 3600);
		FRINTEZZA_MIN_DISTANCE_FOR_ENTRANCE = ConfigLoader.getInt("FrintezzaMinDistanceForEntrance", 300);
		RATE_RAID_BOSS = ConfigLoader.getDouble("RateRaidBoss", 1.0);
		ANTHARAS_RESPAWN_TIME_PATTERN = ConfigLoader.getProperty("AntharasRespawnTimePattern", "");
		VALAKAS_RESPAWN_TIME_PATTERN = ConfigLoader.getProperty("ValakasRespawnTimePattern", "");
		BAIUM_RESPAWN_TIME_PATTERN = ConfigLoader.getProperty("BaiumRespawnTimePattern", "");
		SAILREN_RESPAWN_TIME_PATTERN = ConfigLoader.getProperty("SailrenRespawnTimePattern", "");
		FRINTEZZA_RESPAWN_TIME_PATTERN = ConfigLoader.getProperty("FrintezzaRespawnTimePattern", "");


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
		ALT_OLY_RESTRICTED_ITEMS = getIntList("AltOlyRestrictedItems", List.of());
		ALT_OLY_RESTRICTED_SKILLS = getIntList("AltOlyRestrictedSkills", List.of());
		ALT_OLY_MATCH_HEAL_COUNTS = ConfigLoader.getBoolean("AltOlyMatchHealCounts", false);
		ALT_OLY_SUMMON_DAMAGE_COUNTS = ConfigLoader.getBoolean("AltOlySummonDamageCounts", false);
		OLYMPIAD_ALLOW_BSS = ConfigLoader.getBoolean("OlympiadAllowBSS", false);
		ALT_OLY_SHOW_MONTHLY_WINNERS = ConfigLoader.getBoolean("AltOlyShowMonthlyWinners", true);
		ALY_OLY_LOG_FIGHTS = ConfigLoader.getBoolean("AlyOlyLogFights", true);
		OLYMPIAD_DURATION_TYPE = ConfigLoader.getProperty("OlympiadDurationType", "Month");
		OLYMPIAD_DURATION = ConfigLoader.getInt("OlympiadDuration", 1);
		INCLUDE_SUMMON_DAMAGE = ConfigLoader.getBoolean("IncludeSummonDamage", true);

		// Onda 11: PvP Events (TvT, CTF, DM)
		TVT_AUTO_START = ConfigLoader.getBoolean("TvT.AutoStart", false);
		CTF_AUTO_START = ConfigLoader.getBoolean("CTF.AutoStart", false);
		DM_AUTO_START = ConfigLoader.getBoolean("DeathMatch.AutoStart", false);
		TVT_DELAY_ON_BOOT = ConfigLoader.getInt("TvT.DelayOnBoot", 10);
		CTF_DELAY_ON_BOOT = ConfigLoader.getInt("CTF.DelayOnBoot", 10);
		DM_DELAY_ON_BOOT = ConfigLoader.getInt("DeathMatch.DelayOnBoot", 10);
		TVT_NEXT_DELAY = ConfigLoader.getInt("TvT.NextDelay", 60);
		CTF_NEXT_DELAY = ConfigLoader.getInt("CTF.NextDelay", 60);
		DM_NEXT_DELAY = ConfigLoader.getInt("DeathMatch.NextDelay", 60);

		// TvT Event
		TVT_ENABLED = ConfigLoader.getBoolean("TvTEnabled", true);
		TVT_AURA = ConfigLoader.getBoolean("TvTAura", true);
		TVT_MIN_LEVEL = ConfigLoader.getInt("TvTMinLevel", 1);
		TVT_MAX_LEVEL = ConfigLoader.getInt("TvTMaxLevel", 85);
		TVT_MIN_PLAYERS = ConfigLoader.getInt("TvTMinPlayers", 8);
		TVT_MAX_PLAYERS = ConfigLoader.getInt("TvTMaxPlayers", 60);
		TVT_REWARD_ID = ConfigLoader.getInt("TvTRewardId", 57);
		TVT_REWARD_AMOUNT = ConfigLoader.getInt("TvTRewardAmount", 100000);
		TVT_JOIN_WITH_CURSED_WEAPON = ConfigLoader.getBoolean("TvTJoinWithCursedWeapon", false);
		TVT_ON_START_REMOVE_ALL_EFFECTS = ConfigLoader.getBoolean("TvTOnStartRemoveAllEffects", true);
		TVT_ON_START_UNSUMMON_PET = ConfigLoader.getBoolean("TvTOnStartUnsummonPet", true);
		TVT_CLOSE_COLISEUM_DOORS = ConfigLoader.getBoolean("TvTCloseColiseumDoors", true);
		TVT_PRICE_NO_KILLS = ConfigLoader.getBoolean("TvTPriceNoKills", false);
		TVT_JOIN_TIME = ConfigLoader.getInt("TvTJoinTime", 5);
		TVT_EVENT_TIME = ConfigLoader.getInt("TvTEventTime", 15);
		TVT_ALLOW_INTERFERENCE = ConfigLoader.getBoolean("TvTAllowInterference", false);
		TVT_ALLOW_POTIONS = ConfigLoader.getBoolean("TvTAllowPotions", false);
		TVT_ALLOW_SUMMON = ConfigLoader.getBoolean("TvTAllowSummon", true);
		TVT_REVIVE_RECOVERY = ConfigLoader.getBoolean("TvTReviveRecovery", true);
		TVT_REVIVE_DELAY = ConfigLoader.getInt("TvTReviveDelay", 10);
		TVT_ORIGINAL_POSITION = ConfigLoader.getBoolean("TvTOriginalPosition", false);
		TVT_ALLOW_ENEMY_HEALING = ConfigLoader.getBoolean("TvTAllowEnemyHealing", false);
		TVT_ALLOW_TEAM_CASTING = ConfigLoader.getBoolean("TvTAllowTeamCasting", false);
		TVT_ALLOW_TEAM_ATTACKING = ConfigLoader.getBoolean("TvTAllowTeamAttacking", false);
		TVT_BLUE_TEAM_LOC = ConfigLoader.getProperty("BlueTeamLoc", "150545,46734,-3415");
		TVT_RED_TEAM_LOC = ConfigLoader.getProperty("RedTeamLoc", "148386,46747,-3415");

		// CTF Event
		CTF_ENABLED = ConfigLoader.getBoolean("CTFEnabled", true);
		CTF_AURA = ConfigLoader.getBoolean("CTFAura", true);
		CTF_MIN_LEVEL = ConfigLoader.getInt("CTFMinLevel", 1);
		CTF_MAX_LEVEL = ConfigLoader.getInt("CTFMaxLevel", 85);
		CTF_MIN_PLAYERS = ConfigLoader.getInt("CTFMinPlayers", 8);
		CTF_MAX_PLAYERS = ConfigLoader.getInt("CTFMaxPlayers", 60);
		CTF_REWARD_ID = ConfigLoader.getInt("CTFRewardId", 57);
		CTF_REWARD_AMOUNT = ConfigLoader.getInt("CTFRewardAmount", 100000);
		CTF_IN_INSTANCE = ConfigLoader.getBoolean("CTFInInstance", true);
		CTF_ORIGINAL_POSITION = ConfigLoader.getBoolean("CTFOriginalPosition", false);
		CTF_JOIN_WITH_CURSED_WEAPON = ConfigLoader.getBoolean("CTFJoinWithCursedWeapon", false);
		CTF_CLOSE_COLISEUM_DOORS = ConfigLoader.getBoolean("CTFCloseColiseumDoors", true);
		CTF_JOIN_TIME = ConfigLoader.getInt("CTFJoinTime", 5);
		CTF_EVENT_TIME = ConfigLoader.getInt("CTFEventTime", 15);
		CTF_ALLOW_INTERFERENCE = ConfigLoader.getBoolean("CTFAllowInterference", false);
		CTF_ALLOW_TEAM_CASTING = ConfigLoader.getBoolean("CTFAllowTeamCasting", false);
		CTF_ALLOW_POTIONS = ConfigLoader.getBoolean("CTFAllowPotions", false);
		CTF_ALLOW_SUMMON = ConfigLoader.getBoolean("CTFAllowSummon", true);
		CTF_REVIVE_RECOVERY = ConfigLoader.getBoolean("CTFReviveRecovery", true);
		CTF_BLUE_TEAM_LOC = ConfigLoader.getProperty("BlueTeamLoc", "150545,46734,-3415");
		CTF_RED_TEAM_LOC = ConfigLoader.getProperty("RedTeamLoc", "148386,46747,-3415");
		CTF_BLUE_FLAG_LOC = ConfigLoader.getProperty("BlueFlagLoc", "150545,46734,-3415");
		CTF_RED_FLAG_LOC = ConfigLoader.getProperty("RedFlagLoc", "148386,46747,-3415");

		// DeathMatch Event
		DM_ENABLED = ConfigLoader.getBoolean("DMEnabled", true);
		DM_AURA = ConfigLoader.getBoolean("DmAura", true);
		DM_MIN_LEVEL = ConfigLoader.getInt("MinLevel", 1);
		DM_MAX_LEVEL = ConfigLoader.getInt("MaxLevel", 80);
		DM_MIN_PLAYERS = ConfigLoader.getInt("MinPlayers", 8);
		DM_MAX_PLAYERS = ConfigLoader.getInt("MaxPlayers", 60);
		DM_REWARD_ID = ConfigLoader.getInt("RewardItem", 57);
		DM_REWARD_AMOUNT = ConfigLoader.getInt("RewardItemCount", 50000);
		DM_ORIGINAL_POSITION = ConfigLoader.getBoolean("OriginalPosition", false);
		DM_REG_TIME = ConfigLoader.getInt("RegTime", 5);
		DM_EVENT_TIME = ConfigLoader.getInt("EventTime", 15);
		DM_ON_START_RESTORE_HP_MP_CP = ConfigLoader.getBoolean("OnStartRestoreHpMpCp", true);
		DM_ALLOW_POTION = ConfigLoader.getBoolean("AllowPotion", true);
		DM_ALLOW_SUMMON = ConfigLoader.getBoolean("AllowSummon", true);
		DM_CURSED_WEAPON = ConfigLoader.getBoolean("CursedWeapon", false);
		DM_ALLOW_INTERFERENCE = ConfigLoader.getBoolean("AllowInterference", false);
		DM_RESET_ALL_SKILL = ConfigLoader.getBoolean("ResetAllSkill", false);
		DM_REVIVE_DELAY = ConfigLoader.getInt("ReviveDelay", 10);
		DM_ON_START_REMOVE_ALL_EFFECTS = ConfigLoader.getBoolean("OnStartRemoveAllEffects", true);
		DM_ON_START_UNSUMMON_PET = ConfigLoader.getBoolean("OnStartUnsummonPet", true);
		DM_EVENT_LOCATION = ConfigLoader.getProperty("EventLocation", "149800,46800,-3412");

		ARENA_DUEL_LOC = ConfigLoader.getProperty("ArenaDuelLoc", "149800,46800,-3412");
		TOURNAMENT_1X1_LOC = ConfigLoader.getProperty("Tournament1x1Loc", "149800,46800,-3412");

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

		// Siege towers, artefacts & fortresses (siege.properties)
		CORRECT_DATE_BY_7S = ConfigLoader.getBoolean("CorrectDateBy7s", true);
		CL_SET_SIEGE_TIME_LIST = ConfigLoader.getProperty("CLSetSiegeTimeList", "hour");
		SIEGE_HOUR_LIST = ConfigLoader.getProperty("SiegeHourList", "16,20");
		REWARD_ID = ConfigLoader.getInt("RewardID", 0);
		REWARD_COUNT = ConfigLoader.getInt("RewardCount", 0);
		GLUDIO_CONTROL_TOWER_1 = ConfigLoader.getProperty("GludioControlTower1", "-18133,109474,-2657,13002,8000");
		GLUDIO_CONTROL_TOWER_2 = ConfigLoader.getProperty("GludioControlTower2", "-18137,108583,-2379,13002,20000");
		GLUDIO_CONTROL_TOWER_3 = ConfigLoader.getProperty("GludioControlTower3", "-18061,107294,-2409,13002,8000");
		GLUDIO_CONTROL_TOWER_4 = ConfigLoader.getProperty("GludioControlTower4", "-18359,112879,-2409,13002,8000");
		GLUDIO_ARTEFACT_1 = ConfigLoader.getProperty("GludioArtefact1", "-18120,107984,-2483,16384,35063");
		GLUDIO_FLAME_TOWER_1 = ConfigLoader.getProperty("GludioFlameTower1", "-18154,107591,-2560,13004,70009,71009");
		GLUDIO_FLAME_TOWER_2 = ConfigLoader.getProperty("GludioFlameTower2", "-19329,108154,-2384,13004,70010,71010");
		GIRAN_CONTROL_TOWER_1 = ConfigLoader.getProperty("GiranControlTower1", "118623,145150,-2476,13002,10000");
		GIRAN_CONTROL_TOWER_2 = ConfigLoader.getProperty("GiranControlTower2", "117339,145051,-2446,13002,30000");
		GIRAN_CONTROL_TOWER_3 = ConfigLoader.getProperty("GiranControlTower3", "116116,145016,-2750,13002,10000");
		GIRAN_CONTROL_TOWER_4 = ConfigLoader.getProperty("GiranControlTower4", "113049,144849,-2476,13002,10000");
		GIRAN_ARTEFACT_1 = ConfigLoader.getProperty("GiranArtefact1", "117939,145090,-2550,32768,35147");
		GIRAN_FLAME_TOWER_1 = ConfigLoader.getProperty("GiranFlameTower1", "118331,145055,-2627,13004,70013,71013");
		GIRAN_FLAME_TOWER_2 = ConfigLoader.getProperty("GiranFlameTower2", "117768,143880,-2451,13004,70014,71014");
		DION_CONTROL_TOWER_1 = ConfigLoader.getProperty("DionControlTower1", "22158,161167,-2573,13002,8000");
		DION_CONTROL_TOWER_2 = ConfigLoader.getProperty("DionControlTower2", "22138,159901,-2877,13002,20000");
		DION_CONTROL_TOWER_3 = ConfigLoader.getProperty("DionControlTower3", "22027,162449,-2603,13002,80000");
		DION_CONTROL_TOWER_4 = ConfigLoader.getProperty("DionControlTower4", "22319,156863,-2603,13002,8000");
		DION_ARTEFACT_1 = ConfigLoader.getProperty("DionArtefact1", "22081,161771,-2677,49017,35105");
		DION_FLAME_TOWER_1 = ConfigLoader.getProperty("DionFlameTower1", "22114,162159,-2754,13004,70011,71011");
		DION_FLAME_TOWER_2 = ConfigLoader.getProperty("DionFlameTower2", "23289,161596,-2578,13004,70012,71012");
		OREN_CONTROL_TOWER_1 = ConfigLoader.getProperty("OrenControlTower1", "83416,37164,-2173,13002,10000");
		OREN_CONTROL_TOWER_2 = ConfigLoader.getProperty("OrenControlTower2", "82129,37131,-2477,13002,30000");
		OREN_CONTROL_TOWER_3 = ConfigLoader.getProperty("OrenControlTower3", "84709,37234,-2203,13002,10000");
		OREN_CONTROL_TOWER_4 = ConfigLoader.getProperty("OrenControlTower4", "79103,36942,-2203,13002,10000");
		OREN_ARTEFACT_1 = ConfigLoader.getProperty("OrenArtefact1", "84014,37184,-2277,32768,35189");
		OREN_FLAME_TOWER_1 = ConfigLoader.getProperty("OrenFlameTower1", "84407,37150,-2354,13004,70015,71015");
		OREN_FLAME_TOWER_2 = ConfigLoader.getProperty("OrenFlameTower2", "83844,35975,-2178,13004,70016,71016");
		ADEN_CONTROL_TOWER_1 = ConfigLoader.getProperty("AdenControlTower1", "147455,5624,-911,13002,6000");
		ADEN_CONTROL_TOWER_2 = ConfigLoader.getProperty("AdenControlTower2", "147460,1303,-176,13002,6000");
		ADEN_CONTROL_TOWER_3 = ConfigLoader.getProperty("AdenControlTower3", "146158,6929,-426,13002,6000");
		ADEN_CONTROL_TOWER_4 = ConfigLoader.getProperty("AdenControlTower4", "148755,6930,-426,13002,6000");
		ADEN_CONTROL_TOWER_5 = ConfigLoader.getProperty("AdenControlTower5", "148775,2351,-426,13002,6000");
		ADEN_CONTROL_TOWER_6 = ConfigLoader.getProperty("AdenControlTower6", "146137,2351,-426,13002,6000");
		ADEN_CONTROL_TOWER_7 = ConfigLoader.getProperty("AdenControlTower7", "144954,1603,-494,13002,6000");
		ADEN_CONTROL_TOWER_8 = ConfigLoader.getProperty("AdenControlTower8", "149976,1585,-494,13002,6000");
		ADEN_ARTEFACT_1 = ConfigLoader.getProperty("AdenArtefact1", "147465,1537,-373,16384,35233");
		ADEN_FLAME_TOWER_1 = ConfigLoader.getProperty("AdenFlameTower1", "149976,1583,-450,13004,70001,71001,70002,71002,70003,71003,70004,71004");
		ADEN_FLAME_TOWER_2 = ConfigLoader.getProperty("AdenFlameTower2", "144955,1603,-450,13004,70005,71005,70006,71006,70007,71007,70008,71008");
		INNADRIL_CONTROL_TOWER_1 = ConfigLoader.getProperty("InnadrilControlTower1", "116062,248649,-973,13002,6000");
		INNADRIL_CONTROL_TOWER_2 = ConfigLoader.getProperty("InnadrilControlTower2", "116037,249948,-669,13002,6000");
		INNADRIL_CONTROL_TOWER_3 = ConfigLoader.getProperty("InnadrilControlTower3", "115977,251223,-699,13002,6000");
		INNADRIL_CONTROL_TOWER_4 = ConfigLoader.getProperty("InnadrilControlTower4", "116261,245621,-699,13002,6000");
		INNADRIL_ARTEFACT_1 = ConfigLoader.getProperty("InnadrilArtefact1", "116031,250555,-798,49200,35279");
		INNADRIL_FLAME_TOWER_1 = ConfigLoader.getProperty("InnadrilFlameTower1", "116065,250938,-850,13004,70017,71017");
		INNADRIL_FLAME_TOWER_2 = ConfigLoader.getProperty("InnadrilFlameTower2", "117240,250375,-674,13004,70018,71018");
		GODDARD_CONTROL_TOWER_1 = ConfigLoader.getProperty("GoddardControlTower1", "147456,-46029,-1360,13002,6000");
		GODDARD_CONTROL_TOWER_2 = ConfigLoader.getProperty("GoddardControlTower2", "150183,-48201,-1744,13002,6000");
		GODDARD_CONTROL_TOWER_3 = ConfigLoader.getProperty("GoddardControlTower3", "144741,-48188,-1744,13002,6000");
		GODDARD_CONTROL_TOWER_4 = ConfigLoader.getProperty("GoddardControlTower4", "147477,-48516,-505,13002,6000");
		GODDARD_ARTEFACT_1 = ConfigLoader.getProperty("GoddardArtefact1", "146601,-50439,-1505,32768,35322");
		GODDARD_ARTEFACT_2 = ConfigLoader.getProperty("GoddardArtefact2", "148350,-50457,-1505,0,35323");
		GODDARD_FLAME_TOWER_1 = ConfigLoader.getProperty("GoddardFlameTower1", "148144,-46992,-1609,13004,70019,71019");
		GODDARD_FLAME_TOWER_2 = ConfigLoader.getProperty("GoddardFlameTower2", "146784,-46992,-1609,13004,70020,71020");
		RUNE_CONTROL_TOWER_1 = ConfigLoader.getProperty("RuneControlTower1", "18260,-49161,-571,13002,6000");
		RUNE_CONTROL_TOWER_2 = ConfigLoader.getProperty("RuneControlTower2", "16690,-50330,-641,13002,6000");
		RUNE_CONTROL_TOWER_3 = ConfigLoader.getProperty("RuneControlTower3", "16727,-47952,-641,13002,6000");
		RUNE_CONTROL_TOWER_4 = ConfigLoader.getProperty("RuneControlTower4", "14796,-47041,1027,13002,6000");
		RUNE_CONTROL_TOWER_5 = ConfigLoader.getProperty("RuneControlTower5", "14822,-51282,1027,13002,6000");
		RUNE_CONTROL_TOWER_6 = ConfigLoader.getProperty("RuneControlTower6", "12259,-47510,1295,13002,6000");
		RUNE_ARTEFACT_1 = ConfigLoader.getProperty("RuneArtefact1", "9132,-49152,1094,64270,35469");
		RUNE_FLAME_TOWER_1 = ConfigLoader.getProperty("RuneFlameTower1", "12864,-47440,-1087,13004,70021,71021");
		RUNE_FLAME_TOWER_2 = ConfigLoader.getProperty("RuneFlameTower2", "12225,-50767,1248,13004,70022,71022");
		SCHUTTGART_CONTROL_TOWER_1 = ConfigLoader.getProperty("SchuttgartControlTower1", "77561,-150087,371,13002,6000");
		SCHUTTGART_CONTROL_TOWER_2 = ConfigLoader.getProperty("SchuttgartControlTower2", "80306,-152257,-12,13002,6000");
		SCHUTTGART_CONTROL_TOWER_3 = ConfigLoader.getProperty("SchuttgartControlTower3", "74862,-152162,-12,13002,6000");
		SCHUTTGART_CONTROL_TOWER_4 = ConfigLoader.getProperty("SchuttgartControlTower4", "77568,-152541,1226,13002,6000");
		SCHUTTGART_ARTEFACT_1 = ConfigLoader.getProperty("SchuttgartArtefact1", "76668,-154520,226,0,35515");
		SCHUTTGART_ARTEFACT_2 = ConfigLoader.getProperty("SchuttgartArtefact2", "78446,-154524,227,0,35514");
		SCHUTTGART_FLAME_TOWER_1 = ConfigLoader.getProperty("SchuttgartFlameTower1", "76872,-151043,120,13004,70023,71023");
		SCHUTTGART_FLAME_TOWER_2 = ConfigLoader.getProperty("SchuttgartFlameTower2", "78233,-151037,120,13004,70024,71024");
		FORT_SIEGE_LENGTH = ConfigLoader.getInt("FortSiegeLength", 120);
		FORT_COUNT_DOWN_LENGTH = ConfigLoader.getInt("FortCountDownLength", 10);
		SUSPICIOUS_MERCHANT_RESPAWN_DELAY = ConfigLoader.getInt("SuspiciousMerchantRespawnDelay", 180);
		FORT_MAX_FLAGS = ConfigLoader.getInt("FortMaxFlags", 1);
		FORT_SIEGE_CLAN_MIN_LEVEL = ConfigLoader.getInt("FortSiegeClanMinLevel", 4);
		FORT_ATTACKER_MAX_CLANS = ConfigLoader.getInt("FortAttackerMaxClans", 500);
		FORT_REWARD_ID = ConfigLoader.getInt("FortRewardID", 0);
		FORT_REWARD_COUNT = ConfigLoader.getInt("FortRewardCount", 0);
		COMBAT_FLAG_ID = ConfigLoader.getInt("CombatFlagID", 6718);
		SHANTY_COMMANDER_1 = ConfigLoader.getProperty("ShantyCommander1", "-52435,155188,-1768,20000,35683");
		SHANTY_COMMANDER_2 = ConfigLoader.getProperty("ShantyCommander2", "-52128,157752,-2024,29864,35677");
		SHANTY_COMMANDER_3 = ConfigLoader.getProperty("ShantyCommander3", "-53944,155433,-2024,7304,35680");
		SHANTY_FLAG_1 = ConfigLoader.getProperty("ShantyFlag1", "-53086,156493,-1896");
		SHANTY_FLAG_2 = ConfigLoader.getProperty("ShantyFlag2", "-53054,156605,-1896");
		SHANTY_FLAG_3 = ConfigLoader.getProperty("ShantyFlag3", "-53032,156689,-1896");
		SOUTHERN_COMMANDER_1 = ConfigLoader.getProperty("SouthernCommander1", "-21328,218864,-2952,0,35719");
		SOUTHERN_COMMANDER_2 = ConfigLoader.getProperty("SouthernCommander2", "-22992,218160,-3208,0,35713");
		SOUTHERN_COMMANDER_3 = ConfigLoader.getProperty("SouthernCommander3", "-21520,221504,-3208,45328,35716");
		SOUTHERN_COMMANDER_4 = ConfigLoader.getProperty("SouthernCommander4", "-22728,221746,-3200,33168,35721");
		SOUTHERN_FLAG_1 = ConfigLoader.getProperty("SouthernFlag1", "-22386,219917,-3079");
		SOUTHERN_FLAG_2 = ConfigLoader.getProperty("SouthernFlag2", "-22386,219798,-3079");
		SOUTHERN_FLAG_3 = ConfigLoader.getProperty("SouthernFlag3", "-22386,219679,-3079");
		HIVE_COMMANDER_1 = ConfigLoader.getProperty("HiveCommander1", "15152,188128,-2640,0,35752");
		HIVE_COMMANDER_2 = ConfigLoader.getProperty("HiveCommander2", "17984,187536,-2896,45056,35746");
		HIVE_COMMANDER_3 = ConfigLoader.getProperty("HiveCommander3", "16016,189520,-2888,0,35749");
		HIVE_FLAG_1 = ConfigLoader.getProperty("HiveFlag1", "16685,188358,-2770");
		HIVE_FLAG_2 = ConfigLoader.getProperty("HiveFlag2", "16761,188306,-2770");
		HIVE_FLAG_3 = ConfigLoader.getProperty("HiveFlag3", "16847,188257,-2770");
		VALLEY_COMMANDER_1 = ConfigLoader.getProperty("ValleyCommander1", "124768,121856,-2296,0,35788");
		VALLEY_COMMANDER_2 = ConfigLoader.getProperty("ValleyCommander2", "124299,123614,-2552,49192,35782");
		VALLEY_COMMANDER_3 = ConfigLoader.getProperty("ValleyCommander3", "124768,124640,-2552,54480,35785");
		VALLEY_COMMANDER_4 = ConfigLoader.getProperty("ValleyCommander4", "128048,123344,-2536,35028,35790");
		VALLEY_FLAG_1 = ConfigLoader.getProperty("ValleyFlag1", "125970,123653,-2429");
		VALLEY_FLAG_2 = ConfigLoader.getProperty("ValleyFlag2", "126092,123650,-2429");
		VALLEY_FLAG_3 = ConfigLoader.getProperty("ValleyFlag3", "126205,123648,-2429");
		IVORY_COMMANDER_1 = ConfigLoader.getProperty("IvoryCommander1", "72400,2896,-2760,0,35821");
		IVORY_COMMANDER_2 = ConfigLoader.getProperty("IvoryCommander2", "73788,5479,-3016,55136,35815");
		IVORY_COMMANDER_3 = ConfigLoader.getProperty("IvoryCommander3", "71264,4144,-3008,0,35818");
		IVORY_FLAG_1 = ConfigLoader.getProperty("IvoryFlag1", "72565,4436,-2888");
		IVORY_FLAG_2 = ConfigLoader.getProperty("IvoryFlag2", "72660,4512,-2888");
		IVORY_FLAG_3 = ConfigLoader.getProperty("IvoryFlag3", "72759,4594,-2888");
		NARSELL_COMMANDER_1 = ConfigLoader.getProperty("NarsellCommander1", "154704,53856,-2968,0,35852");
		NARSELL_COMMANDER_2 = ConfigLoader.getProperty("NarsellCommander2", "155576,56592,-3224,59224,35846");
		NARSELL_COMMANDER_3 = ConfigLoader.getProperty("NarsellCommander3", "153328,54848,-3216,5512,35849");
		NARSELL_FLAG_1 = ConfigLoader.getProperty("NarsellFlag1", "154567,55397,-3097");
		NARSELL_FLAG_2 = ConfigLoader.getProperty("NarsellFlag2", "154650,55493,-3097");
		NARSELL_FLAG_3 = ConfigLoader.getProperty("NarsellFlag3", "154715,55587,-3097");
		BAYOU_COMMANDER_1 = ConfigLoader.getProperty("BayouCommander1", "188624,38240,-3128,0,35888");
		BAYOU_COMMANDER_2 = ConfigLoader.getProperty("BayouCommander2", "188160,39920,-3376,49284,35882");
		BAYOU_COMMANDER_3 = ConfigLoader.getProperty("BayouCommander3", "188626,41066,-3376,57140,35885");
		BAYOU_COMMANDER_4 = ConfigLoader.getProperty("BayouCommander4", "191846,39764,-3368,33020,35890");
		BAYOU_FLAG_1 = ConfigLoader.getProperty("BayouFlag1", "189838,40063,-3253");
		BAYOU_FLAG_2 = ConfigLoader.getProperty("BayouFlag2", "189931,40060,-3253");
		BAYOU_FLAG_3 = ConfigLoader.getProperty("BayouFlag3", "190052,40062,-3253");
		WHITE_SANDS_COMMANDER_1 = ConfigLoader.getProperty("WhiteSandsCommander1", "117216,205648,-3048,0,35921");
		WHITE_SANDS_COMMANDER_2 = ConfigLoader.getProperty("WhiteSandsCommander2", "118880,203568,-3304,5396,35915");
		WHITE_SANDS_COMMANDER_3 = ConfigLoader.getProperty("WhiteSandsCommander3", "118560,206560,-3304,48872,35918");
		WHITE_SANDS_FLAG_1 = ConfigLoader.getProperty("WhiteSandsFlag1", "118640,205151,-3176");
		WHITE_SANDS_FLAG_2 = ConfigLoader.getProperty("WhiteSandsFlag2", "118690,205062,-3176");
		WHITE_SANDS_FLAG_3 = ConfigLoader.getProperty("WhiteSandsFlag3", "118742,204968,-3176");
		BORDERLAND_COMMANDER_1 = ConfigLoader.getProperty("BorderlandCommander1", "159664,-72224,-2584,0,35957");
		BORDERLAND_COMMANDER_2 = ConfigLoader.getProperty("BorderlandCommander2", "157968,-71659,-2832,59020,35951");
		BORDERLAND_COMMANDER_3 = ConfigLoader.getProperty("BorderlandCommander3", "157312,-70640,-2832,0,35954");
		BORDERLAND_COMMANDER_4 = ConfigLoader.getProperty("BorderlandCommander4", "160194,-68688,-2824,43272,35959");
		BORDERLAND_FLAG_1 = ConfigLoader.getProperty("BorderlandFlag1", "158817,-70229,-2708");
		BORDERLAND_FLAG_2 = ConfigLoader.getProperty("BorderlandFlag2", "158883,-70145,-2708");
		BORDERLAND_FLAG_3 = ConfigLoader.getProperty("BorderlandFlag3", "158946,-70045,-2708");
		SWAMP_COMMANDER_1 = ConfigLoader.getProperty("SwampCommander1", "71264,-60512,-2504,0,35995");
		SWAMP_COMMANDER_2 = ConfigLoader.getProperty("SwampCommander2", "71248,-62352,-2752,12388,35989");
		SWAMP_COMMANDER_3 = ConfigLoader.getProperty("SwampCommander3", "68688,-59648,-2752,56012,35992");
		SWAMP_COMMANDER_4 = ConfigLoader.getProperty("SwampCommander4", "68005,-60866,-2744,5424,35997");
		SWAMP_FLAG_1 = ConfigLoader.getProperty("SwampFlag1", "69829,-61087,-2629");
		SWAMP_FLAG_2 = ConfigLoader.getProperty("SwampFlag2", "69979,-61144,-2632");
		SWAMP_FLAG_3 = ConfigLoader.getProperty("SwampFlag3", "70069,-61182,-2629");
		ARCHAIC_COMMANDER_1 = ConfigLoader.getProperty("ArchaicCommander1", "109856,-142640,-2672,0,36028");
		ARCHAIC_COMMANDER_2 = ConfigLoader.getProperty("ArchaicCommander2", "109600,-139735,-2928,62612,36022");
		ARCHAIC_COMMANDER_3 = ConfigLoader.getProperty("ArchaicCommander3", "108223,-142209,-2920,8524,36025");
		ARCHAIC_FLAG_1 = ConfigLoader.getProperty("ArchaicFlag1", "109142,-141243,-2801");
		ARCHAIC_FLAG_2 = ConfigLoader.getProperty("ArchaicFlag2", "109184,-141129,-2801");
		ARCHAIC_FLAG_3 = ConfigLoader.getProperty("ArchaicFlag3", "109214,-141016,-2801");
		FLORAN_COMMANDER_1 = ConfigLoader.getProperty("FloranCommander1", "6528,151872,-2608,0,36064");
		FLORAN_COMMANDER_2 = ConfigLoader.getProperty("FloranCommander2", "7006,148242,-2856,32768,36058");
		FLORAN_COMMANDER_3 = ConfigLoader.getProperty("FloranCommander3", "4384,150992,-2856,0,36061");
		FLORAN_COMMANDER_4 = ConfigLoader.getProperty("FloranCommander4", "5246,152319,-2848,49151,36066");
		FLORAN_FLAG_1 = ConfigLoader.getProperty("FloranFlag1", "5293,149624,-2732");
		FLORAN_FLAG_2 = ConfigLoader.getProperty("FloranFlag2", "5306,149743,-2732");
		FLORAN_FLAG_3 = ConfigLoader.getProperty("FloranFlag3", "5299,149870,-2732");
		CLOUD_MOUNTAIN_COMMANDER_1 = ConfigLoader.getProperty("CloudMountainCommander1", "-55248,90496,-2536,0,36102");
		CLOUD_MOUNTAIN_COMMANDER_2 = ConfigLoader.getProperty("CloudMountainCommander2", "-55791,91856,-2792,0,36096");
		CLOUD_MOUNTAIN_COMMANDER_3 = ConfigLoader.getProperty("CloudMountainCommander3", "-54168,92604,-2784,49196,36099");
		CLOUD_MOUNTAIN_COMMANDER_4 = ConfigLoader.getProperty("CloudMountainCommander4", "-50913,92259,-2776,41188,36104");
		CLOUD_MOUNTAIN_FLAG_1 = ConfigLoader.getProperty("CloudMountainFlag1", "-53354,91537,-2664");
		CLOUD_MOUNTAIN_FLAG_2 = ConfigLoader.getProperty("CloudMountainFlag2", "-53237,91537,-2664");
		CLOUD_MOUNTAIN_FLAG_3 = ConfigLoader.getProperty("CloudMountainFlag3", "-53112,91537,-2664");
		TANOR_COMMANDER_1 = ConfigLoader.getProperty("TanorCommander1", "58480,139648,-1464,0,36135");
		TANOR_COMMANDER_2 = ConfigLoader.getProperty("TanorCommander2", "61864,139257,-1728,46896,36129");
		TANOR_COMMANDER_3 = ConfigLoader.getProperty("TanorCommander3", "59436,140834,-1720,47296,36132");
		TANOR_FLAG_1 = ConfigLoader.getProperty("TanorFlag1", "60225,139771,-1597");
		TANOR_FLAG_2 = ConfigLoader.getProperty("TanorFlag2", "60362,139742,-1597");
		TANOR_FLAG_3 = ConfigLoader.getProperty("TanorFlag3", "60467,139727,-1597");
		DRAGONSPINE_COMMANDER_1 = ConfigLoader.getProperty("DragonspineCommander1", "13184,94928,-3144,0,36166");
		DRAGONSPINE_COMMANDER_2 = ConfigLoader.getProperty("DragonspineCommander2", "9472,94992,-3392,0,36160");
		DRAGONSPINE_COMMANDER_3 = ConfigLoader.getProperty("DragonspineCommander3", "12829,96214,-3392,49152,36163");
		DRAGONSPINE_FLAG_1 = ConfigLoader.getProperty("DragonspineFlag1", "11459,95308,-3264");
		DRAGONSPINE_FLAG_2 = ConfigLoader.getProperty("DragonspineFlag2", "11527,95301,-3264");
		DRAGONSPINE_FLAG_3 = ConfigLoader.getProperty("DragonspineFlag3", "11623,95311,-3264");
		ANTHARAS_COMMANDER_1 = ConfigLoader.getProperty("AntharasCommander1", "79440,88752,-2600,0,36202");
		ANTHARAS_COMMANDER_2 = ConfigLoader.getProperty("AntharasCommander2", "77262,91704,-2856,5112,36196");
		ANTHARAS_COMMANDER_3 = ConfigLoader.getProperty("AntharasCommander3", "80929,90510,-2856,40192,36199");
		ANTHARAS_COMMANDER_4 = ConfigLoader.getProperty("AntharasCommander4", "80755,89002,-2848,21984,36204");
		ANTHARAS_FLAG_1 = ConfigLoader.getProperty("AntharasFlag1", "79470,91299,-2728");
		ANTHARAS_FLAG_2 = ConfigLoader.getProperty("AntharasFlag2", "79528,91187,-2728");
		ANTHARAS_FLAG_3 = ConfigLoader.getProperty("AntharasFlag3", "79580,91095,-2728");
		WESTERN_COMMANDER_1 = ConfigLoader.getProperty("WesternCommander1", "113481,-16058,-712,0,36240");
		WESTERN_COMMANDER_2 = ConfigLoader.getProperty("WesternCommander2", "109872,-16624,-968,16384,36234");
		WESTERN_COMMANDER_3 = ConfigLoader.getProperty("WesternCommander3", "112601,-13933,-960,49152,36237");
		WESTERN_COMMANDER_4 = ConfigLoader.getProperty("WesternCommander4", "113929,-14801,-960,32768,36242");
		WESTERN_FLAG_1 = ConfigLoader.getProperty("WesternFlag1", "111280,-14820,-839");
		WESTERN_FLAG_2 = ConfigLoader.getProperty("WesternFlag2", "111380,-14820,-839");
		WESTERN_FLAG_3 = ConfigLoader.getProperty("WesternFlag3", "111480,-14820,-839");
		HUNTERS_COMMANDER_1 = ConfigLoader.getProperty("HuntersCommander1", "123232,94400,-1856,0,36278");
		HUNTERS_COMMANDER_2 = ConfigLoader.getProperty("HuntersCommander2", "122688,95760,-2112,0,36272");
		HUNTERS_COMMANDER_3 = ConfigLoader.getProperty("HuntersCommander3", "124305,96528,-2104,49151,36275");
		HUNTERS_COMMANDER_4 = ConfigLoader.getProperty("HuntersCommander4", "127632,96240,-2096,40892,36280");
		HUNTERS_FLAG_1 = ConfigLoader.getProperty("HuntersFlag1", "125155,95455,-1984");
		HUNTERS_FLAG_2 = ConfigLoader.getProperty("HuntersFlag2", "125255,95455,-1984");
		HUNTERS_FLAG_3 = ConfigLoader.getProperty("HuntersFlag3", "125355,95455,-1984");
		AARU_COMMANDER_1 = ConfigLoader.getProperty("AaruCommander1", "74288,186912,-2296,0,36311");
		AARU_COMMANDER_2 = ConfigLoader.getProperty("AaruCommander2", "71392,184720,-2552,5528,36305");
		AARU_COMMANDER_3 = ConfigLoader.getProperty("AaruCommander3", "71542,186410,-2552,55088,36308");
		AARU_FLAG_1 = ConfigLoader.getProperty("AaruFlag1", "73029,186303,-2424");
		AARU_FLAG_2 = ConfigLoader.getProperty("AaruFlag2", "73923,186247,-2424");
		AARU_FLAG_3 = ConfigLoader.getProperty("AaruFlag3", "72833,186178,-2424");
		DEMON_COMMANDER_1 = ConfigLoader.getProperty("DemonCommander1", "100752,-53664,-360,0,36347");
		DEMON_COMMANDER_2 = ConfigLoader.getProperty("DemonCommander2", "100688,-57440,-616,16384,36341");
		DEMON_COMMANDER_3 = ConfigLoader.getProperty("DemonCommander3", "99484,-54027,-616,0,36344");
		DEMON_FLAG_1 = ConfigLoader.getProperty("DemonFlag1", "100400,-55401,-488");
		DEMON_FLAG_2 = ConfigLoader.getProperty("DemonFlag2", "100400,-55301,-488");
		DEMON_FLAG_3 = ConfigLoader.getProperty("DemonFlag3", "100400,-55201,-488");
		MONASTIC_COMMANDER_1 = ConfigLoader.getProperty("MonasticCommander1", "73680,-95456,-1144,0,36385");
		MONASTIC_COMMANDER_2 = ConfigLoader.getProperty("MonasticCommander2", "70189,-93935,-1400,61576,36379");
		MONASTIC_COMMANDER_3 = ConfigLoader.getProperty("MonasticCommander3", "73831,-94119,-1400,45536,36382");
		MONASTIC_FLAG_1 = ConfigLoader.getProperty("MonasticFlag1", "72174,-94437,-1271");
		MONASTIC_FLAG_2 = ConfigLoader.getProperty("MonasticFlag2", "72294,-94481,-1271");
		MONASTIC_FLAG_3 = ConfigLoader.getProperty("MonasticFlag3", "72401,-94526,-1271");

		// Onda 13
		ENABLE_AIO_SYSTEM = ConfigLoader.getBoolean("EnableAioSystem", true);
		ENABLE_AIO_DELEVEL = ConfigLoader.getBoolean("EnableAioDelevel", false);
		AIO_SET_DELEVEL = ConfigLoader.getInt("AioSetDelevel", 1);
		ALLOW_AIO_SPEAK_NPC = ConfigLoader.getBoolean("AllowAioSpeakNpc", false);
		ALLOW_AIO_LEAVE_TOWN = ConfigLoader.getBoolean("AllowAioLeaveTown", false);
		ALLOW_AIO_TELEPORT = ConfigLoader.getBoolean("AllowAioTeleport", false);
		ALLOW_AIO_NAME_COLOR = ConfigLoader.getBoolean("AllowAioNameColor", true);
		AIO_NAME_COLOR = ConfigLoader.getProperty("AioNameColor", "88AA88");
		ALLOW_AIO_TITLE_COLOR = ConfigLoader.getBoolean("AllowAioTitleColor", true);
		AIO_TITLE_COLOR = ConfigLoader.getProperty("AioTitleColor", "88AA88");
		ALLOW_AIO_DUAL = ConfigLoader.getBoolean("AllowAIODual", true);
		AIO_DIAS = ConfigLoader.getInt("AioDias", 30);
		AIO_DIAS_2 = ConfigLoader.getInt("AioDias2", 60);
		AIO_DIAS_3 = ConfigLoader.getInt("AioDias3", 90);
		AIO_ALLOWED_CLASS_IDS = getIntList("AllowedClassId", List.of(10, 25, 38));
		AIO_CLASSES_NAME = ConfigLoader.getProperty("AioClassesName", "Human Mystic, Elf Mystic or Dark Elf Mystic");
		AIO_ITEM_ID = ConfigLoader.getInt("AioItemId", 9225);
		AIO_ITEM_COUNT = ConfigLoader.getInt("AioItemCount", 15);
		AIO_ITEM_COUNT_2 = ConfigLoader.getInt("AioItemCount2", 30);
		AIO_ITEM_COUNT_3 = ConfigLoader.getInt("AioItemCount3", 45);
		AIO_COIN_TEXT = ConfigLoader.getProperty("AioCoinText", "Donate Coin");
		BUFF_SHOP_ENABLE = ConfigLoader.getBoolean("BuffShopEnable", true);
		BUFF_SHOP_MAX_DAYS = ConfigLoader.getInt("BuffShopMaxDays", 14);
		ALLOW_OFFLINE_BUFF = ConfigLoader.getBoolean("AllowOfflineBuff", true);
		DEFAULT_BUFF_SHOP_SLOTS = ConfigLoader.getInt("DefaultBuffShopSlots", 24);
		BUFF_SHOP_NAME_COLOR = ConfigLoader.getProperty("BuffShopNameColor", "808080");
		BUFF_SHOP_TITLE_COLOR = ConfigLoader.getProperty("BuffShopTitleColor", "00DD00");
		BUFF_SHOP_EFFECT = ConfigLoader.getInt("BuffShopEffect", 80);
		AIO_SKILLS = ConfigLoader.getProperty("AioSkills", "");
		VALID_BUFF_SHOP_SKILLS = ConfigLoader.getProperty("ValidBuffShopSkills", "");

		// Onda 14
		ALLOW_VIP_NAME_COLOR = ConfigLoader.getBoolean("AllowVipNameColor", true);
		VIP_NAME_COLOR = ConfigLoader.getProperty("VipNameColor", "0088FF");
		ALLOW_VIP_TITLE_COLOR = ConfigLoader.getBoolean("AllowVipTitleColor", true);
		VIP_TITLE_COLOR = ConfigLoader.getProperty("VipTitleColor", "0088FF");
		VIP_DIAS = ConfigLoader.getInt("VipDias", 30);
		VIP_DIAS_2 = ConfigLoader.getInt("VipDias2", 60);
		VIP_DIAS_3 = ConfigLoader.getInt("VipDias3", 90);
		CLAN_SKILL_BY_ITEM = ConfigLoader.getInt("ClanSkillByItem", 0);
		CLAN_SKILL_ID = getIntList("ClanSkillID", List.of(0, 0));
		RAID_BOSS_INFO_PAGE_LIMIT = ConfigLoader.getInt("RaidBossInfoPageLimit", 12);
		RAID_BOSS_DROP_PAGE_LIMIT = ConfigLoader.getInt("RaidBossDropPageLimit", 12);
		RAID_BOSS_DATE_FORMAT = ConfigLoader.getProperty("RaidBossDateFormat", "(MMM dd, HH:mm)");
		RAID_BOSS_IDS = ConfigLoader.getProperty("RaidBossIds", "29019,29020,29022,29028,29045,29062,29065,29099,29001,29006,29014,25325,25299,25309,25514,25302,25312,25527,25305,25315,25487,25490");
		DAY_TO_SIEGE = ConfigLoader.getInt("DayToSiege", 14);
		HOUR_TO_SIEGE = ConfigLoader.getInt("HourToSiege", 14);
		CUSTOM_STARTER_ITEMS_ENABLED = ConfigLoader.getBoolean("CustomStarterItemsEnabled", false);
		STARTING_CUSTOM_ITEMS_FIGHTER = ConfigLoader.getProperty("StartingCustomItemsFighter", "57,1000;");
		STARTING_CUSTOM_ITEMS_MAGE = ConfigLoader.getProperty("StartingCustomItemsMage", "57,1000;");
		AUTO_RESTART_ENABLED = ConfigLoader.getBoolean("AutoRestartEnabled", false);
		AUTO_RESTART_TIME = ConfigLoader.getProperty("AutoRestartTime", "05:00");
		AUTO_RESTART_COUNTDOWN = ConfigLoader.getInt("AutoRestartCountdown", 300);
		SERVER_GM_ONLY = ConfigLoader.getBoolean("ServerGMOnly", false);
		ANNOUNCE_HERO_LOGIN = ConfigLoader.getBoolean("AnnounceHeroLogin", false);
		ANNOUNCE_AIOX_LOGIN = ConfigLoader.getBoolean("AnnounceAIOxLogin", false);
		ANNOUNCE_VIP_LOGIN = ConfigLoader.getBoolean("AnnounceVIPLogin", false);
		ANNOUNCE_LORD_LOGIN = ConfigLoader.getBoolean("AnnounceLordLogin", false);
		ANNOUNCE_NEWBIE_LOGIN = ConfigLoader.getBoolean("AnnounceNewbieLogin", false);
		API_KEY_TOPZONE = ConfigLoader.getProperty("ApiKeyTOPZONE", "e2ec0d41791613092ac03b6243ec6b87");
		SERVER_ID_KEY_TOPZONE = ConfigLoader.getProperty("ServerIdKeyTOPZONE", "14093");
		API_KEY_HOPZONE = ConfigLoader.getProperty("ApiKeyHOPZONE", "0vocH6Te6bpQ89H8");
		SERVER_ID_NETWORK = ConfigLoader.getProperty("ServerIdNETWORK", "l2nightmare");
		VOTE_SYSTEM_REWARD_ID = ConfigLoader.getInt("VoteSystemRewardId", 3470);
		VOTE_SYSTEM_REWARD_COUNT = ConfigLoader.getInt("VoteSystemRewardCount", 5);

		// Onda 15
		CANCEL_AUGMENTATION_EFFECT = ConfigLoader.getBoolean("CancelAugumentionEffect", true);
		ALT_DANCE_MP_CONSUME = ConfigLoader.getBoolean("AltDanceMpConsume", true);
		ALT_BUFFER_TIME = ConfigLoader.getInt("AltBufferTime", 100);
		ALT_5MIN_TIME = ConfigLoader.getInt("Alt5MinTime", 100);
		ALT_DANCE_TIME = ConfigLoader.getInt("AltDanceTime", 100);
		ALT_SONG_TIME = ConfigLoader.getInt("AltSongTime", 100);
		ALT_HERO_TIME = ConfigLoader.getInt("AltHeroTime", 100);
		ALT_CH_TIME = ConfigLoader.getInt("AltChTime", 1);
		ENABLE_MODIFY_SKILL_DURATION = ConfigLoader.getBoolean("EnableModifySkillDuration", true);
		String skillDur = ConfigLoader.getProperty("SkillDurationList", "");
		SKILL_DURATION_LIST = parseSkillDurationList(skillDur);
		MAX_BUFF_AMOUNT = ConfigLoader.getInt("MaxBuffAmount", 50);
		CANCEL_LESSER_EFFECT = ConfigLoader.getBoolean("CancelLesserEffect", true);
		STORE_SKILL_COOLTIME = ConfigLoader.getBoolean("StoreSkillCooltime", true);
		GRADE_PENALTY = ConfigLoader.getBoolean("GradePenalty", true);
		ALT_GAME_CANCEL_BY_HIT = ConfigLoader.getProperty("AltGameCancelByHit", "all");
		ALT_SHIELD_BLOCKS = ConfigLoader.getBoolean("AltShieldBlocks", false);
		ALT_PERFECT_SHIELD_BLOCK_RATE = ConfigLoader.getInt("AltPerfectShieldBlockRate", 5);
		CONSUME_ON_SUCCESS = ConfigLoader.getBoolean("ConsumeOnSuccess", true);
		ENABLE_STATIC_REUSE = ConfigLoader.getBoolean("EnableSaticReuse", false);
		OLY_USE_STATIC_REUSE = ConfigLoader.getBoolean("OlyUseStaticReuse", false);
		SKILL_REUSE_DELAY = ConfigLoader.getInt("SkillReuseDelay", 70);
		USE_LEVEL_PENALTY = ConfigLoader.getBoolean("UseLevelPenalty", true);
		M_CRIT_RATE = ConfigLoader.getInt("MCritRate", 2);
		DISABLE_SKILLS_ON_LEVEL_LOST = ConfigLoader.getBoolean("DisableSkillsOnLevelLost", false);
		USE_CHAR_LEVEL_MODIFIER = ConfigLoader.getBoolean("UseCharLevelModifier", true);
		CANCEL_MODE = ConfigLoader.getProperty("CancelMode", "new");
		JAIL_IS_PVP_ZONE = ConfigLoader.getBoolean("JailIsPvpZone", false);
		String forbidden = ConfigLoader.getProperty("ForbiddenNames", "admin,gm,gamemaster,annoucements");
		FORBIDDEN_NAMES = Arrays.stream(forbidden.split("[,;\\s]+"))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();

		GRIDS_ALWAYS_ON = ConfigLoader.getBoolean("GridsAlwaysOn", false);
		GRID_NEIGHBOR_TURN_ON_TIME = ConfigLoader.getInt("GridNeighborTurnOnTime", 1);
		GRID_NEIGHBOR_TURN_OFF_TIME = ConfigLoader.getInt("GridNeighborTurnOffTime", 90);
		DESTROY_EQUIPABLE_ITEM = ConfigLoader.getBoolean("DestroyEquipableItem", false);
		AUTO_DESTROY_HERB_TIME = ConfigLoader.getInt("AutoDestroyHerbTime", 60);
		LIST_OF_PROTECTED_ITEMS = getIntList("ListOfProtectedItems", List.of(57, 5575, 6673));
		SAVE_DROPPED_ITEM = ConfigLoader.getBoolean("SaveDroppedItem", false);
		EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = ConfigLoader.getBoolean("EmptyDroppedItemTableAfterLoad", false);
		SAVE_DROPPED_ITEM_INTERVAL = ConfigLoader.getInt("SaveDroppedItemInterval", 60);
		CLEAR_DROPPED_ITEM_TABLE = ConfigLoader.getBoolean("ClearDroppedItemTable", false);
		LOAD_AUTO_ANNOUNCE_AT_STARTUP = ConfigLoader.getBoolean("LoadAutoAnnounceAtStartup", false);
		FAIL_FAKE_DEATH = ConfigLoader.getBoolean("FailFakeDeath", false);
		ALT_MINIMUM_FALL_HEIGHT = ConfigLoader.getInt("AltMinimumFallHeight", 400);
		ALT_ATTACK_DELAY = ConfigLoader.getDouble("AltAttackDelay", 1.0);
		BUFFER_HATE = ConfigLoader.getInt("BufferHate", 1);
		UNAFFECTED_SKILLS = ConfigLoader.getProperty("UnaffectedSkills", "0");
		ALLOWED_SKILLS = ConfigLoader.getProperty("AllowedSkills", "0");

		// Onda 16
		ARENA_DUEL_ENABLE = ConfigLoader.getBoolean("ArenaDuelEnable", true);
		ARENA_DUEL_CHECK_INTERVAL = ConfigLoader.getInt("ArenaDuelBattleCheckInterval", 15);
		ARENA_DUEL_CALL_INTERVAL = ConfigLoader.getInt("ArenaDuelBattleCallInterval", 60);
		ARENA_DUEL_WAIT_INTERVAL = ConfigLoader.getInt("ArenaDuelBattleWaitInterval", 20);
		ARENA_DUEL_ITEMS_RESTRICTION = getIntList("ArenaDuelItemsRestriction", List.of(1538, 5858));
		ARENA_ALLOW_S = ConfigLoader.getBoolean("ArenaAllowS", true);
		ARENA_DUEL_REWARD = ConfigLoader.getProperty("ArenaDuelReward", "3470,5");
		TOURNAMENT_1X1_ENABLE = ConfigLoader.getBoolean("Tournament1x1Enable", false);
		TOURNAMENT_CHECK_INTERVAL = ConfigLoader.getInt("TournamentBattleCheckInterval", 15);
		TOURNAMENT_CALL_INTERVAL = ConfigLoader.getInt("TournamentBattleCallInterval", 60);
		TOURNAMENT_WAIT_INTERVAL = ConfigLoader.getInt("TournamentBattleWaitInterval", 20);
		TOURNAMENT_ITEMS_RESTRICTION = getIntList("TournamentItemsRestriction", List.of(1538, 5858));
		TOURNAMENT_1X1_REWARD = ConfigLoader.getProperty("Tournament1x1Reward", "3470,5");
		TOURNAMENT_1X1_HWID_BLOCK = ConfigLoader.getBoolean("Tournament1x1HwidBlock", false);

		// Onda 17
		GM_STARTUP_INVISIBLE = ConfigLoader.getBoolean("GMStartupInvisible", false);
		GM_STARTUP_INVULNERABLE = ConfigLoader.getBoolean("GMStartupInvulnerable", false);
		GM_STARTUP_SILENCE = ConfigLoader.getBoolean("GMStartupSilence", false);
		GM_STARTUP_AUTO_LIST = ConfigLoader.getBoolean("GMStartupAutoList", false);
		SHOW_GM_LOGIN = ConfigLoader.getBoolean("ShowGMLogin", false);
		EVERYONE_HAS_ADMIN_RIGHTS = ConfigLoader.getBoolean("EveryoneHasAdminRights", false);
		GM_ITEM_RESTRICTION = ConfigLoader.getBoolean("GmItemRestriction", false);
		GM_MAX_ENCHANT = ConfigLoader.getInt("GMMaxEnchant", 65535);
		STANDARD_RESPAWN_DELAY = ConfigLoader.getInt("StandardRespawnDelay", 60);
		GM_AUDIT = ConfigLoader.getBoolean("GMAudit", true);
		SHOW_HTML_CHAT = ConfigLoader.getBoolean("ShowHTMLChat", true);
		GM_NAME_COLOR = ConfigLoader.getProperty("GmNameColor", "FF9900");
		GM_TITLE_COLOR = ConfigLoader.getProperty("GmTitleColor", "0099FF");
		String suRaw = ConfigLoader.getProperty("SuperUsers", "Cristiano,cristiano,admin,Admin,gm,GM,root");
		SUPERUSERS = Arrays.stream(suRaw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());

		// Onda 18
		FREE_TELEPORTING = ConfigLoader.getBoolean("FreeTeleporting", false);
		FREE_TELEPORTING_MIN_LVL = ConfigLoader.getInt("FreeTeleportingMinLvL", 1);
		FREE_TELEPORTING_MAX_LVL = ConfigLoader.getInt("FreeTeleportingMaxLvL", 99);
		NOBLE_PASS_FREE_TP = ConfigLoader.getBoolean("NoblePassFreeTp", false);
		NOBLE_PASS_FREE_TP_MIN_LVL = ConfigLoader.getInt("NoblePassFreeTpMinLvL", 1);
		NOBLE_PASS_FREE_TP_MAX_LVL = ConfigLoader.getInt("NoblePassFreeTpMaxLvL", 99);
		CLASS_MASTER = ConfigLoader.getBoolean("ClassMaster", true);
		ALT_CLASS_MASTER = ConfigLoader.getBoolean("AltClassMaster", true);
		CLASS_MASTER_UPDATE_STRIDER = ConfigLoader.getBoolean("ClassMasterUpdateStrider", false);
		CLASS_MASTER_ENTIRE_TREE = ConfigLoader.getBoolean("ClassMasterEntireTree", false);
		ALLOW_RENT_PET = ConfigLoader.getBoolean("AllowRentPet", false);
		ALLOW_WYVERN_UPGRADER = ConfigLoader.getBoolean("AllowWyvernUpgrader", false);
		ALT_MOB_AGGRO_IN_PEACE_ZONE = ConfigLoader.getBoolean("AltMobAggroInPeaceZone", true);
		ALT_ATTACKABLE_NPCS = ConfigLoader.getBoolean("AltAttackableNpcs", false);
		ALLOW_PET_WALKER = ConfigLoader.getBoolean("AllowPetWalker", true);
		MANAGER_CRYSTAL_COUNT = ConfigLoader.getInt("ManagerCrystalCount", 25);
		ALLOW_LETHAL_PROTECTION_MOBS = ConfigLoader.getBoolean("AllowLethalProtectionMobs", false);
		LETHAL_PROTECTED_MOBS = getIntList("LethalProtectedMobs", List.of(35062));

		// Onda 19 (fun_events.properties)
		ALT_CASTLE_FOR_DAWN = ConfigLoader.getBoolean("AltCastleForDawn", true);
		ALT_CASTLE_FOR_DUSK = ConfigLoader.getBoolean("AltCastleForDusk", true);
		ALT_REQUIRE_CLAN_CASTLE = ConfigLoader.getBoolean("AltRequireClanCastle", false);
		ALT_JOIN_DAWN_COST = ConfigLoader.getInt("AltJoinDawnCost", 50000);
		ANNOUNCE_MAMMON_SPAWN = ConfigLoader.getBoolean("AnnounceMammonSpawn", false);
		STRICT_SEVEN_SIGNS = ConfigLoader.getBoolean("StrictSevenSigns", true);
		ANNOUNCE_7S = ConfigLoader.getBoolean("Announce7s", true);
		ALT_FESTIVAL_MIN_PLAYER = ConfigLoader.getInt("AltFestivalMinPlayer", 5);
		ALT_MAX_PLAYER_CONTRIB = ConfigLoader.getInt("AltMaxPlayerContrib", 1000000);
		ALT_FESTIVAL_MANAGER_START = ConfigLoader.getInt("AltFestivalManagerStart", 120000);
		ALT_FESTIVAL_LENGTH = ConfigLoader.getInt("AltFestivalLength", 1080000);
		ALT_FESTIVAL_CYCLE_LENGTH = ConfigLoader.getInt("AltFestivalCycleLength", 2280000);
		ALT_DAWN_GATES_PDEF_MULT = ConfigLoader.getDouble("AltDawnGatesPdefMult", 1.1);
		ALT_DUSK_GATES_PDEF_MULT = ConfigLoader.getDouble("AltDuskGatesPdefMult", 0.8);
		ALT_DAWN_GATES_MDEF_MULT = ConfigLoader.getDouble("AltDawnGatesMdefMult", 1.1);
		ALT_DUSK_GATES_MDEF_MULT = ConfigLoader.getDouble("AltDuskGatesMdefMult", 0.8);
		PC_CAFFE_ENABLED = ConfigLoader.getBoolean("PCCaffeEnabled", false);
		PC_CAFE_INTERVAL = ConfigLoader.getInt("PCCafeInterval", 10);
		PC_CAFE_MIN_LEVEL = ConfigLoader.getInt("PCCafeMinLevel", 20);
		PC_CAFE_MAX_LEVEL = ConfigLoader.getInt("PCCafeMaxLevel", 80);
		PC_CAFE_MIN_SCORE = ConfigLoader.getInt("PCCafeMinScore", 0);
		PC_CAFE_MAX_SCORE = ConfigLoader.getInt("PCCafeMaxScore", 10);
		ALT_LOTTERY_PRIZE = ConfigLoader.getInt("AltLotteryPrize", 50000);
		ALT_LOTTERY_TICKET_PRICE = ConfigLoader.getInt("AltLotteryTicketPrice", 2000);
		ALT_LOTTERY_5_NUMBER_RATE = ConfigLoader.getDouble("AltLottery5NumberRate", 0.6);
		ALT_LOTTERY_4_NUMBER_RATE = ConfigLoader.getDouble("AltLottery4NumberRate", 0.2);
		ALT_LOTTERY_3_NUMBER_RATE = ConfigLoader.getDouble("AltLottery3NumberRate", 0.2);
		ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = ConfigLoader.getInt("AltLottery2and1NumberPrize", 200);
		CHAMPION_ENABLE = ConfigLoader.getBoolean("ChampionEnable", false);
		CHAMPION_FREQUENCY = ConfigLoader.getInt("ChampionFrequency", 0);
		CHAMPION_PASSIVE = ConfigLoader.getBoolean("ChampionPassive", false);
		CHAMPION_TITLE = ConfigLoader.getProperty("ChampionTitle", "Champion");
		CHAMPION_HP = ConfigLoader.getInt("ChampionHp", 7);
		CHAMPION_HP_REGEN = ConfigLoader.getDouble("ChampionHpRegen", 1.0);
		CHAMPION_ATK = ConfigLoader.getDouble("ChampionAtk", 1.0);
		CHAMPION_SPD_ATK = ConfigLoader.getDouble("ChampionSpdAtk", 1.0);
		CHAMPION_REWARDS = ConfigLoader.getInt("ChampionRewards", 8);
		CHAMPION_ADENAS_REWARDS = ConfigLoader.getInt("ChampionAdenasRewards", 1);
		CHAMPION_EXP_SP = ConfigLoader.getInt("ChampionExpSp", 8);
		CHAMPION_BOSS = ConfigLoader.getBoolean("ChampionBoss", false);
		CHAMPION_MINIONS = ConfigLoader.getBoolean("ChampionMinions", false);
		CHAMPION_MIN_LVL = ConfigLoader.getInt("ChampionMinLevel", 20);
		CHAMPION_MAX_LVL = ConfigLoader.getInt("ChampionMaxLevel", 60);
		CHAMPION_SPECIAL_ITEM_LEVEL_DIFF = ConfigLoader.getInt("ChampionSpecialItemLevelDiff", 0);
		CHAMPION_SPECIAL_ITEM_CHANCE = ConfigLoader.getInt("ChampionSpecialItemChance", 0);
		CHAMPION_SPECIAL_ITEM_ID = ConfigLoader.getInt("ChampionSpecialItemID", 6393);
		CHAMPION_SPECIAL_ITEM_AMOUNT = ConfigLoader.getInt("ChampionSpecialItemAmount", 1);
		ALLOW_WEDDING = ConfigLoader.getBoolean("AllowWedding", true);
		SPAWN_WEDDING_NPC = ConfigLoader.getBoolean("SpawnWeddingNpc", false);
		WEDDING_PRICE = ConfigLoader.getInt("WeddingPrice", 500000);
		WEDDING_PUNISH_INFIDELITY = ConfigLoader.getBoolean("WeddingPunishInfidelity", true);
		WEDDING_TELEPORT = ConfigLoader.getBoolean("WeddingTeleport", true);
		WEDDING_TELEPORT_PRICE = ConfigLoader.getInt("WeddingTeleportPrice", 500);
		WEDDING_TELEPORT_INTERVAL = ConfigLoader.getInt("WeddingTeleportInterval", 120);
		WEDDING_ALLOW_SAME_SEX = ConfigLoader.getBoolean("WeddingAllowSameSex", false);
		WEDDING_FORMAL_WEAR = ConfigLoader.getBoolean("WeddingFormalWear", true);
		WEDDING_DIVORCE_COSTS = ConfigLoader.getInt("WeddingDivorceCosts", 20);
		WEDDING_GIVE_BOW = ConfigLoader.getBoolean("WeddingGiveBow", true);
		WEDDING_HONEYMOON = ConfigLoader.getBoolean("WeddingHoneyMoon", false);
		WEDDING_USE_NICK_COLOR = ConfigLoader.getBoolean("WeddingUseNickColor", true);
		WEDDING_NORMAL_PAIR_NICK_COLOR = ConfigLoader.getProperty("WeddingNormalPairNickColor", "BF0000");
		WEDDING_GAY_PAIR_NICK_COLOR = ConfigLoader.getProperty("WeddingGayPairNickColor", "0000BF");
		WEDDING_LESBI_PAIR_NICK_COLOR = ConfigLoader.getProperty("WeddingLesbiPairNickColor", "BF00BF");
		MEDAL_ADD_DROP = ConfigLoader.getBoolean("MedalAddDrop", false);
		MEDAL_1_DROP_CHANCE = ConfigLoader.getInt("Medal1DropChance", 10);
		MEDAL_2_DROP_CHANCE = ConfigLoader.getInt("Medal2DropChance", 2);
		CRISTMAS_ADD_DROP = ConfigLoader.getBoolean("CristmasAddDrop", false);
		CRISTMAS_DROP_CHANCE = ConfigLoader.getInt("CristmasDropChance", 10);
		CRISTMAS_TREE_LIFE_TIME = ConfigLoader.getInt("CristmasTreeLifeTime", 5);
		L2_DAY_ADD_DROP = ConfigLoader.getBoolean("L2DayAddDrop", false);
		L2_DAY_DROP_CHANCE = ConfigLoader.getInt("L2DayDropChance", 10);
		BIG_SQUASH_ADD_DROP = ConfigLoader.getBoolean("BigSquashAddDrop", false);
		BIG_SQUASH_DROP_CHANCE = ConfigLoader.getInt("BigSquashDropChance", 10);
		ALT_FESTIVAL_FIRST_SPAWN = ConfigLoader.getInt("AltFestivalFirstSpawn", 120000);
		ALT_FESTIVAL_FIRST_SWARM = ConfigLoader.getInt("AltFestivalFirstSwarm", 300000);
		ALT_FESTIVAL_SECOND_SPAWN = ConfigLoader.getInt("AltFestivalSecondSpawn", 540000);
		ALT_FESTIVAL_SECOND_SWARM = ConfigLoader.getInt("AltFestivalSecondSwarm", 720000);
		ALT_FESTIVAL_CHEST_SPAWN = ConfigLoader.getInt("AltFestivalChestSpawn", 900000);
		ALT_FESTIVAL_ARCHER_AGGRO = ConfigLoader.getInt("AltFestivalArcherAggro", 200);
		ALT_FESTIVAL_CHEST_AGGRO = ConfigLoader.getInt("AltFestivalChestAggro", 0);
		ALT_FESTIVAL_MONSTER_AGGRO = ConfigLoader.getInt("AltFestivalMonsterAggro", 200);
		ENABLE_EVENT_MESSAGE = ConfigLoader.getBoolean("EnableEventMessage", false);
		STAR_ADD_DROP = ConfigLoader.getBoolean("StarAddDrop", false);
		STAR_1_DROP_CHANCE = ConfigLoader.getInt("Star1DropChance", 10);
		STAR_2_DROP_CHANCE = ConfigLoader.getInt("Star2DropChance", 5);
		STAR_3_DROP_CHANCE = ConfigLoader.getInt("Star3DropChance", 2);
		STAR_SPAWN_MANAGER = ConfigLoader.getBoolean("StarSpawnManager", false);
		MEDAL_SPAWN_MENEGER = ConfigLoader.getBoolean("MedalSpawnMeneger", false);
		CRISTMAS_SPAWN_SANTA = ConfigLoader.getBoolean("CristmasSpawnSanta", false);
		L2_DAY_SPAWN_MANAGER = ConfigLoader.getBoolean("L2DaySpawnManager", false);
		L2_DAY_SCROLL_CHS = ConfigLoader.getInt("L2DayScrollChs", 300);
		L2_DAY_ENCH_SCROLL_CHS = ConfigLoader.getInt("L2DayEnchScrollChs", 100);
		L2_DAY_ACC_SCROLL_CHS = ConfigLoader.getInt("L2DayAccScrollChs", 10);
		L2_DAY_REWARDS = ConfigLoader.getProperty("L2DayRewards", "3931,3927,3928,3929,3926,3930,3933,3932,3935,3934");
		L2_DAY_REWARDS_ACCESSORIE = ConfigLoader.getProperty("L2DayRewardsAccessorie", "6662,6660");
		L2_DAY_REWARDS_SCROLL = ConfigLoader.getProperty("L2DayRewardsScroll", "3958,3959");
		L2_DROP_DAY_ADD_DROP = ConfigLoader.getBoolean("L2DropDayAddDrop", false);
		L2_DROP_DAY_ITEM_1 = ConfigLoader.getInt("L2DropDayItem1", 9500);
		L2_DROP_DAY_CHANCE_1 = ConfigLoader.getInt("L2DropDayChance1", 1);
		L2_DROP_DAY_ITEM_2 = ConfigLoader.getInt("L2DropDayItem2", 9501);
		L2_DROP_DAY_CHANCE_2 = ConfigLoader.getInt("L2DropDayChance2", 1);
		L2_DROP_DAY_ITEM_3 = ConfigLoader.getInt("L2DropDayItem3", 9502);
		L2_DROP_DAY_CHANCE_3 = ConfigLoader.getInt("L2DropDayChance3", 1);
		L2_DROP_DAY_ITEM_4 = ConfigLoader.getInt("L2DropDayItem4", 9503);
		L2_DROP_DAY_CHANCE_4 = ConfigLoader.getInt("L2DropDayChance4", 1);
		L2_DROP_DAY_ITEM_5 = ConfigLoader.getInt("L2DropDayItem5", 9504);
		L2_DROP_DAY_CHANCE_5 = ConfigLoader.getInt("L2DropDayChance5", 1);
		L2_DROP_DAY_ITEM_6 = ConfigLoader.getInt("L2DropDayItem6", 9505);
		L2_DROP_DAY_CHANCE_6 = ConfigLoader.getInt("L2DropDayChance6", 1);
		L2_DROP_DAY_ITEM_7 = ConfigLoader.getInt("L2DropDayItem7", 9506);
		L2_DROP_DAY_CHANCE_7 = ConfigLoader.getInt("L2DropDayChance7", 1);
		L2_DROP_DAY_ITEM_8 = ConfigLoader.getInt("L2DropDayItem8", 9507);
		L2_DROP_DAY_CHANCE_8 = ConfigLoader.getInt("L2DropDayChance8", 1);
		L2_DROP_DAY_ITEM_9 = ConfigLoader.getInt("L2DropDayItem9", 9508);
		L2_DROP_DAY_CHANCE_9 = ConfigLoader.getInt("L2DropDayChance9", 1);
		L2_DROP_DAY_ITEM_10 = ConfigLoader.getInt("L2DropDayItem10", 9509);
		L2_DROP_DAY_CHANCE_10 = ConfigLoader.getInt("L2DropDayChance10", 1);
		L2_DROP_DAY_ITEM_11 = ConfigLoader.getInt("L2DropDayItem11", 9510);
		L2_DROP_DAY_CHANCE_11 = ConfigLoader.getInt("L2DropDayChance11", 1);
		L2_DROP_DAY_ITEM_12 = ConfigLoader.getInt("L2DropDayItem12", 9511);
		L2_DROP_DAY_CHANCE_12 = ConfigLoader.getInt("L2DropDayChance12", 1);
		L2_DROP_DAY_ITEM_13 = ConfigLoader.getInt("L2DropDayItem13", 9512);
		L2_DROP_DAY_CHANCE_13 = ConfigLoader.getInt("L2DropDayChance13", 1);
		L2_DROP_DAY_ITEM_14 = ConfigLoader.getInt("L2DropDayItem14", 9513);
		L2_DROP_DAY_CHANCE_14 = ConfigLoader.getInt("L2DropDayChance14", 1);
		L2_DROP_DAY_ITEM_15 = ConfigLoader.getInt("L2DropDayItem15", 9514);
		L2_DROP_DAY_CHANCE_15 = ConfigLoader.getInt("L2DropDayChance15", 1);
		L2_DROP_DAY_ITEM_16 = ConfigLoader.getInt("L2DropDayItem16", 9515);
		L2_DROP_DAY_CHANCE_16 = ConfigLoader.getInt("L2DropDayChance16", 1);
		L2_DROP_DAY_ITEM_17 = ConfigLoader.getInt("L2DropDayItem17", 9516);
		L2_DROP_DAY_CHANCE_17 = ConfigLoader.getInt("L2DropDayChance17", 1);
		L2_DROP_DAY_ITEM_18 = ConfigLoader.getInt("L2DropDayItem18", 9517);
		L2_DROP_DAY_CHANCE_18 = ConfigLoader.getInt("L2DropDayChance18", 1);
		L2_DROP_DAY_ITEM_19 = ConfigLoader.getInt("L2DropDayItem19", 9518);
		L2_DROP_DAY_CHANCE_19 = ConfigLoader.getInt("L2DropDayChance19", 1);
		L2_DROP_DAY_ITEM_20 = ConfigLoader.getInt("L2DropDayItem20", 9519);
		L2_DROP_DAY_CHANCE_20 = ConfigLoader.getInt("L2DropDayChance20", 1);
		L2_DROP_DAY_ITEM_21 = ConfigLoader.getInt("L2DropDayItem21", 9520);
		L2_DROP_DAY_CHANCE_21 = ConfigLoader.getInt("L2DropDayChance21", 1);
		L2_DROP_DAY_ITEM_22 = ConfigLoader.getInt("L2DropDayItem22", 9521);
		L2_DROP_DAY_CHANCE_22 = ConfigLoader.getInt("L2DropDayChance22", 1);
		BIG_SQUASH_SPAWN_MANAGER = ConfigLoader.getBoolean("BigSquashSpawnManager", false);
		BIG_SQUASH_USE_SEEDS = ConfigLoader.getBoolean("BigSquashUseSeeds", false);
		ARENA_ENABLED = ConfigLoader.getBoolean("ArenaEnabled", false);
		ARENA_INTERVAL = ConfigLoader.getInt("ArenaInterval", 60);
		ARENA_REWARD_ID = ConfigLoader.getInt("ArenaRewardId", 57);
		ARENA_REWARD_COUNT = ConfigLoader.getInt("ArenaRewardCount", 0);
		FISHERMAN_ENABLED = ConfigLoader.getBoolean("FishermanEnabled", false);
		FISHERMAN_INTERVAL = ConfigLoader.getInt("FishermanInterval", 60);
		FISHERMAN_REWARD_ID = ConfigLoader.getInt("FishermanRewardId", 57);
		FISHERMAN_REWARD_COUNT = ConfigLoader.getInt("FishermanRewardCount", 0);
		WEDDING_TELEPORT_X = ConfigLoader.getInt("WeddingTeleporX", 0);
		WEDDING_TELEPORT_Y = ConfigLoader.getInt("WeddingTeleporY", 0);
		WEDDING_TELEPORT_Z = ConfigLoader.getInt("WeddingTeleporZ", 0);

		// Onda 20 (gameserver.properties)
		REQUEST_SERVER_ID = ConfigLoader.getInt("RequestServerId", 1);
		ACCEPT_ALTERNATE_ID = ConfigLoader.getBoolean("AcceptAlternateId", true);
		TIME_ZONE = ConfigLoader.getProperty("TimeZone", "America/Sao_Paulo");
		BAN_CHAT_LOG = ConfigLoader.getBoolean("BanChatLog", true);
		BAN_ACCOUNT_LOG = ConfigLoader.getBoolean("BanAccountLog", true);
		JAIL_LOG = ConfigLoader.getBoolean("JailLog", true);
		PLAYER_BAN_LOG = ConfigLoader.getBoolean("PlayerBanLog", true);
		CLASSIC_ANNOUNCE_MODE = ConfigLoader.getBoolean("ClassicAnnounceMode", true);
		ANNOUNCE_BAN_CHAT = ConfigLoader.getBoolean("AnnounceBanChat", false);
		ANNOUNCE_UNBAN_CHAT = ConfigLoader.getBoolean("AnnounceUnbanChat", false);
		ANNOUNCE_BAN_ACCOUNT = ConfigLoader.getBoolean("AnnounceBanAccount", false);
		ANNOUNCE_UNBAN_ACCOUNT = ConfigLoader.getBoolean("AnnounceUnBanAccount", false);
		ANNOUNCE_JAIL = ConfigLoader.getBoolean("AnnounceJail", false);
		ANNOUNCE_UNJAIL = ConfigLoader.getBoolean("AnnounceUnJail", false);
		ANNOUNCE_BAN_CHAR = ConfigLoader.getBoolean("AnnounceBanChar", false);
		ANNOUNCE_UNBAN_CHAR = ConfigLoader.getBoolean("AnnounceUnbanChar", false);
		GLOBAL_BAN_TIME = ConfigLoader.getInt("GlobalBanTime", 15);
		SAFE_REBOOT = ConfigLoader.getBoolean("SafeReboot", true);
		SAFE_REBOOT_TIME = ConfigLoader.getInt("SafeRebootTime", 30);
		SAFE_REBOOT_DISABLE_ENCHANT = ConfigLoader.getBoolean("SafeRebootDisableEnchant", true);
		SAFE_REBOOT_DISABLE_TELEPORT = ConfigLoader.getBoolean("SafeRebootDisableTeleport", true);
		SAFE_REBOOT_DISABLE_CREATE_ITEM = ConfigLoader.getBoolean("SafeRebootDisableCreateItem", true);
		SAFE_REBOOT_DISABLE_TRANSACTION = ConfigLoader.getBoolean("SafeRebootDisableTransaction", true);
		SAFE_REBOOT_DISABLE_PC_INTERACTION = ConfigLoader.getBoolean("SafeRebootDisablePcIteraction", true);
		SAFE_REBOOT_DISABLE_NPC_INTERACTION = ConfigLoader.getBoolean("SafeRebootDisableNpcIteraction", true);
		ONLY_CLAN_LEADER_CAN_SIT_ON_THRONE = ConfigLoader.getBoolean("OnlyClanleaderCanSitOnThrone", true);
		RIFT_MIN_PARTY_SIZE = ConfigLoader.getInt("RiftMinPartySize", 2);
		MAX_RIFT_JUMPS = ConfigLoader.getInt("MaxRiftJumps", 4);
		RIFT_SPAWN_DELAY = ConfigLoader.getInt("RiftSpawnDelay", 10000);
		AUTO_JUMPS_DELAY_MIN = ConfigLoader.getInt("AutoJumpsDelayMin", 480);
		AUTO_JUMPS_DELAY_MAX = ConfigLoader.getInt("AutoJumpsDelayMax", 600);
		BOSS_ROOM_TIME_MULTIPLY = ConfigLoader.getDouble("BossRoomTimeMultiply", 1.5);
		RECRUIT_COST = ConfigLoader.getInt("RecruitCost", 18);
		SOLDIER_COST = ConfigLoader.getInt("SoldierCost", 21);
		OFFICER_COST = ConfigLoader.getInt("OfficerCost", 24);
		CAPTAIN_COST = ConfigLoader.getInt("CaptainCost", 27);
		COMMANDER_COST = ConfigLoader.getInt("CommanderCost", 30);
		HERO_COST = ConfigLoader.getInt("HeroCost", 33);
		RESPAWN_RANDOM_IN_TOWN = ConfigLoader.getBoolean("RespawnRandomInTown", true);
		RESPAWN_RANDOM_MAX_OFFSET = ConfigLoader.getInt("RespawnRandomMaxOffset", 20);
		RESPAWN_RESTORE_CP = ConfigLoader.getInt("RespawnRestoreCP", 30);
		RESPAWN_RESTORE_HP = ConfigLoader.getInt("RespawnRestoreHP", 70);
		RESPAWN_RESTORE_MP = ConfigLoader.getInt("RespawnRestoreMP", 40);
		RAID_MINION_RESPAWN_TIME = ConfigLoader.getInt("RaidMinionRespawnTime", 300000);
		ALT_DEFAULT_RESTART_TOWN = ConfigLoader.getInt("AltDefaultRestartTown", 0);
		USE_MONSTER_RND_SPAWN = ConfigLoader.getBoolean("UseMonsterRndSpawn", true);
		RND_SPAWN_ZONE = ConfigLoader.getInt("RndSpawnZone", 300);
		SPAWN_CLASS_MASTER = ConfigLoader.getBoolean("SpawnClassMaster", true);
		CONFIG_CLASS_MASTER = ConfigLoader.getProperty("ConfigClassMaster", "1;[57(100000)];[];2;[57(1000000)];[];3;[57(10000000)],[5575(1000000)];[6622(1)]");
		ALLOW_DIALOG_CLASS_MASTER = ConfigLoader.getBoolean("AllowDialogClassMater", true);
		CLASS_MASTER_POPUP_WINDOW = ConfigLoader.getBoolean("ClassMasterPopupWindow", true);

		GAME_SERVER_LISTEN = ConfigLoader.getBoolean("GameServerListen", true);
		BAD_ID_CHECKING = ConfigLoader.getBoolean("BadIdChecking", true);
		ID_FACTORY = ConfigLoader.getProperty("IDFactory", "BitSet");
		CLEAN_BAD_IDS = ConfigLoader.getBoolean("CleanBadIDs", true);
		REGISTRATION_MODE = ConfigLoader.getInt("RegistrationMode", 2);
		REGISTRATION_TIME = ConfigLoader.getInt("RegistrationTime", 5);
		MIN_PARTY_COUNT = ConfigLoader.getInt("MinPartyCount", 2);
		MAX_PARTY_COUNT = ConfigLoader.getInt("MaxPartyCount", 5);
		MIN_PLAYER_COUNT = ConfigLoader.getInt("MinPlayerCount", 1);
		MAX_PLAYER_COUNT = ConfigLoader.getInt("MaxPlayerCount", 45);
		TIME_LIMIT = ConfigLoader.getInt("TimeLimit", 35);
		HOT_SPRING_DEBUFF_CHANCE = ConfigLoader.getInt("HotSpringDebuffChance", 15);
		PRIMAVEL_HAS_FLYING_MONSTERS = ConfigLoader.getBoolean("PrimavelHasFlyingMonsters", true);
		PRIMAVEL_FLYING_MONSTERS_COUNT = ConfigLoader.getInt("PrimavelFlyingMonstersCount", 5);
		FOG_MOBS_CLONE_CHANCE = ConfigLoader.getInt("FOGMobsCloneChance", 10);

		// Clan Hall Fees & Ratios
		CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallTeleportFunctionFeeLvl1", 7000);
		CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallTeleportFunctionFeeLvl2", 14000);
		CLAN_HALL_SUPPORT_FEE_LVL_1 = ConfigLoader.getInt("ClanHallSupportFeeLvl1", 2500);
		CLAN_HALL_SUPPORT_FEE_LVL_2 = ConfigLoader.getInt("ClanHallSupportFeeLvl2", 5000);
		CLAN_HALL_SUPPORT_FEE_LVL_3 = ConfigLoader.getInt("ClanHallSupportFeeLvl3", 7000);
		CLAN_HALL_SUPPORT_FEE_LVL_4 = ConfigLoader.getInt("ClanHallSupportFeeLvl4", 11000);
		CLAN_HALL_SUPPORT_FEE_LVL_5 = ConfigLoader.getInt("ClanHallSupportFeeLvl5", 21000);
		CLAN_HALL_SUPPORT_FEE_LVL_6 = ConfigLoader.getInt("ClanHallSupportFeeLvl6", 36000);
		CLAN_HALL_SUPPORT_FEE_LVL_7 = ConfigLoader.getInt("ClanHallSupportFeeLvl7", 37000);
		CLAN_HALL_SUPPORT_FEE_LVL_8 = ConfigLoader.getInt("ClanHallSupportFeeLvl8", 52000);
		CLAN_HALL_MP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallMpRegenerationFeeLvl1", 2000);
		CLAN_HALL_MP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallMpRegenerationFeeLvl2", 3750);
		CLAN_HALL_MP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("ClanHallMpRegenerationFeeLvl3", 6500);
		CLAN_HALL_MP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("ClanHallMpRegenerationFeeLvl4", 13750);
		CLAN_HALL_MP_REGENERATION_FEE_LVL_5 = ConfigLoader.getInt("ClanHallMpRegenerationFeeLvl5", 20000);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl1", 700);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl2", 800);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl3", 1000);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl4", 1166);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_5 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl5", 1500);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_6 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl6", 1750);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_7 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl7", 2000);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_8 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl8", 2250);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_9 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl9", 2500);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_10 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl10", 3250);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_11 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl11", 3750);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_12 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl12", 4250);
		CLAN_HALL_HP_REGENERATION_FEE_LVL_13 = ConfigLoader.getInt("ClanHallHpRegenerationFeeLvl13", 5166);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl1", 3000);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl2", 6000);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl3", 9000);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl4", 15000);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_5 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl5", 21000);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_6 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl6", 23330);
		CLAN_HALL_EXP_REGENERATION_FEE_LVL_7 = ConfigLoader.getInt("ClanHallExpRegenerationFeeLvl7", 30000);
		CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallItemCreationFunctionFeeLvl1", 30000);
		CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallItemCreationFunctionFeeLvl2", 70000);
		CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_3 = ConfigLoader.getInt("ClanHallItemCreationFunctionFeeLvl3", 140000);
		CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallCurtainFunctionFeeLvl1", 2000);
		CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallCurtainFunctionFeeLvl2", 2500);
		CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("ClanHallFrontPlatformFunctionFeeLvl1", 1300);
		CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("ClanHallFrontPlatformFunctionFeeLvl2", 4000);
		CLAN_HALL_TELEPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallTeleportFunctionFeeRatio", 604800000L);
		CLAN_HALL_SUPPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallSupportFunctionFeeRatio", 86400000L);
		CLAN_HALL_MP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallMpRegenerationFunctionFeeRatio", 86400000L);
		CLAN_HALL_HP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallHpRegenerationFunctionFeeRatio", 86400000L);
		CLAN_HALL_EXP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallExpRegenerationFunctionFeeRatio", 86400000L);
		CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallItemCreationFunctionFeeRatio", 86400000L);
		CLAN_HALL_CURTAIN_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallCurtainFunctionFeeRatio", 86400000L);
		CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_RATIO = ConfigLoader.getLong("ClanHallFrontPlatformFunctionFeeRatio", 259200000L);

		// Castle Fees & Ratios
		CASTLE_TELEPORT_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("CastleTeleportFunctionFeeLvl1", 7000);
		CASTLE_TELEPORT_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("CastleTeleportFunctionFeeLvl2", 14000);
		CASTLE_SUPPORT_FEE_LVL_1 = ConfigLoader.getInt("CastleSupportFeeLvl1", 7000);
		CASTLE_SUPPORT_FEE_LVL_2 = ConfigLoader.getInt("CastleSupportFeeLvl2", 21000);
		CASTLE_SUPPORT_FEE_LVL_3 = ConfigLoader.getInt("CastleSupportFeeLvl3", 37000);
		CASTLE_SUPPORT_FEE_LVL_4 = ConfigLoader.getInt("CastleSupportFeeLvl4", 52000);
		CASTLE_MP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("CastleMpRegenerationFeeLvl1", 2000);
		CASTLE_MP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("CastleMpRegenerationFeeLvl2", 6500);
		CASTLE_MP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("CastleMpRegenerationFeeLvl3", 13750);
		CASTLE_MP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("CastleMpRegenerationFeeLvl4", 20000);
		CASTLE_HP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("CastleHpRegenerationFeeLvl1", 1000);
		CASTLE_HP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("CastleHpRegenerationFeeLvl2", 1500);
		CASTLE_HP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("CastleHpRegenerationFeeLvl3", 2250);
		CASTLE_HP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("CastleHpRegenerationFeeLvl4", 3270);
		CASTLE_HP_REGENERATION_FEE_LVL_5 = ConfigLoader.getInt("CastleHpRegenerationFeeLvl5", 5166);
		CASTLE_EXP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("CastleExpRegenerationFeeLvl1", 9000);
		CASTLE_EXP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("CastleExpRegenerationFeeLvl2", 15000);
		CASTLE_EXP_REGENERATION_FEE_LVL_3 = ConfigLoader.getInt("CastleExpRegenerationFeeLvl3", 21000);
		CASTLE_EXP_REGENERATION_FEE_LVL_4 = ConfigLoader.getInt("CastleExpRegenerationFeeLvl4", 30000);
		CASTLE_TELEPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("CastleTeleportFunctionFeeRatio", 604800000L);
		CASTLE_SUPPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("CastleSupportFunctionFeeRatio", 86400000L);
		CASTLE_MP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("CastleMpRegenerationFunctionFeeRatio", 86400000L);
		CASTLE_HP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("CastleHpRegenerationFunctionFeeRatio", 86400000L);
		CASTLE_EXP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("CastleExpRegenerationFunctionFeeRatio", 86400000L);

		// Fort Fees & Ratios
		FORT_TELEPORT_FUNCTION_FEE_LVL_1 = ConfigLoader.getInt("FortTeleportFunctionFeeLvl1", 1000);
		FORT_TELEPORT_FUNCTION_FEE_LVL_2 = ConfigLoader.getInt("FortTeleportFunctionFeeLvl2", 10000);
		FORT_SUPPORT_FEE_LVL_1 = ConfigLoader.getInt("FortSupportFeeLvl1", 7000);
		FORT_SUPPORT_FEE_LVL_2 = ConfigLoader.getInt("FortSupportFeeLvl2", 17000);
		FORT_MP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("FortMpRegenerationFeeLvl1", 6500);
		FORT_MP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("FortMpRegenerationFeeLvl2", 9300);
		FORT_HP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("FortHpRegenerationFeeLvl1", 2000);
		FORT_HP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("FortHpRegenerationFeeLvl2", 3500);
		FORT_EXP_REGENERATION_FEE_LVL_1 = ConfigLoader.getInt("FortExpRegenerationFeeLvl1", 9000);
		FORT_EXP_REGENERATION_FEE_LVL_2 = ConfigLoader.getInt("FortExpRegenerationFeeLvl2", 10000);
		FORT_TELEPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("FortTeleportFunctionFeeRatio", 604800000L);
		FORT_SUPPORT_FUNCTION_FEE_RATIO = ConfigLoader.getLong("FortSupportFunctionFeeRatio", 86400000L);
		FORT_MP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("FortMpRegenerationFunctionFeeRatio", 86400000L);
		FORT_HP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("FortHpRegenerationFunctionFeeRatio", 86400000L);
		FORT_EXP_REGENERATION_FUNCTION_FEE_RATIO = ConfigLoader.getLong("FortExpRegenerationFunctionFeeRatio", 86400000L);

		// Onda 21 (network.properties, authserver.properties)
		MIN_PROTOCOL_VERSION = ConfigLoader.getInt("MinProtocolVersion", 730);
		MAX_PROTOCOL_VERSION = ConfigLoader.getInt("MaxProtocolVersion", 746);
		MAXIMUM_DB_CONNECTIONS = ConfigLoader.getInt("game/main/network.properties", "MaximumDbConnections", 500);
		ACCEPT_NEW_GAME_SERVER = ConfigLoader.getBoolean("AcceptNewGameServer", false);
		GM_MIN_LEVEL = ConfigLoader.getInt("GMMinLevel", 1);
		BRUT_PROTECTION = ConfigLoader.getBoolean("BrutProtection", true);
		SESSION_TTL = ConfigLoader.getInt("SessionTTL", 15);
		MAX_SESSIONS = ConfigLoader.getInt("MaxSessions", 100);
		MAX_ACCOUNT_REGISTRATION = ConfigLoader.getInt("MaxAccountRegistration", 3000);
		ENABLE_FLOOD_PROTECTION = ConfigLoader.getBoolean("EnableFloodProtection", true);
		FAST_CONNECTION_LIMIT = ConfigLoader.getInt("FastConnectionLimit", 15);
		NORMAL_CONNECTION_TIME = ConfigLoader.getInt("NormalConnectionTime", 500);
		FAST_CONNECTION_TIME = ConfigLoader.getInt("FastConnectionTime", 250);
		MAX_CONNECTION_PER_IP = ConfigLoader.getInt("MaxConnectionPerIP", 5);
		INACTIVE_TIMEOUT = ConfigLoader.getInt("InactiveTimeOut", 3);
		LOGIN_AUTH_PORT = ConfigLoader.getInt("AuthPort", 9014);
		LOGIN_AUTH_HOSTNAME = ConfigLoader.getProperty("AuthHostName", "127.0.0.1");
		IP_UPDATE_TIME = ConfigLoader.getInt("IpUpdateTime", 10);
		LOGIN_MAX_DB_CONNECTIONS = ConfigLoader.getInt("login/network.properties", "MaximumDbConnections", 1000);

		// Revision
		REVISION_VERSION = ConfigLoader.getInt("Version", 236);
		REVISION_BUILD_DATE = ConfigLoader.getProperty("BuildDate", "2025-05-28 22:20:05");

		// Authserver
		ON_SELECT_SERVER = ConfigLoader.getProperty("OnSelectServer", "notify");
		ON_SELECT_SERVER_COMMAND = ConfigLoader.getProperty("OnSelectServerCommand", "");
		LOGIN_SERVER_LISTEN = ConfigLoader.getBoolean("LoginServerListen", true);

		// Network Database
		DB_DRIVER = ConfigLoader.getProperty("Driver", "org.mariadb.jdbc.Driver");
		DB_URL = ConfigLoader.getProperty("URL", "jdbc:mariadb://localhost:3306/l2jdb");
		DB_LOGIN = ConfigLoader.getProperty("Login", "root");
		DB_PASSWORD = ConfigLoader.getProperty("Password", "");

		// Options
		SERVER_LIST_BRACKETS = ConfigLoader.getBoolean("ServerListBrackets", false);
		SERVER_LIST_CLOCK = ConfigLoader.getBoolean("ServerListClock", false);
		LOG_ITEMS = ConfigLoader.getBoolean("LogItems", true);
		IGNORE_LOG_ITEMS = ConfigLoader.getProperty("IgnoreLogItems", "CONSUME RESET");
		ALLOW_LOTTERY = ConfigLoader.getBoolean("AllowLottery", true);
		GEO_DATA_ROOT = ConfigLoader.getProperty("GeoDataRoot", "./data");
		GEO_ENGINE = ConfigLoader.getProperty("GeoEngine", "geodata");

		// NPC
		SHOW_NPC_LEVEL = ConfigLoader.getBoolean("ShowNpcLevel", true);
		MIN_NPC_ANIMATION = ConfigLoader.getInt("MinNPCAnimation", 10);
		MAX_NPC_ANIMATION = ConfigLoader.getInt("MaxNPCAnimation", 20);
		MIN_MONSTER_ANIMATION = ConfigLoader.getInt("MinMonsterAnimation", 5);
		MAX_MONSTER_ANIMATION = ConfigLoader.getInt("MaxMonsterAnimation", 20);
		MIN_NPC_WALK_ANIMATION = ConfigLoader.getInt("MinNPCWalkAnimation", 10);
		MAX_NPC_WALK_ANIMATION = ConfigLoader.getInt("MaxNPCWalkAnimation", 20);
		WYVERN_SPEED = ConfigLoader.getInt("WyvernSpeed", 100);
		STRIDER_SPEED = ConfigLoader.getInt("StriderSpeed", 80);
		URN_TEMP_FAIL = ConfigLoader.getInt("UrnTempFail", 10);
		FORCE_UPDATE_RAID_BOSS_ON_DB = ConfigLoader.getBoolean("ForceUpdateRaidBossOnDB", false);
		NO_RESTART_KICK_TIME = ConfigLoader.getInt("NoRestartKickTime", 5);

		// Player
		ALT_GAME_VIEW_NPC = ConfigLoader.getBoolean("AltGameViewNpc", false);
		ALT_GAME_VIEW_NPC_DROP = ConfigLoader.getBoolean("AltGameViewNpcDrop", true);
		ALLOW_EXCHANGE = ConfigLoader.getBoolean("AllowExchange", false);
		CRUMA_TOWER_LEVEL_RESTRICT = ConfigLoader.getInt("CrumaTowerLevelRestrict", 56);
		ALT_GAME_EXPONENT_XP = ConfigLoader.getInt("AltGameExponentXp", 0);
		ALT_GAME_EXPONENT_SP = ConfigLoader.getInt("AltGameExponentSp", 0);
		ALT_SUMMON_PENALTY_RATE = ConfigLoader.getDouble("AltSummonPenaltyRate", 1.0);
		ALT_GAME_SKILL_LEARN = ConfigLoader.getBoolean("AltGameSkillLearn", false);
		AUTO_LEARN_DIVINE_INSPIRATION = ConfigLoader.getBoolean("AutoLearnDivineInspiration", false);
		SP_BOOK_NEEDED = ConfigLoader.getBoolean("SpBookNeeded", false);
		LIFE_CRYSTAL_NEEDED = ConfigLoader.getBoolean("LifeCrystalNeeded", true);
		ENCHANT_SKILL_SP_BOOK_NEEDED = ConfigLoader.getBoolean("EnchantSkillSpBookNeeded", true);
		ENCH_SKILL_XP_NEEDED = ConfigLoader.getBoolean("EnchSkillXpNeeded", true);
		ENCH_SKILL_SP_NEEDED = ConfigLoader.getBoolean("EnchSkillSpNeeded", true);
		DIVINE_INSPIRATION_SP_BOOK_NEEDED = ConfigLoader.getBoolean("DivineInspirationSpBookNeeded", true);
		ALT_ITEM_SKILLS_NOT_INFLUENCED = ConfigLoader.getBoolean("AltItemSkillsNotInfluenced", false);
		CHECK_SKILLS_ON_ENTER = ConfigLoader.getBoolean("CheckSkillsOnEnter", false);
		CHECK_ADDITIONAL_SKILLS = ConfigLoader.getBoolean("CheckAdditionalSkills", false);
		SEND_NOT_DONE_SKILLS = ConfigLoader.getInt("SendNOTDONESkills", 2);
		ALLOW_USER_MENU = ConfigLoader.getBoolean("AllowUserMenu", true);
		ALLOW_USE_EXP_SET = ConfigLoader.getBoolean("AllowUseExpSet", true);
		PLAYER_FAKE_DEATH_UP_PROTECTION = ConfigLoader.getInt("PlayerFakeDeathUpProtection", 0);
		LIST_PET_RENT_NPC = getIntList("ListPetRentNpc", List.of(30827, 30828));
		ALLOW_KEYBOARD_MOVEMENT = ConfigLoader.getBoolean("AllowKeyboardMovement", true);
		CHECK_PLAYER_MACRO = ConfigLoader.getBoolean("CheckPlayerMacro", true);
		String macroCmds = ConfigLoader.getProperty("MacroRestrictedCommandList", "exit,fly,hero,[gm],[adm]");
		MACRO_RESTRICTED_COMMAND_LIST = Arrays.stream(macroCmds.split("[,;\\s]+"))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		CURSED_WEAPON_NPC_INTERACT = ConfigLoader.getBoolean("CursedWeaponNpcInteract", false);
		LIST_OF_PET_ITEMS = getIntList("ListOfPetItems", List.of(2375, 3500, 3501, 3502, 4422, 4423, 4424, 4425, 6648, 6649, 6650, 9882));
		LIST_OF_NON_DROPPABLE_ITEMS = getIntList("ListOfNonDroppableItems", List.of(57, 1147, 425, 1146, 461, 10, 2368, 7, 6, 2370, 2369, 6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 8181, 5575, 7694, 9388, 9389, 9390));

		// Rates & Manor
		AUGMENT_EXCLUDE_NOTDONE = ConfigLoader.getBoolean("AugmentExcludeNotdone", false);
		MAX_WAREHOUSE_SLOTS_FOR_DWARF = ConfigLoader.getInt("MaxWarehouseSlotsForDwarf", 120);
		MAX_WAREHOUSE_SLOTS_FOR_CLAN = ConfigLoader.getInt("MaxWarehouseSlotsForClan", 200);
		MAX_WAREHOUSE_FREIGHT_SLOTS = ConfigLoader.getInt("MaxWarehouseFreightSlots", 100);
		ENABLE_WAREHOUSE_SORTING_CLAN = ConfigLoader.getBoolean("EnableWarehouseSortingClan", false);
		ENABLE_WAREHOUSE_SORTING_PRIVATE = ConfigLoader.getBoolean("EnableWarehouseSortingPrivate", false);
		ENABLE_WAREHOUSE_SORTING_FREIGHT = ConfigLoader.getBoolean("EnableWarehouseSortingFreight", false);
		ALT_GAME_FREIGHTS = ConfigLoader.getBoolean("AltGameFreights", false);
		ALT_GAME_FREIGHT_PRICE = ConfigLoader.getInt("AltGameFreightPrice", 1000);
		ALLOW_FREIGHT = ConfigLoader.getBoolean("AllowFreight", true);
		WAREHOUSE_CACHE = ConfigLoader.getBoolean("WarehouseCache", false);
		WAREHOUSE_CACHE_TIME = ConfigLoader.getInt("WarehouseCacheTime", 15);
		MAX_PVT_STORE_SELL_SLOTS_DWARF = ConfigLoader.getInt("MaxPvtStoreSellSlotsDwarf", 6);
		MAX_PVT_STORE_SELL_SLOTS_OTHER = ConfigLoader.getInt("MaxPvtStoreSellSlotsOther", 4);
		MAX_PVT_STORE_BUY_SLOTS_DWARF = ConfigLoader.getInt("MaxPvtStoreBuySlotsDwarf", 6);
		MAX_PVT_STORE_BUY_SLOTS_OTHER = ConfigLoader.getInt("MaxPvtStoreBuySlotsOther", 4);
		CHECK_ZONE_ON_PVT = ConfigLoader.getBoolean("CheckZoneOnPvt", false);
		NPC_HP_REGEN_MULTIPLIER = ConfigLoader.getInt("NPCHpRegenMultiplier", 100);
		NPC_MP_REGEN_MULTIPLIER = ConfigLoader.getInt("NPCMpRegenMultiplier", 100);
		PET_HP_REGEN_MULTIPLIER = ConfigLoader.getInt("PetHpRegenMultiplier", 100);
		PET_MP_REGEN_MULTIPLIER = ConfigLoader.getInt("PetMpRegenMultiplier", 100);
		RAID_HP_REGEN_MULTIPLIER = ConfigLoader.getInt("RaidHpRegenMultiplier", 100);
		RAID_MP_REGEN_MULTIPLIER = ConfigLoader.getInt("RaidMpRegenMultiplier", 100);
		RAID_P_DEFENCE_MULTIPLIER = ConfigLoader.getInt("RaidPDefenceMultiplier", 100);
		RAID_M_DEFENCE_MULTIPLIER = ConfigLoader.getInt("RaidMDefenceMultiplier", 100);
		ALT_MANOR_REFRESH_TIME = ConfigLoader.getInt("AltManorRefreshTime", 20);
		ALT_MANOR_REFRESH_MIN = ConfigLoader.getInt("AltManorRefreshMin", 0);
		ALT_MANOR_APPROVE_TIME = ConfigLoader.getInt("AltManorApproveTime", 6);
		ALT_MANOR_APPROVE_MIN = ConfigLoader.getInt("AltManorApproveMin", 0);
		ALT_MANOR_MAINTENANCE_PERIOD = ConfigLoader.getInt("AltManorMaintenancePeriod", 360000);
		ALT_MANOR_SAVE_ALL_ACTIONS = ConfigLoader.getBoolean("AltManorSaveAllActions", false);
		ALT_MANOR_SAVE_PERIOD_RATE = ConfigLoader.getInt("AltManorSavePeriodRate", 2);
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

	private static Map<Integer, Integer> parseSkillDurationList(String str) {
		if (str == null || str.isBlank()) {
			return Collections.emptyMap();
		}
		Map<Integer, Integer> map = new HashMap<>();
		String[] entries = str.split(";");
		for (String entry : entries) {
			String clean = entry.trim();
			if (clean.isEmpty()) continue;
			String[] parts = clean.split(",");
			if (parts.length == 2) {
				try {
					int skillId = Integer.parseInt(parts[0].trim());
					int durationSec = Integer.parseInt(parts[1].trim());
					map.put(skillId, durationSec);
				} catch (Exception ignored) {}
			}
		}
		return Collections.unmodifiableMap(map);
	}
}
