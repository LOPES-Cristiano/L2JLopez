package com.lopez.l2j.network.game;

/**
 * Relogio do mundo (porta simplificada do GameTimeController): 1 dia de jogo = 4 horas reais,
 * ou seja, 1 minuto de jogo a cada 10 segundos reais.
 */
public final class GameTime {

	static final int MINUTES_PER_DAY = 24 * 60;
	static final long REAL_MILLIS_PER_GAME_MINUTE = 10_000;

	private GameTime() {
	}

	/** Minutos desde a meia-noite do dia de jogo atual (0..1439). */
	public static int now() {
		return at(System.currentTimeMillis());
	}

	static int at(long epochMillis) {
		return (int) ((epochMillis / REAL_MILLIS_PER_GAME_MINUTE) % MINUTES_PER_DAY);
	}
}
