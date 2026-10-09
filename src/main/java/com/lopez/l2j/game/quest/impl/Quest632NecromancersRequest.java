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
 * Quest 632: Necromancer's Request
 * Shadow Hardin na Floresta dos Mortos (Forest of the Dead).
 * Cacada de mortos-vivos para obter Vampire Hearts e Pagan Hearts por adena e pontos de reputacao.
 */
@Component
public class Quest632NecromancersRequest extends Quest {

	public static final int QUEST_ID = 632;
	public static final String QUEST_NAME = "632_NecromancersRequest";

	public static final int SHADOW_HARDIN = 31522;
	public static final int IVOR = SHADOW_HARDIN;
	public static final int VAMPIRE_HEART = 7542;
	public static final int ZOMBIE_BRAIN = 7543;

	public static final int[] VAMPIRE_MOBS = {
			21568, 21573, 21582, 21585, 21586, 21587, 21588, 21589, 21590, 21591, 21592, 21593, 21594, 21595
	};

	@Autowired
	public Quest632NecromancersRequest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Necromancer's Request");
		addStartNpc(SHADOW_HARDIN);
		addTalkId(SHADOW_HARDIN);
		addKillId(VAMPIRE_MOBS);
		registerQuestItems(VAMPIRE_HEART, ZOMBIE_BRAIN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "shadow_hardin_q0632_0104.htm";
		} else if ("632_3".equalsIgnoreCase(event) || "reward_exchange".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(VAMPIRE_HEART) >= 200) {
				qs.takeItems(VAMPIRE_HEART, 200);
				qs.giveItems(57, 120000); // 120k adena
				qs.playSound(QuestState.SOUND_FINISH);
				return "shadow_hardin_q0632_0202.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 63) return "shadow_hardin_q0632_0101.htm";
			qs.exitQuest(true);
			return "shadow_hardin_q0632_0103.htm";
		} else {
			if (qs.getQuestItemsCount(VAMPIRE_HEART) >= 200) {
				return "shadow_hardin_q0632_0105.htm";
			}
			return "shadow_hardin_q0632_0202.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() >= 1) {
			qs.giveItems(VAMPIRE_HEART, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
