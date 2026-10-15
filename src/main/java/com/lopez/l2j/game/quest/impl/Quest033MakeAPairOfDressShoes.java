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
 * Quest 33: Make a Pair of Dress Shoes
 * Trader Woodley, Leikar e Trader Ian.
 */
@Component
public class Quest033MakeAPairOfDressShoes extends Quest {

	public static final int QUEST_ID = 33;
	public static final String QUEST_NAME = "033_MakeaPairofDressShoes";

	public static final int START_NPC = 30838;
	public static final int WOODLEY = 30838;
	public static final int LEIKAR = 31520;
	public static final int IAN = 30164;

	public static final int LEATHER = 1882;
	public static final int THREAD = 1868;
	public static final int ADENA = 57;
	public static final int DRESS_SHOES_BOX = 7113;

	@Autowired
	public Quest033MakeAPairOfDressShoes(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Make a Pair of Dress Shoes");
		addStartNpc(WOODLEY);
		addTalkId(WOODLEY, LEIKAR, IAN);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30838-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30838-1.htm";
		} else if ("31520-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31520-1.htm";
		} else if ("30838-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30838-3.htm";
		} else if ("30838-5.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(LEATHER) >= 200 && qs.getQuestItemsCount(THREAD) >= 600 && qs.getQuestItemsCount(ADENA) >= 200000) {
				qs.setCond(4);
				qs.takeItems(LEATHER, 200);
				qs.takeItems(THREAD, 600);
				qs.takeItems(ADENA, 200000);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30838-5.htm";
			}
			return "30838-5a.htm";
		} else if ("30164-1.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ADENA) >= 300000) {
				qs.setCond(5);
				qs.takeItems(ADENA, 300000);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30164-1.htm";
			}
			return "30164-1a.htm";
		} else if ("30838-7.htm".equalsIgnoreCase(event)) {
			qs.giveItems(DRESS_SHOES_BOX, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "30838-7.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case WOODLEY:
				if (cond == 0) {
					if (pc.level() >= 60) return "30838-0.htm";
					qs.exitQuest(true);
					return "30838-00.htm";
				} else if (cond == 1) {
					return "30838-1.htm";
				} else if (cond == 2) {
					return "30838-2.htm";
				} else if (cond == 3) {
					if (qs.getQuestItemsCount(LEATHER) >= 200 && qs.getQuestItemsCount(THREAD) >= 600 && qs.getQuestItemsCount(ADENA) >= 200000) {
						return "30838-4.htm";
					}
					return "30838-4r.htm";
				} else if (cond == 4) {
					return "30838-5r.htm";
				} else if (cond == 5) {
					return "30838-6.htm";
				}
				break;
			case LEIKAR:
				if (cond == 1) {
					return "31520-0.htm";
				} else if (cond >= 2) {
					return "31520-1r.htm";
				}
				break;
			case IAN:
				if (cond == 4) {
					return "30164-0.htm";
				} else if (cond >= 5) {
					return "30164-2.htm";
				}
				break;
		}
		return "noquest";
	}
}
