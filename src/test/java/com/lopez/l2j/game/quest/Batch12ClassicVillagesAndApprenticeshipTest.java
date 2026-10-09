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

public class Batch12ClassicVillagesAndApprenticeshipTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestApprentice", 25, 500_000L, 20_000, 0,
				0, 0, false, 0, 0, 0, 2000, 1000, 1500, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 2000.0, 1000.0, 1500.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 12: Quest 151 - Cure for Fever Disease")
	void testQuest151CureforFeverDisease() {
		Quest151CureforFeverDisease q = new Quest151CureforFeverDisease(questManager);
		QuestState qs = new QuestState(q, session);

		NpcInstance elias = Mockito.mock(NpcInstance.class);
		when(elias.getNpcId()).thenReturn(Quest151CureforFeverDisease.ELIAS);

		String talk = q.onTalk(elias, qs);
		assertEquals("30050-02.htm", talk);

		q.onEvent("30050-03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		NpcInstance mob = Mockito.mock(NpcInstance.class);
		when(mob.getNpcId()).thenReturn(20103);
		// Force drop by giving item
		qs.giveItems(Quest151CureforFeverDisease.POISON_SAC, 1);
		qs.setCond(2);

		NpcInstance yohanes = Mockito.mock(NpcInstance.class);
		when(yohanes.getNpcId()).thenReturn(Quest151CureforFeverDisease.YOHANES);
		assertEquals("30032-01.htm", q.onTalk(yohanes, qs));
		assertEquals(3, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest151CureforFeverDisease.FEVER_MEDICINE));

		assertEquals("30050-06.htm", q.onTalk(elias, qs));
		assertTrue(qs.isCompleted());
		assertTrue(qs.hasQuestItems(Quest151CureforFeverDisease.ROUND_SHIELD));
	}

	@Test
	@DisplayName("Lote 12: Quest 152 - Shards of Golem")
	void testQuest152ShardsOfGolem() {
		Quest152ShardsOfGolem q = new Quest152ShardsOfGolem(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 156 - Millennium Love")
	void testQuest156MillenniumLove() {
		Quest156MillenniumLove q = new Quest156MillenniumLove(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 164 - Blood Fiend")
	void testQuest164BloodFiend() {
		Quest164BloodFiend q = new Quest164BloodFiend(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 170 - Dangerous Seduction")
	void testQuest170DangerousSeduction() {
		Quest170DangerousSeduction q = new Quest170DangerousSeduction(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 127 - Kamael A Window To The Future")
	void testQuest127KamaelAWindowToTheFuture() {
		Quest127KamaelAWindowToTheFuture q = new Quest127KamaelAWindowToTheFuture(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 118 - To Lead And Be Led")
	void testQuest118ToLeadAndBeLed() {
		Quest118ToLeadAndBeLed q = new Quest118ToLeadAndBeLed(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 123 - The Leader And The Follower")
	void testQuest123TheLeaderAndTheFollower() {
		Quest123TheLeaderAndTheFollower q = new Quest123TheLeaderAndTheFollower(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 153 - Deliver Goods")
	void testQuest153DeliverGoods() {
		Quest153DeliverGoods q = new Quest153DeliverGoods(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 160 - Nerupas Favor")
	void testQuest160NerupasFavor() {
		Quest160NerupasFavor q = new Quest160NerupasFavor(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 12: Quest 171 - Acts Of Evil")
	void testQuest171ActsOfEvil() {
		Quest171ActsOfEvil q = new Quest171ActsOfEvil(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}
}

