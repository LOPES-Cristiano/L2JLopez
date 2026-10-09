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
 * Quest 293 - 293_HiddenVein
 */
@Component
public class Quest293HiddenVein extends Quest {

	public static final int beI = 30535;
	public static final int beJ = 30539;
	public static final int beK = 20446;
	public static final int beL = 20447;
	public static final int beM = 20448;
	public static final int beN = 1488;
	public static final int beO = 1489;
	public static final int beP = 1490;

	public Quest293HiddenVein(QuestManager questManager) {
		super(293, "293_HiddenVein", "293_HiddenVein");
		addStartNpc(30535);
		addTalkNpc(30539);
		addKillId(20446);
		addKillId(20447);
		addKillId(20448);
		registerQuestItems(1488, 1489, 1490);
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

        int n = qs.getStateId();
        if (event.equalsIgnoreCase("elder_filaur_q0293_03.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("elder_filaur_q0293_06.htm") && n == 2) {
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(false);
        } else if (event.equalsIgnoreCase("chichirin_q0293_03.htm") && n == 2) {
            if (qs.getQuestItemsCount(1489) < 4L) {
                return "chichirin_q0293_02.htm";
            }
            qs.takeItems(1489, 4L);
            qs.giveItems(1490, 1L);
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        switch (n) {
            case 1: {
                if (n2 != 30535) break;
                if (pc.getRace() != Race.dwarf) {
                    html = "elder_filaur_q0293_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() >= 6) {
                    html = "elder_filaur_q0293_02.htm";
                    break;
                }
                html = "elder_filaur_q0293_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 30535) break;
                if (qs.getQuestItemsCount(1488) < 1L && qs.getQuestItemsCount(1490) < 1L) {
                    html = "elder_filaur_q0293_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1488) < 1L && qs.getQuestItemsCount(1490) >= 1L) {
                    html = "elder_filaur_q0293_08.htm";
                    if (qs.getQuestItemsCount(1490) >= 10L) {
                        qs.giveItems(57, qs.getQuestItemsCount(1490) * 500L + 2000L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(1490) * 500L);
                    }
                    qs.takeItems(1490, -1L);
                    if (pc.getLevel() >= 25 || pc.getClassId() != 53 || qs.getInt("p1q2") == 1) break;
                    qs.set("p1q2", "1");
                    qs.showQuestionMark(26);
                    qs.playTutorialVoice("tutorial_voice_026");
                    qs.giveItems(5789, 6000L);
                    break;
                }
                if (qs.getQuestItemsCount(1488) >= 1L && qs.getQuestItemsCount(1490) < 1L) {
                    html = "elder_filaur_q0293_05.htm";
                    if (qs.getQuestItemsCount(1488) >= 10L) {
                        qs.giveItems(57, qs.getQuestItemsCount(1488) * 5L + 2000L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(1488) * 5L);
                    }
                    qs.takeItems(1488, -1L);
                    if (pc.getLevel() >= 25 || pc.getClassId() != 53 || qs.getInt("p1q2") == 1) break;
                    qs.set("p1q2", "1");
                    qs.showQuestionMark(26);
                    qs.playTutorialVoice("tutorial_voice_026");
                    qs.giveItems(5789, 6000L);
                    break;
                }
                if (qs.getQuestItemsCount(1488) < 1L || qs.getQuestItemsCount(1490) < 1L) break;
                html = "elder_filaur_q0293_09.htm";
                if (qs.getQuestItemsCount(1488) + qs.getQuestItemsCount(1490) >= 10L) {
                    qs.giveItems(57, qs.getQuestItemsCount(1488) * 5L + qs.getQuestItemsCount(1490) * 500L + 2000L);
                } else {
                    qs.giveItems(57, qs.getQuestItemsCount(1488) * 5L + qs.getQuestItemsCount(1490) * 500L);
                }
                qs.takeItems(1490, -1L);
                qs.takeItems(1488, -1L);
                if (pc.getLevel() >= 25 || pc.getClassId() != 53 || qs.getInt("p1q2") == 1) break;
                qs.set("p1q2", "1");
                qs.showQuestionMark(26);
                qs.playTutorialVoice("tutorial_voice_026");
                qs.giveItems(5789, 6000L);
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = ThreadLocalRandom.current().nextInt(100);
        if (n > 50) {
            qs.rollAndGive(1488, 1, 100.0);
        } else if (n < 5) {
            qs.rollAndGive(1489, 1, 100.0);
        }
        return null;
    
	}

}
