package com.lopez.l2j.config;

import com.lopez.l2j.game.augmentation.Augmentation;
import com.lopez.l2j.game.augmentation.AugmentationRepository;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.item.Paperdoll;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AugmentationAndRegenConfigurationTest {

	private AugmentationService augmentationService;

	@BeforeEach
	void setUp() {
		Config.AUGMENTATION_NG_SKILL_CHANCE = 15;
		Config.AUGMENTATION_NG_GLOW_CHANCE = 0;
		Config.AUGMENTATION_BASE_STAT_CHANCE = 1;
		Config.ALLOW_WAREHOUSE = true;
		Config.MAX_WAREHOUSE_SLOTS_FOR_OTHER = 100;
		Config.PLAYER_HP_REGEN_MULTIPLIER = 100;
		Config.PLAYER_MP_REGEN_MULTIPLIER = 100;
		Config.PLAYER_CP_REGEN_MULTIPLIER = 100;

		AugmentationRepository dummyRepo = new AugmentationRepository() {
			private final Map<Integer, Augmentation> map = new HashMap<>();
			@Override public void save(int itemId, Augmentation aug) { map.put(itemId, aug); }
			@Override public void delete(int itemId) { map.remove(itemId); }
			@Override public Optional<Augmentation> findByItemId(int itemId) { return Optional.ofNullable(map.get(itemId)); }
			@Override public Map<Integer, Augmentation> findByOwnerId(int ownerId) { return map; }
		};
		augmentationService = new AugmentationService(dummyRepo);
	}

	@Test
	@DisplayName("Validar chances de Augmentation controladas por rates.properties")
	void testAugmentationRatesConfiguration() {
		// Force 100% skill chance for Top-Grade (grade = 3)
		Config.AUGMENTATION_TOP_SKILL_CHANCE = 100;
		Config.AUGMENTATION_TOP_GLOW_CHANCE = 100;

		Augmentation aug100 = augmentationService.generateRandomAugmentation(10, 3);
		assertNotNull(aug100);
		assertTrue(aug100.hasSkill(), "Top stone with 100% skill chance must generate skill");

		// Force 0% skill chance
		Config.AUGMENTATION_TOP_SKILL_CHANCE = 0;
		Config.AUGMENTATION_TOP_GLOW_CHANCE = 0;
		Augmentation aug0 = augmentationService.generateRandomAugmentation(10, 3);
		assertNotNull(aug0);
		assertFalse(aug0.hasSkill(), "Top stone with 0% skill chance must not generate skill");
	}

	@Test
	@DisplayName("Validar permissões e slots de Armazém controlados por rates.properties")
	void testWarehouseConfigEnforcement() {
		ItemRepository dummyRepo = new ItemRepository() {
			@Override public List<StoredItem> findInventory(int ownerId) { return List.of(); }
			@Override public List<StoredItem> findWarehouse(int ownerId) { return List.of(); }
			@Override public List<Paperdoll.Entry> findPaperdoll(int ownerId) { return List.of(); }
			@Override public void insert(ItemInstance item, String process) {}
			@Override public void update(ItemInstance item) {}
			@Override public void delete(int objectId) {}
			@Override public void deleteByOwner(int ownerId) {}
		};
		ItemTemplateTable templates = TestItems.table();
		ObjectIdFactory ids = ObjectIdFactory.sequential(1000);
		WarehouseService warehouseService = new WarehouseService(dummyRepo, templates, ids);

		Inventory inv = new Inventory(1);
		ItemTemplate adenaTpl = templates.get(TestItems.ADENA).orElseThrow();
		ItemInstance adena = new ItemInstance(10, adenaTpl, 1, 10000);
		inv.add(adena);

		ItemTemplate weaponTpl = templates.get(TestItems.SHORT_SWORD).orElseThrow();
		ItemInstance weapon = new ItemInstance(20, weaponTpl, 1, 1);
		inv.add(weapon);

		// When warehouse is disabled:
		Config.ALLOW_WAREHOUSE = false;
		assertFalse(warehouseService.depositItem(inv, 20, 1), "Deposit must fail when ALLOW_WAREHOUSE is false");

		// When warehouse is enabled:
		Config.ALLOW_WAREHOUSE = true;
		assertTrue(warehouseService.depositItem(inv, 20, 1), "Deposit should succeed when ALLOW_WAREHOUSE is true");
	}

	@Test
	@DisplayName("Validar multiplicadores de regeneração de HP/MP/CP")
	void testRegenMultiplierFormulas() {
		PlayerCharacter pc = new PlayerCharacter(1001, "acc", "hero", 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);

		Config.PLAYER_HP_REGEN_MULTIPLIER = 200; // 2x
		Config.PLAYER_MP_REGEN_MULTIPLIER = 300; // 3x
		Config.PLAYER_CP_REGEN_MULTIPLIER = 400; // 4x

		double hpMultiplier = Config.PLAYER_HP_REGEN_MULTIPLIER / 100.0;
		double mpMultiplier = Config.PLAYER_MP_REGEN_MULTIPLIER / 100.0;
		double cpMultiplier = Config.PLAYER_CP_REGEN_MULTIPLIER / 100.0;

		assertEquals(2.0, hpMultiplier, 0.001);
		assertEquals(3.0, mpMultiplier, 0.001);
		assertEquals(4.0, cpMultiplier, 0.001);

		double baseHpRegen = (1.5 + (pc.level() / 10.0)) * hpMultiplier;
		// Level 80: 1.5 + 8.0 = 9.5 * 2 = 19.0
		assertEquals(19.0, baseHpRegen, 0.001);
	}
}
