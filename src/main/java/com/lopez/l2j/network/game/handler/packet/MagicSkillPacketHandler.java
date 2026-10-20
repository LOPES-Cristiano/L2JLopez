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
 * Handler modular para conjuracao de magias, execucao de habilidades, calculo de dano magico,
 * aplicacao de efeitos (buffs, debuffs, HoT, DoT), spoil, cura, ressurreicao e combate magico.
 */
public class MagicSkillPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(MagicSkillPacketHandler.class);

	private final GameSession session;
	private final Map<Integer, Long> skillReuse = new ConcurrentHashMap<>();

	public MagicSkillPacketHandler(GameSession session) {
		this.session = session;
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
		if (p == null) return;
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
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Itens insuficientes para usar esta habilidade."));
				send(new ActionFailed());
				return;
			}
		}
		if (active().currentMp() < sk.mpConsume() + sk.mpInitialConsume()) {
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
		boolean isSweep = "TARGET_CORPSE_MOB".equalsIgnoreCase(sk.target())
				|| "SWEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 42;
		boolean isResurrect = !isSweep && ("RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_"));
		boolean isUnlock = "UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "TARGET_UNLOCKABLE".equalsIgnoreCase(sk.target());
		boolean isSummonTargetOnly = "TARGET_SUMMON".equalsIgnoreCase(sk.target())
				|| "TARGET_PET".equalsIgnoreCase(sk.target());
		boolean isHealSkill = "HEAL".equalsIgnoreCase(sk.skillType())
				|| "HEAL_PERCENT".equalsIgnoreCase(sk.skillType())
				|| "HEAL_STATIC".equalsIgnoreCase(sk.skillType());

		if (isSweep) {
			npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && n.isDead()).orElse(null);
			if (npcTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
			if (!npcTarget.isSweepable()) {
				send(SystemMessage.id(SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
				send(new ActionFailed());
				return;
			}
			if (npcTarget.spoilerPlayerId() != 0 && npcTarget.spoilerPlayerId() != active().objectId()) {
				boolean inParty = session.party() != null && session.party().contains(npcTarget.spoilerPlayerId());
				if (!inParty) {
					send(SystemMessage.id(SystemMessage.SWEEP_NOT_ALLOWED));
					send(new ActionFailed());
					return;
				}
			}
		} else if (isUnlock) {
			npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> !n.isDead() && GameSession.isChestNpc(n)).orElse(null);
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
		} else if (sk.isOffensive()) {
			if (!sk.isAreaAroundSelf()) {
				npcTarget = ctx().world().npc(session.targetObjectId()).filter(n -> n.isAttackable() && !n.isDead()).orElse(null);
				if (npcTarget == null) {
					var other = ctx().world().player(session.targetObjectId()).orElse(null);
					if (other instanceof GameSession gs && gs.active() != null && !gs.active().isDead() && gs != session) {
						playerTarget = gs;
					} else {
						send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
						send(new ActionFailed());
						return;
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
		} else if (!sk.isSelfTargeted() && session.targetObjectId() != 0 && session.targetObjectId() != active().objectId()) {
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

		// Verificacao de Zona de Paz para habilidades ofensivas (aplica-se a PvP contra outros jogadores, nao a monstros)
		if (sk.isOffensive() && ctx().zones() != null && playerTarget != null && playerTarget != session) {
			if (ctx().zones().isInsidePeace(active().x(), active().y(), active().z())
					|| ctx().zones().isInsidePeace(playerTarget.x(), playerTarget.y(), playerTarget.z())) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode usar habilidades ofensivas em zona de paz."));
				send(new ActionFailed());
				return;
			}
		}

		// Bloqueia suporte (cura/buff) a jogadores flaggados ou PK a partir de zona de paz
		if (!sk.isOffensive() && ctx().zones() != null && playerTarget != null && playerTarget != session) {
			if (ctx().zones().isInsidePeace(active().x(), active().y(), active().z())
					&& (playerTarget.active().pvpFlag() > 0 || playerTarget.active().karma() > 0)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode dar suporte a jogadores em combate a partir de uma zona de paz."));
				send(new ActionFailed());
				return;
			}
		}

		// Validacao de condicoes da skill levando o alvo em consideracao (ex: <target undead="true" />)
		Object resolvedTarget = npcTarget != null ? npcTarget
				: (summonTarget != null ? summonTarget : (playerTarget != null ? playerTarget.active() : null));
		if (sk.castCondition() != null && !sk.castCondition().test(active(), resolvedTarget)) {
			if (sk.condMsg() != null && !sk.condMsg().isBlank()) {
				try {
					int msgId = Integer.parseInt(sk.condMsg().trim());
					send(SystemMessage.id(msgId));
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", sk.condMsg()));
				}
			} else {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
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
		if (sk.castRange() > 0 && (npcTarget != null || doorTarget != null || summonTarget != null || (playerTarget != null && playerTarget != session))) {
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
				var move = new MoveToPawn(active().objectId(), targetId, sk.castRange(), active().x(), active().y(),
						active().z());
				send(move);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
				int run = Math.max(50, PlayerStats.calculate(active(), ctx().characters().template(active())).runSpeed());
				long travelMs = (long) ((dist - sk.castRange()) * 1000 / run) + 200;
				double angle = Math.atan2(active().y() - ty, active().x() - tx);
				int stopDist = Math.max(20, sk.castRange() - 30);
				int stopX = (int) (tx + stopDist * Math.cos(angle));
				int stopY = (int) (ty + stopDist * Math.sin(angle));
				int stopZ = tz;
				PlayerCharacter owner = active();
				GameSession.autoAttackScheduler().schedule(() -> {
					if (active() == owner) {
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
		var t = ctx().characters().template(active());
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
		int reuse = (int) (sk.reuseDelay() * speedFactor);
		double mReuse = PlayerStats.applyStat(active(), "mReuse", 1.0);
		if (mReuse > 1.0) {
			reuse = (int) (reuse / mReuse);
		}
		active().currentMp(active().currentMp() - sk.mpInitialConsume());
		if (reuse > 0) {
			skillReuse.put(sk.id(), now + Math.max(reuse, hitTime));
		}
		boolean isSleepOrTrance = "SLEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 1069 || sk.id() == 1394 || sk.id() == 1071 || sk.id() == 1201;
		if (isSleepOrTrance) {
			session.stopAutoAttack();
			session.autoAttacking(false);
		}
		boolean resumeAttack = !isSleepOrTrance && session.isAutoAttacking() && (npcTarget != null || playerTarget != null) && sk.nextActionAttack();
		session.autoAttacking(false); // o auto-ataque para durante o cast

		// Para a movimentacao do personagem para conjurar a habilidade ("da uma paradinha")
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
	public void finishCast(SkillTemplate sk, NpcInstance mainNpc, DoorInstance doorTarget, GameSession targetPlayer, boolean resumeAttack,
			boolean sps, boolean bss) {
		var party = session.party();
		if (active().currentMp() < sk.mpConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active().currentMp(active().currentMp() - sk.mpConsume());
		if (sk.hpConsume() > 0) {
			active().currentHp(active().currentHp() - sk.hpConsume());
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			if (active().inventory().destroyItemByItemId(sk.itemConsumeId(), sk.itemConsumeCount())) {
				send(ItemList.of(active().inventory().items(), false));
				send(SystemMessage.of(SystemMessage.S1_DISAPPEARED, new SystemMessage.ItemName(sk.itemConsumeId())));
			}
		}
		var t = ctx().characters().template(active());

		if (sk.id() == 1312) { // Fishing
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
		if (sk.id() == 1313) { // Pumping
			if (ctx().fishing() != null) {
				ctx().fishing().handlePumping(active(), sk.level(), (int) sk.power(), session::send);
			}
			return;
		}
		if (sk.id() == 1314) { // Reeling
			if (ctx().fishing() != null) {
				ctx().fishing().handleReeling(active(), sk.level(), (int) sk.power(), session::send);
			}
			return;
		}

		if ("TARGET_CORPSE_MOB".equalsIgnoreCase(sk.target()) || "SWEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 42) {
			if (mainNpc != null && ctx().drops() != null) {
				boolean swept = ctx().drops().sweep(active(), mainNpc, ctx().inventories(), session::send);
				if (swept) {
					var dieNoGlow = new Die(mainNpc.objectId(), false, false, false, false);
					send(dieNoGlow);
					if (ctx().world() != null) {
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, dieNoGlow, false);
					}
					if (ctx().npcAi() != null) {
						ctx().npcAi().scheduleQuickDecay(mainNpc, 3000);
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
				handleUnlockDoor(doorTarget, sk);
			} else if (mainNpc != null) {
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
		if (skillRegistry != null && skillRegistry.dispatchSpecialSkill(session, sk, mainNpc, doorTarget, targetPlayer, false, sps, bss)) {
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

			npcTargets.removeIf(n -> !n.isAttackable() || n.isDead() || (ctx().combat() != null && !ctx().combat().canSeeTarget(active(), n)));
			playerTargets.removeIf(p -> p.active() == null || p.active().isDead() || (ctx().combat() != null && !ctx().combat().canSeeTarget(active(), p.active())));

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
				} else if (targetPlayer != null && targetPlayer.active() != null && !targetPlayer.active().isDead() && session.targetObjectId() == targetPlayer.objectId()) {
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
		} else if (!sk.isSelfTargeted() && session.targetObjectId() != 0 && session.targetObjectId() != active().objectId()) {
			if (active().activeSummon() != null && !active().activeSummon().isDead()
					&& active().activeSummon().objectId() == session.targetObjectId()) {
				targetSummon = active().activeSummon();
			} else if (ctx().world() != null) {
				targetSummon = ctx().world().summon(session.targetObjectId()).filter(s -> !s.isDead()).orElse(null);
			}
		}
		if (targetSummon != null) {
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

		// Cura usada em monstro (dano sagrado em Undead ou cura de HP + aggro em monstros normais)
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
				if (mainNpc.isInCombat() && mainNpc.targetPlayerId() != 0 && mainNpc.targetPlayerId() != active().objectId()) {
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
			for (GameSession target : targets) {
				if (target != session && target.active() != null) {
					if (target.active().pvpFlag() > 0 || target.active().karma() > 0) {
						updatePvPFlag(target.active().pvpFlag() > 0);
					}
				}
				target.magicSkillHandler().receivePositiveSkill(sk, active().name(), session, sps, bss);
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
		int chance = (int) Math.max(10, Math.min(100, sk.power() > 0 ? sk.power() : 50));
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
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
		int chance = 50;
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
			int baseChance = (int) (sk.power() > 0 ? sk.power() : 50);
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
			applyControlEffect(com.lopez.l2j.game.combat.CombatService.RAID_CURSE_SILENCE, 1, "silence", System.currentTimeMillis() + 120_000L);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi atingido pela Maldicao de Raid Boss (Raid Curse)!"));
			return;
		}
		CombatService.HitResult hit = null;
		double effPower = sk.power();
		if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && effPower > 0) {
			hit = combat.skillPhysicalNpc(active(), t, npc, effPower, soulshot, sk.skillType().equals("BLOW"),
					sk.isChargedDam());
		} else if (sk.isMagicDamage() && effPower > 0) {
			hit = combat.skillMagicNpc(active(), t, npc, effPower, sk.magicLevel(), sps, bss);
			if (sk.skillType().equals("DRAIN") && hit.damage() > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(hit.damage() * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamNpc(active(), t, npc, effPower, sk.magicLevel(), sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, npc.currentMp() - mpDam);
				npc.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1, new SystemMessage.Number(mpDam)));
				var su = StatusUpdate.mp(npc.objectId(), (int) npc.currentMp(), npc.template().maxMp());
				send(su);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		// Spoil (Skill 254)
		if (sk.id() == 254 || "SPOIL".equalsIgnoreCase(sk.skillType())) {
			handleSpoil(npc, sk);
		}

		// Debuffs de controle (Stun/Sleep/Paralyze/Root/Silence/Fear), DoTs e debuffs de stats nos monstros
		if (!npc.isDead()) {
			boolean damageSkill = hit != null;
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical");
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
							abnormalMask = 0x0040;
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
							abnormalMask = 0x0400;
						}
						case "paralyze" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0010;
						}
						case "petrification", "medusa" -> {
							npc.disable(until, false);
							npc.invul(true);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0800;
							GameSession.autoAttackScheduler().schedule(() -> npc.invul(false), Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
						}
						case "silence", "mute" -> {
							npc.mute(until);
							npc.abortCast();
							abnormalMask = 0x0020;
						}
						case "physicalmute" -> {
							npc.physicalMute(until);
							abnormalMask = 0x0020;
						}
						case "silencemagicphysical" -> {
							npc.mute(until);
							npc.physicalMute(until);
							npc.abortCast();
							abnormalMask = 0x0020;
						}
						case "fear" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0004;
						}
					}
					if (abnormalMask != 0) {
						npc.startAbnormalEffect(abnormalMask);
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
						final int fMask = abnormalMask;
						GameSession.autoAttackScheduler().schedule(() -> {
							if (npc != null) {
								npc.stopAbnormalEffect(fMask);
								ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
							}
						}, Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
					}
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType()) || "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(active().objectId());
					if (ctx().npcAi() != null) {
						ctx().npcAi().startCombat(npc, active().objectId());
					}
				} else if (name.equals("removetarget") || "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType()) || "SWITCH".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(0);
					npc.abortAttack();
					npc.abortCast();
					if (sk.id() == 358) { // Bluff
						npc.heading(active().heading());
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
								new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()), false);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(), npc, sps,
							bss)) {
						startNpcDot(npc, sk, e, t);
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(), npc, sps,
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
		// Nivel superior decai progressivamente de forma exponencial suave (pow(1.3, diff) * 100 em 10000)
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
		if (combat == null || targetSession == null || targetSession.active() == null || targetSession.active().isDead()) {
			return;
		}
		var targetActive = targetSession.active();
		var targetTemplate = ctx().characters().template(targetActive);
		int damage = 0;
		double effPower = sk.power();
		if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active().currentHp() / Math.max(1, active().maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && effPower > 0) {
			damage = combat.skillPhysicalPlayer(active(), t, targetActive, targetTemplate, effPower, soulshot,
					sk.skillType().equals("BLOW"), sk.isChargedDam());
		} else if (sk.isMagicDamage() && effPower > 0) {
			damage = combat.skillMagicPlayer(active(), t, targetActive, targetTemplate, effPower, sk.magicLevel(), sps, bss);
			if (damage <= 1 && targetActive.level() - (sk.magicLevel() > 0 ? Math.min(sk.magicLevel(), active().level()) : active().level()) > 9) {
				send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
						new SystemMessage.Text(targetActive.name()),
						new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			if (sk.skillType().equals("DRAIN") && damage > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(damage * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamPlayer(active(), t, targetActive, targetTemplate, effPower, sk.magicLevel(), sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, targetActive.currentMp() - mpDam);
				targetActive.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1, new SystemMessage.Number(mpDam)));
				targetSession.send(SystemMessage.of(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1, new SystemMessage.Text(active().name()),
						new SystemMessage.Number(mpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.mp(targetActive.objectId(), (int) targetActive.currentMp(), targetActive.maxMp());
				send(su);
				ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		} else if (sk.isCpDamage()) {
			int cpDam = (int) Math.round(targetActive.currentCp() * Math.max(0.0, 1.0 - sk.power()));
			if (cpDam > 0) {
				targetActive.currentCp(Math.max(0, targetActive.currentCp() - cpDam));
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(cpDam)));
				targetSession.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active().name()),
						new SystemMessage.Number(cpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.cp(targetActive.objectId(), (int) targetActive.currentCp(), targetActive.maxCp());
				send(su);
				ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		if (damage > 0) {
			var result = combat.applyDamagePlayer(targetActive, damage);
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(damage)));
			targetSession.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active().name()),
						new SystemMessage.Number(damage)));
			targetSession.sendVitals();

			var su = StatusUpdate.hp(targetActive.objectId(), (int) targetActive.currentHp(), targetActive.maxHp());
			send(su);
			ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);

			if (result.isDead()) {
				targetSession.handlePlayerDeath(active());
				return;
			}
		}

		// Debuffs & DoTs em jogadores
		if (!targetActive.isDead()) {
			if ("CANCEL".equalsIgnoreCase(sk.skillType()) || sk.id() == 1056) {
				var cancelRestore = com.lopez.l2j.game.service.CancelRestoreService.getInstance();
				if (cancelRestore != null) {
					cancelRestore.applyCancelWithRestore(targetSession, 5);
				} else {
					var activeBuffs = new java.util.ArrayList<>(targetActive.effects().active());
					if (!activeBuffs.isEmpty()) {
						java.util.Collections.shuffle(activeBuffs);
						int count = Math.min(5, activeBuffs.size());
						for (int i = 0; i < count; i++) {
							targetActive.effects().remove(activeBuffs.get(i));
						}
						targetSession.sendMagicEffectIcons();
					}
				}
			}
			if ("WARRIOR_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1344 || sk.id() == 1350) {
				var activeBuffs = new ArrayList<>(targetActive.effects().active());
				boolean removed = false;
				for (var b : activeBuffs) {
					boolean isWarriorBuff = b.skillId() == 1086 || b.skillId() == 1204 || b.skillId() == 264 || b.skillId() == 304;
					if (!isWarriorBuff && b.stackType() != null) {
						String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
						isWarriorBuff = st.contains("speed_up") || st.contains("attack_time_down") || st.contains("run_spd");
					}
					if (!isWarriorBuff && b.funcs() != null) {
						isWarriorBuff = b.funcs().stream().anyMatch(f -> "pAtkSpd".equalsIgnoreCase(f.stat()) || "runSpd".equalsIgnoreCase(f.stat()));
					}
					if (isWarriorBuff) {
						targetActive.effects().remove(b);
						removed = true;
					}
				}
				if (removed) {
					targetSession.sendMagicEffectIcons();
					targetSession.send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Buffs de velocidade fisica foram cancelados!"));
				}
			}
			if ("MAGE_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1345 || sk.id() == 1351) {
				var activeBuffs = new ArrayList<>(targetActive.effects().active());
				boolean removed = false;
				for (var b : activeBuffs) {
					boolean isMageBuff = b.skillId() == 1085 || b.skillId() == 1059;
					if (!isMageBuff && b.stackType() != null) {
						String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
						isMageBuff = st.contains("casting_time_down") || st.contains("ma_up") || st.contains("m_atk");
					}
					if (!isMageBuff && b.funcs() != null) {
						isMageBuff = b.funcs().stream().anyMatch(f -> "mAtkSpd".equalsIgnoreCase(f.stat()) || "mAtk".equalsIgnoreCase(f.stat()));
					}
					if (isMageBuff) {
						targetActive.effects().remove(b);
						removed = true;
					}
				}
				if (removed) {
					targetSession.sendMagicEffectIcons();
					targetSession.send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Buffs de poder magico e conjuracao foram cancelados!"));
				}
			}

			if (sk.effects().isEmpty()) {
				String st = sk.skillType().toLowerCase(java.util.Locale.ROOT);
				boolean control = st.equals("stun") || st.equals("sleep") || st.equals("paralyze")
						|| st.equals("root") || st.equals("mute") || st.equals("fear");
				if (control) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss)) {
						long until = System.currentTimeMillis() + 15000L;
						targetSession.applyControlEffect(sk.id(), sk.level(), st, until);
						targetSession.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
								new SystemMessage.SkillName(sk.id(), sk.level())));
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				}
			}

			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical");

				if (control) {
					if (!combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					targetSession.applyControlEffect(sk.id(), sk.level(), name, until);
					targetSession.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
							new SystemMessage.SkillName(sk.id(), sk.level())));
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType()) || "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					targetSession.forceTarget(active().objectId());
					if (targetSession.isAutoAttacking()) {
						targetSession.schedulePlayerAutoAttack(session);
					}
				} else if (name.equals("removetarget") || "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType()) || "SWITCH".equalsIgnoreCase(sk.skillType())) {
					targetSession.clearTarget();
					targetSession.stopAutoAttack();
					targetSession.cancelCast();
					if (sk.id() == 358) { // Bluff
						targetActive.heading(active().heading());
						var valLoc = new ValidateLocation(targetActive.objectId(), targetActive.x(), targetActive.y(), targetActive.z(), active().heading());
						ctx().world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, valLoc, true);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss)) {
						targetSession.startSkillDot(sk, e);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				} else if (!e.funcs().isEmpty() || sk.isDebuff() || sk.isOffensive() || name.equals("debuff")) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active().level(),
							targetActive, sps, bss)) {
						targetSession.applySkillEffects(sk, false);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
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
				abnormalMask = 0x0040;
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
				abnormalMask = 0x0400;
			}
			case "paralyze" -> {
				active().disable(until, false);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0010;
			}
			case "petrification", "medusa" -> {
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
			case "mute", "silence" -> {
				active().mute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "physicalmute" -> {
				active().physicalMute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "silencemagicphysical" -> {
				active().mute(until);
				active().physicalMute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "fear" -> {
				active().disable(until, false);
				session.stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0004;
			}
		}
		ActiveBuff buff = null;
		if (skillId > 0) {
			String stack = "control_" + skillId + "_" + effectName;
			buff = ActiveBuff.ofSkill(skillId, skillLevel, stack, skillLevel, until, List.of(), true);
			active().effects().put(buff, PlayerEffects.getExtraBuffSlots(active()));
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
				if (finalMask != 0) {
					active().stopAbnormalEffect(finalMask);
					session.broadcastAppearance();
				}
				if (finalBuff != null && active().effects().remove(finalBuff)) {
					session.refreshBuffs();
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

		// Limpa buffs ao morrer (regra retail Interlude):
		// Se LeaveBuffsOnDie = False, preserva os buffs.
		boolean preserveBuffs = !Config.LEAVE_BUFFS_ON_DIE;
		boolean hasNoblesse = active().effects().hasSkill(1323);
		if (preserveBuffs) {
			active().effects().clearDebuffs();
			session.saveBuffs();
		} else if (hasNoblesse) {
			active().effects().clearDebuffs();
			active().effects().removeSkill(1323);
			session.saveBuffs();
		} else {
			active().effects().clear();
			if (ctx() != null && ctx().buffRepository() != null) {
				ctx().buffRepository().deleteBuffs(active().objectId());
			}
		}
		if (preserveBuffs || hasNoblesse) {
			session.sendMagicEffectIcons();
		} else {
			send(new MagicEffectIcons(List.of()));
		}
		var tTemplate = ctx().characters().template(active());
		if (ctx().skillService() != null) {
			recalcMaxVitals(tTemplate);
		}

		if (active().level() >= 10 && !active().skills().containsKey(SkillService.SKILL_LUCKY)) {
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
					var t = ctx().characters().template(active());
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

		if (killer != null && ctx() != null && ctx().world() != null) {
			ctx().world().player(killer.objectId()).ifPresentOrElse(op -> {
				if (op instanceof GameSession killerSession) {
					PlayerCombatHandler.handlePlayerDeath(killerSession, session, ctx(), killer, active());
				} else {
					PlayerCombatHandler.handlePlayerDeath(null, session, ctx(), killer, active());
				}
			}, () -> PlayerCombatHandler.handlePlayerDeath(null, session, ctx(), killer, active()));
		} else if (ctx() != null) {
			com.lopez.l2j.game.zone.FlagZoneService flagZone = ctx().getBean(com.lopez.l2j.game.zone.FlagZoneService.class);
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
					double dist = Math.hypot(member.active().x() - killerChar.x(), member.active().y() - killerChar.y());
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
			if (active() == owner && active().pvpFlag() == 1 && System.currentTimeMillis() >= active().pvpFlagEndTime()) {
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
				send(SystemMessage.id(sk != null && sk.magic() ? SystemMessage.CRITICAL_HIT_MAGIC : SystemMessage.CRITICAL_HIT));
			}
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
		}
		int remainingHp = hit.isDead() ? 0 : Math.max(0, hit.remainingHp());
		var su = StatusUpdate.hp(npc.objectId(), remainingHp, (int) npc.maxHp());
		send(su);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
		if (hit.isDead()) {
			PlayerCombatHandler.handleMonsterDeath(session, ctx(), active(), t, npc, party, hit.expReward(), hit.spReward());
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
			case "MANAHEAL", "MANARECHARGE" -> {
				double mpAmount = power;
				if ("MANARECHARGE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1013) {
					int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active().level();
					int summonLvl = summon.template() != null ? summon.template().level() : active().level();
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
			}
		}

		// Aplica buffs de stats no SummonInstance
		for (var e : sk.effects()) {
			long dur = Math.max(1000L, e.durationMs());
			if (Config.ENABLE_MODIFY_SKILL_DURATION && Config.SKILL_DURATION_LIST != null
					&& Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
				dur = Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
			}
			for (var f : e.funcs()) {
				double val = f.value();
				if (val <= 0) {
					continue;
				}
				switch (f.stat()) {
					case "pDef" -> {
						int prev = summon.pDef();
						summon.pDef((int) Math.round(prev * val));
						GameSession.autoAttackScheduler().schedule(() -> summon.pDef(prev), dur, TimeUnit.MILLISECONDS);
					}
					case "mDef" -> {
						int prev = summon.mDef();
						summon.mDef((int) Math.round(prev * val));
						GameSession.autoAttackScheduler().schedule(() -> summon.mDef(prev), dur, TimeUnit.MILLISECONDS);
					}
					case "pAtk" -> {
						int prev = summon.pAtk();
						summon.pAtk((int) Math.round(prev * val));
						GameSession.autoAttackScheduler().schedule(() -> summon.pAtk(prev), dur, TimeUnit.MILLISECONDS);
					}
					case "mAtk" -> {
						int prev = summon.mAtk();
						summon.mAtk((int) Math.round(prev * val));
						GameSession.autoAttackScheduler().schedule(() -> summon.mAtk(prev), dur, TimeUnit.MILLISECONDS);
					}
					case "runSpd" -> {
						int prev = summon.runSpd();
						summon.runSpd((int) Math.round(prev * val));
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon), false);
						GameSession.autoAttackScheduler().schedule(() -> {
							summon.runSpd(prev);
							ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon), false);
						}, dur, TimeUnit.MILLISECONDS);
					}
					case "pAtkSpd" -> {
						int prev = summon.pAtkSpd();
						summon.pAtkSpd((int) Math.round(prev * val));
						ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon), false);
						GameSession.autoAttackScheduler().schedule(() -> {
							summon.pAtkSpd(prev);
							ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new SummonNpcInfo(summon), false);
						}, dur, TimeUnit.MILLISECONDS);
					}
					case "mAtkSpd" -> {
						int prev = summon.mAtkSpd();
						summon.mAtkSpd((int) Math.round(prev * val));
						GameSession.autoAttackScheduler().schedule(() -> summon.mAtkSpd(prev), dur, TimeUnit.MILLISECONDS);
					}
				}
			}
		}

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

	public void receivePositiveSkill(SkillTemplate sk, String casterName, GameSession caster, boolean sps, boolean bss) {
		if (active() == null) {
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
				double healAmount = power + Math.sqrt(Math.max(1.0, mAtk * ssMul));
				healHp(healAmount);
			}
			case "HEAL_STATIC" -> healHp(power);
			case "HEAL_PERCENT" -> healHp(active().maxHp() * power / 100.0);
			case "MANAHEAL" -> {
				double before = active().currentMp();
				active().currentMp(before + power);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active().currentMp() - before))));
			}
			case "MANARECHARGE" -> {
				int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : (casterLevel > 0 ? casterLevel : active().level());
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
			case "NEGATE" -> {
				handleNegateDebuffs(sk);
				if (power > 0) {
					healHp(power);
				}
			}
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
		if (targets == null || targets.isEmpty()) {
			return;
		}
		double totalCurHp = 0;
		double totalMaxHp = 0;
		List<GameSession> validMembers = new ArrayList<>();
		for (GameSession s : targets) {
			if (s != null && s.active() != null && !s.active().isDead()) {
				totalCurHp += s.active().currentHp();
				totalMaxHp += s.active().maxHp();
				validMembers.add(s);
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
		double power = sk.power();
		double hpPercent = power > 0 ? Math.min(100.0, power) : 20.0;
		int revivedHp = Math.max(1, (int) Math.round(active().maxHp() * (hpPercent / 100.0)));
		active().currentHp(revivedHp);
		active().currentCp(0.0);

		var revive = new Revive(active().objectId());
		send(revive);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, revive, false);

		var t = ctx().characters().template(active());
		send(new UserInfo(active(), t));
		session.sendVitals();
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(revivedHp)));
		ctx().characters().save(active(), true);
	}

	public void healHp(double amount) {
		if (active() != null && active().isOlympiadMode() && ctx() != null && ctx().combat() != null && ctx().combat().olyDamageManager() != null) {
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
		applySkillEffects(sk, toggle, 0);
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs) {
		boolean any = false;
		PlayerCharacter owner = active();
		for (var e : sk.effects()) {
			String name = e.name();
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
			boolean isDebuff = !toggle && (sk.isDebuff() || sk.isOffensive());
			boolean isInvincible = name.equalsIgnoreCase("Invincible");
			boolean isBuffOrDebuff = toggle || name.equalsIgnoreCase("Buff") || name.equalsIgnoreCase("Debuff")
					|| sk.isBuff() || isDebuff || isInvincible;
			if (e.funcs().isEmpty() && !isBuffOrDebuff) {
				continue; // efeito sem stats (Stun, Fear, etc.) ainda nao tem motor no jogador
			}
			String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + name
					: e.stackType();
			int stackOrder = e.stackOrder() > 0 ? (int) Math.round(e.stackOrder()) : sk.level();
			long duration = remainingMs > 0 ? remainingMs : e.durationMs();
			if (duration <= 0 && !toggle) {
				duration = 30_000L;
			}
			if (!isDebuff && remainingMs <= 0 && Config.ENABLE_MODIFY_SKILL_DURATION && Config.SKILL_DURATION_LIST != null
					&& Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
				duration = Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
			}
			long end = toggle ? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: System.currentTimeMillis() + duration;
			var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, stackOrder, end, e.funcs(), isDebuff);
			owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner));
			if (isInvincible) {
				owner.invul(true);
			}
			any = true;
			if (toggle && (name.equalsIgnoreCase("DamOverTime") || name.equalsIgnoreCase("ManaDamOverTime")
					|| name.equalsIgnoreCase("MpConsumePerLevel") || name.equalsIgnoreCase("Relax")) && e.val() > 0) {
				startToggleDrainTask(sk, e, owner);
			} else if (!toggle) {
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
						send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
								new SystemMessage.SkillName(sk.id(), sk.level())));
						session.refreshBuffs();
						session.saveBuffs();
					}
				}, duration, TimeUnit.MILLISECONDS);
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
		long period = (e.period() > 0 ? e.period() : 3) * 1000L;
		double drainVal = e.val();
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = GameSession.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (active() != owner || !session.inWorld() || owner.isDead() || !owner.effects().hasSkill(sk.id())) {
				session.stopHot(taskKey, self.get());
				return;
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
					send(SystemMessage.id(SystemMessage.SKILL_REMOVED_DUE_LACK_MP));
					send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
							new SystemMessage.SkillName(sk.id(), sk.level())));
					session.refreshBuffs();
					session.saveBuffs();
					session.sendVitals();
					return;
				}
				owner.currentMp(Math.max(0, owner.currentMp() - drainVal));
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
			abnormalMask = 0x0001;
		} else if (kind.contains("bleed") || stackType.contains("bleed") || sk.skillType().equalsIgnoreCase("BLEED")) {
			abnormalMask = 0x0002;
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
					|| (ctx() != null && ctx().zones() != null && ctx().zones().isInsidePeace(owner.x(), owner.y(), owner.z()))) {
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
			}
			send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			session.refreshBuffs();
			session.saveBuffs();
			return;
		}
		if (active().currentMp() < sk.mpConsume() + sk.mpInitialConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active().currentMp(active().currentMp() - sk.mpConsume() - sk.mpInitialConsume());
		var msu = new MagicSkillUse(active().objectId(), active().objectId(), sk.id(), sk.level(), 0, 0,
				active().x(), active().y(), active().z(), active().x(), active().y(), active().z());
		send(msu);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
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
		double conRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(t.con());
		double menRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(effectiveMen) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(t.men());
		active().maxHp(Math.max(1, (int) PlayerStats.applyStat(active(), "maxHp", (int) Math.round(t.calculateMaxHp(lvl) * conRatio))));
		active().maxMp(Math.max(1, (int) PlayerStats.applyStat(active(), "maxMp", (int) Math.round((t.calculateMaxMp(lvl) + mpBonus) * menRatio))));
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
