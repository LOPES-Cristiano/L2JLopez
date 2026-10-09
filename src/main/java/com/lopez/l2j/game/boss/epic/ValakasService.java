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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador do Covil do Dragao do Fogo Valakas (ValakasManager do L2JDream / L2JLucera2).
 * Controla entrada com Floating Stone (7267) via Klein (31540), limite de capacidade (500 jogadores),
 * animacao e contagem de chegada de Valakas (29028), lava e cubo de teletransporte (31759).
 */
@Service
public class ValakasService {

	private static final Logger log = LoggerFactory.getLogger(ValakasService.class);

	public static final int KLEIN = 31540;
	public static final int FLOATING_STONE = 7267;
	public static final int VALAKAS = 29028;
	public static final int TELEPORT_CUBE = 31759;
	public static final int VALAKAS_NECKLACE = 6657;

	// Coordenadas do Covil de Valakas
	public static final int LAIR_X = 212852;
	public static final int LAIR_Y = -114842;
	public static final int LAIR_Z = -1632;
	public static final int BOSS_HEADING = 833;

	public static final int EXIT_X = 183811;
	public static final int EXIT_Y = -115157;
	public static final int EXIT_Z = -3303;

	private BossStatus status = BossStatus.NOTSPAWN;
	private final List<Integer> playersInside = new CopyOnWriteArrayList<>();
	private NpcInstance activeValakas;
	private NpcInstance exitCube;
	private ScheduledFuture<?> arrivalTask;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("ValakasLairScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public ValakasService(
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
			this.status = grandBossManager.getStatus(GrandBossManager.VALAKAS);
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
		if (playersInside.size() >= Config.VALAKAS_LAIR_CAPACITY) {
			return false;
		}
		if (Config.QUEST_REQUIRED_FOR_BOSS) {
			Inventory inv = player.inventory();
			if (inv == null || inv.byItemId(FLOATING_STONE).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public boolean enterLair(PlayerCharacter player) {
		if (!canEnter(player)) {
			return false;
		}

		if (Config.QUEST_REQUIRED_FOR_BOSS && player.inventory() != null) {
			player.inventory().byItemId(FLOATING_STONE).ifPresent(item -> {
				if (item.count() <= 1) {
					player.inventory().remove(item);
				} else {
					item.count(item.count() - 1);
				}
			});
		}

		player.x(LAIR_X);
		player.y(LAIR_Y);
		player.z(LAIR_Z);
		playersInside.add(player.objectId());

		if (status == BossStatus.NOTSPAWN) {
			status = BossStatus.ALIVE;
			if (grandBossManager != null) {
				grandBossManager.setStatus(GrandBossManager.VALAKAS, BossStatus.ALIVE);
			}
			int arrivalMinutes = Config.VALAKAS_ARRIVED_TIME;
			log.info("Valakas: Primeiro jogador ({}) entrou no covil. Valakas surgira em {} minuto(s)!",
					player.name(), arrivalMinutes);
			arrivalTask = scheduler.schedule(this::spawnValakas, arrivalMinutes * 60L, TimeUnit.SECONDS);
		}

		return true;
	}

	public synchronized void spawnValakas() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}

		NpcTemplate tpl = templates.get(VALAKAS)
				.orElseGet(() -> NpcTemplate.fallback(VALAKAS, "Valakas", "L2GrandBoss"));

		int objId = objectIds.nextId();
		activeValakas = new NpcInstance(objId, tpl, LAIR_X, LAIR_Y, LAIR_Z, BOSS_HEADING);
		world.addNpc(activeValakas);

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.VALAKAS, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Fire Dragon",
					"Valakas has awakened in a fiery storm!"), p -> true);
		}

		log.info("Valakas gerado com sucesso no covil com {} jogadores presentes!", playersInside.size());
	}

	public synchronized void onValakasKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;
		if (arrivalTask != null) arrivalTask.cancel(false);

		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.VALAKAS);
		}

		spawnExitCube();

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Fire Dragon",
					"Valakas has fallen! The Teleportation Cube has appeared!"), p -> true);
		}

		log.info("Valakas foi derrotado! Cubo de saida ativado e respawn agendado.");
	}

	private void spawnExitCube() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}
		NpcTemplate cubeTpl = templates.get(TELEPORT_CUBE)
				.orElseGet(() -> NpcTemplate.fallback(TELEPORT_CUBE, "Teleportation Cube", "L2Npc"));
		int objId = objectIds.nextId();
		exitCube = new NpcInstance(objId, cubeTpl, LAIR_X, LAIR_Y, LAIR_Z, 0);
		world.addNpc(exitCube);

		scheduler.schedule(this::clearLair, 15L * 60L, TimeUnit.SECONDS);
	}

	public void clearLair() {
		if (exitCube != null) {
			world.removeNpc(exitCube);
			exitCube = null;
		}
		if (activeValakas != null) {
			world.removeNpc(activeValakas);
			activeValakas = null;
		}
		for (int playerId : playersInside) {
			world.player(playerId).ifPresent(p -> {
				PlayerCharacter pc = p.character();
				if (pc != null) {
					pc.x(EXIT_X);
					pc.y(EXIT_Y);
					pc.z(EXIT_Z);
				}
			});
		}
		playersInside.clear();
	}

	public Optional<NpcInstance> activeValakas() {
		return Optional.ofNullable(activeValakas);
	}
}
