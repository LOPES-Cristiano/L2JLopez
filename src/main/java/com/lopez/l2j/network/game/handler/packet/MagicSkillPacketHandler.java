package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ExperienceTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.skill.Skill;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handler modular para conjuracao de magias, execucao de habilidades, calculo
 * de dano magico,
 * aplicacao de efeitos (buffs, debuffs, HoT, DoT), spoil, cura, ressurreicao e
 * combate magico.
 */
public class MagicSkillPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(MagicSkillPacketHandler.class);

	private final GameSession session;
	private final Map<Integer, Long> skillReuse = new ConcurrentHashMap<>();
	private volatile ScheduledFuture<?> approachCastTask;
	private volatile int approachingSkillId;

	public MagicSkillPacketHandler(GameSession session) {
		this.session = session;
	}

	public boolean isApproachingToCast() {
		return approachCastTask != null && !approachCastTask.isDone();
	}

	public void cancelApproachCastTask() {
		if (approachCastTask != null && !approachCastTask.isDone()) {
			approachCastTask.cancel(false);
		}
		approachCastTask = null;
		approachingSkillId = 0;
	}

	public GameSession session() {
		return session;
	}

	public Map<Integer, Long> skillReuse() {
		return skillReuse;
	}

	public void clearSkillReuse() {
		skillReuse.clear();
	}

	private void send(GameServerPacket packet) {
		session.send(packet);
	}

	private PlayerCharacter active() {
		return session.active();
	}

	private GameSession.Context ctx() {
		return session.ctx();
	}

	public void finishCast(SkillTemplate sk) {
		finishCast(sk, null, null, null, false, false, false);
	}

	public void grantNoblesseStatus(PlayerCharacter p) {
		if (p == null)
			return;
		if (!p.isNoble() && p.level() >= 75) {
			p.setNoble(true);
			if (p == active()) {
				send(SystemMessage.id(SystemMessage.YOU_HAVE_BECOME_A_NOBLESSE));
				send(new SocialAction(p.objectId(), 16));
				session.sendUserInfoAndBroadcastCharInfo();
			}
			if (ctx() != null && ctx().characters() != null) {
				ctx().characters().save(p, true);
			}
		}
	}

	public int getEffectiveMpConsume(SkillTemplate sk, int baseMp) {
		if (baseMp <= 0 || active() == null || sk == null) {
			return Math.max(0, baseMp);
		}
		double effectiveBase = baseMp;
		// #128: Custo adicional progressivo de MP para Dances e Songs por danca/musica
		// ja ativa
		if (sk.isDanceOrSong() && baseMp == sk.mpConsume() && active().effects() != null) {
			long activeDanceCount = active().effects().active().stream()
					.filter(b -> !b.isDebuff() && !b.isPermanent() && PlayerEffects.isDanceOrSong(b.skillId()))
					.count();
			if (activeDanceCount > 0) {
				effectiveBase += activeDanceCount * 30.0;
			}
		}
		String rateStat = sk.isDanceOrSong()
				? com.lopez.l2j.game.stats.Stat.DANCE_MP_CONSUME_RATE.getValue()
				: (sk.magic()
						? com.lopez.l2j.game.stats.Stat.MAGICAL_MP_CONSUME_RATE.getValue()
						: com.lopez.l2j.game.stats.Stat.PHYSICAL_MP_CONSUME_RATE.getValue());
		double rate = PlayerStats.applyStat(active(), rateStat, 1.0);
		return Math.max(0, (int) Math.round(effectiveBase * rate));
	}

	public void castSkill(SkillTemplate sk, boolean mayMove) {
		if (!session.inWorld() || active() == null || active().isDead() || active().isDisabled()) {
			return;
		}
		if (sk.magic() && active().isMuted()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (!sk.magic() && active().isPhysicalMuted()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (session.casting()) {
			send(new ActionFailed());
			return;
		}
		long now = System.currentTimeMillis();
		Long readyAt = skillReuse.get(sk.id());
		if (readyAt != null && readyAt > now) {
			send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (session.isBow(session.activeWeapon()) && !sk.magic()) {
			if (!session.checkAndConsumeArrow()) {
				return;
			}
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			int have = active().inventory().byItemId(sk.itemConsumeId()).map(ItemInstance::count).orElse(0);
			if (have < sk.itemConsumeCount()) {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED,
						new SystemMessage.SkillName(sk.id(), sk.level())));
				send(SystemMessage.id(351)); // NOT_ENOUGH_ITEMS (#096)
				send(new ActionFailed());
				return;
			}
		}
		int effMpInitial = getEffectiveMpConsume(sk, sk.mpInitialConsume());
		int effMpConsume = getEffectiveMpConsume(sk, sk.mpConsume());
		if (active().currentMp() < effMpConsume + effMpInitial) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			send(new ActionFailed());
			return;
		}
		if (sk.hpConsume() > 0 && active().currentHp() <= sk.hpConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_HP));
			send(new ActionFailed());
			return;
		}
		if (sk.needCharges() > 0 && active().charges() < sk.needCharges()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (sk.giveCharges() > 0 && !sk.continueAfterMax() && active().charges() >= sk.maxCharges()) {
			send(SystemMessage.id(SystemMessage.FORCE_MAXLEVEL_REACHED));
			send(new ActionFailed());
			return;
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			if (active().inventory().getItemCount(sk.itemConsumeId()) < sk.itemConsumeCount()) {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(sk.itemConsumeId())));
				send(new ActionFailed());
				return;
			}
		}

		// Alvo principal
		NpcInstance npcTarget = null;
		DoorInstance doorTarget = null;
		GameSession playerTarget = null;
		com.lopez.l2j.game.summon.SummonInstance summonTarget = null;
		boolean isAreaSweep = "TARGET_AREA_CORPSE_MOB".equalsIgnoreCase(sk.target())
				|| sk.id() == 444
				|| ("SWEEP".equalsIgnoreCase(sk.skillType()) && sk.isAreaAroundSelf());
		boolean isHarvest = "HARVEST".equalsIgnoreCase(sk.skillType()) || sk.id() == 2098;
		boolean isSweep = isAreaSweep
				|| "SWEEP".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 42;
		boolean isCorpseMobSkill = !isSweep && !isHarvest
				&& ("TARGET_CORPSE_MOB".equalsIgnoreCase(sk.target())
						|| sk.id() == 1129 || sk.id() == 1154 || sk.id() == 1334);
		boolean isResurrect = !isSweep && !isHarvest && !isCorpseMobSkill
				&& ("RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_"));
		boolean isUnlock = "UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "TARGET_UNLOCKABLE".equalsIgnoreCase(sk.target());
		boolean isSiegeAssault = "STRSIEGEASSAULT".equalsIgnoreCase(sk.skillType()) || sk.id() == 325;
		boolean isEraseOrBetray = "ERASE".equalsIgnoreCase(sk.skillType()) || "BETRAY".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 1395 || sk.id() == 1380;
		boolean isMobUtilitySkill = "SOW".equalsIgnoreCase(sk.skillType())
				|| "DRAIN_SOUL".equalsIgnoreCase(sk.skillType())
				|| "BEAST_FEED".equalsIgnoreCase(sk.skillType())
				|| "CONFUSION".equalsIgnoreCase(sk.skillType())
				|| "CONFUSE_MOB_ONLY".equalsIgnoreCase(sk.skillType())
				|| "AGGREMOVE".equalsIgnoreCase(sk.skillType())
				|| "AGGREDUCE".equalsIgnoreCase(sk.skillType())
				|| "AGGREDUCE_CHAR".equalsIgnoreCase(sk.skillType())
				|| "AGGDEBUFF".equalsIgnoreCase(sk.skillType())
				|| "AGGDAMAGE".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 2097 || sk.id() == 2099 || sk.id() == 2188 || sk.id() == 2189;
		boolean isSummonTargetOnly = "TARGET_SUMMON".equalsIgnoreCase(sk.target())
				|| "TARGET_PET".equalsIgnoreCase(sk.target());
		boolean isHealSkill = "HEAL".equalsIgnoreCase(sk.skillType())
				|| "HEAL_PERCENT".equalsIgnoreCase(sk.skillType())
				|| "HEAL_STATIC".equalsIgnoreCase(sk.skillType());

		if (isSweep) {
			npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && n.isDead())
					.orElse(null);
			if (isAreaSweep) {
				int sweepRadius = Math.max(250, sk.skillRadius() > 0 ? sk.skillRadius() : 300);
				if (npcTarget != null && (npcTarget.isSweepable() || npcTarget.isSpoiled())
						&& canPlayerSweepCorpse(npcTarget)) {
					// Usa o cadaver selecionado como centro de referencia
				} else {
					npcTarget = null;
					boolean hasAnySweepable = false;
					boolean hasAnySpoiledByOther = false;
					for (NpcInstance corpse : ctx().world().findNpcsAround(active().x(), active().y(), sweepRadius,
							active().instanceId())) {
						if (corpse != null && corpse.isAttackable() && corpse.isDead()
								&& (corpse.isSweepable() || corpse.isSpoiled())) {
							if (canPlayerSweepCorpse(corpse)) {
								hasAnySweepable = true;
								break;
							} else {
								hasAnySpoiledByOther = true;
							}
						}
					}
					if (!hasAnySweepable) {
						send(SystemMessage.id(hasAnySpoiledByOther ? SystemMessage.SWEEP_NOT_ALLOWED
								: SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
						send(new ActionFailed());
						return;
					}
				}
			} else {
				if (npcTarget == null) {
					send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					send(new ActionFailed());
					return;
				}
				if (!npcTarget.isSweepable() && !npcTarget.isSpoiled()) {
					send(SystemMessage.id(SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					send(new ActionFailed());
					return;
				}
				if (!canPlayerSweepCorpse(npcTarget)) {
					send(SystemMessage.id(SystemMessage.SWEEP_NOT_ALLOWED));
					send(new ActionFailed());
					return;
				}
			}
		} else if (isHarvest || isCorpseMobSkill) {
			npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && n.isDead())
					.orElse(null);
			if (npcTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (isUnlock) {
			npcTarget = ctx().world().npc(session.targetObjectId())
					.filter(n -> !n.isDead() && GameSession.isChestNpc(n)).orElse(null);
			if (npcTarget == null && ctx().doors() != null) {
				doorTarget = ctx().doors().door(session.targetObjectId());
			}
			if (npcTarget == null && doorTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
			if (doorTarget != null && !doorTarget.unlockable()) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (isSiegeAssault) {
			if (ctx().doors() != null) {
				doorTarget = ctx().doors().door(session.targetObjectId());
			}
			if (doorTarget == null) {
				npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && !n.isDead())
						.orElse(null);
			}
			if (doorTarget == null && npcTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (isEraseOrBetray) {
			summonTarget = ctx().world().summon(session.targetObjectId()).filter(s -> !s.isDead()).orElse(null);
			if (summonTarget != null) {
				var ownerSess = ctx().world().player(summonTarget.ownerId()).orElse(null);
				if (ownerSess instanceof GameSession gs) {
					playerTarget = gs;
				}
			} else {
				var other = ctx().world().player(session.targetObjectId()).orElse(null);
				if (other instanceof GameSession gs && gs.active() != null && gs.active().activeSummon() != null
						&& !gs.active().activeSummon().isDead()) {
					playerTarget = gs;
					summonTarget = gs.active().activeSummon();
				} else {
					send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					send(new ActionFailed());
					return;
				}
			}
		} else if (isMobUtilitySkill) {
			npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && !n.isDead())
					.orElse(null);
			if (npcTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (sk.isOffensive()) {
			if (!sk.isAreaAroundSelf()) {
				npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && !n.isDead())
						.orElse(null);
				if (npcTarget == null) {
					var other = ctx().world().player(session.targetObjectId()).orElse(null);
					if (other instanceof GameSession gs && gs.active() != null && !gs.active().isDead()
							&& gs != session) {
						playerTarget = gs;
					} else {
						var sumOpt = ctx().world().summon(session.targetObjectId()).filter(s -> !s.isDead());
						if (sumOpt.isPresent()) {
							summonTarget = sumOpt.get();
							var ownerSess = ctx().world().player(summonTarget.ownerId()).orElse(null);
							if (ownerSess instanceof GameSession gs && gs != session) {
								playerTarget = gs;
							}
						} else if (ctx().doors() != null && ctx().doors().door(session.targetObjectId()) != null) {
							doorTarget = ctx().doors().door(session.targetObjectId());
						} else {
							send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
							send(new ActionFailed());
							return;
						}
					}
				}
			}
		} else if (isResurrect) {
			if (session.targetObjectId() != 0 && session.targetObjectId() != active().objectId()) {
				var other = ctx().world().player(session.targetObjectId()).orElse(null);
				if (other instanceof GameSession gs && gs.active() != null && gs.active().isDead()) {
					playerTarget = gs;
				} else {
					send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					send(new ActionFailed());
					return;
				}
			} else {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (isSummonTargetOnly) {
			if (active().activeSummon() != null && !active().activeSummon().isDead()) {
				summonTarget = active().activeSummon();
			} else {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (!sk.isSelfTargeted() && session.targetObjectId() != 0
				&& session.targetObjectId() != active().objectId()) {
			if (active().activeSummon() != null && !active().activeSummon().isDead()
					&& active().activeSummon().objectId() == session.targetObjectId()) {
				summonTarget = active().activeSummon();
			} else if (ctx().world().summon(session.targetObjectId()).filter(s -> !s.isDead()).isPresent()) {
				summonTarget = ctx().world().summon(session.targetObjectId()).get();
			} else if (isHealSkill) {
				var mobOpt = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && !n.isDead());
				if (mobOpt.isPresent()) {
					npcTarget = mobOpt.get();
				}
			}
			if (summonTarget == null && npcTarget == null) {
				var other = ctx().world().player(session.targetObjectId()).orElse(null);
				if (other instanceof GameSession gs && gs.active() != null && !gs.active().isDead()) {
					playerTarget = gs;
				} else {
					playerTarget = session;
				}
			}
		} else {
			playerTarget = session;
		}

		// #096: Verificacao de Zona de Paz para habilidades ofensivas com SystemMessage
		// oficial (84 / 85)
		if (sk.isOffensive() && ctx().zones() != null && playerTarget != null && playerTarget != session) {
			if (ctx().zones().isInsidePeace(active().x(), active().y(), active().z())) {
				send(SystemMessage.id(SystemMessage.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE));
				send(new ActionFailed());
				return;
			}
			if (ctx().zones().isInsidePeace(playerTarget.x(), playerTarget.y(), playerTarget.z())) {
				send(SystemMessage.id(SystemMessage.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE));
				send(new ActionFailed());
				return;
			}
		}

		// Bloqueia suporte (cura/buff) a jogadores flaggados ou PK a partir de zona de
		// paz
		if (!sk.isOffensive() && ctx().zones() != null && playerTarget != null && playerTarget != session) {
			if (ctx().zones().isInsidePeace(active().x(), active().y(), active().z())
					&& (playerTarget.active().pvpFlag() > 0 || playerTarget.active().karma() > 0)) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		}

		// #184: Validacao de todas as condicoes da skill (castCondition e conditions)
		// antes do cast (exceto Area Sweep sem alvo selecionado)
		Object resolvedTarget = npcTarget != null ? npcTarget
				: (summonTarget != null ? summonTarget
						: (doorTarget != null ? doorTarget : (playerTarget != null ? playerTarget.active() : null)));
		boolean condOk = isAreaSweep
				|| (sk.castCondition() == null || sk.castCondition().test(active(), resolvedTarget));
		if (condOk && !isAreaSweep && sk.conditions() != null) {
			for (var cond : sk.conditions()) {
				if (cond != null && !cond.test(active(), resolvedTarget)) {
					condOk = false;
					break;
				}
			}
		}
		if (!condOk) {
			if (sk.condMsg() != null && !sk.condMsg().isBlank()) {
				try {
					int msgId = Integer.parseInt(sk.condMsg().trim());
					send(SystemMessage.id(msgId));
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", sk.condMsg()));
				}
			} else {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED,
						new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			send(new ActionFailed());
			return;
		}

		// Alcance (o L2J anda ate o alvo e depois conjura)
		int tx = npcTarget != null ? npcTarget.x()
				: (doorTarget != null ? doorTarget.x()
						: (summonTarget != null ? summonTarget.x()
								: (playerTarget != null ? playerTarget.x() : active().x())));
		int ty = npcTarget != null ? npcTarget.y()
				: (doorTarget != null ? doorTarget.y()
						: (summonTarget != null ? summonTarget.y()
								: (playerTarget != null ? playerTarget.y() : active().y())));
		int tz = npcTarget != null ? npcTarget.z()
				: (doorTarget != null ? doorTarget.z()
						: (summonTarget != null ? summonTarget.z()
								: (playerTarget != null ? playerTarget.z() : active().z())));
		if (sk.castRange() > 0 && (npcTarget != null || doorTarget != null || summonTarget != null
				|| (playerTarget != null && playerTarget != session))) {
			double dist = Math.hypot(active().x() - tx, active().y() - ty);
			if (dist > 3000.0) {
				send(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
				send(new ActionFailed());
				session.clearTarget();
				return;
			}
			double maxDist = sk.castRange() + 70 + (npcTarget != null ? npcTarget.template().collisionRadius() : 0);
			if (dist > maxDist) {
				if (!mayMove) {
					send(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
					send(new ActionFailed());
					return;
				}
				int targetId = npcTarget != null ? npcTarget.objectId()
						: (doorTarget != null ? doorTarget.objectId()
								: (summonTarget != null ? summonTarget.objectId() : playerTarget.objectId()));
				if (isApproachingToCast() && approachingSkillId == sk.id() && session.targetObjectId() == targetId) {
					return;
				}
				cancelApproachCastTask();
				var move = new MoveToPawn(active().objectId(), targetId, sk.castRange(), active().x(), active().y(),
						active().z());
				send(move);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
				int run = Math.max(50,
						PlayerStats.calculate(active(), ctx().characters().template(active())).runSpeed());
				long travelMs = (long) ((dist - sk.castRange()) * 1000 / run) + 200;
				double angle = Math.atan2(active().y() - ty, active().x() - tx);
				int stopDist = Math.max(20, sk.castRange() - 30);
				int stopX = (int) (tx + stopDist * Math.cos(angle));
				int stopY = (int) (ty + stopDist * Math.sin(angle));
				int stopZ = tz;
				PlayerCharacter owner = active();
				approachingSkillId = sk.id();
				approachCastTask = GameSession.autoAttackScheduler().schedule(() -> {
					approachingSkillId = 0;
					approachCastTask = null;
					if (active() == owner && !owner.isDead() && session.inWorld()) {
						active().moveTo(stopX, stopY, stopZ);
						castSkill(sk, false);
					}
				}, travelMs, TimeUnit.MILLISECONDS);
				return;
			}
		}

		// Line of Sight (LoS) check for targeted skills
		if (sk.isOffensive() && (npcTarget != null || (playerTarget != null && playerTarget != session))) {
			boolean canSee = true;
			if (npcTarget != null) {
				canSee = ctx().combat() == null || ctx().combat().canSeeTarget(active(), npcTarget);
			} else if (playerTarget != null && playerTarget.active() != null) {
				canSee = ctx().combat() == null || ctx().combat().canSeeTarget(active(), playerTarget.active());
			}
			if (!canSee) {
				send(SystemMessage.id(SystemMessage.CANT_SEE_TARGET));
				send(new ActionFailed());
				return;
			}
		}

		// Inicio da conjuracao
		var t = ctx() != null && ctx().characters() != null ? ctx().characters().template(active()) : null;
		var stats = PlayerStats.calculate(active(), t);
		if (sk.magic() && !session.spiritshotCharged()) {
			session.rechargeAutoSoulShots();
		}
		boolean sps = sk.magic() && session.spiritshotCharged() && !session.blessedSpiritshot();
		boolean bss = sk.magic() && session.spiritshotCharged() && session.blessedSpiritshot();
		session.spiritshotCharged(false);

		double speedFactor = 333.0 / Math.max(1, sk.magic() ? stats.mAtkSpd() : stats.pAtkSpd());
		if (bss) {
			speedFactor *= 0.65;
		}
		int minHit = (active().isGm() || active().gmSpeed() > 0) ? 10 : Config.getInt("MinimumHitTime", 330);
		int hitTime = sk.hitTime() > 0 ? Math.max(minHit, (int) (sk.hitTime() * speedFactor)) : 0;
		// #128: Calculo de reuseDelay com Formulas.calcSkillReuseDelay e suporte a
		// staticReuse e musicReuse/atkReuse
		int reuse;
		if (sk.staticReuse()) {
			reuse = sk.reuseDelay();
		} else {
			reuse = com.lopez.l2j.game.combat.Formulas.calcSkillReuseDelay(
					sk.reuseDelay(),
					sk.isPotion(),
					sk.magic(),
					active().isOlympiadMode(),
					stats.pAtkSpd(),
					stats.mAtkSpd());
			String reuseStat = sk.isDanceOrSong()
					? com.lopez.l2j.game.stats.Stat.MUSIC_REUSE_RATE.getValue()
					: (sk.magic()
							? com.lopez.l2j.game.stats.Stat.M_REUSE.getValue()
							: com.lopez.l2j.game.stats.Stat.P_REUSE.getValue());
			double reuseRate = PlayerStats.applyStat(active(), reuseStat, 1.0);
			if (!sk.magic() && !sk.isDanceOrSong()) {
				reuseRate *= PlayerStats.applyStat(active(), com.lopez.l2j.game.stats.Stat.ATK_REUSE.getValue(), 1.0);
			}
			if (reuseRate > 0.0 && reuseRate != 1.0) {
				reuse = (int) Math.round(reuse * reuseRate);
			}
		}
		active().currentMp(Math.max(0, active().currentMp() - effMpInitial));
		if (reuse > 0) {
			skillReuse.put(sk.id(), now + Math.max(reuse, hitTime));
		}
		boolean isSleepOrTrance = "SLEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 1069 || sk.id() == 1394
				|| sk.id() == 1071 || sk.id() == 1201;
		if (isSleepOrTrance) {
			session.stopAutoAttack();
			session.autoAttacking(false);
		}
		boolean resumeAttack = !isSleepOrTrance && (session.isAutoAttacking() || sk.nextActionAttack())
				&& (npcTarget != null || playerTarget != null);
		session.autoAttacking(false); // o auto-ataque para durante o cast

		// Para a movimentacao do personagem para conjurar a habilidade ("da uma
		// paradinha")
		var stop = new StopMove(active().objectId(), active().x(), active().y(), active().z(), active().heading());
		send(stop);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stop, false);

		int mainTargetId = npcTarget != null ? npcTarget.objectId()
				: (doorTarget != null ? doorTarget.objectId()
						: (summonTarget != null ? summonTarget.objectId()
								: (playerTarget != null ? playerTarget.objectId() : active().objectId())));
		var msu = new MagicSkillUse(active().objectId(), mainTargetId, sk.id(), sk.level(), hitTime, reuse,
				active().x(), active().y(), active().z(), tx, ty, tz);
		send(msu);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
		if (sk.isOffensive() && sk.nextActionAttack()) {
			var startAtk = new AutoAttackStart(active().objectId());
			send(startAtk);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, startAtk, false);
			if (playerTarget != null && playerTarget != session) {
				var startAtkTgt = new AutoAttackStart(playerTarget.objectId());
				playerTarget.send(startAtkTgt);
				ctx().world().broadcastAround(playerTarget, GameWorld.VISIBILITY_RADIUS, startAtkTgt, false);
			}
		}
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.SkillName(sk.id(), sk.level())));
		if (hitTime > 0) {
			send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.BLUE, hitTime));
		}
		if (sk.mpInitialConsume() > 0) {
			session.sendVitals();
		}

		session.casting(true);
		session.castingSkill(sk);
		PlayerCharacter owner = active();
		NpcInstance npc = npcTarget;
		DoorInstance door = doorTarget;
		GameSession targetSess = playerTarget;
		Runnable finish = () -> {
			if (!session.casting() || active() != owner || !session.inWorld() || owner.isDead()) {
				return;
			}
			session.casting(false);
			session.castingSkill(null);
			session.castTask(null);
			try {
				finishCast(sk, npc, door, targetSess, resumeAttack, sps, bss);
			} catch (RuntimeException e) {
				log.warn("Erro ao finalizar skill {} de {}: {}", sk.id(), owner.name(), e.toString());
			}
		};
		if (hitTime <= 0) {
			finish.run();
		} else {
			session.castTask(GameSession.autoAttackScheduler().schedule(finish, hitTime, TimeUnit.MILLISECONDS));
		}
	}

	/** onMagicHitTimer: gasta MP, dispara o skill e aplica nos alvos. */
	public void finishCast(SkillTemplate sk, NpcInstance mainNpc, DoorInstance doorTarget, GameSession targetPlayer,
			boolean resumeAttack,
			boolean sps, boolean bss) {
		var party = session.party();
		int effMpConsume = getEffectiveMpConsume(sk, sk.mpConsume());
		if (active().currentMp() < effMpConsume) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active().currentMp(Math.max(0, active().currentMp() - effMpConsume));
		if (sk.hpConsume() > 0) {
			active().currentHp(active().currentHp() - sk.hpConsume());
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			if (active().inventory().destroyItemByItemId(sk.itemConsumeId(), sk.itemConsumeCount())) {
				send(ItemList.of(active().inventory().items(), false));
				send(SystemMessage.of(SystemMessage.S1_DISAPPEARED, new SystemMessage.ItemName(sk.itemConsumeId())));
			}
		}
		var t = ctx() != null && ctx().characters() != null ? ctx().characters().template(active()) : null;

		// #135: SkillMastery proc - zera o reuse imediatamente se ativar
		if (sk.reuseDelay() > 0 && !sk.isPotion()
				&& com.lopez.l2j.game.combat.Formulas.calcSkillMastery(active(), t, active().isMage())) {
			skillReuse.remove(sk.id());
			send(SystemMessage.id(SystemMessage.SKILL_READY_TO_USE_AGAIN)); // Skill Mastery activated
		}

		// #186: EnemyCharge / Warp / Rush / Blink (484, 628, 1448 ou efeito
		// EnemyCharge/Warp/Instant_Jump)
		if (sk.id() == 484 || sk.id() == 628 || sk.id() == 1448 || "INSTANT_JUMP".equalsIgnoreCase(sk.skillType())
				|| sk.effects().stream().anyMatch(e -> "enemycharge".equalsIgnoreCase(e.name())
						|| "warp".equalsIgnoreCase(e.name()) || "instant_jump".equalsIgnoreCase(e.name()))) {
			handleEnemyChargeOrWarp(sk, mainNpc, targetPlayer);
		}
		if ("GET_PLAYER".equalsIgnoreCase(sk.skillType())
				|| sk.effects().stream().anyMatch(e -> "get_player".equalsIgnoreCase(e.name()))) {
			if (targetPlayer != null && targetPlayer != session && targetPlayer.active() != null) {
				targetPlayer.teleportToLocation(active().x(), active().y(), active().z(), 0);
			}
		}
		if ("ZAKEN_MOVE".equalsIgnoreCase(sk.skillType())
				|| sk.effects().stream().anyMatch(e -> "zaken_move".equalsIgnoreCase(e.name()))) {
			session.teleportToLocation(active().x() + ThreadLocalRandom.current().nextInt(-150, 151),
					active().y() + ThreadLocalRandom.current().nextInt(-150, 151), active().z(), 0);
		}
		if ("FAKE_DEATH".equalsIgnoreCase(sk.skillType()) && !sk.isToggle()) {
			toggleSkill(sk);
			return;
		}
		if ((sk.id() == 246 || "TAKECASTLE".equalsIgnoreCase(sk.skillType())) && ctx() != null && ctx().sieges() != null
				&& active().clanId() > 0) {
			for (var siege : ctx().sieges().allSieges()) {
				if (ctx().sieges().isAttacker(siege.castleId(), active().clanId())) {
					ctx().sieges().engraveArtifact(siege.castleId(), active().clanId(), 0);
				}
			}
		}
		if ((sk.id() == 247 || sk.id() == 845 || "TAKEFORT".equalsIgnoreCase(sk.skillType())
				|| (sk.name() != null && sk.name().contains("Flag")))
				&& ctx() != null && ctx().fortressSieges() != null && active().clanId() > 0) {
			ctx().fortressSieges().raiseCombatFlag(101, active().clanId());
		}

		if (sk.id() == 1312 || "FISHING".equalsIgnoreCase(sk.skillType())) { // Fishing
			broadcastLaunched(sk, List.of(active().objectId()));
			if (ctx().fishing() != null) {
				int d = 150;
				double angle = Math.toRadians(active().heading() * (360.0 / 65536.0));
				int fx = active().x() + (int) (d * Math.cos(angle));
				int fy = active().y() + (int) (d * Math.sin(angle));
				int fz = active().z();
				ctx().fishing().startFishing(active(), fx, fy, fz, session::send);
			}
			return;
		}
		if (sk.id() == 1313 || "PUMPING".equalsIgnoreCase(sk.skillType())) { // Pumping
			broadcastLaunched(sk, List.of(active().objectId()));
			if (ctx().fishing() != null) {
				ctx().fishing().handlePumping(active(), sk.level(), (int) sk.power(), session::send);
			}
			return;
		}
		if (sk.id() == 1314 || "REELING".equalsIgnoreCase(sk.skillType())) { // Reeling
			broadcastLaunched(sk, List.of(active().objectId()));
			if (ctx().fishing() != null) {
				ctx().fishing().handleReeling(active(), sk.level(), (int) sk.power(), session::send);
			}
			return;
		}

		// #149: DWARVEN_CRAFT (172, 1321) e COMMON_CRAFT (1322) -> RecipeBookItemList
		if ("DWARVEN_CRAFT".equalsIgnoreCase(sk.skillType()) || "COMMON_CRAFT".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 172 || sk.id() == 1321 || sk.id() == 1322) {
			if (active().privateStoreType() == PlayerCharacter.STORE_MANUFACTURE) {
				send(SystemMessage.id(SystemMessage.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING));
				send(new ActionFailed());
				return;
			}
			boolean isDwarven = !"COMMON_CRAFT".equalsIgnoreCase(sk.skillType()) && sk.id() != 1322;
			broadcastLaunched(sk, List.of(active().objectId()));
			var recipeService = (ctx() != null && ctx().recipes() != null)
					? ctx().recipes()
					: com.lopez.l2j.game.recipe.RecipeService.getInstance();
			List<Integer> recipes = recipeService != null
					? recipeService.getRecipeBook(active().objectId(), isDwarven)
					: List.of();
			send(new GameServerPacket.RecipeBookItemList(isDwarven, active().maxMp(), recipes));
			session.sendVitals();
			return;
		}

		// #150: SOW (2097) e HARVEST (2098)
		if ("SOW".equalsIgnoreCase(sk.skillType()) || sk.id() == 2097) {
			handleSow(mainNpc, sk);
			session.sendVitals();
			return;
		}
		if ("HARVEST".equalsIgnoreCase(sk.skillType()) || sk.id() == 2098) {
			handleHarvest(mainNpc, sk);
			session.sendVitals();
			return;
		}

		// #151: SIGNET e SIGNET_CASTTIME (454-460, 1419-1428)
		if ("SIGNET".equalsIgnoreCase(sk.skillType()) || "SIGNET_CASTTIME".equalsIgnoreCase(sk.skillType())
				|| (sk.id() >= 454 && sk.id() <= 460) || (sk.id() >= 1419 && sk.id() <= 1428)) {
			handleSignet(sk, t, sps, bss);
			session.sendVitals();
			return;
		}

		// #152: FUSION e BUFF_CHARGER (Battle Force 426, Spell Force 427)
		if ("FUSION".equalsIgnoreCase(sk.skillType()) || "BUFF_CHARGER".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 426 || sk.id() == 427) {
			handleFusionSkill(sk, targetPlayer);
			session.sendVitals();
			return;
		}

		// #153: STRSIEGEASSAULT (Strider Siege Assault 325)
		if ("STRSIEGEASSAULT".equalsIgnoreCase(sk.skillType()) || sk.id() == 325) {
			handleStriderSiegeAssault(sk, doorTarget, mainNpc);
			session.sendVitals();
			return;
		}

		// #154 & #155: ERASE (1395) e BETRAY (1380)
		if ("ERASE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1395) {
			handleEraseSkill(sk, targetPlayer, sps, bss);
			session.sendVitals();
			return;
		}
		if ("BETRAY".equalsIgnoreCase(sk.skillType()) || sk.id() == 1380) {
			handleBetraySkill(sk, targetPlayer, sps, bss);
			session.sendVitals();
			return;
		}

		// #156 & #157: CONFUSION, CONFUSE_MOB_ONLY, SHIFT_TARGET
		if ("CONFUSION".equalsIgnoreCase(sk.skillType()) || "CONFUSE_MOB_ONLY".equalsIgnoreCase(sk.skillType())
				|| sk.id() == 11 || sk.id() == 12) {
			handleConfusionSkill(sk, mainNpc, targetPlayer, sps, bss);
			session.sendVitals();
			return;
		}
		if ("SHIFT_TARGET".equalsIgnoreCase(sk.skillType())) {
			handleShiftTargetSkill(sk, targetPlayer);
			session.sendVitals();
			return;
		}

		// #158: DRAIN_SOUL (Absorb Soul 2099 / 2096)
		if ("DRAIN_SOUL".equalsIgnoreCase(sk.skillType()) || sk.id() == 2099 || sk.id() == 2096) {
			handleDrainSoul(sk, mainNpc);
			session.sendVitals();
			return;
		}

		// #159: BEAST_FEED (Golden Spice 2188, Crystal Spice 2189)
		if ("BEAST_FEED".equalsIgnoreCase(sk.skillType()) || sk.id() == 2188 || sk.id() == 2189) {
			handleBeastFeed(sk, mainNpc);
			session.sendVitals();
			return;
		}

		// #161: EXTRACTABLE e EXTRACTABLE_FISH
		if ("EXTRACTABLE".equalsIgnoreCase(sk.skillType()) || "EXTRACTABLE_FISH".equalsIgnoreCase(sk.skillType())) {
			handleExtractableSkill(sk);
			session.sendVitals();
			return;
		}

		boolean isAreaSweep = "TARGET_AREA_CORPSE_MOB".equalsIgnoreCase(sk.target())
				|| sk.id() == 444
				|| ("SWEEP".equalsIgnoreCase(sk.skillType()) && sk.isAreaAroundSelf());
		if (isAreaSweep || "SWEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 42) {
			if (ctx().drops() != null) {
				List<NpcInstance> corpsesToSweep = new ArrayList<>();
				if (isAreaSweep) {
					int sweepRadius = Math.max(250, sk.skillRadius() > 0 ? sk.skillRadius() : 300);
					int cx = mainNpc != null ? mainNpc.x() : active().x();
					int cy = mainNpc != null ? mainNpc.y() : active().y();
					if (mainNpc != null && mainNpc.isDead() && (mainNpc.isSweepable() || mainNpc.isSpoiled())
							&& canPlayerSweepCorpse(mainNpc)) {
						corpsesToSweep.add(mainNpc);
					}
					if (ctx().world() != null) {
						for (NpcInstance corpse : ctx().world().findNpcsAround(cx, cy, sweepRadius,
								active().instanceId())) {
							if (corpse != null && corpse.isAttackable() && corpse.isDead()
									&& (corpse.isSweepable() || corpse.isSpoiled())
									&& canPlayerSweepCorpse(corpse) && !corpsesToSweep.contains(corpse)) {
								corpsesToSweep.add(corpse);
							}
						}
					}
				} else if (mainNpc != null) {
					corpsesToSweep.add(mainNpc);
				}

				if (!corpsesToSweep.isEmpty()) {
					broadcastLaunched(sk, corpsesToSweep.stream().map(NpcInstance::objectId).toList());
				}
				for (NpcInstance corpse : corpsesToSweep) {
					java.util.function.Consumer<GameServerPacket> partySender = party != null
							? pkt -> party.broadcastExcept(pkt, active().objectId())
							: null;
					boolean wasSpoiled = corpse.isSpoiled();
					boolean swept = ctx().drops().sweep(active(), corpse, ctx().inventories(), session::send,
							partySender);
					if (swept) {
						if (sk.absorbPart() > 0) {
							healHp(sk.absorbPart());
						}
					}
					if (swept || wasSpoiled) {
						corpse.spoiled(false);
						corpse.spoilRewards(null);
						var dieNoGlow = new Die(corpse.objectId(), false, false, false, false);
						send(dieNoGlow);
						if (ctx().world() != null) {
							ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, dieNoGlow, false);
						}
						if (ctx().npcAi() != null) {
							ctx().npcAi().scheduleQuickDecay(corpse, 3000);
						}
					}
				}
			}
			session.sendVitals();
			return;
		}

		boolean isUnlock = "UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "TARGET_UNLOCKABLE".equalsIgnoreCase(sk.target());
		if (isUnlock) {
			if (doorTarget != null) {
				broadcastLaunched(sk, List.of(doorTarget.objectId()));
				handleUnlockDoor(doorTarget, sk);
			} else if (mainNpc != null) {
				broadcastLaunched(sk, List.of(mainNpc.objectId()));
				handleUnlockChest(mainNpc, sk);
			}
			session.sendVitals();
			return;
		}

		com.lopez.l2j.game.skill.handler.SkillHandlerRegistry skillRegistry = ctx() != null
				? ctx().getBean(com.lopez.l2j.game.skill.handler.SkillHandlerRegistry.class)
				: null;
		if (skillRegistry == null) {
			skillRegistry = com.lopez.l2j.game.skill.handler.SkillHandlerRegistry.getInstance();
		}
		if (skillRegistry != null && skillRegistry.dispatchSpecialSkill(session, sk, mainNpc, doorTarget, targetPlayer,
				false, sps, bss)) {
			int launchTargetId = mainNpc != null ? mainNpc.objectId()
					: (doorTarget != null ? doorTarget.objectId()
							: (targetPlayer != null ? targetPlayer.objectId() : active().objectId()));
			broadcastLaunched(sk, List.of(launchTargetId));
			send(ItemList.of(active().inventory().items(), false));
			session.sendVitals();
			return;
		}

		if (sk.isOffensive()) {
			boolean ss = false;
			if (sk.isPhysicalDamage() && sk.power() > 0) {
				if (!session.soulshotCharged()) {
					session.rechargeAutoSoulShots();
				}
				ss = session.soulshotCharged();
				session.soulshotCharged(false);
				session.rechargeAutoSoulShots();
			}

			List<NpcInstance> npcTargets = new ArrayList<>();
			List<GameSession> playerTargets = new ArrayList<>();
			int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 200;
			int myInst = active().instanceId();

			if (sk.isAreaAroundSelf()) {
				npcTargets.addAll(ctx().world().findNpcsAround(active().x(), active().y(), radius, myInst));
				for (var p : ctx().world().findPlayersAround(active().x(), active().y(), radius, myInst)) {
					if (p instanceof GameSession gs && session.canAttackInPvP(gs)) {
						playerTargets.add(gs);
					}
				}
			} else if (sk.isAreaAroundTarget()) {
				int cx = mainNpc != null ? mainNpc.x() : (targetPlayer != null ? targetPlayer.x() : active().x());
				int cy = mainNpc != null ? mainNpc.y() : (targetPlayer != null ? targetPlayer.y() : active().y());
				if (mainNpc != null && !mainNpc.isDead()) {
					npcTargets.add(mainNpc);
				}
				if (targetPlayer != null && session.canAttackInPvP(targetPlayer)) {
					playerTargets.add(targetPlayer);
				}
				for (NpcInstance n : ctx().world().findNpcsAround(cx, cy, radius, myInst)) {
					if (!npcTargets.contains(n)) {
						npcTargets.add(n);
					}
				}
				for (var p : ctx().world().findPlayersAround(cx, cy, radius, myInst)) {
					if (p instanceof GameSession gs && session.canAttackInPvP(gs) && !playerTargets.contains(gs)) {
						playerTargets.add(gs);
					}
				}
			} else {
				if (targetPlayer != null && session.canAttackInPvP(targetPlayer)) {
					playerTargets.add(targetPlayer);
				} else if (mainNpc != null && !mainNpc.isDead()) {
					npcTargets.add(mainNpc);
				}
			}

			npcTargets.removeIf(n -> !n.isAttackable() || n.isDead()
					|| (ctx().combat() != null && !ctx().combat().canSeeTarget(active(), n)));
			playerTargets.removeIf(p -> p.active() == null || p.active().isDead()
					|| (ctx().combat() != null && !ctx().combat().canSeeTarget(active(), p.active())));

			List<Integer> launchedIds = new ArrayList<>();
			for (NpcInstance n : npcTargets) {
				launchedIds.add(n.objectId());
			}
			for (GameSession gs : playerTargets) {
				launchedIds.add(gs.objectId());
			}
			broadcastLaunched(sk, launchedIds);

			for (NpcInstance n : npcTargets) {
				applyOffensive(sk, n, t, ss, sps, bss);
			}
			for (GameSession gs : playerTargets) {
				updatePvPFlag();
				gs.updatePvPFlag();
				applyOffensivePlayer(sk, gs, t, ss, sps, bss);
			}
			if (active() != null) {
				if (sk.needCharges() > 0 && sk.consumeCharges()) {
					session.decreaseCharges(sk.needCharges());
				}
				if (sk.giveCharges() > 0) {
					session.increaseCharges(sk.giveCharges(), sk.maxCharges());
				}
				session.triggerWeaponOnCastSkill(mainNpc, targetPlayer);
			}
			session.rechargeAutoSoulShots();
			session.sendVitals();
			if (resumeAttack) {
				if (mainNpc != null && !mainNpc.isDead() && session.targetObjectId() == mainNpc.objectId()) {
					session.autoAttacking(true);
					session.schedulePlayerAutoAttack(mainNpc);
				} else if (targetPlayer != null && targetPlayer.active() != null && !targetPlayer.active().isDead()
						&& session.targetObjectId() == targetPlayer.objectId()) {
					session.autoAttacking(true);
					session.schedulePlayerAutoAttack(targetPlayer);
				}
			}
			return;
		}

		// Cura ou skill positivo usado em Summon/Pet
		com.lopez.l2j.game.summon.SummonInstance targetSummon = null;
		if ("TARGET_SUMMON".equalsIgnoreCase(sk.target()) || "TARGET_PET".equalsIgnoreCase(sk.target())) {
			if (active().activeSummon() != null && !active().activeSummon().isDead()) {
				targetSummon = active().activeSummon();
			}
		} else if (!sk.isSelfTargeted() && session.targetObjectId() != 0
				&& session.targetObjectId() != active().objectId()) {
			if (active().activeSummon() != null && !active().activeSummon().isDead()
					&& active().activeSummon().objectId() == session.targetObjectId()) {
				targetSummon = active().activeSummon();
			} else if (ctx().world() != null) {
				targetSummon = ctx().world().summon(session.targetObjectId()).filter(s -> !s.isDead()).orElse(null);
			}
		}
		if (targetSummon != null) {
			broadcastLaunched(sk, List.of(targetSummon.objectId()));
			handlePositiveSkillOnSummon(sk, targetSummon, t, sps, bss);
			if (active() != null) {
				if (sk.needCharges() > 0 && sk.consumeCharges()) {
					session.decreaseCharges(sk.needCharges());
				}
				session.triggerWeaponOnCastSkill(null, null);
			}
			session.rechargeAutoSoulShots();
			session.sendVitals();
			return;
		}

		// Cura usada em monstro (dano sagrado em Undead ou cura de HP + aggro em
		// monstros normais)
		boolean isHealOnMob = "HEAL".equalsIgnoreCase(sk.skillType())
				|| "HEAL_PERCENT".equalsIgnoreCase(sk.skillType())
				|| "HEAL_STATIC".equalsIgnoreCase(sk.skillType());
		if (isHealOnMob && mainNpc != null && !mainNpc.isDead() && mainNpc.isAttackable()) {
			broadcastLaunched(sk, List.of(mainNpc.objectId()));
			if (mainNpc.template() != null && mainNpc.template().isUndead() && ctx().combat() != null) {
				var hit = ctx().combat().skillMagicNpc(active(), t, mainNpc, sk.power(), sk.magicLevel(), sps, bss);
				handleNpcHit(mainNpc, hit, t, sk);
			} else {
				double healPower = sk.power();
				if ("HEAL_PERCENT".equalsIgnoreCase(sk.skillType())) {
					healPower = mainNpc.maxHp() * sk.power() / 100.0;
				} else if ("HEAL".equalsIgnoreCase(sk.skillType())) {
					int mAtk = t != null ? PlayerStats.calculate(active(), t).mAtk() : 0;
					double ssMul = bss ? 4.0 : (sps ? 2.0 : 1.0);
					double mAtkBonus = mAtk > 0 ? Math.sqrt(Math.max(1.0, mAtk * ssMul)) : 0.0;
					healPower += mAtkBonus;
				}
				mainNpc.currentHp(Math.min(mainNpc.maxHp(), mainNpc.currentHp() + healPower));
				var su = StatusUpdate.hp(mainNpc.objectId(), (int) mainNpc.currentHp(), (int) mainNpc.maxHp());
				send(su);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
				if (ctx().npcAi() != null) {
					ctx().npcAi().startCombat(mainNpc, active().objectId());
				}
				if (mainNpc.isInCombat() && mainNpc.targetPlayerId() != 0
						&& mainNpc.targetPlayerId() != active().objectId()) {
					var combPlayer = ctx().world().player(mainNpc.targetPlayerId()).orElse(null);
					if (combPlayer != null && combPlayer.character() != null
							&& (combPlayer.character().pvpFlag() > 0 || combPlayer.character().karma() > 0)) {
						updatePvPFlag(combPlayer.character().pvpFlag() > 0);
					}
				}
			}
			if (active() != null) {
				if (sk.needCharges() > 0 && sk.consumeCharges()) {
					session.decreaseCharges(sk.needCharges());
				}
				session.triggerWeaponOnCastSkill(mainNpc, null);
			}
			session.rechargeAutoSoulShots();
			session.sendVitals();
			return;
		}

		// Skill positivo (cura, buff, resurrect, song, dance)
		List<GameSession> targets = new ArrayList<>();
		boolean isPartySkill = "TARGET_PARTY".equalsIgnoreCase(sk.target())
				|| "TARGET_PARTY_MEMBER".equalsIgnoreCase(sk.target())
				|| (!sk.isOffensive() && ("TARGET_AURA".equalsIgnoreCase(sk.target())
						|| "TARGET_CLAN".equalsIgnoreCase(sk.target())
						|| "TARGET_ALLY".equalsIgnoreCase(sk.target())));

		if (isPartySkill && party != null) {
			int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 1000;
			double rSq = (double) radius * radius;
			boolean resurrect = "RESURRECT".equalsIgnoreCase(sk.skillType())
					|| sk.target().startsWith("TARGET_CORPSE_");
			for (GameSession member : party.members()) {
				if (member != null && member.active() != null) {
					if (resurrect ? member.active().isDead() : !member.active().isDead()) {
						double dx = active().x() - member.active().x();
						double dy = active().y() - member.active().y();
						if (dx * dx + dy * dy <= rSq) {
							targets.add(member);
						}
					}
				}
			}
			if (targets.isEmpty()) {
				targets.add(session);
			}
		} else if (targetPlayer != null && targetPlayer.active() != null) {
			targets.add(targetPlayer);
		} else {
			targets.add(session);
		}

		broadcastLaunched(sk, targets.stream().map(s -> s.active().objectId()).toList());
		if ("BALANCE_LIFE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1335) {
			handleBalanceLife(sk, targets);
		} else {
			boolean canShareBuffToSummon = sk.isBuff() || sk.isDanceOrSong()
					|| (!sk.isOffensive() && ("TARGET_PARTY".equalsIgnoreCase(sk.target())
							|| "TARGET_SELF".equalsIgnoreCase(sk.target())
							|| "TARGET_AURA".equalsIgnoreCase(sk.target())
							|| "TARGET_CLAN".equalsIgnoreCase(sk.target())
							|| "TARGET_ALLY".equalsIgnoreCase(sk.target())));
			for (GameSession target : targets) {
				if (target != session && target.active() != null) {
					if (target.active().pvpFlag() > 0 || target.active().karma() > 0) {
						updatePvPFlag(target.active().pvpFlag() > 0);
					}
				}
				target.magicSkillHandler().receivePositiveSkill(sk, active().name(), session, sps, bss);
				if (canShareBuffToSummon && target.active() != null) {
					var memberSummon = target.active().activeSummon();
					if (memberSummon != null && !memberSummon.isDead() && memberSummon != targetSummon) {
						double sdx = target.active().x() - memberSummon.x();
						double sdy = target.active().y() - memberSummon.y();
						if (sdx * sdx + sdy * sdy <= 1000.0 * 1000.0) {
							handlePositiveSkillOnSummon(sk, memberSummon, t, sps, bss);
						}
					}
				}
			}
		}
		if (active() != null) {
			if (sk.needCharges() > 0 && sk.consumeCharges()) {
				session.decreaseCharges(sk.needCharges());
			}
			session.triggerWeaponOnCastSkill(null, targetPlayer);
		}
		session.rechargeAutoSoulShots();
		session.sendVitals();
	}

	public void broadcastLaunched(SkillTemplate sk, List<Integer> targets) {
		var launched = new GameServerPacket.MagicSkillLaunched(active().objectId(), sk.id(), sk.level(), targets);
		send(launched);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, launched, false);
	}

	// =========================================================================
	// #150..#161, #186 - Handlers dedicados de SkillTypes especiais
	// =========================================================================

	private static final Map<Integer, ItemPacketHandler.SeededMonsterState> SKILL_SEEDED_MONSTERS = new ConcurrentHashMap<>();

	private void handleEnemyChargeOrWarp(SkillTemplate sk, NpcInstance mainNpc, GameSession targetPlayer) {
		if (active() == null) {
			return;
		}
		int origX = active().x();
		int origY = active().y();
		int origZ = active().z();
		int destX = origX;
		int destY = origY;
		int destZ = origZ;
		int flyType = 2; // FLY_CHARGE

		if (mainNpc != null) {
			double dx = mainNpc.x() - origX;
			double dy = mainNpc.y() - origY;
			double dist = Math.hypot(dx, dy);
			if (dist > 40.0) {
				double ratio = (dist - 35.0) / dist;
				destX = origX + (int) Math.round(dx * ratio);
				destY = origY + (int) Math.round(dy * ratio);
				destZ = mainNpc.z();
			}
		} else if (targetPlayer != null && targetPlayer != session && targetPlayer.active() != null) {
			double dx = targetPlayer.x() - origX;
			double dy = targetPlayer.y() - origY;
			double dist = Math.hypot(dx, dy);
			if (dist > 40.0) {
				double ratio = (dist - 35.0) / dist;
				destX = origX + (int) Math.round(dx * ratio);
				destY = origY + (int) Math.round(dy * ratio);
				destZ = targetPlayer.z();
			}
		} else {
			// Warp (628) / Blink (1448) direcional
			flyType = 4; // FLY_WARP_FORWARD
			double angle = active().heading() * Math.PI / 32768.0;
			int range = sk.castRange() > 0 ? sk.castRange() : 400;
			if (sk.id() == 1448) {
				angle += Math.PI;
				flyType = 3; // FLY_WARP_BACK
			}
			destX = origX + (int) Math.round(Math.cos(angle) * range);
			destY = origY + (int) Math.round(Math.sin(angle) * range);
		}
		try {
			int[] checked = com.lopez.l2j.game.geodata.GeoEngine.getInstance()
					.moveCheck(origX, origY, origZ, destX, destY, destZ).toArray();
			destX = checked[0];
			destY = checked[1];
			destZ = checked[2];
		} catch (Exception ignored) {
		}
		active().moveTo(destX, destY, destZ);
		var fly = new GameServerPacket.FlyToLocation(active().objectId(), destX, destY, destZ, origX, origY, origZ,
				flyType);
		send(fly);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, fly, false);
		var valLoc = new ValidateLocation(active().objectId(), destX, destY, destZ, active().heading());
		send(valLoc);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, valLoc, false);
	}

	public void handleSow(NpcInstance mainNpc, SkillTemplate sk) {
		if (mainNpc == null || mainNpc.isDead() || !mainNpc.isAttackable()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		var existingSeed = ItemPacketHandler.getSeededMonsterState(mainNpc.objectId());
		if (existingSeed == null) {
			existingSeed = SKILL_SEEDED_MONSTERS.get(mainNpc.objectId());
		}
		if (existingSeed != null) {
			send(SystemMessage.id(890)); // THE_SEED_WAS_NOT_SOWN
			return;
		}
		broadcastLaunched(sk, List.of(mainNpc.objectId()));
		int mobLvl = mainNpc.template() != null ? mainNpc.template().level() : active().level();
		int seedId = sk.itemConsumeId() > 0 ? sk.itemConsumeId() : 5016;
		int seedLvl = mobLvl;
		int cropId = seedId + 52;
		var manor = (ctx() != null && ctx().manor() != null)
				? ctx().manor()
				: com.lopez.l2j.game.manor.CastleManorManager.getInstance();
		if (seedId > 0 && manor != null) {
			var seedTpl = manor.getSeed(seedId).orElse(null);
			if (seedTpl != null) {
				if (seedTpl.level() > 0) {
					seedLvl = seedTpl.level();
				}
				if (seedTpl.cropId() > 0) {
					cropId = seedTpl.cropId();
				}
			}
		}
		int diff = Math.abs(mobLvl - seedLvl);
		int chance = 90 - Math.max(0, diff - 5) * 5;
		chance = Math.clamp(chance, 5, 99);
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			SKILL_SEEDED_MONSTERS.put(mainNpc.objectId(),
					new ItemPacketHandler.SeededMonsterState(seedId, cropId, active().objectId(), false));
			send(SystemMessage.id(889)); // THE_SEED_WAS_SUCCESSFULLY_SOWN
		} else {
			send(SystemMessage.id(890)); // THE_SEED_WAS_NOT_SOWN
		}
		if (ctx().npcAi() != null) {
			ctx().npcAi().startCombat(mainNpc, active().objectId());
		}
	}

	public void handleHarvest(NpcInstance mainNpc, SkillTemplate sk) {
		if (mainNpc == null || !mainNpc.isDead() || !mainNpc.isAttackable()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		var seeded = ItemPacketHandler.getSeededMonsterState(mainNpc.objectId());
		boolean fromSkillMap = false;
		if (seeded == null) {
			seeded = SKILL_SEEDED_MONSTERS.get(mainNpc.objectId());
			fromSkillMap = true;
		}
		if (seeded == null || seeded.harvested()) {
			send(SystemMessage.id(892)); // THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN
			return;
		}
		int seederId = seeded.seederPlayerId();
		boolean allowed = seederId == 0 || seederId == active().objectId()
				|| (session.party() != null && session.party().contains(seederId));
		if (!allowed) {
			send(SystemMessage.id(891)); // YOU_ARE_NOT_AUTHORIZED_TO_HARVEST
			return;
		}
		broadcastLaunched(sk, List.of(mainNpc.objectId()));
		if (fromSkillMap) {
			SKILL_SEEDED_MONSTERS.put(mainNpc.objectId(),
					new ItemPacketHandler.SeededMonsterState(seeded.seedItemId(), seeded.cropItemId(), seederId, true));
		}
		int mobLvl = mainNpc.template() != null ? mainNpc.template().level() : active().level();
		int diff = Math.abs(mobLvl - active().level());
		int chance = Math.clamp(90 - Math.max(0, diff - 5) * 5, 10, 99);
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			int cropId = seeded.cropItemId() > 0 ? seeded.cropItemId() : 5088;
			int count = com.lopez.l2j.game.manor.CastleManorManager
					.calculateManorCropDropCount(1 + ThreadLocalRandom.current().nextInt(3));
			if (ctx().inventories() != null) {
				ctx().inventories().addItem(active().inventory(), cropId, count, "ManorHarvest");
			} else {
				active().inventory().addItem(cropId, count);
			}
			send(ItemList.of(active().inventory().items(), false));
			send(SystemMessage.of(SystemMessage.EARNED_S2_S1_S,
					new SystemMessage.ItemName(cropId), new SystemMessage.ItemNumber(count)));
		} else {
			send(SystemMessage.id(893)); // THE_HARVEST_HAS_FAILED
		}
	}

	public void handleSignet(SkillTemplate sk, CharTemplate t, boolean sps, boolean bss) {
		if (active() == null || ctx() == null || ctx().world() == null) {
			return;
		}
		broadcastLaunched(sk, List.of(active().objectId()));
		int cx = active().x();
		int cy = active().y();
		int instId = active().instanceId();
		int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 180;
		int pulses = com.lopez.l2j.game.combat.Formulas.calcSignetPulseCount(16, 2);
		boolean offensiveSignet = sk.id() >= 1419 && sk.id() <= 1424 || sk.id() == 455;
		PlayerCharacter owner = active();
		int[] rem = { pulses };
		AtomicReference<ScheduledFuture<?>> ref = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (active() != owner || !session.inWorld() || owner.isDead() || rem[0]-- <= 0) {
				var f = ref.get();
				if (f != null) {
					f.cancel(false);
				}
				return;
			}
			if (offensiveSignet) {
				for (NpcInstance n : ctx().world().findNpcsAround(cx, cy, radius, instId)) {
					if (n != null && n.isAttackable() && !n.isDead()) {
						applyOffensive(sk, n, t, false, sps, bss);
					}
				}
				for (var p : ctx().world().findPlayersAround(cx, cy, radius, instId)) {
					if (p instanceof GameSession gs && session.canAttackInPvP(gs) && gs.active() != null
							&& !gs.active().isDead()) {
						if (sk.id() == 1424) { // Anti-Summoning Field
							var sum = gs.active().activeSummon();
							if (sum != null && !sum.isPet()) {
								sum.unsummon();
							}
						} else if (sk.id() == 455) { // Symbol of Noise - cancela Dances/Songs
							gs.active().effects().active().stream()
									.filter(b -> PlayerEffects.isDanceOrSong(b.skillId()))
									.forEach(b -> gs.active().effects().remove(b));
							gs.refreshBuffs();
						} else {
							applyOffensivePlayer(sk, gs, t, false, sps, bss);
						}
					}
				}
			} else {
				List<GameSession> allies = new ArrayList<>();
				allies.add(session);
				if (session.party() != null) {
					for (GameSession m : session.party().members()) {
						if (m != null && m != session && m.active() != null && !m.active().isDead()
								&& Math.hypot(m.active().x() - cx, m.active().y() - cy) <= radius) {
							allies.add(m);
						}
					}
				}
				for (GameSession ally : allies) {
					if (sk.id() == 1425) { // Purification Field
						ally.magicSkillHandler().handleNegateDebuffs(sk);
					} else if (sk.id() == 1426) { // Miracle
						ally.magicSkillHandler().healHp(ally.active().maxHp() * 0.50, false);
						ally.sendVitals();
					} else if (sk.id() == 1427) { // Flames of Invincibility
						ally.applySkillEffects(sk, false, 15_000L);
					} else if (sk.id() == 1428) { // Mass Recharge
						ally.active().currentMp(Math.min(ally.active().maxMp(), ally.active().currentMp() + 400));
						ally.sendVitals();
					} else {
						ally.applySkillEffects(sk, false, 5_000L);
					}
				}
			}
		}, 1000L, 2000L, TimeUnit.MILLISECONDS);
		ref.set(task);
	}

	public void handleFusionSkill(SkillTemplate sk, GameSession targetPlayer) {
		GameSession tgt = targetPlayer != null && targetPlayer.active() != null ? targetPlayer : session;
		broadcastLaunched(sk, List.of(tgt.active().objectId()));
		int forceSkillId = sk.id() == 426 ? 5104 : 5105; // Battle Force (5104) / Spell Force (5105)
		String stackType = sk.id() == 426 ? "battle_force" : "spell_force";
		int curStage = tgt.active().effects().getSkillLevel(forceSkillId);
		int nextStage = com.lopez.l2j.game.combat.Formulas.calcFusionNextStage(curStage, 3);
		String statName = sk.id() == 426 ? "pAtk" : "mAtk";
		double bonusMul = 1.0 + (0.05 * nextStage);
		long end = System.currentTimeMillis() + 15_000L;
		var forceBuff = ActiveBuff.ofSkill(
				forceSkillId,
				nextStage,
				stackType,
				nextStage,
				end,
				List.of(new StatFunc(statName, StatFunc.Op.MUL, 0x30, bonusMul)),
				false,
				0,
				"EffectForce",
				0.0,
				15);
		tgt.active().effects().put(forceBuff, PlayerEffects.getExtraBuffSlots(tgt.active()));
		tgt.refreshBuffs();
		tgt.saveBuffs();
	}

	public void handleStriderSiegeAssault(SkillTemplate sk, DoorInstance doorTarget, NpcInstance mainNpc) {
		if (!active().isMounted()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			return;
		}
		if (doorTarget != null) {
			broadcastLaunched(sk, List.of(doorTarget.objectId()));
			var t = ctx().characters() != null ? ctx().characters().template(active()) : null;
			int pAtk = t != null ? PlayerStats.calculate(active(), t).pAtk() : 500;
			int dmg = Math.max(500, (int) Math.round((pAtk + sk.power()) * 0.5));
			doorTarget.currentHp(Math.max(0, doorTarget.currentHp() - dmg));
			if (doorTarget.currentHp() <= 0) {
				doorTarget.openDoor();
			}
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(dmg)));
			var status = new GameServerPacket.DoorStatusUpdate(doorTarget);
			send(status);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, status, false);
		} else if (mainNpc != null && !mainNpc.isDead()) {
			broadcastLaunched(sk, List.of(mainNpc.objectId()));
			var t = ctx().characters() != null ? ctx().characters().template(active()) : null;
			applyOffensive(sk, mainNpc, t, false, false, false);
		}
	}

	public void handleEraseSkill(SkillTemplate sk, GameSession targetPlayer, boolean sps, boolean bss) {
		com.lopez.l2j.game.summon.SummonInstance targetSummon = null;
		if (targetPlayer != null && targetPlayer.active() != null) {
			targetSummon = targetPlayer.active().activeSummon();
		}
		if (targetSummon == null && ctx().world() != null) {
			targetSummon = ctx().world().summon(session.targetObjectId()).orElse(null);
		}
		if (targetSummon == null || targetSummon.isDead() || targetSummon.isPet() || targetSummon.isSiegeSummon()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		broadcastLaunched(sk, List.of(targetSummon.objectId()));
		int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
		if (com.lopez.l2j.game.combat.Formulas.calcSkillSuccess(
				active(), targetSummon, 60.0, magicLvl, 1, "MEN", "derangement", sps, bss, false)) {
			targetSummon.unsummon();
			if (targetPlayer != null && targetPlayer.active() != null
					&& targetPlayer.active().activeSummon() == targetSummon) {
				targetPlayer.active().activeSummon(null);
			}
		} else {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.Text(targetSummon.name()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}
	}

	public void handleBetraySkill(SkillTemplate sk, GameSession targetPlayer, boolean sps, boolean bss) {
		com.lopez.l2j.game.summon.SummonInstance targetSummon = null;
		if (targetPlayer != null && targetPlayer.active() != null) {
			targetSummon = targetPlayer.active().activeSummon();
		}
		if (targetSummon == null && ctx().world() != null) {
			targetSummon = ctx().world().summon(session.targetObjectId()).orElse(null);
		}
		if (targetSummon == null || targetSummon.isDead() || !targetSummon.isServitor()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		broadcastLaunched(sk, List.of(targetSummon.objectId()));
		int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
		if (com.lopez.l2j.game.combat.Formulas.calcSkillSuccess(
				active(), targetSummon, 60.0, magicLvl, 1, "MEN", "betray", sps, bss, false)) {
			targetSummon.betray(System.currentTimeMillis() + 30_000L);
			targetSummon.targetObjectId(targetSummon.ownerId());
		} else {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.Text(targetSummon.name()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}
	}

	public void handleConfusionSkill(SkillTemplate sk, NpcInstance mainNpc, GameSession targetPlayer, boolean sps,
			boolean bss) {
		if (mainNpc != null && !mainNpc.isDead()) {
			broadcastLaunched(sk, List.of(mainNpc.objectId()));
			var unsel = new GameServerPacket.TargetUnselected(mainNpc.objectId(), mainNpc.x(), mainNpc.y(),
					mainNpc.z());
			send(unsel);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, unsel, false);
			if (sk.id() == 11 || "AGGREMOVE".equalsIgnoreCase(sk.skillType())) {
				mainNpc.stopHating(active().objectId());
				mainNpc.targetPlayerId(0);
				mainNpc.abortAttack();
				mainNpc.abortCast();
			} else {
				// Switch (12) / Confusion: randomiza o alvo do monstro entre criaturas na lista
				// de aggro ou ao redor
				List<Integer> candidates = new ArrayList<>(mainNpc.aggroList().keySet());
				candidates.remove(Integer.valueOf(active().objectId()));
				if (!candidates.isEmpty()) {
					int nextTarget = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
					mainNpc.targetPlayerId(nextTarget);
					mainNpc.addDamageHate(nextTarget, 0, Math.max(500, mainNpc.getHate(active().objectId()) + 200));
				} else {
					mainNpc.reduceHate(active().objectId(), Math.max(100, mainNpc.getHate(active().objectId()) / 2));
				}
			}
		} else if (targetPlayer != null && targetPlayer != session && targetPlayer.active() != null) {
			broadcastLaunched(sk, List.of(targetPlayer.objectId()));
			var unsel = new GameServerPacket.TargetUnselected(
					targetPlayer.objectId(), targetPlayer.x(), targetPlayer.y(), targetPlayer.z());
			targetPlayer.send(unsel);
			ctx().world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS, unsel, false);
			targetPlayer.clearTarget();
			targetPlayer.stopAutoAttack();
			targetPlayer.cancelCast();
		}
	}

	public void handleShiftTargetSkill(SkillTemplate sk, GameSession targetPlayer) {
		if (targetPlayer == null || targetPlayer.active() == null || ctx() == null || ctx().world() == null) {
			return;
		}
		int newTargetId = targetPlayer.active().objectId();
		broadcastLaunched(sk, List.of(newTargetId));
		int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 600;
		for (NpcInstance npc : ctx().world().findNpcsAround(active().x(), active().y(), radius,
				active().instanceId())) {
			if (npc != null && npc.isAttackable() && !npc.isDead()) {
				long myHate = Math.max(200L, npc.getHate(active().objectId()));
				npc.addDamageHate(newTargetId, 0, myHate);
				npc.stopHating(active().objectId());
				npc.targetPlayerId(newTargetId);
			}
		}
	}

	public void handleDrainSoul(SkillTemplate sk, NpcInstance mainNpc) {
		if (mainNpc == null || mainNpc.isDead() || !mainNpc.isAttackable()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		broadcastLaunched(sk, List.of(mainNpc.objectId()));
		if (mainNpc.currentHp() <= mainNpc.maxHp() * 0.50 && active() != null) {
			try {
				var f = ItemPacketHandler.class.getDeclaredField("SOUL_CRYSTAL_ABSORBERS");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<Integer, Map<Integer, Integer>> map = (Map<Integer, Map<Integer, Integer>>) f.get(null);
				int crystalId = sk.itemConsumeId() > 0 ? sk.itemConsumeId() : 4629;
				map.computeIfAbsent(mainNpc.objectId(), k -> new ConcurrentHashMap<>())
						.put(active().objectId(), crystalId);
			} catch (Exception ignored) {
			}
		}
		if (ctx().npcAi() != null) {
			ctx().npcAi().startCombat(mainNpc, active().objectId());
		}
	}

	public void handleBeastFeed(SkillTemplate sk, NpcInstance mainNpc) {
		if (mainNpc == null || mainNpc.isDead() || !mainNpc.isAttackable()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		broadcastLaunched(sk, List.of(mainNpc.objectId()));
		mainNpc.currentHp(mainNpc.maxHp());
		var su = StatusUpdate.hp(mainNpc.objectId(), (int) mainNpc.currentHp(), (int) mainNpc.maxHp());
		send(su);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
		if (ctx().npcAi() != null) {
			ctx().npcAi().startCombat(mainNpc, active().objectId());
		}
	}

	public void handleExtractableSkill(SkillTemplate sk) {
		broadcastLaunched(sk, List.of(active().objectId()));
		if (sk.itemConsumeId() > 0 && ctx().extractableItems() != null
				&& ctx().extractableItems().isExtractable(sk.itemConsumeId())) {
			ctx().extractableItems().getExtractableItem(sk.itemConsumeId()).ifPresent(exItem -> {
				if (!exItem.products().isEmpty() && !exItem.products().get(0).items().isEmpty()) {
					var firstReward = exItem.products().get(0).items().get(0);
					int prodId = firstReward.itemId();
					int prodCount = Math.max(1, firstReward.count());
					if (ctx().inventories() != null) {
						ctx().inventories().addItem(active().inventory(), prodId, prodCount, "ExtractableSkill");
					} else {
						active().inventory().addItem(prodId, prodCount);
					}
					send(ItemList.of(active().inventory().items(), false));
					send(SystemMessage.of(SystemMessage.EARNED_S2_S1_S,
							new SystemMessage.ItemName(prodId), new SystemMessage.ItemNumber(prodCount)));
				}
			});
		}
	}

	public void handleUnlockDoor(DoorInstance door, SkillTemplate sk) {
		if (door == null) {
			return;
		}
		if (!door.unlockable()) {
			send(SystemMessage.id(SystemMessage.UNABLE_TO_UNLOCK_DOOR));
			return;
		}
		if (door.isOpen()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A porta ja esta aberta."));
			return;
		}
		// #160: Calculo oficial de Unlock de portas via Formulas.calculateUnlockChance
		boolean unlocked = sk.power() > 0
				? (ThreadLocalRandom.current().nextInt(100) < (int) Math.max(10, Math.min(100, sk.power())))
				: com.lopez.l2j.game.combat.Formulas.calculateUnlockChance(sk.level(), 1, false, 1);
		if (unlocked) {
			door.openDoor();
			var update = new GameServerPacket.DoorStatusUpdate(door);
			send(update);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, update, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta destrancada com sucesso!"));
			GameSession.autoAttackScheduler().schedule(() -> {
				if (door.isOpen()) {
					door.closeDoor();
					var closeUpdate = new GameServerPacket.DoorStatusUpdate(door);
					send(closeUpdate);
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, closeUpdate, false);
				}
			}, 60, TimeUnit.SECONDS);
		} else {
			send(SystemMessage.id(SystemMessage.FAILED_TO_UNLOCK_DOOR));
		}
	}

	public void handleUnlockChest(NpcInstance npc, SkillTemplate sk) {
		var party = session.party();
		if (npc == null || active() == null || npc.isDead()) {
			return;
		}
		int chestLevel = npc.template() != null ? npc.template().level() : 1;
		boolean isMimic = npc.npcId() >= 18257 && npc.npcId() <= 18264;
		if (isMimic) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O bau era uma armadilha (Mimic)!"));
			if (ctx().npcAi() != null) {
				ctx().npcAi().startCombat(npc, active().objectId());
			}
			return;
		}

		int neededGrade = GameSession.getRequiredChestKeyGrade(chestLevel);
		boolean defaultUnlock = com.lopez.l2j.game.combat.Formulas.calculateUnlockChance(sk.level(), chestLevel, true,
				neededGrade);
		int chance = defaultUnlock ? 85 : 15;
		if ("DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType()) || sk.id() == 2229) {
			int keyGrade = sk.level();
			if (keyGrade >= neededGrade) {
				chance = 100;
			} else {
				chance = Math.max(0, 100 - (neededGrade - keyGrade) * 40);
			}
		} else if (sk.id() == 2065) { // Box Key
			int keyGrade = sk.level();
			if (keyGrade >= neededGrade) {
				chance = 40;
			} else {
				chance = Math.max(0, 40 - (neededGrade - keyGrade) * 20);
			}
		} else if (sk.id() == 27) { // Unlock skill
			int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
			int lvlDiff = chestLevel - magicLvl;
			int baseChance = (int) (sk.power() > 0 ? sk.power() : chance);
			if (lvlDiff > 5) {
				baseChance -= (lvlDiff - 5) * 10;
			}
			chance = Math.max(5, Math.min(100, baseChance));
		}

		var t = ctx().characters() != null ? ctx().characters().template(active()) : null;
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			var social = new SocialAction(active().objectId(), 3);
			send(social);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, social, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bau aberto com sucesso!"));

			session.stopAutoAttack();
			npc.dead(true);
			npc.currentHp(0);

			if (ctx().npcAi() != null) {
				ctx().npcAi().stopCombat(npc);
				ctx().npcAi().scheduleDecayAndRespawn(npc);
			}

			var su = StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp());
			send(su);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			var die = new Die(npc.objectId(), false);
			send(die);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, die, false);

			if (npc.template() != null) {
				double rateExp = ctx().rates() != null ? ctx().rates().xp() : 1.0;
				double rateSp = ctx().rates() != null ? ctx().rates().sp() : 1.0;
				long exp = (long) (npc.template().exp() * rateExp);
				int sp = (int) (npc.template().sp() * rateSp);
				if (party != null) {
					double ratePartyXp = ctx().rates() != null ? ctx().rates().partyXp() : 1.0;
					double ratePartySp = ctx().rates() != null ? ctx().rates().partySp() : 1.0;
					party.distributeExpAndSp(exp, sp, active(), ratePartyXp, ratePartySp);
				} else {
					session.applyExpAndSp(exp, sp, t);
				}
			}

			int rewardMobId = npc.npcId();
			if (rewardMobId >= 21801 && rewardMobId <= 21822) {
				rewardMobId -= 3536;
			}
			if (ctx().drops() != null) {
				ctx().drops().rewardMonsterDeath(active(), rewardMobId, chestLevel, ctx().inventories(), session::send);
			}
			if (ctx().questManager() != null) {
				ctx().questManager().onNpcKill(npc, session, false);
			}
			ctx().characters().save(active(), true);
		} else {
			var social = new SocialAction(active().objectId(), 13);
			send(social);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, social, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao abrir o bau!"));
			if (ctx().npcAi() != null) {
				ctx().npcAi().startCombat(npc, active().objectId());
			}
		}
	}

	/** Dano + debuffs de um skill ofensivo num monstro. */
	public void applyOffensive(SkillTemplate sk, NpcInstance npc, CharTemplate t, boolean soulshot,
			boolean sps, boolean bss) {
		var combat = ctx().combat();
		if (combat == null) {
			return;
		}
		if (combat.checkRaidCurse(active(), npc)) {
			applyControlEffect(com.lopez.l2j.game.combat.CombatService.RAID_CURSE_SILENCE, 1, "silence",
					System.currentTimeMillis() + 120_000L);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Voce foi atingido pela Maldicao de Raid Boss (Raid Curse)!"));
			return;
		}
		com.lopez.l2j.game.cubic.CubicService.getInstance().onOwnerAttack(active(), npc);
		CombatService.HitResult hit = null;
		double effPower = sk.power();
		if ("FATAL".equalsIgnoreCase(sk.skillType()) || sk.id() == 510) {
			if (!npc.isRaidBoss() && !npc.isGrandBoss()
					&& com.lopez.l2j.game.combat.Formulas.calcDeadlyRouletteSuccess(active().currentHp(),
							active().maxHp(), sk.power())) {
				int lethalDmg = Math.max(1, (int) Math.floor(npc.currentHp() - 1));
				hit = combat.applyDamage(npc, lethalDmg, 0);
			} else {
				send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
						new SystemMessage.NpcName(npc.npcId()),
						new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			effPower = 0;
		} else if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && (effPower > 0 || sk.id() == 348)) {
			double physPower = (sk.id() == 348 && effPower <= 10.0) ? (active().level() * 8.0) : effPower;
			hit = combat.skillPhysicalNpc(active(), t, npc, physPower, soulshot, sk.skillType().equals("BLOW"),
					sk.isChargedDam(), sk);
		} else if (sk.isMagicDamage() && effPower > 0) {
			hit = combat.skillMagicNpc(active(), t, npc, effPower, sk.magicLevel(), sps, bss, sk.element());
			if (sk.skillType().equals("DRAIN") && hit.damage() > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(hit.damage() * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamNpc(active(), t, npc, effPower, sk.magicLevel(), sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, npc.currentMp() - mpDam);
				npc.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1,
						new SystemMessage.Number(mpDam)));
				var su = StatusUpdate.mp(npc.objectId(), (int) npc.currentMp(), npc.template().maxMp());
				send(su);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		// Spoil (Skill 254 Spoil, 302 Spoil Festival, 348 Spoil Crush)
		boolean hasSpoilEffect = sk.id() == 254 || sk.id() == 302 || sk.id() == 348
				|| "SPOIL".equalsIgnoreCase(sk.skillType())
				|| sk.effects().stream().anyMatch(e -> "spoil".equalsIgnoreCase(e.name()));
		if (hasSpoilEffect) {
			handleSpoil(npc, sk);
		}

		// Debuffs de controle (Stun/Sleep/Paralyze/Root/Silence/Fear), DoTs e debuffs
		// de stats nos monstros
		if (!npc.isDead()) {
			boolean damageSkill = hit != null;
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("petrify")
						|| name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical")
						|| name.equals("bighead") || name.equals("grow");
				if (control) {
					double base = damageSkill ? 50 : (sk.power() > 0 ? sk.power() : 50);
					if (!combat.debuffLands(base, sk.magicLevel(), active().level(), npc, sps, bss)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.NpcName(npc.npcId()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					int abnormalMask = 0;
					switch (name) {
						case "root" -> {
							npc.root(until);
							abnormalMask = 0x0200;
						}
						case "sleep" -> {
							npc.disable(until, true);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0080;
						}
						case "stun" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0040;
						}
						case "paralyze" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0400;
						}
						case "petrification", "petrify", "medusa" -> {
							npc.disable(until, false);
							npc.invul(true);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0800;
							GameSession.autoAttackScheduler().schedule(() -> npc.invul(false),
									Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
						}
						case "bighead" -> abnormalMask = com.lopez.l2j.game.skill.AbnormalEffect.BIG_HEAD;
						case "grow" -> abnormalMask = com.lopez.l2j.game.skill.AbnormalEffect.GROW;
						case "silence", "mute" -> {
							npc.mute(until);
							npc.abortCast();
							abnormalMask = 0x0100;
						}
						case "physicalmute" -> {
							npc.physicalMute(until);
							abnormalMask = 0x0100;
						}
						case "silencemagicphysical" -> {
							npc.mute(until);
							npc.physicalMute(until);
							npc.abortCast();
							abnormalMask = 0x0100;
						}
						case "fear" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0020;
						}
					}
					if (abnormalMask != 0) {
						npc.startAbnormalEffect(abnormalMask);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
						final int fMask = abnormalMask;
						GameSession.autoAttackScheduler().schedule(() -> {
							if (npc != null) {
								npc.stopAbnormalEffect(fMask);
								ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc),
										false);
							}
						}, Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
					}
				} else if (name.equals("stunself")) {
					long selfUntil = System.currentTimeMillis() + Math.max(1000L, e.durationMs());
					applyControlEffect(sk.id(), sk.level(), "stun", selfUntil);
				} else if (name.equals("confusemob") || name.equals("confusion")) {
					handleConfusionSkill(sk, npc, null, sps, bss);
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType())
						|| "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(active().objectId());
					if (ctx().npcAi() != null) {
						ctx().npcAi().startCombat(npc, active().objectId());
					}
				} else if (name.equals("bluff") || name.equals("removetarget")
						|| "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType())
						|| "SWITCH".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(0);
					npc.abortAttack();
					npc.abortCast();
					var unsel = new GameServerPacket.TargetUnselected(npc.objectId(), npc.x(), npc.y(), npc.z());
					send(unsel);
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, unsel, false);
					if (sk.id() == 358 || name.equals("bluff")) { // #171: Bluff - StartRotation + StopRotation 180
																	// graus + Stun
						int newHeading = PlayerEffects.computeBluffHeading(active().heading());
						var startRot = new GameServerPacket.StartRotation(npc.objectId(), npc.heading(), 1, 65535);
						var stopRot = new GameServerPacket.StopRotation(npc.objectId(), newHeading, 65535);
						npc.heading(newHeading);
						send(startRot);
						send(stopRot);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, startRot, false);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stopRot, false);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
								new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()), false);
						long stunUntil = System.currentTimeMillis() + Math.max(3000L, e.durationMs());
						npc.disable(stunUntil, false);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(), npc,
							sps,
							bss)) {
						startNpcDot(npc, sk, e, t);
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(), npc,
							sps,
							bss)) {
						applyNpcDebuff(npc, e);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.NpcName(npc.npcId()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				}
			}
		}

		if (hit != null) {
			handleNpcHit(npc, hit, t, sk);
		} else if (!npc.isDead() && ctx().npcAi() != null) {
			ctx().npcAi().startCombat(npc, active().objectId()); // debuff puro tambem gera aggro
		}
	}

	public boolean canPlayerSweepCorpse(NpcInstance corpse) {
		if (corpse == null || active() == null) {
			return false;
		}
		if (corpse.spoilerPlayerId() == 0 || corpse.spoilerPlayerId() == active().objectId()) {
			return true;
		}
		return session.party() != null && session.party().contains(corpse.spoilerPlayerId());
	}

	public void handleSpoil(NpcInstance npc, SkillTemplate sk) {
		if (npc.isDead()) {
			return;
		}
		if (npc.isSpoiled()) {
			send(SystemMessage.id(SystemMessage.ALREADY_SPOILED));
			if (ctx().npcAi() != null) {
				ctx().npcAi().startCombat(npc, active().objectId());
			}
			return;
		}

		int targetLvl = npc.template() != null ? npc.template().level() : 1;
		int skillLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
		double lvlDiff = targetLvl - skillLvl;
		// Formula oficial Lineage 2 / L2JDream:
		// Mesma faixa de nivel ou inferior = ~99% de sucesso (falha rara)
		// Nivel superior decai progressivamente de forma exponencial suave (pow(1.3,
		// diff) * 100 em 10000)
		int failRate = (int) Math.round(Math.pow(1.3, lvlDiff) * 100.0);
		failRate = Math.max(100, Math.min(9500, failRate)); // Entre 99% e 5% de chance de sucesso
		boolean success = ThreadLocalRandom.current().nextInt(10000) >= failRate;
		if (success) {
			npc.spoiled(true);
			npc.spoilerPlayerId(active().objectId());
			send(SystemMessage.id(SystemMessage.SPOIL_SUCCESS));
		} else {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.NpcName(npc.npcId()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}

		if (ctx().npcAi() != null) {
			ctx().npcAi().startCombat(npc, active().objectId());
		}
	}

	public void applyNpcDebuff(NpcInstance npc, SkillTemplate.EffectTemplate e) {
		for (var f : e.funcs()) {
			double val = f.value() > 0 ? f.value() : 0.77;
			long dur = Math.max(1000, e.durationMs());
			switch (f.stat()) {
				case "pDef" -> {
					double prev = npc.pDefMul();
					npc.pDefMul(Math.min(prev, val));
					GameSession.autoAttackScheduler().schedule(() -> npc.pDefMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "mDef" -> {
					double prev = npc.mDefMul();
					npc.mDefMul(Math.min(prev, val));
					GameSession.autoAttackScheduler().schedule(() -> npc.mDefMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "pAtk" -> {
					double prev = npc.pAtkMul();
					npc.pAtkMul(Math.min(prev, val));
					GameSession.autoAttackScheduler().schedule(() -> npc.pAtkMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "mAtk" -> {
					double prev = npc.mAtkMul();
					npc.mAtkMul(Math.min(prev, val));
					GameSession.autoAttackScheduler().schedule(() -> npc.mAtkMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "runSpd" -> {
					double prev = npc.runSpdMul();
					npc.runSpdMul(Math.min(prev, val));
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					GameSession.autoAttackScheduler().schedule(() -> {
						npc.runSpdMul(prev);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					}, dur, TimeUnit.MILLISECONDS);
				}
				case "pAtkSpd" -> {
					double prev = npc.pAtkSpdMul();
					npc.pAtkSpdMul(Math.min(prev, val));
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					GameSession.autoAttackScheduler().schedule(() -> {
						npc.pAtkSpdMul(prev);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					}, dur, TimeUnit.MILLISECONDS);
				}
				case "mAtkSpd" -> {
					double prev = npc.mAtkSpdMul();
					npc.mAtkSpdMul(Math.min(prev, val));
					GameSession.autoAttackScheduler().schedule(() -> npc.mAtkSpdMul(prev), dur, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	public void startNpcDot(NpcInstance npc, SkillTemplate sk, SkillTemplate.EffectTemplate e, CharTemplate t) {
		int count = Math.max(1, e.count());
		long period = Math.max(1, e.period()) * 1000L;
		int damagePerTick = (int) Math.max(1, Math.round(e.val() > 0 ? e.val() : 20));
		int[] rem = { count };
		PlayerCharacter owner = active();
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		boolean isManaDot = kind.equals("manadamovertime");
		AtomicReference<ScheduledFuture<?>> taskRef = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (npc == null || npc.isDead() || rem[0] <= 0 || !session.inWorld() || active() != owner) {
				var f = taskRef.get();
				if (f != null) {
					f.cancel(false);
				}
				return;
			}
			rem[0]--;
			if (isManaDot) {
				npc.currentMp(Math.max(0, npc.currentMp() - damagePerTick));
				var su = StatusUpdate.mp(npc.objectId(), (int) npc.currentMp(), npc.template().maxMp());
				send(su);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			} else {
				var hit = ctx().combat().applyDamage(npc, damagePerTick, 0);
				handleNpcHit(npc, hit, t, sk);
				if (hit.isDead()) {
					var f = taskRef.get();
					if (f != null) {
						f.cancel(false);
					}
				}
			}
		}, period, period, TimeUnit.MILLISECONDS);
		taskRef.set(task);
	}

	/** Dano + debuffs de um skill ofensivo em outro jogador (PvP). */
	public void applyOffensivePlayer(SkillTemplate sk, GameSession targetSession, CharTemplate t,
			boolean soulshot, boolean sps, boolean bss) {
		var combat = ctx().combat();
		if (combat == null || targetSession == null || targetSession.active() == null
				|| targetSession.active().isDead()) {
			return;
		}
		var targetActive = targetSession.active();
		if ((active().karma() > 0 || active().pkKills() > 0 || active().pvpFlag() > 0)
				&& active().level() - targetActive.level() >= 10
				&& targetActive.effects().hasProtectionBlessing()) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		com.lopez.l2j.game.cubic.CubicService.getInstance().onOwnerAttack(active(), targetActive);
		var targetTemplate = ctx().characters().template(targetActive);
		int damage = 0;
		double effPower = sk.power();
		if ("FATAL".equalsIgnoreCase(sk.skillType()) || sk.id() == 510) {
			if (com.lopez.l2j.game.combat.Formulas.calcDeadlyRouletteSuccess(active().currentHp(), active().maxHp(),
					sk.power())) {
				targetActive.currentCp(1);
				targetActive.currentHp(1);
				targetSession.sendVitals();
				var su = new StatusUpdate(targetActive.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 1),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, 1)));
				send(su);
				ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			} else {
				send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
						new SystemMessage.Text(targetActive.name()),
						new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			effPower = 0;
		} else if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && effPower > 0) {
			damage = combat.skillPhysicalPlayer(active(), t, targetActive, targetTemplate, effPower, soulshot,
					sk.skillType().equals("BLOW"), sk.isChargedDam(), sk);
		} else if (sk.isMagicDamage() && effPower > 0) {
			damage = combat.skillMagicPlayer(active(), t, targetActive, targetTemplate, effPower, sk.magicLevel(), sps,
					bss, sk.element());
			if (damage <= 1 && targetActive.level()
					- (sk.magicLevel() > 0 ? Math.min(sk.magicLevel(), active().level()) : active().level()) > 9) {
				send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
						new SystemMessage.Text(targetActive.name()),
						new SystemMessage.SkillName(sk.id(), sk.level())));
				targetSession.send(SystemMessage.of(
						sk.skillType().equals("DRAIN") ? SystemMessage.RESISTED_S1_DRAIN
								: SystemMessage.RESISTED_S1_MAGIC,
						new SystemMessage.Text(active().name())));
			}
			if (sk.skillType().equals("DRAIN") && damage > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(damage * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamPlayer(active(), t, targetActive, targetTemplate, effPower, sk.magicLevel(),
					sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, targetActive.currentMp() - mpDam);
				targetActive.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1,
						new SystemMessage.Number(mpDam)));
				targetSession.send(SystemMessage.of(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1,
						new SystemMessage.Text(active().name()),
						new SystemMessage.Number(mpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.mp(targetActive.objectId(), (int) targetActive.currentMp(), targetActive.maxMp());
				send(su);
				ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		} else if (sk.isCpDamage()) {
			// #129: CPDAM via Formulas.calcCpDam
			int cpDam = com.lopez.l2j.game.combat.Formulas.calcCpDam(targetActive.currentCp(), sk.power());
			if (cpDam > 0) {
				targetActive.currentCp(Math.max(0, targetActive.currentCp() - cpDam));
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(cpDam)));
				targetSession.send(
						SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active().name()),
								new SystemMessage.Number(cpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.cp(targetActive.objectId(), (int) targetActive.currentCp(), targetActive.maxCp());
				send(su);
				ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		if (damage > 0) {
			if (targetActive.activeSummon() != null && !targetActive.activeSummon().isDead()
					&& !targetActive.hasSkill(1262)) {
				int transferPercent = (int) Math.round(PlayerStats.applyStat(targetActive, "transferDam", 0.0));
				if (transferPercent > 0) {
					damage = targetActive.activeSummon().divertTransferPainDamage(damage, transferPercent);
				}
			}
			var result = combat.applyDamagePlayer(targetActive, damage);
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(damage)));
			targetSession
					.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active().name()),
							new SystemMessage.Number(damage)));
			targetSession.onAttacked(active().objectId(), damage);
			targetSession.sendVitals();

			var su = StatusUpdate.hp(targetActive.objectId(), (int) targetActive.currentHp(), targetActive.maxHp());
			send(su);
			ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);

			if (result.isDead()) {
				targetSession.handlePlayerDeath(active());
				return;
			}
			PlayerCombatHandler.triggerSummonAssistAttackPlayer(session, ctx(), targetSession);
		}

		// Debuffs & DoTs em jogadores
		if (!targetActive.isDead()) {
			// #174: Touch of Death (Skill 342)
			if (sk.id() == 342 || sk.effects().stream().anyMatch(e -> "touchofdeath".equalsIgnoreCase(e.name()))) {
				int effMagicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
				targetActive.effects().applyTouchOfDeath(targetActive, effMagicLvl, sk.power() > 0 ? sk.power() : 25.0);
				targetSession.refreshBuffs();
				targetSession.saveBuffs();
				targetSession.sendVitals();
			}
			// #136: CANCEL (Skill 1056) com probabilidade individual por buff
			// (Formulas.calcCancelSuccess)
			if ("CANCEL".equalsIgnoreCase(sk.skillType()) || sk.id() == 1056) {
				int effMagicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
				double cancelVuln = PlayerStats.applyStat(targetActive, "cancelVuln", 0.0);
				double cancelProf = PlayerStats.applyStat(active(), "cancelProf", 0.0);
				List<ActiveBuff> canceled = targetActive.effects().cancelBuffsWithDetails(
						5, effMagicLvl, targetActive.level(), 25.0, cancelProf, cancelVuln);
				if (!canceled.isEmpty()) {
					targetSession.refreshBuffs();
					targetSession.saveBuffs();
					targetSession.sendMagicEffectIcons();
				} else {
					send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
							new SystemMessage.Text(targetActive.name()),
							new SystemMessage.SkillName(sk.id(), sk.level())));
				}
			}
			// #137: WARRIOR_BANE (1344, 1350)
			if ("WARRIOR_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1344 || sk.id() == 1350) {
				boolean removed = targetActive.effects().removeWarriorBaneBuffs();
				if (removed) {
					targetSession.refreshBuffs();
					targetSession.saveBuffs();
				} else {
					send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
							new SystemMessage.Text(targetActive.name()),
							new SystemMessage.SkillName(sk.id(), sk.level())));
				}
			}
			// #137: MAGE_BANE (1345, 1351)
			if ("MAGE_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1345 || sk.id() == 1351) {
				boolean removed = targetActive.effects().removeMageBaneBuffs();
				if (removed) {
					targetSession.refreshBuffs();
					targetSession.saveBuffs();
				} else {
					send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
							new SystemMessage.Text(targetActive.name()),
							new SystemMessage.SkillName(sk.id(), sk.level())));
				}
			}
			if ("STEAL_BUFF".equalsIgnoreCase(sk.skillType()) || sk.id() == 1440) {
				double cancelVuln = Math.max(0.0,
						Math.min(2.0, PlayerStats.applyStat(targetActive, "cancelVuln", 1.0)));
				int baseSteal = sk.power() > 0 && sk.power() <= 10 ? (int) Math.round(sk.power()) : 3;
				int maxSteal = (int) Math.round(baseSteal * cancelVuln);
				if (maxSteal > 0) {
					var stolenBuffs = targetActive.effects().stealBuffs(maxSteal);
					if (!stolenBuffs.isEmpty()) {
						long now = System.currentTimeMillis();
						PlayerCharacter casterChar = active();
						for (var sb : stolenBuffs) {
							long remMs = sb.remainingMillis(now);
							if (remMs <= 0) {
								continue;
							}
							var copyBuff = new ActiveBuff(sb.skillId(), sb.level(), sb.stackType(), sb.stackOrder(),
									now + remMs, sb.runSpdAdd(), sb.pAtkSpdMul(), sb.mAtkSpdMul(), sb.accuracyAdd(),
									sb.funcs(), false);
							casterChar.effects().put(copyBuff, PlayerEffects.getExtraBuffSlots(casterChar));
							GameSession.autoAttackScheduler().schedule(() -> {
								if (casterChar.effects().remove(copyBuff) && active() == casterChar
										&& session.inWorld()) {
									send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
											new SystemMessage.SkillName(copyBuff.skillId(), copyBuff.level())));
									session.refreshBuffs();
									session.saveBuffs();
								}
							}, remMs, TimeUnit.MILLISECONDS);
						}
						targetSession.refreshBuffs();
						targetSession.saveBuffs();
						session.refreshBuffs();
						session.saveBuffs();
						send(new CreatureSay(0, CreatureSay.ALL, "Combate",
								"Voce roubou " + stolenBuffs.size() + " buff(s) de " + targetActive.name() + "!"));
						targetSession.send(new CreatureSay(0, CreatureSay.ALL, "Combate",
								active().name() + " roubou " + stolenBuffs.size() + " buff(s) seus!"));
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				} else {
					send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
							new SystemMessage.Text(targetActive.name()),
							new SystemMessage.SkillName(sk.id(), sk.level())));
				}
			}
			if (sk.id() == 1358 || sk.id() == 1360) { // Block Shield / Mass Block Shield
				var shieldIds = java.util.Set.of(1040, 4324, 1236, 1389);
				var shieldStacks = java.util.Set.of("pd_up", "pdef", "p_def", "shield", "greater_shield");
				var shieldStats = java.util.Set.of("pDef");
				if (targetActive.effects().removeBuffsByIdsOrStackOrStats(shieldIds, shieldStacks, shieldStats)) {
					targetSession.refreshBuffs();
					targetSession.saveBuffs();
				}
			}
			if (sk.id() == 1359 || sk.id() == 1361) { // Block Wind Walk / Mass Block Wind Walk
				var wwIds = java.util.Set.of(1204, 4322, 268, 1282);
				var wwStacks = java.util.Set.of("speed_up", "runspd", "run_spd", "wind_walk");
				var wwStats = java.util.Set.of("runSpd");
				if (targetActive.effects().removeBuffsByIdsOrStackOrStats(wwIds, wwStacks, wwStats)) {
					targetSession.refreshBuffs();
					targetSession.saveBuffs();
				}
			}

			if (sk.effects().isEmpty()) {
				String st = sk.skillType().toLowerCase(java.util.Locale.ROOT);
				boolean control = st.equals("stun") || st.equals("sleep") || st.equals("paralyze")
						|| st.equals("root") || st.equals("mute") || st.equals("fear");
				if (control) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss, st)) {
						long until = System.currentTimeMillis() + 15000L;
						targetSession.applyControlEffect(sk.id(), sk.level(), st, until);
						targetSession.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
								new SystemMessage.SkillName(sk.id(), sk.level())));
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						targetSession.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.Text(active().name())));
					}
				}
			}

			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("petrify")
						|| name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical")
						|| name.equals("bighead") || name.equals("grow");

				if (control) {
					if (!combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss, name)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						targetSession.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.Text(active().name())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					if (name.equals("fear")) {
						targetActive.effects().startFear(Math.max(1000L, e.durationMs()), active().x(), active().y());
					}
					targetSession.applyControlEffect(sk.id(), sk.level(), name, until);
					targetSession.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
							new SystemMessage.SkillName(sk.id(), sk.level())));
				} else if (name.equals("stunself")) {
					long selfUntil = System.currentTimeMillis() + Math.max(1000L, e.durationMs());
					applyControlEffect(sk.id(), sk.level(), "stun", selfUntil);
				} else if (name.equals("confusemob") || name.equals("confusion")) {
					handleConfusionSkill(sk, null, targetSession, sps, bss);
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType())
						|| "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					targetSession.forceTarget(active().objectId());
					if (targetSession.isAutoAttacking()) {
						targetSession.schedulePlayerAutoAttack(session);
					}
				} else if (name.equals("bluff") || name.equals("removetarget")
						|| "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType())
						|| "SWITCH".equalsIgnoreCase(sk.skillType())) {
					var unsel = new GameServerPacket.TargetUnselected(
							targetActive.objectId(), targetActive.x(), targetActive.y(), targetActive.z());
					targetSession.send(unsel);
					ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, unsel, false);
					targetSession.clearTarget();
					targetSession.stopAutoAttack();
					targetSession.cancelCast();
					if (sk.id() == 358 || name.equals("bluff")) { // #171: Bluff - StartRotation + StopRotation 180
																	// graus + Stun
						int newHeading = PlayerEffects.computeBluffHeading(active().heading());
						var startRot = new GameServerPacket.StartRotation(targetActive.objectId(),
								targetActive.heading(), 1, 65535);
						var stopRot = new GameServerPacket.StopRotation(targetActive.objectId(), newHeading, 65535);
						targetActive.heading(newHeading);
						targetSession.send(startRot);
						targetSession.send(stopRot);
						ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, startRot, false);
						ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, stopRot, false);
						var valLoc = new ValidateLocation(targetActive.objectId(), targetActive.x(), targetActive.y(),
								targetActive.z(), newHeading);
						ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, valLoc, true);
						long stunUntil = System.currentTimeMillis() + Math.max(3000L, e.durationMs());
						targetSession.applyControlEffect(sk.id(), sk.level(), "stun", stunUntil);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					String dotKind = (name.equals("poison") || "POISON".equalsIgnoreCase(sk.skillType())) ? "poison"
							: ((name.equals("bleed") || "BLEED".equalsIgnoreCase(sk.skillType())) ? "bleed" : name);
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss, dotKind)) {
						targetSession.startSkillDot(sk, e);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						targetSession.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.Text(active().name())));
					}
				} else if (!e.funcs().isEmpty() || sk.isDebuff() || sk.isOffensive() || name.equals("debuff")) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss, sk.skillType())) {
						targetSession.applySkillEffects(sk, false);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						targetSession.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.Text(active().name())));
					}
				}
			}
		}
	}

	public void applyControlEffect(String effectName, long until) {
		applyControlEffect(0, 1, effectName, until);
	}

	public void applyControlEffect(int skillId, int skillLevel, String effectName, long until) {
		if (active() == null || active().isDead()) {
			return;
		}
		int abnormalMask = 0;
		switch (effectName) {
			case "root" -> {
				active().root(until);
				abnormalMask = 0x0200;
			}
			case "sleep" -> {
				active().disable(until, true);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0080;
			}
			case "stun" -> {
				active().disable(until, false);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0040;
			}
			case "paralyze" -> {
				active().disable(until, false);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0400;
			}
			case "petrification", "petrify", "medusa" -> {
				active().disable(until, false);
				active().invul(true);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0800;
				long dur = Math.max(1000, until - System.currentTimeMillis());
				GameSession.autoAttackScheduler().schedule(() -> {
					if (active() != null) {
						active().invul(false);
					}
				}, dur, TimeUnit.MILLISECONDS);
			}
			case "bighead" -> abnormalMask = com.lopez.l2j.game.skill.AbnormalEffect.BIG_HEAD;
			case "grow" -> abnormalMask = com.lopez.l2j.game.skill.AbnormalEffect.GROW;
			case "mute", "silence" -> {
				active().mute(until);
				cancelCast();
				abnormalMask = 0x0100;
			}
			case "physicalmute" -> {
				active().physicalMute(until);
				cancelCast();
				abnormalMask = 0x0100;
			}
			case "silencemagicphysical" -> {
				active().mute(until);
				active().physicalMute(until);
				cancelCast();
				abnormalMask = 0x0100;
			}
			case "fear" -> {
				active().disable(until, false);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0020 | PlayerEffects.ABNORMAL_FEAR;
				long fearDur = Math.max(1000L, until - System.currentTimeMillis());
				if (!active().effects().isAfraid()) {
					active().effects().startFear(fearDur, active().x(), active().y());
				}
				// #172: Fear movement - afasta o jogador das coordenadas do conjurador a cada
				// tick
				PlayerCharacter fearedChar = active();
				int ticks = (int) Math.max(1L, fearDur / 2000L);
				int[] remTicks = { ticks };
				AtomicReference<ScheduledFuture<?>> fearRef = new AtomicReference<>();
				ScheduledFuture<?> fearTask = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
					if (active() != fearedChar || !session.inWorld() || fearedChar.isDead()
							|| !fearedChar.effects().isAfraid() || remTicks[0]-- <= 0) {
						var f = fearRef.get();
						if (f != null) {
							f.cancel(false);
						}
						return;
					}
					int ox = fearedChar.x();
					int oy = fearedChar.y();
					int oz = fearedChar.z();
					int[] dest = fearedChar.effects().computeFearDestination(fearedChar);
					fearedChar.moveTo(dest[0], dest[1], dest[2]);
					var move = new MoveToLocation(fearedChar.objectId(), dest[0], dest[1], dest[2], ox, oy, oz);
					send(move);
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
				}, 500L, 2000L, TimeUnit.MILLISECONDS);
				fearRef.set(fearTask);
			}
		}
		ActiveBuff buff = null;
		if (skillId > 0) {
			String stack = "control_" + effectName;
			buff = ActiveBuff.ofSkill(skillId, skillLevel, stack, skillLevel, until, List.of(), true, abnormalMask,
					effectName, 0.0, 0);
			if (!active().effects().put(buff, PlayerEffects.getExtraBuffSlots(active()))) {
				buff = null;
			}
		}
		if (abnormalMask != 0) {
			active().startAbnormalEffect(abnormalMask);
			session.broadcastAppearance();
		}
		final int finalMask = abnormalMask;
		final ActiveBuff finalBuff = buff;
		long delay = Math.max(100, until - System.currentTimeMillis());
		GameSession.autoAttackScheduler().schedule(() -> {
			if (active() != null) {
				if ("fear".equals(effectName)) {
					active().effects().stopFear();
				}
				if (finalBuff != null && active().effects().remove(finalBuff)) {
					send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
							new SystemMessage.SkillName(skillId, skillLevel)));
					session.refreshBuffs();
				}
				if (finalMask != 0) {
					boolean stillHasMask = false;
					for (var b : active().effects().active()) {
						if ((b.abnormalMask() & finalMask) != 0) {
							stillHasMask = true;
							break;
						}
					}
					if (!stillHasMask) {
						active().stopAbnormalEffect(finalMask);
						session.broadcastAppearance();
					}
				}
			}
		}, delay, TimeUnit.MILLISECONDS);

		session.refreshBuffs();
	}

	public void handlePlayerDeath() {
		handlePlayerDeath(null);
	}

	public void onDeath() {
		handlePlayerDeath(null);
	}

	public void onDeath(int killerObjectId) {
		PlayerCharacter killer = null;
		if (killerObjectId != 0 && ctx() != null && ctx().world() != null) {
			var opt = ctx().world().player(killerObjectId);
			if (opt.isPresent()) {
				killer = opt.get().character();
			}
		}
		handlePlayerDeath(killer);
	}

	public void handlePlayerDeath(PlayerCharacter killer) {
		if (active() == null) {
			return;
		}
		session.stopAutoAttack();
		cancelCast();
		session.clearCharges();
		session.clearTarget();
		if (ctx() != null && ctx().npcAi() != null) {
			ctx().npcAi().stopCombatForPlayer(active().objectId());
		}
		session.clearHotTasks();
		active().stopAllAbnormalEffects();
		session.broadcastAppearance();
		active().invul(false);
		active().currentHp(0);
		active().currentCp(0);

		// #166, #167, #168: Salvation / PhoenixBless (1410/438), NoblesseBless (1323),
		// CharmOfLuck (2168) e CharmOfCourage (5041)
		boolean preserveBuffs = !Config.LEAVE_BUFFS_ON_DIE;
		boolean hasSalvation = active().effects().consumeSalvation();
		boolean hasNoblesse = !hasSalvation && active().effects().consumeNoblesseBless();
		boolean hasCharmOfLuck = active().effects().consumeCharmOfLuck();
		boolean hasCharmOfCourage = active().isInSiege() && active().effects().consumeCharmOfCourage();

		if (preserveBuffs || hasSalvation || hasNoblesse) {
			active().effects().clearDebuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
		} else {
			active().effects().clear();
			if (ctx() != null && ctx().buffRepository() != null) {
				ctx().buffRepository().deleteBuffs(active().objectId());
			}
			send(new MagicEffectIcons(List.of()));
		}

		var tTemplate = ctx().characters().template(active());
		if (ctx().skillService() != null) {
			recalcMaxVitals(tTemplate);
		}

		if (active().level() >= 10 && !active().skills().containsKey(SkillService.SKILL_LUCKY)
				&& !hasSalvation && !hasCharmOfLuck && !hasCharmOfCourage) {
			long expForCurLevel = com.lopez.l2j.game.model.ExperienceTable.expForLevel(active().level());
			long expForNextLevel = com.lopez.l2j.game.model.ExperienceTable.expForLevel(active().level() + 1);
			long expDiff = Math.max(1, expForNextLevel - expForCurLevel);
			long lostExp = (long) (expDiff * 0.04);
			if (active().exp() - lostExp < expForCurLevel && !Config.DELEVEL) {
				active().exp(expForCurLevel);
			} else {
				active().exp(Math.max(0, active().exp() - lostExp));
				int newLvl = com.lopez.l2j.game.model.ExperienceTable.calculateLevel(active().exp());
				if (newLvl != active().level()) {
					active().level(newLvl);
					var t = ctx() != null && ctx().characters() != null ? ctx().characters().template(active()) : null;
					session.rewardSkills(t, false);
				}
			}
			send(new UserInfo(active(), ctx().characters().template(active())));
		}

		send(new StatusUpdate(active().objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, 0),
				new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active().exp()),
				new StatusUpdate.Attribute(StatusUpdate.SP, active().sp()))));

		boolean hasClanHall = false;
		boolean hasCastle = false;
		if (ctx().clans() != null && active().clanId() > 0) {
			var clan = ctx().clans().byClanId(active().clanId()).orElse(null);
			if (clan != null) {
				hasClanHall = clan.clanHallId() > 0;
				hasCastle = clan.castleId() > 0;
			}
		}
		var die = new Die(active().objectId(), true, hasClanHall, hasCastle);
		send(die);
		var dieObserver = new Die(active().objectId(), false);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, dieObserver, false);

		// #166: Salvation / Soul of the Phoenix oferece ConfirmDlg (1510) para
		// ressurreicao imediata com 100% EXP
		if (hasSalvation) {
			send(new GameServerPacket.ConfirmDlg(1510, active().name(), 15000, active().objectId()));
		}

		if (killer != null && ctx() != null && ctx().world() != null) {
			ctx().world().player(killer.objectId()).ifPresentOrElse(op -> {
				if (op instanceof GameSession killerSession) {
					PlayerCombatHandler.handlePlayerDeath(killerSession, session, ctx(), killer, active());
				} else {
					PlayerCombatHandler.handlePlayerDeath(null, session, ctx(), killer, active());
				}
			}, () -> PlayerCombatHandler.handlePlayerDeath(null, session, ctx(), killer, active()));
		} else if (ctx() != null) {
			com.lopez.l2j.game.zone.FlagZoneService flagZone = ctx()
					.getBean(com.lopez.l2j.game.zone.FlagZoneService.class);
			if (flagZone != null && flagZone.isInFlagZone(active())) {
				flagZone.onPlayerDeath(active(), session::send);
			}
		}

		ctx().characters().save(active(), true);
	}

	public void rewardBarakielNoblesse(com.lopez.l2j.game.party.Party party, PlayerCharacter killerChar) {
		if (party != null) {
			for (GameSession member : party.members()) {
				if (member != null && member.active() != null) {
					double dist = Math.hypot(member.active().x() - killerChar.x(),
							member.active().y() - killerChar.y());
					if (dist <= com.lopez.l2j.game.party.Party.getPartyRange()) {
						grantNoblesseStatus(member);
					}
				}
			}
		} else {
			grantNoblesseStatus(session);
		}
	}

	public void grantNoblesseStatus(GameSession session) {
		if (session == null || session.active() == null) {
			return;
		}
		PlayerCharacter c = session.active();
		if (!c.isNoble() && c.level() >= 75) {
			c.setNoble(true);
			session.send(SystemMessage.id(SystemMessage.YOU_HAVE_BECOME_A_NOBLESSE));
			session.send(new SocialAction(c.objectId(), 16));
			session.sendUserInfoAndBroadcastCharInfo();
			if (session.ctx() != null && session.ctx().characters() != null) {
				session.ctx().characters().save(c, true);
			}
		}
	}

	public void cancelCast() {
		if (session.casting()) {
			session.casting(false);
			session.castingSkill(null);
			var task = session.castTask();
			if (task != null) {
				task.cancel(false);
				session.castTask(null);
			}
			var cancel = new MagicSkillCanceld(active().objectId());
			send(cancel);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, cancel, false);
			send(SystemMessage.id(SystemMessage.CASTING_INTERRUPTED));
			send(new ActionFailed());
		}
	}

	public void updatePvPFlag() {
		updatePvPFlag(false);
	}

	public void updatePvPFlag(boolean againstPvPPlayer) {
		if (active() == null) {
			return;
		}
		if (ctx().zones() != null && ctx().zones().isInsidePeace(active().x(), active().y(), active().z())) {
			return;
		}
		long flagDuration = againstPvPPlayer ? Config.PVP_VS_PVP_TIME : Config.PVP_VS_NORMAL_TIME;
		if (flagDuration <= 0) {
			flagDuration = 20_000L;
		}
		boolean changed = active().pvpFlag() == 0;
		active().pvpFlag(1);
		active().pvpFlagEndTime(System.currentTimeMillis() + flagDuration);
		if (changed) {
			session.broadcastAppearance();
		}
		PlayerCharacter owner = active();
		GameSession.autoAttackScheduler().schedule(() -> {
			if (active() == owner && active().pvpFlag() == 1
					&& System.currentTimeMillis() >= active().pvpFlagEndTime()) {
				active().pvpFlag(0);
				session.broadcastAppearance();
			}
		}, flagDuration + 100L, TimeUnit.MILLISECONDS);
	}

	/** Mensagem de dano, HP do alvo, morte (EXP/drop) ou aggro. */
	public void handleNpcHit(NpcInstance npc, CombatService.HitResult hit, CharTemplate t, SkillTemplate sk) {
		var party = session.party();
		if (hit.resisted() && sk != null) {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.NpcName(npc.npcId()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}
		if (hit.damage() > 0) {
			if ((hit.flags() & 0x20) != 0) {
				send(SystemMessage
						.id(sk != null && sk.magic() ? SystemMessage.CRITICAL_HIT_MAGIC : SystemMessage.CRITICAL_HIT));
			}
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
		}
		int remainingHp = hit.isDead() ? 0 : Math.max(0, hit.remainingHp());
		var su = StatusUpdate.hp(npc.objectId(), remainingHp, (int) npc.maxHp());
		if (ctx().questManager() != null) {
			ctx().questManager().onNpcAttack(npc, session, false);
		}
		send(su);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
		if (hit.isDead()) {
			PlayerCombatHandler.handleMonsterDeath(session, ctx(), active(), t, npc, party, hit.expReward(),
					hit.spReward());
		} else if (!npc.isDead() && ctx().npcAi() != null) {
			ctx().npcAi().startCombat(npc, active().objectId());
			PlayerCombatHandler.triggerSummonAssistAttack(session, ctx(), npc);
		}
	}

	/**
	 * Cura/buff aplicado a um SummonInstance (Servitor ou Pet).
	 */
	public void handlePositiveSkillOnSummon(SkillTemplate sk, com.lopez.l2j.game.summon.SummonInstance summon,
			CharTemplate t, boolean sps, boolean bss) {
		if (summon == null || summon.isDead() || active() == null) {
			return;
		}
		int mAtk = t != null ? PlayerStats.calculate(active(), t).mAtk() : 1;
		double ssMul = bss ? 4.0 : (sps ? 2.0 : 1.0);
		double power = sk.power();
		switch (sk.skillType()) {
			case "HEAL" -> {
				double healAmount = power + Math.sqrt(Math.max(1.0, mAtk * ssMul));
				double before = summon.currentHp();
				summon.currentHp(Math.min(summon.maxHp(), before + healAmount));
				int restored = (int) Math.max(0, summon.currentHp() - before);
				send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(restored)));
			}
			case "HEAL_STATIC" -> {
				double before = summon.currentHp();
				summon.currentHp(Math.min(summon.maxHp(), before + power));
				int restored = (int) Math.max(0, summon.currentHp() - before);
				send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(restored)));
			}
			case "HEAL_PERCENT" -> {
				double before = summon.currentHp();
				summon.currentHp(Math.min(summon.maxHp(), before + summon.maxHp() * power / 100.0));
				int restored = (int) Math.max(0, summon.currentHp() - before);
				send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(restored)));
			}
			case "BALANCE_LIFE" -> {
				double totalCurHp = active().currentHp() + summon.currentHp();
				double totalMaxHp = Math.max(1.0, active().maxHp() + summon.maxHp());
				double ratio = Math.min(1.0, Math.max(0.01, totalCurHp / totalMaxHp));
				active().currentHp(Math.max(1, Math.min(active().maxHp(), (int) Math.round(active().maxHp() * ratio))));
				summon.currentHp(Math.max(1, Math.min(summon.maxHp(), (int) Math.round(summon.maxHp() * ratio))));
				session.sendVitals();
			}
			case "MANAHEAL", "MANARECHARGE" -> {
				double mpAmount = power;
				if ("MANARECHARGE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1013) {
					int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
					int summonLvl = summon.level() > 0 ? summon.level()
							: (summon.template() != null ? summon.template().level() : active().level());
					int diff = summonLvl - magicLvl;
					if (diff > 5) {
						double mul = Math.max(0.2, 1.0 - (diff - 5) * 0.10);
						mpAmount *= mul;
					}
				}
				double before = summon.currentMp();
				summon.currentMp(Math.min(summon.maxMp(), before + mpAmount));
				int restored = (int) Math.max(0, summon.currentMp() - before);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED, new SystemMessage.Number(restored)));
			}
			case "MANAHEAL_PERCENT" -> {
				double before = summon.currentMp();
				summon.currentMp(Math.min(summon.maxMp(), before + summon.maxMp() * power / 100.0));
				int restored = (int) Math.max(0, summon.currentMp() - before);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED, new SystemMessage.Number(restored)));
			}
			default -> {
				if (sk.id() == 1335) { // Balance Life por ID
					double totalCurHp = active().currentHp() + summon.currentHp();
					double totalMaxHp = Math.max(1.0, active().maxHp() + summon.maxHp());
					double ratio = Math.min(1.0, Math.max(0.01, totalCurHp / totalMaxHp));
					active().currentHp(
							Math.max(1, Math.min(active().maxHp(), (int) Math.round(active().maxHp() * ratio))));
					summon.currentHp(Math.max(1, Math.min(summon.maxHp(), (int) Math.round(summon.maxHp() * ratio))));
					session.sendVitals();
				}
			}
		}

		// Aplica buffs de stats no SummonInstance recalculando a partir dos valores
		// base
		long maxDur = summon.applySkillBuff(sk);
		if (maxDur > 0L) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon), false);
			GameSession.autoAttackScheduler().schedule(() -> {
				if (!summon.isDead()) {
					summon.recalculateStats();
					if (ctx().world() != null) {
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon),
								false);
						ctx().world().player(summon.ownerId()).ifPresent(op -> {
							if (op instanceof GameSession ownerSession) {
								ownerSession.send(new PetStatusUpdate(summon));
								ownerSession.send(new PetInfo(summon, 1));
							}
						});
					}
				}
			}, maxDur + 50L, TimeUnit.MILLISECONDS);
		}

		var suHp = StatusUpdate.hp(summon.objectId(), (int) summon.currentHp(), summon.maxHp());
		send(suHp);
		var su = new StatusUpdate(summon.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) summon.currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.MAX_HP, (int) summon.maxHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) summon.currentMp()),
				new StatusUpdate.Attribute(StatusUpdate.MAX_MP, (int) summon.maxMp())));
		send(su);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
		if (ctx().world() != null) {
			ctx().world().player(summon.ownerId()).ifPresent(op -> {
				if (op instanceof GameSession ownerSession) {
					ownerSession.send(new PetStatusUpdate(summon));
					ownerSession.send(new PetInfo(summon, 1));
				}
			});
		}
	}

	/**
	 * Cura/buff recebido (chamado na sessao do alvo; pode ser o proprio
	 * conjurador).
	 */
	public void receivePositiveSkill(SkillTemplate sk, String casterName) {
		receivePositiveSkill(sk, casterName, 0, false, false, 0, false);
	}

	public void receivePositiveSkill(SkillTemplate sk, String casterName, GameSession caster, boolean sps,
			boolean bss) {
		if (active() == null) {
			return;
		}
		// #162: Bloqueia curas/CP/MP externas em alvos petrificados/invulneraveis
		// (Medusa / Celestial Shield)
		if (active().invul() && caster != null && caster != session && !sk.isBuff() && !sk.isDanceOrSong()
				&& !"RESURRECT".equalsIgnoreCase(sk.skillType())) {
			caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		if (sk.id() == 1013 || "MANARECHARGE".equalsIgnoreCase(sk.skillType())) {
			if (caster == session || active().skills().containsKey(1013)) {
				if (caster != null) {
					caster.send(SystemMessage.id(61)); // NOTHING_HAPPENED
				} else {
					send(SystemMessage.id(61));
				}
				return;
			}
		}
		int casterMAtk = 0;
		int casterLevel = 0;
		if (caster != null && caster.active() != null) {
			casterLevel = caster.active().level();
			if (ctx() != null && ctx().characters() != null) {
				var ct = ctx().characters().template(caster.active());
				if (ct != null) {
					casterMAtk = PlayerStats.calculate(caster.active(), ct).mAtk();
				}
			}
		}
		receivePositiveSkill(sk, casterName, casterMAtk, sps, bss, casterLevel, active().skills().containsKey(1013));
	}

	public void receivePositiveSkill(SkillTemplate sk, String casterName, int casterMAtk, boolean sps, boolean bss,
			int casterLevel, boolean targetKnowsRecharge) {
		if (active() == null) {
			return;
		}
		if (active().isDead()) {
			if ("RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_")) {
				handleResurrect(sk, casterName);
			}
			return;
		}
		if (sk.id() == 1013 || "MANARECHARGE".equalsIgnoreCase(sk.skillType())) {
			if (targetKnowsRecharge || active().skills().containsKey(1013)) {
				send(SystemMessage.id(61)); // NOTHING_HAPPENED
				return;
			}
		}
		if (sk.giveCharges() > 0) {
			session.increaseCharges(sk.giveCharges(), sk.maxCharges());
		}
		applySkillEffects(sk, false);
		double power = sk.power();
		switch (sk.skillType()) {
			case "HEAL" -> {
				int mAtk = casterMAtk;
				if (mAtk <= 0 && ctx() != null && ctx().characters() != null) {
					var ct = ctx().characters().template(active());
					if (ct != null) {
						mAtk = PlayerStats.calculate(active(), ct).mAtk();
					}
				}
				double ssMul = bss ? 4.0 : (sps ? 2.0 : 1.0);
				double healAmount = com.lopez.l2j.game.combat.Formulas.calcHealAmount(null, active(), power, mAtk, sps,
						bss);
				healHp(healAmount, false);
			}
			case "HEAL_STATIC" -> healHp(power);
			case "HEAL_PERCENT" -> healHp(active().maxHp() * power / 100.0, false);
			case "MANAHEAL" -> {
				double before = active().currentMp();
				active().currentMp(before + power);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active().currentMp() - before))));
			}
			case "MANARECHARGE" -> {
				int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel()
						: (casterLevel > 0 ? casterLevel : active().level());
				int diff = active().level() - magicLvl;
				double mpAmount = power;
				if (diff > 5) {
					double mul = Math.max(0.2, 1.0 - (diff - 5) * 0.10);
					mpAmount *= mul;
				}
				double before = active().currentMp();
				active().currentMp(before + mpAmount);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active().currentMp() - before))));
			}
			case "MANAHEAL_PERCENT" -> {
				double before = active().currentMp();
				active().currentMp(before + active().maxMp() * power / 100.0);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active().currentMp() - before))));
			}
			case "COMBATPOINTHEAL" -> {
				double before = active().currentCp();
				active().currentCp(before + power);
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active().currentCp() - before))));
			}
			case "COMBATPOINTPERCENTHEAL" -> {
				double before = active().currentCp();
				active().currentCp(before + active().maxCp() * power / 100.0);
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active().currentCp() - before))));
			}
			case "NEGATE", "CANCEL_DEBUFF" -> {
				handleNegateDebuffs(sk);
				if (power > 0) {
					healHp(power);
				}
			}
			case "GIVE_SP", "GIVESP" -> {
				int spAdded = com.lopez.l2j.game.combat.Formulas.calcGiveSpAmount(sk);
				if (spAdded > 0) {
					active().sp(active().sp() + spAdded);
					send(new StatusUpdate(active().objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.SP, active().sp()))));
				}
			}
			case "FAKE_DEATH" -> toggleSkill(sk);
			case "DRESSME" -> {
				if (ctx() != null && ctx().dressMe() != null) {
					if (!ctx().dressMe().applySkin(active(), sk.id())) {
						ctx().dressMe().toggle(active());
					}
				}
			}
			case "FUSION" -> active().effects().addFusionCharge(sk.id(), sk.numCharges() > 0 ? sk.numCharges() : 3);
			default -> {
				if (sk.id() == 1409 || sk.id() == 1018 || sk.id() == 1020 || sk.id() == 1012 || sk.id() == 34) {
					handleNegateDebuffs(sk);
					if (power > 0) {
						healHp(power);
					}
				}
			}
		}
		session.sendVitals();
	}

	public void handleBalanceLife(SkillTemplate sk, List<GameSession> targets) {
		if (targets == null || targets.isEmpty() || active() == null) {
			return;
		}
		double totalCurHp = 0;
		double totalMaxHp = 0;
		List<GameSession> validMembers = new ArrayList<>();
		List<com.lopez.l2j.game.summon.SummonInstance> validSummons = new ArrayList<>();
		for (GameSession s : targets) {
			// #163: Balance Life exclui mortos, petrificados/invul e membros fora de 1000
			// de alcance
			if (s != null && s.active() != null && !s.active().isDead() && !s.active().invul()) {
				double mdx = active().x() - s.active().x();
				double mdy = active().y() - s.active().y();
				if (mdx * mdx + mdy * mdy > 1000.0 * 1000.0) {
					continue;
				}
				totalCurHp += s.active().currentHp();
				totalMaxHp += s.active().maxHp();
				validMembers.add(s);
				var sum = s.active().activeSummon();
				if (sum != null && !sum.isDead()) {
					double sdx = active().x() - sum.x();
					double sdy = active().y() - sum.y();
					if (sdx * sdx + sdy * sdy <= 1000.0 * 1000.0) {
						totalCurHp += sum.currentHp();
						totalMaxHp += sum.maxHp();
						validSummons.add(sum);
					}
				}
			}
		}
		if (totalMaxHp <= 0 || validMembers.isEmpty()) {
			return;
		}
		double ratio = Math.min(1.0, Math.max(0.01, totalCurHp / totalMaxHp));
		for (GameSession s : validMembers) {
			int newHp = (int) Math.round(s.active().maxHp() * ratio);
			newHp = Math.max(1, Math.min(s.active().maxHp(), newHp));
			s.active().currentHp(newHp);
			s.sendVitals();
			var su = StatusUpdate.hp(s.active().objectId(), (int) s.active().currentHp(), s.active().maxHp());
			s.send(su);
			ctx().world().broadcastAround(s, GameWorld.VISIBILITY_RADIUS, su, false);
		}
		for (var sum : validSummons) {
			int newHp = (int) Math.round(sum.maxHp() * ratio);
			newHp = Math.max(1, Math.min(sum.maxHp(), newHp));
			sum.currentHp(newHp);
			var su = StatusUpdate.hp(sum.objectId(), (int) sum.currentHp(), sum.maxHp());
			send(su);
			if (ctx().world() != null) {
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
				ctx().world().player(sum.ownerId()).ifPresent(op -> {
					if (op instanceof GameSession ownerSession) {
						ownerSession.send(new PetStatusUpdate(sum));
						ownerSession.send(new PetInfo(sum, 1));
					}
				});
			}
		}
	}

	public void handleNegateDebuffs(SkillTemplate sk) {
		if (active() == null || active().isDead()) {
			return;
		}
		int id = sk.id();
		String name = sk.name() != null ? sk.name().toLowerCase(java.util.Locale.ROOT) : "";
		if (id == 1409 || name.contains("cleanse")) {
			active().effects().clearDebuffs();
			active().clearControls();
			session.broadcastAppearance();
			session.sendMagicEffectIcons();
			send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Seus efeitos negativos foram removidos!"));
		} else if (id == 1018 || name.contains("purify")) {
			active().effects().removeDebuffsByStackOrName("poison", "bleed", "paralyze");
			active().clearParalyze();
			session.broadcastAppearance();
			session.sendMagicEffectIcons();
		} else if (id == 1020 || name.contains("vitalize")) {
			active().effects().removeDebuffsByStackOrName("poison", "bleed");
			session.sendMagicEffectIcons();
		} else if (id == 1012 || id == 24 || name.contains("poison")) {
			active().effects().removeDebuffsByStackOrName("poison");
			session.sendMagicEffectIcons();
		} else if (id == 34 || name.contains("bleed")) {
			active().effects().removeDebuffsByStackOrName("bleed");
			session.sendMagicEffectIcons();
		} else {
			active().effects().clearDebuffs();
			session.sendMagicEffectIcons();
		}
	}

	public void handleResurrect(SkillTemplate sk, String casterName) {
		if (active() == null || !active().isDead()) {
			return;
		}
		active().sitting(false);
		var t = ctx() != null && ctx().characters() != null ? ctx().characters().template(active()) : null;
		int wit = t != null
				? (int) Math.round(
						PlayerStats.applyStat(active(), "WIT", t.wit() + active().hennaWIT() + active().augWIT()))
				: 20;
		double power = sk.power();
		// #137: Ressurreicao escalada por WIT
		// (Formulas.calculateSkillResurrectRestorePercent)
		double effRestorePct = power > 0
				? com.lopez.l2j.game.combat.Formulas.calculateSkillResurrectRestorePercent(power, wit)
				: 0.0;
		double hpPercent = effRestorePct > 0 ? Math.min(100.0, effRestorePct) : 20.0;
		int revivedHp = Math.max(1, (int) Math.round(active().maxHp() * (hpPercent / 100.0)));
		active().currentHp(revivedHp);
		active().currentCp(0.0);
		if (effRestorePct > 0 && active().level() > 1) {
			long restoredExp = Math.round(active().level() * 120L * (Math.min(100.0, effRestorePct) / 100.0));
			active().addExpAndSp(restoredExp, 0);
		}

		var revive = new Revive(active().objectId());
		send(revive);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, revive, false);

		send(new UserInfo(active(), t));
		session.sendVitals();
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(revivedHp)));
		ctx().characters().save(active(), true);
	}

	public void healHp(double amount) {
		eloHealHp(amount, true);
	}

	public void healHp(double amount, boolean applyGainHpStat) {
		eloHealHp(amount, applyGainHpStat);
	}

	private void eloHealHp(double amount, boolean applyGainHpStat) {
		if (active() != null && applyGainHpStat) {
			amount = PlayerStats.applyStat(active(), "gainHp", amount);
		}
		if (active() != null && active().isOlympiadMode() && ctx() != null && ctx().combat() != null
				&& ctx().combat().olyDamageManager() != null) {
			amount *= ctx().combat().olyDamageManager().getHealMultiplier(active().classId());
		}
		double before = active().currentHp();
		active().currentHp(before + amount);
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED,
				new SystemMessage.Number((int) (active().currentHp() - before))));
	}

	/**
	 * Aplica os {@code <effect>} do skill no proprio jogador desta sessao (buffs,
	 * HoT, DoT, toggles).
	 */
	public void applySkillEffects(SkillTemplate sk, boolean toggle) {
		applySkillEffects(sk, toggle, 0, false);
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs) {
		applySkillEffects(sk, toggle, remainingMs, false);
	}

	private static boolean isSpecialStateEffectName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.equals("phoenixbless") || lower.equals("salvation")
				|| lower.equals("noblessebless") || lower.equals("charmofcourage")
				|| lower.equals("charmofluck") || lower.equals("luckycharm")
				|| lower.equals("silentmove") || lower.equals("chameleonrest")
				|| lower.equals("fakedeath") || lower.equals("relax")
				|| lower.equals("immobilebuff") || lower.equals("immobilepetbuff")
				|| lower.equals("immobileuntilattacked") || lower.equals("blockbuff")
				|| lower.equals("blockdebuff") || lower.equals("chanceskilltrigger")
				|| lower.equals("protectionblessing") || lower.equals("bestowskill");
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs, boolean forceDebuff) {
		boolean any = false;
		PlayerCharacter owner = active();
		if (owner == null) {
			return;
		}
		var ct = ctx() != null && ctx().characters() != null ? ctx().characters().template(owner) : null;
		// #135: SkillMastery dobra a duracao de buffs normais
		boolean doubleBuffDuration = !toggle && !forceDebuff && !sk.isDebuff() && !sk.isOffensive()
				&& remainingMs <= 0 && !sk.isPotion()
				&& com.lopez.l2j.game.combat.Formulas.calcSkillMastery(owner, ct, owner.isMage());

		// #175: Registra ChanceSkillTrigger se a skill possuir triggeredId configurado
		if (sk.triggeredId() > 0) {
			int actChance = (int) Math.round(sk.effectPower() > 0 ? sk.effectPower() : 30.0);
			owner.effects().registerChanceTrigger(
					sk.id(), sk.triggeredId(), Math.max(1, sk.triggeredLevel()), sk.chanceType(), actChance);
		}

		for (var e : sk.effects()) {
			String name = e.name();
			if (name.equalsIgnoreCase("StunSelf")) {
				long selfUntil = System.currentTimeMillis() + Math.max(1000L, e.durationMs());
				applyControlEffect(sk.id(), sk.level(), "stun", selfUntil);
				any = true;
				continue;
			}
			if (name.equalsIgnoreCase("BigHead") || name.equalsIgnoreCase("Grow")) {
				long until = System.currentTimeMillis() + Math.max(1000L, e.durationMs());
				applyControlEffect(sk.id(), sk.level(), name.toLowerCase(Locale.ROOT), until);
				any = true;
				continue;
			}
			if (name.equalsIgnoreCase("Fusion") || name.equalsIgnoreCase("Seed")) {
				owner.effects().addFusionCharge(sk.id(), sk.numCharges() > 0 ? sk.numCharges() : 3);
				any = true;
				continue;
			}
			if (name.equalsIgnoreCase("BestowSkill") && sk.triggeredId() > 0) {
				owner.effects().addBestowSkill(owner, sk.id(), sk.triggeredId(), Math.max(1, sk.triggeredLevel()));
				any = true;
			}
			if (name.equalsIgnoreCase("HealOverTime") || name.equalsIgnoreCase("ManaHealOverTime")
					|| name.equalsIgnoreCase("CombatPointHealOverTime")) {
				startSkillHot(sk, e);
				any = true;
				continue;
			}
			if (!toggle && (name.equalsIgnoreCase("DamOverTime") || name.equalsIgnoreCase("ManaDamOverTime"))) {
				startSkillDot(sk, e);
				any = true;
				continue;
			}
			boolean isDebuff = forceDebuff
					|| (!toggle && (sk.isDebuff() || sk.isOffensive() || name.equalsIgnoreCase("Debuff")));
			boolean isInvincible = name.equalsIgnoreCase("Invincible");
			boolean isSpecialState = isSpecialStateEffectName(name);
			boolean isBuffOrDebuff = toggle || name.equalsIgnoreCase("Buff") || name.equalsIgnoreCase("Debuff")
					|| sk.isBuff() || isDebuff || isInvincible || isSpecialState;
			if (e.funcs().isEmpty() && !isBuffOrDebuff) {
				continue;
			}
			String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + name
					: e.stackType();
			int stackOrder = PlayerEffects.resolveStackOrder(e.stackOrder(), sk.level());
			long duration = remainingMs > 0 ? remainingMs : e.durationMs();
			if (duration <= 0 && !toggle) {
				duration = 30_000L;
			}
			if (!isDebuff && remainingMs <= 0 && Config.ENABLE_MODIFY_SKILL_DURATION
					&& Config.SKILL_DURATION_LIST != null
					&& Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
				duration = Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
			}
			if (doubleBuffDuration && !isDebuff && !toggle) {
				duration *= 2L;
			}
			long end = toggle ? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: System.currentTimeMillis() + duration;
			var buff = ActiveBuff.ofSkill(
					sk.id(), sk.level(), stack, stackOrder, end, e.funcs(), isDebuff, 0, name, e.val(), e.period());
			if (!owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner))) {
				continue;
			}
			if (isInvincible) {
				owner.invul(true);
			}
			any = true;
			boolean isPeriodicToggle = toggle && (name.equalsIgnoreCase("DamOverTime")
					|| name.equalsIgnoreCase("ManaDamOverTime")
					|| name.equalsIgnoreCase("MpConsumePerLevel")
					|| name.equalsIgnoreCase("Relax")
					|| name.equalsIgnoreCase("SilentMove")
					|| name.equalsIgnoreCase("ChameleonRest")
					|| name.equalsIgnoreCase("FakeDeath"))
					&& (e.val() > 0 || sk.id() == 60 || sk.id() == 221 || sk.id() == 226 || sk.id() == 296);
			if (isPeriodicToggle) {
				startToggleDrainTask(sk, e, owner);
			} else if (!toggle) {
				final long finalDuration = duration;
				GameSession.autoAttackScheduler().schedule(() -> {
					if (owner.effects().remove(buff) && active() == owner && session.inWorld()) {
						if (isInvincible) {
							boolean stillInvul = false;
							for (var b : owner.effects().active()) {
								if (b.stackType() != null && b.stackType().equalsIgnoreCase(stack)) {
									stillInvul = true;
									break;
								}
							}
							if (!stillInvul) {
								owner.invul(false);
							}
						}
						if (name.equalsIgnoreCase("BestowSkill")) {
							owner.effects().removeBestowSkill(owner, sk.id());
						}
						send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
								new SystemMessage.SkillName(sk.id(), sk.level())));
						session.refreshBuffs();
						session.saveBuffs();
					}
				}, finalDuration, TimeUnit.MILLISECONDS);
			}
		}
		if (any) {
			send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(sk.id(), sk.level())));
			session.refreshBuffs();
			session.saveBuffs();
		}
	}

	private void startToggleDrainTask(SkillTemplate sk, SkillTemplate.EffectTemplate e, PlayerCharacter owner) {
		String taskKey = "toggle_" + sk.id();
		var previous = session.hotTasks().remove(taskKey);
		if (previous != null) {
			previous.cancel(false);
		}
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		boolean hpDrain = kind.equals("damovertime");
		boolean isRelax = sk.id() == 226 || kind.equals("relax");
		boolean isFakeDeath = sk.id() == 60 || kind.equals("fakedeath");
		long period = (e.period() > 0 ? e.period() : 3) * 1000L;
		double baseDrain = e.val() > 0 ? e.val() : (isFakeDeath ? 10.0 : 5.0);
		double drainVal = kind.equals("mpconsumeperlevel") ? baseDrain * Math.max(1, owner.level()) : baseDrain;
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (active() != owner || !session.inWorld() || owner.isDead() || !owner.effects().hasSkill(sk.id())) {
				session.stopHot(taskKey, self.get());
				return;
			}
			// #169: Relax cancela se o jogador levantar; nao drena MP se HP ja estiver
			// cheio
			if (isRelax) {
				if (!owner.isSitting()) {
					owner.effects().removeSkill(sk.id());
					session.stopHot(taskKey, self.get());
					send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
							new SystemMessage.SkillName(sk.id(), sk.level())));
					session.refreshBuffs();
					session.saveBuffs();
					return;
				}
				if (owner.currentHp() >= owner.maxHp()) {
					return;
				}
			}
			if (hpDrain) {
				if (owner.currentHp() <= drainVal + 1.0) {
					owner.effects().removeSkill(sk.id());
					session.stopHot(taskKey, self.get());
					send(SystemMessage.id(SystemMessage.NOT_ENOUGH_HP));
					send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
							new SystemMessage.SkillName(sk.id(), sk.level())));
					session.refreshBuffs();
					session.saveBuffs();
					session.sendVitals();
					return;
				}
				owner.currentHp(owner.currentHp() - drainVal);
			} else {
				if (owner.currentMp() < drainVal) {
					owner.effects().removeSkill(sk.id());
					session.stopHot(taskKey, self.get());
					if (isFakeDeath) {
						var stopFd = new GameServerPacket.ChangeWaitType(
								owner.objectId(), 3,
								owner.x(), owner.y(), owner.z());
						send(stopFd);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stopFd, false);
					}
					send(SystemMessage.id(SystemMessage.SKILL_REMOVED_DUE_LACK_MP));
					send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
							new SystemMessage.SkillName(sk.id(), sk.level())));
					session.refreshBuffs();
					session.saveBuffs();
					session.sendVitals();
					return;
				}
				owner.currentMp(Math.max(0, owner.currentMp() - drainVal));
				if (isRelax) {
					owner.currentHp(Math.min(owner.maxHp(), owner.currentHp() + (5.0 * Math.max(1, sk.level()))));
				}
			}
			session.sendVitals();
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		session.hotTasks().put(taskKey, task);
	}

	public void startSkillDot(SkillTemplate sk, SkillTemplate.EffectTemplate e) {
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		String stackType = e.stackType() != null ? e.stackType().toLowerCase(java.util.Locale.ROOT) : "";
		String stack = "skilldot_" + (!stackType.isEmpty() && !stackType.equalsIgnoreCase("none") ? stackType
				: sk.id() + "_" + kind);
		int stackOrder = e.stackOrder() > 0 ? (int) Math.round(e.stackOrder()) : sk.level();
		var previous = session.hotTasks().remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		int abnormalMask = 0;
		if (kind.contains("poison") || stackType.contains("poison") || sk.skillType().equalsIgnoreCase("POISON")) {
			abnormalMask = 0x0002;
		} else if (kind.contains("bleed") || stackType.contains("bleed") || sk.skillType().equalsIgnoreCase("BLEED")) {
			abnormalMask = 0x0001;
		}
		if (abnormalMask != 0) {
			active().startAbnormalEffect(abnormalMask);
			session.broadcastAppearance();
		}
		final int dotMask = abnormalMask;
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(sk.id(), sk.level())));

		PlayerCharacter owner = active();
		int[] remaining = { Math.max(1, e.count()) };
		long period = Math.max(1, e.period()) * 1000L;
		long totalDuration = (long) remaining[0] * period;
		long until = System.currentTimeMillis() + totalDuration;
		var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, stackOrder, until, e.funcs(), true);
		owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner));
		session.refreshBuffs();

		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (active() != owner || !session.inWorld() || owner.isDead() || remaining[0] <= 0
					|| (ctx() != null && ctx().zones() != null
							&& ctx().zones().isInsidePeace(owner.x(), owner.y(), owner.z()))) {
				if (dotMask != 0 && active() != null) {
					active().stopAbnormalEffect(dotMask);
					session.broadcastAppearance();
				}
				if (owner != null) {
					owner.effects().remove(buff);
					if (active() == owner) {
						session.refreshBuffs();
					}
				}
				session.stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			if (kind.equals("manadamovertime")) {
				owner.currentMp(Math.max(0, owner.currentMp() - e.val()));
				session.sendVitals();
			} else {
				double curHp = owner.currentHp();
				double newHp = Math.max(1, curHp - e.val());
				owner.currentHp(newHp);
				session.sendVitals();
			}
			if (remaining[0] <= 0) {
				if (dotMask != 0 && active() != null) {
					active().stopAbnormalEffect(dotMask);
					session.broadcastAppearance();
				}
				if (owner != null) {
					owner.effects().remove(buff);
					if (active() == owner) {
						session.refreshBuffs();
					}
				}
			}
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		session.hotTasks().put(stack, task);
	}

	/**
	 * HealOverTime/ManaHealOverTime do skill: {@code val} por tick a cada
	 * {@code period} s, {@code count} vezes.
	 */
	public void startSkillHot(SkillTemplate sk, SkillTemplate.EffectTemplate e) {
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		String stack = "skillhot_" + kind;
		var previous = session.hotTasks().remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		PlayerCharacter owner = active();
		int[] remaining = { Math.max(1, e.count()) };
		long period = Math.max(1, e.period()) * 1000L;
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (active() != owner || !session.inWorld() || owner.isDead() || remaining[0] <= 0) {
				session.stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			switch (kind) {
				case "manahealovertime" -> owner.currentMp(owner.currentMp() + e.val());
				case "combatpointhealovertime" -> owner.currentCp(owner.currentCp() + e.val());
				default -> owner.currentHp(owner.currentHp() + e.val());
			}
			session.sendVitals();
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		session.hotTasks().put(stack, task);
	}

	/**
	 * Toggle (OP_TOGGLE): liga aplicando os efeitos permanentes, desliga removendo.
	 */
	public void toggleSkill(SkillTemplate sk) {
		if (active().effects().hasSkill(sk.id())) {
			var toggleTask = session.hotTasks().remove("toggle_" + sk.id());
			if (toggleTask != null) {
				toggleTask.cancel(false);
			}
			active().effects().removeSkill(sk.id());
			if (sk.id() == 7029) {
				active().gmSpeed(0);
			} else if (sk.id() == 60) { // #169: Fake Death desligado -> WAIT_STOP_FAKEDEATH
				var stopFd = new GameServerPacket.ChangeWaitType(
						active().objectId(), 3,
						active().x(), active().y(), active().z());
				send(stopFd);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stopFd, false);
			}
			send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			session.refreshBuffs();
			session.saveBuffs();
			return;
		}
		int toggleMp = getEffectiveMpConsume(sk, sk.mpConsume() + sk.mpInitialConsume());
		if (active().currentMp() < toggleMp) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active().currentMp(Math.max(0, active().currentMp() - toggleMp));
		var msu = new MagicSkillUse(active().objectId(), active().objectId(), sk.id(), sk.level(), 0, 0,
				active().x(), active().y(), active().z(), active().x(), active().y(), active().z());
		send(msu);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
		if (sk.id() == 60) { // #169: Fake Death ligado -> WAIT_START_FAKEDEATH + para aggro dos monstros
			session.stopAutoAttack();
			cancelCast();
			var startFd = new GameServerPacket.ChangeWaitType(
					active().objectId(), 2,
					active().x(), active().y(), active().z());
			send(startFd);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, startFd, false);
			if (ctx() != null && ctx().npcAi() != null) {
				ctx().npcAi().stopCombatForPlayer(active().objectId());
			}
		} else if (sk.id() == 226 && !active().isSitting()) { // #169: Relax senta o personagem automaticamente
			active().sitting(true);
			var sitPkt = new GameServerPacket.ChangeWaitType(
					active().objectId(), 0,
					active().x(), active().y(), active().z());
			send(sitPkt);
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, sitPkt, false);
		}
		applySkillEffects(sk, true);
		if (sk.id() == 7029) {
			active().gmSpeed(sk.level());
		}
		session.sendVitals();
	}

	/**
	 * Max HP/MP/CP = formula da classe no nivel + passivas/buffs (maxHp, maxMp,
	 * maxCp).
	 */
	public void recalcMaxVitals(CharTemplate t) {
		if (t == null && ctx().characters() != null && active() != null) {
			t = ctx().characters().template(active());
		}
		if (t == null || active() == null) {
			return;
		}
		int lvl = active().level();
		int mpBonus = (active().inventory() != null)
				? active().inventory().equipped().stream().mapToInt(i -> i.template().mpBonus()).sum()
				: 0;
		int baseCon = t.con() + active().hennaCON() + active().augCON();
		int baseMen = t.men() + active().hennaMEN() + active().augMEN();
		int effectiveCon = Math.max(1, (int) Math.round(PlayerStats.applyStat(active(), "CON", baseCon)));
		int effectiveMen = Math.max(1, (int) Math.round(PlayerStats.applyStat(active(), "MEN", baseMen)));
		double conRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon)
				/ com.lopez.l2j.game.template.BaseStatsTable.conBonus(t.con());
		double menRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(effectiveMen)
				/ com.lopez.l2j.game.template.BaseStatsTable.menBonus(t.men());
		active().maxHp(Math.max(1,
				(int) PlayerStats.applyStat(active(), "maxHp", (int) Math.round(t.calculateMaxHp(lvl) * conRatio))));
		active().maxMp(Math.max(1, (int) PlayerStats.applyStat(active(), "maxMp",
				(int) Math.round((t.calculateMaxMp(lvl) + mpBonus) * menRatio))));
		int baseCp = (int) Math.round(t.calculateMaxCp(lvl) * conRatio);
		if (ctx().sevenSigns() != null && ctx().sevenSigns().isSealValidationPeriod()) {
			int strifeOwner = ctx().sevenSigns().strifeOwner();
			if (strifeOwner != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
				int playerCabal = ctx().sevenSigns().getPlayerCabal(active().objectId());
				if (playerCabal == strifeOwner) {
					baseCp = (int) Math.round(baseCp * 1.10);
				} else if (playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					baseCp = (int) Math.round(baseCp * 0.90);
				}
			}
		}
		active().maxCp(Math.max(1, (int) PlayerStats.applyStat(active(), "maxCp", baseCp)));
		active().currentHp(Math.min(active().maxHp(), active().currentHp()));
		active().currentMp(Math.min(active().maxMp(), active().currentMp()));
		active().currentCp(Math.min(active().maxCp(), active().currentCp()));
	}

}
