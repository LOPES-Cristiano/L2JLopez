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
 * Quest 313 - 313_CollectSpores
 */
@Component
public class Quest313CollectSpores extends Quest {

	public static final int Herbiel = 30150;
	public static final int SporeFungus = 20509;
	public static final int SporeSac = 1118;

	public Quest313CollectSpores(QuestManager questManager) {
		super(313, "313_CollectSpores", "313_CollectSpores");
		addStartNpc(30150);
		addKillId(20509);
		registerQuestItems(1118);
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

        if (event.equalsIgnoreCase("green_q0313_05.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.getLevel() >= 8) {
                html = "green_q0313_03.htm";
            } else {
                html = "green_q0313_02.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1) {
            html = "green_q0313_06.htm";
        } else if (n == 2) {
            if (qs.getQuestItemsCount(1118) < 10L) {
                qs.setCond(1);
                html = "green_q0313_06.htm";
            } else {
                qs.takeItems(1118, -1L);
                qs.giveItems(57, 3500L, true);
                qs.playSound("QuestState.SOUND_FINISH");
                html = "green_q0313_07.htm";
                qs.exitQuest(true);
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
        if (n2 == 1 && n == 20509 && (ThreadLocalRandom.current().nextInt(100) < 70)) {
            qs.giveItems(1118, 1L);
            if (qs.getQuestItemsCount(1118) < 10L) {
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else {
                qs.playSound("QuestState.SOUND_MIDDLE");
                qs.setCond(2);
                qs.setState(State.STARTED);
            }
        }
        return null;
    
	}

}
