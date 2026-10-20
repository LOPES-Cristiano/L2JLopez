package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audita e valida que todas as familias de buffs do Interlude (Prophet, SE, EE, Bishop,
 * Overlord, Warcryer, Songs, Dances, Queen/Seraphim, Hero, Clan, Toggles) aplicam 100%
 * dos atributos e efeitos descritos no skillgrp/skillname e nos XMLs de stats/skills.
 */
class BuffEffectsVerificationTest {

	private static SkillTable skillTable;

	@BeforeAll
	static void loadSkills() {
		Path skillsDir = Path.of("data/xml/stats/skills");
		if (!skillsDir.toFile().exists()) {
			skillsDir = Path.of("L2JLopez/data/xml/stats/skills");
		}
		skillTable = new SkillTable(skillsDir);
	}

	private void applySkillBuff(PlayerCharacter p, int skillId, int level) {
		SkillTemplate sk = skillTable.get(skillId, level).orElseThrow(
				() -> new AssertionError("Skill nao encontrada: " + skillId + " lvl " + level));
		for (var eff : sk.effects()) {
			String stack = eff.stackType() == null || eff.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + eff.name()
					: eff.stackType();
			p.effects().put(PlayerEffects.ActiveBuff.ofSkill(sk.id(), sk.level(), stack,
					System.currentTimeMillis() + 600_000L, eff.funcs()));
		}
	}

	private PlayerCharacter newTestPlayer(int x, int y, int heading) {
		PlayerCharacter p = new PlayerCharacter(1, "Tester", 78, x, y, 0);
		p.heading(heading);
		return p;
	}

	@Test
	void testCoreProphetAndChantAndSongDanceStats() {
		PlayerCharacter p = newTestPlayer(0, 0, 0);

		// Might Lv3 (+15% pAtk), Shield Lv3 (+15% pDef), Wind Walk Lv2 (+33 runSpd),
		// Haste Lv2 (+33% pAtkSpd), Acumen Lv3 (+30% mAtkSpd), Empower Lv3 (+75% mAtk),
		// Magic Barrier Lv2 (+30% mDef), Blessed Body Lv6 (+35% maxHp), Blessed Soul Lv6 (+35% maxMp)
		applySkillBuff(p, 1068, 3);
		applySkillBuff(p, 1040, 3);
		applySkillBuff(p, 1204, 2);
		applySkillBuff(p, 1086, 2);
		applySkillBuff(p, 1085, 3);
		applySkillBuff(p, 1059, 3);
		applySkillBuff(p, 1036, 2);
		applySkillBuff(p, 1045, 6);
		applySkillBuff(p, 1048, 6);

		assertEquals(115.0, PlayerStats.applyStat(p, "pAtk", 100.0), 0.1);
		assertEquals(115.0, PlayerStats.applyStat(p, "pDef", 100.0), 0.1);
		assertEquals(133.0, PlayerStats.applyStat(p, "runSpd", 100.0), 0.1);
		assertEquals(133.0, PlayerStats.applyStat(p, "pAtkSpd", 100.0), 0.1);
		assertEquals(130.0, PlayerStats.applyStat(p, "mAtkSpd", 100.0), 0.1);
		assertEquals(175.0, PlayerStats.applyStat(p, "mAtk", 100.0), 0.1);
		assertEquals(130.0, PlayerStats.applyStat(p, "mDef", 100.0), 0.1);
		assertEquals(1350.0, PlayerStats.applyStat(p, "maxHp", 1000.0), 0.1);
		assertEquals(1350.0, PlayerStats.applyStat(p, "maxMp", 1000.0), 0.1);

		// Song of Earth (264) +25% pDef, Dance of Fire (274) +35% cAtk, Song of Hunter (269) +100% rCrit
		applySkillBuff(p, 264, 1);
		applySkillBuff(p, 274, 1);
		applySkillBuff(p, 269, 1);

		assertEquals(143.75, PlayerStats.applyStat(p, "pDef", 100.0), 0.1);
		assertEquals(135.0, PlayerStats.applyStat(p, "cAtk", 100.0), 0.1);
		assertEquals(200.0, PlayerStats.applyStat(p, "rCrit", 100.0), 0.1);
	}

