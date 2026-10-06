package com.lopez.l2j.game.boss;

/**
 * Informacoes e estado de um Grand Boss no mundo.
 */
public class GrandBossInfo {

	private final int bossId;
	private final String name;
	private final int locX;
	private final int locY;
	private final int locZ;
	private final int heading;
	private final int minRespawnMinutes;
	private final int maxRespawnMinutes;

	private BossStatus status = BossStatus.NOTSPAWN;
	private long respawnTime = 0;
	private double currentHp;
	private double currentMp;

	public GrandBossInfo(int bossId, String name, int locX, int locY, int locZ, int heading,
			int minRespawnMinutes, int maxRespawnMinutes) {
		this.bossId = bossId;
		this.name = name;
		this.locX = locX;
		this.locY = locY;
		this.locZ = locZ;
		this.heading = heading;
		this.minRespawnMinutes = minRespawnMinutes;
		this.maxRespawnMinutes = maxRespawnMinutes;
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

	public long getIntervalMillis() {
		long diff = respawnTime - System.currentTimeMillis();
		return Math.max(0, diff);
	}
}
