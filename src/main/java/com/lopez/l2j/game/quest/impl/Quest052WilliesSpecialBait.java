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
 * Quest 52: Willie's Special Bait
 * Fisher Willie e Tarlk Basilisk.
 */
@Component
public class Quest052WilliesSpecialBait extends Quest {

	public static final int QUEST_ID = 52;
	public static final String QUEST_NAME = "052_WilliesSpecialBait";

	public static final int START_NPC = 31574;
	public static final int WILLIE = 31574;
	public static final int TARLK_BASILISK = 20573;

	public static final int EYE_OF_TARLK_BASILISK = 7623;
	public static final int EARTH_FISHING_LURE = 7612;

	@Autowired
	public Quest052WilliesSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Willie's Special Bait");
		addStartNpc(WILLIE);
		addTalkId(WILLIE);
		addKillId(TARLK_BASILISK);
		registerQuestItems(EYE_OF_TARLK_BASILISK);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_willeri_q0052_0104.htm".equalsIgnoreCase(event) || "31574-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_willeri_q0052_0104.htm";
		} else if ("fisher_willeri_q0052_0201.htm".equalsIgnoreCase(event) || "31574-07.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(EYE_OF_TARLK_BASILISK) >= 100) {
				qs.takeItems(EYE_OF_TARLK_BASILISK, -1);
				qs.giveItems(EARTH_FISHING_LURE, 4);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "fisher_willeri_q0052_0202.htm";
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

		if (npcId == WILLIE) {
			if (cond == 0) {
				if (pc.level() >= 48) return "fisher_willeri_q0052_0101.htm";
				qs.exitQuest(true);
				return "fisher_willeri_q0052_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7612) >= 4) {
					qs.takeItems(7612, -1);
					qs.giveItems(7616, 20);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_willeri_q0052_0202.htm";
				}
				return "fisher_willeri_q0052_0106.htm";
			} else if (cond == 2) {
				return qs.getQuestItemsCount(EYE_OF_TARLK_BASILISK) >= 100 ? "fisher_willeri_q0052_0105.htm" : "fisher_willeri_q0052_0106.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == TARLK_BASILISK && qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(EYE_OF_TARLK_BASILISK);
			if (count < 100) {
				qs.giveItems(EYE_OF_TARLK_BASILISK, 1);
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
