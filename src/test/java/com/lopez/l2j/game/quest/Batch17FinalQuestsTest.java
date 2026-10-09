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

public class Batch17FinalQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestFinalHero", 78, 100_000_000L, 10_000_000, 0,
				0, 0, false, 0, 0, 0, 6000, 3000, 6000, 3000, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 6000.0, 3000.0, 6000.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 17: Quest 644 - Grave Robber Annihilation")
	void testQuest644GraveRobberAnnihilation() {
		Quest644GraveRobberAnnihilation q = new Quest644GraveRobberAnnihilation(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 645 - Ghosts Of Batur")
	void testQuest645GhostsOfBatur() {
		Quest645GhostsOfBatur q = new Quest645GhostsOfBatur(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 646 - Signs Of Revolt")
	void testQuest646SignsOfRevolt() {
		Quest646SignsOfRevolt q = new Quest646SignsOfRevolt(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 648 - An Ice Merchants Dream")
	void testQuest648AnIceMerchantsDream() {
		Quest648AnIceMerchantsDream q = new Quest648AnIceMerchantsDream(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 650 - A Broken Dream")
	void testQuest650ABrokenDream() {
		Quest650ABrokenDream q = new Quest650ABrokenDream(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 653 - Wild Maiden")
	void testQuest653WildMaiden() {
		Quest653WildMaiden q = new Quest653WildMaiden(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 654 - Journey to a Settlement")
	void testQuest654JourneytoaSettlement() {
		Quest654JourneytoaSettlement q = new Quest654JourneytoaSettlement(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 662 - A Game Of Cards")
	void testQuest662AGameOfCards() {
		Quest662AGameOfCards q = new Quest662AGameOfCards(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 663 - Seductive Whispers")
	void testQuest663SeductiveWhispers() {
		Quest663SeductiveWhispers q = new Quest663SeductiveWhispers(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 688 - Defeat The Elrokian Raiders")
	void testQuest688DefeatTheElrokianRaiders() {
		Quest688DefeatTheElrokianRaiders q = new Quest688DefeatTheElrokianRaiders(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 17: Quest 1103 - Oracle Teleport")
	void testQuest1103OracleTeleport() {
		Quest1103OracleTeleport q = new Quest1103OracleTeleport(questManager);
		QuestState qs = new QuestState(q, session);

		assertNotNull(q);
		assertEquals(1103, q.getQuestId());
	}
}
