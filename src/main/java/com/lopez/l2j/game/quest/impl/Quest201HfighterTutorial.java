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
 * Quest 201 - Hfighter Tutorial
 */
@Component
public class Quest201HfighterTutorial extends Quest {

	public static final int aJg = 30008;
	public static final int aJh = 30009;
	public static final int aJi = 18342;
	public static final int aJj = 1067;
	public static final int aJk = 6353;
	public static final int aya = 5789;
	public static final int ayb = 5790;
	public static final int aJl = 0;
	public static final int aJm = 49;
	public static final int aJn = 0x100000;

	public Quest201HfighterTutorial(QuestManager questManager) {
		super(201, "201_HfighterTutorial", "Tutorial");
		addTalkId(30009, 30008);
		addKillId(18342, 20001, 20130);
		addFirstTalkId(30009, 30008);
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
        int n = qs.get("tutorial_quest_ex") == null ? -1 : qs.getInt("tutorial_quest_ex");
        if (event.equalsIgnoreCase("timer_newbie_helper")) {
            if (n == 0) {
                qs.playTutorialVoice("tutorial_voice_009a");
                qs2.set("tutorial_quest_ex", String.valueOf(1), true);
            }
            if (n == 3) {
                qs.playTutorialVoice("tutorial_voice_010a");
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
        if (event.equalsIgnoreCase("reply_31") && qs.getQuestItemsCount(1067) > 0) {
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
            event2 = "roien002.htm";
            qs.takeItems(1067, -1);
            qs.startQuestTimer("timer_grand_master", 60000);
            if (n <= 3) {
                qs2.set("tutorial_quest_ex", String.valueOf(4), true);
            }
        } else if (event.equalsIgnoreCase("reply_42")) {
            event2 = "roien006.htm";
            qs.setState(State.COMPLETED);
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		return onFirstTalk(npc, qs);
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
        int n3 = pc.level();
        int n4 = qs.get("tutorial_quest_ex") == null ? -1 : qs.getInt("tutorial_quest_ex");
        int n5 = qs.getInt("tutorial_quest");
        switch (n) {
            case 30009: {
                int n6 = n5 & 0x7FFFFF00;
                if (n4 < 0) {
                    if (n2 == 0 && pc.race() == 0) {
                        qs2.startQuestTimer("timer_newbie_helper", 30000);
                        html = "carl001.htm";
                        qs.set("tutorial_quest_ex", "0");
                        qs.onTutorialClientEvent(n6 | 0x100000);
                        break;
                    }
                    html = "carl006.htm";
                    break;
                }
                if ((n4 == 1 || n4 == 2 || n4 == 0) && qs2.getQuestItemsCount(6353) < 1) {
                    html = "carl002.htm";
                    break;
                }
                if ((n4 == 1 || n4 == 2 || n4 == 0) && qs2.getQuestItemsCount(6353) > 0) {
                    html = "carl003.htm";
                    qs2.takeItems(6353, -1);
                    qs.set("tutorial_quest_ex", String.valueOf(3), true);
                    qs2.giveItems(1067, 1);
                    qs2.startQuestTimer("timer_newbie_helper", 30000);
                    qs.set("tutorial_quest", String.valueOf(n6 | 4), true);
                    if (!bl && qs2.getQuestItemsCount(5789) <= 0) {
                        qs2.giveItems(5789, 200);
                        qs2.playTutorialVoice("tutorial_voice_026");
                    }
                    if (!bl || qs2.getQuestItemsCount(5789) > 0 || qs2.getQuestItemsCount(5790) > 0) break;
                    if (n2 == 49) {
                        qs2.playTutorialVoice("tutorial_voice_026");
                        qs2.giveItems(5789, 200);
                        break;
                    }
                    qs2.playTutorialVoice("tutorial_voice_027");
                    qs2.giveItems(5790, 200);
                    break;
                }
                if (n4 == 3) {
                    html = "carl004.htm";
                    break;
                }
                if (n4 <= 3) break;
                html = "carl005.htm";
                break;
            }
            case 30008: {
                if (qs2.getQuestItemsCount(1067) > 0) {
                    html = "roien001.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1067) == 0 && n4 > 3) {
                    html = "roien004.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1067) != 0 || n4 > 3) break;
                html = "roien003.htm";
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
        int n = qs.get("tutorial_quest_ex") == null ? 0 : qs.getInt("tutorial_quest_ex");
        if (n == 0 || n == 1) {
            qs.playTutorialVoice("tutorial_voice_011");
            qs.showQuestionMark(3);
            qs2.set("tutorial_quest", String.valueOf(2), true);
        }
        if ((n == 0 || n == 1 || n == 2) && qs.getQuestItemsCount(6353) < 1) {
            qs.giveItems(6353, 1);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.playTutorialVoice("tutorial_voice_013");
            qs.showQuestionMark(5);
        }
        return null;
	}

}
