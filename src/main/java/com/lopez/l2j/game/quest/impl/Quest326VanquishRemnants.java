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
 * Quest 326 - 326_VanquishRemnants
 */
@Component
public class Quest326VanquishRemnants extends Quest {

	public static final int bgm = 30435;
	public static final int bgn = 20053;
	public static final int bgo = 20058;
	public static final int bgp = 20061;
	public static final int bgq = 20063;
	public static final int aJe = 20066;
	public static final int bgr = 20436;
	public static final int bgs = 20437;
	public static final int aJd = 20438;
	public static final int bgt = 20439;
	public static final int bgu = 1359;
	public static final int bgv = 1360;
	public static final int bgw = 1361;
	public static final int bgx = 1369;

	public Quest326VanquishRemnants(QuestManager questManager) {
		super(326, "326_VanquishRemnants", "326_VanquishRemnants");
		addStartNpc(30435);
		addKillId(20061);
		addKillId(20436);
		addKillId(20053);
		addKillId(20058);
		addKillId(20437);
		addKillId(20063);
		addKillId(20439);
		addKillId(20438);
		addKillId(20066);
		registerQuestItems(1359, 1360, 1361);
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
        if (n == 30435) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("vanquish_remnants", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound("QuestState.SOUND_ACCEPT");
                string2 = "leopold_q0326_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                qs.unset("vanquish_remnants");
                qs.playSound("QuestState.SOUND_FINISH");
                qs.exitQuest(true);
                string2 = "leopold_q0326_07.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "leopold_q0326_08.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("vanquish_remnants");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30435) break;
                if (pc.getLevel() < 21 || pc.getLevel() > 30) {
                    html = "leopold_q0326_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "leopold_q0326_02.htm";
                break;
            }
            case 2: {
                if (n2 != 30435 || n != 1) break;
                if (qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) == 0L) {
                    html = "leopold_q0326_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) < 100L && qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) > 0L) {
                    if (qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) >= 10L) {
                        qs.giveItems(57, 4320L + 46L * qs.getQuestItemsCount(1359) + 52L * qs.getQuestItemsCount(1360) + 58L * qs.getQuestItemsCount(1361));
                    } else {
                        qs.giveItems(57, 46L * qs.getQuestItemsCount(1359) + 52L * qs.getQuestItemsCount(1360) + 58L * qs.getQuestItemsCount(1361));
                    }
                    qs.takeItems(1359, -1L);
                    qs.takeItems(1360, -1L);
                    qs.takeItems(1361, -1L);
                    html = "leopold_q0326_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) >= 100L && qs.getQuestItemsCount(1369) == 0L) {
                    qs.giveItems(1369, 1L);
                    qs.giveItems(57, 4320L + 46L * qs.getQuestItemsCount(1359) + 52L * qs.getQuestItemsCount(1360) + 58L * qs.getQuestItemsCount(1361));
                    qs.takeItems(1359, -1L);
                    qs.takeItems(1360, -1L);
                    qs.takeItems(1361, -1L);
                    html = "leopold_q0326_06.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1359) + qs.getQuestItemsCount(1360) + qs.getQuestItemsCount(1361) < 100L || qs.getQuestItemsCount(1369) <= 0L) break;
                qs.giveItems(57, 4320L + 46L * qs.getQuestItemsCount(1359) + 52L * qs.getQuestItemsCount(1360) + 58L * qs.getQuestItemsCount(1361));
                qs.takeItems(1359, -1L);
                qs.takeItems(1360, -1L);
                qs.takeItems(1361, -1L);
                html = "leopold_q0326_09.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("vanquish_remnants");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 20053 || n2 == 20058) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 61) {
                qs.rollAndGive(1359, 1, 100.0);
            }
        } else if (n2 == 20061) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 57) {
                qs.rollAndGive(1360, 1, 100.0);
            }
        } else if (n2 == 20063) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 63) {
                qs.rollAndGive(1360, 1, 100.0);
            }
        } else if (n2 == 20066) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 59) {
                qs.rollAndGive(1361, 1, 100.0);
            }
        } else if (n2 == 20436) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 55) {
                qs.rollAndGive(1360, 1, 100.0);
            }
        } else if (n2 == 20437) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 59) {
                qs.rollAndGive(1359, 1, 100.0);
            }
        } else if (n2 == 20438) {
            if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 60) {
                qs.rollAndGive(1361, 1, 100.0);
            }
        } else if (n2 == 20439 && n == 1 && ThreadLocalRandom.current().nextInt(100) < 62) {
            qs.rollAndGive(1360, 1, 100.0);
        }
        return null;
    
	}

}
