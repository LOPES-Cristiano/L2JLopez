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
 * Quest 328 - 328_SenseForBusiness
 */
@Component
public class Quest328SenseForBusiness extends Quest {

	public static final int bgM = 30436;
	public static final int bgN = 1347;
	public static final int bgO = 1366;
	public static final int bgP = 1348;

	public Quest328SenseForBusiness(QuestManager questManager) {
		super(328, "328_SenseForBusiness", "328_SenseForBusiness");
		addStartNpc(30436);
		addKillId(20068);
		addKillId(20070);
		addKillId(20067);
		addKillId(20055);
		addKillId(20059);
		addKillId(20072);
		registerQuestItems(1347, 1366, 1348);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
            string2 = "trader_salient_q0328_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "trader_salient_q0328_06.htm";
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "trader_salient_q0328_07.htm";
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
                if (n != 30436) break;
                if (pc.getLevel() >= 21) {
                    html = "trader_salient_q0328_02.htm";
                    break;
                }
                html = "trader_salient_q0328_01.htm";
                break;
            }
            case 2: {
                if (n != 30436) break;
                if (qs.getQuestItemsCount(1347) + qs.getQuestItemsCount(1366) + qs.getQuestItemsCount(1348) > 0L) {
                    if (qs.getQuestItemsCount(1347) + qs.getQuestItemsCount(1366) + qs.getQuestItemsCount(1348) >= 10L) {
                        qs.giveItems(57, 618L + 25L * qs.getQuestItemsCount(1347) + 1000L * qs.getQuestItemsCount(1366) + 60L * qs.getQuestItemsCount(1348));
                    } else {
                        qs.giveItems(57, 25L * qs.getQuestItemsCount(1347) + 1000L * qs.getQuestItemsCount(1366) + 60L * qs.getQuestItemsCount(1348));
                    }
                    qs.takeItems(1347, -1L);
                    qs.takeItems(1366, -1L);
                    qs.takeItems(1348, -1L);
                    html = "trader_salient_q0328_05.htm";
                    break;
                }
                html = "trader_salient_q0328_04.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = ThreadLocalRandom.current().nextInt(1, 100 + 1);
        if (n == 20055) {
            if (n2 < 47) {
                qs.giveItems(1347, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 49) {
                qs.giveItems(1366, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20059) {
            if (n2 < 51) {
                qs.giveItems(1347, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 53) {
                qs.giveItems(1366, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20067) {
            if (n2 < 67) {
                qs.giveItems(1347, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 69) {
                qs.giveItems(1366, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20068) {
            if (n2 < 75) {
                qs.giveItems(1347, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 77) {
                qs.giveItems(1366, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20070) {
            if (n2 < 50) {
                qs.giveItems(1348, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20072 && n2 < 51) {
            qs.giveItems(1348, 1L);
            qs.playSound("QuestState.SOUND_ITEMGET");
        }
        return null;
    
	}

}
