package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 117: Ocean of Distant Star
 */
@Component
public class Quest117OceanOfDistantStar extends Quest {

	public static final int QUEST_ID = 117;
	public static final String QUEST_NAME = "117_OceanOfDistantStar";
	public static final int START_NPC = 32053;

	// NPCs
	public static final int ABEY = 32053;
	public static final int GHOST = 32054;
	public static final int ANCIENT_GHOST = 32055;
	public static final int OBI = 32052;
	public static final int BOX = 32076;
	public static final int GALLADUCCI = 30858;

	// Items
	public static final int ENGRAVED_HAMMER = 8488;
	public static final int GREY_STAR = 8489;
	public static final int GREY_STAR_ALT = 8495;

	// Monsters
	public static final int BANDIT_WARRIOR = 22023;
	public static final int BANDIT_INSPECTOR = 22024;

	@Autowired
	public Quest117OceanOfDistantStar(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Ocean of Distant Star");
		addStartNpc(32053);
		addStartNpc(32069);
		addTalkId(32053, 32055, 32052, 30858, 32054, 32076, 32069, 32070);
		addKillId(22023, 22024);
		registerQuestItems(8488, 8495);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "32053-02.htm".equalsIgnoreCase(event) || "railman_abu_q0117_0104.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) ? "railman_obi_q0117_0104.htm" : event;
		} else if ("32055-02.htm".equalsIgnoreCase(event) || "ghost_of_railroadman2_q0117_0201.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32052-02.htm".equalsIgnoreCase(event) || "railman_obi_q0117_0301.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32053-04.htm".equalsIgnoreCase(event) || "32053-06.htm".equalsIgnoreCase(event) && qs.getCond() == 3 || "railman_abu_q0117_0401.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32076-02.htm".equalsIgnoreCase(event) || "q_box_of_railroad_q0117_0501.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.giveItems(ENGRAVED_HAMMER, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32053-06.htm".equalsIgnoreCase(event) || "railman_abu_q0117_0601.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32052-04.htm".equalsIgnoreCase(event) || "railman_obi_q0117_0701.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32052-06.htm".equalsIgnoreCase(event) || "railman_obi_q0117_0801.htm".equalsIgnoreCase(event)) {
			qs.takeItems(GREY_STAR, -1);
			qs.takeItems(GREY_STAR_ALT, -1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32055-04.htm".equalsIgnoreCase(event) || "32055-05.htm".equalsIgnoreCase(event) || "ghost_of_railroadman2_q0117_0901.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ENGRAVED_HAMMER, -1);
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32054-03.htm".equalsIgnoreCase(event) || "ghost_of_railroadman_q0117_1002.htm".equalsIgnoreCase(event)) {
			qs.addExpAndSp(63591, 0);
			qs.giveItems(57, 17647);
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

		if (npcId == ABEY || npcId == 32069) {
			if (cond == 0) {
				if (pc.level() >= 39) return "railman_obi_q0117_0101.htm";
				qs.exitQuest(true);
				return "railman_obi_q0117_0103.htm";
			} else if (cond == 1) {
				return "railman_obi_q0117_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 17647);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "railman_obi_q0117_0202.htm";
			} else if (cond == 3) {
				return "32053-03.htm";
			} else if (cond == 4) {
				return "32053-04.htm";
			} else if (cond == 5) {
				return "32053-05.htm";
			} else if (cond >= 6) {
				return "32053-06.htm";
			}
		} else if (npcId == 32070 && cond == 1) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "ghost_of_adventurer_q0117_0201.htm";
		} else if (npcId == ANCIENT_GHOST) {
			if (cond == 1) return "32055-01.htm";
			if (cond >= 2 && cond <= 8) return "32055-02.htm";
			if (cond == 9) return "32055-03.htm";
			if (cond >= 10) return "32055-05.htm";
		} else if (npcId == OBI) {
			if (cond == 2) return "32052-01.htm";
			if (cond >= 3 && cond <= 5) return "32052-02.htm";
			if (cond == 6) return "32052-03.htm";
			if (cond == 7) return "32052-04.htm";
			if (cond == 8) return "32052-05.htm";
			if (cond >= 9) return "32052-06.htm";
		} else if (npcId == BOX) {
			if (cond == 4) return "32076-01.htm";
			if (cond >= 5) return "32076-03.htm";
		} else if (npcId == GHOST || npcId == GALLADUCCI) {
			if (cond == 10) return "32054-01.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs.getCond() == 7 && ThreadLocalRandom.current().nextInt(100) < 20) {
			if (!qs.hasQuestItems(GREY_STAR)) {
				qs.giveItems(GREY_STAR, 1);
			}
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
