package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementação nativa da Quest 255 (Tutorial) e suporte integrado a Newbie Helpers das 5 raças.
 * Portada com fidelidade aos scripts retail Interlude e base L2JDream.
 */
@Component
public class Quest255Tutorial extends Quest {

	private static final Logger log = LoggerFactory.getLogger(Quest255Tutorial.class);

	public static final int QUEST_ID = 255;
	public static final String QUEST_NAME = "255_Tutorial";

	// Itens Retail do Tutorial
	public static final int TUTORIAL_GUIDE = 5588;
	public static final int BLUE_GEMSTONE = 6353;
	public static final int SOULSHOT_NOVICE = 5789;
	public static final int SPIRITSHOT_NOVICE = 5790;
	public static final int RECOMMENDATION_01 = 1067;
	public static final int RECOMMENDATION_02 = 1068;
	public static final int LEAF_OF_MOTHERTREE = 1069;
	public static final int BLOOD_OF_JUNDIN = 1070;
	public static final int LICENSE_OF_MINER = 1498;
	public static final int VOUCHER_OF_FLAME = 1496;

	// Monstros Iniciais de Caça
	private static final Set<Integer> TUTORIAL_MOBS = Set.of(
			18342, 20001, 20130, // Talking Island (Gremlin / Keltir)
			20417,               // Elven Village (Keltir)
			20418,               // Dark Elven Village (Keltir)
			20419,               // Orc Village (Keltir)
			20420                // Dwarven Village (Keltir)
	);

	// Newbie Guides / Helpers
	private static final Set<Integer> NEWBIE_GUIDES = Set.of(
			30008, 30017, // Human
			30370,        // Elf
			30129,        // Dark Elf
			30573,        // Orc
			30528         // Dwarf
	);

	private static final Set<Integer> NEWBIE_HELPERS = Set.of(
			30009, 30011, 30012, 30018, 30019, 30020, 30021, 30056, // Human
			30400, 30401, 30402, 30403,                             // Elf
			30131, 30404,                                           // Dark Elf
			30574, 30575,                                           // Orc
			30530                                                   // Dwarf
	);

	@Autowired
	public Quest255Tutorial(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Tutorial");

		registerQuestItems(TUTORIAL_GUIDE, BLUE_GEMSTONE);

		for (int npcId : NEWBIE_GUIDES) {
			addStartNpc(npcId);
			addTalkId(npcId);
			addFirstTalkId(npcId);
		}

		for (int npcId : NEWBIE_HELPERS) {
			addTalkId(npcId);
			addFirstTalkId(npcId);
		}

		for (int mobId : TUTORIAL_MOBS) {
			addKillId(mobId);
		}

		questManager.registerQuest(this);
	}

