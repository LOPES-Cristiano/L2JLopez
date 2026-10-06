package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.junit.jupiter.api.Test;

class GroundItemServiceTest {

	@Test
	void testDropAndPickupItem() {
		var world = new GameWorld();
		var idFactory = ObjectIdFactory.sequential(0x80000000);
		var templates = TestItems.table();
		var service = new GroundItemService(world, idFactory, templates);

		// Drop adena (ID 57) x1000 at (100, 200, -50)
		var groundItem = service.dropItem(1001, 57, 1000, 100, 200, -50);
		assertNotNull(groundItem);
		assertEquals(57, groundItem.itemId());
		assertEquals(1000, groundItem.count());
		assertEquals(1, service.size());

		// Find around
		var around = service.findAround(100, 200, 50);
		assertEquals(1, around.size());

		// Player picks up
		var player = new PlayerCharacter(1002, "Hero", "Title", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		player.moveTo(100, 200, -50);

		var picked = service.pickupItem(player, groundItem.objectId());
		assertTrue(picked.isPresent());
		assertEquals(0, service.size());
	}
}
