package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest401PathToWarrior;
import com.lopez.l2j.game.quest.impl.Quest402PathToKnight;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FirstClassQuestsTest {

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
		playerChar.level(18);
		playerChar.classId(0); // Human Fighter

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
	@DisplayName("CP 3.1: Validação de Integridade do Catálogo das 18 Quests de 1ª Classe (401-418)")
	void testFirstClassCatalogIntegrity() {
		var catalog = FirstClassQuestCatalog.getAllQuests();
		assertEquals(18, catalog.size(), "Devem existir exatamente 18 quests de 1ª classe");

		// Verifica se todas do range 401 a 418 existem
		for (int qId = 401; qId <= 418; qId++) {
			assertTrue(catalog.containsKey(qId), "Quest ID " + qId + " deve estar registrada no catálogo");
			var info = catalog.get(qId);
			assertEquals(18, info.minLevel(), "Nível mínimo deve ser 18");
			assertTrue(info.proofItemId() > 0, "Item de prova deve ter ID válido");
			assertTrue(FirstClassQuestCatalog.isFirstClassProofItem(info.proofItemId()));
		}

		// Checa classes específicas
		var warrior = FirstClassQuestCatalog.getByTargetClass(1).orElseThrow();
		assertEquals(401, warrior.questId());
		assertEquals("401_PathToWarrior", warrior.questName());
		assertEquals(1145, warrior.proofItemId());

		var knight = FirstClassQuestCatalog.getByTargetClass(4).orElseThrow();
		assertEquals(402, knight.questId());
		assertEquals(1161, knight.proofItemId());
	}

	@Test
	@DisplayName("CP 3.1: Fluxo completo da Quest 401 (Path to Warrior)")
	void testQuest401PathToWarriorFlow() {
		Quest401PathToWarrior quest = new Quest401PathToWarrior(questManager);

		var auronTpl = new NpcTemplate(30010, 30010, "Auron", false, "Master", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance auron = new NpcInstance(6001, auronTpl, 0, 0, 0, 0);

		// 1. Diálogo inicial e aceitação da quest
		String talk1 = quest.onTalk(auron, session);
		assertEquals("30010-01.htm", talk1);

		String replyAccept = quest.onAdvEvent("1", auron, session);
		assertEquals("30010-06.htm", replyAccept);

		QuestState qs = session.getQuestState(Quest401PathToWarrior.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest401PathToWarrior.EINS_LETTER));

		// 2. Diálogo com Simplon (30253)
		var simplonTpl = new NpcTemplate(30253, 30253, "Simplon", false, "Trader", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance simplon = new NpcInstance(6002, simplonTpl, 0, 0, 0, 0);

		String talkSimplon = quest.onTalk(simplon, session);
		assertEquals("30253-01.htm", talkSimplon);

		quest.onAdvEvent("30253_1", simplon, session);
		assertEquals(2, qs.getCond());
		assertEquals(0, qs.getQuestItemsCount(Quest401PathToWarrior.EINS_LETTER));
		assertEquals(1, qs.getQuestItemsCount(Quest401PathToWarrior.WARRIOR_GUILD_MARK));

		// 3. Simula entrega de 10 Rusted Bronze Swords e avanço para o passo final
		qs.setCond(5);
		qs.takeItems(Quest401PathToWarrior.WARRIOR_GUILD_MARK, -1);
		qs.takeItems(Quest401PathToWarrior.SIMPLONS_LETTER, -1);
		qs.giveItems(Quest401PathToWarrior.RUSTED_BRONZE_SWORD3, 1);
		qs.giveItems(Quest401PathToWarrior.POISON_SPIDER_LEG2, 20);

		String talkFinal = quest.onTalk(auron, session);
		assertEquals("30010-13.htm", talkFinal);
		assertTrue(qs.isCompleted(), "Quest 401 deve estar completada com sucesso");
	}

	@Test
	@DisplayName("CP 3.1: Aceitação da Quest 402 (Path to Knight)")
	void testQuest402PathToKnightAcceptance() {
		Quest402PathToKnight quest = new Quest402PathToKnight(questManager);

		var klausTpl = new NpcTemplate(30417, 30417, "Sir Klaus", false, "Grand Master", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance klaus = new NpcInstance(6003, klausTpl, 0, 0, 0, 0);

		String talk = quest.onTalk(klaus, session);
		assertEquals("30417-01.htm", talk);

		quest.onAdvEvent("30417-08.htm", klaus, session);

		QuestState qs = session.getQuestState(Quest402PathToKnight.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
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
