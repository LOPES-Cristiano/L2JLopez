package com.lopez.l2j.game.castle.mercenary;

/**
 * Representa um mercenario contratado para defesa de castelo (castle_siege_guards).
 */
public record MercenaryRecord(
		int id,
		int castleId,
		int npcId,
		int x,
		int y,
		int z,
		int heading,
		int respawnDelay,
		boolean isHired
) {}
