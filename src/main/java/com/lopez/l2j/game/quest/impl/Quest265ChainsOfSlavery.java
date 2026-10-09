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
 * Quest 265 - Chains Of Slavery
 */
@Component
public class Quest265ChainsOfSlavery extends Quest {

	public static final int aIr = 30357;
	public static final int bdJ = 20004;
	public static final int bdK = 20005;
	public static final int bdL = 1368;

	public Quest265ChainsOfSlavery(QuestManager questManager) {
		super(265, "265_ChainsOfSlavery", "Chains Of Slavery");
		addStartNpc(30357);
		addKillId(20004);
		addKillId(20005);
		registerQuestItems(1368);
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
        if (event.equalsIgnoreCase("sentry_krpion_q0265_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("sentry_krpion_q0265_06.htm")) {
            qs.exitQuest(true);
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30357) break;
                if (pc.level() >= 6 && pc.race() == 2) {
                    html = "sentry_krpion_q0265_02.htm";
                    break;
                }
                if (pc.race() != 2) {
                    html = "sentry_krpion_q0265_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 6) break;
                html = "sentry_krpion_q0265_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n != 30357) break;
                if (qs.getQuestItemsCount(1368) > 0) {
                    if (qs.getQuestItemsCount(1368) >= 10) {
                        qs.giveItems(57, 12 * qs.getQuestItemsCount(1368) + 500);
                    } else {
                        qs.giveItems(57, 12 * qs.getQuestItemsCount(1368));
                    }
                    qs.takeItems(1368, -1);
                    if (pc.level() < 25 && 1 == 1 && !false) {
                        
                        
                        QuestState qs2 = qs;
                        if (qs2 != null && qs2.getInt("tutorial_quest_ex") != 10) {
                            qs.showQuestionMark(26);
                            qs2.set("tutorial_quest_ex", "10");
                            if (pc.isMage()) {
                                qs.playTutorialVoice("tutorial_voice_027");
                                qs.giveItems(5790, 3000);
                            } else {
                                qs.playTutorialVoice("tutorial_voice_026");
                                qs.giveItems(5789, 6000);
                            }
                        }
                    }
                    html = "sentry_krpion_q0265_05.htm";
                    break;
                }
                html = "sentry_krpion_q0265_04.htm";
            }
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && ThreadLocalRandom.current().nextInt(100) < 55) {
            qs.giveItems(1368, 1);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
