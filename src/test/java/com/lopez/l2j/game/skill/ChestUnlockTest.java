package com.lopez.l2j.game.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ChestUnlockTest {

	private static SkillTable table;

	@BeforeAll
	static void load() {
		table = new SkillTable(Path.of("data", "xml", "stats", "skills"));
	}

	@Test
	void testDeluxeChestKeySkills() {
		for (int level = 1; level <= 8; level++) {
			var sk = table.get(2229, level).orElse(null);
			assertNotNull(sk, "Skill 2229 nivel " + level + " deve existir");
			assertEquals("DELUXE_KEY_UNLOCK", sk.skillType());
			assertEquals(6665 + level - 1, sk.itemConsumeId(), "Deluxe Chest Key grade " + level + " itemId incorreto");
			assertEquals(1, sk.itemConsumeCount());
		}
	}

	@Test
	void testNormalChestKeySkills() {
		int[] expectedItems = { 5204, 5203, 5202, 5201, 5200, 5199, 5198, 5197 };
		for (int level = 1; level <= 8; level++) {
			var sk = table.get(2065, level).orElse(null);
			assertNotNull(sk, "Skill 2065 nivel " + level + " deve existir");
			assertEquals("UNLOCK", sk.skillType());
			assertEquals(expectedItems[level - 1], sk.itemConsumeId(), "Normal Chest Key grade " + level + " itemId incorreto");
			assertEquals(1, sk.itemConsumeCount());
		}
	}

	@Test
	void testUnlockSkill() {
		for (int level = 1; level <= 14; level++) {
			var sk = table.get(27, level).orElse(null);
			assertNotNull(sk, "Skill 27 nivel " + level + " deve existir");
			assertEquals("UNLOCK", sk.skillType());
			assertEquals("TARGET_UNLOCKABLE", sk.target());
			assertEquals(1661, sk.itemConsumeId(), "Unlock deve consumir Thief Key (1661)");
			assertTrue(sk.itemConsumeCount() > 0);
		}
	}

	@Test
	void testRequiredGradeFormula() {
		// Grade 1: Lv 1..29, Grade 2: 30..39, ..., Grade 8: 80+
		assertEquals(1, getRequiredChestKeyGrade(21));
		assertEquals(2, getRequiredChestKeyGrade(35));
		assertEquals(3, getRequiredChestKeyGrade(42));
		assertEquals(4, getRequiredChestKeyGrade(55));
		assertEquals(5, getRequiredChestKeyGrade(65));
		assertEquals(6, getRequiredChestKeyGrade(72));
		assertEquals(7, getRequiredChestKeyGrade(78));
		assertEquals(8, getRequiredChestKeyGrade(84));
	}

	private static int getRequiredChestKeyGrade(int chestLevel) {
		if (chestLevel < 30) return 1;
		if (chestLevel < 40) return 2;
		if (chestLevel < 50) return 3;
		if (chestLevel < 60) return 4;
		if (chestLevel < 70) return 5;
		if (chestLevel < 76) return 6;
		if (chestLevel < 80) return 7;
		return 8;
	}
}
