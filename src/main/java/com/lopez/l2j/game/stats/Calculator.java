package com.lopez.l2j.game.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculator unificado de atributo (L2J / Lucera).
 * Mantem e executa a cadeia ordenada de funcoes matematicas (FuncAdd, FuncEnchant, FuncMul, FuncDiv).
 */
public class Calculator {

	private final Stat stat;
	private final List<Func> functions = new ArrayList<>();

	public Calculator(Stat stat) {
		this.stat = stat;
	}

	public Stat getStat() {
		return stat;
	}

	public synchronized int size() {
		return functions.size();
	}

	public synchronized List<Func> getFunctions() {
		return Collections.unmodifiableList(new ArrayList<>(functions));
	}

	public synchronized void addFunc(Func f) {
		if (f == null) return;
		functions.add(f);
		Collections.sort(functions);
	}

	public synchronized void removeFunc(Func f) {
		if (f == null) return;
		functions.remove(f);
	}

	public synchronized void removeOwner(Object owner) {
		if (owner == null) return;
		functions.removeIf(f -> f.getFuncOwner() == owner);
	}

	public synchronized void calc(Env env) {
		for (Func f : functions) {
			f.calc(env);
		}
	}

	public synchronized double calculate(Env env) {
		calc(env);
		return env.getValue();
	}
}
