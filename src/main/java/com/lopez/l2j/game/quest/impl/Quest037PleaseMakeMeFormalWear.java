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
 * Quest 37: Please Make Me Formal Wear
 * Trader Alexis, Maid Leikar, Chef Jeremy e Information Broker Mist.
 */
@Component
public class Quest037PleaseMakeMeFormalWear extends Quest {

	public static final int QUEST_ID = 37;
	public static final String QUEST_NAME = "037_PleaseMakeMeFormalWear";

	public static final int START_NPC = 30842;
	public static final int ALEXIS = 30842;
	public static final int LEIKAR = 31520;
	public static final int JEREMY = 31521;
	public static final int MIST = 31627;

	public static final int MYSTERIOUS_CLOTH = 7076;
	public static final int JEWEL_BOX = 7077;
	public static final int SEWING_KIT = 7078;
	public static final int DRESS_SHOES_BOX = 7113;
	public static final int SIGNET_RING = 7164;
	public static final int ICE_WINE = 7160;
	public static final int BOX_OF_COOKIES = 7159;
	public static final int FORMAL_WEAR = 6408;

	@Autowired
	public Quest037PleaseMakeMeFormalWear(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Please Make Me Formal Wear");
		addStartNpc(ALEXIS);
		addTalkId(ALEXIS, LEIKAR, JEREMY, MIST);
		registerQuestItems(SIGNET_RING, ICE_WINE, BOX_OF_COOKIES);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30842-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30842-1.htm";
		} else if ("31520-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.giveItems(SIGNET_RING, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31520-1.htm";
		} else if ("31521-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.takeItems(SIGNET_RING, 1);
			qs.giveItems(ICE_WINE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31521-1.htm";
		} else if ("31627-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.takeItems(ICE_WINE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31627-1.htm";
		} else if ("31521-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.giveItems(BOX_OF_COOKIES, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31521-3.htm";
		} else if ("31520-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.takeItems(BOX_OF_COOKIES, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31520-3.htm";
		} else if ("31520-5.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.takeItems(MYSTERIOUS_CLOTH, 1);
			qs.takeItems(JEWEL_BOX, 1);
			qs.takeItems(SEWING_KIT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31520-5.htm";
		} else if ("31520-7.htm".equalsIgnoreCase(event)) {
			qs.takeItems(DRESS_SHOES_BOX, 1);
			qs.giveItems(FORMAL_WEAR, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31520-7.htm";
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
			case ALEXIS:
				if (cond == 0) {
					if (pc.level() >= 60) return "30842-0.htm";
					qs.exitQuest(true);
					return "30842-0a.htm";
				} else if (cond >= 1) {
					if (qs.hasQuestItems(DRESS_SHOES_BOX) && qs.hasQuestItems(SEWING_KIT) && qs.hasQuestItems(MYSTERIOUS_CLOTH) && qs.hasQuestItems(JEWEL_BOX)) {
						qs.takeItems(DRESS_SHOES_BOX, -1);
						qs.takeItems(SEWING_KIT, -1);
						qs.takeItems(MYSTERIOUS_CLOTH, -1);
						qs.takeItems(JEWEL_BOX, -1);
						qs.giveItems(FORMAL_WEAR, 1);
						qs.playSound(QuestState.SOUND_FINISH);
						qs.exitQuest(false);
						return "30842-3.htm";
					}
					return "30842-2.htm";
				}
				break;
			case LEIKAR:
				if (cond == 1) {
					return "31520-0.htm";
				} else if (cond >= 2 && cond <= 4) {
					return "31520-1a.htm";
				} else if (cond == 5 && qs.hasQuestItems(BOX_OF_COOKIES)) {
					return "31520-2.htm";
				} else if (cond == 6) {
					if (qs.hasQuestItems(MYSTERIOUS_CLOTH) && qs.hasQuestItems(JEWEL_BOX) && qs.hasQuestItems(SEWING_KIT)) {
						return "31520-4.htm";
					}
					return "31520-3a.htm";
				} else if (cond == 7) {
					return qs.hasQuestItems(DRESS_SHOES_BOX) ? "31520-6.htm" : "31520-5a.htm";
				}
				break;
			case JEREMY:
				if (cond == 2 && qs.hasQuestItems(SIGNET_RING)) {
					return "31521-0.htm";
				} else if (cond == 3) {
					return "31521-1a.htm";
				} else if (cond == 4) {
					return "31521-2.htm";
				} else if (cond >= 5) {
					return "31521-3a.htm";
				}
				break;
			case MIST:
				if (cond == 3 && qs.hasQuestItems(ICE_WINE)) {
					return "31627-0.htm";
				} else if (cond >= 4) {
					return "31627-2.htm";
				}
				break;
		}
		return "noquest";
	}
}
