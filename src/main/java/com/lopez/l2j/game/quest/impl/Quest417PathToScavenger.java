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
 * Quest 417: Path to Scavenger (1ª Troca de Classe do Dwarf Fighter para Scavenger).
 */
@Component
public class Quest417PathToScavenger extends Quest {

	public static final int QUEST_ID = 417;
	public static final String QUEST_NAME = "417_PathToScavenger";

	// NPCs
	public static final int PIPPI = 30524;
	public static final int RAUT = 30316;
	public static final int SHARI = 30517;
	public static final int MION = 30519;
	public static final int BRONK = 30525;
	public static final int ZIMENF = 30538;
	public static final int TOMA = 30556;
	public static final int TORAI = 30557;

	// Monstros
	public static final int HUNTER_TARANTULA = 20403;
	public static final int HONEY_BEAR = 27058;
	public static final int PLUNDER_TARANTULA = 20508;
	public static final int HUNTER_BEAR = 20777;

	// Itens
	public static final int RING_OF_RAVEN = 1642;
	public static final int PIPIS_LETTER = 1643;
	public static final int ROUTS_TP_SCROLL = 1644;
	public static final int SUCCUBUS_UNDIES = 1645;
	public static final int MIONS_LETTER = 1646;
	public static final int BRONKS_INGOT = 1647;
	public static final int CHARIS_AXE = 1648;
	public static final int ZIMENFS_POTION = 1649;
	public static final int BRONKS_PAY = 1650;
	public static final int CHALIS_PAY = 1651;
	public static final int ZIMENFS_PAY = 1652;
	public static final int BEAR_PIC = 1653;
	public static final int TARANTULA_PIC = 1654;
	public static final int HONEY_JAR = 1655;
	public static final int BEAD = 1656;
	public static final int BEAD_PARCEL = 1657;

