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
 * Quest 220: Testimony of Glory
 * 2º Passo de 2ª Classe para todas as classes da raça Orc (Orc Raider, Orc
 * Monk, Orc Shaman).
 */
@Component
public class Quest220TestimonyOfGlory extends Quest {

	public static final int QUEST_ID = 220;
	public static final String QUEST_NAME = "220_TestimonyOfGlory";

	// NPCs
	public static final int KASMAN = 30501;
	public static final int VOKIYAN = 30514;
	public static final int MANAKIA = 30515;
	public static final int KAKAI = 30565;
	public static final int TANAPI = 30571;
	public static final int VOLTAR = 30615;
	public static final int KEPRA = 30616;
	public static final int BURAI = 30617;
	public static final int HARAK = 30618;
	public static final int DRIKO = 30619;
	public static final int CHIANTA = 30642;

	// Monstros
	public static final int TYRANT = 20192;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int GUARDIAN_BASILISK = 20550;
	public static final int MANASHEN_GARGOYLE = 20563;
	public static final int TIMAK_ORC = 20583;
	public static final int TIMAK_ORC_ARCHER = 20584;
	public static final int TIMAK_ORC_SOLDIER = 20585;
	public static final int TIMAK_ORC_WARRIOR = 20586;
	public static final int TIMAK_ORC_SHAMAN = 20587;
	public static final int TIMAK_ORC_OVERLORD = 20588;
	public static final int TAMLIN_ORC = 20601;
	public static final int TAMLIN_ORC_ARCHER = 20602;
	public static final int RAGNA_ORC_OVERLORD = 20778;
	public static final int RAGNA_ORC_SEER = 20779;
	public static final int PASHIKA_SON_OF_VOLTAR = 27080;
	public static final int VULTUS_SON_OF_VOLTAR = 27081;
	public static final int ENKU_ORC_OVERLORD = 27082;
	public static final int MAKUM_BUGBEAR_THUG = 27083;
	public static final int REVENANT_OF_TANTOS_CHIEF = 27086;

	// Itens
	public static final int MARK_OF_GLORY = 3203;
	public static final int VOKIYANS_ORDER1 = 3204;
	public static final int MANASHEN_SHARD = 3205;
	public static final int TYRANT_TALON = 3206;
	public static final int GUARDIAN_BASILISK_FANG = 3207;
	public static final int VOKIYANS_ORDER2 = 3208;
	public static final int NECKLACE_OF_AUTHORITY = 3209;
	public static final int CHIANTAS_ORDER1 = 3210;
	public static final int SCEPTER_OF_BREKA = 3211;
	public static final int SCEPTER_OF_ENKU = 3212;
	public static final int SCEPTER_OF_VUKU = 3213;
	public static final int SCEPTER_OF_TUREK = 3214;
	public static final int SCEPTER_OF_TUNATH = 3215;
	public static final int CHIANTAS_ORDER2 = 3216;
	public static final int CHIANTAS_ORDER3 = 3217;
	public static final int TAMLIN_ORC_SKULL = 3218;
	public static final int TIMAK_ORC_HEAD = 3219;
	public static final int SCEPTER_BOX = 3220;
	public static final int PASHIKA_HEAD = 3221;
	public static final int VULTUS_HEAD = 3222;
	public static final int GLOVE_OF_VOLTAR = 3223;
	public static final int ENKU_OVERLORD_HEAD = 3224;
	public static final int GLOVE_OF_KEPRA = 3225;
	public static final int MAKUM_BUGBEAR_HEAD = 3226;
	public static final int GLOVE_OF_BURAI = 3227;
	public static final int MANAKIA_LETTER_1 = 3228;
	public static final int MANAKIA_LETTER_2 = 3229;
	public static final int KASMAN_LETTER_1 = 3230;
	public static final int KASMAN_LETTER_2 = 3231;
	public static final int KASMAN_LETTER_3 = 3232;
	public static final int DRIKO_CONTRACT = 3233;
	public static final int STAKATO_DRONE_HUSK = 3234;
	public static final int TANAPI_ORDER = 3235;
	public static final int SCEPTER_OF_TANTOS = 3236;
	public static final int RITUAL_BOX = 3237;

