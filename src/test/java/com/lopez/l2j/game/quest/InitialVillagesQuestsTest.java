package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class InitialVillagesQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "NewbiePlayer", 75, 50_000_000L, 2_000_000, 0,
				0, 0, false, 0, 0, 0, 4000, 2000, 2500, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 4000.0, 2000.0, 2500.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testQuest001LettersOfLove() {
		Quest001LettersOfLove q1 = new Quest001LettersOfLove(questManager);
		QuestState qs = new QuestState(q1, session);

		NpcInstance darin = Mockito.mock(NpcInstance.class);
		when(darin.getNpcId()).thenReturn(Quest001LettersOfLove.DARIN);

		NpcInstance roxxy = Mockito.mock(NpcInstance.class);
		when(roxxy.getNpcId()).thenReturn(Quest001LettersOfLove.ROXXY);

		NpcInstance baul = Mockito.mock(NpcInstance.class);
		when(baul.getNpcId()).thenReturn(Quest001LettersOfLove.BAUL);

		assertEquals("daring_q0001_02.htm", q1.onTalk(darin, qs));
		q1.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest001LettersOfLove.DARINS_LETTER));

		// Roxxy
		assertEquals("gatekeeper_roxis_q0001_01.htm", q1.onTalk(roxxy, qs));
		assertEquals(2, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest001LettersOfLove.ROXXYS_KERCHIEF));

		// Darin receipt
		assertEquals("daring_q0001_08.htm", q1.onTalk(darin, qs));
		assertEquals(3, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest001LettersOfLove.DARINS_RECEIPT));

		// Baul potion
		assertEquals("magister_baul_q0001_01.htm", q1.onTalk(baul, qs));
		assertEquals(4, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest001LettersOfLove.BAULS_POTION));

		// Finish
		assertEquals("daring_q0001_10.htm", q1.onTalk(darin, qs));
		assertTrue(qs.hasQuestItems(Quest001LettersOfLove.NECKLACE_OF_KNOWLEDGE));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest002WhatWomenWant() {
		Quest002WhatWomenWant q2 = new Quest002WhatWomenWant(questManager);
		QuestState qs = new QuestState(q2, session);

		NpcInstance arujien = Mockito.mock(NpcInstance.class);
		when(arujien.getNpcId()).thenReturn(Quest002WhatWomenWant.ARUJIEN);

		assertEquals("arujien_q0002_02.htm", q2.onTalk(arujien, qs));
		q2.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest002WhatWomenWant.ARUJIENS_LETTER1));
	}

	@Test
	void testQuest003WillTheSealBeBroken() {
		// Set race dark elf (2)
		PlayerCharacter de = new PlayerCharacter(
				1002, "acc1", "DarkElf", 20, 100_000L, 5000, 2,
				31, 31, false, 0, 0, 0, 1000, 500, 500, 100, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 500.0
		);
		when(session.activeChar()).thenReturn(de);

		Quest003WilltheSealbeBroken q3 = new Quest003WilltheSealbeBroken(questManager);
		QuestState qs = new QuestState(q3, session);

		NpcInstance talloth = Mockito.mock(NpcInstance.class);
		when(talloth.getNpcId()).thenReturn(Quest003WilltheSealbeBroken.TALLOTH);

		assertEquals("tewndrowell_q0003_02.htm", q3.onTalk(talloth, qs));
		q3.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(Quest003WilltheSealbeBroken.ONYX_BEAST_EYE, 1);
		qs.giveItems(Quest003WilltheSealbeBroken.TAINT_STONE, 1);
		qs.giveItems(Quest003WilltheSealbeBroken.SUCCUBUS_BLOOD, 1);
		qs.setCond(2);

		assertEquals("tewndrowell_q0003_06.htm", q3.onTalk(talloth, qs));
		assertTrue(qs.hasQuestItems(Quest003WilltheSealbeBroken.ENCHANT_ARMOR_D));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest006StepIntoTheFuture() {
		Quest006StepIntoTheFuture q6 = new Quest006StepIntoTheFuture(questManager);
		QuestState qs = new QuestState(q6, session);

		NpcInstance roxxy = Mockito.mock(NpcInstance.class);
		when(roxxy.getNpcId()).thenReturn(Quest006StepIntoTheFuture.ROXXY);

		assertEquals("gatekeeper_roxis_q0006_02.htm", q6.onTalk(roxxy, qs));
		q6.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest006StepIntoTheFuture.ROXXYS_LETTER));
	}

	@Test
	void testQuest011SecretMeetingWithKetraOrcs() {
		Quest011SecretMeetingWithKetraOrcs q11 = new Quest011SecretMeetingWithKetraOrcs(questManager);
		QuestState qs = new QuestState(q11, session);

		NpcInstance cadmon = Mockito.mock(NpcInstance.class);
		when(cadmon.getNpcId()).thenReturn(Quest011SecretMeetingWithKetraOrcs.CADMON);

		assertEquals("guard_cadmon_q0011_0101.htm", q11.onTalk(cadmon, qs));
		q11.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		NpcInstance leon = Mockito.mock(NpcInstance.class);
		when(leon.getNpcId()).thenReturn(Quest011SecretMeetingWithKetraOrcs.LEON);

		assertEquals("trader_leon_q0011_0101.htm", q11.onTalk(leon, qs));
		assertEquals(2, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest011SecretMeetingWithKetraOrcs.MUNITIONS_BOX));

		NpcInstance wahkan = Mockito.mock(NpcInstance.class);
		when(wahkan.getNpcId()).thenReturn(Quest011SecretMeetingWithKetraOrcs.WAHKAN);

		assertEquals("herald_wahkan_q0011_0101.htm", q11.onTalk(wahkan, qs));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest013ParcelDelivery() {
		Quest013ParcelDelivery q13 = new Quest013ParcelDelivery(questManager);
		QuestState qs = new QuestState(q13, session);

		NpcInstance fundin = Mockito.mock(NpcInstance.class);
		when(fundin.getNpcId()).thenReturn(Quest013ParcelDelivery.FUNDIN);

		assertEquals("mineral_trader_fundin_q0013_0101.htm", q13.onTalk(fundin, qs));
		q13.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest013ParcelDelivery.PACKAGE));

		NpcInstance vulcan = Mockito.mock(NpcInstance.class);
		when(vulcan.getNpcId()).thenReturn(Quest013ParcelDelivery.VULCAN);

		assertEquals("blacksmith_vulcan_q0013_0101.htm", q13.onTalk(vulcan, qs));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest014WhereaboutsoftheArchaeologist() {
		Quest014WhereaboutsoftheArchaeologist q14 = new Quest014WhereaboutsoftheArchaeologist(questManager);
		QuestState qs = new QuestState(q14, session);

		NpcInstance ghost = Mockito.mock(NpcInstance.class);
		when(ghost.getNpcId()).thenReturn(Quest014WhereaboutsoftheArchaeologist.GHOST);

		assertEquals("ghost_of_adventurer_q0014_0101.htm", q14.onTalk(ghost, qs));
		q14.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest014WhereaboutsoftheArchaeologist.LETTER));

		NpcInstance liesel = Mockito.mock(NpcInstance.class);
		when(liesel.getNpcId()).thenReturn(Quest014WhereaboutsoftheArchaeologist.LIESEL);

		assertEquals("explorer_liesel_q0014_0101.htm", q14.onTalk(liesel, qs));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest015SweetWhispers() {
		Quest015SweetWhispers q15 = new Quest015SweetWhispers(questManager);
		QuestState qs = new QuestState(q15, session);

		NpcInstance vladimir = Mockito.mock(NpcInstance.class);
		when(vladimir.getNpcId()).thenReturn(Quest015SweetWhispers.VLADIMIR);

		assertEquals("trader_vladimir_q0015_0101.htm", q15.onTalk(vladimir, qs));
		q15.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		NpcInstance hierarch = Mockito.mock(NpcInstance.class);
		when(hierarch.getNpcId()).thenReturn(Quest015SweetWhispers.HIERARCH);

		assertEquals("hierarch_q0015_0101.htm", q15.onTalk(hierarch, qs));
		assertEquals(2, qs.getCond());

		NpcInstance mystic = Mockito.mock(NpcInstance.class);
		when(mystic.getNpcId()).thenReturn(Quest015SweetWhispers.MYSTIC);

		assertEquals("dark_mystic_q0015_0101.htm", q15.onTalk(mystic, qs));
		assertTrue(qs.isCompleted());
	}
}
