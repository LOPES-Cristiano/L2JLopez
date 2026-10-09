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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador do 14o Andar da Torre da Insolencia (BaiumManager do L2JDream / L2JLucera2).
 * Controla entrada com Blooded Fabric (4295) via Vortice Angelical (31862), despertar da estatua
 * de pedra de Baium (29025), invocacao dos 5 Arcanjos (29021), combate de Baium (29020),
 * selamento da sala e cubo de saida (29055).
 */
@Service
public class BaiumService {

	private static final Logger log = LoggerFactory.getLogger(BaiumService.class);

	public static final int ANGELIC_VORTEX = 31862;
	public static final int BLOODED_FABRIC = 4295;

	public static final int BAIUM_STATUE = 29025;
	public static final int BAIUM_NPC_ID = 29020;
	public static final int ARCHANGEL = 29021;
	public static final int TELEPORT_CUBE = 29055;
	public static final int RING_OF_BAIUM = 6658;

	// Coordenadas do 14o andar da Torre da Insolencia
	public static final int FLOOR14_X = 116033;
	public static final int FLOOR14_Y = 17447;
	public static final int FLOOR14_Z = 10107;
	public static final int BAIUM_HEADING = 40188;

	// Coordenadas de saida (13o andar)
	public static final int EXIT_X = 114000;
	public static final int EXIT_Y = 13400;
	public static final int EXIT_Z = 10030;

