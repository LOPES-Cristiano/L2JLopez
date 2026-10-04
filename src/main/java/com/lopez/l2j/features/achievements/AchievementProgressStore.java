package com.lopez.l2j.features.achievements;

import java.util.Set;

/** Porta de persistencia do progresso; o servico nao sabe se e JDBC, JPA ou memoria. */
public interface AchievementProgressStore {

	Set<Integer> completedIds(int ownerId);

	/** Registra uma conclusao (incrementa o contador se ja existia). */
	void recordCompletion(int ownerId, int achievementId);
}
