package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArmorSetsTableTest {

	private static ArmorSetsTable table;

	@BeforeAll
	static void setUp() {
		table = new ArmorSetsTable(Path.of("data/xml/player/armorsets.xml"));
	}

	@Test
	void loadsAllArmorSets() {
		assertTrue(table.size() >= 30, "Deveria carregar pelo menos 30 sets de armadura");
		Optional<ArmorSetsTable.ArmorSet> wooden = table.byChest(23);
		assertTrue(wooden.isPresent());
		assertEquals(2386, wooden.get().legs());
		assertEquals(43, wooden.get().head());
		assertEquals(3500, wooden.get().skillId());
	}

	@Test
	void detectsShieldAndEnchant6Properties() {
		Optional<ArmorSetsTable.ArmorSet> briga = table.byChest(352);
		assertTrue(briga.isPresent());
		assertEquals(2493, briga.get().shield());
		assertEquals(3544, briga.get().shieldSkillId());
		assertEquals(3611, briga.get().enchant6Skill());
	}
}
