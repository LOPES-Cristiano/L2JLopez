package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 226: Test of the Healer
 * 3º Passo de 2ª Classe para Bishop, Elven Elder, Paladin, Temple Knight.
 */
@Component
public class Quest226TestOfHealer extends Quest {

	public static final int QUEST_ID = 226;
	public static final String QUEST_NAME = "226_TestOfHealer";

	// NPCs
	public static final int BANDELLOS = 30473;
	public static final int SORIUS = 30327;
	public static final int ALLANA = 30424;
	public static final int PERRIN = 30428;
	public static final int GUPU = 30658;
	public static final int ORPHAN_GIRL = 30659;
	public static final int WINDY = 30660;
	public static final int MYSTERIOUS_DARKELF = 30661;
	public static final int PIPER_LONGBOW = 30662;
	public static final int SLEIN_SHINING_BLADE = 30663;
	public static final int KAIN_FLYING_KNIFE = 30664;
	public static final int KRISTINA = 30665;
	public static final int DAURIN_HAMMERCRUSH = 30674;

	// Monstros
	public static final int LETO_LIZARDMAN_LEADER = 27123;
	public static final int LETO_LIZARDMAN_ASSASSIN = 27124;
	public static final int LETO_LIZARDMAN_SNIPER = 27125;
	public static final int LETO_LIZARDMAN_WIZARD = 27126;
	public static final int LETO_LIZARDMAN_LORD = 27127;
	public static final int TATOMA = 27134;

	// Itens
	public static final int ADENA = 57;
	public static final int REPORT_OF_PERRIN = 2810;
	public static final int CRISTINAS_LETTER = 2811;
	public static final int PICTURE_OF_WINDY = 2812;
	public static final int GOLDEN_STATUE = 2813;
	public static final int WINDYS_PEBBLES = 2814;
	public static final int ORDER_OF_SORIUS = 2815;
	public static final int SECRET_LETTER_1 = 2816;
	public static final int SECRET_LETTER_2 = 2817;
	public static final int SECRET_LETTER_3 = 2818;
	public static final int SECRET_LETTER_4 = 2819;
	public static final int MARK_OF_HEALER = 2820;

	private static final Set<Integer> VALID_CLASSES = Set.of(4, 15, 19, 29, 42);

	@Autowired
	public Quest226TestOfHealer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Healer");

		addStartNpc(BANDELLOS);
		addTalkId(BANDELLOS, SORIUS, ALLANA, PERRIN, GUPU, ORPHAN_GIRL, WINDY,
				MYSTERIOUS_DARKELF, PIPER_LONGBOW, SLEIN_SHINING_BLADE,
				KAIN_FLYING_KNIFE, KRISTINA, DAURIN_HAMMERCRUSH);

		addKillId(LETO_LIZARDMAN_LEADER, LETO_LIZARDMAN_ASSASSIN, LETO_LIZARDMAN_SNIPER,
				LETO_LIZARDMAN_WIZARD, LETO_LIZARDMAN_LORD, TATOMA);

