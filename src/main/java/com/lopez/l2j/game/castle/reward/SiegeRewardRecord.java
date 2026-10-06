package com.lopez.l2j.game.castle.reward;

/**
 * Registro de recompensa de cerco pendente na tabela reward_list.
 */
public record SiegeRewardRecord(
		int charId,
		int itemId,
		long count,
		String castleName,
		boolean rewarded
) {}
