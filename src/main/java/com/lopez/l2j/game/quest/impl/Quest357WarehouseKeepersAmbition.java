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
 * Quest 357 - 357_WarehouseKeepersAmbition
 */
@Component
public class Quest357WarehouseKeepersAmbition extends Quest {

	public static final int brj = 30686;
	public static final int brk = 20594;
	public static final int brl = 20595;
	public static final int brm = 20596;
	public static final int brn = 20597;
	public static final int bro = 5867;

	public Quest357WarehouseKeepersAmbition(QuestManager questManager) {
	super(357, "357_WarehouseKeepersAmbition", "357_WarehouseKeepersAmbition");
		this.addStartNpc(30686);
		this.addKillId(20594, 20595, 20596, 20597);
		this.addQuestItem(5867);
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
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "warehouse_keeper_silva_q0357_05.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "warehouse_keeper_silva_q0357_03.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "warehouse_keeper_silva_q0357_04.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            if (qs.getQuestItemsCount(5867) < 100L && qs.getQuestItemsCount(5867) > 0L) {
                qs.giveItems(57, qs.getQuestItemsCount(5867) * 425L + 13500L);
                qs.takeItems(5867, qs.getQuestItemsCount(5867));
                string2 = "warehouse_keeper_silva_q0357_08.htm";
            } else if (qs.getQuestItemsCount(5867) >= 100L) {
                qs.giveItems(57, qs.getQuestItemsCount(5867) * 425L + 40500L);
                qs.takeItems(5867, qs.getQuestItemsCount(5867));
                string2 = "warehouse_keeper_silva_q0357_09.htm";
            }
        } else if (event.equalsIgnoreCase("reply_4")) {
            string2 = "warehouse_keeper_silva_q0357_10.htm";
        } else if (event.equalsIgnoreCase("reply_5")) {
            if (qs.getQuestItemsCount(5867) < 100L && qs.getQuestItemsCount(5867) > 0L) {
                qs.giveItems(57, qs.getQuestItemsCount(5867) * 425L);
            } else if (qs.getQuestItemsCount(5867) >= 100L) {
                qs.giveItems(57, qs.getQuestItemsCount(5867) * 425L + 40500L);
            }
            qs.takeItems(5867, qs.getQuestItemsCount(5867));
            string2 = "warehouse_keeper_silva_q0357_11.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        switch (n) {
            case 1: {
                if (pc.getLevel() < 47) {
                    html = "warehouse_keeper_silva_q0357_01.htm";
                    break;
                }
                if (pc.getLevel() < 47) break;
                html = "warehouse_keeper_silva_q0357_02.htm";
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount(5867) < 1L) {
                    html = "warehouse_keeper_silva_q0357_06.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5867) < 1L) break;
                html = "warehouse_keeper_silva_q0357_07.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20594 && ThreadLocalRandom.current().nextInt(1000) < 577) {
            qs.rollAndGive(5867, 1, 100.0);
        } else if (n == 20595 && ThreadLocalRandom.current().nextInt(100) < 60) {
            qs.rollAndGive(5867, 1, 100.0);
        } else if (n == 20596 && ThreadLocalRandom.current().nextInt(1000) < 638) {
            qs.rollAndGive(5867, 1, 100.0);
        } else if (n == 20597) {
            if (ThreadLocalRandom.current().nextInt(1000) < 62) {
                qs.rollAndGive(5867, 2, 100.0);
            } else {
                qs.rollAndGive(5867, 1, 100.0);
            }
        }
        return null;
    
	}

}
