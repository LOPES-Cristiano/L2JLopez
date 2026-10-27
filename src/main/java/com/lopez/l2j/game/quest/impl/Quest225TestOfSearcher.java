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
 * Quest 225: Test of the Searcher
 * 3º Passo de 2ª Classe para Treasure Hunter, Plains Walker, Abyss Walker,
 * Bounty Hunter.
 */
@Component
public class Quest225TestOfSearcher extends Quest {

	public static final int QUEST_ID = 225;
	public static final String QUEST_NAME = "225_TestOfSearcher";

	// NPCs
	public static final int LUTHER = 30690;
	public static final int ALEX = 30291;
	public static final int TYRA = 30420;
	public static final int LEIRYNN = 30728;
	public static final int BORYA = 30729;
	public static final int JAX = 30730;
	public static final int TREE = 30627;
	public static final int CHEST = 30628;

	// Monstros
	public static final int HANGMAN_TREE = 20144;
	public static final int ROAD_RATMAN = 20551;
	public static final int GIANT_FUNGUS = 20555;
	public static final int DELU_LIZARDMAN = 20781;
	public static final int NEER_BODYGUARD = 27092;
	public static final int CHIEF_KALKIS = 27093;

	// Itens
	public static final int LUTHERS_LETTER = 2784;
	public static final int ALANKELLS_WARRANT = 2785;
	public static final int LEIRYNNS_ORDER1 = 2786;
	public static final int DELU_TOTEM = 2787;
	public static final int LEIRYNNS_ORDER2 = 2788;
	public static final int CHIEF_KALKIS_FANG = 2789;
	public static final int LEIRYNNS_REPORT = 2790;
	public static final int STRANGE_MAP = 2791;
	public static final int LAMBERTS_MAP = 2792;
	public static final int ALANKELLS_LETTER = 2793;
	public static final int ALANKELLS_ORDER = 2794;
	public static final int WINE_CATALOG = 2795;
	public static final int TYRAS_CONTRACT = 2796;
	public static final int RED_SPORE_DUST = 2797;
	public static final int MALRUKIAN_WINE = 2798;
	public static final int OLD_ORDER = 2799;
	public static final int JAXS_DIARY = 2800;
	public static final int TORN_MAP_PIECE_1 = 2801;
	public static final int TORN_MAP_PIECE_2 = 2802;
	public static final int SOLTS_MAP = 2803;
	public static final int MAKELS_MAP = 2804;
	public static final int COMBINED_MAP = 2805;
	public static final int RUSTED_KEY = 2806;
	public static final int GOLD_BAR = 2807;
	public static final int ALANKELLS_RECOMMEND = 2808;
	public static final int MARK_OF_SEARCHER = 2809;

	private static final Set<Integer> VALID_CLASSES = Set.of(7, 22, 35, 54);