	private BossStatus status = BossStatus.NOTSPAWN;
	private final List<Integer> playersInside = new CopyOnWriteArrayList<>();
	private final List<NpcInstance> archangels = new CopyOnWriteArrayList<>();
	private NpcInstance baiumStatue;
	private NpcInstance activeBaium;
	private NpcInstance exitCube;
	private ScheduledFuture<?> sleepCheckTask;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("BaiumScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public BaiumService(
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
			this.status = grandBossManager.getStatus(GrandBossManager.BAIUM);
		}
		if (this.status == BossStatus.NOTSPAWN || this.status == BossStatus.ALIVE) {
			spawnStatue();
		}
	}

	public BossStatus getStatus() {
		return status;
	}

	public void setStatus(BossStatus status) {
		this.status = status;
	}

	public void setBaiumStatue(NpcInstance baiumStatue) {
		this.baiumStatue = baiumStatue;
	}

	public List<Integer> getPlayersInside() {
		return Collections.unmodifiableList(playersInside);
	}

	public boolean canEnter(PlayerCharacter player) {
		if (player == null || player.isDead()) {
			return false;
		}
		// Quando o combate contra Baium comecou (FIGHTING), ninguem mais entra!
		if (status == BossStatus.FIGHTING || status == BossStatus.INTERVAL || status == BossStatus.DEAD) {
			return false;
		}
		if (Config.QUEST_REQUIRED_FOR_BOSS) {
			Inventory inv = player.inventory();
			if (inv == null || inv.byItemId(BLOODED_FABRIC).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public boolean enterFloor(PlayerCharacter player) {
		if (!canEnter(player)) {
			return false;
		}

		if (Config.QUEST_REQUIRED_FOR_BOSS && player.inventory() != null) {
			player.inventory().byItemId(BLOODED_FABRIC).ifPresent(item -> {
				if (item.count() <= 1) {
					player.inventory().remove(item);
				} else {
					item.count(item.count() - 1);
				}
			});
		}

		player.x(FLOOR14_X);
		player.y(FLOOR14_Y);
		player.z(FLOOR14_Z);
		playersInside.add(player.objectId());

		log.info("Baium: Jogador {} entrou no 14o andar da Torre da Insolencia.", player.name());
		return true;
	}

	public synchronized boolean awakenBaium(PlayerCharacter player) {
		if (baiumStatue == null || status == BossStatus.FIGHTING) {
			return false;
		}

		if (Config.BAIUM_CHECK_QUEST_FOR_AWAKE && player != null) {
			Inventory inv = player.inventory();
			if (inv == null || inv.byItemId(BLOODED_FABRIC).isEmpty()) {
				return false;
			}
			inv.byItemId(BLOODED_FABRIC).ifPresent(item -> {
				if (item.count() <= 1) {
					inv.remove(item);
				} else {
					item.count(item.count() - 1);
				}
			});
		}

		// Despawna estatua
		if (world != null) {
			world.removeNpc(baiumStatue);
		}
		baiumStatue = null;

		// Spawna Baium
		if (templates != null && world != null && objectIds != null) {
			NpcTemplate baiumTpl = templates.get(BAIUM_NPC_ID)
					.orElseGet(() -> NpcTemplate.fallback(BAIUM_NPC_ID, "Baium", "L2GrandBoss"));
			int objId = objectIds.nextId();
			activeBaium = new NpcInstance(objId, baiumTpl, FLOOR14_X, FLOOR14_Y, FLOOR14_Z, BAIUM_HEADING);
			world.addNpc(activeBaium);

			// Spawna 5 Arcanjos ao redor
			NpcTemplate archangelTpl = templates.get(ARCHANGEL)
					.orElseGet(() -> NpcTemplate.fallback(ARCHANGEL, "Archangel", "L2Monster"));
			archangels.clear();
			for (int i = 0; i < 5; i++) {
				double angle = (2 * Math.PI * i) / 5;
				int ax = (int) (FLOOR14_X + Math.cos(angle) * 350);
				int ay = (int) (FLOOR14_Y + Math.sin(angle) * 350);
				NpcInstance angel = new NpcInstance(objectIds.nextId(), archangelTpl, ax, ay, FLOOR14_Z, 0);
				angel.masterObjectId(activeBaium.objectId());
				activeBaium.minions().add(angel);
				archangels.add(angel);
				world.addNpc(angel);
			}
		}

		status = BossStatus.FIGHTING;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.BAIUM, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Baium",
					"Someone dared to awaken Baium! The doors are sealed!"), p -> true);
		}

		log.info("Baium despertou! 5 arcanjos gerados e status FIGHTING ativado.");
		return true;
	}

	public synchronized void onBaiumKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;

		// Remove arcanjos remanescentes
		for (NpcInstance angel : archangels) {
			if (world != null) world.removeNpc(angel);
		}
		archangels.clear();

		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.BAIUM);
		}

		spawnExitCube();

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Baium",
					"Baium has been slain! The Teleportation Cube will remain for 5 minutes!"), p -> true);
		}

		log.info("Baium foi derrotado! Cubo ativado e respawn agendado.");
	}

	private void spawnStatue() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}
		if (baiumStatue != null) {
			return;
		}
		NpcTemplate statueTpl = templates.get(BAIUM_STATUE)
				.orElseGet(() -> NpcTemplate.fallback(BAIUM_STATUE, "Baium", "L2Npc"));
		baiumStatue = new NpcInstance(objectIds.nextId(), statueTpl, FLOOR14_X, FLOOR14_Y, FLOOR14_Z, BAIUM_HEADING);
		world.addNpc(baiumStatue);
	}

	private void spawnExitCube() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}
		NpcTemplate cubeTpl = templates.get(TELEPORT_CUBE)
				.orElseGet(() -> NpcTemplate.fallback(TELEPORT_CUBE, "Teleportation Cube", "L2Npc"));
		exitCube = new NpcInstance(objectIds.nextId(), cubeTpl, FLOOR14_X, FLOOR14_Y, FLOOR14_Z, 0);
		world.addNpc(exitCube);

		int unspawnSec = Config.BAIUM_UNSPAWN_CUBE;
		scheduler.schedule(this::clearFloor, unspawnSec, TimeUnit.SECONDS);
	}

	public void clearFloor() {
		if (exitCube != null && world != null) {
			world.removeNpc(exitCube);
			exitCube = null;
		}
		if (activeBaium != null && world != null) {
			world.removeNpc(activeBaium);
			activeBaium = null;
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

	public Optional<NpcInstance> activeBaium() {
		return Optional.ofNullable(activeBaium);
	}

	public Optional<NpcInstance> baiumStatue() {
		return Optional.ofNullable(baiumStatue);
	}
}
