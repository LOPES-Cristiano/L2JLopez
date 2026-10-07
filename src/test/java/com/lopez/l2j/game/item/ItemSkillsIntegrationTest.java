package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTreeTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemSkillsIntegrationTest {

	private static SkillTable skillTable;
	private static SkillService skillService;
	private static CharTemplateTable charTemplates;

	@BeforeAll
	static void setUp() {
		skillTable = new SkillTable(Path.of("data/xml/stats/skills"));
		skillService = new SkillService(skillTable, new SkillTreeTable(Path.of("data/xml/player/skilltree")), null, false, 0, true);
		charTemplates = new CharTemplateTable();
	}

	private PlayerCharacter createPlayer(int level) {
		PlayerCharacter player = new PlayerCharacter(
				1, "testAcc", "Hero", level, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0,
				1000.0, 500.0, 500.0
		);
		player.inventory(new Inventory(player.objectId()));
		return player;
	}

	@Test
	void acumenWeaponIncreasesCastSpeed() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Homunkulus's Sword - Acumen (Item 6313, SA Skill 3047 level 1 = +15% mAtkSpd)
		var acumenHolder = new ItemSkillHolder(3047, 1);
		var weaponTpl = ItemTemplate.weapon(6313, 6313, "Homunkulus's Sword - Acumen", "rhand", "sword",
				950, "c", 111, 101, 379, 80, 0, 0, 0, 3, 3, 10, 4300000,
				List.of(acumenHolder), null, null, null, true, true, true, true);

		var item = new ItemInstance(1001, weaponTpl, 1, 0);
		player.inventory().add(item);
		player.inventory().equip(item);

		// Simula equip skill tracking
		player.skills().put(acumenHolder.skillId(), acumenHolder.level());
		skillService.refreshPassives(player);

		var equippedStats = PlayerStats.calculate(player, template);
		assertTrue(equippedStats.mAtkSpd() > baseStats.mAtkSpd(),
				"Acumen SA deve aumentar a velocidade de conjuração mágica (mAtkSpd)");
		assertEquals(Math.round(baseStats.mAtkSpd() * 1.15), equippedStats.mAtkSpd());

		// Desequipar
		player.skills().remove(acumenHolder.skillId());
		skillService.refreshPassives(player);
		var unequippedStats = PlayerStats.calculate(player, template);
		assertEquals(baseStats.mAtkSpd(), unequippedStats.mAtkSpd());
	}

	@Test
	void focusWeaponIncreasesCriticalRate() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Samurai Longsword - Focus (Skill 3010 level 4 = +83 rCrit)
		var focusHolder = new ItemSkillHolder(3010, 4);
		var slsFocusTpl = ItemTemplate.weapon(4708, 4708, "Samurai Longsword - Focus", "rhand", "sword",
				1380, "c", 156, 83, 379, 80, 0, 0, 0, 3, 3, 10, 6130000,
				List.of(focusHolder), null, null, null, true, true, true, true);

		var item = new ItemInstance(1002, slsFocusTpl, 1, 0);
		player.inventory().add(item);
		player.inventory().equip(item);

		player.skills().put(focusHolder.skillId(), focusHolder.level());
		skillService.refreshPassives(player);

		var equippedStats = PlayerStats.calculate(player, template);
		assertTrue(equippedStats.critical() > baseStats.critical(),
				"Focus SA deve aumentar a taxa de crítico");

		player.inventory().unequip(item);
		player.skills().remove(focusHolder.skillId());
		skillService.refreshPassives(player);
		var unequippedStats = PlayerStats.calculate(player, template);
		assertEquals(baseStats.critical(), unequippedStats.critical());
	}

	@Test
	void bossJewelQueenAntRingAppliesAccuracyAndSkills() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Ring of Queen Ant (Item 6660, Skill 3562 level 1 = +2 Accuracy, etc.)
		var qaSkill = new ItemSkillHolder(3562, 1);
		var qaRingTpl = ItemTemplate.armor(6660, 6660, "Ring of Queen Ant", "rfinger,lfinger", "none",
				150, "s", 0, 48, 0, 21, 616000, List.of(qaSkill), true, true, true, true);

		var item = new ItemInstance(1003, qaRingTpl, 1, 0);
		player.inventory().add(item);
		player.inventory().equip(item);

		player.skills().put(qaSkill.skillId(), qaSkill.level());
		skillService.refreshPassives(player);

		var equippedStats = PlayerStats.calculate(player, template);
		assertTrue(equippedStats.accuracy() > baseStats.accuracy(),
				"Anel de Queen Ant deve aumentar accuracy");
	}

	@Test
	void dualWeaponsPlus4EnchantSkill() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Dual SLS (Item 2626, enchant4 skill 3045 level 4)
		var e4Holder = new ItemSkillHolder(3045, 4);
		var dualTpl = ItemTemplate.weapon(2626, 2626, "Samurai Long Sword*Samurai Long Sword", "lrhand", "dual",
				2080, "b", 236, 99, 325, 80, 0, 0, 0, 1, 1, 10, 13100000,
				List.of(), e4Holder, null, null, true, true, true, true);

		var dualItem = new ItemInstance(1004, dualTpl, 1, 4);
		player.inventory().add(dualItem);
		player.inventory().equip(dualItem);

		// Com enchant >= 4, ativa e4Holder
		player.skills().put(e4Holder.skillId(), e4Holder.level());
		skillService.refreshPassives(player);

		var equippedStats = PlayerStats.calculate(player, template);
		assertTrue(equippedStats.pAtkSpd() > baseStats.pAtkSpd() || equippedStats.critical() > baseStats.critical(),
				"Dual +4 deve aplicar a habilidade de encantamento (skills_enchant4)");
	}
}
