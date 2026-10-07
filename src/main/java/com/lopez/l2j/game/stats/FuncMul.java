package com.lopez.l2j.game.stats;

public class FuncMul extends Func {

	public FuncMul(Stat stat, int order, Object funcOwner, double value) {
		super(stat, order, funcOwner, value);
	}

	public FuncMul(Stat stat, Object funcOwner, double value) {
		super(stat, ORDER_MUL, funcOwner, value);
	}

	@Override
	public void calc(Env env) {
		env.value *= getValue();
	}
}
