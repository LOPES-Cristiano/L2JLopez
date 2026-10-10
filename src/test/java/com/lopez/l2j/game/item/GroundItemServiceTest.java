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

		java.util.List<com.lopez.l2j.network.game.packet.GameServerPacket> receivedPackets = new java.util.ArrayList<>();
		java.util.Set<Integer> knowns = new java.util.HashSet<>();
		world.add(new GameWorld.OnlinePlayer() {
			@Override public int objectId() { return 1002; }
			@Override public String name() { return "Hero"; }
			@Override public int x() { return 100; }
			@Override public int y() { return 200; }
			@Override public int z() { return -50; }
			@Override public void send(com.lopez.l2j.network.game.packet.GameServerPacket packet) { receivedPackets.add(packet); }
			@Override public void addKnownObject(int id) { knowns.add(id); }
			@Override public void removeKnownObject(int id) { knowns.remove(id); }
		});

		// Drop adena (ID 57) x1000 at (100, 200, -50)
		var groundItem = service.dropItem(1001, 57, 1000, 100, 200, -50);
		assertNotNull(groundItem);
		assertEquals(57, groundItem.itemId());
		assertEquals(1000, groundItem.count());
		assertEquals(1, service.size());
		assertTrue(knowns.contains(groundItem.objectId()));

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
		assertFalse(knowns.contains(groundItem.objectId()));

		boolean hasDeleteObject = receivedPackets.stream()
				.anyMatch(p -> p instanceof com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject del
						&& del.objectId() == groundItem.objectId());
		assertTrue(hasDeleteObject, "Deve enviar DeleteObject para remover o item do chão visualmente");
	}
}
