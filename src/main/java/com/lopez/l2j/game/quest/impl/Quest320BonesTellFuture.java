package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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
 * Quest 320 - 320_BonesTellFuture
 */
@Component
public class Quest320BonesTellFuture extends Quest {

	public static final int BONE_FRAGMENT = 809;

	public Quest320BonesTellFuture(QuestManager questManager) {
		super(320, "320_BonesTellFuture", "320_BonesTellFuture");
		addStartNpc(30359);
		addKillId(20518);
		addKillId(20517);
		registerQuestItems(809);
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

        String string2 = event;
        if (event.equalsIgnoreCase("tetrarch_kaitar_q0320_04.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
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
            if (pc.getRace() != Race.darkelf) {
                html = "tetrarch_kaitar_q0320_00.htm";
                qs.exitQuest(true);
            } else if (pc.getLevel() >= 10) {
                html = "tetrarch_kaitar_q0320_03.htm";
            } else {
                html = "tetrarch_kaitar_q0320_02.htm";
                qs.exitQuest(true);
            }
        } else if (qs.getQuestItemsCount(809) < 10L) {
            html = "tetrarch_kaitar_q0320_05.htm";
        } else {
            html = "tetrarch_kaitar_q0320_06.htm";
            qs.giveItems(57, 8470L, true);
            qs.takeItems(809, -1L);
            qs.exitQuest(true);
            qs.unset("cond");
            qs.playSound("QuestState.SOUND_FINISH");
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        qs.rollAndGive(809, 1, 1, 10, 10.0);
        if (qs.getQuestItemsCount(809) >= 10L) {
            qs.setCond(2);
        }
        qs.setState(State.STARTED);
        return null;
    
	}

}
