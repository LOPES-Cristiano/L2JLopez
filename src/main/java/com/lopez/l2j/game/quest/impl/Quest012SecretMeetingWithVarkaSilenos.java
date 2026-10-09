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
 * Quest 012: Secret Meeting with Varka Silenos
 * Cadmon em Rune, Helmut e Naran Ashanuk em Varka Silenos.
 */
@Component
public class Quest012SecretMeetingWithVarkaSilenos extends Quest {

	public static final int QUEST_ID = 12;
	public static final String QUEST_NAME = "012_SecretMeetingWithVarkaSilenos";

	public static final int CADMON = 31296;
	public static final int HELMUT = 31258;
	public static final int NARAN = 31378;

	public static final int MUNITIONS_BOX = 7232;

	@Autowired
	public Quest012SecretMeetingWithVarkaSilenos(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Secret Meeting with Varka Silenos");
		addStartNpc(CADMON);
		addTalkId(CADMON, HELMUT, NARAN);
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
			return "guard_cadmon_q0012_0104.htm";
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
				if (pc.level() >= 74) return "guard_cadmon_q0012_0101.htm";
				qs.exitQuest(true);
				return "guard_cadmon_q0012_0103.htm";
			} else if (cond == 1) {
				return "guard_cadmon_q0012_0105.htm";
			}
		} else if (npcId == HELMUT) {
			if (cond == 1) {
				qs.giveItems(MUNITIONS_BOX, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_helmut_q0012_0101.htm";
			} else if (cond == 2) {
				return "trader_helmut_q0012_0202.htm";
			}
		} else if (npcId == NARAN) {
			if (cond == 2 && qs.hasQuestItems(MUNITIONS_BOX)) {
				qs.takeItems(MUNITIONS_BOX, -1);
				qs.giveItems(57, 79787);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "herald_naran_q0012_0101.htm";
			}
		}
		return "noquest";
	}
}
