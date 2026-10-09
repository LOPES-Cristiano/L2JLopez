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
 * Quest 32: An Obvious Lie
 */
@Component
public class Quest032AnObviousLie extends Quest {

	public static final int QUEST_ID = 32;
	public static final String QUEST_NAME = "032_AnObviousLie";

	public static final int START_NPC = 30120;

	@Autowired
	public Quest032AnObviousLie(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "An Obvious Lie");
		addStartNpc(START_NPC);
		addTalkId(30120);
		addTalkId(30094);
		
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
			return "maximilian_q0032_0104.htm";
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
				if (pc.level() >= 45) return "maximilian_q0032_0101.htm";
				qs.exitQuest(true);
				return "maximilian_q0032_0102.htm";
			} else if (cond == 1) {
				return "maximilian_q0032_0105.htm";
			}
		} else if (npcId == 30094) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gentler_q0032_0201.htm";
			} else if (cond == 2) {
				qs.giveItems(6843, 1); // Cat Ears
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gentler_q0032_0401.htm";
			}
		}
		return "noquest";
	}
}
