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
 * Quest 121: Pavel The Giants
 */
@Component
public class Quest121PavelTheGiants extends Quest {

	public static final int QUEST_ID = 121;
	public static final String QUEST_NAME = "121_PavelTheGiants";
	public static final int START_NPC = 32046;

	@Autowired
	public Quest121PavelTheGiants(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Pavel The Giants");
		addStartNpc(START_NPC);
		addTalkId(32046);
		addTalkId(32041);
		
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
			return "newheart_q0121_0104.htm";
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
				if (pc.level() >= 70) return "newheart_q0121_0101.htm";
				qs.exitQuest(true);
				return "newheart_q0121_0103.htm";
			} else if (cond == 1) {
				return "newheart_q0121_0105.htm";
			}
		} else if (npcId == 32041 && cond == 1) {
			qs.giveItems(57, 34000);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "collecter_yumi_q0121_0201.htm";
		}
		return "noquest";
	}
}
