package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 105: Skirmish with Orcs
 */
@Component
public class Quest105SkirmishWithOrcs extends Quest {

	public static final int QUEST_ID = 105;
	public static final String QUEST_NAME = "105_SkirmishWithOrcs";
	public static final int START_NPC = 30218;

	public static final int KENDELL = 30218;

	public static final int KENDELL_ORDER_1 = 1836;
	public static final int KENDELL_ORDER_2 = 1837;
	public static final int KENDELL_ORDER_3 = 1838;
	public static final int KENDELL_ORDER_4 = 1839;
	public static final int KENDELL_ORDER_5 = 1840;
	public static final int KENDELL_ORDER_6 = 1841;
	public static final int KENDELL_ORDER_7 = 1842;
	public static final int KENDELL_ORDER_8 = 1843;
	public static final int KABOO_CHIEF_TORC_1 = 1844;
	public static final int KABOO_CHIEF_TORC_2 = 1845;

	public static final int RED_SUNSET_STAFF = 754;
	public static final int RED_SUNSET_SWORD = 981;

	@Autowired
	public Quest105SkirmishWithOrcs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Skirmish with Orcs");
		addStartNpc(START_NPC);
		addTalkId(KENDELL);
		addKillId(27059, 27060, 27061, 27062, 27064, 27065, 27067, 27068);
		registerQuestItems(KENDELL_ORDER_1, KENDELL_ORDER_2, KENDELL_ORDER_3, KENDELL_ORDER_4,
				KENDELL_ORDER_5, KENDELL_ORDER_6, KENDELL_ORDER_7, KENDELL_ORDER_8,
				KABOO_CHIEF_TORC_1, KABOO_CHIEF_TORC_2);
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
				|| "30218-03.htm".equalsIgnoreCase(event)
				|| "sentinel_kendnell_q0105_03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (qs.getQuestItemsCount(KENDELL_ORDER_1, KENDELL_ORDER_2, KENDELL_ORDER_3, KENDELL_ORDER_4) == 0) {
				qs.giveItems(KENDELL_ORDER_1 + ThreadLocalRandom.current().nextInt(4), 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "sentinel_kendnell_q0105_03.htm";
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

		if (npcId == KENDELL) {
			if (cond == 0) {
				if (pc.race() != 1) {
					qs.exitQuest(true);
					return "sentinel_kendnell_q0105_00.htm";
				}
				if (pc.level() >= 10) {
					return "sentinel_kendnell_q0105_02.htm";
				}
				qs.exitQuest(true);
				return "sentinel_kendnell_q0105_10.htm";
			} else if (cond == 1) {
				return "sentinel_kendnell_q0105_04.htm";
			} else if (cond == 2) {
				if (!qs.hasQuestItems(KABOO_CHIEF_TORC_1)) {
					qs.giveItems(RED_SUNSET_STAFF, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "sentinel_kendnell_q0105_10.htm";
				}
				qs.takeAllItems(KENDELL_ORDER_1, KENDELL_ORDER_2, KENDELL_ORDER_3, KENDELL_ORDER_4, KABOO_CHIEF_TORC_1);
				qs.giveItems(KENDELL_ORDER_5 + ThreadLocalRandom.current().nextInt(4), 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "sentinel_kendnell_q0105_06.htm";
			} else if (cond == 3) {
				return "sentinel_kendnell_q0105_07.htm";
			} else if (cond == 4) {
				qs.takeAllItems(KENDELL_ORDER_5, KENDELL_ORDER_6, KENDELL_ORDER_7, KENDELL_ORDER_8, KABOO_CHIEF_TORC_2);
				qs.giveItems(RED_SUNSET_STAFF, 1);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "sentinel_kendnell_q0105_08.htm";
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
		int cond = qs.getCond();
		if (cond == 1 && !qs.hasQuestItems(KABOO_CHIEF_TORC_1)) {
			boolean match = (npcId == 27059 && qs.hasQuestItems(KENDELL_ORDER_1))
					|| (npcId == 27060 && qs.hasQuestItems(KENDELL_ORDER_2))
					|| (npcId == 27061 && qs.hasQuestItems(KENDELL_ORDER_3))
					|| (npcId == 27062 && qs.hasQuestItems(KENDELL_ORDER_4))
					|| ((npcId >= 27059 && npcId <= 27062)
							&& qs.getQuestItemsCount(KENDELL_ORDER_1, KENDELL_ORDER_2, KENDELL_ORDER_3, KENDELL_ORDER_4) == 0);
			if (match) {
				qs.giveItems(KABOO_CHIEF_TORC_1, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if (cond == 3 && !qs.hasQuestItems(KABOO_CHIEF_TORC_2)) {
			boolean match = (npcId == 27064 && qs.hasQuestItems(KENDELL_ORDER_5))
					|| (npcId == 27065 && qs.hasQuestItems(KENDELL_ORDER_6))
					|| (npcId == 27067 && qs.hasQuestItems(KENDELL_ORDER_7))
					|| (npcId == 27068 && qs.hasQuestItems(KENDELL_ORDER_8))
					|| ((npcId == 27064 || npcId == 27065 || npcId == 27067 || npcId == 27068)
							&& qs.getQuestItemsCount(KENDELL_ORDER_5, KENDELL_ORDER_6, KENDELL_ORDER_7, KENDELL_ORDER_8) == 0);
			if (match) {
				qs.giveItems(KABOO_CHIEF_TORC_2, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
