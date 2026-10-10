package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
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
 * Quest 654 - 654_JourneytoaSettlement
 */
@Component
public class Quest654JourneytoaSettlement extends Quest {

	public static final int aFO = 31453;
	public static final int bUe = 21294;
	public static final int bUf = 21295;
	public static final int bUg = 8072;
	public static final int bUh = 8073;

	public Quest654JourneytoaSettlement(QuestManager questManager) {
	super(654, "654_JourneytoaSettlement", "654_JourneytoaSettlement");
		this.addStartNpc(31453);
		this.addKillId(21294, 21295);
		this.addQuestItem(8072);
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
        int n = qs.getInt("to_reach_an_ending");
        int n2 = getFirstStartNpc();
        if (n2 == 31453) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("to_reach_an_ending", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "printessa_spirit_q0654_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                if (n == 1) {
                    qs.setCond(2);
                    qs.set("to_reach_an_ending", String.valueOf(2), true);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "printessa_spirit_q0654_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_2") && n == 2 && qs.getQuestItemsCount(8072) >= 1L) {
                qs.giveItems(8073, 1L);
                qs.takeItems(8072, -1L);
                qs.unset("to_reach_an_ending");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "printessa_spirit_q0654_07.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        QuestState questState2 = qs.getQuestState("119_LastImperialPrince");
        int n = qs.getInt("to_reach_an_ending");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31453) break;
                if (pc.getLevel() >= 74 && questState2 != null && questState2.isCompleted()) {
                    html = "printessa_spirit_q0654_01.htm";
                    break;
                }
                qs.exitQuest(true);
                html = "printessa_spirit_q0654_02.htm";
                break;
            }
            case 2: {
                if (n2 != 31453) break;
                if (n == 1) {
                    qs.setCond(2);
                    qs.set("to_reach_an_ending", String.valueOf(2), true);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "printessa_spirit_q0654_04.htm";
                    break;
                }
                if (n == 2 && qs.getQuestItemsCount(8072) == 0L) {
                    html = "printessa_spirit_q0654_05.htm";
                    break;
                }
                if (n != 2 || qs.getQuestItemsCount(8072) < 1L) break;
                html = "printessa_spirit_q0654_06.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("to_reach_an_ending");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 2 && qs.getQuestItemsCount(8072) == 0L) {
            if (n2 == 21294) {
                if (ThreadLocalRandom.current().nextInt(100) < 84) {
                    qs.setCond(3);
                    qs.giveItems(8072, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21295 && ThreadLocalRandom.current().nextInt(1000) < 893) {
                qs.setCond(3);
                qs.giveItems(8072, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
