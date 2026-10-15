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
 * Quest 017: Light and Darkness
 * Hierarch em Forest of the Dead e os 4 Altares Abençoados.
 */
@Component
public class Quest017LightAndDarkness extends Quest {

	public static final int QUEST_ID = 17;
	public static final String QUEST_NAME = "017_LightAndDarkness";

	public static final int HIERARCH = 31517;
	public static final int SAINT_ALTAR_1 = 31508;
	public static final int SAINT_ALTAR_2 = 31509;
	public static final int SAINT_ALTAR_3 = 31510;
	public static final int SAINT_ALTAR_4 = 31511;
	public static final int[] ALTARS = {SAINT_ALTAR_1, SAINT_ALTAR_2, SAINT_ALTAR_3, SAINT_ALTAR_4};

	public static final int BLOOD_OF_SAINT = 7168;

	@Autowired
	public Quest017LightAndDarkness(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Light and Darkness");
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, SAINT_ALTAR_1, SAINT_ALTAR_2, SAINT_ALTAR_3, SAINT_ALTAR_4);
		registerQuestItems(BLOOD_OF_SAINT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31517-04.htm".equalsIgnoreCase(event) || "dark_presbyter_q0017_04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BLOOD_OF_SAINT, 4);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "dark_presbyter_q0017_04.htm";
		} else if ("31508-02.htm".equalsIgnoreCase(event) || "blessed_altar1_q0017_02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BLOOD_OF_SAINT)) {
				qs.setCond(2);
				qs.takeItems(BLOOD_OF_SAINT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "blessed_altar1_q0017_03.htm";
		} else if ("31509-02.htm".equalsIgnoreCase(event) || "blessed_altar2_q0017_02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BLOOD_OF_SAINT)) {
				qs.setCond(3);
				qs.takeItems(BLOOD_OF_SAINT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "blessed_altar2_q0017_03.htm";
		} else if ("31510-02.htm".equalsIgnoreCase(event) || "blessed_altar3_q0017_02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BLOOD_OF_SAINT)) {
				qs.setCond(4);
				qs.takeItems(BLOOD_OF_SAINT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "blessed_altar3_q0017_03.htm";
		} else if ("31511-02.htm".equalsIgnoreCase(event) || "blessed_altar4_q0017_02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BLOOD_OF_SAINT)) {
				qs.setCond(5);
				qs.takeItems(BLOOD_OF_SAINT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "blessed_altar4_q0017_03.htm";
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
						return "dark_presbyter_q0017_01.htm";
					}
					qs.exitQuest(true);
					return "dark_presbyter_q0017_03.htm";
				} else if (cond >= 1 && cond <= 4) {
					if (!qs.hasQuestItems(BLOOD_OF_SAINT)) {
						qs.playSound(QuestState.SOUND_FINISH);
						qs.exitQuest(true);
						return "dark_presbyter_q0017_06.htm";
					}
					return "dark_presbyter_q0017_05.htm";
				} else if (cond == 5) {
					qs.takeItems(BLOOD_OF_SAINT, -1);
					qs.addExpAndSp(105527, 0);
					qs.giveItems(57, 69140);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "dark_presbyter_q0017_07.htm";
				}
				break;
			case SAINT_ALTAR_1:
				if (cond == 1) {
					return qs.hasQuestItems(BLOOD_OF_SAINT) ? "blessed_altar1_q0017_01.htm" : "blessed_altar1_q0017_03.htm";
				} else if (cond > 1) {
					return "blessed_altar1_q0017_05.htm";
				}
				break;
			case SAINT_ALTAR_2:
				if (cond == 2) {
					return qs.hasQuestItems(BLOOD_OF_SAINT) ? "blessed_altar2_q0017_01.htm" : "blessed_altar2_q0017_03.htm";
				} else if (cond > 2) {
					return "blessed_altar2_q0017_05.htm";
				}
				break;
			case SAINT_ALTAR_3:
				if (cond == 3) {
					return qs.hasQuestItems(BLOOD_OF_SAINT) ? "blessed_altar3_q0017_01.htm" : "blessed_altar3_q0017_03.htm";
				} else if (cond > 3) {
					return "blessed_altar3_q0017_05.htm";
				}
				break;
			case SAINT_ALTAR_4:
				if (cond == 4) {
					return qs.hasQuestItems(BLOOD_OF_SAINT) ? "blessed_altar4_q0017_01.htm" : "blessed_altar4_q0017_03.htm";
				} else if (cond > 4) {
					return "blessed_altar4_q0017_05.htm";
				}
				break;
		}
		return "noquest";
	}
}
