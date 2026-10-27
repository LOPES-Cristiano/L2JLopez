package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 022: Tragedy in von Hellmann Forest
 * High Priest Tifaren e Innocentin em Rune, Ghost of Priest, Ghost of
 * Adventurer e Well em Forest of the Dead.
 */
@Component
public class Quest022TragedyInVonHellmannForest extends Quest {

	public static final int QUEST_ID = 22;
	public static final String QUEST_NAME = "022_TragedyInVonHellmannForest";

	public static final int TIFAREN = 31334;
	public static final int INNOCENTIN = 31328;
	public static final int WELL = 31527;
	public static final int GHOST_OF_PRIEST = 31528;
	public static final int GHOST_OF_ADVENTURER = 31529;

	public static final int CROSS_OF_EINHASAD = 7141;
	public static final int SKULL_OF_CRUEL = 7142;
	public static final int LOST_SKULL_OF_ELF = 7142;
	public static final int LETTER_OF_HELLMANN = 7143;
	public static final int LETTER_OF_INNOCENTIN = 7143;
	public static final int GREEN_JEWEL_OF_ADVENTURER = 7144;
	public static final int RED_JEWEL_OF_ADVENTURER = 7145;
	public static final int SEALED_REPORT_BOX = 7146;
	public static final int REPORT_BOX = 7147;

	public static final int SOUL_OF_WELL = 27217;
	public static final int WELL_TIMER_DELAY = 20000;
	public static final int[] MOBS = { 21553, 21554, 21555, 21556, 21561 };

