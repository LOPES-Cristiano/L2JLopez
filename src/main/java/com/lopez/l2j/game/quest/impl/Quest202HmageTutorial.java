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
 * Quest 202 - Hmage Tutorial
 */
@Component
public class Quest202HmageTutorial extends Quest {

	public static final int aJo = 30017;
	public static final int aJp = 30019;
	public static final int aJi = 18342;
	public static final int aJq = 1068;
	public static final int aJk = 6353;
	public static final int ayb = 5790;
	public static final int aya = 5789;
	public static final int aJr = 10;
	public static final int aJn = 0x100000;

	public Quest202HmageTutorial(QuestManager questManager) {
		super(202, "202_HmageTutorial", "Hmage Tutorial");
		addTalkId(30017, 30019);
		addKillId(18342);
		addFirstTalkId(30017, 30019);
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
        QuestState qs2 = qs;
        if (qs2 == null || qs == null) {
            return null;
        }
        PlayerCharacter player = pc;
        if (player == null) {
            return null;
        }
        int n = qs2.getInt("tutorial_quest_ex");
        if (event.equalsIgnoreCase("timer_newbie_helper")) {
            if (n == 0) {
                qs.playTutorialVoice("tutorial_voice_009b");
                qs2.set("tutorial_quest_ex", String.valueOf(1), true);
            }
            if (n == 3) {
                qs.playTutorialVoice("tutorial_voice_010b");
            }
            return null;
        }
        if (event.equalsIgnoreCase("timer_grand_master")) {
            if (n >= 4) {
                qs.showQuestionMark(7);
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.playTutorialVoice("tutorial_voice_025");
            }
            return null;
        }
        if (event.equalsIgnoreCase("reply_31") && qs.getQuestItemsCount(1068) > 0) {
            if (!pc.isMage() && qs.getQuestItemsCount(5789) <= 200) {
                qs.giveItems(5789, 200);
                qs.playTutorialVoice("tutorial_voice_026");
                qs.addExpAndSp(0, 50);
            }
            if (pc.isMage() && qs.getQuestItemsCount(5789) <= 200 && qs.getQuestItemsCount(5790) <= 100) {
                if (pc.classId() == 49) {
                    qs.giveItems(5789, 200);
                    qs.playTutorialVoice("tutorial_voice_026");
                } else {
                    qs.giveItems(5790, 100);
                    qs.playTutorialVoice("tutorial_voice_027");
                }
                qs.addExpAndSp(0, 50);
            }
            event2 = "gallin002.htm";
            qs.takeItems(1068, -1);
            qs.startQuestTimer("timer_grand_master", 60000);
            if (n <= 3) {
                qs2.set("tutorial_quest_ex", String.valueOf(4), true);
            }
        } else if (event.equalsIgnoreCase("reply_42")) {
            event2 = "gallin006.htm";
            
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return "noquest";
	}


	@Override
	public String onFirstTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        boolean bl = pc.race() != 3 && pc.isMage();
        QuestState qs2 = qs;
        if (qs2 == null) {
            return html;
        }
        int n2 = pc.classId();
        int n3 = qs.getInt("tutorial_quest_ex");
        int n4 = qs.getInt("tutorial_quest");
        switch (n) {
            case 30019: {
                int n5 = n4 & 0x7FFFFF00;
                if (n3 < 0) {
                    if (n2 == 10 && pc.race() == 0) {
                        qs2.startQuestTimer("timer_newbie_helper", 30000);
                        html = "doff001.htm";
                        qs.set("tutorial_quest_ex", "0");
                        qs.onTutorialClientEvent(n5 | 0x100000);
                        break;
                    }
                    html = "doff006.htm";
                    break;
                }
                if ((n3 == 1 || n3 == 2 || n3 == 0) && qs2.getQuestItemsCount(6353) < 1) {
                    html = "doff002.htm";
                    break;
                }
                if ((n3 == 1 || n3 == 2 || n3 == 0) && qs2.getQuestItemsCount(6353) > 0) {
                    html = "doff003.htm";
                    qs2.takeItems(6353, -1);
                    qs.set("tutorial_quest_ex", String.valueOf(3), true);
                    qs2.giveItems(1068, 1);
                    qs2.startQuestTimer("timer_newbie_helper", 30000);
                    qs.set("tutorial_quest", String.valueOf(n5 | 4), true);
                    if (!bl || qs2.getQuestItemsCount(5790) > 0) break;
                    qs2.playTutorialVoice("tutorial_voice_027");
                    qs2.giveItems(5790, 100);
                    break;
                }
                if (n3 == 3) {
                    html = "doff004.htm";
                    break;
                }
                if (n3 <= 3) break;
                html = "doff005.htm";
                break;
            }
            case 30017: {
                if (qs2.getQuestItemsCount(1068) > 0) {
                    html = "gallin001.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1068) == 0 && n3 > 3) {
                    html = "gallin004.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1068) != 0 || n3 > 3) break;
                html = "gallin003.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        QuestState qs2 = qs;
        if (qs2 == null) {
            return null;
        }
        int n = qs2.getInt("tutorial_quest_ex");
        if (!(n != 1 && n != 0 || false)) {
            qs.playTutorialVoice("tutorial_voice_011");
            qs.showQuestionMark(3);
            qs2.set("tutorial_quest", String.valueOf(2), true);
            
        }
        if ((n == 1 || n == 2 || n == 0) && qs.getQuestItemsCount(6353) < 1 && ThreadLocalRandom.current().nextInt(2) <= 1) {
            qs.giveItems(6353, 1);
            if (!false) {
                qs.playSound(QuestState.SOUND_ACCEPT);
                
            }
        }
        return null;
    
	}

}
