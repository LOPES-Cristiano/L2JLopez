package com.lopez.l2j.game.reset;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CharacterResetServiceTest {

	private CharacterResetService characterResetService;

	private PlayerCharacter createPlayer(int id, String name, int level, int pvp, int classId) {
		return new PlayerCharacter(id, "acc", name, level, 500_000_000L, 500_000, 0, classId, classId, false,
				0, 0, 0, 3000, 1500, 1000, 0, pvp, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 3000.0, 1500.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		characterResetService = new CharacterResetService();
	}

	@Test
	void testCanResetValidation() {
		// Player not 3rd class (classId 10)
		PlayerCharacter lowClass = createPlayer(1, "LowClass", 80, 20, 10);
		Map<Integer, Long> inv = new HashMap<>();
		inv.put(57, 100_000_000L);
		assertFalse(characterResetService.canReset(lowClass, inv));

		// Player level 79
		PlayerCharacter lowLevel = createPlayer(2, "LowLevel", 79, 20, 88);
		assertFalse(characterResetService.canReset(lowLevel, inv));

		// Player low PvP (5 PvP, requires 10)
		PlayerCharacter lowPvp = createPlayer(3, "LowPvP", 80, 5, 88);
		assertFalse(characterResetService.canReset(lowPvp, inv));

		// Insufficient Adena (10M, requires 50M)
		PlayerCharacter validChar = createPlayer(4, "HeroReset", 80, 15, 88);
		Map<Integer, Long> poorInv = new HashMap<>();
		poorInv.put(57, 10_000_000L);
		assertFalse(characterResetService.canReset(validChar, poorInv));

		// Fully qualified
		assertTrue(characterResetService.canReset(validChar, inv));
	}

	@Test
	void testPerformReset() {
		PlayerCharacter player = createPlayer(10, "DuelistMaster", 80, 25, 88);
		Map<Integer, Long> inv = new HashMap<>();
		inv.put(57, 60_000_000L);

		assertEquals(0, characterResetService.getTotalResets(10));

		boolean success = characterResetService.performReset(player, inv);
		assertTrue(success);

		// Level dropped to 1, Exp and SP to 0
		assertEquals(1, player.level());
		assertEquals(0L, player.exp());
		assertEquals(0, player.sp());

		// Adena deducted: 60M - 50M = 10M
		assertEquals(10_000_000L, inv.get(57));

		// Counts incremented
		assertEquals(1, characterResetService.getTotalResets(10));
		assertEquals(1, characterResetService.getDailyResets(10));
		assertEquals(1, characterResetService.getMonthlyResets(10));

		List<ResetRankingEntry> ranking = characterResetService.getTopRankings(10);
		assertFalse(ranking.isEmpty());
		assertEquals(10, ranking.get(0).playerId());
		assertEquals(1, ranking.get(0).totalCount());
	}

	@Test
	void testGenerateHtml() {
		PlayerCharacter player = createPlayer(10, "DuelistMaster", 80, 25, 88);
		Map<Integer, Long> inv = new HashMap<>();
		inv.put(57, 60_000_000L);

		String html = characterResetService.generateHtml(player, inv);
		assertNotNull(html);
		assertTrue(html.contains("Character Reset / Rebirth"));
		assertTrue(html.contains("PERFORM RESET"));
	}
}
