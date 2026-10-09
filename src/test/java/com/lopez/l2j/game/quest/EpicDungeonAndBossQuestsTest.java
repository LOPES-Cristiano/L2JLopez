package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EpicDungeonAndBossQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(2001, "BossHunter", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(78);
		playerChar.classId(88);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockInvSvc = org.mockito.Mockito.mock(InventoryService.class);

		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(mockInvSvc);
		org.mockito.Mockito.when(mockCtx.questManager()).thenReturn(questManager);

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", sentPackets::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", playerChar);
		setField(session, "inWorld", true);
	}

	@Test
	@DisplayName("Validação Quest 641: Attack Sailren")
	void testQuest641Flow() {
		Quest641AttackSailren q641 = new Quest641AttackSailren(questManager);
		QuestState qs = q641.newQuestState(session);

		var statueTpl = new NpcTemplate(Quest641AttackSailren.STATUE_OF_SHILEN, Quest641AttackSailren.STATUE_OF_SHILEN, "Statue", false, "", false, 10, 10, 70, "female", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance statue = new NpcInstance(8401, statueTpl, 0, 0, 0, 0);

		String talk = q641.onTalk(statue, session);
		assertEquals("statue_of_shilen_q0641_01.htm", talk);

		q641.onAdvEvent("statue_of_shilen_q0641_05.htm", statue, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Dá 30 fragmentos de Gazkh
		qs.giveItems(Quest641AttackSailren.FRAGMENT_OF_GAZKH, 30);
		qs.setCond(2);

		q641.onAdvEvent("statue_of_shilen_q0641_08.htm", statue, session);
		assertTrue(qs.hasQuestItems(Quest641AttackSailren.GAZKH));
		assertEquals(0, qs.getQuestItemsCount(Quest641AttackSailren.FRAGMENT_OF_GAZKH));
	}

	@Test
	@DisplayName("Validação Quest 620: Four Goblets")
	void testQuest620Flow() {
		Quest620FourGoblets q620 = new Quest620FourGoblets(questManager);
		QuestState qs = q620.newQuestState(session);

		var spiritTpl = new NpcTemplate(Quest620FourGoblets.NAMELESS_SPIRIT, Quest620FourGoblets.NAMELESS_SPIRIT, "Spirit", false, "", false, 10, 10, 70, "male", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance spirit = new NpcInstance(8402, spiritTpl, 0, 0, 0, 0);

		String talk = q620.onTalk(spirit, session);
		assertEquals("31453-1.htm", talk);

		q620.onAdvEvent("31453-13.htm", spirit, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Adiciona 4 cálices
		qs.giveItems(Quest620FourGoblets.GOBLET_ALECTIA, 1);
		qs.giveItems(Quest620FourGoblets.GOBLET_TISHA, 1);
		qs.giveItems(Quest620FourGoblets.GOBLET_MEKARA, 1);
		qs.giveItems(Quest620FourGoblets.GOBLET_MORIGUL, 1);

		String talkReady = q620.onTalk(spirit, session);
		assertEquals("31453-15.htm", talkReady);

		q620.onAdvEvent("31453-16.htm", spirit, session);
		assertEquals(2, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest620FourGoblets.ANTIQUE_BROOCH));
		assertEquals(0, qs.getQuestItemsCount(Quest620FourGoblets.GOBLET_ALECTIA));
	}

	@Test
	@DisplayName("Validação Quest 601 e 602: Eye of Argos (Pagan Temple)")
	void testQuest601And602Flow() {
		Quest601WatchingEyes q601 = new Quest601WatchingEyes(questManager);
		QuestState qs601 = q601.newQuestState(session);

		q601.onAdvEvent("quest_accept", null, session);
		assertTrue(qs601.isStarted());

		qs601.giveItems(Quest601WatchingEyes.PROOF_OF_AVENGER, 100);
		String res601 = q601.onAdvEvent("reply_3", null, session);
		assertEquals("eye_of_argos_q0601_0201.htm", res601);
		assertEquals(0, qs601.getQuestItemsCount(Quest601WatchingEyes.PROOF_OF_AVENGER));

		Quest602ShadowOfLight q602 = new Quest602ShadowOfLight(questManager);
		QuestState qs602 = q602.newQuestState(session);

		q602.onAdvEvent("quest_accept", null, session);
		assertTrue(qs602.isStarted());

		qs602.giveItems(Quest602ShadowOfLight.EYE_OF_DARKNESS, 100);
		String res602 = q602.onAdvEvent("reply_3", null, session);
		assertEquals("eye_of_argos_q0602_0201.htm", res602);
		assertEquals(0, qs602.getQuestItemsCount(Quest602ShadowOfLight.EYE_OF_DARKNESS));
	}

	@Test
	@DisplayName("Validação Quest 624, 625 e 623: Hot Springs Trilogy (Ice Crystal)")
	void testHotSpringsTrilogyFlow() {
		// Quest 624: Ice Crystal
		Quest624TheFinestIngredientsPart1 q624 = new Quest624TheFinestIngredientsPart1(questManager);
		QuestState qs624 = q624.newQuestState(session);

		q624.onAdvEvent("31521-1.htm", null, session);
		assertTrue(qs624.isStarted());

		qs624.giveItems(Quest624TheFinestIngredientsPart1.TRUNK_OF_NEPENTHES, 50);
		qs624.giveItems(Quest624TheFinestIngredientsPart1.FOOT_OF_BANDERSNATCHLING, 50);
		qs624.giveItems(Quest624TheFinestIngredientsPart1.SECRET_SPICE, 50);

		q624.onAdvEvent("31521-4.htm", null, session);
		assertTrue(qs624.hasQuestItems(Quest624TheFinestIngredientsPart1.ICE_CRYSTAL));
		assertTrue(qs624.hasQuestItems(Quest624TheFinestIngredientsPart1.SAUCE));

		// Quest 625: Bumbalump
		Quest625TheFinestIngredientsPart2 q625 = new Quest625TheFinestIngredientsPart2(questManager);
		QuestState qs625 = q625.newQuestState(session);

		q625.onAdvEvent("31521-02.htm", null, session);
		assertTrue(qs625.isStarted());
		assertTrue(qs625.hasQuestItems(Quest625TheFinestIngredientsPart2.FOOD));

		q625.onAdvEvent("31542-02.htm", null, session);
		assertEquals(2, qs625.getCond());

		qs625.giveItems(Quest625TheFinestIngredientsPart2.MEAT, 1);
		q625.onAdvEvent("31521-04.htm", null, session);
		assertEquals(0, qs625.getQuestItemsCount(Quest625TheFinestIngredientsPart2.MEAT));

		// Quest 623: Finest Food
		Quest623TheFinestFood q623 = new Quest623TheFinestFood(questManager);
		QuestState qs623 = q623.newQuestState(session);

		q623.onAdvEvent("31521-03.htm", null, session);
		assertTrue(qs623.isStarted());

		qs623.giveItems(Quest623TheFinestFood.LEAF_OF_FLAVA, 100);
		qs623.giveItems(Quest623TheFinestFood.BUFFALO_MEAT, 100);
		qs623.giveItems(Quest623TheFinestFood.ANTELOPE_HORN, 100);

		q623.onAdvEvent("31521-07.htm", null, session);
		assertEquals(0, qs623.getQuestItemsCount(Quest623TheFinestFood.LEAF_OF_FLAVA));
	}

	private static void setField(Object target, String name, Object val) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
