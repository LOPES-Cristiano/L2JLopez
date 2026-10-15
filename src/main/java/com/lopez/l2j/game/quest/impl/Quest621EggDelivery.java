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
 * Quest 621: Egg Delivery
 * Chef Jeremy em Hot Springs. Entrega de ovos fervidos aos NPCs das fontes termais (Pulin, Naff, Crocus, Kuber, Beolin)
 * e retorno para Jeremy / Brewer Valentine.
 */
@Component
public class Quest621EggDelivery extends Quest {

	public static final int QUEST_ID = 621;
	public static final String QUEST_NAME = "621_EggDelivery";

	// NPCs
	public static final int JEREMY = 31521;
	public static final int PULIN = 31543;
	public static final int NAFF = 31544;
	public static final int CROCUS = 31545;
	public static final int KUBER = 31546;
	public static final int BEOLIN = 31547;
	public static final int VALENTINE = 31584;

	// Itens
	public static final int EGG_BASKET = 7206;
	public static final int BOILED_EGGS = 7206;
	public static final int RECEIPT = 7196;
	public static final int FEE_OF_BOILED_EGG = 7196;

	// Recompensas
	public static final int HASTE_POTION = 734;
	public static final int[] RECIPES = {6847, 6849, 6851};

	public static final int[] NPCS = {
			JEREMY, VALENTINE, PULIN, NAFF, CROCUS, KUBER, BEOLIN
	};

	public Quest621EggDelivery() {
		this(null);
	}

	@Autowired
	public Quest621EggDelivery(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Egg Delivery");
		addStartNpc(JEREMY);
		addTalkId(JEREMY, PULIN, NAFF, CROCUS, KUBER, BEOLIN, VALENTINE);
		registerQuestItems(EGG_BASKET, RECEIPT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private void deliverEgg(QuestState qs, int nextCond) {
		qs.setCond(nextCond);
		qs.takeItems(EGG_BASKET, 1);
		qs.giveItems(RECEIPT, 1);
		qs.playSound(QuestState.SOUND_ITEMGET);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		String htmltext = event;

		if ("31521-02.htm".equalsIgnoreCase(event)
				|| "jeremy_q0621_0104.htm".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)) {
			qs.takeItems(EGG_BASKET, -1);
			qs.takeItems(RECEIPT, -1);
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(EGG_BASKET, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31521-02.htm".equalsIgnoreCase(event) ? "31521-02.htm" : "jeremy_q0621_0104.htm";
		} else if ("31543-02.htm".equalsIgnoreCase(event) || "pulin_q0621_0201.htm".equalsIgnoreCase(event)) {
			deliverEgg(qs, 2);
		} else if ("31544-02.htm".equalsIgnoreCase(event) || "naff_q0621_0301.htm".equalsIgnoreCase(event)) {
			deliverEgg(qs, 3);
		} else if ("31545-02.htm".equalsIgnoreCase(event) || "crocus_q0621_0401.htm".equalsIgnoreCase(event)) {
			deliverEgg(qs, 4);
		} else if ("31546-02.htm".equalsIgnoreCase(event) || "kuber_q0621_0501.htm".equalsIgnoreCase(event)) {
			deliverEgg(qs, 5);
		} else if ("31547-02.htm".equalsIgnoreCase(event) || "beolin_q0621_0601.htm".equalsIgnoreCase(event)) {
			deliverEgg(qs, 6);
		} else if ("31521-06.htm".equalsIgnoreCase(event) || "jeremy_q0621_0701.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(RECEIPT) < 5) {
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "31521-08.htm";
			}
			qs.setCond(7);
			qs.takeItems(RECEIPT, 5);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31584-02.htm".equalsIgnoreCase(event)
				|| "brewer_valentine_q0621_0801.htm".equalsIgnoreCase(event)) {
			qs.takeItems(EGG_BASKET, -1);
			qs.takeItems(RECEIPT, -1);
			if (ThreadLocalRandom.current().nextInt(5) < 1) {
				qs.giveItems(RECIPES[ThreadLocalRandom.current().nextInt(RECIPES.length)], 1);
			} else {
				qs.giveItems(57, 18800);
				qs.giveItems(HASTE_POTION, 1);
			}
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.playerChar();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (qs.getState() == State.CREATED || cond == 0) {
			if (npcId == JEREMY) {
				if (player != null && player.level() < 68) {
					qs.exitQuest(true);
					return "jeremy_q0621_0103.htm";
				}
				return "jeremy_q0621_0101.htm";
			}
			return "noquest";
		}

		switch (npcId) {
			case JEREMY -> {
				if (cond == 1) {
					return "jeremy_q0621_0104.htm";
				} else if (cond == 6) {
					qs.setCond(7);
					return "jeremy_q0621_0701.htm";
				} else if (cond == 7) {
					return "jeremy_q0621_0703.htm";
				}
				return "jeremy_q0621_0202.htm";
			}
			case PULIN -> {
				if (cond == 1 && qs.getQuestItemsCount(EGG_BASKET) >= 1) {
					deliverEgg(qs, 2);
					return "pulin_q0621_0201.htm";
				} else if (cond > 1) {
					return "31543-03.htm";
				}
			}
			case NAFF -> {
				if (cond == 2 && qs.getQuestItemsCount(EGG_BASKET) >= 1) {
					deliverEgg(qs, 3);
					return "naff_q0621_0201.htm";
				} else if (cond > 2) {
					return "31544-03.htm";
				}
			}
			case CROCUS -> {
				if (cond == 3 && qs.getQuestItemsCount(EGG_BASKET) >= 1) {
					deliverEgg(qs, 4);
					return "crocus_q0621_0301.htm";
				} else if (cond > 3) {
					return "31545-03.htm";
				}
			}
			case KUBER -> {
				if (cond == 4 && qs.getQuestItemsCount(EGG_BASKET) >= 1) {
					deliverEgg(qs, 5);
					return "kuber_q0621_0401.htm";
				} else if (cond > 4) {
					return "31546-03.htm";
				}
			}
			case BEOLIN -> {
				if (cond == 5 && qs.getQuestItemsCount(EGG_BASKET) >= 1) {
					deliverEgg(qs, 6);
					return "beolin_q0621_0501.htm";
				} else if (cond > 5) {
					return "31547-03.htm";
				}
			}
			case VALENTINE -> {
				if (cond == 7) {
					return "brewer_valentine_q0621_0701.htm";
				}
			}
		}

		return "noquest";
	}
}
