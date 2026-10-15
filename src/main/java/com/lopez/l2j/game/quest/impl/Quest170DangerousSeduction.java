package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.stereotype.Component;

/**
 * Quest 170 - Dangerous Seduction
 */
@Component
public class Quest170DangerousSeduction extends Quest {

	public Quest170DangerousSeduction(QuestManager questManager) {
		super(170, "170_DangerousSeduction", "Dangerous Seduction");
		addStartNpc(30305);
		addTalkId(30305);
		addKillId(27022);
		registerQuestItems(1046);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30305-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30305-04.htm".equalsIgnoreCase(event) ? "30305-04.htm" : "tetrarch_vellior_q0327_04.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "no-quest";
		int n = qs.getStateId();
		switch (n) {
			case 1: {
				if (pc.getRace() != Race.darkelf) {
					html = "tetrarch_vellior_q0327_00.htm";
					qs.exitQuest(true);
					break;
				}
				if (pc.level() >= 21) {
					html = "tetrarch_vellior_q0327_03.htm";
					break;
				}
				html = "tetrarch_vellior_q0327_02.htm";
				qs.exitQuest(true);
				break;
			}
			case 2: {
				if (qs.getQuestItemsCount(1046) < 1) {
					html = "tetrarch_vellior_q0327_05.htm";
					break;
				}
				html = "tetrarch_vellior_q0327_06.htm";
				qs.giveItems(57, 102680);
				qs.takeItems(1046, -1);
				qs.exitQuest(false);
				qs.playSound(QuestState.SOUND_FINISH);
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = qs.getCond();
		if (n == 1 && qs.getQuestItemsCount(1046) == 0) {
			qs.giveItems(1046, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
