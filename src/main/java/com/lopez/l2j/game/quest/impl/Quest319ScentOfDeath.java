package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
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
 * Quest 319 - 319_ScentOfDeath
 */
@Component
public class Quest319ScentOfDeath extends Quest {

	public static final int bfH = 30138;
	public static final int bdE = 1060;
	public static final int bfI = 1045;
	public static final int[][] DROPLIST_COND = new int[][]{{1, 2, 20015, 0, 1045, 5, 20, 1}, {1, 2, 20020, 0, 1045, 5, 25, 1}};

	public Quest319ScentOfDeath(QuestManager questManager) {
		super(319, "319_ScentOfDeath", "319_ScentOfDeath");
		addStartNpc(30138);
		for (int[] cond : DROPLIST_COND) {
			addKillId(cond[2]);
		}
		registerQuestItems(1045);
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

        String string2 = event;
        if (event.equalsIgnoreCase("mina_q0319_04.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getStateId();
        int n3 = 0;
        if (n2 != 1) {
            n3 = qs.getCond();
        }
        if (n == 30138) {
            if (n3 == 0) {
                if (pc.getLevel() < 11) {
                    html = "mina_q0319_02.htm";
                    qs.exitQuest(true);
                } else {
                    html = "mina_q0319_03.htm";
                }
            } else if (n3 == 1) {
                html = "mina_q0319_05.htm";
            } else if (n3 == 2 && qs.getQuestItemsCount(1045) >= 5L) {
                html = "mina_q0319_06.htm";
                qs.takeItems(1045, -1L);
                qs.giveItems(57, 3350L);
                qs.giveItems(1060, 1L);
                qs.playSound("QuestState.SOUND_FINISH");
                qs.exitQuest(true);
            } else {
                html = "mina_q0319_05.htm";
                qs.setCond(1);
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
        return null;
    
	}

}
