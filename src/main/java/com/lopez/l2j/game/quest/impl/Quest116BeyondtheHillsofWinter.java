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
 * Quest 116: Beyond the Hills of Winter
 */
@Component
public class Quest116BeyondtheHillsofWinter extends Quest {

	public static final int QUEST_ID = 116;
	public static final String QUEST_NAME = "116_BeyondtheHillsofWinter";
	public static final int START_NPC = 30535;

	public static final int FILAUR = 30535;
	public static final int OBI = 32052;
	public static final int RAFFORTY = 32020;

	public static final int BANDAGE = 1833;
	public static final int ENERGY_STONE = 5589;
	public static final int THIEF_KEY = 1661;
	public static final int GOODS = 8098;
	public static final int SSD = 1463;

	@Autowired
	public Quest116BeyondtheHillsofWinter(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Beyond the Hills of Winter");
		addStartNpc(START_NPC);
		addTalkId(30535, 32052, 32020);
		registerQuestItems(8098);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30535-02.htm".equalsIgnoreCase(event) || "elder_filaur_q0116_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(GOODS, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30535-02.htm".equalsIgnoreCase(event) ? "30535-02.htm" : "elder_filaur_q0116_0104.htm";
		} else if ("30535-05.htm".equalsIgnoreCase(event) || "elder_filaur_q0116_0201.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(BANDAGE) >= 20 && qs.getQuestItemsCount(ENERGY_STONE) >= 5 && qs.getQuestItemsCount(THIEF_KEY) >= 10) {
				qs.takeItems(BANDAGE, 20);
				qs.takeItems(ENERGY_STONE, 5);
				qs.takeItems(THIEF_KEY, 10);
			}
			if (!qs.hasQuestItems(GOODS)) {
				qs.giveItems(GOODS, 1);
			}
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32052-01.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32052-01.htm";
		} else if ("materials".equalsIgnoreCase(event)) {
			qs.takeItems(GOODS, -1);
			qs.giveItems(SSD, 1650);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "32052-02.htm";
		} else if ("adena".equalsIgnoreCase(event)) {
			qs.takeItems(GOODS, -1);
			qs.giveItems(57, 16500);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "32052-02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == FILAUR) {
			if (cond == 0) {
				if (pc.level() >= 30) return "elder_filaur_q0116_0101.htm";
				qs.exitQuest(true);
				return "elder_filaur_q0116_0103.htm";
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(BANDAGE) >= 20 && qs.getQuestItemsCount(ENERGY_STONE) >= 5 && qs.getQuestItemsCount(THIEF_KEY) >= 10) {
					qs.takeItems(BANDAGE, 20);
					qs.takeItems(ENERGY_STONE, 5);
					qs.takeItems(THIEF_KEY, 10);
					return "30535-03.htm";
				}
				return "elder_filaur_q0116_0105.htm";
			} else if (cond == 2 || cond == 3) {
				return "30535-05.htm";
			}
		} else if (npcId == OBI) {
			if (cond == 1 || cond == 2) {
				return "railman_obi_q0116_0201.htm";
			} else if (cond == 3) {
				return "32052-01.htm";
			}
		} else if (npcId == RAFFORTY && cond >= 1 && qs.hasQuestItems(GOODS)) {
			qs.takeItems(GOODS, -1);
			qs.giveItems(57, 16500);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "rafferty_q0116_0201.htm";
		}
		return "noquest";
	}
}
