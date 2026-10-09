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
 * Quest 338 - 338_AlligatorHunter
 */
@Component
public class Quest338AlligatorHunter extends Quest {

	public static final int bmL = 30892;
	public static final int aBt = 20135;
	public static final int bmM = 4337;

	public Quest338AlligatorHunter(QuestManager questManager) {
	super(338, "338_AlligatorHunter", "338_AlligatorHunter");
		this.addStartNpc(30892);
		this.addKillId(20135);
		this.addQuestItem(4337);
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
        int n = qs.getInt("crocodile_hunter_ex");
        int n2 = getFirstStartNpc();
        if (n2 == 30892) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("crocodile_hunter", String.valueOf(1), true);
                qs.set("crocodile_hunter_ex", String.valueOf(0), true);
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.setState(State.STARTED);
                string2 = "trader_enverun_q0338_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                if (qs.getQuestItemsCount(4337) == 0L) {
                    string2 = "trader_enverun_q0338_05.htm";
                } else if (qs.getQuestItemsCount(4337) > 0L) {
                    if (qs.getQuestItemsCount(4337) >= 10L) {
                        qs.giveItems(57, 3430L + qs.getQuestItemsCount(4337) * 60L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(4337) * 60L);
                    }
                    qs.set("crocodile_hunter_ex", String.valueOf((long)n + qs.getQuestItemsCount(4337)), true);
                    qs.takeItems(4337, -1L);
                    string2 = "trader_enverun_q0338_06.htm";
                }
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "trader_enverun_q0338_11.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = "trader_enverun_q0338_12.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = "trader_enverun_q0338_15.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                if (n >= 10) {
                    qs.unset("crocodile_hunter");
                    qs.unset("crocodile_hunter_ex");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "trader_enverun_q0338_16.htm";
                } else {
                    qs.unset("crocodile_hunter");
                    qs.unset("crocodile_hunter_ex");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "trader_enverun_q0338_16t.htm";
                }
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("crocodile_hunter");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30892) break;
                if (pc.getLevel() < 40 || pc.getLevel() > 47) {
                    html = "trader_enverun_q0338_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "trader_enverun_q0338_02.htm";
                break;
            }
            case 2: {
                if (n2 != 30892 || n != 1) break;
                html = "trader_enverun_q0338_04.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("crocodile_hunter");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 20135 && n == 1) {
            qs.giveItems(4337, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
            if (ThreadLocalRandom.current().nextInt(100) < 19) {
                qs.giveItems(4337, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
