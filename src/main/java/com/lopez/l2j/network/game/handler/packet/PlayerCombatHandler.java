package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.achievements.AchievementsService;
import com.lopez.l2j.game.arenaduel.ArenaDuelService;
import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.castle.siege.SiegeRecord;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeRecord;
import com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.cubic.CubicService;
import com.lopez.l2j.game.cursed.CursedWeaponsManager;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.LastManStandingService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.fortress.siege.FortressSiegeRecord;
import com.lopez.l2j.game.fortress.siege.FortressSiegeService;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.instance.frintezza.FrintezzaService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.olympiad.OlympiadGameService;
import com.lopez.l2j.game.olympiad.OlympiadMatch;
import com.lopez.l2j.game.olympiad.OlympiadParticipant;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.pve.DungeonService;
import com.lopez.l2j.game.pve.SoloFarmService;
import com.lopez.l2j.game.quest.impl.Quest350EnhanceYourWeapon;
import com.lopez.l2j.game.service.BotsPreventionService;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.tournament.TournamentService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handler centralizado para o ciclo de combate físico/mágico do jogador
 * (Polearm multi-target, Dual 2-hit, Bow reuse/MP/gauge, interceptação de morte
 * em Olympiad)
 * e de seu Summon/Pet ativo, desacoplando regras de morte de monstros,
 * Dungeons, SoloFarm,
 * Anti-Bot Captcha, Raid Points, Quests, Soul Crystals, Cursed Weapons e Drops
 * de {@link GameSession}.
 */
public final class PlayerCombatHandler {

	private PlayerCombatHandler() {
	}

	/**
	 * Representa um acerto adicional de arma Polearm (lança) no cone frontal de
	 * 120° (#139).
	 */
	public record PolearmExtraTarget(NpcInstance npc, int damage, int flags, boolean crit) {
	}

	public static boolean isPolearmWeapon(ItemInstance weapon) {
		return weapon != null && weapon.template() != null
				&& "pole".equalsIgnoreCase(weapon.template().subType());
	}

	public static boolean isDualWeapon(ItemInstance weapon) {
		if (weapon == null || weapon.template() == null || weapon.template().subType() == null) {
			return false;
		}
		String sub = weapon.template().subType();
		return "dual".equalsIgnoreCase(sub) || "dualfist".equalsIgnoreCase(sub);
	}

	public static boolean isBowWeapon(ItemInstance weapon) {
		return weapon != null && weapon.template() != null
				&& "bow".equalsIgnoreCase(weapon.template().subType());
	}

	/**
	 * Calcula a quantidade máxima de alvos atingidos por um ataque de Polearm
	 * (#139),
	 * respeitando Polearm Mastery (skill 216) e modificadores de stat
	 * {@code atkCountMax}.
	 */
	public static int resolvePolearmMaxTargets(PlayerCharacter active) {
		if (active == null) {
			return 1;
		}
		double baseMax = active.skillLevel(216) > 0 ? 5.0 : 3.0;
		int modified = (int) Math.round(PlayerStats.applyStat(active, "atkCountMax", baseMax));
		return Math.max(1, Math.min(12, modified));
	}

	/**
	 * Planeja os acertos secundários de Polearm em um cone frontal de 120° (±60°)
	 * com decaimento progressivo de dano (85%, 72%, 61%, 52%...) (#139).
	 */
	public static List<PolearmExtraTarget> planPolearmExtraHits(
			GameSession session,
			GameSession.Context ctx,
			PlayerCharacter active,
			CharTemplate template,
			NpcInstance mainTarget,
			int ssGrade,
			int attackRange) {
		if (session == null || ctx == null || ctx.world() == null || ctx.combat() == null
				|| active == null || mainTarget == null) {
			return List.of();
		}
		int maxTargets = resolvePolearmMaxTargets(active);
		if (maxTargets <= 1) {
			return List.of();
		}
		int extraLimit = maxTargets - 1;
		int reach = Math.max(80, attackRange) + 60;
		double mainAngle = Math.atan2(mainTarget.y() - active.y(), mainTarget.x() - active.x());
		double halfConeRad = Math.toRadians(60.0); // 120° total frontal cone
		GeoEngine geo = GeoEngine.getInstance();

		List<PolearmExtraTarget> extraHits = new ArrayList<>();
		double decay = 0.85;
		for (NpcInstance cand : ctx.world().findNpcsAround(active.x(), active.y(), reach)) {
			if (cand == null || cand.objectId() == mainTarget.objectId() || cand.isDead()
					|| cand.template() == null || !cand.template().isAttackable()) {
				continue;
			}
			double dx = cand.x() - active.x();
			double dy = cand.y() - active.y();
			double dist = Math.hypot(dx, dy);
			double colRad = cand.template().collisionRadius() > 0 ? cand.template().collisionRadius() : 20.0;
			if (dist > reach + colRad) {
				continue;
			}
			double angle = Math.atan2(dy, dx);
			double diff = Math.abs(normalizeAngleDiff(angle - mainAngle));
			if (diff > halfConeRad) {
				continue;
			}
			if (geo != null && geo.isEnabled()
					&& !geo.canSeeTarget(active.x(), active.y(), active.z(), cand.x(), cand.y(), cand.z())) {
				continue;
			}
			var plan = ctx.combat().planAttackNpc(active, template, cand, ssGrade);
			int scaledDamage = plan.damage() > 0 ? Math.max(1, (int) Math.round(plan.damage() * decay)) : 0;
			extraHits.add(new PolearmExtraTarget(cand, scaledDamage, plan.flags(), plan.crit()));
			decay *= 0.85;
			if (extraHits.size() >= extraLimit) {
				break;
			}
		}
		return extraHits;
	}

