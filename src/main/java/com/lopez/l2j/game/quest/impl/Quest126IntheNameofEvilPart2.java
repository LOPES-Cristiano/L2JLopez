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
 * Quest 126: In the Name of Evil - Part 2
 */
@Component
public class Quest126IntheNameofEvilPart2 extends Quest {

	public static final int QUEST_ID = 126;
	public static final String QUEST_NAME = "126_IntheNameofEvilPart2";
	public static final int START_NPC = 32116;

	@Autowired
	public Quest126IntheNameofEvilPart2(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In the Name of Evil - Part 2");
		addStartNpc(START_NPC);
		addTalkId(32116);
		addTalkId(32117);
		registerQuestItems(8704);
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
			return "asamah_q0126_0104.htm";
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
				if (pc.level() >= 77) return "asamah_q0126_0101.htm";
				qs.exitQuest(true);
				return "asamah_q0126_0103.htm";
			} else if (cond == 1) {
				return "asamah_q0126_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(8704, 1); // Top A / S grade weapon parts or Giant Book
				qs.giveItems(57, 461512);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "asamah_q0126_0202.htm";
			}
		} else if (npcId == 32117 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "shaman_caracawe_q0126_0201.htm";
		}
		return "noquest";
	}
}
