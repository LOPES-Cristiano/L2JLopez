package com.lopez.l2j.game.clan.privilege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanPrivilegeServiceTest {

	private ClanTable clanTable;
	private ClanPrivilegeService privService;

	private PlayerCharacter leader;
	private PlayerCharacter member;
	private Clan clan;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		privService = new ClanPrivilegeService(clanTable, null);

		leader = dummyPlayer(101, "LeaderOne", 75);
		leader.clanId(1);

		member = dummyPlayer(102, "MemberOne", 65);
		member.clanId(1);

		clan = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		clan.reputationScore(20000);

		clan.addMember(new ClanMember(101, "LeaderOne", 75, 88, "", true, 0));
		ClanMember regularMember = new ClanMember(102, "MemberOne", 65, 89, "", true, 0);
		regularMember.powerGrade(5);
		clan.addMember(regularMember);

		clanTable.registerClan(clan);
	}

	@Test
	void testLeaderAlwaysHasPrivilege() {
		// Leader has all privileges unconditionally
		assertTrue(privService.hasPrivilege(leader, Clan.CP_CL_PLEDGE_WAR));
		assertTrue(privService.hasPrivilege(leader, Clan.CP_CL_DISMISS));
		assertTrue(privService.hasPrivilege(leader, Clan.CP_CS_MANOR_ADMIN));
	}

	@Test
	void testMemberPrivilegesByRank() {
		// Regular member has no privileges initially
		assertFalse(privService.hasPrivilege(member, Clan.CP_CL_VIEW_WAREHOUSE));
		assertFalse(privService.hasPrivilege(member, Clan.CP_CL_PLEDGE_WAR));

		// Grant warehouse and war privileges to rank 5
		int granted = Clan.CP_CL_VIEW_WAREHOUSE | Clan.CP_CL_PLEDGE_WAR;
		assertTrue(privService.setRankPrivilege(leader, 5, granted));

		assertTrue(privService.hasPrivilege(member, Clan.CP_CL_VIEW_WAREHOUSE));
		assertTrue(privService.hasPrivilege(member, Clan.CP_CL_PLEDGE_WAR));
		assertFalse(privService.hasPrivilege(member, Clan.CP_CL_DISMISS)); // not granted
	}

	@Test
	void testMemberPowerGradeChange() {
		assertTrue(privService.setMemberPowerGrade(leader, 102, 4));
		assertEquals(4, clan.getMember(102).powerGrade());
	}

	@Test
	void testCreateSubPledges() {
		// Create Academy (Lvl 5, cost 0 rep)
		assertEquals(ClanPrivilegeService.SubPledgeResult.SUCCESS,
				privService.createSubPledge(leader, Clan.SUBUNIT_ACADEMY, "Alpha Academy", 0));
		assertTrue(clan.subPledges().containsKey(Clan.SUBUNIT_ACADEMY));

		// Royal Guard requires lvl 6
		assertEquals(ClanPrivilegeService.SubPledgeResult.CLAN_LEVEL_TOO_LOW,
				privService.createSubPledge(leader, Clan.SUBUNIT_ROYAL1, "Royal Guard 1", 102));

		// Level up to 6
		clan.level(6);
		assertEquals(ClanPrivilegeService.SubPledgeResult.SUCCESS,
				privService.createSubPledge(leader, Clan.SUBUNIT_ROYAL1, "Royal Guard 1", 102));
		assertTrue(clan.subPledges().containsKey(Clan.SUBUNIT_ROYAL1));
		assertEquals(15000, clan.reputationScore()); // 20000 - 5000 = 15000

		// Order of Knights requires lvl 7
		assertEquals(ClanPrivilegeService.SubPledgeResult.CLAN_LEVEL_TOO_LOW,
				privService.createSubPledge(leader, Clan.SUBUNIT_KNIGHT1, "Knights 1", 102));

		clan.level(7);
		assertEquals(ClanPrivilegeService.SubPledgeResult.SUCCESS,
				privService.createSubPledge(leader, Clan.SUBUNIT_KNIGHT1, "Knights 1", 102));
		assertTrue(clan.subPledges().containsKey(Clan.SUBUNIT_KNIGHT1));
		assertEquals(5000, clan.reputationScore()); // 15000 - 10000 = 5000
	}
}
