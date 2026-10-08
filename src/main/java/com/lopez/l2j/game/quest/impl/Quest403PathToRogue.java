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
 * Quest 403: Path to Rogue (1ª Troca de Classe do Human Fighter para Rogue).
 */
@Component
public class Quest403PathToRogue extends Quest {

	public static final int QUEST_ID = 403;
	public static final String QUEST_NAME = "403_PathToRogue";

	// NPCs
	public static final int BEZIQUE = 30379;
	public static final int NETI = 30425;

	// Monstros
	public static final int TRACKER_SKELETON = 20035;
	public static final int TRACKER_SKELETON_LEADER = 20042;
	public static final int MISERY_SKELETON = 20045;
	public static final int SKELETON_ARCHER = 20051;
	public static final int RUIN_SPARTOI = 20054;
	public static final int SKELETON_SCOUT = 20060;
	public static final int CATS_EYE_BANDIT = 27038;

	// Itens
	public static final int BEZIQUES_LETTER = 1180;
	public static final int NETIS_BOW = 1181;
	public static final int NETIS_DAGGER = 1182;
	public static final int SPATOIS_BONES = 1183;
	public static final int HORSESHOE_OF_LIGHT = 1184;
	public static final int WANTED_BILL = 1185;
	public static final int STOLEN_JEWELRY = 1186;
	public static final int STOLEN_TOMES = 1187;
	public static final int STOLEN_RING = 1188;
	public static final int STOLEN_NECKLACE = 1189;
	public static final int BEZIQUES_RECOMMENDATION = 1190;

	@Autowired
	public Quest403PathToRogue(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Rogue");

		addStartNpc(BEZIQUE);
		addTalkId(BEZIQUE);
		addTalkId(NETI);

		addKillId(TRACKER_SKELETON);
		addKillId(TRACKER_SKELETON_LEADER);
		addKillId(MISERY_SKELETON);
		addKillId(SKELETON_ARCHER);
		addKillId(RUIN_SPARTOI);
		addKillId(SKELETON_SCOUT);
		addKillId(CATS_EYE_BANDIT);

		registerQuestItems(BEZIQUES_LETTER, NETIS_BOW, NETIS_DAGGER, SPATOIS_BONES,
				HORSESHOE_OF_LIGHT, WANTED_BILL, STOLEN_JEWELRY, STOLEN_TOMES,
				STOLEN_RING, STOLEN_NECKLACE);

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

		if ("30379_2".equalsIgnoreCase(event)) {
			if (classId == 0) {
				if (level >= 18) {
					return qs.getQuestItemsCount(BEZIQUES_RECOMMENDATION) > 0 ? "30379-04.htm" : "30379-05.htm";
				} else {
					return "30379-03.htm";
				}
			} else {
				return classId == 7 ? "30379-02a.htm" : "30379-02.htm";
			}
		} else if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			qs.giveItems(BEZIQUES_LETTER, 1);
			return "30379-06.htm";
		} else if ("30425_1".equalsIgnoreCase(event)) {
			qs.takeItems(BEZIQUES_LETTER, 1);
			if (qs.getQuestItemsCount(NETIS_BOW) == 0) {
				qs.giveItems(NETIS_BOW, 1);
			}
			if (qs.getQuestItemsCount(NETIS_DAGGER) == 0) {
				qs.giveItems(NETIS_DAGGER, 1);
			}
			qs.setCond(2);
			return "30425-05.htm";
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

		if (npcId == BEZIQUE) {
			if (cond == 0) {
				return "30379-01.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(HORSESHOE_OF_LIGHT) == 0 && haveAllStolenItems(qs)) {
					qs.takeItems(NETIS_BOW, 1);
					qs.takeItems(NETIS_DAGGER, 1);
					qs.takeItems(WANTED_BILL, 1);
					qs.takeItems(STOLEN_JEWELRY, -1);
					qs.takeItems(STOLEN_TOMES, -1);
					qs.takeItems(STOLEN_RING, -1);
					qs.takeItems(STOLEN_NECKLACE, -1);
					qs.rewardItems(57, 81900);
					qs.giveItems(BEZIQUES_RECOMMENDATION, 1);
					qs.addExpAndSp(295862, 16344);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30379-09.htm";
				} else if (qs.getQuestItemsCount(HORSESHOE_OF_LIGHT) == 1) {
					qs.takeItems(HORSESHOE_OF_LIGHT, 1);
					qs.giveItems(WANTED_BILL, 1);
					qs.setCond(5);
					return "30379-08.htm";
				} else if (qs.getQuestItemsCount(BEZIQUES_LETTER) == 1) {
					return "30379-07.htm";
				} else if (qs.getQuestItemsCount(WANTED_BILL) == 1) {
					return "30379-10.htm";
				} else if (qs.getQuestItemsCount(NETIS_BOW) == 1 && qs.getQuestItemsCount(NETIS_DAGGER) == 1) {
					return "30379-07.htm";
				}
			}
		} else if (npcId == NETI && cond > 0) {
			if (qs.getQuestItemsCount(BEZIQUES_LETTER) == 1) {
				return "30425-01.htm";
			} else if (qs.getQuestItemsCount(HORSESHOE_OF_LIGHT) == 0) {
				if (qs.getQuestItemsCount(SPATOIS_BONES) < 10) {
					return "30425-06.htm";
				} else {
					qs.takeItems(SPATOIS_BONES, -1);
					qs.giveItems(HORSESHOE_OF_LIGHT, 1);
					qs.setCond(4);
					qs.playSound("ItemSound.quest_middle");
					return "30425-07.htm";
				}
			} else if (qs.getQuestItemsCount(HORSESHOE_OF_LIGHT) == 1) {
				return "30425-08.htm";
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

		// Checa se os itens de Neti estão em mãos ou no inventário
		if ((cond == 2 || cond == 3) && qs.getQuestItemsCount(SPATOIS_BONES) < 10) {
			if (npcId == TRACKER_SKELETON || npcId == TRACKER_SKELETON_LEADER
					|| npcId == MISERY_SKELETON || npcId == SKELETON_ARCHER
					|| npcId == RUIN_SPARTOI || npcId == SKELETON_SCOUT) {
				qs.giveItems(SPATOIS_BONES, 1);
				if (qs.getQuestItemsCount(SPATOIS_BONES) >= 10) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(3);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 5 && npcId == CATS_EYE_BANDIT) {
			if (qs.getQuestItemsCount(STOLEN_JEWELRY) == 0) {
				qs.giveItems(STOLEN_JEWELRY, 1);
			} else if (qs.getQuestItemsCount(STOLEN_TOMES) == 0) {
				qs.giveItems(STOLEN_TOMES, 1);
			} else if (qs.getQuestItemsCount(STOLEN_RING) == 0) {
				qs.giveItems(STOLEN_RING, 1);
			} else if (qs.getQuestItemsCount(STOLEN_NECKLACE) == 0) {
				qs.giveItems(STOLEN_NECKLACE, 1);
			}
			if (haveAllStolenItems(qs)) {
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(6);
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		}
		return null;
	}

	private boolean haveAllStolenItems(QuestState qs) {
		return qs.getQuestItemsCount(STOLEN_JEWELRY) > 0
				&& qs.getQuestItemsCount(STOLEN_TOMES) > 0
				&& qs.getQuestItemsCount(STOLEN_RING) > 0
				&& qs.getQuestItemsCount(STOLEN_NECKLACE) > 0;
	}
}
