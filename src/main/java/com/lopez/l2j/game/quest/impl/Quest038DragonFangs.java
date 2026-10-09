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
 * Quest 38: Dragon Fangs
 */
@Component
public class Quest038DragonFangs extends Quest {

	public static final int QUEST_ID = 38;
	public static final String QUEST_NAME = "038_DragonFangs";

	public static final int START_NPC = 30386;

	@Autowired
	public Quest038DragonFangs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Dragon Fangs");
		addStartNpc(START_NPC);
		addTalkId(30386);
		registerQuestItems(49);
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
			return "guard_luis_q0038_0104.htm";
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
				if (pc.level() >= 19) return "guard_luis_q0038_0101.htm";
				qs.exitQuest(true);
				return "guard_luis_q0038_0103.htm";
			} else if (cond == 1) {
				qs.giveItems(49, 1); // Bone Helmet
				qs.giveItems(57, 5200);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "guard_luis_q0038_0202.htm";
			}
		}
		return "noquest";
	}
}
