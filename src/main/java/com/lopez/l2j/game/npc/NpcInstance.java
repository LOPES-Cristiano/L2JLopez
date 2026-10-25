package com.lopez.l2j.game.npc;

import com.lopez.l2j.game.model.Creature;

/**
 * Instancia ativa de NPC no mundo do jogo (Monstro, NPC de cidade, Guard...).
 */
public final class NpcInstance implements Creature {

	private final int objectId;
	private final NpcTemplate template;
	private int x;
	private int y;
	private int z;
	private int heading;
	private int instanceId = 0;
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
	public int idTemplate() { return template != null ? template.idTemplate() : 0; }
	@Override
	public int level() { return template != null ? template.level() : 1; }
	public int getLevel() { return level(); }
	public String name() { return template.name(); }
	public int x() { return x; }
	public void x(int value) { this.x = value; }
	public int y() { return y; }
	public void y(int value) { this.y = value; }
	public int z() { return z; }
	public void z(int value) { this.z = value; }
	public int heading() { return heading; }
	public void heading(int value) { this.heading = value; }
	@Override
	public int instanceId() { return instanceId; }
	@Override
	public void instanceId(int value) { this.instanceId = value; }
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

	public boolean applyDamage(int damage) {
		if (invul || dead || damage <= 0) {
			return dead;
		}
		onDamaged();
		currentHp(this.currentHp - damage);
		return dead;
	}

	public void onDamage(int attackerObjId, int damage) {
		addDamageHate(attackerObjId, damage, damage);
		applyDamage(damage);
	}

