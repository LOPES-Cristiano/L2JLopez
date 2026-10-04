package com.lopez.l2j.game.multisell;

import java.util.List;

public record MultiSellContainer(
		int listId,
		boolean applyTaxes,
		boolean maintainEnchantment,
		List<MultiSellEntry> entries) {

	public record MultiSellEntry(
			int entryId,
			List<Ingredient> ingredients,
			List<Ingredient> products) {
	}

	public record Ingredient(
			int itemId,
			long count,
			int enchantLevel) {
	}
}
