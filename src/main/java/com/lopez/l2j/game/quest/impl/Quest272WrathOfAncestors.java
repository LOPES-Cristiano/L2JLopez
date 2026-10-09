package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 272 - Wrath Of Ancestors
 */
@Component
public class Quest272WrathOfAncestors extends Quest {

	public static final int aEK = 30572;
	public static final int beb = 1474;
	public static final int bec = 20319;
	public static final int bed = 20320;
	public static final int[][] DROPLIST_COND = new int[][]{{1, 2, 20319, 0, 1474, 50, 100, 1}, {1, 2, 20320, 0, 1474, 50, 100, 1}};

	public Quest272WrathOfAncestors(QuestManager questManager) {
		super(272, "272_WrathOfAncestors", "Wrath Of Ancestors");
		addStartNpc(30572);
		for (int[] cond : DROPLIST_COND) addKillId(cond[2]);
		registerQuestItems(1474);
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

        String event2 = event;
        if (event.equals("1")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            event2 = "seer_livina_q0272_03.htm";
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getCond();
        if (n != 30572) return html;
        if (n2 == 0) {
            if (pc.race() != 3) {
                html = "seer_livina_q0272_00.htm";
                qs.exitQuest(true);
                return html;
            } else {
                if (pc.level() >= 5) return "seer_livina_q0272_02.htm";
                html = "seer_livina_q0272_01.htm";
                qs.exitQuest(true);
            }
            return html;
        } else {
            if (n2 == 1) {
                return "seer_livina_q0272_04.htm";
            }
            if (n2 != 2) return html;
            qs.takeItems(1474, -1);
            qs.giveItems(57, 1500);
            html = "seer_livina_q0272_05.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
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
            if (n2 != DROPLIST_COND[i][0] || n != DROPLIST_COND[i][2] || DROPLIST_COND[i][3] != 0 && qs.getQuestItemsCount(DROPLIST_COND[i][3]) <= 0) continue;
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
