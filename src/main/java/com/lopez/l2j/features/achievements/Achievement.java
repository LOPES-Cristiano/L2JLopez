package com.lopez.l2j.features.achievements;

import java.util.List;
import java.util.Map;

/** Definicao declarativa de uma conquista, carregada do achievements.xml. */
public record Achievement(
		int id,
		String name,
		String description,
		Map<Integer, Long> rewards,
		boolean repeatable,
		List<AchievementCondition> conditions) {

	public Achievement {
		rewards = Map.copyOf(rewards);
		conditions = List.copyOf(conditions);
	}

	public boolean isMetBy(PlayerSnapshot player, AchievementCondition.Context context) {
		for (AchievementCondition condition : conditions) {
			if (!condition.test(player, context)) {
				return false;
			}
		}
		return true;
	}
}
