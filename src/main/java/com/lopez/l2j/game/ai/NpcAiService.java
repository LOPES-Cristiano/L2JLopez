package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
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

	private final Set<NpcInstance> activeCombatNpcs = ConcurrentHashMap.newKeySet();
	private ScheduledExecutorService scheduler;
	private long tickCount = 0;

	public NpcAiService(GameWorld world, CombatService combatService, CharTemplateTable charTemplates) {
		this.world = world;
		this.combatService = combatService;
		this.charTemplates = charTemplates;
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
		npc.targetPlayerId(targetPlayerId);
		npc.inCombat(true);

		int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
		long cooldownMs = 500_000L / pAtkSpd;
		npc.lastAttackTime(System.currentTimeMillis() - cooldownMs + 600);

		activeCombatNpcs.add(npc);
	}

	/**
	 * Remove um monstro do combate (por morte ou perda de aggro).
	 */
	public void stopCombat(NpcInstance npc) {
		if (npc != null) {
			npc.inCombat(false);
			npc.targetPlayerId(0);
			activeCombatNpcs.remove(npc);
		}
	}

	/**
	 * Agenda o sumico do corpo (decay) e posterior renascimento (respawn) do monstro derrotado.
	 */
	public void scheduleDecayAndRespawn(NpcInstance npc) {
		stopCombat(npc);
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

			// 2. Respawn: Apos o tempo de renascimento, revive com HP total no spawn original
			scheduler.schedule(() -> {
				try {
					npc.dead(false);
					npc.currentHp(npc.template().maxHp());
					npc.currentMp(npc.template().maxMp());
					npc.targetPlayerId(0);
					npc.inCombat(false);
					npc.moveTo(npc.spawnX(), npc.spawnY(), npc.spawnZ(), npc.spawnHeading());

					world.addNpc(npc);
					var npcInfo = new NpcInfo(npc);
					world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npcInfo);
					log.debug("Monstro {} renasceu em ({}, {}, {})", npc.name(), npc.x(), npc.y(), npc.z());
				} catch (Exception e) {
					log.warn("Erro ao executar respawn do monstro {}: {}", npc.name(), e.getMessage());
				}
			}, RESPAWN_DELAY_MS, TimeUnit.MILLISECONDS);

		}, DECAY_DELAY_MS, TimeUnit.MILLISECONDS);
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
			var nearbyNpcs = world.findNpcsAround(player.x(), player.y(), 1000);
			for (var npc : nearbyNpcs) {
				if (npc.isDead() || npc.inCombat() || !npc.isMonster()) {
					continue;
				}
				int aggroRange = npc.template().aggroRange();
				if (aggroRange > 0) {
					double dist = Math.hypot(npc.x() - player.x(), npc.y() - player.y());
					if (dist <= aggroRange) {
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
						int maxOffset = 150;
						int targetX = npc.spawnX() + rnd.nextInt(-maxOffset, maxOffset + 1);
						int targetY = npc.spawnY() + rnd.nextInt(-maxOffset, maxOffset + 1);
						int heading = (int) Math.round(Math.atan2(targetY - npc.y(), targetX - npc.x()) * 10430.378);

						var movePkt = new MoveToLocation(npc.objectId(), targetX, targetY, npc.spawnZ(),
								npc.x(), npc.y(), npc.z());
						npc.moveTo(targetX, targetY, npc.spawnZ(), heading);
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

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dist = Math.hypot(dx, dy);

		// Perda de aggro se o jogador fugir muito longe (> 1500)
		if (dist > 1500.0) {
			returnToSpawn(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		int attackRange = Math.max(40, npc.template().attackRange());
		int reach = attackRange + 30; // margem de contato fisico

		if (npc.isDisabled()) {
			return;
		}

		if (dist > reach && npc.isRooted()) {
			return;
		}

		if (dist > reach) {
			npc.running(true);
			double runSpeed = Math.max(60, npc.template().runSpd());
			double step = Math.min(dist - attackRange, runSpeed * 0.5);
			int newX = (int) Math.round(npc.x() + (dx / dist) * step);
			int newY = (int) Math.round(npc.y() + (dy / dist) * step);
			int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
			npc.moveTo(newX, newY, player.z(), heading);

			var movePawn = new MoveToPawn(npc.objectId(), player.objectId(), attackRange, npc.x(), npc.y(), npc.z());
			player.send(movePawn);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, movePawn, false);
		} else {
			// No alcance de ataque
			int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
			long cooldownMs = 500_000L / pAtkSpd;
			long now = System.currentTimeMillis();

			if (now - npc.lastAttackTime() >= cooldownMs) {
				npc.lastAttackTime(now);
				var template = charTemplates.get(character.classId()).orElse(null);
				if (template != null) {
					var counter = combatService.attackPlayer(npc, character, template);
					if (counter != null) {
						var npcAtk = new Attack(npc.objectId(), character.objectId(), counter.damage(), counter.flags(),
								npc.x(), npc.y(), npc.z());
						player.send(npcAtk);
						world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, npcAtk, false);

						if (counter.damage() > 0) {
							player.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
									new SystemMessage.NpcName(npc.npcId()),
									new SystemMessage.Number(counter.damage())));
						}

						player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
						player.send(new UserInfo(character, template));

						if (character.isDead()) {
							returnToSpawn(npc);
							var die = new Die(character.objectId(), true);
							player.send(die);
							world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, die, false);
						}
					}
				}
			}
		}
	}

	private void returnToSpawn(NpcInstance npc) {
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
			var movePkt = new MoveToLocation(npc.objectId(), sx, sy, sz, npc.x(), npc.y(), npc.z());
			npc.moveTo(sx, sy, sz, sh);
			world.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, movePkt);
		}
	}
}
