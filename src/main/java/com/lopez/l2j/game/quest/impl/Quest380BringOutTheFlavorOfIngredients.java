package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 380 - 380_BringOutTheFlavorOfIngredients
 */
@Component
public class Quest380BringOutTheFlavorOfIngredients extends Quest {

	public static final int bgW = 30069; // ROLLANT
	public static final int bxw = 5895; // RITRONS_FRUIT
	public static final int bxx = 5896; // MOON_FACE_FLOWER
	public static final int bxy = 5897; // LEECH_FLUIDS
	public static final int bxz = 1831; // ANTIDOTE
	public static final int bxA = 5959; // RITRON_JELLY_RECIPE
	public static final int bxB = 5960; // JELLY
	public static final int bxC = 55;
	public static final int bxD = 20205; // DIRE_WOLF
	public static final int bxE = 20206; // KADIF_WEREWOLF
	public static final int aXF = 20225; // GIANT_MIST_LEECH
	public static final int[][] DROPLIST_COND = new int[][]{
		{1, 0, 20205, 0, 5895, 4, 10, 1},
		{1, 0, 20206, 0, 5896, 20, 50, 1},
		{1, 0, 20225, 0, 5897, 10, 50, 1}
	};

	public Quest380BringOutTheFlavorOfIngredients(QuestManager questManager) {
		super(380, "380_BringOutTheFlavorOfIngredients", "380_BringOutTheFlavorOfIngredients");
		addStartNpc(30069);
		addTalkId(30069);
		addKillId(20205, 20206, 20225);
		registerQuestItems(5895, 5896, 5897);
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
			return "rollant_q0380_05.htm";
		}
		String html = event;

		if (event.equalsIgnoreCase("rollant_q0380_05.htm") || event.equalsIgnoreCase("30069-04.htm")) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			html = "rollant_q0380_05.htm";
		} else if (event.equalsIgnoreCase("rollant_q0380_12.htm") || event.equalsIgnoreCase("30069-12.htm")) {
			qs.giveItems(5959, 1L);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			html = "rollant_q0380_12.htm";
		}
		return html;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int n = npc != null ? npc.getNpcId() : 0;
		String html = "noquest";
		int n2 = qs.getCond();
		if (n == 30069) {
			if (n2 == 0) {
				if (pc.getLevel() >= 24) {
					html = "rollant_q0380_02.htm";
				} else {
					html = "rollant_q0380_01.htm";
					qs.exitQuest(true);
				}
			} else if (n2 == 1) {
				html = "rollant_q0380_06.htm";
			} else if (n2 == 2 && qs.getQuestItemsCount(1831) >= 2L) {
				qs.takeItems(1831, 2L);
				qs.takeItems(5895, -1L);
				qs.takeItems(5896, -1L);
				qs.takeItems(5897, -1L);
				html = "rollant_q0380_07.htm";
				qs.setCond(3);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (n2 == 2) {
				html = "rollant_q0380_06.htm";
			} else if (n2 == 3) {
				html = "rollant_q0380_08.htm";
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (n2 == 4) {
				html = "rollant_q0380_09.htm";
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (n2 == 5) {
				html = "rollant_q0380_10.htm";
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (n2 == 6) {
				qs.giveItems(5960, 1L);
				if (ThreadLocalRandom.current().nextDouble(100.0) < 55) {
					html = "rollant_q0380_11.htm";
				} else {
					html = "rollant_q0380_14.htm";
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(true);
				}
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getCond();
		for (int i = 0; i < DROPLIST_COND.length; ++i) {
			if (n2 != DROPLIST_COND[i][0] || n != DROPLIST_COND[i][2] || DROPLIST_COND[i][3] != 0 && qs.getQuestItemsCount(DROPLIST_COND[i][3]) <= 0L) continue;
			if (DROPLIST_COND[i][5] == 0) {
				qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][6]);
				continue;
			}
			if (!qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][7], DROPLIST_COND[i][5], DROPLIST_COND[i][6]) || DROPLIST_COND[i][1] == n2 || DROPLIST_COND[i][1] == 0) continue;
			qs.setCond(DROPLIST_COND[i][1]);
			qs.setState(State.STARTED);
		}
		if (n2 == 1 && qs.getQuestItemsCount(5895) >= 4L && qs.getQuestItemsCount(5896) >= 20L && qs.getQuestItemsCount(5897) >= 10L) {
			qs.setCond(2);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
