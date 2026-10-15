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
 * Quest 106: Forgotten Truth
 */
@Component
public class Quest106ForgottenTruth extends Quest {

	public static final int QUEST_ID = 106;
	public static final String QUEST_NAME = "106_ForgottenTruth";
	public static final int START_NPC = 30358;

	public static final int THIFIELL = 30358;
	public static final int KARTIA = 30133;

	public static final int ONYX_TALISMAN_1 = 984;
	public static final int ONYX_TALISMAN_2 = 985;
	public static final int ANCIENT_SCROLL = 986;
	public static final int ANCIENT_CLAY_TABLET = 987;
	public static final int KARTIA_TRANSLATION = 988;

	public static final int ELDRITCH_DAGGER_REWARD = 753;
	public static final int ELDRITCH_DAGGER = 989;

	@Autowired
	public Quest106ForgottenTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Forgotten Truth");
		addStartNpc(START_NPC);
		addTalkId(THIFIELL, KARTIA);
		addKillId(27070);
		registerQuestItems(ONYX_TALISMAN_1, ONYX_TALISMAN_2, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET, KARTIA_TRANSLATION);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		if ("quest_accept".equalsIgnoreCase(event)
				|| "30358-05.htm".equalsIgnoreCase(event)
				|| "tetrarch_thifiell_q0106_05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(ONYX_TALISMAN_1)) {
				qs.giveItems(ONYX_TALISMAN_1, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "tetrarch_thifiell_q0106_05.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		if (qs.isCompleted()) {
			return "completed";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == THIFIELL) {
			if (cond == 0) {
				if (pc.race() != 2) {
					qs.exitQuest(true);
					return "tetrarch_thifiell_q0106_00.htm";
				}
				if (pc.level() >= 10) {
					return "tetrarch_thifiell_q0106_03.htm";
				}
				qs.exitQuest(true);
				return "tetrarch_thifiell_q0106_02.htm";
			} else if (cond == 1 || cond == 3) {
				return "tetrarch_thifiell_q0106_06.htm";
			} else if (cond == 2) {
				qs.takeItems(ONYX_TALISMAN_2, -1);
				qs.giveItems(ELDRITCH_DAGGER_REWARD, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "tetrarch_thifiell_q0106_08.htm";
			} else if (cond == 4) {
				qs.takeItems(KARTIA_TRANSLATION, -1);
				qs.giveItems(ELDRITCH_DAGGER_REWARD, 1);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "tetrarch_thifiell_q0106_07.htm";
			}
		} else if (npcId == KARTIA) {
			if (cond == 1) {
				qs.takeItems(ONYX_TALISMAN_1, -1);
				qs.giveItems(ONYX_TALISMAN_2, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "kartia_q0106_01.htm";
			} else if (cond == 2) {
				return "karta_q0106_02.htm";
			} else if (cond == 3) {
				qs.takeAllItems(ONYX_TALISMAN_2, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET);
				qs.giveItems(KARTIA_TRANSLATION, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "karta_q0106_03.htm";
			} else if (cond == 4) {
				return "karta_q0106_04.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		if (npc.getNpcId() == 27070 && qs.getCond() == 2) {
			if (!qs.hasQuestItems(ANCIENT_SCROLL)) {
				qs.rollAndGive(ANCIENT_SCROLL, 1, 20.0);
			} else if (!qs.hasQuestItems(ANCIENT_CLAY_TABLET)) {
				qs.rollAndGive(ANCIENT_CLAY_TABLET, 1, 20.0);
			}
			if (qs.hasQuestItems(ANCIENT_SCROLL) && qs.hasQuestItems(ANCIENT_CLAY_TABLET)) {
				qs.setCond(3);
			}
		}
		return null;
	}
}
