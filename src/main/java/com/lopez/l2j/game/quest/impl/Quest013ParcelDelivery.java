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
 * Quest 013: Parcel Delivery
 * Fundin em Rune e Vulcan na Forge of the Gods.
 */
@Component
public class Quest013ParcelDelivery extends Quest {

	public static final int QUEST_ID = 13;
	public static final String QUEST_NAME = "013_ParcelDelivery";

	public static final int FUNDIN = 31274;
	public static final int VULCAN = 31539;
	public static final int PACKAGE = 7263;

	@Autowired
	public Quest013ParcelDelivery(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Parcel Delivery");
		addStartNpc(FUNDIN);
		addTalkId(FUNDIN, VULCAN);
		registerQuestItems(PACKAGE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(PACKAGE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "mineral_trader_fundin_q0013_0104.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == FUNDIN) {
			if (cond == 0) {
				if (pc.level() >= 74) return "mineral_trader_fundin_q0013_0101.htm";
				qs.exitQuest(true);
				return "mineral_trader_fundin_q0013_0103.htm";
			} else if (cond == 1) {
				return "mineral_trader_fundin_q0013_0105.htm";
			}
		} else if (npcId == VULCAN) {
			if (cond == 1 && qs.hasQuestItems(PACKAGE)) {
				qs.takeItems(PACKAGE, -1);
				qs.giveItems(57, 82656);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "blacksmith_vulcan_q0013_0101.htm";
			}
		}
		return "noquest";
	}
}
