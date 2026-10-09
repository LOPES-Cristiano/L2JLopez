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
 * Quest 111: Elrokian Hunter's Proof
 */
@Component
public class Quest111ElrokianHuntersProof extends Quest {

	public static final int QUEST_ID = 111;
	public static final String QUEST_NAME = "111_ElrokianHuntersProof";
	public static final int START_NPC = 32113;

	@Autowired
	public Quest111ElrokianHuntersProof(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Elrokian Hunter's Proof");
		addStartNpc(START_NPC);
		addTalkId(32113);
		addTalkId(32117);
		registerQuestItems(8763, 8764);
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
			return "marquez_q0111_0104.htm";
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
				if (pc.level() >= 75) return "marquez_q0111_0101.htm";
				qs.exitQuest(true);
				return "marquez_q0111_0103.htm";
			} else if (cond == 1) {
				return "marquez_q0111_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(8763, 1); // Elrokian Trap
				qs.giveItems(8764, 100); // Trap Stones
				qs.giveItems(57, 102225);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "marquez_q0111_0301.htm";
			}
		} else if (npcId == 32117 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "kirikachin_q0111_0201.htm";
		}
		return "noquest";
	}
}
