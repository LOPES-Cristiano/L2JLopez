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
 * Quest 51: O'Fulle's Special Bait
 * Fisher O'Fulle e Fettered Soul.
 */
@Component
public class Quest051OFullesSpecialBait extends Quest {

	public static final int QUEST_ID = 51;
	public static final String QUEST_NAME = "051_OFullesSpecialBait";

	public static final int START_NPC = 31572;
	public static final int OFULLE = 31572;
	public static final int FETTERED_SOUL = 20552;

	public static final int LOST_BAIT = 7622;
	public static final int ICY_AIR_LURE = 7611;

	@Autowired
	public Quest051OFullesSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "O'Fulle's Special Bait");
		addStartNpc(OFULLE);
		addTalkId(OFULLE);
		addKillId(FETTERED_SOUL);
		registerQuestItems(LOST_BAIT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_ofulle_q0051_0104.htm".equalsIgnoreCase(event) || "31572-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_ofulle_q0051_0104.htm";
		} else if ("fisher_ofulle_q0051_0201.htm".equalsIgnoreCase(event) || "31572-07.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(LOST_BAIT) >= 100) {
				qs.takeItems(LOST_BAIT, -1);
				qs.giveItems(ICY_AIR_LURE, 4);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "fisher_ofulle_q0051_0202.htm";
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

		if (npcId == OFULLE) {
			if (cond == 0) {
				if (pc.level() >= 36) return "fisher_ofulle_q0051_0101.htm";
				qs.exitQuest(true);
				return "fisher_ofulle_q0051_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7611) >= 4) {
					qs.takeItems(7611, -1);
					qs.giveItems(7615, 20);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_ofulle_q0051_0202.htm";
				}
				return "fisher_ofulle_q0051_0106.htm";
			} else if (cond == 2) {
				return qs.getQuestItemsCount(LOST_BAIT) >= 100 ? "fisher_ofulle_q0051_0105.htm" : "fisher_ofulle_q0051_0106.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == FETTERED_SOUL && qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(LOST_BAIT);
			if (count < 100) {
				qs.giveItems(LOST_BAIT, 1);
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
