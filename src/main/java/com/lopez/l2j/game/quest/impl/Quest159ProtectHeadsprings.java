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
 * Quest 159 - Protect Headsprings
 */
@Component
public class Quest159ProtectHeadsprings extends Quest {

	public static final int PLAGUE_DUST_ID = 1035;
	public static final int HYACINTH_CHARM1_ID = 1071;
	public static final int HYACINTH_CHARM2_ID = 1072;

	public Quest159ProtectHeadsprings(QuestManager questManager) {
		super(159, "159_ProtectHeadsprings", "Protect Headsprings");
		addStartNpc(30154);
		addKillId(27017);
		registerQuestItems(PLAGUE_DUST_ID, HYACINTH_CHARM1_ID, HYACINTH_CHARM2_ID);
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
        if (event.equals("1")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            if (qs.getQuestItemsCount((int)(HYACINTH_CHARM1_ID)) == 0) {
                qs.giveItems((int)(HYACINTH_CHARM1_ID), (int)(1));
                string2 = "30154-04.htm";
            }
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.getRace() != Race.elf) {
                html = "30154-00.htm";
                qs.exitQuest(true);
            } else {
                if (pc.level() >= 12) {
                    html = "30154-03.htm";
                    return html;
                }
                html = "30154-02.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1) {
            html = "30154-05.htm";
        } else if (n == 2) {
            qs.takeItems((int)(PLAGUE_DUST_ID), (int)(-1));
            qs.takeItems((int)(HYACINTH_CHARM1_ID), (int)(-1));
            qs.giveItems((int)(HYACINTH_CHARM2_ID), (int)(1));
            qs.setCond(3);
            html = "30154-06.htm";
        } else if (n == 3) {
            html = "30154-07.htm";
        } else if (n == 4) {
            qs.takeItems((int)(PLAGUE_DUST_ID), (int)(-1));
            qs.takeItems((int)(HYACINTH_CHARM2_ID), (int)(-1));
            qs.giveItems((int)(57), (int)(18250));
            qs.playSound(QuestState.SOUND_FINISH);
            html = "30154-08.htm";
            qs.exitQuest(false);
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getCond();
        if (n == 1 && ThreadLocalRandom.current().nextInt(100) < 60) {
            qs.giveItems((int)(PLAGUE_DUST_ID), (int)(1));
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_MIDDLE);
        } else if (n == 3 && ThreadLocalRandom.current().nextInt(100) < 60) {
            if (qs.getQuestItemsCount((int)(PLAGUE_DUST_ID)) == 4) {
                qs.giveItems((int)(PLAGUE_DUST_ID), (int)(1));
                qs.setCond(4);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.giveItems((int)(PLAGUE_DUST_ID), (int)(1));
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
