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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 336: Coin of Magic
 */
@Component
public class Quest336CoinOfMagic extends Quest {

	public static final int QUEST_ID = 336;
	public static final String QUEST_NAME = "336_CoinOfMagic";

	// NPCs
	public static final int SORINT = 30232;
	public static final int BERNARD = 30702;
	public static final int PAGE = 30696;
	public static final int HAGGER = 30183;
	public static final int STAN = 30200;
	public static final int RALFORD = 30165;
	public static final int FERRIS = 30847;
	public static final int COLLOB = 30092;
	public static final int PANO = 30078;
	public static final int DUNING = 30688;
	public static final int LORAIN = 30673;

	// Quest Items
	public static final int COIN_DIAGRAM = 3811;
	public static final int KALDIS_COIN = 3812;
	public static final int MEMBERSHIP_1 = 3813;
	public static final int MEMBERSHIP_2 = 3814;
	public static final int MEMBERSHIP_3 = 3815;

	// Basic Coins
	public static final int BLOOD_MEDUSA = 3472;
	public static final int BLOOD_WEREWOLF = 3473;
	public static final int BLOOD_BASILISK = 3474;
	public static final int BLOOD_DREVANUL = 3475;
	public static final int BLOOD_SUCCUBUS = 3476;
	public static final int BLOOD_DRAGON = 3477;
	public static final int BELETHS_BLOOD = 3478;
	public static final int MANAKIAS_BLOOD = 3479;
	public static final int NIA_BLOOD_MEDUSA = 3480;
	public static final int GOLD_DRAGON = 3481;
	public static final int GOLD_WYVERN = 3482;
	public static final int GOLD_KNIGHT = 3483;
	public static final int GOLD_GIANT = 3484;
	public static final int GOLD_DRAKE = 3485;
	public static final int GOLD_WYRM = 3486;
	public static final int BELETHS_GOLD = 3487;
	public static final int MANAKIAS_GOLD = 3488;
	public static final int NIA_GOLD_WYVERN = 3489;
	public static final int SILVER_UNICORN = 3490;
	public static final int SILVER_FAIRY = 3491;
	public static final int SILVER_DRYAD = 3492;
	public static final int SILVER_DRAGON = 3493;
	public static final int SILVER_GOLEM = 3494;
	public static final int SILVER_UNDINE = 3495;
	public static final int BELETHS_SILVER = 3496;
	public static final int MANAKIAS_SILVER = 3497;
	public static final int NIA_SILVER_FAIRY = 3498;

	public static final int[] MOBS = {
		20146, 20161, 20235, 20240, 20245, 20276, 20279, 20280, 20284,
		20563, 20564, 20565, 20566, 20567, 20568, 20569, 20572, 20575,
		20583, 20584, 20585, 20587, 20604, 20644, 20645, 20663, 20674,
		20678, 20685, 20954, 20957, 20959, 20960, 21003, 21006, 21008,
		21274, 21275, 21276, 21278, 21279, 21280, 21282, 21283, 21284,
		21286, 21287, 21288, 21521, 21526, 21531, 21539, 21658
	};