	@Override
	public void onEnterWorld(GameSession player) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		PlayerCharacter c = player.activeChar();
		if (c.level() >= 6) {
			return;
		}
		QuestState qs = newQuestState(player);
		if (qs.isCreated()) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("ucMemo", 0);
			String voice = switch (c.classId()) {
				case 0 -> "tutorial_voice_001a"; // Human Fighter
				case 10 -> "tutorial_voice_001b"; // Human Mystic
				case 18 -> "tutorial_voice_001c"; // Elven Fighter
				case 25 -> "tutorial_voice_001d"; // Elven Mystic
				case 31 -> "tutorial_voice_001e"; // Dark Elven Fighter
				case 38 -> "tutorial_voice_001f"; // Dark Elven Mystic
				case 44 -> "tutorial_voice_001g"; // Orc Fighter
				case 49 -> "tutorial_voice_001h"; // Orc Mystic
				case 53 -> "tutorial_voice_001i"; // Dwarven Fighter
				default -> "tutorial_voice_001a";
			};
			qs.playTutorialVoice(voice);
			qs.showQuestionMark(1);
			qs.playSound("ItemSound.quest_tutorial");
		}
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		if (npc == null || player == null || player.activeChar() == null) {
			return null;
		}
		if (!TUTORIAL_MOBS.contains(npc.npcId())) {
			return null;
		}
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs != null && qs.isStarted() && qs.getQuestItemsCount(BLUE_GEMSTONE) == 0) {
			qs.giveItems(BLUE_GEMSTONE, 1);
			qs.set("Gemstone", 1);
			qs.playSound("ItemSound.quest_tutorial");
			qs.playTutorialVoice("tutorial_voice_013");
			qs.showQuestionMark(5);
		}
		return null;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		if (npc == null || player == null || player.activeChar() == null) {
			return null;
		}
		PlayerCharacter c = player.activeChar();
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		int npcId = npc.npcId();

		// Se tem a Blue Gemstone e fala com o Newbie Guide ou Helper
		if (qs.getQuestItemsCount(BLUE_GEMSTONE) > 0) {
			qs.takeItems(BLUE_GEMSTONE, 1);
			boolean isMage = isMageClass(c.classId());

			if (isMage) {
				qs.giveItems(SPIRITSHOT_NOVICE, 100);
			} else {
				qs.giveItems(SOULSHOT_NOVICE, 200);
			}
			qs.addExpAndSp(100, 50);
			qs.setState(State.COMPLETED);
			qs.playSound("ItemSound.quest_finish");

			return "<html><body>Newbie Guide:<br>Very well done! Here is your reward. May your journey in the world of Aden be glorious!</body></html>";
		}

		if (qs.isCompleted()) {
			return "<html><body>Newbie Guide:<br>You have already completed your basic training. Venture forth and speak with the Guild Masters in town!</body></html>";
		}

		return "<html><body>Newbie Guide:<br>Greetings, young adventurer! Defeat a nearby monster to recover a Blue Gemstone and return to me.</body></html>";
	}

	@Override
	public String onFirstTalk(NpcInstance npc, GameSession player) {
		return onTalk(npc, player);
	}

	@Override
	public void onPlayerLevelUp(GameSession player, int oldLevel, int newLevel) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		if (newLevel == 5 && qs.getInt("lvl") < 5) {
			qs.set("lvl", 5);
			qs.playTutorialVoice("tutorial_voice_014");
			qs.playSound("ItemSound.quest_tutorial");
			qs.showQuestionMark(9);
		} else if (newLevel == 6 && qs.getInt("lvl") < 6) {
			qs.set("lvl", 6);
			qs.playTutorialVoice("tutorial_voice_020");
			qs.playSound("ItemSound.quest_tutorial");
			qs.showQuestionMark(24);
		} else if (newLevel == 10 && qs.getInt("lvl") < 10) {
			qs.set("lvl", 10);
			qs.playTutorialVoice("tutorial_voice_030");
			qs.playSound("ItemSound.quest_tutorial");
			qs.showQuestionMark(27);
		} else if (newLevel == 15 && qs.getInt("lvl") < 15) {
			qs.set("lvl", 15);
			qs.playSound("ItemSound.quest_tutorial");
			qs.showQuestionMark(17);
		} else if (newLevel == 19 && qs.getInt("lvl") < 19) {
			qs.set("lvl", 19);
			qs.playSound("ItemSound.quest_tutorial");
			qs.showQuestionMark(35);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null) {
			return null;
		}
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		if (event.startsWith("QM")) {
			int markId = 0;
			try {
				markId = Integer.parseInt(event.substring(2));
			} catch (NumberFormatException ignored) {}

			int classId = player.activeChar() != null ? player.activeChar().classId() : 0;
			if (markId == 1) {
				switch (classId) {
					case 0, 10 -> qs.addRadar(-71424, 258336, -3104);
					case 18, 25 -> qs.addRadar(46112, 41200, -3504);
					case 31, 38 -> qs.addRadar(28384, 11056, -4224);
					case 44, 49 -> qs.addRadar(-45032, -113598, -192);
					case 53 -> qs.addRadar(108516, -174026, -400);
					default -> qs.addRadar(-71424, 258336, -3104);
				}
			}

			String file = switch (markId) {
				case 1 -> switch (classId) {
					case 0 -> "tutorial_human_fighter007.htm";
					case 10 -> "tutorial_human_mage007.htm";
					case 18, 25 -> "tutorial_elf007.htm";
					case 31, 38 -> "tutorial_delf007.htm";
					case 44, 49 -> "tutorial_orc007.htm";
					case 53 -> "tutorial_dwarven_fighter007.htm";
					default -> "tutorial_02.htm";
				};
				case 5 -> "tutorial_11.htm";
				case 9 -> isMageClass(classId) ? "tutorial_mage017.htm" : "tutorial_fighter017.htm";
				case 24 -> switch (classId) {
					case 0, 10 -> "tutorial_human009.htm";
					case 18, 25 -> "tutorial_elf009.htm";
					case 31, 38 -> "tutorial_delf009.htm";
					case 44, 49 -> "tutorial_orc009.htm";
					case 53 -> "tutorial_dwarven009.htm";
					default -> "tutorial_human009.htm";
				};
				case 35 -> "tutorial_21.htm";
				default -> "tutorial_02.htm";
			};

			String html = qs.loadTutorialHtml(file);
			if (html == null || html.isBlank()) {
				html = qs.loadTutorialHtml("tutorial_02.htm");
			}
			if (html == null || html.isBlank()) {
				html = "<html><body><center><font color=\"LEVEL\">[Tutorial]</font></center><br>"
						+ "Welcome to Lineage 2!<br><a action=\"link TE07\">Exit the Tutorial</a></body></html>";
			}
			qs.showTutorialHtml(html);
		} else if ("CE1".equalsIgnoreCase(event)) {
			qs.onTutorialClientEvent(2);
		} else if (event.startsWith("TE")) {
			qs.closeTutorialHtml();
		}
		return null;
	}

	private boolean isMageClass(int classId) {
		return classId == 10 || classId == 25 || classId == 38 || classId == 49;
	}
}
