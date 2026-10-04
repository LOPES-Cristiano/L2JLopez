package com.lopez.l2j.game.drop;

/**
 * Representa uma entrada da tabela droplist/custom_droplist.
 *
 * @param mobId ID do monstro
 * @param itemId ID do item dropado
 * @param min Quantidade minima
 * @param max Quantidade maxima
 * @param category Categoria: 0 = adena, >0 = itens normais, <0 = spoil (-1)
 * @param chance Chance base de drop (escala de 1 a 1.000.000, onde 1.000.000 = 100%)
 */
public record DropData(
		int mobId,
		int itemId,
		int min,
		int max,
		int category,
		int chance) {

	public static final int MAX_CHANCE = 1_000_000;
	public static final int ADENA_ID = 57;

	public boolean isAdena() {
		return itemId == ADENA_ID || category == 0;
	}

	public boolean isSpoil() {
		return category < 0;
	}

	public boolean isNormalDrop() {
		return category >= 0;
	}
}
