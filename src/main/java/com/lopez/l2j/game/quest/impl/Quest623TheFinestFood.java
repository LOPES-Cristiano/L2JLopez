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
 * Quest 623: The Finest Food
 * Farm culinário de Hot Springs com Jeremy para obtenção de receitas e ingredientes raros.
 */
@Component
public class Quest623TheFinestFood extends Quest {

	public static final int QUEST_ID = 623;
	public static final String QUEST_NAME = "623_TheFinestFood";

	// NPCs
	public static final int JEREMY = 31521;

	// Monstros
	public static final int HOT_SPRINGS_BUFFALO = 21315;
	public static final int HOT_SPRINGS_FLAVA = 21316;
	public static final int HOT_SPRINGS_ANTELOPE = 21318;

	// Itens
	public static final int LEAF_OF_FLAVA = 7199;
	public static final int BUFFALO_MEAT = 7200;
	public static final int ANTELOPE_HORN = 7201;

	@Autowired
	public Quest623TheFinestFood(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Finest Food");

		addStartNpc(JEREMY);
		addTalkId(JEREMY);

		addKillId(HOT_SPRINGS_BUFFALO, HOT_SPRINGS_FLAVA, HOT_SPRINGS_ANTELOPE);

		registerQuestItems(LEAF_OF_FLAVA, BUFFALO_MEAT, ANTELOPE_HORN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31521-03.htm".equalsIgnoreCase(event)) {
			PlayerCharacter player = qs.getPlayerCharacter();
			if (player != null && player.level() >= 71) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				return event;
			} else {
				qs.exitCurrentQuest(true);
				return "31521-02.htm";
			}
		} else if ("31521-07.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(LEAF_OF_FLAVA) >= 100
					&& qs.getQuestItemsCount(BUFFALO_MEAT) >= 100
					&& qs.getQuestItemsCount(ANTELOPE_HORN) >= 100) {
				qs.takeItems(LEAF_OF_FLAVA, -1);
				qs.takeItems(BUFFALO_MEAT, -1);
				qs.takeItems(ANTELOPE_HORN, -1);
				qs.giveItems(57, 73000);
				qs.addExpAndSp(230000, 18250);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(true);
				return "31521-06.htm";
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int cond = qs.getCond();

		if (cond == 0) {
			return "31521-01.htm";
		} else if (cond == 1) {
			return "31521-05.htm";
		} else if (cond == 2) {
			return "31521-04.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 1) {
			int targetItem = 0;
			int chance = 0;
			if (npcId == HOT_SPRINGS_BUFFALO) {
				targetItem = BUFFALO_MEAT;
				chance = 80;
			} else if (npcId == HOT_SPRINGS_FLAVA) {
				targetItem = LEAF_OF_FLAVA;
				chance = 70;
			} else if (npcId == HOT_SPRINGS_ANTELOPE) {
				targetItem = ANTELOPE_HORN;
				chance = 90;
			}

			if (targetItem > 0 && qs.getQuestItemsCount(targetItem) < 100) {
				if (ThreadLocalRandom.current().nextInt(100) < chance) {
					qs.giveItems(targetItem, 1);
					if (qs.getQuestItemsCount(LEAF_OF_FLAVA) >= 100
							&& qs.getQuestItemsCount(BUFFALO_MEAT) >= 100
							&& qs.getQuestItemsCount(ANTELOPE_HORN) >= 100) {
						qs.setCond(2);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}

		return null;
	}
}
