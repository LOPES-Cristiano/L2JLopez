package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class Batch15AdenPavelGoddardTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestAdenHero", 55, 5_000_000L, 500_000, 0,
				0, 0, false, 0, 0, 0, 3000, 1500, 3000, 1500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 3000.0, 1500.0, 3000.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 15: Quest 338 - Alligator Hunter")
	void testQuest338AlligatorHunter() {
		Quest338AlligatorHunter q = new Quest338AlligatorHunter(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 340 - Subjugation of Lizardmen")
	void testQuest340SubjugationofLizardmen() {
		Quest340SubjugationofLizardmen q = new Quest340SubjugationofLizardmen(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 343 - Under the Shadow of the Ivory Tower")
	void testQuest343UndertheShadowoftheIvoryTower() {
		Quest343UndertheShadowoftheIvoryTower q = new Quest343UndertheShadowoftheIvoryTower(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 344 - 1000 Years End of Lamentation")
	void testQuest3441000YearsEndofLamentation() {
		Quest3441000YearsEndofLamentation q = new Quest3441000YearsEndofLamentation(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 351 - Black Swan")
	void testQuest351BlackSwan() {
		Quest351BlackSwan q = new Quest351BlackSwan(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 356 - Dig Up The Sea Of Spores")
	void testQuest356DigUpTheSeaOfSpores() {
		Quest356DigUpTheSeaOfSpores q = new Quest356DigUpTheSeaOfSpores(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 365 - Devils Legacy")
	void testQuest365DevilsLegacy() {
		Quest365DevilsLegacy q = new Quest365DevilsLegacy(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 372 - Legacy Of Insolence")
	void testQuest372LegacyOfInsolence() {
		Quest372LegacyOfInsolence q = new Quest372LegacyOfInsolence(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 376 - Exploration Of Giants Cave Part 1")
	void testQuest376ExplorationOfGiantsCavePart1() {
		Quest376ExplorationOfGiantsCavePart1 q = new Quest376ExplorationOfGiantsCavePart1(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 380 - Bring Out The Flavor Of Ingredients")
	void testQuest380BringOutTheFlavorOfIngredients() {
		Quest380BringOutTheFlavorOfIngredients q = new Quest380BringOutTheFlavorOfIngredients(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 384 - Warehouse Keepers Pastime (Bingo)")
	void testQuest384WarehouseKeepersPastime() {
		Quest384WarehouseKeepersPastime q = new Quest384WarehouseKeepersPastime(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("30182-05.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		Bingo bingo = new Bingo("<a href=\"%n%\">%n%</a>");
		assertNotNull(bingo.getDialog(""));
		bingo.Select(1);
		bingo.Select(2);
		bingo.Select(3);
		bingo.Select(4);
		bingo.Select(5);
		String finalDialog = bingo.Select(6);
		assertNotNull(finalDialog);
		assertTrue(bingo.lines >= 0);
	}

	@Test
	@DisplayName("Lote 15: Quest 385 - Yoke Of The Past")
	void testQuest385YokeOfThePast() {
		Quest385YokeOfThePast q = new Quest385YokeOfThePast(questManager);
		QuestState qs = new QuestState(q, session);

		assertTrue(q.checkNPC(31095));
		assertFalse(q.checkNPC(31111));

		q.onEvent("enter_necropolis1_q0385_05.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 15: Quest 386 - Stolen Dignity")
	void testQuest386StolenDignity() {
		Quest386StolenDignity q = new Quest386StolenDignity(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}
}
