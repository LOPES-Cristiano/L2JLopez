package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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
 * Quest 649 - 649_ALooterandaRailroadMan
 */
@Component
public class Quest649ALooterandaRailroadMan extends Quest {

    public static final int OBI = 32052;
    public static final int bTT = 8099;
    public static final int[][] DROPLIST_COND = new int[][] { { 1, 2, 22017, 0, 8099, 200, 50, 1 },
            { 1, 2, 22018, 0, 8099, 200, 50, 1 }, { 1, 2, 22019, 0, 8099, 200, 50, 1 },
            { 1, 2, 22021, 0, 8099, 200, 50, 1 }, { 1, 2, 22022, 0, 8099, 200, 50, 1 },
            { 1, 2, 22023, 0, 8099, 200, 50, 1 }, { 1, 2, 22024, 0, 8099, 200, 50, 1 },
            { 1, 2, 22026, 0, 8099, 200, 50, 1 } };

    public Quest649ALooterandaRailroadMan(QuestManager questManager) {
        super(649, "649_ALooterandaRailroadMan", "649_ALooterandaRailroadMan");
        this.addStartNpc(32052);
        for (int i = 0; i < DROPLIST_COND.length; ++i) {
            this.addKillId(DROPLIST_COND[i][2]);
        }
        this.addQuestItem(8099);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            string2 = "railman_obi_q0649_0103.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("649_3")) {
            if (qs.getQuestItemsCount(8099) == 200L) {
                string2 = "railman_obi_q0649_0201.htm";
                qs.takeItems(8099, -1L);
                qs.giveItems(57, 21698L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                qs.setCond(1);
                string2 = "railman_obi_q0649_0202.htm";
            }
        }
        return string2;

    }

    @Override
    public String onTalk(NpcInstance npc, QuestState qs) {
        PlayerCharacter pc = qs.playerChar();
        if (pc == null)
            return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getStateId();
        int n3 = 0;
        if (n2 != 1) {
            n3 = qs.getCond();
        }
        if (n == 32052) {
            if (n3 == 0) {
                if (pc.getLevel() < 30) {
                    html = "railman_obi_q0649_0102.htm";
                    qs.exitQuest(true);
                } else {
                    html = "railman_obi_q0649_0101.htm";
                }
            } else if (n3 == 1) {
                html = "railman_obi_q0649_0106.htm";
            } else if (n3 == 2 && qs.getQuestItemsCount(8099) == 200L) {
                html = "railman_obi_q0649_0105.htm";
            } else {
                html = "railman_obi_q0649_0106.htm";
                qs.setCond(1);
            }
        }
        return html;

    }

    @Override
    public String onKill(NpcInstance npc, QuestState qs) {
        PlayerCharacter pc = qs.playerChar();
        if (pc == null)
            return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getCond();
        for (int i = 0; i < DROPLIST_COND.length; ++i) {
            if (n2 != DROPLIST_COND[i][0] || n != DROPLIST_COND[i][2]
                    || DROPLIST_COND[i][3] != 0 && qs.getQuestItemsCount(DROPLIST_COND[i][3]) <= 0L)
                continue;
            if (DROPLIST_COND[i][5] == 0) {
                qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][6]);
                continue;
            }
            if (!qs.rollAndGive(DROPLIST_COND[i][4], DROPLIST_COND[i][7], DROPLIST_COND[i][7], DROPLIST_COND[i][5],
                    DROPLIST_COND[i][6]) || DROPLIST_COND[i][1] == n2 || DROPLIST_COND[i][1] == 0)
                continue;
            qs.setCond(DROPLIST_COND[i][1]);
            qs.setState(State.STARTED);
        }
        return null;

    }

}
