package com.lopez.l2j.game.trade;

import java.util.List;
import java.util.Optional;

/**
 * Lista de produtos vendidos por um NPC mercador.
 */
public record NpcBuyList(int listId, int npcId, List<Product> products) {

	public record Product(int itemId, int price, int count) {}

	public Optional<Product> getProduct(int itemId) {
		return products.stream().filter(p -> p.itemId() == itemId).findFirst();
	}
}
