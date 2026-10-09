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
 * Quest 018: Meeting with the Golden Ram
 * Donal em Rune, Daisy e Abercrombie no Swamp of Screams.
 */
@Component
public class Quest018MeetingwiththeGoldenRam extends Quest {

	public static final int QUEST_ID = 18;
	public static final String QUEST_NAME = "018_MeetingwiththeGoldenRam";

	public static final int DONAL = 31314;
	public static final int DAISY = 31315;
	public static final int ABERCROMBIE = 31555;
	public static final int SUPPLY_BOX = 7245;

	@Autowired
	public Quest018MeetingwiththeGoldenRam(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Meeting with the Golden Ram");
		addStartNpc(DONAL);
		addTalkId(DONAL, DAISY, ABERCROMBIE);
		registerQuestItems(SUPPLY_BOX);
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
			return "warehouse_chief_donal_q0018_0104.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == DONAL) {
			if (cond == 0) {
				if (pc.level() >= 66) return "warehouse_chief_donal_q0018_0101.htm";
				qs.exitQuest(true);
				return "warehouse_chief_donal_q0018_0103.htm";
			} else if (cond == 1) {
				return "warehouse_chief_donal_q0018_0105.htm";
			}
		} else if (npcId == DAISY) {
			if (cond == 1) {
				qs.giveItems(SUPPLY_BOX, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "freighter_daisy_q0018_0201.htm";
			} else if (cond == 2) {
				return "freighter_daisy_q0018_0202.htm";
			}
		} else if (npcId == ABERCROMBIE) {
			if (cond == 2 && qs.hasQuestItems(SUPPLY_BOX)) {
				qs.takeItems(SUPPLY_BOX, -1);
				qs.giveItems(57, 15000);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "supplier_abercrombie_q0018_0301.htm";
			}
		}
		return "noquest";
	}
}
