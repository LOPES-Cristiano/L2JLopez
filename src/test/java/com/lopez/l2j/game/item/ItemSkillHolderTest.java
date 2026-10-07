package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemSkillHolderTest {

	@Test
	void parseSingleHolderWithoutChance() {
		ItemSkillHolder h = ItemSkillHolder.parse("3047-1");
		assertNotNull(h);
		assertEquals(3047, h.skillId());
		assertEquals(1, h.level());
		assertEquals(100, h.chance());
	}

	@Test
	void parseSingleHolderWithChance() {
		ItemSkillHolder h = ItemSkillHolder.parse("3020-1-12");
		assertNotNull(h);
		assertEquals(3020, h.skillId());
		assertEquals(1, h.level());
		assertEquals(12, h.chance());
	}

	@Test
	void parseListHandlesMultipleSemicolonsAndSpaces() {
		List<ItemSkillHolder> list = ItemSkillHolder.parseList("3047-1; 3558-1; 0-0; ");
		assertEquals(2, list.size());
		assertEquals(3047, list.get(0).skillId());
		assertEquals(1, list.get(0).level());
		assertEquals(3558, list.get(1).skillId());
		assertEquals(1, list.get(1).level());
	}

	@Test
	void parseInvalidReturnsNullOrEmpty() {
		assertNull(ItemSkillHolder.parse(null));
		assertNull(ItemSkillHolder.parse(""));
		assertNull(ItemSkillHolder.parse("0-0"));
		assertNull(ItemSkillHolder.parse("abc"));

		assertTrue(ItemSkillHolder.parseList(null).isEmpty());
		assertTrue(ItemSkillHolder.parseList("").isEmpty());
		assertTrue(ItemSkillHolder.parseList("0-0;").isEmpty());
	}
}
