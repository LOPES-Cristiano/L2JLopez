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
 * Quest 298 - 298_LizardmensConspiracy
 */
@Component
public class Quest298LizardmensConspiracy extends Quest {

	public static final int PRAGA = 30333;
	public static final int ROHMER = 30344;
	public static final int MAILLE_LIZARDMAN_WARRIOR = 20922;
	public static final int MAILLE_LIZARDMAN_SHAMAN = 20923;
	public static final int MAILLE_LIZARDMAN_MATRIARCH = 20924;
	public static final int POISON_ARANEID = 20926;
	public static final int KING_OF_THE_ARANEID = 20927;
	public static final int REPORT = 7182;
	public static final int SHINING_GEM = 7183;
	public static final int SHINING_RED_GEM = 7184;
	public static final int[][] MobsTable = new int[][]{{20922, 7183}, {20923, 7183}, {20924, 7183}, {20926, 7184}, {20927, 7184}};

	public Quest298LizardmensConspiracy(QuestManager questManager) {
		super(298, "298_LizardmensConspiracy", "298_LizardmensConspiracy");
		addStartNpc(30333);
		addTalkId(30333, 30344);
		addKillId(20922, 20923, 20924, 20926, 20927);
		registerQuestItems(7182, 7183, 7184);
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
			if (qs.getQuestItemsCount(REPORT) == 0) {
				qs.giveItems(REPORT, 1L);
			}
		}

		String string2 = event;
		if (event.equalsIgnoreCase("guard_praga_q0298_0104.htm") || event.equalsIgnoreCase("30333-1.htm")) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(REPORT, 1L);
			qs.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("magister_rohmer_q0298_0201.htm") || event.equalsIgnoreCase("30344-1.htm")) {
			qs.takeItems(REPORT, -1L);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ((event.equalsIgnoreCase("magister_rohmer_q0298_0301.htm") || event.equalsIgnoreCase("30344-4.htm"))
				&& (qs.getCond() == 3 || qs.getQuestItemsCount(SHINING_GEM) + qs.getQuestItemsCount(SHINING_RED_GEM) > 99L)) {
			qs.takeItems(SHINING_GEM, -1L);
			qs.takeItems(SHINING_RED_GEM, -1L);
			qs.addExpAndSp(0L, 42000L);
			qs.exitQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
		}
		return string2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getCond();
		if (n == PRAGA) {
			if (n2 < 1) {
				if (pc.getLevel() < 25) {
					html = "guard_praga_q0298_0102.htm";
					qs.exitQuest(true);
				} else {
					html = "guard_praga_q0298_0101.htm";
				}
			}
			if (n2 == 1) {
				html = "guard_praga_q0298_0105.htm";
			}
		} else if (n == ROHMER) {
			if (n2 < 1) {
				html = "magister_rohmer_q0298_0202.htm";
			} else if (n2 == 1) {
				html = "magister_rohmer_q0298_0101.htm";
			} else if (n2 == 2 || qs.getQuestItemsCount(SHINING_GEM) + qs.getQuestItemsCount(SHINING_RED_GEM) < 100L) {
				html = "magister_rohmer_q0298_0204.htm";
				qs.setCond(2);
			} else if (n2 == 3 && qs.getQuestItemsCount(SHINING_GEM) + qs.getQuestItemsCount(SHINING_RED_GEM) > 99L) {
				html = "magister_rohmer_q0298_0203.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = ThreadLocalRandom.current().nextInt(10);
		if (qs.getCond() == 2) {
			for (int[] nArray : MobsTable) {
				if (n != nArray[0] || n2 >= 6 || qs.getQuestItemsCount(nArray[1]) >= 50L) continue;
				if (n2 < 2 && nArray[1] == SHINING_GEM) {
					qs.giveItems(nArray[1], 2L);
				} else {
					qs.giveItems(nArray[1], 1L);
				}
				if (qs.getQuestItemsCount(SHINING_GEM) + qs.getQuestItemsCount(SHINING_RED_GEM) > 99L) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
					continue;
				}
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
