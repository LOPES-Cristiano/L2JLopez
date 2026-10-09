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
 * Quest 010: Into the World
 * Elder Balanki, Warehouse Chief Reed e Priest Gerald na Dwarf Village.
 */
@Component
public class Quest010IntoTheWorld extends Quest {

	public static final int QUEST_ID = 10;
	public static final String QUEST_NAME = "010_IntoTheWorld";

	public static final int BALANKI = 30533;
	public static final int REED = 30520;
	public static final int GERALD = 30650;

	public static final int VERY_EXPENSIVE_NECKLACE = 7574;
	public static final int SCROLL_OF_ESCAPE_GIRAN = 7126;
	public static final int MARK_OF_TRAVELER = 7570;

	@Autowired
	public Quest010IntoTheWorld(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Into the World");
		addStartNpc(BALANKI);
		addTalkId(BALANKI, REED, GERALD);
		registerQuestItems(VERY_EXPENSIVE_NECKLACE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VERY_EXPENSIVE_NECKLACE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "elder_balanki_q0010_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BALANKI) {
			if (cond == 0) {
				if (pc.level() >= 3 && pc.race() == 4) return "elder_balanki_q0010_02.htm";
				qs.exitQuest(true);
				return "elder_balanki_q0010_01.htm";
			} else if (cond == 1) {
				return "elder_balanki_q0010_04.htm";
			} else if (cond == 2) {
				return "elder_balanki_q0010_05.htm";
			} else if (cond == 3 || cond == 4) {
				qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.giveItems(MARK_OF_TRAVELER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "elder_balanki_q0010_06.htm";
			}
		} else if (npcId == REED) {
			if (cond == 1 && qs.hasQuestItems(VERY_EXPENSIVE_NECKLACE)) {
				qs.takeItems(VERY_EXPENSIVE_NECKLACE, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "warehouse_chief_reed_q0010_01.htm";
			} else if (cond == 2) {
				return "warehouse_chief_reed_q0010_02.htm";
			}
		} else if (npcId == GERALD) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gerald_priest_of_earth_q0010_01.htm";
			} else if (cond >= 3) {
				return "gerald_priest_of_earth_q0010_02.htm";
			}
		}
		return "noquest";
	}
}
