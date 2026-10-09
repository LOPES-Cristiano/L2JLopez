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
	public static final int LEON = 31555;
	public static final int WAHKAN = 31371;

	public static final int MUNITIONS_BOX = 7231;

	@Autowired
	public Quest011SecretMeetingWithKetraOrcs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Secret Meeting with Ketra Orcs");
		addStartNpc(CADMON);
		addTalkId(CADMON, LEON, WAHKAN);
		registerQuestItems(MUNITIONS_BOX);
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
			return "guard_cadmon_q0011_0104.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == CADMON) {
			if (cond == 0) {
				if (pc.level() >= 74) return "guard_cadmon_q0011_0101.htm";
				qs.exitQuest(true);
				return "guard_cadmon_q0011_0103.htm";
			} else if (cond == 1) {
				return "guard_cadmon_q0011_0105.htm";
			}
		} else if (npcId == LEON) {
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
				qs.giveItems(57, 79787);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "herald_wahkan_q0011_0101.htm";
			}
		}
		return "noquest";
	}
}
