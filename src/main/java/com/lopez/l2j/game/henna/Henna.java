package com.lopez.l2j.game.henna;

/**
 * Representa um simbolo / tatuagem (Henna/Dye) de Interlude.
 */
public record Henna(
		int symbolId,
		String name,
		int dyeId,
		int dyeAmount,
		int price,
		int statInt,
		int statStr,
		int statCon,
		int statMen,
		int statDex,
		int statWit
) {}
