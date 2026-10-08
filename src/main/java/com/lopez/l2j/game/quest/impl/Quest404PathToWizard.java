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
 * Quest 404: Path to Human Wizard (1ª Troca de Classe do Human Mystic para Human Wizard).
 */
@Component
public class Quest404PathToWizard extends Quest {

	public static final int QUEST_ID = 404;
	public static final String QUEST_NAME = "404_PathToWizard";

	// NPCs
	public static final int PARINA = 30391;
	public static final int EARTH_SNAKE = 30409;
	public static final int WASTELAND_LIZARDMAN = 30410;
	public static final int FLAME_SALAMANDER = 30411;
	public static final int WIND_SYLPH = 30412;
	public static final int WATER_UNDINE = 30413;

	// Monstros
	public static final int RED_SCAVENGER_ANT = 20079;
	public static final int RATMAN_WARRIOR = 20359;
	public static final int FELIM_LIZARDMAN_WARRIOR = 20014;
	public static final int WATER_SEER = 27030;

	// Itens
	public static final int MAP_OF_LUSTER = 1280;
	public static final int KEY_OF_FLAME = 1281;
	public static final int FLAME_EARING = 1282;
	public static final int BROKEN_BRONZE_MIRROR = 1283;
	public static final int WIND_FEATHER = 1284;
	public static final int WIND_BANGEL = 1285;
	public static final int RAMAS_DIARY = 1286;
	public static final int SPARKLE_PEBBLE = 1287;
	public static final int WATER_NECKLACE = 1288;
	public static final int RUST_GOLD_COIN = 1289;
	public static final int RED_SOIL = 1290;
	public static final int EARTH_RING = 1291;
	public static final int BEAD_OF_SEASON = 1292;

