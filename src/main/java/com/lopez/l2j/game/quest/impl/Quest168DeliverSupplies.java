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
 * Quest 168 - Deliver Supplies
 */
@Component
public class Quest168DeliverSupplies extends Quest {



	public Quest168DeliverSupplies(QuestManager questManager) {
		super(168, "168_DeliverSupplies", "Deliver Supplies");
		addStartNpc(30349);
		addTalkId(30355, 30357, 30360);
		registerQuestItems(1153, 1154, 1155, 1156, 1157);
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
            string2 = "sentry_jenine_q0325_03.htm";
            qs.giveItems((int)(1153), (int)(1));
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
        PlayerCharacter player = pc;
        switch (n2) {
            case 1: {
                if (n != 30349) break;
                if (pc.getRace() != Race.darkelf) {
                    html = "sentry_jenine_q0325_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 3) {
                    html = "sentry_jenine_q0325_02.htm";
                    break;
                }
                html = "sentry_jenine_q0325_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n == 30349) {
                    if (qs.getQuestItemsCount((int)(1153)) >= 1) {
                        html = "sentry_jenine_q0325_04.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1154)) == 1 && qs.getQuestItemsCount((int)(1155)) == 1 && qs.getQuestItemsCount((int)(1156)) == 1) {
                        html = "sentry_jenine_q0325_05.htm";
                        qs.takeItems((int)(1154), (int)(1));
                        qs.setCond(3);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1154)) == 0 && (qs.getQuestItemsCount((int)(1155)) == 1 || qs.getQuestItemsCount((int)(1156)) == 1)) {
                        html = "sentry_jenine_q0325_07.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1157)) != 2) break;
                    html = "sentry_jenine_q0325_06.htm";
                    qs.takeItems((int)(1157), (int)(2));
                    qs.giveItems((int)(57), (int)(820));
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(false);
                    break;
                }
                if (n == 30355) {
                    if (qs.getQuestItemsCount((int)(1155)) == 1 && qs.getQuestItemsCount((int)(1154)) == 0) {
                        html = "sentry_roseline_q0325_01.htm";
                        qs.takeItems((int)(1155), (int)(1));
                        qs.giveItems((int)(1157), (int)(1));
                        if (qs.getQuestItemsCount((int)(1156)) != 0) break;
                        qs.setCond(4);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1155)) != 0 || qs.getQuestItemsCount((int)(1157)) < 1) break;
                    html = "sentry_roseline_q0325_02.htm";
                    break;
                }
                if (n == 30357) {
                    if (qs.getQuestItemsCount((int)(1156)) == 1 && qs.getQuestItemsCount((int)(1154)) == 0) {
                        html = "sentry_krpion_q0325_01.htm";
                        qs.takeItems((int)(1156), (int)(1));
                        qs.giveItems((int)(1157), (int)(1));
                        if (qs.getQuestItemsCount((int)(1155)) != 0) break;
                        qs.setCond(4);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        break;
                    }
                    if (qs.getQuestItemsCount((int)(1156)) != 0 || qs.getQuestItemsCount((int)(1157)) < 1) break;
                    html = "sentry_krpion_q0325_02.htm";
                    break;
                }
                if (n != 30360) break;
                if (qs.getQuestItemsCount((int)(1153)) == 1) {
                    html = "master_harant_q0325_01.htm";
                    qs.takeItems((int)(1153), (int)(1));
                    qs.giveItems((int)(1154), (int)(1));
                    qs.giveItems((int)(1155), (int)(1));
                    qs.giveItems((int)(1156), (int)(1));
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    break;
                }
                if (qs.getQuestItemsCount((int)(1154)) + qs.getQuestItemsCount((int)(1155)) + qs.getQuestItemsCount((int)(1156)) <= 0) break;
                html = "master_harant_q0325_02.htm";
            }
        }
        return html;
    
	}


}
