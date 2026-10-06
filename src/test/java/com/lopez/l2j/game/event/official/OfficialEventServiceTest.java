package com.lopez.l2j.game.event.official;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OfficialEventServiceTest {

	private OfficialEventService officialEventService;

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 76, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 1000, 500, 300, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
	}

	@BeforeEach
	void setUp() {
		officialEventService = new OfficialEventService();
	}

	@Test
	void testDefaultEvents() {
		assertTrue(officialEventService.isEventActive(OfficialEventType.BIG_SQUASH));
		assertTrue(officialEventService.isEventActive(OfficialEventType.EVENT_MEDALS));
		assertFalse(officialEventService.isEventActive(OfficialEventType.CHRISTMAS));

		// Toggle christmas event
		officialEventService.setEventState(OfficialEventType.CHRISTMAS, OfficialEventState.ACTIVE);
		assertTrue(officialEventService.isEventActive(OfficialEventType.CHRISTMAS));
	}

	@Test
	void testMonsterDropCalculation() {
		// Run multiple drop iterations to confirm event drops occur on valid level monsters
		int dropCount = 0;
		for (int i = 0; i < 100; i++) {
			List<OfficialEventService.RolledDrop> drops = officialEventService.calculateMonsterDrops(40);
			dropCount += drops.size();
		}
		assertTrue(dropCount > 0, "Active events should generate drops on level 40 monsters");

		// Below min monster level (e.g. level 5) should yield 0 drops
		List<OfficialEventService.RolledDrop> lowLvlDrops = officialEventService.calculateMonsterDrops(5);
		assertEquals(0, lowLvlDrops.size());
	}

	@Test
	void testExchangeReward() {
		PlayerCharacter player = createPlayer(1, "EventLover");
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(6391, 15); // 15 Nectar

		// Exchange 101: 10 Nectar -> Large Squash Seed
		boolean success = officialEventService.exchangeReward(player, 101, inventory);
		assertTrue(success);
		assertEquals(5, inventory.get(6391)); // 5 Nectar remaining

		// Trying to exchange again with only 5 Nectar should fail
		boolean fail = officialEventService.exchangeReward(player, 101, inventory);
		assertFalse(fail);
		assertEquals(5, inventory.get(6391));
	}

	@Test
	void testGenerateHtml() {
		PlayerCharacter player = createPlayer(1, "EventLover");
		String html = officialEventService.generateHtml(player);
		assertNotNull(html);
		assertTrue(html.contains("Official Events Manager"));
		assertTrue(html.contains("Big Squash"));
	}
}
