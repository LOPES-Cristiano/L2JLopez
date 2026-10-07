package com.lopez.l2j.game.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CharTemplateTableTest {

	final CharTemplateTable table = new CharTemplateTable();

	@Test
	void loadsAllInterludeClasses() {
		assertTrue(table.size() >= 89, "esperado ao menos 89 classes, veio " + table.size());
	}

	@Test
	void humanFighterMatchesLegacyData() {
		CharTemplate t = table.get(0).orElseThrow();
		assertEquals("Human Fighter", t.className());
		assertEquals(0, t.raceId());
		assertEquals(40, t.str());
		assertEquals(43, t.con());
		assertEquals(-71338, t.spawnX());
		assertEquals(258271, t.spawnY());
		assertEquals(-3104, t.spawnZ());
		assertEquals(80.0, t.hpBase());
		assertEquals(30.0, t.mpBase());
		assertEquals(32.0, t.cpBase());
		assertEquals(9.0, t.collisionRadius(false));
		assertEquals(8.0, t.collisionRadius(true));
		assertTrue(t.isStartingClass());
		assertEquals(0, t.classTier());
	}

	@Test
	void classTiersReflectProgression() {
		assertEquals(0, table.get(0).orElseThrow().classTier());   // Human Fighter (tier 0)
		assertEquals(1, table.get(1).orElseThrow().classTier());   // Warrior (tier 1)
		assertEquals(2, table.get(2).orElseThrow().classTier());   // Gladiator (tier 2)
		assertEquals(3, table.get(88).orElseThrow().classTier());  // Duelist (tier 3)
	}

	@Test
	void creationTemplatesFollowLegacyOrder() {
		var list = table.creationTemplates();
		assertEquals(10, list.size());
		int[] expected = { 0, 0, 10, 18, 25, 31, 38, 44, 49, 53 };
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], list.get(i).classId());
			assertTrue(list.get(i).isStartingClass());
		}
		assertTrue(table.get(53).orElseThrow().canCraft(), "anao cria itens");
	}

	@Test
	void rejectsMissingAttributes() {
		String xml = "<list><class Id=\"0\" name=\"x\" RaceId=\"0\"><stats str=\"1\"/></class></list>";
		assertThrows(IllegalStateException.class,
				() -> CharTemplateTable.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void rejectsDoctype() {
		String xml = "<?xml version=\"1.0\"?><!DOCTYPE list [<!ENTITY x \"y\">]><list/>";
		assertThrows(IllegalStateException.class,
				() -> CharTemplateTable.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
	}
}