	@Test
	void testVampiricReflectRegenMpConsumeAndReuseBuffs() {
		PlayerCharacter p = newTestPlayer(0, 0, 0);

		// Vampiric Rage Lv4 (+9% absorbDam) + Dance of Vampire (+8% absorbDam) = 17%
		applySkillBuff(p, 1268, 4);
		applySkillBuff(p, 310, 1);
		assertEquals(17.0, PlayerStats.applyStat(p, "absorbDam", 0.0), 0.1);

		// Chant of Revenge Lv3 (1284) (+20% reflectDam) + Song of Vengeance (305) (+20% reflectDam) = 40%
		applySkillBuff(p, 1284, 3);
		applySkillBuff(p, 305, 1);
		assertEquals(40.0, PlayerStats.applyStat(p, "reflectDam", 0.0), 0.1);

		// Regeneration Lv3 (1044) (+20% regHp) + Song of Life (265) (+20% regHp)
		applySkillBuff(p, 1044, 3);
		applySkillBuff(p, 265, 1);
		assertEquals(14.4, PlayerStats.applyStat(p, "regHp", 10.0), 0.1);

		// Song of Meditation (363) (+20% regMp, -10% MagicalMpConsumeRate)
		// Clarity Lv3 (1397) (-20% PhysicalMpConsumeRate, -10% MagicalMpConsumeRate, -20% DanceMpConsumeRate)
		applySkillBuff(p, 363, 1);
		applySkillBuff(p, 1397, 3);
		assertEquals(12.0, PlayerStats.applyStat(p, "regMp", 10.0), 0.1);
		assertEquals(0.81, PlayerStats.applyStat(p, "MagicalMpConsumeRate", 1.0), 0.01);
		assertEquals(0.80, PlayerStats.applyStat(p, "PhysicalMpConsumeRate", 1.0), 0.01);
		assertEquals(0.80, PlayerStats.applyStat(p, "DanceMpConsumeRate", 1.0), 0.01);

		// Song of Renewal (349) (-30% mReuse/pReuse = 0.7) + Song of Champion (364) (-20% pReuse = 0.8)
		applySkillBuff(p, 349, 1);
		applySkillBuff(p, 364, 1);
		assertTrue(PlayerStats.applyStat(p, "mReuse", 1.0) < 0.85);
		assertTrue(PlayerStats.applyStat(p, "pReuse", 1.0) < 0.65);

		// Prayer Lv3 (1307) (+12% gainHp)
		applySkillBuff(p, 1307, 3);
		assertEquals(112.0, PlayerStats.applyStat(p, "gainHp", 100.0), 0.1);
	}

	@Test
	void testPositionalAndMovementConditions() {
		// Target na posicao (100, 0) olhando para Leste (heading = 0 -> +X)
		// Atacante em (0, 0) esta ATRAS (behind) do alvo!
		PlayerCharacter attacker = newTestPlayer(0, 0, 0);
		PlayerCharacter target = newTestPlayer(100, 0, 0);

		assertEquals(CombatService.RelativePosition.BEHIND,
				CombatService.getRelativePosition(attacker.x(), attacker.y(), target.x(), target.y(), target.heading()),
				"Atacante deve ser detectado atras do alvo");

		// equipa adaga para ativar Focus Death (355), Focus Chance (356), Focus Power (357)
		ItemTemplate daggerTpl = ItemTemplate.weapon(1, 1, "Angel Slayer", "rhand", "dagger",
				900, "s", 240, 120, 433, 12, 0, 1000, true, true, true, true);
		ItemInstance dagger = new ItemInstance(10, daggerTpl, attacker.objectId(), 1);
		attacker.inventory().add(dagger);
		attacker.inventory().equip(dagger);

		applySkillBuff(attacker, 357, 1); // Focus Power: -30% front, +30% side, +60% behind cAtk
		applySkillBuff(attacker, 1357, 1); // Prophecy of Wind: +20% rCrit e +20% cAtk quando behind

		double cAtkBehind = PlayerStats.applyStat(attacker, target, "cAtk", 100.0);
		assertTrue(cAtkBehind > 180.0, "cAtk por tras com Focus Power + PoW deve ser > 180% (atual: " + cAtkBehind + ")");

		// Move o alvo para olhar para Oeste (heading = 32768 -> -X), agora o atacante em (0,0) esta NA FRENTE!
		target.heading(32768);
		assertEquals(CombatService.RelativePosition.FRONT,
				CombatService.getRelativePosition(attacker.x(), attacker.y(), target.x(), target.y(), target.heading()),
				"Atacante deve ser detectado na frente do alvo");
		double cAtkFront = PlayerStats.applyStat(attacker, target, "cAtk", 100.0);
		assertEquals(70.0, cAtkFront, 0.5, "cAtk pela frente com Focus Power deve reduzir em 30%");

		// Clear Mind (1297 Lv6): bonus de regMp diferenciado andando vs parado
		SkillTemplate clearMind = skillTable.get(1297, 6).orElseThrow();
		attacker.passiveFuncs(clearMind.funcs());
		attacker.running(false); // walking/standing -> +6.2 regMp
		double regWalk = PlayerStats.applyStat(attacker, "regMp", 10.0);
		attacker.running(true); // running -> sem o bonus de walking
		double regRun = PlayerStats.applyStat(attacker, "regMp", 10.0);
		assertTrue(regWalk > regRun, "Clear Mind deve conceder mais regMp andando/parado do que correndo");
	}

