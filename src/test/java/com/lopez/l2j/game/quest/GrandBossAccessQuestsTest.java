package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest119LastImperialPrince;
import com.lopez.l2j.game.quest.impl.Quest337AudienceWithLandDragon;
import com.lopez.l2j.game.quest.impl.Quest348AnArrogantSearch;
import com.lopez.l2j.game.quest.impl.Quest618IntoTheFlame;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GrandBossAccessQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(1001, "TestHero", "Slayer", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(75);
		playerChar.classId(88); // Duelist

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
	@DisplayName("CP 5.1-5.4: Integridade do Catálogo de Acesso aos Grand Bosses")
	void testGrandBossAccessCatalogIntegrity() {
		var bosses = GrandBossAccessCatalog.getAllBosses();
		assertTrue(bosses.containsKey("ANTHARAS"));
		assertTrue(bosses.containsKey("BAIUM"));
		assertTrue(bosses.containsKey("VALAKAS"));
		assertTrue(bosses.containsKey("FRINTEZZA"));
		assertTrue(bosses.containsKey("SAILREN"));

		var antharas = GrandBossAccessCatalog.getByBossName("ANTHARAS").orElseThrow();
		assertEquals(337, antharas.questId());
		assertEquals(3865, antharas.accessItemId()); // Portal Stone
		assertTrue(GrandBossAccessCatalog.isBossAccessItem(3865));

		var valakas = GrandBossAccessCatalog.getByBossName("VALAKAS").orElseThrow();
		assertEquals(618, valakas.questId());
		assertEquals(7265, valakas.accessItemId()); // Floating Stone

		var frintezza = GrandBossAccessCatalog.getByBossName("FRINTEZZA").orElseThrow();
		assertEquals(119, frintezza.questId());
		assertEquals(8073, frintezza.accessItemId()); // Frintezza Scroll
	}

	@Test
	@DisplayName("CP 5.3: Fluxo completo da Quest 618 (Into the Flame) para Floating Stone de Valakas")
	void testQuest618IntoTheFlameFlow() {
		Quest618IntoTheFlame quest = new Quest618IntoTheFlame(questManager);

		var kleinTpl = new NpcTemplate(31540, 31540, "Klein", false, "Watcher", false, 10.0, 15.0, 75, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance klein = new NpcInstance(9001, kleinTpl, 0, 0, 0, 0);

		// 1. Início e Aceitação com Klein
		String talk1 = quest.onTalk(klein, session);
		assertEquals("31540-02.htm", talk1);

		quest.onAdvEvent("31540-03.htm", klein, session);

		QuestState qs = session.getQuestState(Quest618IntoTheFlame.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// 2. Diálogo com Hilda (31271) em Goddard
		var hildaTpl = new NpcTemplate(31271, 31271, "Hilda", false, "Blacksmith", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance hilda = new NpcInstance(9002, hildaTpl, 0, 0, 0, 0);

		quest.onAdvEvent("31271-02.htm", hilda, session);
		assertEquals(2, qs.getCond());

		// 3. Simula entrega de 50 Vacualite Ores
		qs.giveItems(Quest618IntoTheFlame.VACUALITE_ORE, 50);
		qs.setCond(3);

		quest.onAdvEvent("31271-05.htm", hilda, session);
		assertEquals(4, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest618IntoTheFlame.VACUALITE));

		// 4. Retorno a Klein e recebimento da Floating Stone
		String replyKlein = quest.onTalk(klein, session);
		assertEquals("31540-04.htm", replyKlein);

		quest.onAdvEvent("31540-05.htm", klein, session);
		assertEquals(1, qs.getQuestItemsCount(Quest618IntoTheFlame.FLOATING_STONE));
	}

	@Test
	@DisplayName("CP 5.4: Fluxo da Quest 119 (Last Imperial Prince) para acesso ao Frintezza")
	void testQuest119LastImperialPrinceFlow() {
		Quest119LastImperialPrince quest = new Quest119LastImperialPrince(questManager);

		var spiritTpl = new NpcTemplate(31453, 31453, "Nameless Spirit", false, "Ghost", false, 10.0, 15.0, 75, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance spirit = new NpcInstance(9003, spiritTpl, 0, 0, 0, 0);

		// Inicialmente sem o Antique Brooch: é rejeitado
		String rejectTalk = quest.onTalk(spirit, session);
		assertTrue(rejectTalk.contains("Four Goblets") || rejectTalk.contains("not accomplished"));

		// Adiciona Antique Brooch (7262)
		QuestState qs = session.getQuestState(Quest119LastImperialPrince.QUEST_NAME);
		if (qs == null) {
			qs = quest.newQuestState(session);
		}
		qs.giveItems(Quest119LastImperialPrince.ANTIQUE_BROOCH, 1);

		// Agora pode iniciar
		String talkOk = quest.onTalk(spirit, session);
		assertEquals("31453-1.htm", talkOk);

		quest.onAdvEvent("31453-4.htm", spirit, session);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Diálogo intermediário com Devorin
		var devorinTpl = new NpcTemplate(32009, 32009, "Devorin", false, "Officer", false, 10.0, 15.0, 75, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance devorin = new NpcInstance(9004, devorinTpl, 0, 0, 0, 0);

		quest.onAdvEvent("32009-3.htm", devorin, session);
		assertEquals(2, qs.getCond());

		// Retorno ao Espírito e concessão do Frintezza Scroll
		quest.onAdvEvent("31453-7.htm", spirit, session);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest119LastImperialPrince.FRINTEZZA_SCROLL));
	}

	@Test
	@DisplayName("Fluxo da Quest 337 (Audience with Land Dragon) para obtencao da Portal Stone de Antharas")
	void testQuest337AudienceWithLandDragonFlow() {
		Quest337AudienceWithLandDragon quest = new Quest337AudienceWithLandDragon(questManager);

		var gabrielleTpl = new NpcTemplate(Quest337AudienceWithLandDragon.GABRIELLE, Quest337AudienceWithLandDragon.GABRIELLE,
				"Gabrielle", false, "", false, 10.0, 15.0, 75, "female", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance gabrielle = new NpcInstance(9101, gabrielleTpl, 0, 0, 0, 0);

		// Inicialmente sem quest: dialogo intro
		String intro = quest.onTalk(gabrielle, session);
		assertEquals("gabrielle_intro.htm", intro);

		// Aceita a missao
		quest.onAdvEvent("quest_accept", gabrielle, session);
		QuestState qs = session.getQuestState(Quest337AudienceWithLandDragon.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest337AudienceWithLandDragon.FEATHER_OF_SEEKER));

		// Coleta dentes de Krakian
		var monsterTpl = new NpcTemplate(Quest337AudienceWithLandDragon.CAVE_KEEPER, Quest337AudienceWithLandDragon.CAVE_KEEPER,
				"Cave Keeper", false, "", false, 10.0, 15.0, 75, "male", "L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(9102, monsterTpl, 0, 0, 0, 0);

		for (int i = 0; i < 10; i++) {
			quest.onKill(mob, session, false);
		}
		assertEquals(2, qs.getCond());
		assertEquals(10, qs.getQuestItemsCount(Quest337AudienceWithLandDragon.KRAKIAN_TOOTH));

		// Entrega e recebe Portal Stone
		quest.onAdvEvent("gabrielle_finish", gabrielle, session);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest337AudienceWithLandDragon.PORTAL_STONE));
	}

	@Test
	@DisplayName("Fluxo da Quest 348 (An Arrogant Search) para obtencao da Blooded Fabric de Baium")
	void testQuest348AnArrogantSearchFlow() {
		Quest348AnArrogantSearch quest = new Quest348AnArrogantSearch(questManager);

		var hanellinTpl = new NpcTemplate(Quest348AnArrogantSearch.HANELLIN, Quest348AnArrogantSearch.HANELLIN,
				"Hanellin", false, "", false, 10.0, 15.0, 75, "female", "L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance hanellin = new NpcInstance(9201, hanellinTpl, 0, 0, 0, 0);

		quest.onAdvEvent("quest_accept", hanellin, session);
		QuestState qs = session.getQuestState(Quest348AnArrogantSearch.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Abate Shaman da ToI para Titans Powerstone
		var shamanTpl = new NpcTemplate(Quest348AnArrogantSearch.PLATINUM_TRIBE_SHAMAN, Quest348AnArrogantSearch.PLATINUM_TRIBE_SHAMAN,
				"Platinum Shaman", false, "", false, 10.0, 15.0, 75, "male", "L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance shaman = new NpcInstance(9202, shamanTpl, 0, 0, 0, 0);
		quest.onKill(shaman, session, false);
		assertEquals(2, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest348AnArrogantSearch.TITANS_POWERSTONE));

		// Entrega pedra e recebe White Fabric
		quest.onAdvEvent("deliver_stone", hanellin, session);
		assertEquals(3, qs.getCond());
		assertEquals(1, qs.getQuestItemsCount(Quest348AnArrogantSearch.WHITE_FABRIC));

		// Abate anjo no topo da ToI para ensanguentar o tecido
		var angelTpl = new NpcTemplate(Quest348AnArrogantSearch.GUARDIAN_ANGEL, Quest348AnArrogantSearch.GUARDIAN_ANGEL,
				"Guardian Angel", false, "", false, 10.0, 15.0, 75, "male", "L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance angel = new NpcInstance(9203, angelTpl, 0, 0, 0, 0);
		quest.onKill(angel, session, false);
		assertTrue(qs.isCompleted());
		assertEquals(1, qs.getQuestItemsCount(Quest348AnArrogantSearch.BLOODED_FABRIC));
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
