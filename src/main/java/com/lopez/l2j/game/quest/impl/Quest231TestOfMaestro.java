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
 * Quest 231: Test of the Maestro
 * 3º Passo de 2ª Classe para Warsmith.
 */
@Component
public class Quest231TestOfMaestro extends Quest {

	public static final int QUEST_ID = 231;
	public static final String QUEST_NAME = "231_TestOfMaestro";

	// NPCs
	public static final int LOCKIRIN = 30531;
	public static final int SPIRON = 30532;
	public static final int BALANKI = 30533;
	public static final int KEEF = 30534;
	public static final int FILAUR = 30535;
	public static final int ARIN = 30536;
	public static final int TOMA = 30556;
	public static final int CROTO = 30671;
	public static final int DUBABAH = 30672;
	public static final int LORAIN = 30673;

	// Monstros
	public static final int KING_BUGBEAR = 20150;
	public static final int GIANT_MIST_LEECH = 20225;
	public static final int STINGER_WASP = 20229;
	public static final int MARSH_SPIDER = 20233;
	public static final int EVIL_EYE_LORD = 27133;

	// Itens
	public static final int RECOMMENDATION_OF_BALANKI = 2864;
	public static final int RECOMMENDATION_OF_FILAUR = 2865;
	public static final int RECOMMENDATION_OF_ARIN = 2866;
	public static final int MARK_OF_MAESTRO = 2867;
	public static final int LETTER_OF_SOLDER_DETACHMENT = 2868;
	public static final int PAINT_OF_KAMURU = 2869;
	public static final int NECKLACE_OF_KAMURU = 2870;
	public static final int PAINT_OF_TELEPORT_DEVICE = 2871;
	public static final int TELEPORT_DEVICE = 2872;
	public static final int ARCHITECTURE_OF_KRUMA = 2873;
	public static final int REPORT_OF_KRUMA = 2874;
	public static final int INGREDIENTS_OF_ANTIDOTE = 2875;
	public static final int STINGER_WASP_NEEDLE = 2876;
	public static final int MARSH_SPIDER_WEB = 2877;
	public static final int BLOOD_OF_LEECH = 2878;
	public static final int BROKEN_TELEPORT_DEVICE = 2916;

	private static final Set<Integer> VALID_CLASSES = Set.of(56); // Artisan

