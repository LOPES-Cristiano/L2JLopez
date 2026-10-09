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
 * Quest 36: Make a Sewing Kit
 */
@Component
public class Quest036MakeASewingKit extends Quest {

	public static final int QUEST_ID = 36;
	public static final String QUEST_NAME = "036_MakeaSewingKit";

	public static final int START_NPC = 30847;

	@Autowired
	public Quest036MakeASewingKit(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Make a Sewing Kit");
		addStartNpc(START_NPC);
		addTalkId(30847);
		registerQuestItems(7078);
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
			return "head_blacksmith_ferris_q0036_0104.htm";
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
				if (pc.level() >= 60) return "head_blacksmith_ferris_q0036_0101.htm";
				qs.exitQuest(true);
				return "head_blacksmith_ferris_q0036_0103.htm";
			} else if (cond == 1) {
				qs.giveItems(7078, 1); // Sewing Kit
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "head_blacksmith_ferris_q0036_0301.htm";
			}
		}
		return "noquest";
	}
}
