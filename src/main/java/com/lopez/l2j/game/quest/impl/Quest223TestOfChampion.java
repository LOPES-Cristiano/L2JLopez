package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 223: Test of the Champion
 * 3º Passo de 2ª Classe para Warlord e Destroyer.
 */
@Component
public class Quest223TestOfChampion extends Quest {

	public static final int QUEST_ID = 223;
	public static final String QUEST_NAME = "223_TestOfChampion";

	// NPCs
	public static final int ASCALON = 30624;
	public static final int MASON = 30625;
	public static final int GROOT = 30093;
	public static final int MOUEN = 30196;

	// Monstros
	public static final int HARPY = 20145;
	public static final int HARPY_MATRIARCH = 27088;
	public static final int MEDUSA = 20158;
	public static final int WINDSUS = 20553;
	public static final int ROAD_COLLECTOR = 27089;
	public static final int ROAD_RATMAN = 20551;
	public static final int LETO_LIZARDMAN = 20577;
	public static final int LETO_LIZARDMAN_ARCHER = 20578;
	public static final int LETO_LIZARDMAN_SOLDIER = 20579;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int BLOODY_AXE_ELITE = 20780;

	// Itens
	public static final int MARK_OF_CHAMPION = 3276;
	public static final int ASCALONS_LETTER1 = 3277;
	public static final int MASONS_LETTER = 3278;
	public static final int IRON_ROSE_RING = 3279;
	public static final int ASCALONS_LETTER2 = 3280;
	public static final int WHITE_ROSE_INSIGNIA = 3281;
	public static final int GROOTS_LETTER = 3282;
	public static final int ASCALONS_LETTER3 = 3283;
	public static final int MOUENS_ORDER1 = 3284;
	public static final int MOUENS_ORDER2 = 3285;
	public static final int MOUENS_LETTER = 3286;
	public static final int HARPYS_EGG = 3287;
	public static final int MEDUSA_VENOM = 3288;
	public static final int WINDSUS_BILE = 3289;
	public static final int BLOODY_AXE_HEAD = 3290;
	public static final int ROAD_RATMAN_HEAD = 3291;
	public static final int LETO_LIZARDMAN_FANG = 3292;

	private static final Set<Integer> VALID_CLASSES = Set.of(1, 45); // Warrior, Orc Raider

