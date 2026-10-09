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

public class PetsAndPKSystemQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "HeroPlayer", 55, 10_000_000L, 500_000, 0,
				2, 2, false, 0, 0, 0, 3000, 1500, 2000, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 3000.0, 1500.0, 2000.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testQuest419GetAPet() {
		Quest419GetAPet q419 = new Quest419GetAPet(questManager);
		QuestState qs = new QuestState(q419, session);

		NpcInstance martin = Mockito.mock(NpcInstance.class);
		when(martin.getNpcId()).thenReturn(Quest419GetAPet.MARTIN);

		assertEquals("Start.htm", q419.onTalk(martin, qs));
		q419.onEvent("agree", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest419GetAPet.ANIMAL_SLAYER_LIST1));

		// Collect 50 fangs
		for (int i = 0; i < 50; i++) {
			qs.giveItems(Quest419GetAPet.BLOODY_FANG, 1);
		}
		assertEquals("419_talk_villagers.htm", q419.onTalk(martin, qs));
		assertEquals(2, qs.getCond());

		// Complete quiz
		q419.onEvent("try_quiz", qs);
		assertTrue(qs.hasQuestItems(Quest419GetAPet.WOLF_COLLAR));
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest420LittleWings() {
		Quest420LittleWings q420 = new Quest420LittleWings(questManager);
		QuestState qs = new QuestState(q420, session);

		NpcInstance cooper = Mockito.mock(NpcInstance.class);
		when(cooper.getNpcId()).thenReturn(Quest420LittleWings.COOPER);

		assertEquals("30829-01.htm", q420.onTalk(cooper, qs));
		q420.onEvent("30829-02.htm", qs);
		assertEquals(1, qs.getCond());

		// Progress to flute reward
		q420.onEvent("reward_flute", qs);
		assertTrue(
				qs.hasQuestItems(Quest420LittleWings.DRAGONFLUTE_OF_WIND) ||
				qs.hasQuestItems(Quest420LittleWings.DRAGONFLUTE_OF_STAR) ||
				qs.hasQuestItems(Quest420LittleWings.DRAGONFLUTE_OF_TWILIGHT)
		);
		assertTrue(qs.isCompleted());
	}

	@Test
	void testQuest421LittleWingAdventures() {
		Quest421LittleWingAdventures q421 = new Quest421LittleWingAdventures(questManager);
		QuestState qs = new QuestState(q421, session);

		qs.giveItems(Quest421LittleWingAdventures.DRAGONFLUTE_OF_WIND, 1);
		q421.onEvent("30610-05.htm", qs);
		assertEquals(1, qs.getCond());

		// Evolve to strider
		q421.onEvent("evolve_strider", qs);
		assertTrue(qs.hasQuestItems(Quest421LittleWingAdventures.STRIDER_WIND));
		assertFalse(qs.hasQuestItems(Quest421LittleWingAdventures.DRAGONFLUTE_OF_WIND));
	}

	@Test
	void testQuest422RepentYourSins() {
		Quest422RepentYourSins q422 = new Quest422RepentYourSins(questManager);
		QuestState qs = new QuestState(q422, session);

		NpcInstance judge = Mockito.mock(NpcInstance.class);
		when(judge.getNpcId()).thenReturn(Quest422RepentYourSins.BLACK_JUDGE);

		assertEquals(5, pc.pkKills());
		assertEquals("black_judge_q0422_02.htm", q422.onTalk(judge, qs));

		q422.onEvent("Start", qs);
		assertTrue(qs.getCond() >= 2);

		q422.onEvent("obtain_manacles", qs);
		assertEquals(16, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest422RepentYourSins.PENITENTS_MANACLES));

		// Clean PK
		q422.onEvent("clean_pk", qs);
		assertTrue(pc.pkKills() < 5); // PK kills reduced!
	}

	@Test
	void testQuest426QuestForFishingShot() {
		Quest426QuestForFishingShot q426 = new Quest426QuestForFishingShot(questManager);
		QuestState qs = new QuestState(q426, session);

		q426.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(Quest426QuestForFishingShot.SWEET_FLUID, 2);
		q426.onEvent("reward_ng", qs);
		assertEquals(66, qs.getQuestItemsCount(Quest426QuestForFishingShot.FISHING_SHOT_NG));
		assertEquals(1, qs.getQuestItemsCount(Quest426QuestForFishingShot.SWEET_FLUID));
	}

	@Test
	void testQuest020BringUpWithLove() {
		Quest020BringUpWithLove q020 = new Quest020BringUpWithLove(questManager);
		QuestState qs = new QuestState(q020, session);

		q020.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(Quest020BringUpWithLove.JEWEL_OF_INNOCENCE, 1);
		q020.onEvent("reply_6", qs);
		assertEquals(68500, qs.getQuestItemsCount(57)); // 68.5k Adena
		assertTrue(qs.isCompleted());
	}
}
