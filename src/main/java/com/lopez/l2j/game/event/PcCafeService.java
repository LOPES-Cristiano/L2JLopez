package com.lopez.l2j.game.event;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de Pontos PC Bang / PC Cafe (repatriado de L2JDreamV2 / L2JLucera2) - #494.
 * Conecta as propriedades PcBangPointEnable, PcBangPointMinLevel, PcBangPointMinCount,
 * PcBangPointMaxCount, PcBangPointDualChance e PcBangPointTimeStamp de fun_events.properties,
 * bloqueando concessao a lojas privadas/AFK e notificando o cliente com ExPCCafePointInfo.
 */
@Service
public class PcCafeService {

	private static final Logger log = LoggerFactory.getLogger(PcCafeService.class);
	public static final long ACTIVE_COMBAT_WINDOW_MS = 15 * 60 * 1000L;

	public record PcBangAward(int pointsGranted, boolean doubled, int totalPoints) {}

	private final Map<Integer, Integer> playerPoints = new ConcurrentHashMap<>();
	private final Map<Integer, Long> lastCombatTimestamps = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	private org.springframework.jdbc.core.simple.JdbcClient jdbc;

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	private com.lopez.l2j.game.service.CharacterVariablesService variablesService;

	public PcCafeService() {
		if (Config.PC_CAFFE_ENABLED) {
			startScheduler();
		}
	}

	@org.springframework.beans.factory.annotation.Autowired
	public PcCafeService(
			@org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.CharacterVariablesService variablesService) {
		this.jdbc = jdbc;
		this.variablesService = variablesService;
		if (Config.PC_CAFFE_ENABLED) {
			startScheduler();
		}
	}

	public synchronized void startScheduler() {
		if (scheduler != null && !scheduler.isShutdown()) {
			return;
		}
		int intervalSec = getIntervalSeconds();
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "PcCafe-Scheduler");
			t.setDaemon(true);
			return t;
		});
		scheduler.scheduleAtFixedRate(this::distributeScheduledPoints, intervalSec, intervalSec, TimeUnit.SECONDS);
		log.info("PcCafeService: agendador iniciado (interval={}s, minLvl={}, minPts={}, maxPts={}, dualChance={}%)",
				intervalSec, getMinLevel(), getMinPoints(), getMaxPoints(), getDualPointChancePercent());
	}

	public synchronized void stopScheduler() {
		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
	}

	public boolean isEnabled() {
		return Config.PC_CAFFE_ENABLED;
	}

	public int getMinLevel() {
		return Math.max(1, Config.PC_CAFE_MIN_LEVEL);
	}

	public int getMaxLevel() {
		return Math.max(getMinLevel(), Config.PC_CAFE_MAX_LEVEL);
	}

	public int getMinPoints() {
		return Math.max(1, Config.PC_CAFE_MIN_SCORE);
	}

	public int getMaxPoints() {
		return Math.max(getMinPoints(), Config.PC_CAFE_MAX_SCORE);
	}

	public int getDualPointChancePercent() {
		return 10;
	}

	public int getIntervalSeconds() {
		return Math.max(1, Config.PC_CAFE_INTERVAL);
	}

	public void recordCombatActivity(int playerId) {
		lastCombatTimestamps.put(playerId, System.currentTimeMillis());
	}

	public boolean hasRecentCombatActivity(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		if (player.isInCombat() || player.autoAttacking()) {
			recordCombatActivity(player.objectId());
			return true;
		}
		Long ts = lastCombatTimestamps.get(player.objectId());
		if (ts == null) {
			return true; // Sem histórico prévio de AFK na sessão
		}
		return (System.currentTimeMillis() - ts) <= ACTIVE_COMBAT_WINDOW_MS;
	}

	public boolean isEligible(PlayerCharacter player) {
		return isEnabled() && player != null && !player.isDead()
				&& player.privateStoreType() == 0 && !player.isBuffShop()
				&& player.level() >= getMinLevel() && player.level() <= getMaxLevel()
				&& hasRecentCombatActivity(player);
	}

	public PcBangAward awardPoints(PlayerCharacter player) {
		if (!isEligible(player)) {
			return new PcBangAward(0, false, player != null ? getPoints(player.objectId()) : 0);
		}
		int min = getMinPoints();
		int max = getMaxPoints();
		int basePoints = (max > min)
				? ThreadLocalRandom.current().nextInt(min, max + 1)
				: min;
		boolean doubled = getDualPointChancePercent() > 0
				&& ThreadLocalRandom.current().nextInt(100) < getDualPointChancePercent();
		int granted = doubled ? basePoints * 2 : basePoints;
		int current = getPoints(player.objectId());
		int newTotal = current + granted;
		playerPoints.put(player.objectId(), newTotal);
		persistPoints(player.objectId(), newTotal);

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.sendPacket(player.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.ExPCCafePointInfo(
					newTotal, granted, 1, 24, doubled ? 2 : 1));
		}
		return new PcBangAward(granted, doubled, newTotal);
	}

	public void distributeScheduledPoints() {
		if (!isEnabled()) {
			return;
		}
		GameWorld world = GameWorld.getInstance();
		if (world == null) {
			return;
		}
		for (var online : world.onlinePlayers()) {
			PlayerCharacter ch = online.character();
			if (isEligible(ch)) {
				awardPoints(ch);
			}
		}
	}

	public int getPoints(int playerId) {
		Integer cached = playerPoints.get(playerId);
		if (cached != null) {
			return cached;
		}
		if (variablesService != null) {
			int v = variablesService.getInt(playerId, "PC_CAFE_POINTS", -1);
			if (v >= 0) {
				playerPoints.put(playerId, v);
				return v;
			}
		}
		if (jdbc != null) {
			try {
				Integer dbPts = jdbc.sql("SELECT pc_point FROM characters WHERE charId = :id")
						.param("id", playerId)
						.query(Integer.class)
						.optional()
						.orElse(null);
				if (dbPts != null && dbPts >= 0) {
					playerPoints.put(playerId, dbPts);
					return dbPts;
				}
			} catch (Exception ignored) {
			}
		}
		return 0;
	}

	public void setPoints(int playerId, int points) {
		int clamped = Math.max(0, points);
		playerPoints.put(playerId, clamped);
		persistPoints(playerId, clamped);
	}

	public boolean consumePoints(int playerId, int amount) {
		if (amount <= 0) {
			return true;
		}
		int current = getPoints(playerId);
		if (current < amount) {
			return false;
		}
		int updated = current - amount;
		playerPoints.put(playerId, updated);
		persistPoints(playerId, updated);
		return true;
	}

	private void persistPoints(int playerId, int points) {
		if (variablesService != null) {
			variablesService.setVar(playerId, "PC_CAFE_POINTS", String.valueOf(points));
		}
		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE characters SET pc_point = :pts WHERE charId = :id")
						.param("pts", points)
						.param("id", playerId)
						.update();
			} catch (Exception ignored) {
			}
		}
	}
}