	@Autowired
	public Quest223TestOfChampion(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Champion");

		addStartNpc(ASCALON);
		addTalkId(ASCALON, MASON, GROOT, MOUEN);

		addAttackId(HARPY, ROAD_RATMAN);
		addKillId(HARPY, HARPY_MATRIARCH, MEDUSA, WINDSUS, ROAD_COLLECTOR, ROAD_RATMAN,
				LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER,
				LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD,
				BLOODY_AXE_ELITE);

		registerQuestItems(ASCALONS_LETTER1, MASONS_LETTER, IRON_ROSE_RING, ASCALONS_LETTER2,
				WHITE_ROSE_INSIGNIA, GROOTS_LETTER, ASCALONS_LETTER3, MOUENS_ORDER1,
				MOUENS_ORDER2, MOUENS_LETTER, HARPYS_EGG, MEDUSA_VENOM, WINDSUS_BILE,
				BLOODY_AXE_HEAD, ROAD_RATMAN_HEAD, LETO_LIZARDMAN_FANG);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30624-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ASCALONS_LETTER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30624-06.htm";
		} else if ("30624_2".equalsIgnoreCase(event) || "30624-10.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MASONS_LETTER, -1);
			qs.giveItems(ASCALONS_LETTER2, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30624-10.htm";
		} else if ("30624_3".equalsIgnoreCase(event) || "30624-14.htm".equalsIgnoreCase(event)) {
			qs.takeItems(GROOTS_LETTER, -1);
			qs.giveItems(ASCALONS_LETTER3, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30624-14.htm";
		} else if ("30625_2".equalsIgnoreCase(event) || "30625-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ASCALONS_LETTER1, -1);
			qs.giveItems(IRON_ROSE_RING, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30625-03.htm";
		} else if ("30093_1".equalsIgnoreCase(event) || "30093-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ASCALONS_LETTER2, -1);
			qs.giveItems(WHITE_ROSE_INSIGNIA, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30093-02.htm";
		} else if ("30196_2".equalsIgnoreCase(event) || "30196-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ASCALONS_LETTER3, -1);
			qs.giveItems(MOUENS_ORDER1, 1);
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30196-03.htm";
		} else if ("30196_3".equalsIgnoreCase(event) || "30196-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MOUENS_ORDER1, -1);
			qs.takeItems(ROAD_RATMAN_HEAD, -1);
			qs.giveItems(MOUENS_ORDER2, 1);
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30196-06.htm";
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

		if (npcId == ASCALON) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30624-03.htm";
				} else {
					return "30624-01.htm";
				}
			} else if (cond == 1) {
				return "30624-07.htm";
			} else if (cond == 2 || cond == 3) {
				return "30624-08.htm";
			} else if (cond == 4 && qs.hasQuestItems(MASONS_LETTER)) {
				return "30624-09.htm";
			} else if (cond == 5) {
				return "30624-11.htm";
			} else if (cond == 6 || cond == 7) {
				return "30624-12.htm";
			} else if (cond == 8 && qs.hasQuestItems(GROOTS_LETTER)) {
				return "30624-13.htm";
			} else if (cond == 9) {
				return "30624-15.htm";
			} else if (cond >= 10 && cond <= 13) {
				return "30624-16.htm";
			} else if (cond == 14 && qs.hasQuestItems(MOUENS_LETTER)) {
				qs.takeItems(MOUENS_LETTER, -1);
				qs.giveItems(MARK_OF_CHAMPION, 1);
				qs.addExpAndSp(117454, 7700);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30624-17.htm";
			}
		} else if (npcId == MASON) {
			if (cond == 1) {
				return "30625-01.htm";
			} else if (cond == 2) {
				return "30625-04.htm";
			} else if (cond == 3 && qs.getQuestItemsCount(BLOODY_AXE_HEAD) >= 100) {
				qs.takeItems(BLOODY_AXE_HEAD, -1);
				qs.takeItems(IRON_ROSE_RING, -1);
				qs.giveItems(MASONS_LETTER, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30625-04.htm";
			} else if (cond == 4) {
				return "30625-06.htm";
			} else if (cond > 4) {
				return "30625-07.htm";
			}
		} else if (npcId == GROOT) {
			if (cond == 5) {
				return "30093-01.htm";
			} else if (cond == 6) {
				return "30093-03.htm";
			} else if (cond == 7 && qs.getQuestItemsCount(HARPYS_EGG) >= 30
					&& qs.getQuestItemsCount(MEDUSA_VENOM) >= 30
					&& qs.getQuestItemsCount(WINDSUS_BILE) >= 30) {
				qs.takeItems(HARPYS_EGG, -1);
				qs.takeItems(MEDUSA_VENOM, -1);
				qs.takeItems(WINDSUS_BILE, -1);
				qs.takeItems(WHITE_ROSE_INSIGNIA, -1);
				qs.giveItems(GROOTS_LETTER, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30093-04.htm";
			} else if (cond == 8) {
				return "30093-05.htm";
			} else if (cond > 8) {
				return "30093-06.htm";
			}
		} else if (npcId == MOUEN) {
			if (cond == 9) {
				return "30196-01.htm";
			} else if (cond == 10) {
				return "30196-04.htm";
			} else if (cond == 11) {
				return "30196-05.htm";
			} else if (cond == 12) {
				return "30196-07.htm";
			} else if (cond == 13 && qs.getQuestItemsCount(LETO_LIZARDMAN_FANG) >= 100) {
				qs.takeItems(LETO_LIZARDMAN_FANG, -1);
				qs.takeItems(MOUENS_ORDER2, -1);
				qs.giveItems(MOUENS_LETTER, 1);
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30196-06.htm";
			} else if (cond > 13) {
				return "30196-09.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == HARPY && cond == 6 && npc.isScriptValue(0)) {
			npc.setScriptValue(1);
			if (ThreadLocalRandom.current().nextBoolean()) {
				qs.addSpawn(HARPY_MATRIARCH, 180000);
			}
		} else if (npcId == ROAD_RATMAN && cond == 10 && npc.isScriptValue(0)) {
			npc.setScriptValue(1);
			if (ThreadLocalRandom.current().nextBoolean()) {
				qs.addSpawn(ROAD_COLLECTOR, 180000);
			}
		}

		return null;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 2 && npcId == BLOODY_AXE_ELITE && qs.getQuestItemsCount(BLOODY_AXE_HEAD) < 100) {
			qs.giveItems(BLOODY_AXE_HEAD, 1);
			if (qs.getQuestItemsCount(BLOODY_AXE_HEAD) >= 100) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 6) {
			if ((npcId == HARPY || npcId == HARPY_MATRIARCH) && qs.getQuestItemsCount(HARPYS_EGG) < 30) {
				qs.giveItems(HARPYS_EGG, 1);
				if (qs.getQuestItemsCount(HARPYS_EGG) >= 30
						&& qs.getQuestItemsCount(MEDUSA_VENOM) >= 30
						&& qs.getQuestItemsCount(WINDSUS_BILE) >= 30) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			} else if (npcId == MEDUSA && qs.getQuestItemsCount(MEDUSA_VENOM) < 30) {
				qs.giveItems(MEDUSA_VENOM, 1);
				if (qs.getQuestItemsCount(HARPYS_EGG) >= 30
						&& qs.getQuestItemsCount(MEDUSA_VENOM) >= 30
						&& qs.getQuestItemsCount(WINDSUS_BILE) >= 30) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			} else if (npcId == WINDSUS && qs.getQuestItemsCount(WINDSUS_BILE) < 30) {
				qs.giveItems(WINDSUS_BILE, 1);
				if (qs.getQuestItemsCount(HARPYS_EGG) >= 30
						&& qs.getQuestItemsCount(MEDUSA_VENOM) >= 30
						&& qs.getQuestItemsCount(WINDSUS_BILE) >= 30) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 10 && (npcId == ROAD_RATMAN || npcId == ROAD_COLLECTOR)
				&& qs.getQuestItemsCount(ROAD_RATMAN_HEAD) < 100) {
			qs.giveItems(ROAD_RATMAN_HEAD, 1);
			if (qs.getQuestItemsCount(ROAD_RATMAN_HEAD) >= 100) {
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 12 && (npcId == LETO_LIZARDMAN || npcId == LETO_LIZARDMAN_ARCHER
				|| npcId == LETO_LIZARDMAN_SOLDIER || npcId == LETO_LIZARDMAN_WARRIOR
				|| npcId == LETO_LIZARDMAN_SHAMAN || npcId == LETO_LIZARDMAN_OVERLORD)
				&& qs.getQuestItemsCount(LETO_LIZARDMAN_FANG) < 100) {
			qs.giveItems(LETO_LIZARDMAN_FANG, 1);
			if (qs.getQuestItemsCount(LETO_LIZARDMAN_FANG) >= 100) {
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
