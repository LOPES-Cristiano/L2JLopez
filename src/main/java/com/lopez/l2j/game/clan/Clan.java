package com.lopez.l2j.game.clan;

import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa um cla no mundo (L2Clan do L2JDream).
 * Gerencia niveis, membros, lideranca, brasao, castelo e reputacao.
 */
public class Clan {

	private final int clanId;
	private final String name;
	private volatile int leaderId;
	private volatile String leaderName;
	private volatile int level;
	private volatile int castleId;
	private volatile int fortId;
	private volatile int crestId;
	private volatile int crestLargeId;
	private volatile int allyId;
	private volatile String allyName;
	private volatile int allyCrestId;
	private volatile int reputationScore;
	private volatile int rank;
	private volatile String notice = "";
	private volatile boolean noticeEnabled;
	private volatile int clanHallId;
	private volatile int auctionBiddedAt;
	private volatile long allyPenaltyExpiryTime;
	private volatile int allyPenaltyType;
	private volatile long charPenaltyExpiryTime;
	private volatile long dissolvingExpiryTime;
	private volatile int newLeaderId;

	public static final int PENALTY_TYPE_NONE = 0;
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;

	// Clan Privileges (CP_*)
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024;
	public static final int CP_CH_OTHER_RIGHTS = 2048;
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192;
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536;
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288;
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;

	// Sub-units
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;

	private final Map<Integer, ClanMember> members = new ConcurrentHashMap<>();
	private final java.util.Set<Integer> enemyClanIds = ConcurrentHashMap.newKeySet();
	private final java.util.Set<Integer> attackerClanIds = ConcurrentHashMap.newKeySet();
	private final Map<Integer, Long> warPenaltyExpiryTimes = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> skills = new ConcurrentHashMap<>(); // skillId -> level
	private final Map<Integer, Integer> rankPrivileges = new ConcurrentHashMap<>(); // rank (1-9) -> bitmask
	private final Map<Integer, SubPledgeRecord> subPledges = new ConcurrentHashMap<>(); // type -> SubPledgeRecord

	public record SubPledgeRecord(int type, String name, int leaderId) {}

	public Clan(int clanId, String name, int leaderId, String leaderName, int level) {
		this.clanId = clanId;
		this.name = name;
		this.leaderId = leaderId;
		this.leaderName = leaderName;
		this.level = level;
	}

	public int clanId() { return clanId; }
	public String name() { return name; }
	public int leaderId() { return leaderId; }
	public void leaderId(int leaderId) { this.leaderId = leaderId; }
	public String leaderName() { return leaderName; }
	public void leaderName(String leaderName) { this.leaderName = leaderName; }
	public int level() { return level; }
	public void level(int level) { this.level = level; }
	public int castleId() { return castleId; }
	public void castleId(int castleId) { this.castleId = castleId; }
	public int fortId() { return fortId; }
	public void fortId(int fortId) { this.fortId = fortId; }
	public int crestId() { return crestId; }
	public void crestId(int crestId) { this.crestId = crestId; }
	public int crestLargeId() { return crestLargeId; }
	public void crestLargeId(int crestLargeId) { this.crestLargeId = crestLargeId; }
	public int allyId() { return allyId; }
	public void allyId(int allyId) { this.allyId = allyId; }
	public String allyName() { return allyName; }
	public void allyName(String allyName) { this.allyName = allyName; }
	public int allyCrestId() { return allyCrestId; }
	public void allyCrestId(int allyCrestId) { this.allyCrestId = allyCrestId; }
	public int reputationScore() { return reputationScore; }
	public void reputationScore(int score) { this.reputationScore = score; }
	public int rank() { return rank; }
	public void rank(int rank) { this.rank = rank; }
	public String notice() { return notice; }
	public void notice(String notice) { this.notice = notice != null ? notice : ""; }
	public boolean isNoticeEnabled() { return noticeEnabled; }
	public void setNoticeEnabled(boolean enabled) { this.noticeEnabled = enabled; }

	public long charPenaltyExpiryTime() { return charPenaltyExpiryTime; }
	public void charPenaltyExpiryTime(long time) { this.charPenaltyExpiryTime = time; }
	public boolean hasCharPenalty() { return System.currentTimeMillis() < charPenaltyExpiryTime; }

	public long allyPenaltyExpiryTime() { return allyPenaltyExpiryTime; }
	public void allyPenaltyExpiryTime(long time) { this.allyPenaltyExpiryTime = time; }
	public boolean hasAllyPenalty() { return System.currentTimeMillis() < allyPenaltyExpiryTime; }

