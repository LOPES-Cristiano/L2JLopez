package com.lopez.l2j.game.model;

import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.skill.StatFunc;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Personagem persistido na tabela legada {@code characters}. Mutavel apenas no
 * que muda durante o jogo
 * (posicao, HP/MP/CP, flags de sessao); o resto e imutavel por enquanto. Acesso
 * sempre pela thread da
 * conexao dona do personagem.
 */
public final class PlayerCharacter {

	private final int objectId;
	private final String account;
	private String name;
	private int level;
	private long exp;
	private int sp;
	private final int race;
	private int classId;
	private int baseClassId;
	private final boolean female;
	private int face;
	private int hairStyle;
	private int hairColor;
	private int maxHp;
	private int maxMp;
	private int maxCp;
	private int karma;
	private int pvpKills;
	private int pkKills;
	private int clanId;
	private volatile long clanJoinExpiryTime;
	private boolean clanLeader;
	private String title;
	private int accessLevel;
	private final long lastAccess;
	private long deleteTime;
	private int mountType;
	private int gmSpeed;
	private boolean invul;
	private boolean invis;
	private boolean silence;
	private boolean diet;
	private int polyNpcId;
	private int charges;

	private int x;
	private int y;
	private int z;
	private int heading;
	private int instanceId = 0;
	private double currentHp;
	private double currentMp;
	private double currentCp;
	private boolean running = true;
	private boolean sitting;
	private Inventory inventory;

	private final Map<Integer, Integer> skills = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> equippedItemSkills = new ConcurrentHashMap<>();
	private final java.util.Set<Integer> armorSetSkillIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private final PlayerEffects effects = new PlayerEffects();
	private List<StatFunc> passiveFuncs = List.of();
	private List<StatFunc> armorSetFuncs = List.of();
	private List<StatFunc> augmentationFuncs = List.of();
	private int augStr;
	private int augCon;
	private int augInt;
	private int augMen;
	private int activeAugmentationSkillId;
	private int activeAugmentationSkillLevel;
	private int abnormalEffect;

	private final int[] hennas = new int[3];
	private int hennaInt;
	private int hennaStr;
	private int hennaCon;
	private int hennaMen;
	private int hennaDex;
	private int hennaWit;

	private int recomHave;
	private int recomLeft = 20;
	private long lastRecomDate;
	private final Set<Integer> recommendedToday = ConcurrentHashMap.newKeySet();
	private final Set<String> blockList = ConcurrentHashMap.newKeySet();
	private volatile boolean blockingAll;
	private int nameColor = 0xFFFFFF;
	private int titleColor = 0xFFFF77;

	public int nameColor() { return nameColor; }
	public void nameColor(int nameColor) { this.nameColor = nameColor; }
	public int titleColor() { return titleColor; }
	public void titleColor(int titleColor) { this.titleColor = titleColor; }

	private boolean aio;
	public boolean isAio() { return aio; }
	public void setAio(boolean aio) { this.aio = aio; }
	public void aio(boolean aio) { this.aio = aio; }

	private volatile long lastCombatTime;

	public boolean isInCombat() {
		return (System.currentTimeMillis() - lastCombatTime) < 15_000L;
	}

	public void enterCombat() {
		this.lastCombatTime = System.currentTimeMillis();
	}

	public void leaveCombat() {
		this.lastCombatTime = 0L;
	}

	public long lastCombatTime() {
		return lastCombatTime;
	}

	public int expertiseGrade() {
		return expertiseGrade(level);
	}

	public static int expertiseGrade(int level) {
		if (level >= 76) return 5; // S
		if (level >= 61) return 4; // A
		if (level >= 52) return 3; // B
		if (level >= 40) return 2; // C
		if (level >= 20) return 1; // D
		return 0; // None
	}

	public boolean isMage() {
		return (classId >= 10 && classId <= 17) || (classId >= 25 && classId <= 30)
				|| (classId >= 38 && classId <= 43) || (classId >= 49 && classId <= 52)
				|| (classId >= 94 && classId <= 98) || (classId >= 103 && classId <= 105)
				|| (classId >= 110 && classId <= 112) || (classId >= 115 && classId <= 116);
	}

	public int getObjectId() { return objectId; }
	public String getName() { return name; }
	public int getLevel() { return level; }
	public int getX() { return x; }
	public int getY() { return y; }
	public int getZ() { return z; }
	public void setX(int x) { this.x = x; }
	public void setY(int y) { this.y = y; }
	public void setZ(int z) { this.z = z; }
	public int getKarma() { return karma; }
	public String accountName() { return account; }

