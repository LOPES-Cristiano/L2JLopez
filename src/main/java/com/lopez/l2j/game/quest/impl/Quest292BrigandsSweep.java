package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.util.Rnd;
import org.springframework.stereotype.Component;

/**
 * Quest 292 - 292_BrigandsSweep
 */
@Component
public class Quest292BrigandsSweep extends Quest {

	// NPCs
	public static final int SPIRON = 30532;
	public static final int BALANKI = 30533;

	// Monsters
	public static final int GOBLIN_BRIGAND = 20322;
	public static final int GOBLIN_BRIGAND_LEADER = 20323;
	public static final int GOBLIN_BRIGAND_LIEUTENANT = 20324;
	public static final int GOBLIN_SNOOPER = 20327;
	public static final int GOBLIN_LORD = 20528;

	// Items
	public static final int GOBLIN_NECKLACE = 1483;
	public static final int GOBLIN_PENDANT = 1484;
	public static final int GOBLIN_LORD_PENDANT = 1485;
	public static final int SUSPICIOUS_MEMO = 1486;
	public static final int SUSPICIOUS_CONTRACT = 1487;
	public static final int ADENA = 57;

	public static final int[][] DROPLIST_COND = new int[][]{
			{1, 0, GOBLIN_BRIGAND, 0, GOBLIN_NECKLACE, 0, 40, 1},
			{1, 0, GOBLIN_BRIGAND_LEADER, 0, GOBLIN_NECKLACE, 0, 40, 1},
			{1, 0, GOBLIN_SNOOPER, 0, GOBLIN_NECKLACE, 0, 40, 1},
			{1, 0, GOBLIN_BRIGAND_LIEUTENANT, 0, GOBLIN_PENDANT, 0, 40, 1},
			{1, 0, GOBLIN_LORD, 0, GOBLIN_LORD_PENDANT, 0, 40, 1}
	};

	public Quest292BrigandsSweep(QuestManager questManager) {
		super(292, "292_BrigandsSweep", "292_BrigandsSweep");

		addStartNpc(SPIRON);
		addTalkId(SPIRON, BALANKI);

		addKillId(GOBLIN_BRIGAND, GOBLIN_BRIGAND_LEADER, GOBLIN_BRIGAND_LIEUTENANT, GOBLIN_SNOOPER, GOBLIN_LORD);

		registerQuestItems(GOBLIN_NECKLACE, GOBLIN_PENDANT, GOBLIN_LORD_PENDANT, SUSPICIOUS_MEMO, SUSPICIOUS_CONTRACT);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)
				|| "elder_spiron_q0292_03.htm".equalsIgnoreCase(event) || "30532-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30532-03.htm".equalsIgnoreCase(event) ? "30532-03.htm" : "elder_spiron_q0292_03.htm";
		} else if ("elder_spiron_q0292_06.htm".equalsIgnoreCase(event) || "30532-06.htm".equalsIgnoreCase(event)) {
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
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

		int npcId = npc != null ? npc.getNpcId() : 0;
		String html = "noquest";
		int cond = qs.getCond();

		if (npcId == SPIRON) {
			if (cond == 0) {
				if (pc.getRace() != Race.dwarf) {
					html = "elder_spiron_q0292_00.htm";
					qs.exitQuest(true);
				} else if (pc.getLevel() < 5) {
					html = "elder_spiron_q0292_01.htm";
					qs.exitQuest(true);
				} else {
					html = "elder_spiron_q0292_02.htm";
				}
			} else if (cond >= 1) {
				long reward = qs.getQuestItemsCount(GOBLIN_NECKLACE) * 12L
						+ qs.getQuestItemsCount(GOBLIN_PENDANT) * 36L
						+ qs.getQuestItemsCount(GOBLIN_LORD_PENDANT) * 33L
						+ qs.getQuestItemsCount(SUSPICIOUS_CONTRACT) * 100L;
				if (reward == 0L) {
					return "elder_spiron_q0292_04.htm";
				}
				html = qs.getQuestItemsCount(SUSPICIOUS_CONTRACT) != 0L ? "elder_spiron_q0292_10.htm"
						: (qs.getQuestItemsCount(SUSPICIOUS_MEMO) == 0L ? "elder_spiron_q0292_05.htm"
						: (qs.getQuestItemsCount(SUSPICIOUS_MEMO) == 1L ? "elder_spiron_q0292_08.htm" : "elder_spiron_q0292_09.htm"));
				qs.takeItems(GOBLIN_NECKLACE, -1L);
				qs.takeItems(GOBLIN_PENDANT, -1L);
				qs.takeItems(GOBLIN_LORD_PENDANT, -1L);
				if (qs.getQuestItemsCount(SUSPICIOUS_CONTRACT) > 0) {
					qs.setCond(1);
					qs.takeItems(SUSPICIOUS_CONTRACT, -1L);
				}
				qs.giveItems(ADENA, reward);
			}
		} else if (npcId == BALANKI && cond >= 1) {
			if (qs.getQuestItemsCount(SUSPICIOUS_CONTRACT) == 0L) {
				html = "balanki_q0292_01.htm";
			} else {
				qs.setCond(1);
				qs.takeItems(SUSPICIOUS_CONTRACT, -1L);
				qs.giveItems(ADENA, 120L);
				html = "balanki_q0292_02.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return null;
		}

		int npcId = npc != null ? npc.getNpcId() : 0;
		int cond = qs.getCond();
		for (int i = 0; i < DROPLIST_COND.length; ++i) {
			if (cond != DROPLIST_COND[i][0] || npcId != DROPLIST_COND[i][2]
					|| (DROPLIST_COND[i][3] != 0 && qs.getQuestItemsCount(DROPLIST_COND[i][3]) <= 0L)) {
				continue;
			}
			if (DROPLIST_COND[i][5] == 0) {
				qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][6]);
				continue;
			}
			if (!qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][7], DROPLIST_COND[i][5], DROPLIST_COND[i][6])
					|| DROPLIST_COND[i][1] == cond || DROPLIST_COND[i][1] == 0) {
				continue;
			}
			qs.setCond(DROPLIST_COND[i][1]);
			qs.setState(State.STARTED);
		}
		if (qs.getQuestItemsCount(SUSPICIOUS_CONTRACT) == 0L && Rnd.chance(10)) {
			if (qs.getQuestItemsCount(SUSPICIOUS_MEMO) < 3L) {
				qs.giveItems(SUSPICIOUS_MEMO, 1L);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else {
				qs.takeItems(SUSPICIOUS_MEMO, -1L);
				qs.giveItems(SUSPICIOUS_CONTRACT, 1L);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
