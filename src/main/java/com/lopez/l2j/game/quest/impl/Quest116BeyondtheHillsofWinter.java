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
 * Quest 116: Beyond the Hills of Winter
 */
@Component
public class Quest116BeyondtheHillsofWinter extends Quest {

	public static final int QUEST_ID = 116;
	public static final String QUEST_NAME = "116_BeyondtheHillsofWinter";
	public static final int START_NPC = 30535;

	@Autowired
	public Quest116BeyondtheHillsofWinter(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Beyond the Hills of Winter");
		addStartNpc(START_NPC);
		addTalkId(30535);
		addTalkId(32020);
		registerQuestItems(8098);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(8098, 1); // Bandage
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "elder_filaur_q0116_0104.htm";
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
				if (pc.level() >= 53) return "elder_filaur_q0116_0101.htm";
				qs.exitQuest(true);
				return "elder_filaur_q0116_0103.htm";
			} else if (cond == 1) {
				return "elder_filaur_q0116_0105.htm";
			}
		} else if (npcId == 32020 && cond == 1 && qs.hasQuestItems(8098)) {
			qs.takeItems(8098, -1);
			qs.giveItems(57, 16500);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "rafferty_q0116_0201.htm";
		}
		return "noquest";
	}
}
