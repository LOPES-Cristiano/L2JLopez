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
 * Quest 642: A Powerful Primeval Creature
 * Dindin em Primeval Isle. Cacada de Dinossauros para obter Dinosaur Tissue e Dinosaur Egg.
 * Recompensa: Receitas de Armas Top A-Grade e S-Grade (Forgotten Blade, Angel Slayer, Draconic Bow, etc.).
 */
@Component
public class Quest642APowerfulPrimevalCreature extends Quest {

	public static final int QUEST_ID = 642;
	public static final String QUEST_NAME = "642_APowerfulPrimevalCreature";

	public static final int DINDIN = 32105;
	public static final int DIN_DIN = DINDIN;
	public static final int DINOSAUR_TISSUE = 8774;
	public static final int DINOSAUR_EGG = 8775;

	public static final int[] DINOS = {
			18344, 22204, 22203, 22225, 22220, 22205, 22201, 22200, 22224, 22219, 22202, 22199
	};

	// S-Grade Weapon Recipes
	public static final int REC_FORGOTTEN_BLADE = 8690;
	public static final int REC_DRACONIC_BOW = 8694;
	public static final int REC_ANGEL_SLAYER = 8698;
	public static final int REC_SAINTS_SPEAR = 8704;

	@Autowired
	public Quest642APowerfulPrimevalCreature(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "A Powerful Primeval Creature");
		addStartNpc(DINDIN);
		addTalkId(DINDIN);
		addKillId(DINOS);
		registerQuestItems(DINOSAUR_TISSUE, DINOSAUR_EGG);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event))) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "dindin_q0642_04.htm";
		} else if ("reward_fb".equalsIgnoreCase(event) || "reward_recipe".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(DINOSAUR_TISSUE) >= 150) {
				qs.takeItems(DINOSAUR_TISSUE, 150);
				if (qs.hasQuestItems(DINOSAUR_EGG)) {
					qs.takeItems(DINOSAUR_EGG, 1);
				}
				qs.giveItems(REC_FORGOTTEN_BLADE, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "dindin_q0642_12.htm";
			}
		} else if ("reward_db".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(DINOSAUR_TISSUE) >= 150 && qs.hasQuestItems(DINOSAUR_EGG)) {
				qs.takeItems(DINOSAUR_TISSUE, 150);
				qs.takeItems(DINOSAUR_EGG, 1);
				qs.giveItems(REC_DRACONIC_BOW, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "dindin_q0642_12.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event)) {
			// Troca tecidos por adena: 5000 por tecido
			long tissues = qs.getQuestItemsCount(DINOSAUR_TISSUE);
			if (tissues > 0) {
				qs.takeItems(DINOSAUR_TISSUE, -1);
				qs.giveItems(57, (int) (tissues * 5000));
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "dindin_q0642_08.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 75) return "dindin_q0642_01.htm";
			qs.exitQuest(true);
			return "dindin_q0642_01a.htm";
		} else {
			if (qs.getQuestItemsCount(DINOSAUR_TISSUE) >= 150 && qs.hasQuestItems(DINOSAUR_EGG)) {
				return "dindin_q0642_10.htm";
			}
			return "dindin_q0642_05.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		qs.giveItems(DINOSAUR_TISSUE, 1);
		if (npc.getNpcId() == 18344 || java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 5) {
			if (!qs.hasQuestItems(DINOSAUR_EGG)) {
				qs.giveItems(DINOSAUR_EGG, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		qs.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}

	public String onKill(NpcInstance npc, QuestState qs) {
		return onKill(npc, qs, false);
	}
}
