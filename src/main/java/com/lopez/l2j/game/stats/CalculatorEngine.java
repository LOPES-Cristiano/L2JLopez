package com.lopez.l2j.game.stats;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Motor central de Calculators gerenciando os calculadores de todos os atributos.
 */
public class CalculatorEngine {

	private final Map<Stat, Calculator> calculators = new EnumMap<>(Stat.class);

	public CalculatorEngine() {
		for (Stat s : Stat.values()) {
			calculators.put(s, new Calculator(s));
		}
	}

	public Calculator getCalculator(Stat stat) {
		return calculators.computeIfAbsent(stat, Calculator::new);
	}

	public void addFunc(Func f) {
		if (f != null && f.getStat() != null) {
			getCalculator(f.getStat()).addFunc(f);
		}
	}

	public void removeFunc(Func f) {
		if (f != null && f.getStat() != null) {
			Calculator calc = calculators.get(f.getStat());
			if (calc != null) {
				calc.removeFunc(f);
			}
		}
	}

	public void removeOwner(Object owner) {
		if (owner == null) return;
		for (Calculator c : calculators.values()) {
			c.removeOwner(owner);
		}
	}

	public double calcStat(Stat stat, double baseValue, PlayerCharacter player) {
		Calculator calc = calculators.get(stat);
		if (calc == null || calc.size() == 0) {
			return baseValue;
		}
		Env env = new Env(player, baseValue);
		return calc.calculate(env);
	}
}
