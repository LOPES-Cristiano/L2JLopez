package com.lopez.l2j.game.quest.impl;

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
 * Quest 645 - 645_GhostsOfBatur
 */
@Component
public class Quest645GhostsOfBatur extends Quest {

	public static final int aEL = 32017;
	public static final int bTy = 8089;
	public static final int[][] REWARDS = new int[][]{{1878, 18}, {1879, 7}, {1880, 4}, {1881, 6}, {1882, 10}, {1883, 2}};
	public static final int[] MOBS = new int[]{22007, 22009, 22010, 22011, 22012, 22013, 22014, 22015, 22016};

	public Quest645GhostsOfBatur(QuestManager questManager) {
	super(645, "645_GhostsOfBatur", "645_GhostsOfBatur");
		this.addStartNpc(32017);
		this.addTalkId(32017);
		for (int n : MOBS) {
		this.addKillId(n);
		}
		this.addQuestItem(8089);
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
        if (event.equalsIgnoreCase("32017-03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (qs.getCond() == 2) {
            if (qs.getQuestItemsCount(8089) >= 180L) {
                for (int i = 0; i < REWARDS.length; ++i) {
                    if (!event.equalsIgnoreCase(String.valueOf(REWARDS[i][0]))) continue;
                    qs.takeItems(8089, -1L);
                    qs.giveItems(REWARDS[i][0], REWARDS[i][1], true);
                    string2 = "32017-07.htm";
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                }
            } else {
                string2 = "32017-04.htm";
                qs.setCond(1);
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.getLevel() < 21 || pc.getLevel() > 32) {
                html = "32017-02.htm";
                qs.exitQuest(true);
            } else {
                html = "32017-01.htm";
            }
        } else if (n == 1) {
            html = "32017-04.htm";
        } else if (n == 2) {
            html = qs.getQuestItemsCount(8089) >= 180L ? "32017-05.htm" : "32017-01.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && qs.getQuestItemsCount(8089) < 180L && qs.rollAndGive(8089, 1, 2, 180, 70.0)) {
            qs.setCond(2);
            qs.setState(State.STARTED);
        }
        return null;
    
	}

}
