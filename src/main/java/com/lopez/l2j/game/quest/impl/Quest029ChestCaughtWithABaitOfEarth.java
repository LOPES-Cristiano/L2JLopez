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
 * Quest 29: Chest Caught with a Bait of Earth
 */
@Component
public class Quest029ChestCaughtWithABaitOfEarth extends Quest {

	public static final int QUEST_ID = 29;
	public static final String QUEST_NAME = "029_ChestCaughtwithaBaitofEarth";

	public static final int START_NPC = 31574;
	public static final int WILLIE = 31574;
	public static final int ANABEL = 30909;

	public static final int SMALL_PURPLE_TREASURE_CHEST = 6507;
	public static final int SMALL_GLASS_BOX = 7627;
	public static final int PLATED_LEATHER_GLOVES = 2455;

	@Autowired
	public Quest029ChestCaughtWithABaitOfEarth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Chest Caught with a Bait of Earth");
		addStartNpc(WILLIE);
		addTalkId(WILLIE, ANABEL);
		registerQuestItems(SMALL_GLASS_BOX);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "fisher_willeri_q0029_0104.htm".equalsIgnoreCase(event) || "31574-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "fisher_willeri_q0029_0104.htm";
		} else if ("fisher_willeri_q0029_0201.htm".equalsIgnoreCase(event) || "31574-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SMALL_PURPLE_TREASURE_CHEST) || qs.hasQuestItems(6502)) {
				qs.takeItems(SMALL_PURPLE_TREASURE_CHEST, 1);
				qs.takeItems(6502, -1);
				qs.giveItems(SMALL_GLASS_BOX, 1);
				qs.giveItems(7611, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "fisher_willeri_q0029_0202.htm";
		} else if ("29_GiveGlassBox".equalsIgnoreCase(event) || "magister_anabel_q0029_0301.htm".equalsIgnoreCase(event) || "30909-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SMALL_GLASS_BOX) || qs.hasQuestItems(7611)) {
				qs.takeItems(SMALL_GLASS_BOX, -1);
				qs.takeItems(7611, -1);
				qs.giveItems(PLATED_LEATHER_GLOVES, 1);
				qs.giveItems(223, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return event.endsWith(".htm") ? event : "magister_anabel_q0029_0301.htm";
			}
			return "magister_anabel_q0029_0302.htm";
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

		if (npcId == WILLIE) {
			if (cond == 0) {
				if (pc.level() >= 48) return "fisher_willeri_q0029_0101.htm";
				qs.exitQuest(true);
				return "fisher_willeri_q0029_0102.htm";
			} else if (cond == 1) {
				if (qs.hasQuestItems(SMALL_PURPLE_TREASURE_CHEST) || qs.hasQuestItems(6502)) {
					qs.takeItems(SMALL_PURPLE_TREASURE_CHEST, 1);
					qs.takeItems(6502, -1);
					qs.giveItems(SMALL_GLASS_BOX, 1);
					qs.giveItems(7611, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "fisher_willeri_q0029_0201.htm";
				}
				return "fisher_willeri_q0029_0106.htm";
			} else if (cond == 2) {
				return "fisher_willeri_q0029_0203.htm";
			}
		} else if (npcId == ANABEL) {
			if (cond == 2 && (qs.hasQuestItems(SMALL_GLASS_BOX) || qs.hasQuestItems(7611))) {
				qs.takeItems(SMALL_GLASS_BOX, -1);
				qs.takeItems(7611, -1);
				qs.giveItems(PLATED_LEATHER_GLOVES, 1);
				qs.giveItems(223, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "magister_anabel_q0029_0301.htm";
			}
		}
		return "noquest";
	}
}