	public PlayerCharacter(int objectId, String account, String name, int level, long exp, int sp, int race,
			int classId, int baseClassId, boolean female, int face, int hairStyle, int hairColor, int maxHp,
			int maxMp, int maxCp, int karma, int pvpKills, int pkKills, int clanId, String title, int accessLevel,
			long lastAccess, long deleteTime, int x, int y, int z, int heading, double currentHp,
			double currentMp, double currentCp) {
		this.objectId = objectId;
		this.account = account;
		this.name = name;
		this.level = level;
		this.exp = exp;
		this.sp = sp;
		this.race = race;
		this.classId = classId;
		this.baseClassId = baseClassId;
		this.female = female;
		this.face = face;
		this.hairStyle = hairStyle;
		this.hairColor = hairColor;
		this.maxHp = maxHp;
		this.maxMp = maxMp;
		this.maxCp = maxCp;
		this.karma = karma;
		this.pvpKills = pvpKills;
		this.pkKills = pkKills;
		this.clanId = clanId;
		this.title = title == null ? "" : title;
		this.accessLevel = accessLevel;
		this.lastAccess = lastAccess;
		this.deleteTime = deleteTime;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.currentHp = currentHp;
		this.currentMp = currentMp;
		this.currentCp = currentCp;
	}

	public int objectId() {
		return objectId;
	}

	public String account() {
		return account;
	}

	public String name() {
		return name;
	}

	public int level() {
		return level;
	}

	public long exp() {
		return exp;
	}

	public int sp() {
		return sp;
	}

	public int race() {
		return race;
	}

	public int classId() {
		return classId;
	}

	public void classId(int value) {
		this.classId = value;
	}

	public int baseClassId() {
		return baseClassId;
	}

	public void baseClassId(int value) {
		this.baseClassId = value;
	}

	private int classIndex = 0;
	private final java.util.Map<Integer, com.lopez.l2j.game.subclass.SubClass> subClasses = new java.util.concurrent.ConcurrentHashMap<>();

	public int classIndex() {
		return classIndex;
	}

	public void classIndex(int value) {
		this.classIndex = value;
	}

	public boolean isSubClassActive() {
		return classIndex > 0;
	}

	public java.util.Map<Integer, com.lopez.l2j.game.subclass.SubClass> subClasses() {
		return subClasses;
	}

	public java.util.Map<Integer, com.lopez.l2j.game.subclass.SubClass> getSubClasses() {
		return subClasses;
	}

	public void setSubClasses(java.util.Map<Integer, com.lopez.l2j.game.subclass.SubClass> map) {
		this.subClasses.clear();
		if (map != null) {
			this.subClasses.putAll(map);
		}
	}

	public boolean female() {
		return female;
	}

	public int face() {
		return face;
	}

	public void face(int value) {
		this.face = value;
	}

	public int hairStyle() {
		return hairStyle;
	}

	public void hairStyle(int value) {
		this.hairStyle = value;
	}

	public int hairColor() {
		return hairColor;
	}

	public void hairColor(int value) {
		this.hairColor = value;
	}

	public int maxHp() {
		return maxHp;
	}

	public void maxHp(int value) {
		this.maxHp = value;
	}

	public int maxMp() {
		return maxMp;
	}

	public void maxMp(int value) {
		this.maxMp = value;
	}

	public int maxCp() {
		return maxCp;
	}

	public void maxCp(int value) {
		this.maxCp = value;
	}

	public int karma() {
		return karma;
	}

	public int pvpKills() {
		return pvpKills;
	}

	public int pkKills() {
		return pkKills;
	}

	public int clanId() {
		return clanId;
	}

	public void clanId(int value) {
		this.clanId = value;
	}

	public long clanJoinExpiryTime() {
		return clanJoinExpiryTime;
	}

	public void clanJoinExpiryTime(long value) {
		this.clanJoinExpiryTime = value;
	}

	public boolean clanLeader() {
		return clanLeader;
	}

	public boolean isClanLeader() {
		return clanLeader;
	}

	public void clanLeader(boolean value) {
		this.clanLeader = value;
	}

	public boolean hasClanJoinPenalty() {
		return System.currentTimeMillis() < clanJoinExpiryTime;
	}

	public String title() {
		return title;
	}

	public int accessLevel() {
		return accessLevel;
	}

	public void accessLevel(int value) {
		this.accessLevel = value;
	}

	public boolean isGm() {
		return accessLevel > 0;
	}

	public long lastAccess() {
		return lastAccess;
	}

	public long deleteTime() {
		return deleteTime;
	}

