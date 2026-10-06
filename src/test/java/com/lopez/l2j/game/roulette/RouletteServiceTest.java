package com.lopez.l2j.game.roulette;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouletteServiceTest {

	private RouletteService rouletteService;

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 76, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 1000, 500, 300, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
	}

	@BeforeEach
	void setUp() {
		rouletteService = new RouletteService();
	}

	@Test
	void testPrizePoolInitialization() {
		assertFalse(rouletteService.getPrizePool().isEmpty());
		assertTrue(rouletteService.getPrizePool().stream().anyMatch(i -> i.rarity() == RouletteRarity.JACKPOT));
		assertTrue(rouletteService.getPrizePool().stream().anyMatch(i -> i.rarity() == RouletteRarity.COMMON));
	}

	@Test
	void testSpinInsufficientFunds() {
		PlayerCharacter player = createPlayer(1, "GamblerOne");
		Map<Integer, Long> inventory = new HashMap<>();
		inventory.put(57, 1_000_000L); // 1M Adena, but spin costs 5M

		RouletteSpinResult result = rouletteService.spin(player, 57, 5_000_000L, inventory);
		assertFalse(result.success());
		assertTrue(result.message().contains("not have enough"));
		assertEquals(1_000_000L, inventory.get(57)); // Unchanged
	}

	@Test
	void testSpinSuccessful() {
		PlayerCharacter player = createPlayer(2, "LuckyPlayer");
		Map<Integer, Long> inventory = new HashMap<>();
		inventory.put(57, 10_000_000L); // 10M Adena

		RouletteSpinResult result = rouletteService.spin(player, 57, 5_000_000L, inventory);
		assertTrue(result.success());
		assertNotNull(result.item());
		assertEquals(5_000_000L, inventory.get(57)); // 5M deducted

		// Cooldown check: second immediate spin fails due to cooldown
		RouletteSpinResult cooldownResult = rouletteService.spin(player, 57, 5_000_000L, inventory);
		assertFalse(cooldownResult.success());
		assertTrue(cooldownResult.message().contains("wait"));
	}

	@Test
	void testGenerateHtml() {
		PlayerCharacter player = createPlayer(3, "Viewer");
		String mainHtml = rouletteService.generateMainHtml(player);
		assertNotNull(mainHtml);
		assertTrue(mainHtml.contains("Lucky Wheel of Fortune"));

		RouletteItem item = rouletteService.getPrizePool().get(0);
		RouletteSpinResult res = RouletteSpinResult.success(item);
		String resHtml = rouletteService.generateResultHtml(res);
		assertNotNull(resHtml);
		assertTrue(resHtml.contains("YOU WON!"));
	}
}
