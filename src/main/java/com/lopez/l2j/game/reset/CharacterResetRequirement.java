package com.lopez.l2j.game.reset;

import java.util.Map;

public record CharacterResetRequirement(
		int minLevel,
		int minPvP,
		boolean requireThirdClass,
		Map<Integer, Long> requiredItems
) {}
