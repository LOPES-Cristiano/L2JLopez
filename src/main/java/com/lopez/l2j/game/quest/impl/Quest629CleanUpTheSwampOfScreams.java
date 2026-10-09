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
 * Quest 629: Clean up the Swamp of Screams
 * Mercenary Captain Pierce no Swamp of Screams.
 * Coleta de Stakato Claws para trocar por Golden Ram Coins.
 */
@Component
public class Quest629CleanUpTheSwampOfScreams extends Quest {

	public static final int QUEST_ID = 629;
	public static final String QUEST_NAME = "629_CleanUpTheSwampOfScreams";

	public static final int PIERCE = 31553;
	public static final int STAKATO_CLAW = 7250;
	public static final int GOLDEN_RAM_COIN = 7251;

	public static final int[] MOBS = {
			21508, 21509, 21510, 21511, 21512, 21513, 21514, 21515, 21516, 21517
	};

	@Autowired
	public Quest629CleanUpTheSwampOfScreams(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Clean up the Swamp of Screams");
		addStartNpc(PIERCE);
		addTalkId(PIERCE);
		addKillId(MOBS);
		registerQuestItems(STAKATO_CLAW);
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
			return "merc_cap_peace_q0629_0104.htm";
		} else if ("reply_3".equalsIgnoreCase(event)) {
			long claws = qs.getQuestItemsCount(STAKATO_CLAW);
			if (claws >= 100) {
				qs.takeItems(STAKATO_CLAW, 100);
				qs.giveItems(GOLDEN_RAM_COIN, 20);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "merc_cap_peace_q0629_0202.htm";
			}
			return "merc_cap_peace_q0629_0203.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.getState() == State.CREATED) {
			if (pc.level() >= 66) return "merc_cap_peace_q0629_0101.htm";
			qs.exitQuest(true);
			return "merc_cap_peace_q0629_0103.htm";
		} else {
			if (qs.getQuestItemsCount(STAKATO_CLAW) >= 100) {
				return "merc_cap_peace_q0629_0105.htm";
			}
			return "merc_cap_peace_q0629_0106.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		qs.giveItems(STAKATO_CLAW, 1);
		qs.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}
