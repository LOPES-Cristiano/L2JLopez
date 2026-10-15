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
 * Quest 32: An Obvious Lie
 * High Priest Maximilian, Magic Trader Gentler e Miki the Cat.
 */
@Component
public class Quest032AnObviousLie extends Quest {

	public static final int QUEST_ID = 32;
	public static final String QUEST_NAME = "032_AnObviousLie";

	public static final int START_NPC = 30120;
	public static final int MAXIMILIAN = 30120;
	public static final int GENTLER = 30094;
	public static final int MIKI_THE_CAT = 31706;

	public static final int ALLIGATOR = 20135;

	public static final int SUEDE = 1866;
	public static final int THREAD = 1868;
	public static final int SPIRIT_ORE = 3031;
	public static final int MAP = 7165;
	public static final int MEDICINAL_HERB = 7166;

	public static final int CAT_EARS = 6843;
	public static final int RACCOON_EARS = 7680;
	public static final int RABBIT_EARS = 7683;

	@Autowired
	public Quest032AnObviousLie(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "An Obvious Lie");
		addStartNpc(MAXIMILIAN);
		addTalkId(MAXIMILIAN, GENTLER, MIKI_THE_CAT);
		addKillId(ALLIGATOR);
		registerQuestItems(MAP, MEDICINAL_HERB);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "30120-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30120-1.htm".equalsIgnoreCase(event) ? "30120-1.htm" : "maximilian_q0032_0104.htm";
		} else if ("30094-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(MAP, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30094-1.htm";
		} else if ("31706-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.takeItems(MAP, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31706-1.htm";
		} else if ("30094-4.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.takeItems(MEDICINAL_HERB, 20);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30094-4.htm";
		} else if ("30094-7.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(SPIRIT_ORE) < 500) {
				return "30094-5.htm";
			}
			qs.setCond(6);
			qs.takeItems(SPIRIT_ORE, 500);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30094-7.htm";
		} else if ("31706-4.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31706-4.htm";
		} else if ("30094-10.htm".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30094-10.htm";
		} else if ("30094-13.htm".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30094-13.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.giveItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gentler_q0032_0201.htm";
			} else if (cond == 2 && qs.hasQuestItems(MAP)) {
				qs.setCond(3);
				qs.takeItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "miki_the_cat_q0032_0301.htm";
			} else if (cond == 4 && qs.getQuestItemsCount(MEDICINAL_HERB) >= 20) {
				qs.setCond(5);
				qs.takeItems(MEDICINAL_HERB, 20);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gentler_q0032_0401.htm";
			} else if (cond == 5) {
				if (qs.getQuestItemsCount(SPIRIT_ORE) < 500) {
					return "gentler_q0032_0503.htm";
				}
				qs.setCond(6);
				qs.takeItems(SPIRIT_ORE, 500);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gentler_q0032_0501.htm";
			} else if (cond == 6) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "miki_the_cat_q0032_0601.htm";
			} else if (cond == 7) {
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gentler_q0032_0701.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(THREAD) >= 1000 && qs.getQuestItemsCount(SUEDE) >= 500) {
				return "gentler_q0032_0801.htm";
			}
			return "gentler_q0032_0703.htm";
		} else if ("cat".equalsIgnoreCase(event) || "reply_11".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(THREAD) < 1000 || qs.getQuestItemsCount(SUEDE) < 500) {
				return "gentler_q0032_0802.htm";
			}
			qs.takeItems(THREAD, 1000);
			qs.takeItems(SUEDE, 500);
			qs.giveItems(CAT_EARS, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "cat".equalsIgnoreCase(event) ? "30094-14.htm" : "gentler_q0032_0803.htm";
		} else if ("racoon".equalsIgnoreCase(event) || "reply_12".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(THREAD) < 1000 || qs.getQuestItemsCount(SUEDE) < 500) {
				return "gentler_q0032_0802.htm";
			}
			qs.takeItems(THREAD, 1000);
			qs.takeItems(SUEDE, 500);
			qs.giveItems(RACCOON_EARS, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "racoon".equalsIgnoreCase(event) ? "30094-14.htm" : "gentler_q0032_0803.htm";
		} else if ("rabbit".equalsIgnoreCase(event) || "reply_13".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(THREAD) < 1000 || qs.getQuestItemsCount(SUEDE) < 500) {
				return "gentler_q0032_0802.htm";
			}
			qs.takeItems(THREAD, 1000);
			qs.takeItems(SUEDE, 500);
			qs.giveItems(RABBIT_EARS, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "rabbit".equalsIgnoreCase(event) ? "30094-14.htm" : "gentler_q0032_0803.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case MAXIMILIAN:
				if (cond == 0) {
					if (pc.level() >= 45) return "maximilian_q0032_0101.htm";
					qs.exitQuest(true);
					return "maximilian_q0032_0103.htm";
				} else if (cond >= 1) {
					return "maximilian_q0032_0105.htm";
				}
				break;
			case GENTLER:
				if (cond == 1) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "gentler_q0032_0201.htm";
				} else if (cond == 2) {
					if (qs.hasQuestItems(MAP)) {
						return "gentler_q0032_0202.htm";
					}
					qs.giveItems(CAT_EARS, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "gentler_q0032_0401.htm";
				} else if (cond == 3) {
					return "gentler_q0032_0302.htm";
				} else if (cond == 4) {
					return qs.getQuestItemsCount(MEDICINAL_HERB) >= 20 ? "gentler_q0032_0301.htm" : "gentler_q0032_0302.htm";
				} else if (cond == 5) {
					return qs.getQuestItemsCount(SPIRIT_ORE) >= 500 ? "gentler_q0032_0403.htm" : "gentler_q0032_0404.htm";
				} else if (cond == 6) {
					return "gentler_q0032_0601.htm";
				} else if (cond == 7) {
					return "gentler_q0032_0601.htm";
				} else if (cond == 8) {
					return "gentler_q0032_0702.htm";
				}
				break;
			case MIKI_THE_CAT:
				if (cond == 2 && qs.hasQuestItems(MAP)) {
					return "miki_the_cat_q0032_0201.htm";
				} else if (cond == 3 || cond == 4) {
					return "miki_the_cat_q0032_0303.htm";
				} else if (cond == 6) {
					return "miki_the_cat_q0032_0501.htm";
				} else if (cond >= 7) {
					return "miki_the_cat_q0032_0602.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == ALLIGATOR && qs.getCond() == 3) {
			long count = qs.getQuestItemsCount(MEDICINAL_HERB);
			if (count < 20) {
				qs.giveItems(MEDICINAL_HERB, 1);
				if (count + 1 >= 20) {
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
