package com.lopez.l2j.game.achievements;

import java.util.List;

public record AchievementDef(
		int id,
		String name,
		String description,
		AchievementType type,
		long requiredValue,
		boolean repeatable,
		List<AchievementReward> rewards
) {}
