package com.lopez.l2j.game.summon;

import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcTemplate;

/**
 * Instância viva no mundo de um Pet (Wolf, Hatchling, Strider, Baby Pet, Sin Eater)
 * ou Servitor (Summon de Warlock, Elemental Summoner, Phantom Summoner, Necromancer, Crafter).
 * Implementa {@link Creature} para participar nativamente de {@code CombatService},
 * {@code GeoEngine}, {@code GameWorld} e handlers de skills.
 */
public final class SummonInstance implements Creature {

	public static final int TYPE_PET = 1;
	public static final int TYPE_SERVITOR = 2;

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

	private volatile long exp;
	private volatile int sp;
	private volatile int currentFed;
	private volatile int maxFed;

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

	private volatile boolean soulshotCharged;
	private volatile int spiritshotCharged; // 0=none, 1=spiritshot, 2=blessed

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

		if (owner != null) {
			this.x = owner.x() + 40;
			this.y = owner.y() + 40;
			this.z = owner.z();
			this.heading = owner.heading();
			this.instanceId = owner.instanceId();
		}

		int scaleLv = Math.max(1, this.level);
		this.maxHp = Math.max(100, this.template.maxHp() > 0 ? this.template.maxHp() : 200 + scaleLv * 45);
		this.maxMp = Math.max(50, this.template.maxMp() > 0 ? this.template.maxMp() : 100 + scaleLv * 22);
		this.currentHp = this.maxHp;
		this.currentMp = this.maxMp;

		this.pAtk = Math.max(15, this.template.pAtk() > 0 ? this.template.pAtk() : 25 + scaleLv * 12);
		this.pDef = Math.max(20, this.template.pDef() > 0 ? this.template.pDef() : 35 + scaleLv * 8);
		this.mAtk = Math.max(15, this.template.mAtk() > 0 ? this.template.mAtk() : 20 + scaleLv * 10);
		this.mDef = Math.max(20, this.template.mDef() > 0 ? this.template.mDef() : 30 + scaleLv * 7);
		this.pAtkSpd = Math.max(200, this.template.pAtkSpd() > 0 ? this.template.pAtkSpd() : 278);
		this.mAtkSpd = Math.max(200, this.template.mAtkSpd() > 0 ? this.template.mAtkSpd() : 333);
		this.runSpd = Math.max(80, this.template.runSpd() > 0 ? this.template.runSpd() : 125);
		this.walkSpd = Math.max(40, this.template.walkSpd() > 0 ? this.template.walkSpd() : 80);

		this.maxFed = summonType == TYPE_PET ? 1200 : 1000;
		this.currentFed = this.maxFed;
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

	public int accuracy() {
		return 30 + level;
	}

	public int evasion() {
		return 30 + level;
	}

	public int critical() {
		return 44;
	}

	public long exp() {
		return exp;
	}

	public void exp(long value) {
		this.exp = Math.max(0L, value);
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
		this.currentFed = Math.max(0, Math.min(maxFed, value));
	}

	public int maxFed() {
		return maxFed;
	}

	public void maxFed(int value) {
		this.maxFed = Math.max(1, value);
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

	@Override
	public boolean isDisabled() {
		return System.currentTimeMillis() < disabledUntil;
	}

	@Override
	public boolean isRooted() {
		return System.currentTimeMillis() < rootedUntil;
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
		return this.currentHp <= 0.0;
	}

	public boolean soulshotCharged() {
		return soulshotCharged;
	}

	public void soulshotCharged(boolean value) {
		this.soulshotCharged = value;
	}

	public int spiritshotCharged() {
		return spiritshotCharged;
	}

	public void spiritshotCharged(int value) {
		this.spiritshotCharged = value;
	}

	@Override
	public boolean isSummon() {
		return true;
	}
}
