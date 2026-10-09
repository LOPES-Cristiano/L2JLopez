package com.lopez.l2j.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PvPEventDetailedConfigurationTest {

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@Test
	@DisplayName("Validar carregamento detalhado de regras TvT de tvtevent.properties")
	void testTvtDetailedConfigs() {
		assertTrue(Config.TVT_ENABLED);
		assertTrue(Config.TVT_AURA);
		assertFalse(Config.TVT_JOIN_WITH_CURSED_WEAPON);
		assertTrue(Config.TVT_ON_START_REMOVE_ALL_EFFECTS);
		assertTrue(Config.TVT_ON_START_UNSUMMON_PET);
		assertTrue(Config.TVT_CLOSE_COLISEUM_DOORS);
		assertFalse(Config.TVT_PRICE_NO_KILLS);
		assertEquals(5, Config.TVT_JOIN_TIME);
		assertEquals(15, Config.TVT_EVENT_TIME);
		assertEquals(10, Config.TVT_REVIVE_DELAY);
		assertTrue(Config.TVT_REVIVE_RECOVERY);
		assertFalse(Config.TVT_ORIGINAL_POSITION);
		assertFalse(Config.TVT_ALLOW_ENEMY_HEALING);
		assertFalse(Config.TVT_ALLOW_TEAM_CASTING);
		assertFalse(Config.TVT_ALLOW_TEAM_ATTACKING);
		assertNotNull(Config.TVT_BLUE_TEAM_LOC);
		assertNotNull(Config.TVT_RED_TEAM_LOC);
	}

	@Test
	@DisplayName("Validar carregamento detalhado de regras CTF de ctfevent.properties")
	void testCtfDetailedConfigs() {
		assertTrue(Config.CTF_ENABLED);
		assertTrue(Config.CTF_AURA);
		assertTrue(Config.CTF_ON_START_REMOVE_ALL_EFFECTS);
		assertTrue(Config.CTF_ON_START_UNSUMMON_PET);
		assertEquals(10, Config.CTF_REVIVE_DELAY);
		assertNotNull(Config.CTF_BLUE_TEAM_LOC);
		assertNotNull(Config.CTF_RED_TEAM_LOC);
		assertNotNull(Config.CTF_BLUE_FLAG_LOC);
		assertNotNull(Config.CTF_RED_FLAG_LOC);
	}

	@Test
	@DisplayName("Validar carregamento detalhado de regras DM de dmevent.properties")
	void testDmDetailedConfigs() {
		assertTrue(Config.DM_ENABLED);
		assertTrue(Config.DM_AURA);
		assertTrue(Config.DM_ON_START_REMOVE_ALL_EFFECTS);
		assertTrue(Config.DM_ON_START_UNSUMMON_PET);
		assertEquals(10, Config.DM_REVIVE_DELAY);
	}
}
