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
 * Quest 165 - Shilens Hunt
 */
@Component
public class Quest165ShilensHunt extends Quest {



	public Quest165ShilensHunt(QuestManager questManager) {
		super(165, "165_ShilensHunt", "Shilens Hunt");
		addStartNpc(30348);
		addKillId(20456, 20529, 20532, 20536);
		registerQuestItems(1160);
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
            string2 = "sentry_nelsya_q0321_03.htm";
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
                if (pc.getRace() != Race.darkelf) {
                    html = "sentry_nelsya_q0321_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 3) {
                    html = "sentry_nelsya_q0321_02.htm";
                    break;
                }
                html = "sentry_nelsya_q0321_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount((int)(1160)) < 13) {
                    html = "sentry_nelsya_q0321_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1160)) < 13) break;
                html = "sentry_nelsya_q0321_05.htm";
                qs.takeItems((int)(1160), (int)(-1));
                qs.giveItems((int)(1060), (int)(5));
                qs.exitQuest(false);
                qs.playSound(QuestState.SOUND_FINISH);
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        if (n == 20456 && ThreadLocalRandom.current().nextInt(3) < 3 && qs.getCond() == 1) {
            qs.rollAndGive(1160, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1160)) >= 13) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            }
        } else if (n == 20529 && ThreadLocalRandom.current().nextInt(3) < 1 && qs.getCond() == 1) {
            qs.rollAndGive(1160, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1160)) >= 13) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            }
        } else if (n == 20532 && ThreadLocalRandom.current().nextInt(3) < 1 && qs.getCond() == 1) {
            qs.rollAndGive(1160, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1160)) >= 13) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            }
        } else if (n == 20536 && ThreadLocalRandom.current().nextInt(3) < 2 && qs.getCond() == 1) {
            qs.rollAndGive(1160, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1160)) >= 13) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            }
        }
        return null;
    
	}

}
