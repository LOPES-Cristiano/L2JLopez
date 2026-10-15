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
 * Quest 224: Test of the Sagittarius
 * 3º Passo de 2ª Classe para Hawkeye, Silver Ranger, Phantom Ranger.
 */
@Component
public class Quest224TestOfSagittarius extends Quest {

	public static final int QUEST_ID = 224;
	public static final String QUEST_NAME = "224_TestOfSagittarius";

	// NPCs
	public static final int BERNARD = 30702;
	public static final int HAMIL = 30626;
	public static final int ARON = 30653;
	public static final int VOKIYAN = 30514;
	public static final int GAUEN = 30717;

	// Monstros
	public static final int ANT = 20079;
	public static final int ANT_CAPTAIN = 20080;
	public static final int ANT_OVERSEER = 20081;
	public static final int ANT_RECRUIT = 20082;
	public static final int ANT_PATROL = 20084;
	public static final int ANT_GUARD = 20086;
	public static final int NOBLE_ANT = 20089;
	public static final int NOBLE_ANT_LEADER = 20090;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int MARSH_STAKATO_WORKER = 20230;
	public static final int MARSH_STAKATO_SOLDIER = 20232;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int MARSH_SPIDER = 20233;
	public static final int ROAD_RATMAN = 20551;
	public static final int MANASHEN_GARGOYLE = 20563;
	public static final int LETO_LIZARDMAN = 20577;
	public static final int LETO_LIZARDMAN_ARCHER = 20578;
	public static final int LETO_LIZARDMAN_SOLDIER = 20579;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int SERPENT_DEMON_KADESH = 27090;

	// Itens
	public static final int MARK_OF_SAGITTARIUS = 3293;
	public static final int BERNARDS_INTRODUCTION = 3294;
	public static final int LETTER_OF_HAMIL1 = 3295;
	public static final int LETTER_OF_HAMIL2 = 3296;
	public static final int LETTER_OF_HAMIL3 = 3297;
	public static final int HUNTERS_RUNE1 = 3298;
	public static final int HUNTERS_RUNE2 = 3299;
	public static final int TALISMAN_OF_KADESH = 3300;
	public static final int TALISMAN_OF_SNAKE = 3301;
	public static final int MITHRIL_CLIP = 3302;
	public static final int STAKATO_CHITIN = 3303;
	public static final int REINFORCED_BOWSTRING = 3304;
	public static final int MANASHEN_HORN = 3305;
	public static final int BLOOD_OF_LIZARDMAN = 3306;
	public static final int CRESCENT_MOON_BOW = 3028;
	public static final int WOODEN_ARROW = 17;

	private static final Set<Integer> VALID_CLASSES = Set.of(7, 22, 35); // Rogue, Elven Scout, Assassin

