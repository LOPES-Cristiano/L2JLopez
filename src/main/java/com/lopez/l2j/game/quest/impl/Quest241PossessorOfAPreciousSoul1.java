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
 * Quest 241: Possessor of a Precious Soul - Part 1
 * Início da jornada de Noblesse em Rune Castle Town.
 */
@Component
public class Quest241PossessorOfAPreciousSoul1 extends Quest {

	public static final int QUEST_ID = 241;
	public static final String QUEST_NAME = "241_PossessorOfAPreciousSoul_1";

	// NPCs
	public static final int STEDMIEL = 30692;
	public static final int GABRIELLE = 30753;
	public static final int GILMORE = 30754;
	public static final int KANTABILON = 31042;
	public static final int NOEL = 31272;
	public static final int RAHORAKTI = 31336;
	public static final int TALIEN = 31739;
	public static final int CARADINE = 31740;
	public static final int VIRGIL = 31742;
	public static final int KASSANDRA = 31743;
	public static final int OGMAR = 31744;

	// Monstros
	public static final int BARAHAM = 27113;
	public static final int MALRUK_SUCCUBUS_1 = 20244;
	public static final int MALRUK_SUCCUBUS_2 = 20245;
	public static final int MALRUK_SUCCUBUS_TAUREN_1 = 20283;
	public static final int MALRUK_SUCCUBUS_TAUREN_2 = 20284;
	public static final int SPLENDOR_1 = 21508;
	public static final int SPLENDOR_2 = 21509;
	public static final int SPLENDOR_3 = 21510;
	public static final int SPLENDOR_4 = 21511;
	public static final int SPLENDOR_5 = 21512;

	// Itens
	public static final int LEGEND_OF_SEVENTEEN = 7587;
	public static final int MALRUK_SUCCUBUS_CLAW = 7597;
	public static final int ECHO_CRYSTAL = 7589;
	public static final int POETRY_BOOK = 7588;
	public static final int CRIMSON_MOSS = 7598;
	public static final int RAHORAKTIS_MEDICINE = 7599;
	public static final int LUNARGENT = 6029;
	public static final int HELLFIRE_OIL = 6033;
	public static final int VIRGILS_LETTER = 7677;

