package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.arenaduel.ArenaDuelService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.tournament.TournamentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArenaAndTournamentConfigurationTest {

	private ArenaDuelService arenaDuelService;
	private TournamentService tournamentService;

	private PlayerCharacter createPlayer(int objId, String name) {
		return new PlayerCharacter(objId, "acc", name, 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		Config.load();
		arenaDuelService = new ArenaDuelService();
		tournamentService = new TournamentService();
	}

	@Test
	void testArenaDuelConfigLoading() {
		assertTrue(Config.ARENA_DUEL_ENABLE);
		assertEquals(15, Config.ARENA_DUEL_CHECK_INTERVAL);
		assertEquals(60, Config.ARENA_DUEL_CALL_INTERVAL);
		assertEquals(20, Config.ARENA_DUEL_WAIT_INTERVAL);
		assertEquals(List.of(1538, 5858), Config.ARENA_DUEL_ITEMS_RESTRICTION);
		assertTrue(Config.ARENA_ALLOW_S);
		assertEquals("3470,5", Config.ARENA_DUEL_REWARD);
	}

	@Test
	void testTournamentConfigLoading() {
		assertFalse(Config.TOURNAMENT_1X1_ENABLE);
		assertEquals(15, Config.TOURNAMENT_CHECK_INTERVAL);
		assertEquals(60, Config.TOURNAMENT_CALL_INTERVAL);
		assertEquals(20, Config.TOURNAMENT_WAIT_INTERVAL);
		assertEquals(List.of(1538, 5858), Config.TOURNAMENT_ITEMS_RESTRICTION);
		assertEquals("3470,5", Config.TOURNAMENT_1X1_REWARD);
		assertFalse(Config.TOURNAMENT_1X1_HWID_BLOCK);
	}

	@Test
	void testArenaDuelRegistrationLifecycle() {
		PlayerCharacter player1 = createPlayer(5001, "DuelistA");
		PlayerCharacter player2 = createPlayer(5002, "DuelistB");

		assertFalse(arenaDuelService.isRegistered(player1.objectId()));

		assertTrue(arenaDuelService.register(player1));
		assertTrue(arenaDuelService.isRegistered(player1.objectId()));

		// Duplicate registration rejected
		assertFalse(arenaDuelService.register(player1));

		// Unregister
		assertTrue(arenaDuelService.unregister(player1));
		assertFalse(arenaDuelService.isRegistered(player1.objectId()));
	}

	@Test
	void testTournamentRegistrationLifecycle() {
		PlayerCharacter player1 = createPlayer(6001, "TourPlayerA");
		PlayerCharacter player2 = createPlayer(6002, "TourPlayerB");

		assertFalse(tournamentService.isRegistered(player1.objectId()));

		assertTrue(tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(player1)));
		assertTrue(tournamentService.isRegistered(player1.objectId()));

		// Duplicate registration rejected
		assertFalse(tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(player1)));

		// Unregister
		assertTrue(tournamentService.unregister(player1.objectId()));
		assertFalse(tournamentService.isRegistered(player1.objectId()));
	}
}
