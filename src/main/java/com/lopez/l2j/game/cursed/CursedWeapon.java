package com.lopez.l2j.game.cursed;

/**
 * Representa o estado de uma arma amaldicoada (Demonic Sword Zariche ou Blood Sword Akamanah).
 */
public class CursedWeapon {

	private final int itemId;
	private final int skillId;
	private final String name;
	private final int dropRate;
	private final int durationMinutes;
	private final int stageKills;

	private int stage = 1;
	private int kills = 0;
	private int playerId = 0;
	private String playerName = "";
	private int x;
	private int y;
	private int z;
	private boolean active = false;
	private boolean dropped = false;
	private long endTime = 0;

	public CursedWeapon(int itemId, int skillId, String name, int dropRate, int durationMinutes, int stageKills) {
		this.itemId = itemId;
		this.skillId = skillId;
		this.name = name;
		this.dropRate = dropRate;
		this.durationMinutes = durationMinutes;
		this.stageKills = stageKills > 0 ? stageKills : 10;
	}

	public int itemId() {
		return itemId;
	}

	public int skillId() {
		return skillId;
	}

	public String name() {
		return name;
	}

	public int dropRate() {
		return dropRate;
	}

	public int durationMinutes() {
		return durationMinutes;
	}

	public int stageKills() {
		return stageKills;
	}

	public int stage() {
		return stage;
	}

	public void stage(int stage) {
		this.stage = Math.max(1, Math.min(10, stage));
	}

	public int kills() {
		return kills;
	}

	public void kills(int kills) {
		this.kills = kills;
		this.stage = Math.max(1, Math.min(10, (kills / stageKills) + 1));
	}

	public void increaseKills() {
		this.kills++;
		this.stage = Math.max(1, Math.min(10, (kills / stageKills) + 1));
	}

	public int playerId() {
		return playerId;
	}

	public void playerId(int playerId) {
		this.playerId = playerId;
	}

	public String playerName() {
		return playerName;
	}

	public void playerName(String playerName) {
		this.playerName = playerName != null ? playerName : "";
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public void setLocation(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isDropped() {
		return dropped;
	}

	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	public long endTime() {
		return endTime;
	}

	public void endTime(long endTime) {
		this.endTime = endTime;
	}

	public void activate(int playerId, String playerName) {
		this.playerId = playerId;
		this.playerName = playerName != null ? playerName : "";
		this.active = true;
		this.dropped = false;
		this.endTime = System.currentTimeMillis() + ((long) durationMinutes * 60_000L);
	}

	public void dropOnGround(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.playerId = 0;
		this.playerName = "";
		this.active = false;
		this.dropped = true;
	}

	public void endOfLife() {
		this.active = false;
		this.dropped = false;
		this.playerId = 0;
		this.playerName = "";
		this.kills = 0;
		this.stage = 1;
		this.endTime = 0;
	}
}
