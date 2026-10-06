package com.lopez.l2j.game.event.official;

import java.util.Map;

public record EventRewardExchange(
		int exchangeId,
		String name,
		Map<Integer, Integer> requiredItems,
		Map<Integer, Integer> rewardItems
) {}
