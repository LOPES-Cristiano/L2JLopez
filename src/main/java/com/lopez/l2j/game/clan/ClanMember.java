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

	/**
	 * Calcula a classe social (Pledge Class / Rank Social 1..11) do membro conforme nivel do cla,
	 * posse de castelo, cargo de lider/sub-lider, sub-unidade e status de Nobless/Hero (#381).
	 */
	public int calculatePledgeClass(Clan clan) {
		return calculatePledgeClass(getPlayer(), clan, this);
	}

	public static int calculatePledgeClass(com.lopez.l2j.game.model.PlayerCharacter player, Clan clan) {
		if (clan == null) {
			return player != null && player.isHero() ? 8 : (player != null && player.isNoble() ? 5 : 0);
		}
		ClanMember member = player != null ? clan.getMember(player.objectId()) : null;
		return calculatePledgeClass(player, clan, member);
	}

	public static int calculatePledgeClass(com.lopez.l2j.game.model.PlayerCharacter player, Clan clan, ClanMember member) {
		int pledgeClass = 0;
		if (clan != null && member != null) {
			int pledgeType = member.pledgeType();
			if (pledgeType == Clan.SUBUNIT_ACADEMY) {
				pledgeClass = 1;
			} else {
				boolean isLeader = clan.isLeader(member.objectId());
				boolean isRoyalLeader = isSubPledgeLeader(clan, member.objectId(), Clan.SUBUNIT_ROYAL1, Clan.SUBUNIT_ROYAL2);
				boolean isKnightLeader = isSubPledgeLeader(clan, member.objectId(),
						Clan.SUBUNIT_KNIGHT1, Clan.SUBUNIT_KNIGHT2, Clan.SUBUNIT_KNIGHT3, Clan.SUBUNIT_KNIGHT4);
				boolean isRoyalMember = pledgeType == Clan.SUBUNIT_ROYAL1 || pledgeType == Clan.SUBUNIT_ROYAL2;
				boolean isKnightMember = pledgeType == Clan.SUBUNIT_KNIGHT1 || pledgeType == Clan.SUBUNIT_KNIGHT2
						|| pledgeType == Clan.SUBUNIT_KNIGHT3 || pledgeType == Clan.SUBUNIT_KNIGHT4;
				boolean hasCastle = clan.hasCastle();

				switch (clan.level()) {
					case 4 -> pledgeClass = isLeader ? 3 : 1;
					case 5 -> pledgeClass = isLeader ? 4 : 2;
					case 6 -> {
						if (hasCastle) {
							if (isLeader) pledgeClass = 8;
							else if (isRoyalLeader) pledgeClass = 6;
							else if (isRoyalMember) pledgeClass = 4;
							else pledgeClass = 5;
						} else {
							if (isLeader) pledgeClass = 5;
							else if (isRoyalLeader) pledgeClass = 4;
							else pledgeClass = 3;
						}
					}
					case 7 -> {
						if (hasCastle) {
							if (isLeader) pledgeClass = 9;
							else if (isRoyalLeader) pledgeClass = 7;
							else if (isKnightLeader) pledgeClass = 6;
							else if (isRoyalMember) pledgeClass = 5;
							else if (isKnightMember) pledgeClass = 4;
							else pledgeClass = 5;
						} else {
							if (isLeader) pledgeClass = 6;
							else if (isRoyalLeader) pledgeClass = 5;
							else if (isKnightLeader) pledgeClass = 4;
							else if (isRoyalMember) pledgeClass = 4;
							else if (isKnightMember) pledgeClass = 3;
							else pledgeClass = 4;
						}
					}
					case 8, 9, 10 -> {
						if (hasCastle) {
							if (isLeader) pledgeClass = 10;
							else if (isRoyalLeader) pledgeClass = 8;
							else if (isKnightLeader) pledgeClass = 7;
							else if (isRoyalMember) pledgeClass = 6;
							else if (isKnightMember) pledgeClass = 5;
							else pledgeClass = 6;
						} else {
							if (isLeader) pledgeClass = 7;
							else if (isRoyalLeader) pledgeClass = 6;
							else if (isKnightLeader) pledgeClass = 5;
							else if (isRoyalMember) pledgeClass = 5;
							else if (isKnightMember) pledgeClass = 4;
							else pledgeClass = 5;
						}
					}
					default -> pledgeClass = 1;
				}
			}
		}
		if (player != null) {
			if (player.isNoble() && pledgeClass < 5) {
				pledgeClass = 5;
			}
			if (player.isHero() && pledgeClass < 8) {
				pledgeClass = 8;
			}
		}
		return pledgeClass;
	}

	private static boolean isSubPledgeLeader(Clan clan, int objectId, int... types) {
		for (int t : types) {
			Clan.SubPledgeRecord rec = clan.getSubPledge(t);
			if (rec != null && rec.leaderId() == objectId) {
				return true;
			}
		}
		return false;
	}

	public com.lopez.l2j.game.model.PlayerCharacter getPlayer() {
		com.lopez.l2j.game.world.GameWorld world = com.lopez.l2j.game.world.GameWorld.getInstance();
		return world != null ? world.getPlayer(objectId) : null;
	}
}
