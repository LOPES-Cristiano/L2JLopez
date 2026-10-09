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
 * Quest 353 - 353_PowerOfDarkness
 */
@Component
public class Quest353PowerOfDarkness extends Quest {

	public static final int bqA = 31044;
	public static final int bqB = 20283;
	public static final int bqC = 20284;
	public static final int bqD = 20244;
	public static final int bqE = 20245;
	public static final int bqF = 5862;
	public static final int bqG = 57;
	public static final int bqH = 50;

	public Quest353PowerOfDarkness(QuestManager questManager) {
	super(353, "353_PowerOfDarkness", "353_PowerOfDarkness");
		this.addStartNpc(bqA);
		this.addKillId(bqB);
		this.addKillId(bqC);
		this.addKillId(bqD);
		this.addKillId(bqE);
		this.addQuestItem(bqF);
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

        int n = qs.getStateId();
        if (event.equalsIgnoreCase("31044-04.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("31044-08.htm") && n == 2) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != bqA) {
            return html;
        }
        if (qs.getState() == State.CREATED) {
            if (pc.getLevel() >= 55) {
                html = "31044-02.htm";
                qs.setCond(0);
            } else {
                html = "31044-01.htm";
                qs.exitQuest(true);
            }
        } else {
            long l = qs.getQuestItemsCount(bqF);
            if (l > 0L) {
                html = "31044-06.htm";
                qs.takeItems(bqF, -1L);
                qs.giveItems(bqG, 2500L + 230L * l);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                html = "31044-05.htm";
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
        if ((ThreadLocalRandom.current().nextDouble(100.0) < (bqH))) {
            qs.giveItems(bqF, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