	public boolean isLeader(int objectId) {
		return leaderId == objectId;
	}

	public ClanMember leader() {
		return members.get(leaderId);
	}

	public ClanMember getMember(int objectId) {
		return members.get(objectId);
	}

	public Collection<ClanMember> members() {
		return Collections.unmodifiableCollection(members.values());
	}

	public int membersCount() {
		return members.size();
	}

	public void addMember(ClanMember member) {
		if (member != null) {
			members.put(member.objectId(), member);
		}
	}

	public ClanMember removeMember(int objectId) {
		return members.remove(objectId);
	}

	public int clanHallId() { return clanHallId; }
	public void clanHallId(int clanHallId) { this.clanHallId = clanHallId; }
	public boolean hasClanHall() { return clanHallId > 0; }
	public boolean hasCastle() { return castleId > 0; }
	public int auctionBiddedAt() { return auctionBiddedAt; }
	public void auctionBiddedAt(int auctionBiddedAt) { this.auctionBiddedAt = auctionBiddedAt; }

	public int allyPenaltyType() { return allyPenaltyType; }
	public void setAllyPenalty(long expiryTime, int penaltyType) {
		this.allyPenaltyExpiryTime = expiryTime;
		this.allyPenaltyType = penaltyType;
	}

	public long dissolvingExpiryTime() { return dissolvingExpiryTime; }
	public void dissolvingExpiryTime(long time) { this.dissolvingExpiryTime = time; }
	public int newLeaderId() { return newLeaderId; }
	public void newLeaderId(int newLeaderId) { this.newLeaderId = newLeaderId; }

	// Clan Wars
	public java.util.Set<Integer> enemyClanIds() { return Collections.unmodifiableSet(enemyClanIds); }
	public void addEnemyClan(int clanId) { enemyClanIds.add(clanId); }
	public void removeEnemyClan(int clanId) { enemyClanIds.remove(clanId); }
	public boolean isEnemyClan(int clanId) { return enemyClanIds.contains(clanId); }

	public java.util.Set<Integer> attackerClanIds() { return Collections.unmodifiableSet(attackerClanIds); }
	public void addAttackerClan(int clanId) { attackerClanIds.add(clanId); }
	public void removeAttackerClan(int clanId) { attackerClanIds.remove(clanId); }
	public boolean isAttackerClan(int clanId) { return attackerClanIds.contains(clanId); }

	public boolean isAtWarWith(int clanId) { return enemyClanIds.contains(clanId); }
	public boolean isMutualWarWith(int clanId) { return enemyClanIds.contains(clanId) && attackerClanIds.contains(clanId); }

	public void addWarPenaltyTime(int clanId, long expiryTime) { warPenaltyExpiryTimes.put(clanId, expiryTime); }
	public long getWarPenaltyExpiryTime(int clanId) { return warPenaltyExpiryTimes.getOrDefault(clanId, 0L); }
	public boolean hasWarPenalty(int clanId) {
		Long expiry = warPenaltyExpiryTimes.get(clanId);
		return expiry != null && expiry > System.currentTimeMillis();
	}

	// Clan Skills
	public Map<Integer, Integer> skills() { return Collections.unmodifiableMap(skills); }
	public void addSkill(int skillId, int level) { skills.put(skillId, level); }
	public void removeSkill(int skillId) { skills.remove(skillId); }
	public int getSkillLevel(int skillId) { return skills.getOrDefault(skillId, 0); }

	// Privileges by rank (1 to 9)
	public int getRankPrivilege(int rank) { return rankPrivileges.getOrDefault(rank, 0); }
	public void setRankPrivilege(int rank, int privs) { rankPrivileges.put(rank, privs); }
	public Map<Integer, Integer> rankPrivileges() { return Collections.unmodifiableMap(rankPrivileges); }

	// Sub-pledges
	public Map<Integer, SubPledgeRecord> subPledges() { return Collections.unmodifiableMap(subPledges); }
	public SubPledgeRecord getSubPledge(int type) { return subPledges.get(type); }
	public void addSubPledge(int type, String name, int leaderId) {
		subPledges.put(type, new SubPledgeRecord(type, name, leaderId));
	}

	/**
	 * Envia um pacote para todos os membros online do cla conectados ao mundo.
	 */
	public void broadcastToOnlineMembers(GameWorld world, GameServerPacket packet) {
		if (world == null || packet == null) {
			return;
		}
		for (ClanMember m : members.values()) {
			var player = world.player(m.objectId());
			player.ifPresent(p -> p.send(packet));
		}
	}
}

