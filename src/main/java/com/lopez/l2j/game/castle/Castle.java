package com.lopez.l2j.game.castle;

/**
 * Representa um dos 9 castelos de Lineage II Interlude.
 */
public class Castle {

	private final int id;
	private final String name;
	private int ownerClanId;
	private int taxPercent;
	private long treasury;
	private long siegeDate;

	public Castle(int id, String name) {
		this(id, name, 0, 0, 0L, 0L);
	}

	public Castle(int id, String name, int ownerClanId, int taxPercent, long treasury, long siegeDate) {
		this.id = id;
		this.name = name;
		this.ownerClanId = ownerClanId;
		this.taxPercent = Math.max(0, Math.min(15, taxPercent));
		this.treasury = Math.max(0, treasury);
		this.siegeDate = siegeDate;
	}

	public int id() {
		return id;
	}

	public String name() {
		return name;
	}

	public int ownerClanId() {
		return ownerClanId;
	}

	public void ownerClanId(int ownerClanId) {
		this.ownerClanId = ownerClanId;
	}

	public boolean hasOwner() {
		return ownerClanId > 0;
	}

	public int taxPercent() {
		return taxPercent;
	}

	public void taxPercent(int taxPercent) {
		this.taxPercent = Math.max(0, Math.min(15, taxPercent));
	}

	public long treasury() {
		return treasury;
	}

	public void treasury(long treasury) {
		this.treasury = Math.max(0, treasury);
	}

	public synchronized void addToTreasury(long amount) {
		if (amount > 0) {
			this.treasury += amount;
		}
	}

	public long siegeDate() {
		return siegeDate;
	}

	public void siegeDate(long siegeDate) {
		this.siegeDate = siegeDate;
	}
}
