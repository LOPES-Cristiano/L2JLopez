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
 * Quest 622: Delivery of Special Liquor
 * Chef Jeremy em Hot Springs. Entrega de bebida especial aos clientes das fontes termais (Beolin, Kuber, Crocus, Naff, Pulin)
 * e conclusão com Jeremy e Warehouse Keeper Lietta / Brewer Valentine.
 */
@Component
public class Quest622DeliveryOfSpecialLiquor extends Quest {

	public static final int QUEST_ID = 622;
	public static final String QUEST_NAME = "622_DeliveryOfSpecialLiquor";

	// NPCs
	public static final int JEREMY = 31521;
	public static final int LIETTA = 31267;
	public static final int PULIN = 31543;
	public static final int NAFF = 31544;
	public static final int CROCUS = 31545;
	public static final int KUBER = 31546;
	public static final int BEOLIN = 31547;
	public static final int VALENTINE = 31584;

	// Itens
	public static final int SPECIAL_LIQUOR = 7207;
	public static final int SPECIAL_DRINK = SPECIAL_LIQUOR;
	public static final int FEE_OF_DRINK = 7198;

	// Recompensas
	public static final int HASTE_POTION = 734;
	public static final int[] RECIPES = {6847, 6849, 6851};

	public static final int[] NPCS = {
			JEREMY, LIETTA, VALENTINE, PULIN, NAFF, CROCUS, KUBER, BEOLIN
	};

	public Quest622DeliveryOfSpecialLiquor() {
		this(null);
	}

	@Autowired
	public Quest622DeliveryOfSpecialLiquor(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Delivery of Special Liquor");
		addStartNpc(JEREMY);
		addTalkId(JEREMY, LIETTA, VALENTINE, PULIN, NAFF, CROCUS, KUBER, BEOLIN);
		registerQuestItems(SPECIAL_LIQUOR, FEE_OF_DRINK);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private void deliverLiquor(QuestState qs, int nextCond) {
		qs.setCond(nextCond);
		qs.takeItems(SPECIAL_LIQUOR, 1);
		qs.giveItems(FEE_OF_DRINK, 1);
		qs.playSound(QuestState.SOUND_MIDDLE);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		String htmltext = event;

		if ("31521-02.htm".equalsIgnoreCase(event)
				|| "jeremy_q0622_0104.htm".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)) {
			qs.takeItems(SPECIAL_LIQUOR, -1);
			qs.takeItems(FEE_OF_DRINK, -1);
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(SPECIAL_LIQUOR, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31521-02.htm".equalsIgnoreCase(event) ? "31521-02.htm" : "jeremy_q0622_0104.htm";
		} else if ("31547-02.htm".equalsIgnoreCase(event) || "beolin_q0622_0201.htm".equalsIgnoreCase(event)) {
			deliverLiquor(qs, 2);
		} else if ("31546-02.htm".equalsIgnoreCase(event) || "kuber_q0622_0301.htm".equalsIgnoreCase(event)) {
			deliverLiquor(qs, 3);
		} else if ("31545-02.htm".equalsIgnoreCase(event) || "crocus_q0622_0401.htm".equalsIgnoreCase(event)) {
			deliverLiquor(qs, 4);
		} else if ("31544-02.htm".equalsIgnoreCase(event) || "naff_q0622_0501.htm".equalsIgnoreCase(event)) {
			deliverLiquor(qs, 5);
		} else if ("31543-02.htm".equalsIgnoreCase(event) || "pulin_q0622_0601.htm".equalsIgnoreCase(event)) {
			deliverLiquor(qs, 6);
		} else if ("31521-06.htm".equalsIgnoreCase(event) || "jeremy_q0622_0701.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.takeItems(FEE_OF_DRINK, 5);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31267-02.htm".equalsIgnoreCase(event)
				|| "31584-02.htm".equalsIgnoreCase(event)
				|| "warehouse_keeper_lietta_q0622_0801.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SPECIAL_LIQUOR, -1);
			qs.takeItems(FEE_OF_DRINK, -1);
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
					return "jeremy_q0622_0103.htm";
				}
				return "jeremy_q0622_0101.htm";
			}
			return "noquest";
		}

		switch (npcId) {
			case JEREMY -> {
				if (cond < 6) {
					return "jeremy_q0622_0202.htm";
				} else if (cond == 6) {
					qs.setCond(7);
					return "jeremy_q0622_0701.htm";
				} else if (cond == 7) {
					return "jeremy_q0622_0703.htm";
				}
			}
			case BEOLIN -> {
				if (cond == 1 && qs.getQuestItemsCount(SPECIAL_LIQUOR) >= 1) {
					deliverLiquor(qs, 2);
					return "beolin_q0622_0201.htm";
				} else if (cond > 1) {
					return "31547-03.htm";
				}
			}
			case KUBER -> {
				if (cond == 2 && qs.getQuestItemsCount(SPECIAL_LIQUOR) >= 1) {
					deliverLiquor(qs, 3);
					return "kuber_q0622_0201.htm";
				} else if (cond > 2) {
					return "31546-03.htm";
				}
			}
			case CROCUS -> {
				if (cond == 3 && qs.getQuestItemsCount(SPECIAL_LIQUOR) >= 1) {
					deliverLiquor(qs, 4);
					return "crocus_q0622_0301.htm";
				} else if (cond > 3) {
					return "31545-03.htm";
				}
			}
			case NAFF -> {
				if (cond == 4 && qs.getQuestItemsCount(SPECIAL_LIQUOR) >= 1) {
					deliverLiquor(qs, 5);
					return "naff_q0622_0401.htm";
				} else if (cond > 4) {
					return "31544-03.htm";
				}
			}
			case PULIN -> {
				if (cond == 5 && qs.getQuestItemsCount(SPECIAL_LIQUOR) >= 1) {
					deliverLiquor(qs, 6);
					return "pulin_q0622_0501.htm";
				} else if (cond > 5) {
					return "31543-03.htm";
				}
			}
			case LIETTA, VALENTINE -> {
				if (cond == 7) {
					return "warehouse_keeper_lietta_q0622_0701.htm";
				}
			}
		}

		return "noquest";
	}
}
