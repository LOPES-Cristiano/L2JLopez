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
 * Quest 115: The Other Side of Truth
 */
@Component
public class Quest115TheOtherSideOfTruth extends Quest {

	public static final int QUEST_ID = 115;
	public static final String QUEST_NAME = "115_TheOtherSideOfTruth";
	public static final int START_NPC = 32020;

	@Autowired
	public Quest115TheOtherSideOfTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Other Side of Truth");
		addStartNpc(START_NPC);
		addTalkId(32020);
		addTalkId(32018);
		
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
			return "rafferty_q0115_0104.htm";
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
				if (pc.level() >= 53) return "rafferty_q0115_0101.htm";
				qs.exitQuest(true);
				return "rafferty_q0115_0103.htm";
			} else if (cond == 1) {
				return "rafferty_q0115_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 60044);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "rafferty_q0115_0202.htm";
			}
		} else if (npcId == 32018 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "misa_q0115_0201.htm";
		}
		return "noquest";
	}
}
