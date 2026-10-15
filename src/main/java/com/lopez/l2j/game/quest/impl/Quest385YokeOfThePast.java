package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 385 - 385_YokeOfThePast
 */
@Component
public class Quest385YokeOfThePast extends Quest {

	public static final int ANCIENT_SCROLL = 5902;
	public static final int BLANK_SCROLL = 5965;

	public Quest385YokeOfThePast(QuestManager questManager) {
		super(385, "385_YokeOfThePast", "385_YokeOfThePast");
		for (int n = 31095; n <= 31126; ++n) {
			if (n == 31111 || n == 31112 || n == 31113) continue;
			addStartNpc(n);
			addTalkId(n);
		}
		for (int n = 21208; n < 21256; ++n) {
			addKillId(n);
		}
		registerQuestItems(ANCIENT_SCROLL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "enter_necropolis1_q0385_05.htm";
		}

		String string2 = event;
		if (event.equalsIgnoreCase("enter_necropolis1_q0385_05.htm") || event.equalsIgnoreCase("05.htm")) {
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.setCond(1);
			string2 = "enter_necropolis1_q0385_05.htm";
		} else if (event.equalsIgnoreCase("enter_necropolis1_q0385_09.htm") || event.equalsIgnoreCase("09.htm") || event.equalsIgnoreCase("10.htm")) {
			string2 = "enter_necropolis1_q0385_10.htm";
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		}
		return string2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int n = npc != null ? npc.getNpcId() : 0;
		String html = "noquest";
		if (this.checkNPC(n) && qs.getCond() == 0) {
			if (pc.getLevel() < 20) {
				html = "enter_necropolis1_q0385_02.htm";
				qs.exitQuest(true);
			} else {
				html = "enter_necropolis1_q0385_01.htm";
			}
		} else if (qs.getCond() == 1 && qs.getQuestItemsCount(ANCIENT_SCROLL) == 0L) {
			html = "enter_necropolis1_q0385_11.htm";
		} else if (qs.getCond() == 1 && qs.getQuestItemsCount(ANCIENT_SCROLL) > 0L) {
			html = "enter_necropolis1_q0385_09.htm";
			qs.giveItems(BLANK_SCROLL, qs.getQuestItemsCount(ANCIENT_SCROLL));
			qs.takeItems(ANCIENT_SCROLL, -1L);
		} else {
			qs.exitQuest(true);
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		qs.rollAndGive(ANCIENT_SCROLL, 1, 60.0);
		return null;
	}

	public boolean checkNPC(int n) {
		return n >= 31095 && n <= 31126 && n != 31100 && n != 31111 && n != 31112 && n != 31113;
	}
}
