package com.lopez.l2j.game.fortress;

/**
 * Funcao ativa de fortaleza (tabela fort_functions).
 */
public record FortressFunctionRecord(
		int fortId,
		int type,
		int level,
		int lease,
		int rate,
		long endTime
) {}
