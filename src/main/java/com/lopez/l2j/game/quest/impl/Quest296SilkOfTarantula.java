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
 * Quest 296 - 296_SilkOfTarantula
 */
@Component
public class Quest296SilkOfTarantula extends Quest {

	public static final int beQ = 30519;
	public static final int beR = 30548;
	public static final int beS = 20394;
	public static final int beT = 20403;
	public static final int beU = 20508;
	public static final int beV = 1493;
	public static final int beW = 1494;
	public static final int beX = 1508;
	public static final int beY = 1509;

	public Quest296SilkOfTarantula(QuestManager questManager) {
		super(296, "296_SilkOfTarantula", "296_SilkOfTarantula");
		addStartNpc(30519);
		addTalkNpc(30548);
		addKillId(20508);
		addKillId(20403);
		addKillId(20394);
		registerQuestItems(1493, 1494);
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
        if (n == 30519) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound("QuestState.SOUND_ACCEPT");
                string2 = "trader_mion_q0296_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                qs.takeItems(1494, -1L);
                string2 = "trader_mion_q0296_06.htm";
                qs.exitQuest(true);
                qs.playSound("QuestState.SOUND_FINISH");
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "trader_mion_q0296_07.htm";
            }
        } else if (n == 30548 && event.equalsIgnoreCase("reply_3")) {
            if (qs.getQuestItemsCount(1494) >= 1L) {
                string2 = "defender_nathan_q0296_03.htm";
                qs.giveItems(1493, (long)(15 + ThreadLocalRandom.current().nextInt(9)) * qs.getQuestItemsCount(1494));
                qs.takeItems(1494, -1L);
            } else {
                string2 = "defender_nathan_q0296_02.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        PlayerCharacter player = pc;
        if (player == null) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30519) break;
                if (player.getLevel() < 15) {
                    html = "trader_mion_q0296_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs.getQuestItemsCount(1508) > 0L || qs.getQuestItemsCount(1509) > 0L) {
                    html = "trader_mion_q0296_02.htm";
                    break;
                }
                html = "trader_mion_q0296_08.htm";
                break;
            }
            case 2: {
                if (n == 30519) {
                    if (qs.getQuestItemsCount(1493) < 1L) {
                        html = "trader_mion_q0296_04.htm";
                    } else if (qs.getQuestItemsCount(1493) >= 1L) {
                        html = "trader_mion_q0296_05.htm";
                        if (qs.getQuestItemsCount(1493) >= 10L) {
                            qs.giveItems(57, qs.getQuestItemsCount(1493) * 30L + 2000L);
                        } else {
                            qs.giveItems(57, qs.getQuestItemsCount(1493) * 30L);
                        }
                        qs.takeItems(1493, -1L);
                        qs.playSound("QuestState.SOUND_FINISH");
                        qs.exitQuest(true);
                    }
                }
                if (n != 30548) break;
                html = "defender_nathan_q0296_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20394 || n == 20403 || n == 20508) {
            int n2 = ThreadLocalRandom.current().nextInt(100);
            if (n2 > 95) {
                qs.rollAndGive(1494, 1, 100.0);
            } else if (n2 > 45) {
                qs.rollAndGive(1493, 1, 100.0);
            }
        }
        return null;
    
	}

}
