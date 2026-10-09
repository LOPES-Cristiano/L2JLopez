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
 * Quest 160 - Nerupas Favor
 */
@Component
public class Quest160NerupasFavor extends Quest {



	public Quest160NerupasFavor(QuestManager questManager) {
		super(160, "160_NerupasFavor", "Nerupas Favor");
		addStartNpc(30370);
		addTalkId(30147, 30149, 30152);
		registerQuestItems(1026, 1027, 1028, 1029);
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
            if (qs.getQuestItemsCount((int)(1026)) == 0) {
                qs.giveItems((int)(1026), (int)(1));
            }
            string2 = "nerupa_q0311_04.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        PlayerCharacter player = pc;
        int n = npc.getNpcId();
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30370) break;
                if (pc.getRace() != Race.elf) {
                    html = "nerupa_q0311_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 3) {
                    html = "nerupa_q0311_03.htm";
                    break;
                }
                html = "nerupa_q0311_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n == 30370) {
                    if (qs.getQuestItemsCount((int)(1026)) != 0 || qs.getQuestItemsCount((int)(1027)) != 0 || qs.getQuestItemsCount((int)(1028)) != 0) {
                        html = "nerupa_q0311_05.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1029)) == 0) break;
                    qs.takeItems((int)(1029), (int)(qs.getQuestItemsCount((int)(1029))));
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.giveItems((int)(1060), (int)(5));
                    qs.addExpAndSp(1000, 0);
                    qs.exitQuest(false);
                    html = "nerupa_q0311_06.htm";
                    break;
                }
                if (n == 30147) {
                    if (qs.getQuestItemsCount((int)(1026)) != 0) {
                        qs.takeItems((int)(1026), (int)(-1));
                        if (qs.getQuestItemsCount((int)(1027)) == 0) {
                            qs.giveItems((int)(1027), (int)(1));
                        }
                        html = "uno_q0311_01.htm";
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1027)) != 0) {
                        html = "uno_q0311_02.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1029)) == 0) break;
                    html = "uno_q0311_03.htm";
                    break;
                }
                if (n == 30149) {
                    if (qs.getQuestItemsCount((int)(1027)) != 0) {
                        qs.takeItems((int)(1027), (int)(-1));
                        if (qs.getQuestItemsCount((int)(1028)) == 0) {
                            qs.giveItems((int)(1028), (int)(1));
                        }
                        html = "cel_q0311_01.htm";
                        qs.setCond(3);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1028)) != 0) {
                        html = "cel_q0311_02.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1029)) == 0) break;
                    html = "cel_q0311_03.htm";
                    break;
                }
                if (n != 30152) break;
                if (qs.getQuestItemsCount((int)(1028)) != 0) {
                    qs.takeItems((int)(1028), (int)(-1));
                    if (qs.getQuestItemsCount((int)(1029)) != 0) break;
                    qs.giveItems((int)(1029), (int)(1));
                    html = "jud_q0311_01.htm";
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    break;
                }
                if (qs.getQuestItemsCount((int)(1029)) == 0) break;
                html = "jud_q0311_02.htm";
            }
        }
        return html;
    
	}


}
