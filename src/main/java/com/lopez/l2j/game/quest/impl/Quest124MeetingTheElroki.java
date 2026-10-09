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
 * Quest 124: Meeting the Elroki
 */
@Component
public class Quest124MeetingTheElroki extends Quest {

	public static final int QUEST_ID = 124;
	public static final String QUEST_NAME = "124_MeetingTheElroki";
	public static final int START_NPC = 32113;

	@Autowired
	public Quest124MeetingTheElroki(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Meeting the Elroki");
		addStartNpc(START_NPC);
		addTalkId(32113);
		addTalkId(32115);
		
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
			return "marquez_q0124_0104.htm";
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
				if (pc.level() >= 75) return "marquez_q0124_0101.htm";
				qs.exitQuest(true);
				return "marquez_q0124_0103.htm";
			} else if (cond == 1) {
				return "marquez_q0124_0105.htm";
			}
		} else if (npcId == 32115 && cond == 1) {
			qs.giveItems(57, 71360);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "mushika_q0124_0201.htm";
		}
		return "noquest";
	}
}
