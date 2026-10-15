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
 * Quest 363 - 363_SorrowfulSoundofFlute
 */
@Component
public class Quest363SorrowfulSoundofFlute extends Quest {

	public static final int NANARIN = 30956;
	public static final int BARBADO = 30959;
	public static final int POITAN = 30458;
	public static final int HOLVAS = 30058;
	public static final int ALDO = 30057;
	public static final int RANSPO = 30594;
	public static final int OPAL = 30595;

	public static final int MUSICAL_SCORE = 4420;
	public static final int EVENT_CLOTHES = 4318;
	public static final int NANARINS_FLUTE = 4319;
	public static final int SABRINS_BLACK_BEER = 4320;
	public static final int Musical_Score = 4420;

	public Quest363SorrowfulSoundofFlute(QuestManager questManager) {
		super(363, "363_SorrowfulSoundofFlute", "363_SorrowfulSoundofFlute");
		addStartNpc(NANARIN);
		addTalkId(NANARIN, BARBADO, POITAN, HOLVAS, ALDO, RANSPO, OPAL);
		registerQuestItems(EVENT_CLOTHES, NANARINS_FLUTE, SABRINS_BLACK_BEER);
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
			return "30956_2.htm";
		}

		String string2 = event;
		if (event.equalsIgnoreCase("30956_2.htm") || event.equalsIgnoreCase("30956-02.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.takeItems(EVENT_CLOTHES, -1L);
			qs.takeItems(NANARINS_FLUTE, -1L);
			qs.takeItems(SABRINS_BLACK_BEER, -1L);
			string2 = "30956_2.htm";
		} else if (event.equalsIgnoreCase("30956_4.htm") || event.equalsIgnoreCase("30956-05.htm")) {
			qs.giveItems(NANARINS_FLUTE, 1L);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(3);
			string2 = "30956_4.htm";
		} else if (event.equalsIgnoreCase("answer1") || event.equalsIgnoreCase("30956-06.htm")) {
			qs.giveItems(EVENT_CLOTHES, 1L);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(3);
			string2 = "30956_6.htm";
		} else if (event.equalsIgnoreCase("answer2") || event.equalsIgnoreCase("30956-07.htm")) {
			qs.giveItems(SABRINS_BLACK_BEER, 1L);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.setCond(3);
			string2 = "30956_6.htm";
		} else if (event.equalsIgnoreCase("30956_7.htm")) {
			qs.giveItems(MUSICAL_SCORE, 1L);
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
		int n2 = qs.getCond();
		if (n == NANARIN) {
			if (n2 == 0) {
				if (pc.getLevel() < 15) {
					html = "30956_1.htm";
					qs.exitQuest(true);
				} else {
					html = "30956_1.htm";
				}
			} else if (n2 == 1) {
				html = "30956_8.htm";
			} else if (n2 == 2) {
				html = "30956_3.htm";
			} else if (n2 == 3) {
				html = "30956_6.htm";
			} else if (n2 == 4) {
				html = "30956_5.htm";
			}
		} else if (n == BARBADO) {
			if (n2 == 3) {
				if (qs.getQuestItemsCount(EVENT_CLOTHES) > 0L) {
					qs.takeItems(EVENT_CLOTHES, -1L);
					html = "30959_2.htm";
					qs.exitQuest(true);
				} else if (qs.getQuestItemsCount(SABRINS_BLACK_BEER) > 0L) {
					qs.takeItems(SABRINS_BLACK_BEER, -1L);
					html = "30959_2.htm";
					qs.exitQuest(true);
				} else {
					qs.takeItems(NANARINS_FLUTE, -1L);
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					html = "30959_1.htm";
				}
			} else if (n2 == 4) {
				html = "30959_3.htm";
			}
		} else if (n == HOLVAS && (n2 == 1 || n2 == 2)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			html = (ThreadLocalRandom.current().nextDouble(100.0) < 60) ? "30058_2.htm" : "30058_1.htm";
		} else if (n == POITAN && (n2 == 1 || n2 == 2)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			html = (ThreadLocalRandom.current().nextDouble(100.0) < 60) ? "30458_2.htm" : "30458_1.htm";
		} else if ((n == ALDO || n == RANSPO || n == OPAL) && (n2 == 1 || n2 == 2)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			html = "30058_1.htm";
		}
		return html;
	}
}
