package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest241PossessorOfAPreciousSoul1;
import com.lopez.l2j.game.quest.impl.Quest242PossessorOfAPreciousSoul2;
import com.lopez.l2j.game.quest.impl.Quest373SupplierOfReagents;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NoblessAndReagentsQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(2001, "NoblesseHero", "Candidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(75);
		playerChar.classId(88);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockInvSvc = org.mockito.Mockito.mock(InventoryService.class);

		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(mockInvSvc);
		org.mockito.Mockito.when(mockCtx.questManager()).thenReturn(questManager);

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", sentPackets::add);
		setField(session, "state", com.lopez.l2j.network.game.packet.GameClientPacket.State.IN_GAME);
		setField(session, "active", playerChar);
		setField(session, "inWorld", true);
	}

	@Test
	@DisplayName("Validação Quest 241: Possessor of a Precious Soul - Part 1")
	void testQuest241Flow() {
		Quest241PossessorOfAPreciousSoul1 q241 = new Quest241PossessorOfAPreciousSoul1(questManager);
		QuestState qs = q241.newQuestState(session);

		var talienTpl = new NpcTemplate(Quest241PossessorOfAPreciousSoul1.TALIEN, Quest241PossessorOfAPreciousSoul1.TALIEN, "Talien", false, "", false, 10, 10, 70, "male", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance talien = new NpcInstance(8101, talienTpl, 0, 0, 0, 0);

		var barahamTpl = new NpcTemplate(Quest241PossessorOfAPreciousSoul1.BARAHAM, Quest241PossessorOfAPreciousSoul1.BARAHAM, "Baraham", false, "", false, 10, 10, 70, "male", "L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance baraham = new NpcInstance(8102, barahamTpl, 0, 0, 0, 0);

		// Início
		String talkTalien = q241.onTalk(talien, session);
		assertEquals("31739-1.htm", talkTalien);

		q241.onAdvEvent("31739-4.htm", talien, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Kill Baraham no cond 3
		qs.setCond(3);
		q241.onKill(baraham, session, false);
		assertEquals(4, qs.getCond());
		assertTrue(qs.hasQuestItems(Quest241PossessorOfAPreciousSoul1.LEGEND_OF_SEVENTEEN));

		// Entrega de itens finais (cond 20 -> 21)
		qs.setCond(20);
		qs.giveItems(Quest241PossessorOfAPreciousSoul1.LUNARGENT, 5);
		qs.giveItems(Quest241PossessorOfAPreciousSoul1.HELLFIRE_OIL, 1);
		q241.onAdvEvent("31272-5.htm", null, session);
		assertEquals(21, qs.getCond());

		// Conclusão com Caradine
		q241.onAdvEvent("31740-5.htm", null, session);
		assertTrue(qs.isCompleted());
		assertTrue(qs.hasQuestItems(Quest241PossessorOfAPreciousSoul1.VIRGILS_LETTER));
	}

	@Test
	@DisplayName("Validação Quest 242: Possessor of a Precious Soul - Part 2")
	void testQuest242Flow() {
		Quest242PossessorOfAPreciousSoul2 q242 = new Quest242PossessorOfAPreciousSoul2(questManager);
		QuestState qs = q242.newQuestState(session);

		qs.giveItems(Quest242PossessorOfAPreciousSoul2.VIRGILS_LETTER, 1);

		var virgilTpl = new NpcTemplate(Quest242PossessorOfAPreciousSoul2.VIRGIL, Quest242PossessorOfAPreciousSoul2.VIRGIL, "Virgil", false, "", false, 10, 10, 70, "male", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance virgil = new NpcInstance(8201, virgilTpl, 0, 0, 0, 0);

		String talkVirgil = q242.onTalk(virgil, session);
		assertEquals("31742-1.htm", talkVirgil);

		q242.onAdvEvent("31742-3.htm", virgil, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(0, qs.getQuestItemsCount(Quest242PossessorOfAPreciousSoul2.VIRGILS_LETTER));

		// Ativação dos 4 cornerstones no cond 9
		qs.setCond(9);
		qs.giveItems(Quest242PossessorOfAPreciousSoul2.ORB_OF_BINDING, 4);

		var cornerstoneTpl = new NpcTemplate(Quest242PossessorOfAPreciousSoul2.CORNERSTONE, Quest242PossessorOfAPreciousSoul2.CORNERSTONE, "Cornerstone", false, "", false, 10, 10, 70, "male", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance cornerstone = new NpcInstance(8202, cornerstoneTpl, 0, 0, 0, 0);

		for (int i = 0; i < 4; i++) {
			q242.onTalk(cornerstone, session);
		}
		assertEquals(10, qs.getCond());

		// Resgate do unicórnio e finalização
		qs.setCond(11);
		String finish = q242.onTalk(virgil, session);
		assertEquals("31742-6.htm", finish);
		assertTrue(qs.isCompleted());
		assertTrue(qs.hasQuestItems(Quest242PossessorOfAPreciousSoul2.CARADINE_LETTER));
	}

	@Test
	@DisplayName("Validação Quest 373: Supplier of Reagents (Urna e Alquimia)")
	void testQuest373Flow() {
		Quest373SupplierOfReagents q373 = new Quest373SupplierOfReagents(questManager);
		QuestState qs = q373.newQuestState(session);

		var wesleyTpl = new NpcTemplate(Quest373SupplierOfReagents.WESLEY, Quest373SupplierOfReagents.WESLEY, "Wesley", false, "", false, 10, 10, 70, "male", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance wesley = new NpcInstance(8301, wesleyTpl, 0, 0, 0, 0);

		// Aceite da missão
		q373.onAdvEvent("30166-4.htm", wesley, session);
		assertTrue(qs.isStarted());
		assertTrue(qs.hasQuestItems(Quest373SupplierOfReagents.MIXING_STONE));

		// Alquimia Pure Silver: 1 Lunargent + 1 Quicksilver -> Pure Silver (Temp 1 = 100%)
		qs.giveItems(Quest373SupplierOfReagents.LUNARGENT, 1);
		qs.giveItems(Quest373SupplierOfReagents.QUICKSILVER, 1);
		String res = q373.onAdvEvent("TempPureSilver1", null, session);
		assertEquals("New.htm", res);
		assertTrue(qs.hasQuestItems(Quest373SupplierOfReagents.PURE_SILVER));

		// Alquimia True Gold: 10 Magma Dust + 1 Quicksilver -> True Gold (Temp 1 = 100%)
		qs.giveItems(Quest373SupplierOfReagents.MAGMA_DUST, 10);
		qs.giveItems(Quest373SupplierOfReagents.QUICKSILVER, 1);
		String resGold = q373.onAdvEvent("TempTrueGold1", null, session);
		assertEquals("New.htm", resGold);
		assertTrue(qs.hasQuestItems(Quest373SupplierOfReagents.TRUE_GOLD));
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
