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
 * Quest 262 - Tradewiththe Ivory Tower
 */
@Component
public class Quest262TradewiththeIvoryTower extends Quest {

	public static final int VOLODOS = 30137;
	public static final int GREEN_FUNGUS = 20007;
	public static final int BLOOD_FUNGUS = 20400;
	public static final int FUNGUS_SAC = 707;

	public Quest262TradewiththeIvoryTower(QuestManager questManager) {
		super(262, "262_TradewiththeIvoryTower", "Tradewiththe Ivory Tower");
		addStartNpc(30137);
		addKillId(20400, 20007);
		registerQuestItems(707);
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
        if (event.equals("vollodos_q0262_03.htm")) {
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
            if (pc.level() >= 8) {
                html = "vollodos_q0262_02.htm";
                return html;
            }
            html = "vollodos_q0262_01.htm";
            qs.exitQuest(true);
        } else if (n == 1 && qs.getQuestItemsCount(707) < 10) {
            html = "vollodos_q0262_04.htm";
        } else if (n == 2 && qs.getQuestItemsCount(707) >= 10) {
            qs.giveItems(57, 3000);
            qs.takeItems(707, -1);
            qs.setCond(0);
            qs.playSound(QuestState.SOUND_FINISH);
            html = "vollodos_q0262_05.htm";
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = ThreadLocalRandom.current().nextInt(10);
        if (qs.getCond() == 1 && qs.getQuestItemsCount(707) < 10 && (n == 20007 && n2 < 3 || n == 20400 && n2 < 4)) {
            qs.giveItems(707, 1);
            if (qs.getQuestItemsCount(707) == 10) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
