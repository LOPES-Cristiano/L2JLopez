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
 * Quest 625: The Finest Ingredients - Part 2
 * Derrota do Raid Boss Icicle Emperor Bumbalump em Hot Springs e obtenção de Dyes raros.
 */
@Component
public class Quest625TheFinestIngredientsPart2 extends Quest {

	public static final int QUEST_ID = 625;
	public static final String QUEST_NAME = "625_TheFinestIngredientsPart2";

	// NPCs
	public static final int JEREMY = 31521;
	public static final int TABLE = 31542;

	// Raid Boss
	public static final int BUMBALUMP = 25296;

	// Itens
	public static final int SAUCE = 7205;
	public static final int FOOD = 7209;
	public static final int MEAT = 7210;

	// Dyes de Recompensa
	public static final int[] REWARDS = {4589, 4590, 4591, 4592, 4593, 4594};

	@Autowired
	public Quest625TheFinestIngredientsPart2(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Finest Ingredients - Part 2");

		addStartNpc(JEREMY);
		addTalkId(JEREMY, TABLE);

		addKillId(BUMBALUMP);

		registerQuestItems(FOOD, MEAT);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31521-02.htm".equalsIgnoreCase(event)) {
			PlayerCharacter player = qs.getPlayerCharacter();
			if (player != null && player.level() >= 73 && qs.hasQuestItems(SAUCE)) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.takeItems(SAUCE, 1);
				qs.giveItems(FOOD, 1);
				qs.playSound(QuestState.SOUND_ACCEPT);
				return event;
			} else {
				qs.exitCurrentQuest(true);
				return "31521-00b.htm";
			}
		} else if ("31542-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(FOOD)) {
				qs.takeItems(FOOD, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			} else {
				return "31542-04.htm";
			}
		} else if ("31521-04.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MEAT)) {
				qs.takeItems(MEAT, -1);
				int rewardDye = REWARDS[ThreadLocalRandom.current().nextInt(REWARDS.length)];
				qs.giveItems(rewardDye, 5);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(true);
				return "31521-04.htm";
			} else {
				return "31521-05.htm";
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == JEREMY) {
			if (cond == 0) {
				if (qs.hasQuestItems(SAUCE)) {
					return "31521-01.htm";
				} else {
					return "31521-00.htm";
				}
			} else if (cond == 1) {
				return "31521-02.htm";
			} else if (cond == 2) {
				return "31521-03.htm";
			} else if (cond == 3) {
				return "31521-04.htm";
			}
		} else if (npcId == TABLE) {
			if (cond == 1 && qs.hasQuestItems(FOOD)) {
				return "31542-01.htm";
			} else if (cond == 2) {
				return "31542-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (qs.getCond() == 2 && npc.getNpcId() == BUMBALUMP) {
			if (!qs.hasQuestItems(MEAT)) {
				qs.giveItems(MEAT, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}

		return null;
	}
}
