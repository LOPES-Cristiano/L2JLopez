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
 * Quest 620: Four Goblets
 * Acesso ao Four Sepulchers, obtenção dos 4 Cálices sagrados e do Antique Brooch para Frintezza.
 */
@Component
public class Quest620FourGoblets extends Quest {

	public static final int QUEST_ID = 620;
	public static final String QUEST_NAME = "620_FourGoblets";

	// NPCs
	public static final int GHOST_OF_WIGOTH_1 = 31452;
	public static final int NAMELESS_SPIRIT = 31453;
	public static final int GHOST_OF_WIGOTH_2 = 31454;

	public static final int GHOST_CHAMBERLAIN_1 = 31919;
	public static final int GHOST_CHAMBERLAIN_2 = 31920;

	public static final int CONQUERORS_SEPULCHER_MANAGER = 31921;
	public static final int EMPERORS_SEPULCHER_MANAGER = 31922;
	public static final int GREAT_SAGES_SEPULCHER_MANAGER = 31923;
	public static final int JUDGES_SEPULCHER_MANAGER = 31924;

	// Monstros / Raid Bosses Halisha
	public static final int HALISHA_1 = 25339;
	public static final int HALISHA_2 = 25342;
	public static final int HALISHA_3 = 25346;
	public static final int HALISHA_4 = 25349;
	public static final int SEPULCHER_MOB_START = 18120;
	public static final int SEPULCHER_MOB_END = 18256;

	// Itens
	public static final int RELIC = 7254;
	public static final int SEALED_BOX = 7255;
	public static final int GOBLET_ALECTIA = 7256;
	public static final int GOBLET_TISHA = 7257;
	public static final int GOBLET_MEKARA = 7258;
	public static final int GOBLET_MORIGUL = 7259;
	public static final int GRAVE_PASS = 7261;
	public static final int ANTIQUE_BROOCH = 7262;

	// S-Grade Recipes
	public static final int[] S_RECIPES = {6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580};

	public Quest620FourGoblets() {
		this(null);
	}

