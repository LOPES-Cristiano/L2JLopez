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
 * Quest 002: What Women Want
 * Arujien, Mirabel, Herbiel e Greenis na Elven Village.
 */
@Component
public class Quest002WhatWomenWant extends Quest {

	public static final int QUEST_ID = 2;
	public static final String QUEST_NAME = "002_WhatWomenWant";

	public static final int ARUJIEN = 30223;
	public static final int MIRABEL = 30146;
	public static final int HERBIEL = 30150;
	public static final int GREENIS = 30157;

	public static final int ARUJIENS_LETTER1 = 1092;
	public static final int ARUJIENS_LETTER2 = 1093;
	public static final int ARUJIENS_LETTER3 = 1094;
	public static final int POETRY_BOOK = 689;
	public static final int GREENIS_LETTER = 693;
	public static final int MYSTICS_EARRING = 113;

	@Autowired
	public Quest002WhatWomenWant(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "What Women Want");
		addStartNpc(ARUJIEN);
		addTalkId(ARUJIEN, MIRABEL, HERBIEL, GREENIS);
		registerQuestItems(ARUJIENS_LETTER1, ARUJIENS_LETTER2, ARUJIENS_LETTER3, POETRY_BOOK, GREENIS_LETTER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ARUJIENS_LETTER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "arujien_q0002_04.htm";
		} else if ("2_1".equalsIgnoreCase(event)) {
			qs.takeItems(ARUJIENS_LETTER3, -1);
			qs.giveItems(POETRY_BOOK, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "arujien_q0002_08.htm";
		} else if ("2_2".equalsIgnoreCase(event)) {
			qs.takeItems(ARUJIENS_LETTER3, -1);
			qs.giveItems(57, 450);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "arujien_q0002_09.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ARUJIEN) {
			if (cond == 0) {
				if (pc.level() >= 2 && (pc.race() == 1 || pc.race() == 0)) return "arujien_q0002_02.htm";
				qs.exitQuest(true);
				return "arujien_q0002_01.htm";
			} else if (cond == 1) {
				return "arujien_q0002_05.htm";
			} else if (cond == 2) {
				return "arujien_q0002_06.htm";
			} else if (cond == 3) {
				return "arujien_q0002_07.htm";
			} else if (cond == 4) {
				return "arujien_q0002_10.htm";
			} else if (cond == 5 && qs.hasQuestItems(GREENIS_LETTER)) {
				qs.takeItems(GREENIS_LETTER, -1);
				qs.giveItems(MYSTICS_EARRING, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "arujien_q0002_11.htm";
			}
		} else if (npcId == MIRABEL) {
			if (cond == 1 && qs.hasQuestItems(ARUJIENS_LETTER1)) {
				qs.takeItems(ARUJIENS_LETTER1, -1);
				qs.giveItems(ARUJIENS_LETTER2, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "mint_q0002_01.htm";
			} else if (cond == 2) {
				return "mint_q0002_02.htm";
			}
		} else if (npcId == HERBIEL) {
			if (cond == 2 && qs.hasQuestItems(ARUJIENS_LETTER2)) {
				qs.takeItems(ARUJIENS_LETTER2, -1);
				qs.giveItems(ARUJIENS_LETTER3, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "green_q0002_01.htm";
			} else if (cond == 3) {
				return "green_q0002_02.htm";
			}
		} else if (npcId == GREENIS) {
			if (cond == 4 && qs.hasQuestItems(POETRY_BOOK)) {
				qs.takeItems(POETRY_BOOK, -1);
				qs.giveItems(GREENIS_LETTER, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "grain_q0002_02.htm";
			} else if (cond == 5) {
				return "grain_q0002_03.htm";
			}
		}
		return "noquest";
	}
}
