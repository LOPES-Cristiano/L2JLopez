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
 * Quest 006: Step into the Future
 * Gatekeeper Roxxy em Talking Island, Magister Baul e Sir Collin Windawood em Gludio.
 */
@Component
public class Quest006StepIntoTheFuture extends Quest {

	public static final int QUEST_ID = 6;
	public static final String QUEST_NAME = "006_StepIntoTheFuture";

	public static final int ROXXY = 30006;
	public static final int BAUL = 30033;
	public static final int SIR_COLLIN = 30311;

	public static final int ROXXYS_LETTER = 7571;
	public static final int SCROLL_OF_ESCAPE_GIRAN = 7126;
	public static final int MARK_OF_TRAVELER = 7570;

	@Autowired
	public Quest006StepIntoTheFuture(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Step into the Future");
		addStartNpc(ROXXY);
		addTalkId(ROXXY, BAUL, SIR_COLLIN);
		registerQuestItems(ROXXYS_LETTER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ROXXYS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "gatekeeper_roxis_q0006_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ROXXY) {
			if (cond == 0) {
				if (pc.level() >= 3 && pc.race() == 0) return "gatekeeper_roxis_q0006_02.htm";
				qs.exitQuest(true);
				return "gatekeeper_roxis_q0006_01.htm";
			} else if (cond == 1) {
				return "gatekeeper_roxis_q0006_04.htm";
			} else if (cond == 2) {
				return "gatekeeper_roxis_q0006_05.htm";
			} else if (cond == 3) {
				qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.giveItems(MARK_OF_TRAVELER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gatekeeper_roxis_q0006_06.htm";
			}
		} else if (npcId == BAUL) {
			if (cond == 1 && qs.hasQuestItems(ROXXYS_LETTER)) {
				qs.takeItems(ROXXYS_LETTER, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "magister_baul_q0006_01.htm";
			} else if (cond == 2) {
				return "magister_baul_q0006_02.htm";
			}
		} else if (npcId == SIR_COLLIN) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "sir_collin_windawood_q0006_01.htm";
			} else if (cond == 3) {
				return "sir_collin_windawood_q0006_02.htm";
			}
		}
		return "noquest";
	}
}
