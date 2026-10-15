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
 * Quest 229: Test of the Witchcraft
 * 3º Passo de 2ª Classe para Necromancer, Dark Avenger, Shillien Knight.
 */
@Component
public class Quest229TestOfWitchcraft extends Quest {

	public static final int QUEST_ID = 229;
	public static final String QUEST_NAME = "229_TestOfWitchcraft";

	// NPCs
	public static final int LARA = 30063;
	public static final int ALEXANDRIA = 30098;
	public static final int IKER = 30110;
	public static final int VADIN = 30188;
	public static final int NESTLE = 30314;
	public static final int VASPER = 30417;
	public static final int LEOPOLD = 30435;
	public static final int KAIRA = 30476;
	public static final int ORIM = 30630;
	public static final int RODERIK = 30631;
	public static final int ENDRIGO = 30632;
	public static final int EVERT = 30633;

	// Monstros
	public static final int DIRE_WYRM = 20557;
	public static final int ENCHANTED_STONE_GOLEM = 20565;
	public static final int LETO_LIZARDMAN = 20577;
	public static final int LETO_LIZARDMAN_ARCHER = 20578;
	public static final int LETO_LIZARDMAN_SOLDIER = 20579;
	public static final int LETO_LIZARDMAN_WARRIOR = 20580;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int TAMLIN_ORC = 20601;
	public static final int TAMLIN_ORC_ARCHER = 20602;
	public static final int NAMELESS_REVENANT = 27099;
	public static final int SKELETAL_MERCENARY = 27100;
	public static final int DREVANUL_PRINCE_ZERUEL = 27101;

	// Itens
	public static final int SWORD_OF_BINDING = 3029;
	public static final int MARK_OF_WITCHCRAFT = 3307;
	public static final int ORIMS_DIAGRAM = 3308;
	public static final int ALEXANDRIAS_BOOK = 3309;
	public static final int IKERS_LIST = 3310;
	public static final int DIRE_WYRM_FANG = 3311;
	public static final int LETO_LIZARDMAN_CHARM = 3312;
	public static final int EN_GOLEM_HEARTSTONE = 3313;
	public static final int LARAS_MEMO = 3314;
	public static final int NESTLES_MEMO = 3315;
	public static final int LEOPOLDS_JOURNAL = 3316;
	public static final int AKLANTOTH_GEM_1 = 3317;
	public static final int AKLANTOTH_GEM_2 = 3318;
	public static final int AKLANTOTH_GEM_3 = 3319;
	public static final int AKLANTOTH_GEM_4 = 3320;
	public static final int AKLANTOTH_GEM_5 = 3321;
	public static final int AKLANTOTH_GEM_6 = 3322;
	public static final int BRIMSTONE1 = 3323;
	public static final int ORIMS_INSTRUCTIONS = 3324;
	public static final int ORIMS_LETTER_1 = 3325;
	public static final int ORIMS_LETTER_2 = 3326;
	public static final int SIR_VASPERS_LETTER = 3327;
	public static final int VADINS_CRUCIFIX = 3328;
	public static final int TAMLIN_ORC_AMULET = 3329;
	public static final int VADINS_SANCTIONS = 3330;
	public static final int IKERS_AMULET = 3331;
	public static final int SOULTRAP_CRYSTAL = 3332;
	public static final int PURGATORY_KEY = 3333;
	public static final int ZERUEL_BIND_CRYSTAL = 3334;
	public static final int BRIMSTONE2 = 3335;

	private static final Set<Integer> VALID_CLASSES = Set.of(4, 11, 32, 39);

