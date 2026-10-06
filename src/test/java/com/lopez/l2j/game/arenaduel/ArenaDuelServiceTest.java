package com.lopez.l2j.game.arenaduel;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaDuelServiceTest {

	private ArenaDuelService arenaDuelService;

	private PlayerCharacter createPlayer(int id, String name, int x, int y, int z) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, 80, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 3000, 1500, 1000, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 3000.0, 1500.0, 1000.0);
		player.teleport(x, y, z);
		return player;
	}

	@BeforeEach
	void setUp() {
		arenaDuelService = new ArenaDuelService();
	}

	@Test
	void testRegisterAndUnregister() {
		PlayerCharacter p1 = createPlayer(1, "GladiatorA", 83000, 148000, -3400);

		assertTrue(arenaDuelService.register(p1));
		assertTrue(arenaDuelService.isRegistered(1));
		assertEquals(1, arenaDuelService.getQueueSize());

		// Cannot register twice
		assertFalse(arenaDuelService.register(p1));

		// Unregister
		assertTrue(arenaDuelService.unregister(p1));
		assertFalse(arenaDuelService.isRegistered(1));
		assertEquals(0, arenaDuelService.getQueueSize());
	}

	@Test
	void testMatchmakingAndTeleport() {
		PlayerCharacter p1 = createPlayer(1, "GladiatorA", 83000, 148000, -3400);
		PlayerCharacter p2 = createPlayer(2, "GladiatorB", 83100, 148100, -3400);

		arenaDuelService.register(p1);
		arenaDuelService.register(p2);

		// With 2 players registered, matchmaking triggers automatically
		assertEquals(0, arenaDuelService.getQueueSize());
		assertEquals(1, arenaDuelService.getActiveMatches().size());
		assertTrue(arenaDuelService.isInMatch(1));
		assertTrue(arenaDuelService.isInMatch(2));

		ArenaMatch match = arenaDuelService.getPlayerMatch(1);
		assertNotNull(match);
		assertEquals(ArenaDuelState.COUNTDOWN, match.getState());

		// Start fight
		arenaDuelService.startFight(match.getMatchId());
		assertEquals(ArenaDuelState.FIGHTING, match.getState());

		// Winner p1
		arenaDuelService.finishMatch(match.getMatchId(), 1);
		assertFalse(arenaDuelService.isInMatch(1));
		assertFalse(arenaDuelService.isInMatch(2));
		assertEquals(0, arenaDuelService.getActiveMatches().size());

		// Returned to original coordinates
		assertEquals(83000, p1.x());
		assertEquals(148000, p1.y());
		assertEquals(83100, p2.x());
		assertEquals(148100, p2.y());
	}

	@Test
	void testPlayerDeathEndsMatch() {
		PlayerCharacter p1 = createPlayer(10, "FighterA", 10000, 20000, -3000);
		PlayerCharacter p2 = createPlayer(20, "FighterB", 10050, 20050, -3000);

		arenaDuelService.register(p1);
		arenaDuelService.register(p2);

		ArenaMatch match = arenaDuelService.getPlayerMatch(10);
		arenaDuelService.startFight(match.getMatchId());

		// FighterB dies
		arenaDuelService.onPlayerDeath(p2);

		// Match finishes, FighterA wins
		assertNull(arenaDuelService.getPlayerMatch(10));
		assertNull(arenaDuelService.getPlayerMatch(20));
		assertEquals(0, arenaDuelService.getActiveMatches().size());
	}
}
