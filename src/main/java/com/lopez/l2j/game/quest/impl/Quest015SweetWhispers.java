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
 * Quest 015: Sweet Whispers
 * Vladimir em Rune, Hierarch e Darkness Mystic em Forest of the Dead.
 */
@Component
public class Quest015SweetWhispers extends Quest {

	public static final int QUEST_ID = 15;
	public static final String QUEST_NAME = "015_SweetWhispers";

	public static final int VLADIMIR = 31302;
	public static final int HIERARCH = 31517;
	public static final int MYSTIC = 31518;

	@Autowired
	public Quest015SweetWhispers(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Sweet Whispers");
		addStartNpc(VLADIMIR);
		addTalkId(VLADIMIR, HIERARCH, MYSTIC);
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
			return "trader_vladimir_q0015_0104.htm";
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
				if (pc.level() >= 60) return "trader_vladimir_q0015_0101.htm";
				qs.exitQuest(true);
				return "trader_vladimir_q0015_0103.htm";
			} else if (cond == 1) {
				return "trader_vladimir_q0015_0105.htm";
			}
		} else if (npcId == HIERARCH) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "hierarch_q0015_0101.htm";
			} else if (cond == 2) {
				return "hierarch_q0015_0202.htm";
			}
		} else if (npcId == MYSTIC) {
			if (cond == 2) {
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "dark_mystic_q0015_0101.htm";
			}
		}
		return "noquest";
	}
}
