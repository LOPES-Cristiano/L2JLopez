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
 * Quest 303 - 303_CollectArrowheads
 */
@Component
public class Quest303CollectArrowheads extends Quest {

	public static final int ORCISH_ARROWHEAD = 963;

	public Quest303CollectArrowheads(QuestManager questManager) {
		super(303, "303_CollectArrowheads", "303_CollectArrowheads");
		addStartNpc(30029);
		addKillId(20361);
		registerQuestItems(this.ORCISH_ARROWHEAD);
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
        if (event.equalsIgnoreCase("minx_q0303_04.htm")) {
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
            if (pc.getLevel() >= 10) {
                html = "minx_q0303_03.htm";
            } else {
                html = "minx_q0303_02.htm";
                qs.exitQuest(true);
            }
        } else if (qs.getQuestItemsCount(this.ORCISH_ARROWHEAD) < 10L) {
            html = "minx_q0303_05.htm";
        } else {
            qs.takeItems(this.ORCISH_ARROWHEAD, -1L);
            qs.giveItems(57, 1000L);
            qs.addExpAndSp(2000L, 0L);
            html = "minx_q0303_06.htm";
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(false);
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount(this.ORCISH_ARROWHEAD) < 10L) {
            qs.giveItems(this.ORCISH_ARROWHEAD, 1L);
            if (qs.getQuestItemsCount(this.ORCISH_ARROWHEAD) == 10L) {
                qs.setCond(2);
                qs.playSound("QuestState.SOUND_MIDDLE");
            } else {
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        }
        return null;
    
	}

}
