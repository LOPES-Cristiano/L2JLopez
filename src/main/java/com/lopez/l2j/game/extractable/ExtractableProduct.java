package com.lopez.l2j.game.extractable;

import java.util.List;

/**
 * Representa uma opcao de recompensa com probabilidade (chance) associada
 * em extractable_items.xml.
 */
public record ExtractableProduct(int skillId, int skillLevel, int chance, List<ProductItem> items) {

	public ExtractableProduct(int chance, List<ProductItem> items) {
		this(0, 0, chance, items);
	}

	public record ProductItem(int itemId, int count) {
	}
}
