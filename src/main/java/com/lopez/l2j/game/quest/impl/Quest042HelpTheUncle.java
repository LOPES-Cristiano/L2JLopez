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
 * Quest 42: Help the Uncle
 */
@Component
public class Quest042HelpTheUncle extends Quest {

	public static final int QUEST_ID = 42;
	public static final String QUEST_NAME = "042_HelptheUncle";

	public static final int START_NPC = 30828;

	@Autowired
	public Quest042HelpTheUncle(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Help the Uncle");
		addStartNpc(START_NPC);
		addTalkId(30828);
		registerQuestItems(3438);
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
			return "pet_manager_waters_q0042_0104.htm";
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
				if (pc.level() >= 25) return "pet_manager_waters_q0042_0101.htm";
				qs.exitQuest(true);
				return "pet_manager_waters_q0042_0103.htm";
			} else if (cond == 1) {
				qs.giveItems(3438, 1); // Pet Collar
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "pet_manager_waters_q0042_0301.htm";
			}
		}
		return "noquest";
	}
}
