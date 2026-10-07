package com.lopez.l2j.game.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorEngineTest {

	@Test
	@DisplayName("Cadeia ordenada de Func (Add -> Enchant -> Mul -> Div) executada na ordem correta")
	void testCalculatorPipelineOrder() {
		Calculator calc = new Calculator(Stat.P_ATK);

		// Adiciona fora de ordem propositalmente
		calc.addFunc(new FuncDiv(Stat.P_ATK, "debuff", 2.0));        // Order 0x40 (/ 2.0)
		calc.addFunc(new FuncAdd(Stat.P_ATK, "sword", 100.0));       // Order 0x10 (+ 100.0)
		calc.addFunc(new FuncMul(Stat.P_ATK, "might_buff", 1.20));   // Order 0x30 (* 1.20)
		calc.addFunc(new FuncEnchant(Stat.P_ATK, "enchant", 30.0));   // Order 0x20 (+ 30.0)

		Env env = new Env(null, 50.0); // Base = 50.0

		// Execucao esperada:
		// 1. Base 50.0 + FuncAdd 100.0 = 150.0
		// 2. 150.0 + FuncEnchant 30.0 = 180.0
		// 3. 180.0 * FuncMul 1.20 = 216.0
		// 4. 216.0 / FuncDiv 2.0 = 108.0
		double result = calc.calculate(env);
		assertEquals(108.0, result, 0.001, "Pipeline deve respeitar rigorosamente a ordem 0x10, 0x20, 0x30, 0x40");
	}

	@Test
	@DisplayName("Remover funcoes por proprietario (owner) limpa buffs expirados")
	void testRemoveOwner() {
		Calculator calc = new Calculator(Stat.RUN_SPEED);
		Object windWalk = new Object();
		Object berserkerSpirit = new Object();

		calc.addFunc(new FuncAdd(Stat.RUN_SPEED, windWalk, 33.0));
		calc.addFunc(new FuncAdd(Stat.RUN_SPEED, berserkerSpirit, 8.0));
		assertEquals(2, calc.size());

		calc.removeOwner(windWalk);
		assertEquals(1, calc.size());

		Env env = new Env(null, 120.0);
		double result = calc.calculate(env);
		assertEquals(128.0, result, 0.001);
	}

	@Test
	@DisplayName("CalculatorEngine gerencia calculadores de todos os atributos")
	void testCalculatorEngine() {
		CalculatorEngine engine = new CalculatorEngine();
		engine.addFunc(new FuncMul(Stat.M_ATK, "empower", 1.75));

		double baseMAtk = 200.0;
		double buffedMAtk = engine.calcStat(Stat.M_ATK, baseMAtk, null);
		assertEquals(350.0, buffedMAtk, 0.001);

		// Stat sem modificadores retorna o valor base inalterado
		double pDef = engine.calcStat(Stat.P_DEF, 150.0, null);
		assertEquals(150.0, pDef, 0.001);
	}
}
