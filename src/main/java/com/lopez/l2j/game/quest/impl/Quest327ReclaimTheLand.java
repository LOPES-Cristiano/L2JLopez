package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 327 - Reclaim The Land
 */
@Component
public class Quest327ReclaimTheLand extends Quest {

	public static final int PIOTUR = 30597;
	public static final int IRIS = 30034;
	public static final int ACELLOPY = 30313;
	public static final int NESTLE = 30314;
	public static final int LEIKAN = 30382;

	public static final int LEIKAN_LETTER = 5012;
	public static final int TUREK_DOG_TAG = 1846;
	public static final int TUREK_MEDALLION = 1847;
	public static final int CLAY_URN_FRAGMENT = 1848;
	public static final int BRASS_TIARA_PIECE = 1849;
	public static final int BRONZE_MIRROR_PIECE = 1850;
	public static final int JADE_NECKLACE_BEAD = 1851;
	public static final int ANCIENT_CLAY_URN = 1852;
	public static final int ANCIENT_BRASS_TIARA = 1853;
	public static final int ANCIENT_BRONZE_MIRROR = 1854;
	public static final int ANCIENT_JADE_NECKLACE = 1855;
	public static final int CHANCE_ASSEMBLE = 80;

	public static final int SOULSHOT_D = 1463;
	public static final int SPIRITSHOT_D = 2510;
	public static final int HASTE_POTION = 734;
	public static final int HEALING_POTION = 1061;
	public static final int ADENA = 57;

	private static final int[] MONSTERS = {
		20495, 20496, 20497, 20498, 20499, 20500, 20501
	};

	private static final Map<String, int[]> EXP_REWARD = new HashMap<>();
	static {
		EXP_REWARD.put("30034-03.htm", new int[]{CLAY_URN_FRAGMENT, 307});
		EXP_REWARD.put("30034-04.htm", new int[]{BRASS_TIARA_PIECE, 368});
		EXP_REWARD.put("30034-05.htm", new int[]{BRONZE_MIRROR_PIECE, 368});
		EXP_REWARD.put("30034-06.htm", new int[]{JADE_NECKLACE_BEAD, 430});
	}

