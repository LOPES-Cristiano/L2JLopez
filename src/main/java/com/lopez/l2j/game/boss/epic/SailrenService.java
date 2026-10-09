package com.lopez.l2j.game.boss.epic;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador do Covil de Sailren na Ilha Primeval (SailrenManager do L2JDream / L2JLucera2).
 * Controla entrada com Gazkh (8784) via Estatua de Shilen (32109), ondas de monstros
 * pre-requisitos (Velociraptor -> Pterosaur -> Trex) ate o despertar de Sailren (29065) e saida.
 */
@Service
public class SailrenService {

	private static final Logger log = LoggerFactory.getLogger(SailrenService.class);

	public static final int STATUE = 32109;
	public static final int GAZKH = 8784;
	public static final int SAILREN = 29065;
	public static final int VELOCIRAPTOR = 22196;
	public static final int PTEROSAUR = 22199;
	public static final int TREX = 22218;
	public static final int TELEPORT_CUBE = 32110;

	// Coordenadas do Ninho de Sailren
	public static final int NEST_X = 27333;
	public static final int NEST_Y = -6835;
	public static final int NEST_Z = -1970;

	public static final int EXIT_X = 10468;
	public static final int EXIT_Y = -24569;
	public static final int EXIT_Z = -3645;

	private BossStatus status = BossStatus.NOTSPAWN;
	private final List<Integer> playersInside = new CopyOnWriteArrayList<>();
	private NpcInstance activeSailren;
	private NpcInstance exitCube;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("SailrenScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public SailrenService(
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds) {
		this.grandBossManager = grandBossManager;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
	}

	@PostConstruct
	public void init() {
		if (grandBossManager != null) {
			this.status = grandBossManager.getStatus(GrandBossManager.SAILREN);
		}
	}

	public BossStatus getStatus() {
		return status;
	}

	public List<Integer> getPlayersInside() {
		return Collections.unmodifiableList(playersInside);
	}

	public boolean canEnter(PlayerCharacter player) {
		if (player == null || player.isDead()) {
			return false;
		}
		if (status == BossStatus.INTERVAL || status == BossStatus.DEAD) {
			return false;
		}
		if (Config.QUEST_REQUIRED_FOR_BOSS) {
			Inventory inv = player.inventory();
			if (inv == null || inv.byItemId(GAZKH).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public boolean enterNest(PlayerCharacter player) {
		if (!canEnter(player)) {
			return false;
		}

		if (Config.QUEST_REQUIRED_FOR_BOSS && player.inventory() != null) {
			player.inventory().byItemId(GAZKH).ifPresent(item -> {
				if (item.count() <= 1) {
					player.inventory().remove(item);
				} else {
					item.count(item.count() - 1);
				}
			});
		}

		player.x(NEST_X);
		player.y(NEST_Y);
		player.z(NEST_Z);
		playersInside.add(player.objectId());

		if (status == BossStatus.NOTSPAWN) {
			status = BossStatus.ALIVE;
			if (grandBossManager != null) {
				grandBossManager.setStatus(GrandBossManager.SAILREN, BossStatus.ALIVE);
			}
			startWaveSequence();
		}

		return true;
	}

	private void startWaveSequence() {
		log.info("Sailren: Sequencia de ondas de dinossauros iniciada!");
		// Onda 1: Velociraptor e Pterosaur (apos 10 segundos)
		scheduler.schedule(() -> spawnWaveMob(VELOCIRAPTOR, "Velociraptor"), 10, TimeUnit.SECONDS);
		scheduler.schedule(() -> spawnWaveMob(PTEROSAUR, "Pterosaur"), 25, TimeUnit.SECONDS);

		// Onda 2: Trex (apos 45 segundos)
		scheduler.schedule(() -> spawnWaveMob(TREX, "Tyrannosaurus"), 45, TimeUnit.SECONDS);

		// Onda 3: Sailren (apos 75 segundos)
		scheduler.schedule(this::spawnSailren, 75, TimeUnit.SECONDS);
	}

	private void spawnWaveMob(int npcId, String mobName) {
		if (templates == null || world == null || objectIds == null) return;
		NpcTemplate tpl = templates.get(npcId).orElseGet(() -> NpcTemplate.fallback(npcId, mobName, "L2Monster"));
		NpcInstance mob = new NpcInstance(objectIds.nextId(), tpl, NEST_X + 150, NEST_Y + 150, NEST_Z, 0);
		world.addNpc(mob);
		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Primeval Isle",
				"A roar echoes across the nest: " + mobName + " approaches!"), p -> true);
	}

	public synchronized void spawnSailren() {
		if (templates == null || world == null || objectIds == null) return;
		NpcTemplate tpl = templates.get(SAILREN).orElseGet(() -> NpcTemplate.fallback(SAILREN, "Sailren", "L2GrandBoss"));
		activeSailren = new NpcInstance(objectIds.nextId(), tpl, NEST_X, NEST_Y, NEST_Z, 0);
		world.addNpc(activeSailren);

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.SAILREN, BossStatus.ALIVE);
		}

		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Sailren",
				"The ancient raptor Sailren has appeared in the nest!"), p -> true);
		log.info("Sailren gerado no ninho!");
	}

	public synchronized void onSailrenKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;
		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.SAILREN);
		}

		spawnExitCube();

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Sailren",
					"Sailren has been defeated! The Teleportation Cube has appeared!"), p -> true);
		}
	}

	private void spawnExitCube() {
		if (templates == null || world == null || objectIds == null) return;
		NpcTemplate cubeTpl = templates.get(TELEPORT_CUBE)
				.orElseGet(() -> NpcTemplate.fallback(TELEPORT_CUBE, "Teleportation Cube", "L2Npc"));
		exitCube = new NpcInstance(objectIds.nextId(), cubeTpl, NEST_X, NEST_Y, NEST_Z, 0);
		world.addNpc(exitCube);

		scheduler.schedule(this::clearNest, 15L * 60L, TimeUnit.SECONDS);
	}

	public void clearNest() {
		if (exitCube != null && world != null) {
			world.removeNpc(exitCube);
			exitCube = null;
		}
		if (activeSailren != null && world != null) {
			world.removeNpc(activeSailren);
			activeSailren = null;
		}
		for (int playerId : playersInside) {
			if (world != null) {
				world.player(playerId).ifPresent(p -> {
					PlayerCharacter pc = p.character();
					if (pc != null) {
						pc.x(EXIT_X);
						pc.y(EXIT_Y);
						pc.z(EXIT_Z);
					}
				});
			}
		}
		playersInside.clear();
	}

	public Optional<NpcInstance> activeSailren() {
		return Optional.ofNullable(activeSailren);
	}
}
