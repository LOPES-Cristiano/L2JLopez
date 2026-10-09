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
 * Quest 105: Skirmish with Orcs
 */
@Component
public class Quest105SkirmishWithOrcs extends Quest {

	public static final int QUEST_ID = 105;
	public static final String QUEST_NAME = "105_SkirmishWithOrcs";
	public static final int START_NPC = 30218;

	@Autowired
	public Quest105SkirmishWithOrcs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Skirmish with Orcs");
		addStartNpc(START_NPC);
		addTalkId(30218);
		registerQuestItems(754);
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
			return "sentinel_kendnell_q0105_03.htm";
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
				if (pc.level() >= 10 && pc.race() == 1) return "sentinel_kendnell_q0105_02.htm";
				qs.exitQuest(true);
				return "sentinel_kendnell_q0105_00.htm";
			} else if (cond == 1) {
				return "sentinel_kendnell_q0105_04.htm";
			} else if (cond == 2) {
				qs.giveItems(754, 1); // Red Sunset Sword
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "sentinel_kendnell_q0105_10.htm";
			}
		}
		return "noquest";
	}
}
