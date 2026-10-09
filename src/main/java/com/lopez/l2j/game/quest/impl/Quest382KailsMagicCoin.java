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
 * Quest 382 - 382_KailsMagicCoin
 */
@Component
public class Quest382KailsMagicCoin extends Quest {

	public static final int bxI = 5898;
	public static final int bxO = 30687;
	public static final Map<Integer, int[]> bxP = new HashMap<Integer, int[]>();

	public Quest382KailsMagicCoin(QuestManager questManager) {
	super(382, "382_KailsMagicCoin", "382_KailsMagicCoin");
		this.addStartNpc(bxO);
		for (int n : bxP.keySet()) {
		this.addKillId(n);
		}
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
        if (event.equalsIgnoreCase("head_blacksmith_vergara_q0382_03.htm")) {
            if (pc.getLevel() >= 55 && qs.getQuestItemsCount(bxI) > 0L) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
            } else {
                string2 = "head_blacksmith_vergara_q0382_01.htm";
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("list")) {
            // multisell
            string2 = null;
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (qs.getQuestItemsCount(bxI) == 0L || pc.getLevel() < 55) {
            html = "head_blacksmith_vergara_q0382_01.htm";
            qs.exitQuest(true);
        } else {
            html = n == 0 ? "head_blacksmith_vergara_q0382_02.htm" : "head_blacksmith_vergara_q0382_04.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED || qs.getQuestItemsCount(bxI) == 0L) {
            return null;
        }
        int[] nArray = bxP.get(npc.getNpcId());
        qs.rollAndGive(nArray[ThreadLocalRandom.current().nextInt(nArray.length)], 1, 10.0);
        return null;
    
	}

}
