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
 * Quest 33: Make a Pair of Dress Shoes
 */
@Component
public class Quest033MakeAPairOfDressShoes extends Quest {

	public static final int QUEST_ID = 33;
	public static final String QUEST_NAME = "033_MakeaPairofDressShoes";

	public static final int START_NPC = 30838;

	@Autowired
	public Quest033MakeAPairOfDressShoes(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Make a Pair of Dress Shoes");
		addStartNpc(START_NPC);
		addTalkId(30838);
		addTalkId(31520);
		registerQuestItems(7113);
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
			return "30838-1.htm";
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
				if (pc.level() >= 60) return "30838-0.htm";
				qs.exitQuest(true);
				return "30838-0a.htm";
			} else if (cond == 1) {
				return "30838-1.htm";
			} else if (cond == 2) {
				qs.giveItems(7113, 1); // Dress Shoes Box
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30838-5.htm";
			}
		} else if (npcId == 31520 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31520-1.htm";
		}
		return "noquest";
	}
}
