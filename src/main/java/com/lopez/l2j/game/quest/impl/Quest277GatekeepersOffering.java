package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 277 - Gatekeepers Offering
 */
@Component
public class Quest277GatekeepersOffering extends Quest {

	public static final int TAMIL = 30576;
	public static final int GRAYSTONE_GOLEM = 20333;
	public static final int STARSTONE = 1572;
	public static final int GATEKEEPER_CHARM = 1658;

	public static final int bev = 1572;
	public static final int bew = 1658;

	public Quest277GatekeepersOffering(QuestManager questManager) {
		super(277, "277_GatekeepersOffering", "Gatekeepers Offering");
		addStartNpc(30576);
		addTalkId(30576);
		addKillId(20333);
		registerQuestItems(1572);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)
				|| "gatekeeper_tamil_q0277_03.htm".equalsIgnoreCase(event) || "30576-03.htm".equalsIgnoreCase(event)) {
			if (pc != null && pc.level() < 15 && "1".equals(event)) {
				return "gatekeeper_tamil_q0277_01.htm";
			}
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			if ("1".equals(event)) {
				return "gatekeeper_tamil_q0277_03.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "noquest";
		int n = npc != null ? npc.getNpcId() : 0;
		int n2 = qs.getCond();
		if (n == TAMIL && n2 == 0) {
			html = pc.level() < 15 ? "gatekeeper_tamil_q0277_01.htm" : "gatekeeper_tamil_q0277_02.htm";
		} else if (n == TAMIL && n2 == 1 && qs.getQuestItemsCount(STARSTONE) < 20) {
			html = "gatekeeper_tamil_q0277_04.htm";
		} else if (n == TAMIL && n2 == 2 && qs.getQuestItemsCount(STARSTONE) < 20) {
			html = "gatekeeper_tamil_q0277_04.htm";
		} else if (n == TAMIL && n2 == 2 && qs.getQuestItemsCount(STARSTONE) >= 20) {
			html = "gatekeeper_tamil_q0277_05.htm";
			qs.takeItems(STARSTONE, -1);
			qs.giveItems(GATEKEEPER_CHARM, 2);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1) {
			qs.rollAndGive(STARSTONE, 1, 1, 20, 33.0);
			if (qs.getQuestItemsCount(STARSTONE) >= 20) {
				qs.setCond(2);
			}
		}
		return null;
	}
}
