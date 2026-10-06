package com.lopez.l2j.game.clan.alliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AllianceServiceTest {

	private ClanTable clanTable;
	private AllianceService allianceService;

	private PlayerCharacter leader1;
	private Clan clan1;

	private PlayerCharacter leader2;
	private Clan clan2;

	private PlayerCharacter leader3;
	private Clan clan3;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		allianceService = new AllianceService(clanTable, null, null);

		// Clan 1 - Leader Lvl 5+
		leader1 = dummyPlayer(101, "LeaderOne", 75);
		leader1.clanId(1);
		clan1 = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		clan1.addMember(new ClanMember(101, "LeaderOne", 75, 88, "", true, 0));
		clanTable.registerClan(clan1);

		// Clan 2 - Leader Lvl 5+
		leader2 = dummyPlayer(102, "LeaderTwo", 75);
		leader2.clanId(2);
		clan2 = new Clan(2, "ClanBeta", 102, "LeaderTwo", 5);
		clan2.addMember(new ClanMember(102, "LeaderTwo", 75, 89, "", true, 0));
		clanTable.registerClan(clan2);

		// Clan 3 - Leader Lvl 5+
		leader3 = dummyPlayer(103, "LeaderThree", 75);
		leader3.clanId(3);
		clan3 = new Clan(3, "ClanGamma", 103, "LeaderThree", 5);
		clan3.addMember(new ClanMember(103, "LeaderThree", 75, 90, "", true, 0));
		clanTable.registerClan(clan3);
	}

	@Test
	void testCreateAllianceSuccessAndValidation() {
		// Low level clan fails
		clan1.level(4);
		assertEquals(AllianceService.CreateResult.LEVEL_TOO_LOW,
				allianceService.createAlliance(leader1, "AllianceOne"));

		// Level 5 succeeds
		clan1.level(5);
		assertEquals(AllianceService.CreateResult.SUCCESS,
				allianceService.createAlliance(leader1, "AllianceOne"));
		assertEquals(1, clan1.allyId());
		assertEquals("AllianceOne", clan1.allyName());

		// Cannot create again if already in ally
		assertEquals(AllianceService.CreateResult.ALREADY_IN_ALLIANCE,
				allianceService.createAlliance(leader1, "AllianceTwo"));

		// Duplicate name fails for another clan
		assertEquals(AllianceService.CreateResult.NAME_ALREADY_EXISTS,
				allianceService.createAlliance(leader2, "AllianceOne"));
	}

	@Test
	void testInviteAndJoinAlliance() {
		// Create ally
		allianceService.createAlliance(leader1, "AlliedForces");

		// Valid invite condition
		assertEquals(AllianceService.InviteResult.SUCCESS,
				allianceService.checkAllyJoinCondition(leader1, leader2));

		// Add clan 2 to alliance
		assertTrue(allianceService.addClanToAlliance(clan1.allyId(), clan2));
		assertEquals(clan1.allyId(), clan2.allyId());
		assertEquals("AlliedForces", clan2.allyName());

		// Add clan 3 to alliance (reaches max 3)
		assertTrue(allianceService.addClanToAlliance(clan1.allyId(), clan3));

		// Attempting 4th clan fails limit check
		PlayerCharacter leader4 = dummyPlayer(104, "LeaderFour", 75);
		leader4.clanId(4);
		Clan clan4 = new Clan(4, "ClanDelta", 104, "LeaderFour", 5);
		clanTable.registerClan(clan4);

		assertEquals(AllianceService.InviteResult.ALLIANCE_FULL,
				allianceService.checkAllyJoinCondition(leader1, leader4));
		assertFalse(allianceService.addClanToAlliance(clan1.allyId(), clan4));
	}

	@Test
	void testLeaveAlliance() {
		allianceService.createAlliance(leader1, "AlliedForces");
		allianceService.addClanToAlliance(clan1.allyId(), clan2);

		// Leader clan cannot leave
		assertEquals(AllianceService.LeaveResult.LEADER_CANNOT_LEAVE,
				allianceService.leaveAlliance(leader1));

		// Member clan leaves successfully
		assertEquals(AllianceService.LeaveResult.SUCCESS,
				allianceService.leaveAlliance(leader2));
		assertEquals(0, clan2.allyId());
		assertNull(clan2.allyName());
		assertEquals(Clan.PENALTY_TYPE_CLAN_LEAVED, clan2.allyPenaltyType());
		assertTrue(clan2.allyPenaltyExpiryTime() > System.currentTimeMillis());
	}

	@Test
	void testDismissClan() {
		allianceService.createAlliance(leader1, "AlliedForces");
		allianceService.addClanToAlliance(clan1.allyId(), clan2);

		// Dismiss member clan
		assertEquals(AllianceService.DismissResult.SUCCESS,
				allianceService.dismissClan(leader1, clan2));
		assertEquals(0, clan2.allyId());
		assertEquals(Clan.PENALTY_TYPE_CLAN_DISMISSED, clan2.allyPenaltyType());
		assertEquals(Clan.PENALTY_TYPE_DISMISS_CLAN, clan1.allyPenaltyType());
	}

	@Test
	void testDissolveAlliance() {
		allianceService.createAlliance(leader1, "AlliedForces");
		allianceService.addClanToAlliance(clan1.allyId(), clan2);

		assertEquals(AllianceService.DissolveResult.SUCCESS,
				allianceService.dissolveAlliance(leader1));
		assertEquals(0, clan1.allyId());
		assertEquals(0, clan2.allyId());
		assertEquals(Clan.PENALTY_TYPE_DISSOLVE_ALLY, clan1.allyPenaltyType());
	}

	@Test
	void testSetAllyCrestPropagates() {
		allianceService.createAlliance(leader1, "AlliedForces");
		allianceService.addClanToAlliance(clan1.allyId(), clan2);

		assertTrue(allianceService.setAllyCrest(leader1, 9999));
		assertEquals(9999, clan1.allyCrestId());
		assertEquals(9999, clan2.allyCrestId());
	}
}
