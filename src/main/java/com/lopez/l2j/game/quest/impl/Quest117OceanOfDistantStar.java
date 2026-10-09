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
 * Quest 117: Ocean of Distant Star
 */
@Component
public class Quest117OceanOfDistantStar extends Quest {

	public static final int QUEST_ID = 117;
	public static final String QUEST_NAME = "117_OceanOfDistantStar";
	public static final int START_NPC = 32069;

	@Autowired
	public Quest117OceanOfDistantStar(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Ocean of Distant Star");
		addStartNpc(START_NPC);
		addTalkId(32069);
		addTalkId(32070);
		
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
			return "railman_obi_q0117_0104.htm";
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
				if (pc.level() >= 39) return "railman_obi_q0117_0101.htm";
				qs.exitQuest(true);
				return "railman_obi_q0117_0103.htm";
			} else if (cond == 1) {
				return "railman_obi_q0117_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 17647);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "railman_obi_q0117_0202.htm";
			}
		} else if (npcId == 32070 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "ghost_of_adventurer_q0117_0201.htm";
		}
		return "noquest";
	}
}
