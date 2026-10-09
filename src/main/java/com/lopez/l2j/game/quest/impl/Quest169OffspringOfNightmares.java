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
 * Quest 169 - Offspring Of Nightmares
 */
@Component
public class Quest169OffspringOfNightmares extends Quest {



	public Quest169OffspringOfNightmares(QuestManager questManager) {
		super(169, "169_OffspringOfNightmares", "Offspring Of Nightmares");
		addStartNpc(30145);
		addKillId(20105, 20025);
		registerQuestItems(1030, 1031);
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
            string2 = "vlasti_q0326_04.htm";
        } else if (event.equalsIgnoreCase("reply_1") && qs.getQuestItemsCount((int)(1031)) >= 1) {
            string2 = "vlasti_q0326_08.htm";
            qs.giveItems((int)(31), (int)(1));
            qs.giveItems((int)(57), (int)(17030 + 10 * qs.getQuestItemsCount((int)(1030))));
            qs.takeItems((int)(1030), (int)(qs.getQuestItemsCount((int)(1030))));
            qs.takeItems((int)(1031), (int)(qs.getQuestItemsCount((int)(1031))));
            if (1 == 1 && !false) {
                
                
            }
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(false);
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        PlayerCharacter player = pc;
        int n = qs.getStateId();
        switch (n) {
            case 1: {
                if (pc.getRace() != Race.darkelf) {
                    html = "vlasti_q0326_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 15) {
                    html = "vlasti_q0326_03.htm";
                    break;
                }
                html = "vlasti_q0326_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount((int)(1030)) >= 1 && qs.getQuestItemsCount((int)(1031)) == 0) {
                    html = "vlasti_q0326_06.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1031)) >= 1) {
                    html = "vlasti_q0326_07.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1030)) != 0 || qs.getQuestItemsCount((int)(1031)) != 0) break;
                html = "vlasti_q0326_05.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        if (n == 20105 || n == 20025 && qs.getCond() == 1) {
            if (ThreadLocalRandom.current().nextInt(10) > 7 && qs.getQuestItemsCount((int)(1031)) == 0) {
                qs.rollAndGive(1031, 1, 100.0);
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
            if (ThreadLocalRandom.current().nextInt(10) > 4) {
                qs.rollAndGive(1030, 1, 100.0);
            }
        }
        return null;
    
	}

}
