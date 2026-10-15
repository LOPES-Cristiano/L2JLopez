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
		if ("quest_accept".equalsIgnoreCase(event) || "30533-03.htm".equalsIgnoreCase(event) || "elder_balanki_q0010_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "elder_balanki_q0010_0104.htm";
		} else if ("30520-02.htm".equalsIgnoreCase(event) || "warehouse_chief_reed_q0010_0201.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(VERY_EXPENSIVE_NECKLACE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30650-02.htm".equalsIgnoreCase(event) || "gerald_priest_of_earth_q0010_0301.htm".equalsIgnoreCase(event)) {
			if (!qs.hasQuestItems(VERY_EXPENSIVE_NECKLACE)) {
				return "gerald_priest_of_earth_q0010_0302.htm";
			}
			qs.setCond(3);
			qs.takeItems(VERY_EXPENSIVE_NECKLACE, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30520-04.htm".equalsIgnoreCase(event) || "warehouse_chief_reed_q0010_0401.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30533-06.htm".equalsIgnoreCase(event) || "elder_balanki_q0010_0501.htm".equalsIgnoreCase(event)) {
			qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
			qs.giveItems(MARK_OF_TRAVELER, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		if (npcId == BALANKI) {
			if (cond == 0) {
				if (pc.race() == 4 && pc.level() >= 3) {
					return "elder_balanki_q0010_0101.htm";
				}
				qs.exitQuest(true);
				return "elder_balanki_q0010_0102.htm";
			} else if (cond >= 1 && cond < 4) {
				return "elder_balanki_q0010_0105.htm";
			} else if (cond == 4) {
				return "elder_balanki_q0010_0401.htm";
			}
		} else if (npcId == REED) {
			if (cond == 1) {
				return "warehouse_chief_reed_q0010_0101.htm";
			} else if (cond == 2) {
				return "warehouse_chief_reed_q0010_0202.htm";
			} else if (cond == 3) {
				return "warehouse_chief_reed_q0010_0301.htm";
			} else if (cond == 4) {
				return "warehouse_chief_reed_q0010_0402.htm";
			}
		} else if (npcId == GERALD) {
			if (cond == 2) {
				return "gerald_priest_of_earth_q0010_0201.htm";
			} else if (cond >= 3) {
				return "gerald_priest_of_earth_q0010_0303.htm";
			}
		}
		return "noquest";
	}
}
