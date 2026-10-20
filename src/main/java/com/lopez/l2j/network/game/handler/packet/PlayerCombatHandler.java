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

		boolean hasSweep = false;
		if (npc.isSpoiled() && ctx.drops() != null) {
			double spoilMult = ctx.drops().resolveNpcTypeDropMultiplier(npc, false, true, 0);
			var spoilDrops = ctx.drops().rollSpoil(npc.npcId(), active.level(),
					npc.template() != null ? npc.template().level() : 0, spoilMult);
			if (spoilDrops != null && !spoilDrops.isEmpty()) {
				npc.spoilRewards(spoilDrops);
				hasSweep = true;
			} else {
				npc.spoilRewards(java.util.List.of());
			}
		}

		var die = new Die(npc.objectId(), false, false, false, hasSweep);
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
		BossManager bm = BossManager.getInstance();
		if (killedNpcId == AntharasService.ANTHARAS_WEAK
				|| killedNpcId == AntharasService.ANTHARAS_NORMAL
				|| killedNpcId == AntharasService.ANTHARAS_STRONG) {
			if (bm == null || bm.antharasService() == null) {
				AntharasService antharasSvc = ctx.getBean(AntharasService.class);
				if (antharasSvc != null) antharasSvc.onAntharasKilled(active);
			}
		} else if (killedNpcId == ValakasService.VALAKAS) {
			if (bm == null || bm.valakasService() == null) {
				ValakasService valakasSvc = ctx.getBean(ValakasService.class);
				if (valakasSvc != null) valakasSvc.onValakasKilled(active);
			}
		} else if (killedNpcId == BaiumService.BAIUM_NPC_ID) {
			if (bm == null || bm.baiumService() == null) {
				BaiumService baiumSvc = ctx.getBean(BaiumService.class);
				if (baiumSvc != null) baiumSvc.onBaiumKilled(active);
			}
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

		// Integração com PartyFarm Event (drop especial de moeda do evento)
		if (ctx.partyFarm() != null && ctx.partyFarm().isPartyFarmMonster(npc.objectId())) {
			int coinId = ctx.partyFarm().getEventCoinItemId();
			if (coinId > 0 && ctx.inventories() != null && active.inventory() != null) {
				var added = ctx.inventories().addItem(active.inventory(), coinId, 1, "PartyFarmDrop");
				if (added != null) {
					session.send(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
							new SystemMessage.Number(1),
							new SystemMessage.ItemName(coinId)));
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
	 * Processa a morte de um jogador abatido por outro jogador (PvP, PK, TownWar, GvE, FlagZone).
	 */
	public static void handlePlayerDeath(
			GameSession killerSession,
			GameSession victimSession,
			GameSession.Context ctx,
			PlayerCharacter attacker,
			PlayerCharacter targetPlayer) {
		if (attacker == null || targetPlayer == null || attacker.objectId() == targetPlayer.objectId()) {
			return;
		}

		com.lopez.l2j.game.war.GvEService gve = ctx != null ? ctx.getBean(com.lopez.l2j.game.war.GvEService.class) : null;
		com.lopez.l2j.game.war.TownWarService townWar = ctx != null ? ctx.getBean(com.lopez.l2j.game.war.TownWarService.class) : null;
		com.lopez.l2j.game.zone.FlagZoneService flagZone = ctx != null ? ctx.getBean(com.lopez.l2j.game.zone.FlagZoneService.class) : null;
		com.lopez.l2j.game.service.PvPColorService pvpColor = ctx != null && ctx.pvpColor() != null
				? ctx.pvpColor()
				: (ctx != null ? ctx.getBean(com.lopez.l2j.game.service.PvPColorService.class) : null);
		com.lopez.l2j.game.service.PvPRankService pvpRank = ctx != null && ctx.pvpRank() != null
				? ctx.pvpRank()
				: (ctx != null ? ctx.getBean(com.lopez.l2j.game.service.PvPRankService.class) : null);

		handlePlayerKill(attacker, targetPlayer, killerSession, victimSession, ctx, gve, townWar, flagZone, pvpColor, pvpRank);
	}

	/**
	 * Processa o abate entre jogadores integrando TownWar, GvE, FlagZone, PvPColor e PvPRank.
	 */
	public static void handlePlayerKill(
			PlayerCharacter attacker,
			PlayerCharacter targetPlayer,
			GameSession killerSession,
			GameSession victimSession,
			GameSession.Context ctx,
			com.lopez.l2j.game.war.GvEService gve,
			com.lopez.l2j.game.war.TownWarService townWar,
			com.lopez.l2j.game.zone.FlagZoneService flagZone,
			com.lopez.l2j.game.service.PvPColorService pvpColor,
			com.lopez.l2j.game.service.PvPRankService pvpRank) {
		if (attacker == null || targetPlayer == null || attacker.objectId() == targetPlayer.objectId()) {
			return;
		}

		boolean inTownWar = townWar != null
				&& (townWar.isInsideWarZone(targetPlayer, ctx != null ? ctx.zones() : null)
						|| townWar.isInsideWarZone(attacker, ctx != null ? ctx.zones() : null));
		boolean inFlagZone = flagZone != null
				&& (flagZone.isInFlagZone(targetPlayer) || flagZone.isInFlagZone(attacker));
		boolean inGvE = gve != null && gve.isEnemy(attacker, targetPlayer);

		if (inTownWar) {
			townWar.onWarKill(attacker, targetPlayer);
		}
		if (inGvE) {
			gve.onKill(attacker, targetPlayer);
		}
		if (inFlagZone) {
			flagZone.onPlayerDeath(targetPlayer, victimSession != null ? victimSession::send : null);
		}

		boolean karmaFreeEventOrWar = inTownWar || inFlagZone || inGvE;

		if (ctx != null && ctx.pvpRewards() != null && killerSession != null && victimSession != null) {
			ctx.pvpRewards().handleKill(killerSession, victimSession, karmaFreeEventOrWar);
		} else {
			boolean isPvp = karmaFreeEventOrWar || targetPlayer.pvpFlag() > 0 || targetPlayer.karma() > 0;
			if (isPvp) {
				attacker.pvpKills(attacker.pvpKills() + 1);
			} else {
				attacker.pkKills(attacker.pkKills() + 1);
				int karmaToAdd = Math.max(1000, 2000 * attacker.pkKills());
				attacker.karma(attacker.karma() + karmaToAdd);
			}
			if (pvpColor != null) {
				pvpColor.applyColors(attacker);
			}
			if (pvpRank != null) {
				pvpRank.recordKill(attacker.objectId(), targetPlayer.objectId());
			}
			if (killerSession != null) {
				killerSession.sendUserInfoAndBroadcastCharInfo();
			}
			if (ctx != null && ctx.characters() != null) {
				ctx.characters().save(attacker, true);
			}
		}
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

			if (hit.damage() > 0 && !summon.isDead() && summon.absorbDam() > 0.0 && summon.currentHp() < summon.maxHp()) {
				double vampHeal = Math.min(hit.damage(), Math.max(1.0, hit.damage() * (summon.absorbDam() / 100.0)));
				summon.currentHp(Math.min(summon.maxHp(), summon.currentHp() + vampHeal));
				StatusUpdate sumSu = StatusUpdate.hp(summon.objectId(), (int) summon.currentHp(), summon.maxHp());
				session.send(sumSu);
				session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(summon));
				if (ctx.world() != null) {
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, sumSu, false);
				}
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

	/**
	 * Executa ataque assistido do Summon/Pet ativo contra o jogador alvo (PvP) caso o dono esteja atacando.
	 */
	public static void triggerSummonAssistAttackPlayer(GameSession session, GameSession.Context ctx, GameSession targetSession) {
		if (session == null || ctx == null || targetSession == null || targetSession.active() == null || targetSession.active().isDead()) {
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
		PlayerCharacter targetPlayer = targetSession.active();
		double dist = summon.distanceTo(targetPlayer);
		if (dist > 120.0) {
			double angle = Math.atan2(summon.y() - targetPlayer.y(), summon.x() - targetPlayer.x());
			int nx = targetPlayer.x() + (int) Math.round(Math.cos(angle) * 60);
			int ny = targetPlayer.y() + (int) Math.round(Math.sin(angle) * 60);
			int nz = targetPlayer.z();
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
			CharTemplate tgtTpl = ctx.characters() != null ? ctx.characters().template(targetPlayer.classId()).orElse(null) : null;
			var dmgRes = ctx.combat().summonAttackPlayer(summon, targetPlayer, tgtTpl);
			Attack atk = new Attack(summon.objectId(), targetPlayer.objectId(), dmgRes.damage(), 0,
					summon.x(), summon.y(), summon.z());
			session.send(atk);
			if (ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, atk, false);
			}

			if (dmgRes.damage() > 0) {
				targetSession.onAttacked(owner.objectId(), dmgRes.damage());
				targetSession.sendVitals();
				StatusUpdate su = StatusUpdate.hp(targetPlayer.objectId(), dmgRes.remainingHp(), targetPlayer.maxHp());
				session.send(su);
				if (ctx.world() != null) {
					ctx.world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
				}
				if (!summon.isDead() && summon.absorbDam() > 0.0 && summon.currentHp() < summon.maxHp()) {
					double vampHeal = Math.min(dmgRes.damage(), Math.max(1.0, dmgRes.damage() * (summon.absorbDam() / 100.0)));
					summon.currentHp(Math.min(summon.maxHp(), summon.currentHp() + vampHeal));
					StatusUpdate sumSu = StatusUpdate.hp(summon.objectId(), (int) summon.currentHp(), summon.maxHp());
					session.send(sumSu);
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(summon));
					if (ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, sumSu, false);
					}
				}
			}

			if (dmgRes.isDead()) {
				session.stopAutoAttack();
				targetSession.handlePlayerDeath(owner);
			}
		}
	}
}
