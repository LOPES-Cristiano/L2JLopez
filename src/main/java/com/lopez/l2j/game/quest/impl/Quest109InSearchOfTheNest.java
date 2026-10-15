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
 * Quest 109: In Search of the Nest
 */
@Component
public class Quest109InSearchOfTheNest extends Quest {

	public static final int QUEST_ID = 109;
	public static final String QUEST_NAME = "109_InSearchOfTheNest";
	public static final int START_NPC = 31553;

	public static final int PIERCE = 31553;
	public static final int KAHMAN = 31554;
	public static final int SCOUT_CORPSE = 32015;

	public static final int SCOUT_MEMO = 8083;
	public static final int RECRUIT_BADGE = 7246;
	public static final int SOLDIER_BADGE = 7247;

	@Autowired
	public Quest109InSearchOfTheNest(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In Search of the Nest");
		addStartNpc(PIERCE);
		addStartNpc(KAHMAN);
		addTalkId(PIERCE, SCOUT_CORPSE, KAHMAN);
		registerQuestItems(SCOUT_MEMO);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		if ("quest_accept".equalsIgnoreCase(event)
				|| "31553-01.htm".equalsIgnoreCase(event)
				|| "merc_cap_peace_q0109_0105.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "merc_kahmun_q0109_0104.htm";
		} else if ("Memo".equalsIgnoreCase(event)
				|| "32015-01.htm".equalsIgnoreCase(event)
				|| "32015-02.htm".equalsIgnoreCase(event)) {
			if (!qs.hasQuestItems(SCOUT_MEMO)) {
				qs.giveItems(SCOUT_MEMO, 1);
			}
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event.endsWith(".htm") ? event : "corpse_of_scout_q0109_0101.htm";
		} else if ("31553-02.htm".equalsIgnoreCase(event)
				|| "31553-03.htm".equalsIgnoreCase(event)
				|| "merc_cap_peace_q0109_0301.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SCOUT_MEMO, -1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("exit".equalsIgnoreCase(event)
				|| "31554-02.htm".equalsIgnoreCase(event)
				|| "merc_kahmun_q0109_0401.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SCOUT_MEMO, -1);
			qs.giveItems(57, 16180);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event.endsWith(".htm") ? event : "merc_kahmun_q0109_0401.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		if (qs.isCompleted()) {
			return "completed";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == KAHMAN) {
			if (cond == 0) {
				if (pc.level() >= 66) {
					return "merc_kahmun_q0109_0101.htm";
				}
				qs.exitQuest(true);
				return "merc_kahmun_q0109_0103.htm";
			} else if (cond == 1) {
				return "merc_kahmun_q0109_0105.htm";
			} else if (cond == 2 || cond == 3) {
				qs.takeItems(SCOUT_MEMO, -1);
				qs.giveItems(57, 16180);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return cond == 2 ? "merc_kahmun_q0109_0202.htm" : "merc_kahmun_q0109_0401.htm";
			}
		} else if (npcId == PIERCE) {
			if (cond == 0) {
				if (pc.level() >= 66) {
					return "31553-00.htm";
				}
				qs.exitQuest(true);
				return "merc_cap_peace_q0109_0103.htm";
			} else if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "scout_pierce_q0109_0101.htm";
			} else if (cond == 2) {
				if (qs.hasQuestItems(SCOUT_MEMO)) {
					qs.takeItems(SCOUT_MEMO, -1);
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
				return "merc_cap_peace_q0109_0201.htm";
			} else if (cond == 3) {
				return "merc_cap_peace_q0109_0303.htm";
			}
		} else if (npcId == SCOUT_CORPSE) {
			if (cond == 1) {
				if (!qs.hasQuestItems(SCOUT_MEMO)) {
					qs.giveItems(SCOUT_MEMO, 1);
				}
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "corpse_of_scout_q0109_0101.htm";
			} else if (cond >= 2) {
				return "corpse_of_scout_q0109_0203.htm";
			}
		}
		return "noquest";
	}
}
