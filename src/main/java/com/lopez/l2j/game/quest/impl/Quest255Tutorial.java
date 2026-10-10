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

		registerQuestItems(TUTORIAL_GUIDE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
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
		if (qs.getInt("onlyone") == 0) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("onlyone", 1);
			qs.set("ucMemo", 0);
			if (qs.getQuestItemsCount(TUTORIAL_GUIDE) == 0) {
				qs.giveItems(TUTORIAL_GUIDE, 1);
			}
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
			String initialHtm = switch (c.classId()) {
				case 0 -> "tutorial_human_fighter001.htm";
				case 10 -> "tutorial_human_mage001.htm";
				case 18 -> "tutorial_elven_fighter001.htm";
				case 25 -> "tutorial_elven_mage001.htm";
				case 31 -> "tutorial_delf_fighter001.htm";
				case 38 -> "tutorial_delf_mage001.htm";
				case 44 -> "tutorial_orc_fighter001.htm";
				case 49 -> "tutorial_orc_mage001.htm";
				case 53 -> "tutorial_dwarven_fighter001.htm";
				default -> "tutorial_human_fighter001.htm";
			};
			qs.playTutorialVoice(voice);
			qs.showQuestionMark(1);
			qs.playSound("ItemSound.quest_tutorial");
			qs.showTutorialHtml(initialHtm);
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

		String cleanEvent = event.trim();
		if (cleanEvent.startsWith("link ")) {
			cleanEvent = cleanEvent.substring(5).trim();
		} else if (cleanEvent.startsWith("bypass -h ")) {
			cleanEvent = cleanEvent.substring(10).trim();
		}

		if (cleanEvent.startsWith("QM")) {
			int markId = 0;
			try {
				markId = Integer.parseInt(cleanEvent.substring(2));
			} catch (NumberFormatException ignored) {}

			int classId = player.activeChar() != null ? player.activeChar().classId() : 0;
			if (markId == 1 || markId == 5) {
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
				case 3 -> "tutorial_09.htm";
				case 5 -> "tutorial_11.htm";
				case 7, 12 -> "tutorial_15.htm";
				case 8 -> "tutorial_18.htm";
				case 9 -> isMageClass(classId) ? "tutorial_mage017.htm" : "tutorial_fighter017.htm";
				case 10 -> "tutorial_19.htm";
				case 11 -> "tutorial_mage017.htm";
				case 13 -> "tutorial_30.htm";
				case 17 -> "tutorial_27.htm";
				case 23 -> "tutorial_24.htm";
				case 24 -> switch (classId) {
					case 0, 10 -> "tutorial_human009.htm";
					case 18, 25 -> "tutorial_elf009.htm";
					case 31, 38 -> "tutorial_delf009.htm";
					case 44, 49 -> "tutorial_orc009.htm";
					case 53 -> "tutorial_dwarven009.htm";
					default -> "tutorial_human009.htm";
				};
				case 26 -> isMageClass(classId) ? "tutorial_newbie004b.htm" : "tutorial_newbie004a.htm";
				case 27 -> "tutorial_20.htm";
				case 34 -> "tutorial_28.htm";
				case 35 -> switch (classId) {
					case 0 -> "tutorial_21.htm";
					case 10 -> "tutorial_21a.htm";
					case 18 -> "tutorial_21b.htm";
					case 25 -> "tutorial_21c.htm";
					case 31 -> "tutorial_21g.htm";
					case 38 -> "tutorial_21h.htm";
					case 44 -> "tutorial_21d.htm";
					case 49 -> "tutorial_21e.htm";
					case 53 -> "tutorial_21f.htm";
					default -> "tutorial_21.htm";
				};
				default -> "tutorial_02.htm";
			};

			String html = qs.loadTutorialHtml(file);
			if (html == null || html.isBlank()) {
				html = qs.loadTutorialHtml("tutorial_02.htm");
			}
			if (html == null || html.isBlank()) {
				html = "<html><body><center><font color=\"LEVEL\">[Tutorial]</font></center><br>"
						+ "Welcome to Lineage 2!<br><a action=\"link TE00\">Exit the Tutorial</a></body></html>";
			}
			qs.showTutorialHtml(html);
		} else if (cleanEvent.startsWith("CE")) {
			int ceId = 0;
			try {
				ceId = Integer.parseInt(cleanEvent.substring(2));
			} catch (NumberFormatException ignored) {}

			PlayerCharacter c = player.activeChar();
			int classId = c != null ? c.classId() : 0;
			int lvl = c != null ? c.level() : 1;

			if (ceId == 1) {
				if (lvl < 6) {
					qs.playTutorialVoice("tutorial_voice_004");
					qs.playSound("ItemSound.quest_tutorial");
					qs.onTutorialClientEvent(2);
					String htm = qs.loadTutorialHtml("tutorial_03.htm");
					if (htm != null) qs.showTutorialHtml(htm);
				}
			} else if (ceId == 2) {
				if (lvl < 6) {
					qs.playTutorialVoice("tutorial_voice_005");
					qs.playSound("ItemSound.quest_tutorial");
					qs.onTutorialClientEvent(8);
					String htm = qs.loadTutorialHtml("tutorial_05.htm");
					if (htm != null) qs.showTutorialHtml(htm);
				}
			} else if (ceId == 8) {
				if (lvl < 6) {
					String file = switch (classId) {
						case 0 -> "tutorial_human_fighter007.htm";
						case 10 -> "tutorial_human_mage007.htm";
						case 18, 25 -> "tutorial_elf007.htm";
						case 31, 38 -> "tutorial_delf007.htm";
						case 44, 49 -> "tutorial_orc007.htm";
						case 53 -> "tutorial_dwarven_fighter007.htm";
						default -> "tutorial_human_fighter007.htm";
					};
					switch (classId) {
						case 0 -> qs.addRadar(-71424, 258336, -3104);
						case 10 -> qs.addRadar(-91036, 248044, -3568);
						case 18, 25 -> qs.addRadar(46112, 41200, -3504);
						case 31, 38 -> qs.addRadar(28384, 11056, -4224);
						case 44, 49 -> qs.addRadar(-56736, -113680, -672);
						case 53 -> qs.addRadar(108567, -173994, -406);
						default -> qs.addRadar(-71424, 258336, -3104);
					}
					qs.playSound("ItemSound.quest_tutorial");
					qs.playTutorialVoice("tutorial_voice_007");
					String htm = qs.loadTutorialHtml(file);
					if (htm != null) qs.showTutorialHtml(htm);
				}
			} else if (ceId == 30) {
				if (lvl < 6 && qs.getInt("Die") == 0) {
					qs.playTutorialVoice("tutorial_voice_016");
					qs.playSound("ItemSound.quest_tutorial");
					qs.set("Die", 1);
					qs.showQuestionMark(8);
					qs.onTutorialClientEvent(0);
				}
			} else if (ceId == 45) {
				if (lvl < 6 && qs.getInt("HP") == 0) {
					qs.playTutorialVoice("tutorial_voice_017");
					qs.playSound("ItemSound.quest_tutorial");
					qs.set("HP", 1);
					qs.showQuestionMark(10);
					qs.onTutorialClientEvent(800000);
				}
			} else if (ceId == 57) {
				if (lvl < 6 && qs.getInt("Adena") == 0) {
					qs.playTutorialVoice("tutorial_voice_012");
					qs.playSound("ItemSound.quest_tutorial");
					qs.set("Adena", 1);
					qs.showQuestionMark(23);
				}
			} else if (ceId == 6353) {
				if (lvl < 6 && qs.getInt("Gemstone") == 0) {
					qs.playTutorialVoice("tutorial_voice_013");
					qs.playSound("ItemSound.quest_tutorial");
					qs.set("Gemstone", 1);
					qs.showQuestionMark(5);
				}
			}
		} else if (cleanEvent.startsWith("TE")) {
			int teId = -1;
			try {
				teId = Integer.parseInt(cleanEvent.substring(2));
			} catch (NumberFormatException ignored) {}

			if (teId == 0 || teId == 12) {
				qs.closeTutorialHtml();
			} else if (teId == 1) {
				qs.closeTutorialHtml();
				qs.playTutorialVoice("tutorial_voice_006");
				qs.showQuestionMark(1);
				qs.playSound("ItemSound.quest_tutorial");
			} else if (teId == 2) {
				qs.playTutorialVoice("tutorial_voice_003");
				qs.onTutorialClientEvent(1);
				String htm = qs.loadTutorialHtml("tutorial_02.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 3) {
				qs.onTutorialClientEvent(2);
				String htm = qs.loadTutorialHtml("tutorial_03.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 5) {
				qs.onTutorialClientEvent(8);
				String htm = qs.loadTutorialHtml("tutorial_05.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 7) {
				qs.onTutorialClientEvent(0);
				String htm = qs.loadTutorialHtml("tutorial_100.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 8) {
				qs.onTutorialClientEvent(0);
				String htm = qs.loadTutorialHtml("tutorial_101.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 10) {
				qs.onTutorialClientEvent(0);
				String htm = qs.loadTutorialHtml("tutorial_103.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 27) {
				String htm = qs.loadTutorialHtml("tutorial_29.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			} else if (teId == 28) {
				String htm = qs.loadTutorialHtml("tutorial_28.htm");
				if (htm != null) qs.showTutorialHtml(htm);
			}
		}
		return null;
	}

	private boolean isMageClass(int classId) {
		return classId == 10 || classId == 25 || classId == 38 || classId == 49;
	}
}