	@Autowired
	public Quest231TestOfMaestro(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Maestro");

		addStartNpc(LOCKIRIN);
		addTalkId(LOCKIRIN, SPIRON, BALANKI, KEEF, FILAUR, ARIN, TOMA, CROTO, DUBABAH, LORAIN);

		addKillId(GIANT_MIST_LEECH, STINGER_WASP, MARSH_SPIDER, EVIL_EYE_LORD);

		registerQuestItems(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR, RECOMMENDATION_OF_ARIN,
				LETTER_OF_SOLDER_DETACHMENT, PAINT_OF_KAMURU, NECKLACE_OF_KAMURU,
				PAINT_OF_TELEPORT_DEVICE, TELEPORT_DEVICE, ARCHITECTURE_OF_KRUMA,
				REPORT_OF_KRUMA, INGREDIENTS_OF_ANTIDOTE, STINGER_WASP_NEEDLE,
				MARSH_SPIDER_WEB, BLOOD_OF_LEECH, BROKEN_TELEPORT_DEVICE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30531-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30531-04.htm";
		} else if ("30533-02.htm".equalsIgnoreCase(event) || "30533_1".equalsIgnoreCase(event)) {
			qs.set("bCond", "1");
			return "30533-02.htm";
		} else if ("30671-02.htm".equalsIgnoreCase(event) || "30671_1".equalsIgnoreCase(event)) {
			qs.giveItems(PAINT_OF_KAMURU, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30671-02.htm";
		} else if ("30556-05.htm".equalsIgnoreCase(event) || "30556_1".equalsIgnoreCase(event)) {
			qs.takeItems(PAINT_OF_TELEPORT_DEVICE, -1);
			qs.giveItems(BROKEN_TELEPORT_DEVICE, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			qs.addSpawn(KING_BUGBEAR, 140333, -194153, -3138, 200000);
			qs.addSpawn(KING_BUGBEAR, 140395, -194147, -3146, 200000);
			qs.addSpawn(KING_BUGBEAR, 140304, -194082, -3157, 200000);
			return "30556-05.htm";
		} else if ("30673-04.htm".equalsIgnoreCase(event) || "30673_1".equalsIgnoreCase(event)) {
			qs.set("fCond", "2");
			qs.takeItems(BLOOD_OF_LEECH, -1);
			qs.takeItems(INGREDIENTS_OF_ANTIDOTE, -1);
			qs.takeItems(MARSH_SPIDER_WEB, -1);
			qs.takeItems(STINGER_WASP_NEEDLE, -1);
			qs.giveItems(REPORT_OF_KRUMA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30673-04.htm";
		} else if ("30531-06.htm".equalsIgnoreCase(event)) {
			if (hasAllThreeRecommendations(qs)) {
				qs.takeItems(RECOMMENDATION_OF_BALANKI, -1);
				qs.takeItems(RECOMMENDATION_OF_FILAUR, -1);
				qs.takeItems(RECOMMENDATION_OF_ARIN, -1);
				qs.giveItems(MARK_OF_MAESTRO, 1);
				qs.addExpAndSp(153053, 21500);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private boolean hasAllThreeRecommendations(QuestState qs) {
		return qs.hasQuestItems(RECOMMENDATION_OF_BALANKI)
				&& qs.hasQuestItems(RECOMMENDATION_OF_FILAUR)
				&& qs.hasQuestItems(RECOMMENDATION_OF_ARIN);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == LOCKIRIN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30531-03.htm";
				} else {
					return "30531-01.htm";
				}
			} else if (cond >= 1) {
				if (hasAllThreeRecommendations(qs)) {
					return "30531-05.htm";
				}
				return "30531-04a.htm";
			}
		} else if (npcId == SPIRON) {
			return "30532-01.htm";
		} else if (npcId == KEEF) {
			return "30534-01.htm";
		} else if (npcId == BALANKI) {
			int bCond = qs.getInt("bCond");
			if (bCond == 2) {
				qs.set("bCond", "3");
				qs.takeItems(LETTER_OF_SOLDER_DETACHMENT, -1);
				qs.giveItems(RECOMMENDATION_OF_BALANKI, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30533-04.htm";
			} else if (bCond == 3 || qs.hasQuestItems(RECOMMENDATION_OF_BALANKI)) {
				return "30533-05.htm";
			} else if (bCond == 1) {
				return "30533-03.htm";
			} else if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_BALANKI)) {
				qs.giveItems(RECOMMENDATION_OF_BALANKI, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
				}
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30533-03.htm";
			}
		} else if (npcId == CROTO) {
			int bCond = qs.getInt("bCond");
			if (bCond == 1) {
				if (!qs.hasQuestItems(PAINT_OF_KAMURU)) {
					return "30671-01.htm";
				} else if (!qs.hasQuestItems(NECKLACE_OF_KAMURU)) {
					return "30671-03.htm";
				}
				qs.set("bCond", "2");
				qs.takeItems(NECKLACE_OF_KAMURU, -1);
				qs.takeItems(PAINT_OF_KAMURU, -1);
				qs.giveItems(LETTER_OF_SOLDER_DETACHMENT, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "30671-04.htm";
			} else if (bCond > 1) {
				return "30671-05.htm";
			}
		} else if (npcId == DUBABAH) {
			return "30672-01.htm";
		} else if (npcId == ARIN) {
			int aCond = qs.getInt("aCond");
			if (aCond == 2) {
				qs.set("aCond", "3");
				qs.takeItems(TELEPORT_DEVICE, -1);
				qs.giveItems(RECOMMENDATION_OF_ARIN, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30536-03.htm";
			} else if (aCond == 3 || qs.hasQuestItems(RECOMMENDATION_OF_ARIN)) {
				return "30536-04.htm";
			} else if (aCond == 1) {
				return "30536-02.htm";
			} else if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_ARIN)) {
				qs.giveItems(RECOMMENDATION_OF_ARIN, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
				}
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30536-03.htm";
			}
		} else if (npcId == TOMA) {
			int aCond = qs.getInt("aCond");
			if (aCond == 1) {
				if (!qs.hasQuestItems(BROKEN_TELEPORT_DEVICE)) {
					return "30556-01.htm";
				} else if (!qs.hasQuestItems(TELEPORT_DEVICE)) {
					qs.set("aCond", "2");
					qs.takeItems(BROKEN_TELEPORT_DEVICE, -1);
					qs.giveItems(TELEPORT_DEVICE, 5);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30556-06.htm";
				}
			} else if (aCond > 1) {
				return "30556-07.htm";
			}
		} else if (npcId == FILAUR) {
			int fCond = qs.getInt("fCond");
			if (fCond == 2) {
				qs.set("fCond", "3");
				qs.takeItems(REPORT_OF_KRUMA, -1);
				qs.giveItems(RECOMMENDATION_OF_FILAUR, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				return "30535-03.htm";
			} else if (fCond == 3 || qs.hasQuestItems(RECOMMENDATION_OF_FILAUR)) {
				return "30535-04.htm";
			} else if (fCond == 1) {
				return "30535-02.htm";
			} else if (cond == 1 && !qs.hasQuestItems(RECOMMENDATION_OF_FILAUR)) {
				qs.giveItems(RECOMMENDATION_OF_FILAUR, 1);
				if (hasAllThreeRecommendations(qs)) {
					qs.setCond(2);
				}
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30535-03.htm";
			}
		} else if (npcId == LORAIN) {
			int fCond = qs.getInt("fCond");
			if (fCond == 1 || cond == 1) {
				if (!qs.hasQuestItems(REPORT_OF_KRUMA)) {
					if (!qs.hasQuestItems(INGREDIENTS_OF_ANTIDOTE)) {
						qs.takeItems(ARCHITECTURE_OF_KRUMA, -1);
						qs.giveItems(INGREDIENTS_OF_ANTIDOTE, 1);
						qs.playSound(QuestState.SOUND_ITEMGET);
						return "30673-01.htm";
					} else if (qs.getQuestItemsCount(STINGER_WASP_NEEDLE) < 10
							|| qs.getQuestItemsCount(MARSH_SPIDER_WEB) < 10
							|| qs.getQuestItemsCount(BLOOD_OF_LEECH) < 10) {
						return "30673-02.htm";
					}
					return "30673-03.htm";
				}
			} else if (fCond > 1) {
				return "30673-05.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED || qs.getCond() != 1) {
			return null;
		}

		switch (npc.getNpcId()) {
			case GIANT_MIST_LEECH -> {
				if (qs.hasQuestItems(INGREDIENTS_OF_ANTIDOTE) && qs.getQuestItemsCount(BLOOD_OF_LEECH) < 10) {
					qs.giveItems(BLOOD_OF_LEECH, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case STINGER_WASP -> {
				if (qs.hasQuestItems(INGREDIENTS_OF_ANTIDOTE) && qs.getQuestItemsCount(STINGER_WASP_NEEDLE) < 10) {
					qs.giveItems(STINGER_WASP_NEEDLE, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case MARSH_SPIDER -> {
				if (qs.hasQuestItems(INGREDIENTS_OF_ANTIDOTE) && qs.getQuestItemsCount(MARSH_SPIDER_WEB) < 10) {
					qs.giveItems(MARSH_SPIDER_WEB, 1);
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
			case EVIL_EYE_LORD -> {
				if (qs.hasQuestItems(PAINT_OF_KAMURU) && !qs.hasQuestItems(NECKLACE_OF_KAMURU)) {
					qs.giveItems(NECKLACE_OF_KAMURU, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}
