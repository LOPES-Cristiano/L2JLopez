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
 * Quest 52: Willie's Special Bait
 */
@Component
public class Quest052WilliesSpecialBait extends Quest {

	public static final int QUEST_ID = 52;
	public static final String QUEST_NAME = "052_WilliesSpecialBait";
	public static final int START_NPC = 31574;

	@Autowired
	public Quest052WilliesSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Willie's Special Bait");
		addStartNpc(START_NPC);
		addTalkId(31574);
		registerQuestItems(7612);
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
			return "fisher_lanosco_q0050_0104.htm";
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
				if (pc.level() >= 48) return "fisher_willeri_q0052_0101.htm";
				qs.exitQuest(true);
				return "fisher_willeri_q0052_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7612) >= 4) {
					qs.takeItems(7612, -1);
					qs.giveItems(7616, 20); // Earth Bait
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_willeri_q0052_0202.htm";
				}
				return "fisher_willeri_q0052_0201.htm";
			}
		}
		return "noquest";
	}
}
