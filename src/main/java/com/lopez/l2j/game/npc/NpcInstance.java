package com.lopez.l2j.game.npc;

/**
 * Instancia ativa de NPC no mundo do jogo (Monstro, NPC de cidade, Guard...).
 */
public final class NpcInstance {

	private final int objectId;
	private final NpcTemplate template;
	private int x;
	private int y;
	private int z;
	private int heading;
	private double currentHp;
	private double currentMp;
	private boolean dead;
	private boolean inCombat;
	private boolean running;
	private volatile int targetPlayerId;
	private volatile long lastAttackTime;

	private final int spawnX;
	private final int spawnY;
	private final int spawnZ;
	private final int spawnHeading;

	public NpcInstance(int objectId, NpcTemplate template, int x, int y, int z, int heading) {
		this.objectId = objectId;
		this.template = template;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.spawnX = x;
		this.spawnY = y;
		this.spawnZ = z;
		this.spawnHeading = heading;
		this.currentHp = template.maxHp();
		this.currentMp = template.maxMp();
	}

	public int objectId() { return objectId; }
	public NpcTemplate template() { return template; }
	public NpcTemplate getTemplate() { return template; }
	public int npcId() { return template.id(); }
	public int getNpcId() { return template.id(); }
	public int getLevel() { return template != null ? template.level() : 1; }
	public String name() { return template.name(); }
	public int x() { return x; }
	public void x(int value) { this.x = value; }
	public int y() { return y; }
	public void y(int value) { this.y = value; }
	public int z() { return z; }
	public void z(int value) { this.z = value; }
	public int heading() { return heading; }
	public void heading(int value) { this.heading = value; }
	public int spawnX() { return spawnX; }
	public int spawnY() { return spawnY; }
	public int spawnZ() { return spawnZ; }
	public int spawnHeading() { return spawnHeading; }
	public double currentHp() {
		return dead ? 0.0 : currentHp;
	}

	public void currentHp(double value) {
		if (dead) {
			this.currentHp = 0.0;
			return;
		}
		this.currentHp = Math.max(0.0, value);
		if (this.currentHp <= 0.0) {
			this.dead = true;
		}
	}

	public double currentMp() { return currentMp; }
	public void currentMp(double value) { this.currentMp = value; }

	public boolean isDead() {
		return dead;
	}

	public void dead(boolean value) {
		this.dead = value;
		if (value) {
			this.currentHp = 0.0;
			abortAttack();
			abortCast();
		}
	}
	public boolean isInCombat() { return inCombat; }
	public boolean inCombat() { return inCombat; }
	public void inCombat(boolean value) {
		this.inCombat = value;
		if (!value) {
			abortAttack();
			abortCast();
		}
	}
	public boolean isRunning() { return running; }
	public void running(boolean value) { this.running = value; }
	public boolean isAttackable() { return template.isAttackable(); }
	public boolean isMonster() { return template.isMonster(); }

	private volatile java.util.concurrent.ScheduledFuture<?> currentAttackTask;
	private volatile java.util.concurrent.ScheduledFuture<?> currentCastTask;
	private volatile boolean casting;
	private volatile long attackEndTime;

	public boolean isCasting() {
		return casting;
	}

	public void casting(boolean value) {
		this.casting = value;
	}

	public long attackEndTime() {
		return attackEndTime;
	}

	public void attackEndTime(long attackEndTime) {
		this.attackEndTime = attackEndTime;
	}

	public boolean isAttacking() {
		return System.currentTimeMillis() < attackEndTime;
	}

	public java.util.concurrent.ScheduledFuture<?> currentAttackTask() {
		return currentAttackTask;
	}

	public void currentAttackTask(java.util.concurrent.ScheduledFuture<?> task) {
		var old = this.currentAttackTask;
		if (old != null && old != task && !old.isDone()) {
			old.cancel(false);
		}
		this.currentAttackTask = task;
	}

	public java.util.concurrent.ScheduledFuture<?> currentCastTask() {
		return currentCastTask;
	}

	public void currentCastTask(java.util.concurrent.ScheduledFuture<?> task) {
		var old = this.currentCastTask;
		if (old != null && old != task && !old.isDone()) {
			old.cancel(false);
		}
		this.currentCastTask = task;
	}

	public void abortAttack() {
		this.attackEndTime = 0;
		var task = this.currentAttackTask;
		if (task != null) {
			task.cancel(false);
			this.currentAttackTask = null;
		}
	}

	public void abortCast() {
		this.casting = false;
		var task = this.currentCastTask;
		if (task != null) {
			task.cancel(false);
			this.currentCastTask = null;
		}
	}

	public void deleteMe() {
		this.dead = true;
	}

	public int getX() { return (int) x; }
	public int getY() { return (int) y; }
	public int getZ() { return (int) z; }
	public int getHeading() { return heading; }

	private volatile double pDefMul = 1.0;
	private volatile double mDefMul = 1.0;
	private volatile double pAtkMul = 1.0;
	private volatile double mAtkMul = 1.0;
	private volatile double runSpdMul = 1.0;
	private volatile double pAtkSpdMul = 1.0;
	private volatile double mAtkSpdMul = 1.0;
	private volatile double maxHpMul = 1.0;
	private volatile boolean champion;
	private volatile String championTitle;
	private volatile boolean invul;

	public double maxHp() { return template.maxHp() * maxHpMul; }
	public double maxHpMul() { return maxHpMul; }
	public void maxHpMul(double value) { this.maxHpMul = value; }
	public boolean isChampion() { return champion; }
	public void champion(boolean value) { this.champion = value; }
	public String championTitle() { return championTitle; }
	public void championTitle(String value) { this.championTitle = value; }
	public boolean invul() { return invul; }
	public void invul(boolean value) { this.invul = value; }

