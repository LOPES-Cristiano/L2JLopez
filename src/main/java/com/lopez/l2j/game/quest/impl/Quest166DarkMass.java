package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 166 - Dark Mass
 */
@Component
public class Quest166DarkMass extends Quest {



	public Quest166DarkMass(QuestManager questManager) {
		super(166, "166_DarkMass", "Dark Mass");
		addStartNpc(30130);
		addTalkId(30135, 30139, 30143);
		registerQuestItems(1088, 1089, 1090, 1091);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.giveItems((int)(1088), (int)(1));
            string2 = "undres_q0322_04.htm";
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
                if (n2 != 30130) break;
                if (pc.getRace() != Race.darkelf) {
                    html = "undres_q0322_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 2) {
                    html = "undres_q0322_03.htm";
                    break;
                }
                html = "undres_q0322_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 30130) {
                    if (qs.getQuestItemsCount((int)(1088)) == 1 && (qs.getQuestItemsCount((int)(1091)) < 1 || qs.getQuestItemsCount((int)(1090)) < 1 || qs.getQuestItemsCount((int)(1089)) < 1)) {
                        html = "undres_q0322_05.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1088)) != 1 || qs.getQuestItemsCount((int)(1089)) != 1 || qs.getQuestItemsCount((int)(1090)) != 1 || qs.getQuestItemsCount((int)(1091)) != 1) break;
                    html = "undres_q0322_06.htm";
                    qs.takeItems((int)(1089), (int)(1));
                    qs.takeItems((int)(1090), (int)(1));
                    qs.takeItems((int)(1091), (int)(1));
                    qs.takeItems((int)(1088), (int)(1));
                    qs.giveItems((int)(57), (int)(500));
                    if (1 == 1 && !false) {
                        
                    }
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(false);
                    break;
                }
                if (n2 == 30135) {
                    if (qs.getQuestItemsCount((int)(1088)) != 1 || qs.getQuestItemsCount((int)(1089)) != 0) break;
                    qs.giveItems((int)(1089), (int)(1));
                    html = "iria_q0322_01.htm";
                    if (qs.getQuestItemsCount((int)(1090)) <= 0 || qs.getQuestItemsCount((int)(1091)) <= 0) break;
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    break;
                }
                if (n2 == 30139) {
                    if (qs.getQuestItemsCount((int)(1088)) != 1 || qs.getQuestItemsCount((int)(1090)) != 0) break;
                    qs.giveItems((int)(1090), (int)(1));
                    html = "doran_q0322_01.htm";
                    if (qs.getQuestItemsCount((int)(1089)) <= 0 || qs.getQuestItemsCount((int)(1091)) <= 0) break;
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    break;
                }
                if (n2 != 30143 || qs.getQuestItemsCount((int)(1088)) != 1 || qs.getQuestItemsCount((int)(1091)) != 0) break;
                qs.giveItems((int)(1091), (int)(1));
                html = "trudy_q0322_01.htm";
                if (qs.getQuestItemsCount((int)(1089)) <= 0 || qs.getQuestItemsCount((int)(1090)) <= 0) break;
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
        }
        return html;
    
	}


}
