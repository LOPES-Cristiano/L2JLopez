package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest234FatesWhisper;
import com.lopez.l2j.game.quest.impl.Quest235MimirsElixir;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SubclassAndSagasTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(1001, "TestVeteran", "SubclassCandidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(75);
		playerChar.classId(2); // Gladiator

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
	@DisplayName("CP 4.2 & CP 4.3: Validação do Catálogo das 31 Sagas de 3ª Classe e Quests de Nobless")
	void testNoblessAndSagasCatalogIntegrity() {
		var sagas = NoblessAndSagaCatalog.getAllSagas();
		assertEquals(31, sagas.size(), "Devem existir exatamente 31 sagas de 3ª classe (70 a 100)");

		for (int qId = 70; qId <= 100; qId++) {
			assertTrue(sagas.containsKey(qId), "Saga ID " + qId + " deve estar cadastrada");
			var saga = sagas.get(qId);
			assertTrue(saga.secondClassId() > 0, "Segunda classe de origem deve ser válida");
			assertTrue(saga.thirdClassId() >= 88, "Terceira classe deve ser >= 88");
		}

		var duelSaga = NoblessAndSagaCatalog.getSagaByThirdClass(88).orElseThrow();
		assertEquals(73, duelSaga.questId());
		assertEquals("Duelist", duelSaga.thirdClassName());

		var nobless = NoblessAndSagaCatalog.getAllNoblessQuests();
		assertEquals(4, nobless.size(), "Devem existir exatamente 4 partes de Nobless");
		assertTrue(nobless.containsKey(241));
		assertTrue(nobless.containsKey(242));
		assertTrue(nobless.containsKey(246));
		assertTrue(nobless.containsKey(247));
	}

	@Test
	@DisplayName("CP 4.1: Fluxo da Quest 234 (Fate's Whisper) para obtenção da Star of Destiny")
	void testQuest234FatesWhisperFlow() {
		Quest234FatesWhisper quest = new Quest234FatesWhisper(questManager);

		var reorinTpl = new NpcTemplate(31002, 31002, "Reorin", false, "Maestro", false, 10.0, 15.0, 75, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance reorin = new NpcInstance(8001, reorinTpl, 0, 0, 0, 0);

		// 1. Início e Aceitação com Reorin
		String talk1 = quest.onTalk(reorin, session);
		assertEquals("31002-02.htm", talk1);

		quest.onAdvEvent("1", reorin, session);

		QuestState qs = session.getQuestState(Quest234FatesWhisper.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// 2. Coleta de Reirias' Soul Orb no Cabrio Coffer (31027)
		var cofferTpl = new NpcTemplate(31027, 31027, "Coffer", false, "Chest", false, 10.0, 15.0, 1, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance coffer = new NpcInstance(8002, cofferTpl, 0, 0, 0, 0);

		quest.onTalk(coffer, session);
		assertEquals(1, qs.getQuestItemsCount(Quest234FatesWhisper.REIRIAS_SOUL_ORB));

		// Retorna a Reorin -> avança para cond 2
		quest.onTalk(reorin, session);
		assertEquals(2, qs.getCond());

		// 3. Simula coleta dos 3 cetros e avanço para o final com 984 Cristais B
		qs.giveItems(Quest234FatesWhisper.KERMONS_INFERNIUM_SCEPTER, 1);
		qs.giveItems(Quest234FatesWhisper.GOLCONDAS_INFERNIUM_SCEPTER, 1);
		qs.giveItems(Quest234FatesWhisper.HALLATES_INFERNIUM_SCEPTER, 1);

		quest.onTalk(reorin, session);
		assertEquals(3, qs.getCond());

		// Passo final: cond 11 com 984 Cristais B
		qs.setCond(11);
		qs.giveItems(Quest234FatesWhisper.CRYSTAL_B, 984);

		String finalReply = quest.onTalk(reorin, session);
		assertEquals("31002-12.htm", finalReply);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest234FatesWhisper.STAR_OF_DESTINY));
	}

	@Test
	@DisplayName("CP 4.1: Fluxo da Quest 235 (Mimir's Elixir) para desbloqueio de Subclasse")
	void testQuest235MimirsElixirFlow() {
		Quest235MimirsElixir quest = new Quest235MimirsElixir(questManager);

		// Inicialmente sem a Star of Destiny: Ladd recusa
		var laddTpl = new NpcTemplate(30721, 30721, "Ladd", false, "Magister", false, 10.0, 15.0, 75, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance ladd = new NpcInstance(8003, laddTpl, 0, 0, 0, 0);

		String replyNoStar = quest.onTalk(ladd, session);
		assertEquals("30721-01a.htm", replyNoStar);

		// Adiciona a Star of Destiny e aceita a quest
		QuestState qs = session.getQuestState(Quest235MimirsElixir.QUEST_NAME);
		if (qs == null) {
			qs = quest.newQuestState(session);
		}
		qs.giveItems(Quest235MimirsElixir.STAR_OF_DESTINY, 1);

		String replyWithStar = quest.onTalk(ladd, session);
		assertEquals("30721-01.htm", replyWithStar);

		quest.onAdvEvent("1", ladd, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Simula sintetização do elixir (cond 8)
		qs.setCond(8);
		qs.giveItems(Quest235MimirsElixir.PURE_SILVER, 1);
		qs.giveItems(Quest235MimirsElixir.TRUE_GOLD, 1);
		qs.giveItems(Quest235MimirsElixir.BLOOD_FIRE, 1);

		String finishReply = quest.onTalk(ladd, session);
		assertEquals("30721-07.htm", finishReply);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest235MimirsElixir.MIMIRS_ELIXIR));
		assertEquals(1, qs.getQuestItemsCount(Quest235MimirsElixir.ENCHANT_WEAPON_A));
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
