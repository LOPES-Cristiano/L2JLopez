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
 * Quest 405: Path to Cleric (1ª Troca de Classe do Human Mystic para Cleric).
 */
@Component
public class Quest405PathToCleric extends Quest {

	public static final int QUEST_ID = 405;
	public static final String QUEST_NAME = "405_PathToCleric";

	// NPCs
	public static final int ZIGAUNT = 30022;
	public static final int SIMPLON = 30253;
	public static final int PRAGA = 30333;
	public static final int VIVYAN = 30030;
	public static final int LIONEL = 30408;
	public static final int GALLINT = 30017;

	// Monstros
	public static final int RUIN_ZOMBIE = 20026;
	public static final int RUIN_ZOMBIE_LEADER = 20029;

	// Itens
	public static final int LETTER_OF_ORDER1 = 1191;
	public static final int LETTER_OF_ORDER2 = 1192;
	public static final int BOOK_OF_LEMONIELL = 1193;
	public static final int BOOK_OF_VIVI = 1194;
	public static final int BOOK_OF_SIMLON = 1195;
	public static final int BOOK_OF_PRAGA = 1196;
	public static final int CERTIFICATE_OF_GALLINT = 1197;
	public static final int PENDANT_OF_MOTHER = 1198;
	public static final int NECKLACE_OF_MOTHER = 1199;
	public static final int LEMONIELLS_COVENANT = 1200;
	public static final int MARK_OF_FAITH = 1201;

