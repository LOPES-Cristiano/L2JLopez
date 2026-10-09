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
 * Quest 34: In Search of Clothes
 */
@Component
public class Quest034InSearchOfClothes extends Quest {

	public static final int QUEST_ID = 34;
	public static final String QUEST_NAME = "034_InSearchofClothes";

	public static final int START_NPC = 30088;

	@Autowired
	public Quest034InSearchOfClothes(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In Search of Clothes");
		addStartNpc(START_NPC);
		addTalkId(30088);
		addTalkId(30165);
		registerQuestItems(7076);
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
			return "radia_q0034_0104.htm";
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
				if (pc.level() >= 60) return "radia_q0034_0101.htm";
				qs.exitQuest(true);
				return "radia_q0034_0103.htm";
			} else if (cond == 1) {
				return "radia_q0034_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(7076, 1); // Mysterious Cloth
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "radia_q0034_0601.htm";
			}
		} else if (npcId == 30165 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "ralford_q0034_0201.htm";
		}
		return "noquest";
	}
}
