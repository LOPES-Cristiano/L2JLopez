package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.npc.SpawnService;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador central de Grand Bosses (Antharas, Valakas, Baium, Queen Ant, Zaken, etc.)
 * com persistencia em grandboss_data e grandboss_intervallist e agendamento de respawn.
 */
@Service
public class GrandBossManager {

	private static final Logger log = LoggerFactory.getLogger(GrandBossManager.class);

	public static final int QUEEN_ANT = 29001;
	public static final int CORE = 29006;
	public static final int ORFEN = 29014;
	public static final int ANTHARAS = 29019;
	public static final int BAIUM = 29020;
	public static final int ZAKEN = 29022;
	public static final int VALAKAS = 29028;
	public static final int FRINTEZZA = 29045;
	public static final int VAN_HALTER = 29062;
	public static final int SAILREN = 29065;

	private final Map<Integer, GrandBossInfo> bosses = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;
	private final SpawnService spawnService;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("GrandBossScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public GrandBossManager(@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) SpawnService spawnService) {
		this.jdbc = jdbc;
		this.spawnService = spawnService;
		initDefinitions();
	}

	private void initDefinitions() {
		register(new GrandBossInfo(QUEEN_ANT, "Queen Ant", -21610, 181594, -5734, 0,
				Config.getInt("QueenAntMinRespawn", 1140), Config.getInt("QueenAntMaxRespawn", 2160)));
		register(new GrandBossInfo(CORE, "Core", 17726, 108915, -6480, 0,
				Config.getInt("CoreMinRespawn", 2220), Config.getInt("CoreMaxRespawn", 3600)));
		register(new GrandBossInfo(ORFEN, "Orfen", 55024, 17368, -5412, 0,
				Config.getInt("OrfenMinRespawn", 1680), Config.getInt("OrfenMaxRespawn", 2880)));
		register(new GrandBossInfo(ANTHARAS, "Antharas", 181323, 114850, -7670, 32542,
				Config.getInt("AntharasMinRespawn", 11520), Config.getInt("AntharasMaxRespawn", 15840)));
		register(new GrandBossInfo(BAIUM, "Baium", 116033, 17447, 10107, 40188,
				Config.getInt("BaiumMinRespawn", 7200), Config.getInt("BaiumMaxRespawn", 10080)));
		register(new GrandBossInfo(ZAKEN, "Zaken", 55312, 219168, -3223, 0,
				Config.getInt("ZakenMinRespawn", 2400), Config.getInt("ZakenMaxRespawn", 3600)));
		register(new GrandBossInfo(VALAKAS, "Valakas", 212852, -114842, -1632, 833,
				Config.getInt("ValakasMinRespawn", 11520), Config.getInt("ValakasMaxRespawn", 15840)));
		register(new GrandBossInfo(FRINTEZZA, "Frintezza", -87784, -155083, -9083, 16048,
				Config.getInt("FrintezzaMinRespawn", 2400), Config.getInt("FrintezzaMaxRespawn", 3600)));
		register(new GrandBossInfo(VAN_HALTER, "High Priestess van Halter", -16375, -53658, 10448, 0,
				Config.getInt("VanHalterMinRespawn", 720), Config.getInt("VanHalterMaxRespawn", 2160)));
		register(new GrandBossInfo(SAILREN, "Sailren", 27333, -6835, -1970, 0,
				Config.getInt("SailrenMinRespawn", 720), Config.getInt("SailrenMaxRespawn", 2160)));
	}

	public void register(GrandBossInfo info) {
		bosses.put(info.bossId(), info);
	}

	@PostConstruct
	public void init() {
		loadFromDb();
		scheduleOrSpawnAll();
	}

	public boolean isGrandBoss(int npcId) {
		return bosses.containsKey(npcId);
	}

	public Optional<GrandBossInfo> getBoss(int bossId) {
		return Optional.ofNullable(bosses.get(bossId));
	}

	public Collection<GrandBossInfo> allBosses() {
		return Collections.unmodifiableCollection(bosses.values());
	}

	public BossStatus getStatus(int bossId) {
		GrandBossInfo info = bosses.get(bossId);
		return info != null ? info.status() : BossStatus.NOTSPAWN;
	}

	public synchronized void setStatus(int bossId, BossStatus status) {
		GrandBossInfo info = bosses.get(bossId);
		if (info != null) {
			info.status(status);
			persistDb(info);
			log.info("GrandBoss {} (ID {}) status alterado para {}", info.name(), bossId, status);
		}
	}

