package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.saga.Quest070SagaOfThePhoenixKnight;
import com.lopez.l2j.game.quest.saga.Quest073SagaOfTheDuelist;
import com.lopez.l2j.game.quest.saga.Quest094SagaOfTheSoultaker;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class SagasCompleteQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "HeroPlayer", 76, 50_000_000L, 1_000_000, 0,
				2, 2, false, 0, 0, 0, 3000, 1500, 2000, 0, 10, 0, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 3000.0, 1500.0, 2000.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testSagaOfTheDuelistRegistrationAndFlow() {
		Quest073SagaOfTheDuelist q73 = new Quest073SagaOfTheDuelist(questManager);
		assertEquals(73, q73.getQuestId());
		assertEquals(88, q73.getTargetClassId()); // Duelist
		assertEquals(2, q73.getRequiredPrevClass()); // Gladiator

		QuestState qs = new QuestState(q73, session);

		// Npc 0: Sedrick (30849)
		NpcInstance sedrick = Mockito.mock(NpcInstance.class);
		when(sedrick.getNpcId()).thenReturn(30849);

		// onTalk at cond 0
		String html = q73.onTalk(sedrick, qs);
		assertEquals("0-01.htm", html);

		// Accept quest
		String acceptHtml = q73.onEvent("accept", qs);
		assertEquals("0-03.htm", acceptHtml);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(7096)); // Starter item

		// Progress to cond 3
		q73.onEvent("1-3", qs);
		assertEquals(3, qs.getCond());

		// Cond 6: Guardian Angels kill count test
		qs.setCond(6);
		NpcInstance angel = Mockito.mock(NpcInstance.class);
		when(angel.getNpcId()).thenReturn(27214);
		for (int i = 0; i < 9; i++) {
			q73.onKill(angel, qs, false);
			assertEquals(6, qs.getCond());
		}
		// 10th kill gives Resonance Amulet 2 and sets cond 7
		q73.onKill(angel, qs, false);
		assertEquals(7, qs.getCond());
		assertTrue(qs.hasQuestItems(7302));

		// Cond 15: Halisha marks and Archon kill
		qs.setCond(15);
		NpcInstance minion = Mockito.mock(NpcInstance.class);
		when(minion.getNpcId()).thenReturn(21646);
		q73.onKill(minion, qs, false);
		assertEquals(1, qs.getQuestItemsCount(7488)); // Halisha's Mark count

		// Kill Archon of Halisha -> Resonance Amulet 5, cond 16
		NpcInstance halisha = Mockito.mock(NpcInstance.class);
		when(halisha.getNpcId()).thenReturn(27222);
		q73.onKill(halisha, qs, false);
		assertEquals(16, qs.getCond());
		assertTrue(qs.hasQuestItems(7395)); // Amulet 5

		// Complete quest at cond 20 with Sedrick
		qs.setCond(20);
		String completeHtml = q73.onTalk(sedrick, qs);
		assertEquals("0-09.htm", completeHtml);

		// Class transfer completed: classId is now 88 (Duelist)!
		assertEquals(88, pc.classId());
		assertEquals(88, pc.baseClassId());
		assertTrue(qs.isCompleted());
		assertEquals(5_000_000L, qs.getQuestItemsCount(57)); // 5kk adena
		assertEquals(1, qs.getQuestItemsCount(6622)); // Book of Giants
	}

	@Test
	void testSagaOfThePhoenixKnightRequirements() {
		Quest070SagaOfThePhoenixKnight q70 = new Quest070SagaOfThePhoenixKnight(questManager);
		assertEquals(70, q70.getQuestId());
		assertEquals(90, q70.getTargetClassId()); // Phoenix Knight
		assertEquals(5, q70.getRequiredPrevClass()); // Paladin

		// Player with Gladiator class cannot talk to start Paladin saga
		QuestState qs = new QuestState(q70, session);
		NpcInstance npc = Mockito.mock(NpcInstance.class);
		when(npc.getNpcId()).thenReturn(30849);
		assertEquals("noquest", q70.onTalk(npc, qs));

		// Set player class to Paladin (5)
		pc.classId(5);
		assertEquals("0-01.htm", q70.onTalk(npc, qs));
	}

	@Test
	void testSagaOfTheSoultakerRequirements() {
		Quest094SagaOfTheSoultaker q94 = new Quest094SagaOfTheSoultaker(questManager);
		assertEquals(94, q94.getQuestId());
		assertEquals(95, q94.getTargetClassId()); // Soultaker
		assertEquals(13, q94.getRequiredPrevClass()); // Necromancer
	}
}
