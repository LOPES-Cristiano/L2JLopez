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
 * Quest 27: Chest Caught with a Bait of Wind
 */
@Component
public class Quest027ChestCaughtWithABaitOfWind extends Quest {

	public static final int QUEST_ID = 27;
	public static final String QUEST_NAME = "027_ChestCaughtwithaBaitofWind";

	public static final int START_NPC = 31570;

	@Autowired
	public Quest027ChestCaughtWithABaitOfWind(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Wind");
		addStartNpc(START_NPC);
		addTalkId(31570);
		addTalkId(31434);
		registerQuestItems(6500, 7609);
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
			return "fisher_lanosco_q0027_0104.htm";
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
				if (pc.level() >= 27) return "fisher_lanosco_q0027_0101.htm";
				qs.exitQuest(true);
				return "fisher_lanosco_q0027_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(6500)) {
					qs.takeItems(6500, -1);
					qs.giveItems(7609, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_lanosco_q0027_0201.htm";
				}
				return "fisher_lanosco_q0027_0105.htm";
			}
		} else if (npcId == 31434 && cond == 2 && qs.hasQuestItems(7609)) {
			qs.takeItems(7609, -1);
			qs.giveItems(884, 1); // Blue Gem / Ring
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "blueprint_seller_shaling_q0027_0301.htm";
		}
		return "noquest";
	}
}
