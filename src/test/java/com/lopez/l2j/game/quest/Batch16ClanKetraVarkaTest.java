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

public class Batch16ClanKetraVarkaTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestClanLeader", 75, 50_000_000L, 5_000_000, 0,
				0, 0, false, 0, 0, 0, 5000, 2000, 5000, 2000, 10, 5, 1, "", 0,
				0L, 0L, 0, 0, 0, 0, 5000.0, 2000.0, 5000.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 16: Quest 431 - Wedding March")
	void testQuest431WeddingMarch() {
		Quest431WeddingMarch q = new Quest431WeddingMarch(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 432 - Birthday Party Song")
	void testQuest432BirthdayPartySong() {
		Quest432BirthdayPartySong q = new Quest432BirthdayPartySong(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 504 - Competition For The Bandit Stronghold")
	void testQuest504BanditStronghold() {
		Quest504CompetitionForTheBanditStronghold q = new Quest504CompetitionForTheBanditStronghold(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 508 - The Clan's Reputation")
	void testQuest508TheClansReputation() {
		Quest508TheClansReputation q = new Quest508TheClansReputation(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 509 - The Clan's Prestige")
	void testQuest509TheClansPrestige() {
		Quest509TheClansPrestige q = new Quest509TheClansPrestige(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 510 - A Clan's Reputation")
	void testQuest510AClansReputation() {
		Quest510AClansReputation q = new Quest510AClansReputation(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 606 - War with Varka Silenos")
	void testQuest606WarWithVarka() {
		Quest606WarwithVarkaSilenos q = new Quest606WarwithVarkaSilenos(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 612 - War with Ketra Orcs")
	void testQuest612WarWithKetra() {
		Quest612WarwithKetraOrcs q = new Quest612WarwithKetraOrcs(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 635 - In The Dimensional Rift")
	void testQuest635DimensionalRift() {
		Quest635InTheDimensionalRift q = new Quest635InTheDimensionalRift(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 16: Quest 640 - The Zero Hour")
	void testQuest640TheZeroHour() {
		Quest640TheZeroHour q = new Quest640TheZeroHour(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}
}
