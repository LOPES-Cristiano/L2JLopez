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
 * Quest 213: Trial of the Seeker
 * 1º Passo de 2ª Classe para Hawkeye, Silver Ranger, Phantom Ranger, Treasure Hunter, Plains Walker, Abyss Walker.
 */
@Component
public class Quest213TrialOfSeeker extends Quest {

	public static final int QUEST_ID = 213;
	public static final String QUEST_NAME = "213_TrialOfSeeker";

	// NPCs
	public static final int DUFNER = 30106;
	public static final int TERRY = 30064;
	public static final int VIKTOR = 30684;
	public static final int MARINA = 30715;
	public static final int BRUNON = 30526;

	// Monstros
	public static final int NEER_GHOUL = 20198;
	public static final int OL_MAHUM_CAPTAIN = 20211;
	public static final int TUREK_ORC_WARLORD = 20495;
	public static final int ANT_CAPTAIN = 20080;
	public static final int TURAK_BUGBEAR_WARRIOR = 20249;
	public static final int MEDUSA = 20158;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int ANT_WARRIOR_CAPTAIN = 20088;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;

	// Itens
	public static final int DUFNERS_LETTER = 2647;
	public static final int TERYS_ORDER1 = 2648;
	public static final int TERYS_ORDER2 = 2649;
	public static final int TERYS_LETTER = 2650;
	public static final int VIKTORS_LETTER = 2651;
	public static final int HAWKEYES_LETTER = 2652;
	public static final int MYSTERIOUS_RUNESTONE = 2653;
	public static final int OL_MAHUM_RUNESTONE = 2654;
	public static final int TUREK_RUNESTONE = 2655;
	public static final int ANT_RUNESTONE = 2656;
	public static final int TURAK_BUGBEAR_RUNESTONE = 2657;
	public static final int TERYS_BOX = 2658;
	public static final int VIKTORS_REQUEST = 2659;
	public static final int MEDUSAS_SCALES = 2660;
	public static final int SILENS_RUNESTONE = 2661;
	public static final int ANALYSIS_REQUEST = 2662;
	public static final int MARINAS_LETTER = 2663;
	public static final int EXPERIMENT_TOOLS = 2664;
	public static final int ANALYSIS_RESULT = 2665;
	public static final int LIST_OF_HOST = 2667;
	public static final int ABYSS_RUNESTONE1 = 2668;
	public static final int ABYSS_RUNESTONE2 = 2669;
	public static final int ABYSS_RUNESTONE3 = 2670;
	public static final int ABYSS_RUNESTONE4 = 2671;
	public static final int TERYS_REPORT = 2672;
	public static final int MARK_OF_SEEKER = 2673;

	private static final Set<Integer> VALID_CLASSES = Set.of(7, 22, 35); // Rogue, Elven Scout, Assassin

