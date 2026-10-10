package com.lopez.l2j.game.ai;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillCanceld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de IA para NPCs e monstros:
 * - Perseguicao e combate contra jogadores
 * - Deteccao de jogadores por monstros agressivos (aggroRange)
 * - Movimentacao aleatoria (roaming) ao redor do ponto de spawn
 * - Despawn/decay de corpos e renascimento (respawn) automatico de monstros
 */
@Service
public class NpcAiService {

	private static final Logger log = LoggerFactory.getLogger(NpcAiService.class);

	private static final long DECAY_DELAY_MS = 7_000L;
	private static final long RESPAWN_DELAY_MS = 25_000L;

	private final GameWorld world;
	private final CombatService combatService;
	private final CharTemplateTable charTemplates;
	private final NpcSkillTable npcSkillTable;
	private final SkillTable skillTable;
	private final com.lopez.l2j.game.champion.ChampionService championService;
	private final com.lopez.l2j.game.zone.ZoneTable zones;

	private final Set<NpcInstance> activeCombatNpcs = ConcurrentHashMap.newKeySet();
	private final java.util.Map<Integer, AbstractNpcAI> aiArchetypes = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;
	private long tickCount = 0;

	public ScheduledExecutorService scheduler() {
		if (scheduler != null && !scheduler.isShutdown()) {
			return scheduler;
		}
		return com.lopez.l2j.network.game.GameSession.autoAttackScheduler();
	}

