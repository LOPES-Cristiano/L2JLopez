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
 * Quest 104: Spirit of Mirror
 */
@Component
public class Quest104SpiritOfMirror extends Quest {

	public static final int QUEST_ID = 104;
	public static final String QUEST_NAME = "104_SpiritOfMirror";
	public static final int START_NPC = 30017;

	@Autowired
	public Quest104SpiritOfMirror(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Spirit of Mirror");
		addStartNpc(START_NPC);
		addTalkId(30017);
		registerQuestItems(748);
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
			return "gallin_q0104_03.htm";
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
				if (pc.level() >= 10 && pc.race() == 0) return "gallin_q0104_02.htm";
				qs.exitQuest(true);
				return "gallin_q0104_00.htm";
			} else if (cond == 1) {
				return "gallin_q0104_04.htm";
			} else if (cond == 2) {
				qs.giveItems(748, 1); // Wand of Adept
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gallin_q0104_06.htm";
			}
		}
		return "noquest";
	}
}
