package com.lopez.l2j.features.achievements;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tabela normalizada player_achievement (V123), no lugar da antiga achievements com uma coluna
 * aN por conquista e ALTER TABLE em tempo de execucao.
 */
@Repository
@ConditionalOnProperty(prefix = "l2.features.achievements", name = "enabled", havingValue = "true")
class JdbcAchievementProgressStore implements AchievementProgressStore {

	private final JdbcClient jdbc;

	JdbcAchievementProgressStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Set<Integer> completedIds(int ownerId) {
		return new HashSet<>(jdbc.sql("SELECT achievement_id FROM player_achievement WHERE owner_id = :owner")
				.param("owner", ownerId)
				.query(Integer.class)
				.list());
	}

	@Override
	public void recordCompletion(int ownerId, int achievementId) {
		// UPDATE-then-INSERT: portavel entre MariaDB e H2 (sem ON DUPLICATE KEY).
		int updated = jdbc.sql("""
				UPDATE player_achievement
				   SET times_completed = times_completed + 1, last_completed_at = CURRENT_TIMESTAMP
				 WHERE owner_id = :owner AND achievement_id = :ach
				""")
				.param("owner", ownerId)
				.param("ach", achievementId)
				.update();
		if (updated == 0) {
			jdbc.sql("INSERT INTO player_achievement (owner_id, achievement_id, times_completed) VALUES (:owner, :ach, 1)")
					.param("owner", ownerId)
					.param("ach", achievementId)
					.update();
		}
	}
}
