package com.lopez.l2j.game.roulette;

public record RouletteItem(
		int itemId,
		String name,
		long count,
		int enchantLevel,
		double weight,
		RouletteRarity rarity,
		String icon
) {}
