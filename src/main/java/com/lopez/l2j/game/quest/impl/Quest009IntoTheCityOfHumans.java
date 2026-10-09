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
 * Quest 009: Into the City of Humans
 * Centurion Petukai, Seer Tanapi e Gatekeeper Tamata na Orc Village.
 */
@Component
public class Quest009IntoTheCityOfHumans extends Quest {

	public static final int QUEST_ID = 9;
	public static final String QUEST_NAME = "009_IntoTheCityOfHumans";

	public static final int PETUKAI = 30583;
	public static final int TANAPI = 30571;
	public static final int TAMATA = 30576;

	public static final int SCROLL_OF_ESCAPE_GIRAN = 7126;
	public static final int MARK_OF_TRAVELER = 7570;

	@Autowired
	public Quest009IntoTheCityOfHumans(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Into the City of Humans");
		addStartNpc(PETUKAI);
		addTalkId(PETUKAI, TANAPI, TAMATA);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "centurion_petukai_q0009_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == PETUKAI) {
			if (cond == 0) {
				if (pc.level() >= 3 && pc.race() == 3) return "centurion_petukai_q0009_02.htm";
				qs.exitQuest(true);
				return "centurion_petukai_q0009_01.htm";
			} else if (cond == 1) {
				return "centurion_petukai_q0009_04.htm";
			}
		} else if (npcId == TANAPI) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "seer_tanapi_q0009_01.htm";
			} else if (cond == 2) {
				return "seer_tanapi_q0009_02.htm";
			}
		} else if (npcId == TAMATA) {
			if (cond == 2) {
				qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.giveItems(MARK_OF_TRAVELER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gatekeeper_tamatha_q0009_01.htm";
			}
		}
		return "noquest";
	}
}
