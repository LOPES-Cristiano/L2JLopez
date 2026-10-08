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
 * Quest 406: Path to Elven Knight (1ª Troca de Classe do Elven Fighter para Elven Knight).
 */
@Component
public class Quest406PathToElvenKnight extends Quest {

	public static final int QUEST_ID = 406;
	public static final String QUEST_NAME = "406_PathToElvenKnight";

	// NPCs
	public static final int SORIUS = 30327;
	public static final int KLUTO = 30317;

	// Monstros
	public static final int TRACKER_SKELETON = 20035;
	public static final int TRACKER_SKELETON_LEADER = 20042;
	public static final int SKELETON_SCOUT = 20060;
	public static final int OL_MAHUM_NOVICE = 20782;

	// Itens
	public static final int SORIUS_LETTER1 = 1202;
	public static final int KLUTO_BOX = 1203;
	public static final int ELVEN_KNIGHT_BROOCH = 1204;
	public static final int TOPAZ_PIECE = 1205;
	public static final int EMERALD_PIECE = 1206;
	public static final int KLUTO_MEMO = 1276;

	@Autowired
	public Quest406PathToElvenKnight(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Elven Knight");

		addStartNpc(SORIUS);
		addTalkId(SORIUS);
		addTalkId(KLUTO);

		addKillId(TRACKER_SKELETON);
		addKillId(TRACKER_SKELETON_LEADER);
		addKillId(SKELETON_SCOUT);
		addKillId(OL_MAHUM_NOVICE);

		registerQuestItems(SORIUS_LETTER1, KLUTO_BOX, TOPAZ_PIECE, EMERALD_PIECE, KLUTO_MEMO);

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

		if ("30327-05.htm".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			if (classId != 0x12) {
				return classId == 0x13 ? "30327-02a.htm" : "30327-02.htm";
			} else if (level < 18) {
				return "30327-03.htm";
			} else if (qs.getQuestItemsCount(ELVEN_KNIGHT_BROOCH) > 0) {
				return "30327-04.htm";
			} else {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				return "30327-06.htm";
			}
		} else if ("30327-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			return event;
		} else if ("30317-02.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() == 3) {
				qs.takeItems(SORIUS_LETTER1, -1);
				if (qs.getQuestItemsCount(KLUTO_MEMO) == 0) {
					qs.giveItems(KLUTO_MEMO, 1);
					qs.setCond(4);
				}
			}
			return event;
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

		if (npcId == SORIUS) {
			if (cond == 0) {
				return "30327-01.htm";
			} else if (cond == 1) {
				return qs.getQuestItemsCount(TOPAZ_PIECE) == 0 ? "30327-07.htm" : "30327-08.htm";
			} else if (cond == 2) {
				if (qs.getQuestItemsCount(SORIUS_LETTER1) == 0) {
					qs.giveItems(SORIUS_LETTER1, 1);
				}
				qs.setCond(3);
				return "30327-09.htm";
			} else if (cond >= 3 && cond <= 5) {
				return "30327-11.htm";
			} else if (cond == 6) {
				qs.rewardItems(57, 81900);
				qs.takeItems(KLUTO_BOX, -1);
				if (qs.getQuestItemsCount(ELVEN_KNIGHT_BROOCH) == 0) {
					qs.giveItems(ELVEN_KNIGHT_BROOCH, 1);
				}
				qs.addExpAndSp(228064, 14925);
				qs.setCond(0);
				qs.exitQuest(false);
				qs.playSound("ItemSound.quest_finish");
				return "30327-10.htm";
			}
		} else if (npcId == KLUTO && cond > 0) {
			if (cond == 3) {
				return "30317-01.htm";
			} else if (cond == 4) {
				return qs.getQuestItemsCount(EMERALD_PIECE) == 0 ? "30317-03.htm" : "30317-04.htm";
			} else if (cond == 5) {
				qs.takeItems(EMERALD_PIECE, -1);
				qs.takeItems(KLUTO_MEMO, -1);
				if (qs.getQuestItemsCount(KLUTO_BOX) == 0) {
					qs.giveItems(KLUTO_BOX, 1);
				}
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				return "30317-05.htm";
			} else if (cond == 6) {
				return "30317-06.htm";
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

		if (cond == 1 && (npcId == TRACKER_SKELETON || npcId == TRACKER_SKELETON_LEADER || npcId == SKELETON_SCOUT)) {
			long count = qs.getQuestItemsCount(TOPAZ_PIECE);
			if (count < 20 && ThreadLocalRandom.current().nextInt(10) < 7) {
				qs.giveItems(TOPAZ_PIECE, 1);
				if (count + 1 >= 20) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(2);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 4 && npcId == OL_MAHUM_NOVICE) {
			long count = qs.getQuestItemsCount(EMERALD_PIECE);
			if (count < 20 && ThreadLocalRandom.current().nextInt(2) == 0) {
				qs.giveItems(EMERALD_PIECE, 1);
				if (count + 1 >= 20) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(5);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return null;
	}
}
