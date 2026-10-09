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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento do covil de High Priestess van Halter (Pagan Temple).
 * Paridade com as implementacoes retail / L2JDreamV2 / L2JLucera2.
 */
@Service
public class VanHalterService {

	private static final Logger log = LoggerFactory.getLogger(VanHalterService.class);

	public static final int VAN_HALTER = 29062;
	public static final int ROYAL_GUARD_CAPTAIN = 22191;
	public static final int ROYAL_GUARD_1 = 22192;
	public static final int ROYAL_GUARD_2 = 22193;
	public static final int ACOLYTE_1 = 29063;
	public static final int ACOLYTE_2 = 29064;

	// Coordenadas centrais do Altar de Sacrificio em Pagan Temple
	public static final int ALTAR_X = -16377;
	public static final int ALTAR_Y = -53303;
	public static final int ALTAR_Z = -10448;
	public static final int ALTAR_HEADING = 0;

	// Portas do Altar de Pagan Temple
	public static final int[] ALTAR_DOOR_IDS = { 19160014, 19160015, 19160016, 19160017 };

	private final GameWorld world;
	private final NpcTemplateTable templates;
	private final ObjectIdFactory objectIds;
	private final GrandBossManager grandBossManager;
	private final DoorTable doorService;

	private volatile BossStatus status = BossStatus.NOTSPAWN;
	private NpcInstance activeVanHalter;
	private final List<NpcInstance> minions = Collections.synchronizedList(new ArrayList<>());

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "VanHalter-Scheduler");
		t.setDaemon(true);
		return t;
	});

	private ScheduledFuture<?> fightTimeoutTask;
	private ScheduledFuture<?> lockDoorsTask;

	@Autowired
	public VanHalterService(
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) ObjectIdFactory objectIds,
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) DoorTable doorService) {
		this.world = world;
		this.templates = templates;
		this.objectIds = objectIds;
		this.grandBossManager = grandBossManager;
		this.doorService = doorService;
	}

	public synchronized void spawnVanHalter() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}

		// Despawna remanescentes se houver
		clearMinions();
		if (activeVanHalter != null) {
			world.removeNpc(activeVanHalter);
			activeVanHalter = null;
		}

		NpcTemplate tpl = templates.get(VAN_HALTER)
				.orElseGet(() -> NpcTemplate.fallback(VAN_HALTER, "High Priestess van Halter", "L2GrandBoss"));
		activeVanHalter = new NpcInstance(objectIds.nextId(), tpl, ALTAR_X, ALTAR_Y, ALTAR_Z, ALTAR_HEADING);
		world.addNpc(activeVanHalter);

		// Spawna guarda real e acolitos
		spawnHelpers();

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.VAN_HALTER, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Pagan Temple",
					"High Priestess van Halter has appeared at the Altar of the Pagan Temple!"), p -> true);
		}

		// Agenda fechamento das portas do altar apos o tempo de entrada
		int lockTimeMinutes = Config.VAN_HALTER_TIME_OF_LOCK_UP_DOOR_OF_ALTAR;
		if (lockDoorsTask != null) lockDoorsTask.cancel(false);
		lockDoorsTask = scheduler.schedule(this::lockAltarDoors, lockTimeMinutes * 60L, TimeUnit.SECONDS);

		// Tempo maximo de luta
		int fightMinutes = Config.VAN_HALTER_FIGHT_TIME;
		if (fightTimeoutTask != null) fightTimeoutTask.cancel(false);
		fightTimeoutTask = scheduler.schedule(this::onFightTimeout, fightMinutes * 60L, TimeUnit.SECONDS);

		log.info("High Priestess van Halter gerada no Altar de Pagan Temple com {} ajudantes!", minions.size());
	}

	public synchronized void onVanHalterKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;
		if (fightTimeoutTask != null) fightTimeoutTask.cancel(false);
		if (lockDoorsTask != null) lockDoorsTask.cancel(false);

		// Remove acolitos e guardas
		clearMinions();

		// Abre as portas do altar
		openAltarDoors();

		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.VAN_HALTER);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Pagan Temple",
					"High Priestess van Halter has been defeated! The darkness fades from the Altar!"), p -> true);
		}

		log.info("High Priestess van Halter foi derrotada! Portas abertas e respawn agendado.");
	}

	private void onFightTimeout() {
		if (lockDoorsTask != null) lockDoorsTask.cancel(false);
		if (status == BossStatus.ALIVE || status == BossStatus.FIGHTING) {
			log.info("Tempo de luta de Van Halter esgotado. Despawnando boss e resetando altar.");
			if (activeVanHalter != null && world != null) {
				world.removeNpc(activeVanHalter);
				activeVanHalter = null;
			}
			clearMinions();
			openAltarDoors();
			status = BossStatus.NOTSPAWN;
			if (grandBossManager != null) {
				grandBossManager.setStatus(GrandBossManager.VAN_HALTER, BossStatus.NOTSPAWN);
			}
		}
	}

	private void spawnHelpers() {
		int[] helperIds = { ROYAL_GUARD_CAPTAIN, ROYAL_GUARD_1, ROYAL_GUARD_2, ACOLYTE_1, ACOLYTE_2 };
		for (int id : helperIds) {
			NpcTemplate tpl = templates.get(id).orElseGet(() -> NpcTemplate.fallback(id, "Pagan Helper", "L2Monster"));
			int ox = ALTAR_X + java.util.concurrent.ThreadLocalRandom.current().nextInt(-250, 250);
			int oy = ALTAR_Y + java.util.concurrent.ThreadLocalRandom.current().nextInt(-250, 250);
			NpcInstance helper = new NpcInstance(objectIds.nextId(), tpl, ox, oy, ALTAR_Z, 0);
			if (activeVanHalter != null) {
				helper.masterObjectId(activeVanHalter.objectId());
				activeVanHalter.minions().add(helper);
			}
			world.addNpc(helper);
			minions.add(helper);
		}
	}

	private void clearMinions() {
		for (NpcInstance m : minions) {
			if (world != null) {
				world.removeNpc(m);
			}
		}
		minions.clear();
	}

	private void lockAltarDoors() {
		if (status != BossStatus.ALIVE) {
			return;
		}
		if (doorService != null) {
			for (int doorId : ALTAR_DOOR_IDS) {
				doorService.closeDoor(doorId);
			}
		}
		status = BossStatus.FIGHTING;
		log.info("Portas do Altar de Pagan Temple foram trancadas.");
	}

	private void openAltarDoors() {
		if (doorService != null) {
			for (int doorId : ALTAR_DOOR_IDS) {
				doorService.openDoor(doorId);
			}
		}
		log.info("Portas do Altar de Pagan Temple foram abertas.");
	}

	public BossStatus status() {
		return status;
	}

	public Optional<NpcInstance> activeVanHalter() {
		return Optional.ofNullable(activeVanHalter);
	}
}
