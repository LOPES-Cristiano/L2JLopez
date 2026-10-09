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
 * Quest 257 - Guard Is Busy
 */
@Component
public class Quest257GuardIsBusy extends Quest {

	public static final int bdl = 30039;
	public static final int bdm = 1084;
	public static final int ORC_AMULET = 752;
	public static final int ORC_NECKLACE = 1085;
	public static final int bdn = 1086;

	public Quest257GuardIsBusy(QuestManager questManager) {
		super(257, "257_GuardIsBusy", "Guard Is Busy");
		addStartNpc(30039);
		addKillId(20130, 20131, 20132, 20342, 20343, 20006, 20093, 20096, 20098);
		registerQuestItems(752, 1085, 1086, 1084);
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
        if (event.equalsIgnoreCase("gilbert_q0257_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.takeItems(1084, -1);
            qs.giveItems(1084, 1);
        } else if (event.equalsIgnoreCase("257_2")) {
            event2 = "gilbert_q0257_05.htm";
            qs.takeItems(1084, -1);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("257_3")) {
            event2 = "gilbert_q0257_06.htm";
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        switch (n) {
            case 1: {
                if (n2 != 30039) break;
                if (pc.level() >= 6) {
                    html = "gilbert_q0257_02.htm";
                    break;
                }
                html = "gilbert_q0257_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 30039) break;
                if (qs.getQuestItemsCount(752) < 1 && qs.getQuestItemsCount(1085) < 1 && qs.getQuestItemsCount(1086) < 1) {
                    html = "gilbert_q0257_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(752) <= 0 && qs.getQuestItemsCount(1085) <= 0 && qs.getQuestItemsCount(1086) <= 0) break;
                html = "gilbert_q0257_07.htm";
                if (qs.getQuestItemsCount(752) + qs.getQuestItemsCount(1085) + qs.getQuestItemsCount(1086) >= 10) {
                    qs.giveItems(57, 10 * qs.getQuestItemsCount(752) + 20 * qs.getQuestItemsCount(1085) + 20 * qs.getQuestItemsCount(1086) + 1000);
                } else {
                    qs.giveItems(57, 10 * qs.getQuestItemsCount(752) + 20 * qs.getQuestItemsCount(1085) + 20 * qs.getQuestItemsCount(1086));
                }
                qs.takeAllItems(752, 1085, 1086);
                
            }
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getQuestItemsCount(1084) > 0 && qs.getCond() > 0) {
            if (n == 20130 || n == 20131 || n == 20006) {
                qs.rollAndGive(752, 1, 50.0);
            } else if (n == 20093 || n == 20096 || n == 20098) {
                qs.rollAndGive(1085, 1, 50.0);
            } else if (n == 20132) {
                qs.rollAndGive(1086, 1, 33.0);
            } else if (n == 20343) {
                qs.rollAndGive(1086, 1, 50.0);
            } else if (n == 20342) {
                qs.rollAndGive(1086, 1, 75.0);
            }
        }
        return null;
    
	}

}
