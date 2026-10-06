package com.lopez.l2j.game.clan.clanhall.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.clanhall.ClanHallService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanHallSiegeServiceTest {

	private ClanTable clanTable;
	private ClanHallService clanHallService;
	private ClanHallSiegeService siegeService;

	private PlayerCharacter leader1;
	private Clan clan1;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		clanHallService = new ClanHallService(clanTable, null);
		siegeService = new ClanHallSiegeService(clanHallService, clanTable, null);

		leader1 = dummyPlayer(101, "LeaderOne", 75);
		leader1.clanId(1);
		clan1 = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		clan1.addMember(new ClanMember(101, "LeaderOne", 75, 88, "", true, 0));
		clanTable.registerClan(clan1);
	}

	@Test
	void testRegistrationValidation() {
		int hallId = 35; // Bandit Stronghold

		// Level < 4 fails
		clan1.level(3);
		assertEquals(ClanHallSiegeService.RegisterResult.CLAN_LEVEL_TOO_LOW,
				siegeService.registerClan(leader1, hallId));

		clan1.level(5);

		// Non-leader fails
		PlayerCharacter nonLeader = dummyPlayer(102, "Member", 70);
		nonLeader.clanId(1);
		assertEquals(ClanHallSiegeService.RegisterResult.NOT_CLAN_LEADER,
				siegeService.registerClan(nonLeader, hallId));

		// Success registration
		assertEquals(ClanHallSiegeService.RegisterResult.SUCCESS,
				siegeService.registerClan(leader1, hallId));

		// Already registered fails
		assertEquals(ClanHallSiegeService.RegisterResult.ALREADY_REGISTERED,
				siegeService.registerClan(leader1, hallId));

		// Unregister
		assertTrue(siegeService.unregisterClan(leader1, hallId));
		assertFalse(siegeService.getSiege(hallId).orElseThrow().isRegistered(1));
	}

	@Test
	void testSiegeLifecycleAndWinner() {
		int hallId = 34; // Devastated Castle

		siegeService.registerClan(leader1, hallId);

		// Start siege
		assertTrue(siegeService.startSiege(hallId));
		assertTrue(siegeService.isUnderSiege(hallId));

		// End siege with winner
		assertTrue(siegeService.endSiege(hallId, clan1.clanId()));
		assertFalse(siegeService.isUnderSiege(hallId));

		// Winner became owner of the clan hall
		var hall = clanHallService.getClanHall(hallId).orElseThrow();
		assertEquals(clan1.clanId(), hall.ownerId());
		assertEquals(hallId, clan1.clanHallId());

		// Next siege is scheduled in the future and registered clans reset
		var siege = siegeService.getSiege(hallId).orElseThrow();
		assertTrue(siege.siegeDate() > System.currentTimeMillis());
		assertTrue(siege.registeredClanIds().isEmpty());
	}
}