	public double pDef() { return template.pDef() * pDefMul; }
	public double mDef() { return template.mDef() * mDefMul; }
	public double pAtk() { return template.pAtk() * pAtkMul; }
	public double mAtk() { return template.mAtk() * mAtkMul; }
	public int runSpd() { return (int) Math.max(1, template.runSpd() * runSpdMul); }
	public int walkSpd() { return (int) Math.max(1, template.walkSpd() * runSpdMul); }
	public int pAtkSpd() { return (int) Math.max(1, template.pAtkSpd() * pAtkSpdMul); }
	public int mAtkSpd() { return (int) Math.max(1, template.mAtkSpd() * mAtkSpdMul); }
	public void pDefMul(double value) { this.pDefMul = value; }
	public double pDefMul() { return pDefMul; }
	public void mDefMul(double value) { this.mDefMul = value; }
	public double mDefMul() { return mDefMul; }
	public void pAtkMul(double value) { this.pAtkMul = value; }
	public double pAtkMul() { return pAtkMul; }
	public void mAtkMul(double value) { this.mAtkMul = value; }
	public double mAtkMul() { return mAtkMul; }
	public void runSpdMul(double value) { this.runSpdMul = value; }
	public double runSpdMul() { return runSpdMul; }
	public void pAtkSpdMul(double value) { this.pAtkSpdMul = value; }
	public double pAtkSpdMul() { return pAtkSpdMul; }
	public void mAtkSpdMul(double value) { this.mAtkSpdMul = value; }
	public double mAtkSpdMul() { return mAtkSpdMul; }
	public int targetPlayerId() { return targetPlayerId; }
	public void targetPlayerId(int value) { this.targetPlayerId = value; }
	public long lastAttackTime() { return lastAttackTime; }
	public void lastAttackTime(long value) { this.lastAttackTime = value; }

	private volatile int masterObjectId;
	private final java.util.List<NpcInstance> minions = new java.util.concurrent.CopyOnWriteArrayList<>();
	private volatile boolean raidMinion;

	public int masterObjectId() { return masterObjectId; }
	public void masterObjectId(int id) { this.masterObjectId = id; }
	public java.util.List<NpcInstance> minions() { return minions; }
	public boolean hasMinions() { return !minions.isEmpty(); }
	public boolean isMinion() { return masterObjectId != 0 || template.isMinion(); }
	public boolean isRaidMinion() {
		return raidMinion || (template != null && (
				"L2RaidMinion".equalsIgnoreCase(template.type())
				|| "L2GrandBossMinion".equalsIgnoreCase(template.type())
		));
	}
	public void raidMinion(boolean value) { this.raidMinion = value; }

	public void moveTo(int x, int y, int z) {
		moveTo(x, y, z, this.heading);
	}

	public void moveTo(int x, int y, int z, int heading) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	// ---- controle de Spoil (Anões: Spoil / Sweeper) ----

	private volatile boolean spoiled;
	private volatile int spoilerPlayerId;
	private volatile java.util.List<com.lopez.l2j.game.drop.DropReward> spoilRewards;

	public boolean isSpoiled() { return spoiled; }
	public void spoiled(boolean value) { this.spoiled = value; }
	public int spoilerPlayerId() { return spoilerPlayerId; }
	public void spoilerPlayerId(int value) { this.spoilerPlayerId = value; }
	public java.util.List<com.lopez.l2j.game.drop.DropReward> spoilRewards() { return spoilRewards; }
	public void spoilRewards(java.util.List<com.lopez.l2j.game.drop.DropReward> rewards) { this.spoilRewards = rewards; }

	public void addMinion(NpcInstance minion) {
		if (minion != null && !minions.contains(minion)) {
			minion.masterObjectId(this.objectId);
			if (this.template != null && (this.template.isRaidBoss() || this.template.isGrandBoss())) {
				minion.raidMinion(true);
			}
			minions.add(minion);
		}
	}

	public void removeMinion(NpcInstance minion) {
		if (minion != null) {
			minions.remove(minion);
		}
	}

	// ---- controle de efeitos anormais (AbnormalEffect bitmask) ----

	private volatile int abnormalEffect;

	public int abnormalEffect() { return abnormalEffect; }
	public void abnormalEffect(int value) { this.abnormalEffect = value; }
	public void startAbnormalEffect(int mask) { this.abnormalEffect |= mask; }
	public void stopAbnormalEffect(int mask) { this.abnormalEffect &= ~mask; }

	// ---- controle de debuffs de skills (Stun/Sleep/Paralyze/Root) ----

	private volatile long disabledUntil;
	private volatile boolean sleeping;
	private volatile long rootedUntil;
	private volatile long mutedUntil;
	private volatile long physicalMutedUntil;

	/** Stun/Paralyze/Sleep: nao anda nem ataca ate {@code until}. Sleep quebra ao tomar dano. */
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

	public void physicalMute(long until) {
		this.physicalMutedUntil = Math.max(physicalMutedUntil, until);
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

	public boolean isPhysicalMuted() {
		return System.currentTimeMillis() < physicalMutedUntil;
	}

	/** Chamado quando o monstro toma dano: acorda do Sleep. */
	public void onDamaged() {
		if (sleeping) {
			sleeping = false;
			disabledUntil = 0;
			stopAbnormalEffect(0x0080);
		}
	}

	@Override
	public String toString() {
		return template.name() + " (" + template.id() + ") #" + objectId + " [" + x + "," + y + "," + z + "]";
	}
}
