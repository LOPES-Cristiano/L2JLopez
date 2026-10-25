package com.lopez.l2j.game.summon;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.pet.PetDataTable;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.pet.PetStatTemplate;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Instância viva no mundo de um Pet (Wolf, Hatchling, Strider, Baby Pet, Sin Eater)
 * ou Servitor (Summon de Warlock, Elemental Summoner, Phantom Summoner, Necromancer, Crafter).
 * Implementa {@link Creature} para participar nativamente de {@code CombatService},
 * {@code GeoEngine}, {@code GameWorld} e handlers de skills.
 */
public final class SummonInstance implements Creature {

	public static final int TYPE_PET = 1;
	public static final int TYPE_SERVITOR = 2;

	// Constantes de tempo oficiais Interlude (20 minutos)
	public static final long PET_CORPSE_DECAY_MS = 1_200_000L;
	public static final long DEFAULT_SERVITOR_LIFETIME_MS = 1_200_000L;

	// IDs de Beast Shots oficiais
	public static final int BEAST_SOULSHOT_ID = 6645;
	public static final int BEAST_SPIRITSHOT_ID = 6646;
	public static final int BLESSED_BEAST_SPIRITSHOT_ID = 6647;

	// IDs de Cristais e Gemstones consumidos por Servitors e Siege Summons
	public static final int CRYSTAL_D = 1458;
	public static final int CRYSTAL_C = 1459;
	public static final int CRYSTAL_B = 1460;
	public static final int CRYSTAL_A = 1461;
	public static final int CRYSTAL_S = 1462;
	public static final int GEMSTONE_C = 2131;
	public static final int GEMSTONE_B = 2132;
	public static final int SIEGE_CANNONBALL_ID = 6037;

	// IDs de Siege Summons e Sin Eater
	public static final int SIN_EATER_ID = 12564;
	public static final int SIEGE_GOLEM_ID = 14737;
	public static final int WILD_HOG_CANNON_ID = 14768;
	public static final int SWOOP_CANNON_ID = 14839;
	public static final int SWOOP_CANNON_ALT_ID = 14801;

	// Penalidades de EXP oficiais de Servitors
	public static final float DEFAULT_SERVITOR_EXP_PENALTY = 0.30f;
	public static final float DARK_PANTHER_EXP_PENALTY = 0.90f;
	public static final float THIRD_CLASS_SERVITOR_EXP_PENALTY = 0.0f;
	public static final float SIN_EATER_EXP_PENALTY = 1.00f;

	// Transfer Pain (Skill 1262)
	public static final int TRANSFER_PAIN_SKILL_ID = 1262;
	public static final int TRANSFER_PAIN_RANGE = 1000;

	public static final List<String> SIN_EATER_CURSES = List.of(
			"Ei, mestre! Nao esqueca que seus pecados me alimentam!",
			"Que fome... Vamos matar mais alguem?",
			"O peso do seu Karma e delicioso!",
			"Voce acha mesmo que vai escapar do seu destino?",
			"Mais sangue! Eu preciso crescer!"
	);

