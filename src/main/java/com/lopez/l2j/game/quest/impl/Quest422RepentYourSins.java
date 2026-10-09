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
 * Quest 422: Repent Your Sins
 * Black Judge em Floran Village / Hardin's Academy.
 * Sistema penal de reducao e limpeza de pontos PK atraves do Sin Eater (Penitent's Manacles).
 */
@Component
public class Quest422RepentYourSins extends Quest {

	public static final int QUEST_ID = 422;
	public static final String QUEST_NAME = "422_RepentYourSins";

	public static final int BLACK_JUDGE = 30981;
	public static final int KATARI = 30668;
	public static final int PIOTUR = 30597;
	public static final int CASIAN = 30612;
	public static final int JOAN = 30718;
	public static final int PUSHKIN = 30300;

	// Itens
	public static final int PENITENTS_MANACLES = 4425; // Pet collar do Sin Eater
	public static final int SIN_EATER_MANUAL = 4426;
	public static final int RATMAN_SCAVENGER_SKULL = 4326;
	public static final int TUREK_WAR_HOUND_TAIL = 4327;
	public static final int TYRANT_TALON = 4328;
	public static final int TRISALIM_TARANTULA_VENOM = 4329;

	// Mobs
	public static final int RATMAN_SCAVENGER = 20039;
	public static final int TUREK_WAR_HOUND = 20494;
	public static final int TYRANT = 20193;
	public static final int TRISALIM_TARANTULA = 20561;

	@Autowired
	public Quest422RepentYourSins(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Repent Your Sins");
		addStartNpc(BLACK_JUDGE);
		addTalkId(BLACK_JUDGE, KATARI, PIOTUR, CASIAN, JOAN, PUSHKIN);
		addKillId(RATMAN_SCAVENGER, TUREK_WAR_HOUND, TYRANT, TRISALIM_TARANTULA);
		registerQuestItems(
				PENITENTS_MANACLES, RATMAN_SCAVENGER_SKULL,
				TUREK_WAR_HOUND_TAIL, TYRANT_TALON, TRISALIM_TARANTULA_VENOM
		);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		if ("Start".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			if (pc.level() <= 20) {
				qs.setCond(2);
				return "black_judge_q0422_03.htm";
			} else if (pc.level() <= 30) {
				qs.setCond(3);
				return "black_judge_q0422_04.htm";
			} else if (pc.level() <= 40) {
				qs.setCond(4);
				return "black_judge_q0422_05.htm";
			} else {
				qs.setCond(5);
				return "black_judge_q0422_06.htm";
			}
		} else if ("obtain_manacles".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(16);
			qs.giveItems(PENITENTS_MANACLES, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "black_judge_q0422_11.htm";
		} else if ("clean_pk".equalsIgnoreCase(event) || "3".equalsIgnoreCase(event)) {
			// Remove pontos de PK com base na evolucao do Sin Eater
			int pk = pc.pkKills();
			int pkReduced = ThreadLocalRandom.current().nextInt(1, 11);
			if (pk <= pkReduced) {
				pc.pkKills(0);
				qs.giveItems(SIN_EATER_MANUAL, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(true);
				return "black_judge_q0422_15.htm";
			} else {
				pc.pkKills(pk - pkReduced);
				qs.takeItems(PENITENTS_MANACLES, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "black_judge_q0422_16.htm";
			}
		} else if ("Quit".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "black_judge_q0422_18.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BLACK_JUDGE) {
			if (cond == 0) {
				if (pc.pkKills() >= 1) {
					return "black_judge_q0422_02.htm";
				}
				qs.exitQuest(true);
				return "black_judge_q0422_01.htm";
			} else if (cond >= 16) {
				if (qs.hasQuestItems(PENITENTS_MANACLES)) {
					return "black_judge_q0422_12.htm";
				}
				return "black_judge_q0422_16t.htm";
			}
			return "black_judge_q0422_07.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == RATMAN_SCAVENGER && cond == 2) {
			if (qs.getQuestItemsCount(RATMAN_SCAVENGER_SKULL) < 10) {
				qs.giveItems(RATMAN_SCAVENGER_SKULL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == TUREK_WAR_HOUND && cond == 3) {
			if (qs.getQuestItemsCount(TUREK_WAR_HOUND_TAIL) < 10) {
				qs.giveItems(TUREK_WAR_HOUND_TAIL, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == TYRANT && cond == 4) {
			if (qs.getQuestItemsCount(TYRANT_TALON) < 10) {
				qs.giveItems(TYRANT_TALON, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == TRISALIM_TARANTULA && cond == 5) {
			if (qs.getQuestItemsCount(TRISALIM_TARANTULA_VENOM) < 10) {
				qs.giveItems(TRISALIM_TARANTULA_VENOM, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
