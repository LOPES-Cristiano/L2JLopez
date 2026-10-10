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

	@Test
	void platedLeatherArmorSetAppliesStrBonusAndIncreasesPatk() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Plated Leather Set (skill 3511: STR +4, CON -1)
		player.skills().put(3511, 1);
		skillService.refreshPassives(player);

		var equippedStats = PlayerStats.calculate(player, template);
		assertEquals(baseStats.str() + 4, equippedStats.str(), "Plated Leather deve conceder +4 STR");
		assertEquals(baseStats.con() - 1, equippedStats.con(), "Plated Leather deve reduzir 1 CON");
		assertTrue(equippedStats.pAtk() > baseStats.pAtk(), "STR adicional deve aumentar o pAtk via strBonus ratio");
	}

	@Test
	void darkCrystalRobeSetAppliesWitAndMenAndIncreasesCastSpeed() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();
		var baseStats = PlayerStats.calculate(player, template);

		// Dark Crystal Robe Set (skill 3535: +15% Cast Spd, +8% P.Def, +7 Run Spd, WIT +2, MEN -2)
		player.skills().put(3535, 1);
		skillService.refreshPassives(player);

		var dcStats = PlayerStats.calculate(player, template);
		assertEquals(baseStats.wit() + 2, dcStats.wit(), "Dark Crystal Robe deve conceder +2 WIT");
		assertEquals(baseStats.men() - 2, dcStats.men(), "Dark Crystal Robe deve reduzir 2 MEN");
		assertEquals(baseStats.runSpeed() + 7, dcStats.runSpeed(), "Dark Crystal Robe deve conceder +7 Run Speed");
		assertTrue(dcStats.pDef() > baseStats.pDef(), "Dark Crystal Robe deve conceder +8% P.Def");
		assertTrue(dcStats.mAtkSpd() > baseStats.mAtkSpd(), "Dark Crystal Robe deve aumentar Cast Speed");
	}

	@Test
	void weaponSaHealthAndManaUpIncreasesMaxVitals() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();

		// Base vitals
		int baseMaxHp = (int) Math.round(template.calculateMaxHp(player.level())
				* (com.lopez.l2j.game.template.BaseStatsTable.conBonus(template.con()) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(template.con())));
		int baseMaxMp = (int) Math.round(template.calculateMaxMp(player.level())
				* (com.lopez.l2j.game.template.BaseStatsTable.menBonus(template.men()) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(template.men())));

		// Weapon SA: Health (skill 3013 level 5 = mul maxHp 1.25)
		player.skills().put(3013, 5);
		skillService.refreshPassives(player);

		int boostedHp = (int) Math.round(PlayerStats.applyStat(player, "maxHp", baseMaxHp));
		assertEquals(Math.round(baseMaxHp * 1.25), boostedHp, "SA Health deve conceder +25% Max HP");

		// Weapon SA: Mana Up (skill 3014 level 3 = mul maxMp 1.30)
		player.skills().put(3014, 3);
		skillService.refreshPassives(player);

		int boostedMp = (int) Math.round(PlayerStats.applyStat(player, "maxMp", baseMaxMp));
		assertEquals(Math.round(baseMaxMp * 1.30), boostedMp, "SA Mana Up deve conceder +30% Max MP");
	}

	@Test
	void criticalDamageFromBossJewelsMultipliesCritDamageInCombat() {
		var player = createPlayer(75);

		// Sem anéis: cAtk base = 1.0
		double baseCAtk = PlayerStats.applyStat(player, "cAtk", 1.0);
		assertEquals(1.0, baseCAtk);

		// Equipando Baium Ring (skill 3561: mul cAtk 1.15)
		player.skills().put(3561, 1);
		skillService.refreshPassives(player);

		double baiumCAtk = PlayerStats.applyStat(player, "cAtk", 1.0);
		assertEquals(1.15, baiumCAtk, 0.001, "Baium Ring deve conceder +15% Critical Damage");

		// Equipando Queen Ant Ring (skill 3562: mul cAtk 1.15)
		player.skills().put(3562, 1);
		skillService.refreshPassives(player);

		double dualCAtk = PlayerStats.applyStat(player, "cAtk", 1.0);
		assertEquals(1.15 * 1.15, dualCAtk, 0.001, "Baium + Queen Ant devem acumular dano crítico multiplicativo");
	}

	@Test
	void userInfoPacketEncodesEffectiveStatsFromItemPassives() {
		var player = createPlayer(75);
		var template = charTemplates.get(player.classId()).orElseThrow();

		// Equipando Plated Leather (+4 STR, -1 CON)
		player.skills().put(3511, 1);
		skillService.refreshPassives(player);

		var stats = PlayerStats.calculate(player, template);
		var userInfo = new com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo(
				player, template, player.inventory().paperdollView(), 0, stats);

		byte[] bytes = userInfo.encode();
		var buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		buf.get(); // 0x04 opcode
		buf.getInt(); buf.getInt(); buf.getInt(); buf.getInt(); // x, y, z, heading
		buf.getInt(); // objId
		while (buf.getChar() != 0) {
			// lê string UTF-16LE terminada em null
		}
		buf.getInt(); buf.getInt(); buf.getInt(); // race, sex, classId
		buf.getInt(); buf.getLong(); // level, exp

		int encodedStr = buf.getInt();
		int encodedDex = buf.getInt();
		int encodedCon = buf.getInt();

		assertEquals(template.str() + 4, encodedStr, "UserInfo deve enviar STR com bônus do set (+4)");
		assertEquals(template.dex(), encodedDex, "UserInfo deve enviar DEX inalterado");
		assertEquals(template.con() - 1, encodedCon, "UserInfo deve enviar CON com penalidade do set (-1)");
	}
}

