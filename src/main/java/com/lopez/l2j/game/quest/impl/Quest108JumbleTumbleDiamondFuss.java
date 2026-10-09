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
 * Quest 108: Jumble, Tumble, Diamond Fuss
 */
@Component
public class Quest108JumbleTumbleDiamondFuss extends Quest {

	public static final int QUEST_ID = 108;
	public static final String QUEST_NAME = "108_JumbleTumbleDiamondFuss";
	public static final int START_NPC = 30523;

	@Autowired
	public Quest108JumbleTumbleDiamondFuss(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Jumble, Tumble, Diamond Fuss");
		addStartNpc(START_NPC);
		addTalkId(30523);
		addTalkId(30526);
		registerQuestItems(756);
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
			return "collector_gouph_q0108_03.htm";
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
				if (pc.level() >= 10 && pc.race() == 4) return "collector_gouph_q0108_02.htm";
				qs.exitQuest(true);
				return "collector_gouph_q0108_00.htm";
			} else if (cond == 1) {
				return "collector_gouph_q0108_04.htm";
			} else if (cond == 2) {
				qs.giveItems(756, 1); // Silversmith Hammer
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collector_gouph_q0108_07.htm";
			}
		} else if (npcId == 30526 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "blacksmith_bronp_q0108_02.htm";
		}
		return "noquest";
	}
}