	public synchronized void onBossKilled(int bossId) {
		GrandBossInfo info = bosses.get(bossId);
		if (info == null) {
			return;
		}

		long minRespawn = (long) info.minRespawnMinutes() * 60_000L;
		long maxRespawn = (long) info.maxRespawnMinutes() * 60_000L;
		long delay = minRespawn;
		if (maxRespawn > minRespawn) {
			delay = ThreadLocalRandom.current().nextLong(minRespawn, maxRespawn);
		}

		long respawnTime = System.currentTimeMillis() + delay;
		info.respawnTime(respawnTime);
		info.status(BossStatus.INTERVAL);
		persistDb(info);

		log.info("GrandBoss {} foi morto! Respawn agendado para {} ms a frente (status INTERVAL)", info.name(), delay);
		scheduler.schedule(() -> spawnBoss(bossId), delay, TimeUnit.MILLISECONDS);
	}

	public void notifyBossKilled(int bossId) {
		onBossKilled(bossId);
	}

	public synchronized void spawnBoss(int bossId) {
		GrandBossInfo info = bosses.get(bossId);
		if (info == null) {
			return;
		}

		info.status(BossStatus.ALIVE);
		info.respawnTime(0);
		persistDb(info);

		if (spawnService != null) {
			spawnService.spawn(bossId, info.locX(), info.locY(), info.locZ(), info.heading());
		}
		log.info("GrandBoss {} (ID {}) renasceu em ({}, {}, {})! Status ALIVE",
				info.name(), bossId, info.locX(), info.locY(), info.locZ());
	}

	private void scheduleOrSpawnAll() {
		for (GrandBossInfo info : bosses.values()) {
			if (info.status() == BossStatus.INTERVAL || info.status() == BossStatus.DEAD) {
				long interval = info.getIntervalMillis();
				if (interval <= 0) {
					spawnBoss(info.bossId());
				} else {
					log.info("GrandBoss {} em INTERVAL, agendando renascimento em {} ms", info.name(), interval);
					scheduler.schedule(() -> spawnBoss(info.bossId()), interval, TimeUnit.MILLISECONDS);
				}
			} else if (info.status() == BossStatus.ALIVE || info.status() == BossStatus.NOTSPAWN) {
				spawnBoss(info.bossId());
			}
		}
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT boss_id, loc_x, loc_y, loc_z, heading, respawn_time, currentHP, currentMP, status
					FROM grandboss_data
					""").query().listOfRows();

			for (var row : rows) {
				int bossId = ((Number) row.get("boss_id")).intValue();
				long respawnTime = ((Number) row.get("respawn_time")).longValue();
				int statusId = ((Number) row.get("status")).intValue();

				GrandBossInfo info = bosses.get(bossId);
				if (info != null) {
					info.respawnTime(respawnTime);
					info.status(BossStatus.fromId(statusId));
					if (row.get("currentHP") != null) {
						info.currentHp(((Number) row.get("currentHP")).doubleValue());
					}
					if (row.get("currentMP") != null) {
						info.currentMp(((Number) row.get("currentMP")).doubleValue());
					}
				}
			}
			log.info("GrandBossManager restaurou {} bosses do banco grandboss_data", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar grandboss_data do banco: {}", ex.getMessage());
		}
	}

	private void persistDb(GrandBossInfo info) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO grandboss_data (boss_id, loc_x, loc_y, loc_z, heading, respawn_time, currentHP, currentMP, status)
					VALUES (:boss_id, :loc_x, :loc_y, :loc_z, :heading, :respawn_time, :currentHP, :currentMP, :status)
					ON DUPLICATE KEY UPDATE
						respawn_time = :respawn_time,
						currentHP = :currentHP,
						currentMP = :currentMP,
						status = :status
					""")
					.param("boss_id", info.bossId())
					.param("loc_x", info.locX())
					.param("loc_y", info.locY())
					.param("loc_z", info.locZ())
					.param("heading", info.heading())
					.param("respawn_time", info.respawnTime())
					.param("currentHP", info.currentHp())
					.param("currentMP", info.currentMp())
					.param("status", info.status().id())
					.update();

			jdbc.sql("""
					INSERT INTO grandboss_intervallist (boss_id, respawn_time, state)
					VALUES (:boss_id, :respawn_time, :state)
					ON DUPLICATE KEY UPDATE
						respawn_time = :respawn_time,
						state = :state
					""")
					.param("boss_id", info.bossId())
					.param("respawn_time", info.respawnTime())
					.param("state", info.status().id())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir grandboss {}", info.bossId(), ex);
		}
	}
}
