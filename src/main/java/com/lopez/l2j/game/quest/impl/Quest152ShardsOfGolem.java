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
 * Quest 152 - Shards Of Golem
 */
@Component
public class Quest152ShardsOfGolem extends Quest {



	public Quest152ShardsOfGolem(QuestManager questManager) {
		super(152, "152_ShardsOfGolem", "Shards Of Golem");
		addStartNpc(30035);
		addTalkId(30283);
		addKillId(20016);
		registerQuestItems(1008, 1009, 1010, 1011);
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
        if (n == 30035) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                if (qs.getQuestItemsCount((int)(1008)) == 0) {
                    qs.giveItems((int)(1008), (int)(1));
                }
                string2 = "harry_q0152_04.htm";
            }
        } else if (n == 30283 && event.equalsIgnoreCase("reply=2") && qs.getQuestItemsCount((int)(1008)) > 0) {
            qs.setCond(2);
            qs.takeItems((int)(1008), (int)(-1));
            qs.playSound(QuestState.SOUND_MIDDLE);
            if (qs.getQuestItemsCount((int)(1009)) == 0) {
                qs.giveItems((int)(1009), (int)(1));
            }
            string2 = "blacksmith_alltran_q0152_02.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = npc.getNpcId();
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30035) break;
                if (pc.level() < 10 || pc.level() > 17) {
                    html = "harry_q0152_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "harry_q0152_03.htm";
                break;
            }
            case 2: {
                if (n == 30035) {
                    if (qs.getQuestItemsCount((int)(1008)) != 0 && qs.getQuestItemsCount((int)(1011)) == 0) {
                        html = "harry_q0152_05a.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1009)) != 0 && qs.getQuestItemsCount((int)(1011)) == 0) {
                        html = "harry_q0152_05.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1011)) == 0) break;
                    qs.takeItems((int)(1011), (int)(-1));
                    qs.takeItems((int)(1009), (int)(-1));
                    qs.giveItems((int)(23), (int)(1));
                    qs.addExpAndSp(5000, 0);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(false);
                    html = "harry_q0152_06.htm";
                    break;
                }
                if (n != 30283) break;
                if (qs.getQuestItemsCount((int)(1008)) != 0) {
                    html = "blacksmith_alltran_q0152_01.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1009)) != 0 && qs.getQuestItemsCount((int)(1010)) < 5 && qs.getQuestItemsCount((int)(1011)) == 0) {
                    html = "blacksmith_alltran_q0152_03.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1009)) != 0 && qs.getQuestItemsCount((int)(1010)) >= 5 && qs.getQuestItemsCount((int)(1011)) == 0) {
                    qs.setCond(4);
                    qs.takeItems((int)(1010), (int)(-1));
                    if (qs.getQuestItemsCount((int)(1011)) == 0) {
                        qs.giveItems((int)(1011), (int)(1));
                    }
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "blacksmith_alltran_q0152_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1009)) == 0 || qs.getQuestItemsCount((int)(1011)) == 0) break;
                html = "blacksmith_alltran_q0152_05.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        if (n == 20016 && qs.getQuestItemsCount((int)(1010)) < 5 && ThreadLocalRandom.current().nextInt(100) < 30) {
            qs.giveItems((int)(1010), (int)(1));
            if (qs.getQuestItemsCount((int)(1010)) >= 5) {
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
