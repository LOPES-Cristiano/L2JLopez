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
 * Quest 273 - Invaders Of Holyland
 */
@Component
public class Quest273InvadersOfHolyland extends Quest {

	public static final int bee = 30566;
	public static final int bef = 1475;
	public static final int beg = 1476;

	public Quest273InvadersOfHolyland(QuestManager questManager) {
		super(273, "273_InvadersOfHolyland", "Invaders Of Holyland");
		addStartNpc(30566);
		addKillId(20311, 20312, 20313);
		registerQuestItems(1475, 1476);
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
        if (event.equals("atuba_chief_varkees_q0273_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("atuba_chief_varkees_q0273_07.htm")) {
            qs.setCond(0);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equals("atuba_chief_varkees_q0273_08.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
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
                if (n != 30566) break;
                if (pc.race() != 3) {
                    html = "atuba_chief_varkees_q0273_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() < 6) {
                    html = "atuba_chief_varkees_q0273_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "atuba_chief_varkees_q0273_02.htm";
                break;
            }
            case 2: {
                if (n != 30566) break;
                if (qs.getQuestItemsCount(1475) + qs.getQuestItemsCount(1476) == 0) {
                    html = "atuba_chief_varkees_q0273_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1476) == 0) {
                    long l = qs.getQuestItemsCount(1475);
                    html = "atuba_chief_varkees_q0273_05.htm";
                    if (qs.getQuestItemsCount(1475) >= 10) {
                        qs.giveItems(57, l * 3 + 1500);
                    } else {
                        qs.giveItems(57, l * 3);
                    }
                    qs.takeItems(1475, l);
                    if (1 == 1 && pc.level() < 25 && !false) {
                        
                        
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
                    qs.exitQuest(true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    break;
                }
                html = "atuba_chief_varkees_q0273_06.htm";
                if (qs.getQuestItemsCount(1475) + qs.getQuestItemsCount(1476) >= 10) {
                    qs.giveItems(57, qs.getQuestItemsCount(1476) * 10 + qs.getQuestItemsCount(1475) * 3 + 1800);
                } else {
                    qs.giveItems(57, qs.getQuestItemsCount(1476) * 10 + qs.getQuestItemsCount(1475) * 3);
                }
                qs.takeAllItems(1475, 1476);
                if (1 == 1 && pc.level() < 25 && !false) {
                    
                    
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
                qs.exitQuest(true);
                qs.playSound(QuestState.SOUND_FINISH);
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
        if (n == 20311) {
            if (n2 == 1) {
                if (ThreadLocalRandom.current().nextInt(100) < 90) {
                    qs.giveItems(1475, 1);
                } else {
                    qs.giveItems(1476, 1);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20312) {
            if (n2 == 1) {
                if (ThreadLocalRandom.current().nextInt(100) < 87) {
                    qs.giveItems(1475, 1);
                } else {
                    qs.giveItems(1476, 1);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20313 && n2 == 1) {
            if (ThreadLocalRandom.current().nextInt(100) < 77) {
                qs.giveItems(1475, 1);
            } else {
                qs.giveItems(1476, 1);
            }
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
