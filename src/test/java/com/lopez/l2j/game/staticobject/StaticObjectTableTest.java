package com.lopez.l2j.game.staticobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.ObjectIdFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaticObjectTableTest {

	@Test
	void testLoadStaticObjectsFromXml() {
		var idFactory = ObjectIdFactory.sequential(0x70000000);
		var table = new StaticObjectTable(idFactory, "data/xml/world/staticobjects.xml");
		table.load(Path.of("data/xml/world/staticobjects.xml"));

		assertTrue(table.size() > 20, "Must load static objects from staticobjects.xml");

		// Test finding dark elf town map
		var darkElfMap = table.byStaticId(20180001);
		assertTrue(darkElfMap.isPresent());
		assertEquals(StaticObjectInstance.TYPE_TOWN_MAP, darkElfMap.get().type());
		assertEquals("town_map_darkelf_t00", darkElfMap.get().texture());
		assertTrue(darkElfMap.get().isTownMap());

		// Test finding objects around coords
		var around = table.findAround(15258, 15631, 500);
		assertFalse(around.isEmpty());
		assertTrue(around.stream().anyMatch(o -> o.staticObjectId() == 20180001));
	}
}
