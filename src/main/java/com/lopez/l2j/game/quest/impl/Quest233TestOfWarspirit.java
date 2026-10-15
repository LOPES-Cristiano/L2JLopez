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
 * Quest 233: Test of the Warspirit
 * 3º Passo de 2ª Classe para Warcryer.
 */
@Component
public class Quest233TestOfWarspirit extends Quest {

	public static final int QUEST_ID = 233;
	public static final String QUEST_NAME = "233_TestOfWarspirit";

	// NPCs
	public static final int VIVYAN = 30030;
	public static final int SARIEN = 30436;
	public static final int RACOY = 30507;
	public static final int SOMAK = 30510;
	public static final int MANAKIA = 30515;
	public static final int ORIM = 30630;
	public static final int ANCESTOR_MARTANKUS = 30649;
	public static final int PEKIRON = 30682;

	// Monstros
	public static final int NOBLE_ANT = 20089;
	public static final int NOBLE_ANT_LEADER = 20090;
	public static final int MEDUSA = 20158;
	public static final int PORTA = 20213;
	public static final int EXCURO = 20214;
	public static final int MORDEO = 20215;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int TAMLIN_ORC = 20601;
	public static final int TAMLIN_ORC_ARCHER = 20602;
	public static final int STENOA_GORGON_QUEEN = 27108;

	// Itens
	public static final int MARK_OF_WARSPIRIT = 2879;
	public static final int VENDETTA_TOTEM = 2880;
	public static final int TAMLIN_ORC_HEAD = 2881;
	public static final int WARSPIRIT_TOTEM = 2882;
	public static final int ORIM_CONTRACT = 2883;
	public static final int PORTA_EYE = 2884;
	public static final int EXCURO_SCALE = 2885;
	public static final int MORDEO_TALON = 2886;
	public static final int BRAKIS_REMAINS1 = 2887;
	public static final int PEKIRON_TOTEM = 2888;
	public static final int TONAR_SKULL = 2889;
	public static final int TONAR_RIBBONE = 2890;
	public static final int TONAR_SPINE = 2891;
	public static final int TONAR_ARMBONE = 2892;
	public static final int TONAR_THIGHBONE = 2893;
	public static final int TONARS_REMAINS1 = 2894;
	public static final int MANAKIA_TOTEM = 2895;
	public static final int HERMODT_SKULL = 2896;
	public static final int HERMODT_RIBBONE = 2897;
	public static final int HERMODT_SPINE = 2898;
	public static final int HERMODT_ARMBONE = 2899;
	public static final int HERMODT_THIGHBONE = 2900;
	public static final int HERMODTS_REMAINS1 = 2901;
	public static final int RACOY_TOTEM = 2902;
	public static final int VIVYAN_LETTER = 2903;
	public static final int INSECT_DIAGRAM_BOOK = 2904;
	public static final int KIRUNA_SKULL = 2905;
	public static final int KIRUNA_RIBBONE = 2906;
	public static final int KIRUNA_SPINE = 2907;
	public static final int KIRUNA_ARMBONE = 2908;
	public static final int KIRUNA_THIGHBONE = 2909;
	public static final int KIRUNAS_REMAINS1 = 2910;
	public static final int BRAKI_REMAINS_2 = 2911;
	public static final int TONAR_REMAINS_2 = 2912;
	public static final int HERMODT_REMAINS_2 = 2913;
	public static final int KIRUNA_REMAINS_2 = 2914;

	private static final Set<Integer> VALID_CLASSES = Set.of(50); // Orc Shaman

