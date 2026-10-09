package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 167 - Dwarven Kinship
 */
@Component
public class Quest167DwarvenKinship extends Quest {



	public Quest167DwarvenKinship(QuestManager questManager) {
		super(167, "167_DwarvenKinship", "Dwarven Kinship");
		addStartNpc(30350);
		addTalkId(30255, 30210);
		registerQuestItems(1076, 1106);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

        String string2 = event;
        int n = 0;
        if (n == 30350) {
            if (event.equalsIgnoreCase("quest_accept")) {
                string2 = "calculain_q0323_04.htm";
                qs.giveItems((int)(1076), (int)(1));
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.setCond(1);
                qs.setState(State.STARTED);
            }
        } else if (n == 30255) {
            if (event.equalsIgnoreCase("reply_1") && qs.getCond() == 1) {
                string2 = "harprock_q0323_03.htm";
                qs.takeItems((int)(1076), (int)(1));
                qs.giveItems((int)(1106), (int)(1));
                qs.giveItems((int)(57), (int)(2000));
                qs.setCond(2);
            } else if (event.equalsIgnoreCase("reply_2") && qs.getQuestItemsCount((int)(1076)) == 1) {
                qs.takeItems((int)(1076), (int)(1));
                string2 = "harprock_q0323_04.htm";
                qs.giveItems((int)(57), (int)(15000));
                qs.exitQuest(false);
                qs.playSound(QuestState.SOUND_FINISH);
            }
        } else if (n == 30210 && event.equalsIgnoreCase("reply_1") && qs.getQuestItemsCount((int)(1106)) == 1) {
            string2 = "warehouse_keeper_norman_q0323_02.htm";
            qs.takeItems((int)(1106), (int)(1));
            qs.giveItems((int)(57), (int)(20000));
            qs.exitQuest(false);
            qs.playSound(QuestState.SOUND_FINISH);
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = qs.getStateId();
        PlayerCharacter player = pc;
        int n2 = npc.getNpcId();
        switch (n) {
            case 1: {
                if (n2 == 30350 && pc.level() >= 15) {
                    html = "calculain_q0323_03.htm";
                    break;
                }
                html = "calculain_q0323_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 30350) {
                    if (qs.getQuestItemsCount((int)(1076)) != 1) break;
                    html = "calculain_q0323_05.htm";
                    break;
                }
                if (n2 == 30255) {
                    if (qs.getQuestItemsCount((int)(1076)) == 1) {
                        html = "harprock_q0323_01.htm";
                    }
                    if (qs.getQuestItemsCount((int)(1106)) != 1) break;
                    html = "harprock_q0323_05.htm";
                    break;
                }
                if (n2 != 30210 || qs.getQuestItemsCount((int)(1106)) != 1) break;
                html = "warehouse_keeper_norman_q0323_01.htm";
            }
        }
        return html;
    
	}


}
