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
 * Quest 27: Chest Caught with a Bait of Wind
 */
@Component
public class Quest027ChestCaughtWithABaitOfWind extends Quest {

	public static final int QUEST_ID = 27;
	public static final String QUEST_NAME = "027_ChestCaughtwithaBaitofWind";

	public static final int START_NPC = 31570;
	public static final int LANOSCO = 31570;
	public static final int SHALING = 31434;

	public static final int BIG_BLUE_TREASURE_CHEST = 6500;
	public static final int STRANGE_BLUEPRINT = 7625;
	public static final int BLACK_PEARL_RING = 880;

	@Autowired
	public Quest027ChestCaughtWithABaitOfWind(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Wind");
		addStartNpc(LANOSCO);
		addTalkId(LANOSCO, SHALING);
		registerQuestItems(STRANGE_BLUEPRINT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_lanosco_q0027_0104.htm".equalsIgnoreCase(event) || "31570-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_lanosco_q0027_0104.htm";
		} else if ("fisher_lanosco_q0027_0201.htm".equalsIgnoreCase(event) || "31570-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(BIG_BLUE_TREASURE_CHEST)) {
				qs.takeItems(BIG_BLUE_TREASURE_CHEST, 1);
				qs.giveItems(STRANGE_BLUEPRINT, 1);
				qs.giveItems(7609, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "fisher_lanosco_q0027_0202.htm";
		} else if ("blueprint_seller_shaling_q0027_0301.htm".equalsIgnoreCase(event) || "31434-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(STRANGE_BLUEPRINT) || qs.hasQuestItems(7609)) {
				qs.takeItems(STRANGE_BLUEPRINT, -1);
				qs.takeItems(7609, -1);
				qs.giveItems(BLACK_PEARL_RING, 1);
				qs.giveItems(884, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "blueprint_seller_shaling_q0027_0302.htm";
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

		if (npcId == LANOSCO) {
			if (cond == 0) {
				if (pc.level() >= 27) return "fisher_lanosco_q0027_0101.htm";
				qs.exitQuest(true);
				return "fisher_lanosco_q0027_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(BIG_BLUE_TREASURE_CHEST)) {
					qs.takeItems(BIG_BLUE_TREASURE_CHEST, 1);
					qs.giveItems(STRANGE_BLUEPRINT, 1);
					qs.giveItems(7609, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_lanosco_q0027_0201.htm";
				}
				return "fisher_lanosco_q0027_0106.htm";
			} else if (cond == 2) {
				return "fisher_lanosco_q0027_0203.htm";
			}
		} else if (npcId == SHALING) {
			if (cond == 2 && (qs.hasQuestItems(STRANGE_BLUEPRINT) || qs.hasQuestItems(7609))) {
				qs.takeItems(STRANGE_BLUEPRINT, -1);
				qs.takeItems(7609, -1);
				qs.giveItems(BLACK_PEARL_RING, 1);
				qs.giveItems(884, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "blueprint_seller_shaling_q0027_0301.htm";
			}
		}
		return "noquest";
	}
}
