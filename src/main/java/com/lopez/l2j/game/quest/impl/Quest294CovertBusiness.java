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
 * Quest 294 - 294_CovertBusiness
 */
@Component
public class Quest294CovertBusiness extends Quest {

	public static final int BatFang = 1491;
	public static final int RingOfRaccoon = 1508;
	public static final int BarbedBat = 20370;
	public static final int BladeBat = 20480;
	public static final int Keef = 30534;

	public Quest294CovertBusiness(QuestManager questManager) {
		super(294, "294_CovertBusiness", "294_CovertBusiness");
		addStartNpc(Keef);
		addKillId(BarbedBat);
		addKillId(BladeBat);
		registerQuestItems(BatFang);
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
        if (event.equalsIgnoreCase("elder_keef_q0294_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.getRace() != Race.dwarf) {
                html = "elder_keef_q0294_00.htm";
                qs.exitQuest(true);
            } else {
                if (pc.getLevel() >= 10) {
                    html = "elder_keef_q0294_02.htm";
                    return html;
                }
                html = "elder_keef_q0294_01.htm";
                qs.exitQuest(true);
            }
        } else if (qs.getQuestItemsCount(BatFang) < 100L) {
            html = "elder_keef_q0294_04.htm";
        } else {
            if (qs.getQuestItemsCount(RingOfRaccoon) < 1L) {
                qs.giveItems(RingOfRaccoon, 1L);
                html = "elder_keef_q0294_05.htm";
            } else {
                qs.giveItems(57, 2400L);
                html = "elder_keef_q0294_06.htm";
            }
            qs.addExpAndSp(0L, 600L);
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1) {
            qs.rollAndGive(BatFang, 1, 2, 100, 100.0);
        }
        return null;
    
	}

}
