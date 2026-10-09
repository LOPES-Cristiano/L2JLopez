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
 * Quest 317 - 317_CatchTheWind
 */
@Component
public class Quest317CatchTheWind extends Quest {

	public static final int bfD = 30361;
	public static final int bfE = 20036;
	public static final int bfF = 20044;
	public static final int bfG = 1078;

	public Quest317CatchTheWind(QuestManager questManager) {
		super(317, "317_CatchTheWind", "317_CatchTheWind");
		addStartNpc(30361);
		addKillId(20036);
		addKillId(20044);
		registerQuestItems(1078);
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
            string2 = "rizraell_q0317_04.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            if (qs.getQuestItemsCount(1078) > 0L) {
                if (qs.getQuestItemsCount(1078) >= 10L) {
                    qs.giveItems(57, 2988L + 40L * qs.getQuestItemsCount(1078));
                } else {
                    qs.giveItems(57, 40L * qs.getQuestItemsCount(1078));
                }
            }
            qs.takeItems(1078, -1L);
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
            string2 = "rizraell_q0317_08.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            if (qs.getQuestItemsCount(1078) > 0L) {
                if (qs.getQuestItemsCount(1078) >= 10L) {
                    qs.giveItems(57, 2988L + 40L * qs.getQuestItemsCount(1078));
                } else {
                    qs.giveItems(57, 40L * qs.getQuestItemsCount(1078));
                }
            }
            qs.takeItems(1078, -1L);
            string2 = "rizraell_q0317_09.htm";
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
                if (n != 30361) break;
                if (pc.getLevel() < 18 || pc.getLevel() > 23) {
                    html = "rizraell_q0317_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "rizraell_q0317_03.htm";
                break;
            }
            case 2: {
                if (n != 30361) break;
                if (qs.getQuestItemsCount(1078) == 0L) {
                    html = "rizraell_q0317_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount(1078) == 0L) break;
                html = "rizraell_q0317_07.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = ThreadLocalRandom.current().nextInt(100);
        if (n < 50) {
            qs.rollAndGive(1078, 1, 100.0);
        }
        return null;
    
	}

}
