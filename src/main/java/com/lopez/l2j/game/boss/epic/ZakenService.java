package com.lopez.l2j.game.boss.epic;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador do Navio Pirata do Zaken em Devil's Isle (ZakenManager do L2JDream / L2JLucera2).
 * Controla abertura das portas (porta 21240006) a meia-noite por 5 minutos,
 * limite de nivel maximo (80), mecanicas de teletransporte para dentro das celas e regeneracao noturna.
 */
@Service
public class ZakenService {

	private static final Logger log = LoggerFactory.getLogger(ZakenService.class);

	public static final int ZAKEN = 29022;
	public static final int ZAKEN_DOOR_ID = 21240006;
	public static final int ZAKENS_EARRING = 6659;

	// Coordenadas do Navio de Zaken
	public static final int SHIP_X = 55312;
	public static final int SHIP_Y = 219168;
	public static final int SHIP_Z = -3223;

	// Coordenadas das celas/camaras do navio para onde Zaken e jogadores sao teletransportados
	public static final int[][] SHIP_ROOMS = {
			{55272, 219112, -3496},
			{56296, 218072, -3496},
			{54232, 218072, -3496},
			{54248, 220136, -3496},
			{56296, 220136, -3496},
			{55272, 219112, -3224},
			{56296, 218072, -3224},
			{54232, 218072, -3224},
			{54248, 220136, -3224},
			{56296, 220136, -3224},
			{55272, 219112, -2952},
			{56296, 218072, -2952},
			{54232, 218072, -2952},
			{54248, 220136, -2952},
			{56296, 220136, -2952}
	};

	private BossStatus status = BossStatus.NOTSPAWN;
	private NpcInstance activeZaken;
	private boolean doorsOpen = false;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final DoorTable doorService;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("ZakenDoorScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public ZakenService(
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds,
			@Autowired(required = false) DoorTable doorService) {
		this.grandBossManager = grandBossManager;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		this.doorService = doorService;
	}

	@PostConstruct
	public void init() {
		if (grandBossManager != null) {
			this.status = grandBossManager.getStatus(GrandBossManager.ZAKEN);
		}
	}

	public BossStatus getStatus() {
		return status;
	}

	public boolean isLevelExceeded(PlayerCharacter player) {
		return player != null && player.level() > Config.ZAKEN_MAX_LEVEL_IN_ZONE;
	}

	public synchronized void openDoorForMidnight() {
		if (doorService != null) {
			doorService.openDoor(ZAKEN_DOOR_ID);
		}
		doorsOpen = true;
		int openMinutes = Config.ZAKEN_DOOR_OPEN_TIME;
		log.info("Zaken: Portas do navio pirata abertas por {} minutos!", openMinutes);

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Devil's Isle",
					"The gates to Zaken's pirate ship have swung open!"), p -> true);
		}

		// Agenda fechamento
		scheduler.schedule(this::closeDoorAfterMidnight, openMinutes * 60L, TimeUnit.SECONDS);
	}

	public synchronized void closeDoorAfterMidnight() {
		if (doorService != null) {
			doorService.closeDoor(ZAKEN_DOOR_ID);
		}
		doorsOpen = false;
		log.info("Zaken: Portas do navio pirata foram trancadas.");
	}

	public synchronized void spawnZaken() {
		if (templates == null || world == null || objectIds == null) return;

		NpcTemplate tpl = templates.get(ZAKEN)
				.orElseGet(() -> NpcTemplate.fallback(ZAKEN, "Zaken", "L2GrandBoss"));
		activeZaken = new NpcInstance(objectIds.nextId(), tpl, SHIP_X, SHIP_Y, SHIP_Z, 0);
		world.addNpc(activeZaken);

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.ZAKEN, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Devil's Isle",
					"The immortal pirate Zaken stalks the corridors of his cursed ship!"), p -> true);
		}
	}

	public synchronized void onZakenKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;
		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.ZAKEN);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Devil's Isle",
					"The pirate Zaken has been defeated!"), p -> true);
		}
	}

	public int[] getRandomRoom() {
		int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(SHIP_ROOMS.length);
		return SHIP_ROOMS[idx];
	}

	public boolean areDoorsOpen() {
		return doorsOpen;
	}

	public Optional<NpcInstance> activeZaken() {
		return Optional.ofNullable(activeZaken);
	}
}
