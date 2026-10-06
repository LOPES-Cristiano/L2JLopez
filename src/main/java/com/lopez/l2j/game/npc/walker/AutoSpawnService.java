package com.lopez.l2j.game.npc.walker;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico para gerenciamento de NPCs andantes/patrulheiros (NpcWalker) e Spawns Automaticos
 * periodicos (AutoSpawn).
 * Porta de AutoSpawnManager e NpcWalkerAI do legado L2JDream.
 */
@Service
public class AutoSpawnService {

	private static final Logger log = LoggerFactory.getLogger(AutoSpawnService.class);

	private final NpcWalkerRoutesTable routesTable;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final NpcTemplateTable templates;

	private final Map<Integer, NpcWalkerState> activeWalkers = new ConcurrentHashMap<>();
	private final Map<Integer, AutoSpawnTask> registeredAutoSpawns = new ConcurrentHashMap<>();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("autospawn-walker-", 0).factory());

	@Autowired
	public AutoSpawnService(NpcWalkerRoutesTable routesTable,
							GameWorld world,
							ObjectIdFactory objectIds,
							NpcTemplateTable templates) {
		this.routesTable = routesTable;
		this.world = world;
		this.objectIds = objectIds;
		this.templates = templates;
	}

	@PostConstruct
	public void init() {
		scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
		log.info("AutoSpawnService iniciado com sucesso. {} rotas carregadas na tabela.", routesTable.totalRoutes());
	}

	@PreDestroy
	public void stop() {
		scheduler.shutdown();
		activeWalkers.clear();
		for (AutoSpawnTask task : registeredAutoSpawns.values()) {
			despawnAutoSpawn(task);
		}
		registeredAutoSpawns.clear();
	}

	// ==========================================
	//  NPC WALKERS (PATRULHEIROS)
	// ==========================================

	public Optional<NpcInstance> spawnAndRegisterWalker(int npcId, int routeId) {
		List<NpcWalkerNode> route = routesTable.getRoute(routeId);
		if (route.isEmpty()) {
			log.warn("Nenhuma rota encontrada para o routeId {}", routeId);
			return Optional.empty();
		}

		NpcWalkerNode firstNode = route.get(0);
		NpcTemplate tpl = templates.get(npcId)
				.orElseGet(() -> NpcTemplate.fallback(npcId, "Walker " + npcId, "L2Npc"));

		int objectId = objectIds.nextId();
		NpcInstance walkerNpc = new NpcInstance(objectId, tpl, firstNode.moveX(), firstNode.moveY(), firstNode.moveZ(), 0);
		walkerNpc.running(firstNode.running());

		if (world != null) {
			world.addNpc(walkerNpc);
		}

		registerWalker(walkerNpc, routeId);
		return Optional.of(walkerNpc);
	}

	public void registerWalker(NpcInstance npc, int routeId) {
		if (npc == null) {
			return;
		}
		List<NpcWalkerNode> route = routesTable.getRoute(routeId);
		if (route.isEmpty()) {
			return;
		}

		NpcWalkerState state = new NpcWalkerState(npc, route, 0, -1L, true);
		activeWalkers.put(npc.objectId(), state);
	}

	public void unregisterWalker(int npcObjectId) {
		activeWalkers.remove(npcObjectId);
	}

	public int activeWalkersCount() {
		return activeWalkers.size();
	}

	public Optional<NpcWalkerState> getWalkerState(int npcObjectId) {
		return Optional.ofNullable(activeWalkers.get(npcObjectId));
	}

	// ==========================================
	//  AUTO SPAWNS PERIODICOS
	// ==========================================

	public void registerAutoSpawn(int spawnId, int npcId, int x, int y, int z, int heading,
								  long spawnIntervalMs, long despawnDelayMs, String announceText) {
		AutoSpawnTask task = new AutoSpawnTask(spawnId, npcId, x, y, z, heading,
				spawnIntervalMs, despawnDelayMs, announceText, -1L, null);
		registeredAutoSpawns.put(spawnId, task);
	}

	public void cancelAutoSpawn(int spawnId) {
		AutoSpawnTask task = registeredAutoSpawns.remove(spawnId);
		if (task != null) {
			despawnAutoSpawn(task);
		}
	}

	public int autoSpawnsCount() {
		return registeredAutoSpawns.size();
	}

	// ==========================================
	//  MOTOR DE PROCESSAMENTO (TICK)
	// ==========================================

	public void tick() {
		step(System.currentTimeMillis());
	}

	public void step(long nowMillis) {
		stepWalkers(nowMillis);
		stepAutoSpawns(nowMillis);
	}

	private void stepWalkers(long nowMillis) {
		for (NpcWalkerState state : activeWalkers.values()) {
			try {
				if (state.npc.isDead()) {
					continue;
				}

				if (state.nextActionTime < 0) {
					state.nextActionTime = nowMillis + state.route.get(0).delay() * 1000L;
				}

				if (nowMillis < state.nextActionTime) {
					continue;
				}

				List<NpcWalkerNode> route = state.route;
				if (route.isEmpty()) {
					continue;
				}

				// Proximo waypoint
				int nextIdx = (state.currentIndex + 1) % route.size();
				NpcWalkerNode targetNode = route.get(nextIdx);

				int fromX = state.npc.x();
				int fromY = state.npc.y();
				int fromZ = state.npc.z();

				// Move o NPC
				state.npc.running(targetNode.running());
				state.npc.moveTo(targetNode.moveX(), targetNode.moveY(), targetNode.moveZ(), 0);
				if (world != null) {
					world.updateNpcPosition(state.npc, fromX, fromY);
				}

				// Fala do NPC caso configurada no no
				if (targetNode.chatText() != null && !targetNode.chatText().isBlank()) {
					if (world != null) {
						var say = new CreatureSay(state.npc.objectId(), CreatureSay.ALL, state.npc.name(), targetNode.chatText());
						world.broadcast(say, p -> isInsideRadius(p.x(), p.y(), state.npc.x(), state.npc.y(), 1250));
					}
				}

				// Notifica movimento aos jogadores proximos
				if (world != null) {
					var move = new MoveToLocation(state.npc.objectId(),
							targetNode.moveX(), targetNode.moveY(), targetNode.moveZ(), fromX, fromY, fromZ);
					world.broadcast(move, p -> isInsideRadius(p.x(), p.y(), state.npc.x(), state.npc.y(), GameWorld.VISIBILITY_RADIUS));
				}

				state.currentIndex = nextIdx;
				// Proxima acao ocorre apos o delay configurado para o no
				state.nextActionTime = nowMillis + Math.max(1, targetNode.delay()) * 1000L;

			} catch (Exception e) {
				log.warn("Erro ao avancar NpcWalker {}: {}", state.npc.objectId(), e.getMessage());
			}
		}
	}

	private void stepAutoSpawns(long nowMillis) {
		for (AutoSpawnTask task : registeredAutoSpawns.values()) {
			try {
				if (task.nextSpawnTime < 0) {
					task.nextSpawnTime = nowMillis + task.spawnIntervalMs;
				}
				if (task.spawnedNpc == null && nowMillis >= task.nextSpawnTime) {
					// Hora de spawnar
					NpcTemplate tpl = templates.get(task.npcId)
							.orElseGet(() -> NpcTemplate.fallback(task.npcId, "AutoSpawn " + task.npcId, "L2Npc"));
					int objectId = objectIds.nextId();
					NpcInstance npc = new NpcInstance(objectId, tpl, task.x, task.y, task.z, task.heading);
					if (world != null) {
						world.addNpc(npc);
					}
					task.spawnedNpc = npc;
					task.despawnTime = nowMillis + task.despawnDelayMs;

					// Anuncio global ou local caso configurado
					if (task.announceText != null && !task.announceText.isBlank() && world != null) {
						world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Server", task.announceText), p -> true);
					}

				} else if (task.spawnedNpc != null && nowMillis >= task.despawnTime) {
					// Hora de despawnar
					despawnAutoSpawn(task);
					task.nextSpawnTime = nowMillis + task.spawnIntervalMs;
				}
			} catch (Exception e) {
				log.warn("Erro no ciclo de AutoSpawn {}: {}", task.spawnId, e.getMessage());
			}
		}
	}

	private void despawnAutoSpawn(AutoSpawnTask task) {
		if (task.spawnedNpc != null) {
			if (world != null) {
				world.removeNpc(task.spawnedNpc);
			}
			task.spawnedNpc = null;
		}
	}

	// ==========================================
	//  CLASSES DE ESTADO
	// ==========================================

	public static final class NpcWalkerState {
		private final NpcInstance npc;
		private final List<NpcWalkerNode> route;
		private int currentIndex;
		private long nextActionTime;
		private boolean waitingAtNode;

		public NpcWalkerState(NpcInstance npc, List<NpcWalkerNode> route, int currentIndex, long nextActionTime, boolean waitingAtNode) {
			this.npc = npc;
			this.route = route;
			this.currentIndex = currentIndex;
			this.nextActionTime = nextActionTime;
			this.waitingAtNode = waitingAtNode;
		}

		public NpcInstance npc() { return npc; }
		public List<NpcWalkerNode> route() { return Collections.unmodifiableList(route); }
		public int currentIndex() { return currentIndex; }
		public long nextActionTime() { return nextActionTime; }
		public boolean isWaitingAtNode() { return waitingAtNode; }
	}

	private static final class AutoSpawnTask {
		final int spawnId;
		final int npcId;
		final int x;
		final int y;
		final int z;
		final int heading;
		final long spawnIntervalMs;
		final long despawnDelayMs;
		final String announceText;
		long nextSpawnTime;
		long despawnTime;
		NpcInstance spawnedNpc;

		AutoSpawnTask(int spawnId, int npcId, int x, int y, int z, int heading,
					  long spawnIntervalMs, long despawnDelayMs, String announceText,
					  long nextSpawnTime, NpcInstance spawnedNpc) {
			this.spawnId = spawnId;
			this.npcId = npcId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.heading = heading;
			this.spawnIntervalMs = spawnIntervalMs;
			this.despawnDelayMs = despawnDelayMs;
			this.announceText = announceText;
			this.nextSpawnTime = nextSpawnTime;
			this.spawnedNpc = spawnedNpc;
		}
	}

	private static boolean isInsideRadius(int x1, int y1, int x2, int y2, int radius) {
		long dx = x1 - x2;
		long dy = y1 - y2;
		return dx * dx + dy * dy <= (long) radius * radius;
	}
}
