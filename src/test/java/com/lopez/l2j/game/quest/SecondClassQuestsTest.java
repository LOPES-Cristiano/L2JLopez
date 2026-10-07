package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest211TrialOfChallenger;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SecondClassQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(1001, "TestWarrior", "Candidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(35);
		playerChar.classId(1); // Human Warrior (elegível para Gladiator / Warlord)

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
	@DisplayName("CP 3.2: Validação de Integridade do Catálogo de 2ª Classe (23 Quests e 31 Classes)")
	void testSecondClassCatalogIntegrity() {
		var quests = SecondClassQuestCatalog.getAllQuests();
		assertEquals(23, quests.size(), "Devem existir exatamente 23 quests de 2ª classe (211 a 233)");

		for (int qId = 211; qId <= 233; qId++) {
			assertTrue(quests.containsKey(qId), "Quest ID " + qId + " deve estar cadastrada");
			var qInfo = quests.get(qId);
			assertTrue(qInfo.proofItemId() > 0, "Proof item ID deve ser válido");
			assertTrue(SecondClassQuestCatalog.isSecondClassProofItem(qInfo.proofItemId()));
		}

		var reqs = SecondClassQuestCatalog.getAllRequirements();
		assertEquals(31, reqs.size(), "Devem existir exatamente 31 classes de 2ª profissão no Interlude");

		// Gladiator (ID 2): Challenger (211) + Trust (217) + Duelist (222)
		var gladiator = reqs.get(2);
		assertNotNull(gladiator);
		assertEquals("Gladiator", gladiator.targetClassName());
		assertEquals(211, gladiator.trialQuestId());
		assertEquals(217, gladiator.testimonyQuestId());
		assertEquals(222, gladiator.testQuestId());
		assertEquals(2627, gladiator.trialProofItem()); // Mark of Challenger
		assertEquals(2734, gladiator.testimonyProofItem()); // Mark of Trust
		assertEquals(2762, gladiator.testProofItem()); // Mark of Duelist

		// Paladin (ID 5): Duty (212) + Trust (217) + Healer (226)
		var paladin = reqs.get(5);
		assertNotNull(paladin);
		assertEquals(212, paladin.trialQuestId());
		assertEquals(217, paladin.testimonyQuestId());
		assertEquals(226, paladin.testQuestId());

		// Bounty Hunter (ID 55): Guildsman (216) + Prosperity (221) + Searcher (225)
		var bh = reqs.get(55);
		assertNotNull(bh);
		assertEquals(216, bh.trialQuestId());
		assertEquals(221, bh.testimonyQuestId());
		assertEquals(225, bh.testQuestId());
	}

	@Test
	@DisplayName("CP 3.2: Fluxo completo da Quest 211 (Trial of the Challenger)")
	void testQuest211TrialOfChallengerFlow() {
		Quest211TrialOfChallenger quest = new Quest211TrialOfChallenger(questManager);

		var kashTpl = new NpcTemplate(30644, 30644, "Kash", false, "Martial Master", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance kash = new NpcInstance(7001, kashTpl, 0, 0, 0, 0);

		// 1. Início e Aceitação da Quest com Kash
		String talk1 = quest.onTalk(kash, session);
		assertEquals("30644-03.htm", talk1);

		quest.onAdvEvent("1", kash, session);

		QuestState qs = session.getQuestState(Quest211TrialOfChallenger.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// 2. Abate de Shyslassys (27110)
		var shyslassysTpl = new NpcTemplate(27110, 27110, "Shyslassys", false, "Quest Monster", false, 10.0, 15.0, 35, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(7002, shyslassysTpl, 0, 0, 0, 0);

		quest.onKill(mob, session, false);
		assertEquals(2, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest211TrialOfChallenger.BROKEN_KEY));

		// 3. Conversa com Chest of Shyslassys
		var chestTpl = new NpcTemplate(30647, 30647, "Chest", false, "Chest", false, 10.0, 15.0, 1, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance chest = new NpcInstance(7003, chestTpl, 0, 0, 0, 0);

		quest.onAdvEvent("30647_1", chest, session);
		assertEquals(1, qs.getQuestItemsCount(Quest211TrialOfChallenger.SCROLL_OF_SHYSLASSY));

		// 4. Retorno a Kash e avanço para o final com Raldo
		quest.onTalk(kash, session);
		assertEquals(3, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest211TrialOfChallenger.LETTER_OF_KASH));

		// Simula avanço para passo final (cond = 10) e entrega do Mark of Challenger
		qs.setCond(10);
		var raldoTpl = new NpcTemplate(30646, 30646, "Raldo", false, "Grand Master", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance raldo = new NpcInstance(7004, raldoTpl, 0, 0, 0, 0);

		String finalReply = quest.onTalk(raldo, session);
		assertEquals("30646-07.htm", finalReply);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest211TrialOfChallenger.MARK_OF_CHALLENGER));
		assertEquals(8, qs.getQuestItemsCount(Quest211TrialOfChallenger.DIMENSIONAL_DIAMOND));
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
