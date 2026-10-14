package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.instance.frintezza.FrintezzaService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.pve.DungeonService;
import com.lopez.l2j.game.pve.SoloFarmService;
import com.lopez.l2j.game.service.BotsPreventionService;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handler centralizado para o ciclo pós-impacto de combate físico/mágico do jogador
 * e de seu Summon/Pet ativo, desacoplando regras de morte de monstros, Dungeons, SoloFarm,
 * Anti-Bot Captcha, Raid Points, Quests e Drops de {@link GameSession}.
 */
public final class PlayerCombatHandler {

	private PlayerCombatHandler() {
	}

	/**
	 * Processa todos os gatilhos de morte de um NPC/Monstro abatido por {@code session}.
	 */
	public static void handleMonsterDeath(
			GameSession session,
			GameSession.Context ctx,
			PlayerCharacter active,
			CharTemplate template,
			NpcInstance npc,
			Party party,
			long expReward,
			int spReward) {
		if (session == null || ctx == null || active == null || npc == null) {
			return;
		}

		session.stopAutoAttack();

		if (ctx.npcAi() != null) {
			ctx.npcAi().stopCombat(npc);
			ctx.npcAi().scheduleDecayAndRespawn(npc);
		}

		if (BossManager.getInstance() != null) {
			BossManager.getInstance().onBossKilled(npc, active, party);
		}

		var die = new Die(npc.objectId(), false);
		session.send(die);
		if (ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, die, false);
		}

		if (party != null) {
			double ratePartyXp = ctx.rates() != null ? ctx.rates().partyXp() : 1.0;
			double ratePartySp = ctx.rates() != null ? ctx.rates().partySp() : 1.0;
			party.distributeExpAndSp(expReward, spReward, active, ratePartyXp, ratePartySp);
		} else {
			session.applyExpAndSp(expReward, spReward, template);
		}

		if (npc.isSpoiled() && ctx.drops() != null) {
			var spoilDrops = ctx.drops().rollSpoil(npc.npcId(), active.level(),
					npc.template() != null ? npc.template().level() : 0);
			npc.spoilRewards(spoilDrops);
		}

		if (ctx.drops() != null) {
			ctx.drops().rewardMonsterDeath(active, npc, ctx.inventories(), session::send);
		}

		if (ctx.questManager() != null) {
			ctx.questManager().onNpcKill(npc, session, false);
		}

		if (ctx.raidPoints() != null && npc.template() != null && npc.template().isRaidBoss()) {
			int lvl = npc.template().level();
			int points = Math.max(1, lvl / 2 + ThreadLocalRandom.current().nextInt(-5, 6));
			if (party != null) {
				for (GameSession member : party.members()) {
					if (member != null && member.active() != null) {
						double dist = Math.hypot(member.active().x() - active.x(), member.active().y() - active.y());
						if (dist <= Party.getPartyRange()) {
							ctx.raidPoints().addPoints(member.active().objectId(), npc.npcId(), points);
							member.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
						}
					}
				}
			} else {
				ctx.raidPoints().addPoints(active.objectId(), npc.npcId(), points);
				session.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
			}
		}

		if (npc.npcId() == 25325 && Config.KILL_BARAKIEL_SET_NOBLESS) {
			session.rewardBarakielNoblesse(party, active);
		}

