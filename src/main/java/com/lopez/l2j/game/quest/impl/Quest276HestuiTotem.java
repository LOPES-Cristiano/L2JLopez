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
 * Quest 276 - Hestui Totem
 */
@Component
public class Quest276HestuiTotem extends Quest {

	public static final int aPG = 30571;
	public static final int beq = 20479;
	public static final int ber = 27044;
	public static final int Leather_Pants = 29;
	public static final int bes = 1500;
	public static final int bet = 1480;
	public static final int beu = 1481;

	public Quest276HestuiTotem(QuestManager questManager) {
		super(276, "276_HestuiTotem", "Hestui Totem");
		addStartNpc(aPG);
		addKillId(beq);
		addKillId(ber);
		registerQuestItems(bet);
		registerQuestItems(beu);
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

        if (event.equalsIgnoreCase("seer_tanapi_q0276_03.htm") && qs.getState() == State.CREATED && pc.race() == 3 && pc.level() >= 15) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return html;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != aPG) {
            return html;
        }
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.race() != 3) {
                html = "seer_tanapi_q0276_00.htm";
                qs.exitQuest(true);
            } else if (pc.level() < 15) {
                html = "seer_tanapi_q0276_01.htm";
                qs.exitQuest(true);
            } else {
                html = "seer_tanapi_q0276_02.htm";
                qs.setCond(0);
            }
        } else if (n == 2) {
            if (qs.getQuestItemsCount(beu) > 0) {
                html = "seer_tanapi_q0276_05.htm";
                qs.takeItems(bet, -1);
                qs.takeItems(beu, -1);
                qs.giveItems(Leather_Pants, 1);
                qs.giveItems(bes, 1);
                
                if (1 == 1 && !false) {
                    
                    
                }
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                html = "seer_tanapi_q0276_04.htm";
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
        if (n == beq && qs.getQuestItemsCount(beu) == 0) {
            if (qs.getQuestItemsCount(bet) < 50) {
                qs.giveItems(bet, 1);
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else {
                qs.takeItems(bet, -1);
                qs.addSpawn(ber);
            }
        } else if (n == ber && qs.getQuestItemsCount(beu) == 0) {
            qs.giveItems(beu, 1);
            qs.playSound(QuestState.SOUND_MIDDLE);
        }
        return null;
    
	}

}
