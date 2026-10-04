package com.lopez.l2j.game.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.template.CharTemplateTable;
import org.junit.jupiter.api.Test;

class ConsumableTest {

	private static PlayerCharacter player() {
		return new PlayerCharacter(0x10000001, "tester", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
	}

	@Test
	void tableMapsTheItemsFromTheScreenshot() {
		assertEquals(ConsumableTable.Type.SOULSHOT, ConsumableTable.get(1835).orElseThrow().type());
		assertEquals(ConsumableTable.Type.BUFF, ConsumableTable.get(734).orElseThrow().type()); // Haste Potion
		assertEquals(ConsumableTable.Type.BUFF, ConsumableTable.get(735).orElseThrow().type()); // Alacrity
		assertEquals(ConsumableTable.Type.BUFF, ConsumableTable.get(6035).orElseThrow().type()); // Magic Haste
		assertEquals(ConsumableTable.Type.HOT_HP, ConsumableTable.get(1061).orElseThrow().type());
		assertTrue(ConsumableTable.isSoulshot(1463));
	}

	@Test
	void buffsChangeComputedStatsAndExpire() {
		var template = new CharTemplateTable().get(0).orElseThrow();
		var p = player();
		var base = PlayerStats.calculate(p, template);

		long future = System.currentTimeMillis() + 60_000;
		p.effects().put(new ActiveBuff(2011, 1, "speed_up", future, 20, 1.0, 1.0, 0));
		p.effects().put(new ActiveBuff(2012, 1, "attack_time_down", future, 0, 1.15, 1.0, 0));
		var buffed = PlayerStats.calculate(p, template);
		assertEquals(base.runSpeed() + 20, buffed.runSpeed());
		assertEquals(Math.round(base.pAtkSpd() * 1.15), buffed.pAtkSpd());

		// Mesmo stackType substitui (Greater Haste no lugar de Haste)
		p.effects().put(new ActiveBuff(2034, 1, "speed_up", future, 33, 1.0, 1.0, 0));
		assertEquals(base.runSpeed() + 33, PlayerStats.calculate(p, template).runSpeed());

		// Buff expirado nao conta
		p.effects().clear();
		p.effects().put(new ActiveBuff(2011, 1, "speed_up", System.currentTimeMillis() - 1, 20, 1.0, 1.0, 0));
		assertEquals(base.runSpeed(), PlayerStats.calculate(p, template).runSpeed());
	}

	@Test
	void gradeIndexMatchesAttackFlagOrder() {
		assertEquals(0, ConsumableTable.gradeIndex("none"));
		assertEquals(1, ConsumableTable.gradeIndex("d"));
		assertEquals(5, ConsumableTable.gradeIndex("s"));
	}

	@Test
	void specialPotionsAreMapped() {
		assertEquals(ConsumableTable.Type.HEAL_CP, ConsumableTable.get(5592).orElseThrow().type());
		assertEquals(ConsumableTable.Type.HEAL_MP, ConsumableTable.get(728).orElseThrow().type());
		var mystery = ConsumableTable.get(5234).orElseThrow();
		assertEquals(ConsumableTable.Type.MYSTERY, mystery.type());
		assertEquals(1_200_000L, (long) mystery.ticks() * mystery.intervalMs());
		assertEquals(2, (int) ConsumableTable.get(5237).orElseThrow().amount()); // Facelifting C
		assertEquals(ConsumableTable.Type.HAIR_COLOR, ConsumableTable.get(5241).orElseThrow().type());
		assertEquals(6, (int) ConsumableTable.get(5248).orElseThrow().amount()); // Hair Style G
		assertEquals(ConsumableTable.Type.REMEDY, ConsumableTable.get(1831).orElseThrow().type());
	}

	@Test
	void abnormalEffectMaskStartsAndStops() {
		var p = player();
		p.startAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		assertEquals(0x2000, p.abnormalEffect());
		p.stopAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		assertEquals(0, p.abnormalEffect());
	}
}
