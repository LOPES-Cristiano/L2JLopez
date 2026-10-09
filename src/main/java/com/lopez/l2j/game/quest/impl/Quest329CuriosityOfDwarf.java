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
 * Quest 329 - 329_CuriosityOfDwarf
 */
@Component
public class Quest329CuriosityOfDwarf extends Quest {

	public static final int bgQ = 1346;
	public static final int bgR = 1365;
	public static final int aIK = 30437;
	public static final int bgS = 20083;
	public static final int bgT = 20085;

	public Quest329CuriosityOfDwarf(QuestManager questManager) {
		super(329, "329_CuriosityOfDwarf", "329_CuriosityOfDwarf");
		addStartNpc(30437);
		addKillId(20083);
		addKillId(20085);
		registerQuestItems(1365, 1346);
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
            string2 = "trader_rolento_q0329_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "trader_rolento_q0329_06.htm";
            qs.exitQuest(true);
            qs.playSound("QuestState.SOUND_FINISH");
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "trader_rolento_q0329_07.htm";
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
                if (n != 30437) break;
                if (pc.getLevel() >= 33) {
                    html = "trader_rolento_q0329_02.htm";
                    break;
                }
                html = "trader_rolento_q0329_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n != 30437) break;
                if (qs.getQuestItemsCount(1365) + qs.getQuestItemsCount(1346) > 0L) {
                    if (qs.getQuestItemsCount(1365) + qs.getQuestItemsCount(1346) >= 10L) {
                        qs.giveItems(57, 1183L + 50L * qs.getQuestItemsCount(1365) + 1000L * qs.getQuestItemsCount(1346));
                    } else {
                        qs.giveItems(57, 50L * qs.getQuestItemsCount(1365) + 1000L * qs.getQuestItemsCount(1346));
                    }
                    qs.takeItems(1365, -1L);
                    qs.takeItems(1346, -1L);
                    html = "trader_rolento_q0329_05.htm";
                    break;
                }
                html = "trader_rolento_q0329_04.htm";
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
        if (n == 20085) {
            if (n2 < 5) {
                qs.giveItems(1346, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 58) {
                qs.giveItems(1365, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20083) {
            if (n2 < 6) {
                qs.giveItems(1346, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 56) {
                qs.giveItems(1365, 1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        }
        return null;
    
	}

}
