package com.lopez.l2j.game.stats;

public class FuncEnchant extends Func {

	public FuncEnchant(Stat stat, int order, Object funcOwner, double value) {
		super(stat, order, funcOwner, value);
	}

	public FuncEnchant(Stat stat, Object funcOwner, double value) {
		super(stat, ORDER_ENCHANT, funcOwner, value);
	}

	@Override
	public void calc(Env env) {
		env.value += getValue();
	}
}
