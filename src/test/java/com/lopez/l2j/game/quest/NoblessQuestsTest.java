package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest246PossessorOfAPreciousSoul3;
import com.lopez.l2j.game.quest.impl.Quest247PossessorOfAPreciousSoul4;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NoblessQuestsTest {

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
		playerChar.classId(88); // Archmage (3rd class / high level)

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
	@DisplayName("CP 7: Validação de Catálogo Nobless (Quests 241, 242, 246, 247)")
	void testNoblessCatalogIntegrity() {
		var catalog = NoblessAndSagaCatalog.getAllNoblessQuests();
		assertEquals(4, catalog.size());

		assertTrue(catalog.containsKey(241));
		assertTrue(catalog.containsKey(242));
		assertTrue(catalog.containsKey(246));
		assertTrue(catalog.containsKey(247));

		for (var entry : catalog.values()) {
			assertEquals(75, entry.minLevel(), "Todas as quests de nobless requerem nível 75");
			assertTrue(entry.starterNpcId() > 0);
		}
	}

	@Test
	@DisplayName("CP 7: Fluxo Completo da Quest 246 (Possessor of a Precious Soul - Part 3 / Barakiel)")
	void testQuest246BarakielFlow() {
		Quest246PossessorOfAPreciousSoul3 quest = new Quest246PossessorOfAPreciousSoul3(questManager);

		// Dá a carta 1 de Caradine
		var qs = quest.newQuestState(session);
		qs.giveItems(Quest246PossessorOfAPreciousSoul3.CARADINE_LETTER_1, 1);

		var caradineTpl = new NpcTemplate(31740, 31740, "Caradine", false, "Noble Lady", false, 10.0, 15.0, 70, "female",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance caradine = new NpcInstance(8001, caradineTpl, 0, 0, 0, 0);

		var ossianTpl = new NpcTemplate(31741, 31741, "Ossian", false, "Magister", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance ossian = new NpcInstance(8002, ossianTpl, 0, 0, 0, 0);

		var laddTpl = new NpcTemplate(30721, 30721, "Ladd", false, "Grand Magister", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance ladd = new NpcInstance(8003, laddTpl, 0, 0, 0, 0);

		// 1. Inicia com Caradine
		String talkCaradine = quest.onTalk(caradine, session);
		assertEquals("31740-01.htm", talkCaradine);

		quest.onAdvEvent("31740-04.htm", caradine, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(0, qs.getQuestItemsCount(Quest246PossessorOfAPreciousSoul3.CARADINE_LETTER_1));

		// 2. Fala com Ossian e avança para obter relíquias
		quest.onAdvEvent("31741-02.htm", ossian, session);
		assertEquals(2, qs.getCond());

		// 3. Simula drop de Waterbinder e Evergreen
		qs.giveItems(Quest246PossessorOfAPreciousSoul3.WATERBINDER, 1);
		qs.giveItems(Quest246PossessorOfAPreciousSoul3.EVERGREEN, 1);
		qs.setCond(3);

		quest.onAdvEvent("31741-05.htm", ossian, session);
		assertEquals(4, qs.getCond(), "Ao entregar as relíquias, condição avança para 4 (derrotar Barakiel)");
		assertEquals(0, qs.getQuestItemsCount(Quest246PossessorOfAPreciousSoul3.WATERBINDER));

		// 4. Derrota do Raid Boss Barakiel (25325)
		var barakielTpl = new NpcTemplate(25325, 25325, "Flame of Splendor Barakiel", false, "Raid Boss", false, 10.0, 15.0, 70, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance barakiel = new NpcInstance(8004, barakielTpl, 0, 0, 0, 0);

		quest.onKill(barakiel, qs, false);
		assertEquals(1, qs.getQuestItemsCount(Quest246PossessorOfAPreciousSoul3.RAIN_SONG));
		assertEquals(5, qs.getCond());

		// 5. Ossian entrega a Relic Box
		quest.onAdvEvent("31741-08.htm", ossian, session);
		assertEquals(6, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest246PossessorOfAPreciousSoul3.RELIC_BOX));

		// 6. Ladd recebe a Relic Box e entrega a Carta de Caradine 2
		quest.onAdvEvent("30721-02.htm", ladd, session);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest246PossessorOfAPreciousSoul3.CARADINE_LETTER_2));
	}

	@Test
	@DisplayName("CP 7: Fluxo Completo da Quest 247 (Consagração Nobless e Coroação com Tiara)")
	void testQuest247NoblessConsecration() {
		Quest247PossessorOfAPreciousSoul4 quest = new Quest247PossessorOfAPreciousSoul4(questManager);

		var qs = quest.newQuestState(session);
		qs.giveItems(Quest247PossessorOfAPreciousSoul4.CARADINE_LETTER, 1);
		assertFalse(playerChar.isNoble(), "Jogador não deve ser nobless antes da quest");

		var caradineTpl = new NpcTemplate(31740, 31740, "Caradine", false, "Noble Lady", false, 10.0, 15.0, 70, "female",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance caradine = new NpcInstance(8005, caradineTpl, 0, 0, 0, 0);

		var ladyTpl = new NpcTemplate(31745, 31745, "Lady of the Lake", false, "Goddess", false, 10.0, 15.0, 70, "female",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance lady = new NpcInstance(8006, ladyTpl, 0, 0, 0, 0);

		// 1. Fala com Caradine e aceita
		String talkCaradine = quest.onTalk(caradine, session);
		assertEquals("31740-01.htm", talkCaradine);

		quest.onAdvEvent("31740-03.htm", caradine, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(0, qs.getQuestItemsCount(Quest247PossessorOfAPreciousSoul4.CARADINE_LETTER));

		quest.onAdvEvent("31740-05.htm", caradine, session);
		assertEquals(2, qs.getCond());

		// 2. Fala com a Lady of the Lake
		String talkLady = quest.onTalk(lady, session);
		assertEquals("31745-01.htm", talkLady);

		// 3. Consagração Nobless
		quest.onAdvEvent("31745-05.htm", lady, session);
		assertTrue(qs.isCompleted());
		assertTrue(playerChar.isNoble(), "Jogador agora DEVE ser oficialmente Nobless");
		assertEquals(1, qs.getQuestItemsCount(Quest247PossessorOfAPreciousSoul4.NOBLESS_TIARA));
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
