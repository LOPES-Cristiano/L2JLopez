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
 * Quest 107: Merciless Punishment
 */
@Component
public class Quest107MercilessPunishment extends Quest {

	public static final int QUEST_ID = 107;
	public static final String QUEST_NAME = "107_MercilessPunishment";
	public static final int START_NPC = 30568;

	public static final int HATOS = 30568;
	public static final int PARUGON = 30580;

	public static final int HATOS_ORDER_1 = 1553;
	public static final int HATOS_ORDER_2 = 1554;
	public static final int HATOS_ORDER_3 = 1555;
	public static final int LETTER_TO_DARKELF = 1556;
	public static final int LETTER_TO_HUMAN = 1557;
	public static final int LETTER_TO_ELF = 1558;

	public static final int BUTCHER_SWORD_REWARD = 758;
	public static final int BUTCHER_SWORD = 1510;

	@Autowired
	public Quest107MercilessPunishment(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Merciless Punishment");
		addStartNpc(START_NPC);
		addTalkId(HATOS, PARUGON);
		addKillId(27041);
		registerQuestItems(HATOS_ORDER_1, HATOS_ORDER_2, HATOS_ORDER_3,
				LETTER_TO_DARKELF, LETTER_TO_HUMAN, LETTER_TO_ELF);
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
				|| "30568-03.htm".equalsIgnoreCase(event)
				|| "urutu_chief_hatos_q0107_03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(HATOS_ORDER_1)) {
				qs.giveItems(HATOS_ORDER_1, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "urutu_chief_hatos_q0107_03.htm";
		} else if ("30568-06.htm".equalsIgnoreCase(event)
				|| "urutu_chief_hatos_q0107_06.htm".equalsIgnoreCase(event)) {
			qs.takeAllItems(HATOS_ORDER_1, HATOS_ORDER_2, HATOS_ORDER_3,
					LETTER_TO_DARKELF, LETTER_TO_HUMAN, LETTER_TO_ELF);
			qs.giveItems(57, 200);
			qs.playSound("ItemSound.quest_giveup");
			qs.exitQuest(true);
			return event;
		} else if ("30568-07.htm".equalsIgnoreCase(event)
				|| "urutu_chief_hatos_q0107_07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(HATOS_ORDER_1, -1);
			if (!qs.hasQuestItems(HATOS_ORDER_2)) {
				qs.giveItems(HATOS_ORDER_2, 1);
			}
			qs.setCond(4);
			return event;
		} else if ("30568-09.htm".equalsIgnoreCase(event)
				|| "urutu_chief_hatos_q0107_09.htm".equalsIgnoreCase(event)) {
			qs.takeItems(HATOS_ORDER_2, -1);
			if (!qs.hasQuestItems(HATOS_ORDER_3)) {
				qs.giveItems(HATOS_ORDER_3, 1);
			}
			qs.setCond(6);
			return event;
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

		if (npcId == HATOS) {
			if (cond == 0) {
				if (pc.race() != 3) {
					qs.exitQuest(true);
					return "urutu_chief_hatos_q0107_00.htm";
				}
				if (pc.level() >= 10) {
					return "urutu_chief_hatos_q0107_02.htm";
				}
				qs.exitQuest(true);
				return "urutu_chief_hatos_q0107_01.htm";
			} else if (cond == 1) {
				return "urutu_chief_hatos_q0107_06.htm";
			} else if (cond == 2) {
				qs.takeItems(HATOS_ORDER_1, -1);
				qs.giveItems(BUTCHER_SWORD_REWARD, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "urutu_chief_hatos_q0107_09.htm";
			} else if (cond == 3 || cond == 4) {
				return "urutu_chief_hatos_q0107_05.htm";
			} else if (cond == 5 || cond == 6) {
				return "urutu_chief_hatos_q0107_08.htm";
			} else if (cond == 7) {
				qs.takeAllItems(HATOS_ORDER_1, HATOS_ORDER_2, HATOS_ORDER_3,
						LETTER_TO_DARKELF, LETTER_TO_HUMAN, LETTER_TO_ELF);
				qs.giveItems(BUTCHER_SWORD_REWARD, 1);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "urutu_chief_hatos_q0107_10.htm";
			}
		} else if (npcId == PARUGON && cond >= 1) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "centurion_parugon_q0107_01.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		if (npc.getNpcId() == 27041) {
			int cond = qs.getCond();
			if (cond == 2) {
				qs.giveItems(LETTER_TO_HUMAN, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (cond == 4) {
				qs.giveItems(LETTER_TO_DARKELF, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else if (cond == 6) {
				qs.giveItems(LETTER_TO_ELF, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
