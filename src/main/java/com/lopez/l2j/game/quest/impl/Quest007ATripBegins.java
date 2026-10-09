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
 * Quest 007: A Trip Begins
 * Gatekeeper Mirabel, Ariel e Asterios na Elven Village.
 */
@Component
public class Quest007ATripBegins extends Quest {

	public static final int QUEST_ID = 7;
	public static final String QUEST_NAME = "007_ATripBegins";

	public static final int MIRABEL = 30146;
	public static final int ARIEL = 30148;
	public static final int ASTERIOS = 30154;

	public static final int ARIELS_RECOMMENDATION = 7572;
	public static final int SCROLL_OF_ESCAPE_GIRAN = 7126;
	public static final int MARK_OF_TRAVELER = 7570;

	@Autowired
	public Quest007ATripBegins(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "A Trip Begins");
		addStartNpc(MIRABEL);
		addTalkId(MIRABEL, ARIEL, ASTERIOS);
		registerQuestItems(ARIELS_RECOMMENDATION);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ARIELS_RECOMMENDATION, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "mint_q0007_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == MIRABEL) {
			if (cond == 0) {
				if (pc.level() >= 3 && pc.race() == 1) return "mint_q0007_02.htm";
				qs.exitQuest(true);
				return "mint_q0007_01.htm";
			} else if (cond == 1) {
				return "mint_q0007_04.htm";
			} else if (cond == 2) {
				return "mint_q0007_05.htm";
			} else if (cond == 3) {
				qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.giveItems(MARK_OF_TRAVELER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "mint_q0007_06.htm";
			}
		} else if (npcId == ARIEL) {
			if (cond == 1 && qs.hasQuestItems(ARIELS_RECOMMENDATION)) {
				qs.takeItems(ARIELS_RECOMMENDATION, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "ariel_q0007_01.htm";
			} else if (cond == 2) {
				return "ariel_q0007_02.htm";
			}
		} else if (npcId == ASTERIOS) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "asterios_q0007_01.htm";
			} else if (cond == 3) {
				return "asterios_q0007_02.htm";
			}
		}
		return "noquest";
	}
}
