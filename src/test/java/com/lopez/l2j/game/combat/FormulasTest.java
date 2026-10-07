package com.lopez.l2j.game.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormulasTest {

	@Test
	@DisplayName("LevelMod oficial: (Level + 89.0) / 100.0")
	void testLevelMod() {
		assertEquals(0.90, Formulas.calcLevelMod(1), 0.001);
		assertEquals(1.29, Formulas.calcLevelMod(40), 0.001);
		assertEquals(1.64, Formulas.calcLevelMod(75), 0.001);
		assertEquals(1.69, Formulas.calcLevelMod(80), 0.001);
	}

	@Test
	@DisplayName("Dano fisico canonico com soulshot (2x) e bloqueio de escudo")
	void testPhysicalDamage() {
		// (77.0 * 500 * 1.0) / 200 = 192.5 -> ~193
		int normal = Formulas.calcPhysicalDamage(500, 200, false, 1.0, false, false, 0);
		assertEquals(193, normal);

		// Com Soulshot (2x): (77.0 * 500 * 2.0) / 200 = 385
		int withSoulshot = Formulas.calcPhysicalDamage(500, 200, true, 1.0, false, false, 0);
		assertEquals(385, withSoulshot);

		// Com Crítico (2x adicional): 385 * 2 = 770
		int withCrit = Formulas.calcPhysicalDamage(500, 200, true, 1.0, true, false, 0);
		assertEquals(770, withCrit);

		// Com Bloqueio de Escudo (+150 pDef): (77.0 * 500 * 1.0) / 350 = 110
		int shieldBlocked = Formulas.calcPhysicalDamage(500, 200, false, 1.0, false, true, 150);
		assertEquals(110, shieldBlocked);
		assertTrue(shieldBlocked < normal);
	}

	@Test
	@DisplayName("Dano magico canonico com Spiritshot (BSS 4x) e critico magico (3x)")
	void testMagicDamage() {
		// 91.0 * power 100 * sqrt(400) / mDef 200 = 91 * 100 * 20 / 200 = 910 (+- 5% rnd)
		int dmgNoShots = Formulas.calcMagicDamage(400, 100, 200, false, false, false);
		assertTrue(dmgNoShots >= 860 && dmgNoShots <= 960);

		// Com BSS: sqrt(400 * 4) = 40 (dobra o multiplicador de mAtk)
		int dmgWithBss = Formulas.calcMagicDamage(400, 100, 200, false, true, false);
		assertTrue(dmgWithBss >= 1700 && dmgWithBss <= 1950);

		// Com Critico Magico (3x)
		int dmgWithCrit = Formulas.calcMagicDamage(400, 100, 200, false, false, true);
		assertTrue(dmgWithCrit >= 2500 && dmgWithCrit <= 2900);
	}

	@Test
	@DisplayName("Cap de taxa de critico fisico (500) e magico (200)")
	void testCritCaps() {
		int uncappedPhys = Formulas.calcCritRate(400, 1.5, 2.0); // 1200
		assertEquals(Formulas.MAX_PCRIT_CAP, uncappedPhys, "Critico fisico deve respeitar o cap de 500 (50%)");

		int uncappedMag = Formulas.calcMagicCritRate(150, 2.5); // 375
		assertEquals(Formulas.MAX_MCRIT_CAP, uncappedMag, "Critico magico deve respeitar o cap de 200 (20%)");
	}

	@Test
	@DisplayName("Penetracao de CP no PvP: Dano drena primeiro CP e o excedente atinge HP")
	void testCpPenetration() {
		// Caso 1: Dano menor que o CP restante
		var res1 = Formulas.calcCpPenetration(300, 500, 1000);
		assertEquals(300, res1.cpDamage());
		assertEquals(0, res1.hpDamage());
		assertEquals(200.0, res1.remainingCp());
		assertEquals(1000.0, res1.remainingHp());

		// Caso 2: Dano maior que o CP (penetra no HP)
		var res2 = Formulas.calcCpPenetration(700, 500, 1000);
		assertEquals(500, res2.cpDamage());
		assertEquals(200, res2.hpDamage());
		assertEquals(0.0, res2.remainingCp());
		assertEquals(800.0, res2.remainingHp());
	}
}