		registerQuestItems(REPORT_OF_PERRIN, CRISTINAS_LETTER, PICTURE_OF_WINDY, GOLDEN_STATUE,
				WINDYS_PEBBLES, ORDER_OF_SORIUS, SECRET_LETTER_1, SECRET_LETTER_2,
				SECRET_LETTER_3, SECRET_LETTER_4);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30473-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(REPORT_OF_PERRIN, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30473-04.htm";
		} else if ("30473_2".equalsIgnoreCase(event) || "30473-09.htm".equalsIgnoreCase(event)) {
			qs.takeItems(GOLDEN_STATUE, -1);
			qs.giveItems(MARK_OF_HEALER, 1);
			qs.addExpAndSp(118304, 26250);
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitCurrentQuest(false);
			return "30473-09.htm";
		} else if ("30428-02.htm".equalsIgnoreCase(event) || "30428_1".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(TATOMA, -93254, 147559, -2679, 200000);
			return "30428-02.htm";
		} else if ("30658-02.htm".equalsIgnoreCase(event) || "30658_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ADENA) >= 100000) {
				qs.takeItems(ADENA, 100000);
				qs.giveItems(PICTURE_OF_WINDY, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30658-02.htm";
			}
			return "30658-05.htm";
		} else if ("30658-03.htm".equalsIgnoreCase(event) || "30658_2".equalsIgnoreCase(event)) {
			qs.set("gupu", "1");
			return "30658-03.htm";
		} else if ("30658-07.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30658-07.htm";
		} else if ("30660-03.htm".equalsIgnoreCase(event) || "30660_1".equalsIgnoreCase(event)) {
			qs.takeItems(PICTURE_OF_WINDY, -1);
			qs.giveItems(WINDYS_PEBBLES, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30660-03.htm";
		} else if ("30674-02.htm".equalsIgnoreCase(event) || "30674_1".equalsIgnoreCase(event)) {
			qs.takeItems(ORDER_OF_SORIUS, -1);
			qs.setCond(11);
			qs.playSound(QuestState.SOUND_BEFORE_BATTLE);
			qs.addSpawn(LETO_LIZARDMAN_LEADER, -97441, 106585, -3405, 200000);
			return "30674-02.htm";
		} else if ("30665-02.htm".equalsIgnoreCase(event) || "30665_1".equalsIgnoreCase(event)) {
			qs.takeItems(SECRET_LETTER_1, -1);
			qs.takeItems(SECRET_LETTER_2, -1);
			qs.takeItems(SECRET_LETTER_3, -1);
			qs.takeItems(SECRET_LETTER_4, -1);
			qs.giveItems(CRISTINAS_LETTER, 1);
			qs.setCond(22);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30665-02.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BANDELLOS) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30473-03.htm";
				} else {
					return "30473-01.htm";
				}
			} else if (cond >= 1 && cond < 20) {
				return "30473-05.htm";
			} else if (cond >= 20 && cond < 23) {
				return "30473-08.htm";
			} else if (cond == 23) {
				if (!qs.hasQuestItems(GOLDEN_STATUE)) {
					qs.giveItems(MARK_OF_HEALER, 1);
					qs.addExpAndSp(118304, 26250);
					qs.setCond(0);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitCurrentQuest(false);
					return "30473-08.htm";
				}
				return "30473-08.htm";
			}
		} else if (npcId == PERRIN) {
			if (cond == 1 || cond == 2) {
				return "30428-01.htm";
			} else if (cond == 3) {
				qs.takeItems(REPORT_OF_PERRIN, -1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30428-03.htm";
			} else if (cond >= 4) {
				return "30428-04.htm";
			}
		} else if (npcId == ORPHAN_GIRL) {
			return "30659-0" + ThreadLocalRandom.current().nextInt(1, 6) + ".htm";
		} else if (npcId == ALLANA) {
			if (cond == 4) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30424-01.htm";
			} else if (cond > 4) {
				return "30424-02.htm";
			}
		} else if (npcId == GUPU) {
			if (qs.getInt("gupu") == 1 && cond != 9) {
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30658-07.htm";
			} else if (cond == 5) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30658-01.htm";
			} else if (cond == 6) {
				return "30658-01.htm";
			} else if (cond == 7) {
				return "30658-04.htm";
			} else if (cond == 8) {
				qs.takeItems(WINDYS_PEBBLES, -1);
				qs.giveItems(GOLDEN_STATUE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30658-06.htm";
			} else if (cond > 8) {
				return "30658-07.htm";
			}
		} else if (npcId == WINDY) {
			if (cond == 7) {
				return "30660-01.htm";
			} else if (qs.hasQuestItems(WINDYS_PEBBLES)) {
				return "30660-04.htm";
			}
		} else if (npcId == SORIUS) {
			if (cond == 9) {
				qs.giveItems(ORDER_OF_SORIUS, 1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30327-01.htm";
			} else if (cond > 9 && cond < 22) {
				return "30327-02.htm";
			} else if (cond == 22) {
				qs.takeItems(CRISTINAS_LETTER, -1);
				qs.setCond(23);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30327-03.htm";
			} else if (cond == 23) {
				return "30327-04.htm";
			}
		} else if (npcId == DAURIN_HAMMERCRUSH) {
			if (cond == 10) {
				return "30674-01.htm";
			} else if (cond == 11) {
				qs.addSpawn(LETO_LIZARDMAN_LEADER, -97441, 106585, -3405, 200000);
				return "30674-02a.htm";
			} else if (cond == 12) {
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30674-03.htm";
			} else if (cond > 12) {
				return "30674-04.htm";
			}
		} else if (npcId == PIPER_LONGBOW || npcId == SLEIN_SHINING_BLADE || npcId == KAIN_FLYING_KNIFE) {
			if (cond == 13 || cond == 14) {
				return npcId == PIPER_LONGBOW ? "30662-01.htm" : (npcId == SLEIN_SHINING_BLADE ? "30663-01.htm" : "30664-01.htm");
			} else if (cond >= 15 && cond <= 18) {
				return npcId == PIPER_LONGBOW ? "30662-02.htm" : (npcId == SLEIN_SHINING_BLADE ? "30663-02.htm" : "30664-02.htm");
			} else if (cond >= 19 && cond <= 21) {
				qs.setCond(21);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return npcId == PIPER_LONGBOW ? "30662-03.htm" : (npcId == SLEIN_SHINING_BLADE ? "30663-03.htm" : "30664-03.htm");
			}
		} else if (npcId == MYSTERIOUS_DARKELF) {
			if (cond == 13) {
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_BEFORE_BATTLE);
				qs.addSpawn(LETO_LIZARDMAN_ASSASSIN);
				qs.addSpawn(LETO_LIZARDMAN_ASSASSIN);
				qs.addSpawn(LETO_LIZARDMAN_ASSASSIN);
				return "30661-01.htm";
			} else if (cond == 14) {
				return "30661-01.htm";
			} else if (cond == 15) {
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_BEFORE_BATTLE);
				qs.addSpawn(LETO_LIZARDMAN_SNIPER);
				qs.addSpawn(LETO_LIZARDMAN_SNIPER);
				qs.addSpawn(LETO_LIZARDMAN_SNIPER);
				return "30661-02.htm";
			} else if (cond == 16) {
				return "30661-02.htm";
			} else if (cond == 17) {
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_BEFORE_BATTLE);
				qs.addSpawn(LETO_LIZARDMAN_WIZARD);
				qs.addSpawn(LETO_LIZARDMAN_WIZARD);
				qs.addSpawn(LETO_LIZARDMAN_LORD);
				return "30661-03.htm";
			} else if (cond == 18) {
				return "30661-03.htm";
			} else if (cond == 19) {
				qs.setCond(20);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30661-04.htm";
			} else if (cond == 20 || cond == 21) {
				return "30661-04.htm";
			}
		} else if (npcId == KRISTINA) {
			if (cond >= 19 && cond <= 21) {
				return "30665-01.htm";
			} else if (cond >= 22) {
				return "30665-04.htm";
			} else {
				return "30665-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case TATOMA -> {
				if (cond == 1 || cond == 2) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case LETO_LIZARDMAN_LEADER -> {
				if (cond == 10 || cond == 11) {
					qs.giveItems(SECRET_LETTER_1, 1);
					qs.setCond(12);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case LETO_LIZARDMAN_ASSASSIN -> {
				if (cond == 13 || cond == 14) {
					qs.giveItems(SECRET_LETTER_2, 1);
					qs.setCond(15);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case LETO_LIZARDMAN_SNIPER -> {
				if (cond == 15 || cond == 16) {
					qs.giveItems(SECRET_LETTER_3, 1);
					qs.setCond(17);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case LETO_LIZARDMAN_LORD -> {
				if (cond == 17 || cond == 18) {
					qs.giveItems(SECRET_LETTER_4, 1);
					qs.setCond(19);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}
