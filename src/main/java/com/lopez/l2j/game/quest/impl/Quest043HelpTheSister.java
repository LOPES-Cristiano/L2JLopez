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
 * Quest 43: Help the Sister!
 * Pet Manager Cooper e Trader Galladucci em Giran.
 */
@Component
public class Quest043HelpTheSister extends Quest {

	public static final int QUEST_ID = 43;
	public static final String QUEST_NAME = "043_HelptheSister";

	public static final int START_NPC = 30829;
	public static final int COOPER = 30829;
	public static final int GALLADUCCI = 30097;

	public static final int SPECTER = 20171;
	public static final int SORROW_MAIDEN = 20197;

	public static final int CRAFTED_DAGGER = 220;
	public static final int MAP_PIECE = 7550;
	public static final int MAP = 7551;
	public static final int PET_TICKET = 7584;

	@Autowired
	public Quest043HelpTheSister(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Help the Sister!");
		addStartNpc(COOPER);
		addTalkId(COOPER, GALLADUCCI);
		addKillId(SPECTER, SORROW_MAIDEN);
		registerQuestItems(MAP_PIECE, MAP);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event) || "30829-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "pet_manager_cooper_q0043_0104.htm";
		} else if ("3".equalsIgnoreCase(event) || "30829-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CRAFTED_DAGGER)) {
				qs.setCond(2);
				qs.takeItems(CRAFTED_DAGGER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_cooper_q0043_0201.htm";
			}
			return "pet_manager_cooper_q0043_0204.htm";
		} else if ("4".equalsIgnoreCase(event) || "30829-05.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(MAP_PIECE) >= 30) {
				qs.setCond(4);
				qs.takeItems(MAP_PIECE, 30);
				qs.giveItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_cooper_q0043_0301.htm";
			}
			return "pet_manager_cooper_q0043_0303.htm";
		} else if ("5".equalsIgnoreCase(event) || "30097-06.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MAP)) {
				qs.setCond(5);
				qs.takeItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "galladuchi_q0043_0401.htm";
			}
			return "galladuchi_q0043_0401.htm";
		} else if ("7".equalsIgnoreCase(event) || "30829-07.htm".equalsIgnoreCase(event)) {
			qs.giveItems(PET_TICKET, 1);
			qs.giveItems(3438, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "pet_manager_cooper_q0043_0501.htm";
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

		switch (npcId) {
			case COOPER:
				if (cond == 0) {
					if (pc.level() >= 26) return "pet_manager_cooper_q0043_0101.htm";
					qs.exitQuest(true);
					return "pet_manager_cooper_q0043_0103.htm";
				} else if (cond == 1) {
					return qs.hasQuestItems(CRAFTED_DAGGER) ? "pet_manager_cooper_q0043_0105.htm" : "pet_manager_cooper_q0043_0106.htm";
				} else if (cond == 2) {
					return "pet_manager_cooper_q0043_0204.htm";
				} else if (cond == 3 && qs.getQuestItemsCount(MAP_PIECE) >= 30) {
					return "pet_manager_cooper_q0043_0203.htm";
				} else if (cond == 4) {
					return "pet_manager_cooper_q0043_0303.htm";
				} else if (cond == 5) {
					return "pet_manager_cooper_q0043_0401.htm";
				}
				break;
			case GALLADUCCI:
				if (cond == 4 && qs.hasQuestItems(MAP)) {
					return "galladuchi_q0043_0301.htm";
				} else if (cond == 5) {
					return "galladuchi_q0043_0401.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 2) {
			long count = qs.getQuestItemsCount(MAP_PIECE);
			if (count < 30) {
				qs.giveItems(MAP_PIECE, 1);
				if (count + 1 >= 30) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