	private static double normalizeAngleDiff(double angle) {
		while (angle <= -Math.PI)
			angle += 2.0 * Math.PI;
		while (angle > Math.PI)
			angle -= 2.0 * Math.PI;
		return angle;
	}

	/**
	 * Calcula o tempo de recarga (reuse delay) do arco em milissegundos (#141).
	 */
	public static int calculateBowReuseDelayMs(int pAtkSpd) {
		return Math.max(400, (1500 * 333) / Math.max(1, pAtkSpd));
	}

	/**
	 * Retorna o custo de MP por disparo de arco conforme o Grade da arma (#141).
	 */
	public static int getBowMpConsume(ItemInstance weapon) {
		if (weapon == null || weapon.template() == null || weapon.template().crystalType() == null) {
			return 1;
		}
		return switch (weapon.template().crystalType().toLowerCase(java.util.Locale.ROOT)) {
			case "c", "b" -> 2;
			case "a", "s" -> 3;
			default -> 1;
		};
	}

	/**
	 * Valida e consome MP do disparo de arco e envia barra vermelha
	 * {@link SetupGauge#RED} (#141).
	 */
	public static boolean checkAndConsumeBowMpAndGauge(
			GameSession session,
			PlayerCharacter active,
			ItemInstance weapon,
			int pAtkSpd) {
		if (!isBowWeapon(weapon) || session == null || active == null) {
			return true;
		}
		int mpCost = getBowMpConsume(weapon);
		if (active.currentMp() < mpCost) {
			session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			session.send(new ActionFailed());
			session.stopAutoAttack();
			session.send(new AutoAttackStop(active.objectId()));
			return false;
		}
		active.currentMp(Math.max(0.0, active.currentMp() - mpCost));
		session.sendVitals();
		int reuseMs = calculateBowReuseDelayMs(pAtkSpd);
		session.send(new SetupGauge(SetupGauge.RED, reuseMs));
		return true;
	}

	/**
	 * Executa um tick de ataque automático contra outro jogador, aplicando Transfer
	 * Pain (#180, #183),
	 * Cubic combat actions (#255) e interceptação de dano letal em Olympiad (#416).
	 */
	public static CombatService.PlayerDamageResult doAutoAttackPlayerTick(
			GameSession attackerSession,
			GameSession victimSession,
			GameSession.Context ctx,
			int damage) {
		return applyPlayerDamageWithOlympiadCheck(attackerSession, victimSession, ctx, damage);
	}

	/**
	 * Aplica dano entre jogadores interceptando dano letal durante partidas das
	 * Grandes Olimpíadas
	 * (clamp HP >= 1.0, registra dano no {@link OlympiadGameService} e encerra a
	 * partida sem matar o personagem) (#416),
	 * além de redirecionar dano via Transfer Pain (Skill 1262) para o Servitor
	 * ativo da vítima (#180, #183)
	 * e acionar os Cubics ativos do atacante e da vítima (#255).
	 */
	public static CombatService.PlayerDamageResult applyPlayerDamageWithOlympiadCheck(
			GameSession attackerSession,
			GameSession victimSession,
			GameSession.Context ctx,
			int damage) {
		if (victimSession == null || victimSession.activeChar() == null) {
			return new CombatService.PlayerDamageResult(0, 0, 0, false, 0, 0);
		}
		PlayerCharacter victim = victimSession.activeChar();
		PlayerCharacter attacker = attackerSession != null ? attackerSession.activeChar() : null;

		// Aciona Cubics em combate (atacante contra vitima e vitima se defendendo)
		CubicService cubicSvc = (ctx != null && ctx.getBean(CubicService.class) != null)
				? ctx.getBean(CubicService.class)
				: CubicService.getInstance();
		if (cubicSvc != null) {
			if (attacker != null) {
				cubicSvc.onOwnerAttack(attacker, victim);
			}
			if (attacker != null && !victim.isDead()) {
				cubicSvc.onOwnerAttack(victim, attacker);
			}
		}

		// Redirecionamento de dano via Transfer Pain (Skill 1262) para o Servitor ativo
		// da vitima (#180, #183)
		int effectiveDamage = Math.max(0, damage);
		SummonInstance victimSummon = victim.activeSummon();
		if (effectiveDamage > 0 && victimSummon != null && victimSummon.isServitor() && !victimSummon.isDead()) {
			int absorbed = victimSummon.divertTransferPainDamage(effectiveDamage);
			if (absorbed > 0) {
				effectiveDamage = Math.max(0, effectiveDamage - absorbed);
				victimSession
						.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(victimSummon));
			}
		}