	@Autowired
	public Quest225TestOfSearcher(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Searcher");

		addStartNpc(LUTHER);
		addTalkId(LUTHER, ALEX, TYRA, LEIRYNN, BORYA, JAX, TREE, CHEST);

		addAttackId(DELU_LIZARDMAN);
		addKillId(HANGMAN_TREE, ROAD_RATMAN, GIANT_FUNGUS, DELU_LIZARDMAN, NEER_BODYGUARD, CHIEF_KALKIS);

		registerQuestItems(LUTHERS_LETTER, ALANKELLS_WARRANT, LEIRYNNS_ORDER1, DELU_TOTEM,
				LEIRYNNS_ORDER2, CHIEF_KALKIS_FANG, LEIRYNNS_REPORT, STRANGE_MAP, LAMBERTS_MAP,
				ALANKELLS_LETTER, ALANKELLS_ORDER, WINE_CATALOG, TYRAS_CONTRACT, RED_SPORE_DUST,
				MALRUKIAN_WINE, OLD_ORDER, JAXS_DIARY, TORN_MAP_PIECE_1, TORN_MAP_PIECE_2,
				SOLTS_MAP, MAKELS_MAP, COMBINED_MAP, RUSTED_KEY, GOLD_BAR, ALANKELLS_RECOMMEND);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30690-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(LUTHERS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30690-05.htm";
		} else if ("30291-07.htm".equalsIgnoreCase(event) || "30291_1".equalsIgnoreCase(event)) {
			qs.giveItems(ALANKELLS_LETTER, 1);
			qs.giveItems(ALANKELLS_ORDER, 1);
			qs.giveItems(LAMBERTS_MAP, 1);
			qs.takeItems(STRANGE_MAP, -1);
			qs.takeItems(LEIRYNNS_REPORT, -1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30291-07.htm";
		} else if ("30420-01a.htm".equalsIgnoreCase(event) || "30420_1".equalsIgnoreCase(event)) {
			qs.takeItems(WINE_CATALOG, -1);
			qs.giveItems(TYRAS_CONTRACT, 1);
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30420-01a.htm";
		} else if ("30730-01d.htm".equalsIgnoreCase(event) || "30730_1".equalsIgnoreCase(event)) {
			qs.takeItems(OLD_ORDER, -1);
			qs.giveItems(JAXS_DIARY, 1);
			qs.setCond(14);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30730-01d.htm";
		} else if ("30627-01a.htm".equalsIgnoreCase(event) || "30627_1".equalsIgnoreCase(event)) {
			if (qs.getCond() == 16) {
				qs.giveItems(RUSTED_KEY, 1);
				qs.setCond(17);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			qs.addSpawn(CHEST, 10098, 157287, -2406, 300000);
			return "30627-01a.htm";
		} else if ("30628-01a.htm".equalsIgnoreCase(event) || "30628_1".equalsIgnoreCase(event)) {
			if (!qs.hasQuestItems(RUSTED_KEY)) {
				return "30628-02.htm";
			}
			qs.takeItems(RUSTED_KEY, -1);
			qs.giveItems(GOLD_BAR, 20);
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30628-01a.htm";
		} else if ("30690-07.htm".equalsIgnoreCase(event) || "30690-08.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ALANKELLS_RECOMMEND)) {
				qs.takeItems(ALANKELLS_RECOMMEND, -1);
				qs.giveItems(MARK_OF_SEARCHER, 1);
				qs.addExpAndSp(49000, 5700);
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

		if (npcId == LUTHER) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30690-03.htm";
				} else {
					return "30690-01.htm";
				}
			} else if (cond == 1) {
				return "30690-06.htm";
			} else if (cond > 1 && cond < 19) {
				return "30690-07.htm";
			} else if (cond == 19 && qs.hasQuestItems(ALANKELLS_RECOMMEND)) {
				return "30690-07.htm";
			}
		} else if (npcId == ALEX) {
			if (cond == 1) {
				qs.takeItems(LUTHERS_LETTER, -1);
				qs.giveItems(ALANKELLS_WARRANT, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30291-01.htm";
			} else if (cond == 2) {
				return "30291-02.htm";
			} else if (cond >= 3 && cond <= 6) {
				return "30291-03.htm";
			} else if (cond == 7) {
				return "30291-04.htm";
			} else if (cond >= 8 && cond <= 12) {
				return "30291-08.htm";
			} else if (cond >= 13 && cond <= 15) {
				return "30291-09.htm";
			} else if (cond == 16 || cond == 17) {
				return "30291-10.htm";
			} else if (cond == 18 && qs.getQuestItemsCount(GOLD_BAR) >= 20) {
				qs.takeItems(ALANKELLS_ORDER, -1);
				qs.takeItems(COMBINED_MAP, -1);
				qs.takeItems(GOLD_BAR, -1);
				qs.giveItems(ALANKELLS_RECOMMEND, 1);
				qs.setCond(19);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30291-11.htm";
			} else if (cond == 19) {
				return "30291-12.htm";
			}
		} else if (npcId == LEIRYNN) {
			if (cond == 2) {
				qs.takeItems(ALANKELLS_WARRANT, -1);
				qs.giveItems(LEIRYNNS_ORDER1, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30728-01.htm";
			} else if (cond == 3) {
				return "30728-02.htm";
			} else if (cond == 4) {
				qs.takeItems(DELU_TOTEM, -1);
				qs.takeItems(LEIRYNNS_ORDER1, -1);
				qs.giveItems(LEIRYNNS_ORDER2, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30728-03.htm";
			} else if (cond == 5) {
				return "30728-04.htm";
			} else if (cond == 6) {
				qs.takeItems(CHIEF_KALKIS_FANG, -1);
				qs.takeItems(LEIRYNNS_ORDER2, -1);
				qs.giveItems(LEIRYNNS_REPORT, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30728-05.htm";
			} else if (cond == 7) {
				return "30728-06.htm";
			} else if (cond > 7) {
				return "30728-07.htm";
			}
		} else if (npcId == BORYA) {
			if (cond == 8) {
				qs.takeItems(ALANKELLS_LETTER, -1);
				qs.giveItems(WINE_CATALOG, 1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30729-01.htm";
			} else if (cond >= 9 && cond <= 11) {
				return "30729-02.htm";
			} else if (cond == 12) {
				qs.takeItems(MALRUKIAN_WINE, -1);
				qs.takeItems(WINE_CATALOG, -1);
				qs.giveItems(OLD_ORDER, 1);
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30729-03.htm";
			} else if (cond == 13) {
				return "30729-04.htm";
			} else if (cond > 13) {
				return "30729-05.htm";
			}
		} else if (npcId == TYRA) {
			if (cond == 9) {
				return "30420-01.htm";
			} else if (cond == 10) {
				return "30420-02.htm";
			} else if (cond == 11) {
				qs.takeItems(RED_SPORE_DUST, -1);
				qs.takeItems(TYRAS_CONTRACT, -1);
				qs.giveItems(MALRUKIAN_WINE, 1);
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30420-03.htm";
			} else if (cond > 11) {
				return "30420-04.htm";
			}
		} else if (npcId == JAX) {
			if (cond == 13) {
				return "30730-01.htm";
			} else if (cond == 14) {
				return "30730-02.htm";
			} else if (cond == 15) {
				qs.takeItems(LAMBERTS_MAP, -1);
				qs.takeItems(MAKELS_MAP, -1);
				qs.takeItems(JAXS_DIARY, -1);
				qs.takeItems(SOLTS_MAP, -1);
				qs.giveItems(COMBINED_MAP, 1);
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30730-03.htm";
			} else if (cond > 15) {
				return "30730-04.htm";
			}
		} else if (npcId == TREE) {
			if (cond == 16 || cond == 17) {
				return "30627-01.htm";
			}
		} else if (npcId == CHEST) {
			if (cond == 17) {
				return "30628-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (npc.getNpcId() == DELU_LIZARDMAN && qs.hasQuestItems(LEIRYNNS_ORDER1) && npc.isScriptValue(0)) {
			npc.setScriptValue(1);
			qs.addSpawn(NEER_BODYGUARD, 200000);
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

		switch (npcId) {
			case DELU_LIZARDMAN, NEER_BODYGUARD -> {
				if (cond == 3 && qs.getQuestItemsCount(DELU_TOTEM) < 10) {
					qs.giveItems(DELU_TOTEM, 1);
					if (qs.getQuestItemsCount(DELU_TOTEM) >= 10) {
						qs.setCond(4);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				} else if (cond == 5) {
					qs.addSpawn(CHIEF_KALKIS, 200000);
				}
			}
			case CHIEF_KALKIS -> {
				if (cond == 5) {
					qs.giveItems(CHIEF_KALKIS_FANG, 1);
					qs.giveItems(STRANGE_MAP, 1);
					qs.setCond(6);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case GIANT_FUNGUS -> {
				if (cond == 10 && qs.getQuestItemsCount(RED_SPORE_DUST) < 10) {
					qs.giveItems(RED_SPORE_DUST, 1);
					if (qs.getQuestItemsCount(RED_SPORE_DUST) >= 10) {
						qs.setCond(11);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case ROAD_RATMAN -> {
				if (cond == 14 && !qs.hasQuestItems(SOLTS_MAP)) {
					qs.giveItems(TORN_MAP_PIECE_1, 1);
					if (qs.getQuestItemsCount(TORN_MAP_PIECE_1) >= 4) {
						qs.takeItems(TORN_MAP_PIECE_1, -1);
						qs.giveItems(SOLTS_MAP, 1);
						if (qs.hasQuestItems(MAKELS_MAP)) {
							qs.setCond(15);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case HANGMAN_TREE -> {
				if (cond == 14 && !qs.hasQuestItems(MAKELS_MAP)) {
					qs.giveItems(TORN_MAP_PIECE_2, 1);
					if (qs.getQuestItemsCount(TORN_MAP_PIECE_2) >= 4) {
						qs.takeItems(TORN_MAP_PIECE_2, -1);
						qs.giveItems(MAKELS_MAP, 1);
						if (qs.hasQuestItems(SOLTS_MAP)) {
							qs.setCond(15);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}

		return null;
	}
}
