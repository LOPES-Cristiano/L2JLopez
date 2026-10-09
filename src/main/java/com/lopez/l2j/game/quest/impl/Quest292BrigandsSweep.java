package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.util.Rnd;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 292 - 292_BrigandsSweep
 */
@Component
public class Quest292BrigandsSweep extends Quest {

	public static final int aQP = 30532;
	public static final int aQQ = 30533;
	public static final int bex = 20322;
	public static final int bey = 20323;
	public static final int bez = 20324;
	public static final int beA = 20327;
	public static final int beB = 20528;
	public static final int beC = 1483;
	public static final int beD = 1484;
	public static final int beE = 1485;
	public static final int beF = 1486;
	public static final int beG = 1487;
	public static final int beH = 10;
	public static final int[][] DROPLIST_COND = new int[][]{{1, 0, bex, 0, beC, 0, 40, 1}, {1, 0, bey, 0, beC, 0, 40, 1}, {1, 0, beA, 0, beC, 0, 40, 1}, {1, 0, bez, 0, beD, 0, 40, 1}, {1, 0, beB, 0, beE, 0, 40, 1}};

	public Quest292BrigandsSweep(QuestManager questManager) {
		super(292, "292_BrigandsSweep", "292_BrigandsSweep");
		addStartNpc(aQP);
		addTalkNpc(aQQ);
		for (int[] cond : DROPLIST_COND) {
			addKillId(cond[2]);
		}
		registerQuestItems(beF, beG, beC, beD, beE);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		String html = event;

        if (event.equalsIgnoreCase("elder_spiron_q0292_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("elder_spiron_q0292_06.htm")) {
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getCond();
        if (n == aQP) {
            if (n2 == 0) {
                if (pc.getRace() != Race.dwarf) {
                    html = "elder_spiron_q0292_00.htm";
                    qs.exitQuest(true);
                } else if (pc.getLevel() < 5) {
                    html = "elder_spiron_q0292_01.htm";
                    qs.exitQuest(true);
                } else {
                    html = "elder_spiron_q0292_02.htm";
                }
            } else if (n2 == 1) {
                long l = qs.getQuestItemsCount(beC) * 12L + qs.getQuestItemsCount(beD) * 36L + qs.getQuestItemsCount(beE) * 33L + qs.getQuestItemsCount(beG) * 100L;
                if (l == 0L) {
                    return "elder_spiron_q0292_04.htm";
                }
                html = qs.getQuestItemsCount(beG) != 0L ? "elder_spiron_q0292_10.htm" : (qs.getQuestItemsCount(beF) == 0L ? "elder_spiron_q0292_05.htm" : (qs.getQuestItemsCount(beF) == 1L ? "elder_spiron_q0292_08.htm" : "elder_spiron_q0292_09.htm"));
                qs.takeItems(beC, -1L);
                qs.takeItems(beD, -1L);
                qs.takeItems(beE, -1L);
                qs.takeItems(beG, -1L);
                qs.giveItems(57, l);
            }
        } else if (n == aQQ && n2 == 1) {
            if (qs.getQuestItemsCount(beG) == 0L) {
                html = "balanki_q0292_01.htm";
            } else {
                qs.takeItems(beG, -1L);
                qs.giveItems(57, 120L);
                html = "balanki_q0292_02.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getCond();
        for (int i = 0; i < DROPLIST_COND.length; ++i) {
            if (n2 != DROPLIST_COND[i][0] || n != DROPLIST_COND[i][2] || DROPLIST_COND[i][3] != 0 && qs.getQuestItemsCount(DROPLIST_COND[i][3]) <= 0L) continue;
            if (DROPLIST_COND[i][5] == 0) {
                qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][6]);
                continue;
            }
            if (!qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][7], DROPLIST_COND[i][5], DROPLIST_COND[i][6]) || DROPLIST_COND[i][1] == n2 || DROPLIST_COND[i][1] == 0) continue;
            qs.setCond(DROPLIST_COND[i][1]);
            qs.setState(State.STARTED);
        }
        if (qs.getQuestItemsCount(beG) == 0L && Rnd.chance(beH)) {
            if (qs.getQuestItemsCount(beF) < 3L) {
                qs.giveItems(beF, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else {
                qs.takeItems(beF, -1L);
                qs.giveItems(beG, 1L);
                qs.playSound("QuestState.SOUND_MIDDLE");
            }
        }
        return null;
    
	}

}