	@Autowired
	public Quest405PathToCleric(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Cleric");

		addStartNpc(ZIGAUNT);
		addTalkId(ZIGAUNT);
		addTalkId(SIMPLON);
		addTalkId(PRAGA);
		addTalkId(VIVYAN);
		addTalkId(LIONEL);
		addTalkId(GALLINT);

		addKillId(RUIN_ZOMBIE);
		addKillId(RUIN_ZOMBIE_LEADER);

		registerQuestItems(LETTER_OF_ORDER1, LETTER_OF_ORDER2, BOOK_OF_LEMONIELL,
				BOOK_OF_VIVI, BOOK_OF_SIMLON, BOOK_OF_PRAGA, CERTIFICATE_OF_GALLINT,
				PENDANT_OF_MOTHER, NECKLACE_OF_MOTHER, LEMONIELLS_COVENANT);

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
			if (level >= 18 && classId == 0x0a && qs.getQuestItemsCount(MARK_OF_FAITH) == 0) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				qs.giveItems(LETTER_OF_ORDER1, 1);
				return "30022-05.htm";
			} else if (classId != 0x0a) {
				return classId == 0x0f ? "30022-02a.htm" : "30022-02.htm";
			} else if (level < 18 && classId == 0x0a) {
				return "30022-03.htm";
			} else if (qs.getQuestItemsCount(MARK_OF_FAITH) > 0) {
				return "30022-04.htm";
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

		if (npcId == ZIGAUNT) {
			if (cond == 0) {
				return qs.getQuestItemsCount(MARK_OF_FAITH) == 0 ? "30022-01.htm" : "30022-04.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(LETTER_OF_ORDER2) == 1 && qs.getQuestItemsCount(LEMONIELLS_COVENANT) == 1) {
					qs.rewardItems(57, 81900);
					qs.takeItems(LETTER_OF_ORDER2, 1);
					qs.takeItems(LEMONIELLS_COVENANT, 1);
					qs.giveItems(MARK_OF_FAITH, 1);
					qs.addExpAndSp(295862, 17664);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30022-09.htm";
				} else if (qs.getQuestItemsCount(LETTER_OF_ORDER2) == 1) {
					return "30022-07.htm";
				} else if (qs.getQuestItemsCount(LETTER_OF_ORDER1) == 1) {
					if (qs.getQuestItemsCount(BOOK_OF_VIVI) == 1
							&& qs.getQuestItemsCount(BOOK_OF_SIMLON) > 0
							&& qs.getQuestItemsCount(BOOK_OF_PRAGA) == 1) {
						qs.takeItems(BOOK_OF_PRAGA, -1);
						qs.takeItems(BOOK_OF_VIVI, -1);
						qs.takeItems(BOOK_OF_SIMLON, -1);
						qs.takeItems(LETTER_OF_ORDER1, 1);
						qs.giveItems(LETTER_OF_ORDER2, 1);
						qs.setCond(3);
						qs.playSound("ItemSound.quest_middle");
						return "30022-08.htm";
					} else {
						return "30022-06.htm";
					}
				}
			}
		} else if (npcId == SIMPLON && cond > 0 && qs.getQuestItemsCount(LETTER_OF_ORDER1) == 1) {
			if (qs.getQuestItemsCount(BOOK_OF_SIMLON) == 0) {
				qs.giveItems(BOOK_OF_SIMLON, 3);
				return "30253-01.htm";
			} else {
				return "30253-02.htm";
			}
		} else if (npcId == VIVYAN && cond > 0 && qs.getQuestItemsCount(LETTER_OF_ORDER1) == 1) {
			if (qs.getQuestItemsCount(BOOK_OF_VIVI) == 0) {
				qs.giveItems(BOOK_OF_VIVI, 1);
				return "30030-01.htm";
			} else {
				return "30030-02.htm";
			}
		} else if (npcId == PRAGA && cond > 0 && qs.getQuestItemsCount(LETTER_OF_ORDER1) == 1) {
			if (qs.getQuestItemsCount(BOOK_OF_PRAGA) == 0 && qs.getQuestItemsCount(NECKLACE_OF_MOTHER) == 0 && qs.getQuestItemsCount(PENDANT_OF_MOTHER) == 0) {
				qs.giveItems(NECKLACE_OF_MOTHER, 1);
				qs.setCond(2);
				return "30333-01.htm";
			} else if (qs.getQuestItemsCount(NECKLACE_OF_MOTHER) == 1 && qs.getQuestItemsCount(PENDANT_OF_MOTHER) == 0) {
				return "30333-02.htm";
			} else if (qs.getQuestItemsCount(PENDANT_OF_MOTHER) == 1) {
				qs.takeItems(PENDANT_OF_MOTHER, -1);
				qs.takeItems(NECKLACE_OF_MOTHER, -1);
				qs.giveItems(BOOK_OF_PRAGA, 1);
				return "30333-03.htm";
			} else if (qs.getQuestItemsCount(BOOK_OF_PRAGA) == 1) {
				return "30333-04.htm";
			}
		} else if (npcId == LIONEL && cond > 0 && qs.getQuestItemsCount(LETTER_OF_ORDER2) == 1) {
			if (qs.getQuestItemsCount(BOOK_OF_LEMONIELL) == 0 && qs.getQuestItemsCount(LEMONIELLS_COVENANT) == 0 && qs.getQuestItemsCount(CERTIFICATE_OF_GALLINT) == 0) {
				qs.giveItems(BOOK_OF_LEMONIELL, 1);
				qs.setCond(4);
				return "30408-01.htm";
			} else if (qs.getQuestItemsCount(CERTIFICATE_OF_GALLINT) == 1) {
				qs.takeItems(CERTIFICATE_OF_GALLINT, -1);
				qs.giveItems(LEMONIELLS_COVENANT, 1);
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				return "30408-03.htm";
			} else {
				return "30408-02.htm";
			}
		} else if (npcId == GALLINT && cond > 0 && qs.getQuestItemsCount(BOOK_OF_LEMONIELL) == 1) {
			qs.takeItems(BOOK_OF_LEMONIELL, -1);
			qs.giveItems(CERTIFICATE_OF_GALLINT, 1);
			qs.setCond(5);
			return "30017-01.htm";
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

		if ((npcId == RUIN_ZOMBIE || npcId == RUIN_ZOMBIE_LEADER) && cond == 2 && qs.getQuestItemsCount(PENDANT_OF_MOTHER) == 0) {
			qs.giveItems(PENDANT_OF_MOTHER, 1);
			qs.playSound("ItemSound.quest_middle");
		}
		return null;
	}
}