	@Autowired
	public Quest233TestOfWarspirit(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Warspirit");

		addStartNpc(SOMAK);
		addTalkId(SOMAK, VIVYAN, SARIEN, RACOY, MANAKIA, ORIM, ANCESTOR_MARTANKUS, PEKIRON);

		addKillId(NOBLE_ANT, NOBLE_ANT_LEADER, MEDUSA, PORTA, EXCURO, MORDEO,
				LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD,
				TAMLIN_ORC, TAMLIN_ORC_ARCHER, STENOA_GORGON_QUEEN);

		registerQuestItems(VENDETTA_TOTEM, TAMLIN_ORC_HEAD, WARSPIRIT_TOTEM, ORIM_CONTRACT,
				PORTA_EYE, EXCURO_SCALE, MORDEO_TALON, BRAKIS_REMAINS1, PEKIRON_TOTEM,
				TONAR_SKULL, TONAR_RIBBONE, TONAR_SPINE, TONAR_ARMBONE, TONAR_THIGHBONE,
				TONARS_REMAINS1, MANAKIA_TOTEM, HERMODT_SKULL, HERMODT_RIBBONE,
				HERMODT_SPINE, HERMODT_ARMBONE, HERMODT_THIGHBONE, HERMODTS_REMAINS1,
				RACOY_TOTEM, VIVYAN_LETTER, INSECT_DIAGRAM_BOOK, KIRUNA_SKULL,
				KIRUNA_RIBBONE, KIRUNA_SPINE, KIRUNA_ARMBONE, KIRUNA_THIGHBONE,
				KIRUNAS_REMAINS1, BRAKI_REMAINS_2, TONAR_REMAINS_2,
				HERMODT_REMAINS_2, KIRUNA_REMAINS_2);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30510-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30510-05.htm";
		} else if ("30630-04.htm".equalsIgnoreCase(event) || "30630_1".equalsIgnoreCase(event)) {
			qs.giveItems(ORIM_CONTRACT, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30630-04.htm";
		} else if ("30507-02.htm".equalsIgnoreCase(event) || "30507_1".equalsIgnoreCase(event)) {
			qs.giveItems(RACOY_TOTEM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30507-02.htm";
		} else if ("30030-04.htm".equalsIgnoreCase(event) || "30030_1".equalsIgnoreCase(event)) {
			qs.giveItems(VIVYAN_LETTER, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30030-04.htm";
		} else if ("30682-02.htm".equalsIgnoreCase(event) || "30682_1".equalsIgnoreCase(event)) {
			qs.giveItems(PEKIRON_TOTEM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30682-02.htm";
		} else if ("30515-02.htm".equalsIgnoreCase(event) || "30515_1".equalsIgnoreCase(event)) {
			qs.giveItems(MANAKIA_TOTEM, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30515-02.htm";
		} else if ("30649-03.htm".equalsIgnoreCase(event) || "30649_1".equalsIgnoreCase(event)) {
			qs.takeItems(TAMLIN_ORC_HEAD, -1);
			qs.takeItems(WARSPIRIT_TOTEM, -1);
			qs.takeItems(BRAKI_REMAINS_2, -1);
			qs.takeItems(HERMODT_REMAINS_2, -1);
			qs.takeItems(KIRUNA_REMAINS_2, -1);
			qs.takeItems(TONAR_REMAINS_2, -1);
			qs.giveItems(MARK_OF_WARSPIRIT, 1);
			qs.addExpAndSp(118304, 26250);
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitCurrentQuest(false);
			return "30649-03.htm";
		} else if ("30510-09.htm".equalsIgnoreCase(event)) {
			if (hasAllFourRemains(qs)) {
				qs.takeItems(BRAKIS_REMAINS1, -1);
				qs.takeItems(HERMODTS_REMAINS1, -1);
				qs.takeItems(KIRUNAS_REMAINS1, -1);
				qs.takeItems(TONARS_REMAINS1, -1);
				qs.giveItems(MARK_OF_WARSPIRIT, 1);
				qs.addExpAndSp(118304, 26250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private boolean hasAllFourRemains(QuestState qs) {
		return qs.hasQuestItems(BRAKIS_REMAINS1) && qs.hasQuestItems(HERMODTS_REMAINS1)
				&& qs.hasQuestItems(KIRUNAS_REMAINS1) && qs.hasQuestItems(TONARS_REMAINS1);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == SOMAK) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30510-03.htm";
				} else {
					return "30510-01.htm";
				}
			} else if (cond == 1) {
				if (hasAllFourRemains(qs)) {
					return "30510-08.htm";
				}
				return "30510-06.htm";
			} else if (cond == 2) {
				qs.takeItems(BRAKIS_REMAINS1, -1);
				qs.takeItems(HERMODTS_REMAINS1, -1);
				qs.takeItems(KIRUNAS_REMAINS1, -1);
				qs.takeItems(TONARS_REMAINS1, -1);
				qs.giveItems(VENDETTA_TOTEM, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30510-07.htm";
			} else if (cond == 3) {
				return "30510-08.htm";
			} else if (cond == 4) {
				qs.takeItems(VENDETTA_TOTEM, -1);
				qs.giveItems(BRAKI_REMAINS_2, 1);
				qs.giveItems(HERMODT_REMAINS_2, 1);
				qs.giveItems(KIRUNA_REMAINS_2, 1);
				qs.giveItems(TONAR_REMAINS_2, 1);
				qs.giveItems(WARSPIRIT_TOTEM, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30510-09.htm";
			} else if (cond == 5) {
				return "30510-10.htm";
			}
		} else if (npcId == ORIM) {
			if (cond == 1 && !qs.hasQuestItems(BRAKIS_REMAINS1)) {
				if (qs.hasQuestItems(ORIM_CONTRACT)) {
					if (qs.getQuestItemsCount(PORTA_EYE) + qs.getQuestItemsCount(EXCURO_SCALE) + qs.getQuestItemsCount(MORDEO_TALON) >= 30) {
						qs.takeItems(EXCURO_SCALE, -1);
						qs.takeItems(MORDEO_TALON, -1);
						qs.takeItems(PORTA_EYE, -1);
						qs.takeItems(ORIM_CONTRACT, -1);
						qs.giveItems(BRAKIS_REMAINS1, 1);
						if (hasAllFourRemains(qs)) {
							qs.setCond(2);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
						return "30630-06.htm";
					}
					return "30630-05.htm";
				}
				qs.giveItems(BRAKIS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30630-05.htm";
			}
			return "30630-07.htm";
		} else if (npcId == MANAKIA) {
			if (cond == 1 && !qs.hasQuestItems(HERMODTS_REMAINS1)) {
				if (qs.hasQuestItems(MANAKIA_TOTEM)) {
					if (qs.hasQuestItems(HERMODT_ARMBONE) && qs.hasQuestItems(HERMODT_RIBBONE)
							&& qs.hasQuestItems(HERMODT_SKULL) && qs.hasQuestItems(HERMODT_SPINE)
							&& qs.hasQuestItems(HERMODT_THIGHBONE)) {
						qs.takeItems(MANAKIA_TOTEM, -1);
						qs.takeItems(HERMODT_ARMBONE, -1);
						qs.takeItems(HERMODT_RIBBONE, -1);
						qs.takeItems(HERMODT_SKULL, -1);
						qs.takeItems(HERMODT_SPINE, -1);
						qs.takeItems(HERMODT_THIGHBONE, -1);
						qs.giveItems(HERMODTS_REMAINS1, 1);
						if (hasAllFourRemains(qs)) {
							qs.setCond(2);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
						return "30515-04.htm";
					}
					return "30515-03.htm";
				}
				qs.giveItems(HERMODTS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30515-05.htm";
			}
			return "30515-05.htm";
		} else if (npcId == PEKIRON) {
			if (cond == 1 && !qs.hasQuestItems(TONARS_REMAINS1)) {
				if (qs.hasQuestItems(PEKIRON_TOTEM)) {
					if (qs.hasQuestItems(TONAR_ARMBONE) && qs.hasQuestItems(TONAR_RIBBONE)
							&& qs.hasQuestItems(TONAR_SKULL) && qs.hasQuestItems(TONAR_SPINE)
							&& qs.hasQuestItems(TONAR_THIGHBONE)) {
						qs.takeItems(PEKIRON_TOTEM, -1);
						qs.takeItems(TONAR_ARMBONE, -1);
						qs.takeItems(TONAR_RIBBONE, -1);
						qs.takeItems(TONAR_SKULL, -1);
						qs.takeItems(TONAR_SPINE, -1);
						qs.takeItems(TONAR_THIGHBONE, -1);
						qs.giveItems(TONARS_REMAINS1, 1);
						if (hasAllFourRemains(qs)) {
							qs.setCond(2);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
						return "30682-04.htm";
					}
					return "30682-03.htm";
				}
				qs.giveItems(TONARS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30682-05.htm";
			}
			return "30682-05.htm";
		} else if (npcId == RACOY) {
			if (cond == 1 && !qs.hasQuestItems(KIRUNAS_REMAINS1)) {
				if (qs.hasQuestItems(RACOY_TOTEM)) {
					if (qs.hasQuestItems(VIVYAN_LETTER)) {
						return "30507-04.htm";
					} else if (qs.hasQuestItems(INSECT_DIAGRAM_BOOK)) {
						if (qs.hasQuestItems(KIRUNA_ARMBONE) && qs.hasQuestItems(KIRUNA_RIBBONE)
								&& qs.hasQuestItems(KIRUNA_SKULL) && qs.hasQuestItems(KIRUNA_SPINE)
								&& qs.hasQuestItems(KIRUNA_THIGHBONE)) {
							qs.takeItems(INSECT_DIAGRAM_BOOK, -1);
							qs.takeItems(RACOY_TOTEM, -1);
							qs.takeItems(KIRUNA_ARMBONE, -1);
							qs.takeItems(KIRUNA_RIBBONE, -1);
							qs.takeItems(KIRUNA_SKULL, -1);
							qs.takeItems(KIRUNA_SPINE, -1);
							qs.takeItems(KIRUNA_THIGHBONE, -1);
							qs.giveItems(KIRUNAS_REMAINS1, 1);
							if (hasAllFourRemains(qs)) {
								qs.setCond(2);
								qs.playSound(QuestState.SOUND_MIDDLE);
							} else {
								qs.playSound(QuestState.SOUND_ITEMGET);
							}
							return "30507-06.htm";
						}
						return "30507-05.htm";
					}
					return "30507-03.htm";
				}
				qs.giveItems(KIRUNAS_REMAINS1, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30507-05.htm";
			}
			return "30507-07.htm";
		} else if (npcId == VIVYAN) {
			if (cond == 1 && qs.hasQuestItems(RACOY_TOTEM)) {
				if (qs.hasQuestItems(VIVYAN_LETTER)) return "30030-05.htm";
				if (qs.hasQuestItems(INSECT_DIAGRAM_BOOK)) return "30030-06.htm";
				return "30030-01.htm";
			}
			return "30030-07.htm";
		} else if (npcId == SARIEN) {
			if (cond == 1 && qs.hasQuestItems(RACOY_TOTEM)) {
				if (qs.hasQuestItems(VIVYAN_LETTER)) {
					qs.takeItems(VIVYAN_LETTER, -1);
					qs.giveItems(INSECT_DIAGRAM_BOOK, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30436-01.htm";
				} else if (qs.hasQuestItems(INSECT_DIAGRAM_BOOK)) {
					return "30436-02.htm";
				}
			}
			return "30436-03.htm";
		} else if (npcId == ANCESTOR_MARTANKUS) {
			if (cond == 5) {
				return "30649-01.htm";
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
			case PORTA -> {
				if (qs.hasQuestItems(ORIM_CONTRACT) && qs.getQuestItemsCount(PORTA_EYE) < 10) {
					qs.giveItems(PORTA_EYE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case EXCURO -> {
				if (qs.hasQuestItems(ORIM_CONTRACT) && qs.getQuestItemsCount(EXCURO_SCALE) < 10) {
					qs.giveItems(EXCURO_SCALE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MORDEO -> {
				if (qs.hasQuestItems(ORIM_CONTRACT) && qs.getQuestItemsCount(MORDEO_TALON) < 10) {
					qs.giveItems(MORDEO_TALON, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case NOBLE_ANT, NOBLE_ANT_LEADER -> {
				if (qs.hasQuestItems(INSECT_DIAGRAM_BOOK)) {
					if (!qs.hasQuestItems(KIRUNA_SKULL)) qs.giveItems(KIRUNA_SKULL, 1);
					else if (!qs.hasQuestItems(KIRUNA_RIBBONE)) qs.giveItems(KIRUNA_RIBBONE, 1);
					else if (!qs.hasQuestItems(KIRUNA_SPINE)) qs.giveItems(KIRUNA_SPINE, 1);
					else if (!qs.hasQuestItems(KIRUNA_ARMBONE)) qs.giveItems(KIRUNA_ARMBONE, 1);
					else if (!qs.hasQuestItems(KIRUNA_THIGHBONE)) qs.giveItems(KIRUNA_THIGHBONE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD -> {
				if (qs.hasQuestItems(PEKIRON_TOTEM)) {
					if (!qs.hasQuestItems(TONAR_SKULL)) qs.giveItems(TONAR_SKULL, 1);
					else if (!qs.hasQuestItems(TONAR_RIBBONE)) qs.giveItems(TONAR_RIBBONE, 1);
					else if (!qs.hasQuestItems(TONAR_SPINE)) qs.giveItems(TONAR_SPINE, 1);
					else if (!qs.hasQuestItems(TONAR_ARMBONE)) qs.giveItems(TONAR_ARMBONE, 1);
					else if (!qs.hasQuestItems(TONAR_THIGHBONE)) qs.giveItems(TONAR_THIGHBONE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MEDUSA -> {
				if (qs.hasQuestItems(MANAKIA_TOTEM)) {
					if (!qs.hasQuestItems(HERMODT_RIBBONE)) qs.giveItems(HERMODT_RIBBONE, 1);
					else if (!qs.hasQuestItems(HERMODT_SPINE)) qs.giveItems(HERMODT_SPINE, 1);
					else if (!qs.hasQuestItems(HERMODT_ARMBONE)) qs.giveItems(HERMODT_ARMBONE, 1);
					else if (!qs.hasQuestItems(HERMODT_THIGHBONE)) qs.giveItems(HERMODT_THIGHBONE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case STENOA_GORGON_QUEEN -> {
				if (qs.hasQuestItems(MANAKIA_TOTEM) && !qs.hasQuestItems(HERMODT_SKULL)) {
					qs.giveItems(HERMODT_SKULL, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			case TAMLIN_ORC, TAMLIN_ORC_ARCHER -> {
				if (qs.hasQuestItems(VENDETTA_TOTEM) && qs.getQuestItemsCount(TAMLIN_ORC_HEAD) < 13) {
					qs.giveItems(TAMLIN_ORC_HEAD, 1);
					if (qs.getQuestItemsCount(TAMLIN_ORC_HEAD) >= 13) {
						qs.setCond(4);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}

		return null;
	}
}
