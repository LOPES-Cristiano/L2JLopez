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
 * Quest 101: Sword of Solidarity
 */
@Component
public class Quest101SwordOfSolidarity extends Quest {

	public static final int QUEST_ID = 101;
	public static final String QUEST_NAME = "101_SwordOfSolidarity";
	public static final int START_NPC = 30008;
	public static final int ROIEN = 30008;
	public static final int ALTRAN = 30283;

	public static final int ROIENS_LETTER = 796;
	public static final int HOWTOGO_RUINS = 937;
	public static final int BROKEN_SWORD_HANDLE = 739;
	public static final int BROKEN_BLADE_BOTTOM = 740;
	public static final int BROKEN_BLADE_TOP = 741;
	public static final int ALLTRANS_NOTE = 742;
	public static final int SWORD_OF_SOLIDARITY = 738;

	public static final int TUNATH_ORC_MARKSMAN = 20361;
	public static final int TUNATH_ORC_WARRIOR = 20362;

	@Autowired
	public Quest101SwordOfSolidarity(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Sword of Solidarity");
		addStartNpc(START_NPC);
		addTalkId(ROIEN, ALTRAN);
		addKillId(TUNATH_ORC_MARKSMAN, TUNATH_ORC_WARRIOR);
		registerQuestItems(BROKEN_SWORD_HANDLE, BROKEN_BLADE_BOTTOM, BROKEN_BLADE_TOP, ALLTRANS_NOTE, ROIENS_LETTER, HOWTOGO_RUINS);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		if ("quest_accept".equalsIgnoreCase(event)
				|| "roien_q0101_04.htm".equalsIgnoreCase(event)
				|| "30008-03.htm".equalsIgnoreCase(event)
				|| "30008-04.htm".equalsIgnoreCase(event)
				|| "roiens_letter".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(ROIENS_LETTER)) {
				qs.giveItems(ROIENS_LETTER, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "roien_q0101_04.htm";
		} else if ("blacksmith_alltran_q0101_02.htm".equalsIgnoreCase(event)
				|| "30283-02.htm".equalsIgnoreCase(event)
				|| "ruins_info".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.takeItems(ROIENS_LETTER, -1);
			if (!qs.hasQuestItems(HOWTOGO_RUINS)) {
				qs.giveItems(HOWTOGO_RUINS, 1);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "blacksmith_alltran_q0101_02.htm";
		} else if ("blacksmith_alltran_q0101_07.htm".equalsIgnoreCase(event)
				|| "30283-06.htm".equalsIgnoreCase(event)
				|| "30283-07.htm".equalsIgnoreCase(event)
				|| "sword_of_solidarity".equalsIgnoreCase(event)) {
			qs.takeItems(BROKEN_SWORD_HANDLE, -1);
			qs.giveItems(SWORD_OF_SOLIDARITY, 1);
			qs.giveItems(1060, 100);
			for (int echo = 4412; echo <= 4416; echo++) {
				qs.giveItems(echo, 10);
			}
			qs.giveItems(5789, 7000);
			qs.addExpAndSp(25747, 2171);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "blacksmith_alltran_q0101_07.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		if (qs.isCompleted()) {
			return "completed";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == ROIEN) {
			if (cond == 0) {
				if (pc.race() != 0) {
					qs.exitQuest(true);
					return "roien_q0101_00.htm";
				}
				if (pc.level() >= 9) {
					return "roien_q0101_02.htm";
				}
				qs.exitQuest(true);
				return "roien_q0101_08.htm";
			} else if (cond == 1) {
				return "roien_q0101_05.htm";
			} else if (cond == 2 || cond == 3) {
				if (qs.hasQuestItems(BROKEN_BLADE_TOP) && qs.hasQuestItems(BROKEN_BLADE_BOTTOM)) {
					return "roien_q0101_12.htm";
				}
				if (qs.getQuestItemsCount(BROKEN_BLADE_TOP) + qs.getQuestItemsCount(BROKEN_BLADE_BOTTOM) == 1) {
					return "roien_q0101_11.htm";
				}
				return "roien_q0101_10.htm";
			} else if (cond == 4) {
				if (qs.hasQuestItems(798)) {
					qs.takeItems(798, -1);
					qs.takeItems(ALLTRANS_NOTE, -1);
					qs.giveItems(SWORD_OF_SOLIDARITY, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "roien_q0101_07.htm";
				}
				if (qs.hasQuestItems(ALLTRANS_NOTE)) {
					qs.takeItems(ALLTRANS_NOTE, -1);
					qs.giveItems(BROKEN_SWORD_HANDLE, 1);
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "roien_q0101_06.htm";
				}
			} else if (cond == 5) {
				return "roien_q0101_07.htm";
			}
		} else if (npcId == ALTRAN) {
			if (cond == 1 && qs.hasQuestItems(ROIENS_LETTER)) {
				qs.takeItems(ROIENS_LETTER, -1);
				qs.giveItems(HOWTOGO_RUINS, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_alltran_q0101_02.htm";
			} else if (cond == 2) {
				if (qs.getQuestItemsCount(BROKEN_BLADE_TOP) + qs.getQuestItemsCount(BROKEN_BLADE_BOTTOM) == 1) {
					return "blacksmith_alltran_q0101_08.htm";
				}
				return "blacksmith_alltran_q0101_03.htm";
			} else if (cond == 3) {
				if (qs.hasQuestItems(BROKEN_BLADE_TOP) && qs.hasQuestItems(BROKEN_BLADE_BOTTOM)) {
					qs.takeItems(HOWTOGO_RUINS, -1);
					qs.takeItems(BROKEN_BLADE_TOP, -1);
					qs.takeItems(BROKEN_BLADE_BOTTOM, -1);
					qs.giveItems(ALLTRANS_NOTE, 1);
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "blacksmith_alltran_q0101_04.htm";
				}
				qs.takeItems(HOWTOGO_RUINS, -1);
				qs.giveItems(ALLTRANS_NOTE, 1);
				qs.giveItems(798, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_alltran_q0101_05.htm";
			} else if (cond == 4) {
				return "blacksmith_alltran_q0101_05.htm";
			} else if (cond == 5 && qs.hasQuestItems(BROKEN_SWORD_HANDLE)) {
				return "blacksmith_alltran_q0101_06.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		int npcId = npc.getNpcId();
		if ((npcId == TUNATH_ORC_MARKSMAN || npcId == TUNATH_ORC_WARRIOR) && qs.getCond() == 2) {
			if (!qs.hasQuestItems(BROKEN_BLADE_TOP)) {
				qs.rollAndGive(BROKEN_BLADE_TOP, 1, 60.0);
			} else if (!qs.hasQuestItems(BROKEN_BLADE_BOTTOM)) {
				qs.rollAndGive(BROKEN_BLADE_BOTTOM, 1, 60.0);
			}
			if (qs.hasQuestItems(BROKEN_BLADE_TOP) && qs.hasQuestItems(BROKEN_BLADE_BOTTOM)) {
				qs.setCond(3);
			}
		}
		return null;
	}
}
