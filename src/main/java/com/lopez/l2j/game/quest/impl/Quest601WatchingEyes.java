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
 * Quest 601: Watching Eyes
 * Parte 1 do acesso ao Pagan Temple (Eye of Argos em Wall of Argos).
 */
@Component
public class Quest601WatchingEyes extends Quest {

	public static final int QUEST_ID = 601;
	public static final String QUEST_NAME = "601_WatchingEyes";

	// NPCs
	public static final int EYE_OF_ARGOS = 31683;

	// Monstros
	public static final int DISCIPLE_OF_PROTECTION = 21306;
	public static final int DISCIPLE_OF_PUNISHMENT = 21308;
	public static final int DISCIPLE_OF_GRIEF = 21309;
	public static final int DISCIPLE_OF_SECRECY = 21310;
	public static final int DISCIPLE_OF_DEFIANCE = 21311;

	// Itens
	public static final int PROOF_OF_AVENGER = 7188;

	// Recompensas
	public static final int SEALED_TALLUM_GLOVES = 6699;
	public static final int SEALED_MAJESTIC_GAUNTLETS = 6698;
	public static final int SEALED_DARK_CRYSTAL_GLOVES = 6700;

	@Autowired
	public Quest601WatchingEyes(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Watching Eyes");

		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS);

		addKillId(DISCIPLE_OF_PROTECTION, DISCIPLE_OF_PUNISHMENT, DISCIPLE_OF_GRIEF, DISCIPLE_OF_SECRECY, DISCIPLE_OF_DEFIANCE);

		registerQuestItems(PROOF_OF_AVENGER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("quest_accept".equalsIgnoreCase(event) || "eye_of_argos_q0601_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "eye_of_argos_q0601_0104.htm";
		} else if ("reply_3".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(PROOF_OF_AVENGER) >= 100) {
				qs.takeItems(PROOF_OF_AVENGER, 100);
				int roll = ThreadLocalRandom.current().nextInt(1000);
				if (roll < 200) {
					qs.giveItems(SEALED_TALLUM_GLOVES, 5);
					qs.giveItems(57, 90000);
					qs.addExpAndSp(120000, 10000);
				} else if (roll < 400) {
					qs.giveItems(SEALED_MAJESTIC_GAUNTLETS, 5);
					qs.giveItems(57, 80000);
					qs.addExpAndSp(120000, 10000);
				} else if (roll < 500) {
					qs.giveItems(SEALED_DARK_CRYSTAL_GLOVES, 5);
					qs.giveItems(57, 40000);
					qs.addExpAndSp(120000, 10000);
				} else {
					qs.giveItems(57, 230000);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(true);
				return "eye_of_argos_q0601_0201.htm";
			} else {
				return "eye_of_argos_q0601_0202.htm";
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int cond = qs.getCond();

		if (cond == 0) {
			if (player != null && player.level() >= 71) {
				return "eye_of_argos_q0601_0101.htm";
			} else {
				return "eye_of_argos_q0601_0103.htm";
			}
		} else if (cond == 1) {
			if (qs.getQuestItemsCount(PROOF_OF_AVENGER) >= 100) {
				return "eye_of_argos_q0601_0105.htm";
			}
			return "eye_of_argos_q0601_0106.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (qs.getQuestItemsCount(PROOF_OF_AVENGER) < 100) {
			if (ThreadLocalRandom.current().nextInt(100) < 65) {
				qs.giveItems(PROOF_OF_AVENGER, 1);
				if (qs.getQuestItemsCount(PROOF_OF_AVENGER) >= 100) {
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}
