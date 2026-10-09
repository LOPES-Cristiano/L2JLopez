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
 * Quest 112: Walk of Fate
 */
@Component
public class Quest112WalkOfFate extends Quest {

	public static final int QUEST_ID = 112;
	public static final String QUEST_NAME = "112_WalkOfFate";
	public static final int START_NPC = 30572;

	@Autowired
	public Quest112WalkOfFate(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Walk of Fate");
		addStartNpc(START_NPC);
		addTalkId(30572);
		addTalkId(32017);
		registerQuestItems(956);
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
			return "seer_livina_q0112_0104.htm";
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
				if (pc.level() >= 20) return "seer_livina_q0112_0101.htm";
				qs.exitQuest(true);
				return "seer_livina_q0112_0103.htm";
			} else if (cond == 1) {
				return "seer_livina_q0112_0105.htm";
			}
		} else if (npcId == 32017 && cond == 1) {
			qs.giveItems(956, 1); // Enchant Armor D
			qs.giveItems(57, 22308);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "karuda_q0112_0201.htm";
		}
		return "noquest";
	}
}
