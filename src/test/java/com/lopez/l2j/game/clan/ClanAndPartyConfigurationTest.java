package com.lopez.l2j.game.clan;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.alliance.AllianceService;
import com.lopez.l2j.game.clan.clanhall.ClanHallService;
import com.lopez.l2j.game.clan.war.ClanWarService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanAndPartyConfigurationTest {

	private ClanTable clanTable;
	private ClanWarService warService;
	private ClanHallService clanHallService;

	private PlayerCharacter createPlayer(int objId, String name, int level, int clanId) {
		return new PlayerCharacter(objId, "acc", name, level, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, clanId, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		Config.load();
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		warService = new ClanWarService(clanTable, null);
		clanHallService = new ClanHallService(clanTable, null);
	}

	@Test
	void testClanMaxMembersByLevel() {
		Clan clan0 = new Clan(1, "ClanZero", 100, "LeaderZero", 0);
		assertEquals(Config.MAX_MEMBERS_CLAN_0, clan0.getMaxMembers(0));

		clan0.level(1);
		assertEquals(Config.MAX_MEMBERS_CLAN_1, clan0.getMaxMembers(1));

		clan0.level(3);
		assertEquals(Config.MAX_MEMBERS_CLAN_3, clan0.getMaxMembers(3));

		clan0.level(8);
		assertEquals(Config.MAX_MEMBERS_CLAN_8, clan0.getMaxMembers(8));
		assertEquals(Config.MAX_MEMBERS_ROYALS, clan0.getMaxRoyalMembers());
		assertEquals(Config.MAX_MEMBERS_KNIGHTS, clan0.getMaxKnightMembers());
	}

	@Test
	void testClanWarMemberRequirements() {
		PlayerCharacter leaderA = createPlayer(101, "LeaderA", 75, 0);
		Clan clanA = clanTable.createClan(leaderA, "WarClanA");
		assertNotNull(clanA);
		clanA.level(5);

		PlayerCharacter leaderB = createPlayer(102, "LeaderB", 75, 0);
		Clan clanB = clanTable.createClan(leaderB, "WarClanB");
		assertNotNull(clanB);
		clanB.level(5);

		// With only 1 member, war declaration fails when ALT_CLAN_MEMBERS_FOR_WAR is 15
		var res = warService.declareWar(leaderA, "WarClanB");
		assertEquals(ClanWarService.DeclareResult.CLAN_LEVEL_OR_MEMBERS_TOO_LOW, res);

		// Add enough members to meet Config.ALT_CLAN_MEMBERS_FOR_WAR
		for (int i = 0; i < Config.ALT_CLAN_MEMBERS_FOR_WAR; i++) {
			clanA.addMember(new ClanMember(200 + i, "MemberA" + i, 40, 1, "Title", false, 0));
			clanB.addMember(new ClanMember(300 + i, "MemberB" + i, 40, 1, "Title", false, 0));
		}

		var successRes = warService.declareWar(leaderA, "WarClanB");
		assertEquals(ClanWarService.DeclareResult.SUCCESS, successRes);
	}

	@Test
	void testClanHallAuctionMinLevel() {
		PlayerCharacter leader = createPlayer(500, "BidLeader", 70, 0);
		Clan clan = clanTable.createClan(leader, "BidClan");
		assertNotNull(clan);
		assertEquals(0, clan.level());

		// Clan level 0 is below LVL_FOR_USE_AUCTION (default 2)
		var res = clanHallService.bidOnAuction(leader, 21, 10_000_000L);
		assertEquals(ClanHallService.BidResult.CLAN_LEVEL_TOO_LOW, res);

		clan.level(Config.LVL_FOR_USE_AUCTION);
		// Once level requirement is met, error moves to next validation (e.g. adena)
		var res2 = clanHallService.bidOnAuction(leader, 21, 10_000_000L);
		assertNotEquals(ClanHallService.BidResult.CLAN_LEVEL_TOO_LOW, res2);
	}

	@Test
	void testClanJoinPenaltyOnLeave() {
		PlayerCharacter leader = createPlayer(600, "PledgeBoss", 70, 0);
		Clan clan = clanTable.createClan(leader, "PenaltyClan");
		assertNotNull(clan);

		PlayerCharacter member = createPlayer(601, "MemberGuy", 70, 0);
		assertTrue(clanTable.addClanMember(clan, member));

		assertFalse(member.hasClanJoinPenalty());
		assertTrue(clanTable.removeClanMember(clan, member, false));
		assertTrue(member.hasClanJoinPenalty());
		assertTrue(member.clanJoinExpiryTime() >= System.currentTimeMillis() + (Config.DAYS_BEFORE_JOIN_A_CLAN * 86_400_000L - 5000L));
	}

	@Test
	void testAllianceMaxClansAndPenalties() {
		assertEquals(Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY, AllianceService.getMaxClansInAlliance());
		assertEquals(Config.DAYS_BEFORE_JOIN_ALLY_WHEN_LEAVED * 86_400_000L, AllianceService.getPenaltyLeaveDaysMillis());
		assertEquals(Config.DAYS_BEFORE_JOIN_ALLY_WHEN_DISMISSED * 86_400_000L, AllianceService.getPenaltyDismissDaysMillis());
	}
}
