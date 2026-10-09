package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 648 - 648_AnIceMerchantsDream
 */
@Component
public class Quest648AnIceMerchantsDream extends Quest {

	public static final int bTL = 32020;
	public static final int bTM = 32023;
	public static final int bTN = 8057;
	public static final int bTO = 8077;
	public static final int bTP = 8078;
	public static final int bTQ = 10;
	public static final int bTR = 30;
	public static final List<Integer> bTS = new ArrayList<Integer>();

	public Quest648AnIceMerchantsDream(QuestManager questManager) {
	super(648, "648_AnIceMerchantsDream", "648_AnIceMerchantsDream");
		this.addStartNpc(bTL);
		this.addStartNpc(bTM);
		for (int i = 22080; i <= 22098; ++i) {
		if (i == 22095) continue;
		this.addKillId(i);
		}
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

        int n = qs.getStateId();
        if (event.equalsIgnoreCase("repre_q0648_04.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("repre_q0648_22.htm") && n == 2) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        if (n != 2) {
            return html;
        }
        long l = qs.getQuestItemsCount(bTO);
        long l2 = qs.getQuestItemsCount(bTP);
        if (event.equalsIgnoreCase("repre_q0648_14.htm")) {
            long l3 = l * 300L + l2 * 1200L;
            if (l3 <= 0L) return "repre_q0648_15.htm";
            qs.takeItems(bTO, -1L);
            qs.takeItems(bTP, -1L);
            qs.giveItems(57, l3);
            return html;
        }
        if (event.equalsIgnoreCase("ice_lathe_q0648_06.htm")) {
            int n2 = pc.objectId();
            List<Integer> list = bTS;
            synchronized (list) {
                if (bTS.contains(n2)) {
                    return html;
                }
                if (l <= 0L) {
                    return "cheat.htm";
                }
                bTS.add(n2);
            }
            qs.takeItems(bTO, 1L);
            qs.playSound("ItemSound2.broken_key");
            return html;
        }
        if (!event.equalsIgnoreCase("ice_lathe_q0648_08.htm")) return html;
        Integer n3 = pc.objectId();
        List<Integer> list = bTS;
        synchronized (list) {
            if (!bTS.contains(n3)) return "cheat.htm";
            while (bTS.contains(n3)) {
                bTS.remove(n3);
            }
        }
        if ((ThreadLocalRandom.current().nextDouble(100.0) < (bTR))) {
            qs.giveItems(bTP, 1L);
            qs.playSound("ItemSound3.sys_enchant_sucess");
            return html;
        } else {
            qs.playSound("ItemSound3.sys_enchant_failed");
            return "ice_lathe_q0648_09.htm";
        }
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getCond();
        if (n == 1) {
            if (n2 == bTL) {
                if (pc.getLevel() >= 53) {
                    qs.setCond(0);
                    return "repre_q0648_03.htm";
                }
                qs.exitQuest(true);
                return "repre_q0648_01.htm";
            }
            if (n2 == bTM) {
                return "ice_lathe_q0648_01.htm";
            }
        }
        if (n != 2) {
            return "noquest";
        }
        long l = qs.getQuestItemsCount(bTO);
        if (n2 == bTM) {
            return l > 0L ? "ice_lathe_q0648_03.htm" : "ice_lathe_q0648_02.htm";
        }
        long l2 = qs.getQuestItemsCount(bTP);
        if (n2 == bTL) {
            QuestState questState2 = qs.getQuestState("115_TheOtherSideOfTruth");
            if (questState2 != null && questState2.isCompleted()) {
                n3 = 2;
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
            if (n3 == 1) {
                if (l > 0L || l2 > 0L) {
                    return "repre_q0648_10.htm";
                }
                return "repre_q0648_08.htm";
            }
            if (n3 == 2) {
                return l > 0L || l2 > 0L ? "repre_q0648_11.htm" : "repre_q0648_09.htm";
            }
        }
        return "noquest";
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getCond();
        if (n > 0) {
            qs.rollAndGive(bTO, 1, npc.getNpcId() - 22050);
            if (n == 2) {
                qs.rollAndGive(bTN, 1, bTQ);
            }
        }
        return null;
    
	}

}
