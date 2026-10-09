package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 641: Attack Sailren
 * Missão de acesso ao Grand Boss Sailren em Primeval Isle através do Gazkh Fragment.
 */
@Component
public class Quest641AttackSailren extends Quest {

	public static final int QUEST_ID = 641;
	public static final String QUEST_NAME = "641_AttackSailren";

	// NPCs
	public static final int STATUE_OF_SHILEN = 32109;

	// Monstros (Primeval Isle Dinossauros)
	public static final int VELOCIRAPTOR = 22196;
	public static final int PTEROSAUR = 22197;
	public static final int ORNITHOMIMUS = 22198;
	public static final int DEINONYCHUS = 22218;
	public static final int PACHYCEPHALOSAURUS = 22223;
	public static final int TYRANNOSAURUS = 22199;

	// Itens
	public static final int FRAGMENT_OF_GAZKH = 8782;
	public static final int GAZKH = 8784;

	@Autowired
	public Quest641AttackSailren(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Attack Sailren");

		addStartNpc(STATUE_OF_SHILEN);
		addTalkId(STATUE_OF_SHILEN);

		addKillId(VELOCIRAPTOR, PTEROSAUR, ORNITHOMIMUS, DEINONYCHUS, PACHYCEPHALOSAURUS, TYRANNOSAURUS);

		registerQuestItems(FRAGMENT_OF_GAZKH);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("statue_of_shilen_q0641_05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("statue_of_shilen_q0641_08.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(FRAGMENT_OF_GAZKH) >= 30) {
				qs.takeItems(FRAGMENT_OF_GAZKH, -1);
				qs.giveItems(GAZKH, 1);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(true);
				return event;
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
			if (player != null && player.level() >= 77) {
				return "statue_of_shilen_q0641_01.htm";
			} else {
				return "statue_of_shilen_q0641_02.htm";
			}
		} else if (cond == 1) {
			return "statue_of_shilen_q0641_05.htm";
		} else if (cond == 2) {
			if (qs.getQuestItemsCount(FRAGMENT_OF_GAZKH) >= 30) {
				return "statue_of_shilen_q0641_07.htm";
			}
			return "statue_of_shilen_q0641_05.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (qs.getQuestItemsCount(FRAGMENT_OF_GAZKH) < 30) {
			qs.giveItems(FRAGMENT_OF_GAZKH, 1);
			if (qs.getQuestItemsCount(FRAGMENT_OF_GAZKH) >= 30) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
