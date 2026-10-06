package com.lopez.l2j.game.event.official;

public record EventDropEntry(
		int itemId,
		int minCount,
		int maxCount,
		double chancePercent,
		int minMonsterLevel
) {}
