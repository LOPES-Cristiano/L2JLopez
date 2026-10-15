package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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

/**
 * RetailNpcScriptsIntegrationTest: Validação unitária e de ciclo de vida das 14 classes
 * de Teleporters e NPCs Retail portadas na Onda F3.
 */
class RetailNpcScriptsIntegrationTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter player;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		player = new PlayerCharacter(1001, "TestHero", "Candidate", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.level(10);
		player.classId(0);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockInvSvc = org.mockito.Mockito.mock(InventoryService.class);
		var mockWorld = org.mockito.Mockito.mock(com.lopez.l2j.game.world.GameWorld.class);
		var mockCharSvc = org.mockito.Mockito.mock(com.lopez.l2j.game.service.CharacterService.class);

		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(mockInvSvc);
		org.mockito.Mockito.when(mockCtx.questManager()).thenReturn(questManager);
		org.mockito.Mockito.when(mockCtx.world()).thenReturn(mockWorld);
		org.mockito.Mockito.when(mockCtx.characters()).thenReturn(mockCharSvc);
		org.mockito.Mockito.when(mockWorld.players()).thenReturn(java.util.Collections.emptyList());

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", sentPackets::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
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

	private static NpcInstance createNpc(int npcId, String name) {
		var tpl = new NpcTemplate(npcId, npcId, name, false, "Title", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		return new NpcInstance(8000 + npcId % 1000, tpl, 0, 0, 0, 0);
	}

	@Test
	@DisplayName("1. Quest1104NewbieTravelToken - Consumo e teleporte nível < 20")
	void testQuest1104NewbieTravelToken() {
		Quest1104NewbieTravelToken q = new Quest1104NewbieTravelToken(questManager);

		assertEquals(1104, q.getQuestId());
		assertTrue(q.hasStartNpc(30598));
		assertTrue(q.hasTalkNpc(30601));

		QuestState qs = q.newQuestState(session);
		player.level(10);
		NpcInstance npc = createNpc(30598, "NewbieGuide");

		assertEquals("30598.htm", q.onTalk(npc, qs));

		player.level(25);
		assertEquals("1.htm", q.onTalk(npc, qs));

		player.level(10);
		assertEquals("Incorrect item count", q.onAdvEvent("30600", npc, session));

		qs.giveItems(Quest1104NewbieTravelToken.TOKEN, 1);
		assertNull(q.onAdvEvent("30600", npc, session));
		assertEquals(0, qs.getQuestItemsCount(Quest1104NewbieTravelToken.TOKEN));
	}

	@Test
	@DisplayName("2. Quest1630PaganTeleporters - Portas e espelhos de Pagan Temple")
	void testQuest1630PaganTeleporters() {
		Quest1630PaganTeleporters q = new Quest1630PaganTeleporters(questManager, null);

		assertEquals(1630, q.getQuestId());
		assertTrue(q.hasStartNpc(32034));
		assertTrue(q.hasFirstTalkNpc(32039));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(32034, "PaganGate");

		// Sem visitor mark -> Mark1.htm
		assertEquals("Mark1.htm", q.onTalk(npc, qs));

		// Com visitor mark -> FadedMark.htm
		qs.giveItems(Quest1630PaganTeleporters.VISITOR_MARK, 1);
		assertEquals("FadedMark.htm", q.onTalk(npc, qs));

		// Primeiro talk em 32039 -> teleporte para coordenadas de Pagan
		NpcInstance mirrorNpc = createNpc(32039, "TriolsMirror");
		assertEquals("", q.onFirstTalk(mirrorNpc, qs));
		assertEquals(-12766, player.x());
		assertEquals(-35840, player.y());
		assertEquals(-10856, player.z());
	}

	@Test
	@DisplayName("3. Quest2000NoblesseTeleport - Verificação estrita de Nobreza")
	void testQuest2000NoblesseTeleport() {
		Quest2000NoblesseTeleport q = new Quest2000NoblesseTeleport(questManager);

		assertEquals(2000, q.getQuestId());
		assertTrue(q.hasStartNpc(30006));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(30006, "Gatekeeper");

		player.setNoble(false);
		assertEquals("nobleteleporter-no.htm", q.onTalk(npc, qs));

		player.setNoble(true);
		assertEquals("noble.htm", q.onTalk(npc, qs));
	}

	@Test
	@DisplayName("4. Quest2211HuntingGroundsTeleport - Validação de cabal do Seven Signs")
	void testQuest2211HuntingGroundsTeleport() {
		Quest2211HuntingGroundsTeleport q = new Quest2211HuntingGroundsTeleport(questManager, null);

		assertEquals(2211, q.getQuestId());
		assertTrue(q.hasStartNpc(31078));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(31078, "DawnPriest");

		// Sem cabal -> dawn_tele-no.htm
		assertEquals("dawn_tele-no.htm", q.onTalk(npc, qs));
	}

	@Test
	@DisplayName("5. Quest1201NewbieToken - Cupons de iniciante e abertura de multisell")
	void testQuest1201NewbieToken() {
		Quest1201NewbieToken q = new Quest1201NewbieToken(questManager, null);

		assertEquals(1201, q.getQuestId());
		assertTrue(q.hasStartNpc(30598));

		NpcInstance npc = createNpc(30598, "NewbieGuide");

		player.level(10);
		player.pkKills(0);
		player.setNewbie(0);

		// Pedir cupons de arma
		assertEquals("2.htm", q.onAdvEvent("newbie_give_weapon_coupon", npc, session));
		QuestState qs = q.checkQuestState(session);
		assertNotNull(qs);
		assertEquals(5, qs.getQuestItemsCount(Quest1201NewbieToken.COUPON_ONE));

		// Já recebeu -> 3.htm
		assertEquals("3.htm", q.onAdvEvent("newbie_give_weapon_coupon", npc, session));
	}

	@Test
	@DisplayName("6. Quest4000ShadowWeapons - Validação de cupons D e C")
	void testQuest4000ShadowWeapons() {
		Quest4000ShadowWeapons q = new Quest4000ShadowWeapons(questManager);

		assertEquals(4000, q.getQuestId());
		assertTrue(q.hasStartNpc(30037));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(30037, "Master");

		// Sem cupons -> exchange-no.htm
		assertEquals("exchange-no.htm", q.onAdvEvent("", npc, session));

		// Com cupom D -> abre multisell (retorna null)
		qs.giveItems(Quest4000ShadowWeapons.D_COUPON, 1);
		assertNull(q.onAdvEvent("", npc, session));
	}

	@Test
	@DisplayName("7. Quest5000MissQueen - Entrega para iniciantes e viajantes")
	void testQuest5000MissQueen() {
		Quest5000MissQueen q = new Quest5000MissQueen(questManager, null);

		assertEquals(5000, q.getQuestId());
		assertTrue(q.hasStartNpc(31760));

		NpcInstance npc = createNpc(31760, "MissQueen");

		player.level(15);
		player.pkKills(0);
		player.setNewbie(0);

		assertEquals("31760-2.htm", q.onAdvEvent("newbie_give_coupon", npc, session));
		QuestState qs = q.checkQuestState(session);
		assertNotNull(qs);
		assertEquals(1, qs.getQuestItemsCount(Quest5000MissQueen.COUPON_ONE));
	}

	@Test
	@DisplayName("8. Quest6050 & 6051 Ketra e Varka Support - Buffs por aliança")
	void testKetraAndVarkaSupport() {
		Quest6050KetraOrcSupport ketra = new Quest6050KetraOrcSupport(questManager, null);
		Quest6051VarkaSilenosSupport varka = new Quest6051VarkaSilenosSupport(questManager, null);

		assertEquals(6050, ketra.getQuestId());
		assertEquals(6051, varka.getQuestId());

		// Teste Ketra
		QuestState qsKetra = ketra.newQuestState(session);
		player.setAllianceWithVarkaKetra(3);
		NpcInstance asefa = createNpc(Quest6050KetraOrcSupport.ASEFA, "Asefa");

		assertEquals("31372-2.htm", ketra.onFirstTalk(asefa, qsKetra));
		qsKetra.giveItems(Quest6050KetraOrcSupport.HORN, 10);
		assertEquals("31372-4.htm", ketra.onFirstTalk(asefa, qsKetra));

		// Comprar Buff Focus (custo 2)
		assertEquals("31372-4.htm", ketra.onAdvEvent("1", asefa, session));
		assertEquals(8, qsKetra.getQuestItemsCount(Quest6050KetraOrcSupport.HORN));
	}

	@Test
	@DisplayName("9. Quest6999 & 7000 Hero Circlet e Hero Items")
	void testHeroCircletAndItems() {
		Quest6999HeroCirclet circlet = new Quest6999HeroCirclet(questManager);
		Quest7000HeroItems items = new Quest7000HeroItems(questManager);

		assertEquals(6999, circlet.getQuestId());
		assertEquals(7000, items.getQuestId());

		QuestState qsCirclet = circlet.newQuestState(session);
		NpcInstance monument = createNpc(31690, "Monument");

		// Não é hero
		player.setHero(false);
		assertTrue(circlet.onTalk(monument, qsCirclet).contains("You're not a Hero"));

		// Vira hero
		player.setHero(true);
		assertTrue(circlet.onTalk(monument, qsCirclet).contains("Enjoy your Wings of Destiny Circlet"));
		assertEquals(1, qsCirclet.getQuestItemsCount(Quest6999HeroCirclet.CIRCLET));

		// Obter Arma Heroica
		QuestState qsItems = items.newQuestState(session);
		String res = items.onAdvEvent("_6611", monument, session);
		assertTrue(res.contains("Enjoy your Infinity Blade"));
		assertEquals(1, qsItems.getQuestItemsCount(6611));
	}

	@Test
	@DisplayName("10. Quest7001 Clan Items - Verificação de Clan Leader")
	void testClanItems() {
		Quest7001ClanItems q = new Quest7001ClanItems(questManager);

		assertEquals(7001, q.getQuestId());
		assertTrue(q.hasStartNpc(32024));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(32024, "ClanTrader");

		player.setClanLeader(false);
		assertEquals("32024-no.htm", q.onFirstTalk(npc, qs));

		player.setClanLeader(true);
		assertEquals("32024.htm", q.onFirstTalk(npc, qs));
	}

	@Test
	@DisplayName("11. Quest8002 AskForAdvise - Filtragem por raça")
	void testAskForAdvise() {
		Quest8002AskForAdvise q = new Quest8002AskForAdvise(questManager);

		assertEquals(8002, q.getQuestId());
		assertTrue(q.hasStartNpc(30598));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(30598, "HumanGuide");

		player.setRace(Race.human);
		assertEquals("30598.htm", q.onTalk(npc, qs));

		player.setRace(Race.elf);
		assertEquals("30598-no.htm", q.onTalk(npc, qs));
	}

	@Test
	@DisplayName("12. Quest8003 MeetBaium - Consumo de Blooded Fabric e teleporte")
	void testMeetBaium() {
		Quest8003MeetBaium q = new Quest8003MeetBaium(questManager, null);

		assertEquals(8003, q.getQuestId());
		assertTrue(q.hasStartNpc(Quest8003MeetBaium.ANGELIC_VORTEX));

		QuestState qs = q.newQuestState(session);
		NpcInstance npc = createNpc(Quest8003MeetBaium.ANGELIC_VORTEX, "Vortex");

		// Sem Blooded Fabric -> 31862-no.htm
		assertEquals("31862-no.htm", q.onFirstTalk(npc, qs));
		assertEquals("31862-no.htm", q.onAdvEvent("baium", npc, session));

		// Com Blooded Fabric -> teleporte ao 14º andar
		qs.giveItems(Quest8003MeetBaium.BLOODED_FABRIC, 1);
		assertEquals("31862.htm", q.onFirstTalk(npc, qs));
		assertNull(q.onAdvEvent("baium", npc, session));
		assertEquals(0, qs.getQuestItemsCount(Quest8003MeetBaium.BLOODED_FABRIC));
		assertEquals(116033, player.x());
		assertEquals(17447, player.y());
		assertEquals(10107, player.z());
	}
}