		// Gatilhos de Grand Bosses Épicos (Antharas, Valakas, Baium, Frintezza / Scarlet Van Halisha)
		int killedNpcId = npc.npcId();
		if (killedNpcId == AntharasService.ANTHARAS_WEAK
				|| killedNpcId == AntharasService.ANTHARAS_NORMAL
				|| killedNpcId == AntharasService.ANTHARAS_STRONG) {
			AntharasService antharasSvc = ctx.getBean(AntharasService.class);
			if (antharasSvc != null) antharasSvc.onAntharasKilled(active);
		} else if (killedNpcId == ValakasService.VALAKAS) {
			ValakasService valakasSvc = ctx.getBean(ValakasService.class);
			if (valakasSvc != null) valakasSvc.onValakasKilled(active);
		} else if (killedNpcId == BaiumService.BAIUM_NPC_ID) {
			BaiumService baiumSvc = ctx.getBean(BaiumService.class);
			if (baiumSvc != null) baiumSvc.onBaiumKilled(active);
		} else if (killedNpcId == FrintezzaService.SCARLET_WEAK_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null) frintSvc.transitionToPhase2();
		} else if (killedNpcId == FrintezzaService.SCARLET_MEDIUM_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null) frintSvc.transitionToPhase3();
		} else if (killedNpcId == FrintezzaService.SCARLET_STRONG_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null) frintSvc.onScarletDefeated();
		}

		// Integração com Dungeons Instanciadas (Onda C9)
		int instId = active.instanceId();
		if (instId > 0) {
			DungeonService dungeonSvc = ctx.getBean(DungeonService.class);
			if (dungeonSvc != null) {
				int prevX = active.x();
				int prevY = active.y();
				int prevZ = active.z();
				boolean stageChanged = dungeonSvc.onMonsterKilled(instId);
				if (stageChanged) {
					var dSession = dungeonSvc.getSession(instId);
					if (dSession != null && dSession.isCompleted()) {
						session.send(new CreatureSay(0, CreatureSay.HERO_VOICE, "Dungeon",
								"Parabens! Dungeon '" + dSession.template().name() + "' concluida com sucesso!"));
						session.teleportToLocation(82698, 148638, -3473, 0);
					} else if (dSession != null && (active.x() != prevX || active.y() != prevY || active.z() != prevZ)) {
						session.send(new CreatureSay(0, CreatureSay.PARTY, "Dungeon",
								"Avancando para o Estagio " + dSession.currentStageNumber() + "!"));
						session.teleportToLocation(active.x(), active.y(), active.z(), instId);
					}
				}
			}

			// Integração com SoloFarm Instanciado (Onda C9)
			SoloFarmService soloFarmSvc = ctx.getBean(SoloFarmService.class);
			if (soloFarmSvc != null) {
				boolean finished = soloFarmSvc.onMobKilled(instId);
				if (finished) {
					session.send(new CreatureSay(0, CreatureSay.PARTY, "SoloFarm",
							"Pacote de monstros concluido! Retornando para Giran."));
					session.teleportToLocation(SoloFarmService.EXIT_X, SoloFarmService.EXIT_Y, SoloFarmService.EXIT_Z, 0);
				}
			}
		}

		// Integração com Anti-Bot Captcha (BotsPreventionService)
		if (ctx.botsPrevention() != null && ctx.botsPrevention().isEnabled()) {
			Optional<BotsPreventionService.CaptchaChallenge> challengeOpt = ctx.botsPrevention().onMobKill(active.objectId());
			if (challengeOpt.isPresent()) {
				String html = ctx.botsPrevention().buildCaptchaHtml(challengeOpt.get());
				session.send(new NpcHtmlMessage(0, html));
			}
		}

		if (ctx.characters() != null) {
			ctx.characters().save(active, true);
		}
		session.saveBuffs();
	}

	/**
	 * Executa ataque assistido do Summon/Pet ativo contra o NPC alvo caso o dono esteja atacando.
	 */
	public static void triggerSummonAssistAttack(GameSession session, GameSession.Context ctx, NpcInstance npc) {
		if (session == null || ctx == null || npc == null || npc.isDead()) {
			return;
		}
		PlayerCharacter owner = session.active();
		if (owner == null) {
			return;
		}
		SummonInstance summon = owner.activeSummon();
		if (summon == null || summon.isDead() || summon.isDisabled()) {
			return;
		}

		double dist = summon.distanceTo(npc);
		if (dist > 120.0) {
			double angle = Math.atan2(summon.y() - npc.y(), summon.x() - npc.x());
			int nx = npc.x() + (int) Math.round(Math.cos(angle) * 60);
			int ny = npc.y() + (int) Math.round(Math.sin(angle) * 60);
			int nz = npc.z();
			int ox = summon.x();
			int oy = summon.y();
			int oz = summon.z();
			summon.moveTo(nx, ny, nz);
			MoveToLocation mv = new MoveToLocation(summon.objectId(), nx, ny, nz, ox, oy, oz);
			session.send(mv);
			if (ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, mv, false);
			}
		}

		if (ctx.combat() != null) {
			var hit = ctx.combat().summonAttackNpc(summon, npc);
			Attack atk = new Attack(summon.objectId(), npc.objectId(), hit.damage(), hit.flags(),
					summon.x(), summon.y(), summon.z());
			session.send(atk);
			if (ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, atk, false);
			}

			int remHp = hit.isDead() ? 0 : Math.max(0, hit.remainingHp());
			StatusUpdate su = StatusUpdate.hp(npc.objectId(), remHp, (int) npc.maxHp());
			session.send(su);
			if (ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			}

			if (hit.isDead()) {
				CharTemplate tpl = ctx.characters() != null ? ctx.characters().template(owner.classId()).orElse(null) : null;
				handleMonsterDeath(session, ctx, owner, tpl, npc, session.party(), hit.expReward(), hit.spReward());
			}
		}
	}
}
