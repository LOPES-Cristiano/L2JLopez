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
 * Quest 50: Lanosco's Special Bait
 * Fisher Lanosco e Singing Wind.
 */
@Component
public class Quest050LanoscosSpecialBait extends Quest {

	public static final int QUEST_ID = 50;
	public static final String QUEST_NAME = "050_LanoscosSpecialBait";

	public static final int START_NPC = 31570;
	public static final int LANOSCO = 31570;
	public static final int SINGING_WIND = 21026;

	public static final int ESSENCE_OF_WIND = 7621;
	public static final int WIND_FISHING_LURE = 7610;

	@Autowired
	public Quest050LanoscosSpecialBait(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Lanosco's Special Bait");
		addStartNpc(LANOSCO);
		addTalkId(LANOSCO);
		addKillId(SINGING_WIND);
		registerQuestItems(ESSENCE_OF_WIND);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_lanosco_q0050_0104.htm".equalsIgnoreCase(event) || "31570-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_lanosco_q0050_0104.htm";
		} else if ("fisher_lanosco_q0050_0201.htm".equalsIgnoreCase(event) || "31570-07.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ESSENCE_OF_WIND) >= 100) {
				qs.takeItems(ESSENCE_OF_WIND, -1);
				qs.giveItems(WIND_FISHING_LURE, 4);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "fisher_lanosco_q0050_0202.htm";
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

		if (npcId == LANOSCO) {
			if (cond == 0) {
				if (pc.level() >= 27) return "fisher_lanosco_q0050_0101.htm";
				qs.exitQuest(true);
				return "fisher_lanosco_q0050_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(7610) >= 4) {
					qs.takeItems(7610, -1);
					qs.giveItems(7614, 20);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "fisher_lanosco_q0050_0202.htm";
				}
				return "fisher_lanosco_q0050_0106.htm";
			} else if (cond == 2) {
				return qs.getQuestItemsCount(ESSENCE_OF_WIND) >= 100 ? "fisher_lanosco_q0050_0105.htm" : "fisher_lanosco_q0050_0106.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == SINGING_WIND && qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(ESSENCE_OF_WIND);
			if (count < 100) {
				qs.giveItems(ESSENCE_OF_WIND, 1);
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
