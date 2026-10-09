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

public class StarterWeaponsAndPavelStorylineTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "StarterHero", 78, 50_000_000L, 2_000_000, 0,
				0, 0, false, 0, 0, 0, 4000, 2000, 2500, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 4000.0, 2000.0, 2500.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testQuest101SwordOfSolidarity() {
		Quest101SwordOfSolidarity q101 = new Quest101SwordOfSolidarity(questManager);
		QuestState qs = new QuestState(q101, session);

		NpcInstance roien = Mockito.mock(NpcInstance.class);
		when(roien.getNpcId()).thenReturn(Quest101SwordOfSolidarity.START_NPC);

		assertEquals("roien_q0101_02.htm", q101.onTalk(roien, qs));
		q101.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(796));

		NpcInstance alltran = Mockito.mock(NpcInstance.class);
		when(alltran.getNpcId()).thenReturn(30283);

		assertEquals("blacksmith_alltran_q0101_02.htm", q101.onTalk(alltran, qs));
		assertEquals(2, qs.getCond());

		qs.setCond(3);
		assertEquals("blacksmith_alltran_q0101_05.htm", q101.onTalk(alltran, qs));
		assertEquals(4, qs.getCond());
		assertTrue(qs.hasQuestItems(798));

		assertEquals("roien_q0101_07.htm", q101.onTalk(roien, qs));
		assertTrue(qs.hasQuestItems(738)); // Sword of Solidarity
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest050LanoscosSpecialBait() {
		Quest050LanoscosSpecialBait q50 = new Quest050LanoscosSpecialBait(questManager);
		QuestState qs = new QuestState(q50, session);

		NpcInstance lanosco = Mockito.mock(NpcInstance.class);
		when(lanosco.getNpcId()).thenReturn(Quest050LanoscosSpecialBait.START_NPC);

		assertEquals("fisher_lanosco_q0050_0101.htm", q50.onTalk(lanosco, qs));
		q50.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(7610, 4);
		assertEquals("fisher_lanosco_q0050_0202.htm", q50.onTalk(lanosco, qs));
		assertTrue(qs.hasQuestItems(7614)); // Wind Bait
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest110ToThePrimevalIsle() {
		Quest110ToThePrimevalIsle q110 = new Quest110ToThePrimevalIsle(questManager);
		QuestState qs = new QuestState(q110, session);

		NpcInstance anton = Mockito.mock(NpcInstance.class);
		when(anton.getNpcId()).thenReturn(Quest110ToThePrimevalIsle.START_NPC);

		assertEquals("scroll_seller_anton_q0110_0101.htm", q110.onTalk(anton, qs));
		q110.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(8777)); // Marquez's Letter

		NpcInstance marquez = Mockito.mock(NpcInstance.class);
		when(marquez.getNpcId()).thenReturn(32113);

		assertEquals("marquez_q0110_0201.htm", q110.onTalk(marquez, qs));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest119LastImperialPrince() {
		Quest119LastImperialPrince q119 = new Quest119LastImperialPrince(questManager);
		QuestState qs = new QuestState(q119, session);

		NpcInstance spirit = Mockito.mock(NpcInstance.class);
		when(spirit.getNpcId()).thenReturn(Quest119LastImperialPrince.START_NPC);

		String reject = q119.onTalk(spirit, qs);
		assertTrue(reject.contains("Four Goblets") || reject.contains("not accomplished"));

		qs.giveItems(Quest119LastImperialPrince.ANTIQUE_BROOCH, 1);
		assertEquals("31453-1.htm", q119.onTalk(spirit, qs));

		q119.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		NpcInstance devorin = Mockito.mock(NpcInstance.class);
		when(devorin.getNpcId()).thenReturn(32009);

		assertEquals("devorin_q0119_0201.htm", q119.onTalk(devorin, qs));
		assertEquals(2, qs.getCond());

		assertEquals("nameless_spirit_q0119_0202.htm", q119.onTalk(spirit, qs));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest125InTheNameOfEvilPart1() {
		Quest125InTheNameOfEvilPart1 q125 = new Quest125InTheNameOfEvilPart1(questManager);
		QuestState qs = new QuestState(q125, session);

		NpcInstance mushika = Mockito.mock(NpcInstance.class);
		when(mushika.getNpcId()).thenReturn(Quest125InTheNameOfEvilPart1.START_NPC);

		assertEquals("mushika_q0125_0101.htm", q125.onTalk(mushika, qs));
		q125.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		NpcInstance shaman = Mockito.mock(NpcInstance.class);
		when(shaman.getNpcId()).thenReturn(32117);

		assertEquals("shaman_caracawe_q0125_0201.htm", q125.onTalk(shaman, qs));
		assertEquals(2, qs.getCond());

		assertEquals("mushika_q0125_0202.htm", q125.onTalk(mushika, qs));
		assertTrue(qs.isCompleted());
	}
}
