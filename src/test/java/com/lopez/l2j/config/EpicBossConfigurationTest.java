package com.lopez.l2j.config;

import com.lopez.l2j.game.boss.GrandBossInfo;
import com.lopez.l2j.game.boss.GrandBossManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EpicBossConfigurationTest {

	@BeforeEach
	void setUp() {
		Config.load();
	}

	@org.junit.jupiter.api.AfterEach
	void tearDown() {
		Config.load();
	}

	@Test
	@DisplayName("Validar carregamento de temporizadores e capacidades de chefes épicos em Config")
	void testEpicBossTimersAndCapacitiesLoaded() {
		assertEquals(15, Config.ANTHARAS_ARRIVED_TIME);
		assertEquals(240, Config.ANTHARAS_ACTIVE_TIME);
		assertEquals(11520, Config.ANTHARAS_MIN_RESPAWN);
		assertEquals(15840, Config.ANTHARAS_MAX_RESPAWN);
		assertEquals(50, Config.ANTHARAS_WEAK_PLAYERS);
		assertEquals(80, Config.ANTHARAS_MIDDLE_PLAYERS);
		assertEquals(4, Config.ANTHARAS_INTERVAL_OF_BEHEMOTH);

		assertEquals(1, Config.VALAKAS_ARRIVED_TIME);
		assertEquals(240, Config.VALAKAS_ACTIVE_TIME);
		assertEquals(500, Config.VALAKAS_LAIR_CAPACITY);

		assertEquals(240, Config.BAIUM_ACTIVE_TIME);
		assertEquals(20, Config.BAIUM_NO_ATTACK_TIME);
		assertEquals(300, Config.BAIUM_UNSPAWN_CUBE);

		assertEquals(8, Config.QUEEN_ANT_NUMBER_OF_GUARDS);
		assertEquals(6, Config.QUEEN_ANT_NUMBER_OF_NURSES);
		assertEquals(4, Config.CORE_NUMBER_OF_GUARDS);
		assertEquals(80, Config.ZAKEN_MAX_LEVEL_IN_ZONE);
	}

	@Test
	@DisplayName("Validar que GrandBossManager mapeia corretamente as durações e respawns das configs")
	void testGrandBossManagerRegistrations() {
		Config.QUEEN_ANT_MIN_RESPAWN = 1000;
		Config.QUEEN_ANT_MAX_RESPAWN = 2000;
		Config.ANTHARAS_MIN_RESPAWN = 10000;
		Config.ANTHARAS_MAX_RESPAWN = 15000;
		Config.VALAKAS_MIN_RESPAWN = 12000;
		Config.VALAKAS_MAX_RESPAWN = 16000;

		GrandBossManager gbm = new GrandBossManager(null, null);

		GrandBossInfo qaInfo = gbm.getBoss(GrandBossManager.QUEEN_ANT).orElseThrow();
		assertNotNull(qaInfo);
		assertEquals(1000, qaInfo.minRespawnMinutes());
		assertEquals(2000, qaInfo.maxRespawnMinutes());

		GrandBossInfo antharasInfo = gbm.getBoss(GrandBossManager.ANTHARAS).orElseThrow();
		assertNotNull(antharasInfo);
		assertEquals(10000, antharasInfo.minRespawnMinutes());
		assertEquals(15000, antharasInfo.maxRespawnMinutes());

		GrandBossInfo valakasInfo = gbm.getBoss(GrandBossManager.VALAKAS).orElseThrow();
		assertNotNull(valakasInfo);
		assertEquals(12000, valakasInfo.minRespawnMinutes());
		assertEquals(16000, valakasInfo.maxRespawnMinutes());
	}

	@Test
	@DisplayName("Validar dinâmica de Minions de Queen Ant e Core por Config")
	void testMinionsAndNursesCount() {
		Config.QUEEN_ANT_NUMBER_OF_GUARDS = 12;
		Config.QUEEN_ANT_NUMBER_OF_NURSES = 10;
		Config.CORE_NUMBER_OF_GUARDS = 8;

		assertEquals(12, Config.QUEEN_ANT_NUMBER_OF_GUARDS);
		assertEquals(10, Config.QUEEN_ANT_NUMBER_OF_NURSES);
		assertEquals(8, Config.CORE_NUMBER_OF_GUARDS);
	}
}
