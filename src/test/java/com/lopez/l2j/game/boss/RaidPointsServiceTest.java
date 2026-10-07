package com.lopez.l2j.game.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RaidPointsServiceTest {

	private RaidPointsService service;

	@BeforeEach
	void setUp() {
		service = new RaidPointsService(null);
	}

	@Test
	void emptyServiceReturnsZeroAndEmptyMap() {
		assertEquals(0, service.getPointsByOwnerId(1001));
		assertEquals(0, service.calculateRanking(1001));
		assertTrue(service.getList(1001).isEmpty());
	}

	@Test
	void addPointsCalculatesTotalAndRankingsCorrectly() {
		int player1 = 1001;
		int player2 = 1002;
		int player3 = 1003;

		service.addPoints(player1, 25001, 50);
		service.addPoints(player1, 25004, 30); // player1 total = 80

		service.addPoints(player2, 25001, 100); // player2 total = 100

		service.addPoints(player3, 25010, 20); // player3 total = 20

		assertEquals(80, service.getPointsByOwnerId(player1));
		assertEquals(100, service.getPointsByOwnerId(player2));
		assertEquals(20, service.getPointsByOwnerId(player3));

		// player2 tem 100 -> rank 1
		assertEquals(1, service.calculateRanking(player2));
		// player1 tem 80 -> rank 2
		assertEquals(2, service.calculateRanking(player1));
		// player3 tem 20 -> rank 3
		assertEquals(3, service.calculateRanking(player3));

		Map<Integer, Integer> p1List = service.getList(player1);
		assertEquals(2, p1List.size());
		assertEquals(50, p1List.get(25001));
		assertEquals(30, p1List.get(25004));
	}
}
