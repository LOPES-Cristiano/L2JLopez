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
 * Quest 35: Find Glittering Jewelry
 * Jeweler Ellie e Wharf Manager Felton.
 */
@Component
public class Quest035FindGlitteringJewelry extends Quest {

	public static final int QUEST_ID = 35;
	public static final String QUEST_NAME = "035_FindGlitteringJewelry";

	public static final int START_NPC = 30091;
	public static final int ELLIE = 30091;
	public static final int FELTON = 30879;

	public static final int ALLIGATOR = 20135;

	public static final int ROUGH_JEWEL = 7162;
	public static final int ORIHARUKON = 1893;
	public static final int SILVER_NUGGET = 1873;
	public static final int THONS = 4044;
	public static final int JEWEL_BOX = 7077;

	@Autowired
	public Quest035FindGlitteringJewelry(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Find Glittering Jewelry");
		addStartNpc(ELLIE);
		addTalkId(ELLIE, FELTON);
		addKillId(ALLIGATOR);
		registerQuestItems(ROUGH_JEWEL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30091-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30091-1.htm";
		} else if ("30879-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30879-1.htm";
		} else if ("30091-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.takeItems(ROUGH_JEWEL, 10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30091-3.htm";
		} else if ("30091-5.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ORIHARUKON) >= 5 && qs.getQuestItemsCount(SILVER_NUGGET) >= 500 && qs.getQuestItemsCount(THONS) >= 150) {
				qs.takeItems(ORIHARUKON, 5);
				qs.takeItems(SILVER_NUGGET, 500);
				qs.takeItems(THONS, 150);
				qs.giveItems(JEWEL_BOX, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30091-5.htm";
			}
			return "30091-hvnmat-bug.htm";
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
			case ELLIE:
				if (cond == 0) {
					if (pc.level() >= 60) return "30091-0.htm";
					qs.exitQuest(true);
					return "30091-0a.htm";
				} else if (cond == 1) {
					return "30091-1r.htm";
				} else if (cond == 2) {
					return "30091-1r2.htm";
				} else if (cond == 3 && qs.getQuestItemsCount(ROUGH_JEWEL) >= 10) {
					return "30091-2.htm";
				} else if (cond == 4) {
					if (qs.getQuestItemsCount(ORIHARUKON) >= 5 && qs.getQuestItemsCount(SILVER_NUGGET) >= 500 && qs.getQuestItemsCount(THONS) >= 150) {
						return "30091-4.htm";
					}
					return "30091-hvnmat.htm";
				}
				break;
			case FELTON:
				if (cond == 1) {
					return "30879-0.htm";
				} else if (cond >= 2) {
					return "30879-1r.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == ALLIGATOR && qs.getCond() == 2) {
			long count = qs.getQuestItemsCount(ROUGH_JEWEL);
			if (count < 10) {
				qs.giveItems(ROUGH_JEWEL, 1);
				if (count + 1 >= 10) {
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
