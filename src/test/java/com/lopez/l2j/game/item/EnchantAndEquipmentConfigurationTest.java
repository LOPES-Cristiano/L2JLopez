package com.lopez.l2j.game.item;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.EnchantScrollTable.ScrollInfo;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantAndEquipmentConfigurationTest {

	@BeforeEach
	void setUp() {
		// Resetar configuracoes padrao de enchant
		Config.ALLOW_CRYSTAL_SCROLL = false;
		Config.NORMAL_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
		Config.NORMAL_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
		Config.NORMAL_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,95;5,90;6,85;7,80;8,75;9,70;10,65;11,60;12,55;13,50;14,45;15,40;16,35;";
		Config.BLESS_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
		Config.BLESS_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
		Config.BLESS_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,96;5,92;6,88;7,84;8,80;9,76;10,72;11,68;12,64;13,60;14,56;15,62;16,58;";
		Config.CRYSTAL_WEAPON_ENCHANT_LEVEL = "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;";
		Config.CRYSTAL_ARMOR_ENCHANT_LEVEL = "1,100;2,100;3,100;4,98;5,96;6,94;7,92;8,90;9,88;10,86;11,84;12,82;13,80;14,78;15,76;16,74;";
		Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL = "1,100;2,100;3,100;4,97;5,94;6,91;7,88;8,85;9,82;10,79;11,76;12,73;13,70;14,67;15,64;16,61;";
		Config.ENCHANT_MAX_WEAPON_NORMAL = 0;
		Config.ENCHANT_MAX_ARMOR_NORMAL = 0;
		Config.ENCHANT_MAX_JEWELRY_NORMAL = 0;
		Config.ENCHANT_MAX_WEAPON_BLESSED = 0;
		Config.ENCHANT_MAX_ARMOR_BLESSED = 0;
		Config.ENCHANT_MAX_JEWELRY_BLESSED = 0;
		Config.ENCHANT_MAX_WEAPON_CRYSTAL = 0;
		Config.ENCHANT_MAX_ARMOR_CRYSTAL = 0;
		Config.ENCHANT_MAX_JEWELRY_CRYSTAL = 0;
		Config.ENCHANT_OVER_CHANT_CHECK = 0;
		Config.CHECK_ENCHANT_LEVEL_EQUIP = true;
		Config.ENCHANT_SAFE_MAX = 3;
		Config.ENCHANT_SAFE_MAX_FULL = 4;
		Config.ALT_ENC_LVL_AFTER_FAIL = false;
		Config.ENCHANT_ROLL_BACK = false;
		Config.ENCHANT_ROLL_BACK_VALUE = 0;
		Config.ENCHANT_DWARF_SYSTEM = false;
		Config.ENCHANT_DWARF_1_ENCHANT_LEVEL = 8;
		Config.ENCHANT_DWARF_2_ENCHANT_LEVEL = 10;
		Config.ENCHANT_DWARF_3_ENCHANT_LEVEL = 12;
		Config.ENCHANT_DWARF_1_CHANCE = 15;
		Config.ENCHANT_DWARF_2_CHANCE = 15;
		Config.ENCHANT_DWARF_3_CHANCE = 15;

		// Resetar restricoes de equipamentos
		Config.ALLOW_LIGHT_USE_HEAVY = true;
		Config.NOT_ALLOWED_USE_HEAVY = List.of(8, 9, 23, 24, 36, 37, 92, 93, 101, 102, 108, 109);
		Config.ALLOW_HEAVY_USE_LIGHT = true;
		Config.NOT_ALLOWED_USE_LIGHT = List.of(3, 4, 5, 6, 19, 20, 21, 32, 33, 34, 90, 91, 99, 100, 106, 107, 112);
		Config.ALT_DISABLE_BOW = false;
		Config.DISABLE_BOW_FOR_CLASSES = List.of(88, 89);
		Config.ALT_DISABLE_DAGGER = false;
		Config.DISABLE_DAGGER_FOR_CLASSES = List.of(90, 91);
		Config.ALT_DISABLE_SWORD = false;
		Config.DISABLE_SWORD_FOR_CLASSES = List.of(92, 93);
		Config.ALT_DISABLE_BLUNT = false;
		Config.DISABLE_BLUNT_FOR_CLASSES = List.of(94, 95);
		Config.ALT_DISABLE_DUAL = false;
		Config.DISABLE_DUAL_FOR_CLASSES = List.of(96, 97);
		Config.ALT_DISABLE_POLLE = false;
		Config.DISABLE_POLLE_FOR_CLASSES = List.of(98, 99);
		Config.ALT_DISABLE_BIG_SWORD = false;
		Config.DISABLE_BIG_SWORD_FOR_CLASSES = List.of(100, 101);
	}

	private ItemInstance createItem(int objectId, ItemTemplate tpl, int enchant) {
		ItemInstance item = new ItemInstance(objectId, tpl, 1, 1);
		item.enchant(enchant);
		return item;
	}

	@Test
	@DisplayName("EnchantTableService: Parser de series e chances por nível")
	void testParseSeriesAndChances() {
		// Nivel seguro (+0, +1, +2)
		assertEquals(100, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 1));
		assertEquals(100, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 2));
		assertEquals(100, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 3));

		// Nivel 4 a 6
		assertEquals(96, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 4));
		assertEquals(92, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 5));
		assertEquals(88, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 6));

		// Nivel alem do maior configurado (>16) usa a ultima probabilidade (58)
		assertEquals(58, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 20));
		assertEquals(58, EnchantTableService.parseAndGetChance(Config.NORMAL_WEAPON_ENCHANT_LEVEL, 30));
	}

	@Test
	@DisplayName("EnchantTableService: Safe limits respeitados para armaduras normais e full-body")
	void testSafeLimits() {
		var weaponTpl = ItemTemplate.weapon(1, 1, "Sword", "rhand", "sword", 1000, "c", 100, 50, 300, 10, 0, 1000, true, true, true, true);
		var fullArmorTpl = ItemTemplate.armor(2, 2, "Full Plate", "fullarmor", "heavy", 5000, "c", 200, 100, 0, 0, 5000, List.of(), true, true, true, true);

		assertEquals(3, EnchantTableService.getSafeLimit(weaponTpl));
		assertEquals(4, EnchantTableService.getSafeLimit(fullArmorTpl));

		var normalScroll = EnchantScrollTable.get(951).orElseThrow(); // Weapon C normal
		var itemWeapon = createItem(10, weaponTpl, 2); // +2 -> seguro
		assertEquals(100, EnchantTableService.getEnchantChance(normalScroll, itemWeapon, 0));

		var itemWeaponAt3 = createItem(11, weaponTpl, 3); // +3 -> chance de ir pra +4 (96%)
		assertEquals(96, EnchantTableService.getEnchantChance(normalScroll, itemWeaponAt3, 0));

		var armorScroll = EnchantScrollTable.get(952).orElseThrow(); // Armor C normal
		var itemFullArmorAt3 = createItem(12, fullArmorTpl, 3); // +3 full armor ainda é seguro (safe limit 4)
		assertEquals(100, EnchantTableService.getEnchantChance(armorScroll, itemFullArmorAt3, 0));

		var itemFullArmorAt4 = createItem(13, fullArmorTpl, 4); // +4 full armor vai para +5 (92%)
		assertEquals(92, EnchantTableService.getEnchantChance(armorScroll, itemFullArmorAt4, 0));
	}

	@Test
	@DisplayName("EnchantTableService: Bônus do sistema Dwarf adiciona chance cumulativa por nível")
	void testDwarfSystemBonus() {
		Config.ENCHANT_DWARF_SYSTEM = true;
		Config.ENCHANT_DWARF_1_ENCHANT_LEVEL = 8;
		Config.ENCHANT_DWARF_1_CHANCE = 10;
		Config.ENCHANT_DWARF_2_ENCHANT_LEVEL = 10;
		Config.ENCHANT_DWARF_2_CHANCE = 15;
		Config.ENCHANT_DWARF_3_ENCHANT_LEVEL = 12;
		Config.ENCHANT_DWARF_3_CHANCE = 20;

		var weaponTpl = ItemTemplate.weapon(1, 1, "Sword", "rhand", "sword", 1000, "c", 100, 50, 300, 10, 0, 1000, true, true, true, true);
		var normalScroll = EnchantScrollTable.get(951).orElseThrow();

		// Item +8 tentando ir para +9 (chance base = 76%)
		var itemAt8 = createItem(20, weaponTpl, 8);

		// Outras racas (ex: Humano = 0) recebem a chance normal (76%)
		assertEquals(76, EnchantTableService.getEnchantChance(normalScroll, itemAt8, 0));

		// Dwarf (race = 4) recebe +10% de bonus no nivel 8 -> 86%
		assertEquals(86, EnchantTableService.getEnchantChance(normalScroll, itemAt8, 4));

		// Item +12 tentando ir para +13 (chance base = 60%) -> Dwarf recebe +20% -> 80%
		var itemAt12 = createItem(21, weaponTpl, 12);
		assertEquals(60, EnchantTableService.getEnchantChance(normalScroll, itemAt12, 0));
		assertEquals(80, EnchantTableService.getEnchantChance(normalScroll, itemAt12, 4));
	}

	@Test
	@DisplayName("EnchantTableService: Comportamento de falha (Blessed rollback, Crystal no-loss, Normal evaporate)")
	void testFailureRollbacks() {
		var weaponTpl = ItemTemplate.weapon(1, 1, "Sword", "rhand", "sword", 1000, "c", 100, 50, 300, 10, 0, 1000, true, true, true, true);
		var item = createItem(30, weaponTpl, 10);

		var normalScroll = EnchantScrollTable.get(951).orElseThrow();
		var blessedScroll = EnchantScrollTable.get(6573).orElseThrow(); // Blessed C
		var crystalScroll = EnchantScrollTable.get(953).orElseThrow(); // Crystal C

		// Normal scroll: quebra e evapora (-1)
		assertEquals(-1, EnchantTableService.calculateFailureEnchant(normalScroll, item));

		// Crystal scroll: mantém o mesmo nível (10)
		assertEquals(10, EnchantTableService.calculateFailureEnchant(crystalScroll, item));

		// Blessed padrão: reseta para 0
		Config.ALT_ENC_LVL_AFTER_FAIL = false;
		assertEquals(0, EnchantTableService.calculateFailureEnchant(blessedScroll, item));

		// Blessed com AltEncLvlAfterFail = true e sem RollBack: vai para o safe point (3)
		Config.ALT_ENC_LVL_AFTER_FAIL = true;
		Config.ENCHANT_ROLL_BACK = false;
		assertEquals(3, EnchantTableService.calculateFailureEnchant(blessedScroll, item));

		// Blessed com AltEncLvlAfterFail = true e com RollBack = 2: 10 - 2 = 8
		Config.ENCHANT_ROLL_BACK = true;
		Config.ENCHANT_ROLL_BACK_VALUE = 2;
		assertEquals(8, EnchantTableService.calculateFailureEnchant(blessedScroll, item));
	}

	@Test
	@DisplayName("EnchantTableService: Verificação de limites máximos e AllowCrystalScroll")
	void testLimitsAndAllowCrystal() {
		var normalScroll = EnchantScrollTable.get(951).orElseThrow();
		var crystalScroll = EnchantScrollTable.get(953).orElseThrow();

		// AllowCrystalScroll = false
		Config.ALLOW_CRYSTAL_SCROLL = false;
		assertFalse(EnchantTableService.isScrollAllowed(crystalScroll));
		assertTrue(EnchantTableService.isScrollAllowed(normalScroll));

		Config.ALLOW_CRYSTAL_SCROLL = true;
		assertTrue(EnchantTableService.isScrollAllowed(crystalScroll));

		// EnchantMaxWeaponNormal = 16
		Config.ENCHANT_MAX_WEAPON_NORMAL = 16;
		var weaponTpl = ItemTemplate.weapon(1, 1, "Sword", "rhand", "sword", 1000, "c", 100, 50, 300, 10, 0, 1000, true, true, true, true);
		var item15 = createItem(40, weaponTpl, 15);
		var item16 = createItem(41, weaponTpl, 16);

		assertFalse(EnchantTableService.isOverEnchant(item15, normalScroll));
		assertTrue(EnchantTableService.isOverEnchant(item16, normalScroll));

		// EnchantOverChantCheck = 20
		Config.ENCHANT_MAX_WEAPON_NORMAL = 0;
		Config.ENCHANT_OVER_CHANT_CHECK = 20;
		var item20 = createItem(42, weaponTpl, 20);
		assertTrue(EnchantTableService.isOverEnchant(item20, normalScroll));
	}

	@Test
	@DisplayName("EquipmentRestrictionService: Restrições de Heavy/Light armor e armas por classe")
	void testEquipmentRestrictions() {
		PlayerCharacter duelist = new PlayerCharacter(101, "Duelist", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		duelist.classId(88); // 88 = Duelist

		PlayerCharacter archer = new PlayerCharacter(102, "Sagittarius", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		archer.classId(92); // 92 = Sagittarius (Light user)

		var heavyArmorTpl = ItemTemplate.armor(50, 50, "Heavy Armor", "chest", "heavy", 5000, "c", 100, 50, 0, 0, 1000, List.of(), true, true, true, true);
		var heavyItem = createItem(501, heavyArmorTpl, 0);

		var bowTpl = ItemTemplate.weapon(60, 60, "Bow", "lrhand", "bow", 1500, "c", 200, 50, 250, 12, 0, 2000, true, true, true, true);
		var bowItem = createItem(601, bowTpl, 0);

		// Permitido por padrao
		assertTrue(EquipmentRestrictionService.canEquip(archer, heavyItem));
		assertTrue(EquipmentRestrictionService.canEquip(duelist, bowItem));

		// 1. Proibir Light de usar Heavy
		Config.ALLOW_LIGHT_USE_HEAVY = false;
		Config.NOT_ALLOWED_USE_HEAVY = List.of(92); // Sagittarius na lista
		assertFalse(EquipmentRestrictionService.canEquip(archer, heavyItem));
		assertTrue(EquipmentRestrictionService.canEquip(duelist, heavyItem)); // Duelist pode

		// 2. Desativar Bow para classe Duelist (88)
		Config.ALT_DISABLE_BOW = true;
		Config.DISABLE_BOW_FOR_CLASSES = List.of(88);
		assertFalse(EquipmentRestrictionService.canEquip(duelist, bowItem));
		assertTrue(EquipmentRestrictionService.canEquip(archer, bowItem)); // Archer pode usar bow

		// 3. Verificacao de EnchantLevelEquip
		Config.CHECK_ENCHANT_LEVEL_EQUIP = true;
		Config.ENCHANT_OVER_CHANT_CHECK = 16;
		var overEnchantedBow = createItem(602, bowTpl, 17);
		assertFalse(EquipmentRestrictionService.canEquip(archer, overEnchantedBow));
	}
}
