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
 * Quest 331 - 331_ArrowForVengeance
 */
@Component
public class Quest331ArrowForVengeance extends Quest {

	public static final int bhN = 1452;
	public static final int bhO = 1453;
	public static final int bhP = 1454;
	public static final int bhQ = 20145;
	public static final int bhR = 20158;
	public static final int bhS = 20176;
	public static final int bhT = 30125;

	public Quest331ArrowForVengeance(QuestManager questManager) {
		super(331, "331_ArrowForVengeance", "331_ArrowForVengeance");
		addStartNpc(30125);
		addKillId(20158);
		addKillId(20145);
		addKillId(20176);
		registerQuestItems(1452, 1453, 1454);
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
            string2 = "beltkem_q0331_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "beltkem_q0331_06.htm";
            qs.exitQuest(true);
            qs.playSound("QuestState.SOUND_FINISH");
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "beltkem_q0331_07.htm";
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
                if (n != 30125) break;
                if (pc.getLevel() >= 32) {
                    html = "beltkem_q0331_02.htm";
                    break;
                }
                html = "beltkem_q0331_01.htm";
                break;
            }
            case 2: {
                if (n != 30125) break;
                if (qs.getQuestItemsCount(1452) + qs.getQuestItemsCount(1453) + qs.getQuestItemsCount(1454) > 0L) {
                    if (qs.getQuestItemsCount(1452) + qs.getQuestItemsCount(1453) + qs.getQuestItemsCount(1454) >= 10L) {
                        qs.giveItems(57, 3100L + 78L * qs.getQuestItemsCount(1452) + 88L * qs.getQuestItemsCount(1453) + 92L * qs.getQuestItemsCount(1454));
                    } else {
                        qs.giveItems(57, 78L * qs.getQuestItemsCount(1452) + 88L * qs.getQuestItemsCount(1453) + 92L * qs.getQuestItemsCount(1454));
                    }
                    qs.takeItems(1452, -1L);
                    qs.takeItems(1453, -1L);
                    qs.takeItems(1454, -1L);
                    html = "beltkem_q0331_05.htm";
                    break;
                }
                html = "beltkem_q0331_04.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20145 && ThreadLocalRandom.current().nextInt(100) < 59) {
            qs.rollAndGive(1452, 1, 100.0);
        } else if (n == 20158 && ThreadLocalRandom.current().nextInt(100) < 61) {
            qs.rollAndGive(1453, 1, 100.0);
        } else if (n == 20176 && ThreadLocalRandom.current().nextInt(100) < 60) {
            qs.rollAndGive(1454, 1, 100.0);
        }
        return null;
    
	}

}
