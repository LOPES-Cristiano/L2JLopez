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
 * Quest 407: Path to Elven Scout (1ª Troca de Classe do Elven Fighter para Elven Scout).
 */
@Component
public class Quest407PathToElvenScout extends Quest {

	public static final int QUEST_ID = 407;
	public static final String QUEST_NAME = "407_PathToElvenScout";

	// NPCs
	public static final int REISA = 30328;
	public static final int MORETTI = 30337;
	public static final int PIPPEN = 30426;

	// Monstros
	public static final int OL_MAHUM_PATROL = 20053;
	public static final int OL_MAHUM_SENTRY = 20058;

	// Itens
	public static final int REORIA_LETTER2 = 1207;
	public static final int PRIGUNS_TEAR_LETTER1 = 1208;
	public static final int PRIGUNS_TEAR_LETTER2 = 1209;
	public static final int PRIGUNS_TEAR_LETTER3 = 1210;
	public static final int PRIGUNS_TEAR_LETTER4 = 1211;
	public static final int MORETTIS_HERB = 1212;
	public static final int MORETTIS_LETTER = 1214;
	public static final int PRIGUNS_LETTER = 1215;
	public static final int HONORARY_GUARD = 1216;
	public static final int REORIA_RECOMMENDATION = 1217;
	public static final int RUSTED_KEY = 1293;

	@Autowired
	public Quest407PathToElvenScout(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Elven Scout");

		addStartNpc(REISA);
		addTalkId(REISA);
		addTalkId(MORETTI);
		addTalkId(PIPPEN);

		addKillId(OL_MAHUM_PATROL);
		addKillId(OL_MAHUM_SENTRY);

		registerQuestItems(REORIA_LETTER2, PRIGUNS_TEAR_LETTER1, PRIGUNS_TEAR_LETTER2,
				PRIGUNS_TEAR_LETTER3, PRIGUNS_TEAR_LETTER4, MORETTIS_HERB,
				MORETTIS_LETTER, PRIGUNS_LETTER, HONORARY_GUARD, RUSTED_KEY);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int level = player != null ? player.getLevel() : 0;
		int classId = player != null ? player.getClassId() : -1;

		if ("1".equalsIgnoreCase(event)) {
			if (classId == 0x12) {
				if (level >= 18) {
					if (qs.getQuestItemsCount(REORIA_RECOMMENDATION) > 0) {
						return "30328-04.htm";
					} else {
						qs.giveItems(REORIA_LETTER2, 1);
						qs.setCond(1);
						qs.setState(State.STARTED);
						qs.playSound("ItemSound.quest_accept");
						return "30328-05.htm";
					}
				} else {
					return "30328-03.htm";
				}
			} else {
				return classId == 0x16 ? "30328-02a.htm" : "30328-02.htm";
			}
		} else if ("30337_1".equalsIgnoreCase(event)) {
			qs.takeItems(REORIA_LETTER2, 1);
			qs.setCond(2);
			qs.playSound("ItemSound.quest_middle");
			return "30337-03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == REISA) {
			if (cond == 0) {
				return "30328-01.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(HONORARY_GUARD) == 1) {
					qs.rewardItems(57, 81900);
					qs.takeItems(HONORARY_GUARD, 1);
					qs.giveItems(REORIA_RECOMMENDATION, 1);
					qs.addExpAndSp(295862, 17964);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30328-09.htm";
				} else if (qs.getQuestItemsCount(REORIA_LETTER2) > 0) {
					return "30328-06.htm";
				} else {
					return "30328-08.htm";
				}
			}
		} else if (npcId == MORETTI && cond > 0) {
			if (qs.getQuestItemsCount(REORIA_LETTER2) > 0) {
				return "30337-01.htm";
			} else if (qs.getQuestItemsCount(MORETTIS_LETTER) == 0 && qs.getQuestItemsCount(PRIGUNS_LETTER) == 0 && qs.getQuestItemsCount(HONORARY_GUARD) == 0) {
				long pieces = qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER1) + qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER2)
						+ qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER3) + qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER4);
				if (pieces == 0) {
					return "30337-04.htm";
				} else if (pieces < 4) {
					return "30337-05.htm";
				} else {
					qs.takeItems(PRIGUNS_TEAR_LETTER1, -1);
					qs.takeItems(PRIGUNS_TEAR_LETTER2, -1);
					qs.takeItems(PRIGUNS_TEAR_LETTER3, -1);
					qs.takeItems(PRIGUNS_TEAR_LETTER4, -1);
					qs.giveItems(MORETTIS_HERB, 1);
					qs.giveItems(MORETTIS_LETTER, 1);
					qs.setCond(4);
					qs.playSound("ItemSound.quest_middle");
					return "30337-06.htm";
				}
			} else if (qs.getQuestItemsCount(PRIGUNS_LETTER) == 1) {
				qs.takeItems(PRIGUNS_LETTER, 1);
				qs.giveItems(HONORARY_GUARD, 1);
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				return "30337-08.htm";
			} else if (qs.getQuestItemsCount(HONORARY_GUARD) == 1) {
				return "30337-09.htm";
			}
		} else if (npcId == PIPPEN && cond > 0 && qs.getQuestItemsCount(MORETTIS_LETTER) > 0 && qs.getQuestItemsCount(MORETTIS_HERB) > 0) {
			if (qs.getQuestItemsCount(RUSTED_KEY) < 1) {
				qs.setCond(5);
				qs.playSound("ItemSound.quest_middle");
				return "30426-01.htm";
			} else {
				qs.takeItems(RUSTED_KEY, 1);
				qs.takeItems(MORETTIS_HERB, 1);
				qs.takeItems(MORETTIS_LETTER, 1);
				qs.giveItems(PRIGUNS_LETTER, 1);
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				return "30426-02.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == OL_MAHUM_PATROL && cond == 2) {
			if (qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER1) == 0) {
				qs.giveItems(PRIGUNS_TEAR_LETTER1, 1);
			} else if (qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER2) == 0) {
				qs.giveItems(PRIGUNS_TEAR_LETTER2, 1);
			} else if (qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER3) == 0) {
				qs.giveItems(PRIGUNS_TEAR_LETTER3, 1);
			} else if (qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER4) == 0) {
				qs.giveItems(PRIGUNS_TEAR_LETTER4, 1);
			}

			long pieces = qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER1) + qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER2)
					+ qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER3) + qs.getQuestItemsCount(PRIGUNS_TEAR_LETTER4);
			if (pieces >= 4) {
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(3);
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		} else if (npcId == OL_MAHUM_SENTRY && cond == 5 && qs.getQuestItemsCount(RUSTED_KEY) == 0) {
			qs.giveItems(RUSTED_KEY, 1);
			qs.playSound("ItemSound.quest_middle");
		}
		return null;
	}
}