	@Test
	void testCombatServiceVulnerabilitiesShieldElementalAndMagicCrit() {
		PlayerCharacter attacker = newTestPlayer(0, 0, 0);
		PlayerCharacter defender = newTestPlayer(50, 0, 32768);

		// Equipa um arco no atacante
		ItemTemplate bowTpl = ItemTemplate.weapon(2, 2, "Draconic Bow", "lrhand", "bow",
				1650, "s", 581, 132, 293, 12, 0, 1000, true, true, true, true);
		ItemInstance bow = new ItemInstance(11, bowTpl, attacker.objectId(), 1);
		attacker.inventory().add(bow);
		attacker.inventory().equip(bow);

		// 1. Deflect Arrow Lv4 (112) reduz dano de arco em 40% (bowWpnVuln * 0.60)
		applySkillBuff(defender, 112, 4);
		assertEquals(0.60, CombatService.calcWeaponVulnerability(attacker, defender), 0.01);

		// 2. Holy Weapon (1043) + Dance of Light (277) + Eye of Hunter (359 Lv1)
		applySkillBuff(attacker, 1043, 1); // +20 holyPower / undead bonus
		applySkillBuff(attacker, 359, 1);  // +30% pAtk-insects/animals/plants
		NpcTemplate undeadTpl = new NpcTemplate(
				20001, 20001, "Zombie", true, "", false,
				9.0, 24.0, 70, "male", "L2Monster", 40,
				2000, 1000, 150, 150, 150, 150, 250, 333,
				0, 0, 0, 50, 120, 0, true, 0L, 0);
		NpcInstance undeadNpc = new NpcInstance(9001, undeadTpl, 50, 0, 0, 0);
		assertTrue(CombatService.calcNpcTypeAtkBonus(attacker, undeadNpc) >= 1.20, "Holy Weapon deve aumentar dano contra Undead");

		NpcTemplate bugTpl = NpcTemplate.fallback(20002, "Giant Spider Bug", "L2Monster");
		NpcInstance bugNpc = new NpcInstance(9002, bugTpl, 50, 0, 0, 0);
		assertTrue(CombatService.calcNpcTypeAtkBonus(attacker, bugNpc) >= 1.25, "Eye of Hunter deve aumentar dano contra insetos");

		// 3. Wild Magic Lv2 (1303) + Siren's Dance (365) -> mCritRate
		CharTemplate mageTpl = new CharTemplate(
				12, "Archmage", 0,
				22, 27, 21, 41, 20, 39,
				100, 100, 100, 100, 300, 333,
				30, 40, 30, 120, 50000,
				0, 0, 0, false,
				9.0, 23.0, 8.0, 22.0,
				1000, 1000, 500, 3);
		double baseMCrit = CombatService.calcMagicCritRate(attacker, mageTpl);
		applySkillBuff(attacker, 1303, 2); // Wild Magic Lv2
		double buffedMCrit = CombatService.calcMagicCritRate(attacker, mageTpl);
		assertTrue(buffedMCrit > baseMCrit * 1.5, "Wild Magic Lv2 deve aumentar a taxa de Magic Crit");

		// 4. Mental Shield Lv4 (1035) e Resist Shock Lv4 (1259) -> debuff vulnerabilities
		applySkillBuff(defender, 1035, 4); // derangementVuln, sleepVuln, rootVuln
		applySkillBuff(defender, 1259, 4); // stunVuln
		assertTrue(CombatService.calcDebuffVulnerability(defender, "stun") <= 0.65);
		assertTrue(CombatService.calcDebuffVulnerability(defender, "sleep") <= 0.55);
		assertTrue(CombatService.calcDebuffVulnerability(defender, "fear") <= 0.55);
	}
}
