package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
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
 * Quest 626 - 626_ADarkTwilight
 */
@Component
public class Quest626ADarkTwilight extends Quest {

	public static final int bPP = 31517;
	public static final int bPQ = 7169;

	public Quest626ADarkTwilight(QuestManager questManager) {
	super(626, "626_ADarkTwilight", "626_ADarkTwilight");
		this.addStartNpc(31517);
		int n = 21520;
		while (n <= 21542) {
		this.addKillId(n++);
		}
		this.addQuestItem(bPQ);
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
        if (event.equalsIgnoreCase("dark_presbyter_q0626_0104.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("dark_presbyter_q0626_0201.htm")) {
            if (qs.getQuestItemsCount(bPQ) < 300L) {
                string2 = "dark_presbyter_q0626_0203.htm";
            }
        } else if (event.equalsIgnoreCase("rew_exp")) {
            qs.takeItems(bPQ, -1L);
            qs.addExpAndSp(162773L, 12500L);
            string2 = "dark_presbyter_q0626_0202.htm";
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("rew_adena")) {
            qs.takeItems(bPQ, -1L);
            qs.giveItems(57, 100000L, true);
            string2 = "dark_presbyter_q0626_0202.htm";
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 31517) {
            if (n == 0) {
                if (pc.getLevel() < 60) {
                    html = "dark_presbyter_q0626_0103.htm";
                    qs.exitQuest(true);
                } else {
                    html = "dark_presbyter_q0626_0101.htm";
                }
            } else if (n == 1) {
                html = "dark_presbyter_q0626_0106.htm";
            } else if (n == 2) {
                html = "dark_presbyter_q0626_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && (ThreadLocalRandom.current().nextDouble(100.0) < (70))) {
            qs.giveItems(bPQ, 1L);
            if (qs.getQuestItemsCount(bPQ) == 300L) {
                qs.setCond(2);
            }
        }
        return null;
    
	}

}
