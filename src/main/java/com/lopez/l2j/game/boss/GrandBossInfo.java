package com.lopez.l2j.game.boss;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.scheduling.support.CronExpression;

/**
 * Informacoes e estado de um Grand Boss no mundo com suporte a agendamento Cron.
 */
public class GrandBossInfo {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private final int bossId;
	private final String name;
	private final int locX;
	private final int locY;
	private final int locZ;
	private final int heading;
	private final int minRespawnMinutes;
	private final int maxRespawnMinutes;
	private String cronExpression;

	private BossStatus status = BossStatus.NOTSPAWN;
	private long respawnTime = 0;
	private double currentHp;
	private double currentMp;

	public GrandBossInfo(int bossId, String name, int locX, int locY, int locZ, int heading,
			int minRespawnMinutes, int maxRespawnMinutes) {
		this(bossId, name, locX, locY, locZ, heading, minRespawnMinutes, maxRespawnMinutes, null);
	}

	public GrandBossInfo(int bossId, String name, int locX, int locY, int locZ, int heading,
			int minRespawnMinutes, int maxRespawnMinutes, String cronExpression) {
		this.bossId = bossId;
		this.name = name;
		this.locX = locX;
		this.locY = locY;
		this.locZ = locZ;
		this.heading = heading;
		this.minRespawnMinutes = minRespawnMinutes;
		this.maxRespawnMinutes = maxRespawnMinutes;
		this.cronExpression = cronExpression;
	}

	public int bossId() {
		return bossId;
	}

	public String name() {
		return name;
	}

	public int locX() {
		return locX;
	}

	public int locY() {
		return locY;
	}

	public int locZ() {
		return locZ;
	}

	public int heading() {
		return heading;
	}

	public int minRespawnMinutes() {
		return minRespawnMinutes;
	}

	public int maxRespawnMinutes() {
		return maxRespawnMinutes;
	}

	public BossStatus status() {
		return status;
	}

	public void status(BossStatus status) {
		this.status = status != null ? status : BossStatus.NOTSPAWN;
	}

	public long respawnTime() {
		return respawnTime;
	}

	public void respawnTime(long respawnTime) {
		this.respawnTime = respawnTime;
	}

	public double currentHp() {
		return currentHp;
	}

	public void currentHp(double currentHp) {
		this.currentHp = currentHp;
	}

	public double currentMp() {
		return currentMp;
	}

	public void currentMp(double currentMp) {
		this.currentMp = currentMp;
	}

	public boolean isAlive() {
		return status == BossStatus.ALIVE;
	}

	public String cronExpression() {
		return cronExpression;
	}

	public void cronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public long calculateNextRespawnTime() {
		if (cronExpression != null && !cronExpression.isBlank()) {
			try {
				CronExpression cron = CronExpression.parse(cronExpression);
				ZonedDateTime next = cron.next(ZonedDateTime.now(ZoneId.systemDefault()));
				if (next != null) {
					return next.toInstant().toEpochMilli();
				}
			} catch (Exception ignored) {
				// Fallback to min/max interval if cron invalid
			}
		}

		long minRespawn = (long) minRespawnMinutes * 60_000L;
		long maxRespawn = (long) maxRespawnMinutes * 60_000L;
		long delay = minRespawn;
		if (maxRespawn > minRespawn) {
			delay = ThreadLocalRandom.current().nextLong(minRespawn, maxRespawn);
		}
		return System.currentTimeMillis() + delay;
	}

	public String getNextRespawnFormatted() {
		if (respawnTime <= 0) {
			return isAlive() ? "ALIVE" : "NOT SPAWNED";
		}
		return FORMATTER.format(Instant.ofEpochMilli(respawnTime));
	}

	public long getIntervalMillis() {
		long diff = respawnTime - System.currentTimeMillis();
		return Math.max(0, diff);
	}
}