	private static final Set<Integer> VALID_CLASSES = Set.of(45, 47, 50);

	@Autowired
	public Quest220TestimonyOfGlory(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Glory");

		addStartNpc(VOKIYAN);
		addTalkId(KASMAN, VOKIYAN, MANAKIA, KAKAI, TANAPI, VOLTAR, KEPRA, BURAI, HARAK, DRIKO, CHIANTA);

		addAttackId(RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER, REVENANT_OF_TANTOS_CHIEF);
		addKillId(TYRANT, MARSH_STAKATO_DRONE, GUARDIAN_BASILISK, MANASHEN_GARGOYLE,
				TIMAK_ORC, TIMAK_ORC_ARCHER, TIMAK_ORC_SOLDIER, TIMAK_ORC_WARRIOR, TIMAK_ORC_SHAMAN, TIMAK_ORC_OVERLORD,
				TAMLIN_ORC, TAMLIN_ORC_ARCHER, RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER,
				PASHIKA_SON_OF_VOLTAR, VULTUS_SON_OF_VOLTAR, ENKU_ORC_OVERLORD, MAKUM_BUGBEAR_THUG,
				REVENANT_OF_TANTOS_CHIEF);

		registerQuestItems(VOKIYANS_ORDER1, MANASHEN_SHARD, TYRANT_TALON, GUARDIAN_BASILISK_FANG,
				VOKIYANS_ORDER2, NECKLACE_OF_AUTHORITY, CHIANTAS_ORDER1, SCEPTER_OF_BREKA, SCEPTER_OF_ENKU,
				SCEPTER_OF_VUKU, SCEPTER_OF_TUREK, SCEPTER_OF_TUNATH, CHIANTAS_ORDER2, CHIANTAS_ORDER3,
				TAMLIN_ORC_SKULL, TIMAK_ORC_HEAD, SCEPTER_BOX, PASHIKA_HEAD, VULTUS_HEAD, GLOVE_OF_VOLTAR,
				ENKU_OVERLORD_HEAD, GLOVE_OF_KEPRA, MAKUM_BUGBEAR_HEAD, GLOVE_OF_BURAI, MANAKIA_LETTER_1,
				MANAKIA_LETTER_2, KASMAN_LETTER_1, KASMAN_LETTER_2, KASMAN_LETTER_3, DRIKO_CONTRACT,
				STAKATO_DRONE_HUSK, TANAPI_ORDER, SCEPTER_OF_TANTOS, RITUAL_BOX);

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

		if ("1".equalsIgnoreCase(event) || "30514-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VOKIYANS_ORDER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30514-05.htm";
		} else if ("30642-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(VOKIYANS_ORDER2, -1);
			qs.giveItems(CHIANTAS_ORDER1, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30642-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CHIANTAS_ORDER1, -1);
			qs.takeItems(KASMAN_LETTER_1, -1);
			qs.takeItems(MANAKIA_LETTER_1, -1);
			qs.takeItems(MANAKIA_LETTER_2, -1);
			qs.takeItems(SCEPTER_OF_BREKA, -1);
			qs.takeItems(SCEPTER_OF_ENKU, -1);
			qs.takeItems(SCEPTER_OF_TUNATH, -1);
			qs.takeItems(SCEPTER_OF_TUREK, -1);
			qs.takeItems(SCEPTER_OF_VUKU, -1);
			if (player != null && player.level() < 37) {
				qs.giveItems(CHIANTAS_ORDER2, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30642-06.htm";
			}
			qs.giveItems(CHIANTAS_ORDER3, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30642-07.htm";
		} else if ("30501-02.htm".equalsIgnoreCase(event) && !qs.hasQuestItems(SCEPTER_OF_VUKU)) {
			qs.addRadar(-2150, 124443, -3724);
			if (qs.hasQuestItems(KASMAN_LETTER_1)) {
				return "30501-04.htm";
			}
			qs.giveItems(KASMAN_LETTER_1, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30501-03.htm";
		} else if ("30501-05.htm".equalsIgnoreCase(event) && !qs.hasQuestItems(SCEPTER_OF_TUREK)) {
			qs.addRadar(-94294, 110818, -3563);
			if (qs.hasQuestItems(KASMAN_LETTER_2)) {
				return "30501-07.htm";
			}
			qs.giveItems(KASMAN_LETTER_2, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30501-06.htm";
		} else if ("30501-08.htm".equalsIgnoreCase(event) && !qs.hasQuestItems(SCEPTER_OF_TUNATH)) {
			qs.addRadar(-55217, 200628, -3724);
			if (qs.hasQuestItems(KASMAN_LETTER_3)) {
				return "30501-10.htm";
			}
			qs.giveItems(KASMAN_LETTER_3, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30501-09.htm";
		} else if ("30515-02.htm".equalsIgnoreCase(event) && !qs.hasQuestItems(SCEPTER_OF_BREKA)) {
			qs.addRadar(80100, 119991, -2264);
			if (qs.hasQuestItems(MANAKIA_LETTER_1)) {
				return "30515-04.htm";
			}
			qs.giveItems(MANAKIA_LETTER_1, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30515-03.htm";
		} else if ("30515-05.htm".equalsIgnoreCase(event) && !qs.hasQuestItems(SCEPTER_OF_ENKU)) {
			qs.addRadar(19815, 189703, -3032);
			if (qs.hasQuestItems(MANAKIA_LETTER_2)) {
				return "30515-07.htm";
			}
			qs.giveItems(MANAKIA_LETTER_2, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30515-06.htm";
		} else if ("30615-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MANAKIA_LETTER_1, -1);
			qs.giveItems(GLOVE_OF_VOLTAR, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			qs.addSpawn(PASHIKA_SON_OF_VOLTAR, 80117, 120039, -2259, 200000);
			qs.addSpawn(VULTUS_SON_OF_VOLTAR, 80058, 120038, -2259, 200000);
			return "30615-04.htm";
		} else if ("30616-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MANAKIA_LETTER_2, -1);
			qs.giveItems(GLOVE_OF_KEPRA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			qs.addSpawn(ENKU_ORC_OVERLORD, 19894, 189743, -3074, 200000);
			qs.addSpawn(ENKU_ORC_OVERLORD, 19869, 189800, -3059, 200000);
			qs.addSpawn(ENKU_ORC_OVERLORD, 19818, 189818, -3047, 200000);
			qs.addSpawn(ENKU_ORC_OVERLORD, 19753, 189837, -3027, 200000);
			return "30616-05.htm";
		} else if ("30617-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KASMAN_LETTER_2, -1);
			qs.giveItems(GLOVE_OF_BURAI, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			qs.addSpawn(MAKUM_BUGBEAR_THUG, -94292, 110781, -3701, 200000);
			qs.addSpawn(MAKUM_BUGBEAR_THUG, -94293, 110861, -3701, 200000);
			return "30617-04.htm";
		} else if ("30618-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KASMAN_LETTER_3, -1);
			qs.giveItems(SCEPTER_OF_TUNATH, 1);
			checkAllScepters(qs);
			return "30618-03.htm";
		} else if ("30619-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KASMAN_LETTER_1, -1);
			qs.giveItems(DRIKO_CONTRACT, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30619-03.htm";
		} else if ("30571-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SCEPTER_BOX, -1);
			qs.giveItems(TANAPI_ORDER, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30571-03.htm";
		} else if ("30565-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RITUAL_BOX)) {
				qs.takeItems(RITUAL_BOX, -1);
				qs.giveItems(MARK_OF_GLORY, 1);
				qs.addExpAndSp(91457, 13000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private void checkAllScepters(QuestState qs) {
		if (qs.hasQuestItems(SCEPTER_OF_BREKA) && qs.hasQuestItems(SCEPTER_OF_ENKU)
				&& qs.hasQuestItems(SCEPTER_OF_VUKU) && qs.hasQuestItems(SCEPTER_OF_TUREK)
				&& qs.hasQuestItems(SCEPTER_OF_TUNATH)) {
			qs.setCond(5);
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

		if (npcId == VOKIYAN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30514-03.htm";
				} else {
					return "30514-02.htm";
				}
			} else if (cond == 1) {
				return "30514-06.htm";
			} else if (cond == 2 && qs.getQuestItemsCount(TYRANT_TALON) >= 10
					&& qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) >= 10
					&& qs.getQuestItemsCount(MANASHEN_SHARD) >= 10) {
				qs.takeItems(TYRANT_TALON, -1);
				qs.takeItems(GUARDIAN_BASILISK_FANG, -1);
				qs.takeItems(MANASHEN_SHARD, -1);
				qs.takeItems(VOKIYANS_ORDER1, -1);
				qs.giveItems(VOKIYANS_ORDER2, 1);
				qs.giveItems(NECKLACE_OF_AUTHORITY, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30514-08.htm";
			} else if (cond == 3) {
				return "30514-09.htm";
			} else if (cond == 8) {
				return "30514-10.htm";
			}
		} else if (npcId == CHIANTA) {
			if (cond == 3) {
				return "30642-01.htm";
			} else if (cond == 4) {
				return "30642-04.htm";
			} else if (cond == 5) {
				if (qs.hasQuestItems(CHIANTAS_ORDER2)) {
					if (player != null && player.level() >= 37) {
						qs.takeItems(CHIANTAS_ORDER2, -1);
						qs.giveItems(CHIANTAS_ORDER3, 1);
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
						return "30642-09.htm";
					}
					return "30642-08.htm";
				}
				return "30642-05.htm";
			} else if (cond == 6) {
				return "30642-10.htm";
			} else if (cond == 7) {
				qs.takeItems(CHIANTAS_ORDER3, -1);
				qs.takeItems(NECKLACE_OF_AUTHORITY, -1);
				qs.takeItems(TAMLIN_ORC_SKULL, -1);
				qs.takeItems(TIMAK_ORC_HEAD, -1);
				qs.giveItems(SCEPTER_BOX, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30642-11.htm";
			} else if (cond == 8) {
				return "30642-12.htm";
			} else if (cond > 8) {
				return "30642-13.htm";
			}
		} else if (npcId == KASMAN) {
			if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
				return "30501-01.htm";
			} else if (cond > 4) {
				return "30501-11.htm";
			}
		} else if (npcId == MANAKIA) {
			if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
				return "30515-01.htm";
			} else if (cond > 4) {
				return "30515-08.htm";
			}
		} else if (npcId == VOLTAR) {
			if (cond > 3) {
				if (qs.hasQuestItems(MANAKIA_LETTER_1)) {
					qs.removeRadar(80100, 119991, -2264);
					return "30615-02.htm";
				} else if (qs.hasQuestItems(GLOVE_OF_VOLTAR)) {
					qs.addSpawn(PASHIKA_SON_OF_VOLTAR, 80117, 120039, -2259, 200000);
					qs.addSpawn(VULTUS_SON_OF_VOLTAR, 80058, 120038, -2259, 200000);
					return "30615-05.htm";
				} else if (qs.hasQuestItems(PASHIKA_HEAD) && qs.hasQuestItems(VULTUS_HEAD)) {
					qs.takeItems(PASHIKA_HEAD, -1);
					qs.takeItems(VULTUS_HEAD, -1);
					qs.giveItems(SCEPTER_OF_BREKA, 1);
					checkAllScepters(qs);
					return "30615-06.htm";
				} else if (qs.hasQuestItems(SCEPTER_OF_BREKA)) {
					return "30615-07.htm";
				} else if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
					return "30615-01.htm";
				} else if (cond < 9) {
					return "30615-08.htm";
				}
			}
		} else if (npcId == KEPRA) {
			if (cond > 3) {
				if (qs.hasQuestItems(MANAKIA_LETTER_2)) {
					qs.removeRadar(19815, 189703, -3032);
					return "30616-02.htm";
				} else if (qs.hasQuestItems(GLOVE_OF_KEPRA)) {
					qs.addSpawn(ENKU_ORC_OVERLORD, 19894, 189743, -3074, 200000);
					qs.addSpawn(ENKU_ORC_OVERLORD, 19869, 189800, -3059, 200000);
					qs.addSpawn(ENKU_ORC_OVERLORD, 19818, 189818, -3047, 200000);
					qs.addSpawn(ENKU_ORC_OVERLORD, 19753, 189837, -3027, 200000);
					return "30616-05.htm";
				} else if (qs.getQuestItemsCount(ENKU_OVERLORD_HEAD) >= 4) {
					qs.takeItems(ENKU_OVERLORD_HEAD, -1);
					qs.giveItems(SCEPTER_OF_ENKU, 1);
					checkAllScepters(qs);
					return "30616-06.htm";
				} else if (qs.hasQuestItems(SCEPTER_OF_ENKU)) {
					return "30616-07.htm";
				} else if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
					return "30616-01.htm";
				} else if (cond < 9) {
					return "30616-08.htm";
				}
			}
		} else if (npcId == BURAI) {
			if (cond > 3) {
				if (qs.hasQuestItems(KASMAN_LETTER_2)) {
					qs.removeRadar(-94294, 110818, -3563);
					return "30617-02.htm";
				} else if (qs.hasQuestItems(GLOVE_OF_BURAI)) {
					qs.addSpawn(MAKUM_BUGBEAR_THUG, -94292, 110781, -3701, 200000);
					qs.addSpawn(MAKUM_BUGBEAR_THUG, -94293, 110861, -3701, 200000);
					return "30617-04.htm";
				} else if (qs.getQuestItemsCount(MAKUM_BUGBEAR_HEAD) >= 2) {
					qs.takeItems(MAKUM_BUGBEAR_HEAD, -1);
					qs.giveItems(SCEPTER_OF_TUREK, 1);
					checkAllScepters(qs);
					return "30617-05.htm";
				} else if (qs.hasQuestItems(SCEPTER_OF_TUREK)) {
					return "30617-06.htm";
				} else if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
					return "30617-01.htm";
				} else if (cond < 8) {
					return "30617-07.htm";
				}
			}
		} else if (npcId == HARAK) {
			if (cond > 3) {
				if (qs.hasQuestItems(KASMAN_LETTER_3)) {
					qs.removeRadar(-55217, 200628, -3724);
					return "30618-02.htm";
				} else if (qs.hasQuestItems(SCEPTER_OF_TUNATH)) {
					return "30618-04.htm";
				} else if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
					return "30618-01.htm";
				} else if (cond < 9) {
					return "30618-05.htm";
				}
			}
		} else if (npcId == DRIKO) {
			if (cond > 3) {
				if (qs.hasQuestItems(KASMAN_LETTER_1)) {
					qs.removeRadar(-2150, 124443, -3724);
					return "30619-02.htm";
				} else if (qs.hasQuestItems(DRIKO_CONTRACT)) {
					if (qs.getQuestItemsCount(STAKATO_DRONE_HUSK) >= 30) {
						qs.takeItems(DRIKO_CONTRACT, -1);
						qs.takeItems(STAKATO_DRONE_HUSK, -1);
						qs.giveItems(SCEPTER_OF_VUKU, 1);
						checkAllScepters(qs);
						return "30619-05.htm";
					}
					return "30619-04.htm";
				} else if (qs.hasQuestItems(SCEPTER_OF_VUKU)) {
					return "30619-06.htm";
				} else if (qs.hasQuestItems(CHIANTAS_ORDER1)) {
					return "30619-01.htm";
				} else if (cond < 8) {
					return "30619-07.htm";
				}
			}
		} else if (npcId == TANAPI) {
			if (cond == 8) {
				return "30571-01.htm";
			} else if (cond == 9) {
				return "30571-04.htm";
			} else if (cond == 10) {
				qs.takeItems(SCEPTER_OF_TANTOS, -1);
				qs.takeItems(TANAPI_ORDER, -1);
				qs.giveItems(RITUAL_BOX, 1);
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30571-05.htm";
			} else if (cond == 11) {
				return "30571-06.htm";
			}
		} else if (npcId == KAKAI) {
			if (cond == 10 && qs.hasQuestItems(RITUAL_BOX)) {
				return "30565-01.htm";
			} else if (cond == 11 && qs.hasQuestItems(RITUAL_BOX)) {
				qs.takeItems(RITUAL_BOX, -1);
				qs.giveItems(MARK_OF_GLORY, 1);
				qs.addExpAndSp(91457, 13000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30565-02.htm";
			} else if (cond > 7 && cond < 11) {
				return "30565-01.htm";
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

		if (cond == 1) {
			if (npcId == TYRANT && qs.getQuestItemsCount(TYRANT_TALON) < 10) {
				qs.giveItems(TYRANT_TALON, 1);
			} else if (npcId == GUARDIAN_BASILISK && qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) < 10) {
				qs.giveItems(GUARDIAN_BASILISK_FANG, 1);
			} else if (npcId == MANASHEN_GARGOYLE && qs.getQuestItemsCount(MANASHEN_SHARD) < 10) {
				qs.giveItems(MANASHEN_SHARD, 1);
			}

			if (qs.getQuestItemsCount(TYRANT_TALON) >= 10
					&& qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) >= 10
					&& qs.getQuestItemsCount(MANASHEN_SHARD) >= 10) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == MARSH_STAKATO_DRONE && qs.hasQuestItems(DRIKO_CONTRACT)) {
			if (qs.getQuestItemsCount(STAKATO_DRONE_HUSK) < 30) {
				qs.giveItems(STAKATO_DRONE_HUSK, 1);
				qs.playSound(qs.getQuestItemsCount(STAKATO_DRONE_HUSK) >= 30 ? QuestState.SOUND_MIDDLE
						: QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == PASHIKA_SON_OF_VOLTAR && qs.hasQuestItems(GLOVE_OF_VOLTAR)
				&& !qs.hasQuestItems(PASHIKA_HEAD)) {
			qs.giveItems(PASHIKA_HEAD, 1);
			if (qs.hasQuestItems(VULTUS_HEAD)) {
				qs.takeItems(GLOVE_OF_VOLTAR, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == VULTUS_SON_OF_VOLTAR && qs.hasQuestItems(GLOVE_OF_VOLTAR)
				&& !qs.hasQuestItems(VULTUS_HEAD)) {
			qs.giveItems(VULTUS_HEAD, 1);
			if (qs.hasQuestItems(PASHIKA_HEAD)) {
				qs.takeItems(GLOVE_OF_VOLTAR, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == ENKU_ORC_OVERLORD && qs.hasQuestItems(GLOVE_OF_KEPRA)) {
			if (qs.getQuestItemsCount(ENKU_OVERLORD_HEAD) < 4) {
				qs.giveItems(ENKU_OVERLORD_HEAD, 1);
				if (qs.getQuestItemsCount(ENKU_OVERLORD_HEAD) >= 4) {
					qs.takeItems(GLOVE_OF_KEPRA, -1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (npcId == MAKUM_BUGBEAR_THUG && qs.hasQuestItems(GLOVE_OF_BURAI)) {
			if (qs.getQuestItemsCount(MAKUM_BUGBEAR_HEAD) < 2) {
				qs.giveItems(MAKUM_BUGBEAR_HEAD, 1);
				if (qs.getQuestItemsCount(MAKUM_BUGBEAR_HEAD) >= 2) {
					qs.takeItems(GLOVE_OF_BURAI, -1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 6) {
			if (npcId >= TIMAK_ORC && npcId <= TIMAK_ORC_OVERLORD && qs.getQuestItemsCount(TIMAK_ORC_HEAD) < 20) {
				qs.giveItems(TIMAK_ORC_HEAD, 1);
			} else if ((npcId == TAMLIN_ORC || npcId == TAMLIN_ORC_ARCHER)
					&& qs.getQuestItemsCount(TAMLIN_ORC_SKULL) < 20) {
				qs.giveItems(TAMLIN_ORC_SKULL, 1);
			}

			if (qs.getQuestItemsCount(TIMAK_ORC_HEAD) >= 20 && qs.getQuestItemsCount(TAMLIN_ORC_SKULL) >= 20) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 9 && (npcId == RAGNA_ORC_OVERLORD || npcId == RAGNA_ORC_SEER)) {
			qs.addSpawn(REVENANT_OF_TANTOS_CHIEF);
		} else if (cond == 9 && npcId == REVENANT_OF_TANTOS_CHIEF && !qs.hasQuestItems(SCEPTER_OF_TANTOS)) {
			qs.giveItems(SCEPTER_OF_TANTOS, 1);
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
