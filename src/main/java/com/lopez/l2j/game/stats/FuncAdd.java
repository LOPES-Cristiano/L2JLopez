package com.lopez.l2j.game.stats;

public class FuncAdd extends Func {

	public FuncAdd(Stat stat, int order, Object funcOwner, double value) {
		super(stat, order, funcOwner, value);
	}

	public FuncAdd(Stat stat, Object funcOwner, double value) {
		super(stat, ORDER_ADD, funcOwner, value);
	}

	@Override
	public void calc(Env env) {
		env.value += getValue();
	}
}
