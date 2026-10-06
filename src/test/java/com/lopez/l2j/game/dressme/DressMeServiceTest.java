package com.lopez.l2j.game.dressme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DressMeServiceTest {

	private DressMeData data;
	private DressMeService service;

	@BeforeEach
	void setUp() {
		data = new DressMeData("data/xml/DressMeData.xml");
		service = new DressMeService(data);
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	@Test
	void testDataLoading() {
		assertEquals(3, data.size());

		var draconic = data.get(9100).orElse(null);
		assertNotNull(draconic);
		assertEquals("Draconic Armor", draconic.name());
		assertTrue(draconic.isArmor());
		assertNotNull(draconic.armor());
		assertEquals(6379, draconic.armor().chest());
		assertEquals(6841, draconic.armor().helmet());

		var bow = data.get(9103).orElse(null);
		assertNotNull(bow);
		assertEquals("Valakas Style Weapon", bow.name());
		assertTrue(bow.isWeapon());
		assertNotNull(bow.weapon());
		assertEquals(7575, bow.weapon().rhand());
	}

	@Test
	void testApplyAndRemoveSkins() {
		var player = createPlayer(101, "Fashionista");
		assertFalse(player.isDressMe());

		// Apply armor skin
		boolean appliedArmor = service.applySkin(player, 9100);
		assertTrue(appliedArmor);
		assertTrue(player.isDressMe());
		assertNotNull(player.dressMeArmor());
		assertEquals(9100, player.dressMeArmor().skillId());

		// Apply weapon skin
		boolean appliedWep = service.applySkin(player, 9103);
		assertTrue(appliedWep);
		assertNotNull(player.dressMeWeapon());
		assertEquals(9103, player.dressMeWeapon().skillId());

		// Toggle off / on
		boolean toggledOff = service.toggle(player);
		assertFalse(toggledOff);
		assertFalse(player.isDressMe());

		boolean toggledOn = service.toggle(player);
		assertTrue(toggledOn);
		assertTrue(player.isDressMe());

		// Remove armor
		service.removeArmorSkin(player);
		assertTrue(player.isDressMe()); // still has weapon skin

		// Remove weapon
		service.removeWeaponSkin(player);
		assertFalse(player.isDressMe());
	}
}
