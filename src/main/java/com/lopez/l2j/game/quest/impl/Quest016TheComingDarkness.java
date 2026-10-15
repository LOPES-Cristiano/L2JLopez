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
 * Quest 016: The Coming Darkness
 * Hierarch em Forest of the Dead e os 5 Altares Malignos.
 */
@Component
public class Quest016TheComingDarkness extends Quest {

	public static final int QUEST_ID = 16;
	public static final String QUEST_NAME = "016_TheComingDarkness";

	public static final int HIERARCH = 31517;
	public static final int EVIL_ALTAR_1 = 31512;
	public static final int EVIL_ALTAR_2 = 31513;
	public static final int EVIL_ALTAR_3 = 31514;
	public static final int EVIL_ALTAR_4 = 31515;
	public static final int EVIL_ALTAR_5 = 31516;
	public static final int[] ALTARS = {EVIL_ALTAR_1, EVIL_ALTAR_2, EVIL_ALTAR_3, EVIL_ALTAR_4, EVIL_ALTAR_5};

	public static final int CRYSTAL_OF_SEAL = 7167;

	@Autowired
	public Quest016TheComingDarkness(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Coming Darkness");
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, EVIL_ALTAR_1, EVIL_ALTAR_2, EVIL_ALTAR_3, EVIL_ALTAR_4, EVIL_ALTAR_5);
		registerQuestItems(CRYSTAL_OF_SEAL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31517-2.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(CRYSTAL_OF_SEAL, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31517-2.htm".equalsIgnoreCase(event) ? "31517-2.htm" : "dark_presbyter_q0016_04.htm";
		} else if ("dark_presbyter_q0016_02.htm".equalsIgnoreCase(event)) {
			return "dark_presbyter_q0016_02.htm";
		} else if ("31512-1.htm".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
				qs.setCond(2);
				qs.takeItems(CRYSTAL_OF_SEAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31512-1.htm".equalsIgnoreCase(event) ? "31512-1.htm" : "vicious_altar1_q0016_02.htm";
			}
			return "vicious_altar1_q0016_03.htm";
		} else if ("31513-1.htm".equalsIgnoreCase(event) || "reply_1a".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
				qs.setCond(3);
				qs.takeItems(CRYSTAL_OF_SEAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31513-1.htm".equalsIgnoreCase(event) ? "31513-1.htm" : "vicious_altar2_q0016_02.htm";
			}
			return "vicious_altar2_q0016_03.htm";
		} else if ("31514-1.htm".equalsIgnoreCase(event) || "reply_1b".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
				qs.setCond(4);
				qs.takeItems(CRYSTAL_OF_SEAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31514-1.htm".equalsIgnoreCase(event) ? "31514-1.htm" : "vicious_altar3_q0016_02.htm";
			}
			return "vicious_altar3_q0016_03.htm";
		} else if ("31515-1.htm".equalsIgnoreCase(event) || "reply_1c".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
				qs.setCond(5);
				qs.takeItems(CRYSTAL_OF_SEAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31515-1.htm".equalsIgnoreCase(event) ? "31515-1.htm" : "vicious_altar4_q0016_02.htm";
			}
			return "vicious_altar4_q0016_03.htm";
		} else if ("31516-1.htm".equalsIgnoreCase(event) || "reply_1d".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
				qs.setCond(6);
				qs.takeItems(CRYSTAL_OF_SEAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31516-1.htm".equalsIgnoreCase(event) ? "31516-1.htm" : "vicious_altar5_q0016_02.htm";
			}
			return "vicious_altar5_q0016_03.htm";
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
			case HIERARCH:
				if (cond == 0) {
					if (pc.level() >= 61) {
						return "dark_presbyter_q0016_01.htm";
					}
					qs.exitQuest(true);
					return "dark_presbyter_q0016_03.htm";
				} else if (cond >= 1 && cond <= 5) {
					if (!qs.hasQuestItems(CRYSTAL_OF_SEAL)) {
						qs.playSound(QuestState.SOUND_FINISH);
						qs.exitQuest(true);
						return "dark_presbyter_q0016_07.htm";
					}
					return "dark_presbyter_q0016_05.htm";
				} else if (cond == 6) {
					qs.takeItems(CRYSTAL_OF_SEAL, -1);
					qs.addExpAndSp(221958, 0);
					qs.giveItems(57, 86515);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "dark_presbyter_q0016_06.htm";
				}
				break;
			case EVIL_ALTAR_1:
				if (cond == 1) {
					return qs.hasQuestItems(CRYSTAL_OF_SEAL) ? "vicious_altar1_q0016_01.htm" : "vicious_altar1_q0016_04.htm";
				} else if (cond > 1) {
					return "vicious_altar1_q0016_05.htm";
				}
				break;
			case EVIL_ALTAR_2:
				if (cond == 2) {
					return qs.hasQuestItems(CRYSTAL_OF_SEAL) ? "vicious_altar2_q0016_01.htm" : "vicious_altar2_q0016_04.htm";
				} else if (cond > 2) {
					return "vicious_altar2_q0016_05.htm";
				}
				break;
			case EVIL_ALTAR_3:
				if (cond == 3) {
					return qs.hasQuestItems(CRYSTAL_OF_SEAL) ? "vicious_altar3_q0016_01.htm" : "vicious_altar3_q0016_04.htm";
				} else if (cond > 3) {
					return "vicious_altar3_q0016_05.htm";
				}
				break;
			case EVIL_ALTAR_4:
				if (cond == 4) {
					return qs.hasQuestItems(CRYSTAL_OF_SEAL) ? "vicious_altar4_q0016_01.htm" : "vicious_altar4_q0016_04.htm";
				} else if (cond > 4) {
					return "vicious_altar4_q0016_05.htm";
				}
				break;
			case EVIL_ALTAR_5:
				if (cond == 5) {
					return qs.hasQuestItems(CRYSTAL_OF_SEAL) ? "vicious_altar5_q0016_01.htm" : "vicious_altar5_q0016_04.htm";
				} else if (cond > 5) {
					return "vicious_altar5_q0016_05.htm";
				}
				break;
		}
		return "noquest";
	}
}