	public AbstractNpcAI getOrAssignAI(NpcInstance npc) {
		return aiArchetypes.computeIfAbsent(npc.objectId(), id ->
				NpcAiFactory.createAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler(), zones));
	}

	@org.springframework.beans.factory.annotation.Autowired
	public NpcAiService(
			GameWorld world,
			CombatService combatService,
			CharTemplateTable charTemplates,
			@org.springframework.beans.factory.annotation.Autowired(required = false) NpcSkillTable npcSkillTable,
			@org.springframework.beans.factory.annotation.Autowired(required = false) SkillTable skillTable,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.champion.ChampionService championService,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.zone.ZoneTable zones) {
		this.world = world;
		this.combatService = combatService;
		this.charTemplates = charTemplates;
		this.npcSkillTable = npcSkillTable;
		this.skillTable = skillTable;
		this.championService = championService;
		this.zones = zones;
	}

	public NpcAiService(
			GameWorld world,
			CombatService combatService,
			CharTemplateTable charTemplates,
			NpcSkillTable npcSkillTable,
			SkillTable skillTable) {
		this(world, combatService, charTemplates, npcSkillTable, skillTable, null, null);
	}

	public NpcAiService(GameWorld world, CombatService combatService, CharTemplateTable charTemplates) {
		this(world, combatService, charTemplates, null, null, null, null);
	}

	@PostConstruct
	public void start() {
		scheduler = Executors.newScheduledThreadPool(2, r -> {
			Thread t = new Thread(r, "NpcAiThread");
			t.setDaemon(true);
			return t;
		});
		scheduler.scheduleAtFixedRate(this::tick, 500, 500, TimeUnit.MILLISECONDS);
		log.info("NpcAiService iniciado (tick a cada 500ms)");
	}

	@PreDestroy
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Registra ou atualiza um monstro em estado de combate perseguindo/atacando um jogador.
	 */
	public void startCombat(NpcInstance npc, int targetPlayerId) {
		if (npc == null || npc.isDead()) {
			return;
		}
		var playerOpt = world.player(targetPlayerId);
		if (playerOpt.isPresent()) {
			var p = playerOpt.get();
			if (zones != null && (zones.isInsidePeace(p.x(), p.y(), p.z()) || zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
				return;
			}
		}
		npc.targetPlayerId(targetPlayerId);
		npc.inCombat(true);

		var npcStart = new com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStart(npc.objectId());
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npcStart);

		world.player(targetPlayerId).ifPresent(p -> p.onAttacked(npc.objectId()));

		int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
		long cooldownMs = 500_000L / pAtkSpd;
		npc.lastAttackTime(System.currentTimeMillis() - cooldownMs + 600);

		activeCombatNpcs.add(npc);

		// 1. Notifica lacaios (minions) para atacarem o mesmo alvo
		if (npc.hasMinions()) {
			for (NpcInstance minion : npc.minions()) {
				if (minion != null && !minion.isDead() && (!minion.inCombat() || minion.targetPlayerId() != targetPlayerId)) {
					startCombat(minion, targetPlayerId);
				}
			}
		}

		// 2. Se for lacaio sendo atacado, notifica o mestre e todos os lacaios irmaos do grupo para revidarem juntos (L2JDream)
		if (npc.isMinion() && npc.masterObjectId() != 0) {
			world.npc(npc.masterObjectId()).ifPresent(master -> {
				if (!master.isDead()) {
					if (!master.inCombat() || master.targetPlayerId() != targetPlayerId) {
						startCombat(master, targetPlayerId);
					}
					if (master.hasMinions()) {
						for (NpcInstance brother : master.minions()) {
							if (brother != null && !brother.isDead() && brother != npc
									&& (!brother.inCombat() || brother.targetPlayerId() != targetPlayerId)) {
								startCombat(brother, targetPlayerId);
							}
						}
					}
				}
			});
		}

		// Notifica monstros aliados da mesma Faccao (Social Call / Help Clan)
		notifyFactionCall(npc, targetPlayerId);
	}

	private void notifyFactionCall(NpcInstance npc, int targetPlayerId) {
		if (npc.template() == null) {
			return;
		}
		String factionId = npc.template().factionId();
		if (factionId == null || factionId.isBlank() || "null".equalsIgnoreCase(factionId)) {
			return;
		}
		int range = npc.template().factionRange();
		if (range <= 0) {
			return;
		}
		var nearby = world.findNpcsAround(npc.x(), npc.y(), range);
		for (NpcInstance ally : nearby) {
			if (ally == npc || ally.isDead() || ally.inCombat() || !ally.isMonster()) {
				continue;
			}
			// Aliados da facção só respondem ao chamado se estiverem no mesmo plano vertical (não em andares/cavernas)
			if (Math.abs(ally.z() - npc.z()) > 150) {
				continue;
			}
			if (ally.template() != null && factionId.equalsIgnoreCase(ally.template().factionId())) {
				if (combatService != null && !combatService.canSeeTarget(ally.x(), ally.y(), ally.z(), npc.x(), npc.y(), npc.z())) {
					continue;
				}
				startCombat(ally, targetPlayerId);
			}
		}
	}

	/**
	 * Remove um monstro do combate (por morte ou perda de aggro).
	 */
	public void stopCombat(NpcInstance npc) {
		if (npc != null) {
			boolean wasCasting = npc.isCasting();
			npc.abortCast();
			npc.abortAttack();
			npc.inCombat(false);
			npc.targetPlayerId(0);
			activeCombatNpcs.remove(npc);
			aiArchetypes.remove(npc.objectId());
			var stopAtk = new AutoAttackStop(npc.objectId());
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, stopAtk);
			if (wasCasting) {
				var cancelPkt = new MagicSkillCanceld(npc.objectId());
				world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, cancelPkt);
			}
		}
	}

	/**
	 * Cancela imediatamente o combate de todos os monstros que tinham este jogador como alvo
	 * (chamado quando o jogador morre, teleporta ou da To Village).
	 */
	public void stopCombatForPlayer(int playerId) {
		if (playerId == 0) {
			return;
		}
		var playerOpt = world.player(playerId);
		for (NpcInstance npc : java.util.List.copyOf(activeCombatNpcs)) {
			if (npc != null && (npc.targetPlayerId() == playerId || (npc.targetPlayerId() == 0 && npc.inCombat()))) {
				if (npc.isCasting()) {
					var cancelPkt = new MagicSkillCanceld(npc.objectId());
					playerOpt.ifPresent(p -> p.send(cancelPkt));
					world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, cancelPkt);
				}
				npc.abortCast();
				npc.abortAttack();
				returnToSpawn(npc);
			}
		}
		for (NpcInstance npc : world.npcs()) {
			if (npc != null && npc.targetPlayerId() == playerId) {
				if (npc.isCasting()) {
					var cancelPkt = new MagicSkillCanceld(npc.objectId());
					playerOpt.ifPresent(p -> p.send(cancelPkt));
					world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, cancelPkt);
				}
				npc.abortCast();
				npc.abortAttack();
				returnToSpawn(npc);
			}
		}
	}

	/**
	 * Agenda o sumico do corpo (decay) e posterior renascimento (respawn) do monstro derrotado.
	 */
	public void scheduleDecayAndRespawn(NpcInstance npc) {
		stopCombat(npc);
		// Apenas Raid Boss despawna seus lacaios ao morrer (regra retail / L2JDream)
		// Monstros comuns com lacaios mantem seus lacaios vivos lutando individualmente!
		if (npc.hasMinions() && npc.template() != null && npc.template().isRaidBoss()) {
			onMasterDied(npc);
		}

		if (scheduler == null || scheduler.isShutdown()) {
			return;
		}

		// 1. Decay: Apos 7s o corpo some do chao dos jogadores proximos
		scheduler.schedule(() -> {
			try {
				var deletePacket = new DeleteObject(npc.objectId());
				world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, deletePacket);
				world.removeNpc(npc);
			} catch (Exception e) {
				log.warn("Erro ao executar decay do monstro {}: {}", npc.name(), e.getMessage());
			}

			// Se for Raid Boss ou Grand Boss, o respawn NAO e gerido pelo NpcAiService!
			// O renascimento e controlado pelo RaidBossSpawnManager ou GrandBossManager com horas/dias.
			if (npc.template() != null && (npc.template().isRaidBoss() || npc.template().isGrandBoss())) {
				return;
			}

			// 2. Respawn: Apos o tempo de renascimento, revive com HP total no spawn original
			scheduler.schedule(() -> {
				try {
					if (npc.isMinion() && npc.masterObjectId() != 0) {
						var masterOpt = world.npc(npc.masterObjectId());
						if (masterOpt.isEmpty() || masterOpt.get().isDead()) {
							// Mestre esta morto, minion aguarda o mestre renascer para recompor o grupo
							return;
						}
					}

					respawnNpc(npc);

					// Se for mestre renascendo, revive tambem os minions vinculados que estiverem mortos
					if (npc.hasMinions()) {
						for (NpcInstance minion : npc.minions()) {
							if (minion != null && minion.isDead()) {
								respawnMinion(minion, npc);
							}
						}
					}
				} catch (Exception e) {
					log.warn("Erro ao executar respawn do monstro {}: {}", npc.name(), e.getMessage());
				}
			}, RESPAWN_DELAY_MS, TimeUnit.MILLISECONDS);

		}, DECAY_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	private void respawnNpc(NpcInstance npc) {
		npc.dead(false);
		if (championService != null) {
			championService.tryRollChampion(npc);
		} else {
			npc.currentHp(npc.maxHp());
			npc.currentMp(npc.template().maxMp());
		}
		npc.targetPlayerId(0);
		npc.inCombat(false);

		int targetX = npc.spawnX();
		int targetY = npc.spawnY();
		int targetZ = npc.spawnZ();
		int targetHeading = npc.spawnHeading();

		if (npc.isMinion() && npc.masterObjectId() != 0) {
			var masterOpt = world.npc(npc.masterObjectId());
			if (masterOpt.isPresent() && !masterOpt.get().isDead()) {
				var master = masterOpt.get();
				double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
				int dist = ThreadLocalRandom.current().nextInt(40, 90);
				targetX = master.x() + (int) (Math.cos(angle) * dist);
				targetY = master.y() + (int) (Math.sin(angle) * dist);
				targetZ = master.z();
				targetHeading = master.heading();
			}
		}
		npc.moveTo(targetX, targetY, targetZ, targetHeading);

		world.addNpc(npc);
		var npcInfo = new NpcInfo(npc);
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npcInfo);
		log.debug("Monstro {} renasceu em ({}, {}, {})", npc.name(), npc.x(), npc.y(), npc.z());
	}

	private void respawnMinion(NpcInstance minion, NpcInstance master) {
		if (minion == null || !minion.isDead() || master == null) {
			return;
		}
		minion.dead(false);
		minion.currentHp(minion.template().maxHp());
		minion.currentMp(minion.template().maxMp());
		minion.targetPlayerId(0);
		minion.inCombat(false);

		double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
		int dist = ThreadLocalRandom.current().nextInt(40, 90);
		int targetX = master.x() + (int) (Math.cos(angle) * dist);
		int targetY = master.y() + (int) (Math.sin(angle) * dist);
		int targetZ = master.z();
		int targetHeading = master.heading();
		minion.moveTo(targetX, targetY, targetZ, targetHeading);

		world.addNpc(minion);
		var npcInfo = new NpcInfo(minion);
		world.broadcastAround(minion.x(), minion.y(), GameWorld.VISIBILITY_RADIUS, npcInfo);
		log.debug("Minion {} renasceu junto ao mestre {} em ({}, {}, {})", minion.name(), master.name(), targetX, targetY, targetZ);
	}

	/**
	 * Executa um ciclo da IA para monstros em combate, deteccao de aggro e movimentacao.
	 */
	public void tick() {
		tickCount++;

		// 1. Processa monstros em combate
		for (NpcInstance npc : activeCombatNpcs) {
			try {
				processCombatNpc(npc);
			} catch (Exception e) {
				log.warn("Erro ao processar combate do monstro {}: {}", npc.name(), e.getMessage());
			}
		}

		// 2. Checagem de aggro (a cada 1 segundo = 2 ticks)
		if (tickCount % 2 == 0) {
			checkAggroAroundPlayers();
		}

		// 3. Roaming aleatorio de monstros pacíficos / ociosos (a cada 10 segundos = 20 ticks)
		if (tickCount % 20 == 0) {
			roamIdleMonsters();
		}
	}

	private void checkAggroAroundPlayers() {
		for (var player : world.players()) {
			var character = player.character();
			if (character == null || character.isDead() || character.isGm()) {
				continue;
			}
			if (zones != null && zones.isInsidePeace(player.x(), player.y(), player.z())) {
				continue;
			}
			var nearbyNpcs = world.findNpcsAround(player.x(), player.y(), 1000);
			for (var npc : nearbyNpcs) {
				if (npc.isDead() || npc.inCombat() || !npc.isMonster()) {
					continue;
				}
				// Diferença vertical: monstros subterrâneos ou em andares diferentes não agram na superfície
				int deltaZ = Math.abs(npc.z() - player.z());
				if (deltaZ > 150) {
					continue;
				}
				// Regra oficial Lineage II / L2JDream: Friendly Mobs (ex: Pixy, Bloody Pixy, Treant) so agram jogadores PK (Karma > 0)
				if (npc.template().isFriendlyMob() && character.karma() <= 0) {
					continue;
				}
				int aggroRange = npc.template().aggroRange();
				if (aggroRange > 0) {
					// Regra oficial Lineage II / L2JDream / AltMobNoAttackWithLevelDifference:
					// Monstros comuns nao agram jogadores alem do diferencial de nivel (com excecao de Raid Bosses e Grand Bosses)
					if (!npc.template().isRaidBoss() && !npc.template().isGrandBoss()) {
						int diff = Config.ALT_MOB_NO_ATTACK_WITH_LEVEL_DIFFERENCE;
						int delta = diff >= 0 ? diff : 9;
						if (character.level() >= npc.template().level() + delta) {
							continue;
						}
					}

					double dist3d = Math.sqrt(Math.pow(npc.x() - player.x(), 2) + Math.pow(npc.y() - player.y(), 2) + Math.pow(deltaZ, 2));
					if (dist3d <= aggroRange) {
						// Linha de visao (LoS): monstro precisa ver o jogador
						if (combatService != null && !combatService.canSeeTarget(npc, character)) {
							continue;
						}
						startCombat(npc, player.objectId());
						break; // um aggro por jogador por ciclo
					}
				}
			}
		}
	}

	private void roamIdleMonsters() {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		for (var player : world.players()) {
			var nearbyNpcs = world.findNpcsAround(player.x(), player.y(), 1200);
			for (var npc : nearbyNpcs) {
				if (npc.isDead() || npc.inCombat()) {
					continue;
				}
				if (npc.isMonster()) {
					// 25% de chance de caminhar um pouco
					if (rnd.nextInt(100) < 25) {
						int maxOffset = Config.MAX_DRIFT_RANGE > 0 ? Config.MAX_DRIFT_RANGE : 120;
						int targetX = npc.spawnX() + rnd.nextInt(-maxOffset, maxOffset + 1);
						int targetY = npc.spawnY() + rnd.nextInt(-maxOffset, maxOffset + 1);
						int heading = (int) Math.round(Math.atan2(targetY - npc.y(), targetX - npc.x()) * 10430.378);

						int fromX = npc.x();
						int fromY = npc.y();
						var movePkt = new MoveToLocation(npc.objectId(), targetX, targetY, npc.spawnZ(),
								npc.x(), npc.y(), npc.z());
						npc.moveTo(targetX, targetY, npc.spawnZ(), heading);
						world.updateNpcPosition(npc, fromX, fromY);
						world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, movePkt);
					}
				} else {
					// NPCs de cidade / Guardas / Mestres: 20% de chance de animacao social (respirar, olhar em volta)
					if (rnd.nextInt(100) < 20) {
						int actionId = rnd.nextInt(2, 4); // SocialAction 2 ou 3
						var social = new SocialAction(npc.objectId(), actionId);
						world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, social);
					}
				}
			}
		}
	}

	private void processCombatNpc(NpcInstance npc) {
		if (npc.isDead() || npc.targetPlayerId() == 0) {
			stopCombat(npc);
			return;
		}

		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) {
			returnToSpawn(npc);
			return;
		}

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead()) {
			returnToSpawn(npc);
			return;
		}

		// Se o jogador ou o monstro estiver em zona de paz (ex: vila/cidade), interrompe combate imediatamente
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z()) || zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			returnToSpawn(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dz = player.z() - npc.z();
		double dist = Math.hypot(dx, dy);
		double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);

		// Perda de aggro se o jogador fugir muito longe (> 1500) ou se a diferenca de altura for excessiva (> 350)
		if (dist3d > 1500.0 || Math.abs(dz) > 350.0) {
			returnToSpawn(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		AbstractNpcAI ai = getOrAssignAI(npc);
		ai.processCombat();
	}

	public void onMasterDied(NpcInstance master) {
		if (master == null || !master.hasMinions()) {
			return;
		}
		// Apenas Raid Boss ou Grand Boss limpa/despawna lacaios ao morrer (Retail L2 / L2JDream)
		// Monstros comuns com lacaios (ex: Ketra Prophet) mantem seus lacaios vivos lutando individualmente!
		if (master.template() != null && master.template().isRaidBoss()) {
			for (NpcInstance minion : master.minions()) {
				if (minion != null && !minion.isDead()) {
					stopCombat(minion);
					minion.dead(true);
					minion.currentHp(0);
					var deletePacket = new DeleteObject(minion.objectId());
					world.broadcastAround(minion.x(), minion.y(), GameWorld.VISIBILITY_RADIUS, deletePacket);
					world.removeNpc(minion);
				}
			}
		}
	}

	private SkillTemplate chooseMonsterSkill(NpcInstance npc) {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var holders = npcSkillTable.getSkills(npc.npcId());
		if (holders.isEmpty()) {
			return null;
		}

		// 1. Se HP < 50%, prioriza habilidade de cura se possuir
		if (npc.template() != null && npc.currentHp() < (npc.template().maxHp() * 0.5)) {
			for (var holder : holders) {
				var opt = skillTable.get(holder.skillId(), holder.level());
				if (opt.isPresent()) {
					var sk = opt.get();
					if (sk.isHeal() && npc.currentMp() >= sk.mpConsume()) {
						return sk;
					}
				}
			}
		}

		// 2. ~35% de chance de conjurar habilidade ofensiva, buff ou debuff
		if (ThreadLocalRandom.current().nextInt(100) >= 35) {
			return null;
		}

		for (var holder : holders) {
			var opt = skillTable.get(holder.skillId(), holder.level());
			if (opt.isPresent()) {
				var sk = opt.get();
				if (!sk.isPassive() && !sk.isToggle()) {
					if ((sk.isOffensive() || sk.isPhysicalDamage() || sk.isMagicDamage() || sk.isDebuff() || sk.isBuff())
							&& npc.currentMp() >= sk.mpConsume()) {
						return sk;
					}
				}
			}
		}
		return null;
	}

	public void returnToSpawn(NpcInstance npc) {
		stopCombat(npc);
		if (npc.isDead()) {
			return;
		}
		npc.currentHp(npc.template().maxHp());
		int sx = npc.spawnX();
		int sy = npc.spawnY();
		int sz = npc.spawnZ();
		int sh = npc.spawnHeading();
		if (npc.x() != sx || npc.y() != sy) {
			int fromX = npc.x();
			int fromY = npc.y();
			var movePkt = new MoveToLocation(npc.objectId(), sx, sy, sz, npc.x(), npc.y(), npc.z());
			npc.moveTo(sx, sy, sz, sh);
			world.updateNpcPosition(npc, fromX, fromY);
			world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, movePkt);
			world.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, movePkt);
		}

		// Se for mestre com lacaios, faz os lacaios retornarem tambem
		if (npc.hasMinions()) {
			for (NpcInstance minion : npc.minions()) {
				if (minion != null && !minion.isDead()) {
					returnToSpawn(minion);
				}
			}
		}
	}
}
