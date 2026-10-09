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
 * Quest 37: Please Make Me Formal Wear
 */
@Component
public class Quest037PleaseMakeMeFormalWear extends Quest {

	public static final int QUEST_ID = 37;
	public static final String QUEST_NAME = "037_PleaseMakeMeFormalWear";

	public static final int START_NPC = 30842;

	@Autowired
	public Quest037PleaseMakeMeFormalWear(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Please Make Me Formal Wear");
		addStartNpc(START_NPC);
		addTalkId(30842);
		registerQuestItems(6408);
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
			return "30842-1.htm";
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
				if (pc.level() >= 60) return "30842-0.htm";
				qs.exitQuest(true);
				return "30842-0a.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(7113) && qs.hasQuestItems(7078) && qs.hasQuestItems(7076) && qs.hasQuestItems(7077)) {
					qs.takeItems(7113, -1);
					qs.takeItems(7078, -1);
					qs.takeItems(7076, -1);
					qs.takeItems(7077, -1);
					qs.giveItems(6408, 1); // Formal Wear
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "30842-3.htm";
				}
				return "30842-1.htm";
			}
		}
		return "noquest";
	}
}
