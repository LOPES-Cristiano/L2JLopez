package com.lopez.l2j.game.cubic;

import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.AbnormalEffect;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Instancia de um Cubic ativo orbitando um {@link PlayerCharacter} (porta de {@code L2CubicInstance} de L2JDream / L2JLucera2).
 * Suporta todos os 9 Cubics oficiais do Lineage II Interlude com task periodica de IA para cura, ataque magico,
 * dreno vampirico, veneno, debuff, paralisia, stun, root e aggro (#180, #255, #256, #257).
 */
public final class CubicInstance {

	// Os 9 Cubics oficiais do Lineage II Interlude
	public static final int STORM_CUBIC = 1;
	public static final int VAMPIRIC_CUBIC = 2;
	public static final int LIFE_CUBIC = 3;
	public static final int VIPER_CUBIC = 4;
	public static final int POLTERGEIST_CUBIC = 5;
	public static final int PHANTOM_CUBIC = 5;
	public static final int BINDING_CUBIC = 6;
	public static final int AQUA_CUBIC = 7;
	public static final int SPARK_CUBIC = 8;
	public static final int ATTRACTIVE_CUBIC = 9;

	// Skills executadas pelos Cubics (data/xml/stats/skills/4000-4099.xml, 4100-4199.xml, 5100-5199.xml)
	public static final int SKILL_STORM_FLARE = 4049;
	public static final int SKILL_VAMPIRIC_DRAIN = 4050;
	public static final int SKILL_LIFE_HEAL = 4051;
	public static final int SKILL_VIPER_POISON = 4052;
	public static final int SKILL_PHANTOM_DECREASE_PATK = 4053;
	public static final int SKILL_PHANTOM_DECREASE_PDEF = 4054;
	public static final int SKILL_PHANTOM_DECREASE_ATKSPD = 4055;
	public static final int SKILL_BINDING_PARALYZE = 4164;
	public static final int SKILL_AQUA_FROST = 4165;
	public static final int SKILL_SPARK_SHOCK = 4166;
	public static final int SKILL_ATTRACTIVE_HOLD = 5115;
	public static final int SKILL_ATTRACTIVE_HATE = 5116;

	public static final long DEFAULT_CUBIC_LIFETIME_MS = 900_000L; // 15 minutos
	public static final int MAX_CUBIC_RANGE = 900;

	private static final ScheduledExecutorService CUBIC_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
		Thread t = new Thread(r, "cubic-ai-scheduler");
		t.setDaemon(true);
		return t;
	});

	private final int id;
	private final int level;
	private final PlayerCharacter owner;
	private final boolean givenByOther;
	private final int mAtk;
	private final int activationDelaySec;
	private final int activationChance;
	private final long totalLifeTimeMs;
	private final long spawnTimeMillis;
	private final List<Integer> skillIds;

	private volatile boolean active = true;
	private volatile Creature currentTarget;
	private volatile int actionsExecuted;
	private volatile ScheduledFuture<?> actionTask;
	private volatile ScheduledFuture<?> disappearTask;
	private volatile Runnable onExpireCallback;

	public CubicInstance(
			PlayerCharacter owner,
			int id,
			int level,
			boolean givenByOther) {
		this(owner, id, level, givenByOther, resolveDefaultMAtk(level), resolveDefaultDelaySec(id),
				resolveDefaultChance(id), DEFAULT_CUBIC_LIFETIME_MS);
	}

	public CubicInstance(
			PlayerCharacter owner,
			int id,
			int level,
			boolean givenByOther,
			int mAtk,
			int activationDelaySec,
			int activationChance,
			long totalLifeTimeMs) {
		this.owner = owner;
		this.id = id;
		this.level = Math.max(1, level);
		this.givenByOther = givenByOther;
		this.mAtk = Math.max(1, mAtk);
		this.activationDelaySec = Math.max(1, activationDelaySec);
		this.activationChance = Math.clamp(activationChance, 1, 100);
		this.totalLifeTimeMs = Math.max(1_000L, totalLifeTimeMs);
		this.spawnTimeMillis = System.currentTimeMillis();
		this.skillIds = resolveCubicSkillIds(id);
	}

	public static int resolveDefaultMAtk(int level) {
		int lv = Math.max(1, level);
		return 180 + lv * 115;
	}

	public static int resolveDefaultDelaySec(int cubicId) {
		return switch (cubicId) {
			case STORM_CUBIC -> 10;
			case VAMPIRIC_CUBIC -> 13;
			case LIFE_CUBIC -> 10;
			case VIPER_CUBIC -> 15;
			case POLTERGEIST_CUBIC -> 12;
			case BINDING_CUBIC -> 14;
			case AQUA_CUBIC -> 11;
			case SPARK_CUBIC -> 12;
			case ATTRACTIVE_CUBIC -> 10;
			default -> 10;
		};
	}

	public static int resolveDefaultChance(int cubicId) {
		return switch (cubicId) {
			case LIFE_CUBIC -> 100;
			case STORM_CUBIC, AQUA_CUBIC, ATTRACTIVE_CUBIC -> 80;
			default -> 70;
		};
	}

	public static List<Integer> resolveCubicSkillIds(int cubicId) {
		return switch (cubicId) {
			case STORM_CUBIC -> List.of(SKILL_STORM_FLARE);
			case VAMPIRIC_CUBIC -> List.of(SKILL_VAMPIRIC_DRAIN);
			case LIFE_CUBIC -> List.of(SKILL_LIFE_HEAL);
			case VIPER_CUBIC -> List.of(SKILL_VIPER_POISON);
			case POLTERGEIST_CUBIC -> List.of(SKILL_PHANTOM_DECREASE_PATK, SKILL_PHANTOM_DECREASE_PDEF, SKILL_PHANTOM_DECREASE_ATKSPD);
			case BINDING_CUBIC -> List.of(SKILL_BINDING_PARALYZE);
			case AQUA_CUBIC -> List.of(SKILL_AQUA_FROST);
			case SPARK_CUBIC -> List.of(SKILL_SPARK_SHOCK);
			case ATTRACTIVE_CUBIC -> List.of(SKILL_ATTRACTIVE_HOLD, SKILL_ATTRACTIVE_HATE);
			default -> List.of(SKILL_STORM_FLARE);
		};
	}

	public int id() {
		return id;
	}

	public int level() {
		return level;
	}

	public PlayerCharacter owner() {
		return owner;
	}

	public boolean givenByOther() {
		return givenByOther;
	}

	public int mAtk() {
		return mAtk;
	}

	public int activationDelaySec() {
		return activationDelaySec;
	}

	public int activationChance() {
		return activationChance;
	}

	public long totalLifeTimeMs() {
		return totalLifeTimeMs;
	}

	public long spawnTimeMillis() {
		return spawnTimeMillis;
	}

	public List<Integer> skillIds() {
		return skillIds;
	}

	public boolean isActive() {
		return active && (System.currentTimeMillis() - spawnTimeMillis < totalLifeTimeMs);
	}

	public boolean isLifeCubic() {
		return id == LIFE_CUBIC;
	}

	public boolean isOffensiveCubic() {
		return id != LIFE_CUBIC;
	}

	public Creature currentTarget() {
		return currentTarget;
	}

	public void currentTarget(Creature target) {
		this.currentTarget = target;
	}

	public int actionsExecuted() {
		return actionsExecuted;
	}

	public void onExpireCallback(Runnable callback) {
		this.onExpireCallback = callback;
	}

	public synchronized void start() {
		stopTasksOnly();
		this.active = true;
		long periodMs = this.activationDelaySec * 1000L;
		this.actionTask = CUBIC_SCHEDULER.scheduleAtFixedRate(() -> {
			try {
				if (!isActive() || owner == null || owner.isDead()) {
					return;
				}
				if (id == LIFE_CUBIC || owner.isInCombat() || currentTarget != null) {
					if (ThreadLocalRandom.current().nextInt(100) < activationChance) {
						doAction(currentTarget, null);
					}
				}
			} catch (Exception ignored) {
			}
		}, periodMs, periodMs, TimeUnit.MILLISECONDS);

		this.disappearTask = CUBIC_SCHEDULER.schedule(this::stop, this.totalLifeTimeMs, TimeUnit.MILLISECONDS);
	}

	private synchronized void stopTasksOnly() {
		ScheduledFuture<?> at = this.actionTask;
		this.actionTask = null;
		if (at != null) {
			at.cancel(false);
		}
		ScheduledFuture<?> dt = this.disappearTask;
		this.disappearTask = null;
		if (dt != null) {
			dt.cancel(false);
		}
	}

	public synchronized void stop() {
		this.active = false;
		stopTasksOnly();
		Runnable cb = this.onExpireCallback;
		this.onExpireCallback = null;
		if (cb != null) {
			cb.run();
		}
	}

	public void cancel() {
		stop();
	}

	/**
	 * Executa imediatamente a acao de IA do Cubic (cura para Life Cubic ou ataque/debuff para Cubics ofensivos).
	 */
	public boolean doAction() {
		return doAction(this.currentTarget, null);
	}

	public boolean doAction(Creature explicitTarget) {
		return doAction(explicitTarget, null);
	}

	public boolean doAction(Creature explicitTarget, Collection<PlayerCharacter> partyMembers) {
		if (!active || owner == null || owner.isDead()) {
			return false;
		}
		if (id == LIFE_CUBIC) {
			return executeLifeCubicHeal(partyMembers);
		}
		Creature target = explicitTarget != null ? explicitTarget : this.currentTarget;
		if (target == null || target.isDead()) {
			return false;
		}
		return executeOffensiveCubicSkill(target);
	}

	/**
	 * Life Cubic (id = 3): seleciona o membro vivo da party (ou dono / summon) com menor percentual de HP
	 * dentro do raio de alcance (900) e aplica a cura (skill 4051).
	 */
	public boolean executeLifeCubicHeal(Collection<PlayerCharacter> partyMembers) {
		if (owner == null || owner.isDead()) {
			return false;
		}
		Creature bestTarget = null;
		double lowestRatio = 1.0;

		if (owner.currentHp() < owner.maxHpValue()) {
			lowestRatio = owner.currentHp() / Math.max(1.0, owner.maxHpValue());
			bestTarget = owner;
		}

		SummonInstance ownerSummon = owner.activeSummon();
		if (ownerSummon != null && !ownerSummon.isDead() && !ownerSummon.isHealBlocked()) {
			double sRatio = ownerSummon.currentHp() / Math.max(1.0, ownerSummon.maxHpValue());
			if (sRatio < lowestRatio && isWithinRange(owner, ownerSummon)) {
				lowestRatio = sRatio;
				bestTarget = ownerSummon;
			}
		}

		if (partyMembers != null) {
			for (PlayerCharacter member : partyMembers) {
				if (member == null || member.isDead() || !isWithinRange(owner, member)) {
					continue;
				}
				double ratio = member.currentHp() / Math.max(1.0, member.maxHpValue());
				if (ratio < lowestRatio) {
					lowestRatio = ratio;
					bestTarget = member;
				}
			}
		}

		if (bestTarget == null || lowestRatio >= 1.0) {
			return false;
		}

		int healPower = 150 + this.level * 45 + (int) Math.round(Math.sqrt(this.mAtk) * 4.0);
		if (bestTarget instanceof PlayerCharacter pc) {
			pc.currentHp(Math.min(pc.maxHp(), pc.currentHp() + healPower));
		} else if (bestTarget instanceof SummonInstance sum) {
			sum.applyHeal(healPower);
		} else {
			bestTarget.currentHp(Math.min(bestTarget.maxHpValue(), bestTarget.currentHp() + healPower));
		}

		this.actionsExecuted++;
		broadcastCubicSkillUse(bestTarget, SKILL_LIFE_HEAL);
		return true;
	}

	/**
	 * Executa a habilidade ofensiva / debuff / aggro do Cubic sobre o alvo de combate do dono.
	 */
	public boolean executeOffensiveCubicSkill(Creature target) {
		if (target == null || target.isDead() || target == owner || !isWithinRange(owner, target)) {
			return false;
		}

		int chosenSkillId = skillIds.isEmpty()
				? SKILL_STORM_FLARE
				: skillIds.get(ThreadLocalRandom.current().nextInt(skillIds.size()));
		int targetMDef = resolveTargetMDef(target);
		int baseMagicDamage = Math.max(15, (int) Math.round(91.0 * Math.sqrt(this.mAtk) * (35.0 + this.level * 12.0) / Math.max(50, targetMDef)));
		long now = System.currentTimeMillis();

		switch (id) {
			case STORM_CUBIC -> {
				// 4049 Cubic Flare (MDAM)
				inflictDamage(target, baseMagicDamage);
			}
			case VAMPIRIC_CUBIC -> {
				// 4050 Cubic Drain (DRAIN: causa dano magico e absorve HP para o dono)
				inflictDamage(target, baseMagicDamage);
				int drained = Math.max(1, (int) Math.round(baseMagicDamage * 0.40));
				owner.currentHp(Math.min(owner.maxHp(), owner.currentHp() + drained));
			}
			case VIPER_CUBIC -> {
				// 4052 Cubic Poison (POISON + dano inicial)
				int poisonDmg = Math.max(10, baseMagicDamage / 2);
				inflictDamage(target, poisonDmg);
				target.startAbnormalEffect(AbnormalEffect.POISON);
			}
			case POLTERGEIST_CUBIC -> {
				// 4053 Decrease P.Atk, 4054 Decrease P.Def, 4055 Decrease Atk.Spd
				if (target instanceof NpcInstance npc) {
					if (chosenSkillId == SKILL_PHANTOM_DECREASE_PATK) {
						npc.pAtkMul(Math.max(0.5, npc.pAtkMul() * 0.77));
					} else if (chosenSkillId == SKILL_PHANTOM_DECREASE_PDEF) {
						npc.pDefMul(Math.max(0.5, npc.pDefMul() * 0.77));
					} else {
						npc.pAtkSpdMul(Math.max(0.5, npc.pAtkSpdMul() * 0.77));
					}
				}
			}
			case BINDING_CUBIC -> {
				// 4164 Cubic Paralyze (PARALYZE)
				target.disable(now + 10_000L, false);
				target.startAbnormalEffect(AbnormalEffect.HOLD_2);
			}
			case AQUA_CUBIC -> {
				// 4165 Cubic Frost (MDAM Water + MDOT)
				inflictDamage(target, baseMagicDamage);
			}
			case SPARK_CUBIC -> {
				// 4166 Cubic Shock (MDAM + STUN)
				inflictDamage(target, Math.max(10, baseMagicDamage * 2 / 3));
				target.disable(now + 9_000L, false);
				target.startAbnormalEffect(AbnormalEffect.STUN);
			}
			case ATTRACTIVE_CUBIC -> {
				// 5115 Cubic Hold (ROOT) + 5116 Cubic Hate (AGGDAMAGE)
				target.root(now + 10_000L);
				target.startAbnormalEffect(AbnormalEffect.ROOT);
				if (target instanceof NpcInstance npc) {
					npc.targetPlayerId(owner.objectId());
				}
			}
			default -> {
				inflictDamage(target, baseMagicDamage);
			}
		}

		this.actionsExecuted++;
		broadcastCubicSkillUse(target, chosenSkillId);
		return true;
	}

	private void inflictDamage(Creature target, int damage) {
		if (damage <= 0 || target.invul() || target.isDead()) {
			return;
		}
		if (target instanceof NpcInstance npc) {
			npc.onDamage(owner.objectId(), damage);
		} else if (target instanceof SummonInstance sum) {
			sum.applyDamage(damage);
		} else if (target instanceof PlayerCharacter pc) {
			double cp = pc.currentCp();
			if (cp >= damage) {
				pc.currentCp(cp - damage);
			} else {
				pc.currentCp(0.0);
				pc.currentHp(Math.max(0.0, pc.currentHp() - (damage - cp)));
			}
			pc.onDamaged();
		} else {
			target.currentHp(Math.max(0.0, target.currentHp() - damage));
			target.onDamaged();
		}
	}

	private static int resolveTargetMDef(Creature target) {
		if (target instanceof NpcInstance npc && npc.template() != null) {
			return Math.max(50, npc. effectiveMDef());
		}
		if (target instanceof SummonInstance sum) {
			return Math.max(50, sum.mDef());
		}
		return 150;
	}

	private static boolean isWithinRange(Creature a, Creature b) {
		if (a == null || b == null || a.instanceId() != b.instanceId()) {
			return false;
		}
		long dx = (long) a.x() - b.x();
		long dy = (long) a.y() - b.y();
		return dx * dx + dy * dy <= (long) MAX_CUBIC_RANGE * MAX_CUBIC_RANGE;
	}

	private void broadcastCubicSkillUse(Creature target, int skillId) {
		GameWorld world = GameWorld.getInstance();
		if (world == null || owner == null || target == null) {
			return;
		}
		world.broadcastAround(owner.x(), owner.y(), GameWorld.VISIBILITY_RADIUS, owner.instanceId(),
				new GameServerPacket.MagicSkillUse(
						owner.objectId(), target.objectId(), skillId, this.level, 0, 0,
						owner.x(), owner.y(), owner.z()));
	}
}
