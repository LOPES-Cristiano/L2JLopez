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
 * Quest 023: Lidia's Heart
 * High Priest Innocentin em Rune, Broken Bookshelf, Tombstone, Ghost of von
 * Hellmann,
 * Violet e Box em Forest of the Dead.
 */
@Component
public class Quest023LidiasHeart extends Quest {

	public static final int QUEST_ID = 23;
	public static final String QUEST_NAME = "023_LidiasHeart";

	public static final int INNOCENTIN = 31328;
	public static final int BOOKSHELF = 31526;
	public static final int BROKEN_BOOKSHELF = 31526;
	public static final int GHOST_OF_VON_HELLMANN = 31524;
	public static final int TOMBSTONE = 31523;
	public static final int VIOLET = 31386;
	public static final int BOX = 31530;

	public static final int MAP_FOREST = 7063;
	public static final int MAP_FOREST_OF_DEADMAN = 7063;
	public static final int SILVER_KEY = 7149;
	public static final int LIDIAS_HAIRPIN = 7148;
	public static final int LIDIAS_DIARY = 7064;
	public static final int SILVER_SPEAR = 7150;

	@Autowired
	public Quest023LidiasHeart(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Lidia's Heart");
		addStartNpc(INNOCENTIN);
		addTalkId(INNOCENTIN, BROKEN_BOOKSHELF, GHOST_OF_VON_HELLMANN, TOMBSTONE, VIOLET, BOX);
		registerQuestItems(MAP_FOREST_OF_DEADMAN, LIDIAS_DIARY, LIDIAS_HAIRPIN, SILVER_KEY, SILVER_SPEAR);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31328-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(MAP_FOREST_OF_DEADMAN, 1);
			qs.giveItems(SILVER_KEY, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31328-02.htm".equalsIgnoreCase(event) ? "31328-02.htm" : "highpriest_innocentin_q0023_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event) || "31328-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-03.htm".equalsIgnoreCase(event) ? "31328-03.htm" : "highpriest_innocentin_q0023_05.htm";
		} else if ("reply_2".equalsIgnoreCase(event) || "31328-05.htm".equalsIgnoreCase(event)) {
			return "31328-05.htm".equalsIgnoreCase(event) ? "31328-05.htm" : "highpriest_innocentin_q0023_06.htm";
		} else if ("reply_3".equalsIgnoreCase(event) || "31328-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-06.htm".equalsIgnoreCase(event) ? "31328-06.htm" : "highpriest_innocentin_q0023_07.htm";
		} else if ("reply_4".equalsIgnoreCase(event) || "31526-01.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SILVER_KEY)) {
				qs.takeItems(SILVER_KEY, 1);
				return "31526-01.htm".equalsIgnoreCase(event) ? "31526-01.htm" : "broken_desk1_q0023_02.htm";
			}
			return "broken_desk1_q0023_03.htm";
		} else if ("reply_5".equalsIgnoreCase(event) || "31526-03.htm".equalsIgnoreCase(event)) {
			return "31526-03.htm".equalsIgnoreCase(event) ? "31526-03.htm" : "broken_desk1_q0023_04.htm";
		} else if ("reply_6".equalsIgnoreCase(event) || "31526-07.htm".equalsIgnoreCase(event)) {
			return "31526-07.htm".equalsIgnoreCase(event) ? "31526-07.htm" : "broken_desk1_q0023_08.htm";
		} else if ("reply_7".equalsIgnoreCase(event) || "31526-04.htm".equalsIgnoreCase(event)) {
			return "31526-04.htm".equalsIgnoreCase(event) ? "31526-04.htm" : "broken_desk1_q0023_05.htm";
		} else if ("reply_8".equalsIgnoreCase(event) || "31526-05.htm".equalsIgnoreCase(event)) {
			qs.giveItems(LIDIAS_HAIRPIN, 1);
			if (qs.hasQuestItems(LIDIAS_DIARY)) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "31526-05.htm".equalsIgnoreCase(event) ? "31526-05.htm" : "broken_desk1_q0023_06.htm";
		} else if ("reply_9".equalsIgnoreCase(event)) {
			return "broken_desk1_q0023_07.htm";
		} else if ("reply_10".equalsIgnoreCase(event) || "31526-08.htm".equalsIgnoreCase(event)) {
			return "31526-08.htm".equalsIgnoreCase(event) ? "31526-08.htm" : "broken_desk1_q0023_09.htm";
		} else if ("reply_11".equalsIgnoreCase(event) || "31526-09.htm".equalsIgnoreCase(event)) {
			return "31526-09.htm".equalsIgnoreCase(event) ? "31526-09.htm" : "broken_desk1_q0023_10.htm";
		} else if ("31526-10.htm".equalsIgnoreCase(event)) {
			return "31526-10.htm";
		} else if ("reply_12".equalsIgnoreCase(event) || "31526-11.htm".equalsIgnoreCase(event)) {
			qs.giveItems(LIDIAS_DIARY, 1);
			if (qs.hasQuestItems(LIDIAS_HAIRPIN)) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "31526-11.htm".equalsIgnoreCase(event) ? "31526-11.htm" : "broken_desk1_q0023_11.htm";
		} else if ("highpriest_innocentin_q0023_10.htm".equalsIgnoreCase(event)
				|| "31328-09.htm".equalsIgnoreCase(event) || "31328-10.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_13".equalsIgnoreCase(event) || "31328-11.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-11.htm".equalsIgnoreCase(event) ? "31328-11.htm" : "highpriest_innocentin_q0023_11.htm";
		} else if ("reply_14".equalsIgnoreCase(event) || "31328-12.htm".equalsIgnoreCase(event)) {
			return "31328-12.htm".equalsIgnoreCase(event) ? "31328-12.htm" : "highpriest_innocentin_q0023_12.htm";
		} else if ("reply_15".equalsIgnoreCase(event) || "31328-13.htm".equalsIgnoreCase(event)) {
			return "31328-13.htm".equalsIgnoreCase(event) ? "31328-13.htm" : "highpriest_innocentin_q0023_13.htm";
		} else if ("31328-14.htm".equalsIgnoreCase(event) || "31328-15.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_16".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "highpriest_innocentin_q0023_21.htm";
		} else if ("reply_17".equalsIgnoreCase(event) || "31328-16.htm".equalsIgnoreCase(event)) {
			return "31328-16.htm".equalsIgnoreCase(event) ? "31328-16.htm" : "highpriest_innocentin_q0023_16.htm";
		} else if ("reply_18".equalsIgnoreCase(event) || "31328-17.htm".equalsIgnoreCase(event)) {
			return "31328-17.htm".equalsIgnoreCase(event) ? "31328-17.htm" : "highpriest_innocentin_q0023_17.htm";
		} else if ("reply_19".equalsIgnoreCase(event) || "31328-18.htm".equalsIgnoreCase(event)) {
			return "31328-18.htm".equalsIgnoreCase(event) ? "31328-18.htm" : "highpriest_innocentin_q0023_18.htm";
		} else if ("reply_20".equalsIgnoreCase(event) || "31328-19.htm".equalsIgnoreCase(event)) {
			return "31328-19.htm".equalsIgnoreCase(event) ? "31328-19.htm" : "highpriest_innocentin_q0023_19.htm";
		} else if ("reply_21".equalsIgnoreCase(event) || "31328-20.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31328-20.htm".equalsIgnoreCase(event) ? "31328-20.htm" : "highpriest_innocentin_q0023_20.htm";
		} else if ("reply_22".equalsIgnoreCase(event) || "31523-02.htm".equalsIgnoreCase(event)) {
			qs.playSound("SkillSound5.horror_02");
			qs.addSpawn(GHOST_OF_VON_HELLMANN, 51432, -54570, -3136, 300000);
			return "31523-02.htm".equalsIgnoreCase(event) ? "31523-02.htm" : "q_forest_stone1_q0023_02.htm";
		} else if ("reply_23".equalsIgnoreCase(event) || "31524-02.htm".equalsIgnoreCase(event)) {
			return "31524-02.htm".equalsIgnoreCase(event) ? "31524-02.htm" : "rune_ghost1_q0023_02.htm";
		} else if ("reply_24".equalsIgnoreCase(event) || "31524-03.htm".equalsIgnoreCase(event)) {
			return "31524-03.htm".equalsIgnoreCase(event) ? "31524-03.htm" : "rune_ghost1_q0023_03.htm";
		} else if ("reply_25".equalsIgnoreCase(event) || "31524-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.takeItems(LIDIAS_DIARY, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31524-04.htm".equalsIgnoreCase(event) ? "31524-04.htm" : "rune_ghost1_q0023_04.htm";
		} else if ("reply_26".equalsIgnoreCase(event) || "31523-05.htm".equalsIgnoreCase(event)) {
			qs.giveItems(SILVER_KEY, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31523-05.htm".equalsIgnoreCase(event) ? "31523-05.htm" : "q_forest_stone1_q0023_05.htm";
		} else if ("reply_27".equalsIgnoreCase(event) || "31530-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.takeItems(SILVER_KEY, 1);
			qs.giveItems(SILVER_SPEAR, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "31530-02.htm".equalsIgnoreCase(event) ? "31530-02.htm" : "rust_box1_q0023_02.htm";
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
			case INNOCENTIN:
				if (cond == 0) {
					if (pc.level() >= 64) {
						return "highpriest_innocentin_q0023_01.htm";
					}
					qs.exitQuest(true);
					return "highpriest_innocentin_q0023_02.htm";
				} else if (cond == 1) {
					return "highpriest_innocentin_q0023_03.htm";
				} else if (cond == 2) {
					return "highpriest_innocentin_q0023_08.htm";
				} else if (cond == 4) {
					return "highpriest_innocentin_q0023_09.htm";
				} else if (cond == 5) {
					return "highpriest_innocentin_q0023_14.htm";
				} else if (cond >= 6) {
					return "highpriest_innocentin_q0023_21.htm";
				}
				break;
			case BROKEN_BOOKSHELF:
				if (cond == 2 && qs.hasQuestItems(SILVER_KEY)) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "broken_desk1_q0023_01.htm";
				} else if (cond == 3) {
					if (!qs.hasQuestItems(LIDIAS_HAIRPIN) && qs.hasQuestItems(LIDIAS_DIARY)) {
						return "broken_desk1_q0023_12.htm";
					} else if (qs.hasQuestItems(LIDIAS_HAIRPIN) && !qs.hasQuestItems(LIDIAS_DIARY)) {
						return "broken_desk1_q0023_07.htm";
					} else if (!qs.hasQuestItems(LIDIAS_HAIRPIN) && !qs.hasQuestItems(LIDIAS_DIARY)) {
						return "broken_desk1_q0023_02.htm";
					}
				} else if (cond >= 4) {
					return "broken_desk1_q0023_13.htm";
				}
				break;
			case TOMBSTONE:
				if (cond == 6) {
					return "q_forest_stone1_q0023_01.htm";
				} else if (cond == 7) {
					return "q_forest_stone1_q0023_04.htm";
				} else if (cond >= 8) {
					return "q_forest_stone1_q0023_06.htm";
				}
				break;
			case GHOST_OF_VON_HELLMANN:
				if (cond == 6) {
					return "rune_ghost1_q0023_01.htm";
				} else if (cond == 7) {
					return "rune_ghost1_q0023_05.htm";
				} else if (cond >= 8) {
					return "rune_ghost1_q0023_06.htm";
				}
				break;
			case VIOLET:
				if (cond == 8) {
					qs.setCond(9);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "day_violet_q0023_01.htm";
				} else if (cond == 9) {
					return "day_violet_q0023_02.htm";
				} else if (cond == 10 && qs.hasQuestItems(SILVER_SPEAR)) {
					qs.takeItems(SILVER_SPEAR, 1);
					qs.giveItems(57, 350000);
					qs.addExpAndSp(456893, 42112);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "day_violet_q0023_03.htm";
				}
				break;
			case BOX:
				if (cond == 9 && qs.hasQuestItems(SILVER_KEY)) {
					return "rust_box1_q0023_01.htm";
				} else if (cond >= 10) {
					return "rust_box1_q0023_03.htm";
				}
				break;
		}
		return "noquest";
	}
}
