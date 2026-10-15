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
 * Mysterious Wizard (31522) na Floresta dos Mortos (Forest of the Dead).
 * Cacada de mortos-vivos e vampiros para obter Vampire Hearts e Zombie Brains.
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

	public static final int[] UNDEAD_MOBS = {
			21547, 21548, 21549, 21550, 21551, 21552, 21555, 21556, 21562, 21571, 21576, 21577, 21579
	};

	@Autowired
	public Quest632NecromancersRequest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Necromancer's Request");
		addStartNpc(SHADOW_HARDIN);
		addTalkId(SHADOW_HARDIN);
		addKillId(VAMPIRE_MOBS);
		addKillId(UNDEAD_MOBS);
		registerQuestItems(VAMPIRE_HEART, ZOMBIE_BRAIN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "31522-03.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31522-03.htm".equalsIgnoreCase(event) ? "31522-03.htm" : "shadow_hardin_q0632_0104.htm";
		} else if ("31522-02.htm".equalsIgnoreCase(event)) {
			return "31522-02.htm";
		} else if ("632_1".equalsIgnoreCase(event)) {
			return "31522-05.htm";
		} else if ("632_3".equalsIgnoreCase(event) || "reward_exchange".equalsIgnoreCase(event) || "31522-06.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(VAMPIRE_HEART) >= 200) {
				qs.takeItems(VAMPIRE_HEART, 200);
				qs.giveItems(57, 120000); // 120k adena
				qs.setCond(1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "31522-06.htm".equalsIgnoreCase(event) ? "31522-06.htm" : "shadow_hardin_q0632_0202.htm";
			}
			return "31522-05.htm";
		} else if ("632_4".equalsIgnoreCase(event) || "31522-08.htm".equalsIgnoreCase(event)) {
			qs.takeItems(VAMPIRE_HEART, -1);
			qs.takeItems(ZOMBIE_BRAIN, -1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "31522-08.htm".equalsIgnoreCase(event) ? "31522-08.htm" : "shadow_hardin_q0632_0204.htm";
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

	private boolean isUndeadMob(int npcId) {
		for (int id : UNDEAD_MOBS) {
			if (id == npcId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() >= 1) {
			if (npc != null && isUndeadMob(npc.getNpcId())) {
				qs.giveItems(ZOMBIE_BRAIN, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else {
				if (qs.getQuestItemsCount(VAMPIRE_HEART) < 200) {
					qs.giveItems(VAMPIRE_HEART, 1);
					if (qs.getQuestItemsCount(VAMPIRE_HEART) >= 200) {
						qs.setCond(2);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
				}
			}
		}
		return null;
	}
}
