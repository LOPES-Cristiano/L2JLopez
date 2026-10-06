package com.lopez.l2j.game.castle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CastleManagerTest {

	private CastleManager castleManager;

	@BeforeEach
	void setUp() {
		castleManager = new CastleManager(null);
	}

	@Test
	void initializesAllNineRetailCastles() {
		assertEquals(9, castleManager.size(), "Deve conter os 9 castelos oficiais de Interlude");

		assertTrue(castleManager.getCastleById(1).isPresent());
		assertEquals("Gludio", castleManager.getCastleById(1).get().name());

		assertTrue(castleManager.getCastleById(3).isPresent());
		assertEquals("Giran", castleManager.getCastleById(3).get().name());

		assertTrue(castleManager.getCastleById(5).isPresent());
		assertEquals("Aden", castleManager.getCastleById(5).get().name());

		assertTrue(castleManager.getCastleById(9).isPresent());
		assertEquals("Schuttgart", castleManager.getCastleById(9).get().name());
	}

	@Test
	void looksUpCastleByNameCaseInsensitive() {
		assertTrue(castleManager.getCastleByName("giran").isPresent());
		assertTrue(castleManager.getCastleByName("GIRAN").isPresent());
		assertTrue(castleManager.getCastleByName("Giran").isPresent());
		assertFalse(castleManager.getCastleByName("Atlantis").isPresent());
	}

	@Test
	void mapsTownsToResponsibleCastles() {
		// Town 1 (Talking Island) -> Gludio (id 1)
		var c1 = castleManager.getCastleByTownId(1);
		assertTrue(c1.isPresent());
		assertEquals("Gludio", c1.get().name());

		// Town 10 (Giran) -> Giran (id 3)
		var c3 = castleManager.getCastleByTownId(10);
		assertTrue(c3.isPresent());
		assertEquals("Giran", c3.get().name());

		// Town 16 (Heine) -> Innadril (id 6)
		var c6 = castleManager.getCastleByTownId(16);
		assertTrue(c6.isPresent());
		assertEquals("Innadril", c6.get().name());
	}

	@Test
	void setsAndCapsTaxPercent() {
		castleManager.setTaxPercent(3, 10);
		assertEquals(10, castleManager.getCastleById(3).get().taxPercent());

		// Limite maximo oficial de Interlude e 15%
		castleManager.setTaxPercent(3, 25);
		assertEquals(15, castleManager.getCastleById(3).get().taxPercent());

		// Minimo 0%
		castleManager.setTaxPercent(3, -5);
		assertEquals(0, castleManager.getCastleById(3).get().taxPercent());
	}

	@Test
	void updatesTreasuryAndOwner() {
		Castle giran = castleManager.getCastleById(3).orElseThrow();
		assertEquals(0, giran.treasury());
		assertFalse(giran.hasOwner());

		castleManager.addToTreasury(3, 100_000L);
		assertEquals(100_000L, giran.treasury());

		castleManager.setOwner(3, 1001);
		assertTrue(giran.hasOwner());
		assertEquals(1001, giran.ownerClanId());
	}
}
