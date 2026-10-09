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
 * Quest 647 - 647_InfluxOfMachines
 */
@Component
public class Quest647InfluxOfMachines extends Quest {

	public static final int bTI = 60;
	public static final int bTJ = 8100;
	public static final int[] bTK = new int[]{4963, 4964, 4965, 4966, 4967, 4968, 4969, 4970, 4971, 4972, 5000, 5001, 5002, 5003, 5004, 5005, 5006, 5007, 8298, 8306, 8310, 8312, 8322, 8324};

	public Quest647InfluxOfMachines(QuestManager questManager) {
	super(647, "647_InfluxOfMachines", "647_InfluxOfMachines");
		this.addStartNpc(32069);
		this.addTalkId(32069);
		int n = 22052;
		while (n < 22079) {
		this.addKillId(n++);
		}
		this.addQuestItem(8100);
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
            string2 = "collecter_gutenhagen_q0647_0103.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("647_3")) {
            if (qs.getQuestItemsCount(8100) >= 500L) {
                qs.takeItems(8100, -1L);
                qs.giveItems(bTK[ThreadLocalRandom.current().nextInt(bTK.length)], 1L);
                string2 = "collecter_gutenhagen_q0647_0201.htm";
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                string2 = "collecter_gutenhagen_q0647_0106.htm";
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
        long l = qs.getQuestItemsCount(8100);
        if (n == 0) {
            if (pc.getLevel() >= 46) {
                html = "collecter_gutenhagen_q0647_0101.htm";
            } else {
                html = "collecter_gutenhagen_q0647_0102.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1 && l < 500L) {
            html = "collecter_gutenhagen_q0647_0106.htm";
        } else if (n == 2 && l >= 500L) {
            html = "collecter_gutenhagen_q0647_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && qs.rollAndGive(8100, 1, 1, 500, 60.0 * npc.getTemplate().rateHp)) {
            qs.setCond(2);
        }
        return null;
    
	}

}
