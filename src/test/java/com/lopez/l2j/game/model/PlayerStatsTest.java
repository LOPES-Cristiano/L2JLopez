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
		assertEquals(8, stats.critical(), "Critical deve vir da arma equipada");
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
}
