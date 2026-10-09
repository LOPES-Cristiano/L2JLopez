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
 * Quest 110: To the Primeval Isle
 */
@Component
public class Quest110ToThePrimevalIsle extends Quest {

	public static final int QUEST_ID = 110;
	public static final String QUEST_NAME = "110_ToThePrimevalIsle";
	public static final int START_NPC = 31338;

	@Autowired
	public Quest110ToThePrimevalIsle(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "To the Primeval Isle");
		addStartNpc(START_NPC);
		addTalkId(31338);
		addTalkId(32113);
		registerQuestItems(8777);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(8777, 1); // Marquez's Letter
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "scroll_seller_anton_q0110_0104.htm";
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
				if (pc.level() >= 75) return "scroll_seller_anton_q0110_0101.htm";
				qs.exitQuest(true);
				return "scroll_seller_anton_q0110_0103.htm";
			} else if (cond == 1) {
				return "scroll_seller_anton_q0110_0105.htm";
			}
		} else if (npcId == 32113 && cond == 1 && qs.hasQuestItems(8777)) {
			qs.takeItems(8777, -1);
			qs.giveItems(57, 191678);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "marquez_q0110_0201.htm";
		}
		return "noquest";
	}
}
