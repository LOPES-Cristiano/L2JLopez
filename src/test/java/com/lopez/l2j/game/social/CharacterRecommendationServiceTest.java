package com.lopez.l2j.game.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class CharacterRecommendationServiceTest {

	private CharacterRecommendationService service;
	private JdbcClient jdbc;

	@BeforeEach
	void setUp() {
		jdbc = mock(JdbcClient.class);
		var statementSpec = mock(JdbcClient.StatementSpec.class);
		when(jdbc.sql(anyString())).thenReturn(statementSpec);
		when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(statementSpec);
		when(statementSpec.update()).thenReturn(1);

		service = new CharacterRecommendationService(jdbc);
	}

	private PlayerCharacter createPlayer(int id, String name, int level) {
		PlayerCharacter c = new PlayerCharacter(id, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		return c;
	}

	@Test
	void testSuccessfulRecommendation() {
		var actor = createPlayer(1001, "Recommender", 25);
		actor.recomLeft(6);

		var target = createPlayer(1002, "Target", 15);
		target.recomHave(0);

		var res = service.evaluate(actor, target);
		assertTrue(res.success());
		assertEquals(5, actor.recomLeft());
		assertEquals(1, target.recomHave());
		assertTrue(actor.recommendedToday().contains(1002));

		// Second attempt same day should fail
		var res2 = service.evaluate(actor, target);
		assertFalse(res2.success());
		assertTrue(res2.message().contains("ja recomendou"));
	}

	@Test
	void testCannotRecommendSelf() {
		var actor = createPlayer(1001, "Selfie", 40);
		var res = service.evaluate(actor, actor);
		assertFalse(res.success());
		assertTrue(res.message().contains("si mesmo"));
	}

	@Test
	void testLevelRequirement() {
		var lowLevelActor = createPlayer(1001, "Newbie", 9);
		lowLevelActor.recomLeft(3);

		var target = createPlayer(1002, "Target", 15);
		var res = service.evaluate(lowLevelActor, target);
		assertFalse(res.success());
		assertTrue(res.message().contains("nivel 10"));
	}

	@Test
	void testNoRecomsLeft() {
		var actor = createPlayer(1001, "Exhausted", 30);
		actor.recomLeft(0);

		var target = createPlayer(1002, "Target", 15);
		var res = service.evaluate(actor, target);
		assertFalse(res.success());
		assertTrue(res.message().contains("nao possui mais"));
	}

	@Test
	void testTargetAtMaximum() {
		var actor = createPlayer(1001, "Actor", 50);
		actor.recomLeft(10);

		var target = createPlayer(1002, "Famous", 75);
		target.recomHave(255);

		var res = service.evaluate(actor, target);
		assertFalse(res.success());
		assertTrue(res.message().contains("maxima"));
	}

	@Test
	void testDailyResetAndDecay() {
		var player = createPlayer(1001, "Veteran", 55);
		player.recomHave(10);
		player.recomLeft(2);
		player.recommendedToday().add(2001);

		// Last reset was 2 days ago
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -2);
		player.lastRecomDate(cal.getTimeInMillis());

		service.checkDailyReset(player);

		// Level 55 gets 20 recomLeft and decays 3 recomHave
		assertEquals(20, player.recomLeft());
		assertEquals(7, player.recomHave());
		assertTrue(player.recommendedToday().isEmpty());
	}

	@Test
	void testAdminSetRec() {
		var player = createPlayer(1001, "Player", 40);
		service.adminSetRec(player, 120);
		assertEquals(120, player.recomHave());
	}
}
