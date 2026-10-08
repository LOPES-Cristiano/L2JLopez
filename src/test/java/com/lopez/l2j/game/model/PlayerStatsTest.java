package com.lopez.l2j.game.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerStatsTest {

	private CharTemplate template;
	private PlayerCharacter player;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		template = new CharTemplateTable().get(0).orElseThrow();
		player = new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		inventory = new Inventory(player.objectId());
		player.inventory(inventory);
	}

	@Test
	void baseStatsMatchTemplateWhenUnarmed() {
		var stats = PlayerStats.calculate(player, template);
		assertEquals(template.pAtk(), stats.pAtk());
		assertEquals(template.pDef(), stats.pDef());
		assertEquals(template.mAtk(), stats.mAtk());
		assertEquals(template.mDef(), stats.mDef());
		assertEquals(template.pAtkSpd(), stats.pAtkSpd());
		assertEquals(template.critical(), stats.critical());
	}

	@Test
	void weaponIncreasesPAtkAndChangesAtkSpeed() {
		// Espada com 35 pAtk, 379 atkSpeed, 8 critical
		var swordTemplate = ItemTemplate.weapon(1, 1, "Short Sword", "rhand", "sword", 1300, "none",
				35, 10, 379, 8, 0, 100, true, true, true, true);
		var sword = new ItemInstance(0x20000001, swordTemplate, player.objectId(), 1);
		inventory.add(sword);
		inventory.equip(sword);

		var stats = PlayerStats.calculate(player, template);
		assertEquals(template.pAtk() + 35, stats.pAtk(), "P.Atk deve somar o dano da arma");
		assertEquals(379, stats.pAtkSpd(), "AtkSpd deve vir da arma equipada");
		assertEquals(80, stats.critical(), "Critical deve vir da arma equipada normalizado para base 1000");
	}

	@Test
	void armorIncreasesPDefAndJewelsIncreaseMDef() {
		// Peitoral com 40 pDef
		var chestTemplate = ItemTemplate.armor(10, 10, "Tunic", "chest", "magic", 1000, "none",
				40, 0, 200, true, true, true, true);
		var chest = new ItemInstance(0x20000002, chestTemplate, player.objectId(), 1);
		inventory.add(chest);
		inventory.equip(chest);

		// Colar com 18 mDef
		var neckTemplate = ItemTemplate.armor(20, 20, "Necklace", "neck", "none", 150, "none",
				0, 18, 500, true, true, true, true);
		var neck = new ItemInstance(0x20000003, neckTemplate, player.objectId(), 1);
		inventory.add(neck);
		inventory.equip(neck);

		var stats = PlayerStats.calculate(player, template);
		assertEquals(template.pDef() + 40, stats.pDef(), "P.Def deve somar o valor da armadura");
		assertEquals(template.mDef() + 18, stats.mDef(), "M.Def deve somar o valor da joia");
	}

	@Test
	void enchantLevelIncreasesStats() {
		// Espada +3 (+6 P.Atk)
		var swordTemplate = ItemTemplate.weapon(1, 1, "Short Sword", "rhand", "sword", 1300, "none",
				35, 10, 379, 8, 0, 100, true, true, true, true);
		var sword = new ItemInstance(0x20000001, swordTemplate, player.objectId(), 1);
		sword.enchant(3);
		inventory.add(sword);
		inventory.equip(sword);

		var stats = PlayerStats.calculate(player, template);
		assertEquals(template.pAtk() + 35 + (3 * 2), stats.pAtk(), "Enchant deve adicionar bônus de P.Atk");
	}

	@Test
	void levelProgressionIncreasesAccuracyAndEvasion() {
		// Level 1: Math.round(sqrt(30) * 6) + 1 = 33 + 1 = 34
		var statsLv1 = PlayerStats.calculate(player, template);
		assertEquals(34, statsLv1.accuracy(), "Accuracy no Lv 1 deve ser base DEX + Level");
		assertEquals(34, statsLv1.evasion(), "Evasion no Lv 1 deve ser base DEX + Level");

		// Simulando Level 60
		player.level(60);
		var statsLv60 = PlayerStats.calculate(player, template);
		assertEquals(93, statsLv60.accuracy(), "Accuracy no Lv 60 deve progredir com o nivel (33 + 60 = 93)");
		assertEquals(93, statsLv60.evasion(), "Evasion no Lv 60 deve progredir com o nivel (33 + 60 = 93)");
	}

	@Test
	void criticalScalesWithWeaponAndDex() {
		// Arco com 12 critical (120 em base 1000) e penalidade de hit -3
		var bowTemplate = ItemTemplate.weapon(14, 14, "Bow", "lrhand", "bow", 1930, "none",
				23, 9, 293, 12, -3, 0, 0, 12500, true, true, true, true);
		var bow = new ItemInstance(0x20000005, bowTemplate, player.objectId(), 1);
		inventory.add(bow);
		inventory.equip(bow);

		player.level(60);
		var stats = PlayerStats.calculate(player, template);
		// Accuracy: 33 + 60 - 3 = 90
		assertEquals(90, stats.accuracy(), "Arco deve aplicar modificador hitModify na precisao");
		// DEX 30 bonus = 1.00 -> 120 * 1.00 = 120
		assertEquals(120, stats.critical(), "Bow critical com DEX 30 deve ser 120");
	}

	@Test
	void statsCapsRespectPlayerPropertiesConfiguration() {
		// Configura limites restritivos
		com.lopez.l2j.config.Config.setProperty("MaxPAtkSpeed", "300");
		com.lopez.l2j.config.Config.setProperty("MaxRunSpeed", "100");
		com.lopez.l2j.config.Config.setProperty("MaxEvasion", "20");
		com.lopez.l2j.config.Config.setProperty("AltPCriticalCap", "50");

		var swordTemplate = ItemTemplate.weapon(1, 1, "Short Sword", "rhand", "sword", 1300, "none",
				35, 10, 379, 8, 0, 100, true, true, true, true);
		var sword = new ItemInstance(0x20000001, swordTemplate, player.objectId(), 1);
		inventory.add(sword);
		inventory.equip(sword);

		player.level(40);
		var stats = PlayerStats.calculate(player, template);

		// Sem limite seria 379, com cap deve ser 300
		assertEquals(300, stats.pAtkSpd(), "PAtkSpd deve respeitar MaxPAtkSpeed");
		// Sem limite seria template.runSpeed() (126), com cap deve ser 100
		assertEquals(100, stats.runSpeed(), "RunSpeed deve respeitar MaxRunSpeed");
		// Sem limite seria 33 + 40 = 73, com cap deve ser 20
		assertEquals(20, stats.evasion(), "Evasion deve respeitar MaxEvasion");
		// Sem limite seria 80, com cap deve ser 50
		assertEquals(50, stats.critical(), "Critical deve respeitar AltPCriticalCap");

		// Restaurar valores padrao
		com.lopez.l2j.config.Config.setProperty("MaxPAtkSpeed", "9999");
		com.lopez.l2j.config.Config.setProperty("MaxRunSpeed", "9999");
		com.lopez.l2j.config.Config.setProperty("MaxEvasion", "200");
		com.lopez.l2j.config.Config.setProperty("AltPCriticalCap", "500");
	}
}
