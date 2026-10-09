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
 * Quest 624: The Finest Ingredients - Part 1
 * Requisito essencial da 3ª mudança de classe: obtenção do Ice Crystal (Cryolite 7080) com Jeremy em Hot Springs.
 */
@Component
public class Quest624TheFinestIngredientsPart1 extends Quest {

	public static final int QUEST_ID = 624;
	public static final String QUEST_NAME = "624_TheFinestIngredientsPart1";

	// NPCs
	public static final int JEREMY = 31521;

	// Monstros
	public static final int ATROX = 21321;
	public static final int ATROX_SPAWN = 21317;
	public static final int BANDERSNATCHLING = 21314;
	public static final int NEPENTHES = 21319;

	// Itens
	public static final int TRUNK_OF_NEPENTHES = 7202;
	public static final int FOOT_OF_BANDERSNATCHLING = 7203;
	public static final int SECRET_SPICE = 7204;
	public static final int SAUCE = 7205;
	public static final int ICE_CRYSTAL = 7080;

	@Autowired
	public Quest624TheFinestIngredientsPart1(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Finest Ingredients - Part 1");

		addStartNpc(JEREMY);
		addTalkId(JEREMY);

		addKillId(ATROX, ATROX_SPAWN, BANDERSNATCHLING, NEPENTHES);

		registerQuestItems(TRUNK_OF_NEPENTHES, FOOT_OF_BANDERSNATCHLING, SECRET_SPICE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31521-1.htm".equalsIgnoreCase(event)) {
			PlayerCharacter player = qs.getPlayerCharacter();
			if (player != null && player.level() >= 73) {
				qs.setCond(1);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				return event;
			} else {
				qs.exitCurrentQuest(true);
				return "31521-0a.htm";
			}
		} else if ("31521-4.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(TRUNK_OF_NEPENTHES) >= 50
					&& qs.getQuestItemsCount(FOOT_OF_BANDERSNATCHLING) >= 50
					&& qs.getQuestItemsCount(SECRET_SPICE) >= 50) {
				qs.takeItems(TRUNK_OF_NEPENTHES, -1);
				qs.takeItems(FOOT_OF_BANDERSNATCHLING, -1);
				qs.takeItems(SECRET_SPICE, -1);
				qs.giveItems(SAUCE, 1);
				qs.giveItems(ICE_CRYSTAL, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(true);
				return "31521-4.htm";
			} else {
				qs.setCond(1);
				return "31521-5.htm";
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
			return "31521-0.htm";
		} else if (cond == 1 || cond == 2) {
			return "31521-2.htm";
		} else if (cond == 3) {
			return "31521-3.htm";
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

		if (cond == 1 || cond == 2) {
			int targetItem = 0;
			if (npcId == ATROX || npcId == ATROX_SPAWN) {
				targetItem = SECRET_SPICE;
			} else if (npcId == BANDERSNATCHLING) {
				targetItem = FOOT_OF_BANDERSNATCHLING;
			} else if (npcId == NEPENTHES) {
				targetItem = TRUNK_OF_NEPENTHES;
			}

			if (targetItem > 0 && qs.getQuestItemsCount(targetItem) < 50) {
				if (ThreadLocalRandom.current().nextInt(100) < 80) {
					qs.giveItems(targetItem, 1);
					checkCompletion(qs);
				}
			}
		}

		return null;
	}

	private void checkCompletion(QuestState qs) {
		if (qs.getQuestItemsCount(TRUNK_OF_NEPENTHES) >= 50
				&& qs.getQuestItemsCount(FOOT_OF_BANDERSNATCHLING) >= 50
				&& qs.getQuestItemsCount(SECRET_SPICE) >= 50) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}
}
