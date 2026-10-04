package com.lopez.l2j.game.drop;

/**
 * Item dropado calculado para recompensa ao jogador.
 *
 * @param itemId ID do item
 * @param count Quantidade calculada
 * @param isAdena Se o item e adena
 * @param isSpoil Se veio de spoil
 */
public record DropReward(
		int itemId,
		int count,
		boolean isAdena,
		boolean isSpoil) {

	public DropReward(int itemId, int count, boolean isAdena) {
		this(itemId, count, isAdena, false);
	}
}
