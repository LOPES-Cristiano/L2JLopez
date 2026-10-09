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
 * Quest 001: Letters of Love
 * Darin, Gatekeeper Roxxy e Magister Baul na Ilha das Almas / Talking Island.
 */
@Component
public class Quest001LettersOfLove extends Quest {

	public static final int QUEST_ID = 1;
	public static final String QUEST_NAME = "001_LettersOfLove";

	public static final int DARIN = 30048;
	public static final int ROXXY = 30006;
	public static final int BAUL = 30033;

	public static final int DARINS_LETTER = 687;
	public static final int ROXXYS_KERCHIEF = 688;
	public static final int DARINS_RECEIPT = 1079;
	public static final int BAULS_POTION = 1080;
	public static final int NECKLACE_OF_KNOWLEDGE = 906;

	@Autowired
	public Quest001LettersOfLove(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Letters of Love");
		addStartNpc(DARIN);
		addTalkId(DARIN, ROXXY, BAUL);
		registerQuestItems(DARINS_LETTER, ROXXYS_KERCHIEF, DARINS_RECEIPT, BAULS_POTION);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(DARINS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "daring_q0001_06.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == DARIN) {
			if (cond == 0) {
				if (pc.level() >= 2) return "daring_q0001_02.htm";
				qs.exitQuest(true);
				return "daring_q0001_01.htm";
			} else if (cond == 1) {
				return "daring_q0001_07.htm";
			} else if (cond == 2 && qs.hasQuestItems(ROXXYS_KERCHIEF)) {
				qs.takeItems(ROXXYS_KERCHIEF, -1);
				qs.giveItems(DARINS_RECEIPT, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "daring_q0001_08.htm";
			} else if (cond == 3) {
				return "daring_q0001_09.htm";
			} else if (cond == 4 && qs.hasQuestItems(BAULS_POTION)) {
				qs.takeItems(BAULS_POTION, -1);
				qs.giveItems(NECKLACE_OF_KNOWLEDGE, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "daring_q0001_10.htm";
			}
		} else if (npcId == ROXXY) {
			if (cond == 1 && qs.hasQuestItems(DARINS_LETTER)) {
				qs.takeItems(DARINS_LETTER, -1);
				qs.giveItems(ROXXYS_KERCHIEF, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "gatekeeper_roxis_q0001_01.htm";
			} else if (cond == 2) {
				return "gatekeeper_roxis_q0001_02.htm";
			} else if (cond > 2) {
				return "gatekeeper_roxis_q0001_03.htm";
			}
		} else if (npcId == BAUL) {
			if (cond == 3 && qs.hasQuestItems(DARINS_RECEIPT)) {
				qs.takeItems(DARINS_RECEIPT, -1);
				qs.giveItems(BAULS_POTION, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "magister_baul_q0001_01.htm";
			} else if (cond == 4) {
				return "magister_baul_q0001_02.htm";
			}
		}
		return "noquest";
	}
}
