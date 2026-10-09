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
 * Quest 019: Go to the Pastureland!
 * Vladimir em Rune e Tunatun na Beast Farm.
 */
@Component
public class Quest019GoToThePastureland extends Quest {

	public static final int QUEST_ID = 19;
	public static final String QUEST_NAME = "019_GoToThePastureland";

	public static final int VLADIMIR = 31302;
	public static final int TUNATUN = 31537;
	public static final int BEAST_MEAT = 7547;

	@Autowired
	public Quest019GoToThePastureland(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Go to the Pastureland!");
		addStartNpc(VLADIMIR);
		addTalkId(VLADIMIR, TUNATUN);
		registerQuestItems(BEAST_MEAT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BEAST_MEAT, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "trader_vladimir_q0019_0104.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == VLADIMIR) {
			if (cond == 0) {
				if (pc.level() >= 63) return "trader_vladimir_q0019_0101.htm";
				qs.exitQuest(true);
				return "trader_vladimir_q0019_0103.htm";
			} else if (cond == 1) {
				return "trader_vladimir_q0019_0105.htm";
			}
		} else if (npcId == TUNATUN) {
			if (cond == 1 && qs.hasQuestItems(BEAST_MEAT)) {
				qs.takeItems(BEAST_MEAT, -1);
				qs.giveItems(57, 30000);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "beast_herder_tunatun_q0019_0201.htm";
			}
		}
		return "noquest";
	}
}
