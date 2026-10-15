package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 261 - Collectors Dream
 */
@Component
public class Quest261CollectorsDream extends Quest {

	public static final int ALSHUPES = 30222;
	public static final int GIANT_SPIDER_LEG = 1087;

	public Quest261CollectorsDream(QuestManager questManager) {
		super(261, "261_CollectorsDream", "Collectors Dream");
		addStartNpc(30222);
		addTalkId(30222);
		addKillId(20308, 20460, 20466);
		registerQuestItems(GIANT_SPIDER_LEG);
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
		}

		String event2 = event;
		if (event.equalsIgnoreCase("moneylender_alshupes_q0261_03.htm") || event.equalsIgnoreCase("30222-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		return event2;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = qs.getCond();
		if (n == 0) {
			if (pc.level() >= 15) {
				html = "moneylender_alshupes_q0261_02.htm";
				return html;
			}
			html = "moneylender_alshupes_q0261_01.htm";
			qs.exitQuest(true);
		} else if (n == 1 || qs.getQuestItemsCount(GIANT_SPIDER_LEG) < 8) {
			html = "moneylender_alshupes_q0261_04.htm";
		} else if (n == 2 && qs.getQuestItemsCount(GIANT_SPIDER_LEG) >= 8) {
			qs.takeItems(GIANT_SPIDER_LEG, -1);
			qs.giveItems(57, 1000);
			qs.addExpAndSp(2000, 0);
			html = "moneylender_alshupes_q0261_05.htm";
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1 && qs.getQuestItemsCount(GIANT_SPIDER_LEG) < 8) {
			qs.giveItems(GIANT_SPIDER_LEG, 1);
			if (qs.getQuestItemsCount(GIANT_SPIDER_LEG) == 8) {
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.setCond(2);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
