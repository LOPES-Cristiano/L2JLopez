package com.lopez.l2j.game.ai;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillCanceld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
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
 * - Perseguicao e combate contra jogadores por arquetipo (Fighter, Ranger, Mystic, Priest, Guard)
 * - Faction Social Call (V.07A) para aliados da mesma faccao ou mesmo npcId em raio de 400u
 * - Deteccao de jogadores por monstros agressivos (aggroRange) e Guardas contra PKs (karma > 0)
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

	private boolean isGuardNpc(NpcInstance npc) {
		if (npc == null || npc.template() == null) {
			return false;
		}
		String type = npc.template().type();
		String name = npc.template().name() != null ? npc.template().name().toLowerCase(java.util.Locale.ROOT) : "";
		return "L2Guard".equalsIgnoreCase(type) || npc.template().isGuard() || name.contains("guard") || name.contains("sentry");
	}

	private boolean hasLoS(int x1, int y1, int z1, int x2, int y2, int z2) {
		if (combatService == null || combatService.geoEngine() == null) {
			return Math.abs(z2 - z1) <= 250;
		}
		return combatService.canSeeTarget(x1, y1, z1, x2, y2, z2);
	}

	private boolean hasLoS(NpcInstance npc, com.lopez.l2j.game.model.PlayerCharacter character) {
		if (npc == null || character == null) {
			return false;
		}
		if (npc.instanceId() != character.instanceId()) {
			return false;
		}
		if (combatService == null || combatService.geoEngine() == null) {
			return Math.abs(character.z() - npc.z()) <= 250;
		}
		return combatService.canSeeTarget(npc, character);
	}

	/**
	 * Registra ou atualiza um monstro/guarda em estado de combate perseguindo/atacando um jogador.
	 */
	public void startCombat(NpcInstance npc, int targetPlayerId) {
		if (npc == null || npc.isDead()) {
			return;
		}
		var playerOpt = world.player(targetPlayerId);
		if (playerOpt.isPresent()) {
			var p = playerOpt.get();
			var ch = p.character();
			boolean guardHuntingPk = isGuardNpc(npc) && ch != null && ch.karma() > 0;
			if (!guardHuntingPk && zones != null
					&& (zones.isInsidePeace(p.x(), p.y(), p.z()) || zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
				return;
			}
		}
		npc.targetPlayerId(targetPlayerId);
		npc.inCombat(true);

		var npcStart = new com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStart(npc.objectId());
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npcStart);

		world.player(targetPlayerId).ifPresent(p -> p.onAttacked(npc.objectId()));

		npc.lastAttackTime(0L);

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

		// 3. Faction Social Call (V.07A): Notifica monstros aliados da mesma Faccao ou mesmo npcId em raio de 400u
		notifyFactionCall(npc, targetPlayerId);
	}

	private void notifyFactionCall(NpcInstance npc, int targetPlayerId) {
		if (npc.template() == null) {
			return;
		}
		String factionId = npc.template().factionId();
		boolean hasFaction = factionId != null && !factionId.isBlank() && !"null".equalsIgnoreCase(factionId);
		int factionRange = npc.template().factionRange();
		if (!hasFaction || factionRange <= 0) {
			return;
		}

		var nearby = world.findNpcsAround(npc.x(), npc.y(), factionRange);
		for (NpcInstance ally : nearby) {
			if (ally == npc || ally.isDead() || ally.inCombat() || !ally.isMonster()) {
				continue;
			}
			if (ally.instanceId() != npc.instanceId()) {
				continue;
			}
			// Aliados da faccao so respondem ao chamado se estiverem no mesmo plano vertical (nao em andares/cavernas)
			if (Math.abs(ally.z() - npc.z()) > 150) {
				continue;
			}
			double dist2d = Math.hypot(ally.x() - npc.x(), ally.y() - npc.y());
			boolean sameFaction = ally.template() != null
					&& factionId.equalsIgnoreCase(ally.template().factionId()) && dist2d <= factionRange;

			if (sameFaction) {
				if (!hasLoS(ally.x(), ally.y(), ally.z(), npc.x(), npc.y(), npc.z())) {
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
			long respawnDelayMs = resolveRespawnDelayMs(npc);
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
			}, respawnDelayMs, TimeUnit.MILLISECONDS);

		}, DECAY_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	public long resolveRespawnDelayMs(NpcInstance npc) {
		if (npc != null && npc.isRaidMinion() && Config.RAID_MINION_RESPAWN_TIME > 0) {
			return Config.RAID_MINION_RESPAWN_TIME;
		}
		if (npc != null && npc.isMinion() && Config.ALL_MINIONS_RESPAWN_INTERVAL > 0) {
			return Config.ALL_MINIONS_RESPAWN_INTERVAL;
		}
		return RESPAWN_DELAY_MS;
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
	 * Executa um ciclo da IA para monstros em combate, deteccao de aggro, guardas contra PKs e movimentacao.
	 */
	public void tick() {
		tickCount++;

		// 1. Checagem de aggro e Guardas contra PKs
		if (tickCount == 1 || tickCount % 2 == 0) {
			checkAggroAroundPlayers();
		}

		// 2. Processa monstros e guardas em combate
		for (NpcInstance npc : activeCombatNpcs) {
			try {
				processCombatNpc(npc);
			} catch (Exception e) {
				log.warn("Erro ao processar combate do monstro {}: {}", npc.name(), e.getMessage());
			}
		}

		// 3. Suporte de Priests ociosos proximos a aliados feridos
		processIdlePriests();

		// 4. Roaming aleatorio de monstros pacificos / ociosos (a cada 10 segundos = 20 ticks)
		if (tickCount % 20 == 0) {
			roamIdleMonsters();
		}
	}

	private void processIdlePriests() {
		for (NpcInstance npc : world.npcs()) {
			if (npc == null || npc.isDead() || npc.inCombat() || npc.isCasting()) {
				continue;
			}
			AbstractNpcAI ai = getOrAssignAI(npc);
			if (ai instanceof PriestAI priestAI) {
				NpcInstance wounded = priestAI.findWoundedAlly();
				if (wounded != null) {
					priestAI.processCombat();
				}
			}
		}
	}

	public boolean isMobAggroAllowedInPeaceZone() {
		return Config.ALT_MOB_AGGRO_IN_PEACE_ZONE;
	}

	private void checkAggroAroundPlayers() {
		for (var player : world.players()) {
			var character = player.character();
			if (character == null || character.isDead() || character.isGm()) {
				continue;
			}
			boolean inPeaceZone = zones != null && zones.isInsidePeace(player.x(), player.y(), player.z());
			var nearbyNpcs = world.findNpcsAround(player.x(), player.y(), 1000);
			for (var npc : nearbyNpcs) {
				if (npc.isDead() || npc.inCombat()) {
					continue;
				}
				if (npc.instanceId() != character.instanceId()) {
					continue;
				}
				// Diferenca vertical: monstros subterraneos ou em andares diferentes nao agram na superficie
				int deltaZ = Math.abs(npc.z() - player.z());
				if (deltaZ > 150) {
					continue;
				}

				// Guardas atacam automaticamente jogadores com karma > 0 (PKs) no raio de visao
				if (isGuardNpc(npc)) {
					if (character.karma() > 0) {
						int guardRange = (npc.template() != null && npc.template().aggroRange() > 0)
								? npc.template().aggroRange() : 1000;
						double dist3d = Math.sqrt(Math.pow(npc.x() - player.x(), 2) + Math.pow(npc.y() - player.y(), 2) + Math.pow(deltaZ, 2));
						if (dist3d <= guardRange && hasLoS(npc, character)) {
							startCombat(npc, player.objectId());
						}
					}
					continue;
				}

				if ((inPeaceZone && (!Config.ALT_MOB_AGGRO_IN_PEACE_ZONE || zones != null)) || !npc.isMonster()) {
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
						if (!hasLoS(npc, character)) {
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
		int minMonsterAnim = Math.max(0, Config.MIN_MONSTER_ANIMATION);
		int maxMonsterAnim = Math.max(minMonsterAnim, Config.MAX_MONSTER_ANIMATION);
		int minNpcAnim = Math.max(0, Config.MIN_NPC_ANIMATION);
		int maxNpcAnim = Math.max(minNpcAnim, Config.MAX_NPC_ANIMATION);
		int minWalkAnim = Math.max(0, Config.MIN_NPC_WALK_ANIMATION);
		int maxWalkAnim = Math.max(minWalkAnim, Config.MAX_NPC_WALK_ANIMATION);

		for (var player : world.players()) {
			var nearbyNpcs = world.findNpcsAround(player.x(), player.y(), 1200);
			for (var npc : nearbyNpcs) {
				if (npc.isDead() || npc.inCombat()) {
					continue;
				}
				if (npc.isMonster()) {
					if (maxMonsterAnim <= 0 || maxWalkAnim <= 0) {
						continue;
					}
					int chance = Math.clamp(100 - minMonsterAnim, 10, 50);
					if (rnd.nextInt(100) < chance) {
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
					if (maxNpcAnim <= 0) {
						continue;
					}
					int chance = Math.clamp(100 - minNpcAnim, 10, 40);
					// NPCs de cidade / Guardas / Mestres: animacao social (respirar, olhar em volta)
					if (rnd.nextInt(100) < chance) {
						int actionId = rnd.nextInt(2, 4); // SocialAction 2 ou 3
						var social = new SocialAction(npc.objectId(), actionId);
						world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, social);
					}
				}
			}
		}
	}

	public boolean tryRollHotSpringDebuff(NpcInstance npc) {
		if (npc == null || Config.HOT_SPRING_DEBUFF_CHANCE <= 0) {
			return false;
		}
		int id = npc.npcId();
		if (id >= 21314 && id <= 21323) {
			return ThreadLocalRandom.current().nextInt(100) < Config.HOT_SPRING_DEBUFF_CHANCE;
		}
		return false;
	}

	public boolean tryRollFogMobClone(NpcInstance npc) {
		if (npc == null || Config.FOG_MOBS_CLONE_CHANCE <= 0) {
			return false;
		}
		int id = npc.npcId();
		if (id >= 21376 && id <= 21437) {
			return ThreadLocalRandom.current().nextInt(100) < Config.FOG_MOBS_CLONE_CHANCE;
		}
		return false;
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

		boolean guardHuntingPk = isGuardNpc(npc) && character.karma() > 0;
		// Se o jogador ou o monstro estiver em zona de paz (ex: vila/cidade), interrompe combate imediatamente (exceto Guardas contra PK)
		if (!guardHuntingPk && zones != null
				&& (zones.isInsidePeace(player.x(), player.y(), player.z()) || zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			returnToSpawn(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dz = player.z() - npc.z();
		double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);

		// Perda de aggro se o jogador fugir muito longe (> 1500) ou se a diferenca de altura for excessiva (> 350)
		if (dist3d > 1500.0 || Math.abs(dz) > 350.0) {
			returnToSpawn(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		tryRollHotSpringDebuff(npc);
		tryRollFogMobClone(npc);

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
