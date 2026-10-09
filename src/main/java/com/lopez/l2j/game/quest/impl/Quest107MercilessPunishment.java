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
 * Quest 107: Merciless Punishment
 */
@Component
public class Quest107MercilessPunishment extends Quest {

	public static final int QUEST_ID = 107;
	public static final String QUEST_NAME = "107_MercilessPunishment";
	public static final int START_NPC = 30568;

	@Autowired
	public Quest107MercilessPunishment(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Merciless Punishment");
		addStartNpc(START_NPC);
		addTalkId(30568);
		addTalkId(30580);
		registerQuestItems(758);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "urutu_chief_hatos_q0107_03.htm";
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
				if (pc.level() >= 10 && pc.race() == 3) return "urutu_chief_hatos_q0107_02.htm";
				qs.exitQuest(true);
				return "urutu_chief_hatos_q0107_01.htm";
			} else if (cond == 1) {
				return "urutu_chief_hatos_q0107_06.htm";
			} else if (cond == 2) {
				qs.giveItems(758, 1); // Butcher's Sword
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "urutu_chief_hatos_q0107_09.htm";
			}
		} else if (npcId == 30580 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "centurion_parugon_q0107_01.htm";
		}
		return "noquest";
	}
}
