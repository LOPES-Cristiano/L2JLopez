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
 * Quest 113: Status of the Beacon Tower
 */
@Component
public class Quest113StatusOfTheBeaconTower extends Quest {

	public static final int QUEST_ID = 113;
	public static final String QUEST_NAME = "113_StatusOfTheBeaconTower";
	public static final int START_NPC = 31979;

	@Autowired
	public Quest113StatusOfTheBeaconTower(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Status of the Beacon Tower");
		addStartNpc(START_NPC);
		addTalkId(31979);
		addTalkId(32016);
		registerQuestItems(8086);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(8086, 1); // Box
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "seer_moira_q0113_0104.htm";
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
				if (pc.level() >= 40) return "seer_moira_q0113_0101.htm";
				qs.exitQuest(true);
				return "seer_moira_q0113_0103.htm";
			} else if (cond == 1) {
				return "seer_moira_q0113_0105.htm";
			}
		} else if (npcId == 32016 && cond == 1 && qs.hasQuestItems(8086)) {
			qs.takeItems(8086, -1);
			qs.giveItems(57, 21580);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "torrant_q0113_0201.htm";
		}
		return "noquest";
	}
}
