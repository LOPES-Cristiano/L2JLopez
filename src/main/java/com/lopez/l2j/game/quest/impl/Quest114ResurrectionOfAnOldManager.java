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
 * Quest 114: Resurrection of an Old Manager
 */
@Component
public class Quest114ResurrectionOfAnOldManager extends Quest {

	public static final int QUEST_ID = 114;
	public static final String QUEST_NAME = "114_ResurrectionOfAnOldManager";
	public static final int START_NPC = 32041;

	@Autowired
	public Quest114ResurrectionOfAnOldManager(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Resurrection of an Old Manager");
		addStartNpc(START_NPC);
		addTalkId(32041);
		addTalkId(32047);
		
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
			return "collecter_yumi_q0114_0104.htm";
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
				if (pc.level() >= 49) return "collecter_yumi_q0114_0101.htm";
				qs.exitQuest(true);
				return "collecter_yumi_q0114_0103.htm";
			} else if (cond == 1) {
				return "collecter_yumi_q0114_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 70362);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collecter_yumi_q0114_0202.htm";
			}
		} else if (npcId == 32047 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "wendy_q0114_0201.htm";
		}
		return "noquest";
	}
}