	private static final Map<Integer, Integer> BASIC_COINS = new HashMap<>();
	static {
		BASIC_COINS.put(20583, BLOOD_MEDUSA);
		BASIC_COINS.put(20584, BLOOD_MEDUSA);
		BASIC_COINS.put(20585, BLOOD_MEDUSA);
		BASIC_COINS.put(20587, BLOOD_MEDUSA);
		BASIC_COINS.put(20604, BLOOD_MEDUSA);
		BASIC_COINS.put(20678, BLOOD_MEDUSA);
		BASIC_COINS.put(21521, BLOOD_MEDUSA);
		BASIC_COINS.put(21526, BLOOD_MEDUSA);
		BASIC_COINS.put(21531, BLOOD_MEDUSA);
		BASIC_COINS.put(21539, BLOOD_MEDUSA);

		BASIC_COINS.put(20235, BLOOD_WEREWOLF);
		BASIC_COINS.put(20663, BLOOD_WEREWOLF);
		BASIC_COINS.put(20954, BLOOD_WEREWOLF);
		BASIC_COINS.put(20960, BLOOD_WEREWOLF);
		BASIC_COINS.put(21006, BLOOD_WEREWOLF);
		BASIC_COINS.put(21274, BLOOD_WEREWOLF);
		BASIC_COINS.put(21275, BLOOD_WEREWOLF);
		BASIC_COINS.put(21276, BLOOD_WEREWOLF);

		BASIC_COINS.put(20146, BLOOD_BASILISK);
		BASIC_COINS.put(20161, BLOOD_BASILISK);
		BASIC_COINS.put(20240, BLOOD_BASILISK);
		BASIC_COINS.put(20245, BLOOD_BASILISK);
		BASIC_COINS.put(20568, BLOOD_BASILISK);
		BASIC_COINS.put(20569, BLOOD_BASILISK);
		BASIC_COINS.put(20572, BLOOD_BASILISK);
		BASIC_COINS.put(20685, BLOOD_BASILISK);

		BASIC_COINS.put(20564, GOLD_WYVERN);
		BASIC_COINS.put(20566, GOLD_WYVERN);
		BASIC_COINS.put(20674, GOLD_WYVERN);
		BASIC_COINS.put(21278, GOLD_WYVERN);
		BASIC_COINS.put(21279, GOLD_WYVERN);
		BASIC_COINS.put(21280, GOLD_WYVERN);
		BASIC_COINS.put(21658, GOLD_WYVERN);

		BASIC_COINS.put(20565, GOLD_KNIGHT);
		BASIC_COINS.put(20567, GOLD_KNIGHT);
		BASIC_COINS.put(20957, GOLD_KNIGHT);
		BASIC_COINS.put(21008, GOLD_KNIGHT);

		BASIC_COINS.put(20276, GOLD_GIANT);
		BASIC_COINS.put(20279, GOLD_GIANT);
		BASIC_COINS.put(20280, GOLD_GIANT);
		BASIC_COINS.put(20284, GOLD_GIANT);

		BASIC_COINS.put(20563, SILVER_FAIRY);
		BASIC_COINS.put(20575, SILVER_FAIRY);
		BASIC_COINS.put(21282, SILVER_FAIRY);
		BASIC_COINS.put(21283, SILVER_FAIRY);
		BASIC_COINS.put(21284, SILVER_FAIRY);

		BASIC_COINS.put(20959, SILVER_DRYAD);
		BASIC_COINS.put(21003, SILVER_DRYAD);
		BASIC_COINS.put(21286, SILVER_DRYAD);
		BASIC_COINS.put(21287, SILVER_DRYAD);
		BASIC_COINS.put(21288, SILVER_DRYAD);
	}

	private static final int[][] PROMOTE = {
		{},
		{},
		{SILVER_DRYAD, BLOOD_BASILISK, BLOOD_SUCCUBUS, SILVER_UNDINE, GOLD_GIANT, GOLD_WYRM},
		{BLOOD_WEREWOLF, GOLD_DRAKE, SILVER_FAIRY, BLOOD_DREVANUL, GOLD_KNIGHT, SILVER_GOLEM}
	};

	private static final int[][] EXCHANGE_LEVEL = {
		{PAGE, 3},
		{LORAIN, 3},
		{HAGGER, 3},
		{RALFORD, 2},
		{STAN, 2},
		{DUNING, 2},
		{FERRIS, 1},
		{COLLOB, 1},
		{PANO, 1}
	};

	@Autowired
	public Quest336CoinOfMagic(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Coin of Magic");
		addStartNpc(SORINT);
		addTalkId(SORINT, BERNARD, PAGE, HAGGER, STAN, RALFORD, FERRIS, COLLOB, PANO, DUNING, LORAIN);
		addKillId(MOBS);
		registerQuestItems(COIN_DIAGRAM, KALDIS_COIN, MEMBERSHIP_1, MEMBERSHIP_2, MEMBERSHIP_3);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30232-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(COIN_DIAGRAM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30232-05.htm".equalsIgnoreCase(event) ? "30232-05.htm" : "warehouse_keeper_sorint_q0336_05.htm";
		} else if ("talk_bernard".equalsIgnoreCase(event) || "30702-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30702-03.htm".equalsIgnoreCase(event) ? "30702-03.htm" : "hunter_bernard_q0336_02.htm";
		} else if ("30702-04.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() < 7) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "30702-04.htm";
		} else if ("30232-22.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() < 4) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "30232-22.htm";
		} else if ("30232-23.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() < 5) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "30232-23.htm";
		} else if ("30232-08.htm".equalsIgnoreCase(event)) {
			return promote(qs);
		} else if ("30232-18.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "30232-18.htm";
		} else if ("raise".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			int grade = qs.getInt("grade");
			if (grade == 1) {
				return "30232-15.htm";
			}
			int h = 0;
			int[] coins = PROMOTE[grade > 1 && grade <= 3 ? grade : 3];
			for (int coin : coins) {
				if (qs.getQuestItemsCount(coin) > 0) {
					h++;
				}
			}
			if (h == 6) {
				for (int coin : coins) {
					qs.takeItems(coin, 1);
				}
				qs.takeItems(3812 + grade, -1);
				qs.giveItems(3811 + grade, 1);
				qs.set("grade", String.valueOf(grade - 1));
				if (grade == 3) {
					qs.setCond(9);
				} else if (grade == 2) {
					qs.setCond(11);
				}
				qs.playSound(QuestState.SOUND_FANFARE_MIDDLE);
				return grade == 3 ? "30232-12.htm" : "30232-11.htm";
			}
			if (grade == 3) {
				qs.setCond(8);
			} else if (grade == 2) {
				qs.setCond(10);
			}
			return grade == 3 ? "30232-14.htm" : "30232-13.htm";
		}
		return event;
	}

