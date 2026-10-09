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
 * Quest 271 - Proof Of Valor
 */
@Component
public class Quest271ProofOfValor extends Quest {

	public static final int bdX = 30577;
	public static final int bdY = 1473;
	public static final int bdZ = 1507;
	public static final int bea = 1506;
	public static final int[][] DROPLIST_COND = new int[][]{{1, 2, 20475, 0, 1473, 50, 25, 2}};

	public Quest271ProofOfValor(QuestManager questManager) {
		super(271, "271_ProofOfValor", "Proof Of Valor");
		addStartNpc(30577);
		addTalkId(30577);
		for (int[] cond : DROPLIST_COND) addKillId(cond[2]);
		registerQuestItems(1473);
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
        if (event.equalsIgnoreCase("praetorian_rukain_q0271_03.htm")) {
            qs.playSound(QuestState.SOUND_ACCEPT);
            if (qs.getQuestItemsCount(1506) > 0 || qs.getQuestItemsCount(1507) > 0) {
                event2 = "praetorian_rukain_q0271_07.htm";
            }
            qs.setCond(1);
            qs.setState(State.STARTED);
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
        if (n == 30577) {
            if (n2 == 0) {
                if (pc.race() != 3) {
                    html = "praetorian_rukain_q0271_00.htm";
                    qs.exitQuest(true);
                } else if (pc.level() < 4) {
                    html = "praetorian_rukain_q0271_01.htm";
                    qs.exitQuest(true);
                } else if (qs.getQuestItemsCount(1506) > 0 || qs.getQuestItemsCount(1507) > 0) {
                    html = "praetorian_rukain_q0271_06.htm";
                    qs.exitQuest(true);
                } else {
                    html = "praetorian_rukain_q0271_02.htm";
                }
            } else if (n2 == 1) {
                html = "praetorian_rukain_q0271_04.htm";
            } else if (n2 == 2 && qs.getQuestItemsCount(1473) == 50) {
                qs.takeItems(1473, -1);
                if (ThreadLocalRandom.current().nextInt(100) < 14) {
                    qs.takeItems(1507, -1);
                    qs.giveItems(1507, 1);
                } else {
                    qs.takeItems(1506, -1);
                    qs.giveItems(1506, 1);
                }
                html = "praetorian_rukain_q0271_05.htm";
                qs.exitQuest(true);
            } else if (n2 == 2 && qs.getQuestItemsCount(1473) < 50) {
                html = "praetorian_rukain_q0271_04.htm";
                qs.setCond(1);
                qs.setState(State.STARTED);
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
