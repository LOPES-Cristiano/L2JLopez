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
 * Quest 50: Lanosco's Special Bait
 */
@Component
public class Quest050LanoscosSpecialBait extends Quest {

	public static final int QUEST_ID = 50;
	public static final String QUEST_NAME = "050_LanoscosSpecialBait";
	public static final int START_NPC = 31570;

	@Autowired
	public Quest050LanoscosSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Lanosco's Special Bait");
		addStartNpc(START_NPC);
		addTalkId(31570);
		registerQuestItems(7610);
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
				if (pc.level() >= 27) return "fisher_lanosco_q0050_0101.htm";
				qs.exitQuest(true);
				return "fisher_lanosco_q0050_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7610) >= 4) {
					qs.takeItems(7610, -1);
					qs.giveItems(7614, 20); // Wind Bait
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_lanosco_q0050_0202.htm";
				}
				return "fisher_lanosco_q0050_0201.htm";
			}
		}
		return "noquest";
	}
}
