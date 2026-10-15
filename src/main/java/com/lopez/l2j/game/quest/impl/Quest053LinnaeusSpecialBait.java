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
 * Quest 53: Linnaeus' Special Bait
 * Fisher Linnaeus e Crimson Drake.
 */
@Component
public class Quest053LinnaeusSpecialBait extends Quest {

	public static final int QUEST_ID = 53;
	public static final String QUEST_NAME = "053_LinnaeusSpecialBait";

	public static final int START_NPC = 31577;
	public static final int LINNAEUS = 31577;
	public static final int CRIMSON_DRAKE = 20670;

	public static final int HEART_OF_CRIMSON_DRAKE = 7624;
	public static final int FLAMING_FISHING_LURE = 7613;

	@Autowired
	public Quest053LinnaeusSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Linnaeus' Special Bait");
		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS);
		addKillId(CRIMSON_DRAKE);
		registerQuestItems(HEART_OF_CRIMSON_DRAKE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_linneaus_q0053_0104.htm".equalsIgnoreCase(event) || "31577-03.htm".equalsIgnoreCase(event) || "31577-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_linneaus_q0053_0104.htm";
		} else if ("fisher_linneaus_q0053_0201.htm".equalsIgnoreCase(event) || "31577-07.htm".equalsIgnoreCase(event) || "31577-3.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(HEART_OF_CRIMSON_DRAKE) >= 100) {
				qs.takeItems(HEART_OF_CRIMSON_DRAKE, -1);
				qs.giveItems(FLAMING_FISHING_LURE, 4);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "fisher_linneaus_q0053_0202.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == LINNAEUS) {
			if (cond == 0) {
				if (pc.level() >= 60) return "fisher_linneaus_q0053_0101.htm";
				qs.exitQuest(true);
				return "fisher_linneaus_q0053_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7613) >= 4) {
					qs.takeItems(7613, -1);
					qs.giveItems(7617, 20);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_linneaus_q0053_0202.htm";
				}
				return "fisher_linneaus_q0053_0106.htm";
			} else if (cond == 2) {
				return qs.getQuestItemsCount(HEART_OF_CRIMSON_DRAKE) >= 100 ? "fisher_linneaus_q0053_0105.htm" : "fisher_linneaus_q0053_0106.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == CRIMSON_DRAKE && qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(HEART_OF_CRIMSON_DRAKE);
			if (count < 100) {
				qs.giveItems(HEART_OF_CRIMSON_DRAKE, 1);
				if (count + 1 >= 100) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
