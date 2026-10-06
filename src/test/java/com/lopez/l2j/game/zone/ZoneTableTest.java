package com.lopez.l2j.game.zone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZoneTableTest {

	private ZoneTable zoneTable;

	@BeforeEach
	void setUp() {
		zoneTable = new ZoneTable("data/xml/zone");
	}

	@Test
	void loadsZonesFromXmlDirectory() {
		assertTrue(zoneTable.size() > 50, "Deve carregar mais de 50 zonas de data/xml/zone");
		assertFalse(zoneTable.zonesByType(ZoneType.PEACE).isEmpty(), "Deve conter zonas de paz");
		assertFalse(zoneTable.zonesByType(ZoneType.ARENA).isEmpty(), "Deve conter zonas de arena");
		assertFalse(zoneTable.zonesByType(ZoneType.WATER).isEmpty(), "Deve conter zonas de agua");
	}

	@Test
	void detectsTalkingIslandPeaceZone() {
		// Ponto dentro de Talking Island Village (zone id 1: x=-84000, y=243000, z=-3700)
		assertTrue(zoneTable.isInsidePeace(-84000, 243000, -3700), "Centro de Talking Island deve ser Peace Zone");

		// Ponto fora em alto mar / campo aberto nao deve ser Peace Zone
		assertFalse(zoneTable.isInsidePeace(0, 0, 0), "Coordenadas 0,0,0 nao devem ser zona de paz");
	}

	@Test
	void detectsArenaZone() {
		// Gludin Arena (zone id 1: x entre -88411 e -87429, y entre 141732 e 142708, z=-3500)
		assertTrue(zoneTable.isInsideArena(-88000, 142000, -3500), "Centro da Arena de Gludin deve ser Arena Zone");
		assertFalse(zoneTable.isInsideArena(0, 0, 0), "Coordenadas 0,0,0 nao devem ser arena");
	}

	@Test
	void queriesZonesAtPosition() {
		var zones = zoneTable.getZonesAt(-84000, 243000, -3700);
		assertNotNull(zones);
		assertFalse(zones.isEmpty(), "Deve encontrar pelo menos uma zona em Talking Island");
	}
}