	public void deleteTime(long value) {
		this.deleteTime = value;
	}

	public int instanceId() {
		return instanceId;
	}

	public void instanceId(int value) {
		this.instanceId = value;
	}

	public int x() {
		return x;
	}

	public void x(int value) {
		this.x = value;
	}

	public int y() {
		return y;
	}

	public void y(int value) {
		this.y = value;
	}

	public int z() {
		return z;
	}

	public void z(int value) {
		this.z = value;
	}

	public void teleport(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int heading() {
		return heading;
	}

	public double currentHp() {
		return currentHp;
	}

	public void currentHp(double value) {
		this.currentHp = Math.max(0, Math.min(maxHp, value));
	}

	public boolean isDead() {
		return currentHp <= 0.0;
	}

	public boolean isAlikeDead() {
		return isDead();
	}

	private volatile boolean olympiadMode;

	public boolean isOlympiadMode() {
		return olympiadMode;
	}

	public void inOlympiadMode(boolean value) {
		this.olympiadMode = value;
	}

	public void setOlympiadMode(boolean value) {
		this.olympiadMode = value;
	}

	public double currentMp() {
		return currentMp;
	}

	public void currentMp(double value) {
		this.currentMp = Math.max(0, Math.min(maxMp, value));
	}

	public double currentCp() {
		return currentCp;
	}

	public void currentCp(double value) {
		this.currentCp = Math.max(0, Math.min(maxCp, value));
	}

	public void exp(long value) {
		this.exp = value;
	}

	public void sp(int value) {
		this.sp = value;
	}

	public void level(int value) {
		this.level = value;
	}

	public boolean running() {
		return running;
	}

	public boolean isMoving() {
		return running;
	}

	public void running(boolean value) {
		this.running = value;
	}

	public boolean sitting() {
		return sitting;
	}

	public boolean isSitting() {
		return sitting;
	}

	public void sitting(boolean value) {
		this.sitting = value;
	}

	public Map<Integer, Integer> skills() {
		return skills;
	}

	public int skillLevel(int skillId) {
		return skills.getOrDefault(skillId, 0);
	}

	public PlayerEffects effects() {
		return effects;
	}

	public Map<Integer, Integer> equippedItemSkills() {
		return equippedItemSkills;
	}

	public java.util.Set<Integer> armorSetSkillIds() {
		return armorSetSkillIds;
	}

	public List<StatFunc> passiveFuncs() {
		return passiveFuncs;
	}

	public void passiveFuncs(List<StatFunc> funcs) {
		this.passiveFuncs = funcs == null ? List.of() : List.copyOf(funcs);
	}

	public List<StatFunc> armorSetFuncs() {
		return armorSetFuncs;
	}

	public void armorSetFuncs(List<StatFunc> funcs) {
		this.armorSetFuncs = funcs == null ? List.of() : List.copyOf(funcs);
	}

	public List<StatFunc> augmentationFuncs() {
		return augmentationFuncs;
	}

	public void augmentationFuncs(List<StatFunc> funcs) {
		this.augmentationFuncs = funcs == null ? List.of() : List.copyOf(funcs);
	}

	public int augSTR() { return augStr; }
	public void augSTR(int val) { this.augStr = val; }
	public int augCON() { return augCon; }
	public void augCON(int val) { this.augCon = val; }
	public int augINT() { return augInt; }
	public void augINT(int val) { this.augInt = val; }
	public int augMEN() { return augMen; }
	public void augMEN(int val) { this.augMen = val; }

	public int activeAugmentationSkillId() { return activeAugmentationSkillId; }
	public int activeAugmentationSkillLevel() { return activeAugmentationSkillLevel; }

	public void setAugmentationSkill(int id, int level) {
		this.activeAugmentationSkillId = id;
		this.activeAugmentationSkillLevel = level;
	}

	public void clearAugmentationBonus() {
		this.augmentationFuncs = List.of();
		this.augStr = 0;
		this.augCon = 0;
		this.augInt = 0;
		this.augMen = 0;
		this.activeAugmentationSkillId = 0;
		this.activeAugmentationSkillLevel = 0;
	}

	public int abnormalEffect() {
		return abnormalEffect;
	}

	public void startAbnormalEffect(int mask) {
		this.abnormalEffect |= mask;
	}

	public void stopAbnormalEffect(int mask) {
		this.abnormalEffect &= ~mask;
	}

	private volatile long disabledUntil;
	private volatile boolean sleeping;
	private volatile long rootedUntil;
	private volatile long mutedUntil;

	public void disable(long until, boolean sleep) {
		this.disabledUntil = Math.max(disabledUntil, until);
		this.sleeping = sleep;
	}

	public void root(long until) {
		this.rootedUntil = Math.max(rootedUntil, until);
	}

	public void mute(long until) {
		this.mutedUntil = Math.max(mutedUntil, until);
	}

	public boolean isDisabled() {
		return System.currentTimeMillis() < disabledUntil;
	}

	public boolean isRooted() {
		return System.currentTimeMillis() < rootedUntil;
	}

	public boolean isMuted() {
		return System.currentTimeMillis() < mutedUntil;
	}

	public void onDamaged() {
		if (sleeping) {
			sleeping = false;
			disabledUntil = 0;
		}
	}

	/**
	 * Inventario carregado ao entrar no jogo (vazio enquanto o personagem esta so
	 * na lista).
	 */
	public Inventory inventory() {
		if (inventory == null) {
			inventory = new Inventory(objectId);
		}
		return inventory;
	}

	public void inventory(Inventory value) {
		this.inventory = value;
	}

	/**
	 * Nivel de encantamento da arma equipada (0..127) para exibicao de brilho/aura visual no cliente
	 * (Interlude: 0=sem brilho, 4..15=azul, 16+=vermelho).
	 */
	public int enchantEffect() {
		if (inventory == null) {
			return 0;
		}
		var wpn = inventory.paperdoll(com.lopez.l2j.game.item.ItemSlots.RHAND);
		return wpn != null ? Math.min(127, Math.max(0, wpn.enchant())) : 0;
	}

	public void moveTo(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void heading(int value) {
		this.heading = value;
	}

	public void name(String value) {
		this.name = value;
	}

	public void title(String value) {
		this.title = value;
	}

	public void karma(int value) {
		this.karma = value;
	}

	public void pvpKills(int value) {
		this.pvpKills = value;
	}

	public void pkKills(int value) {
		this.pkKills = value;
	}

	public int mountType() {
		return mountType;
	}

	public void mountType(int value) {
		this.mountType = value;
	}

	public int gmSpeed() {
		return gmSpeed;
	}

	public void gmSpeed(int value) {
		this.gmSpeed = value;
	}

	public boolean invul() {
		return invul;
	}

	public void invul(boolean value) {
		this.invul = value;
	}

	public int charges() {
		return charges;
	}

	public void charges(int value) {
		this.charges = Math.max(0, value);
	}

	public boolean invis() {
		return invis;
	}

	public void invis(boolean value) {
		this.invis = value;
	}

	public boolean silence() {
		return silence;
	}

	public void silence(boolean value) {
		this.silence = value;
	}

	public boolean diet() {
		return diet;
	}

	public void diet(boolean value) {
		this.diet = value;
	}

	public int polyNpcId() {
		return polyNpcId;
	}

	public void polyNpcId(int value) {
		this.polyNpcId = value;
	}

	public void isDisabled(boolean val) {
		this.disabledUntil = val ? Long.MAX_VALUE : 0;
	}

	private int armorSetChestId;

	public int armorSetChestId() {
		return armorSetChestId;
	}

	public void setArmorSetBonus(int chestId, List<StatFunc> funcs) {
		this.armorSetChestId = chestId;
		armorSetFuncs(funcs);
	}

	public void clearArmorSetBonus() {
		this.armorSetChestId = 0;
		armorSetFuncs(List.of());
	}

	public int getHenna(int slot) {
		if (slot >= 1 && slot <= 3) {
			return hennas[slot - 1];
		}
		return 0;
	}

	public void setHenna(int slot, int symbolId) {
		if (slot >= 1 && slot <= 3) {
			hennas[slot - 1] = symbolId;
		}
	}

	public int[] hennas() {
		return hennas;
	}

	public int hennaINT() { return hennaInt; }
	public int hennaSTR() { return hennaStr; }
	public int hennaCON() { return hennaCon; }
	public int hennaMEN() { return hennaMen; }
	public int hennaDEX() { return hennaDex; }
	public int hennaWIT() { return hennaWit; }

	public void recalcHennaStats(com.lopez.l2j.game.henna.HennaTable table) {
		hennaInt = 0;
		hennaStr = 0;
		hennaCon = 0;
		hennaMen = 0;
		hennaDex = 0;
		hennaWit = 0;

		if (table == null) {
			return;
		}

		for (int symbolId : hennas) {
			if (symbolId > 0) {
				var h = table.get(symbolId);
				if (h != null) {
					hennaInt += h.statInt();
					hennaStr += h.statStr();
					hennaCon += h.statCon();
					hennaMen += h.statMen();
					hennaDex += h.statDex();
					hennaWit += h.statWit();
				}
			}
		}

		// Regra oficial do Lineage II Interlude: bonus positivo limitado a no maximo +5
		hennaInt = Math.min(5, hennaInt);
		hennaStr = Math.min(5, hennaStr);
		hennaCon = Math.min(5, hennaCon);
		hennaMen = Math.min(5, hennaMen);
		hennaDex = Math.min(5, hennaDex);
		hennaWit = Math.min(5, hennaWit);
	}

	public int recomHave() {
		return recomHave;
	}

	public void recomHave(int value) {
		this.recomHave = Math.clamp(value, 0, 255);
	}

	public int recomLeft() {
		return recomLeft;
	}

	public void recomLeft(int value) {
		this.recomLeft = Math.max(0, value);
	}

	public long lastRecomDate() {
		return lastRecomDate;
	}

	public void lastRecomDate(long value) {
		this.lastRecomDate = value;
	}

	public Set<Integer> recommendedToday() {
		return recommendedToday;
	}

	public Set<String> blockList() {
		return blockList;
	}

	public boolean isBlockingAll() {
		return blockingAll;
	}

	public void setBlockingAll(boolean value) {
		this.blockingAll = value;
	}

	public boolean isBlocked(String name) {
		if (blockingAll) {
			return true;
		}
		return name != null && blockList.contains(name.toLowerCase(Locale.ROOT));
	}

	private com.lopez.l2j.game.dressme.DressMeEntry dressMeArmor;
	private com.lopez.l2j.game.dressme.DressMeEntry dressMeWeapon;
	private volatile boolean dressMeEnabled;

	public com.lopez.l2j.game.dressme.DressMeEntry dressMeArmor() {
		return dressMeArmor;
	}

	public void dressMeArmor(com.lopez.l2j.game.dressme.DressMeEntry entry) {
		this.dressMeArmor = entry;
	}

	public com.lopez.l2j.game.dressme.DressMeEntry dressMeWeapon() {
		return dressMeWeapon;
	}

	public void dressMeWeapon(com.lopez.l2j.game.dressme.DressMeEntry entry) {
		this.dressMeWeapon = entry;
	}

	public boolean isDressMe() {
		return dressMeEnabled;
	}

	public void dressMeEnabled(boolean value) {
		this.dressMeEnabled = value;
	}

	private volatile int privateStoreType;
	private volatile String storeTitle;
	private volatile boolean buffShop;

	public int privateStoreType() {
		return privateStoreType;
	}

	public void privateStoreType(int privateStoreType) {
		this.privateStoreType = privateStoreType;
	}

	public String storeTitle() {
		return storeTitle;
	}

	public void storeTitle(String storeTitle) {
		this.storeTitle = storeTitle;
	}

	public boolean isBuffShop() {
		return buffShop;
	}

	public void setBuffShop(boolean buffShop) {
		this.buffShop = buffShop;
	}

	private volatile boolean fishing;
	private volatile int fishX;
	private volatile int fishY;
	private volatile int fishZ;

	public boolean isFishing() {
		return fishing;
	}

	public void isFishing(boolean fishing) {
		this.fishing = fishing;
	}

	public int fishX() {
		return fishX;
	}

	public int fishY() {
		return fishY;
	}

	public int fishZ() {
		return fishZ;
	}

	public void setFishCoordinates(int x, int y, int z) {
		this.fishX = x;
		this.fishY = y;
		this.fishZ = z;
	}

	private volatile int petObjectId;
	private volatile int mountNpcId;

	public int petObjectId() {
		return petObjectId;
	}

	public void petObjectId(int petObjectId) {
		this.petObjectId = petObjectId;
	}

	public boolean hasPet() {
		return petObjectId > 0;
	}

	public int mountNpcId() {
		return mountNpcId;
	}

	public void mountNpcId(int mountNpcId) {
		this.mountNpcId = mountNpcId;
	}

	public boolean isMounted() {
		return mountNpcId > 0;
	}

	private volatile int pvpFlag;
	private volatile long pvpFlagEndTime;

	public int pvpFlag() {
		return pvpFlag;
	}

	public void pvpFlag(int pvpFlag) {
		this.pvpFlag = pvpFlag;
	}

	public long pvpFlagEndTime() {
		return pvpFlagEndTime;
	}

	public void pvpFlagEndTime(long pvpFlagEndTime) {
		this.pvpFlagEndTime = pvpFlagEndTime;
	}
}
