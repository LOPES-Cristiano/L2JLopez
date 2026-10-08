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
 * Quest 418: Path to Artisan (1ª Troca de Classe do Dwarf Fighter para Artisan).
 */
@Component
public class Quest418PathToArtisan extends Quest {

	public static final int QUEST_ID = 418;
	public static final String QUEST_NAME = "418_PathToArtisan";

	// NPCs
	public static final int SILVERA = 30527;
	public static final int KLUTO = 30317;
	public static final int PINTER = 30298;

	// Monstros
	public static final int BOOGLE_RATMAN = 20389;
	public static final int BOOGLE_RATMAN_LEADER = 20390;
	public static final int VUKU_LIZARDMAN = 20017;

	// Itens
	public static final int SILVERYS_RING = 1632;
	public static final int PASS_CERTIFICATE_1ST = 1633;
	public static final int PASS_CERTIFICATE_2ND = 1634;
	public static final int PASS_FINAL = 1635;
	public static final int BOOGLE_RATMAN_TOOTH = 1636;
	public static final int BOOGLE_RATMAN_LEADER_TOOTH = 1637;
	public static final int KLUTOS_LETTER = 1638;
	public static final int FOOTPRINT_OF_THIEF = 1639;
	public static final int STOLEN_SECRET_BOX = 1640;
	public static final int SECRET_BOX = 1641;

	@Autowired
	public Quest418PathToArtisan(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Artisan");

		addStartNpc(SILVERA);
		addTalkId(SILVERA);
		addTalkId(KLUTO);
		addTalkId(PINTER);

		addKillId(BOOGLE_RATMAN);
		addKillId(BOOGLE_RATMAN_LEADER);
		addKillId(VUKU_LIZARDMAN);

		registerQuestItems(SILVERYS_RING, PASS_CERTIFICATE_1ST, PASS_CERTIFICATE_2ND,
				BOOGLE_RATMAN_TOOTH, BOOGLE_RATMAN_LEADER_TOOTH, KLUTOS_LETTER,
				FOOTPRINT_OF_THIEF, STOLEN_SECRET_BOX, SECRET_BOX);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30527-06.htm".equalsIgnoreCase(event)) {
			qs.giveItems(SILVERYS_RING, 1);
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			return event;
		} else if ("30317-04.htm".equalsIgnoreCase(event) || "30317-07.htm".equalsIgnoreCase(event)) {
			qs.giveItems(KLUTOS_LETTER, 1);
			qs.setCond(4);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("30298-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KLUTOS_LETTER, -1);
			qs.giveItems(FOOTPRINT_OF_THIEF, 1);
			qs.setCond(5);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("30298-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(STOLEN_SECRET_BOX, -1);
			qs.takeItems(FOOTPRINT_OF_THIEF, -1);
			qs.giveItems(SECRET_BOX, 1);
			qs.giveItems(PASS_CERTIFICATE_2ND, 1);
			qs.setCond(7);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("30317-10.htm".equalsIgnoreCase(event) || "30317-12.htm".equalsIgnoreCase(event)) {
			qs.takeItems(PASS_CERTIFICATE_1ST, -1);
			qs.takeItems(PASS_CERTIFICATE_2ND, -1);
			qs.takeItems(SECRET_BOX, -1);
			qs.giveItems(PASS_FINAL, 1);
			qs.rewardItems(57, 81900);
			qs.addExpAndSp(295862, 18194);
			qs.playSound("ItemSound.quest_finish");
			qs.exitCurrentQuest(false);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == SILVERA) {
			if (qs.getQuestItemsCount(PASS_FINAL) > 0) {
				return "30527-04.htm";
			}
			if (cond == 0) {
				if (player.getClassId() != 53) {
					return player.getClassId() == 56 ? "30527-02a.htm" : "30527-02.htm";
				}
				if (player.getLevel() < 18) {
					return "30527-03.htm";
				}
				return "30527-01.htm";
			} else if (cond == 1) {
				return "30527-07.htm";
			} else if (cond == 2) {
				qs.takeItems(BOOGLE_RATMAN_TOOTH, -1);
				qs.takeItems(BOOGLE_RATMAN_LEADER_TOOTH, -1);
				qs.takeItems(SILVERYS_RING, -1);
				qs.giveItems(PASS_CERTIFICATE_1ST, 1);
				qs.setCond(3);
				qs.playSound("ItemSound.quest_middle");
				return "30527-08.htm";
			} else if (cond >= 3) {
				return "30527-09.htm";
			}
		} else if (npcId == KLUTO) {
			if (cond == 3) {
				return "30317-01.htm";
			} else if (cond == 4 || cond == 5) {
				return "30317-08.htm";
			} else if (cond == 7) {
				return "30317-09.htm";
			}
		} else if (npcId == PINTER) {
			if (cond == 4) {
				return "30298-01.htm";
			} else if (cond == 5) {
				return "30298-04.htm";
			} else if (cond == 6) {
				return "30298-05.htm";
			} else if (cond == 7) {
				return "30298-07.htm";
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

		if (cond == 1 && qs.getQuestItemsCount(SILVERYS_RING) > 0) {
			if (npcId == BOOGLE_RATMAN && qs.getQuestItemsCount(BOOGLE_RATMAN_TOOTH) < 10) {
				qs.giveItems(BOOGLE_RATMAN_TOOTH, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (npcId == BOOGLE_RATMAN_LEADER && qs.getQuestItemsCount(BOOGLE_RATMAN_LEADER_TOOTH) < 2) {
				qs.giveItems(BOOGLE_RATMAN_LEADER_TOOTH, 1);
				qs.playSound("ItemSound.quest_itemget");
			}

			if (qs.getQuestItemsCount(BOOGLE_RATMAN_TOOTH) >= 10 && qs.getQuestItemsCount(BOOGLE_RATMAN_LEADER_TOOTH) >= 2) {
				qs.setCond(2);
				qs.playSound("ItemSound.quest_middle");
			}
		} else if (cond == 5 && npcId == VUKU_LIZARDMAN && qs.getQuestItemsCount(STOLEN_SECRET_BOX) == 0) {
			qs.giveItems(STOLEN_SECRET_BOX, 1);
			qs.setCond(6);
			qs.playSound("ItemSound.quest_middle");
		}

		return null;
	}
}
