package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 232: Test of the Lord
 * 3º Passo de 2ª Classe para Overlord.
 */
@Component
public class Quest232TestOfLord extends Quest {

	public static final int QUEST_ID = 232;
	public static final String QUEST_NAME = "232_TestOfLord";

	// NPCs
	public static final int SOMAK = 30510;
	public static final int MANAKIA = 30515;
	public static final int JAKAL = 30558;
	public static final int SUMARI = 30564;
	public static final int KAKAI = 30565;
	public static final int VARKEES = 30566;
	public static final int TANTUS = 30567;
	public static final int HATOS = 30568;
	public static final int TAKUNA = 30641;
	public static final int CHIANTA = 30642;
	public static final int FIRST_ORC = 30643;
	public static final int MARTANKUS = 30649;

	// Monstros
	public static final int MARSH_SPIDER = 20233;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int ENCHANTED_MONSTEREYE = 20564;
	public static final int TIMAK_ORC = 20583;
	public static final int TIMAK_ORC_ARCHER = 20584;
	public static final int TIMAK_ORC_SOLDIER = 20585;
	public static final int TIMAK_ORC_WARRIOR = 20586;
	public static final int TIMAK_ORC_SHAMAN = 20587;
	public static final int TIMAK_ORC_OVERLORD = 20588;
	public static final int RAGNA_ORC_OVERLORD = 20778;
	public static final int RAGNA_ORC_SEER = 20779;

	// Itens
	public static final int ADENA = 57;
	public static final int BONE_ARROW = 1341;
	public static final int MARK_OF_LORD = 3390;
	public static final int ORDEAL_NECKLACE = 3391;
	public static final int VARKEES_CHARM = 3392;
	public static final int TANTUS_CHARM = 3393;
	public static final int HATOS_CHARM = 3394;
	public static final int TAKUNA_CHARM = 3395;
	public static final int CHIANTA_CHARM = 3396;
	public static final int MANAKIAS_ORDERS = 3397;
	public static final int BREKA_ORC_FANG = 3398;
	public static final int MANAKIAS_AMULET = 3399;
	public static final int HUGE_ORC_FANG = 3400;
	public static final int SUMARIS_LETTER = 3401;
	public static final int URUTU_BLADE = 3402;
	public static final int TIMAK_ORC_SKULL = 3403;
	public static final int SWORD_INTO_SKULL = 3404;
	public static final int NERUGA_AXE_BLADE = 3405;
	public static final int AXE_OF_CEREMONY = 3406;
	public static final int MARSH_SPIDER_FEELER = 3407;
	public static final int MARSH_SPIDER_FEET = 3408;
	public static final int HANDIWORK_SPIDER_BROOCH = 3409;
	public static final int MONSTEREYE_CORNEA = 3410;
	public static final int MONSTEREYE_WOODCARVING = 3411;
	public static final int BEAR_FANG_NECKLACE = 3412;
	public static final int MARTANKUS_CHARM = 3413;
	public static final int RAGNA_ORC_HEAD = 3414;
	public static final int RAGNA_CHIEF_NOTICE = 3415;
	public static final int IMMORTAL_FLAME = 3416;

	private static final Set<Integer> VALID_CLASSES = Set.of(50); // Orc Shaman

