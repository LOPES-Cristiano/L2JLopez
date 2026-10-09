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
 * Quest 122: Ominous News
 */
@Component
public class Quest122OminousNews extends Quest {

	public static final int QUEST_ID = 122;
	public static final String QUEST_NAME = "122_OminousNews";
	public static final int START_NPC = 31979;

	@Autowired
	public Quest122OminousNews(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Ominous News");
		addStartNpc(START_NPC);
		addTalkId(31979);
		addTalkId(32017);
		
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
			return "seer_moira_q0122_0104.htm";
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
				if (pc.level() >= 20) return "seer_moira_q0122_0101.htm";
				qs.exitQuest(true);
				return "seer_moira_q0122_0103.htm";
			} else if (cond == 1) {
				return "seer_moira_q0122_0105.htm";
			}
		} else if (npcId == 32017 && cond == 1) {
			qs.giveItems(57, 1695);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "karuda_q0122_0201.htm";
		}
		return "noquest";
	}
}
