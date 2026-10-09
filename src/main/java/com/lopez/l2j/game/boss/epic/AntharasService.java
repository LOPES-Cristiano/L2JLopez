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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador de instancia do covil do Dragao da Terra Antharas (AntharasManager do L2JDream / L2JLucera2).
 * Controla entrada com Portal Stone (3865) via Coracao de Protecao (13001), contagem de chegada,
 * selecao de forca (Fraco / Normal / Forte), invocacao de Behemoth Dragons, verificacao de aniquilacao e cubo de saida (31859).
 */
@Service
public class AntharasService {

	private static final Logger log = LoggerFactory.getLogger(AntharasService.class);

	public static final int HEART_OF_WARDING = 13001;
	public static final int PORTAL_STONE = 3865;
	public static final int TELEPORT_CUBE = 31859;
	public static final int ANTHARAS_EARRING = 6656;

	public static final int ANTHARAS_WEAK = 29019;
	public static final int ANTHARAS_NORMAL = 29067;
	public static final int ANTHARAS_STRONG = 29068;
	public static final int BEHEMOTH_DRAGON = 29069;
	public static final int TARASQUE_DRAGON = 29070;

	// Coordenadas do Covil de Antharas
	public static final int LAIR_X = 173826;
	public static final int LAIR_Y = 115333;
	public static final int LAIR_Z = -7708;

	public static final int BOSS_X = 181323;
	public static final int BOSS_Y = 114850;
	public static final int BOSS_Z = -7670;
	public static final int BOSS_HEADING = 32542;

	public static final int EXIT_X = 79800;
	public static final int EXIT_Y = 151200;
	public static final int EXIT_Z = -3534;

	private BossStatus status = BossStatus.NOTSPAWN;
	private final List<Integer> playersInside = new CopyOnWriteArrayList<>();
	private NpcInstance activeAntharas;
	private NpcInstance exitCube;
	private ScheduledFuture<?> arrivalTask;
	private ScheduledFuture<?> minionTask;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final JdbcClient jdbc;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("AntharasLairScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public AntharasService(
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds,
			@Autowired(required = false) JdbcClient jdbc) {
		this.grandBossManager = grandBossManager;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		this.jdbc = jdbc;
	}

	@PostConstruct
	public void init() {
		if (grandBossManager != null) {
			this.status = grandBossManager.getStatus(GrandBossManager.ANTHARAS);
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
			if (inv == null || inv.byItemId(PORTAL_STONE).isEmpty()) {
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
			player.inventory().byItemId(PORTAL_STONE).ifPresent(item -> {
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
				grandBossManager.setStatus(GrandBossManager.ANTHARAS, BossStatus.ALIVE);
			}
			int arrivalMinutes = Config.ANTHARAS_ARRIVED_TIME;
			log.info("Antharas: Primeiro jogador ({}) entrou no covil. Antharas surgira em {} minutos!",
					player.name(), arrivalMinutes);
			arrivalTask = scheduler.schedule(this::spawnAntharas, arrivalMinutes * 60L, TimeUnit.SECONDS);
		}

		return true;
	}

	public synchronized void spawnAntharas() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}

		int playerCount = playersInside.size();
		int antharasId = ANTHARAS_WEAK;
		int weakThreshold = Config.ANTHARAS_WEAK_PLAYERS;
		int middleThreshold = Config.ANTHARAS_MIDDLE_PLAYERS;

		if (playerCount > middleThreshold) {
			antharasId = ANTHARAS_STRONG;
		} else if (playerCount > weakThreshold) {
			antharasId = ANTHARAS_NORMAL;
		}

		NpcTemplate tpl = templates.get(antharasId)
				.orElseGet(() -> NpcTemplate.fallback(ANTHARAS_WEAK, "Antharas", "L2GrandBoss"));

		int objId = objectIds.nextId();
		activeAntharas = new NpcInstance(objId, tpl, BOSS_X, BOSS_Y, BOSS_Z, BOSS_HEADING);
		world.addNpc(activeAntharas);

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.ANTHARAS, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Earth Dragon",
					"Antharas has awakened and descends into the Lair!"), p -> true);
		}

		log.info("Antharas: Gerado boss ID {} (dificuldade {} jogadores) no covil com sucesso!",
				antharasId, playerCount);

		// Agenda invocacao periodica de lacaios Behemoth (retail 4 minutos)
		int behemothInterval = Config.ANTHARAS_INTERVAL_OF_BEHEMOTH;
		minionTask = scheduler.scheduleAtFixedRate(this::spawnMinions,
				behemothInterval * 60L, behemothInterval * 60L, TimeUnit.SECONDS);
	}

	private void spawnMinions() {
		if (activeAntharas == null || activeAntharas.isDead() || templates == null || world == null || objectIds == null) {
			return;
		}
		NpcTemplate minionTpl = templates.get(BEHEMOTH_DRAGON)
				.orElseGet(() -> NpcTemplate.fallback(BEHEMOTH_DRAGON, "Behemoth Dragon", "L2Monster"));
		for (int i = 0; i < 2; i++) {
			int objId = objectIds.nextId();
			int mx = BOSS_X + java.util.concurrent.ThreadLocalRandom.current().nextInt(-400, 400);
			int my = BOSS_Y + java.util.concurrent.ThreadLocalRandom.current().nextInt(-400, 400);
			NpcInstance minion = new NpcInstance(objId, minionTpl, mx, my, BOSS_Z, 0);
			world.addNpc(minion);
		}
	}

	public synchronized void onAntharasKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;
		if (arrivalTask != null) arrivalTask.cancel(false);
		if (minionTask != null) minionTask.cancel(false);

		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.ANTHARAS);
		}

		// Gera Cubo de Teletransporte de Saida
		spawnExitCube();

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Earth Dragon",
					"Antharas has been defeated! The Teleportation Cube has appeared!"), p -> true);
		}

		log.info("Antharas foi derrotado! Cubo de saida ativado e respawn agendado.");
	}

	private void spawnExitCube() {
		if (templates == null || world == null || objectIds == null) {
			return;
		}
		NpcTemplate cubeTpl = templates.get(TELEPORT_CUBE)
				.orElseGet(() -> NpcTemplate.fallback(TELEPORT_CUBE, "Teleportation Cube", "L2Npc"));
		int objId = objectIds.nextId();
		exitCube = new NpcInstance(objId, cubeTpl, BOSS_X, BOSS_Y, BOSS_Z, 0);
		world.addNpc(exitCube);

		// Remove o cubo e limpa a caverna apos 15 minutos
		scheduler.schedule(this::clearLair, 15L * 60L, TimeUnit.SECONDS);
	}

	public void clearLair() {
		if (exitCube != null) {
			world.removeNpc(exitCube);
			exitCube = null;
		}
		if (activeAntharas != null) {
			world.removeNpc(activeAntharas);
			activeAntharas = null;
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

	public Optional<NpcInstance> activeAntharas() {
		return Optional.ofNullable(activeAntharas);
	}
}
