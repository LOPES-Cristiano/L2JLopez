package com.lopez.l2j.features.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementXmlParserTest {

	private static InputStream xml(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void parsesTheLegacyL2JDreamFile() throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(AchievementService.CATALOG_RESOURCE)) {
			Map<Integer, Achievement> all = AchievementXmlParser.parse(in);
			assertEquals(25, all.size());
			assertEquals(Map.of(9142, 5L), all.get(1).rewards());
			// "Armor" exige 5 pecas; "Champion" so o nivel
			assertEquals(5, all.get(23).conditions().size());
			assertEquals(1, all.get(1).conditions().size());
		}
	}

	@Test
	void rejectsDuplicateIds() {
		String x = "<list><achievement id='1' name='a' reward='1,1' minLevel='1'/>"
				+ "<achievement id='1' name='b' reward='1,1' minLevel='1'/></list>";
		assertThrows(IllegalStateException.class, () -> AchievementXmlParser.parse(xml(x)));
	}

	@Test
	void rejectsUnknownAttributeInsteadOfSilentlyIgnoringIt() {
		String x = "<list><achievement id='1' name='a' reward='1,1' minLevell='1'/></list>";
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> AchievementXmlParser.parse(xml(x)));
		assertTrue(e.getMessage().contains("minLevell"));
	}

	@Test
	void rejectsMalformedReward() {
		String x = "<list><achievement id='1' name='a' reward='abc' minLevel='1'/></list>";
		assertThrows(IllegalStateException.class, () -> AchievementXmlParser.parse(xml(x)));
	}

	@Test
	void rejectsDoctypeToBlockXxe() {
		String x = "<?xml version='1.0'?><!DOCTYPE list [<!ENTITY e SYSTEM 'file:///etc/passwd'>]>"
				+ "<list><achievement id='1' name='&e;' reward='1,1' minLevel='1'/></list>";
		assertThrows(IllegalStateException.class, () -> AchievementXmlParser.parse(xml(x)));
	}

	@Test
	void falseFlagCreatesNoCondition() {
		String x = "<list><achievement id='1' name='a' reward='1,1' mustBeHero='false'/></list>";
		Achievement a = AchievementXmlParser.parse(xml(x)).get(1);
		assertTrue(a.conditions().isEmpty());
		assertFalse(a.repeatable());
	}
}
