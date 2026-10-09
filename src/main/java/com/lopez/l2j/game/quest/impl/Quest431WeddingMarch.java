package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
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
 * Quest 431 - 431_WeddingMarch
 */
@Component
public class Quest431WeddingMarch extends Quest {

	public static final int bHF = 31042;
	public static final int bHG = 7540;
	public static final int bHH = 7062;

	public Quest431WeddingMarch(QuestManager questManager) {
	super(431, "431_WeddingMarch", "431_WeddingMarch");
		this.addStartNpc(bHF);
		this.addKillId(20786);
		this.addKillId(20787);
		this.addQuestItem(bHG);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            string2 = "muzyk_q0431_0104.htm";
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("431_3")) {
            if (qs.getQuestItemsCount(bHG) == 50L) {
                string2 = "muzyk_q0431_0201.htm";
                qs.takeItems(bHG, -1L);
                qs.giveItems(bHH, 25L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                string2 = "muzyk_q0431_0202.htm";
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
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        if (n2 == bHF) {
            if (n3 != 2) {
                if (pc.getLevel() < 38) {
                    html = "muzyk_q0431_0103.htm";
                    qs.exitQuest(true);
                } else {
                    html = "muzyk_q0431_0101.htm";
                }
            } else if (n == 1) {
                html = "muzyk_q0431_0106.htm";
            } else if (n == 2 && qs.getQuestItemsCount(bHG) == 50L) {
                html = "muzyk_q0431_0105.htm";
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
        int n = npc != null ? npc.getNpcId() : 0;
        if ((n == 20786 || n == 20787) && qs.getCond() == 1 && qs.getQuestItemsCount(bHG) < 50L) {
            qs.giveItems(bHG, 1L);
            if (qs.getQuestItemsCount(bHG) == 50L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
