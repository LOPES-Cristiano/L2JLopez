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
 * Quest 119: Last Imperial Prince
 */
@Component
public class Quest119LastImperialPrince extends Quest {

	public static final int QUEST_ID = 119;
	public static final String QUEST_NAME = "119_LastImperialPrince";
	public static final int START_NPC = 31453;
	public static final int ANTIQUE_BROOCH = 7262;
	public static final int FRINTEZZA_SCROLL = 8073;

	@Autowired
	public Quest119LastImperialPrince(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Last Imperial Prince");
		addStartNpc(START_NPC);
		addTalkId(31453);
		addTalkId(32009);
		
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31453-4.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "nameless_spirit_q0119_0104.htm";
		} else if ("32009-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "devorin_q0119_0201.htm";
		} else if ("31453-7.htm".equalsIgnoreCase(event)) {
			qs.giveItems(FRINTEZZA_SCROLL, 1);
			qs.giveItems(57, 68787);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "nameless_spirit_q0119_0202.htm";
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
				if (!qs.hasQuestItems(ANTIQUE_BROOCH)) {
					return "31453-0.htm (Four Goblets not accomplished)";
				}
				if (pc.level() >= 74) return "31453-1.htm";
				qs.exitQuest(true);
				return "nameless_spirit_q0119_0103.htm";
			} else if (cond == 1) {
				return "nameless_spirit_q0119_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(FRINTEZZA_SCROLL, 1);
				qs.giveItems(57, 68787);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "nameless_spirit_q0119_0202.htm";
			}
		} else if (npcId == 32009 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "devorin_q0119_0201.htm";
		}
		return "noquest";
	}
}
