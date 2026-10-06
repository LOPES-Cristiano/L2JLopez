package com.lopez.l2j.game.clan.war;

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

class ClanWarServiceTest {

	private ClanTable clanTable;
	private ClanWarService warService;

	private PlayerCharacter leader1;
	private Clan clan1;

	private PlayerCharacter leader2;
	private Clan clan2;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		warService = new ClanWarService(clanTable, null);

		leader1 = dummyPlayer(101, "LeaderOne", 75);
		leader1.clanId(1);
		clan1 = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		for (int i = 0; i < 16; i++) {
			clan1.addMember(new ClanMember(200 + i, "MemberA" + i, 60, 1, "", true, 0));
		}
		clanTable.registerClan(clan1);

		leader2 = dummyPlayer(102, "LeaderTwo", 75);
		leader2.clanId(2);
		clan2 = new Clan(2, "ClanBeta", 102, "LeaderTwo", 5);
		for (int i = 0; i < 16; i++) {
			clan2.addMember(new ClanMember(300 + i, "MemberB" + i, 60, 1, "", true, 0));
		}
		clanTable.registerClan(clan2);
	}

	@Test
	void testDeclareWarValidationAndSuccess() {
		// Low level fails
		clan1.level(2);
		assertEquals(ClanWarService.DeclareResult.CLAN_LEVEL_OR_MEMBERS_TOO_LOW,
				warService.declareWar(leader1, "ClanBeta"));
		clan1.level(5);

		// Same clan fails
		assertEquals(ClanWarService.DeclareResult.SAME_CLAN,
				warService.declareWar(leader1, "ClanAlpha"));

		// Target not found
		assertEquals(ClanWarService.DeclareResult.TARGET_NOT_FOUND,
				warService.declareWar(leader1, "NonExistingClan"));

		// Same alliance fails
		clan1.allyId(99);
		clan2.allyId(99);
		assertEquals(ClanWarService.DeclareResult.SAME_ALLIANCE,
				warService.declareWar(leader1, "ClanBeta"));
		clan1.allyId(0);
		clan2.allyId(0);

		// Successful declaration
		assertEquals(ClanWarService.DeclareResult.SUCCESS,
				warService.declareWar(leader1, "ClanBeta"));
		assertTrue(warService.isAtWar(1, 2));
		assertFalse(warService.isMutualWar(1, 2));
		assertFalse(warService.canAttackWithoutPenalty(leader1, leader2));

		// Cannot declare again
		assertEquals(ClanWarService.DeclareResult.ALREADY_AT_WAR,
				warService.declareWar(leader1, "ClanBeta"));

		// Clan 2 counter-declares -> becomes Mutual War
		assertEquals(ClanWarService.DeclareResult.SUCCESS,
				warService.declareWar(leader2, "ClanAlpha"));
		assertTrue(warService.isMutualWar(1, 2));
		assertTrue(warService.canAttackWithoutPenalty(leader1, leader2));
	}

	@Test
	void testStopWarAndCooldownPenalty() {
		warService.declareWar(leader1, "ClanBeta");
		assertTrue(warService.isAtWar(1, 2));

		// Stop war
		assertEquals(ClanWarService.StopResult.SUCCESS,
				warService.stopWar(leader1, "ClanBeta"));
		assertFalse(warService.isAtWar(1, 2));

		// Cooldown penalty active for Clan 1 against Clan 2
		assertTrue(clan1.hasWarPenalty(2));
		assertEquals(ClanWarService.DeclareResult.PENALTY_ACTIVE,
				warService.declareWar(leader1, "ClanBeta"));
	}
}
