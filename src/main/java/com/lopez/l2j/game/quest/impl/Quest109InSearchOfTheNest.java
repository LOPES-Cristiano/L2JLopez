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
 * Quest 109: In Search of the Nest
 */
@Component
public class Quest109InSearchOfTheNest extends Quest {

	public static final int QUEST_ID = 109;
	public static final String QUEST_NAME = "109_InSearchOfTheNest";
	public static final int START_NPC = 31554;

	@Autowired
	public Quest109InSearchOfTheNest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In Search of the Nest");
		addStartNpc(START_NPC);
		addTalkId(31554);
		addTalkId(31553);
		
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
			return "merc_kahmun_q0109_0104.htm";
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
				if (pc.level() >= 66) return "merc_kahmun_q0109_0101.htm";
				qs.exitQuest(true);
				return "merc_kahmun_q0109_0103.htm";
			} else if (cond == 1) {
				return "merc_kahmun_q0109_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 16180);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "merc_kahmun_q0109_0202.htm";
			}
		} else if (npcId == 31553 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "scout_pierce_q0109_0101.htm";
		}
		return "noquest";
	}
}
