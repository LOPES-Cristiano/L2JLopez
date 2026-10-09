package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
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

	private static final int[] MONSTERS = {
		20495, 20496, 20497, 20498, 20499, 20500, 20501
	};

	public Quest327ReclaimTheLand(QuestManager questManager) {
		super(327, "327_ReclaimTheLand", "327_ReclaimTheLand");
		addStartNpc(PIOTUR);
		addTalkNpc(PIOTUR);
		addTalkNpc(IRIS);
		addTalkNpc(ACELLOPY);
		for (int npcId : MONSTERS) {
			addKillId(npcId);
		}
		registerQuestItems(TUREK_DOG_TAG, TUREK_MEDALLION);
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
		} else if (event.equalsIgnoreCase("piotur_q0327_06.htm") && n == 2) {
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
			if (event.equalsIgnoreCase("trader_acellopy_q0327_04.htm") && qs.getQuestItemsCount(BRASS_TIARA_PIECE) >= 5L) {
				qs.takeItems(BRASS_TIARA_PIECE, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_10.htm";
				}
				qs.giveItems(ANCIENT_BRASS_TIARA, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_05.htm";
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
			if (event.equalsIgnoreCase("trader_acellopy_q0327_08.htm") && qs.getQuestItemsCount(JADE_NECKLACE_BEAD) >= 5L) {
				qs.takeItems(JADE_NECKLACE_BEAD, 5L);
				if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_ASSEMBLE) {
					return "trader_acellopy_q0327_09.htm";
				}
				qs.giveItems(ANCIENT_JADE_NECKLACE, 1L);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_acellopy_q0327_07.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_03.htm")) {
				return exchangeItem(qs, CLAY_URN_FRAGMENT, 57, 40) ? "iris_q0327_03.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_04.htm")) {
				return exchangeItem(qs, BRASS_TIARA_PIECE, 57, 50) ? "iris_q0327_04.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_05.htm")) {
				return exchangeItem(qs, BRONZE_MIRROR_PIECE, 57, 50) ? "iris_q0327_05.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_06.htm")) {
				return exchangeItem(qs, JADE_NECKLACE_BEAD, 57, 60) ? "iris_q0327_06.htm" : "iris_q0327_02.htm";
			}
			if (event.equalsIgnoreCase("iris_q0327_07.htm")) {
				boolean rewarded = exchangeItem(qs, ANCIENT_CLAY_URN, 57, 1000) ||
								   exchangeItem(qs, ANCIENT_BRASS_TIARA, 57, 1200) ||
								   exchangeItem(qs, ANCIENT_BRONZE_MIRROR, 57, 1400) ||
								   exchangeItem(qs, ANCIENT_JADE_NECKLACE, 57, 1600);
				return rewarded ? "iris_q0327_07.htm" : "iris_q0327_02.htm";
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
		if (n2 == PIOTUR) {
			long l = qs.getQuestItemsCount(TUREK_DOG_TAG) * 40L + qs.getQuestItemsCount(TUREK_MEDALLION) * 50L;
			if (l == 0L) {
				return "piotur_q0327_04.htm";
			}
			qs.takeItems(TUREK_DOG_TAG, -1L);
			qs.takeItems(TUREK_MEDALLION, -1L);
			qs.giveItems(57, l);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "piotur_q0327_05.htm";
		}
		if (n2 == IRIS) {
			return "iris_q0327_01.htm";
		}
		if (n2 == ACELLOPY) {
			return "trader_acellopy_q0327_01.htm";
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
