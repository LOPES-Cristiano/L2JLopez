package com.lopez.l2j.game.olympiad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OlympiadManagerTest {

	private OlympiadManager manager;

	@BeforeEach
	void setUp() {
		manager = new OlympiadManager(null);
	}

	@Test
	void nobleRegistrationAndInitialPoints() {
		var noble = manager.registerNoble(1001, "GladiatorKing", 88);
		assertThat(noble.charId()).isEqualTo(1001);
		assertThat(noble.charName()).isEqualTo("GladiatorKing");
		assertThat(noble.classId()).isEqualTo(88);
		assertThat(noble.points()).isEqualTo(OlympiadNoble.DEFAULT_POINTS);
		assertThat(noble.competitionsDone()).isEqualTo(0);

		assertThat(manager.isNoble(1001)).isTrue();
		assertThat(manager.isNoble(9999)).isFalse();
	}

	@Test
	void recordsMatchWinLossAndDraw() {
		var p1 = manager.registerNoble(1001, "PlayerOne", 88);
		var p2 = manager.registerNoble(1002, "PlayerTwo", 89);

		// Match 1: PlayerOne wins
		manager.recordMatch(1001, 1002, false);
		assertThat(p1.competitionsDone()).isEqualTo(1);
		assertThat(p1.competitionsWon()).isEqualTo(1);
		assertThat(p1.points()).isGreaterThan(OlympiadNoble.DEFAULT_POINTS);

		assertThat(p2.competitionsDone()).isEqualTo(1);
		assertThat(p2.competitionsLost()).isEqualTo(1);
		assertThat(p2.points()).isLessThan(OlympiadNoble.DEFAULT_POINTS);

		// Match 2: Draw
		int p1PtsBefore = p1.points();
		int p2PtsBefore = p2.points();
		manager.recordMatch(1001, 1002, true);
		assertThat(p1.competitionsDone()).isEqualTo(2);
		assertThat(p1.competitionsDrawn()).isEqualTo(1);
		assertThat(p1.points()).isEqualTo(p1PtsBefore);

		assertThat(p2.competitionsDone()).isEqualTo(2);
		assertThat(p2.competitionsDrawn()).isEqualTo(1);
		assertThat(p2.points()).isEqualTo(p2PtsBefore);
	}

	@Test
	void computesHeroesByClass() {
		// Duelist (class 88)
		var d1 = manager.registerNoble(101, "DuelistAlpha", 88);
		var d2 = manager.registerNoble(102, "DuelistBeta", 88);

		for (int i = 0; i < 5; i++) {
			manager.recordMatch(101, 102, false);
		}

		var heroes = manager.computeHeroes();
		assertThat(heroes).containsKey(88);
		assertThat(heroes.get(88).charId()).isEqualTo(101);
		assertThat(heroes.get(88).charName()).isEqualTo("DuelistAlpha");
	}

	@Test
	void resetsWeeklyPoints() {
		var noble = manager.registerNoble(2001, "Tanker", 90);
		noble.points(10);

		manager.resetWeeklyPoints();
		assertThat(noble.points()).isEqualTo(10 + OlympiadNoble.DEFAULT_POINTS);
	}
}
