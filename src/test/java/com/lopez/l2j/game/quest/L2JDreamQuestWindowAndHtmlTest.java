package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class L2JDreamQuestWindowAndHtmlTest {

	private QuestManager questManager;
	private HtmCache htmCache;
	private GameWorld gameWorld;
	private CharacterService characterService;
	private InventoryService inventoryService;
	private GameSession.Context context;
	private GameSession session;
	private PlayerCharacter player;
	private final List<GameServerPacket> sentPackets = new ArrayList<>();

	@BeforeEach
	void setUp() {
		sentPackets.clear();
		questManager = new QuestManager(null);
		htmCache = new HtmCache("data/html");

		gameWorld = Mockito.mock(GameWorld.class);
		characterService = Mockito.mock(CharacterService.class);
		inventoryService = Mockito.mock(InventoryService.class);

		context = Mockito.mock(GameSession.Context.class);
		Mockito.when(context.questManager()).thenReturn(questManager);
		Mockito.when(context.htmls()).thenReturn(htmCache);
		Mockito.when(context.world()).thenReturn(gameWorld);
		Mockito.when(context.characters()).thenReturn(characterService);
		Mockito.when(context.inventories()).thenReturn(inventoryService);

		player = new PlayerCharacter(1001, "TestHero", "HeroTitle", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.level(20);
		player.classId(0);

		session = new GameSession(context, new byte[16], "127.0.0.1", sentPackets::add);
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

	private static Object invokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
		try {
			var m = target.getClass().getDeclaredMethod(name, paramTypes);
			m.setAccessible(true);
			return m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("1. QuestMessage provê títulos canônicos no padrão L2JDream")
	void testQuestMessageTitles() {
		assertEquals("Letters of Love", QuestMessage.getTitleById(1));
		assertEquals("What Women Want", QuestMessage.getTitleById(2));
		assertEquals("Tutorial", QuestMessage.getTitleById(201));
		assertEquals("A Grand Plan for Taming Wild Beasts", QuestMessage.getTitleById(655));
		assertEquals("Seekers of the Holy Grail", QuestMessage.getTitleById(638));
		assertEquals("Defeat the Elrokian Raiders!", QuestMessage.getTitleById(688));

		// Cria uma quest sem descr explícito e verifica que ela adota o título canônico do QuestMessage
		Quest customQ = new Quest(655, "655_AGrandPlanForTamingWildBeasts", "") {
		};
		assertEquals("A Grand Plan for Taming Wild Beasts", customQ.getDescr());
	}

	@Test
	@DisplayName("2. NPC com múltiplas quests exibe menu no formato oficial do L2JDream [Quest (In progress)]")
	void testMultipleQuestsChooseWindow() {
		int npcId = 30006;
		var npcTpl = new NpcTemplate(npcId, npcId, "TestNpc", false, "L2Npc", false, 10.0, 15.0, 50, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(7001, npcTpl, 0, 0, 0, 0);
		Mockito.when(gameWorld.npc(7001)).thenReturn(Optional.of(npc));

		Quest q1 = new Quest(1, "001_LettersOfLove", "Letters of Love") {
		};
		q1.addStartNpc(npcId);
		questManager.registerQuest(q1);

		Quest q2 = new Quest(2, "002_WhatWomenWant", "What Women Want") {
		};
		q2.addStartNpc(npcId);
		questManager.registerQuest(q2);

		// Jogador já iniciou q1
		QuestState qs1 = q1.newQuestState(session);
		qs1.setState(State.STARTED);
		qs1.set("cond", "1");

		// Clica no link de Quest do NPC
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_7001_Quest"));

		NpcHtmlMessage msg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();

		String html = msg.html();
		// Formato do L2JDream: <a action="bypass -h npc_%objectId%_Quest <q.getName()>"> [<q.getDescr()> (<status>)]</a><br>
		assertTrue(html.contains("<a action=\"bypass -h npc_7001_Quest 001_LettersOfLove\"> [Letters of Love (In progress)]</a><br>"),
				"HTML deve conter q1 no formato L2JDream: " + html);
		assertTrue(html.contains("<a action=\"bypass -h npc_7001_Quest 002_WhatWomenWant\"> [What Women Want]</a><br>"),
				"HTML deve conter q2 no formato L2JDream: " + html);
	}

	@Test
	@DisplayName("3. NPC sem quests para o jogador exibe mensagem canônica MSG_NO_QUEST do L2JDream")
	void testNoQuestMessage() {
		int npcId = 30006;
		var npcTpl = new NpcTemplate(npcId, npcId, "TestNpc", false, "L2Npc", false, 10.0, 15.0, 50, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(7002, npcTpl, 0, 0, 0, 0);
		Mockito.when(gameWorld.npc(7002)).thenReturn(Optional.of(npc));

		// Clica no link de Quest sem nenhuma quest registrada para este NPC
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_7002_Quest"));

		NpcHtmlMessage msg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();

		assertTrue(msg.html().contains("You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements."),
				"Deve exibir mensagem padrão de no-quest do L2JDream");
	}

	@Test
	@DisplayName("4. showNpcHtml exibe diálogo normal do NPC sem chamar onNpcTalk prematuramente")
	void testShowNpcHtmlDoesNotHijackDialog() {
		int npcId = 30001; // Guard / Npc com diálogo normal
		var npcTpl = new NpcTemplate(npcId, npcId, "Guard", false, "L2Guard", false, 10.0, 15.0, 50, "male",
				"L2Guard", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(7003, npcTpl, 0, 0, 0, 0);
		Mockito.when(gameWorld.npc(7003)).thenReturn(Optional.of(npc));

		// Registra uma quest onde o npcId responde talk
		Quest q = new Quest(999, "999_Test", "Test Quest") {
			@Override
			public String onTalk(NpcInstance talkNpc, QuestState qs) {
				return "<html><body>Quest Dialog Hijacked!</body></html>";
			}
		};
		q.addTalkNpc(npcId);
		questManager.registerQuest(q);

		// Jogador clica no NPC no mundo
		session.showNpcHtml(npc, 0);

		NpcHtmlMessage msg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();

		// O diálogo normal do NPC deve ser exibido, e NÃO "Quest Dialog Hijacked!"
		assertFalse(msg.html().contains("Quest Dialog Hijacked!"),
				"showNpcHtml não deve executar onTalk e sequestrar o diálogo principal do NPC");
	}

	@Test
	@DisplayName("5. Mozella Cruma Tower: nível alto exibe 30483-biglvl.htm e nível válido teleporta")
	void testCrumaTowerMozellaTeleportAndLevelRestriction() {
		int npcId = 30483; // Gatekeeper Mozella
		var npcTpl = new NpcTemplate(npcId, npcId, "Mozella", false, "L2Teleporter", false, 10.0, 15.0, 50, "female",
				"L2Teleporter", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(7004, npcTpl, 0, 0, 0, 0);
		Mockito.when(gameWorld.npc(7004)).thenReturn(Optional.of(npc));

		// 1. Jogador level 76 (acima de 56) falando com Mozella
		player.level(76);
		player.accessLevel(0);
		sentPackets.clear();
		session.showNpcHtml(npc, 0);

		NpcHtmlMessage bigMsg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();
		assertTrue(bigMsg.html().contains("your level is too high!"), "Deve exibir diálogo 30483-biglvl.htm para level alto");
		assertFalse(bigMsg.html().contains("I have nothing to say to you"), "Não deve cair em npcdefault");

		// 2. Jogador level 76 tenta bypass 1108_CrumaTower
		sentPackets.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_7004_Quest 1108_CrumaTower"));
		NpcHtmlMessage bypassBigMsg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();
		assertTrue(bypassBigMsg.html().contains("your level is too high!"), "Bypass deve retornar 30483-biglvl.htm");

		// 3. Jogador level 40 (apto) fala com Mozella
		player.level(40);
		sentPackets.clear();
		session.showNpcHtml(npc, 0);
		NpcHtmlMessage normalMsg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();
		assertTrue(normalMsg.html().contains("Teleport into the tower"), "Level permitido deve exibir opção de teleport");

		// 4. Jogador level 40 clica em Teleport into the tower
		sentPackets.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_7004_Quest 1108_CrumaTower"));
		assertEquals(17724, player.x());
		assertEquals(114004, player.y());
		assertEquals(-11672, player.z());
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.TeleportToLocation),
				"Deve teleportar para o 1º andar da Cruma Tower");
	}
}
