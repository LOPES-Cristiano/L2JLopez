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

	@Autowired
	public Quest106ForgottenTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Forgotten Truth");
		addStartNpc(START_NPC);
		addTalkId(30358);
		addTalkId(30133);
		registerQuestItems(753);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "tetrarch_thifiell_q0106_05.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == START_NPC) {
			if (cond == 0) {
				if (pc.level() >= 10 && pc.race() == 2) return "tetrarch_thifiell_q0106_03.htm";
				qs.exitQuest(true);
				return "tetrarch_thifiell_q0106_00.htm";
			} else if (cond == 1) {
				return "tetrarch_thifiell_q0106_06.htm";
			} else if (cond == 2) {
				qs.giveItems(753, 1); // Eldritch Dagger
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "tetrarch_thifiell_q0106_08.htm";
			}
		} else if (npcId == 30133 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "kartia_q0106_01.htm";
		}
		return "noquest";
	}
}
