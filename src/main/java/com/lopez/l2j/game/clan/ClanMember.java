package com.lopez.l2j.game.clan;

/**
 * Representa um membro de cla (L2ClanMember do L2JDream).
 */
public class ClanMember {

	private final int objectId;
	private final String name;
	private volatile int level;
	private volatile int classId;
	private volatile String title;
	private volatile boolean online;
	private volatile int pledgeType; // 0 = main clan, subpledges etc.
	private volatile int powerGrade = 5;
	private volatile int sponsor;
	private volatile int apprentice;

	public ClanMember(int objectId, String name, int level, int classId, String title, boolean online, int pledgeType) {
		this.objectId = objectId;
		this.name = name;
		this.level = level;
		this.classId = classId;
		this.title = title != null ? title : "";
		this.online = online;
		this.pledgeType = pledgeType;
	}

	public int objectId() { return objectId; }
	public String name() { return name; }
	public int level() { return level; }
	public void level(int level) { this.level = level; }
	public int classId() { return classId; }
	public void classId(int classId) { this.classId = classId; }
	public String title() { return title; }
	public void title(String title) { this.title = title != null ? title : ""; }
	public boolean isOnline() { return online; }
	public void setOnline(boolean online) { this.online = online; }
	public int pledgeType() { return pledgeType; }
	public void pledgeType(int pledgeType) { this.pledgeType = pledgeType; }
	public int powerGrade() { return powerGrade; }
	public void powerGrade(int powerGrade) { this.powerGrade = powerGrade; }
	public int sponsor() { return sponsor; }
	public void sponsor(int sponsor) { this.sponsor = sponsor; }
	public int apprentice() { return apprentice; }
	public void apprentice(int apprentice) { this.apprentice = apprentice; }

	public com.lopez.l2j.game.model.PlayerCharacter getPlayer() {
		com.lopez.l2j.game.world.GameWorld world = com.lopez.l2j.game.world.GameWorld.getInstance();
		return world != null ? world.getPlayer(objectId) : null;
	}
}
