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
 * Quest 021: Hidden Truth
 * Shadow Hardin em Forest of the Dead, Tombstone, Ghost of von Hellmann e Broken Bookshelf.
 */
@Component
public class Quest021HiddenTruth extends Quest {

	public static final int QUEST_ID = 21;
	public static final String QUEST_NAME = "021_HiddenTruth";

	public static final int HARDIN = 31522;
	public static final int TOMBSTONE = 31523;
	public static final int GHOST = 31524;
	public static final int BOOKSHELF = 31526;
	public static final int AGRIPEL = 31348;
	public static final int INNOCENTIN = 31328;

	public static final int CASKET = 7140;
	public static final int CROSS_OF_EINHASAD = 7141;

	@Autowired
	public Quest021HiddenTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hidden Truth");
		addStartNpc(HARDIN);
		addTalkId(HARDIN, TOMBSTONE, GHOST, BOOKSHELF, AGRIPEL, INNOCENTIN);
		registerQuestItems(CASKET, CROSS_OF_EINHASAD);
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
			return "shadow_hardin_q0021_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == HARDIN) {
			if (cond == 0) {
				if (pc.level() >= 63) return "shadow_hardin_q0021_01.htm";
				qs.exitQuest(true);
				return "shadow_hardin_q0021_03.htm";
			} else if (cond == 1) {
				return "shadow_hardin_q0021_04.htm";
			}
		} else if (npcId == TOMBSTONE) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "q_forest_stone1_q0021_02.htm";
			}
		} else if (npcId == GHOST) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "rune_ghost1_q0021_06a.htm";
			}
		} else if (npcId == BOOKSHELF) {
			if (cond == 3) {
				qs.giveItems(CROSS_OF_EINHASAD, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "broken_desk1_q0021_03.htm";
			}
		} else if (npcId == INNOCENTIN) {
			if (cond == 4 && qs.hasQuestItems(CROSS_OF_EINHASAD)) {
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "highpriest_innocentin_q0021_02.htm";
			}
		}
		return "noquest";
	}
}
