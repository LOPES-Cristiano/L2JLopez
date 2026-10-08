package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest211TrialOfChallenger;
import com.lopez.l2j.game.quest.impl.Quest212TrialOfDuty;
import com.lopez.l2j.game.quest.impl.Quest217TestimonyOfTrust;
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

		playerChar = new PlayerCharacter(1002, "KnightTester", "Candidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(37);
		playerChar.classId(4); // Human Knight

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
	@DisplayName("CP 5: Integridade do Catálogo de 2ª Classe (23 Quests e 31 Classes)")
	void testSecondClassCatalogIntegrity() {
		var allQuests = SecondClassQuestCatalog.getAllQuests();
		assertEquals(23, allQuests.size(), "Devem existir 23 quests de 2ª classe no catálogo");

		var allReqs = SecondClassQuestCatalog.getAllRequirements();
		assertEquals(31, allReqs.size(), "Devem existir 31 classes de 2ª profissão mapeadas");

		// Verifica se todas as classes possuem 3 provas válidas
		for (var req : allReqs.values()) {
			assertEquals(3, req.requiredProofItems().size(), "Cada classe deve exigir exatamente 3 provas");
			assertTrue(SecondClassQuestCatalog.isSecondClassProofItem(req.trialProofItem()));
			assertTrue(SecondClassQuestCatalog.isSecondClassProofItem(req.testimonyProofItem()));
			assertTrue(SecondClassQuestCatalog.isSecondClassProofItem(req.testProofItem()));
		}
	}

	@Test
	@DisplayName("CP 5: Fluxo da Quest 212 (Trial of Duty)")
	void testQuest212TrialOfDutyFlow() {
		Quest212TrialOfDuty quest = new Quest212TrialOfDuty(questManager);

		var hannavaltTpl = new NpcTemplate(30109, 30109, "Duke Hannavalt", false, "Duke", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance hannavalt = new NpcInstance(8010, hannavaltTpl, 0, 0, 0, 0);

		String talk = quest.onTalk(hannavalt, session);
		assertEquals("30109-03.htm", talk);

		quest.onAdvEvent("1", hannavalt, session);
		QuestState qs = session.getQuestState(Quest212TrialOfDuty.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Simula avanço para entrega final com Dustin e Hannavalt
		qs.setCond(18);
		qs.giveItems(Quest212TrialOfDuty.LETTER_OF_DUSTIN, 1);

		String talkFinal = quest.onTalk(hannavalt, session);
		assertEquals("30109-05.htm", talkFinal);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest212TrialOfDuty.MARK_OF_DUTY));
	}

	@Test
	@DisplayName("CP 5: Fluxo da Quest 217 (Testimony of Trust)")
	void testQuest217TestimonyOfTrustFlow() {
		Quest217TestimonyOfTrust quest = new Quest217TestimonyOfTrust(questManager);

		var hollinTpl = new NpcTemplate(30191, 30191, "Hollin", false, "High Priest", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance hollin = new NpcInstance(8011, hollinTpl, 0, 0, 0, 0);

		String talk = quest.onTalk(hollin, session);
		assertEquals("30191-03.htm", talk);

		quest.onAdvEvent("1", hollin, session);
		QuestState qs = session.getQuestState(Quest217TestimonyOfTrust.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest217TestimonyOfTrust.LETTER_TO_ELF));
		assertEquals(1, qs.getQuestItemsCount(Quest217TestimonyOfTrust.LETTER_TO_DARKELF));
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
