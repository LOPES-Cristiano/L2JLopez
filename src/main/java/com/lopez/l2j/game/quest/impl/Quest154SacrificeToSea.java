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
 * Quest 154 - Sacrifice To Sea
 */
@Component
public class Quest154SacrificeToSea extends Quest {



	public Quest154SacrificeToSea(QuestManager questManager) {
		super(154, "154_SacrificeToSea", "Sacrifice To Sea");
		addStartNpc(30312);
		addTalkId(30051, 30055);
		addKillId(20481, 20544, 20545);
		registerQuestItems(1032, 1033, 1034);
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
            qs.set("sacrifice_to_sea", String.valueOf(1), true);
            string2 = "rockswell_q0304_04.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
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
        int n3 = qs.getInt("sacrifice_to_sea");
        switch (n2) {
            case 1: {
                if (n != 30312) break;
                if (pc.level() >= 2) {
                    html = "rockswell_q0304_03.htm";
                    break;
                }
                html = "rockswell_q0304_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n == 30312) {
                    if (n3 == 1 && qs.getQuestItemsCount((int)(1033)) == 0 && qs.getQuestItemsCount((int)(1034)) == 0 && qs.getQuestItemsCount((int)(1032)) < 10) {
                        html = "rockswell_q0304_05.htm";
                    } else if (n3 == 1 && qs.getQuestItemsCount((int)(1032)) >= 10) {
                        html = "rockswell_q0304_08.htm";
                    } else if (n3 == 1 && qs.getQuestItemsCount((int)(1033)) >= 1) {
                        html = "rockswell_q0304_06.htm";
                    } else if (n3 == 1 && qs.getQuestItemsCount((int)(1034)) >= 1) {
                        html = "rockswell_q0304_07.htm";
                        qs.takeItems((int)(1034), (int)(-1));
                        qs.takeItems((int)(1032), (int)(-1));
                        qs.giveItems((int)(113), (int)(1));
                        qs.unset("sacrifice_to_sea");
                        qs.playSound(QuestState.SOUND_FINISH);
                        qs.exitQuest(false);
                    }
                }
                if (n == 30051) {
                    if (n3 == 1 && qs.getQuestItemsCount((int)(1032)) < 10 && qs.getQuestItemsCount((int)(1032)) > 0) {
                        html = "cristel_q0304_01.htm";
                        break;
                    }
                    if (n3 == 1 && qs.getQuestItemsCount((int)(1032)) >= 10 && qs.getQuestItemsCount((int)(1033)) == 0 && qs.getQuestItemsCount((int)(1034)) == 0 && qs.getQuestItemsCount((int)(1034)) < 10) {
                        html = "cristel_q0304_02.htm";
                        qs.giveItems((int)(1033), (int)(1));
                        qs.takeItems((int)(1032), (int)(-1));
                        qs.setCond(3);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (n3 == 1 && qs.getQuestItemsCount((int)(1033)) >= 1) {
                        html = "cristel_q0304_03.htm";
                        break;
                    }
                    if (n3 != 1 || qs.getQuestItemsCount((int)(1034)) != 1) break;
                    html = "cristel_q0304_04.htm";
                    break;
                }
                if (n != 30055) break;
                if (n3 == 1 && qs.getQuestItemsCount((int)(1033)) >= 1) {
                    html = "rollfnan_q0304_01.htm";
                    qs.giveItems((int)(1034), (int)(1));
                    qs.takeItems((int)(1033), (int)(-1));
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    break;
                }
                if (n3 == 1 && qs.getQuestItemsCount((int)(1034)) >= 1) {
                    html = "rollfnan_q0304_02.htm";
                    break;
                }
                if (n3 != 1 || qs.getQuestItemsCount((int)(1033)) != 0 || qs.getQuestItemsCount((int)(1034)) != 0) break;
                html = "rollfnan_q0304_03.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount((int)(1032)) < 10 && qs.getQuestItemsCount((int)(1033)) == 0 && qs.getQuestItemsCount((int)(1034)) == 0 && ThreadLocalRandom.current().nextInt(10) < 4) {
            qs.rollAndGive(1032, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1032)) > 9) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
