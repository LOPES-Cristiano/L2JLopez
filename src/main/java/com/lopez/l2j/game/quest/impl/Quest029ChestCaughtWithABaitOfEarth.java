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
 * Quest 29: Chest Caught with a Bait of Earth
 */
@Component
public class Quest029ChestCaughtWithABaitOfEarth extends Quest {

	public static final int QUEST_ID = 29;
	public static final String QUEST_NAME = "029_ChestCaughtwithaBaitofEarth";

	public static final int START_NPC = 31574;

	@Autowired
	public Quest029ChestCaughtWithABaitOfEarth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Earth");
		addStartNpc(START_NPC);
		addTalkId(31574);
		addTalkId(30909);
		registerQuestItems(6502, 7611);
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
			return "fisher_willeri_q0029_0104.htm";
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
				if (pc.level() >= 48) return "fisher_willeri_q0029_0101.htm";
				qs.exitQuest(true);
				return "fisher_willeri_q0029_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(6502)) {
					qs.takeItems(6502, -1);
					qs.giveItems(7611, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_willeri_q0029_0201.htm";
				}
				return "fisher_willeri_q0029_0105.htm";
			}
		} else if (npcId == 30909 && cond == 2 && qs.hasQuestItems(7611)) {
			qs.takeItems(7611, -1);
			qs.giveItems(223, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "magister_anabel_q0029_0301.htm";
		}
		return "noquest";
	}
}
