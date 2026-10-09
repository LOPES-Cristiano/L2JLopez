package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
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
 * Quest 356 - 356_DigUpTheSeaOfSpores
 */
@Component
public class Quest356DigUpTheSeaOfSpores extends Quest {

	public static final int bre = 30717;
	public static final int brf = 20562;
	public static final int brg = 20558;
	public static final int brh = 5865;
	public static final int bri = 5866;

	public Quest356DigUpTheSeaOfSpores(QuestManager questManager) {
	super(356, "356_DigUpTheSeaOfSpores", "356_DigUpTheSeaOfSpores");
		this.addStartNpc(30717);
		this.addKillId(20562, 20558);
		this.addQuestItem(5865, 5866);
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

        String string2 = event;
        int n = getFirstStartNpc();
        if (n == 30717) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "magister_gauen_q0356_06.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "magister_gauen_q0356_05.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                qs.setCond(1);
                string2 = "magister_gauen_q0356_11.htm";
            } else if (event.equalsIgnoreCase("reply_3") && qs.getQuestItemsCount(5866) >= 50L) {
                qs.addExpAndSp(31850L, 0L);
                qs.takeItems(5866, -1L);
                string2 = "magister_gauen_q0356_12.htm";
            } else if (event.equalsIgnoreCase("reply_4") && qs.getQuestItemsCount(5865) >= 50L) {
                qs.addExpAndSp(0L, 1820L);
                qs.takeItems(5865, -1L);
                string2 = "magister_gauen_q0356_13.htm";
            } else if (event.equalsIgnoreCase("reply_5") && qs.getQuestItemsCount(5865) >= 50L && qs.getQuestItemsCount(5866) >= 50L) {
                qs.addExpAndSp(45500L, 2600L);
                qs.takeItems(5866, -1L);
                qs.takeItems(5865, -1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "magister_gauen_q0356_14.htm";
            } else if (event.equalsIgnoreCase("reply_6")) {
                string2 = "magister_gauen_q0356_15.htm";
            } else if (event.equalsIgnoreCase("reply_7") && qs.getQuestItemsCount(5865) >= 50L && qs.getQuestItemsCount(5866) >= 50L) {
                int n2 = ThreadLocalRandom.current().nextInt(100);
                qs.takeItems(5866, -1L);
                qs.takeItems(5865, -1L);
                if (n2 < 20) {
                    qs.giveItems(57, 44000L);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "magister_gauen_q0356_16.htm";
                } else if (n2 < 70) {
                    qs.giveItems(57, 20950L);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "magister_gauen_q0356_17.htm";
                } else if (n2 >= 70) {
                    qs.giveItems(57, 10400L);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "magister_gauen_q0356_18.htm";
                }
            } else if (event.equalsIgnoreCase("reply_8")) {
                qs.takeItems(5866, -1L);
                qs.takeItems(5865, -1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "magister_gauen_q0356_19.htm";
            }
        }
        return string2;
    
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
                if (n != 30717) break;
                if (pc.getLevel() < 43 || pc.getLevel() > 51) {
                    html = "magister_gauen_q0356_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "magister_gauen_q0356_02.htm";
                break;
            }
            case 2: {
                if (n != 30717) break;
                if (qs.getQuestItemsCount(5866) < 50L && qs.getQuestItemsCount(5865) < 50L) {
                    html = "magister_gauen_q0356_07.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5866) >= 50L && qs.getQuestItemsCount(5865) < 50L) {
                    html = "magister_gauen_q0356_08.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5866) < 50L && qs.getQuestItemsCount(5865) >= 50L) {
                    html = "magister_gauen_q0356_09.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5866) < 50L || qs.getQuestItemsCount(5865) < 50L) break;
                html = "magister_gauen_q0356_10.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20562 && qs.getQuestItemsCount(5865) < 50L) {
            if (ThreadLocalRandom.current().nextInt(100) < 94) {
                qs.giveItems(5865, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
                if (qs.getQuestItemsCount(5866) >= 50L && qs.getQuestItemsCount(5865) >= 50L) {
                    qs.setCond(3);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (qs.getQuestItemsCount(5865) >= 50L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n == 20558 && qs.getQuestItemsCount(5866) < 50L && ThreadLocalRandom.current().nextInt(100) < 73) {
            qs.giveItems(5866, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
            if (qs.getQuestItemsCount(5866) >= 50L && qs.getQuestItemsCount(5865) >= 50L) {
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else if (qs.getQuestItemsCount(5866) >= 50L) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
