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
 * Quest 36: Make a Sewing Kit
 * Head Blacksmith Ferris e Enchanted Iron Golem.
 */
@Component
public class Quest036MakeASewingKit extends Quest {

	public static final int QUEST_ID = 36;
	public static final String QUEST_NAME = "036_MakeaSewingKit";

	public static final int START_NPC = 30847;
	public static final int FERRIS = 30847;
	public static final int ENCHANTED_IRON_GOLEM = 20566;

	public static final int REINFORCED_STEEL = 7163;
	public static final int ARTISANS_FRAME = 1891;
	public static final int ORIHARUKON = 1893;
	public static final int SEWING_KIT = 7078;

	@Autowired
	public Quest036MakeASewingKit(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Make a Sewing Kit");
		addStartNpc(FERRIS);
		addTalkId(FERRIS);
		addKillId(ENCHANTED_IRON_GOLEM);
		registerQuestItems(REINFORCED_STEEL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "30847-1.htm".equalsIgnoreCase(event) || "head_blacksmith_ferris_q0036_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "head_blacksmith_ferris_q0036_0104.htm";
		} else if ("30847-3.htm".equalsIgnoreCase(event) || "head_blacksmith_ferris_q0036_0201.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.takeItems(REINFORCED_STEEL, 5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30847-5.htm".equalsIgnoreCase(event) || "head_blacksmith_ferris_q0036_0301.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(ARTISANS_FRAME) >= 10 && qs.getQuestItemsCount(ORIHARUKON) >= 10) {
				qs.takeItems(ARTISANS_FRAME, 10);
				qs.takeItems(ORIHARUKON, 10);
				qs.giveItems(SEWING_KIT, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event;
			}
			return "head_blacksmith_ferris_q0036_0302.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == FERRIS) {
			if (cond == 0) {
				if (pc.level() >= 60) return "head_blacksmith_ferris_q0036_0101.htm";
				qs.exitQuest(true);
				return "head_blacksmith_ferris_q0036_0103.htm";
			} else if (cond == 1) {
				return "head_blacksmith_ferris_q0036_0106.htm";
			} else if (cond == 2 && qs.getQuestItemsCount(REINFORCED_STEEL) >= 5) {
				return "head_blacksmith_ferris_q0036_0105.htm";
			} else if (cond == 3) {
				if (qs.getQuestItemsCount(ARTISANS_FRAME) >= 10 && qs.getQuestItemsCount(ORIHARUKON) >= 10) {
					return "head_blacksmith_ferris_q0036_0203.htm";
				}
				return "head_blacksmith_ferris_q0036_0204.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == ENCHANTED_IRON_GOLEM && qs.getCond() == 1) {
			long count = qs.getQuestItemsCount(REINFORCED_STEEL);
			if (count < 5) {
				qs.giveItems(REINFORCED_STEEL, 1);
				if (count + 1 >= 5) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
