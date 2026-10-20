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
	@Override
	public double maxHpValue() { return maxHp(); }
	@Override
	public double maxMpValue() { return template != null ? template.maxMp() : 100.0; }
	@Override
	public boolean isNpc() { return true; }
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

	// ---- controle de Reuse (Cooldown), Self-Buffs e Passivas de Monstros ----

	private final java.util.Map<Integer, Long> skillReuses = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<Integer, Long> activeBuffs = new java.util.concurrent.ConcurrentHashMap<>();
	private volatile boolean passivesInitialized;

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
		Long until = activeBuffs.get(skillId);
		if (until == null) {
			return false;
		}
		if (System.currentTimeMillis() < until) {
			return true;
		}
		activeBuffs.remove(skillId, until);
		return false;
	}

	public void applySkillBuff(com.lopez.l2j.game.skill.SkillTemplate skill) {
		if (skill == null) {
			return;
		}
		long durationMs = 60_000L;
		for (var eff : skill.effects()) {
			if (eff.durationMs() > 0) {
				durationMs = Math.max(durationMs, eff.durationMs());
			}
		}
		boolean alreadyActive = hasActiveBuff(skill.id());
		activeBuffs.put(skill.id(), System.currentTimeMillis() + durationMs);
		if (alreadyActive) {
			return;
		}
		applyStatFuncsToMultipliers(skill.funcs());
		for (var eff : skill.effects()) {
			applyStatFuncsToMultipliers(eff.funcs());
		}
	}

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
				applyStatFuncsToMultipliers(sk.funcs());
				for (var eff : sk.effects()) {
					applyStatFuncsToMultipliers(eff.funcs());
				}
			}
		}
		double newMaxHp = maxHp();
		if (newMaxHp != oldMaxHp && oldMaxHp > 0 && !dead) {
			double ratio = currentHp / oldMaxHp;
			currentHp = Math.min(newMaxHp, newMaxHp * ratio);
		}
	}

	private void applyStatFuncsToMultipliers(java.util.List<com.lopez.l2j.game.skill.StatFunc> funcs) {
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
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.pAtkMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtk() > 0) this.pAtkMul += (val / template.pAtk());
				}
				case "pdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.pDefMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pDef() > 0) this.pDefMul += (val / template.pDef());
				}
				case "matk" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.mAtkMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtk() > 0) this.mAtkMul += (val / template.mAtk());
				}
				case "mdef" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.mDefMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mDef() > 0) this.mDefMul += (val / template.mDef());
				}
				case "patkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.pAtkSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.pAtkSpd() > 0) this.pAtkSpdMul += (val / template.pAtkSpd());
				}
				case "matkspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.mAtkSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.mAtkSpd() > 0) this.mAtkSpdMul += (val / template.mAtkSpd());
				}
				case "runspd" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.runSpdMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.runSpd() > 0) this.runSpdMul += (val / template.runSpd());
				}
				case "maxhp" -> {
					if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.MUL) this.maxHpMul *= val;
					else if (f.op() == com.lopez.l2j.game.skill.StatFunc.Op.ADD && template.maxHp() > 0) this.maxHpMul += (val / template.maxHp());
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
