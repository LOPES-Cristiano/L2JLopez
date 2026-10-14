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
 * Servico de Pontos PC Bang / PC Cafe (repatriado de L2JDreamV2 / L2JLucera2).
 * Conecta as propriedades PcBangPointEnable, PcBangPointMinLevel, PcBangPointMinCount,
 * PcBangPointMaxCount, PcBangPointDualChance e PcBangPointTimeStamp de fun_events.properties.
 */
@Service
public class PcCafeService {

	private static final Logger log = LoggerFactory.getLogger(PcCafeService.class);

	public record PcBangAward(int pointsGranted, boolean doubled, int totalPoints) {}

	private final Map<Integer, Integer> playerPoints = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;

	public PcCafeService() {
		if (Config.PCB_ENABLE) {
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
		return Config.PCB_ENABLE;
	}

	public int getMinLevel() {
		return Math.max(1, Config.PCB_MIN_LEVEL);
	}

	public int getMinPoints() {
		return Math.max(1, Config.PCB_POINT_MIN);
	}

	public int getMaxPoints() {
		return Math.max(getMinPoints(), Config.PCB_POINT_MAX);
	}

	public int getDualPointChancePercent() {
		return Math.max(0, Math.min(100, Config.PCB_CHANCE_DUAL_POINT));
	}

	public int getIntervalSeconds() {
		return Math.max(1, Config.PCB_INTERVAL);
	}

	public boolean isEligible(PlayerCharacter player) {
		return isEnabled() && player != null && !player.isDead() && player.level() >= getMinLevel();
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
		int newTotal = playerPoints.merge(player.objectId(), granted, Integer::sum);
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
		return playerPoints.getOrDefault(playerId, 0);
	}

	public void setPoints(int playerId, int points) {
		playerPoints.put(playerId, Math.max(0, points));
	}

	public boolean consumePoints(int playerId, int amount) {
		if (amount <= 0) {
			return true;
		}
		int current = getPoints(playerId);
		if (current < amount) {
			return false;
		}
		playerPoints.put(playerId, current - amount);
		return true;
	}
}
