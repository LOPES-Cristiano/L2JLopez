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
 * Chef Jeremy em Hot Springs. Entrega de bebida especial aos clientes das fontes termais.
 */
@Component
public class Quest622DeliveryOfSpecialLiquor extends Quest {

	public static final int QUEST_ID = 622;
	public static final String QUEST_NAME = "622_DeliveryOfSpecialLiquor";

	public static final int JEREMY = 31521;
	public static final int LIETTA = 31267;
	public static final int SPECIAL_LIQUOR = 7207;
	public static final int SPECIAL_DRINK = SPECIAL_LIQUOR;
	public static final int FEE_OF_DRINK = 7198;

	public static final int[] NPCS = {31521, 31267, 31543, 31544, 31545, 31546, 31547};

	@Autowired
	public Quest622DeliveryOfSpecialLiquor(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Delivery of Special Liquor");
		addStartNpc(JEREMY);
		addTalkId(NPCS);
		registerQuestItems(SPECIAL_LIQUOR, FEE_OF_DRINK);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("jeremy_q0622_0104.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(SPECIAL_LIQUOR, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("warehouse_keeper_lietta_q0622_0801.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SPECIAL_LIQUOR, -1);
			qs.takeItems(FEE_OF_DRINK, -1);
			qs.giveItems(57, 18800);
			qs.giveItems(734, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == JEREMY) {
			if (cond == 0) return "jeremy_q0622_0101.htm";
			if (cond == 6) {
				qs.setCond(7);
				return "jeremy_q0622_0701.htm";
			}
			return "jeremy_q0622_0202.htm";
		} else if (npcId == LIETTA && cond == 7) {
			return "warehouse_keeper_lietta_q0622_0701.htm";
		} else if (cond >= 1 && cond <= 5) {
			qs.takeItems(SPECIAL_LIQUOR, 1);
			qs.giveItems(FEE_OF_DRINK, 1);
			qs.setCond(cond + 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "beolin_q0622_0201.htm";
		}
		return "noquest";
	}
}
