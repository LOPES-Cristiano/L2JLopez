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
 * Quest 102: Sea of Spores Fever
 */
@Component
public class Quest102SeaofSporesFever extends Quest {

	public static final int QUEST_ID = 102;
	public static final String QUEST_NAME = "102_SeaofSporesFever";
	public static final int START_NPC = 30284;

	@Autowired
	public Quest102SeaofSporesFever(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Sea of Spores Fever");
		addStartNpc(START_NPC);
		addTalkId(30284);
		addTalkId(30156);
		registerQuestItems(964, 966, 743);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(964, 1); // Alberryus' Letter
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "alberryus_q0102_02.htm";
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
				if (pc.level() >= 12 && pc.race() == 1) return "alberryus_q0102_07.htm";
				qs.exitQuest(true);
				return "alberryus_q0102_00.htm";
			} else if (cond == 1) {
				return "alberryus_q0102_03.htm";
			} else if (cond == 4 && qs.hasQuestItems(966)) {
				qs.takeItems(966, -1);
				qs.giveItems(743, 1); // Sword of Sentinel
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "alberryus_q0102_05.htm";
			}
		} else if (npcId == 30156 && cond == 1 && qs.hasQuestItems(964)) {
			qs.takeItems(964, -1);
			qs.giveItems(966, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "cobweb_q0102_01.htm";
		}
		return "noquest";
	}
}
