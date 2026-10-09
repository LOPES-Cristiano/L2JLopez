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
 * Quest 267 - Wrath Of Verdure
 */
@Component
public class Quest267WrathOfVerdure extends Quest {

	public static final int bdT = 31853;
	public static final int bdU = 20325;
	public static final int bdV = 1335;
	public static final int bdW = 1340;

	public Quest267WrathOfVerdure(QuestManager questManager) {
		super(267, "267_WrathOfVerdure", "Wrath Of Verdure");
		addStartNpc(31853);
		addKillId(20325);
		registerQuestItems(1335);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
            event2 = "bri_mec_tran_q0267_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            event2 = "bri_mec_tran_q0267_06.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            event2 = "bri_mec_tran_q0267_07.htm";
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        PlayerCharacter player = pc;
        switch (n) {
            case 1: {
                if (pc.level() >= 4 && pc.race() == 1) {
                    html = "bri_mec_tran_q0267_02.htm";
                    break;
                }
                if (pc.race() != 1) {
                    html = "bri_mec_tran_q0267_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 4) break;
                html = "bri_mec_tran_q0267_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount(1335) > 0) {
                    qs.giveItems(1340, qs.getQuestItemsCount(1335));
                    if (qs.getQuestItemsCount(1335) >= 10) {
                        qs.giveItems(57, 600);
                    }
                    qs.takeItems(1335, -1);
                    html = "bri_mec_tran_q0267_05.htm";
                    break;
                }
                html = "bri_mec_tran_q0267_04.htm";
            }
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (ThreadLocalRandom.current().nextInt(10) < 5) {
            qs.rollAndGive(1335, 1, 100.0);
        }
        return null;
    
	}

}
