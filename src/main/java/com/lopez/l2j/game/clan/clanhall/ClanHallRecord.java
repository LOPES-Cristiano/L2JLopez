package com.lopez.l2j.game.clan.clanhall;

/**
 * Representa um Clan Hall do mundo (ClanHall / clanhall table do L2JDream).
 */
public class ClanHallRecord {

	private final int id;
	private final String name;
	private volatile int ownerId;
	private volatile int lease;
	private final String desc;
	private final String location;
	private volatile long paidUntil;
	private volatile long paidDayTime;
	private final int grade;
	private volatile boolean paid;
	private final boolean siegeType;

	public ClanHallRecord(int id, String name, int ownerId, int lease, String desc,
			String location, long paidUntil, long paidDayTime, int grade, boolean paid, boolean siegeType) {
		this.id = id;
		this.name = name != null ? name : "";
		this.ownerId = ownerId;
		this.lease = lease;
		this.desc = desc != null ? desc : "";
		this.location = location != null ? location : "";
		this.paidUntil = paidUntil;
		this.paidDayTime = paidDayTime;
		this.grade = grade;
		this.paid = paid;
		this.siegeType = siegeType;
	}

	public int id() { return id; }
	public String name() { return name; }
	public int ownerId() { return ownerId; }
	public void ownerId(int ownerId) { this.ownerId = ownerId; }
	public int lease() { return lease; }
	public void lease(int lease) { this.lease = lease; }
	public String desc() { return desc; }
	public String location() { return location; }
	public long paidUntil() { return paidUntil; }
	public void paidUntil(long paidUntil) { this.paidUntil = paidUntil; }
	public long paidDayTime() { return paidDayTime; }
	public void paidDayTime(long paidDayTime) { this.paidDayTime = paidDayTime; }
	public int grade() { return grade; }
	public boolean isPaid() { return paid; }
	public void setPaid(boolean paid) { this.paid = paid; }
	public boolean isSiegeType() { return siegeType; }
}
