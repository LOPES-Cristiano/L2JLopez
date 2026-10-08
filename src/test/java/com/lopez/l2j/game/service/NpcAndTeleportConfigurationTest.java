package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NpcAndTeleportConfigurationTest {

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@Test
	void testTeleportConfigLoading() {
		assertFalse(Config.FREE_TELEPORTING);
		assertEquals(1, Config.FREE_TELEPORTING_MIN_LVL);
		assertEquals(99, Config.FREE_TELEPORTING_MAX_LVL);
		assertFalse(Config.NOBLE_PASS_FREE_TP);
		assertEquals(1, Config.NOBLE_PASS_FREE_TP_MIN_LVL);
		assertEquals(99, Config.NOBLE_PASS_FREE_TP_MAX_LVL);
	}

	@Test
	void testClassMasterConfigLoading() {
		assertTrue(Config.CLASS_MASTER);
		assertTrue(Config.ALT_CLASS_MASTER);
		assertFalse(Config.CLASS_MASTER_UPDATE_STRIDER);
		assertFalse(Config.CLASS_MASTER_ENTIRE_TREE);
	}

	@Test
	void testNpcServicesAndMobProperties() {
		assertFalse(Config.ALLOW_RENT_PET);
		assertFalse(Config.ALLOW_WYVERN_UPGRADER);
		assertTrue(Config.ALT_MOB_AGGRO_IN_PEACE_ZONE);
		assertFalse(Config.ALT_ATTACKABLE_NPCS);
		assertTrue(Config.ALLOW_PET_WALKER);
		assertEquals(25, Config.MANAGER_CRYSTAL_COUNT);
		assertFalse(Config.ALLOW_LETHAL_PROTECTION_MOBS);
		assertEquals(List.of(35062), Config.LETHAL_PROTECTED_MOBS);
	}
}
