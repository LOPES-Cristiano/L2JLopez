package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 297 - 297_GateKeepersFavor
 */
@Component
public class Quest297GateKeepersFavor extends Quest {

	public static final int WIRPHY = 30540;
	public static final int WHINSTONE_GOLEM = 20521;
	public static final int STARSTONE2 = 1573;
	public static final int GATEKEEPER_TOKEN = 1659;

	public static final int beZ = 1573;
	public static final int bfa = 1659;

	public Quest297GateKeepersFavor(QuestManager questManager) {
		super(297, "297_GateKeepersFavor", "297_GateKeepersFavor");
		addStartNpc(30540);
		addTalkId(30540);
		addKillId(20521);
		registerQuestItems(1573);
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

		String string2 = event;
		if (event.equalsIgnoreCase("gatekeeper_wirphy_q0297_03.htm") || event.equalsIgnoreCase("30540-03.htm")) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
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
		if (n == WIRPHY) {
			if (n2 == 0) {
				html = pc.getLevel() >= 15 ? "gatekeeper_wirphy_q0297_02.htm" : "gatekeeper_wirphy_q0297_01.htm";
			} else if (n2 == 1 && qs.getQuestItemsCount(STARSTONE2) < 20L) {
				html = "gatekeeper_wirphy_q0297_04.htm";
			} else if (n2 == 2 && qs.getQuestItemsCount(STARSTONE2) < 20L) {
				html = "gatekeeper_wirphy_q0297_04.htm";
			} else if (n2 == 2 && qs.getQuestItemsCount(STARSTONE2) >= 20L) {
				html = "gatekeeper_wirphy_q0297_05.htm";
				qs.takeItems(STARSTONE2, -1L);
				qs.giveItems(GATEKEEPER_TOKEN, 2L);
				qs.exitQuest(true);
				qs.playSound(QuestState.SOUND_FINISH);
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if (qs.getCond() == 1) {
			qs.rollAndGive(STARSTONE2, 1, 1, 20, 33.0);
			if (qs.getQuestItemsCount(STARSTONE2) >= 20L) {
				qs.setCond(2);
			}
		}
		return null;
	}
}
