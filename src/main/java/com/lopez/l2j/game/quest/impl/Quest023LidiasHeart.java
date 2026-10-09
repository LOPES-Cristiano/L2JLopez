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
 * Quest 023: Lidia's Heart
 * High Priest Innocentin em Rune, Broken Bookshelf e Tombstone em Forest of the Dead.
 */
@Component
public class Quest023LidiasHeart extends Quest {

	public static final int QUEST_ID = 23;
	public static final String QUEST_NAME = "023_LidiasHeart";

	public static final int INNOCENTIN = 31328;
	public static final int BOOKSHELF = 31526;
	public static final int TOMBSTONE = 31523;

	public static final int MAP_FOREST = 7063;
	public static final int LIDIAS_HAIRPIN = 7148;
	public static final int LIDIAS_DIARY = 7149;

	@Autowired
	public Quest023LidiasHeart(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Lidia's Heart");
		addStartNpc(INNOCENTIN);
		addTalkId(INNOCENTIN, BOOKSHELF, TOMBSTONE);
		registerQuestItems(MAP_FOREST, LIDIAS_HAIRPIN, LIDIAS_DIARY);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(MAP_FOREST, 1);
			qs.giveItems(LIDIAS_HAIRPIN, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "highpriest_innocentin_q0023_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == INNOCENTIN) {
			if (cond == 0) {
				if (pc.level() >= 64) return "highpriest_innocentin_q0023_01.htm";
				qs.exitQuest(true);
				return "highpriest_innocentin_q0023_02.htm";
			} else if (cond == 1) {
				return "highpriest_innocentin_q0023_04.htm";
			} else if (cond == 2 && qs.hasQuestItems(LIDIAS_DIARY)) {
				qs.takeItems(LIDIAS_DIARY, -1);
				qs.takeItems(MAP_FOREST, -1);
				qs.giveItems(57, 100000);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "highpriest_innocentin_q0023_11.htm";
			}
		} else if (npcId == BOOKSHELF) {
			if (cond == 1) {
				qs.giveItems(LIDIAS_DIARY, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "broken_desk1_q0023_02.htm";
			}
		}
		return "noquest";
	}
}
