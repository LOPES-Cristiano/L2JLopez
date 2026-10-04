package com.lopez.l2j.game.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.template.CharTemplateTable;
import org.junit.jupiter.api.Test;

class ExperienceTableTest {

	@Test
	void testLevelProgression() {
		assertEquals(1, ExperienceTable.calculateLevel(0));
		assertEquals(1, ExperienceTable.calculateLevel(67));
		assertEquals(2, ExperienceTable.calculateLevel(68));
		assertEquals(2, ExperienceTable.calculateLevel(362));
		assertEquals(3, ExperienceTable.calculateLevel(363));
		assertEquals(80, ExperienceTable.calculateLevel(4_200_000_000L));
		assertEquals(80, ExperienceTable.calculateLevel(5_000_000_000L));
	}

	@Test
	void testMaxStatsScalingOnLevelUp() {
		var table = new CharTemplateTable();
		var humanFighter = table.get(0).orElseThrow();

		// Level 1: hpBase = 80, cpBase = 32, mpBase = 30
		assertEquals(80, humanFighter.calculateMaxHp(1));
		assertEquals(32, humanFighter.calculateMaxCp(1));
		assertEquals(30, humanFighter.calculateMaxMp(1));

		// Level 2 deve ter vida, cp e mp maiores que no level 1
		assertTrue(humanFighter.calculateMaxHp(2) > humanFighter.calculateMaxHp(1));
		assertTrue(humanFighter.calculateMaxCp(2) > humanFighter.calculateMaxCp(1));
		assertTrue(humanFighter.calculateMaxMp(2) > humanFighter.calculateMaxMp(1));

		// Valores calculados com a formula do char_template.xml para Level 2
		assertEquals(92, humanFighter.calculateMaxHp(2));
		assertEquals(37, humanFighter.calculateMaxCp(2));
		assertEquals(36, humanFighter.calculateMaxMp(2));
	}
}
