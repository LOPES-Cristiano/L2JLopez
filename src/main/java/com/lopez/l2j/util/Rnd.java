package com.lopez.l2j.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilitario de geracao de numeros aleatorios e checagem de chances (padrao L2J).
 */
public final class Rnd {

	private Rnd() {
	}

	public static boolean chance(int chance) {
		return chance > 0 && ThreadLocalRandom.current().nextInt(100) < chance;
	}

	public static boolean chance(double chance) {
		return chance > 0.0 && ThreadLocalRandom.current().nextDouble(100.0) < chance;
	}

	public static int get(int max) {
		return max <= 0 ? 0 : ThreadLocalRandom.current().nextInt(max);
	}

	public static int get(int min, int max) {
		return min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	public static double get(double min, double max) {
		return min >= max ? min : ThreadLocalRandom.current().nextDouble(min, max);
	}
}