	@Autowired
	public Quest417PathToScavenger(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Scavenger");

		addStartNpc(PIPPI);
		addTalkId(PIPPI);
		addTalkId(RAUT);
		addTalkId(SHARI);
		addTalkId(MION);
		addTalkId(BRONK);
		addTalkId(ZIMENF);
		addTalkId(TOMA);
		addTalkId(TORAI);

		addKillId(HUNTER_TARANTULA);
		addKillId(HONEY_BEAR);
		addKillId(PLUNDER_TARANTULA);
		addKillId(HUNTER_BEAR);

		registerQuestItems(RING_OF_RAVEN, PIPIS_LETTER, ROUTS_TP_SCROLL, SUCCUBUS_UNDIES,
				MIONS_LETTER, BRONKS_INGOT, CHARIS_AXE, ZIMENFS_POTION, BRONKS_PAY,
				CHALIS_PAY, ZIMENFS_PAY, BEAR_PIC, TARANTULA_PIC, HONEY_JAR, BEAD, BEAD_PARCEL);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30524-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(PIPIS_LETTER, 1);
			qs.playSound("ItemSound.quest_accept");
			return "30524-05.htm";
		} else if ("30519_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(PIPIS_LETTER) > 0) {
				qs.takeItems(PIPIS_LETTER, -1);
				qs.giveItems(ZIMENFS_POTION, 1);
				qs.setCond(2);
				qs.playSound("ItemSound.quest_middle");
				return "30519-02.htm";
			}
		} else if ("30316_1".equalsIgnoreCase(event) || "30316_2".equalsIgnoreCase(event) || "30316-02.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BEAD_PARCEL) > 0) {
				qs.takeItems(BEAD_PARCEL, -1);
				qs.giveItems(ROUTS_TP_SCROLL, 1);
				qs.setCond(10);
				qs.playSound("ItemSound.quest_middle");
				return "30316-02.htm";
			}
		} else if ("30557_1".equalsIgnoreCase(event)) {
			return "30557-02.htm";
		} else if ("30557_2".equalsIgnoreCase(event) || "30557-03.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ROUTS_TP_SCROLL) > 0) {
				qs.takeItems(ROUTS_TP_SCROLL, -1);
				qs.giveItems(SUCCUBUS_UNDIES, 1);
				qs.setCond(11);
				qs.playSound("ItemSound.quest_middle");
				return "30557-03.htm";
			}
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

		if (npcId == PIPPI) {
			if (qs.getQuestItemsCount(RING_OF_RAVEN) > 0) {
				return "30524-04.htm";
			}
			if (cond == 0) {
				if (player.getClassId() != 53) {
					return player.getClassId() == 54 ? "30524-02a.htm" : "30524-08.htm";
				}
				if (player.getLevel() < 18) {
					return "30524-02.htm";
				}
				return "30524-01.htm";
			} else if (cond == 1) {
				return "30524-06.htm";
			} else {
				return "30524-07.htm";
			}
		} else if (npcId == MION) {
			if (cond == 1 && qs.getQuestItemsCount(PIPIS_LETTER) > 0) {
				return "30519-01.htm";
			} else if (cond >= 2 && (qs.getQuestItemsCount(CHALIS_PAY) > 0 || qs.getQuestItemsCount(BRONKS_PAY) > 0 || qs.getQuestItemsCount(ZIMENFS_PAY) > 0)) {
				qs.takeItems(CHALIS_PAY, -1);
				qs.takeItems(BRONKS_PAY, -1);
				qs.takeItems(ZIMENFS_PAY, -1);
				qs.giveItems(MIONS_LETTER, 1);
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				return "30519-15.htm";
			} else if (cond == 4 || qs.getQuestItemsCount(MIONS_LETTER) > 0) {
				return "30519-13.htm";
			} else if (cond > 4) {
				return "30519-14.htm";
			}
		} else if (npcId == SHARI) {
			if (qs.getQuestItemsCount(CHARIS_AXE) > 0) {
				qs.takeItems(CHARIS_AXE, -1);
				qs.giveItems(CHALIS_PAY, 1);
				qs.setCond(3);
				qs.playSound("ItemSound.quest_middle");
				return "30517-02.htm";
			} else if (qs.getQuestItemsCount(CHALIS_PAY) > 0) {
				return "30517-03.htm";
			}
		} else if (npcId == BRONK) {
			if (qs.getQuestItemsCount(BRONKS_INGOT) > 0) {
				qs.takeItems(BRONKS_INGOT, -1);
				qs.giveItems(BRONKS_PAY, 1);
				qs.setCond(3);
				qs.playSound("ItemSound.quest_middle");
				return "30525-02.htm";
			} else if (qs.getQuestItemsCount(BRONKS_PAY) > 0) {
				return "30525-03.htm";
			}
		} else if (npcId == ZIMENF) {
			if (qs.getQuestItemsCount(ZIMENFS_POTION) > 0) {
				qs.takeItems(ZIMENFS_POTION, -1);
				qs.giveItems(ZIMENFS_PAY, 1);
				qs.setCond(3);
				qs.playSound("ItemSound.quest_middle");
				return "30538-02.htm";
			} else if (qs.getQuestItemsCount(ZIMENFS_PAY) > 0) {
				return "30538-03.htm";
			}
		} else if (npcId == TOMA) {
			if (cond == 4 && qs.getQuestItemsCount(MIONS_LETTER) > 0) {
				qs.takeItems(MIONS_LETTER, -1);
				qs.giveItems(BEAR_PIC, 1);
				qs.setCond(5);
				qs.playSound("ItemSound.quest_middle");
				return "30556-01.htm";
			} else if (cond == 5) {
				return "30556-02.htm";
			} else if (cond == 6 || (cond == 5 && qs.getQuestItemsCount(HONEY_JAR) >= 5)) {
				qs.takeItems(HONEY_JAR, -1);
				qs.takeItems(BEAR_PIC, -1);
				qs.giveItems(TARANTULA_PIC, 1);
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				return "30556-03.htm";
			} else if (cond == 7) {
				return "30556-04.htm";
			} else if (cond == 8 || (cond == 7 && qs.getQuestItemsCount(BEAD) >= 20)) {
				qs.takeItems(BEAD, -1);
				qs.takeItems(TARANTULA_PIC, -1);
				qs.giveItems(BEAD_PARCEL, 1);
				qs.setCond(9);
				qs.playSound("ItemSound.quest_middle");
				return "30556-05.htm";
			} else if (cond == 9) {
				return "30556-06.htm";
			} else if (cond > 9) {
				return "30556-07.htm";
			}
		} else if (npcId == RAUT) {
			if (cond == 9 && qs.getQuestItemsCount(BEAD_PARCEL) > 0) {
				return "30316-01.htm";
			} else if (cond == 10 && qs.getQuestItemsCount(ROUTS_TP_SCROLL) > 0) {
				return "30316-04.htm";
			} else if (cond == 11 && qs.getQuestItemsCount(SUCCUBUS_UNDIES) > 0) {
				qs.takeItems(SUCCUBUS_UNDIES, -1);
				qs.giveItems(RING_OF_RAVEN, 1);
				qs.rewardItems(57, 81900);
				qs.addExpAndSp(295862, 7080);
				qs.playSound("ItemSound.quest_finish");
				qs.exitCurrentQuest(false);
				return "30316-05.htm";
			}
		} else if (npcId == TORAI) {
			if (cond == 10 && qs.getQuestItemsCount(ROUTS_TP_SCROLL) > 0) {
				return "30557-01.htm";
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

		if ((cond == 5 || cond == 6) && (npcId == HUNTER_BEAR || npcId == HONEY_BEAR)) {
			if (qs.getQuestItemsCount(BEAR_PIC) > 0 && qs.getQuestItemsCount(HONEY_JAR) < 5) {
				qs.giveItems(HONEY_JAR, 1);
				if (qs.getQuestItemsCount(HONEY_JAR) >= 5) {
					qs.setCond(6);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if ((cond == 7 || cond == 8) && (npcId == HUNTER_TARANTULA || npcId == PLUNDER_TARANTULA)) {
			if (qs.getQuestItemsCount(TARANTULA_PIC) > 0 && qs.getQuestItemsCount(BEAD) < 20) {
				qs.giveItems(BEAD, 1);
				if (qs.getQuestItemsCount(BEAD) >= 20) {
					qs.setCond(8);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		}

		return null;
	}
}
