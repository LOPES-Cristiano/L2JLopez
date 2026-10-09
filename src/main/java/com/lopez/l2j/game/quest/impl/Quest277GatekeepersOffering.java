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
 * Quest 277 - Gatekeepers Offering
 */
@Component
public class Quest277GatekeepersOffering extends Quest {

	public static final int bev = 1572;
	public static final int bew = 1658;

	public Quest277GatekeepersOffering(QuestManager questManager) {
		super(277, "277_GatekeepersOffering", "Gatekeepers Offering");
		addStartNpc(30576);
		addKillId(20333);
		registerQuestItems(1572);
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
            if (pc.level() >= 15) {
                event2 = "gatekeeper_tamil_q0277_03.htm";
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
            } else {
                event2 = "gatekeeper_tamil_q0277_01.htm";
            }
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
        if (n == 30576 && n2 == 0) {
            html = "gatekeeper_tamil_q0277_02.htm";
        } else if (n == 30576 && n2 == 1 && qs.getQuestItemsCount(1572) < 20) {
            html = "gatekeeper_tamil_q0277_04.htm";
        } else if (n == 30576 && n2 == 2 && qs.getQuestItemsCount(1572) < 20) {
            html = "gatekeeper_tamil_q0277_04.htm";
        } else if (n == 30576 && n2 == 2 && qs.getQuestItemsCount(1572) >= 20) {
            html = "gatekeeper_tamil_q0277_05.htm";
            qs.takeItems(1572, -1);
            qs.giveItems(1658, 2);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        qs.rollAndGive(1572, 1, 1, 20, 33.0);
        if (qs.getQuestItemsCount(1572) >= 20) {
            qs.setCond(2);
        }
        return null;
    
	}

}
