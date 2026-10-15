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
 * Quest 121: Pavel The Giants
 */
@Component
public class Quest121PavelTheGiants extends Quest {

	public static final int QUEST_ID = 121;
	public static final String QUEST_NAME = "121_PavelTheGiants";
	public static final int START_NPC = 31961;

	public static final int NEWYEAR = 31961;
	public static final int YUMI = 32041;
	public static final int STONES = 32046;

	@Autowired
	public Quest121PavelTheGiants(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Pavel The Giants");
		addStartNpc(31961);
		addStartNpc(32046);
		addTalkId(31961, 32041, 32046);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31961-02.htm".equalsIgnoreCase(event) || "31961-2.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) ? "newheart_q0121_0104.htm" : event;
		} else if ("32041-02.htm".equalsIgnoreCase(event) || "32041-2.htm".equalsIgnoreCase(event) || "collecter_yumi_q0121_0201.htm".equalsIgnoreCase(event)) {
			qs.addExpAndSp(10000, 0);
			qs.giveItems(57, 34000);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == NEWYEAR || npcId == STONES) {
			if (cond == 0) {
				if (pc.level() >= 46) return "newheart_q0121_0101.htm";
				qs.exitQuest(true);
				return "newheart_q0121_0103.htm";
			} else if (cond == 1) {
				return "newheart_q0121_0105.htm";
			}
		} else if (npcId == YUMI && cond == 1) {
			qs.addExpAndSp(10000, 0);
			qs.giveItems(57, 34000);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "collecter_yumi_q0121_0201.htm";
		}
		return "noquest";
	}
}
