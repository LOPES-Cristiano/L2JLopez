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
 * Quest 354: Conquest of Alligator Island
 * Warehouse Keeper Kluck em Heine. Cacada em Alligator Island para obter Alligator Tooth e Mystery Map.
 */
@Component
public class Quest354ConquestOfAlligatorIsland extends Quest {

	public static final int QUEST_ID = 354;
	public static final String QUEST_NAME = "354_ConquestOfAlligatorIsland";

	public static final int KLUCK = 30895;
	public static final int ALLIGATOR_TOOTH = 5863;
	public static final int TORN_MAP_FRAGMENT = 5864;
	public static final int PIRATES_TREASURE_MAP = 5915;

	public static final int[] MOBS = {20804, 20805, 20806, 20807, 20808, 20991};

	@Autowired
	public Quest354ConquestOfAlligatorIsland(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Conquest of Alligator Island");
		addStartNpc(KLUCK);
		addTalkId(KLUCK);
		addKillId(MOBS);
		registerQuestItems(ALLIGATOR_TOOTH, TORN_MAP_FRAGMENT, PIRATES_TREASURE_MAP);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "warehouse_keeper_kluck_q0354_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event) || "exchange_teeth".equalsIgnoreCase(event)) {
			long count = qs.getQuestItemsCount(ALLIGATOR_TOOTH);
			if (count >= 100) {
				qs.giveItems(57, (int) (count * 220 + 10700));
				qs.takeItems(ALLIGATOR_TOOTH, -1);
				return "warehouse_keeper_kluck_q0354_06b.htm";
			} else if (count > 0) {
				qs.giveItems(57, (int) (count * 220 + 3100));
				qs.takeItems(ALLIGATOR_TOOTH, -1);
				return "warehouse_keeper_kluck_q0354_06a.htm";
			}
			return "warehouse_keeper_kluck_q0354_06.htm";
		} else if ("reply_4".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(TORN_MAP_FRAGMENT) >= 10) {
				qs.takeItems(TORN_MAP_FRAGMENT, 10);
				qs.giveItems(PIRATES_TREASURE_MAP, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "warehouse_keeper_kluck_q0354_10.htm";
			}
			return "warehouse_keeper_kluck_q0354_09.htm";
		} else if ("reply_3".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "warehouse_keeper_kluck_q0354_08.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 38) return "warehouse_keeper_kluck_q0354_02.htm";
			qs.exitQuest(true);
			return "warehouse_keeper_kluck_q0354_01.htm";
		} else {
			return "warehouse_keeper_kluck_q0354_05.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		qs.giveItems(ALLIGATOR_TOOTH, 1);
		if (ThreadLocalRandom.current().nextInt(100) < 10) {
			qs.giveItems(TORN_MAP_FRAGMENT, 1);
		}
		qs.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}
