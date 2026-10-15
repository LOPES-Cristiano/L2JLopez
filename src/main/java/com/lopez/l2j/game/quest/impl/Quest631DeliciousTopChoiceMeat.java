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
 * Quest 631: Delicious Top Choice Meat
 * Beast Herder Tunatun em Beast Farm.
 * Coleta de Top Quality Meat de bestas domesticadas para trocar por Mold Glue, Lubricant, Enria, Asofe, Thons.
 */
@Component
public class Quest631DeliciousTopChoiceMeat extends Quest {

	public static final int QUEST_ID = 631;
	public static final String QUEST_NAME = "631_DeliciousTopChoiceMeat";

	public static final int TUNATUN = 31537;
	public static final int TOP_QUALITY_MEAT = 7546;

	public static final int[][] REWARDS = {
			{4039, 15}, // Mold Glue
			{4043, 15}, // Asofe
			{4044, 15}, // Thons
			{4040, 10}, // Mold Lubricant
			{4042, 10}, // Enria
			{4041, 5}   // Mold Hardener
	};

	public static final int[] MOBS = {
			21460, 21461, 21462, 21463, 21464, 21465, 21466, 21467, 21468, 21469,
			21479, 21480, 21481, 21482, 21483, 21484, 21485, 21486, 21487, 21488,
			21498, 21499, 21500, 21501, 21502, 21503, 21504, 21505, 21506, 21507
	};

	@Autowired
	public Quest631DeliciousTopChoiceMeat(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Delicious Top Choice Meat");
		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);
		addKillId(MOBS);
		registerQuestItems(TOP_QUALITY_MEAT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("31537-03.htm".equalsIgnoreCase(event) || "quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31537-03.htm";
		} else if ("31537-05.htm".equalsIgnoreCase(event)) {
			return "31537-05.htm";
		} else if ("reward_enria".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120) {
				qs.takeItems(TOP_QUALITY_MEAT, -1);
				qs.giveItems(4042, 10); // 10 Enria
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "31537-06.htm";
			}
			return "31537-07.htm";
		} else if ("0".equalsIgnoreCase(event)
				|| "1".equalsIgnoreCase(event)
				|| "2".equalsIgnoreCase(event)
				|| "3".equalsIgnoreCase(event)
				|| "4".equalsIgnoreCase(event)
				|| "5".equalsIgnoreCase(event)) {
			int idx = Integer.parseInt(event);
			if (qs.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120) {
				qs.takeItems(TOP_QUALITY_MEAT, -1);
				qs.rewardItems(REWARDS[idx][0], REWARDS[idx][1]);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "31537-06.htm";
			}
			qs.setCond(1);
			return "31537-07.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 65) return "31537-01.htm";
			qs.exitQuest(true);
			return "31537-02.htm";
		} else {
			if (qs.getCond() == 2 || qs.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120) {
				return "31537-04.htm";
			}
			return "31537-01a.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() >= 1 && qs.getQuestItemsCount(TOP_QUALITY_MEAT) < 120) {
			qs.giveItems(TOP_QUALITY_MEAT, 1);
			if (qs.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
