package com.lopez.l2j.game.teleport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TeleportLocationTableTest {

	@Test
	void loadsTeleportsFromLegacyXmlIfPresent() {
		TeleportLocationTable table = new TeleportLocationTable("data/xml/world/teleports.xml");
		if (table.size() > 0) {
			var loc = table.get(1);
			assertTrue(loc.isPresent(), "Teleport 1 deveria existir");
			assertEquals(-12694, loc.get().locX());
			assertEquals(122776, loc.get().locY());
			assertEquals(-3114, loc.get().locZ());
			assertEquals(10000, loc.get().price());
			assertFalse(loc.get().forNoble());
		}
	}

	@Test
	void gracefullyHandlesMissingFile() {
		TeleportLocationTable table = new TeleportLocationTable("caminho_inexistente.xml");
		assertEquals(0, table.size());
		assertTrue(table.get(999).isEmpty());
	}
}
