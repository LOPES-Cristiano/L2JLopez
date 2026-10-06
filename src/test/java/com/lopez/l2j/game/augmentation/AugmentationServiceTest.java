package com.lopez.l2j.game.augmentation;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AugmentationServiceTest {

	static class InMemoryAugmentationRepository implements AugmentationRepository {
		private final Map<Integer, Augmentation> store = new HashMap<>();

		@Override
		public void save(int itemId, Augmentation aug) {
			if (aug == null) store.remove(itemId);
			else store.put(itemId, aug);
		}

		@Override
		public void delete(int itemId) {
			store.remove(itemId);
		}

		@Override
		public Optional<Augmentation> findByItemId(int itemId) {
			return Optional.ofNullable(store.get(itemId));
		}

		@Override
		public Map<Integer, Augmentation> findByOwnerId(int ownerId) {
			return new HashMap<>(store);
		}
	}

	private InMemoryAugmentationRepository repository;
	private AugmentationService service;

	@BeforeEach
	void setUp() {
		repository = new InMemoryAugmentationRepository();
		service = new AugmentationService(repository);
	}

	private ItemTemplate createWeaponTemplate(int id, String name, String crystalType) {
		return ItemTemplate.weapon(
				id, id, name, "rhand", "sword",
				1000, crystalType, 100, 100, 379, 10, 0, 0,
				true, true, true, true
		);
	}

	@Test
	@DisplayName("Validacao de niveis e graus de Life Stones")
	void testLifeStoneGradesAndLevels() {
		// 8723: Level 46 No-Grade (Lv 1, Grade 0)
		assertTrue(AugmentationService.isLifeStone(8723));
		assertEquals(0, AugmentationService.getLifeStoneGrade(8723));
		assertEquals(1, AugmentationService.getLifeStoneLevel(8723));
		assertEquals(46, AugmentationService.getMinPlayerLevel(1));

		// 8732: Level 76 No-Grade (Lv 10, Grade 0)
		assertEquals(0, AugmentationService.getLifeStoneGrade(8732));
		assertEquals(10, AugmentationService.getLifeStoneLevel(8732));
		assertEquals(76, AugmentationService.getMinPlayerLevel(10));

		// 8742: Level 76 Mid-Grade (Lv 10, Grade 1)
		assertEquals(1, AugmentationService.getLifeStoneGrade(8742));
		assertEquals(10, AugmentationService.getLifeStoneLevel(8742));

		// 8752: Level 76 High-Grade (Lv 10, Grade 2)
		assertEquals(2, AugmentationService.getLifeStoneGrade(8752));
		assertEquals(10, AugmentationService.getLifeStoneLevel(8752));

		// 8762: Level 76 Top-Grade (Lv 10, Grade 3)
		assertEquals(3, AugmentationService.getLifeStoneGrade(8762));
		assertEquals(10, AugmentationService.getLifeStoneLevel(8762));
	}

	@Test
	@DisplayName("Requisitos de Gemstones e precos de remocao de augmentacao por Grade")
	void testGemstoneRequirementsAndCancelPrices() {
		var cWeapon = new ItemInstance(1, createWeaponTemplate(100, "C-Weapon", "C"), 1000, 1);
		var bWeapon = new ItemInstance(2, createWeaponTemplate(101, "B-Weapon", "B"), 1000, 1);
		var aWeapon = new ItemInstance(3, createWeaponTemplate(102, "A-Weapon", "A"), 1000, 1);
		var sWeapon = new ItemInstance(4, createWeaponTemplate(103, "S-Weapon", "S"), 1000, 1);

		assertEquals(AugmentationService.GEMSTONE_D, AugmentationService.getGemstoneItemId(cWeapon));
		assertEquals(20, AugmentationService.getGemstoneCount(cWeapon));
		assertEquals(210000, AugmentationService.getCancelPrice(cWeapon));

		assertEquals(AugmentationService.GEMSTONE_D, AugmentationService.getGemstoneItemId(bWeapon));
		assertEquals(30, AugmentationService.getGemstoneCount(bWeapon));
		assertEquals(270000, AugmentationService.getCancelPrice(bWeapon));

		assertEquals(AugmentationService.GEMSTONE_C, AugmentationService.getGemstoneItemId(aWeapon));
		assertEquals(20, AugmentationService.getGemstoneCount(aWeapon));
		assertEquals(420000, AugmentationService.getCancelPrice(aWeapon));

		assertEquals(AugmentationService.GEMSTONE_C, AugmentationService.getGemstoneItemId(sWeapon));
		assertEquals(25, AugmentationService.getGemstoneCount(sWeapon));
		assertEquals(480000, AugmentationService.getCancelPrice(sWeapon));
	}

	@Test
	@DisplayName("Validacao de elegibilidade de itens para augmentacao")
	void testIsAugmentable() {
		var sWeapon = new ItemInstance(10, createWeaponTemplate(103, "S-Weapon", "S"), 1000, 1);
		assertTrue(service.isAugmentable(sWeapon));

		var ngWeapon = new ItemInstance(11, createWeaponTemplate(104, "NG-Weapon", "none"), 1000, 1);
		assertFalse(service.isAugmentable(ngWeapon));

		sWeapon.augmentation(new Augmentation(123456, 0, 0));
		assertFalse(service.isAugmentable(sWeapon)); // ja augmentada
	}

	@Test
	@DisplayName("Geracao e aplicacao de augmentacao e decodificacao de atributos")
	void testGenerateAndApplyAugmentation() {
		var sWeapon = new ItemInstance(20, createWeaponTemplate(103, "Draconic Bow", "S"), 1000, 1);
		assertFalse(sWeapon.isAugmented());

		Augmentation aug = service.applyAugmentation(sWeapon, 8762); // Top-Grade Lv 76
		assertNotNull(aug);
		assertTrue(sWeapon.isAugmented());
		assertEquals(aug, sWeapon.augmentation());
		assertTrue(repository.findByItemId(sWeapon.objectId()).isPresent());

		// Teste de decodificacao de stats
		var stats = service.getAugStatsById(aug.attributes());
		assertNotNull(stats);
		assertFalse(stats.isEmpty());

		// Remocao de augmentacao
		service.removeAugmentation(sWeapon);
		assertFalse(sWeapon.isAugmented());
		assertFalse(repository.findByItemId(sWeapon.objectId()).isPresent());
	}

	@Test
	@DisplayName("Decodificacao de bonus de atributos base (STR, CON, INT, MEN)")
	void testBaseStatDecoding() {
		int strAugId = AugmentationService.BASESTAT_STR << 16;
		var stats = service.getAugStatsById(strAugId);
		assertFalse(stats.isEmpty());
		assertEquals("STR", stats.get(0).stat());
		assertEquals(1.0, stats.get(0).value());

		int conAugId = AugmentationService.BASESTAT_CON << 16;
		stats = service.getAugStatsById(conAugId);
		assertFalse(stats.isEmpty());
		assertEquals("CON", stats.get(0).stat());
	}
}
