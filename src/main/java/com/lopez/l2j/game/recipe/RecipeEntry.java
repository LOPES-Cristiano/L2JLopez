package com.lopez.l2j.game.recipe;

import java.util.List;

/**
 * Representa uma receita de crafting (Dwarven ou Common Craft) do Lineage II Interlude.
 */
public record RecipeEntry(
		int id,
		String recipeName,
		int recipeItemId,
		int level,
		int productId,
		int productCount,
		int mpCost,
		int successRate,
		boolean isDwarven,
		List<Ingredient> ingredients
) {
	public record Ingredient(int itemId, int count) {}

	public RecipeEntry {
		recipeName = recipeName == null ? "" : recipeName;
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
	}
}
