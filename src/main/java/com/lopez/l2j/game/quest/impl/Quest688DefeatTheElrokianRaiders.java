package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.Clan;
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
 * Quest 688 - 688_DefeatTheElrokianRaiders
 */
@Component
public class Quest688DefeatTheElrokianRaiders extends Quest {

	public static final int bSx = 32105;
	public static final int bVP = 22214;
	public static final int bVQ = 8785;

	public Quest688DefeatTheElrokianRaiders(QuestManager questManager) {
	super(688, "688_DefeatTheElrokianRaiders", "688_DefeatTheElrokianRaiders");
		this.addStartNpc(32105);
		this.addKillId(22214);
		this.addQuestItem(bVQ);
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
        if (n == 32105) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("repulse_the_elcroki", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "dindin_q0688_04.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "dindin_q0688_03.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                if (qs.getQuestItemsCount(bVQ) >= 1L) {
                    if (qs.getQuestItemsCount(bVQ) >= 10L) {
                        qs.giveItems(57, qs.getQuestItemsCount(bVQ) * 3000L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(bVQ) * 3000L);
                    }
                    qs.takeItems(bVQ, -1L);
                    string2 = "dindin_q0688_07.htm";
                }
            } else if (event.equalsIgnoreCase("reply_7")) {
                if (qs.getQuestItemsCount(bVQ) >= 1L) {
                    qs.giveItems(57, qs.getQuestItemsCount(bVQ) * 3000L);
                    qs.unset("repulse_the_elcroki");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dindin_q0688_08.htm";
                } else {
                    qs.unset("repulse_the_elcroki");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dindin_q0688_09.htm";
                }
            } else if (event.equalsIgnoreCase("reply_8")) {
                string2 = "dindin_q0688_10.htm";
            } else if (event.equalsIgnoreCase("reply_9")) {
                if (qs.getQuestItemsCount(bVQ) < 100L) {
                    string2 = "dindin_q0688_11.htm";
                }
                if (qs.getQuestItemsCount(bVQ) >= 100L) {
                    if (ThreadLocalRandom.current().nextInt(1000) < 500) {
                        qs.giveItems(57, 450000L);
                        qs.takeItems(bVQ, 100L);
                        string2 = "dindin_q0688_12.htm";
                    } else {
                        qs.giveItems(57, 150000L);
                        string2 = "dindin_q0688_13.htm";
                        qs.takeItems(bVQ, 100L);
                    }
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
        int n = qs.getInt("repulse_the_elcroki");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 32105) break;
                if (pc.getLevel() < 75) {
                    html = "dindin_q0688_02.htm";
                    break;
                }
                qs.exitQuest(true);
                html = "dindin_q0688_01.htm";
                break;
            }
            case 2: {
                if (n2 != 32105 || n != 1) break;
                if (qs.getQuestItemsCount(bVQ) >= 1L) {
                    html = "dindin_q0688_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount(bVQ) >= 1L) break;
                html = "dindin_q0688_06.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        int n2 = qs.getInt("repulse_the_elcroki");
        int n3 = npc.getNpcId();
        if (n2 == 1 && n3 == 22214 && (n = ThreadLocalRandom.current().nextInt(1000)) < 448) {
            qs.giveItems(bVQ, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
