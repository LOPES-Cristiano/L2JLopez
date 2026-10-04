package com.lopez.l2j.game.trade;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BuyListTableTest {

	@Test
	void loadsBuyListsFromLegacyXmlIfPresent() {
		BuyListTable table = new BuyListTable("data/xml/world/buylists.xml");
		if (table.size() > 0) {
			var list = table.get(1);
			assertTrue(list.isPresent(), "BuyList 1 deveria existir");
			assertEquals(30001, list.get().npcId());
			assertFalse(list.get().products().isEmpty());
			var prod = list.get().getProduct(1);
			assertTrue(prod.isPresent());
			assertEquals(883, prod.get().price());
		}
	}

	@Test
	void gracefullyHandlesMissingFile() {
		BuyListTable table = new BuyListTable("caminho_inexistente.xml");
		assertEquals(0, table.size());
		assertTrue(table.get(999).isEmpty());
	}
}
