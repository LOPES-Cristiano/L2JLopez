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
 * Quest 263 - Orc Subjugation
 */
@Component
public class Quest263OrcSubjugation extends Quest {

	public static final int KAYLEEN = 30346;
	public static final int BALOR_ORC_ARCHER = 20385;
	public static final int BALOR_ORC_FIGHTER = 20386;
	public static final int BALOR_ORC_FIGHTER_LEADER = 20387;
	public static final int BALOR_ORC_LIEUTENANT = 20388;
	public static final int ORC_AMULET = 1116;
	public static final int ORC_NECKLACE = 1117;

	public Quest263OrcSubjugation(QuestManager questManager) {
		super(263, "263_OrcSubjugation", "Orc Subjugation");
		addStartNpc(30346);
		addKillId(20385, 20386, 20387, 20388);
		registerQuestItems(1116, 1117);
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
        if (event.equals("sentry_kayleen_q0263_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("sentry_kayleen_q0263_06.htm")) {
            qs.setCond(0);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
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
            if (pc.level() >= 8 && pc.race() == 2) {
                html = "sentry_kayleen_q0263_02.htm";
                return html;
            }
            if (pc.race() != 2) {
                html = "sentry_kayleen_q0263_00.htm";
                qs.exitQuest(true);
            } else if (pc.level() < 8) {
                html = "sentry_kayleen_q0263_01.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1) {
            if (qs.getQuestItemsCount(1116) == 0 && qs.getQuestItemsCount(1117) == 0) {
                html = "sentry_kayleen_q0263_04.htm";
            } else if (qs.getQuestItemsCount(1116) + qs.getQuestItemsCount(1117) >= 10) {
                html = "sentry_kayleen_q0263_05.htm";
                qs.giveItems(57, qs.getQuestItemsCount(1116) * 20 + qs.getQuestItemsCount(1117) * 30 + 1100);
                qs.takeItems(1116, -1);
                qs.takeItems(1117, -1);
            } else {
                html = "sentry_kayleen_q0263_05.htm";
                qs.giveItems(57, qs.getQuestItemsCount(1116) * 20 + qs.getQuestItemsCount(1117) * 30);
                qs.takeItems(1116, -1);
                qs.takeItems(1117, -1);
            }
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getCond() == 1 && ThreadLocalRandom.current().nextInt(100) < 60) {
            if (n == 20385) {
                qs.giveItems(1116, 1);
            } else if (n == 20386 || n == 20387 || n == 20388) {
                qs.giveItems(1117, 1);
            }
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