		if (victim.isOlympiadMode() || (attacker != null && attacker.isOlympiadMode())) {
			OlympiadGameService olyGames = ctx != null ? ctx.getBean(OlympiadGameService.class) : null;
			int matchId = findActiveOlympiadMatchId(olyGames, victim.objectId());
			int actualDmg = Math.max(0, effectiveDamage);
			if (matchId > 0 && olyGames != null && attacker != null) {
				boolean matchEnded = olyGames.recordCombatDamage(matchId, attacker.objectId(), victim.objectId(),
						actualDmg);
				if (matchEnded) {
					if (attackerSession != null) {
						attackerSession.stopAutoAttack();
						attackerSession.cancelCast();
					}
					victimSession.stopAutoAttack();
					victimSession.cancelCast();
				}
				return new CombatService.PlayerDamageResult(actualDmg, 0, actualDmg, false,
						(int) Math.max(1.0, victim.currentHp()), (int) victim.currentCp());
			}
			// Mesmo sem match registrada no serviço, impede morte real se olympiadMode
			// estiver ativo
			double newHp = victim.currentHp() - actualDmg;
			if (newHp <= 1.0) {
				victim.currentHp(1.0);
				if (attackerSession != null) {
					attackerSession.stopAutoAttack();
					attackerSession.cancelCast();
				}
				victimSession.stopAutoAttack();
				victimSession.cancelCast();
				return new CombatService.PlayerDamageResult(actualDmg, 0, actualDmg, false, 1,
						(int) victim.currentCp());
			}
			victim.currentHp(newHp);
			return new CombatService.PlayerDamageResult(actualDmg, 0, actualDmg, false,
					(int) victim.currentHp(), (int) victim.currentCp());
		}

