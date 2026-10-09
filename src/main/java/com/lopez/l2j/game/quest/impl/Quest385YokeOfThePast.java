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
 * Quest 385 - 385_YokeOfThePast
 */
@Component
public class Quest385YokeOfThePast extends Quest {

	public static final int ANCIENT_SCROLL = 5902;
	public static final int BLANK_SCROLL = 5965;

	public Quest385YokeOfThePast(QuestManager questManager) {
	super(385, "385_YokeOfThePast", "385_YokeOfThePast");
		int n;
		for (n = 31095; n <= 31126; ++n) {
		if (n == 31111 || n == 31112 || n == 31113) continue;
		this.addStartNpc(n);
		}
		n = 21208;
		while (n < 21256) {
		this.addKillId(n++);
		}
		this.addQuestItem(5902);
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
        if (event.equalsIgnoreCase("enter_necropolis1_q0385_05.htm")) {
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.setCond(1);
        } else if (event.equalsIgnoreCase("enter_necropolis1_q0385_09.htm")) {
            string2 = "enter_necropolis1_q0385_10.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        if (this.checkNPC(n) && qs.getCond() == 0) {
            if (pc.getLevel() < 20) {
                html = "enter_necropolis1_q0385_02.htm";
                qs.exitQuest(true);
            } else {
                html = "enter_necropolis1_q0385_01.htm";
            }
        } else if (qs.getCond() == 1 && qs.getQuestItemsCount(5902) == 0L) {
            html = "enter_necropolis1_q0385_11.htm";
        } else if (qs.getCond() == 1 && qs.getQuestItemsCount(5902) > 0L) {
            html = "enter_necropolis1_q0385_09.htm";
            qs.giveItems(5965, qs.getQuestItemsCount(5902));
            qs.takeItems(5902, -1L);
        } else {
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        double d = 60.0;
        qs.rollAndGive(5902, 1, d);
        return null;
    
	}

public boolean checkNPC(int n) {
        return n >= 31095 && n <= 31126 && n != 31100 && n != 31111 && n != 31112 && n != 31113;
    }

}
