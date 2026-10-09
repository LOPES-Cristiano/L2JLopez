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
 * Quest 368 - 368_TrespassingIntoTheSacredArea
 */
@Component
public class Quest368TrespassingIntoTheSacredArea extends Quest {

	public static final int bsO = 30926;
	public static final int bsP = 20794;
	public static final int bsQ = 20795;
	public static final int bsR = 20796;
	public static final int bsS = 20797;
	public static final int bsT = 5881;

	public Quest368TrespassingIntoTheSacredArea(QuestManager questManager) {
	super(368, "368_TrespassingIntoTheSacredArea", "368_TrespassingIntoTheSacredArea");
		this.addStartNpc(30926);
		this.addKillId(20794, 20795, 20796, 20797);
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
        int n = getFirstStartNpc();
        if (n == 30926) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "priestess_restina_q0368_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                qs.exitQuest(true);
                qs.playSound(QuestState.SOUND_FINISH);
                string2 = "priestess_restina_q0368_06.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "priestess_restina_q0368_07.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30926) break;
                if (pc.getLevel() < 36 || pc.getLevel() > 48) {
                    html = "priestess_restina_q0368_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "priestess_restina_q0368_01.htm";
                break;
            }
            case 2: {
                if (n != 30926) break;
                if (qs.getQuestItemsCount(5881) < 1L) {
                    html = "priestess_restina_q0368_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5881) < 1L) break;
                if (qs.getQuestItemsCount(5881) >= 10L) {
                    qs.giveItems(57, qs.getQuestItemsCount(5881) * 250L + 9450L);
                } else {
                    qs.giveItems(57, qs.getQuestItemsCount(5881) * 250L + 2000L);
                }
                qs.takeItems(5881, -1L);
                html = "priestess_restina_q0368_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20794) {
            if (ThreadLocalRandom.current().nextInt(100) < 60) {
                qs.giveItems(5881, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20795) {
            if (ThreadLocalRandom.current().nextInt(100) < 57) {
                qs.giveItems(5881, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20796) {
            if (ThreadLocalRandom.current().nextInt(100) < 61) {
                qs.giveItems(5881, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20797 && ThreadLocalRandom.current().nextInt(100) < 93) {
            qs.giveItems(5881, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
