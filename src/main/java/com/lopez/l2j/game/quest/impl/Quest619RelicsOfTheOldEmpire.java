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

	// Monstros da entrada do Imperial Tomb e Four Sepulchers
	public static final int[] ENTRANCE_MOBS = {
			21798, 21799, 21800, 18120, 18256, 25339, 25342, 25346, 25349
	};

	public Quest619RelicsOfTheOldEmpire() {
		this(null);
	}

	@Autowired
	public Quest619RelicsOfTheOldEmpire(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Relics of the Old Empire");

		addStartNpc(GHOST_OF_ADVENTURER);
		addTalkId(GHOST_OF_ADVENTURER);

		// Monstros de IT (21396 a 21434)
		for (int mob = 21396; mob <= 21434; mob++) {
			addKillId(mob);
		}
		// Monstros da entrada de IT (21798, 21799, 21800) e Four Sepulchers (18120 a 18256)
		addKillId(ENTRANCE_MOBS);
		for (int mob = 18120; mob <= 18256; mob++) {
			addKillId(mob);
		}

		registerQuestItems(BROKEN_RELIC_PART);

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

		if ("31538-03.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			if (player == null || player.getLevel() >= 74) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				return "31538-03.htm";
			}
			qs.exitCurrentQuest(true);
			return "31538-02.htm";
		} else if ("31538-07.htm".equalsIgnoreCase(event)
				|| "31538-09.htm".equalsIgnoreCase(event)
				|| "reply_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BROKEN_RELIC_PART) >= 1000) {
				qs.takeItems(BROKEN_RELIC_PART, 1000);
				int reward = REWARD_RECIPES.get(ThreadLocalRandom.current().nextInt(REWARD_RECIPES.size()));
				qs.giveItems(reward, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "31538-09.htm".equalsIgnoreCase(event) ? "31538-09.htm" : "31538-07.htm";
			}
			return "31538-06.htm";
		} else if ("31538-08.htm".equalsIgnoreCase(event)
				|| "31538-10.htm".equalsIgnoreCase(event)
				|| "reply_2".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
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
				return "31538-06.htm";
			} else {
				return "31538-07.htm";
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
		qs.playSound(QuestState.SOUND_ITEMGET);

		if (ThreadLocalRandom.current().nextInt(100) < 5) {
			qs.giveItems(SEALED_PASS_TO_IMPERIAL_TOMB, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
