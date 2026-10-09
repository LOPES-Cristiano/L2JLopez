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

public class Batch14MidLevelRepeatablesTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		playerChar = new PlayerCharacter(
				1001, "acc1", "TestAdventurer", 35, 500_000L, 50_000, 0,
				0, 0, false, 0, 0, 0, 1000, 500, 1000, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 1000.0
		);

		session = Mockito.mock(GameSession.class);
		when(session.activeChar()).thenReturn(playerChar);
		when(session.inWorld()).thenReturn(true);
	}

	@Test
	@DisplayName("Lote 14: Quest 291 - Revenge Of The Redbonnet")
	void testQuest291RevengeOfTheRedbonnet() {
		Quest291RevengeOfTheRedbonnet q = new Quest291RevengeOfTheRedbonnet(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 292 - Brigands Sweep")
	void testQuest292BrigandsSweep() {
		Quest292BrigandsSweep q = new Quest292BrigandsSweep(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 293 - Hidden Vein")
	void testQuest293HiddenVein() {
		Quest293HiddenVein q = new Quest293HiddenVein(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 296 - Silk Of Tarantula")
	void testQuest296SilkOfTarantula() {
		Quest296SilkOfTarantula q = new Quest296SilkOfTarantula(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 300 - Hunting Leto Lizardman")
	void testQuest300HuntingLetoLizardman() {
		Quest300HuntingLetoLizardman q = new Quest300HuntingLetoLizardman(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 316 - Destroy Plaguebringers")
	void testQuest316DestroyPlaguebringers() {
		Quest316DestroyPlaguebringers q = new Quest316DestroyPlaguebringers(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		NpcInstance varak = Mockito.mock(NpcInstance.class);
		when(varak.getNpcId()).thenReturn(27020);
		q.onAttack(varak, qs);
		assertEquals(1, qs.getInt("cry"));
	}

	@Test
	@DisplayName("Lote 14: Quest 325 - Grim Collector")
	void testQuest325GrimCollector() {
		Quest325GrimCollector q = new Quest325GrimCollector(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 326 - Vanquish Remnants")
	void testQuest326VanquishRemnants() {
		Quest326VanquishRemnants q = new Quest326VanquishRemnants(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 333 - Black Lion Hunt")
	void testQuest333BlackLionHunt() {
		Quest333BlackLionHunt q = new Quest333BlackLionHunt(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 334 - The Wishing Potion")
	void testQuest334TheWishingPotion() {
		Quest334TheWishingPotion q = new Quest334TheWishingPotion(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	@DisplayName("Lote 14: Quest 335 - The Song Of The Hunter")
	void testQuest335TheSongOfTheHunter() {
		Quest335TheSongOfTheHunter q = new Quest335TheSongOfTheHunter(questManager);
		QuestState qs = new QuestState(q, session);

		q.onEvent("quest_accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		int[] array = new int[]{1, 2, 3};
		int packed = Quest.packInt(array, 5);
		int[] unpacked = Quest.unpackInt(packed, 5);
		assertEquals(1, unpacked[0]);
		assertEquals(2, unpacked[1]);
		assertEquals(3, unpacked[2]);
	}
}
