package com.lopez.l2j.game.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DoorTableTest {

	private static DoorTable table;

	@BeforeAll
	static void setUp() {
		table = new DoorTable(Path.of("data/xml/world/door.xml"), null);
	}

	@Test
	void loadsAllWorldDoors() {
		assertTrue(table.size() >= 100, "Deveria carregar centenas de portas do mundo");
		DoorInstance door = table.getDoor(17220001);
		assertNotNull(door);
		assertEquals("gludin_clanhall_001", door.name());
		assertEquals(-84495, door.x());
		assertEquals(155209, door.y());
		assertEquals(158250, door.maxHp());
		assertFalse(door.isOpen());
	}

	@Test
	void opensAndClosesDoor() {
		DoorInstance door = table.getDoor(17220001);
		assertNotNull(door);
		assertFalse(door.isOpen());

		assertTrue(table.openDoor(17220001));
		assertTrue(door.isOpen());

		assertTrue(table.closeDoor(17220001));
		assertFalse(door.isOpen());
	}

	@Test
	void findsDoorsAroundCoordinates() {
		// Busca portas ao redor de Gludin Clanhall
		List<DoorInstance> doors = table.findDoorsAround(-84495, 155209, 2000);
		assertNotNull(doors);
		assertFalse(doors.isEmpty());
		assertTrue(doors.stream().anyMatch(d -> d.doorId() == 17220001));
	}
}
