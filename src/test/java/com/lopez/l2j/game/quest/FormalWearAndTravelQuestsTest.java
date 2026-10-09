package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class FormalWearAndTravelQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "FashionHero", 65, 30_000_000L, 1_000_000, 0,
				0, 0, false, 0, 0, 0, 4000, 2000, 2500, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 4000.0, 2000.0, 2500.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testQuest037FormalWearChain() {
		Quest037PleaseMakeMeFormalWear q37 = new Quest037PleaseMakeMeFormalWear(questManager);
		QuestState qs = new QuestState(q37, session);

		NpcInstance alexis = Mockito.mock(NpcInstance.class);
		when(alexis.getNpcId()).thenReturn(Quest037PleaseMakeMeFormalWear.START_NPC);

		assertEquals("30842-0.htm", q37.onTalk(alexis, qs));
		q37.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		// Give all 4 required components from previous quests:
		// 7113 Dress Shoes Box, 7078 Sewing Kit, 7076 Mysterious Cloth, 7077 Jewel Box
		qs.giveItems(7113, 1);
		qs.giveItems(7078, 1);
		qs.giveItems(7076, 1);
		qs.giveItems(7077, 1);

		assertEquals("30842-3.htm", q37.onTalk(alexis, qs));
		assertTrue(qs.hasQuestItems(6408)); // Formal Wear
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest032AnObviousLie() {
		Quest032AnObviousLie q32 = new Quest032AnObviousLie(questManager);
		QuestState qs = new QuestState(q32, session);

		NpcInstance max = Mockito.mock(NpcInstance.class);
		when(max.getNpcId()).thenReturn(Quest032AnObviousLie.START_NPC);

		assertEquals("maximilian_q0032_0101.htm", q32.onTalk(max, qs));
		q32.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		NpcInstance gentler = Mockito.mock(NpcInstance.class);
		when(gentler.getNpcId()).thenReturn(30094);

		assertEquals("gentler_q0032_0201.htm", q32.onTalk(gentler, qs));
		assertEquals(2, qs.getCond());

		assertEquals("gentler_q0032_0401.htm", q32.onTalk(gentler, qs));
		assertTrue(qs.hasQuestItems(6843)); // Cat Ears
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest027ChestCaughtWithABaitOfWind() {
		Quest027ChestCaughtWithABaitOfWind q27 = new Quest027ChestCaughtWithABaitOfWind(questManager);
		QuestState qs = new QuestState(q27, session);

		NpcInstance lanosco = Mockito.mock(NpcInstance.class);
		when(lanosco.getNpcId()).thenReturn(Quest027ChestCaughtWithABaitOfWind.START_NPC);

		assertEquals("fisher_lanosco_q0027_0101.htm", q27.onTalk(lanosco, qs));
		q27.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(6500, 1); // Strange Box
		assertEquals("fisher_lanosco_q0027_0201.htm", q27.onTalk(lanosco, qs));
		assertEquals(2, qs.getCond());
		assertTrue(qs.hasQuestItems(7609)); // Blue Gem

		NpcInstance shaling = Mockito.mock(NpcInstance.class);
		when(shaling.getNpcId()).thenReturn(31434);

		assertEquals("blueprint_seller_shaling_q0027_0301.htm", q27.onTalk(shaling, qs));
		assertTrue(qs.hasQuestItems(884));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest042HelpTheUncle() {
		Quest042HelpTheUncle q42 = new Quest042HelpTheUncle(questManager);
		QuestState qs = new QuestState(q42, session);

		NpcInstance waters = Mockito.mock(NpcInstance.class);
		when(waters.getNpcId()).thenReturn(Quest042HelpTheUncle.START_NPC);

		assertEquals("pet_manager_waters_q0042_0101.htm", q42.onTalk(waters, qs));
		q42.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		assertEquals("pet_manager_waters_q0042_0301.htm", q42.onTalk(waters, qs));
		assertTrue(qs.hasQuestItems(3438)); // Pet Collar
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest045ToTalkingIsland() {
		Quest045ToTalkingIsland q45 = new Quest045ToTalkingIsland(questManager);
		QuestState qs = new QuestState(q45, session);

		NpcInstance galladuchi = Mockito.mock(NpcInstance.class);
		when(galladuchi.getNpcId()).thenReturn(Quest045ToTalkingIsland.START_NPC);

		assertEquals("galladuchi_q0045_0101.htm", q45.onTalk(galladuchi, qs));
		q45.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		assertEquals("galladuchi_q0045_0301.htm", q45.onTalk(galladuchi, qs));
		assertTrue(qs.hasQuestItems(7554)); // Scroll of Escape: Talking Island
		assertTrue(qs.isCompleted());
	}
}
