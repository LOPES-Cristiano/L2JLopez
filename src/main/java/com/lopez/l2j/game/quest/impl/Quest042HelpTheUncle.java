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
 * Quest 42: Help the Uncle!
 * Pet Manager Waters e Chief Mercenary Sophia em Dion.
 */
@Component
public class Quest042HelpTheUncle extends Quest {

	public static final int QUEST_ID = 42;
	public static final String QUEST_NAME = "042_HelptheUncle";

	public static final int START_NPC = 30828;
	public static final int WATERS = 30828;
	public static final int SOPHIA = 30735;

	public static final int MONSTER_EYE_DESTROYER = 20068;
	public static final int MONSTER_EYE_GAZER = 20266;

	public static final int TRIDENT = 291;
	public static final int MAP_PIECE = 7548;
	public static final int MAP = 7549;
	public static final int PET_TICKET = 7583;

	@Autowired
	public Quest042HelpTheUncle(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Help the Uncle!");
		addStartNpc(WATERS);
		addTalkId(WATERS, SOPHIA);
		addKillId(MONSTER_EYE_DESTROYER, MONSTER_EYE_GAZER);
		registerQuestItems(MAP_PIECE, MAP);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event) || "30828-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "pet_manager_waters_q0042_0104.htm";
		} else if ("3".equalsIgnoreCase(event) || "30828-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TRIDENT)) {
				qs.setCond(2);
				qs.takeItems(TRIDENT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_waters_q0042_0201.htm";
			}
			return "pet_manager_waters_q0042_0204.htm";
		} else if ("4".equalsIgnoreCase(event) || "30828-05.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(MAP_PIECE) >= 30) {
				qs.setCond(4);
				qs.takeItems(MAP_PIECE, 30);
				qs.giveItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_waters_q0042_0301.htm";
			}
			return "pet_manager_waters_q0042_0303.htm";
		} else if ("5".equalsIgnoreCase(event) || "30735-06.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MAP)) {
				qs.setCond(5);
				qs.takeItems(MAP, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "sophia_q0042_0401.htm";
			}
			return "sophia_q0042_0402.htm";
		} else if ("7".equalsIgnoreCase(event) || "30828-07.htm".equalsIgnoreCase(event)) {
			qs.giveItems(PET_TICKET, 1);
			qs.giveItems(3438, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "pet_manager_waters_q0042_0501.htm";
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
			case WATERS:
				if (cond == 0) {
					if (pc.level() >= 25) return "pet_manager_waters_q0042_0101.htm";
					qs.exitQuest(true);
					return "pet_manager_waters_q0042_0103.htm";
				} else if (cond == 1) {
					if (qs.hasQuestItems(TRIDENT)) {
						return "pet_manager_waters_q0042_0105.htm";
					}
					qs.giveItems(3438, 1);
					qs.giveItems(PET_TICKET, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "pet_manager_waters_q0042_0301.htm";
				} else if (cond == 2) {
					return "pet_manager_waters_q0042_0204.htm";
				} else if (cond == 3 && qs.getQuestItemsCount(MAP_PIECE) >= 30) {
					return "pet_manager_waters_q0042_0203.htm";
				} else if (cond == 4) {
					return "pet_manager_waters_q0042_0303.htm";
				} else if (cond == 5) {
					return "pet_manager_waters_q0042_0401.htm";
				}
				break;
			case SOPHIA:
				if (cond == 4 && qs.hasQuestItems(MAP)) {
					return "sophia_q0042_0301.htm";
				} else if (cond == 5) {
					return "sophia_q0042_0402.htm";
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
