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
 * Quest 228: Test of the Magus
 * 3º Passo de 2ª Classe para Sorcerer, Spellsinger, Spellhowler.
 */
@Component
public class Quest228TestOfMagus extends Quest {

	public static final int QUEST_ID = 228;
	public static final String QUEST_NAME = "228_TestOfMagus";

	// NPCs
	public static final int RUKAL = 30629;
	public static final int PARINA = 30391;
	public static final int CASIAN = 30612;
	public static final int SALAMANDER = 30411;
	public static final int SYLPH = 30412;
	public static final int UNDINE = 30413;
	public static final int SERPENT = 30409;

	// Monstros
	public static final int HARPY = 20145;
	public static final int MARSH_STAKATO = 20157;
	public static final int WYRM = 20176;
	public static final int MARSH_STAKATO_WORKER = 20230;
	public static final int TOAD_LORD = 20231;
	public static final int MARSH_STAKATO_SOLDIER = 20232;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int WINDSUS = 20553;
	public static final int ENCHANTED_MONSTEREYE = 20564;
	public static final int ENCHANTED_STONE_GOLEM = 20565;
	public static final int ENCHANTED_IRON_GOLEM = 20566;
	public static final int SINGING_FLOWER_PHANTASM = 27095;
	public static final int SINGING_FLOWER_NIGHTMARE = 27096;
	public static final int SINGING_FLOWER_DARKLING = 27097;
	public static final int GHOST_FIRE = 27098;

