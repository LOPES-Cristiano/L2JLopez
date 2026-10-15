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
 * Quest 47: Into The Dark Forest
 * Trader Galladucci, Magic Trader Gentler, Jeweler Sandra e Priest Dustin em Giran.
 */
@Component
public class Quest047IntoTheDarkForest extends Quest {

	public static final int QUEST_ID = 47;
	public static final String QUEST_NAME = "047_IntoTheDarkForest";

	public static final int START_NPC = 30097;
	public static final int GALLADUCCI = 30097;
	public static final int GENTLER = 30094;
	public static final int SANDRA = 30090;
	public static final int DUSTIN = 30116;

	public static final int ORDER_DOCUMENT_1 = 7563;
	public static final int ORDER_DOCUMENT_2 = 7564;
	public static final int ORDER_DOCUMENT_3 = 7565;
	public static final int PURIFIED_MAGIC_NECKLACE = 7566;
	public static final int GEMSTONE_POWDER = 7567;
	public static final int MAGIC_SWORD_HILT = 7568;
	public static final int MARK_OF_TRAVELER = 7570;
	public static final int SCROLL_OF_ESCAPE_SPECIAL = 7556;

	@Autowired
	public Quest047IntoTheDarkForest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Into The Dark Forest");
		addStartNpc(GALLADUCCI);
		addTalkId(GALLADUCCI, GENTLER, SANDRA, DUSTIN);
		registerQuestItems(ORDER_DOCUMENT_1, ORDER_DOCUMENT_2, ORDER_DOCUMENT_3, PURIFIED_MAGIC_NECKLACE, GEMSTONE_POWDER, MAGIC_SWORD_HILT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event) || "30097-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ORDER_DOCUMENT_1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "galladuchi_q0047_0104.htm";
		} else if ("2".equalsIgnoreCase(event) || "30094-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.takeItems(ORDER_DOCUMENT_1, 1);
			qs.giveItems(MAGIC_SWORD_HILT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "gentler_q0047_0201.htm";
		} else if ("3".equalsIgnoreCase(event) || "30097-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.takeItems(MAGIC_SWORD_HILT, 1);
			qs.giveItems(ORDER_DOCUMENT_2, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "galladuchi_q0047_0301.htm";
		} else if ("4".equalsIgnoreCase(event) || "30090-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.takeItems(ORDER_DOCUMENT_2, 1);
			qs.giveItems(GEMSTONE_POWDER, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "sandra_q0047_0401.htm";
		} else if ("5".equalsIgnoreCase(event) || "30097-09.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.takeItems(GEMSTONE_POWDER, 1);
			qs.giveItems(ORDER_DOCUMENT_3, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "galladuchi_q0047_0501.htm";
		} else if ("6".equalsIgnoreCase(event) || "30116-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.takeItems(ORDER_DOCUMENT_3, 1);
			qs.giveItems(PURIFIED_MAGIC_NECKLACE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "dustin_q0047_0601.htm";
		} else if ("7".equalsIgnoreCase(event) || "30097-12.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MARK_OF_TRAVELER, -1);
			qs.takeItems(PURIFIED_MAGIC_NECKLACE, 1);
			qs.giveItems(SCROLL_OF_ESCAPE_SPECIAL, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "galladuchi_q0047_0701.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case GALLADUCCI:
				if (cond == 0) {
					if (pc.race() == 2 && pc.level() >= 1) return "galladuchi_q0047_0101.htm";
					qs.exitQuest(true);
					return "galladuchi_q0047_0103.htm";
				} else if (cond == 1) {
					return "galladuchi_q0047_0105.htm";
				} else if (cond == 2) {
					return "galladuchi_q0047_0201.htm";
				} else if (cond == 3) {
					return "galladuchi_q0047_0303.htm";
				} else if (cond == 4) {
					return "galladuchi_q0047_0401.htm";
				} else if (cond == 5) {
					return "galladuchi_q0047_0503.htm";
				} else if (cond == 6) {
					return "galladuchi_q0047_0601.htm";
				}
				break;
			case GENTLER:
				if (cond == 1) {
					return "gentler_q0047_0101.htm";
				} else if (cond >= 2) {
					return "gentler_q0047_0203.htm";
				}
				break;
			case SANDRA:
				if (cond == 3) {
					return "sandra_q0047_0301.htm";
				} else if (cond >= 4) {
					return "sandra_q0047_0403.htm";
				}
				break;
			case DUSTIN:
				if (cond == 5) {
					return "dustin_q0047_0501.htm";
				} else if (cond >= 6) {
					return "dustin_q0047_0603.htm";
				}
				break;
		}
		return "noquest";
	}
}
