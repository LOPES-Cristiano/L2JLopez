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
 * Quest 300 - 300_HuntingLetoLizardman
 */
@Component
public class Quest300HuntingLetoLizardman extends Quest {

	public static final int bfj = 30126;
	public static final int bfk = 7139;
	public static final int bfl = 1872;
	public static final int bfm = 1867;
	public static final int bfn = 70;

	public Quest300HuntingLetoLizardman(QuestManager questManager) {
		super(300, "300_HuntingLetoLizardman", "300_HuntingLetoLizardman");
		addStartNpc(bfj);
		for (int n = 20577; n <= 20582; n++) {
			addKillId(n);
		}
		registerQuestItems(bfk);
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
        int n = qs.getStateId();
        if (event.equalsIgnoreCase("rarshints_q0300_0104.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("rarshints_q0300_0201.htm") && n == 2) {
            if (qs.getQuestItemsCount(bfk) < 60L) {
                string2 = "rarshints_q0300_0202.htm";
                qs.setCond(1);
            } else {
                qs.takeItems(bfk, -1L);
                switch (ThreadLocalRandom.current().nextInt(3)) {
                    case 0: {
                        qs.giveItems(57, 30000L, true);
                        break;
                    }
                    case 1: {
                        qs.giveItems(bfl, 50L, true);
                        break;
                    }
                    case 2: {
                        qs.giveItems(bfm, 50L, true);
                    }
                }
                qs.playSound("QuestState.SOUND_FINISH");
                qs.exitQuest(true);
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != bfj) {
            return html;
        }
        if (qs.getState() == State.CREATED) {
            if (pc.getLevel() < 34) {
                html = "rarshints_q0300_0103.htm";
                qs.exitQuest(true);
            } else {
                html = "rarshints_q0300_0101.htm";
                qs.setCond(0);
            }
        } else if (qs.getQuestItemsCount(bfk) < 60L) {
            html = "rarshints_q0300_0106.htm";
            qs.setCond(1);
        } else {
            html = "rarshints_q0300_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        long l = qs.getQuestItemsCount(bfk);
        if (l < 60L && (ThreadLocalRandom.current().nextInt(100) < bfn)) {
            qs.giveItems(bfk, 1L);
            if (l == 59L) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