	public Quest327ReclaimTheLand(QuestManager questManager) {
		super(327, "327_ReclaimTheLand", "327_ReclaimTheLand");
		addStartNpc(LEIKAN, PIOTUR);
		addTalkId(LEIKAN, PIOTUR, IRIS, ACELLOPY, NESTLE);
		addKillId(MONSTERS);
		registerQuestItems(TUREK_DOG_TAG, TUREK_MEDALLION, LEIKAN_LETTER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "piotur_q0327_03.htm";
		}
		int n = qs.getStateId();
		if (event.equalsIgnoreCase("piotur_q0327_03.htm") && n == 1) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if (event.equalsIgnoreCase("30597-03.htm") && qs.getInt("cond") < 1) {
			qs.setState(State.STARTED);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if (event.equalsIgnoreCase("30382-03.htm")) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			qs.giveItems(LEIKAN_LETTER, 1L);
			return event;
		} else if (event.equalsIgnoreCase("piotur_q0327_06.htm") || event.equalsIgnoreCase("30597-06.htm")) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return event;
		} else if (n == 2) {
			if (event.equalsIgnoreCase("trader_acellopy_q0327_02.htm") && qs.getQuestItemsCount(CLAY_URN_FRAGMENT) >= 5L) {
				qs.takeItems(CLAY_URN_FRAGMENT, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_10.htm";
				}
				qs.giveItems(ANCIENT_CLAY_URN, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_03.htm";
			}
			if (event.equalsIgnoreCase("30313-02.htm")) {
				if (qs.getQuestItemsCount(CLAY_URN_FRAGMENT) >= 5L) {
					qs.takeItems(CLAY_URN_FRAGMENT, 5L);
					if (ThreadLocalRandom.current().nextInt(6) < 5) {
						qs.giveItems(ANCIENT_CLAY_URN, 1L);
						return "30313-03.htm";
					}
					return "30313-10.htm";
				}
				return event;
			}
			if (event.equalsIgnoreCase("trader_acellopy_q0327_04.htm") && qs.getQuestItemsCount(BRASS_TIARA_PIECE) >= 5L) {
				qs.takeItems(BRASS_TIARA_PIECE, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_10.htm";
				}
				qs.giveItems(ANCIENT_BRASS_TIARA, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_05.htm";
			}
			if (event.equalsIgnoreCase("30313-04.htm")) {
				if (qs.getQuestItemsCount(BRASS_TIARA_PIECE) >= 5L) {
					qs.takeItems(BRASS_TIARA_PIECE, 5L);
					if (ThreadLocalRandom.current().nextInt(7) < 6) {
						qs.giveItems(ANCIENT_BRASS_TIARA, 1L);
						return "30313-05.htm";
					}
					return "30313-10.htm";
				}
				return event;
			}
			if (event.equalsIgnoreCase("trader_acellopy_q0327_06.htm") && qs.getQuestItemsCount(BRONZE_MIRROR_PIECE) >= 5L) {
				qs.takeItems(BRONZE_MIRROR_PIECE, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_10.htm";
				}
				qs.giveItems(ANCIENT_BRONZE_MIRROR, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_07.htm";
			}
			if (event.equalsIgnoreCase("30313-06.htm")) {
				if (qs.getQuestItemsCount(BRONZE_MIRROR_PIECE) >= 5L) {
					qs.takeItems(BRONZE_MIRROR_PIECE, 5L);
					if (ThreadLocalRandom.current().nextInt(7) < 6) {
						qs.giveItems(ANCIENT_BRONZE_MIRROR, 1L);
						return "30313-07.htm";
					}
					return "30313-10.htm";
				}
				return event;
			}
			if (event.equalsIgnoreCase("trader_acellopy_q0327_08.htm") && qs.getQuestItemsCount(JADE_NECKLACE_BEAD) >= 5L) {
				qs.takeItems(JADE_NECKLACE_BEAD, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_09.htm";
				}
				qs.giveItems(ANCIENT_JADE_NECKLACE, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_07.htm";
			}
			if (event.equalsIgnoreCase("30313-08.htm")) {
				if (qs.getQuestItemsCount(JADE_NECKLACE_BEAD) >= 5L) {
					qs.takeItems(JADE_NECKLACE_BEAD, 5L);
					if (ThreadLocalRandom.current().nextInt(8) < 7) {
						qs.giveItems(ANCIENT_JADE_NECKLACE, 1L);
						return "30313-09.htm";
					}
					return "30313-10.htm";
				}
				return event;
			}
			if (event.equalsIgnoreCase("iris_q0327_03.htm")) {
				return exchangeItem(qs, CLAY_URN_FRAGMENT, ADENA, 40) ? "iris_q0327_03.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_04.htm")) {
				return exchangeItem(qs, BRASS_TIARA_PIECE, ADENA, 50) ? "iris_q0327_04.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_05.htm")) {
				return exchangeItem(qs, BRONZE_MIRROR_PIECE, ADENA, 50) ? "iris_q0327_05.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_06.htm")) {
				return exchangeItem(qs, JADE_NECKLACE_BEAD, ADENA, 60) ? "iris_q0327_06.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_07.htm")) {
				boolean rewarded = exchangeItem(qs, ANCIENT_CLAY_URN, ADENA, 1000) ||
								   exchangeItem(qs, ANCIENT_BRASS_TIARA, ADENA, 1200) ||
								   exchangeItem(qs, ANCIENT_BRONZE_MIRROR, ADENA, 1400) ||
								   exchangeItem(qs, ANCIENT_JADE_NECKLACE, ADENA, 1600);
				return rewarded ? "iris_q0327_07.htm" : "iris_q0327_02.htm";
			}
			if (EXP_REWARD.containsKey(event)) {
				int[] reward = EXP_REWARD.get(event);
				if (qs.getQuestItemsCount(reward[0]) > 0L) {
					qs.takeItems(reward[0], 1L);
					qs.addExpAndSp(reward[1], 0);
					return event;
				}
				return "30034-02.htm";
			}
			if (event.equalsIgnoreCase("30034-07.htm")) {
				boolean rewarded = false;
				for (int itm = ANCIENT_CLAY_URN; itm <= ANCIENT_JADE_NECKLACE; itm++) {
					if (qs.getQuestItemsCount(itm) > 0L) {
						qs.takeItems(itm, 1L);
						qs.addExpAndSp((itm == ANCIENT_JADE_NECKLACE) ? 430 : (itm - ANCIENT_CLAY_URN) * 307 + 1840, 0);
						rewarded = true;
					}
				}
				return rewarded ? event : "30034-02.htm";
			}
			if (event.equalsIgnoreCase("30314-03.htm")) {
				if (!qs.hasQuestItems(ANCIENT_CLAY_URN)) {
					return "30314-07.htm";
				}
				qs.takeItems(ANCIENT_CLAY_URN, 1L);
				qs.giveItems(SOULSHOT_D, 70 + ThreadLocalRandom.current().nextInt(41));
				return event;
			}
			if (event.equalsIgnoreCase("30314-04.htm")) {
				if (!qs.hasQuestItems(ANCIENT_BRASS_TIARA)) {
					return "30314-07.htm";
				}
				qs.takeItems(ANCIENT_BRASS_TIARA, 1L);
				int rnd = ThreadLocalRandom.current().nextInt(100);
				if (rnd < 40) {
					qs.giveItems(HEALING_POTION, 1L);
				} else if (rnd < 84) {
					qs.giveItems(HASTE_POTION, 1L);
				} else {
					qs.giveItems(SPIRITSHOT_D, 1 + ThreadLocalRandom.current().nextInt(4));
				}
				return event;
			}
			if (event.equalsIgnoreCase("30314-05.htm")) {
				if (!qs.hasQuestItems(ANCIENT_BRONZE_MIRROR)) {
					return "30314-07.htm";
				}
				qs.takeItems(ANCIENT_BRONZE_MIRROR, 1L);
				qs.giveItems(ThreadLocalRandom.current().nextInt(100) < 59 ? 736 : 737, 1L);
				return event;
			}
			if (event.equalsIgnoreCase("30314-06.htm")) {
				if (!qs.hasQuestItems(ANCIENT_JADE_NECKLACE)) {
					return "30314-07.htm";
				}
				qs.takeItems(ANCIENT_JADE_NECKLACE, 1L);
				qs.giveItems(SPIRITSHOT_D, 50 + ThreadLocalRandom.current().nextInt(41));
				return event;
			}
		}
		return event;
	}

	private boolean exchangeItem(QuestState qs, int itemId, int rewardId, int count) {
		if (qs.getQuestItemsCount(itemId) > 0) {
			qs.takeItems(itemId, 1);
			qs.giveItems(rewardId, count);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return true;
		}
		return false;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int n = qs.getStateId();
		int n2 = npc != null ? npc.getNpcId() : 0;
		if (n == 1) {
			if (n2 == LEIKAN) {
				if (pc.getLevel() < 25) {
					qs.exitQuest(true);
					return "30382-01.htm";
				}
				return "30382-02.htm";
			}
			if (n2 != PIOTUR) {
				return "noquest";
			}
			if (pc.getLevel() < 25) {
				qs.exitQuest(true);
				return "piotur_q0327_01.htm";
			}
			qs.setCond(0);
			return "piotur_q0327_02.htm";
		}
		if (n != 2) {
			return "noquest";
		}
		int cond = qs.getCond();
		if (n2 == LEIKAN) {
			if (cond == 1) {
				return "30382-04.htm";
			}
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30382-05.htm";
		}
		if (n2 == PIOTUR) {
			if (qs.hasQuestItems(LEIKAN_LETTER)) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				qs.takeItems(LEIKAN_LETTER, 1L);
				return "30597-03a.htm";
			}
			long l = qs.getQuestItemsCount(TUREK_DOG_TAG) * 40L + qs.getQuestItemsCount(TUREK_MEDALLION) * 50L;
			if (l == 0L) {
				return "piotur_q0327_04.htm";
			}
			qs.takeItems(TUREK_DOG_TAG, -1L);
			qs.takeItems(TUREK_MEDALLION, -1L);
			qs.giveItems(ADENA, l);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "piotur_q0327_05.htm";
		}
		if (n2 == IRIS) {
			return "iris_q0327_01.htm";
		}
		if (n2 == ACELLOPY) {
			return "trader_acellopy_q0327_01.htm";
		}
		if (n2 == NESTLE) {
			if (!qs.hasQuestItems(ANCIENT_CLAY_URN) && !qs.hasQuestItems(ANCIENT_BRASS_TIARA) && !qs.hasQuestItems(ANCIENT_BRONZE_MIRROR) && !qs.hasQuestItems(ANCIENT_JADE_NECKLACE)) {
				return "30314-01.htm";
			}
			return "30314-02.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null || qs.getState() != State.STARTED) {
			return null;
		}
		int npcId = npc != null ? npc.getNpcId() : 0;
		if (npcId == 20501 || npcId == 20500 || npcId == 20495) {
			qs.giveItems(TUREK_MEDALLION, 1);
		} else {
			qs.giveItems(TUREK_DOG_TAG, 1);
		}
		int roll = ThreadLocalRandom.current().nextInt(100);
		if (roll < 20) {
			int piece = ThreadLocalRandom.current().nextInt(4);
			if (piece == 0) {
				qs.giveItems(CLAY_URN_FRAGMENT, 1);
			} else if (piece == 1) {
				qs.giveItems(BRASS_TIARA_PIECE, 1);
			} else if (piece == 2) {
				qs.giveItems(BRONZE_MIRROR_PIECE, 1);
			} else {
				qs.giveItems(JADE_NECKLACE_BEAD, 1);
			}
		}
		qs.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}
