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

	@Autowired
	public Quest030ChestCaughtWithABaitOfFire(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Fire");
		addStartNpc(START_NPC);
		addTalkId(31577);
		addTalkId(30629);
		registerQuestItems(6503, 7612);
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
			return "fisher_linneaus_q0030_0104.htm";
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
				if (pc.level() >= 60) return "fisher_linneaus_q0030_0101.htm";
				qs.exitQuest(true);
				return "fisher_linneaus_q0030_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(6503)) {
					qs.takeItems(6503, -1);
					qs.giveItems(7612, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_linneaus_q0030_0201.htm";
				}
				return "fisher_linneaus_q0030_0105.htm";
			}
		} else if (npcId == 30629 && cond == 2 && qs.hasQuestItems(7612)) {
			qs.takeItems(7612, -1);
			qs.giveItems(909, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "bard_rukal_q0030_0301.htm";
		}
		return "noquest";
	}
}