	@Autowired
	public Quest213TrialOfSeeker(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Seeker");

		addStartNpc(DUFNER);
		addTalkId(DUFNER, TERRY, VIKTOR, MARINA, BRUNON);

		addKillId(NEER_GHOUL, OL_MAHUM_CAPTAIN, TUREK_ORC_WARLORD, ANT_CAPTAIN, TURAK_BUGBEAR_WARRIOR,
				MEDUSA, MARSH_STAKATO_DRONE, BREKA_ORC_OVERLORD, ANT_WARRIOR_CAPTAIN, LETO_LIZARDMAN_WARRIOR);

		registerQuestItems(DUFNERS_LETTER, TERYS_ORDER1, TERYS_ORDER2, TERYS_LETTER, VIKTORS_LETTER,
				HAWKEYES_LETTER, MYSTERIOUS_RUNESTONE, OL_MAHUM_RUNESTONE, TUREK_RUNESTONE, ANT_RUNESTONE,
				TURAK_BUGBEAR_RUNESTONE, TERYS_BOX, VIKTORS_REQUEST, MEDUSAS_SCALES, SILENS_RUNESTONE,
				ANALYSIS_REQUEST, MARINAS_LETTER, EXPERIMENT_TOOLS, ANALYSIS_RESULT, LIST_OF_HOST,
				ABYSS_RUNESTONE1, ABYSS_RUNESTONE2, ABYSS_RUNESTONE3, ABYSS_RUNESTONE4, TERYS_REPORT);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("30106-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(DUFNERS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30064-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(DUFNERS_LETTER, 1);
			qs.giveItems(TERYS_ORDER1, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30064-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MYSTERIOUS_RUNESTONE, -1);
			qs.takeItems(TERYS_ORDER1, -1);
			qs.giveItems(TERYS_ORDER2, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30064-10.htm".equalsIgnoreCase(event)) {
			qs.takeItems(OL_MAHUM_RUNESTONE, -1);
			qs.takeItems(TUREK_RUNESTONE, -1);
			qs.takeItems(ANT_RUNESTONE, -1);
			qs.takeItems(TURAK_BUGBEAR_RUNESTONE, -1);
			qs.takeItems(TERYS_ORDER2, -1);
			qs.giveItems(TERYS_LETTER, 1);
			qs.giveItems(TERYS_BOX, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30064-18.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ANALYSIS_RESULT, -1);
			qs.giveItems(LIST_OF_HOST, 1);
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30684-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(TERYS_LETTER, 1);
			qs.giveItems(VIKTORS_LETTER, 1);
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30684-11.htm".equalsIgnoreCase(event)) {
			qs.takeItems(TERYS_BOX, -1);
			qs.takeItems(HAWKEYES_LETTER, -1);
			qs.takeItems(VIKTORS_LETTER, -1);
			qs.giveItems(VIKTORS_REQUEST, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30684-15.htm".equalsIgnoreCase(event)) {
			qs.takeItems(VIKTORS_REQUEST, -1);
			qs.takeItems(MEDUSAS_SCALES, -1);
			qs.giveItems(SILENS_RUNESTONE, 1);
			qs.giveItems(ANALYSIS_REQUEST, 1);
			qs.setCond(11);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30715-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SILENS_RUNESTONE, -1);
			qs.takeItems(ANALYSIS_REQUEST, -1);
			qs.giveItems(MARINAS_LETTER, 1);
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30715-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(EXPERIMENT_TOOLS, -1);
			qs.giveItems(ANALYSIS_RESULT, 1);
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30526-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MARINAS_LETTER, -1);
			qs.giveItems(EXPERIMENT_TOOLS, 1);
			qs.setCond(13);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
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

		if (npcId == DUFNER) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 35 && VALID_CLASSES.contains(player.classId())) {
					return "30106-03.htm";
				} else {
					return "30106-02.htm";
				}
			} else if (cond == 1) {
				return "30106-06.htm";
			} else if (cond == 18 && qs.hasQuestItems(TERYS_REPORT)) {
				qs.takeItems(TERYS_REPORT, -1);
				qs.giveItems(MARK_OF_SEEKER, 1);
				qs.addExpAndSp(72110, 11000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30106-07.htm";
			}
		} else if (npcId == TERRY) {
			if (cond == 1) {
				return "30064-01.htm";
			} else if (cond == 2) {
				return "30064-04.htm";
			} else if (cond == 3) {
				return "30064-05.htm";
			} else if (cond == 4) {
				return "30064-07.htm";
			} else if (cond == 5) {
				return "30064-09.htm";
			} else if (cond == 6) {
				return "30064-11.htm";
			} else if (cond == 15) {
				return "30064-16.htm";
			} else if (cond == 16) {
				return "30064-19.htm";
			} else if (cond == 17) {
				qs.takeItems(LIST_OF_HOST, -1);
				qs.takeItems(ABYSS_RUNESTONE1, -1);
				qs.takeItems(ABYSS_RUNESTONE2, -1);
				qs.takeItems(ABYSS_RUNESTONE3, -1);
				qs.takeItems(ABYSS_RUNESTONE4, -1);
				qs.giveItems(TERYS_REPORT, 1);
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30064-20.htm";
			} else if (cond == 18) {
				return "30064-21.htm";
			}
		} else if (npcId == VIKTOR) {
			if (cond == 6) {
				return "30684-01.htm";
			} else if (cond == 7) {
				return "30684-06.htm";
			} else if (cond == 8) {
				return "30684-08.htm";
			} else if (cond == 9) {
				return "30684-12.htm";
			} else if (cond == 10) {
				return "30684-14.htm";
			} else if (cond == 11) {
				return "30684-16.htm";
			}
		} else if (npcId == MARINA) {
			if (cond == 11) {
				return "30715-01.htm";
			} else if (cond == 12) {
				return "30715-03.htm";
			} else if (cond == 13) {
				return "30715-04.htm";
			} else if (cond == 15) {
				return "30715-06.htm";
			}
		} else if (npcId == BRUNON) {
			if (cond == 12) {
				return "30526-01.htm";
			} else if (cond == 13) {
				return "30526-03.htm";
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

		if (cond == 2 && npcId == NEER_GHOUL && !qs.hasQuestItems(MYSTERIOUS_RUNESTONE)) {
			if (ThreadLocalRandom.current().nextInt(100) < 50) {
				qs.giveItems(MYSTERIOUS_RUNESTONE, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if (cond == 4) {
			int stone = 0;
			if (npcId == OL_MAHUM_CAPTAIN && !qs.hasQuestItems(OL_MAHUM_RUNESTONE)) {
				stone = OL_MAHUM_RUNESTONE;
			} else if (npcId == TUREK_ORC_WARLORD && !qs.hasQuestItems(TUREK_RUNESTONE)) {
				stone = TUREK_RUNESTONE;
			} else if (npcId == ANT_CAPTAIN && !qs.hasQuestItems(ANT_RUNESTONE)) {
				stone = ANT_RUNESTONE;
			} else if (npcId == TURAK_BUGBEAR_WARRIOR && !qs.hasQuestItems(TURAK_BUGBEAR_RUNESTONE)) {
				stone = TURAK_BUGBEAR_RUNESTONE;
			}

			if (stone > 0 && ThreadLocalRandom.current().nextInt(100) < 60) {
				qs.giveItems(stone, 1);
				if (qs.hasQuestItems(OL_MAHUM_RUNESTONE) && qs.hasQuestItems(TUREK_RUNESTONE)
						&& qs.hasQuestItems(ANT_RUNESTONE) && qs.hasQuestItems(TURAK_BUGBEAR_RUNESTONE)) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 9 && npcId == MEDUSA) {
			if (qs.getQuestItemsCount(MEDUSAS_SCALES) < 10) {
				if (ThreadLocalRandom.current().nextInt(100) < 60) {
					qs.giveItems(MEDUSAS_SCALES, 1);
					if (qs.getQuestItemsCount(MEDUSAS_SCALES) >= 10) {
						qs.setCond(10);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		} else if (cond == 16) {
			int abyss = 0;
			if (npcId == MARSH_STAKATO_DRONE && !qs.hasQuestItems(ABYSS_RUNESTONE1)) {
				abyss = ABYSS_RUNESTONE1;
			} else if (npcId == BREKA_ORC_OVERLORD && !qs.hasQuestItems(ABYSS_RUNESTONE2)) {
				abyss = ABYSS_RUNESTONE2;
			} else if (npcId == ANT_WARRIOR_CAPTAIN && !qs.hasQuestItems(ABYSS_RUNESTONE3)) {
				abyss = ABYSS_RUNESTONE3;
			} else if (npcId == LETO_LIZARDMAN_WARRIOR && !qs.hasQuestItems(ABYSS_RUNESTONE4)) {
				abyss = ABYSS_RUNESTONE4;
			}

			if (abyss > 0 && ThreadLocalRandom.current().nextInt(100) < 60) {
				qs.giveItems(abyss, 1);
				if (qs.hasQuestItems(ABYSS_RUNESTONE1) && qs.hasQuestItems(ABYSS_RUNESTONE2)
						&& qs.hasQuestItems(ABYSS_RUNESTONE3) && qs.hasQuestItems(ABYSS_RUNESTONE4)) {
					qs.setCond(17);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}
