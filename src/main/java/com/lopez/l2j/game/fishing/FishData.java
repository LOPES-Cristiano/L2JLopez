package com.lopez.l2j.game.fishing;

/**
 * Representa os dados de um peixe carregado de fishes.xml.
 */
public record FishData(
		int id,
		int level,
		String name,
		int hp,
		int hpRegen,
		int type,
		int group,
		int guts,
		int gutsCheckTime,
		int waitTime,
		int combatTime
) {
}