	@Autowired
	public Quest404PathToWizard(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Human Wizard");

		addStartNpc(PARINA);
		addTalkId(PARINA);
		addTalkId(EARTH_SNAKE);
		addTalkId(WASTELAND_LIZARDMAN);
		addTalkId(FLAME_SALAMANDER);
		addTalkId(WIND_SYLPH);
		addTalkId(WATER_UNDINE);

		addKillId(RED_SCAVENGER_ANT);
		addKillId(RATMAN_WARRIOR);
		addKillId(FELIM_LIZARDMAN_WARRIOR);
		addKillId(WATER_SEER);

		registerQuestItems(MAP_OF_LUSTER, KEY_OF_FLAME, FLAME_EARING, BROKEN_BRONZE_MIRROR,
				WIND_FEATHER, WIND_BANGEL, RAMAS_DIARY, SPARKLE_PEBBLE, WATER_NECKLACE,
				RUST_GOLD_COIN, RED_SOIL, EARTH_RING);

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
			if (classId == 0x0a) {
				if (level >= 18) {
					if (qs.getQuestItemsCount(BEAD_OF_SEASON) > 0) {
						return "30391-03.htm";
					} else {
						qs.setCond(1);
						qs.setState(State.STARTED);
						qs.playSound("ItemSound.quest_accept");
						return "30391-08.htm";
					}
				} else {
					return "30391-02.htm";
				}
			} else {
				return classId == 0x0b ? "30391-02a.htm" : "30391-01.htm";
			}
		} else if ("30410_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(WIND_FEATHER) == 0) {
				qs.giveItems(WIND_FEATHER, 1);
				qs.setCond(6);
				return "30410-03.htm";
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

		if (npcId == PARINA) {
			if (cond == 0) {
				return "30391-04.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(FLAME_EARING) > 0
						&& qs.getQuestItemsCount(WIND_BANGEL) > 0
						&& qs.getQuestItemsCount(WATER_NECKLACE) > 0
						&& qs.getQuestItemsCount(EARTH_RING) > 0) {
					qs.takeItems(FLAME_EARING, -1);
					qs.takeItems(WIND_BANGEL, -1);
					qs.takeItems(WATER_NECKLACE, -1);
					qs.takeItems(EARTH_RING, -1);
					qs.rewardItems(57, 81900);
					qs.giveItems(BEAD_OF_SEASON, 1);
					qs.addExpAndSp(295862, 20794);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30391-06.htm";
				} else {
					return "30391-05.htm";
				}
			}
		} else if (npcId == FLAME_SALAMANDER && cond > 0) {
			if (qs.getQuestItemsCount(MAP_OF_LUSTER) == 0 && qs.getQuestItemsCount(FLAME_EARING) == 0) {
				qs.giveItems(MAP_OF_LUSTER, 1);
				qs.setCond(2);
				return "30411-01.htm";
			} else if (qs.getQuestItemsCount(MAP_OF_LUSTER) > 0 && qs.getQuestItemsCount(KEY_OF_FLAME) == 0) {
				return "30411-02.htm";
			} else if (qs.getQuestItemsCount(MAP_OF_LUSTER) > 0 && qs.getQuestItemsCount(KEY_OF_FLAME) > 0) {
				qs.takeItems(KEY_OF_FLAME, -1);
				qs.takeItems(MAP_OF_LUSTER, -1);
				qs.giveItems(FLAME_EARING, 1);
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				return "30411-03.htm";
			} else if (qs.getQuestItemsCount(FLAME_EARING) > 0) {
				return "30411-04.htm";
			}
		} else if (npcId == WIND_SYLPH && cond > 0) {
			if (qs.getQuestItemsCount(BROKEN_BRONZE_MIRROR) == 0 && qs.getQuestItemsCount(WIND_BANGEL) == 0) {
				qs.giveItems(BROKEN_BRONZE_MIRROR, 1);
				qs.setCond(5);
				return "30412-01.htm";
			} else if (qs.getQuestItemsCount(BROKEN_BRONZE_MIRROR) > 0 && qs.getQuestItemsCount(WIND_FEATHER) == 0) {
				return "30412-02.htm";
			} else if (qs.getQuestItemsCount(BROKEN_BRONZE_MIRROR) > 0 && qs.getQuestItemsCount(WIND_FEATHER) > 0) {
				qs.takeItems(WIND_FEATHER, -1);
				qs.takeItems(BROKEN_BRONZE_MIRROR, -1);
				qs.giveItems(WIND_BANGEL, 1);
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				return "30412-03.htm";
			} else if (qs.getQuestItemsCount(WIND_BANGEL) > 0) {
				return "30412-04.htm";
			}
		} else if (npcId == WASTELAND_LIZARDMAN && cond > 0) {
			if (qs.getQuestItemsCount(BROKEN_BRONZE_MIRROR) > 0 && qs.getQuestItemsCount(WIND_FEATHER) == 0) {
				return "30410-01.htm";
			} else if (qs.getQuestItemsCount(WIND_FEATHER) > 0) {
				return "30410-04.htm";
			}
		} else if (npcId == WATER_UNDINE && cond > 0) {
			if (qs.getQuestItemsCount(RAMAS_DIARY) == 0 && qs.getQuestItemsCount(WATER_NECKLACE) == 0) {
				qs.giveItems(RAMAS_DIARY, 1);
				qs.setCond(8);
				return "30413-01.htm";
			} else if (qs.getQuestItemsCount(RAMAS_DIARY) > 0 && qs.getQuestItemsCount(SPARKLE_PEBBLE) < 2) {
				return "30413-02.htm";
			} else if (qs.getQuestItemsCount(RAMAS_DIARY) > 0 && qs.getQuestItemsCount(SPARKLE_PEBBLE) >= 2) {
				qs.takeItems(SPARKLE_PEBBLE, -1);
				qs.takeItems(RAMAS_DIARY, -1);
				qs.giveItems(WATER_NECKLACE, 1);
				qs.setCond(10);
				qs.playSound("ItemSound.quest_middle");
				return "30413-03.htm";
			} else if (qs.getQuestItemsCount(WATER_NECKLACE) > 0) {
				return "30413-04.htm";
			}
		} else if (npcId == EARTH_SNAKE && cond > 0) {
			if (qs.getQuestItemsCount(RUST_GOLD_COIN) == 0 && qs.getQuestItemsCount(EARTH_RING) == 0) {
				qs.giveItems(RUST_GOLD_COIN, 1);
				qs.setCond(11);
				return "30409-01.htm";
			} else if (qs.getQuestItemsCount(RUST_GOLD_COIN) > 0 && qs.getQuestItemsCount(RED_SOIL) == 0) {
				return "30409-02.htm";
			} else if (qs.getQuestItemsCount(RUST_GOLD_COIN) > 0 && qs.getQuestItemsCount(RED_SOIL) > 0) {
				qs.takeItems(RED_SOIL, -1);
				qs.takeItems(RUST_GOLD_COIN, -1);
				qs.giveItems(EARTH_RING, 1);
				qs.setCond(13);
				qs.playSound("ItemSound.quest_middle");
				return "30409-03.htm";
			} else if (qs.getQuestItemsCount(EARTH_RING) > 0) {
				return "30409-04.htm";
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

		if (npcId == RATMAN_WARRIOR && cond == 2 && qs.getQuestItemsCount(MAP_OF_LUSTER) > 0 && qs.getQuestItemsCount(KEY_OF_FLAME) == 0) {
			qs.giveItems(KEY_OF_FLAME, 1);
			qs.playSound("ItemSound.quest_middle");
			qs.setCond(3);
		} else if (npcId == WATER_SEER && cond == 8 && qs.getQuestItemsCount(RAMAS_DIARY) > 0 && qs.getQuestItemsCount(SPARKLE_PEBBLE) < 2) {
			qs.giveItems(SPARKLE_PEBBLE, 1);
			if (qs.getQuestItemsCount(SPARKLE_PEBBLE) >= 2) {
				qs.playSound("ItemSound.quest_middle");
				qs.setCond(9);
			} else {
				qs.playSound("ItemSound.quest_itemget");
			}
		} else if (npcId == RED_SCAVENGER_ANT && cond == 11 && qs.getQuestItemsCount(RUST_GOLD_COIN) > 0 && qs.getQuestItemsCount(RED_SOIL) == 0) {
			qs.giveItems(RED_SOIL, 1);
			qs.playSound("ItemSound.quest_middle");
			qs.setCond(12);
		}
		return null;
	}
}
