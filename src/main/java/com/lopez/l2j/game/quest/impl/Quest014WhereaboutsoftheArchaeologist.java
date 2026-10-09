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
 * Quest 014: Whereabouts of the Archaeologist
 * Ghost of Adventurer em Ghostgate e Explorer Liesel em Border Outpost.
 */
@Component
public class Quest014WhereaboutsoftheArchaeologist extends Quest {

	public static final int QUEST_ID = 14;
	public static final String QUEST_NAME = "014_WhereaboutsoftheArchaeologist";

	public static final int GHOST = 31263;
	public static final int LIESEL = 31538;
	public static final int LETTER = 7253;

	@Autowired
	public Quest014WhereaboutsoftheArchaeologist(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Whereabouts of the Archaeologist");
		addStartNpc(GHOST);
		addTalkId(GHOST, LIESEL);
		registerQuestItems(LETTER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "ghost_of_adventurer_q0014_0104.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == GHOST) {
			if (cond == 0) {
				if (pc.level() >= 74) return "ghost_of_adventurer_q0014_0101.htm";
				qs.exitQuest(true);
				return "ghost_of_adventurer_q0014_0103.htm";
			} else if (cond == 1) {
				return "ghost_of_adventurer_q0014_0105.htm";
			}
		} else if (npcId == LIESEL) {
			if (cond == 1 && qs.hasQuestItems(LETTER)) {
				qs.takeItems(LETTER, -1);
				qs.giveItems(57, 113228);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "explorer_liesel_q0014_0101.htm";
			}
		}
		return "noquest";
	}
}
