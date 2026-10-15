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
 * Quest 113: Status of the Beacon Tower
 */
@Component
public class Quest113StatusOfTheBeaconTower extends Quest {

	public static final int QUEST_ID = 113;
	public static final String QUEST_NAME = "113_StatusOfTheBeaconTower";
	public static final int START_NPC = 31979;

	public static final int MOIRA = 31979;
	public static final int TORRANT = 32016;
	public static final int BOX = 8086;

	@Autowired
	public Quest113StatusOfTheBeaconTower(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Status of the Beacon Tower");
		addStartNpc(START_NPC);
		addTalkId(MOIRA, TORRANT);
		registerQuestItems(BOX);
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
				|| "31979-02.htm".equalsIgnoreCase(event)
				|| "seer_moira_q0113_0104.htm".equalsIgnoreCase(event)
				|| "seer_moirase_q0113_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(BOX)) {
				qs.giveItems(BOX, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "seer_moira_q0113_0104.htm";
		} else if ("32016-02.htm".equalsIgnoreCase(event)
				|| "torrant_q0113_0201.htm".equalsIgnoreCase(event)
				|| "torant_q0113_0201.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BOX, -1);
			qs.giveItems(57, 21580);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
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

		if (npcId == MOIRA) {
			if (cond == 0) {
				if (pc.level() >= 40) {
					return "seer_moira_q0113_0101.htm";
				}
				qs.exitQuest(true);
				return "seer_moira_q0113_0103.htm";
			} else if (cond == 1) {
				return "seer_moira_q0113_0105.htm";
			}
		} else if (npcId == TORRANT && cond == 1) {
			qs.takeItems(BOX, -1);
			qs.giveItems(57, 21580);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "torrant_q0113_0201.htm";
		}
		return "noquest";
	}
}
