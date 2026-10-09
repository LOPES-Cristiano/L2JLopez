package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 164 - Blood Fiend
 */
@Component
public class Quest164BloodFiend extends Quest {



	public Quest164BloodFiend(QuestManager questManager) {
		super(164, "164_BloodFiend", "Blood Fiend");
		addStartNpc(30149);
		addKillId(27021);
		registerQuestItems(1044);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

        String string2 = event;
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "cel_q0318_04.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = qs.getStateId();
        PlayerCharacter player = pc;
        switch (n) {
            case 1: {
                if (pc.getRace() == Race.darkelf) {
                    html = "cel_q0318_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 21) {
                    html = "cel_q0318_03.htm";
                    break;
                }
                html = "cel_q0318_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount((int)(1044)) < 1) {
                    html = "cel_q0318_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1044)) < 1) break;
                html = "cel_q0318_06.htm";
                qs.giveItems((int)(57), (int)(42130));
                qs.takeItems((int)(1044), (int)(1));
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(false);
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount((int)(1044)) == 0 && qs.getCond() == 1) {
            qs.giveItems((int)(1044), (int)(1));
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_MIDDLE);
        }
        return null;
    
	}

}