	@Autowired
	public Quest232TestOfLord(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Lord");

		addStartNpc(KAKAI);
		addTalkId(KAKAI, CHIANTA, HATOS, SOMAK, SUMARI, TAKUNA, TANTUS, JAKAL, VARKEES, MANAKIA, MARTANKUS, FIRST_ORC);

		addKillId(MARSH_SPIDER, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD, ENCHANTED_MONSTEREYE,
				TIMAK_ORC, TIMAK_ORC_ARCHER, TIMAK_ORC_SOLDIER, TIMAK_ORC_WARRIOR,
				TIMAK_ORC_SHAMAN, TIMAK_ORC_OVERLORD, RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER);

		registerQuestItems(ORDEAL_NECKLACE, VARKEES_CHARM, TANTUS_CHARM, HATOS_CHARM, TAKUNA_CHARM,
				CHIANTA_CHARM, MANAKIAS_ORDERS, BREKA_ORC_FANG, MANAKIAS_AMULET, HUGE_ORC_FANG,
				SUMARIS_LETTER, URUTU_BLADE, TIMAK_ORC_SKULL, SWORD_INTO_SKULL, NERUGA_AXE_BLADE,
				AXE_OF_CEREMONY, MARSH_SPIDER_FEELER, MARSH_SPIDER_FEET, HANDIWORK_SPIDER_BROOCH,
				MONSTEREYE_CORNEA, MONSTEREYE_WOODCARVING, BEAR_FANG_NECKLACE, MARTANKUS_CHARM,
				RAGNA_ORC_HEAD, IMMORTAL_FLAME);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30565-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ORDEAL_NECKLACE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30565-05.htm";
		} else if ("30565-08.htm".equalsIgnoreCase(event) || "30565_1".equalsIgnoreCase(event)) {
			qs.takeItems(SWORD_INTO_SKULL, -1);
			qs.takeItems(AXE_OF_CEREMONY, -1);
			qs.takeItems(MONSTEREYE_WOODCARVING, -1);
			qs.takeItems(HANDIWORK_SPIDER_BROOCH, -1);
			qs.takeItems(ORDEAL_NECKLACE, -1);
			qs.takeItems(HUGE_ORC_FANG, -1);
			qs.giveItems(BEAR_FANG_NECKLACE, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30565-08.htm";
		} else if ("30566-02.htm".equalsIgnoreCase(event) || "30566_1".equalsIgnoreCase(event)) {
			qs.giveItems(VARKEES_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30566-02.htm";
		} else if ("30567-02.htm".equalsIgnoreCase(event) || "30567_1".equalsIgnoreCase(event)) {
			qs.giveItems(TANTUS_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30567-02.htm";
		} else if ("30558-02.htm".equalsIgnoreCase(event) || "30558_1".equalsIgnoreCase(event)) {
			qs.takeItems(ADENA, 1000);
			qs.giveItems(NERUGA_AXE_BLADE, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30558-02.htm";
		} else if ("30568-02.htm".equalsIgnoreCase(event) || "30568_1".equalsIgnoreCase(event)) {
			qs.giveItems(HATOS_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30568-02.htm";
		} else if ("30641-02.htm".equalsIgnoreCase(event) || "30641_1".equalsIgnoreCase(event)) {
			qs.giveItems(TAKUNA_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30641-02.htm";
		} else if ("30642-02.htm".equalsIgnoreCase(event) || "30642_1".equalsIgnoreCase(event)) {
			qs.giveItems(CHIANTA_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30642-02.htm";
		} else if ("30643-02.htm".equalsIgnoreCase(event) || "30643_1".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30643-02.htm";
		} else if ("30649-04.htm".equalsIgnoreCase(event) || "30649_1".equalsIgnoreCase(event)) {
			qs.takeItems(BEAR_FANG_NECKLACE, -1);
			qs.giveItems(MARTANKUS_CHARM, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30649-04.htm";
		} else if ("30649-07.htm".equalsIgnoreCase(event) || "30649_2".equalsIgnoreCase(event)) {
			qs.addSpawn(FIRST_ORC, 21036, -107690, -3038, 200000);
			return "30649-07.htm";
		} else if ("30565_2".equalsIgnoreCase(event) || "30565-11.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(IMMORTAL_FLAME)) {
				qs.takeItems(IMMORTAL_FLAME, -1);
				qs.giveItems(MARK_OF_LORD, 1);
				qs.addExpAndSp(115000, 12000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30565-11.htm";
			}
		}

		return event;
	}

	private void checkPhaseOneComplete(QuestState qs) {
		if (qs.hasQuestItems(SWORD_INTO_SKULL) && qs.hasQuestItems(AXE_OF_CEREMONY)
				&& qs.hasQuestItems(MONSTEREYE_WOODCARVING) && qs.hasQuestItems(HANDIWORK_SPIDER_BROOCH)
				&& qs.hasQuestItems(ORDEAL_NECKLACE) && qs.hasQuestItems(HUGE_ORC_FANG)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == KAKAI) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30565-01.htm";
				} else {
					return "30565-02.htm";
				}
			} else if (cond >= 2 && qs.hasQuestItems(IMMORTAL_FLAME)) {
				return "30565-10.htm";
			} else if (cond == 1) {
				return "30565-06.htm";
			} else if (cond == 2) {
				return "30565-07.htm";
			} else if (cond == 3) {
				return "30565-09.htm";
			} else if (cond >= 4 && cond <= 6) {
				return "30565-10.htm";
			} else if (cond == 7) {
				return "30565-10.htm";
			}
		} else if (npcId == VARKEES) {
			if (qs.hasQuestItems(HUGE_ORC_FANG)) {
				return "30566-05.htm";
			} else if (qs.hasQuestItems(VARKEES_CHARM)) {
				if (qs.hasQuestItems(MANAKIAS_AMULET)) {
					qs.takeItems(VARKEES_CHARM, -1);
					qs.takeItems(MANAKIAS_AMULET, -1);
					qs.giveItems(HUGE_ORC_FANG, 1);
					checkPhaseOneComplete(qs);
					return "30566-04.htm";
				}
				return "30566-03.htm";
			}
			return "30566-01.htm";
		} else if (npcId == MANAKIA) {
			if (qs.hasQuestItems(HUGE_ORC_FANG)) {
				return "30515-05.htm";
			} else if (qs.hasQuestItems(MANAKIAS_AMULET)) {
				return "30515-04.htm";
			} else if (qs.hasQuestItems(MANAKIAS_ORDERS)) {
				if (qs.getQuestItemsCount(BREKA_ORC_FANG) >= 20) {
					qs.takeItems(MANAKIAS_ORDERS, -1);
					qs.takeItems(BREKA_ORC_FANG, -1);
					qs.giveItems(MANAKIAS_AMULET, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30515-03.htm";
				}
				return "30515-02.htm";
			}
			qs.giveItems(MANAKIAS_ORDERS, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30515-01.htm";
		} else if (npcId == TANTUS) {
			if (qs.hasQuestItems(AXE_OF_CEREMONY)) {
				return "30567-05.htm";
			} else if (qs.hasQuestItems(TANTUS_CHARM)) {
				if (qs.getQuestItemsCount(BONE_ARROW) >= 1000 && qs.hasQuestItems(NERUGA_AXE_BLADE)) {
					qs.takeItems(BONE_ARROW, 1000);
					qs.takeItems(NERUGA_AXE_BLADE, -1);
					qs.takeItems(TANTUS_CHARM, -1);
					qs.giveItems(AXE_OF_CEREMONY, 1);
					checkPhaseOneComplete(qs);
					return "30567-04.htm";
				}
				return "30567-03.htm";
			}
			return "30567-01.htm";
		} else if (npcId == JAKAL) {
			if (qs.hasQuestItems(AXE_OF_CEREMONY)) {
				return "30558-05.htm";
			} else if (qs.hasQuestItems(NERUGA_AXE_BLADE)) {
				return "30558-04.htm";
			} else if (qs.hasQuestItems(TANTUS_CHARM)) {
				return qs.getQuestItemsCount(ADENA) >= 1000 ? "30558-01.htm" : "30558-03.htm";
			}
		} else if (npcId == HATOS) {
			if (qs.hasQuestItems(SWORD_INTO_SKULL)) {
				return "30568-05.htm";
			} else if (qs.hasQuestItems(HATOS_CHARM)) {
				if (qs.hasQuestItems(URUTU_BLADE) && qs.getQuestItemsCount(TIMAK_ORC_SKULL) >= 10) {
					qs.takeItems(HATOS_CHARM, -1);
					qs.takeItems(URUTU_BLADE, -1);
					qs.takeItems(TIMAK_ORC_SKULL, -1);
					qs.giveItems(SWORD_INTO_SKULL, 1);
					checkPhaseOneComplete(qs);
					return "30568-04.htm";
				}
				return "30568-03.htm";
			}
			return "30568-01.htm";
		} else if (npcId == SUMARI) {
			if (qs.hasQuestItems(URUTU_BLADE)) {
				return "30564-03.htm";
			} else if (qs.hasQuestItems(SUMARIS_LETTER)) {
				return "30564-02.htm";
			} else if (qs.hasQuestItems(HATOS_CHARM)) {
				qs.giveItems(SUMARIS_LETTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30564-01.htm";
			}
		} else if (npcId == SOMAK) {
			if (qs.hasQuestItems(SWORD_INTO_SKULL)) {
				return "30510-03.htm";
			} else if (qs.hasQuestItems(URUTU_BLADE)) {
				return "30510-02.htm";
			} else if (qs.hasQuestItems(SUMARIS_LETTER)) {
				qs.takeItems(SUMARIS_LETTER, -1);
				qs.giveItems(URUTU_BLADE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30510-01.htm";
			}
		} else if (npcId == TAKUNA) {
			if (qs.hasQuestItems(HANDIWORK_SPIDER_BROOCH)) {
				return "30641-05.htm";
			} else if (qs.hasQuestItems(TAKUNA_CHARM)) {
				if (qs.getQuestItemsCount(MARSH_SPIDER_FEELER) >= 10 && qs.getQuestItemsCount(MARSH_SPIDER_FEET) >= 10) {
					qs.takeItems(MARSH_SPIDER_FEELER, -1);
					qs.takeItems(MARSH_SPIDER_FEET, -1);
					qs.takeItems(TAKUNA_CHARM, -1);
					qs.giveItems(HANDIWORK_SPIDER_BROOCH, 1);
					checkPhaseOneComplete(qs);
					return "30641-04.htm";
				}
				return "30641-03.htm";
			}
			return "30641-01.htm";
		} else if (npcId == CHIANTA) {
			if (qs.hasQuestItems(MONSTEREYE_WOODCARVING)) {
				return "30642-05.htm";
			} else if (qs.hasQuestItems(CHIANTA_CHARM)) {
				if (qs.getQuestItemsCount(MONSTEREYE_CORNEA) >= 20) {
					qs.takeItems(MONSTEREYE_CORNEA, -1);
					qs.takeItems(CHIANTA_CHARM, -1);
					qs.giveItems(MONSTEREYE_WOODCARVING, 1);
					checkPhaseOneComplete(qs);
					return "30642-04.htm";
				}
				return "30642-03.htm";
			}
			return "30642-01.htm";
		} else if (npcId == MARTANKUS) {
			if (cond == 1) {
				qs.giveItems(IMMORTAL_FLAME, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30649-01.htm";
			} else if (cond == 3) {
				return "30649-01.htm";
			} else if (cond == 4) {
				return "30649-05.htm";
			} else if (cond == 5) {
				qs.takeItems(MARTANKUS_CHARM, -1);
				qs.takeItems(RAGNA_ORC_HEAD, -1);
				qs.takeItems(RAGNA_CHIEF_NOTICE, -1);
				qs.giveItems(IMMORTAL_FLAME, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30649-06.htm";
			} else if (cond == 6) {
				return "30649-07.htm";
			} else if (cond == 7) {
				return "30649-08.htm";
			}
		} else if (npcId == FIRST_ORC) {
			if (cond == 6) {
				return "30643-01.htm";
			} else if (cond == 7) {
				return "30643-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		switch (npc.getNpcId()) {
			case ENCHANTED_MONSTEREYE -> {
				if (qs.hasQuestItems(CHIANTA_CHARM) && qs.getQuestItemsCount(MONSTEREYE_CORNEA) < 20) {
					qs.giveItems(MONSTEREYE_CORNEA, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case TIMAK_ORC, TIMAK_ORC_ARCHER, TIMAK_ORC_SOLDIER, TIMAK_ORC_WARRIOR, TIMAK_ORC_SHAMAN, TIMAK_ORC_OVERLORD -> {
				if (qs.hasQuestItems(HATOS_CHARM) && qs.getQuestItemsCount(TIMAK_ORC_SKULL) < 10) {
					qs.giveItems(TIMAK_ORC_SKULL, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MARSH_SPIDER -> {
				if (qs.hasQuestItems(TAKUNA_CHARM)) {
					if (qs.getQuestItemsCount(MARSH_SPIDER_FEELER) < 10) {
						qs.giveItems(MARSH_SPIDER_FEELER, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					} else if (qs.getQuestItemsCount(MARSH_SPIDER_FEET) < 10) {
						qs.giveItems(MARSH_SPIDER_FEET, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD -> {
				if (qs.hasQuestItems(MANAKIAS_ORDERS) && qs.getQuestItemsCount(BREKA_ORC_FANG) < 20) {
					qs.giveItems(BREKA_ORC_FANG, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER -> {
				if (qs.hasQuestItems(MARTANKUS_CHARM)) {
					if (!qs.hasQuestItems(RAGNA_CHIEF_NOTICE)) {
						qs.giveItems(RAGNA_CHIEF_NOTICE, 1);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else if (!qs.hasQuestItems(RAGNA_ORC_HEAD)) {
						qs.giveItems(RAGNA_ORC_HEAD, 1);
						qs.setCond(5);
						qs.playSound(QuestState.SOUND_MIDDLE);
					}
				}
			}
		}

		return null;
	}
}
