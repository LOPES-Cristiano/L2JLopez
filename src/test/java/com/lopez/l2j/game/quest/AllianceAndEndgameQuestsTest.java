package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest605AllianceWithKetraOrcs;
import com.lopez.l2j.game.quest.impl.Quest611AllianceWithVarkaSilenos;
import com.lopez.l2j.game.quest.impl.Quest619RelicsOfTheOldEmpire;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AllianceAndEndgameQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(3001, "AllianceWarrior", "Candidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(76);
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
	@DisplayName("CP 8: Catálogo de Quests de Farm Endgame e Alianças")
	void testEndgameFarmCatalogIntegrity() {
		var all = EndgameFarmCatalog.allEntries();
		assertTrue(all.containsKey(350), "Quest 350 SA");
		assertTrue(all.containsKey(373), "Quest 373 Reagentes");
		assertTrue(all.containsKey(617), "Quest 617 FotG");
		assertTrue(all.containsKey(619), "Quest 619 IT");
		assertTrue(all.containsKey(605), "Quest 605 Ketra");
		assertTrue(all.containsKey(611), "Quest 611 Varka");
	}

	@Test
	@DisplayName("CP 8: Aliança com Varka Silenos (Quest 611)")
	void testQuest611AllianceWithVarkaFlow() {
		Quest611AllianceWithVarkaSilenos quest = new Quest611AllianceWithVarkaSilenos(questManager);

		var naranTpl = new NpcTemplate(31378, 31378, "Naran Ashanuk", false, "Varka Captain", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance naran = new NpcInstance(9001, naranTpl, 0, 0, 0, 0);

		String talkInit = quest.onTalk(naran, session);
		assertEquals("31378-01.htm", talkInit);

		quest.onAdvEvent("31378-04.htm", naran, session);
		var qs = session.getQuestState(Quest611AllianceWithVarkaSilenos.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Adiciona 100 Ketra Soldier Badges e promove para Estágio 1
		qs.giveItems(Quest611AllianceWithVarkaSilenos.KETRA_BADGE_SOLDIER, 100);
		String talkLevel1 = quest.onTalk(naran, session);
		assertEquals("31378-05.htm", talkLevel1);
		assertEquals(1, qs.getQuestItemsCount(Quest611AllianceWithVarkaSilenos.MARK_VARKA_1));
		assertEquals(0, qs.getQuestItemsCount(Quest611AllianceWithVarkaSilenos.KETRA_BADGE_SOLDIER));
		assertEquals(2, qs.getCond());
	}

	@Test
	@DisplayName("CP 8: Relíquias do Antigo Império (Quest 619 - Imperial Tomb)")
	void testQuest619RelicsOfTheOldEmpireFlow() {
		Quest619RelicsOfTheOldEmpire quest = new Quest619RelicsOfTheOldEmpire(questManager);

		var ghostTpl = new NpcTemplate(31538, 31538, "Ghost of Adventurer", false, "Ghost", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance ghost = new NpcInstance(9002, ghostTpl, 0, 0, 0, 0);

		String talkInit = quest.onTalk(ghost, session);
		assertEquals("31538-01.htm", talkInit);

		quest.onAdvEvent("31538-03.htm", ghost, session);
		var qs = session.getQuestState(Quest619RelicsOfTheOldEmpire.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());

		// Dá 1000 Broken Relic Parts e troca por receita S-Grade
		qs.giveItems(Quest619RelicsOfTheOldEmpire.BROKEN_RELIC_PART, 1000);
		quest.onAdvEvent("31538-07.htm", ghost, session);

		assertEquals(0, qs.getQuestItemsCount(Quest619RelicsOfTheOldEmpire.BROKEN_RELIC_PART));
		long recipeCount = Quest619RelicsOfTheOldEmpire.REWARD_RECIPES.stream()
				.filter(id -> qs.getQuestItemsCount(id) > 0)
				.count();
		assertTrue(recipeCount > 0, "O jogador deve ter recebido uma receita S-Grade da lista de recompensas");
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
