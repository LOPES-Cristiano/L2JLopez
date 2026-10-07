package com.lopez.l2j.game.stats;

/**
 * Funcao abstrata base do motor de Calculators do L2J/Lucera.
 * Cada funcao possui uma ordem de precedencia (order):
 * 0x10 = FuncAdd (adicoes fixas de itens/templates)
 * 0x20 = FuncEnchant (bonus de encantamento de itens)
 * 0x30 = FuncMul (multiplicadores de buffs, passivas)
 * 0x40 = FuncDiv (reducoes de debuffs, penalidades de peso/grade)
 */
public abstract class Func implements Comparable<Func> {

	public static final int ORDER_ADD = 0x10;
	public static final int ORDER_ENCHANT = 0x20;
	public static final int ORDER_MUL = 0x30;
	public static final int ORDER_DIV = 0x40;

	private final Stat stat;
	private final int order;
	private final Object funcOwner;
	private final double value;

	public Func(Stat stat, int order, Object funcOwner, double value) {
		this.stat = stat;
		this.order = order;
		this.funcOwner = funcOwner;
		this.value = value;
	}

	public Stat getStat() {
		return stat;
	}

	public int getOrder() {
		return order;
	}

	public Object getFuncOwner() {
		return funcOwner;
	}

	public double getValue() {
		return value;
	}

	public abstract void calc(Env env);

	@Override
	public int compareTo(Func o) {
		return Integer.compare(this.order, o.order);
	}
}
