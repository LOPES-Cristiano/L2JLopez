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
 * Quest 31: Secret Buried in the Swamp
 */
@Component
public class Quest031SecretBuriedInTheSwamp extends Quest {

	public static final int QUEST_ID = 31;
	public static final String QUEST_NAME = "031_SecretBuriedintheSwamp";

	public static final int START_NPC = 31555;

	@Autowired
	public Quest031SecretBuriedInTheSwamp(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Secret Buried in the Swamp");
		addStartNpc(START_NPC);
		addTalkId(31555);
		addTalkId(31665);
		registerQuestItems(7252);
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
			return "supplier_abercrombie_q0031_0104.htm";
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
				if (pc.level() >= 66) return "supplier_abercrombie_q0031_0101.htm";
				qs.exitQuest(true);
				return "supplier_abercrombie_q0031_0103.htm";
			} else if (cond == 1) {
				return "supplier_abercrombie_q0031_0105.htm";
			} else if (cond == 2 && qs.hasQuestItems(7252)) {
				qs.takeItems(7252, -1);
				qs.giveItems(57, 120000);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "supplier_abercrombie_q0031_0801.htm";
			}
		} else if (npcId == 31665 && cond == 1) {
			qs.giveItems(7252, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "corpse_of_dwarf_q0031_0101.htm";
		}
		return "noquest";
	}
}
