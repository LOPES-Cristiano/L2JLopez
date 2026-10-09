package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 324 - 324_SweetestVenom
 */
@Component
public class Quest324SweetestVenom extends Quest {

	public static final int bfJ = 30351;
	public static final int bfK = 20034;
	public static final int bfL = 20038;
	public static final int bfM = 20043;
	public static final int bfN = 1077;
	public static final int bfO = 60;

	public Quest324SweetestVenom(QuestManager questManager) {
		super(324, "324_SweetestVenom", "324_SweetestVenom");
		addStartNpc(bfJ);
		addKillId(bfL);
		addKillId(bfM);
		addKillId(bfK);
		registerQuestItems(bfN);
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

        if (event.equalsIgnoreCase("astaron_q0324_04.htm") && qs.getState() == State.CREATED) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != bfJ) {
            return html;
        }
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.getLevel() >= 18) {
                html = "astaron_q0324_03.htm";
                qs.setCond(0);
            } else {
                html = "astaron_q0324_02.htm";
                qs.exitQuest(true);
            }
        } else if (n == 2) {
            long l = qs.getQuestItemsCount(bfN);
            if (l >= 10L) {
                html = "astaron_q0324_06.htm";
                qs.takeItems(bfN, -1L);
                qs.giveItems(57, 5810L);
                qs.playSound("QuestState.SOUND_FINISH");
                qs.exitQuest(true);
            } else {
                html = "astaron_q0324_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        long l = qs.getQuestItemsCount(bfN);
        int n = bfO + (npc.getNpcId() - bfK) / 4 * 12;
        if (l < 10L && (ThreadLocalRandom.current().nextInt(100) < n)) {
            qs.giveItems(bfN, 1L);
            if (l == 9L) {
                qs.setCond(2);
                qs.playSound("QuestState.SOUND_MIDDLE");
            } else {
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        }
        return null;
    
	}

}
