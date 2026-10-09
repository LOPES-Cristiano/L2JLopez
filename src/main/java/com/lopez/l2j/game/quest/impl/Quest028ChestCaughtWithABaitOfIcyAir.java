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
 * Quest 28: Chest Caught with a Bait of Icy Air
 */
@Component
public class Quest028ChestCaughtWithABaitOfIcyAir extends Quest {

	public static final int QUEST_ID = 28;
	public static final String QUEST_NAME = "028_ChestCaughtwithaBaitofIcyAir";

	public static final int START_NPC = 31572;

	@Autowired
	public Quest028ChestCaughtWithABaitOfIcyAir(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Icy Air");
		addStartNpc(START_NPC);
		addTalkId(31572);
		addTalkId(31442);
		registerQuestItems(6501, 7610);
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
			return "fisher_ofulle_q0028_0104.htm";
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
				if (pc.level() >= 36) return "fisher_ofulle_q0028_0101.htm";
				qs.exitQuest(true);
				return "fisher_ofulle_q0028_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(6501)) {
					qs.takeItems(6501, -1);
					qs.giveItems(7610, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_ofulle_q0028_0201.htm";
				}
				return "fisher_ofulle_q0028_0105.htm";
			}
		} else if (npcId == 31442 && cond == 2 && qs.hasQuestItems(7610)) {
			qs.takeItems(7610, -1);
			qs.giveItems(884, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "mineral_trader_kiki_q0028_0301.htm";
		}
		return "noquest";
	}
}