	@Autowired
	public Quest224TestOfSagittarius(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Sagittarius");

		addStartNpc(BERNARD);
		addTalkId(BERNARD, HAMIL, ARON, VOKIYAN, GAUEN);

		addKillId(ANT, ANT_CAPTAIN, ANT_OVERSEER, ANT_RECRUIT, ANT_PATROL, ANT_GUARD,
				NOBLE_ANT, NOBLE_ANT_LEADER, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD,
				MARSH_STAKATO_WORKER, MARSH_STAKATO_SOLDIER, MARSH_STAKATO_DRONE,
				MARSH_SPIDER, ROAD_RATMAN, MANASHEN_GARGOYLE, LETO_LIZARDMAN,
				LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR,
				LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD, SERPENT_DEMON_KADESH);

		registerQuestItems(BERNARDS_INTRODUCTION, LETTER_OF_HAMIL1, LETTER_OF_HAMIL2, LETTER_OF_HAMIL3,
				HUNTERS_RUNE1, HUNTERS_RUNE2, TALISMAN_OF_KADESH, TALISMAN_OF_SNAKE,
				MITHRIL_CLIP, STAKATO_CHITIN, REINFORCED_BOWSTRING, MANASHEN_HORN,
				BLOOD_OF_LIZARDMAN, CRESCENT_MOON_BOW);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30702-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BERNARDS_INTRODUCTION, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30702-04.htm";
		} else if ("30626_1".equalsIgnoreCase(event) || "30626-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BERNARDS_INTRODUCTION, -1);
			qs.giveItems(LETTER_OF_HAMIL1, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30626-03.htm";
		} else if ("30626_2".equalsIgnoreCase(event) || "30626-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(HUNTERS_RUNE1, -1);
			qs.giveItems(LETTER_OF_HAMIL2, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30626-07.htm";
		} else if ("30653_1".equalsIgnoreCase(event) || "30653-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_HAMIL1, -1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30653-02.htm";
		} else if ("30514_1".equalsIgnoreCase(event) || "30514-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_HAMIL2, -1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30514-02.htm";
		} else if ("30626_7".equalsIgnoreCase(event) || "30626-13.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TALISMAN_OF_KADESH)) {
				qs.takeItems(BLOOD_OF_LIZARDMAN, -1);
				qs.takeItems(CRESCENT_MOON_BOW, -1);
				qs.takeItems(TALISMAN_OF_KADESH, -1);
				qs.giveItems(MARK_OF_SAGITTARIUS, 1);
				qs.addExpAndSp(54726, 20250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30626-13.htm";
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

		if (npcId == BERNARD) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30702-02.htm";
				} else {
					return "30702-01.htm";
				}
			} else if (cond >= 1) {
				return "30702-05.htm";
			}
		} else if (npcId == HAMIL) {
			if (cond == 1) {
				return "30626-01.htm";
			} else if (cond == 2 || cond == 3) {
				return "30626-04.htm";
			} else if (cond == 4) {
				return "30626-05.htm";
			} else if (cond >= 5 && cond <= 7) {
				return "30626-08.htm";
			} else if (cond == 8) {
				qs.takeItems(HUNTERS_RUNE2, -1);
				qs.giveItems(LETTER_OF_HAMIL3, 1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30626-09.htm";
			} else if (cond >= 9 && cond <= 11) {
				return "30626-10.htm";
			} else if (cond == 12) {
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30626-11.htm";
			} else if (cond == 13) {
				return "30626-12.htm";
			} else if (cond == 14 && qs.hasQuestItems(TALISMAN_OF_KADESH)) {
				return "30626-12.htm";
			}
		} else if (npcId == ARON) {
			if (cond == 2) {
				return "30653-01.htm";
			} else if (cond > 2) {
				return "30653-03.htm";
			}
		} else if (npcId == VOKIYAN) {
			if (cond == 5) {
				return "30514-01.htm";
			} else if (cond == 6) {
				return "30514-03.htm";
			} else if (cond == 7) {
				qs.takeItems(TALISMAN_OF_SNAKE, -1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30514-04.htm";
			} else if (cond > 7) {
				return "30514-05.htm";
			}
		} else if (npcId == GAUEN) {
			if (cond == 9) {
				qs.takeItems(LETTER_OF_HAMIL3, -1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30717-01.htm";
			} else if (cond == 10) {
				return "30717-03.htm";
			} else if (cond == 11) {
				qs.takeItems(MANASHEN_HORN, -1);
				qs.takeItems(MITHRIL_CLIP, -1);
				qs.takeItems(REINFORCED_BOWSTRING, -1);
				qs.takeItems(STAKATO_CHITIN, -1);
				qs.giveItems(CRESCENT_MOON_BOW, 1);
				qs.giveItems(WOODEN_ARROW, 10);
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30717-02.htm";
			} else if (cond > 11) {
				return "30717-04.htm";
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

		switch (npcId) {
			case ANT, ANT_CAPTAIN, ANT_OVERSEER, ANT_RECRUIT, ANT_PATROL, ANT_GUARD, NOBLE_ANT, NOBLE_ANT_LEADER -> {
				if (cond == 3 && qs.getQuestItemsCount(HUNTERS_RUNE1) < 10) {
					qs.giveItems(HUNTERS_RUNE1, 1);
					if (qs.getQuestItemsCount(HUNTERS_RUNE1) >= 10) {
						qs.setCond(4);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD -> {
				if (cond == 6 && qs.getQuestItemsCount(HUNTERS_RUNE2) < 10) {
					qs.giveItems(HUNTERS_RUNE2, 1);
					if (qs.getQuestItemsCount(HUNTERS_RUNE2) >= 10) {
						qs.giveItems(TALISMAN_OF_SNAKE, 1);
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case MARSH_STAKATO_WORKER, MARSH_STAKATO_SOLDIER, MARSH_STAKATO_DRONE -> {
				if (cond == 10 && !qs.hasQuestItems(STAKATO_CHITIN)) {
					qs.giveItems(STAKATO_CHITIN, 1);
					if (qs.hasQuestItems(MANASHEN_HORN) && qs.hasQuestItems(MITHRIL_CLIP) && qs.hasQuestItems(REINFORCED_BOWSTRING)) {
						qs.setCond(11);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case MARSH_SPIDER -> {
				if (cond == 10 && !qs.hasQuestItems(REINFORCED_BOWSTRING)) {
					qs.giveItems(REINFORCED_BOWSTRING, 1);
					if (qs.hasQuestItems(MANASHEN_HORN) && qs.hasQuestItems(MITHRIL_CLIP) && qs.hasQuestItems(STAKATO_CHITIN)) {
						qs.setCond(11);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case ROAD_RATMAN -> {
				if (cond == 10 && !qs.hasQuestItems(MITHRIL_CLIP)) {
					qs.giveItems(MITHRIL_CLIP, 1);
					if (qs.hasQuestItems(MANASHEN_HORN) && qs.hasQuestItems(REINFORCED_BOWSTRING) && qs.hasQuestItems(STAKATO_CHITIN)) {
						qs.setCond(11);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case MANASHEN_GARGOYLE -> {
				if (cond == 10 && !qs.hasQuestItems(MANASHEN_HORN)) {
					qs.giveItems(MANASHEN_HORN, 1);
					if (qs.hasQuestItems(REINFORCED_BOWSTRING) && qs.hasQuestItems(MITHRIL_CLIP) && qs.hasQuestItems(STAKATO_CHITIN)) {
						qs.setCond(11);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR,
					LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD -> {
				if (cond == 13) {
					if (((qs.getQuestItemsCount(BLOOD_OF_LIZARDMAN) - 120) * 5) > ThreadLocalRandom.current().nextInt(100)) {
						qs.takeItems(BLOOD_OF_LIZARDMAN, -1);
						qs.addSpawn(SERPENT_DEMON_KADESH);
						qs.playSound(QuestState.SOUND_BEFORE_BATTLE);
					} else {
						qs.giveItems(BLOOD_OF_LIZARDMAN, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case SERPENT_DEMON_KADESH -> {
				if (cond == 13 && !qs.hasQuestItems(TALISMAN_OF_KADESH)) {
					qs.giveItems(TALISMAN_OF_KADESH, 1);
					qs.setCond(14);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}
