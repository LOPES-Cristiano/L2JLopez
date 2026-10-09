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
 * Quest 264 - Keen Claws
 */
@Component
public class Quest264KeenClaws extends Quest {

	public static final int bdz = 30136;
	public static final int bdA = 1367;
	public static final int bdB = 36;
	public static final int bdC = 43;
	public static final int bdD = 462;
	public static final int bdE = 1061;
	public static final int bdF = 48;
	public static final int bdG = 35;
	public static final int bdH = 20003;
	public static final int bdI = 20456;
	public static final int[][] DROPLIST_COND = new int[][]{{1, 2, 20003, 0, 1367, 50, 50, 2}, {1, 2, 20456, 0, 1367, 50, 50, 2}};

	public Quest264KeenClaws(QuestManager questManager) {
		super(264, "264_KeenClaws", "Keen Claws");
		addStartNpc(30136);
		addKillId(20003);
		addKillId(20456);
		registerQuestItems(1367);
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
        if (event.equalsIgnoreCase("paint_q0264_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getCond();
        if (n != 30136) return html;
        if (n2 == 0) {
            if (pc.level() >= 3) {
                return "paint_q0264_02.htm";
            }
            qs.exitQuest(true);
            return "paint_q0264_01.htm";
        }
        if (n2 == 1) {
            return "paint_q0264_04.htm";
        }
        if (n2 != 2) return html;
        qs.takeItems(1367, -1);
        int n3 = ThreadLocalRandom.current().nextInt(17);
        if (n3 == 0) {
            qs.giveItems(43, 1);
            qs.playSound(QuestState.SOUND_FINISH);
        } else if (n3 < 2) {
            qs.giveItems(57, 1000);
        } else if (n3 < 5) {
            qs.giveItems(36, 1);
        } else if (n3 < 8) {
            qs.giveItems(462, 1);
            qs.giveItems(57, 50);
        } else if (n3 < 11) {
            qs.giveItems(1061, 1);
        } else if (n3 < 14) {
            qs.giveItems(48, 1);
        } else {
            qs.giveItems(35, 1);
        }
        html = "paint_q0264_05.htm";
        qs.playSound(QuestState.SOUND_FINISH);
        qs.exitQuest(true);
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