	@Autowired
	public Quest022TragedyInVonHellmannForest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Tragedy in von Hellmann Forest");
		addStartNpc(TIFAREN, INNOCENTIN);
		addTalkId(TIFAREN, INNOCENTIN, WELL, GHOST_OF_PRIEST, GHOST_OF_ADVENTURER);
		addAttackId(SOUL_OF_WELL);
		addKillId(SOUL_OF_WELL);
		addKillId(MOBS);
		registerQuestItems(LOST_SKULL_OF_ELF, LETTER_OF_INNOCENTIN, GREEN_JEWEL_OF_ADVENTURER, RED_JEWEL_OF_ADVENTURER,
				SEALED_REPORT_BOX, REPORT_BOX);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31334-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31334-04.htm".equalsIgnoreCase(event) ? "31334-04.htm" : "grandmagister_tifaren_q0022_04.htm";
		} else if ("31334-02.htm".equalsIgnoreCase(event)
				|| "grandmagister_tifaren_q0022_02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CROSS_OF_EINHASAD)) {
				return event;
			}
			return "grandmagister_tifaren_q0022_03.htm";
		} else if ("31334-03.htm".equalsIgnoreCase(event)) {
			return "31334-03.htm";
		} else if ("reply_2".equalsIgnoreCase(event) || "31334-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(LOST_SKULL_OF_ELF)) {
				return "31334-07.htm".equalsIgnoreCase(event) ? "31334-07.htm" : "grandmagister_tifaren_q0022_08.htm";
			}
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "grandmagister_tifaren_q0022_07.htm";
		} else if ("31334-06.htm".equalsIgnoreCase(event)) {
			return "31334-06.htm";
		} else if ("reply_3".equalsIgnoreCase(event) || "31334-08.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CROSS_OF_EINHASAD)) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31334-08.htm".equalsIgnoreCase(event) ? "31334-08.htm" : "grandmagister_tifaren_q0022_09.htm";
			}
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "grandmagister_tifaren_q0022_07.htm";
		} else if ("31334-12.htm".equalsIgnoreCase(event)
				|| "grandmagister_tifaren_q0022_12.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_5".equalsIgnoreCase(event) || "31334-13.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CROSS_OF_EINHASAD) && qs.hasQuestItems(LOST_SKULL_OF_ELF)) {
				qs.setCond(4);
				qs.takeItems(LOST_SKULL_OF_ELF, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.addSpawn(GHOST_OF_PRIEST, 38354, -49777, -1128, 120000);
				return "31334-13.htm".equalsIgnoreCase(event) ? "31334-13.htm" : "grandmagister_tifaren_q0022_13.htm";
			}
			return "grandmagister_tifaren_q0022_16.htm";
		} else if ("31528-02.htm".equalsIgnoreCase(event) || "31528-03.htm".equalsIgnoreCase(event)
				|| "31528-04.htm".equalsIgnoreCase(event) || "31528-05.htm".equalsIgnoreCase(event)
				|| "31528-06.htm".equalsIgnoreCase(event) || "31528-07.htm".equalsIgnoreCase(event)
				|| "rune_ghost2_q0022_02.htm".equalsIgnoreCase(event)
				|| "rune_ghost2_q0022_03.htm".equalsIgnoreCase(event)
				|| "rune_ghost2_q0022_05.htm".equalsIgnoreCase(event)
				|| "rune_ghost2_q0022_06.htm".equalsIgnoreCase(event)
				|| "rune_ghost2_q0022_07.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_6".equalsIgnoreCase(event)) {
			return "rune_ghost2_q0022_04.htm";
		} else if ("reply_7".equalsIgnoreCase(event) || "31528-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31528-08.htm".equalsIgnoreCase(event) ? "31528-08.htm" : "rune_ghost2_q0022_08.htm";
		} else if ("31528-09.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31528-09.htm";
		} else if ("reply_8".equalsIgnoreCase(event) || "31328-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CROSS_OF_EINHASAD, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-04.htm".equalsIgnoreCase(event) ? "31328-04.htm" : "highpriest_innocentin_q0022_03.htm";
		} else if ("31328-06.htm".equalsIgnoreCase(event) || "31328-07.htm".equalsIgnoreCase(event)
				|| "31328-08.htm".equalsIgnoreCase(event) || "31328-09.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_05.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_06.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_07.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_08.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_10".equalsIgnoreCase(event) || "31328-10.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.giveItems(LETTER_OF_INNOCENTIN, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-10.htm".equalsIgnoreCase(event) ? "31328-10.htm" : "highpriest_innocentin_q0022_09.htm";
		} else if ("31529-02.htm".equalsIgnoreCase(event) || "rune_ghost3_q0022_02.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_12".equalsIgnoreCase(event) || "31529-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(LETTER_OF_INNOCENTIN)) {
				qs.takeItems(LETTER_OF_INNOCENTIN, 1);
				return "31529-03.htm".equalsIgnoreCase(event) ? "31529-03.htm" : "rune_ghost3_q0022_03.htm";
			}
			return "rune_ghost3_q0022_03a.htm";
		} else if ("31529-05.htm".equalsIgnoreCase(event) || "31529-06.htm".equalsIgnoreCase(event)
				|| "31529-07.htm".equalsIgnoreCase(event) || "31529-08.htm".equalsIgnoreCase(event)
				|| "31529-09.htm".equalsIgnoreCase(event) || "31529-10.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_04.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_05.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_06.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_07.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_09.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_13.htm".equalsIgnoreCase(event)
				|| "rune_ghost3_q0022_13a.htm".equalsIgnoreCase(event) || "31529-14.htm".equalsIgnoreCase(event)
				|| "31529-15.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_14".equalsIgnoreCase(event)) {
			return "rune_ghost3_q0022_08.htm";
		} else if ("reply_15".equalsIgnoreCase(event) || "31529-12.htm".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.giveItems(GREEN_JEWEL_OF_ADVENTURER, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31529-12.htm".equalsIgnoreCase(event) ? "31529-12.htm" : "rune_ghost3_q0022_11.htm";
		} else if ("reply_16".equalsIgnoreCase(event) || "31527-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.takeItems(GREEN_JEWEL_OF_ADVENTURER, 1);
			qs.giveItems(RED_JEWEL_OF_ADVENTURER, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			qs.addSpawn(SOUL_OF_WELL, 34706, -54590, -2054, 120000);
			return "31527-02.htm".equalsIgnoreCase(event) ? "31527-02.htm" : "umul_q0022_02.htm";
		} else if ("reply_17".equalsIgnoreCase(event) || "31328-13.htm".equalsIgnoreCase(event)) {
			qs.setCond(15);
			qs.takeItems(SEALED_REPORT_BOX, 1);
			qs.giveItems(REPORT_BOX, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-13.htm".equalsIgnoreCase(event) ? "31328-13.htm" : "highpriest_innocentin_q0022_11.htm";
		} else if ("31328-15.htm".equalsIgnoreCase(event) || "31328-16.htm".equalsIgnoreCase(event)
				|| "31328-17.htm".equalsIgnoreCase(event) || "31328-18.htm".equalsIgnoreCase(event)
				|| "31328-19.htm".equalsIgnoreCase(event) || "31328-20.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_13.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_14.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_15.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_16.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_17.htm".equalsIgnoreCase(event)
				|| "highpriest_innocentin_q0022_18.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_19".equalsIgnoreCase(event) || "31328-21.htm".equalsIgnoreCase(event)) {
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-21.htm".equalsIgnoreCase(event) ? "31328-21.htm" : "highpriest_innocentin_q0022_19.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null)
			return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case TIFAREN:
				if (cond == 0) {
					if (pc.level() >= 63) {
						return "grandmagister_tifaren_q0022_01.htm";
					}
					qs.exitQuest(true);
					return "grandmagister_tifaren_q0022_03.htm";
				} else if (cond == 1) {
					return "grandmagister_tifaren_q0022_05.htm";
				} else if (cond == 2) {
					return qs.hasQuestItems(LOST_SKULL_OF_ELF) ? "grandmagister_tifaren_q0022_10.htm"
							: "grandmagister_tifaren_q0022_09.htm";
				} else if (cond == 3 && qs.hasQuestItems(LOST_SKULL_OF_ELF)) {
					return "grandmagister_tifaren_q0022_11.htm";
				} else if (cond == 4) {
					qs.addSpawn(GHOST_OF_PRIEST, 38354, -49777, -1128, 120000);
					return "grandmagister_tifaren_q0022_17.htm";
				} else if (cond == 5) {
					return "grandmagister_tifaren_q0022_19.htm";
				}
				break;
			case GHOST_OF_PRIEST:
				if (cond == 4) {
					return "rune_ghost2_q0022_01.htm";
				} else if (cond >= 5) {
					return "rune_ghost2_q0022_08.htm";
				}
				break;
			case INNOCENTIN:
				if (cond < 5) {
					if (!qs.hasQuestItems(CROSS_OF_EINHASAD)) {
						qs.giveItems(CROSS_OF_EINHASAD, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
						return "highpriest_innocentin_q0022_01.htm";
					}
					return "highpriest_innocentin_q0022_01b.htm";
				} else if (cond == 5) {
					return "highpriest_innocentin_q0022_02.htm";
				} else if (cond == 6) {
					return "highpriest_innocentin_q0022_04.htm";
				} else if (cond == 7) {
					return "highpriest_innocentin_q0022_09a.htm";
				} else if (cond == 14 && qs.hasQuestItems(SEALED_REPORT_BOX)) {
					return "highpriest_innocentin_q0022_10.htm";
				} else if (cond == 15 && qs.hasQuestItems(REPORT_BOX)) {
					qs.takeItems(REPORT_BOX, 1);
					return "highpriest_innocentin_q0022_12.htm";
				} else if (cond == 16) {
					qs.addExpAndSp(345966, 31578);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return pc.level() >= 64 ? "highpriest_innocentin_q0022_20.htm"
							: "highpriest_innocentin_q0022_21.htm";
				}
				break;
			case GHOST_OF_ADVENTURER:
				if (cond == 7 && qs.hasQuestItems(LETTER_OF_INNOCENTIN)) {
					return "rune_ghost3_q0022_01.htm";
				} else if (cond == 8) {
					return "rune_ghost3_q0022_12.htm";
				} else if (cond == 9) {
					return "rune_ghost3_q0022_10.htm";
				} else if (cond == 10 || cond == 11) {
					qs.setCond(12);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "rune_ghost3_q0022_14.htm";
				} else if (cond == 12 && qs.hasQuestItems(RED_JEWEL_OF_ADVENTURER)) {
					return "rune_ghost3_q0022_15.htm";
				} else if (cond == 13 && qs.hasQuestItems(RED_JEWEL_OF_ADVENTURER)
						&& qs.hasQuestItems(SEALED_REPORT_BOX)) {
					qs.setCond(14);
					qs.takeItems(RED_JEWEL_OF_ADVENTURER, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "rune_ghost3_q0022_16.htm";
				} else if (cond >= 14) {
					return "rune_ghost3_q0022_17.htm";
				}
				break;
			case WELL:
				if ((cond == 8 || cond == 9) && qs.hasQuestItems(GREEN_JEWEL_OF_ADVENTURER)) {
					qs.setCond(9);
					return "umul_q0022_01.htm";
				} else if (cond == 10 || cond == 11) {
					qs.setCond(11);
					return "umul_q0022_03.htm";
				} else if (cond == 12) {
					qs.setCond(13);
					qs.giveItems(SEALED_REPORT_BOX, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "umul_q0022_04.htm";
				} else if (cond >= 13) {
					return "umul_q0022_05.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		if (npcId == SOUL_OF_WELL) {
			if (cond == 9 || cond == 10) {
				qs.setCond(11);
				qs.takeItems(GREEN_JEWEL_OF_ADVENTURER, -1);
				if (!qs.hasQuestItems(RED_JEWEL_OF_ADVENTURER)) {
					qs.giveItems(RED_JEWEL_OF_ADVENTURER, 1);
				}
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		} else {
			if ((cond == 1 || cond == 2) && !qs.hasQuestItems(LOST_SKULL_OF_ELF)) {
				qs.giveItems(LOST_SKULL_OF_ELF, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
