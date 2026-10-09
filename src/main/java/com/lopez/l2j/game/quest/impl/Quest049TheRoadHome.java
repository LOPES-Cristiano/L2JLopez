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
 * Quest 49: The Road Home
 */
@Component
public class Quest049TheRoadHome extends Quest {

	public static final int QUEST_ID = 49;
	public static final String QUEST_NAME = "049_TheRoadHome";

	public static final int START_NPC = 30097;

	@Autowired
	public Quest049TheRoadHome(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Road Home");
		addStartNpc(START_NPC);
		addTalkId(30097);
		registerQuestItems(7558);
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
			return "galladuchi_q0049_0104.htm";
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
				if (pc.level() >= 1 && pc.race() == 4) return "galladuchi_q0049_0101.htm";
				qs.exitQuest(true);
				return "galladuchi_q0049_0103.htm";
			} else if (cond == 1) {
				qs.giveItems(7558, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "galladuchi_q0049_0301.htm";
			}
		}
		return "noquest";
	}
}
