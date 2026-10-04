package com.lopez.l2j.features.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JdbcAchievementProgressStoreTest {

	@Autowired
	AchievementProgressStore store;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void schema() {
		// Flyway fica desligado no H2 (DDL do MariaDB); recriamos so a tabela deste teste.
		jdbc.sql("DROP TABLE IF EXISTS player_achievement").update();
		jdbc.sql("""
				CREATE TABLE player_achievement (
				  owner_id INT NOT NULL, achievement_id INT NOT NULL,
				  times_completed INT NOT NULL DEFAULT 1,
				  last_completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (owner_id, achievement_id))
				""").update();
	}

	@Test
	void recordsAndReadsBack() {
		assertTrue(store.completedIds(5).isEmpty());
		store.recordCompletion(5, 1);
		store.recordCompletion(5, 3);
		assertEquals(java.util.Set.of(1, 3), store.completedIds(5));
		assertTrue(store.completedIds(6).isEmpty());
	}

	@Test
	void repeatedCompletionIncrementsCounterWithoutDuplicateRow() {
		store.recordCompletion(5, 1);
		store.recordCompletion(5, 1);
		int times = jdbc.sql("SELECT times_completed FROM player_achievement WHERE owner_id=5 AND achievement_id=1")
				.query(Integer.class).single();
		assertEquals(2, times);
	}
}
