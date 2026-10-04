package com.lopez.l2j.features.achievements;

import java.util.Set;

/** Regra unica de uma conquista; todas as regras de uma conquista precisam passar. */
@FunctionalInterface
public interface AchievementCondition {

	boolean test(PlayerSnapshot player, Context context);

	/** Estado extra necessario para avaliar (ex.: quantas conquistas ja foram feitas). */
	record Context(Set<Integer> completedIds) {
		public Context {
			completedIds = Set.copyOf(completedIds);
		}

		public int completedCount() {
			return completedIds.size();
		}
	}
}
