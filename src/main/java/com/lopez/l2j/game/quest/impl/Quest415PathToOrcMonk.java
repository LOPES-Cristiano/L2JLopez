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
 * Quest 415: Path to Orc Monk (1ª Troca de Classe do Orc Fighter para Orc Monk).
 */
@Component
public class Quest415PathToOrcMonk extends Quest {

	public static final int QUEST_ID = 415;
	public static final String QUEST_NAME = "415_PathToOrcMonk";

	// NPCs
	public static final int GANTAKI = 30587;
	public static final int ROSHEEK = 30590;
	public static final int KASMAN = 30501;
	public static final int TORUKU = 30591;

	// Monstros
	public static final int KASHA_BEAR = 20479;
	public static final int KASHA_BLADE_SPIDER = 20478;
	public static final int SCARLET_SALAMANDER = 20415;
	public static final int FELIM_LIZARDMAN_WARRIOR = 20017;
	public static final int RATMAN_HUNTER = 20359;
	public static final int LANGK_LIZARDMAN_WARRIOR = 20024;
	public static final int FELIM_LIZARDMAN_SCOUT = 20014;

	// Itens
	public static final int POMEGRANATE = 1593;
	public static final int LEATHER_POUCH_1 = 1594;
	public static final int LEATHER_POUCH_2 = 1595;
	public static final int LEATHER_POUCH_3 = 1596;
	public static final int LEATHER_POUCH_1_FULL = 1597;
	public static final int LEATHER_POUCH_2_FULL = 1598;
	public static final int LEATHER_POUCH_3_FULL = 1599;
	public static final int KASHA_BEAR_CLAW = 1600;
	public static final int KASHA_SPIDER_TALON = 1601;
	public static final int SALAMANDER_SCALE = 1602;
	public static final int SCROLL_FIERY_SPIRIT = 1603;
	public static final int ROSHEEKS_LETTER = 1604;
	public static final int GANTAKIS_LETTER = 1605;
	public static final int FIG = 1606;
	public static final int LEATHER_PURSE_4 = 1607;
	public static final int LEATHER_POUCH_4_FULL = 1608;
	public static final int VUKU_TUSK = 1609;
	public static final int RATMAN_FANG = 1610;
	public static final int LANGK_TOOTH = 1611;
	public static final int FELIM_TOOTH = 1612;
	public static final int SCROLL_IRON_WILL = 1613;
	public static final int TORUKUS_LETTER = 1614;
	public static final int KHAVATARI_TOTEM = 1615;

