package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Locale;

/**
 * Funcao de stat do L2J ({@code <add>}, {@code <mul>}... dentro de {@code <for>}). As funcoes de um stat sao
 * aplicadas em ordem crescente de {@code order} sobre o valor base (template + itens), como o Calculator do L2J.
 *
 * @param condition condicao opcional (ex.: {@code <using kind="Light"/>}); null = sempre aplica
 */
public record StatFunc(String stat, Op op, int order, double value, SkillCondition condition) {

	public enum Op {
		ADD, SUB, MUL, DIV, SET, BASEMUL;

		public static Op parse(String tag) {
			return switch (tag.toLowerCase(Locale.ROOT)) {
				case "add" -> ADD;
				case "sub" -> SUB;
				case "mul" -> MUL;
				case "div" -> DIV;
				case "set" -> SET;
				case "basemul" -> BASEMUL;
				default -> null;
			};
		}
	}

	public StatFunc(String stat, Op op, int order, double value) {
		this(stat, op, order, value, null);
	}

	public boolean appliesTo(PlayerCharacter p) {
		return condition == null || condition.test(p);
	}

	/** Aplica a funcao ao valor corrente; {@code base} e o valor antes de qualquer funcao (para basemul). */
	public double apply(double current, double base) {
		return switch (op) {
			case ADD -> current + value;
			case SUB -> current - value;
			case MUL -> current * value;
			case DIV -> value == 0 ? current : current / value;
			case SET -> value;
			case BASEMUL -> current + base * value;
		};
	}
}
