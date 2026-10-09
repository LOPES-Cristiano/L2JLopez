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
 * Quest 39: Red Eyed Invaders
 */
@Component
public class Quest039RedEyedInvaders extends Quest {

	public static final int QUEST_ID = 39;
	public static final String QUEST_NAME = "039_RedEyedInvaders";

	public static final int START_NPC = 30334;

	@Autowired
	public Quest039RedEyedInvaders(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Red Eyed Invaders");
		addStartNpc(START_NPC);
		addTalkId(30334);
		addTalkId(30332);
		
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
			return "guard_babenco_q0039_0104.htm";
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
				if (pc.level() >= 20) return "guard_babenco_q0039_0101.htm";
				qs.exitQuest(true);
				return "guard_babenco_q0039_0103.htm";
			} else if (cond == 1) {
				return "guard_babenco_q0039_0105.htm";
			}
		} else if (npcId == 30332 && cond == 1) {
			qs.giveItems(57, 6200);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "captain_bathia_q0039_0201.htm";
		}
		return "noquest";
	}
}
