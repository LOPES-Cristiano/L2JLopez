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
 * Quest 162 - Curse Of Underground Fortress
 */
@Component
public class Quest162CurseOfUndergroundFortress extends Quest {



	public Quest162CurseOfUndergroundFortress(QuestManager questManager) {
		super(162, "162_CurseOfUndergroundFortress", "Curse Of Underground Fortress");
		addStartNpc(30147);
		addKillId(20033, 20345, 20371, 20463, 20464, 20504);
		registerQuestItems(1159, 1158);
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
            string2 = "uno_q0314_04.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "uno_q0314_03.htm";
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
        switch (n) {
            case 1: {
                if (pc.getRace() == Race.darkelf) {
                    html = "uno_q0314_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 12) {
                    html = "uno_q0314_02.htm";
                    break;
                }
                html = "uno_q0314_01.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount((int)(1159)) + qs.getQuestItemsCount((int)(1158)) < 13) {
                    html = "uno_q0314_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1159)) + qs.getQuestItemsCount((int)(1158)) < 13) break;
                html = "uno_q0314_06.htm";
                qs.giveItems((int)(625), (int)(1));
                qs.giveItems((int)(57), (int)(24000));
                qs.takeItems((int)(1159), (int)(-1));
                qs.takeItems((int)(1158), (int)(-1));
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(false);
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        if (n == 20033 && ThreadLocalRandom.current().nextInt(100) < 25 && qs.getQuestItemsCount((int)(1159)) < 3 && qs.getCond() == 1) {
            qs.rollAndGive(1159, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1159)) >= 2) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                    qs.setCond(2);
                }
            }
        } else if (n == 20345 && ThreadLocalRandom.current().nextInt(100) < 26 && qs.getQuestItemsCount((int)(1159)) < 3 && qs.getCond() == 1) {
            qs.rollAndGive(1159, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1159)) >= 2) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                    qs.setCond(2);
                }
            }
        } else if (n == 20371 && ThreadLocalRandom.current().nextInt(100) < 23 && qs.getQuestItemsCount((int)(1159)) < 3 && qs.getCond() == 1) {
            qs.rollAndGive(1159, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1159)) >= 2) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                    qs.setCond(2);
                }
            }
        } else if (n == 20463 && ThreadLocalRandom.current().nextInt(4) == 1 && qs.getCond() == 1) {
            qs.rollAndGive(1158, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1159)) >= 3) {
                    qs.setCond(2);
                }
            }
        } else if (n == 20464 && ThreadLocalRandom.current().nextInt(100) < 23 && qs.getCond() == 1) {
            qs.rollAndGive(1158, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1159)) >= 10) {
                    qs.setCond(2);
                }
            }
        } else if (n == 20504 && ThreadLocalRandom.current().nextInt(100) < 26 && qs.getCond() == 1) {
            qs.rollAndGive(1158, 1, 100.0);
            if (qs.getQuestItemsCount((int)(1158)) >= 10) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                if (qs.getQuestItemsCount((int)(1159)) >= 3) {
                    qs.setCond(2);
                }
            }
        }
        return null;
    
	}

}
