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
 * Quest 35: Find Glittering Jewelry
 */
@Component
public class Quest035FindGlitteringJewelry extends Quest {

	public static final int QUEST_ID = 35;
	public static final String QUEST_NAME = "035_FindGlitteringJewelry";

	public static final int START_NPC = 30091;

	@Autowired
	public Quest035FindGlitteringJewelry(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Find Glittering Jewelry");
		addStartNpc(START_NPC);
		addTalkId(30091);
		addTalkId(30879);
		registerQuestItems(7077);
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
			return "30091-1.htm";
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
				if (pc.level() >= 60) return "30091-0.htm";
				qs.exitQuest(true);
				return "30091-0a.htm";
			} else if (cond == 1) {
				return "30091-1.htm";
			} else if (cond == 2) {
				qs.giveItems(7077, 1); // Box of Jewelry
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30091-3.htm";
			}
		} else if (npcId == 30879 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30879-1.htm";
		}
		return "noquest";
	}
}
