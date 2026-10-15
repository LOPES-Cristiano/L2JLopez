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
 * Quest 124: Meeting the Elroki
 */
@Component
public class Quest124MeetingTheElroki extends Quest {

	public static final int QUEST_ID = 124;
	public static final String QUEST_NAME = "124_MeetingTheElroki";
	public static final int START_NPC = 32113;

	public static final int MARQUEZ = 32113;
	public static final int MUSHIKA = 32114;
	public static final int ASAMAH = 32115;
	public static final int KARAKAWEI = 32117;
	public static final int MANTARASA = 32118;

	public static final int MANTARASA_EGG = 8778;

	@Autowired
	public Quest124MeetingTheElroki(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Meeting the Elroki");
		addStartNpc(START_NPC);
		addTalkId(32113, 32114, 32115, 32117, 32118);
		registerQuestItems(8778);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "32113-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) ? "marquez_q0124_0104.htm" : event;
		} else if ("32113-03.htm".equalsIgnoreCase(event) || "32113-04.htm".equalsIgnoreCase(event) || "reply_2".equalsIgnoreCase(event)) {
			if (qs.getCond() == 0) {
				qs.setState(State.STARTED);
				qs.setCond(1);
				qs.playSound(QuestState.SOUND_ACCEPT);
			} else {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("32114-02.htm".equalsIgnoreCase(event) || "reply_2a".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32114-02.htm".equalsIgnoreCase(event) ? "32114-02.htm" : "mushika_q0124_03.htm";
		} else if ("32115-04.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32115-04.htm".equalsIgnoreCase(event) ? "32115-04.htm" : "asama_q0124_06.htm";
		} else if ("32117-02.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() == 4) {
				qs.set("progress", "1");
			}
			return "32117-02.htm";
		} else if ("32117-03.htm".equalsIgnoreCase(event) || "reply_9".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32117-03.htm".equalsIgnoreCase(event) ? "32117-03.htm" : "shaman_caracawe_q0124_05.htm";
		} else if ("32118-02.htm".equalsIgnoreCase(event) || "reply_10".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.giveItems(MANTARASA_EGG, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32118-02.htm".equalsIgnoreCase(event) ? "32118-02.htm" : "egg_of_mantarasa_q0124_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == MARQUEZ) {
			if (cond == 0) {
				if (pc.level() >= 75) return "marquez_q0124_0101.htm";
				qs.exitQuest(true);
				return "marquez_q0124_0103.htm";
			} else if (cond == 1) {
				return "marquez_q0124_0105.htm";
			} else if (cond >= 2) {
				return "32113-04a.htm";
			}
		} else if (npcId == MUSHIKA) {
			if (cond == 2) return "32114-01.htm";
			if (cond > 2) return "32114-03.htm";
		} else if (npcId == ASAMAH) {
			if (cond == 1) {
				qs.giveItems(57, 71360);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "mushika_q0124_0201.htm";
			} else if (cond == 3) {
				return "32115-01.htm";
			} else if (cond == 6) {
				qs.takeItems(MANTARASA_EGG, -1);
				qs.giveItems(57, 71318);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "32115-05.htm";
			}
		} else if (npcId == KARAKAWEI) {
			if (cond == 4) {
				return "1".equals(qs.get("progress")) ? "32117-02.htm" : "32117-01.htm";
			} else if (cond > 4) {
				return "32117-04.htm";
			}
		} else if (npcId == MANTARASA) {
			if (cond == 5) return "32118-01.htm";
			if (cond > 5) return "32118-03.htm";
		}
		return "noquest";
	}
}
