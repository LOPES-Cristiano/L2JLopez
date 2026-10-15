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
 * Quest 021: Hidden Truth
 * Shadow Hardin em Forest of the Dead, Tombstone, Ghost of von Hellmann, Page, Broken Bookshelf,
 * Agripel, Dominic, Benedict e High Priest Innocentin em Rune.
 */
@Component
public class Quest021HiddenTruth extends Quest {

	public static final int QUEST_ID = 21;
	public static final String QUEST_NAME = "021_HiddenTruth";

	public static final int HARDIN = 31522;
	public static final int TOMBSTONE = 31523;
	public static final int GHOST = 31524;
	public static final int VON_HELLMAN_DUKE = 31524;
	public static final int VON_HELLMAN_PAGE = 31525;
	public static final int BOOKSHELF = 31526;
	public static final int BROKEN_BOOKSHELF = 31526;
	public static final int AGRIPEL = 31348;
	public static final int DOMINIC = 31350;
	public static final int BENEDICT = 31349;
	public static final int INNOCENTIN = 31328;

	public static final int CASKET = 7140;
	public static final int CROSS_OF_EINHASAD = 7141;

	public static final int PAGE_TIMER_DELAY = 18000;

	@Autowired
	public Quest021HiddenTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hidden Truth");
		addStartNpc(HARDIN);
		addTalkId(HARDIN, TOMBSTONE, VON_HELLMAN_DUKE, VON_HELLMAN_PAGE, BROKEN_BOOKSHELF, AGRIPEL, DOMINIC, BENEDICT, INNOCENTIN);
		registerQuestItems(CASKET);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31522-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31522-02.htm".equalsIgnoreCase(event) ? "31522-02.htm" : "shadow_hardin_q0021_02.htm";
		} else if ("31522-01.htm".equalsIgnoreCase(event) || "31522-04.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_1".equalsIgnoreCase(event) || "31523-02.htm".equalsIgnoreCase(event)) {
			return "31523-02.htm".equalsIgnoreCase(event) ? "31523-02.htm" : "q_forest_stone1_q0021_02.htm";
		} else if ("reply_2".equalsIgnoreCase(event) || "31523-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31523-03.htm".equalsIgnoreCase(event) ? "31523-03.htm" : "q_forest_stone1_q0021_03.htm";
		} else if ("31523-01.htm".equalsIgnoreCase(event)) {
			return "31523-01.htm";
		} else if ("reply_3".equalsIgnoreCase(event) || "31524-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31524-06.htm".equalsIgnoreCase(event) ? "31524-06.htm" : "rune_ghost1_q0021_06.htm";
		} else if ("31524-02.htm".equalsIgnoreCase(event) || "31524-03.htm".equalsIgnoreCase(event)
				|| "31524-04.htm".equalsIgnoreCase(event) || "31524-05.htm".equalsIgnoreCase(event)
				|| "rune_ghost1_q0021_02.htm".equalsIgnoreCase(event) || "rune_ghost1_q0021_03.htm".equalsIgnoreCase(event)
				|| "rune_ghost1_q0021_04.htm".equalsIgnoreCase(event) || "rune_ghost1_q0021_05.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("1".equalsIgnoreCase(event) || "2".equalsIgnoreCase(event) || "3".equalsIgnoreCase(event) || "4".equalsIgnoreCase(event) || "despawn".equalsIgnoreCase(event)) {
			return null;
		} else if ("reply_4".equalsIgnoreCase(event) || "31526-03.htm".equalsIgnoreCase(event)) {
			qs.playSound("ItemSound.item_drop_equip_armor_cloth");
			return "31526-03.htm".equalsIgnoreCase(event) ? "31526-03.htm" : "broken_desk1_q0021_03.htm";
		} else if ("reply_5".equalsIgnoreCase(event) || "31526-04.htm".equalsIgnoreCase(event)) {
			return "31526-04.htm".equalsIgnoreCase(event) ? "31526-04.htm" : "broken_desk1_q0021_04.htm";
		} else if ("reply_6".equalsIgnoreCase(event) || "31526-05.htm".equalsIgnoreCase(event)) {
			return "31526-05.htm".equalsIgnoreCase(event) ? "31526-05.htm" : "broken_desk1_q0021_05.htm";
		} else if ("reply_7".equalsIgnoreCase(event) || "31526-07.htm".equalsIgnoreCase(event)) {
			return "31526-07.htm".equalsIgnoreCase(event) ? "31526-07.htm" : "broken_desk1_q0021_07.htm";
		} else if ("reply_8".equalsIgnoreCase(event) || "31526-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31526-08.htm".equalsIgnoreCase(event) ? "31526-08.htm" : "broken_desk1_q0021_08.htm";
		} else if ("reply_9".equalsIgnoreCase(event) || "31526-12.htm".equalsIgnoreCase(event)) {
			return "31526-12.htm".equalsIgnoreCase(event) ? "31526-12.htm" : "broken_desk1_q0021_12.htm";
		} else if ("reply_10".equalsIgnoreCase(event) || "31526-13.htm".equalsIgnoreCase(event)) {
			return "31526-13.htm".equalsIgnoreCase(event) ? "31526-13.htm" : "broken_desk1_q0021_13.htm";
		} else if ("reply_11".equalsIgnoreCase(event) || "31526-14.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() == 5) {
				qs.setCond(6);
				qs.giveItems(CROSS_OF_EINHASAD, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31526-14.htm".equalsIgnoreCase(event) ? "31526-14.htm" : "broken_desk1_q0021_14.htm";
			}
			return "highpriest_innocentin_q0021_02.htm";
		} else if ("31526-01.htm".equalsIgnoreCase(event) || "31526-02.htm".equalsIgnoreCase(event)
				|| "31526-06.htm".equalsIgnoreCase(event) || "31526-09.htm".equalsIgnoreCase(event)
				|| "broken_desk1_q0021_01.htm".equalsIgnoreCase(event) || "broken_desk1_q0021_02.htm".equalsIgnoreCase(event)
				|| "broken_desk1_q0021_06.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_12".equalsIgnoreCase(event) || "31328-02.htm".equalsIgnoreCase(event)) {
			return "31328-02.htm".equalsIgnoreCase(event) ? "31328-02.htm" : "highpriest_innocentin_q0021_03.htm";
		} else if ("reply_13".equalsIgnoreCase(event) || "31328-03.htm".equalsIgnoreCase(event)) {
			return "31328-03.htm".equalsIgnoreCase(event) ? "31328-03.htm" : "highpriest_innocentin_q0021_04.htm";
		} else if ("reply_14".equalsIgnoreCase(event) || "31328-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CASKET, -1);
			if (!qs.hasQuestItems(CROSS_OF_EINHASAD)) {
				qs.giveItems(CROSS_OF_EINHASAD, 1);
			}
			qs.addExpAndSp(131228, 11978);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31328-05.htm".equalsIgnoreCase(event) ? "31328-05.htm" : "highpriest_innocentin_q0021_05.htm";
		} else if ("31328-01.htm".equalsIgnoreCase(event) || "31328-04.htm".equalsIgnoreCase(event)) {
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			if (npc.getNpcId() == INNOCENTIN) {
				return "highpriest_innocentin_q0021_06.htm";
			}
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case HARDIN:
				if (cond == 0) {
					if (pc.level() >= 63) {
						return "shadow_hardin_q0021_01.htm";
					}
					qs.exitQuest(true);
					return "shadow_hardin_q0021_03.htm";
				} else if (cond >= 1) {
					return "shadow_hardin_q0021_05.htm";
				}
				break;
			case TOMBSTONE:
				if (cond == 1) {
					return "q_forest_stone1_q0021_01.htm";
				} else if (cond == 2 || cond == 3) {
					return "q_forest_stone1_q0021_04.htm";
				}
				break;
			case VON_HELLMAN_DUKE:
				if (cond == 2) {
					return "rune_ghost1_q0021_01.htm";
				} else if (cond == 3) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "rune_ghost1_q0021_07.htm";
				} else if (cond >= 4) {
					return "rune_ghost1_q0021_07b.htm";
				}
				break;
			case VON_HELLMAN_PAGE:
				if (cond == 3) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "rune_ghost1b_q0021_01.htm";
				} else if (cond >= 4) {
					return "rune_ghost1b_q0021_02.htm";
				}
				break;
			case BROKEN_BOOKSHELF:
				if (cond == 3 || cond == 4) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "broken_desk1_q0021_01.htm";
				} else if (cond == 5) {
					qs.setCond(6);
					qs.giveItems(CROSS_OF_EINHASAD, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "broken_desk1_q0021_10.htm";
				} else if (cond >= 6) {
					return "broken_desk1_q0021_15.htm";
				}
				break;
			case AGRIPEL:
				if (cond == 6 && qs.hasQuestItems(CROSS_OF_EINHASAD)) {
					qs.set("agripel", "1");
					if (qs.getInt("dominic") == 1 && qs.getInt("benedict") == 1) {
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "falsepriest_agripel_q0021_04.htm";
					}
					return "falsepriest_agripel_q0021_01.htm";
				} else if (cond == 7) {
					return "falsepriest_agripel_q0021_04.htm";
				}
				break;
			case DOMINIC:
				if (cond == 6 && qs.hasQuestItems(CROSS_OF_EINHASAD)) {
					qs.set("dominic", "1");
					if (qs.getInt("agripel") == 1 && qs.getInt("benedict") == 1) {
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "falsepriest_dominic_q0021_04.htm";
					}
					return "falsepriest_dominic_q0021_01.htm";
				} else if (cond == 7) {
					return "falsepriest_dominic_q0021_04.htm";
				}
				break;
			case BENEDICT:
				if (cond == 6 && qs.hasQuestItems(CROSS_OF_EINHASAD)) {
					qs.set("benedict", "1");
					if (qs.getInt("agripel") == 1 && qs.getInt("dominic") == 1) {
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "falsepriest_benedict_q0021_04.htm";
					}
					return "falsepriest_benedict_q0021_01.htm";
				} else if (cond == 7) {
					return "falsepriest_benedict_q0021_04.htm";
				}
				break;
			case INNOCENTIN:
				if ((cond == 6 || cond == 7) && qs.hasQuestItems(CROSS_OF_EINHASAD)) {
					return "highpriest_innocentin_q0021_01.htm";
				}
				break;
		}
		return "noquest";
	}
}
