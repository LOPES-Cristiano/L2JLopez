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
 * Chef Jeremy em Hot Springs. Entrega de ovos de dinossauro aos NPCs das fontes termais.
 */
@Component
public class Quest621EggDelivery extends Quest {

	public static final int QUEST_ID = 621;
	public static final String QUEST_NAME = "621_EggDelivery";

	public static final int JEREMY = 31521;
	public static final int VALENTINE = 31584;
	public static final int EGG_BASKET = 7206;
	public static final int BOILED_EGGS = EGG_BASKET;
	public static final int RECEIPT = 7196;

	public static final int[] NPCS = {31521, 31584, 31543, 31544, 31545, 31546, 31547};

	@Autowired
	public Quest621EggDelivery(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Egg Delivery");
		addStartNpc(JEREMY);
		addTalkId(NPCS);
		registerQuestItems(EGG_BASKET, RECEIPT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("jeremy_q0621_0104.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(EGG_BASKET, 5);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("brewer_valentine_q0621_0801.htm".equalsIgnoreCase(event)) {
			qs.takeItems(EGG_BASKET, -1);
			qs.takeItems(RECEIPT, -1);
			qs.giveItems(57, 18800);
			qs.giveItems(734, 1); // Haste Potion
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
			if (cond == 0) return "jeremy_q0621_0101.htm";
			if (cond == 6) {
				qs.setCond(7);
				return "jeremy_q0621_0701.htm";
			}
			return "jeremy_q0621_0202.htm";
		} else if (npcId == VALENTINE && cond == 7) {
			return "brewer_valentine_q0621_0701.htm";
		} else if (cond >= 1 && cond <= 5) {
			qs.takeItems(EGG_BASKET, 1);
			qs.giveItems(RECEIPT, 1);
			qs.setCond(cond + 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "pulin_q0621_0201.htm";
		}
		return "noquest";
	}
}
