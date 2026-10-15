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
 * Quest 104: Spirit of Mirror
 */
@Component
public class Quest104SpiritOfMirror extends Quest {

	public static final int QUEST_ID = 104;
	public static final String QUEST_NAME = "104_SpiritOfMirror";
	public static final int START_NPC = 30017;

	public static final int GALLINT = 30017;
	public static final int ARNOLD = 30041;
	public static final int JOHNSTONE = 30043;
	public static final int KENYOS = 30045;

	public static final int GALLINS_OAK_WAND = 748;
	public static final int WAND_SPIRITBOUND_1 = 1135;
	public static final int WAND_SPIRITBOUND_2 = 1136;
	public static final int WAND_SPIRITBOUND_3 = 1137;
	public static final int WAND_OF_ADEPT = 747;

	@Autowired
	public Quest104SpiritOfMirror(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Spirit of Mirror");
		addStartNpc(START_NPC);
		addTalkId(GALLINT, ARNOLD, JOHNSTONE, KENYOS);
		addKillId(27003, 27004, 27005);
		registerQuestItems(GALLINS_OAK_WAND, WAND_SPIRITBOUND_1, WAND_SPIRITBOUND_2, WAND_SPIRITBOUND_3);
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
				|| "30017-03.htm".equalsIgnoreCase(event)
				|| "gallin_q0104_03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(GALLINS_OAK_WAND)) {
				qs.giveItems(GALLINS_OAK_WAND, 3);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "gallin_q0104_03.htm";
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

		if (npcId == GALLINT) {
			if (cond == 0) {
				if (pc.race() != 0) {
					qs.exitQuest(true);
					return "gallin_q0104_00.htm";
				}
				if (pc.level() >= 10) {
					return "gallin_q0104_02.htm";
				}
				qs.exitQuest(true);
				return "gallin_q0104_06.htm";
			} else if (cond == 1) {
				return "gallin_q0104_04.htm";
			} else if (cond == 2) {
				qs.giveItems(GALLINS_OAK_WAND, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gallin_q0104_06.htm";
			} else if (cond == 3) {
				qs.takeAllItems(GALLINS_OAK_WAND, WAND_SPIRITBOUND_1, WAND_SPIRITBOUND_2, WAND_SPIRITBOUND_3);
				qs.giveItems(WAND_OF_ADEPT, 1);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "gallin_q0104_05.htm";
			}
		} else if (npcId == ARNOLD || npcId == JOHNSTONE || npcId == KENYOS) {
			if (cond == 1) {
				qs.set("guard", 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			if (npcId == ARNOLD) {
				return "arnold_q0104_01.htm";
			} else if (npcId == JOHNSTONE) {
				return "johnson_q0104_01.htm";
			} else {
				return "ken_q0104_01.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		int cond = qs.getCond();
		if (cond != 1 && cond != 2) {
			return null;
		}
		if (qs.getItemEquipped(7) != GALLINS_OAK_WAND && !qs.hasQuestItems(GALLINS_OAK_WAND)) {
			return null;
		}
		int npcId = npc.getNpcId();
		int wandId = 0;
		if (npcId == 27003 && !qs.hasQuestItems(WAND_SPIRITBOUND_1)) {
			wandId = WAND_SPIRITBOUND_1;
		} else if (npcId == 27004 && !qs.hasQuestItems(WAND_SPIRITBOUND_2)) {
			wandId = WAND_SPIRITBOUND_2;
		} else if (npcId == 27005 && !qs.hasQuestItems(WAND_SPIRITBOUND_3)) {
			wandId = WAND_SPIRITBOUND_3;
		}
		if (wandId != 0) {
			qs.takeItems(GALLINS_OAK_WAND, 1);
			qs.giveItems(wandId, 1);
			if (qs.hasQuestItems(WAND_SPIRITBOUND_1)
					&& qs.hasQuestItems(WAND_SPIRITBOUND_2)
					&& qs.hasQuestItems(WAND_SPIRITBOUND_3)) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
