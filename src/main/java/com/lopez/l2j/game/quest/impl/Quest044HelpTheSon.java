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
 * Quest 44: Help the Son!
 * Pet Manager Lundy e High Prefect Drikus em Gludio.
 */
@Component
public class Quest044HelpTheSon extends Quest {

	public static final int QUEST_ID = 44;
	public static final String QUEST_NAME = "044_HelptheSon";

	public static final int START_NPC = 30827;
	public static final int LUNDY = 30827;
	public static final int DRIKUS = 30505;

	public static final int MAILLE_LIZARDMAN = 20919;
	public static final int MAILLE_LIZARDMAN_SCOUT = 20920;
	public static final int MAILLE_LIZARDMAN_GUARD = 20921;

	public static final int WORK_HAMMER = 168;
	public static final int GEMSTONE_FRAGMENT = 7552;
	public static final int GEMSTONE = 7553;
	public static final int PET_TICKET = 7585;

	@Autowired
	public Quest044HelpTheSon(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Help the Son!");
		addStartNpc(LUNDY);
		addTalkId(LUNDY, DRIKUS);
		addKillId(MAILLE_LIZARDMAN, MAILLE_LIZARDMAN_SCOUT, MAILLE_LIZARDMAN_GUARD);
		registerQuestItems(GEMSTONE_FRAGMENT, GEMSTONE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event) || "30827-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "pet_manager_lundy_q0044_0104.htm";
		} else if ("3".equalsIgnoreCase(event) || "30827-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(WORK_HAMMER)) {
				qs.setCond(2);
				qs.takeItems(WORK_HAMMER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_lundy_q0044_0201.htm";
			}
			return "pet_manager_lundy_q0044_0204.htm";
		} else if ("4".equalsIgnoreCase(event) || "30827-05.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(GEMSTONE_FRAGMENT) >= 30) {
				qs.setCond(4);
				qs.takeItems(GEMSTONE_FRAGMENT, 30);
				qs.giveItems(GEMSTONE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "pet_manager_lundy_q0044_0301.htm";
			}
			return "pet_manager_lundy_q0044_0303.htm";
		} else if ("5".equalsIgnoreCase(event) || "30505-06.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(GEMSTONE)) {
				qs.setCond(5);
				qs.takeItems(GEMSTONE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event.endsWith(".htm") ? event : "high_prefect_drikus_q0044_0401.htm";
			}
			return "high_prefect_drikus_q0044_0403.htm";
		} else if ("7".equalsIgnoreCase(event) || "30827-07.htm".equalsIgnoreCase(event)) {
			qs.giveItems(PET_TICKET, 1);
			qs.giveItems(3438, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "pet_manager_lundy_q0044_0501.htm";
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
			case LUNDY:
				if (cond == 0) {
					if (pc.level() >= 24) return "pet_manager_lundy_q0044_0101.htm";
					qs.exitQuest(true);
					return "pet_manager_lundy_q0044_0103.htm";
				} else if (cond == 1) {
					return qs.hasQuestItems(WORK_HAMMER) ? "pet_manager_lundy_q0044_0105.htm" : "pet_manager_lundy_q0044_0106.htm";
				} else if (cond == 2) {
					return "pet_manager_lundy_q0044_0204.htm";
				} else if (cond == 3 && qs.getQuestItemsCount(GEMSTONE_FRAGMENT) >= 30) {
					return "pet_manager_lundy_q0044_0203.htm";
				} else if (cond == 4) {
					return "pet_manager_lundy_q0044_0303.htm";
				} else if (cond == 5) {
					return "pet_manager_lundy_q0044_0401.htm";
				}
				break;
			case DRIKUS:
				if (cond == 4 && qs.hasQuestItems(GEMSTONE)) {
					return "high_prefect_drikus_q0044_0301.htm";
				} else if (cond == 5) {
					return "high_prefect_drikus_q0044_0403.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 2) {
			long count = qs.getQuestItemsCount(GEMSTONE_FRAGMENT);
			if (count < 30) {
				qs.giveItems(GEMSTONE_FRAGMENT, 1);
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
