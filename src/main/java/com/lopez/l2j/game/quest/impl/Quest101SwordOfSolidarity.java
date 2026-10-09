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
 * Quest 101: Sword of Solidarity
 */
@Component
public class Quest101SwordOfSolidarity extends Quest {

	public static final int QUEST_ID = 101;
	public static final String QUEST_NAME = "101_SwordOfSolidarity";
	public static final int START_NPC = 30008;

	@Autowired
	public Quest101SwordOfSolidarity(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Sword of Solidarity");
		addStartNpc(START_NPC);
		addTalkId(30008);
		addTalkId(30283);
		registerQuestItems(796, 797, 798, 738);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(796, 1); // Roien's Letter
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "roien_q0101_04.htm";
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
				if (pc.level() >= 9 && pc.race() == 0) return "roien_q0101_02.htm";
				qs.exitQuest(true);
				return "roien_q0101_00.htm";
			} else if (cond == 1) {
				return "roien_q0101_05.htm";
			} else if (cond == 4 && qs.hasQuestItems(798)) {
				qs.takeItems(798, -1);
				qs.giveItems(738, 1); // Sword of Solidarity
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "roien_q0101_07.htm";
			}
		} else if (npcId == 30283) {
			if (cond == 1 && qs.hasQuestItems(796)) {
				qs.takeItems(796, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_alltran_q0101_02.htm";
			} else if (cond == 3) {
				qs.giveItems(798, 1); // Alltran's Note
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_alltran_q0101_05.htm";
			}
		}
		return "noquest";
	}
}
