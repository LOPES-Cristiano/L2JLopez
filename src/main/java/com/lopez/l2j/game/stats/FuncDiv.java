package com.lopez.l2j.game.stats;

public class FuncDiv extends Func {

	public FuncDiv(Stat stat, int order, Object funcOwner, double value) {
		super(stat, order, funcOwner, value);
	}

	public FuncDiv(Stat stat, Object funcOwner, double value) {
		super(stat, ORDER_DIV, funcOwner, value);
	}

	@Override
	public void calc(Env env) {
		if (getValue() != 0.0) {
			env.value /= getValue();
		}
	}
}
