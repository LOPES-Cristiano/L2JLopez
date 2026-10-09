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
 * Quest 024: Inhabitants of the Forest of the Dead
 * Dorian e Maid of Lidia em Forest of the Dead.
 */
@Component
public class Quest024InhabitantsOfTheForestOfTheDead extends Quest {

	public static final int QUEST_ID = 24;
	public static final String QUEST_NAME = "024_InhabitantsOfTheForestOfTheDead";

	public static final int DORIAN = 31389;
	public static final int MAID = 31532;
	public static final int HARDIN = 31522;

	public static final int FLOWER = 7152;
	public static final int SILVER_CROSS = 7153;
	public static final int BROKEN_SILVER_CROSS = 7154;
	public static final int SUSPICIOUS_TOTEM = 7151;

	public static final int[] MOBS = {21557, 21558, 21560, 21563, 21564, 21565, 21566, 21567};

	@Autowired
	public Quest024InhabitantsOfTheForestOfTheDead(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Inhabitants of the Forest of the Dead");
		addStartNpc(DORIAN);
		addTalkId(DORIAN, MAID, HARDIN);
		addKillId(MOBS);
		registerQuestItems(FLOWER, SILVER_CROSS, BROKEN_SILVER_CROSS, SUSPICIOUS_TOTEM);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(FLOWER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "day_dorian_q0024_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == DORIAN) {
			if (cond == 0) {
				if (pc.level() >= 65) return "day_dorian_q0024_01.htm";
				qs.exitQuest(true);
				return "day_dorian_q0024_02.htm";
			} else if (cond == 1) {
				return "day_dorian_q0024_04.htm";
			} else if (cond == 2) {
				qs.giveItems(SILVER_CROSS, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "day_dorian_q0024_08.htm";
			}
		} else if (npcId == MAID) {
			if (cond == 3 && qs.hasQuestItems(SUSPICIOUS_TOTEM)) {
				qs.takeItems(SUSPICIOUS_TOTEM, -1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "maid_of_ridia_q0024_01.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 3 && !qs.hasQuestItems(SUSPICIOUS_TOTEM)) {
			qs.giveItems(SUSPICIOUS_TOTEM, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