		if (ctx != null && ctx.combat() != null) {
			return ctx.combat().applyDamagePlayer(victim, effectiveDamage);
		}
		victim.currentHp(Math.max(0.0, victim.currentHp() - Math.max(0, effectiveDamage)));
		return new CombatService.PlayerDamageResult(effectiveDamage, 0, effectiveDamage, victim.isDead(),
				(int) victim.currentHp(), (int) victim.currentCp());
	}

	@SuppressWarnings("unchecked")
	private static int findActiveOlympiadMatchId(OlympiadGameService olyGames, int charId) {
		if (olyGames == null || charId <= 0) {
			return -1;
		}
		try {
			var field = OlympiadGameService.class.getDeclaredField("activeMatches");
			field.setAccessible(true);
			Map<Integer, OlympiadMatch> matches = (Map<Integer, OlympiadMatch>) field.get(olyGames);
			if (matches != null) {
				for (OlympiadMatch m : matches.values()) {
					for (OlympiadParticipant p : m.teamA()) {
						if (p.charId() == charId)
							return m.matchId();
					}
					for (OlympiadParticipant p : m.teamB()) {
						if (p.charId() == charId)
							return m.matchId();
					}
				}
			}
		} catch (Exception ignored) {
		}
		return -1;
	}

	/**
	 * Processa todos os gatilhos de morte de um NPC/Monstro abatido por
	 * {@code session}.
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

		boolean hasSweep = npc.isSpoiled();
		if (npc.isSpoiled() && ctx.drops() != null) {
			double spoilMult = ctx.drops().resolveNpcTypeDropMultiplier(npc, false, true, 0);
			var spoilDrops = ctx.drops().rollSpoil(npc.npcId(), active.level(),
					npc.template() != null ? npc.template().level() : 0, spoilMult);
			if (spoilDrops != null && !spoilDrops.isEmpty()) {
				npc.spoilRewards(spoilDrops);
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

		// Verificação de Drop de Cursed Weapons (Zariche 8190 / Akamanah 8689) (#275)
		CursedWeaponsManager cwm = ctx.cursedWeapons() != null ? ctx.cursedWeapons()
				: CursedWeaponsManager.getInstance();
		if (cwm != null && npc.isMonster() && !npc.isRaid()) {
			cwm.checkDrop(npc.npcId(), active);
		}

		if (ctx.questManager() != null) {
			ctx.questManager().onNpcKill(npc, session, false);
		}

		// Absorção e evolução de Soul Crystal (Quest 350 Enhance Your Weapon) (#203,
		// #435)
		tryAbsorbSoulCrystal(session, ctx, active, npc);

		if (ctx.raidPoints() != null && npc.template() != null && npc.template().isRaidBoss()) {
			int lvl = npc.template().level();
			int points = Math.max(1, lvl / 2 + ThreadLocalRandom.current().nextInt(-5, 6));
			if (party != null) {
				for (GameSession member : party.members()) {
					if (member != null && member.active() != null) {
						double dist = Math.hypot(member.active().x() - active.x(), member.active().y() - active.y());
						if (dist <= Party.getPartyRange()) {
							ctx.raidPoints().addPoints(member.active().objectId(), npc.npcId(), points);
							member.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS,
									new SystemMessage.Number(points)));
						}
					}
				}
			} else {
				ctx.raidPoints().addPoints(active.objectId(), npc.npcId(), points);
				session.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
			}
		}

		// Progresso de Conquistas (AchievementsService) no abate de monstros e Raid
		// Bosses
		AchievementsService achievements = ctx.achievements() != null
				? ctx.achievements()
				: ctx.getBean(AchievementsService.class);
		if (npc.isRaid() && ctx.characterVariables() != null) {
			int raidKills = ctx.characterVariables().getInt(active.objectId(), "RAID_KILLS", 0) + 1;
			ctx.characterVariables().set(active.objectId(), "RAID_KILLS", raidKills);
		}
		if (achievements != null) {
			achievements.checkAvailableAchievements(active);
		}

		if (npc.npcId() == 25325 && Config.KILL_BARAKIEL_SET_NOBLESS) {
			session.rewardBarakielNoblesse(party, active);
		}

		// Gatilhos de Grand Bosses Épicos (Antharas, Valakas, Baium, Frintezza /
		// Scarlet Van Halisha)
		int killedNpcId = npc.npcId();
		BossManager bm = BossManager.getInstance();
		if (killedNpcId == AntharasService.ANTHARAS_WEAK
				|| killedNpcId == AntharasService.ANTHARAS_NORMAL
				|| killedNpcId == AntharasService.ANTHARAS_STRONG) {
			if (bm == null || bm.antharasService() == null) {
				AntharasService antharasSvc = ctx.getBean(AntharasService.class);
				if (antharasSvc != null)
					antharasSvc.onAntharasKilled(active);
			}
		} else if (killedNpcId == ValakasService.VALAKAS) {
			if (bm == null || bm.valakasService() == null) {
				ValakasService valakasSvc = ctx.getBean(ValakasService.class);
				if (valakasSvc != null)
					valakasSvc.onValakasKilled(active);
			}
		} else if (killedNpcId == BaiumService.BAIUM_NPC_ID) {
			if (bm == null || bm.baiumService() == null) {
				BaiumService baiumSvc = ctx.getBean(BaiumService.class);
				if (baiumSvc != null)
					baiumSvc.onBaiumKilled(active);
			}
		} else if (killedNpcId == FrintezzaService.SCARLET_WEAK_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null)
				frintSvc.transitionToPhase2();
		} else if (killedNpcId == FrintezzaService.SCARLET_MEDIUM_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null)
				frintSvc.transitionToPhase3();
		} else if (killedNpcId == FrintezzaService.SCARLET_STRONG_ID) {
			FrintezzaService frintSvc = ctx.getBean(FrintezzaService.class);
			if (frintSvc != null)
				frintSvc.onScarletDefeated();
		}

		// Gatilhos de Cerco a Castelos: Control Towers e Flame Towers (#387, #388)
		handleCastleSiegeStructureDeath(ctx, npc, killedNpcId);

		// Gatilhos de Cerco a Fortalezas: Subunit Commanders (#395)
		handleFortressSiegeCommanderDeath(ctx, npc, killedNpcId);

		// Gatilhos de Cerco a Clan Halls Contestáveis: Bosses, Bandeiras e Gourds
		// (#399, #400, #401)
		handleClanHallSiegeNpcDeath(ctx, active, npc, killedNpcId);

		// Dimensional Rift Boss Anakazel (NPCs 25333..25338) (#345)
		if (killedNpcId >= 25333 && killedNpcId <= 25338 && ctx.dimensionalRift() != null) {
			int leaderId = party != null ? party.leaderObjectId() : active.objectId();
			List<PlayerCharacter> members = party != null
					? party.members().stream().map(GameSession::activeChar).filter(Objects::nonNull).toList()
					: List.of(active);
			ctx.dimensionalRift().onBossDefeated(leaderId, killedNpcId, members);
		}

		if (ctx.fourSepulchers() != null) {
			var fs = ctx.fourSepulchers();
			for (int sepId = 1; sepId <= 4; sepId++) {
				var mausoleum = fs.getMausoleum(sepId);
				if (mausoleum != null && mausoleum.isInUse() && mausoleum.players().contains(active.objectId())) {
					if (killedNpcId == mausoleum.bossId()) {
						fs.defeatShadowOfHalisha(sepId, active);
						session.send(new CreatureSay(0, CreatureSay.PARTY, "FourSepulchers",
								"Shadow of Halisha derrotada! Voce recebeu o Calice do Sepulcro!"));
					} else if (mausoleum.spawnedMonsters().contains(npc)) {
						boolean allDead = mausoleum.spawnedMonsters().stream().allMatch(m -> m == npc || m.isDead());
						if (allDead) {
							fs.clearRoom(sepId, active);
							fs.openNextRoomDoor(sepId, active);
							session.send(new CreatureSay(0, CreatureSay.PARTY, "FourSepulchers",
									"Sala limpa! Chapel Key entregue e grupo avancou para a Sala " + mausoleum.stage()
											+ "/5."));
						}
					}
					break;
				}
			}
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
					} else if (dSession != null
							&& (active.x() != prevX || active.y() != prevY || active.z() != prevZ)) {
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
					session.teleportToLocation(SoloFarmService.EXIT_X, SoloFarmService.EXIT_Y, SoloFarmService.EXIT_Z,
							0);
				}
			}
		}

		// Integração com PartyFarm Event (drop especial de moeda do evento)
		if (ctx.partyFarm() != null && ctx.partyFarm().isPartyFarmMonster(npc.objectId())) {
			int coinId = 6393; // Event - Glistering Medal
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
			Optional<BotsPreventionService.CaptchaChallenge> challengeOpt = ctx.botsPrevention()
					.onMobKill(active.objectId());
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

	private static void handleCastleSiegeStructureDeath(GameSession.Context ctx, NpcInstance npc, int killedNpcId) {
		SiegeService sieges = ctx.sieges() != null ? ctx.sieges() : ctx.getBean(SiegeService.class);
		if (sieges == null) {
			return;
		}
		String npcType = npc.template() != null && npc.template().type() != null ? npc.template().type() : "";
		boolean isControlTower = killedNpcId == SiegeService.CONTROL_TOWER_NPC_ID
				|| killedNpcId == 13003
				|| "L2ControlTower".equalsIgnoreCase(npcType);
		boolean isFlameTower = killedNpcId == SiegeService.FLAME_TOWER_NPC_ID
				|| killedNpcId == 13005
				|| "L2FlameTower".equalsIgnoreCase(npcType);
		if (!isControlTower && !isFlameTower) {
			return;
		}

		int targetCastleId = resolveActiveSiegeCastleId(sieges, npc);
		if (targetCastleId <= 0) {
			return;
		}
		if (isControlTower) {
			sieges.onControlTowerDestroyed(targetCastleId);
		} else {
			int towerIndex = 0;
			List<SiegeService.SiegeSpawn> ftSpawns = sieges.getFlameTowerSpawns(targetCastleId);
			for (int i = 0; i < ftSpawns.size(); i++) {
				SiegeService.SiegeSpawn sp = ftSpawns.get(i);
				if (Math.hypot(sp.x() - npc.x(), sp.y() - npc.y()) <= 200.0) {
					towerIndex = i;
					break;
				}
			}
			sieges.onFlameTowerDestroyed(targetCastleId, towerIndex);
		}
	}

	private static int resolveActiveSiegeCastleId(SiegeService sieges, NpcInstance npc) {
		int bestCastleId = -1;
		double bestDist = Double.MAX_VALUE;
		for (SiegeRecord rec : sieges.allSieges()) {
			if (rec.status() != SiegeStatus.IN_PROGRESS) {
				continue;
			}
			if (bestCastleId < 0) {
				bestCastleId = rec.castleId();
			}
			for (SiegeService.SiegeSpawn sp : sieges.getControlTowerSpawns(rec.castleId())) {
				double d = Math.hypot(sp.x() - npc.x(), sp.y() - npc.y());
				if (d < bestDist) {
					bestDist = d;
					bestCastleId = rec.castleId();
				}
			}
			for (SiegeService.SiegeSpawn sp : sieges.getFlameTowerSpawns(rec.castleId())) {
				double d = Math.hypot(sp.x() - npc.x(), sp.y() - npc.y());
				if (d < bestDist) {
					bestDist = d;
					bestCastleId = rec.castleId();
				}
			}
		}
		return bestCastleId;
	}

	private static void handleFortressSiegeCommanderDeath(GameSession.Context ctx, NpcInstance npc, int killedNpcId) {
		FortressSiegeService fortSieges = ctx.fortressSieges() != null
				? ctx.fortressSieges()
				: ctx.getBean(FortressSiegeService.class);
		if (fortSieges == null) {
			return;
		}
		String npcType = npc.template() != null && npc.template().type() != null ? npc.template().type() : "";
		String npcName = npc.name() != null ? npc.name().toLowerCase(java.util.Locale.ROOT) : "";
		boolean isCommander = (killedNpcId >= 35657 && killedNpcId <= 35970)
				|| "L2FortCommander".equalsIgnoreCase(npcType)
				|| npcName.contains("commander");

		for (FortressSiegeRecord rec : fortSieges.allSieges()) {
			if (rec.status() != SiegeStatus.IN_PROGRESS) {
				continue;
			}
			List<NpcInstance> activeNpcs = fortSieges.getActiveSiegeNpcs(rec.fortId());
			if (activeNpcs.contains(npc)) {
				fortSieges.defeatCommander(rec.fortId());
				return;
			}
			for (FortressSiegeService.CommanderSpawn cs : fortSieges.getCommanderSpawns(rec.fortId())) {
				if (cs.npcId() == killedNpcId) {
					fortSieges.defeatCommander(rec.fortId());
					return;
				}
			}
		}
		if (isCommander) {
			for (FortressSiegeRecord rec : fortSieges.allSieges()) {
				if (rec.status() == SiegeStatus.IN_PROGRESS) {
					fortSieges.defeatCommander(rec.fortId());
					return;
				}
			}
		}
	}

	private static void handleClanHallSiegeNpcDeath(
			GameSession.Context ctx,
			PlayerCharacter active,
			NpcInstance npc,
			int killedNpcId) {
		ClanHallSiegeService chSieges = ctx.clanHallSieges() != null
				? ctx.clanHallSieges()
				: ctx.getBean(ClanHallSiegeService.class);
		if (chSieges == null) {
			return;
		}
		int clanId = active != null ? active.clanId() : 0;
		long bossMaxHp = Math.max(1L, (long) npc.maxHp());

		if (killedNpcId == ClanHallSiegeService.NURKA_NPC_ID) {
			if (clanId > 0) {
				chSieges.recordBossDamage(21, clanId, killedNpcId, bossMaxHp);
			}
			chSieges.onSiegeBossKilled(21, killedNpcId);
		} else if (killedNpcId == ClanHallSiegeService.DIETRICH_NPC_ID
				|| killedNpcId == ClanHallSiegeService.MIKHAIL_NPC_ID
				|| killedNpcId == ClanHallSiegeService.GUSTAV_NPC_ID) {
			if (clanId > 0) {
				chSieges.recordBossDamage(34, clanId, killedNpcId, bossMaxHp);
			}
			if (killedNpcId == ClanHallSiegeService.GUSTAV_NPC_ID) {
				chSieges.onSiegeBossKilled(34, killedNpcId);
			}
		} else if (killedNpcId == ClanHallSiegeService.LIDIA_VON_HELLMANN_NPC_ID
				|| killedNpcId == ClanHallSiegeService.ALFRED_VON_HELLMANN_NPC_ID
				|| killedNpcId == ClanHallSiegeService.GISELLE_VON_HELLMANN_NPC_ID) {
			if (clanId > 0) {
				chSieges.recordBossDamage(64, clanId, killedNpcId, bossMaxHp);
			}
			if (killedNpcId == ClanHallSiegeService.LIDIA_VON_HELLMANN_NPC_ID) {
				chSieges.onSiegeBossKilled(64, killedNpcId);
			}
		} else if (killedNpcId >= 35423 && killedNpcId <= 35427) {
			for (int hallId : new int[] { 35, 63 }) {
				ClanHallSiegeRecord siege = chSieges.getSiege(hallId).orElse(null);
				if (siege != null && siege.state() == ClanHallSiegeRecord.SiegeState.IN_PROGRESS) {
					Integer flagOwnerClanId = null;
					for (Map.Entry<Integer, Integer> entry : siege.clanFlagNpcIds().entrySet()) {
						if (entry.getValue() != null && entry.getValue() == killedNpcId) {
							flagOwnerClanId = entry.getKey();
							break;
						}
					}
					if (flagOwnerClanId != null) {
						chSieges.onTeamFlagDestroyed(hallId, flagOwnerClanId);
					}
				}
			}
		} else if ((killedNpcId == 35588 || killedNpcId == 35589) && clanId > 0) {
			chSieges.damageRainbowGourd(clanId, ClanHallSiegeService.DEFAULT_GOURD_MAX_HP);
		}
	}

	private static void tryAbsorbSoulCrystal(
			GameSession session,
			GameSession.Context ctx,
			PlayerCharacter active,
			NpcInstance npc) {
		if (session == null || ctx == null || ctx.inventories() == null || active == null
				|| active.inventory() == null || npc == null || npc.template() == null) {
			if (npc != null) {
				ItemPacketHandler.clearSoulCrystalAbsorbers(npc.objectId());
			}
			return;
		}
		try {
			int mobLevel = npc.template().level();
			boolean isRaidOrBoss = npc.isRaid() || npc.template().isRaidBoss()
					|| "L2GrandBoss".equalsIgnoreCase(npc.template().type());
			if (mobLevel < 40 && !isRaidOrBoss) {
				return;
			}
			// Se Quest 350 já estiver ativa, QuestManager.onNpcKill já processou a evolução
			var qs = session.getQuestState(Quest350EnhanceYourWeapon.QUEST_NAME);
			if (qs != null && qs.isStarted()) {
				return;
			}

			Set<Integer> registeredAbsorbers = ItemPacketHandler.getSoulCrystalAbsorbers(npc.objectId()).keySet();
			List<GameSession> candidateSessions = new ArrayList<>();
			Set<Integer> addedPlayerIds = new LinkedHashSet<>();

			if (isRaidOrBoss && session.party() != null) {
				for (GameSession member : session.party().members()) {
					if (member != null && member.activeChar() != null && !member.activeChar().isDead()) {
						double dist = Math.hypot(member.activeChar().x() - npc.x(), member.activeChar().y() - npc.y());
						if (dist <= Party.getPartyRange() && addedPlayerIds.add(member.activeChar().objectId())) {
							candidateSessions.add(member);
						}
					}
				}
			}
			if (registeredAbsorbers != null && !registeredAbsorbers.isEmpty() && ctx.world() != null) {
				for (Integer absId : registeredAbsorbers) {
					if (absId != null && addedPlayerIds.add(absId)) {
						ctx.world().player(absId).ifPresent(op -> {
							if (op instanceof GameSession gs && gs.activeChar() != null && !gs.activeChar().isDead()) {
								candidateSessions.add(gs);
							}
						});
					}
				}
			}
			if (candidateSessions.isEmpty() && addedPlayerIds.add(active.objectId())) {
				candidateSessions.add(session);
			}

			int chance = isRaidOrBoss ? 100 : Math.clamp(Config.CHANCE_TO_LEVEL_SA, 1, 100);
			for (GameSession candSession : candidateSessions) {
				PlayerCharacter cand = candSession.activeChar();
				if (cand == null || cand.inventory() == null) {
					continue;
				}
				// Regra de Ressonância: 2 ou mais Soul Crystals no inventário impedem absorção
				// (#204)
				if (Quest350EnhanceYourWeapon.hasResonance(cand)) {
					candSession.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"O Soul Crystal ressoou com outro cristal no inventario e falhou em absorver a alma!"));
					continue;
				}
				// Para monstros normais que exigem uso prévio do cristal sobre o alvo, se houve
				// registro de absorvedores, valida participação
				if (!isRaidOrBoss && registeredAbsorbers != null && !registeredAbsorbers.isEmpty()
						&& !registeredAbsorbers.contains(cand.objectId())) {
					continue;
				}

				for (ItemInstance item : new ArrayList<>(cand.inventory().items())) {
					if (item == null)
						continue;
					int nextId = Quest350EnhanceYourWeapon.getNextCrystalId(item.itemId());
					if (nextId > 0) {
						int stage = Quest350EnhanceYourWeapon.getSoulCrystalStage(item.itemId());
						if (!Quest350EnhanceYourWeapon.isCompatibleMonsterLevel(stage, mobLevel, isRaidOrBoss)) {
							break;
						}
						if (ThreadLocalRandom.current().nextInt(100) < chance) {
							var removed = ctx.inventories().destroyItem(cand.inventory(), item.objectId(), 1,
									"SoulCrystalAbsorb");
							var added = ctx.inventories().addItem(cand.inventory(), nextId, 1, "SoulCrystalAbsorb");
							List<ItemInfo> updates = new ArrayList<>();
							if (removed != null && removed.item() != null) {
								updates.add(ItemInfo.of(removed.item(),
										removed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
							}
							if (added != null && added.item() != null) {
								updates.add(ItemInfo.of(added.item(),
										added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							}
							if (!updates.isEmpty()) {
								candSession.send(new InventoryUpdate(updates));
							}
							candSession.send(new ItemList(cand, false));
							candSession.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seu Soul Crystal absorveu a alma do monstro e evoluiu!"));
						}
						break;
					}
				}
			}
		} finally {
			ItemPacketHandler.clearSoulCrystalAbsorbers(npc.objectId());
		}
	}

	/**
	 * Processa a morte de um jogador abatido por outro jogador (PvP, PK, TownWar,
	 * GvE, FlagZone, CursedWeapon, CTF).
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

		com.lopez.l2j.game.war.GvEService gve = ctx != null ? ctx.getBean(com.lopez.l2j.game.war.GvEService.class)
				: null;
		com.lopez.l2j.game.war.TownWarService townWar = ctx != null
				? ctx.getBean(com.lopez.l2j.game.war.TownWarService.class)
				: null;
		com.lopez.l2j.game.zone.FlagZoneService flagZone = ctx != null
				? ctx.getBean(com.lopez.l2j.game.zone.FlagZoneService.class)
				: null;
		com.lopez.l2j.game.service.PvPColorService pvpColor = ctx != null && ctx.pvpColor() != null
				? ctx.pvpColor()
				: (ctx != null ? ctx.getBean(com.lopez.l2j.game.service.PvPColorService.class) : null);
		com.lopez.l2j.game.service.PvPRankService pvpRank = ctx != null && ctx.pvpRank() != null
				? ctx.pvpRank()
				: (ctx != null ? ctx.getBean(com.lopez.l2j.game.service.PvPRankService.class) : null);

		handlePlayerKill(attacker, targetPlayer, killerSession, victimSession, ctx, gve, townWar, flagZone, pvpColor,
				pvpRank);
	}

	/**
	 * Processa o abate entre jogadores integrando TownWar, GvE, FlagZone, Siege,
	 * ClanWar, Olympiad,
	 * CursedWeaponsManager, CTF, TvT, DM, LMS, Tournament, ArenaDuel, Achievements,
	 * PvPColor e PvPRank (#275, #403, #481, #483).
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

		// Integração com CursedWeaponsManager (evolução de estágio ou drop da arma
		// amaldiçoada) (#275)
		CursedWeaponsManager cwm = (ctx != null && ctx.cursedWeapons() != null)
				? ctx.cursedWeapons()
				: CursedWeaponsManager.getInstance();
		if (cwm != null) {
			cwm.onPlayerKilled(attacker, targetPlayer);
		}

		// Integração com CTF Event (drop de bandeira e respawn caso o portador morra)
		// (#481)
		CtfEventService ctf = ctx != null ? ctx.ctf() : null;
		boolean inCtf = ctf != null && ctf.isParticipant(targetPlayer.objectId());
		if (inCtf) {
			if (ctf.isFlagCarrier(targetPlayer.objectId())) {
				ctf.dropFlag(targetPlayer.objectId());
			}
			ctf.reviveParticipant(targetPlayer.objectId());
		}

		// Integração com TvT Event (Team vs Team)
		TvtEventService tvt = ctx != null && ctx.tvt() != null ? ctx.tvt()
				: (ctx != null ? ctx.getBean(TvtEventService.class) : null);
		boolean inTvt = tvt != null && tvt.isPlayerInEvent(targetPlayer.objectId());
		if (inTvt) {
			tvt.onKill(attacker.objectId(), targetPlayer.objectId());
		}

		// Integração com DM Event (DeathMatch Free-For-All)
		DmEventService dm = ctx != null && ctx.dm() != null ? ctx.dm()
				: (ctx != null ? ctx.getBean(DmEventService.class) : null);
		boolean inDm = dm != null && dm.isPlayerInEvent(targetPlayer.objectId());
		if (inDm) {
			dm.onKill(attacker.objectId(), targetPlayer.objectId());
		}

		// Integração com Last Man Standing (LMS Battle Royale) (#483)
		LastManStandingService lms = ctx != null ? ctx.getBean(LastManStandingService.class) : null;
		boolean inLms = lms != null && lms.isParticipant(targetPlayer.objectId());
		if (inLms) {
			lms.onPlayerDeath(targetPlayer.objectId());
		}

		// Integração com Tournament (1x1, 3x3, 5x5)
		TournamentService tournament = ctx != null ? ctx.getBean(TournamentService.class) : null;
		boolean inTournament = tournament != null && tournament.isInMatch(targetPlayer.objectId());
		if (inTournament) {
			tournament.onPlayerDeath(targetPlayer.objectId());
		}

		// Integração com Arena Duel 1x1
		ArenaDuelService arenaDuel = ctx != null && ctx.arenaDuel() != null ? ctx.arenaDuel()
				: (ctx != null ? ctx.getBean(ArenaDuelService.class) : null);
		boolean inArenaDuel = arenaDuel != null && arenaDuel.isInMatch(targetPlayer.objectId());
		if (inArenaDuel) {
			arenaDuel.onPlayerDeath(targetPlayer);
		}

		// Integração com Fortress Siege (drop de Combat Flag se o portador morrer)
		// (#395)
		FortressSiegeService fortSieges = ctx != null && ctx.fortressSieges() != null
				? ctx.fortressSieges()
				: (ctx != null ? ctx.getBean(FortressSiegeService.class) : null);
		if (fortSieges != null) {
			for (FortressSiegeRecord rec : fortSieges.allSieges()) {
				if (rec.status() == SiegeStatus.IN_PROGRESS) {
					fortSieges.dropCombatFlag(rec.fortId(), targetPlayer);
				}
			}
		}

		boolean inTownWar = townWar != null
				&& (townWar.isInsideWarZone(targetPlayer, ctx != null ? ctx.zones() : null)
						|| townWar.isInsideWarZone(attacker, ctx != null ? ctx.zones() : null));
		boolean inFlagZone = flagZone != null
				&& (flagZone.isInFlagZone(targetPlayer) || flagZone.isInFlagZone(attacker));
		boolean inGvE = gve != null && gve.isEnemy(attacker, targetPlayer);
		boolean inOlympiad = attacker.isOlympiadMode() || targetPlayer.isOlympiadMode();
		boolean inMutualClanWar = ctx != null && ctx.clanWars() != null
				&& attacker.clanId() > 0 && targetPlayer.clanId() > 0
				&& ctx.clanWars().isMutualWar(attacker.clanId(), targetPlayer.clanId());
		boolean inPvpCombatZone = (killerSession != null && killerSession.isInsidePvpCombatZone())
				|| (victimSession != null && victimSession.isInsidePvpCombatZone());

		if (inTownWar) {
			townWar.onWarKill(attacker, targetPlayer);
		}
		if (inGvE) {
			gve.onKill(attacker, targetPlayer);
		}
		if (inFlagZone) {
			flagZone.onPlayerDeath(targetPlayer, victimSession != null ? victimSession::send : null);
		}

		boolean karmaFreeEventOrWar = inTownWar || inFlagZone || inGvE || inOlympiad
				|| inMutualClanWar || inPvpCombatZone || inCtf || inTvt || inDm
				|| inLms || inTournament || inArenaDuel;

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
				String killerIp = killerSession != null ? killerSession.clientIp() : null;
				String victimIp = victimSession != null ? victimSession.clientIp() : null;
				pvpRank.registerPvPKill(attacker.objectId(), killerIp, targetPlayer.objectId(), victimIp, !isPvp);
			}
			if (killerSession != null) {
				killerSession.sendUserInfoAndBroadcastCharInfo();
			}
			if (ctx != null && ctx.characters() != null) {
				ctx.characters().save(attacker, true);
			}
		}

		// Verifica conquistas de PvP / PK (AchievementsService)
		AchievementsService achievements = ctx != null && ctx.achievements() != null
				? ctx.achievements()
				: (ctx != null ? ctx.getBean(AchievementsService.class) : null);
		if (achievements != null) {
			achievements.checkAvailableAchievements(attacker);
		}
	}

	/**
	 * Executa ataque assistido do Summon/Pet ativo contra o NPC alvo caso o dono
	 * esteja atacando.
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

			if (hit.damage() > 0 && !summon.isDead() && summon.absorbDam() > 0.0
					&& summon.currentHp() < summon.maxHp()) {
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
			if (ctx.questManager() != null) {
				ctx.questManager().onNpcAttack(npc, session, true);
			}
			StatusUpdate su = StatusUpdate.hp(npc.objectId(), remHp, (int) npc.maxHp());
			session.send(su);
			if (ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
			}

			if (hit.isDead()) {
				CharTemplate tpl = ctx.characters() != null ? ctx.characters().template(owner.classId()).orElse(null)
						: null;
				handleMonsterDeath(session, ctx, owner, tpl, npc, session.party(), hit.expReward(), hit.spReward());
			}
		}
	}

	/**
	 * Executa ataque assistido do Summon/Pet ativo contra o jogador alvo (PvP) caso
	 * o dono esteja atacando.
	 */
	public static void triggerSummonAssistAttackPlayer(GameSession session, GameSession.Context ctx,
			GameSession targetSession) {
		if (session == null || ctx == null || targetSession == null || targetSession.active() == null
				|| targetSession.active().isDead()) {
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
			CharTemplate tgtTpl = ctx.characters() != null
					? ctx.characters().template(targetPlayer.classId()).orElse(null)
					: null;
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
					double vampHeal = Math.min(dmgRes.damage(),
							Math.max(1.0, dmgRes.damage() * (summon.absorbDam() / 100.0)));
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
