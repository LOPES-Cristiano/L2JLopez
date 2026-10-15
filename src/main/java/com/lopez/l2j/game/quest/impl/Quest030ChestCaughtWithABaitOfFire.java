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
 * Quest 30: Chest Caught with a Bait of Fire
 */
@Component
public class Quest030ChestCaughtWithABaitOfFire extends Quest {

	public static final int QUEST_ID = 30;
	public static final String QUEST_NAME = "030_ChestCaughtwithaBaitofFire";

	public static final int START_NPC = 31577;
	public static final int LINNAEUS = 31577;
	public static final int RUKAL = 30629;

	public static final int RED_TREASURE_BOX = 6511;
	public static final int RUKALS_MUSICAL_SCORE = 7628;
	public static final int NECKLACE_OF_PROTECTION = 916;

	@Autowired
	public Quest030ChestCaughtWithABaitOfFire(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Fire");
		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS, RUKAL);
		registerQuestItems(RUKALS_MUSICAL_SCORE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_linneaus_q0030_0104.htm".equalsIgnoreCase(event) || "31577-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_linneaus_q0030_0104.htm";
		} else if ("fisher_linneaus_q0030_0201.htm".equalsIgnoreCase(event) || "31577-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RED_TREASURE_BOX) || qs.hasQuestItems(6503)) {
				qs.takeItems(RED_TREASURE_BOX, 1);
				qs.takeItems(6503, -1);
				qs.giveItems(RUKALS_MUSICAL_SCORE, 1);
				qs.giveItems(7612, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "fisher_linneaus_q0030_0202.htm";
		} else if ("bard_rukal_q0030_0301.htm".equalsIgnoreCase(event) || "30629-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RUKALS_MUSICAL_SCORE) || qs.hasQuestItems(7612)) {
				qs.takeItems(RUKALS_MUSICAL_SCORE, -1);
				qs.takeItems(7612, -1);
				qs.giveItems(NECKLACE_OF_PROTECTION, 1);
				qs.giveItems(909, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "bard_rukal_q0030_0302.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == LINNAEUS) {
			if (cond == 0) {
				if (pc.level() >= 60) return "fisher_linneaus_q0030_0101.htm";
				qs.exitQuest(true);
				return "fisher_linneaus_q0030_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(RED_TREASURE_BOX) || qs.hasQuestItems(6503)) {
					qs.takeItems(RED_TREASURE_BOX, 1);
					qs.takeItems(6503, -1);
					qs.giveItems(RUKALS_MUSICAL_SCORE, 1);
					qs.giveItems(7612, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_linneaus_q0030_0201.htm";
				}
				return "fisher_linneaus_q0030_0106.htm";
			} else if (cond == 2) {
				return "fisher_linneaus_q0030_0203.htm";
			}
		} else if (npcId == RUKAL) {
			if (cond == 2 && (qs.hasQuestItems(RUKALS_MUSICAL_SCORE) || qs.hasQuestItems(7612))) {
				qs.takeItems(RUKALS_MUSICAL_SCORE, -1);
				qs.takeItems(7612, -1);
				qs.giveItems(NECKLACE_OF_PROTECTION, 1);
				qs.giveItems(909, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "bard_rukal_q0030_0301.htm";
			}
		}
		return "noquest";
	}
}
