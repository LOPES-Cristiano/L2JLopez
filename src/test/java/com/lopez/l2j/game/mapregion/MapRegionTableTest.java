package com.lopez.l2j.game.mapregion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MapRegionTableTest {

	private static MapRegionTable table;

	@BeforeAll
	static void setUp() {
		table = new MapRegionTable(Path.of("data/xml/world/mapregion/mapregion.xml"));
	}

	@Test
	void loadsAllRestartPointsAndRegions() {
		assertTrue(table.restartPointsCount() >= 38, "Deveria carregar pelo menos 38 pontos de restart");
		assertTrue(table.regionsCount() >= 80, "Deveria carregar regioes poligonais do mundo");
	}

	@Test
	void resolvesTownRestartPointCorrectly() {
		// Perto de Giran: (83400, 147943, -3404)
		int[] respawn = table.getRestartCoordinates(83400, 147943, -3404, 0);
		assertNotNull(respawn);
		assertEquals(3, respawn.length);
		// Deve estar dentro da regiao de Giran
		assertTrue(Math.abs(respawn[0] - 83400) < 50000);
	}

	@Test
	void resolvesRaceSpecificRestartForElvenVillage() {
		// Perto da vila dos elfos (46000, 51000, -2900)
		int[] elvenRespawn = table.getRestartCoordinates(46000, 51000, -2900, 1); // 1 = Elf
		assertNotNull(elvenRespawn);
		assertEquals(3, elvenRespawn.length);
	}

	@Test
	void resolvesChaoticRestartPoint() {
		int[] chaoticRespawn = table.getRestartCoordinates(83400, 147943, -3404, 0, true);
		assertNotNull(chaoticRespawn);
		assertEquals(3, chaoticRespawn.length);
	}
}
