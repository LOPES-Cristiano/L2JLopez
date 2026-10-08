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
 * Quest 643: Rise and Fall of the Elroki Tribe
 * Singsing e Shaman Karakawei em Primeval Isle.
 * Cacada de Velociraptors e Pterossauros para obter Bones of a Plains Dinosaur trocados por 5 partes de armas Top A-Grade.
 */
@Component
public class Quest643RiseAndFallOfTheElrokiTribe extends Quest {

	public static final int QUEST_ID = 643;
	public static final String QUEST_NAME = "643_RiseandFalloftheElrokiTribe";

	public static final int SINGSING = 32106;
	public static final int KARAKAWEI = 32117;
	public static final int DINOSAUR_BONES = 8776;

	// Aliases de compatibilidade para testes
	public static final int SIN_SIN = SINGSING;
	public static final int BONES = DINOSAUR_BONES;

	public static final int[] MOBS = {
			22209, 22208, 22226, 22221, 22210, 22212, 22211, 22227, 22222, 22213
	};

	@Autowired
	public Quest643RiseAndFallOfTheElrokiTribe(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Rise and Fall of the Elroki Tribe");
		addStartNpc(SINGSING);
		addTalkId(SINGSING, KARAKAWEI);
		addKillId(MOBS);
		registerQuestItems(DINOSAUR_BONES);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "singsing_q0643_05.htm";
		} else if ("reply_2".equalsIgnoreCase(event)) {
			long bones = qs.getQuestItemsCount(DINOSAUR_BONES);
			if (bones > 0) {
				qs.takeItems(DINOSAUR_BONES, -1);
				qs.giveItems(57, (int) (bones * 1374));
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "singsing_q0643_08.htm";
			}
		} else if ("reward_parts".equalsIgnoreCase(event) || "reply_7".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(DINOSAUR_BONES) >= 300) {
				qs.takeItems(DINOSAUR_BONES, 300);
				int partId = 8712 + ThreadLocalRandom.current().nextInt(11);
				qs.giveItems(partId, 5); // 5 partes de arma Top A
				qs.playSound(QuestState.SOUND_FINISH);
				return "shaman_caracawe_q0643_06.htm";
			}
			return "shaman_caracawe_q0643_05.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		if (npcId == SINGSING) {
			if (qs.getState() == State.CREATED) {
				if (pc.level() >= 75) return "singsing_q0643_01.htm";
				qs.exitQuest(true);
				return "singsing_q0643_05a.htm";
			}
			return "singsing_q0643_06.htm";
		} else if (npcId == KARAKAWEI) {
			if (qs.getQuestItemsCount(DINOSAUR_BONES) >= 300) {
				return "shaman_caracawe_q0643_03.htm";
			}
			return "shaman_caracawe_q0643_02.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		qs.giveItems(DINOSAUR_BONES, 1);
		qs.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}
