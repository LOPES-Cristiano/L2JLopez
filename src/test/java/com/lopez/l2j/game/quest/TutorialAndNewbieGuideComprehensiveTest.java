package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest201HfighterTutorial;
import com.lopez.l2j.game.quest.impl.Quest255Tutorial;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.TutorialCloseHtml;
import com.lopez.l2j.network.game.packet.GameServerPacket.TutorialShowHtml;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TutorialAndNewbieGuideComprehensiveTest {

	private QuestManager questManager;
	private HtmCache htmCache;
	private SkillTable skillTable;
	private SkillService skillService;
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

		skillTable = Mockito.mock(SkillTable.class);
		skillService = Mockito.mock(SkillService.class);
		Mockito.when(skillService.table()).thenReturn(skillTable);

		// Skill Wind Walk 1204
		var statFunc = new StatFunc("runSpd", StatFunc.Op.ADD, 0x30, 20.0);
		var effect = new SkillTemplate.EffectTemplate("Buff", 1, 1200000, 0, "speed_up", 1.0, List.of(statFunc));
		SkillTemplate windWalk = new SkillTemplate(
				1204, 1, "Wind Walk", SkillTemplate.OperateType.ACTIVE, "BUFF",
				"TARGET_ONE", false, 10, 0, 0, 0.0,
				600, 0, 1500, 500, 0, 1,
				0.0, false, 0, 0,
				List.of(statFunc), List.of(effect), null, null
		);
		Mockito.when(skillTable.get(1204, 1)).thenReturn(Optional.of(windWalk));

		gameWorld = Mockito.mock(GameWorld.class);
		characterService = Mockito.mock(CharacterService.class);
		inventoryService = Mockito.mock(InventoryService.class);

		context = Mockito.mock(GameSession.Context.class);
		Mockito.when(context.questManager()).thenReturn(questManager);
		Mockito.when(context.htmls()).thenReturn(htmCache);
		Mockito.when(context.skillService()).thenReturn(skillService);
		Mockito.when(context.world()).thenReturn(gameWorld);
		Mockito.when(context.characters()).thenReturn(characterService);
		Mockito.when(context.inventories()).thenReturn(inventoryService);

		player = new PlayerCharacter(1001, "TestHero", "HeroTitle", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.level(1);
		player.classId(0); // Human Fighter

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
	@DisplayName("1. HtmCache converte sintaxe de colchetes [cmd|texto] em links HTML clicaveis")
	void testBracketSyntaxConversion() {
		String raw1 = "Welcome! [npc_%objectId%_Quest|Quest]";
		String rendered1 = htmCache.render(raw1, 12345, "Guide", "TestHero");
		assertEquals("Welcome! <a action=\"bypass -h npc_12345_Quest\">Quest</a>", rendered1);

		String raw2 = "[link TE00|Close Window]";
		String rendered2 = htmCache.render(raw2, 12345, "Guide", "TestHero");
		assertEquals("<a action=\"link TE00\">Close Window</a>", rendered2);

		String raw3 = "[bypass -h Quest _201_HfighterTutorial reply_31|I brought the Recommendation.]";
		String rendered3 = htmCache.render(raw3, 12345, "Guide", "TestHero");
		assertEquals("<a action=\"bypass -h Quest _201_HfighterTutorial reply_31\">I brought the Recommendation.</a>", rendered3);
	}

	@Test
	@DisplayName("2. Quest 255 entrega Tutorial Guide uma unica vez e salva flag onlyone=1")
	void testQuest255SingleTutorialGuideAndSave() {
		Quest255Tutorial q255 = new Quest255Tutorial(questManager);

		// Primeiro login: nível 1
		q255.onEnterWorld(session);
		QuestState qs = session.getQuestState(Quest255Tutorial.QUEST_NAME);
		assertNotNull(qs);
		assertEquals(1, qs.getInt("onlyone"));
		assertEquals(1, qs.getQuestItemsCount(Quest255Tutorial.TUTORIAL_GUIDE));

		// Segundo login (simulando relog)
		q255.onEnterWorld(session);
		// Não deve duplicar o Tutorial Guide
		assertEquals(1, qs.getQuestItemsCount(Quest255Tutorial.TUTORIAL_GUIDE));
	}

	@Test
	@DisplayName("3. Quest 255 Question Mark 5 exibe tutorial_11.htm com link de fechar e TE00 fecha a janela")
	void testQuest255QuestionMark5AndClose() {
		Quest255Tutorial q255 = new Quest255Tutorial(questManager);
		q255.onEnterWorld(session);
		sentPackets.clear();

		// Clica no Question Mark 5
		questManager.onTutorialQuestionMark(session, 5);

		boolean htmlSent = sentPackets.stream().anyMatch(p -> p instanceof TutorialShowHtml);
		assertTrue(htmlSent, "TutorialShowHtml deve ser enviado para QM5");

		TutorialShowHtml htmlPkt = (TutorialShowHtml) sentPackets.stream()
				.filter(p -> p instanceof TutorialShowHtml)
				.findFirst()
				.orElseThrow();
		assertTrue(htmlPkt.html().contains("Blue Gemstone"), "Deve conter mensagem sobre Blue Gemstone");
		assertTrue(htmlPkt.html().contains("TE00") || htmlPkt.html().contains("Close Window"), "Deve conter link de fechar TE00");

		// Clica no link TE00 para fechar a janela
		sentPackets.clear();
		questManager.onTutorialLink(session, "link TE00");
		boolean closed = sentPackets.stream().anyMatch(p -> p instanceof TutorialCloseHtml);
		assertTrue(closed, "TutorialCloseHtml deve ser enviado ao acionar TE00");
	}

	@Test
	@DisplayName("4. Quest 201 Fluxo Completo: Carl -> Gremlin -> Blue Gemstone -> Carl -> Roien")
	void testQuest201CompleteTutorialFlow() {
		Quest201HfighterTutorial q201 = new Quest201HfighterTutorial(questManager);
		Quest255Tutorial q255 = new Quest255Tutorial(questManager);
		q255.onEnterWorld(session);

		var carlTpl = new NpcTemplate(30009, 30009, "Newbie Helper", false, "L2NewbieHelper", false, 10.0, 15.0, 70, "male",
				"L2NewbieHelper", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance carl = new NpcInstance(6001, carlTpl, 0, 0, 0, 0);

		var roienTpl = new NpcTemplate(30008, 30008, "Grand Master Roien", false, "L2Trainer", false, 10.0, 15.0, 70, "male",
				"L2Trainer", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance roien = new NpcInstance(6002, roienTpl, 0, 0, 0, 0);

		var gremlinTpl = new NpcTemplate(18342, 18342, "Gremlin", false, "L2Monster", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance gremlin = new NpcInstance(6003, gremlinTpl, 0, 0, 0, 0);

		// Passo A: Primeiro diálogo com o Newbie Helper Carl
		String carlFirstHtml = q201.notifyFirstTalk(carl, session);
		assertEquals("carl001.htm", carlFirstHtml, "Primeiro dialogo com Carl deve ser carl001.htm");

		QuestState qs201 = session.getQuestState("201_HfighterTutorial");
		assertNotNull(qs201);
		assertEquals("0", qs201.get("tutorial_quest_ex"));

		// Passo B: Abate do Gremlin
		q201.onKill(gremlin, qs201);
		assertEquals(1, qs201.getQuestItemsCount(6353), "Gremlin deve dropar Blue Gemstone (6353)");

		// Passo C: Retorno a Carl com a Blue Gemstone
		String carlSecondHtml = q201.notifyFirstTalk(carl, session);
		assertEquals("carl003.htm", carlSecondHtml, "Carl deve agradecer e entregar Recommendation Letter");
		assertEquals(0, qs201.getQuestItemsCount(6353), "Blue Gemstone deve ser recolhida");
		assertEquals(1, qs201.getQuestItemsCount(1067), "Deve receber a Recommendation Letter (1067)");
		assertEquals(200, qs201.getQuestItemsCount(5789), "Fighter deve receber 200 Soulshots de novato");
		assertEquals("3", qs201.get("tutorial_quest_ex"));

		// Passo D: Fala com Grand Master Roien
		String roienFirstHtml = q201.notifyFirstTalk(roien, session);
		assertEquals("roien001.htm", roienFirstHtml, "Roien deve reconhecer a Recommendation Letter");

		// Passo E: Roien aceita Recommendation Letter
		String roienReply31 = q201.notifyEvent("reply_31", roien, session);
		assertEquals("roien002.htm", roienReply31);
		assertEquals(0, qs201.getQuestItemsCount(1067), "Recommendation Letter deve ser entregue");
		assertEquals("4", qs201.get("tutorial_quest_ex"));

		// Passo F: Roien finaliza com orientacao da ilha
		String roienReply42 = q201.notifyEvent("reply_42", roien, session);
		assertEquals("roien006.htm", roienReply42);
		assertTrue(qs201.isCompleted(), "Quest 201 deve ser finalizada");
	}

	@Test
	@DisplayName("5. Newbie Guide na cidade entrega diálogo retail e aplica buffs via SupportMagic")
	void testNewbieGuideInTownAndSupportMagic() {
		var guideTpl = new NpcTemplate(30598, 30598, "Newbie Guide", false, "L2NewbieHelper", false, 10.0, 15.0, 70, "male",
				"L2NewbieHelper", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		NpcInstance guide = new NpcInstance(6004, guideTpl, 0, 0, 0, 0);
		Mockito.when(gameWorld.npc(6004)).thenReturn(Optional.of(guide));

		// 1. Jogador nível 8 clica no Newbie Guide
		player.level(8);
		session.showNpcHtml(guide, 0);

		NpcHtmlMessage guideMsg = (NpcHtmlMessage) sentPackets.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.findFirst()
				.orElseThrow();
		assertTrue(guideMsg.html().contains("If you need advice, ask me at any time!"), "HTML recebido: " + guideMsg.html());
		assertTrue(guideMsg.html().contains("SupportMagic"), "Deve conter o link de SupportMagic");

		// 2. Aciona bypass npc_%objectId%_SupportMagic
		session.targetObjectId(guide.objectId());
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_" + guide.objectId() + "_SupportMagic"));

		// Jogador deve estar com o buff de Wind Walk aplicado
		assertTrue(player.effects().hasSkill(1204), "Jogador deve receber o buff de Wind Walk (1204)");
	}
}
