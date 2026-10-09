package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador central de spawn e respawn de todos os Raid Bosses do mundo (L2JDream / L2JLucera2).
 * Carrega a tabela raidboss_spawnlist, gerencia estados ALIVE/DEAD, agenda tarefas de renascimento
 * com atraso aleatorio configuravel, atualiza banco de dados e envia anuncios globais.
 */
@Service
public class RaidBossSpawnManager {

	private static final Logger log = LoggerFactory.getLogger(RaidBossSpawnManager.class);
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public enum Status {
		ALIVE,
		DEAD,
		UNDEFINED
	}

	public record RaidBossRecord(
			int bossId,
			int amount,
			int locX,
			int locY,
			int locZ,
			int zoneSize,
			int heading,
			int minDelaySec,
			int maxDelaySec,
			long respawnTime,
			double currentHp,
			double currentMp,
			boolean broadcastSpawn
	) {}

	private final Map<Integer, RaidBossRecord> storedInfo = new ConcurrentHashMap<>();
	private final Map<Integer, NpcInstance> activeBosses = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

	private final JdbcClient jdbc;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final GrandBossManager grandBossManager;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = Thread.ofVirtual().name("RaidBossScheduler").unstarted(r);
		return t;
	});

	@Autowired
	public RaidBossSpawnManager(
			@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds,
			@Autowired(required = false) GrandBossManager grandBossManager) {
		this.jdbc = jdbc;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		this.grandBossManager = grandBossManager;
	}

	@PostConstruct
	public void init() {
		loadFromDb();
		scheduleOrSpawnAll();
	}

	public synchronized void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT boss_id, amount, loc_x, loc_y, loc_z, zone_size, heading,
					       respawn_min_delay, respawn_max_delay, respawn_time, currentHp, currentMp, broadcastSpawn
					FROM raidboss_spawnlist
					ORDER BY boss_id
					""").query().listOfRows();

			storedInfo.clear();
			for (var row : rows) {
				int bossId = ((Number) row.get("boss_id")).intValue();
				int amount = row.get("amount") != null ? ((Number) row.get("amount")).intValue() : 1;
				int x = ((Number) row.get("loc_x")).intValue();
				int y = ((Number) row.get("loc_y")).intValue();
				int z = ((Number) row.get("loc_z")).intValue();
				int zoneSize = row.get("zone_size") != null ? ((Number) row.get("zone_size")).intValue() : 0;
				int heading = row.get("heading") != null ? ((Number) row.get("heading")).intValue() : 0;
				int minDelay = row.get("respawn_min_delay") != null ? ((Number) row.get("respawn_min_delay")).intValue() : 43200;
				int maxDelay = row.get("respawn_max_delay") != null ? ((Number) row.get("respawn_max_delay")).intValue() : 129600;
				long respawnTime = row.get("respawn_time") != null ? ((Number) row.get("respawn_time")).longValue() : 0L;
				double hp = row.get("currentHp") != null ? ((Number) row.get("currentHp")).doubleValue() : 0.0;
				double mp = row.get("currentMp") != null ? ((Number) row.get("currentMp")).doubleValue() : 0.0;
				String bc = row.get("broadcastSpawn") != null ? row.get("broadcastSpawn").toString() : "false";
				boolean broadcast = "true".equalsIgnoreCase(bc);

				storedInfo.put(bossId, new RaidBossRecord(
						bossId, amount, x, y, z, zoneSize, heading,
						minDelay, maxDelay, respawnTime, hp, mp, broadcast));
			}
			log.info("RaidBossSpawnManager: Carregados {} registros de raidboss_spawnlist.", storedInfo.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar raidboss_spawnlist: {}", ex.getMessage());
		}
	}

	private void scheduleOrSpawnAll() {
		long now = System.currentTimeMillis();
		int spawned = 0;
		int scheduled = 0;

		for (RaidBossRecord rec : storedInfo.values()) {
			if (rec.respawnTime() == 0L || rec.respawnTime() <= now) {
				spawnBoss(rec.bossId());
				spawned++;
			} else {
				long delayMs = rec.respawnTime() - now;
				scheduleSpawn(rec.bossId(), delayMs);
				scheduled++;
			}
		}
		log.info("RaidBossSpawnManager: {} Raid Bosses gerados imediatamente, {} agendados.", spawned, scheduled);
	}

	public synchronized Optional<NpcInstance> spawnBoss(int bossId) {
		RaidBossRecord rec = storedInfo.get(bossId);
		if (rec == null || templates == null || world == null || objectIds == null) {
			return Optional.empty();
		}

		// Se ja estiver ativo no mundo, nao cria duplicata
		NpcInstance existing = activeBosses.get(bossId);
		if (existing != null && !existing.isDead()) {
			return Optional.of(existing);
		}

		NpcTemplate template = templates.get(bossId).orElseGet(() ->
				NpcTemplate.fallback(bossId, "Raid Boss " + bossId, "L2RaidBoss"));

		int objectId = objectIds.nextId();
		NpcInstance boss = new NpcInstance(objectId, template, rec.locX(), rec.locY(), rec.locZ(), rec.heading());

		// Aplica modificadores de stats de Raid Boss (Lucera / Dream formulas.properties)
		boss.pAtkMul(Math.max(0.1, Config.RAID_BOSS_P_ATK_MODIFIER));
		boss.pDefMul(Math.max(0.1, Config.RAID_BOSS_P_DEF_MODIFIER));
		boss.mDefMul(Math.max(0.1, Config.RAID_BOSS_M_DEF_MODIFIER));
		boss.maxHpMul(Math.max(0.1, Config.RAID_BOSS_MAX_HP_MODIFIER));
		boss.currentHp(boss.maxHp());
		boss.currentMp(template.maxMp() * Math.max(0.1, Config.RAID_BOSS_MAX_MP_MODIFIER));

		activeBosses.put(bossId, boss);
		world.addNpc(boss);

		// Atualiza registro e zera respawnTime no banco
		RaidBossRecord updated = new RaidBossRecord(
				rec.bossId(), rec.amount(), rec.locX(), rec.locY(), rec.locZ(), rec.zoneSize(), rec.heading(),
				rec.minDelaySec(), rec.maxDelaySec(), 0L, boss.currentHp(), boss.currentMp(), rec.broadcastSpawn()
		);
		storedInfo.put(bossId, updated);
		persistDb(updated);

		// Notificacao global de spawn
		if (Config.ANNOUNCE_RAID_SPAWN || rec.broadcastSpawn()) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Raid Boss",
					"Raid Boss " + template.name() + " has spawned in the world!"), p -> true);
		}

		log.info("RaidBoss {} (ID {}) renasceu em ({}, {}, {})! Status ALIVE",
				template.name(), bossId, rec.locX(), rec.locY(), rec.locZ());
		return Optional.of(boss);
	}

	public synchronized void onBossKilled(NpcInstance npc, PlayerCharacter killer) {
		if (npc == null) {
			return;
		}
		int bossId = npc.npcId();

		// Se for Grand Boss registrado no GrandBossManager, notifica-o tambem
		if (grandBossManager != null && grandBossManager.isGrandBoss(bossId)) {
			grandBossManager.onBossKilled(bossId);
		}

		RaidBossRecord rec = storedInfo.get(bossId);
		if (rec == null) {
			return;
		}

		activeBosses.remove(bossId);

		// Calculo do proximo renascimento com randomizador e multiplicadores
		long minDelay = rec.minDelaySec();
		long maxDelay = rec.maxDelaySec();
		long delaySec = minDelay;
		if (maxDelay > minDelay) {
			delaySec += java.util.concurrent.ThreadLocalRandom.current().nextLong(maxDelay - minDelay);
		}
		long delayMs = delaySec * 1000L;
		long nextRespawnTime = System.currentTimeMillis() + delayMs;

		RaidBossRecord updated = new RaidBossRecord(
				rec.bossId(), rec.amount(), rec.locX(), rec.locY(), rec.locZ(), rec.zoneSize(), rec.heading(),
				rec.minDelaySec(), rec.maxDelaySec(), nextRespawnTime, 0.0, 0.0, rec.broadcastSpawn()
		);
		storedInfo.put(bossId, updated);
		persistDb(updated);

		// Anuncio de derrota
		if (Config.ANNOUNCE_RAID_DEATH && world != null) {
			String killerName = killer != null ? killer.name() : "brave heroes";
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Raid Boss",
					"Raid Boss " + npc.name() + " has been defeated by " + killerName + "!"), p -> true);
		}

		log.info("RaidBoss {} (ID {}) foi morto! Respawn agendado para {} ({} ms)",
				npc.name(), bossId, FORMATTER.format(Instant.ofEpochMilli(nextRespawnTime)), delayMs);

		scheduleSpawn(bossId, delayMs);
	}

	private void scheduleSpawn(int bossId, long delayMs) {
		ScheduledFuture<?> existing = schedules.remove(bossId);
		if (existing != null) {
			existing.cancel(false);
		}
		ScheduledFuture<?> future = scheduler.schedule(() -> {
			try {
				spawnBoss(bossId);
			} catch (Exception ex) {
				log.error("Erro ao realizar spawn agendado de RaidBoss {}", bossId, ex);
			}
		}, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
		schedules.put(bossId, future);
	}

	private void persistDb(RaidBossRecord rec) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					UPDATE raidboss_spawnlist
					SET respawn_time = :respawn_time, currentHp = :currentHp, currentMp = :currentMp
					WHERE boss_id = :boss_id
					""")
					.param("boss_id", rec.bossId())
					.param("respawn_time", rec.respawnTime())
					.param("currentHp", rec.currentHp())
					.param("currentMp", rec.currentMp())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao atualizar raidboss_spawnlist para {}: {}", rec.bossId(), ex.getMessage());
		}
	}

	public boolean isBossAlive(int bossId) {
		NpcInstance boss = activeBosses.get(bossId);
		return boss != null && !boss.isDead();
	}

	public Status getStatus(int bossId) {
		if (isBossAlive(bossId)) {
			return Status.ALIVE;
		}
		if (schedules.containsKey(bossId) || (storedInfo.containsKey(bossId) && storedInfo.get(bossId).respawnTime() > 0)) {
			return Status.DEAD;
		}
		return Status.UNDEFINED;
	}

	public long getRespawnTime(int bossId) {
		RaidBossRecord rec = storedInfo.get(bossId);
		return rec != null ? rec.respawnTime() : 0L;
	}

	public String getNextRespawnFormatted(int bossId) {
		long time = getRespawnTime(bossId);
		if (time <= 0L) {
			return isBossAlive(bossId) ? "VIVO" : "DISPONIVEL";
		}
		return FORMATTER.format(Instant.ofEpochMilli(time));
	}

	public Optional<NpcInstance> getActiveBoss(int bossId) {
		return Optional.ofNullable(activeBosses.get(bossId));
	}

	public Collection<RaidBossRecord> getAllBossRecords() {
		return Collections.unmodifiableCollection(storedInfo.values());
	}

	public Collection<NpcInstance> getAllActiveBosses() {
		return Collections.unmodifiableCollection(activeBosses.values());
	}

	public long calculateRespawnDelaySec(RaidBossRecord rec) {
		if (rec == null) {
			return 0L;
		}
		long minSec = (long) (rec.minDelaySec() * Config.RAID_MIN_RESPAWN_MULTIPLIER);
		long maxSec = (long) (rec.maxDelaySec() * Config.RAID_MAX_RESPAWN_MULTIPLIER);
		if (maxSec < minSec) {
			maxSec = minSec;
		}
		return maxSec > minSec ? ThreadLocalRandom.current().nextLong(minSec, maxSec + 1) : minSec;
	}

	public Map<Integer, RaidBossRecord> getStoredInfo() {
		return storedInfo;
	}

	public boolean isDefined(int bossId) {
		return storedInfo.containsKey(bossId);
	}

	public synchronized void reloadBosses() {
		init();
	}
}