	// Itens
	public static final int MARK_OF_MAGUS = 2840;
	public static final int RUKALS_LETTER = 2841;
	public static final int PARINAS_LETTER = 2842;
	public static final int LILAC_CHARM = 2843;
	public static final int GOLDEN_SEED_1 = 2844;
	public static final int GOLDEN_SEED_2 = 2845;
	public static final int GOLDEN_SEED_3 = 2846;
	public static final int SCORE_OF_ELEMENTS = 2847;
	public static final int DAZZLING_DROP = 2848;
	public static final int FLAME_CRYSTAL = 2849;
	public static final int HARPY_FEATHER = 2850;
	public static final int WYRM_WINGBONE = 2851;
	public static final int WINDSUS_MANE = 2852;
	public static final int EN_MONSTEREYE_SHELL = 2853;
	public static final int EN_STONEGOLEM_POWDER = 2854;
	public static final int EN_IRONGOLEM_SCRAP = 2855;
	public static final int TONE_OF_WATER = 2856;
	public static final int TONE_OF_FIRE = 2857;
	public static final int TONE_OF_WIND = 2858;
	public static final int TONE_OF_EARTH = 2859;
	public static final int SALAMANDER_CHARM = 2860;
	public static final int SYLPH_CHARM = 2861;
	public static final int UNDINE_CHARM = 2862;
	public static final int SERPENT_CHARM = 2863;

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 26, 39);

	@Autowired
	public Quest228TestOfMagus(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Magus");

		addStartNpc(RUKAL);
		addTalkId(RUKAL, PARINA, CASIAN, SALAMANDER, SYLPH, UNDINE, SERPENT);

		addKillId(HARPY, MARSH_STAKATO, WYRM, MARSH_STAKATO_WORKER, TOAD_LORD,
				MARSH_STAKATO_SOLDIER, MARSH_STAKATO_DRONE, WINDSUS,
				ENCHANTED_MONSTEREYE, ENCHANTED_STONE_GOLEM, ENCHANTED_IRON_GOLEM,
				SINGING_FLOWER_PHANTASM, SINGING_FLOWER_NIGHTMARE,
				SINGING_FLOWER_DARKLING, GHOST_FIRE);

		registerQuestItems(RUKALS_LETTER, PARINAS_LETTER, LILAC_CHARM, GOLDEN_SEED_1,
				GOLDEN_SEED_2, GOLDEN_SEED_3, SCORE_OF_ELEMENTS, DAZZLING_DROP,
				FLAME_CRYSTAL, HARPY_FEATHER, WYRM_WINGBONE, WINDSUS_MANE,
				EN_MONSTEREYE_SHELL, EN_STONEGOLEM_POWDER, EN_IRONGOLEM_SCRAP,
				TONE_OF_WATER, TONE_OF_FIRE, TONE_OF_WIND, TONE_OF_EARTH,
				SALAMANDER_CHARM, SYLPH_CHARM, UNDINE_CHARM, SERPENT_CHARM);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30629-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(RUKALS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30629-04.htm";
		} else if ("30391_1".equalsIgnoreCase(event) || "30391-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(RUKALS_LETTER, -1);
			qs.giveItems(PARINAS_LETTER, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30391-02.htm";
		} else if ("30612_1".equalsIgnoreCase(event) || "30612-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(PARINAS_LETTER, -1);
			qs.giveItems(LILAC_CHARM, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30612-02.htm";
		} else if ("30629_2".equalsIgnoreCase(event) || "30629-10.htm".equalsIgnoreCase(event)) {
			qs.takeItems(GOLDEN_SEED_1, -1);
			qs.takeItems(GOLDEN_SEED_2, -1);
			qs.takeItems(GOLDEN_SEED_3, -1);
			qs.takeItems(LILAC_CHARM, -1);
			qs.giveItems(SCORE_OF_ELEMENTS, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30629-10.htm";
		} else if ("30412-02.htm".equalsIgnoreCase(event) || "30412_1".equalsIgnoreCase(event)) {
			qs.giveItems(SYLPH_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30412-02.htm";
		} else if ("30409-03.htm".equalsIgnoreCase(event) || "30409_1".equalsIgnoreCase(event)) {
			qs.giveItems(SERPENT_CHARM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30409-03.htm";
		} else if ("30629-12.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE)
					&& qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
				qs.takeItems(SCORE_OF_ELEMENTS, -1);
				qs.takeItems(TONE_OF_WATER, -1);
				qs.takeItems(TONE_OF_FIRE, -1);
				qs.takeItems(TONE_OF_WIND, -1);
				qs.takeItems(TONE_OF_EARTH, -1);
				qs.giveItems(MARK_OF_MAGUS, 1);
				qs.addExpAndSp(139039, 16000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
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

		if (npcId == RUKAL) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30629-03.htm";
				} else {
					return "30629-01.htm";
				}
			} else if (cond == 1) {
				return "30629-05.htm";
			} else if (cond == 2) {
				return "30629-06.htm";
			} else if (cond == 3) {
				return "30629-07.htm";
			} else if (cond == 4) {
				return "30629-09.htm";
			} else if (cond == 5) {
				return "30629-11.htm";
			} else if (cond == 6 && qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE)
					&& qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
				return "30629-11.htm";
			}
		} else if (npcId == PARINA) {
			if (cond == 1) {
				return "30391-01.htm";
			} else if (cond == 2) {
				return "30391-03.htm";
			} else if (cond == 3 || cond == 4) {
				return "30391-04.htm";
			} else if (cond > 4) {
				return "30391-05.htm";
			}
		} else if (npcId == CASIAN) {
			if (cond == 2) {
				return "30612-01.htm";
			} else if (cond == 3) {
				return "30612-03.htm";
			} else if (cond == 4) {
				return "30612-04.htm";
			} else if (cond > 4) {
				return "30612-05.htm";
			}
		} else if (npcId == UNDINE) {
			if (cond == 5) {
				if (qs.hasQuestItems(UNDINE_CHARM)) {
					if (qs.getQuestItemsCount(DAZZLING_DROP) < 20) {
						return "30413-02.htm";
					}
					qs.takeItems(DAZZLING_DROP, 20);
					qs.takeItems(UNDINE_CHARM, 1);
					qs.giveItems(TONE_OF_WATER, 1);
					if (qs.hasQuestItems(TONE_OF_FIRE) && qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
					return "30413-03.htm";
				} else if (!qs.hasQuestItems(TONE_OF_WATER)) {
					qs.giveItems(UNDINE_CHARM, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30413-01.htm";
				}
				return "30413-04.htm";
			} else if (cond == 6) {
				return "30413-04.htm";
			}
		} else if (npcId == SALAMANDER) {
			if (cond == 5) {
				if (qs.hasQuestItems(SALAMANDER_CHARM)) {
					if (qs.getQuestItemsCount(FLAME_CRYSTAL) < 5) {
						return "30411-02.htm";
					}
					qs.takeItems(FLAME_CRYSTAL, 5);
					qs.takeItems(SALAMANDER_CHARM, 1);
					qs.giveItems(TONE_OF_FIRE, 1);
					if (qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_WIND) && qs.hasQuestItems(TONE_OF_EARTH)) {
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
					return "30411-03.htm";
				} else if (!qs.hasQuestItems(TONE_OF_FIRE)) {
					qs.giveItems(SALAMANDER_CHARM, 1);
					return "30411-01.htm";
				}
				return "30411-04.htm";
			} else if (cond == 6) {
				return "30411-04.htm";
			}
		} else if (npcId == SYLPH) {
			if (cond == 5) {
				if (qs.hasQuestItems(SYLPH_CHARM)) {
					if (qs.getQuestItemsCount(HARPY_FEATHER) + qs.getQuestItemsCount(WYRM_WINGBONE) + qs.getQuestItemsCount(WINDSUS_MANE) < 40) {
						return "30412-03.htm";
					}
					qs.takeItems(HARPY_FEATHER, 20);
					qs.takeItems(SYLPH_CHARM, 1);
					qs.takeItems(WINDSUS_MANE, 10);
					qs.takeItems(WYRM_WINGBONE, 10);
					qs.giveItems(TONE_OF_WIND, 1);
					if (qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE) && qs.hasQuestItems(TONE_OF_EARTH)) {
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
					return "30412-04.htm";
				} else if (!qs.hasQuestItems(TONE_OF_WIND)) {
					return "30412-01.htm";
				}
				return "30412-05.htm";
			} else if (cond == 6) {
				return "30412-05.htm";
			}
		} else if (npcId == SERPENT) {
			if (cond == 5) {
				if (qs.hasQuestItems(SERPENT_CHARM)) {
					if (qs.getQuestItemsCount(EN_MONSTEREYE_SHELL) + qs.getQuestItemsCount(EN_STONEGOLEM_POWDER) + qs.getQuestItemsCount(EN_IRONGOLEM_SCRAP) < 30) {
						return "30409-04.htm";
					}
					qs.takeItems(EN_IRONGOLEM_SCRAP, 10);
					qs.takeItems(EN_MONSTEREYE_SHELL, 10);
					qs.takeItems(EN_STONEGOLEM_POWDER, 10);
					qs.takeItems(SERPENT_CHARM, 1);
					qs.giveItems(TONE_OF_EARTH, 1);
					if (qs.hasQuestItems(TONE_OF_WATER) && qs.hasQuestItems(TONE_OF_FIRE) && qs.hasQuestItems(TONE_OF_WIND)) {
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
					return "30409-05.htm";
				} else if (!qs.hasQuestItems(TONE_OF_EARTH)) {
					return "30409-01.htm";
				}
				return "30409-06.htm";
			} else if (cond == 6) {
				return "30409-06.htm";
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

		if (cond == 3) {
			switch (npcId) {
				case SINGING_FLOWER_PHANTASM -> {
					if (!qs.hasQuestItems(GOLDEN_SEED_1)) {
						qs.giveItems(GOLDEN_SEED_1, 1);
						if (qs.hasQuestItems(GOLDEN_SEED_2) && qs.hasQuestItems(GOLDEN_SEED_3)) {
							qs.setCond(4);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
					}
				}
				case SINGING_FLOWER_NIGHTMARE -> {
					if (!qs.hasQuestItems(GOLDEN_SEED_2)) {
						qs.giveItems(GOLDEN_SEED_2, 1);
						if (qs.hasQuestItems(GOLDEN_SEED_1) && qs.hasQuestItems(GOLDEN_SEED_3)) {
							qs.setCond(4);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
					}
				}
				case SINGING_FLOWER_DARKLING -> {
					if (!qs.hasQuestItems(GOLDEN_SEED_3)) {
						qs.giveItems(GOLDEN_SEED_3, 1);
						if (qs.hasQuestItems(GOLDEN_SEED_1) && qs.hasQuestItems(GOLDEN_SEED_2)) {
							qs.setCond(4);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
					}
				}
			}
		} else if (cond == 5) {
			switch (npcId) {
				case GHOST_FIRE -> {
					if (qs.hasQuestItems(SALAMANDER_CHARM) && qs.getQuestItemsCount(FLAME_CRYSTAL) < 5) {
						qs.giveItems(FLAME_CRYSTAL, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case TOAD_LORD, MARSH_STAKATO, MARSH_STAKATO_WORKER, MARSH_STAKATO_SOLDIER, MARSH_STAKATO_DRONE -> {
					if (qs.hasQuestItems(UNDINE_CHARM) && qs.getQuestItemsCount(DAZZLING_DROP) < 20) {
						qs.giveItems(DAZZLING_DROP, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case HARPY -> {
					if (qs.hasQuestItems(SYLPH_CHARM) && qs.getQuestItemsCount(HARPY_FEATHER) < 20) {
						qs.giveItems(HARPY_FEATHER, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case WYRM -> {
					if (qs.hasQuestItems(SYLPH_CHARM) && qs.getQuestItemsCount(WYRM_WINGBONE) < 10) {
						qs.giveItems(WYRM_WINGBONE, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case WINDSUS -> {
					if (qs.hasQuestItems(SYLPH_CHARM) && qs.getQuestItemsCount(WINDSUS_MANE) < 10) {
						qs.giveItems(WINDSUS_MANE, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case ENCHANTED_MONSTEREYE -> {
					if (qs.hasQuestItems(SERPENT_CHARM) && qs.getQuestItemsCount(EN_MONSTEREYE_SHELL) < 10) {
						qs.giveItems(EN_MONSTEREYE_SHELL, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case ENCHANTED_STONE_GOLEM -> {
					if (qs.hasQuestItems(SERPENT_CHARM) && qs.getQuestItemsCount(EN_STONEGOLEM_POWDER) < 10) {
						qs.giveItems(EN_STONEGOLEM_POWDER, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				case ENCHANTED_IRON_GOLEM -> {
					if (qs.hasQuestItems(SERPENT_CHARM) && qs.getQuestItemsCount(EN_IRONGOLEM_SCRAP) < 10) {
						qs.giveItems(EN_IRONGOLEM_SCRAP, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}

		return null;
	}
}