	private static final ScheduledExecutorService SUMMON_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
		Thread t = new Thread(r, "summon-ai-scheduler");
		t.setDaemon(true);
		return t;
	});

	private final int objectId;
	private final PlayerCharacter owner;
	private final NpcTemplate template;
	private final int summonType;
	private final int itemObjectId;
	private final int referenceSkillId;

	private volatile String name;
	private volatile String title;
	private volatile int level;

	private volatile int x;
	private volatile int y;
	private volatile int z;
	private volatile int heading;
	private volatile int instanceId;

	private volatile double currentHp;
	private volatile double currentMp;
	private volatile int maxHp;
	private volatile int maxMp;

	private volatile int pAtk;
	private volatile int pDef;
	private volatile int mAtk;
	private volatile int mDef;
	private volatile int pAtkSpd;
	private volatile int mAtkSpd;
	private volatile int runSpd;
	private volatile int walkSpd;
	private volatile int accuracy;
	private volatile int evasion;
	private volatile int critical;
	private volatile double critDamageMul = 1.0;
	private volatile double critDamageAdd = 0.0;
	private volatile double mCritRate = 80.0;
	private volatile double shldRate = 0.0;
	private volatile int shldDef = 0;
	private volatile double absorbDam;

	private volatile long exp;
	private volatile long expThisLevel;
	private volatile long expNextLevel = 1000L;
	private volatile int sp;
	private volatile int currentFed;
	private volatile int maxFed;
	private volatile int feedBattle = 2;
	private volatile int feedNormal = 1;
	private volatile int maxLoad = 54000;
	private volatile int baseHpRegen = 2;
	private volatile int baseMpRegen = 1;
	private volatile float expPenalty;

	private volatile boolean following = true;
	private volatile boolean running = true;
	private volatile int targetObjectId;
	private volatile long combatStanceUntil;
	private volatile boolean invul;
	private volatile int abnormalEffect;

	private volatile long disabledUntil;
	private volatile boolean sleeping;
	private volatile long rootedUntil;
	private volatile long mutedUntil;
	private volatile long physMutedUntil;
	private volatile boolean betrayed;
	private volatile long betrayedUntil;

	private volatile boolean soulshotCharged;
	private volatile int spiritshotCharged; // 0=none, 1=spiritshot, 2=blessed
	private volatile boolean autoSoulShot = true;
	private volatile boolean autoSpiritShot = true;
	private volatile int soulShotsPerHit = 1;
	private volatile int spiritShotsPerHit = 1;

	private volatile int baseMaxHp;
	private volatile int baseMaxMp;
	private volatile int basePAtk;
	private volatile int basePDef;
	private volatile int baseMAtk;
	private volatile int baseMDef;
	private volatile int basePAtkSpd;
	private volatile int baseMAtkSpd;
	private volatile int baseRunSpd;
	private volatile int baseWalkSpd;
	private volatile int baseAccuracy;
	private volatile int baseEvasion;
	private volatile int baseCritical;

	// Inventario e equipamentos do Pet (#236, #237)
	private final Inventory petInventory;
	private volatile int equippedWeaponId;
	private volatile int equippedArmorId;
	private volatile int equippedJewelId;
	private volatile int weaponPAtkBonus;
	private volatile int weaponMAtkBonus;
	private volatile int armorPDefBonus;
	private volatile int armorMDefBonus;

	// Tempo de vida e consumo de cristais de Servitors / Siege Summons (#253, #254)
	private volatile long totalLifeTimeMs = DEFAULT_SERVITOR_LIFETIME_MS;
	private volatile long remainingLifeTimeMs = DEFAULT_SERVITOR_LIFETIME_MS;
	private volatile int itemConsumeIdOT;
	private volatile int itemConsumeCountOT;
	private volatile long itemConsumePeriodMs = 60_000L;
	private volatile long timeSinceLastCrystalConsumeMs;

	// Siege Summon flags (#254)
	private volatile boolean healBlocked;
	private volatile boolean siegeMode;

	// Morte e Decay de 20 minutos (#240)
	private volatile long deathTimeMillis;

	// Tasks periodicas
	private volatile ScheduledFuture<?> feedTask;
	private volatile ScheduledFuture<?> decayTask;
	private volatile ScheduledFuture<?> lifeTimeTask;
	private volatile ScheduledFuture<?> followTask;
	private volatile ScheduledFuture<?> buffTickTask;

	public record SummonBuff(
			int skillId,
			int level,
			String stackType,
			int stackOrder,
			long endTimeMillis,
			List<StatFunc> funcs,
			String effectName,
			int periodSeconds,
			double tickValue) {

		public SummonBuff(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs) {
			this(skillId, level, stackType, stackOrder, endTimeMillis, funcs, "Buff", 0, 0.0);
		}
	}

	private final Map<String, SummonBuff> activeBuffs = new ConcurrentHashMap<>();

	public SummonInstance(
			int objectId,
			PlayerCharacter owner,
			NpcTemplate template,
			int summonType,
			int itemObjectId,
			int referenceSkillId,
			int level) {
		this.objectId = objectId;
		this.owner = owner;
		this.template = template != null ? template : NpcTemplate.fallback(12077, "Summon", "L2Summon");
		this.summonType = summonType;
		this.itemObjectId = itemObjectId;
		this.referenceSkillId = referenceSkillId;
		this.level = Math.max(1, level > 0 ? level : this.template.level());
		this.name = this.template.name();
		this.title = owner != null ? owner.name() : "";
		this.petInventory = new Inventory(objectId);

		if (owner != null) {
			this.x = owner.x() + 40;
			this.y = owner.y() + 40;
			this.z = owner.z();
			this.heading = owner.heading();
			this.instanceId = owner.instanceId();
		}

		int scaleLv = Math.max(1, this.level);
		PetStatTemplate petStat = null;
		if (summonType == TYPE_PET && PetDataTable.getInstance() != null) {
			petStat = PetDataTable.getInstance().getStat(this.template.id(), scaleLv).orElse(null);
		}

		if (petStat != null) {
			applyPetStatTemplateInternal(petStat, true);
		} else {
			this.baseMaxHp = Math.max(100, this.template.maxHp() > 0 ? this.template.maxHp() : 200 + scaleLv * 45);
			this.baseMaxMp = Math.max(50, this.template.maxMp() > 0 ? this.template.maxMp() : 100 + scaleLv * 22);
			this.basePAtk = Math.max(15, this.template.pAtk() > 0 ? this.template.pAtk() : 25 + scaleLv * 12);
			this.basePDef = Math.max(20, this.template.pDef() > 0 ? this.template.pDef() : 35 + scaleLv * 8);
			this.baseMAtk = Math.max(15, this.template.mAtk() > 0 ? this.template.mAtk() : 20 + scaleLv * 10);
			this.baseMDef = Math.max(20, this.template.mDef() > 0 ? this.template.mDef() : 30 + scaleLv * 7);
			this.basePAtkSpd = Math.max(200, this.template.pAtkSpd() > 0 ? this.template.pAtkSpd() : 278);
			this.baseMAtkSpd = Math.max(200, this.template.mAtkSpd() > 0 ? this.template.mAtkSpd() : 333);
			this.baseRunSpd = Math.max(80, this.template.runSpd() > 0 ? this.template.runSpd() : 125);
			this.baseWalkSpd = Math.max(40, this.template.walkSpd() > 0 ? this.template.walkSpd() : 80);
			this.baseAccuracy = 30 + scaleLv;
			this.baseEvasion = 30 + scaleLv;
			this.baseCritical = 44;
			this.maxFed = summonType == TYPE_PET ? 1200 : 1000;
			this.currentFed = this.maxFed;
			this.expPenalty = resolveDefaultExpPenalty(this.template.id(), referenceSkillId, summonType);
			this.maxHp = this.baseMaxHp;
			this.maxMp = this.baseMaxMp;
			this.currentHp = this.maxHp;
			this.currentMp = this.maxMp;
		}

		this.healBlocked = isSiegeSummonNpc(this.template.id());
		configureDefaultCrystalConsumption();
		recalculateStats();
	}

	private void applyPetStatTemplateInternal(PetStatTemplate stat, boolean resetVitals) {
		if (stat == null) {
			return;
		}
		this.level = Math.max(1, stat.level());
		this.baseMaxHp = Math.max(1, stat.hpMax());
		this.baseMaxMp = Math.max(1, stat.mpMax());
		this.basePAtk = Math.max(1, stat.pAtk());
		this.basePDef = Math.max(1, stat.pDef());
		this.baseMAtk = Math.max(1, stat.mAtk());
		this.baseMDef = Math.max(1, stat.mDef());
		this.baseAccuracy = stat.accuracy() > 0 ? stat.accuracy() : (30 + this.level);
		this.baseEvasion = stat.evasion() > 0 ? stat.evasion() : (30 + this.level);
		this.baseCritical = stat.critical() > 0 ? stat.critical() : 44;
		this.baseRunSpd = Math.max(10, stat.speed() > 0 ? stat.speed() : 125);
		this.baseWalkSpd = Math.max(10, this.baseRunSpd * 2 / 3);
		this.basePAtkSpd = Math.max(100, stat.atkSpeed() > 0 ? stat.atkSpeed() : 278);
		this.baseMAtkSpd = Math.max(100, stat.castSpeed() > 0 ? stat.castSpeed() : 333);
		this.maxFed = Math.max(1, stat.feedMax());
		this.feedBattle = Math.max(1, stat.feedBattle());
		this.feedNormal = Math.max(1, stat.feedNormal());
		this.maxLoad = stat.loadMax() > 0 ? stat.loadMax() : 54000;
		this.baseHpRegen = Math.max(1, stat.hpRegen());
		this.baseMpRegen = Math.max(1, stat.mpRegen());
		this.expNextLevel = Math.max(1000L, stat.expMax());
		if (this.template.id() == SIN_EATER_ID) {
			this.expPenalty = SIN_EATER_EXP_PENALTY;
		} else {
			this.expPenalty = (float) Math.clamp(stat.ownerExpTaken() > 1.0 ? stat.ownerExpTaken() / 100.0 : stat.ownerExpTaken(), 0.0, 1.0);
		}
		if (resetVitals) {
			this.maxHp = this.baseMaxHp;
			this.maxMp = this.baseMaxMp;
			this.currentHp = this.maxHp;
			this.currentMp = this.maxMp;
			this.currentFed = this.maxFed;
		}
	}

	/**
	 * Aplica o template de atributos por nivel do {@link PetDataTable} e recalcula os status ativos.
	 */
	public synchronized void applyPetStatTemplate(PetStatTemplate stat) {
		if (stat == null) {
			return;
		}
		applyPetStatTemplateInternal(stat, false);
		recalculateStats();
	}

	public static float resolveDefaultExpPenalty(int npcId, int referenceSkillId, int summonType) {
		if (npcId == SIN_EATER_ID) {
			return SIN_EATER_EXP_PENALTY;
		}
		if (summonType == TYPE_PET) {
			return 0.10f;
		}
		// 3ª Classe e Servitors de Party Buff: 0.0f
		if (referenceSkillId == 1331 || referenceSkillId == 1332 || referenceSkillId == 1333
				|| referenceSkillId == 1406 || referenceSkillId == 1407 || referenceSkillId == 1408) {
			return THIRD_CLASS_SERVITOR_EXP_PENALTY;
		}
		if ((npcId >= 14251 && npcId <= 14294)
				|| (npcId >= 14435 && npcId <= 14478)
				|| (npcId >= 14619 && npcId <= 14662)
				|| (npcId >= 14835 && npcId <= 14879)) {
			if (!isSiegeSummonNpc(npcId)) {
				return THIRD_CLASS_SERVITOR_EXP_PENALTY;
			}
		}
		// Dark Panther (Skill 283, NPC 14799..14835) e Summons de Necromancer (Reanimated/Corrupted/Cursed Man): 0.90f
		if (referenceSkillId == 283 || referenceSkillId == 1129 || referenceSkillId == 1154 || referenceSkillId == 1334
				|| (npcId >= 14799 && npcId <= 14834)
				|| (npcId >= 14001 && npcId <= 14110)
				|| npcId == 14918) {
			return DARK_PANTHER_EXP_PENALTY;
		}
		if (isSiegeSummonNpc(npcId)) {
			return 0.0f;
		}
		return DEFAULT_SERVITOR_EXP_PENALTY;
	}

	private void configureDefaultCrystalConsumption() {
		if (summonType != TYPE_SERVITOR) {
			return;
		}
		int id = this.template.id();
		if (isSiegeGolem()) {
			this.itemConsumeIdOT = GEMSTONE_C;
			this.itemConsumeCountOT = 70;
			this.itemConsumePeriodMs = 60_000L;
		} else if (isWildHogCannon()) {
			this.itemConsumeIdOT = GEMSTONE_B;
			this.itemConsumeCountOT = 30;
			this.itemConsumePeriodMs = 60_000L;
		} else if (isSwoopCannon()) {
			this.itemConsumeIdOT = GEMSTONE_B;
			this.itemConsumeCountOT = 27;
			this.itemConsumePeriodMs = 60_000L;
		} else if (this.level >= 76 || referenceSkillId >= 1406) {
			this.itemConsumeIdOT = CRYSTAL_A;
			this.itemConsumeCountOT = 1;
			this.itemConsumePeriodMs = 60_000L;
		} else if (this.level >= 61) {
			this.itemConsumeIdOT = CRYSTAL_B;
			this.itemConsumeCountOT = 1;
			this.itemConsumePeriodMs = 60_000L;
		} else if (this.level >= 40) {
			this.itemConsumeIdOT = CRYSTAL_C;
			this.itemConsumeCountOT = 1;
			this.itemConsumePeriodMs = 60_000L;
		} else {
			this.itemConsumeIdOT = CRYSTAL_D;
			this.itemConsumeCountOT = 1;
			this.itemConsumePeriodMs = 60_000L;
		}
	}

	@Override
	public int objectId() {
		return objectId;
	}

	public PlayerCharacter owner() {
		return owner;
	}

	public int ownerId() {
		return owner != null ? owner.objectId() : 0;
	}

	public NpcTemplate template() {
		return template;
	}

	public int npcId() {
		return template.id();
	}

	public int idTemplate() {
		return template.idTemplate() > 0 ? template.idTemplate() : template.id();
	}

	public int summonType() {
		return summonType;
	}

	public boolean isPet() {
		return summonType == TYPE_PET;
	}

	public boolean isServitor() {
		return summonType == TYPE_SERVITOR;
	}

	public boolean isSinEater() {
		return npcId() == SIN_EATER_ID;
	}

	public static boolean isSiegeSummonNpc(int npcId) {
		return (npcId >= 14737 && npcId <= 14798)
				|| (npcId >= 14839 && npcId <= 14869)
				|| npcId == SWOOP_CANNON_ALT_ID;
	}

	public boolean isSiegeSummon() {
		return isSiegeSummonNpc(npcId()) || referenceSkillId == 13 || referenceSkillId == 299 || referenceSkillId == 448;
	}

	public boolean isSiegeGolem() {
		int id = npcId();
		return (id >= 14737 && id <= 14767) || referenceSkillId == 13;
	}

	public boolean isWildHogCannon() {
		int id = npcId();
		return (id >= 14768 && id <= 14798) || referenceSkillId == 299;
	}

	public boolean isSwoopCannon() {
		int id = npcId();
		return (id >= 14839 && id <= 14869) || id == SWOOP_CANNON_ALT_ID || referenceSkillId == 448;
	}

	public int itemObjectId() {
		return itemObjectId;
	}

	public int referenceSkillId() {
		return referenceSkillId;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value != null ? value : "";
	}

	public String title() {
		return title;
	}

	public void title(String value) {
		this.title = value != null ? value : "";
	}

	@Override
	public int level() {
		return level;
	}

	public void level(int value) {
		this.level = Math.max(1, value);
		if (summonType == TYPE_PET && PetDataTable.getInstance() != null) {
			PetDataTable.getInstance().getStat(npcId(), this.level).ifPresent(this::applyPetStatTemplate);
		}
	}

	@Override
	public int x() {
		return x;
	}

	@Override
	public void x(int value) {
		this.x = value;
	}

	@Override
	public int y() {
		return y;
	}

	@Override
	public void y(int value) {
		this.y = value;
	}

	@Override
	public int z() {
		return z;
	}

	@Override
	public void z(int value) {
		this.z = value;
	}

	@Override
	public int heading() {
		return heading;
	}

	@Override
	public void heading(int value) {
		this.heading = value;
	}

	@Override
	public int instanceId() {
		return instanceId;
	}

	@Override
	public void instanceId(int value) {
		this.instanceId = Math.max(0, value);
	}

	@Override
	public double currentHp() {
		return currentHp;
	}

	@Override
	public void currentHp(double value) {
		this.currentHp = Math.max(0, Math.min(maxHp, value));
	}

	@Override
	public double currentMp() {
		return currentMp;
	}

	@Override
	public void currentMp(double value) {
		this.currentMp = Math.max(0, Math.min(maxMp, value));
	}

	public int maxHp() {
		return maxHp;
	}

	public void maxHp(int value) {
		this.maxHp = Math.max(1, value);
	}

	public int maxMp() {
		return maxMp;
	}

	public void maxMp(int value) {
		this.maxMp = Math.max(1, value);
	}

	@Override
	public double maxHpValue() {
		return maxHp;
	}

	@Override
	public double maxMpValue() {
		return maxMp;
	}

	public int pAtk() {
		return pAtk;
	}

	public void pAtk(int value) {
		this.pAtk = Math.max(1, value);
	}

	public int pDef() {
		return pDef;
	}

	public void pDef(int value) {
		this.pDef = Math.max(1, value);
	}

	public int mAtk() {
		return mAtk;
	}

	public void mAtk(int value) {
		this.mAtk = Math.max(1, value);
	}

	public int mDef() {
		return mDef;
	}

	public void mDef(int value) {
		this.mDef = Math.max(1, value);
	}

	public int pAtkSpd() {
		return pAtkSpd;
	}

	public void pAtkSpd(int value) {
		this.pAtkSpd = Math.max(1, value);
	}

	public int mAtkSpd() {
		return mAtkSpd;
	}

	public void mAtkSpd(int value) {
		this.mAtkSpd = Math.max(1, value);
	}

	public int runSpd() {
		return runSpd;
	}

	public void runSpd(int value) {
		this.runSpd = Math.max(1, value);
	}

	public int walkSpd() {
		return walkSpd;
	}

	public void walkSpd(int value) {
		this.walkSpd = Math.max(1, value);
	}

	public double absorbDam() {
		return absorbDam;
	}

	public void absorbDam(double value) {
		this.absorbDam = Math.max(0.0, value);
	}

	public int accuracy() {
		return accuracy > 0 ? accuracy : (30 + level);
	}

	public void accuracy(int value) {
		this.accuracy = Math.max(1, value);
	}

	public int evasion() {
		return evasion > 0 ? evasion : (30 + level);
	}

	public void evasion(int value) {
		this.evasion = Math.max(1, value);
	}

	public int critical() {
		return critical > 0 ? critical : 44;
	}

	public void critical(int value) {
		this.critical = Math.max(1, value);
	}

	public double critDamageMul() {
		return critDamageMul;
	}

	public double critDamageAdd() {
		return critDamageAdd;
	}

	public double mCritRate() {
		return mCritRate;
	}

	public double shldRate() {
		return shldRate;
	}

	public int shldDef() {
		return shldDef;
	}

	public long exp() {
		return exp;
	}

	public void exp(long value) {
		this.exp = Math.max(0L, value);
	}

	public long expThisLevel() {
		return expThisLevel;
	}

	public void expThisLevel(long value) {
		this.expThisLevel = Math.max(0L, value);
	}

	public long expNextLevel() {
		return expNextLevel;
	}

	public void expNextLevel(long value) {
		this.expNextLevel = Math.max(1L, value);
	}

	public int sp() {
		return sp;
	}

	public void sp(int value) {
		this.sp = Math.max(0, value);
	}

	public int currentFed() {
		return currentFed;
	}

	public void currentFed(int value) {
		int prev = this.currentFed;
		this.currentFed = Math.max(0, Math.min(maxFed, value));
		if ((prev <= 0 && this.currentFed > 0) || (prev > 0 && this.currentFed <= 0)) {
			recalculateStats();
		}
	}

	public int maxFed() {
		return maxFed;
	}

	public void maxFed(int value) {
		this.maxFed = Math.max(1, value);
	}

	public int feedBattle() {
		return feedBattle;
	}

	public void feedBattle(int value) {
		this.feedBattle = Math.max(1, value);
	}

	public int feedNormal() {
		return feedNormal;
	}

	public void feedNormal(int value) {
		this.feedNormal = Math.max(1, value);
	}

	public boolean isHungry() {
		return isPet() && currentFed <= 0;
	}

	public boolean isNeedAutoFeed() {
		return isPet() && currentFed < (maxFed * 55 / 100);
	}

	public Inventory petInventory() {
		return petInventory;
	}

	public Inventory inventory() {
		return petInventory;
	}

	public int currentLoad() {
		return petInventory.currentLoad();
	}

	public int maxLoad() {
		return maxLoad;
	}

	public void maxLoad(int value) {
		this.maxLoad = Math.max(1, value);
	}

	public int equippedWeaponId() {
		return equippedWeaponId;
	}

	public void equippedWeaponId(int itemId) {
		this.equippedWeaponId = Math.max(0, itemId);
		recalculateStats();
	}

	public int equippedArmorId() {
		return equippedArmorId;
	}

	public void equippedArmorId(int itemId) {
		this.equippedArmorId = Math.max(0, itemId);
		recalculateStats();
	}

	public int equippedJewelId() {
		return equippedJewelId;
	}

	public void equippedJewelId(int itemId) {
		this.equippedJewelId = Math.max(0, itemId);
		recalculateStats();
	}

	public void setEquipmentBonuses(int weaponPAtk, int weaponMAtk, int armorPDef, int armorMDef) {
		this.weaponPAtkBonus = Math.max(0, weaponPAtk);
		this.weaponMAtkBonus = Math.max(0, weaponMAtk);
		this.armorPDefBonus = Math.max(0, armorPDef);
		this.armorMDefBonus = Math.max(0, armorMDef);
		recalculateStats();
	}

	public boolean following() {
		return following;
	}

	public void following(boolean value) {
		this.following = value;
	}

	public boolean running() {
		return running;
	}

	public void running(boolean value) {
		this.running = value;
	}

	public int targetObjectId() {
		return targetObjectId;
	}

	public void targetObjectId(int value) {
		this.targetObjectId = value;
	}

	@Override
	public boolean isDead() {
		return currentHp <= 0;
	}

	public void markCombat() {
		this.combatStanceUntil = System.currentTimeMillis() + 15_000L;
	}

	@Override
	public boolean isInCombat() {
		return System.currentTimeMillis() < combatStanceUntil;
	}

	@Override
	public boolean invul() {
		return invul;
	}

	@Override
	public void invul(boolean value) {
		this.invul = value;
	}

	@Override
	public int abnormalEffect() {
		return abnormalEffect;
	}

	@Override
	public void startAbnormalEffect(int mask) {
		this.abnormalEffect |= mask;
	}

	@Override
	public void stopAbnormalEffect(int mask) {
		this.abnormalEffect &= ~mask;
	}

	@Override
	public void disable(long until, boolean sleep) {
		this.disabledUntil = Math.max(this.disabledUntil, until);
		if (sleep) {
			this.sleeping = true;
		}
	}

	@Override
	public void root(long until) {
		this.rootedUntil = Math.max(this.rootedUntil, until);
	}

	@Override
	public void mute(long until) {
		this.mutedUntil = Math.max(this.mutedUntil, until);
	}

	@Override
	public void physicalMute(long until) {
		this.physMutedUntil = Math.max(this.physMutedUntil, until);
	}

	public void betray(long until) {
		this.betrayed = true;
		this.betrayedUntil = Math.max(this.betrayedUntil, until);
	}

	public boolean isBetrayed() {
		if (!betrayed) {
			return false;
		}
		if (System.currentTimeMillis() >= betrayedUntil) {
			betrayed = false;
			return false;
		}
		return true;
	}

	@Override
	public boolean isDisabled() {
		return System.currentTimeMillis() < disabledUntil;
	}

	@Override
	public boolean isRooted() {
		return siegeMode || System.currentTimeMillis() < rootedUntil;
	}

	@Override
	public boolean isMuted() {
		return System.currentTimeMillis() < mutedUntil;
	}

	@Override
	public boolean isPhysicalMuted() {
		return System.currentTimeMillis() < physMutedUntil;
	}

	@Override
	public void onDamaged() {
		markCombat();
		if (sleeping) {
			sleeping = false;
			disabledUntil = 0L;
			stopAbnormalEffect(0x0080);
		}
	}

	@Override
	public void moveTo(int nx, int ny, int nz) {
		if (siegeMode) {
			return;
		}
		int dx = nx - this.x;
		int dy = ny - this.y;
		if (dx != 0 || dy != 0) {
			this.heading = (int) (Math.atan2(-dx, -dy) * 10430.378350470453) + 32768;
		}
		this.x = nx;
		this.y = ny;
		this.z = nz;
	}

	public boolean applyDamage(int damage) {
		if (invul || isDead() || damage <= 0) {
			return false;
		}
		onDamaged();
		this.currentHp = Math.max(0.0, this.currentHp - damage);
		if (this.currentHp <= 0.0) {
			doDie();
			return true;
		}
		broadcastStatusUpdate();
		return false;
	}

	/**
	 * Processa a morte do Pet/Servitor, agendando a DecayTask de 20 minutos (1.200.000 ms) para Pets (#240).
	 */
	public synchronized void doDie() {
		this.currentHp = 0.0;
		this.deathTimeMillis = System.currentTimeMillis();
		stopFeedTask();
		stopLifeTimeTask();
		stopFollowTask();

		// Noblesse Blessing (skill 1323) preserva os buffs na morte
		boolean hasNoblesse = activeBuffs.values().stream()
				.anyMatch(b -> b.skillId() == 1323 || "noblesse_blessing".equalsIgnoreCase(b.stackType())
						|| "NoblesseBless".equalsIgnoreCase(b.effectName()));
		if (hasNoblesse) {
			activeBuffs.values().removeIf(b -> b.skillId() == 1323
					|| "noblesse_blessing".equalsIgnoreCase(b.stackType())
					|| "NoblesseBless".equalsIgnoreCase(b.effectName()));
			recalculateStats();
		} else {
			clearBuffs();
		}

		if (isPet()) {
			startDecayTask();
		}
		broadcastStatusUpdate();
	}

	public long deathTimeMillis() {
		return deathTimeMillis;
	}

	public void startDecayTask() {
		stopDecayTask();
		this.decayTask = SUMMON_SCHEDULER.schedule(this::onCorpseDecay, PET_CORPSE_DECAY_MS, TimeUnit.MILLISECONDS);
	}

	public void stopDecayTask() {
		ScheduledFuture<?> task = this.decayTask;
		this.decayTask = null;
		if (task != null) {
			task.cancel(false);
		}
	}

	public boolean isDecayTaskActive() {
		return decayTask != null && !decayTask.isDone();
	}

	/**
	 * Executado quando o cadaver do Pet nao e ressuscitado em 20 minutos (1.200.000 ms).
	 */
	public synchronized void onCorpseDecay() {
		stopAllTasks();
		if (owner != null) {
			// Devolve itens do inventario do Pet para o dono antes de recolher
			if (owner.inventory() != null) {
				for (ItemInstance item : new ArrayList<>(petInventory.items())) {
					petInventory.remove(item);
					item.location(ItemInstance.Location.INVENTORY, 0);
					owner.inventory().addItem(item);
				}
			}
			if (owner.activeSummon() == this) {
				owner.activeSummon(null);
				owner.petObjectId(0);
			}
		}
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.removeSummon(this.objectId);
			world.broadcastAround(this.x, this.y, GameWorld.VISIBILITY_RADIUS, this.instanceId,
					new GameServerPacket.DeleteObject(this.objectId));
			if (owner != null) {
				world.sendPacket(owner.objectId(), new GameServerPacket.PetDelete(this.summonType, this.objectId));
			}
		}
	}

	/**
	 * Ressuscita o Pet morto, cancelando o timer de 20 minutos de decay (#219, #240).
	 */
	public synchronized boolean resurrect(double expRestorePercent) {
		if (!isDead()) {
			return false;
		}
		stopDecayTask();
		this.deathTimeMillis = 0L;
		this.currentHp = Math.max(1.0, this.maxHp * 0.70);
		this.currentMp = Math.max(1.0, this.maxMp * 0.70);
		if (isPet() && currentFed > 0) {
			startFeedTask();
		}
		startFollowTask();
		broadcastStatusUpdate();
		return true;
	}

	// ---- ANTI-HEAL & SIEGE SUMMON MECHANICS (#254) ----

	public boolean isHealBlocked() {
		return healBlocked || isSiegeSummon() || isDead();
	}

	public void setHealBlocked(boolean healBlocked) {
		this.healBlocked = healBlocked;
	}

	public boolean canBeHealed() {
		return !isHealBlocked();
	}

	public int applyHeal(double healAmount) {
		if (isHealBlocked() || healAmount <= 0) {
			return 0;
		}
		double before = this.currentHp;
		this.currentHp = Math.min(this.maxHp, this.currentHp + healAmount);
		int healed = (int) Math.round(this.currentHp - before);
		if (healed > 0) {
			broadcastStatusUpdate();
		}
		return healed;
	}

	public boolean isSiegeMode() {
		return siegeMode;
	}

	public void setSiegeMode(boolean siegeMode) {
		this.siegeMode = siegeMode;
	}

	/**
	 * Alterna o modo de disparo (Change Mode) do Wild Hog Cannon (14768).
	 */
	public boolean toggleWildHogCannonMode() {
		if (!isWildHogCannon()) {
			return false;
		}
		this.siegeMode = !this.siegeMode;
		if (this.siegeMode) {
			this.following = false;
		}
		return this.siegeMode;
	}

	/**
	 * Verifica e consome 1 Siege Cannonball (6037) do inventario do dono para disparo de canhao de cerco.
	 */
	public boolean consumeSiegeCannonball() {
		if (owner == null || owner.inventory() == null) {
			return false;
		}
		if (owner.inventory().getItemCount(SIEGE_CANNONBALL_ID) < 1) {
			return false;
		}
		return owner.inventory().destroyItemByItemId(SIEGE_CANNONBALL_ID, 1);
	}

	/**
	 * Valida se o Siege Summon continua em zona de cerco ativa; se nao estiver, recolhe o summon.
	 */
	public boolean validateSiegeZone() {
		if (!isSiegeSummon()) {
			return true;
		}
		if (owner == null || !owner.isInSiege()) {
			unsummon();
			return false;
		}
		return true;
	}

	// ---- BEAST SOULSHOT & SPIRITSHOT (#229, #247) ----

	public boolean soulshotCharged() {
		return soulshotCharged;
	}

	public void soulshotCharged(boolean value) {
		this.soulshotCharged = value;
	}

	public boolean isChargedSoulShot() {
		return soulshotCharged;
	}

	public void setChargedSoulShot(boolean value) {
		this.soulshotCharged = value;
	}

	public int spiritshotCharged() {
		return spiritshotCharged;
	}

	public void spiritshotCharged(int value) {
		this.spiritshotCharged = Math.clamp(value, 0, 2);
	}

	public int getChargedSpiritShot() {
		return spiritshotCharged;
	}

	public void setChargedSpiritShot(int value) {
		this.spiritshotCharged = Math.clamp(value, 0, 2);
	}

	public boolean isAutoSoulShot() {
		return autoSoulShot;
	}

	public void setAutoSoulShot(boolean autoSoulShot) {
		this.autoSoulShot = autoSoulShot;
	}

	public boolean isAutoSpiritShot() {
		return autoSpiritShot;
	}

	public void setAutoSpiritShot(boolean autoSpiritShot) {
		this.autoSpiritShot = autoSpiritShot;
	}

	public int soulShotsPerHit() {
		return soulShotsPerHit;
	}

	public void soulShotsPerHit(int count) {
		this.soulShotsPerHit = Math.max(1, count);
	}

	public int spiritShotsPerHit() {
		return spiritShotsPerHit;
	}

	public void spiritShotsPerHit(int count) {
		this.spiritShotsPerHit = Math.max(1, count);
	}

	/**
	 * Recarrega Beast Soulshot (6645) e Beast/Blessed Beast Spiritshot (6646/6647) do inventario do dono.
	 */
	public boolean rechargeShotsFromOwner(boolean needSoulshot, boolean needSpiritshot) {
		if (owner == null || owner.inventory() == null) {
			return false;
		}
		Inventory inv = owner.inventory();
		boolean recharged = false;
		if (needSoulshot && !this.soulshotCharged) {
			int needed = Math.max(1, this.soulShotsPerHit);
			if (inv.getItemCount(BEAST_SOULSHOT_ID) >= needed && inv.destroyItemByItemId(BEAST_SOULSHOT_ID, needed)) {
				this.soulshotCharged = true;
				recharged = true;
			}
		}
		if (needSpiritshot && this.spiritshotCharged == 0) {
			int needed = Math.max(1, this.spiritShotsPerHit);
			if (inv.getItemCount(BLESSED_BEAST_SPIRITSHOT_ID) >= needed
					&& inv.destroyItemByItemId(BLESSED_BEAST_SPIRITSHOT_ID, needed)) {
				this.spiritshotCharged = 2;
				recharged = true;
			} else if (inv.getItemCount(BEAST_SPIRITSHOT_ID) >= needed
					&& inv.destroyItemByItemId(BEAST_SPIRITSHOT_ID, needed)) {
				this.spiritshotCharged = 1;
				recharged = true;
			}
		}
		return recharged;
	}

	/**
	 * Consome o Beast Soulshot carregado para um ataque fisico (multiplicador 2.0x) e auto-recarrega se habilitado.
	 */
	public double consumeSoulShotAndRecharge() {
		if (!this.soulshotCharged && this.autoSoulShot) {
			rechargeShotsFromOwner(true, false);
		}
		if (this.soulshotCharged) {
			this.soulshotCharged = false;
			if (this.autoSoulShot) {
				rechargeShotsFromOwner(true, false);
			}
			return 2.0;
		}
		return 1.0;
	}

	/**
	 * Consome o Beast Spiritshot carregado para uma skill magica (1.0 = nenhum, 2.0 = spiritshot, 4.0 = blessed)
	 * e auto-recarrega do inventario do dono.
	 */
	public double consumeSpiritShotAndRecharge() {
		if (this.spiritshotCharged == 0 && this.autoSpiritShot) {
			rechargeShotsFromOwner(false, true);
		}
		int charged = this.spiritshotCharged;
		if (charged > 0) {
			this.spiritshotCharged = 0;
			if (this.autoSpiritShot) {
				rechargeShotsFromOwner(false, true);
			}
			return charged == 2 ? 4.0 : 2.0;
		}
		return 1.0;
	}

	// ---- EXP PENALTY & SIN EATER (#239, #245) ----

	public float expPenalty() {
		return expPenalty;
	}

	public void setExpPenalty(float expPenalty) {
		this.expPenalty = Math.clamp(expPenalty, 0.0f, 1.0f);
	}

	public double getEffectiveExpPenalty() {
		return getEffectiveExpPenalty(this.expPenalty);
	}

	public double getEffectiveExpPenalty(double baseExpPenalty) {
		return Math.clamp(baseExpPenalty * Math.max(0.0, Config.ALT_SUMMON_PENALTY_RATE), 0.0, 1.0);
	}

	public String triggerSinEaterCurse() {
		if (!isSinEater()) {
			return null;
		}
		String msg = SIN_EATER_CURSES.get(ThreadLocalRandom.current().nextInt(SIN_EATER_CURSES.size()));
		GameWorld world = GameWorld.getInstance();
		if (world != null && owner != null) {
			world.sendPacket(owner.objectId(), new GameServerPacket.CreatureSay(objectId, GameServerPacket.CreatureSay.ALL, name, msg));
		}
		return msg;
	}

	public double getEffectiveHpRegen(double baseHpRegen) {
		return baseHpRegen * (Config.PET_HP_REGEN_MULTIPLIER / 100.0);
	}

	public double getEffectiveMpRegen(double baseMpRegen) {
		return baseMpRegen * (Config.PET_MP_REGEN_MULTIPLIER / 100.0);
	}

	// ---- TRANSFER PAIN (#180, #183) ----

	public double calculateTransferPainPercent() {
		if (owner == null || isDead() || !isServitor()) {
			return 0.0;
		}
		int tpLvl = 0;
		if (owner.effects() != null && owner.effects().hasSkill(TRANSFER_PAIN_SKILL_ID)) {
			tpLvl = owner.effects().getSkillLevel(TRANSFER_PAIN_SKILL_ID);
		}
		if (tpLvl <= 0) {
			tpLvl = owner.skillLevel(TRANSFER_PAIN_SKILL_ID);
		}
		if (tpLvl <= 0) {
			return 0.0;
		}
		return Math.clamp(tpLvl * 0.10, 0.0, 0.70);
	}

	/**
	 * Redireciona parte do dano recebido pelo dono para este Servitor via Transfer Pain (Skill 1262),
	 * retornando o dano efetivamente absorvido pelo Servitor.
	 */
	public int divertTransferPainDamage(int incomingDamage) {
		if (incomingDamage <= 0 || owner == null || isDead() || !isServitor()) {
			return 0;
		}
		long dx = (long) owner.x() - this.x;
		long dy = (long) owner.y() - this.y;
		if (dx * dx + dy * dy > (long) TRANSFER_PAIN_RANGE * TRANSFER_PAIN_RANGE) {
			return 0;
		}
		double ratio = calculateTransferPainPercent();
		if (ratio <= 0.0) {
			return 0;
		}
		int transferred = (int) Math.min(this.currentHp, Math.round(incomingDamage * ratio));
		if (transferred > 0) {
			applyDamage(transferred);
		}
		return transferred;
	}

	// ---- HUNGER / FEED TASK (#234, #235) ----

	public void startFeedTask() {
		startFeedTask(PetService.getInstance());
	}

	public synchronized void startFeedTask(PetService petService) {
		stopFeedTask();
		if (!isPet()) {
			return;
		}
		this.feedTask = SUMMON_SCHEDULER.scheduleAtFixedRate(() -> {
			try {
				tickHunger(petService);
			} catch (Exception ignored) {
			}
		}, 10_000L, 10_000L, TimeUnit.MILLISECONDS);
	}

	public synchronized void stopFeedTask() {
		ScheduledFuture<?> task = this.feedTask;
		this.feedTask = null;
		if (task != null) {
			task.cancel(false);
		}
	}

	public boolean isFeedTaskActive() {
		return feedTask != null && !feedTask.isDone();
	}

	public boolean tickHunger() {
		return tickHunger(PetService.getInstance());
	}

	/**
	 * Processa 1 tick de fome do Pet, consumindo racao automaticamente quando abaixo de 55% e recolhendo em 0.
	 *
	 * @return {@code true} se o Pet continua ativo; {@code false} se foi recolhido por inanição.
	 */
	public synchronized boolean tickHunger(PetService petService) {
		if (!isPet() || isDead()) {
			return false;
		}
		int consume = isInCombat() ? Math.max(1, feedBattle) : Math.max(1, feedNormal);
		this.currentFed = Math.max(0, this.currentFed - consume);

		if (this.currentFed < (this.maxFed * 55 / 100)) {
			tryAutoConsumeFood(petService);
		}

		if (this.currentFed <= 0) {
			this.currentFed = 0;
			recalculateStats();
			unsummon();
			return false;
		}
		broadcastStatusUpdate();
		return true;
	}

	/**
	 * Tenta consumir automaticamente 1 unidade de racao compativel do inventario do Pet (ou do dono).
	 */
	public synchronized boolean tryAutoConsumeFood() {
		return tryAutoConsumeFood(PetService.getInstance());
	}

	public synchronized boolean tryAutoConsumeFood(PetService petService) {
		PetDataTable dataTable = PetDataTable.getInstance();
		Set<Integer> validFoods = dataTable != null ? dataTable.getFoodIds(npcId()) : Set.of();
		if (validFoods.isEmpty()) {
			validFoods = Set.of(
					PetDataTable.FOOD_WOLF,
					PetDataTable.FOOD_HATCHLING,
					PetDataTable.FOOD_STRIDER,
					PetDataTable.FOOD_STRIDER_DELUXE,
					PetDataTable.FOOD_BABY,
					PetDataTable.FOOD_WYVERN,
					PetDataTable.FOOD_GREAT_WOLF);
		}

		int consumedFoodId = 0;
		for (int foodId : validFoods) {
			if (petInventory.getItemCount(foodId) > 0 && petInventory.destroyItemByItemId(foodId, 1)) {
				consumedFoodId = foodId;
				break;
			}
		}
		if (consumedFoodId == 0 && owner != null && owner.inventory() != null) {
			for (int foodId : validFoods) {
				if (owner.inventory().getItemCount(foodId) > 0 && owner.inventory().destroyItemByItemId(foodId, 1)) {
					consumedFoodId = foodId;
					break;
				}
			}
		}
		if (consumedFoodId == 0) {
			return false;
		}

		int foodRate = Math.max(1, Math.round(Config.PET_FOOD_RATE));
		int fedGain = Math.max(10, (this.maxFed * 25 / 100) * foodRate);
		this.currentFed = Math.min(this.maxFed, this.currentFed + fedGain);
		recalculateStats();

		if (petService != null && itemObjectId > 0) {
			petService.getPet(itemObjectId).ifPresent(rec -> petService.savePet(rec.withFed(this.currentFed)));
		}

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			int foodSkillId = PetDataTable.getFoodSkillId(consumedFoodId);
			world.broadcastAround(this.x, this.y, GameWorld.VISIBILITY_RADIUS, this.instanceId,
					new GameServerPacket.MagicSkillUse(this.objectId, this.objectId, foodSkillId, 1, 0, 0, this.x, this.y, this.z));
		}
		broadcastStatusUpdate();
		return true;
	}

	// ---- SERVITOR LIFETIME & CRYSTAL CONSUMPTION (#253, #254) ----

	public void configureLifeTime(long totalLifeTimeMs, int itemConsumeIdOT, int itemConsumeCountOT, long periodMs) {
		this.totalLifeTimeMs = Math.max(1_000L, totalLifeTimeMs);
		this.remainingLifeTimeMs = this.totalLifeTimeMs;
		this.itemConsumeIdOT = Math.max(0, itemConsumeIdOT);
		this.itemConsumeCountOT = Math.max(0, itemConsumeCountOT);
		this.itemConsumePeriodMs = Math.max(1_000L, periodMs);
		this.timeSinceLastCrystalConsumeMs = 0L;
	}

	public long totalLifeTimeMs() {
		return totalLifeTimeMs;
	}

	public long remainingLifeTimeMs() {
		return remainingLifeTimeMs;
	}

	public int itemConsumeIdOT() {
		return itemConsumeIdOT;
	}

	public int itemConsumeCountOT() {
		return itemConsumeCountOT;
	}

	public synchronized void startLifeTimeTask() {
		stopLifeTimeTask();
		if (!isServitor()) {
			return;
		}
		this.lifeTimeTask = SUMMON_SCHEDULER.scheduleAtFixedRate(() -> {
			try {
				tickLifeTime(5_000L);
			} catch (Exception ignored) {
			}
		}, 5_000L, 5_000L, TimeUnit.MILLISECONDS);
	}

	public synchronized void stopLifeTimeTask() {
		ScheduledFuture<?> task = this.lifeTimeTask;
		this.lifeTimeTask = null;
		if (task != null) {
			task.cancel(false);
		}
	}

	public boolean isLifeTimeTaskActive() {
		return lifeTimeTask != null && !lifeTimeTask.isDone();
	}

	/**
	 * Processa a passagem de tempo de vida do Servitor e o consumo periodico de Cristais / Gemstones.
	 *
	 * @return {@code true} se o Servitor permanece ativo; {@code false} se expirou ou faltou cristal.
	 */
	public synchronized boolean tickLifeTime(long elapsedMs) {
		if (!isServitor() || isDead()) {
			return false;
		}
		if (isSiegeSummon() && !validateSiegeZone()) {
			return false;
		}

		this.remainingLifeTimeMs = Math.max(0L, this.remainingLifeTimeMs - Math.max(0L, elapsedMs));
		this.currentFed = (int) Math.round((double) this.remainingLifeTimeMs / this.totalLifeTimeMs * this.maxFed);

		if (this.remainingLifeTimeMs <= 0L) {
			unsummon();
			return false;
		}

		if (this.itemConsumeIdOT > 0 && this.itemConsumeCountOT > 0) {
			this.timeSinceLastCrystalConsumeMs += Math.max(0L, elapsedMs);
			while (this.timeSinceLastCrystalConsumeMs >= this.itemConsumePeriodMs) {
				this.timeSinceLastCrystalConsumeMs -= this.itemConsumePeriodMs;
				if (owner == null || owner.inventory() == null
						|| owner.inventory().getItemCount(this.itemConsumeIdOT) < this.itemConsumeCountOT
						|| !owner.inventory().destroyItemByItemId(this.itemConsumeIdOT, this.itemConsumeCountOT)) {
					unsummon();
					return false;
				}
			}
		}

		broadcastStatusUpdate();
		return true;
	}

	// ---- FOLLOW TASK & MOVEMENT BROADCAST (#248) ----

	public synchronized void startFollowTask() {
		stopFollowTask();
		this.following = true;
		this.followTask = SUMMON_SCHEDULER.scheduleAtFixedRate(() -> {
			try {
				followOwnerTick();
			} catch (Exception ignored) {
			}
		}, 500L, 500L, TimeUnit.MILLISECONDS);
	}

	public synchronized void stopFollowTask() {
		ScheduledFuture<?> task = this.followTask;
		this.followTask = null;
		if (task != null) {
			task.cancel(false);
		}
	}

	public boolean isFollowTaskActive() {
		return followTask != null && !followTask.isDone();
	}

	public void followOwnerTick() {
		if (!following || owner == null || isDead() || isDisabled() || isRooted() || siegeMode) {
			return;
		}
		int dx = owner.x() - this.x;
		int dy = owner.y() - this.y;
		double dist = Math.hypot(dx, dy);
		if (dist <= 70.0) {
			return;
		}
		if (dist > 2500.0 || owner.instanceId() != this.instanceId) {
			this.instanceId = owner.instanceId();
			this.x = owner.x() + 40;
			this.y = owner.y() + 40;
			this.z = owner.z();
			return;
		}
		double ratio = Math.max(0.0, (dist - 45.0) / dist);
		int nx = this.x + (int) Math.round(dx * ratio);
		int ny = this.y + (int) Math.round(dy * ratio);
		int nz = owner.z();
		broadcastMovement(nx, ny, nz);
	}

	public void broadcastMovement(int nx, int ny, int nz) {
		int fromX = this.x;
		int fromY = this.y;
		int fromZ = this.z;
		moveTo(nx, ny, nz);
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			GameServerPacket movePkt = (following && owner != null)
					? new GameServerPacket.MoveToPawn(this.objectId, owner.objectId(), 45, nx, ny, nz)
					: new GameServerPacket.MoveToLocation(this.objectId, nx, ny, nz, fromX, fromY, fromZ);
			world.broadcastAround(this.x, this.y, GameWorld.VISIBILITY_RADIUS, this.instanceId, movePkt);
		}
	}

	/**
	 * Recolhe o Pet/Servitor, encerrando todas as tasks agendadas e notificando o mundo.
	 */
	public synchronized void unsummon() {
		stopAllTasks();
		if (isPet() && itemObjectId > 0 && PetService.getInstance() != null) {
			PetService.getInstance().storePetStatus(this);
		}
		if (owner != null && owner.activeSummon() == this) {
			owner.activeSummon(null);
			owner.petObjectId(0);
		}
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.removeSummon(this.objectId);
			world.broadcastAround(this.x, this.y, GameWorld.VISIBILITY_RADIUS, this.instanceId,
					new GameServerPacket.DeleteObject(this.objectId));
			if (owner != null) {
				world.sendPacket(owner.objectId(), new GameServerPacket.PetDelete(this.summonType, this.objectId));
			}
		}
	}

	public synchronized void stopAllTasks() {
		stopFeedTask();
		stopDecayTask();
		stopLifeTimeTask();
		stopFollowTask();
		ScheduledFuture<?> bt = this.buffTickTask;
		this.buffTickTask = null;
		if (bt != null) {
			bt.cancel(false);
		}
	}

	@Override
	public boolean isSummon() {
		return true;
	}

	// ---- BUFFS, HOT/DOT & STAT RECALCULATION (#199, #200) ----

	public List<SummonBuff> activeBuffs() {
		long now = System.currentTimeMillis();
		boolean removed = activeBuffs.values().removeIf(b -> b.endTimeMillis() <= now);
		if (removed) {
			recalculateStats();
			sendEffectIcons();
		}
		return List.copyOf(activeBuffs.values());
	}

	/**
	 * Aplica os efeitos de buff/HoT/DoT de uma {@link SkillTemplate} no Summon/Pet,
	 * respeitando {@code Config.MAX_BUFFS_AMOUNT} (#200), recalculando atributos a partir dos valores base
	 * (#199) e atualizando os icones da janela do Pet/PartySpelled.
	 */
	public synchronized long applySkillBuff(SkillTemplate sk) {
		if (sk == null || sk.effects() == null || sk.effects().isEmpty()) {
			return 0L;
		}
		long now = System.currentTimeMillis();
		activeBuffs.values().removeIf(b -> b.endTimeMillis() <= now);
		int maxBuffs = Math.max(1, Config.MAX_BUFFS_AMOUNT > 0 ? Config.MAX_BUFFS_AMOUNT : 20);

		long maxDuration = 0L;
		boolean modified = false;
		boolean hasPeriodic = false;

		for (var e : sk.effects()) {
			boolean isPeriodic = isPeriodicEffectName(e.name());
			if ((e.funcs() == null || e.funcs().isEmpty()) && !isPeriodic && !"NoblesseBless".equalsIgnoreCase(e.name())) {
				continue;
			}
			long dur = Math.max(1000L, e.durationMs());
			if (Config.ENABLE_MODIFY_SKILL_DURATION
					&& Config.SKILL_DURATION_LIST != null
					&& Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
				dur = Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
			}
			String stack = (e.stackType() == null || e.stackType().equalsIgnoreCase("none") || e.stackType().isBlank())
					? "skill_" + sk.id() + "_" + e.name()
					: e.stackType().toLowerCase(Locale.ROOT);
			int order = e.stackOrder() > 0 ? (int) Math.round(e.stackOrder()) : sk.level();
			SummonBuff existing = activeBuffs.get(stack);
			if (existing != null && existing.endTimeMillis() > now && Config.CANCEL_LESSER_EFFECT
					&& order < existing.stackOrder()) {
				continue;
			}

			// Se for um novo stackType e atingiu o limite maximo de buffs, remove o mais antigo (#200)
			if (existing == null && activeBuffs.size() >= maxBuffs) {
				String oldestKey = activeBuffs.entrySet().stream()
						.min(Comparator.comparingLong(en -> en.getValue().endTimeMillis()))
						.map(Map.Entry::getKey)
						.orElse(null);
				if (oldestKey != null) {
					activeBuffs.remove(oldestKey);
				}
			}

			List<StatFunc> funcList = e.funcs() != null ? List.copyOf(e.funcs()) : List.of();
			SummonBuff buff = new SummonBuff(
					sk.id(),
					sk.level(),
					stack,
					order,
					now + dur,
					funcList,
					e.name() != null ? e.name() : "Buff",
					Math.max(1, e.period()),
					e.val());
			activeBuffs.put(stack, buff);
			maxDuration = Math.max(maxDuration, dur);
			modified = true;
			if (isPeriodic) {
				hasPeriodic = true;
			}
		}
		if (modified) {
			recalculateStats();
			sendEffectIcons();
			broadcastStatusUpdate();
			if (hasPeriodic) {
				ensureBuffTickTask();
			}
		}
		return maxDuration;
	}

	private static boolean isPeriodicEffectName(String name) {
		if (name == null) {
			return false;
		}
		return switch (name.toLowerCase(Locale.ROOT)) {
			case "healovertime", "manahealovertime", "damovertime", "mdamovertime", "poison", "bleed" -> true;
			default -> false;
		};
	}

	private synchronized void ensureBuffTickTask() {
		if (this.buffTickTask != null && !this.buffTickTask.isDone()) {
			return;
		}
		this.buffTickTask = SUMMON_SCHEDULER.scheduleAtFixedRate(() -> {
			try {
				tickPeriodicEffects();
			} catch (Exception ignored) {
			}
		}, 3_000L, 3_000L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Executa um ciclo de efeitos periodicos (HoT/DoT) e expira buffs vencidos (#200).
	 */
	public synchronized void tickPeriodicEffects() {
		long now = System.currentTimeMillis();
		boolean expired = activeBuffs.values().removeIf(b -> b.endTimeMillis() <= now);
		boolean vitalsChanged = false;

		if (!isDead()) {
			for (SummonBuff b : activeBuffs.values()) {
				if (b.tickValue() == 0.0) {
					continue;
				}
				String ename = b.effectName() != null ? b.effectName().toLowerCase(Locale.ROOT) : "";
				switch (ename) {
					case "healovertime" -> {
						if (!isHealBlocked()) {
							this.currentHp = Math.min(this.maxHp, this.currentHp + Math.abs(b.tickValue()));
							vitalsChanged = true;
						}
					}
					case "manahealovertime" -> {
						this.currentMp = Math.min(this.maxMp, this.currentMp + Math.abs(b.tickValue()));
						vitalsChanged = true;
					}
					case "damovertime", "mdamovertime", "poison", "bleed" -> {
						double dmg = Math.abs(b.tickValue());
						if (this.currentHp > dmg) {
							this.currentHp -= dmg;
						} else {
							this.currentHp = 1.0;
						}
						vitalsChanged = true;
					}
					default -> {
					}
				}
			}
		}

		if (expired) {
			recalculateStats();
			sendEffectIcons();
		}
		if (expired || vitalsChanged) {
			broadcastStatusUpdate();
		}
	}

	public synchronized boolean removeBuffByStack(String stackType) {
		if (stackType == null) {
			return false;
		}
		boolean removed = activeBuffs.remove(stackType.toLowerCase(Locale.ROOT)) != null;
		if (removed) {
			recalculateStats();
			sendEffectIcons();
			broadcastStatusUpdate();
		}
		return removed;
	}

	public synchronized void clearBuffs() {
		activeBuffs.clear();
		recalculateStats();
		sendEffectIcons();
	}

	/**
	 * Recalcula todos os status de combate do Pet/Servitor a partir dos atributos base,
	 * equipamentos de Pet, penalidade de fome e funcoes de Buffs ativos (#199).
	 */
	public synchronized void recalculateStats() {
		long now = System.currentTimeMillis();
		activeBuffs.values().removeIf(b -> b.endTimeMillis() <= now);

		List<StatFunc> allFuncs = new ArrayList<>();
		for (SummonBuff b : activeBuffs.values()) {
			if (b.funcs() != null) {
				allFuncs.addAll(b.funcs());
			}
		}
		allFuncs.sort(Comparator.comparingInt(StatFunc::order));

		int effBasePAtk = this.basePAtk + this.weaponPAtkBonus + (this.equippedWeaponId > 0 && this.weaponPAtkBonus == 0 ? 25 : 0);
		int effBaseMAtk = this.baseMAtk + this.weaponMAtkBonus + (this.equippedWeaponId > 0 && this.weaponMAtkBonus == 0 ? 15 : 0);
		int effBasePDef = this.basePDef + this.armorPDefBonus + (this.equippedArmorId > 0 && this.armorPDefBonus == 0 ? 20 : 0);
		int effBaseMDef = this.baseMDef + this.armorMDefBonus + (this.equippedJewelId > 0 && this.armorMDefBonus == 0 ? 15 : 0);

		double hungerMult = isHungry() ? 0.5 : 1.0;

		this.pAtk = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("pAtk"), effBasePAtk)));
		this.pDef = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("pDef"), effBasePDef)));
		this.mAtk = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("mAtk"), effBaseMAtk)));
		this.mDef = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("mDef"), effBaseMDef)));
		this.pAtkSpd = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("pAtkSpd"), this.basePAtkSpd) * hungerMult));
		this.mAtkSpd = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("mAtkSpd"), this.baseMAtkSpd) * hungerMult));
		this.runSpd = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("runSpd"), this.baseRunSpd) * hungerMult));
		this.walkSpd = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("walkSpd"), this.baseWalkSpd) * hungerMult));
		this.maxHp = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("maxHp"), this.baseMaxHp)));
		this.maxMp = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("maxMp"), this.baseMaxMp)));
		this.accuracy = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("accCombat", "accuracy"), this.baseAccuracy)));
		this.evasion = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("rEvas", "evasion"), this.baseEvasion)));
		this.critical = Math.max(1, (int) Math.round(calcStat(allFuncs, Set.of("rCrit", "critical"), this.baseCritical)));
		this.critDamageMul = Math.max(0.1, calcStat(allFuncs, Set.of("cAtk"), 1.0));
		this.critDamageAdd = calcStat(allFuncs, Set.of("cAtkAdd"), 0.0);
		this.mCritRate = Math.max(1.0, calcStat(allFuncs, Set.of("mCritRate"), 80.0));
		this.shldRate = Math.max(0.0, calcStat(allFuncs, Set.of("rShld", "shldRate"), 0.0));
		this.shldDef = Math.max(0, (int) Math.round(calcStat(allFuncs, Set.of("sDef", "shldDef"), 0.0)));
		this.absorbDam = Math.max(0.0, calcStat(allFuncs, Set.of("absorbDam"), 0.0));

		if (this.currentHp > this.maxHp) {
			this.currentHp = this.maxHp;
		}
		if (this.currentMp > this.maxMp) {
			this.currentMp = this.maxMp;
		}
	}

	private double calcStat(List<StatFunc> funcs, Set<String> statNames, double baseVal) {
		double env = baseVal;
		for (StatFunc f : funcs) {
			if (f.stat() != null && matchesStat(statNames, f.stat())
					&& (f.condition() == null || f.condition().test(this.owner, this))) {
				env = f.apply(env, baseVal);
			}
		}
		return env;
	}

	private static boolean matchesStat(Set<String> statNames, String candidate) {
		for (String name : statNames) {
			if (name.equalsIgnoreCase(candidate)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Envia {@code PartySpelled} e {@code PetInfo} para o dono com a lista de buffs ativos (#200).
	 */
	public void sendEffectIcons() {
		GameWorld world = GameWorld.getInstance();
		if (world == null || owner == null) {
			return;
		}
		long now = System.currentTimeMillis();
		List<GameServerPacket.EffectInfo> effects = new ArrayList<>();
		for (SummonBuff b : activeBuffs.values()) {
			int remSec = (int) Math.max(1L, (b.endTimeMillis() - now) / 1000L);
			effects.add(new GameServerPacket.EffectInfo(b.skillId(), b.level(), remSec));
		}
		world.sendPacket(owner.objectId(), new GameServerPacket.PartySpelled(this.summonType, this.objectId, effects));
		world.sendPacket(owner.objectId(), new GameServerPacket.PetInfo(this, 1));
	}

	/**
	 * Envia {@code PetStatusUpdate} para o dono (#183, #200).
	 */
	public void broadcastStatusUpdate() {
		GameWorld world = GameWorld.getInstance();
		if (world == null || owner == null) {
			return;
		}
		world.sendPacket(owner.objectId(), new GameServerPacket.PetStatusUpdate(this));
	}
}
