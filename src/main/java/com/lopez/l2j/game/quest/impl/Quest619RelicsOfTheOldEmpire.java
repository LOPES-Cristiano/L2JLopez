package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 619: Relics of the Old Empire
 *
 * Farm massivo no Imperial Tomb (IT).
 * Ghost of Adventurer (31538) troca 1000 Broken Relic Parts por receitas S-Grade.
 */
@Component
public class Quest619RelicsOfTheOldEmpire extends Quest {

	public static final int QUEST_ID = 619;
	public static final String QUEST_NAME = "619_RelicsOfTheOldEmpire";

	public static final int GHOST_OF_ADVENTURER = 31538;

	// Itens
	public static final int BROKEN_RELIC_PART = 7254;
	public static final int SEALED_PASS_TO_IMPERIAL_TOMB = 7075;

	// Receitas S-Grade (60%)
	public static final List<Integer> REWARD_RECIPES = List.of(
			6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580
	);

	@Autowired
	public Quest619RelicsOfTheOldEmpire(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Relics of the Old Empire");

		addStartNpc(GHOST_OF_ADVENTURER);
		addTalkId(GHOST_OF_ADVENTURER);

		// Monstros de IT (21396 a 21434 e 21798 a 21800)
		for (int mob = 21396; mob <= 21434; mob++) {
			addKillId(mob);
		}
		for (int mob = 21798; mob <= 21800; mob++) {
			addKillId(mob);
		}

		registerQuestItems(BROKEN_RELIC_PART, SEALED_PASS_TO_IMPERIAL_TOMB);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();

		if ("31538-03.htm".equalsIgnoreCase(event)) {
			if (player != null && player.getLevel() >= 74) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound("ItemSound.quest_accept");
				return event;
			}
			return "31538-02.htm";
		} else if ("31538-07.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BROKEN_RELIC_PART) >= 1000) {
				qs.takeItems(BROKEN_RELIC_PART, 1000);
				int reward = REWARD_RECIPES.get(ThreadLocalRandom.current().nextInt(REWARD_RECIPES.size()));
				qs.giveItems(reward, 1);
				qs.playSound("ItemSound.quest_finish");
				return event;
			}
			return "31538-05.htm";
		} else if ("31538-08.htm".equalsIgnoreCase(event)) {
			qs.exitCurrentQuest(true);
			return event;
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();

		if (qs.getState() == State.CREATED) {
			if (player != null && player.getLevel() >= 74) {
				return "31538-01.htm";
			}
			return "31538-02.htm";
		}

		if (qs.getState() == State.STARTED) {
			long relics = qs.getQuestItemsCount(BROKEN_RELIC_PART);
			if (relics >= 1000) {
				return "31538-04.htm";
			} else if (qs.hasQuestItems(SEALED_PASS_TO_IMPERIAL_TOMB)) {
				return "31538-05.htm";
			} else {
				return "31538-05a.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		qs.giveItems(BROKEN_RELIC_PART, 1);
		qs.playSound("ItemSound.quest_itemget");

		if (ThreadLocalRandom.current().nextInt(100) < 5) {
			qs.giveItems(SEALED_PASS_TO_IMPERIAL_TOMB, 1);
			qs.playSound("ItemSound.quest_middle");
		}

		return null;
	}
}
