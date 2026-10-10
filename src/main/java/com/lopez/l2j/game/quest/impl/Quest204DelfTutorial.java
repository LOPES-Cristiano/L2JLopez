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
 * Quest 204 - Delf Tutorial
 */
@Component
public class Quest204DelfTutorial extends Quest {

	public static final int aJu = 30129;
	public static final int aJv = 30131;
	public static final int aJi = 18342;
	public static final int aJw = 1070;
	public static final int aJk = 6353;
	public static final int aya = 5789;
	public static final int ayb = 5790;
	public static final int aJn = 0x100000;

	public Quest204DelfTutorial(QuestManager questManager) {
		super(204, "204_DelfTutorial", "Tutorial");
		addTalkId(30129, 30131);
		addKillId(18342, 20418);
		addFirstTalkId(30129, 30131);
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
        int n2 = pc.isMage() ? 1 : 0;
        boolean bl = pc.race() != 3 && pc.isMage();
        if (event.equalsIgnoreCase("timer_newbie_helper")) {
            if (n == 0) {
                if (n2 == 0) {
                    qs.playTutorialVoice("tutorial_voice_009a");
                } else {
                    qs.playTutorialVoice("tutorial_voice_009b");
                }
                qs2.set("tutorial_quest_ex", String.valueOf(1), true);
            }
            if (n == 3) {
                qs.playTutorialVoice("tutorial_voice_010d");
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
        if (event.equalsIgnoreCase("reply_31") && qs.getQuestItemsCount(1070) > 0) {
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
            event2 = "jundin002.htm";
            qs.takeItems(1070, -1);
            qs.startQuestTimer("timer_grand_master", 60000);
            if (n <= 3) {
                qs2.set("tutorial_quest_ex", String.valueOf(4), true);
            }
        } else if (event.equalsIgnoreCase("reply_42")) {
            event2 = "jundin006.htm";
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
        int n2 = qs.get("tutorial_quest_ex") == null ? -1 : qs.getInt("tutorial_quest_ex");
        int n3 = qs.getInt("tutorial_quest");
        switch (n) {
            case 30131: {
                int n4 = n3 & 0x7FFFFF00;
                if (n2 < 0) {
                    if (pc.race() == 2) {
                        qs2.startQuestTimer("timer_newbie_helper", 30000);
                        html = !bl ? "carl001.htm" : "doff001.htm";
                        qs.set("tutorial_quest_ex", "0");
                        qs.onTutorialClientEvent(n4 | 0x100000);
                        break;
                    }
                    html = "carl006.htm";
                    break;
                }
                if ((n2 == 1 || n2 == 2 || n2 == 0) && qs2.getQuestItemsCount(6353) < 1) {
                    if (!bl) {
                        html = "carl002.htm";
                        break;
                    }
                    html = "doff002.htm";
                    break;
                }
                if ((n2 == 1 || n2 == 2 || n2 == 0) && qs2.getQuestItemsCount(6353) > 0) {
                    qs2.takeItems(6353, -1);
                    qs.set("tutorial_quest_ex", String.valueOf(3), true);
                    qs2.giveItems(1070, 1);
                    qs2.startQuestTimer("timer_newbie_helper", 30000);
                    qs.set("tutorial_quest", String.valueOf(n4 | 4), true);
                    if (!bl && qs2.getQuestItemsCount(5789) <= 0) {
                        qs2.giveItems(5789, 200);
                        qs2.playTutorialVoice("tutorial_voice_026");
                        html = "poeny003f.htm";
                    }
                    if (!bl || qs2.getQuestItemsCount(5790) > 0) break;
                    qs2.playTutorialVoice("tutorial_voice_027");
                    qs2.giveItems(5790, 100);
                    html = "poeny003m.htm";
                    break;
                }
                if (n2 == 3) {
                    html = "poeny004.htm";
                    break;
                }
                if (n2 <= 3) break;
                html = "carl005.htm";
                break;
            }
            case 30129: {
                if (qs2.getQuestItemsCount(1070) > 0) {
                    html = "jundin001.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1070) == 0 && n2 > 3) {
                    html = "jundin004.htm";
                    break;
                }
                if (qs2.getQuestItemsCount(1070) != 0 || n2 > 3) break;
                html = "jundin003.htm";
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
