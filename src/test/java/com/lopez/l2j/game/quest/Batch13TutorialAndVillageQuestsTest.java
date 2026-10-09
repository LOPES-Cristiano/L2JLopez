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

public class Batch13TutorialAndVillageQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestNovice", 15, 100_000L, 5_000, 0,
				0, 0, false, 0, 0, 0, 1000, 500, 1000, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 1000.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 13: Quest 201 - Human Fighter Tutorial")
	void testQuest201HfighterTutorial() {
		Quest201HfighterTutorial q = new Quest201HfighterTutorial(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("timer_newbie_helper", qs);
		assertNotNull(q.getName());
	}

	@Test
	@DisplayName("Lote 13: Quest 202 - Human Mage Tutorial")
	void testQuest202HmageTutorial() {
		Quest202HmageTutorial q = new Quest202HmageTutorial(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("timer_newbie_helper", qs);
		assertNotNull(q.getName());
	}

	@Test
	@DisplayName("Lote 13: Quest 257 - The Guard Is Busy")
	void testQuest257GuardIsBusy() {
		Quest257GuardIsBusy q = new Quest257GuardIsBusy(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("gilbert_q0257_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		NpcInstance mob = Mockito.mock(NpcInstance.class);
		when(mob.getNpcId()).thenReturn(20130);
		qs.giveItems(752, 5); // Orc Amulet
		assertEquals(5, qs.getQuestItemsCount(752));

		NpcInstance gilbert = Mockito.mock(NpcInstance.class);
		when(gilbert.getNpcId()).thenReturn(30039);
		String reply = q.onTalk(gilbert, qs);
		assertEquals("gilbert_q0257_07.htm", reply);
	}

	@Test
	@DisplayName("Lote 13: Quest 258 - Bring Wolf Pelts")
	void testQuest258BringWolfPelts() {
		Quest258BringWolfPelts q = new Quest258BringWolfPelts(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("lector_q0258_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		qs.giveItems(q.WOLF_PELT, 40);
		qs.setCond(2);
		NpcInstance lector = Mockito.mock(NpcInstance.class);
		when(lector.getNpcId()).thenReturn(30001);
		String reply = q.onTalk(lector, qs);
		assertEquals("lector_q0258_06.htm", reply);
	}

	@Test
	@DisplayName("Lote 13: Quest 259 - Rancher's Plea")
	void testQuest259RanchersPlea() {
		Quest259RanchersPlea q = new Quest259RanchersPlea(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 260 - Hunt The Orcs")
	void testQuest260HuntTheOrcs() {
		Quest260HuntTheOrcs q = new Quest260HuntTheOrcs(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("sentinel_rayjien_q0260_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 264 - Keen Claws")
	void testQuest264KeenClaws() {
		Quest264KeenClaws q = new Quest264KeenClaws(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("paint_q0264_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 266 - Plea Of Pixies")
	void testQuest266PleaOfPixies() {
		Quest266PleaOfPixies q = new Quest266PleaOfPixies(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("pixy_murika_q0266_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 271 - Proof Of Valor")
	void testQuest271ProofOfValor() {
		Quest271ProofOfValor q = new Quest271ProofOfValor(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("praetorian_rukain_q0271_03.htm", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 276 - Hestui Totem")
	void testQuest276HestuiTotem() {
		Quest276HestuiTotem q = new Quest276HestuiTotem(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 13: Quest 277 - Gatekeeper's Offering")
	void testQuest277GatekeepersOffering() {
		Quest277GatekeepersOffering q = new Quest277GatekeepersOffering(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("1", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}
}
