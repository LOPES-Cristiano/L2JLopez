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
 * Quest 151 - Curefor Fever Disease
 */
@Component
public class Quest151CureforFeverDisease extends Quest {

	public static final int POISON_SAC = 703;
	public static final int FEVER_MEDICINE = 704;
	public static final int ROUND_SHIELD = 102;
	public static final int ELIAS = 30050;
	public static final int YOHANES = 30032;

	public Quest151CureforFeverDisease(QuestManager questManager) {
		super(151, "151_CureforFeverDisease", "Curefor Fever Disease");
		addStartNpc(30050);
		addTalkId(30032);
		addKillId(20103, 20106, 20108);
		registerQuestItems(FEVER_MEDICINE, POISON_SAC);
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
        if (event.equals("30050-03.htm")) {
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

        int n = npc.getNpcId();
        String html = "noquest";
        int n2 = qs.getStateId();
        int n3 = 0;
        if (n2 != 1) {
            n3 = qs.getCond();
        }
        if (n == 30050) {
            if (n3 == 0) {
                if (pc.level() >= 15) {
                    html = "30050-02.htm";
                } else {
                    html = "30050-01.htm";
                    qs.exitQuest(true);
                }
            } else if (n3 == 1 && qs.getQuestItemsCount((int)(POISON_SAC)) == 0 && qs.getQuestItemsCount((int)(FEVER_MEDICINE)) == 0) {
                html = "30050-04.htm";
            } else if (n3 == 1 && qs.getQuestItemsCount((int)(POISON_SAC)) == 1) {
                html = "30050-05.htm";
            } else if (n3 == 3 && qs.getQuestItemsCount((int)(FEVER_MEDICINE)) == 1) {
                qs.takeItems((int)(FEVER_MEDICINE), (int)(-1));
                qs.giveItems((int)(ROUND_SHIELD), (int)(1));
                qs.addExpAndSp(13106, 613);
                if (1 == 1 && !false) {
                    
                    
                }
                html = "30050-06.htm";
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(false);
            }
        } else if (n == 30032) {
            if (n3 == 2 && qs.getQuestItemsCount((int)(POISON_SAC)) > 0) {
                qs.giveItems((int)(FEVER_MEDICINE), (int)(1));
                qs.takeItems((int)(POISON_SAC), (int)(-1));
                qs.setCond(3);
                html = "30032-01.htm";
            } else if (n3 == 3 && qs.getQuestItemsCount((int)(FEVER_MEDICINE)) > 0) {
                html = "30032-02.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        if ((n == 20103 || n == 20106 || n == 20108) && qs.getQuestItemsCount((int)(POISON_SAC)) == 0 && qs.getCond() == 1 && ThreadLocalRandom.current().nextInt(100) < 50) {
            qs.setCond(2);
            qs.giveItems((int)(POISON_SAC), (int)(1));
            qs.playSound(QuestState.SOUND_MIDDLE);
        }
        return null;
    
	}

}
