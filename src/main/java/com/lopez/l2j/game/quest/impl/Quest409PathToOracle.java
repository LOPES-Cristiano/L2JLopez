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
 * Quest 409: Path to Elven Oracle (1ª Troca de Classe do Elven Mystic para Elven Oracle).
 */
@Component
public class Quest409PathToOracle extends Quest {

	public static final int QUEST_ID = 409;
	public static final String QUEST_NAME = "409_PathToOracle";

	// NPCs
	public static final int MANUEL = 30293;
	public static final int ALLANA = 30424;
	public static final int PERRIN = 30428;

	// Monstros
	public static final int LIZARDMAN_WARRIOR = 27032;
	public static final int LIZARDMAN_SCOUT = 27033;
	public static final int LIZARDMAN = 27034;
	public static final int TAMIL = 27035;

	// Itens
	public static final int CRYSTAL_MEDALLION = 1231;
	public static final int MONEY_OF_SWINDLER = 1232;
	public static final int DAIRY_OF_ALLANA = 1233;
	public static final int LIZARD_CAPTAIN_ORDER = 1234;
	public static final int LEAF_OF_ORACLE = 1235;
	public static final int HALF_OF_DAIRY = 1236;
	public static final int TAMATOS_NECKLACE = 1275;

	@Autowired
	public Quest409PathToOracle(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Elven Oracle");

		addStartNpc(MANUEL);
		addTalkId(MANUEL);
		addTalkId(ALLANA);
		addTalkId(PERRIN);

		addKillId(LIZARDMAN_WARRIOR);
		addKillId(LIZARDMAN_SCOUT);
		addKillId(LIZARDMAN);
		addKillId(TAMIL);

		registerQuestItems(CRYSTAL_MEDALLION, MONEY_OF_SWINDLER, DAIRY_OF_ALLANA,
				LIZARD_CAPTAIN_ORDER, HALF_OF_DAIRY, TAMATOS_NECKLACE);

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
			if (level >= 18 && classId == 0x19 && qs.getQuestItemsCount(LEAF_OF_ORACLE) == 0) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				qs.giveItems(CRYSTAL_MEDALLION, 1);
				return "30293-05.htm";
			} else if (classId != 0x19) {
				return classId == 0x1d ? "30293-02a.htm" : "30293-02.htm";
			} else if (level < 18 && classId == 0x19) {
				return "30293-03.htm";
			} else if (qs.getQuestItemsCount(LEAF_OF_ORACLE) > 0) {
				return "30293-04.htm";
			}
		} else if ("30424-08.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() > 0) {
				qs.setCond(2);
			}
			return event;
		} else if ("30424_1".equalsIgnoreCase(event)) {
			return "";
		} else if ("30428_1".equalsIgnoreCase(event)) {
			if (qs.getCond() > 0) {
				return "30428-02.htm";
			}
		} else if ("30428_2".equalsIgnoreCase(event)) {
			if (qs.getCond() > 0) {
				return "30428-03.htm";
			}
		} else if ("30428_3".equalsIgnoreCase(event)) {
			if (qs.getCond() > 0) {
				return "30428-04.htm";
			}
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

		if (npcId == MANUEL) {
			if (cond == 0) {
				if (qs.getQuestItemsCount(LEAF_OF_ORACLE) == 0) {
					return "30293-01.htm";
				} else {
					return "30293-04.htm";
				}
			} else if (cond > 0 && qs.getQuestItemsCount(CRYSTAL_MEDALLION) > 0) {
				if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 0
						&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 0
						&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 0
						&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 0) {
					return cond > 0 ? "30293-09.htm" : "30293-06.htm";
				} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 1
						&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 1
						&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 1
						&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 0) {
					qs.takeItems(MONEY_OF_SWINDLER, 1);
					qs.takeItems(DAIRY_OF_ALLANA, 1);
					qs.takeItems(LIZARD_CAPTAIN_ORDER, 1);
					qs.takeItems(CRYSTAL_MEDALLION, 1);
					qs.rewardItems(57, 81900);
					qs.giveItems(LEAF_OF_ORACLE, 1);
					qs.addExpAndSp(295862, 16894);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30293-08.htm";
				} else {
					return "30293-07.htm";
				}
			}
		} else if (npcId == ALLANA && cond > 0 && qs.getQuestItemsCount(CRYSTAL_MEDALLION) > 0) {
			if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 0
					&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 0
					&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 0
					&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 0) {
				return cond > 2 ? "30424-05.htm" : "30424-01.htm";
			} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 0
					&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 0
					&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 1
					&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 0) {
				qs.giveItems(HALF_OF_DAIRY, 1);
				qs.setCond(4);
				return "30424-02.htm";
			} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 0
					&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 0
					&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 1
					&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 1) {
				return qs.getQuestItemsCount(TAMATOS_NECKLACE) == 0 ? "30424-06.htm" : "30424-03.htm";
			} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 1
					&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) == 0
					&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 1
					&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 1) {
				qs.takeItems(HALF_OF_DAIRY, 1);
				qs.giveItems(DAIRY_OF_ALLANA, 1);
				qs.setCond(7);
				return "30424-04.htm";
			} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) == 1
					&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 1
					&& qs.getQuestItemsCount(HALF_OF_DAIRY) == 0
					&& qs.getQuestItemsCount(DAIRY_OF_ALLANA) > 0) {
				return "30424-05.htm";
			}
		} else if (npcId == PERRIN && cond > 0
				&& qs.getQuestItemsCount(CRYSTAL_MEDALLION) > 0
				&& qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) > 0) {
			if (qs.getQuestItemsCount(TAMATOS_NECKLACE) == 1) {
				qs.giveItems(MONEY_OF_SWINDLER, 1);
				qs.takeItems(TAMATOS_NECKLACE, 1);
				qs.setCond(6);
				return "30428-04.htm";
			} else if (qs.getQuestItemsCount(MONEY_OF_SWINDLER) > 0) {
				return "30428-05.htm";
			} else {
				return cond > 4 ? "30428-06.htm" : "30428-01.htm";
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

		if (npcId == LIZARDMAN_WARRIOR) {
			if (cond > 0 && qs.getQuestItemsCount(LIZARD_CAPTAIN_ORDER) == 0) {
				qs.giveItems(LIZARD_CAPTAIN_ORDER, 1);
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(3);
			}
		} else if (npcId == TAMIL) {
			if (cond > 0 && qs.getQuestItemsCount(TAMATOS_NECKLACE) == 0) {
				qs.giveItems(TAMATOS_NECKLACE, 1);
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(5);
			}
		}
		return null;
	}
}
