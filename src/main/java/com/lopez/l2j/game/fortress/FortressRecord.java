package com.lopez.l2j.game.fortress;

/**
 * Representa uma das 21 fortalezas de Lineage II Interlude (tabela fort).
 */
public class FortressRecord {

	public static final int TYPE_SMALL = 0;
	public static final int TYPE_LARGE = 1;

	public static final int STATE_NONE = 0;
	public static final int STATE_CONTRACTED = 1;
	public static final int STATE_INDEPENDENT = 2;

	private final int id;
	private final String name;
	private volatile long siegeDate;
	private volatile long lastOwnedTime;
	private volatile int ownerClanId;
	private final int fortType;
	private volatile int state; // 0=None, 1=Contracted, 2=Independent
	private volatile int castleId; // Castelo ao qual a fortaleza e territorialmente vinculada

	public FortressRecord(int id, String name, long siegeDate, long lastOwnedTime, int ownerClanId, int fortType, int state, int castleId) {
		this.id = id;
		this.name = name;
		this.siegeDate = siegeDate;
		this.lastOwnedTime = lastOwnedTime;
		this.ownerClanId = ownerClanId;
		this.fortType = fortType;
		this.state = state;
		this.castleId = castleId;
	}

	public int id() { return id; }
	public String name() { return name; }
	public long siegeDate() { return siegeDate; }
	public void siegeDate(long siegeDate) { this.siegeDate = siegeDate; }
	public long lastOwnedTime() { return lastOwnedTime; }
	public void lastOwnedTime(long lastOwnedTime) { this.lastOwnedTime = lastOwnedTime; }
	public int ownerClanId() { return ownerClanId; }
	public void ownerClanId(int ownerClanId) { this.ownerClanId = ownerClanId; }
	public boolean hasOwner() { return ownerClanId > 0; }
	public int fortType() { return fortType; }
	public int state() { return state; }
	public void state(int state) { this.state = state; }
	public int castleId() { return castleId; }
	public void castleId(int castleId) { this.castleId = castleId; }
}
