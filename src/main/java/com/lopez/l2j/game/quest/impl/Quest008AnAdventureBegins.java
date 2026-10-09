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
 * Quest 008: An Adventure Begins
 * Gatekeeper Jasmine, Roselyn e Harne na Dark Elven Village.
 */
@Component
public class Quest008AnAdventureBegins extends Quest {

	public static final int QUEST_ID = 8;
	public static final String QUEST_NAME = "008_AnAdventureBegins";

	public static final int JASMINE = 30134;
	public static final int ROSELYN = 30355;
	public static final int HARNE = 30144;

	public static final int ROSELYNS_NOTE = 7573;
	public static final int SCROLL_OF_ESCAPE_GIRAN = 7126;
	public static final int MARK_OF_TRAVELER = 7570;

	@Autowired
	public Quest008AnAdventureBegins(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "An Adventure Begins");
		addStartNpc(JASMINE);
		addTalkId(JASMINE, ROSELYN, HARNE);
		registerQuestItems(ROSELYNS_NOTE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(ROSELYNS_NOTE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "sentry_jasmine_q0008_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == JASMINE) {
			if (cond == 0) {
				if (pc.level() >= 3 && pc.race() == 2) return "sentry_jasmine_q0008_02.htm";
				qs.exitQuest(true);
				return "sentry_jasmine_q0008_01.htm";
			} else if (cond == 1) {
				return "sentry_jasmine_q0008_04.htm";
			} else if (cond == 2) {
				return "sentry_jasmine_q0008_05.htm";
			} else if (cond == 3) {
				qs.giveItems(SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.giveItems(MARK_OF_TRAVELER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "sentry_jasmine_q0008_06.htm";
			}
		} else if (npcId == ROSELYN) {
			if (cond == 1 && qs.hasQuestItems(ROSELYNS_NOTE)) {
				qs.takeItems(ROSELYNS_NOTE, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "sentry_roseline_q0008_01.htm";
			} else if (cond == 2) {
				return "sentry_roseline_q0008_02.htm";
			}
		} else if (npcId == HARNE) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "harne_q0008_01.htm";
			} else if (cond == 3) {
				return "harne_q0008_02.htm";
			}
		}
		return "noquest";
	}
}