	@Autowired
	public Quest415PathToOrcMonk(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Orc Monk");

		addStartNpc(GANTAKI);
		addTalkId(GANTAKI);
		addTalkId(ROSHEEK);
		addTalkId(KASMAN);
		addTalkId(TORUKU);

		addKillId(KASHA_BEAR);
		addKillId(KASHA_BLADE_SPIDER);
		addKillId(SCARLET_SALAMANDER);
		addKillId(FELIM_LIZARDMAN_WARRIOR);
		addKillId(RATMAN_HUNTER);
		addKillId(LANGK_LIZARDMAN_WARRIOR);
		addKillId(FELIM_LIZARDMAN_SCOUT);

		registerQuestItems(POMEGRANATE, LEATHER_POUCH_1, LEATHER_POUCH_2, LEATHER_POUCH_3,
				LEATHER_POUCH_1_FULL, LEATHER_POUCH_2_FULL, LEATHER_POUCH_3_FULL,
				KASHA_BEAR_CLAW, KASHA_SPIDER_TALON, SALAMANDER_SCALE, SCROLL_FIERY_SPIRIT,
				ROSHEEKS_LETTER, GANTAKIS_LETTER, FIG, LEATHER_PURSE_4, LEATHER_POUCH_4_FULL,
				VUKU_TUSK, RATMAN_FANG, LANGK_TOOTH, FELIM_TOOTH, SCROLL_IRON_WILL, TORUKUS_LETTER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30587-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(POMEGRANATE, 1);
			qs.playSound("ItemSound.quest_accept");
			return "30587-06.htm";
		} else if ("30587-09a.htm".equalsIgnoreCase(event) || "30587-09.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ROSHEEKS_LETTER, -1);
			qs.giveItems(GANTAKIS_LETTER, 1);
			qs.setCond(9);
			qs.playSound("ItemSound.quest_middle");
			return "30587-09.htm";
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

		if (npcId == GANTAKI) {
			if (qs.getQuestItemsCount(KHAVATARI_TOTEM) > 0) {
				return "30587-04.htm";
			}
			if (cond == 0) {
				if (player.getClassId() != 44) {
					return player.getClassId() == 47 ? "30587-02a.htm" : "30587-02.htm";
				}
				if (player.getLevel() < 18) {
					return "30587-03.htm";
				}
				return "30587-01.htm";
			} else if (cond == 1) {
				return "30587-07.htm";
			} else if (cond >= 2 && cond <= 7) {
				return "30587-08.htm";
			} else if (cond == 8) {
				qs.takeItems(ROSHEEKS_LETTER, -1);
				qs.giveItems(GANTAKIS_LETTER, 1);
				qs.setCond(9);
				return "30587-09.htm";
			} else if (cond == 9) {
				return "30587-10.htm";
			} else if (cond >= 10) {
				return "30587-11.htm";
			}
		} else if (npcId == ROSHEEK) {
			if (cond == 1) {
				qs.takeItems(POMEGRANATE, -1);
				qs.giveItems(LEATHER_POUCH_1, 1);
				qs.setCond(2);
				qs.playSound("ItemSound.quest_middle");
				return "30590-01.htm";
			} else if (cond == 2) {
				return "30590-02.htm";
			} else if (cond == 3) {
				qs.takeItems(LEATHER_POUCH_1_FULL, -1);
				qs.giveItems(LEATHER_POUCH_2, 1);
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				return "30590-03.htm";
			} else if (cond == 4) {
				return "30590-04.htm";
			} else if (cond == 5) {
				qs.takeItems(LEATHER_POUCH_2_FULL, -1);
				qs.giveItems(LEATHER_POUCH_3, 1);
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				return "30590-05.htm";
			} else if (cond == 6) {
				return "30590-06.htm";
			} else if (cond == 7) {
				qs.takeItems(LEATHER_POUCH_3_FULL, -1);
				qs.giveItems(SCROLL_FIERY_SPIRIT, 1);
				qs.giveItems(ROSHEEKS_LETTER, 1);
				qs.setCond(8);
				qs.playSound("ItemSound.quest_middle");
				return "30590-07.htm";
			} else if (cond == 8) {
				return "30590-08.htm";
			} else if (cond >= 9) {
				return "30590-09.htm";
			}
		} else if (npcId == KASMAN) {
			if (cond == 9) {
				qs.takeItems(GANTAKIS_LETTER, -1);
				qs.giveItems(FIG, 1);
				qs.setCond(10);
				qs.playSound("ItemSound.quest_middle");
				return "30501-01.htm";
			} else if (cond == 10) {
				return "30501-02.htm";
			} else if (cond == 11 || cond == 12) {
				return "30501-03.htm";
			} else if (cond == 13) {
				qs.takeItems(SCROLL_IRON_WILL, -1);
				qs.takeItems(SCROLL_FIERY_SPIRIT, -1);
				qs.takeItems(TORUKUS_LETTER, -1);
				qs.giveItems(KHAVATARI_TOTEM, 1);
				qs.rewardItems(57, 81900);
				qs.addExpAndSp(295862, 19344);
				qs.playSound("ItemSound.quest_finish");
				qs.exitCurrentQuest(false);
				return "30501-04.htm";
			}
		} else if (npcId == TORUKU) {
			if (cond == 10) {
				qs.takeItems(FIG, -1);
				qs.giveItems(LEATHER_PURSE_4, 1);
				qs.setCond(11);
				qs.playSound("ItemSound.quest_middle");
				return "30591-01.htm";
			} else if (cond == 11) {
				return "30591-02.htm";
			} else if (cond == 12) {
				qs.takeItems(LEATHER_POUCH_4_FULL, -1);
				qs.giveItems(SCROLL_IRON_WILL, 1);
				qs.giveItems(TORUKUS_LETTER, 1);
				qs.setCond(13);
				qs.playSound("ItemSound.quest_middle");
				return "30591-03.htm";
			} else if (cond == 13) {
				return "30591-04.htm";
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

		if (npcId == KASHA_BEAR && (cond == 2 || cond == 1)) {
			if (qs.getQuestItemsCount(LEATHER_POUCH_1) > 0 && qs.getQuestItemsCount(KASHA_BEAR_CLAW) < 5) {
				qs.giveItems(KASHA_BEAR_CLAW, 1);
				if (qs.getQuestItemsCount(KASHA_BEAR_CLAW) >= 5) {
					qs.takeItems(KASHA_BEAR_CLAW, -1);
					qs.takeItems(LEATHER_POUCH_1, -1);
					qs.giveItems(LEATHER_POUCH_1_FULL, 1);
					qs.setCond(3);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == KASHA_BLADE_SPIDER && (cond == 4 || cond == 3)) {
			if (qs.getQuestItemsCount(LEATHER_POUCH_2) > 0 && qs.getQuestItemsCount(KASHA_SPIDER_TALON) < 5) {
				qs.giveItems(KASHA_SPIDER_TALON, 1);
				if (qs.getQuestItemsCount(KASHA_SPIDER_TALON) >= 5) {
					qs.takeItems(KASHA_SPIDER_TALON, -1);
					qs.takeItems(LEATHER_POUCH_2, -1);
					qs.giveItems(LEATHER_POUCH_2_FULL, 1);
					qs.setCond(5);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == SCARLET_SALAMANDER && (cond == 6 || cond == 5)) {
			if (qs.getQuestItemsCount(LEATHER_POUCH_3) > 0 && qs.getQuestItemsCount(SALAMANDER_SCALE) < 5) {
				qs.giveItems(SALAMANDER_SCALE, 1);
				if (qs.getQuestItemsCount(SALAMANDER_SCALE) >= 5) {
					qs.takeItems(SALAMANDER_SCALE, -1);
					qs.takeItems(LEATHER_POUCH_3, -1);
					qs.giveItems(LEATHER_POUCH_3_FULL, 1);
					qs.setCond(7);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 11 && qs.getQuestItemsCount(LEATHER_PURSE_4) > 0) {
			if (npcId == FELIM_LIZARDMAN_WARRIOR && qs.getQuestItemsCount(VUKU_TUSK) < 3) {
				qs.giveItems(VUKU_TUSK, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (npcId == RATMAN_HUNTER && qs.getQuestItemsCount(RATMAN_FANG) < 3) {
				qs.giveItems(RATMAN_FANG, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (npcId == LANGK_LIZARDMAN_WARRIOR && qs.getQuestItemsCount(LANGK_TOOTH) < 3) {
				qs.giveItems(LANGK_TOOTH, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (npcId == FELIM_LIZARDMAN_SCOUT && qs.getQuestItemsCount(FELIM_TOOTH) < 3) {
				qs.giveItems(FELIM_TOOTH, 1);
				qs.playSound("ItemSound.quest_itemget");
			}

			if (qs.getQuestItemsCount(VUKU_TUSK) >= 3
					&& qs.getQuestItemsCount(RATMAN_FANG) >= 3
					&& qs.getQuestItemsCount(LANGK_TOOTH) >= 3
					&& qs.getQuestItemsCount(FELIM_TOOTH) >= 3) {
				qs.takeItems(VUKU_TUSK, -1);
				qs.takeItems(RATMAN_FANG, -1);
				qs.takeItems(LANGK_TOOTH, -1);
				qs.takeItems(FELIM_TOOTH, -1);
				qs.takeItems(LEATHER_PURSE_4, -1);
				qs.giveItems(LEATHER_POUCH_4_FULL, 1);
				qs.setCond(12);
				qs.playSound("ItemSound.quest_middle");
			}
		}

		return null;
	}
}
