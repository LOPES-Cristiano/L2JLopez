package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.game.quest.impl.Quest255Tutorial;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList.QuestEntry;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class QuestEngineAndTutorialTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(1001, "TestHero", "HeroTitle", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(1);
		playerChar.classId(0); // Human Fighter

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
	@DisplayName("CP 2.1: Registro de Quest no QuestManager e ciclo de vida básico")
	void testQuestRegistrationAndLifecycle() {
		Quest testQuest = new Quest(999, "Test_Custom_Quest", "Custom Quest Description") {
			@Override
			public String onTalk(NpcInstance npc, GameSession player) {
				QuestState qs = player.getQuestState(getName());
				if (qs == null) {
					qs = newQuestState(player);
					qs.setState(State.STARTED);
					qs.setCond(1);
					return "quest_started.htm";
				} else if (qs.isStarted()) {
					qs.setState(State.COMPLETED);
					return "quest_finished.htm";
				}
				return "already_done.htm";
			}
		};

		questManager.registerQuest(testQuest);
		assertNotNull(questManager.getQuest(999));
		assertEquals("Test_Custom_Quest", questManager.getQuest("Test_Custom_Quest").getName());

		var tpl = new NpcTemplate(30001, 30001, "TestNpc", false, "", false, 10.0, 15.0, 20, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(5001, tpl, 0, 0, 0, 0);

		// Primeiro talk: inicia quest
		String html1 = testQuest.onTalk(npc, session);
		assertEquals("quest_started.htm", html1);
		QuestState qs = session.getQuestState("Test_Custom_Quest");
		assertNotNull(qs);
		assertTrue(qs.isStarted());
		assertEquals(1, qs.getCond());

		// Segundo talk: finaliza quest
		String html2 = testQuest.onTalk(npc, session);
		assertEquals("quest_finished.htm", html2);
		assertTrue(qs.isCompleted());
	}

	@Test
	@DisplayName("CP 2.1: Manipulação de variáveis e condições do QuestState")
	void testQuestStateVariables() {
		Quest q = new Quest(100, "Q100_Variables", "Var Test") {};
		QuestState qs = new QuestState(q, session, State.STARTED);

		qs.set("step", 5);
		assertEquals(5, qs.getInt("step"));
		assertEquals("5", qs.get("step"));

		qs.set("flag", "ready");
		assertEquals("ready", qs.get("flag"));

		qs.unset("flag");
		assertNull(qs.get("flag"));
		assertEquals(0, qs.getInt("flag"));

		qs.setCond(3);
		assertEquals(3, qs.getCond());
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.PlaySound));
	}

	@Test
	@DisplayName("CP 2.2: Fluxo completo da Quest 255 (Tutorial) para Fighter iniciante")
	void testQuest255TutorialFlow() {
		Quest255Tutorial tutorial = new Quest255Tutorial(questManager);

		// 1. Jogador nível 1 entra no mundo
		tutorial.onEnterWorld(session);
		QuestState qs = session.getQuestState(Quest255Tutorial.QUEST_NAME);
		assertNotNull(qs, "QuestState do tutorial deve ser criado no login");
		assertTrue(qs.isStarted());

		// Verifica se pacote TutorialShowQuestionMark com ID 1 foi enviado
		boolean markSent = sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.TutorialShowQuestionMark);
		assertTrue(markSent, "TutorialShowQuestionMark deve ser enviado");

		// 2. Abate do Gremlin inicial (NPC 18342 para Humanos)
		var gremlinTpl = new NpcTemplate(18342, 18342, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance gremlin = new NpcInstance(6001, gremlinTpl, 0, 0, 0, 0);

		tutorial.onKill(gremlin, session, false);
		assertEquals(1, qs.getInt("Gemstone"), "Deve ter dropado a Blue Gemstone");

		// Simula que o item foi adicionado ao inventário para o diálogo com o Newbie Helper
		ItemTemplate gemTpl = ItemTemplate.etc(Quest255Tutorial.BLUE_GEMSTONE, Quest255Tutorial.BLUE_GEMSTONE, "Blue Gemstone", "quest", "asset", 1, "none", 0, false, false, false, false);
		playerChar.inventory().add(new ItemInstance(9001, gemTpl, playerChar.objectId(), 1));
		assertEquals(1, qs.getQuestItemsCount(Quest255Tutorial.BLUE_GEMSTONE));

		// 3. Conversa com o Newbie Helper (NPC 30009)
		var helperTpl = new NpcTemplate(30009, 30009, "Newbie Helper", false, "Helper", false, 10.0, 15.0, 70, "male",
				"L2Npc", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance helper = new NpcInstance(6002, helperTpl, 0, 0, 0, 0);

		String reply = tutorial.onTalk(helper, session);
		assertNotNull(reply);
		assertTrue(reply.contains("reward") || reply.contains("done"), "Diálogo deve conter recompensa");
		assertTrue(qs.isCompleted(), "Tutorial deve estar concluído");
	}

	@Test
	@DisplayName("CP 2.3: Clique no icone de interrogacao (QM1) deve enviar TutorialShowHtml com HTML valido")
	void testQuestionMarkDeliversValidHtmlContent() {
		Quest255Tutorial tutorial = new Quest255Tutorial(questManager);
		tutorial.onEnterWorld(session);
		sentPackets.clear();

		// Simula clique no question mark ID 1
		tutorial.onAdvEvent("QM1", null, session);

		var htmlPacket = sentPackets.stream()
				.filter(p -> p instanceof GameServerPacket.TutorialShowHtml)
				.map(p -> (GameServerPacket.TutorialShowHtml) p)
				.findFirst()
				.orElse(null);

		assertNotNull(htmlPacket, "TutorialShowHtml deve ser enviado");
		assertNotNull(htmlPacket.html());
		assertTrue(htmlPacket.html().contains("<html>") && htmlPacket.html().contains("<body>"),
				"HTML deve conter tags <html> e <body> para nao quebrar NCHtmlFrame no cliente");
		assertFalse(htmlPacket.html().equals("tutorial_02.htm"),
				"Nao deve enviar nome de arquivo bruto");
	}

	@Test
	@DisplayName("CP 2.1: Codificação de QuestList (0x80) com bitmask retail")
	void testQuestListPacketEncoding() {
		List<QuestEntry> list = List.of(
				new QuestEntry(255, 1),
				new QuestEntry(1, 2)
		);
		QuestList packet = new QuestList(list);
		byte[] encoded = packet.encode();

		assertNotNull(encoded);
		// 1 byte opcode (0x80) + 2 bytes size + 2 * (4 bytes id + 4 bytes cond) + 32 bytes mask = 1 + 2 + 16 + 32 = 51 bytes
		assertEquals(51, encoded.length);

		ByteBuffer bb = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		int opcode = bb.get() & 0xFF;
		assertEquals(0x80, opcode);

		int count = bb.getShort() & 0xFFFF;
		assertEquals(2, count);

		int q1Id = bb.getInt();
		int q1Cond = bb.getInt();
		assertEquals(255, q1Id);
		assertEquals(1, q1Cond);

		int q2Id = bb.getInt();
		int q2Cond = bb.getInt();
		assertEquals(1, q2Id);
		assertEquals(2, q2Cond);

		// 32 bytes de máscara de quests completadas
		byte[] mask = new byte[32];
		bb.get(mask);
		assertEquals(32, mask.length);
	}

	@Test
	@DisplayName("CP 2.2: Opcodes de rede retail de Tutorial e Radar (0xa6, 0xa7, 0xa8, 0xa9, 0xf1)")
	void testTutorialAndRadarPacketOpcodeEncodings() {
		// 0xa6 TutorialShowHtml
		byte[] htmlBytes = new GameServerPacket.TutorialShowHtml("<html></html>").encode();
		assertNotNull(htmlBytes);
		assertEquals(0xa6, htmlBytes[0] & 0xFF, "Opcode de TutorialShowHtml deve ser 0xa6");

		// 0xa7 TutorialShowQuestionMark
		byte[] qmBytes = new GameServerPacket.TutorialShowQuestionMark(1).encode();
		assertNotNull(qmBytes);
		assertEquals(0xa7, qmBytes[0] & 0xFF, "Opcode de TutorialShowQuestionMark deve ser 0xa7");

		// 0xa8 TutorialEnableClientEvent
		byte[] ceBytes = new GameServerPacket.TutorialEnableClientEvent(2).encode();
		assertNotNull(ceBytes);
		assertEquals(0xa8, ceBytes[0] & 0xFF, "Opcode de TutorialEnableClientEvent deve ser 0xa8");

		// 0xa9 TutorialCloseHtml
		byte[] closeBytes = new GameServerPacket.TutorialCloseHtml().encode();
		assertNotNull(closeBytes);
		assertEquals(0xa9, closeBytes[0] & 0xFF, "Opcode de TutorialCloseHtml deve ser 0xa9");

		// 0xf1 RadarControl
		byte[] radarBytes = new GameServerPacket.RadarControl(0, 1, 100, 200, 300).encode();
		assertNotNull(radarBytes);
		assertEquals(0xf1, radarBytes[0] & 0xFF, "Opcode de RadarControl deve ser 0xf1");
	}

	@Test
	@DisplayName("CP 2.4: Suporte multi-raças e radar correto para Elfos, Orcs e Anões")
	void testMultiRaceTutorialAndRadar() {
		Quest255Tutorial tutorial = new Quest255Tutorial(questManager);

		playerChar.classId(25); // Elven Mage
		tutorial.onEnterWorld(session);
		QuestState qs = session.getQuestState(Quest255Tutorial.QUEST_NAME);
		assertNotNull(qs);
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.PlaySound ps && ps.soundFile().equals("tutorial_voice_001d")),
				"Deve tocar tutorial_voice_001d para Elven Mage");

		sentPackets.clear();
		tutorial.onAdvEvent("QM1", null, session);
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.RadarControl rc && rc.x() == 46112 && rc.y() == 41200),
				"Radar de Elfo deve apontar para as coordenadas de Elven Village");

		// Testa Client Event CE1 (movimento)
		sentPackets.clear();
		tutorial.onAdvEvent("CE1", null, session);
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof GameServerPacket.TutorialEnableClientEvent te && te.eventId() == 2),
				"CE1 deve disparar TutorialEnableClientEvent(2)");
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