	@Autowired
	public Quest229TestOfWitchcraft(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Witchcraft");

		addStartNpc(ORIM);
		addTalkId(LARA, ALEXANDRIA, IKER, VADIN, NESTLE, VASPER, LEOPOLD, KAIRA, ORIM, RODERIK, ENDRIGO, EVERT);

		addAttackId(NAMELESS_REVENANT, SKELETAL_MERCENARY, DREVANUL_PRINCE_ZERUEL);
		addKillId(DIRE_WYRM, ENCHANTED_STONE_GOLEM, LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER,
				LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN,
				LETO_LIZARDMAN_OVERLORD, TAMLIN_ORC, TAMLIN_ORC_ARCHER,
				NAMELESS_REVENANT, SKELETAL_MERCENARY, DREVANUL_PRINCE_ZERUEL);

		registerQuestItems(SWORD_OF_BINDING, ORIMS_DIAGRAM, ALEXANDRIAS_BOOK, IKERS_LIST,
				DIRE_WYRM_FANG, LETO_LIZARDMAN_CHARM, EN_GOLEM_HEARTSTONE, LARAS_MEMO,
				NESTLES_MEMO, LEOPOLDS_JOURNAL, AKLANTOTH_GEM_1, AKLANTOTH_GEM_2,
				AKLANTOTH_GEM_3, AKLANTOTH_GEM_4, AKLANTOTH_GEM_5, AKLANTOTH_GEM_6,
				BRIMSTONE1, ORIMS_INSTRUCTIONS, ORIMS_LETTER_1, ORIMS_LETTER_2,
				SIR_VASPERS_LETTER, VADINS_CRUCIFIX, TAMLIN_ORC_AMULET, VADINS_SANCTIONS,
				IKERS_AMULET, SOULTRAP_CRYSTAL, PURGATORY_KEY, ZERUEL_BIND_CRYSTAL, BRIMSTONE2);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30630-08.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ORIMS_DIAGRAM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30630-08.htm";
		} else if ("30630-14.htm".equalsIgnoreCase(event) || "30630_1".equalsIgnoreCase(event)) {
			qs.takeItems(AKLANTOTH_GEM_1, -1);
			qs.takeItems(AKLANTOTH_GEM_2, -1);
			qs.takeItems(AKLANTOTH_GEM_3, -1);
			qs.takeItems(AKLANTOTH_GEM_4, -1);
			qs.takeItems(AKLANTOTH_GEM_5, -1);
			qs.takeItems(AKLANTOTH_GEM_6, -1);
			qs.takeItems(ALEXANDRIAS_BOOK, -1);
			qs.giveItems(BRIMSTONE1, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(DREVANUL_PRINCE_ZERUEL, 70381, 109638, -3726, 120000);
			return "30630-14.htm";
		} else if ("30630-16.htm".equalsIgnoreCase(event) || "30630_2".equalsIgnoreCase(event)) {
			qs.takeItems(BRIMSTONE1, -1);
			qs.giveItems(ORIMS_INSTRUCTIONS, 1);
			qs.giveItems(ORIMS_LETTER_1, 1);
			qs.giveItems(ORIMS_LETTER_2, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30630-16.htm";
		} else if ("30630-22.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ZERUEL_BIND_CRYSTAL)) {
				qs.takeItems(IKERS_AMULET, -1);
				qs.takeItems(ORIMS_INSTRUCTIONS, -1);
				qs.takeItems(PURGATORY_KEY, -1);
				qs.takeItems(SWORD_OF_BINDING, -1);
				qs.takeItems(ZERUEL_BIND_CRYSTAL, -1);
				qs.giveItems(MARK_OF_WITCHCRAFT, 1);
				qs.addExpAndSp(139039, 16000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		} else if ("30098-03.htm".equalsIgnoreCase(event) || "30098_1".equalsIgnoreCase(event)) {
			qs.takeItems(ORIMS_DIAGRAM, -1);
			qs.giveItems(ALEXANDRIAS_BOOK, 1);
			qs.set("gem456", "1");
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30098-03.htm";
		} else if ("30110-03.htm".equalsIgnoreCase(event) || "30110_1".equalsIgnoreCase(event)) {
			qs.giveItems(IKERS_LIST, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30110-03.htm";
		} else if ("30110-08.htm".equalsIgnoreCase(event) || "30110_2".equalsIgnoreCase(event)) {
			qs.takeItems(ORIMS_LETTER_2, -1);
			qs.giveItems(IKERS_AMULET, 1);
			qs.giveItems(SOULTRAP_CRYSTAL, 1);
			if (qs.hasQuestItems(SWORD_OF_BINDING)) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "30110-08.htm";
		} else if ("30476-02.htm".equalsIgnoreCase(event) || "30476_1".equalsIgnoreCase(event)) {
			qs.giveItems(AKLANTOTH_GEM_2, 1);
			if (qs.hasQuestItems(AKLANTOTH_GEM_1) && qs.hasQuestItems(AKLANTOTH_GEM_3) && qs.getInt("gem456") == 6) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
			return "30476-02.htm";
		} else if ("30063-02.htm".equalsIgnoreCase(event) || "30063_1".equalsIgnoreCase(event)) {
			qs.giveItems(LARAS_MEMO, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30063-02.htm";
		} else if ("30314-02.htm".equalsIgnoreCase(event) || "30314_1".equalsIgnoreCase(event)) {
			qs.set("gem456", "2");
			qs.giveItems(NESTLES_MEMO, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30314-02.htm";
		} else if ("30435-02.htm".equalsIgnoreCase(event) || "30435_1".equalsIgnoreCase(event)) {
			qs.set("gem456", "3");
			qs.takeItems(NESTLES_MEMO, -1);
			qs.giveItems(LEOPOLDS_JOURNAL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30435-02.htm";
		} else if ("30417-03.htm".equalsIgnoreCase(event) || "30417_1".equalsIgnoreCase(event)) {
			qs.takeItems(ORIMS_LETTER_1, -1);
			qs.giveItems(SIR_VASPERS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30417-03.htm";
		} else if ("30633-02.htm".equalsIgnoreCase(event) || "30633_1".equalsIgnoreCase(event)) {
			qs.giveItems(BRIMSTONE2, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(DREVANUL_PRINCE_ZERUEL, 13395, 169807, -3708, 299000);
			return "30633-02.htm";
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
		int gem456 = qs.getInt("gem456");

		if (npcId == ORIM) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30630-03.htm";
				} else {
					return "30630-01.htm";
				}
			} else if (cond == 1) {
				return "30630-09.htm";
			} else if (cond == 2) {
				return "30630-10.htm";
			} else if (cond == 3) {
				return "30630-11.htm";
			} else if (cond == 4) {
				return "30630-14.htm";
			} else if (cond == 5) {
				return "30630-15.htm";
			} else if (cond == 6) {
				return "30630-17.htm";
			} else if (cond == 7) {
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30630-18.htm";
			} else if (cond == 8 || cond == 9) {
				return "30630-18.htm";
			} else if (cond == 10 && !qs.hasQuestItems(ZERUEL_BIND_CRYSTAL)) {
				return "30630-19.htm";
			} else if (cond >= 10 && qs.hasQuestItems(ZERUEL_BIND_CRYSTAL)) {
				return "30630-21.htm";
			}
		} else if (npcId == ALEXANDRIA) {
			if (cond == 1) {
				return "30098-01.htm";
			} else if (cond == 2) {
				return "30098-04.htm";
			} else if (cond > 2) {
				return "30098-05.htm";
			}
		} else if (npcId == KAIRA) {
			if (qs.hasQuestItems(AKLANTOTH_GEM_2)) {
				return "30476-03.htm";
			} else if (cond == 2) {
				return "30476-01.htm";
			} else if (cond > 3) {
				return "30476-04.htm";
			}
		} else if (npcId == IKER) {
			if (qs.hasQuestItems(AKLANTOTH_GEM_1)) {
				return "30110-06.htm";
			} else if (qs.hasQuestItems(IKERS_LIST)) {
				if (qs.getQuestItemsCount(DIRE_WYRM_FANG) + qs.getQuestItemsCount(LETO_LIZARDMAN_CHARM) + qs.getQuestItemsCount(EN_GOLEM_HEARTSTONE) < 60) {
					return "30110-04.htm";
				}
				qs.takeItems(IKERS_LIST, -1);
				qs.takeItems(DIRE_WYRM_FANG, -1);
				qs.takeItems(EN_GOLEM_HEARTSTONE, -1);
				qs.takeItems(LETO_LIZARDMAN_CHARM, -1);
				qs.giveItems(AKLANTOTH_GEM_1, 1);
				if (qs.hasQuestItems(AKLANTOTH_GEM_2) && qs.hasQuestItems(AKLANTOTH_GEM_3) && gem456 == 6) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30110-05.htm";
			} else if (cond == 2) {
				return "30110-01.htm";
			} else if (cond == 6 && !qs.hasQuestItems(SOULTRAP_CRYSTAL)) {
				return "30110-07.htm";
			} else if (cond >= 6 && cond < 10) {
				return "30110-09.htm";
			} else if (cond >= 10) {
				return "30110-10.htm";
			}
		} else if (npcId == LARA) {
			if (qs.hasQuestItems(AKLANTOTH_GEM_3)) {
				return "30063-04.htm";
			} else if (qs.hasQuestItems(LARAS_MEMO)) {
				return "30063-03.htm";
			} else if (cond == 2) {
				return "30063-01.htm";
			} else if (cond > 2) {
				return "30063-05.htm";
			}
		} else if (npcId == RODERIK || npcId == ENDRIGO) {
			if (qs.hasQuestItems(LARAS_MEMO) || qs.hasQuestItems(AKLANTOTH_GEM_3)) {
				return npcId == RODERIK ? "30631-01.htm" : "30632-01.htm";
			}
		} else if (npcId == NESTLE) {
			if (gem456 == 1) {
				return "30314-01.htm";
			} else if (gem456 == 2) {
				return "30314-03.htm";
			} else if (gem456 > 2) {
				return "30314-04.htm";
			}
		} else if (npcId == LEOPOLD) {
			if (gem456 == 2) {
				return "30435-01.htm";
			} else if (gem456 >= 3 && gem456 <= 5) {
				return "30435-03.htm";
			} else if (gem456 == 6) {
				return "30435-04.htm";
			} else if (cond > 3) {
				return "30435-05.htm";
			}
		} else if (npcId == VASPER) {
			if (qs.hasQuestItems(SIR_VASPERS_LETTER) || qs.hasQuestItems(VADINS_CRUCIFIX)) {
				return "30417-04.htm";
			} else if (qs.hasQuestItems(VADINS_SANCTIONS)) {
				qs.takeItems(VADINS_SANCTIONS, -1);
				qs.giveItems(SWORD_OF_BINDING, 1);
				if (qs.hasQuestItems(SOULTRAP_CRYSTAL)) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30417-05.htm";
			} else if (cond == 6) {
				return "30417-01.htm";
			} else if (cond > 6) {
				return "30417-06.htm";
			}
		} else if (npcId == VADIN) {
			if (qs.hasQuestItems(SIR_VASPERS_LETTER)) {
				qs.takeItems(SIR_VASPERS_LETTER, -1);
				qs.giveItems(VADINS_CRUCIFIX, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30188-01.htm";
			} else if (qs.hasQuestItems(VADINS_CRUCIFIX)) {
				if (qs.getQuestItemsCount(TAMLIN_ORC_AMULET) < 20) {
					return "30188-02.htm";
				}
				qs.takeItems(TAMLIN_ORC_AMULET, -1);
				qs.takeItems(VADINS_CRUCIFIX, -1);
				qs.giveItems(VADINS_SANCTIONS, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30188-03.htm";
			} else if (qs.hasQuestItems(VADINS_SANCTIONS)) {
				return "30188-04.htm";
			} else if (cond > 6) {
				return "30188-05.htm";
			}
		} else if (npcId == EVERT) {
			if (cond == 7 || cond == 8) {
				return "30633-01.htm";
			} else if (cond == 9) {
				qs.addSpawn(DREVANUL_PRINCE_ZERUEL, 13395, 169807, -3708, 299000);
				return "30633-02.htm";
			} else if (cond >= 10) {
				return "30633-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (npc.getNpcId() == DREVANUL_PRINCE_ZERUEL && qs.getCond() == 4) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
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
			case DIRE_WYRM -> {
				if (qs.hasQuestItems(IKERS_LIST) && qs.getQuestItemsCount(DIRE_WYRM_FANG) < 20) {
					qs.giveItems(DIRE_WYRM_FANG, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case ENCHANTED_STONE_GOLEM -> {
				if (qs.hasQuestItems(IKERS_LIST) && qs.getQuestItemsCount(EN_GOLEM_HEARTSTONE) < 20) {
					qs.giveItems(EN_GOLEM_HEARTSTONE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER,
					LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD -> {
				if (qs.hasQuestItems(IKERS_LIST) && qs.getQuestItemsCount(LETO_LIZARDMAN_CHARM) < 20) {
					qs.giveItems(LETO_LIZARDMAN_CHARM, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case NAMELESS_REVENANT -> {
				if (qs.hasQuestItems(LARAS_MEMO)) {
					qs.takeItems(LARAS_MEMO, -1);
					qs.giveItems(AKLANTOTH_GEM_3, 1);
					if (qs.hasQuestItems(AKLANTOTH_GEM_1) && qs.hasQuestItems(AKLANTOTH_GEM_2) && qs.getInt("gem456") == 6) {
						qs.setCond(3);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case SKELETAL_MERCENARY -> {
				int gem456 = qs.getInt("gem456");
				if (gem456 == 3) {
					qs.set("gem456", "4");
					qs.giveItems(AKLANTOTH_GEM_4, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				} else if (gem456 == 4) {
					qs.set("gem456", "5");
					qs.giveItems(AKLANTOTH_GEM_5, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				} else if (gem456 == 5) {
					qs.set("gem456", "6");
					qs.takeItems(LEOPOLDS_JOURNAL, -1);
					qs.giveItems(AKLANTOTH_GEM_6, 1);
					if (qs.hasQuestItems(AKLANTOTH_GEM_1) && qs.hasQuestItems(AKLANTOTH_GEM_2) && qs.hasQuestItems(AKLANTOTH_GEM_3)) {
						qs.setCond(3);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
			case TAMLIN_ORC, TAMLIN_ORC_ARCHER -> {
				if (qs.hasQuestItems(VADINS_CRUCIFIX) && qs.getQuestItemsCount(TAMLIN_ORC_AMULET) < 20) {
					qs.giveItems(TAMLIN_ORC_AMULET, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case DREVANUL_PRINCE_ZERUEL -> {
				if (cond == 4) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else if (cond == 9) {
					qs.takeItems(BRIMSTONE2, -1);
					qs.takeItems(SOULTRAP_CRYSTAL, -1);
					qs.giveItems(PURGATORY_KEY, 1);
					qs.giveItems(ZERUEL_BIND_CRYSTAL, 1);
					qs.setCond(10);
					qs.setCond(14);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}