	@Autowired
	public Quest620FourGoblets(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Four Goblets");

		addStartNpc(
				NAMELESS_SPIRIT,
				CONQUERORS_SEPULCHER_MANAGER,
				EMPERORS_SEPULCHER_MANAGER,
				GREAT_SAGES_SEPULCHER_MANAGER,
				JUDGES_SEPULCHER_MANAGER,
				GHOST_CHAMBERLAIN_1,
				GHOST_CHAMBERLAIN_2
		);
		addTalkId(
				NAMELESS_SPIRIT,
				GHOST_OF_WIGOTH_1,
				GHOST_OF_WIGOTH_2,
				GHOST_CHAMBERLAIN_1,
				GHOST_CHAMBERLAIN_2,
				CONQUERORS_SEPULCHER_MANAGER,
				EMPERORS_SEPULCHER_MANAGER,
				GREAT_SAGES_SEPULCHER_MANAGER,
				JUDGES_SEPULCHER_MANAGER
		);

		addKillId(HALISHA_1, HALISHA_2, HALISHA_3, HALISHA_4, SEPULCHER_MOB_START, SEPULCHER_MOB_END);
		for (int id = SEPULCHER_MOB_START; id <= SEPULCHER_MOB_END; id++) {
			addKillId(id);
		}

		registerQuestItems(GOBLET_ALECTIA, GOBLET_TISHA, GOBLET_MEKARA, GOBLET_MORIGUL, SEALED_BOX, GRAVE_PASS);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		String htmltext = event;

		if ("31452-02.htm".equalsIgnoreCase(event)) {
			return "31452-02.htm";
		} else if ("31452-05.htm".equalsIgnoreCase(event)) {
			if (ThreadLocalRandom.current().nextBoolean()) {
				htmltext = ThreadLocalRandom.current().nextBoolean() ? "31452-03.htm" : "31452-04.htm";
			}
		} else if ("31452-06.htm".equalsIgnoreCase(event)) {
			qs.teleportPlayer(169589, -90493, -2914);
			return "31452-06.htm";
		} else if ("31453-13.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			if (qs.getCond() == 2) {
				return "31453-19.htm";
			}
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31453-13.htm";
		} else if ("31453-16.htm".equalsIgnoreCase(event)) {
			if (hasAllFourGoblets(qs)) {
				qs.takeItems(GOBLET_ALECTIA, -1);
				qs.takeItems(GOBLET_TISHA, -1);
				qs.takeItems(GOBLET_MEKARA, -1);
				qs.takeItems(GOBLET_MORIGUL, -1);
				qs.giveItems(ANTIQUE_BROOCH, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_FINISH);
				return "31453-16.htm";
			}
			return "31453-14.htm";
		} else if ("31453-18.htm".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "31453-18.htm";
		} else if ("31454-11.htm".equalsIgnoreCase(event)
				|| "31454-14.htm".equalsIgnoreCase(event)
				|| "31454-16.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("OpenBox".equalsIgnoreCase(event)
				|| "boxes".equalsIgnoreCase(event)
				|| "31919-06.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SEALED_BOX)) {
				qs.takeItems(SEALED_BOX, 1);
				boolean rewarded = calculateBoxReward(qs);
				qs.playSound(QuestState.SOUND_ITEMGET);
				if ("OpenBox".equalsIgnoreCase(event)) {
					return "31454-BoxOpened.htm";
				} else if ("31919-06.htm".equalsIgnoreCase(event)) {
					return rewarded ? "31919-03.htm"
							: (ThreadLocalRandom.current().nextBoolean() ? "31919-04.htm" : "31919-05.htm");
				}
				return rewarded ? "31454-08.htm"
						: (ThreadLocalRandom.current().nextBoolean() ? "31454-09.htm" : "31454-10.htm");
			}
		} else if ("tele_4sep".equalsIgnoreCase(event) || "tele_it".equalsIgnoreCase(event)) {
			boolean allowed = false;
			if (qs.hasQuestItems(ANTIQUE_BROOCH)) {
				allowed = true;
			} else if (qs.hasQuestItems(GRAVE_PASS)) {
				qs.takeItems(GRAVE_PASS, 1);
				allowed = true;
			}
			if (allowed) {
				if ("tele_4sep".equalsIgnoreCase(event)) {
					qs.teleportPlayer(178298, -84574, -7216);
				} else {
					qs.teleportPlayer(186942, -75602, -2834);
				}
				return null;
			}
			return "31919-00.htm";
		} else if ("Enter".equalsIgnoreCase(event)) {
			int npcId = qs.resolveEventNpcId(CONQUERORS_SEPULCHER_MANAGER);
			if (npcId == EMPERORS_SEPULCHER_MANAGER) {
				qs.teleportPlayer(181217, -75577, -7216);
			} else if (npcId == GREAT_SAGES_SEPULCHER_MANAGER) {
				qs.teleportPlayer(179561, -88978, -7216);
			} else if (npcId == JUDGES_SEPULCHER_MANAGER) {
				qs.teleportPlayer(175015, -86192, -7216);
			} else {
				qs.teleportPlayer(181632, -85587, -7218);
			}
			return null;
		} else if ("6881".equalsIgnoreCase(event)
				|| "6883".equalsIgnoreCase(event)
				|| "6885".equalsIgnoreCase(event)
				|| "6887".equalsIgnoreCase(event)
				|| "6891".equalsIgnoreCase(event)
				|| "6893".equalsIgnoreCase(event)
				|| "6895".equalsIgnoreCase(event)
				|| "6897".equalsIgnoreCase(event)
				|| "6899".equalsIgnoreCase(event)
				|| "7580".equalsIgnoreCase(event)) {
			int recipeId = Integer.parseInt(event);
			if (qs.getQuestItemsCount(RELIC) >= 1000) {
				qs.takeItems(RELIC, 1000);
				qs.giveItems(recipeId, 1);
			}
			return "31454-12.htm";
		}

		return htmltext;
	}

	private boolean hasAllFourGoblets(QuestState qs) {
		return qs.hasQuestItems(GOBLET_ALECTIA) && qs.hasQuestItems(GOBLET_TISHA)
				&& qs.hasQuestItems(GOBLET_MEKARA) && qs.hasQuestItems(GOBLET_MORIGUL);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == NAMELESS_SPIRIT) {
			if (cond == 0) {
				if (player != null && player.level() >= 74) {
					return "31453-1.htm";
				} else {
					return "31453-12.htm";
				}
			} else if (cond == 1) {
				if (hasAllFourGoblets(qs)) {
					return "31453-15.htm";
				}
				return "31453-14.htm";
			} else if (cond == 2) {
				return "31453-17.htm";
			}
		} else if (npcId == GHOST_OF_WIGOTH_1) {
			if (cond == 1) {
				return "31452-01.htm";
			} else if (cond == 2) {
				return "31452-02.htm";
			}
		} else if (npcId == GHOST_OF_WIGOTH_2) {
			if (cond >= 1) {
				int index = hasAllFourGoblets(qs) ? 4 : 0;
				boolean gotSealBoxes = qs.hasQuestItems(SEALED_BOX);
				boolean gotEnoughRelics = qs.getQuestItemsCount(RELIC) >= 1000;
				if (gotSealBoxes && gotEnoughRelics) {
					index += 3;
				} else if (!gotSealBoxes && gotEnoughRelics) {
					index += 2;
				} else if (gotSealBoxes) {
					index += 1;
				}
				if (index == 0) {
					return "31454-1.htm";
				}
				return "31454-0" + index + ".htm";
			}
		} else if (npcId == GHOST_CHAMBERLAIN_1) {
			return "31919-01.htm";
		} else if (npcId == GHOST_CHAMBERLAIN_2) {
			return "31920-01.htm";
		} else if (npcId == CONQUERORS_SEPULCHER_MANAGER) {
			return "31921-01.htm";
		} else if (npcId == EMPERORS_SEPULCHER_MANAGER) {
			return "31922-01.htm";
		} else if (npcId == GREAT_SAGES_SEPULCHER_MANAGER) {
			return "31923-01.htm";
		} else if (npcId == JUDGES_SEPULCHER_MANAGER) {
			return "31924-01.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		if (npcId == HALISHA_1 && !qs.hasQuestItems(GOBLET_ALECTIA)) {
			qs.giveItems(GOBLET_ALECTIA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_2 && !qs.hasQuestItems(GOBLET_TISHA)) {
			qs.giveItems(GOBLET_TISHA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_3 && !qs.hasQuestItems(GOBLET_MEKARA)) {
			qs.giveItems(GOBLET_MEKARA, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALISHA_4 && !qs.hasQuestItems(GOBLET_MORIGUL)) {
			qs.giveItems(GOBLET_MORIGUL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId >= SEPULCHER_MOB_START && npcId <= SEPULCHER_MOB_END) {
			if (ThreadLocalRandom.current().nextInt(100) < 30) {
				qs.giveItems(SEALED_BOX, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}

	private static boolean calculateBoxReward(QuestState st) {
		boolean reward = false;
		int rnd = ThreadLocalRandom.current().nextInt(5);

		if (rnd == 0) {
			st.giveItems(57, 10000);
			reward = true;
		} else if (rnd == 1) {
			if (ThreadLocalRandom.current().nextInt(1000) < 848) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 43) st.giveItems(1884, 42);
				else if (i < 66) st.giveItems(1895, 36);
				else if (i < 184) st.giveItems(1876, 4);
				else if (i < 250) st.giveItems(1881, 6);
				else if (i < 287) st.giveItems(5549, 8);
				else if (i < 484) st.giveItems(1874, 1);
				else if (i < 681) st.giveItems(1889, 1);
				else if (i < 799) st.giveItems(1877, 1);
				else if (i < 902) st.giveItems(1894, 1);
				else st.giveItems(4043, 1);
			}
			if (ThreadLocalRandom.current().nextInt(1000) < 323) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 335) st.giveItems(1888, 1);
				else if (i < 556) st.giveItems(4040, 1);
				else if (i < 725) st.giveItems(1890, 1);
				else if (i < 872) st.giveItems(5550, 1);
				else if (i < 962) st.giveItems(1893, 1);
				else if (i < 986) st.giveItems(4046, 1);
				else st.giveItems(4048, 1);
			}
		} else if (rnd == 2) {
			if (ThreadLocalRandom.current().nextInt(1000) < 847) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 148) st.giveItems(1878, 8);
				else if (i < 175) st.giveItems(1882, 24);
				else if (i < 273) st.giveItems(1879, 4);
				else if (i < 322) st.giveItems(1880, 6);
				else if (i < 357) st.giveItems(1885, 6);
				else if (i < 554) st.giveItems(1875, 1);
				else if (i < 685) st.giveItems(1883, 1);
				else if (i < 803) st.giveItems(5220, 1);
				else if (i < 901) st.giveItems(4039, 1);
				else st.giveItems(4044, 1);
			}
			if (ThreadLocalRandom.current().nextInt(1000) < 251) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 350) st.giveItems(1887, 1);
				else if (i < 587) st.giveItems(4042, 1);
				else if (i < 798) st.giveItems(1886, 1);
				else if (i < 922) st.giveItems(4041, 1);
				else if (i < 966) st.giveItems(1892, 1);
				else if (i < 996) st.giveItems(1891, 1);
				else st.giveItems(4047, 1);
			}
		} else if (rnd == 3) {
			if (ThreadLocalRandom.current().nextInt(1000) < 31) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 223) st.giveItems(730, 1);
				else if (i < 893) st.giveItems(948, 1);
				else st.giveItems(960, 1);
			}
			if (ThreadLocalRandom.current().nextInt(1000) < 5) {
				reward = true;
				int i = ThreadLocalRandom.current().nextInt(1000);
				if (i < 202) st.giveItems(729, 1);
				else if (i < 928) st.giveItems(947, 1);
				else st.giveItems(959, 1);
			}
		} else if (rnd == 4) {
			int rcp = S_RECIPES[ThreadLocalRandom.current().nextInt(S_RECIPES.length)];
			st.giveItems(rcp, 1);
			reward = true;
		}

		return reward;
	}
}
