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
 * Quest 011: Secret Meeting with Ketra Orcs
 * Cadmon em Rune, Trader Leon e Wahkan nos arredores de Ketra.
 */
@Component
public class Quest011SecretMeetingWithKetraOrcs extends Quest {

	public static final int QUEST_ID = 11;
	public static final String QUEST_NAME = "011_SecretMeetingWithKetraOrcs";

	public static final int CADMON = 31296;
	public static final int LEON = 31256;
	public static final int WAHKAN = 31371;

	public static final int MUNITIONS_BOX = 7231;

	@Autowired
	public Quest011SecretMeetingWithKetraOrcs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Secret Meeting with Ketra Orcs");
		addStartNpc(CADMON);
		addTalkId(CADMON, LEON, WAHKAN, 31555);
		registerQuestItems(MUNITIONS_BOX);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "guard_cadmon_q0011_0104.htm".equalsIgnoreCase(event) || "31296-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "guard_cadmon_q0011_0104.htm";
		} else if ("trader_leon_q0011_0201.htm".equalsIgnoreCase(event) || "31256-02.htm".equalsIgnoreCase(event)) {
			qs.giveItems(MUNITIONS_BOX, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("herald_wakan_q0011_0301.htm".equalsIgnoreCase(event) || "31371-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MUNITIONS_BOX, -1);
			qs.addExpAndSp(79787, 0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		if (npcId == CADMON) {
			if (cond == 0) {
				if (pc.level() >= 74) return "guard_cadmon_q0011_0101.htm";
				qs.exitQuest(true);
				return "guard_cadmon_q0011_0103.htm";
			} else if (cond == 1) {
				return "guard_cadmon_q0011_0105.htm";
			}
		} else if (npcId == LEON || npcId == 31555) {
			if (cond == 1) {
				qs.giveItems(MUNITIONS_BOX, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_leon_q0011_0101.htm";
			} else if (cond == 2) {
				return "trader_leon_q0011_0202.htm";
			}
		} else if (npcId == WAHKAN) {
			if (cond == 2 && qs.hasQuestItems(MUNITIONS_BOX)) {
				qs.takeItems(MUNITIONS_BOX, -1);
				qs.addExpAndSp(79787, 0);
				qs.giveItems(57, 79787);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "herald_wahkan_q0011_0101.htm";
			}
		}
		return "noquest";
	}
}