	public int effectiveMDef() {
		return (int) mDef();
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

	private volatile double basePDefMul = 1.0;
	private volatile double baseMDefMul = 1.0;
	private volatile double basePAtkMul = 1.0;
	private volatile double baseMAtkMul = 1.0;
	private volatile double baseRunSpdMul = 1.0;
	private volatile double basePAtkSpdMul = 1.0;
	private volatile double baseMAtkSpdMul = 1.0;
	private volatile double baseMaxHpMul = 1.0;

	private volatile double passivePDefMul = 1.0;
	private volatile double passiveMDefMul = 1.0;
	private volatile double passivePAtkMul = 1.0;
	private volatile double passiveMAtkMul = 1.0;
	private volatile double passiveRunSpdMul = 1.0;
	private volatile double passivePAtkSpdMul = 1.0;
	private volatile double passiveMAtkSpdMul = 1.0;
	private volatile double passiveMaxHpMul = 1.0;

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

	public double maxHp() { purgeExpiredEffectsIfNeeded(); return template.maxHp() * maxHpMul; }
	@Override
	public double maxHpValue() { return maxHp(); }
	@Override
	public double maxMpValue() { return template != null ? template.maxMp() : 100.0; }
	@Override
	public boolean isNpc() { return true; }
	public double maxHpMul() { purgeExpiredEffectsIfNeeded(); return maxHpMul; }
	public void maxHpMul(double value) { this.baseMaxHpMul = value; recalculateStatMultipliers(); }
	public boolean isChampion() { return champion; }
	public void champion(boolean value) { this.champion = value; }
	public String championTitle() { return championTitle; }
	public void championTitle(String value) { this.championTitle = value; }
	public boolean invul() { return invul; }
	public void invul(boolean value) { this.invul = value; }

	public double pDef() { purgeExpiredEffectsIfNeeded(); return template.pDef() * pDefMul; }
	public double mDef() { purgeExpiredEffectsIfNeeded(); return template.mDef() * mDefMul; }
	public double pAtk() { purgeExpiredEffectsIfNeeded(); return template.pAtk() * pAtkMul; }
	public double mAtk() { purgeExpiredEffectsIfNeeded(); return template.mAtk() * mAtkMul; }
	public int runSpd() { purgeExpiredEffectsIfNeeded(); return (int) Math.max(1, template.runSpd() * runSpdMul); }
	public int walkSpd() { purgeExpiredEffectsIfNeeded(); return (int) Math.max(1, template.walkSpd() * runSpdMul); }
	public int pAtkSpd() { purgeExpiredEffectsIfNeeded(); return (int) Math.max(1, template.pAtkSpd() * pAtkSpdMul); }
	public int mAtkSpd() { purgeExpiredEffectsIfNeeded(); return (int) Math.max(1, template.mAtkSpd() * mAtkSpdMul); }
	public void pDefMul(double value) { this.basePDefMul = value; recalculateStatMultipliers(); }
	public double pDefMul() { purgeExpiredEffectsIfNeeded(); return pDefMul; }
	public void mDefMul(double value) { this.baseMDefMul = value; recalculateStatMultipliers(); }
	public double mDefMul() { purgeExpiredEffectsIfNeeded(); return mDefMul; }
	public void pAtkMul(double value) { this.basePAtkMul = value; recalculateStatMultipliers(); }
	public double pAtkMul() { purgeExpiredEffectsIfNeeded(); return pAtkMul; }
	public void mAtkMul(double value) { this.baseMAtkMul = value; recalculateStatMultipliers(); }
	public double mAtkMul() { purgeExpiredEffectsIfNeeded(); return mAtkMul; }
	public void runSpdMul(double value) { this.baseRunSpdMul = value; recalculateStatMultipliers(); }
	public double runSpdMul() { purgeExpiredEffectsIfNeeded(); return runSpdMul; }
	public void pAtkSpdMul(double value) { this.basePAtkSpdMul = value; recalculateStatMultipliers(); }
	public double pAtkSpdMul() { purgeExpiredEffectsIfNeeded(); return pAtkSpdMul; }
	public void mAtkSpdMul(double value) { this.baseMAtkSpdMul = value; recalculateStatMultipliers(); }
	public double mAtkSpdMul() { purgeExpiredEffectsIfNeeded(); return mAtkSpdMul; }
	public int targetPlayerId() { return targetPlayerId; }
	public void targetPlayerId(int value) { this.targetPlayerId = value; }
	public long lastAttackTime() { return lastAttackTime; }
	public void lastAttackTime(long value) { this.lastAttackTime = value; }

	// ---- AggroList (Damage & Hate Table para IA de Monstros e Raid Bosses) ----

	public static final class AggroInfo {
		private final int attackerId;
		private volatile long damage;
		private volatile long hate;

		public AggroInfo(int attackerId) {
			this.attackerId = attackerId;
		}

		public int attackerId() { return attackerId; }
		public long damage() { return damage; }
		public long hate() { return hate; }
		public synchronized void addDamageHate(long dmg, long h) {
			if (dmg > 0) {
				this.damage += dmg;
			}
			this.hate = Math.max(0L, this.hate + h);
		}
		public synchronized void reduceHate(long amount) {
			this.hate = Math.max(0L, this.hate - Math.max(0L, amount));
		}
	}

	private final java.util.Map<Integer, AggroInfo> aggroList = new java.util.concurrent.ConcurrentHashMap<>();

	public java.util.Map<Integer, AggroInfo> aggroList() {
		return aggroList;
	}

	public void addDamageHate(int attackerId, long damage, long hate) {
		if (attackerId <= 0) {
			return;
		}
		long effectiveHate = hate != 0 ? hate : Math.max(1L, damage);
		aggroList.computeIfAbsent(attackerId, AggroInfo::new).addDamageHate(damage, effectiveHate);
		int mostHated = getMostHatedId();
		if (mostHated != 0) {
			this.targetPlayerId = mostHated;
		} else if (this.targetPlayerId == 0) {
			this.targetPlayerId = attackerId;
		}
	}

	public void reduceHate(int attackerId, long amount) {
		AggroInfo info = aggroList.get(attackerId);
		if (info != null) {
			info.reduceHate(amount);
			if (info.hate() <= 0 && this.targetPlayerId == attackerId) {
				this.targetPlayerId = getMostHatedId();
			}
		}
	}

	public void stopHating(int attackerId) {
		aggroList.remove(attackerId);
		if (this.targetPlayerId == attackerId) {
			this.targetPlayerId = getMostHatedId();
		}
	}

	public void clearAggro() {
		aggroList.clear();
	}

	public int getMostHatedId() {
		int bestId = 0;
		long maxHate = 0L;
		for (AggroInfo info : aggroList.values()) {
			if (info.hate() > maxHate) {
				maxHate = info.hate();
				bestId = info.attackerId();
			}
		}
		return bestId;
	}

	public long getHate(int attackerId) {
		AggroInfo info = aggroList.get(attackerId);
		return info != null ? info.hate() : 0L;
	}

	public long getDamage(int attackerId) {
		AggroInfo info = aggroList.get(attackerId);
		return info != null ? info.damage() : 0L;
	}

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
	public boolean hasSpoilRewards() { return spoilRewards != null && !spoilRewards.isEmpty(); }
	public boolean isSweepable() { return dead && spoiled && spoilRewards != null && !spoilRewards.isEmpty(); }

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

	// ---- controle de Reuse (Cooldown), Efeitos Ativos (ActiveNpcEffect), Self-Buffs e Passivas de Monstros ----

	public record ActiveNpcEffect(
			int skillId,
			int skillLevel,
			String stackType,
			double stackOrder,
			long expiresAt,
			double pAtkMul,
			double pDefMul,
			double mAtkMul,
			double mDefMul,
			double pAtkSpdMul,
			double mAtkSpdMul,
			double runSpdMul,
			double maxHpMul,
			double pAtkAdd,
			double pDefAdd,
			double mAtkAdd,
			double mDefAdd,
			double pAtkSpdAdd,
			double mAtkSpdAdd,
			double runSpdAdd,
			double maxHpAdd,
			int dotDamagePerTick,
			int abnormalMask) {

		public ActiveNpcEffect(int skillId, int skillLevel, String stackType, double stackOrder, long expiresAt,
				double pAtkMul, double pDefMul, double mAtkMul, double mDefMul,
				double pAtkSpdMul, double mAtkSpdMul, double runSpdMul, double maxHpMul,
				int dotDamagePerTick, int abnormalMask) {
			this(skillId, skillLevel, stackType, stackOrder, expiresAt,
					pAtkMul, pDefMul, mAtkMul, mDefMul, pAtkSpdMul, mAtkSpdMul, runSpdMul, maxHpMul,
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, dotDamagePerTick, abnormalMask);
		}
	}

	private final java.util.Map<Integer, Long> skillReuses = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<Integer, Long> activeBuffs = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<String, ActiveNpcEffect> activeEffects = new java.util.concurrent.ConcurrentHashMap<>();
	private volatile boolean passivesInitialized;

	public java.util.Map<String, ActiveNpcEffect> activeEffects() {
		purgeExpiredEffectsIfNeeded();
		return activeEffects;
	}

	public boolean isSkillDisabled(int skillId) {
		Long until = skillReuses.get(skillId);
		if (until == null) {
			return false;
		}
		if (System.currentTimeMillis() < until) {
			return true;
		}
		skillReuses.remove(skillId, until);
		return false;
	}

	public void disableSkill(int skillId, long delayMs) {
		if (delayMs > 0) {
			skillReuses.put(skillId, System.currentTimeMillis() + delayMs);
		}
	}

	public boolean hasActiveBuff(int skillId) {
		purgeExpiredEffectsIfNeeded();
		Long until = activeBuffs.get(skillId);
		if (until != null) {
			if (System.currentTimeMillis() < until) {
				return true;
			}
			activeBuffs.remove(skillId, until);
		}
		for (ActiveNpcEffect eff : activeEffects.values()) {
			if (eff.skillId() == skillId && System.currentTimeMillis() < eff.expiresAt()) {
				return true;
			}
		}
		return false;
	}

	public void addActiveEffect(ActiveNpcEffect effect) {
		if (effect == null) {
			return;
		}
		purgeExpiredEffectsIfNeeded();
		String key = (effect.stackType() != null && !effect.stackType().isBlank() && !"none".equalsIgnoreCase(effect.stackType()))
				? effect.stackType().toLowerCase(java.util.Locale.ROOT)
				: "skill_" + effect.skillId();
		ActiveNpcEffect existing = activeEffects.get(key);
		if (existing != null && System.currentTimeMillis() < existing.expiresAt() && existing.stackOrder() > effect.stackOrder()) {
			return;
		}
		if (existing != null && existing.abnormalMask() != 0 && existing.abnormalMask() != effect.abnormalMask()) {
			stopAbnormalEffect(existing.abnormalMask());
		}
		activeEffects.put(key, effect);
		if (effect.skillId() > 0) {
			activeBuffs.put(effect.skillId(), effect.expiresAt());
		}
		if (effect.abnormalMask() != 0) {
			startAbnormalEffect(effect.abnormalMask());
		}
		recalculateStatMultipliers();
	}

	public void removeActiveEffect(String stackKey) {
		if (stackKey == null) {
			return;
		}
		ActiveNpcEffect removed = activeEffects.remove(stackKey);
		if (removed != null) {
			if (removed.skillId() > 0) {
				activeBuffs.remove(removed.skillId());
			}
			if (removed.abnormalMask() != 0) {
				stopAbnormalEffect(removed.abnormalMask());
			}
			recalculateStatMultipliers();
		}
	}

	public void clearActiveEffects() {
		for (ActiveNpcEffect eff : activeEffects.values()) {
			if (eff.abnormalMask() != 0) {
				stopAbnormalEffect(eff.abnormalMask());
			}
		}
		activeEffects.clear();
		activeBuffs.clear();
		recalculateStatMultipliers();
	}

	public void applyTemporaryStatModifier(String stackType, String stat, double factor, long durationMs) {
		if (stat == null || durationMs <= 0) {
			return;
		}
		String st = stat.toLowerCase(java.util.Locale.ROOT);
		double pAtkM = 1.0, pDefM = 1.0, mAtkM = 1.0, mDefM = 1.0, pAtkSpdM = 1.0, mAtkSpdM = 1.0, runSpdM = 1.0, maxHpM = 1.0;
		switch (st) {
			case "patk" -> pAtkM = factor;
			case "pdef" -> pDefM = factor;
			case "matk" -> mAtkM = factor;
			case "mdef" -> mDefM = factor;
			case "patkspd" -> pAtkSpdM = factor;
			case "matkspd" -> mAtkSpdM = factor;
			case "runspd" -> runSpdM = factor;
			case "maxhp" -> maxHpM = factor;
		}
		String key = (stackType != null && !stackType.isBlank()) ? stackType : ("temp_" + st);
		addActiveEffect(new ActiveNpcEffect(
				0, 1, key, 1.0, System.currentTimeMillis() + durationMs,
				pAtkM, pDefM, mAtkM, mDefM, pAtkSpdM, mAtkSpdM, runSpdM, maxHpM, 0, 0));
	}

	public void applySkillBuff(com.lopez.l2j.game.skill.SkillTemplate skill) {
		if (skill == null) {
			return;
		}
		long durationMs = 60_000L;
		String stackType = null;
		double stackOrder = skill.level();
		int dotDmg = 0;
		int abnMask = 0;
		for (var eff : skill.effects()) {
			if (eff.durationMs() > 0) {
				durationMs = Math.max(durationMs, eff.durationMs());
			}
			if (eff.stackType() != null && !eff.stackType().isBlank() && !"none".equalsIgnoreCase(eff.stackType())) {
				stackType = eff.stackType();
				stackOrder = eff.stackOrder();
			}
			if (eff.abnormalMask() != 0) {
				abnMask |= eff.abnormalMask();
			}
			if (eff.name() != null && ("DamOverTime".equalsIgnoreCase(eff.name())
					|| "Poison".equalsIgnoreCase(eff.name()) || "Bleed".equalsIgnoreCase(eff.name()))) {
				dotDmg = Math.max(dotDmg, (int) Math.round(Math.abs(eff.val())));
			}
		}
		double[] muls = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
		double[] adds = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		accumulateStatFuncs(skill.funcs(), muls, adds);
		for (var eff : skill.effects()) {
			accumulateStatFuncs(eff.funcs(), muls, adds);
		}
		long expiresAt = System.currentTimeMillis() + durationMs;
		activeBuffs.put(skill.id(), expiresAt);
		addActiveEffect(new ActiveNpcEffect(
				skill.id(), skill.level(), stackType, stackOrder, expiresAt,
				muls[0], muls[1], muls[2], muls[3], muls[4], muls[5], muls[6], muls[7],
				adds[0], adds[1], adds[2], adds[3], adds[4], adds[5], adds[6], adds[7],
				dotDmg, abnMask));
	}

	public void applySkillDebuff(com.lopez.l2j.game.skill.SkillTemplate skill) {
		applySkillBuff(skill);
	}

	public void purgeExpiredEffectsIfNeeded() {
		if (activeEffects.isEmpty() && activeBuffs.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		boolean changed = false;
		var it = activeEffects.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			ActiveNpcEffect eff = entry.getValue();
			if (now >= eff.expiresAt()) {
				it.remove();
				if (eff.skillId() > 0) {
					activeBuffs.remove(eff.skillId());
				}
				if (eff.abnormalMask() != 0) {
					stopAbnormalEffect(eff.abnormalMask());
				}
				changed = true;
			}
		}
		var buffIt = activeBuffs.entrySet().iterator();
		while (buffIt.hasNext()) {
			var bEntry = buffIt.next();
			if (now >= bEntry.getValue()) {
				buffIt.remove();
			}
		}
		if (changed) {
			recalculateStatMultipliers();
		}
	}

	/**
	 * Processa um tick (1s) dos efeitos ativos (purga expirados e aplica DoT Poison/Bleed).
	 */
	public int tickActiveEffects() {
		purgeExpiredEffectsIfNeeded();
		if (dead || activeEffects.isEmpty()) {
			return 0;
		}
		int totalDot = 0;
		for (ActiveNpcEffect eff : activeEffects.values()) {
			if (eff.dotDamagePerTick() > 0 && System.currentTimeMillis() < eff.expiresAt()) {
				totalDot += eff.dotDamagePerTick();
			}
		}
		if (totalDot > 0 && currentHp > 1.0) {
			double nextHp = Math.max(1.0, currentHp - totalDot);
			currentHp(nextHp);
		}
		return totalDot;
	}

	private volatile boolean seeded;
	private volatile int seederPlayerId;
	private volatile int seedItemId;
	private volatile boolean overhit;
	private volatile int overhitDamage;
	private volatile int overhitAttackerId;

	public boolean isSeeded() { return seeded; }
	public void seeded(boolean val) { this.seeded = val; }
	public int seederPlayerId() { return seederPlayerId; }
	public void seederPlayerId(int val) { this.seederPlayerId = val; }
	public int seedItemId() { return seedItemId; }
	public void seedItemId(int val) { this.seedItemId = val; }
	public boolean isOverhit() { return overhit; }
	public void overhit(boolean val) { this.overhit = val; }
	public int overhitDamage() { return overhitDamage; }
	public void overhitDamage(int val) { this.overhitDamage = val; }
	public int overhitAttackerId() { return overhitAttackerId; }
	public void overhitAttackerId(int val) { this.overhitAttackerId = val; }

	public void applyPassiveSkills(NpcSkillTable npcSkillTable, com.lopez.l2j.game.skill.SkillTable skillTable) {
		if (passivesInitialized || npcSkillTable == null || skillTable == null || template == null) {
			return;
		}
		passivesInitialized = true;
		var skills = npcSkillTable.getSkills(npcId(), idTemplate());
		if (skills == null || skills.isEmpty()) {
			return;
		}
		double oldMaxHp = maxHp();
		for (var holder : skills) {
			var opt = skillTable.get(holder.skillId(), holder.level());
			if (opt.isPresent() && opt.get().isPassive()) {
				var sk = opt.get();
				applyStatFuncsToPassiveMultipliers(sk.funcs());
				for (var eff : sk.effects()) {
					applyStatFuncsToPassiveMultipliers(eff.funcs());
				}
			}
		}
		recalculateStatMultipliers();
		double newMaxHp = maxHp();
		if (newMaxHp != oldMaxHp && oldMaxHp > 0 && !dead) {
			double ratio = currentHp / oldMaxHp;
			currentHp = Math.min(newMaxHp, newMaxHp * ratio);
		}
	}

	public synchronized void recalculateStatMultipliers() {
		double ePAtkMul = 1.0, ePDefMul = 1.0, eMAtkMul = 1.0, eMDefMul = 1.0;
		double ePAtkSpdMul = 1.0, eMAtkSpdMul = 1.0, eRunSpdMul = 1.0, eMaxHpMul = 1.0;
		double ePAtkAdd = 0.0, ePDefAdd = 0.0, eMAtkAdd = 0.0, eMDefAdd = 0.0;
		double ePAtkSpdAdd = 0.0, eMAtkSpdAdd = 0.0, eRunSpdAdd = 0.0, eMaxHpAdd = 0.0;

		long now = System.currentTimeMillis();
		for (ActiveNpcEffect eff : activeEffects.values()) {
			if (now >= eff.expiresAt()) {
				continue;
			}
			ePAtkMul *= eff.pAtkMul();
			ePDefMul *= eff.pDefMul();
			eMAtkMul *= eff.mAtkMul();
			eMDefMul *= eff.mDefMul();
			ePAtkSpdMul *= eff.pAtkSpdMul();
			eMAtkSpdMul *= eff.mAtkSpdMul();
			eRunSpdMul *= eff.runSpdMul();
			eMaxHpMul *= eff.maxHpMul();

			ePAtkAdd += eff.pAtkAdd();
			ePDefAdd += eff.pDefAdd();
			eMAtkAdd += eff.mAtkAdd();
			eMDefAdd += eff.mDefAdd();
			ePAtkSpdAdd += eff.pAtkSpdAdd();
			eMAtkSpdAdd += eff.mAtkSpdAdd();
			eRunSpdAdd += eff.runSpdAdd();
			eMaxHpAdd += eff.maxHpAdd();
		}

		this.pAtkMul = Math.max(0.05, basePAtkMul * passivePAtkMul * ePAtkMul + ePAtkAdd);
		this.pDefMul = Math.max(0.05, basePDefMul * passivePDefMul * ePDefMul + ePDefAdd);
		this.mAtkMul = Math.max(0.05, baseMAtkMul * passiveMAtkMul * eMAtkMul + eMAtkAdd);
		this.mDefMul = Math.max(0.05, baseMDefMul * passiveMDefMul * eMDefMul + eMDefAdd);
		this.pAtkSpdMul = Math.max(0.05, basePAtkSpdMul * passivePAtkSpdMul * ePAtkSpdMul + ePAtkSpdAdd);
		this.mAtkSpdMul = Math.max(0.05, baseMAtkSpdMul * passiveMAtkSpdMul * eMAtkSpdMul + eMAtkSpdAdd);
		this.runSpdMul = Math.max(0.05, baseRunSpdMul * passiveRunSpdMul * eRunSpdMul + eRunSpdAdd);
		this.maxHpMul = Math.max(0.05, baseMaxHpMul * passiveMaxHpMul * eMaxHpMul + eMaxHpAdd);
	}

	private void accumulateStatFuncs(java.util.List<com.lopez.l2j.game.skill.StatFunc> funcs, double[] muls, double[] adds) {
		if (funcs == null || funcs.isEmpty() || template == null) {
			return;
		}
		for (var f : funcs) {
			if (f == null || f.stat() == null || f.op() == null) {
				continue;
			}
			String st = f.stat().toLowerCase(java.util.Locale.ROOT);
			double val = f.value();
			if (val <= 0 && f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) {
				continue;
			}
			switch (st) {
				case "patk" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[0] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtk() > 0) adds[0] += (val / template.pAtk());
				}
				case "pdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[1] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pDef() > 0) adds[1] += (val / template.pDef());
				}
				case "matk" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[2] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtk() > 0) adds[2] += (val / template.mAtk());
				}
				case "mdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[3] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mDef() > 0) adds[3] += (val / template.mDef());
				}
				case "patkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[4] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtkSpd() > 0) adds[4] += (val / template.pAtkSpd());
				}
				case "matkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[5] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtkSpd() > 0) adds[5] += (val / template.mAtkSpd());
				}
				case "runspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[6] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.runSpd() > 0) adds[6] += (val / template.runSpd());
				}
				case "maxhp" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) muls[7] *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.maxHp() > 0) adds[7] += (val / template.maxHp());
				}
			}
		}
	}

	private void applyStatFuncsToPassiveMultipliers(java.util.List<com.lopez.l2j.game.skill.StatFunc> funcs) {
		if (funcs == null || funcs.isEmpty() || template == null) {
			return;
		}
		for (var f : funcs) {
			if (f == null || f.stat() == null || f.op() == null) {
				continue;
			}
			String st = f.stat().toLowerCase(java.util.Locale.ROOT);
			double val = f.value();
			if (val <= 0 && f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) {
				continue;
			}
			switch (st) {
				case "patk" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passivePAtkMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtk() > 0) this.passivePAtkMul += (val / template.pAtk());
				}
				case "pdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passivePDefMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pDef() > 0) this.passivePDefMul += (val / template.pDef());
				}
				case "matk" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passiveMAtkMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtk() > 0) this.passiveMAtkMul += (val / template.mAtk());
				}
				case "mdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passiveMDefMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mDef() > 0) this.passiveMDefMul += (val / template.mDef());
				}
				case "patkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passivePAtkSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtkSpd() > 0) this.passivePAtkSpdMul += (val / template.pAtkSpd());
				}
				case "matkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passiveMAtkSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtkSpd() > 0) this.passiveMAtkSpdMul += (val / template.mAtkSpd());
				}
				case "runspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passiveRunSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.runSpd() > 0) this.passiveRunSpdMul += (val / template.runSpd());
				}
				case "maxhp" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.passiveMaxHpMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.maxHp() > 0) this.passiveMaxHpMul += (val / template.maxHp());
				}
			}
		}
	}

	// ---- Propriedades e regras de configuracao (npc.properties / bosses.properties / options.properties) ----

	public boolean isRaid() {
		return template != null && (template.isRaidBoss() || template.isGrandBoss() || isRaidMinion());
	}

	public boolean canBeAttackedByPlayer() {
		return isAttackable() || (com.lopez.l2j.config.Config.ALT_ATTACKABLE_NPCS && !invul);
	}

	public String displayTitle() {
		String baseTitle = champion && championTitle != null && !championTitle.isBlank()
				? championTitle
				: (template != null && template.title() != null ? template.title() : "");
		if (com.lopez.l2j.config.Config.SHOW_NPC_LEVEL && template != null) {
			String lvlPrefix = "Lv " + level() + (template.aggroRange() > 0 ? "* " : " ");
			return baseTitle.isBlank() ? lvlPrefix.trim() : lvlPrefix + baseTitle;
		}
		return baseTitle;
	}

	public boolean isLethalProtected() {
		if (isRaid()) {
			return true;
		}
		if (com.lopez.l2j.config.Config.ALLOW_LETHAL_PROTECTION_MOBS
				&& com.lopez.l2j.config.Config.LETHAL_PROTECTED_MOBS != null) {
			return com.lopez.l2j.config.Config.LETHAL_PROTECTED_MOBS.contains(npcId());
		}
		return false;
	}

	public boolean shouldApplyRaidCurse(int attackerLevel) {
		if (com.lopez.l2j.config.Config.DISABLE_RAID_BOSS_FOSSILIZATION || !isRaid()) {
			return false;
		}
		return (attackerLevel - level()) > com.lopez.l2j.config.Config.MAX_LEVEL_RAID_BOSS_CURSE;
	}

	public long effectiveExpReward() {
		if (template == null) {
			return 0L;
		}
		double rate = isRaid() ? com.lopez.l2j.config.Config.RATE_RAID_EXP : 1.0;
		return Math.round(template.exp() * Math.max(0.0, rate));
	}

	public int effectiveSpReward() {
		if (template == null) {
			return 0;
		}
		double rate = isRaid() ? com.lopez.l2j.config.Config.RATE_RAID_SP : 1.0;
		return (int) Math.round(template.sp() * Math.max(0.0, rate));
	}

	public double effectiveRaidDropMultiplier() {
		return isRaid() ? Math.max(0.0, com.lopez.l2j.config.Config.RATE_RAID_BOSS) : 1.0;
	}

	public double effectiveRaidRegenMultiplier() {
		return isRaid() ? Math.max(0.1, com.lopez.l2j.config.Config.RATE_RAID_REGEN) : 1.0;
	}

	public double effectiveRaidAttackMultiplier() {
		return isRaid() ? Math.max(0.1, com.lopez.l2j.config.Config.RATE_RAID_ATTACK) : 1.0;
	}

	public double effectiveRaidDefenseMultiplier() {
		return isRaid() ? Math.max(0.1, com.lopez.l2j.config.Config.RATE_RAID_DEFENSE) : 1.0;
	}

	public double effectiveRaidMAtkModifier() {
		return isRaid() ? Math.max(0.1, com.lopez.l2j.config.Config.RAID_BOSS_M_ATK_MODIFIER) : 1.0;
	}

	public static int getMountSpeed(boolean wyvern) {
		return wyvern ? com.lopez.l2j.config.Config.WYVERN_SPEED : com.lopez.l2j.config.Config.STRIDER_SPEED;
	}

	public boolean canRentPet() {
		return com.lopez.l2j.config.Config.ALLOW_RENT_PET;
	}

	public boolean canUpgradeWyvern(int crystalCount) {
		return com.lopez.l2j.config.Config.ALLOW_WYVERN_UPGRADER
				&& crystalCount >= com.lopez.l2j.config.Config.MANAGER_CRYSTAL_COUNT;
	}

	public int getRequiredWyvernCrystals() {
		return com.lopez.l2j.config.Config.MANAGER_CRYSTAL_COUNT;
	}

	public int getUrnTempFailChance() {
		return com.lopez.l2j.config.Config.URN_TEMP_FAIL;
	}

	public boolean canWearItems() {
		return com.lopez.l2j.config.Config.ALLOW_WEAR;
	}

	public boolean canUseLottery() {
		return com.lopez.l2j.config.Config.ALLOW_LOTTERY;
	}

	public boolean canUseFishingService() {
		return com.lopez.l2j.config.Config.ALLOW_FISHING;
	}

	public boolean isClassMaster() {
		return npcId() == 31228 || (template != null && "L2ClassMaster".equalsIgnoreCase(template.type()));
	}

	public boolean isClassMasterSpawnEnabled() {
		return com.lopez.l2j.config.Config.SPAWN_CLASS_MASTER;
	}

	public boolean isClassMasterDialogAllowed() {
		return com.lopez.l2j.config.Config.ALLOW_DIALOG_CLASS_MASTER;
	}

	public boolean isClassMasterEntireTree() {
		return com.lopez.l2j.config.Config.CLASS_MASTER_ENTIRE_TREE;
	}

	public boolean canClassMasterUpdateStrider() {
		return com.lopez.l2j.config.Config.CLASS_MASTER_UPDATE_STRIDER;
	}

	public String getClassMasterConfigLine() {
		return com.lopez.l2j.config.Config.CONFIG_CLASS_MASTER;
	}

	public boolean shouldResetToBaseClassIfFail() {
		return com.lopez.l2j.config.Config.RESET_TO_BASE_CLASS_IF_FAIL;
	}

	@Override
	public String toString() {
		return template.name() + " (" + template.id() + ") #" + objectId + " [" + x + "," + y + "," + z + "]";
	}
}
