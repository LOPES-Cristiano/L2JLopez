package com.lopez.l2j.game.instance;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.world.GameWorld;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico central do motor de instancias (Reflection Engine) do L2JLopez.
 * Gerencia ciclo de vida, isolamento espacial por instanceId no GameWorld,
 * teleporte de entrada/saida e limpeza automatica de NPCs e jogadores.
 */
@Service
public class InstanceService {

	private static final Logger log = LoggerFactory.getLogger(InstanceService.class);

	// Coordenadas default de saida: Praca central de Giran
	public static final int DEFAULT_EXIT_X = 82698;
	public static final int DEFAULT_EXIT_Y = 148638;
	public static final int DEFAULT_EXIT_Z = -3473;

	private final Map<Integer, InstanceWorld> instances = new ConcurrentHashMap<>();
	private final Map<Integer, int[]> playerReturnLocations = new ConcurrentHashMap<>();
	private final AtomicInteger instanceIdGenerator = new AtomicInteger(1000);

	private final GameWorld gameWorld;
	private ScheduledExecutorService cleanupScheduler;

	public InstanceService() {
		this(null);
	}

	@Autowired
	public InstanceService(@Autowired(required = false) GameWorld gameWorld) {
		this.gameWorld = gameWorld != null ? gameWorld : GameWorld.getInstance();
	}

	@PostConstruct
	public void init() {
		if (cleanupScheduler == null) {
			cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "InstanceService-Cleanup");
				t.setDaemon(true);
				return t;
			});
			cleanupScheduler.scheduleAtFixedRate(this::checkExpiredInstances, 60L, 60L, TimeUnit.SECONDS);
		}
	}

	@PreDestroy
	public void shutdown() {
		if (cleanupScheduler != null) {
			cleanupScheduler.shutdownNow();
			cleanupScheduler = null;
		}
	}

	/**
	 * Verifica e destroi automaticamente todas as instancias cujo tempo limite (durationMillis) expirou.
	 */
	public void checkExpiredInstances() {
		List<Integer> expiredIds = new ArrayList<>();
		for (Map.Entry<Integer, InstanceWorld> entry : instances.entrySet()) {
			if (entry.getValue().isExpired()) {
				expiredIds.add(entry.getKey());
			}
		}
		for (int instanceId : expiredIds) {
			log.info("InstanceService: Instancia #{} expirou por tempo limite. Destruindo...", instanceId);
			destroyInstance(instanceId);
		}
	}

	public int createInstance(int templateId, int durationMinutes) {
		return createInstance(templateId, durationMinutes, DEFAULT_EXIT_X, DEFAULT_EXIT_Y, DEFAULT_EXIT_Z);
	}

	public int createInstance(int templateId, int durationMinutes, int exitX, int exitY, int exitZ) {
		int instanceId = instanceIdGenerator.getAndIncrement();
		InstanceWorld world = new InstanceWorld(instanceId, templateId, durationMinutes, exitX, exitY, exitZ);
		instances.put(instanceId, world);
		log.info("InstanceService: Instancia #{} criada (templateId={}, duration={}m)", instanceId, templateId, durationMinutes);
		return instanceId;
	}

	public InstanceWorld getInstance(int instanceId) {
		return instances.get(instanceId);
	}

	public void enterInstance(PlayerCharacter player, int instanceId, int x, int y, int z) {
		if (player == null) return;
		InstanceWorld instance = instances.get(instanceId);
		if (instance == null) {
			log.warn("InstanceService: Tentativa de entrar em instancia inexistente #{} pelo jogador {}", instanceId, player.name());
			return;
		}

		playerReturnLocations.putIfAbsent(player.objectId(), new int[]{player.x(), player.y(), player.z()});

		instance.addPlayer(player.objectId());
		player.instanceId(instanceId);

		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();
		if (world != null) {
			world.teleportPlayer(player, x, y, z, instanceId);
		} else {
			player.x(x);
			player.y(y);
			player.z(z);
		}
		log.debug("InstanceService: Jogador {} entrou na instancia #{} em ({},{},{})", player.name(), instanceId, x, y, z);
	}

	public void exitInstance(PlayerCharacter player) {
		if (player == null) return;
		int currentInstanceId = player.instanceId();
		InstanceWorld instance = instances.get(currentInstanceId);
		if (instance != null) {
			instance.removePlayer(player.objectId());
		}

		int[] returnLoc = playerReturnLocations.remove(player.objectId());
		int destX = returnLoc != null ? returnLoc[0] : (instance != null ? instance.exitX() : DEFAULT_EXIT_X);
		int destY = returnLoc != null ? returnLoc[1] : (instance != null ? instance.exitY() : DEFAULT_EXIT_Y);
		int destZ = returnLoc != null ? returnLoc[2] : (instance != null ? instance.exitZ() : DEFAULT_EXIT_Z);

		player.instanceId(0);
		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();
		if (world != null) {
			world.teleportPlayer(player, destX, destY, destZ, 0);
		} else {
			player.x(destX);
			player.y(destY);
			player.z(destZ);
		}
		log.debug("InstanceService: Jogador {} saiu da instancia #{} e retornou para ({},{},{}) no mundo 0",
				player.name(), currentInstanceId, destX, destY, destZ);
	}

	public void destroyInstance(int instanceId) {
		InstanceWorld instance = instances.remove(instanceId);
		if (instance == null) return;

		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();

		if (world != null) {
			for (Integer pId : instance.players()) {
				var op = world.player(pId);
				if (op.isPresent() && op.get().character() != null) {
					exitInstance(op.get().character());
				}
			}
			world.removeNpcsByInstance(instanceId);
		}

		log.info("InstanceService: Instancia #{} destruida com sucesso. NPCs e jogadores purgados.", instanceId);
	}

	public List<PlayerCharacter> getPlayersInInstance(int instanceId) {
		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();
		if (world == null) return List.of();
		List<PlayerCharacter> list = new ArrayList<>();
		for (var op : world.players()) {
			if (op.instanceId() == instanceId && op.character() != null) {
				list.add(op.character());
			}
		}
		return Collections.unmodifiableList(list);
	}

	public List<NpcInstance> getNpcsInInstance(int instanceId) {
		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();
		if (world == null) return List.of();
		List<NpcInstance> list = new ArrayList<>();
		for (NpcInstance npc : world.npcs()) {
			if (npc.instanceId() == instanceId) {
				list.add(npc.instanceId() == instanceId ? npc : null);
			}
		}
		return Collections.unmodifiableList(list);
	}

	public Map<Integer, InstanceWorld> getActiveInstances() {
		return Collections.unmodifiableMap(instances);
	}
}