	@Autowired
	public Quest241PossessorOfAPreciousSoul1(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Possessor of a Precious Soul - Part 1");

		addStartNpc(TALIEN);
		addTalkId(TALIEN, STEDMIEL, GABRIELLE, GILMORE, KANTABILON, NOEL, RAHORAKTI, CARADINE, VIRGIL, KASSANDRA, OGMAR);

		addKillId(BARAHAM, MALRUK_SUCCUBUS_1, MALRUK_SUCCUBUS_2, MALRUK_SUCCUBUS_TAUREN_1, MALRUK_SUCCUBUS_TAUREN_2,
				SPLENDOR_1, SPLENDOR_2, SPLENDOR_3, SPLENDOR_4, SPLENDOR_5);

		registerQuestItems(LEGEND_OF_SEVENTEEN, MALRUK_SUCCUBUS_CLAW, ECHO_CRYSTAL, POETRY_BOOK, CRIMSON_MOSS, RAHORAKTIS_MEDICINE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("31739-4.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30753-2.htm".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30754-2.htm".equalsIgnoreCase(event)) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31739-8.htm".equalsIgnoreCase(event)) {
			if (cond == 4 && qs.hasQuestItems(LEGEND_OF_SEVENTEEN)) {
				qs.setCond(5);
				qs.takeItems(LEGEND_OF_SEVENTEEN, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31042-2.htm".equalsIgnoreCase(event)) {
			if (cond == 5) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31042-5.htm".equalsIgnoreCase(event)) {
			if (cond == 7 && qs.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) >= 10) {
				qs.setCond(8);
				qs.takeItems(MALRUK_SUCCUBUS_CLAW, 10);
				qs.giveItems(ECHO_CRYSTAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31739-12.htm".equalsIgnoreCase(event)) {
			if (cond == 8 && qs.hasQuestItems(ECHO_CRYSTAL)) {
				qs.setCond(9);
				qs.takeItems(ECHO_CRYSTAL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30692-2.htm".equalsIgnoreCase(event)) {
			if (cond == 9 && !qs.hasQuestItems(POETRY_BOOK)) {
				qs.setCond(10);
				qs.giveItems(POETRY_BOOK, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31739-15.htm".equalsIgnoreCase(event)) {
			if (cond == 10 && qs.hasQuestItems(POETRY_BOOK)) {
				qs.setCond(11);
				qs.takeItems(POETRY_BOOK, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31742-2.htm".equalsIgnoreCase(event)) {
			if (cond == 11) {
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31744-2.htm".equalsIgnoreCase(event)) {
			if (cond == 12) {
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31336-2.htm".equalsIgnoreCase(event)) {
			if (cond == 13) {
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31336-5.htm".equalsIgnoreCase(event)) {
			if (cond == 15 && qs.getQuestItemsCount(CRIMSON_MOSS) >= 5) {
				qs.setCond(16);
				qs.takeItems(CRIMSON_MOSS, 5);
				qs.giveItems(RAHORAKTIS_MEDICINE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31743-2.htm".equalsIgnoreCase(event)) {
			if (cond == 16 && qs.hasQuestItems(RAHORAKTIS_MEDICINE)) {
				qs.setCond(17);
				qs.takeItems(RAHORAKTIS_MEDICINE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31742-5.htm".equalsIgnoreCase(event)) {
			if (cond == 17) {
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31740-2.htm".equalsIgnoreCase(event)) {
			if (cond == 18) {
				qs.setCond(19);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31272-2.htm".equalsIgnoreCase(event)) {
			if (cond == 19) {
				qs.setCond(20);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("31272-5.htm".equalsIgnoreCase(event)) {
			if (cond == 20 && qs.getQuestItemsCount(LUNARGENT) >= 5 && qs.hasQuestItems(HELLFIRE_OIL)) {
				qs.setCond(21);
				qs.takeItems(LUNARGENT, 5);
				qs.takeItems(HELLFIRE_OIL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			} else {
				return "31272-4.htm";
			}
		} else if ("31740-5.htm".equalsIgnoreCase(event)) {
			if (cond == 21) {
				qs.giveItems(VIRGILS_LETTER, 1);
				qs.addExpAndSp(263043, 13206);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
			}
			return event;
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

		if (npcId == TALIEN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 50) {
					return "31739-1.htm";
				} else {
					return "31739-2.htm";
				}
			} else if (cond == 1) {
				return "31739-5.htm";
			} else if (cond == 4 && qs.hasQuestItems(LEGEND_OF_SEVENTEEN)) {
				return "31739-6.htm";
			} else if (cond == 5) {
				return "31739-9.htm";
			} else if (cond == 8 && qs.hasQuestItems(ECHO_CRYSTAL)) {
				return "31739-11.htm";
			} else if (cond == 9) {
				return "31739-13.htm";
			} else if (cond == 10 && qs.hasQuestItems(POETRY_BOOK)) {
				return "31739-14.htm";
			} else if (cond == 11) {
				return "31739-16.htm";
			}
		} else if (npcId == GABRIELLE) {
			if (cond == 1) {
				return "30753-1.htm";
			} else if (cond == 2) {
				return "30753-3.htm";
			}
		} else if (npcId == GILMORE) {
			if (cond == 2) {
				return "30754-1.htm";
			} else if (cond == 3) {
				return "30754-3.htm";
			}
		} else if (npcId == KANTABILON) {
			if (cond == 5) {
				return "31042-1.htm";
			} else if (cond == 6) {
				return "31042-4.htm";
			} else if (cond == 7 && qs.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) >= 10) {
				return "31042-3.htm";
			} else if (cond == 8) {
				return "31042-6.htm";
			}
		} else if (npcId == STEDMIEL) {
			if (cond == 9) {
				return "30692-1.htm";
			} else if (cond == 10) {
				return "30692-3.htm";
			}
		} else if (npcId == VIRGIL) {
			if (cond == 11) {
				return "31742-1.htm";
			} else if (cond == 12) {
				return "31742-3.htm";
			} else if (cond == 17) {
				return "31742-4.htm";
			} else if (cond == 18) {
				return "31742-6.htm";
			}
		} else if (npcId == OGMAR) {
			if (cond == 12) {
				return "31744-1.htm";
			} else if (cond == 13) {
				return "31744-3.htm";
			}
		} else if (npcId == RAHORAKTI) {
			if (cond == 13) {
				return "31336-1.htm";
			} else if (cond == 14) {
				return "31336-4.htm";
			} else if (cond == 15 && qs.getQuestItemsCount(CRIMSON_MOSS) >= 5) {
				return "31336-3.htm";
			} else if (cond == 16) {
				return "31336-6.htm";
			}
		} else if (npcId == KASSANDRA) {
			if (cond == 16 && qs.hasQuestItems(RAHORAKTIS_MEDICINE)) {
				return "31743-1.htm";
			} else if (cond == 17) {
				return "31743-3.htm";
			}
		} else if (npcId == CARADINE) {
			if (cond == 18) {
				return "31740-1.htm";
			} else if (cond == 19) {
				return "31740-3.htm";
			} else if (cond == 21) {
				return "31740-4.htm";
			}
		} else if (npcId == NOEL) {
			if (cond == 19) {
				return "31272-1.htm";
			} else if (cond == 20) {
				if (qs.getQuestItemsCount(LUNARGENT) >= 5 && qs.hasQuestItems(HELLFIRE_OIL)) {
					return "31272-3.htm";
				} else {
					return "31272-4.htm";
				}
			} else if (cond == 21) {
				return "31272-7.htm";
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

		if (cond == 3 && npcId == BARAHAM) {
			if (!qs.hasQuestItems(LEGEND_OF_SEVENTEEN)) {
				qs.giveItems(LEGEND_OF_SEVENTEEN, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 6 && (npcId == MALRUK_SUCCUBUS_1 || npcId == MALRUK_SUCCUBUS_2 ||
				npcId == MALRUK_SUCCUBUS_TAUREN_1 || npcId == MALRUK_SUCCUBUS_TAUREN_2)) {
			if (qs.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) < 10) {
				if (ThreadLocalRandom.current().nextInt(100) < 60) {
					qs.giveItems(MALRUK_SUCCUBUS_CLAW, 1);
					if (qs.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) >= 10) {
						qs.setCond(7);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		} else if (cond == 14 && (npcId >= SPLENDOR_1 && npcId <= SPLENDOR_5)) {
			if (qs.getQuestItemsCount(CRIMSON_MOSS) < 5) {
				if (ThreadLocalRandom.current().nextInt(100) < 50) {
					qs.giveItems(CRIMSON_MOSS, 1);
					if (qs.getQuestItemsCount(CRIMSON_MOSS) >= 5) {
						qs.setCond(15);
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
