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
 * Quest 646 - 646_SignsOfRevolt
 */
@Component
public class Quest646SignsOfRevolt extends Quest {

	public static final int aEN = 32016;
	public static final int bTz = 22029;
	public static final int bTA = 22044;
	public static final int bTB = 22047;
	public static final int bTC = 22049;
	public static final int bTD = 1880;
	public static final int bTE = 1881;
	public static final int bTF = 1882;
	public static final int bTG = 8087;
	public static final int bTH = 75;

	public Quest646SignsOfRevolt(QuestManager questManager) {
	super(646, "646_SignsOfRevolt", "646_SignsOfRevolt");
		this.addStartNpc(aEN);
		int n = bTz;
		while (n <= bTA) {
		this.addKillId(n++);
		}
		this.addKillId(bTB);
		this.addKillId(bTC);
		this.addQuestItem(bTG);
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
        if (event.equalsIgnoreCase("torant_q0646_0103.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else {
            if (event.equalsIgnoreCase("reward_adena") && n == 2) {
                return d(qs, 57, 21600);
            }
            if (event.equalsIgnoreCase("reward_cbp") && n == 2) {
                return d(qs, bTE, 12);
            }
            if (event.equalsIgnoreCase("reward_steel") && n == 2) {
                return d(qs, bTD, 9);
            }
            if (event.equalsIgnoreCase("reward_leather") && n == 2) {
                return d(qs, bTF, 20);
            }
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != aEN) {
            return html;
        }
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.getLevel() < 40) {
                html = "torant_q0646_0102.htm";
                qs.exitQuest(true);
            } else {
                html = "torant_q0646_0101.htm";
                qs.setCond(0);
            }
        } else if (n == 2) {
            html = qs.getQuestItemsCount(bTG) >= 180L ? "torant_q0646_0105.htm" : "torant_q0646_0106.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        long l = qs.getQuestItemsCount(bTG);
        if (l < 180L && (ThreadLocalRandom.current().nextDouble(100.0) < (bTH))) {
            qs.giveItems(bTG, 1L);
            if (l == 179L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

private static String d(QuestState qs, int n, int n2) {
        if (qs.getQuestItemsCount(bTG) < 180L) {
            return null;
        }
        qs.takeItems(bTG, -1L);
        qs.giveItems(n, n2, true);
        qs.playSound(QuestState.SOUND_FINISH);
        qs.exitQuest(true);
        return "torant_q0646_0202.htm";
    }

}
