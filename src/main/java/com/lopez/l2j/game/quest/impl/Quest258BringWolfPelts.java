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
 * Quest 258 - Bring Wolf Pelts
 */
@Component
public class Quest258BringWolfPelts extends Quest {

	public static final int WOLF_PELT = 702;
	public static final int Cotton_Shirt = 390;
	public static final int Leather_Pants = 29;
	public static final int Leather_Shirt = 22;
	public static final int Short_Leather_Gloves = 1119;
	public static final int Tunic = 426;

	public Quest258BringWolfPelts(QuestManager questManager) {
		super(258, "258_BringWolfPelts", "Bring Wolf Pelts");
		addStartNpc(30001);
		addKillId(20120);
		addKillId(20442);
		registerQuestItems(WOLF_PELT);
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

        String event2 = event;
        if (event.equalsIgnoreCase("lector_q0258_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.level() >= 3) {
                html = "lector_q0258_02.htm";
                return html;
            }
            html = "lector_q0258_01.htm";
            qs.exitQuest(true);
        } else if (n == 1 && qs.getQuestItemsCount(WOLF_PELT) >= 0 && qs.getQuestItemsCount(WOLF_PELT) < 40) {
            html = "lector_q0258_05.htm";
        } else if (n == 2 && qs.getQuestItemsCount(WOLF_PELT) >= 40) {
            qs.takeItems(WOLF_PELT, 40);
            int n2 = ThreadLocalRandom.current().nextInt(16);
            if (n2 == 0) {
                qs.giveItems(Cotton_Shirt, 1);
                qs.playSound(QuestState.SOUND_FINISH);
            } else if (n2 < 6) {
                qs.giveItems(Leather_Pants, 1);
            } else if (n2 < 9) {
                qs.giveItems(Leather_Shirt, 1);
            } else if (n2 < 13) {
                qs.giveItems(Short_Leather_Gloves, 1);
            } else {
                qs.giveItems(Tunic, 1);
            }
            html = "lector_q0258_06.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        long l = qs.getQuestItemsCount(WOLF_PELT);
        if (l < 40 && qs.getCond() == 1) {
            qs.giveItems(WOLF_PELT, 1);
            if (l == 39) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
