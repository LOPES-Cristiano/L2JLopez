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
 * Quest 103: Spirit of Craftsman
 */
@Component
public class Quest103SpiritOfCraftsman extends Quest {

	public static final int QUEST_ID = 103;
	public static final String QUEST_NAME = "103_SpiritOfCraftsman";
	public static final int START_NPC = 30307;

	@Autowired
	public Quest103SpiritOfCraftsman(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Spirit of Craftsman");
		addStartNpc(START_NPC);
		addTalkId(30307);
		addTalkId(30132);
		addTalkId(30144);
		registerQuestItems(968, 747);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(968, 1); // Karrod's Letter
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "blacksmith_karoyd_q0103_05.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == START_NPC) {
			if (cond == 0) {
				if (pc.level() >= 10 && pc.race() == 2) return "blacksmith_karoyd_q0103_03.htm";
				qs.exitQuest(true);
				return "blacksmith_karoyd_q0103_00.htm";
			} else if (cond == 1) {
				return "blacksmith_karoyd_q0103_06.htm";
			} else if (cond == 3) {
				qs.giveItems(747, 1); // Blood Saber
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "blacksmith_karoyd_q0103_07.htm";
			}
		} else if (npcId == 30132 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "cecktinon_q0103_01.htm";
		} else if (npcId == 30144 && cond == 2) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "harne_q0103_01.htm";
		}
		return "noquest";
	}
}
