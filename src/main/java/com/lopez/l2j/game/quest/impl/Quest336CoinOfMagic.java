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
 * Quest 336: Coin of Magic
 * Hunter's Village Sorcerer Bernard e Warehouse Keeper Sorint.
 * Sistema de moedas de sangue/ouro e colecao numismatica para equipamentos B/A Grade.
 */
@Component
public class Quest336CoinOfMagic extends Quest {

	public static final int QUEST_ID = 336;
	public static final String QUEST_NAME = "336_CoinOfMagic";

	public static final int SORINT = 30232;
	public static final int BERNARD = 30702;

	// Moedas basicas
	public static final int BLOOD_MEDUSA = 3472;
	public static final int BLOOD_WEREWOLF = 3473;
	public static final int BLOOD_BASILISK = 3474;
	public static final int SILVER_DRAGON = 3475;
	public static final int GOLD_DRAGON = 3476;
	public static final int COIN_DIAGRAM = 3811;
	public static final int KALDIS_COIN = 3812;

	public static final int[] MOBS = {
			20584, 20585, 20587, 20604, 20678, 20663, 20235, 20583,
			20146, 20240, 20245, 20568, 20569, 20685, 20572, 20161
	};

	@Autowired
	public Quest336CoinOfMagic(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Coin of Magic");
		addStartNpc(SORINT);
		addTalkId(SORINT, BERNARD);
		addKillId(MOBS);
		registerQuestItems(COIN_DIAGRAM, KALDIS_COIN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event))) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(COIN_DIAGRAM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "warehouse_keeper_sorint_q0336_05.htm";
		} else if ("talk_bernard".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "hunter_bernard_q0336_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == SORINT) {
			if (cond == 0) {
				if (pc.level() >= 40) return "warehouse_keeper_sorint_q0336_01.htm";
				qs.exitQuest(true);
				return "warehouse_keeper_sorint_q0336_03.htm";
			}
			return "warehouse_keeper_sorint_q0336_06.htm";
		} else if (npcId == BERNARD) {
			if (cond == 1) return "hunter_bernard_q0336_01.htm";
			return "hunter_bernard_q0336_03.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() >= 1 && ThreadLocalRandom.current().nextInt(100) < 30) {
			int coin = switch (ThreadLocalRandom.current().nextInt(3)) {
				case 0 -> BLOOD_MEDUSA;
				case 1 -> BLOOD_WEREWOLF;
				default -> BLOOD_BASILISK;
			};
			qs.giveItems(coin, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