	private String promote(QuestState qs) {
		int grade = qs.getInt("grade");
		if (grade == 1) {
			return "30232-15.htm";
		}
		int idx = (grade >= 2 && grade <= 3) ? grade : 3;
		int h = 0;
		for (int coin : PROMOTE[idx]) {
			if (qs.getQuestItemsCount(coin) > 0) {
				h++;
			}
		}
		if (h == 6) {
			for (int coin : PROMOTE[idx]) {
				qs.takeItems(coin, 1);
			}
			qs.takeItems(3812 + idx, -1);
			qs.giveItems(3811 + idx, 1);
			qs.set("grade", String.valueOf(idx - 1));
			if (idx == 3) {
				qs.setCond(9);
			} else if (idx == 2) {
				qs.setCond(11);
			}
			qs.playSound(QuestState.SOUND_FANFARE_MIDDLE);
			return idx == 3 ? "30232-12.htm" : "30232-11.htm";
		}
		if (idx == 3) {
			qs.setCond(8);
		} else if (idx == 2) {
			qs.setCond(10);
		}
		return idx == 3 ? "30232-14.htm" : "30232-13.htm";
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		int grade = qs.getInt("grade");

		if (npcId == SORINT) {
			if (cond == 0) {
				if (pc.level() >= 40) return "warehouse_keeper_sorint_q0336_01.htm";
				qs.exitQuest(true);
				return "warehouse_keeper_sorint_q0336_03.htm";
			}
			if (qs.getQuestItemsCount(COIN_DIAGRAM) > 0) {
				if (qs.getQuestItemsCount(KALDIS_COIN) > 0) {
					qs.takeItems(KALDIS_COIN, -1);
					qs.takeItems(COIN_DIAGRAM, -1);
					qs.giveItems(MEMBERSHIP_3, 1);
					qs.set("grade", "3");
					qs.setCond(4);
					qs.playSound(QuestState.SOUND_FANFARE_MIDDLE);
					return "30232-07.htm";
				}
				return "warehouse_keeper_sorint_q0336_06.htm";
			}
			if (grade == 3) {
				return "30232-12.htm";
			}
			if (grade == 2) {
				return "30232-11.htm";
			}
			if (grade == 1) {
				return "30232-10.htm";
			}
			return "warehouse_keeper_sorint_q0336_06.htm";
		} else if (npcId == BERNARD) {
			if (cond == 1) return "hunter_bernard_q0336_01.htm";
			if (qs.getQuestItemsCount(COIN_DIAGRAM) > 0 && grade == 0) {
				return "hunter_bernard_q0336_03.htm";
			}
			if (grade == 3) {
				return "30702-05.htm";
			}
			return "hunter_bernard_q0336_03.htm";
		} else {
			for (int[] e : EXCHANGE_LEVEL) {
				if (npcId == e[0] && grade <= e[1]) {
					return switch (npcId) {
						case PAGE -> "30696-01.htm";
						case LORAIN -> "30673-01.htm";
						case HAGGER -> "30183-01.htm";
						case RALFORD -> "30165-01.htm";
						case STAN -> "30200-01.htm";
						case DUNING -> "30688-01.htm";
						case FERRIS -> "30847-01.htm";
						case COLLOB -> "30092-01.htm";
						case PANO -> "30078-01.htm";
						default -> "noquest";
					};
				}
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getCond() < 1) {
			return null;
		}
		int npcId = npc != null ? npc.getNpcId() : 0;
		if (npcId == 20644 || npcId == 20645) {
			if (qs.getCond() == 2 && qs.getQuestItemsCount(KALDIS_COIN) == 0) {
				qs.giveItems(KALDIS_COIN, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return null;
		}
		Integer coinId = BASIC_COINS.get(npcId);
		if (coinId != null) {
			if (ThreadLocalRandom.current().nextInt(100) < 30) {
				qs.giveItems(coinId, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (ThreadLocalRandom.current().nextInt(100) < 30) {
			int coin = switch (ThreadLocalRandom.current().nextInt(3)) {
				case 0 -> BLOOD_MEDUSA;
				case 1 -> BLOOD_WEREWOLF;
				default -> BLOOD_BASILISK;
			};
			qs.giveItems(coin, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
