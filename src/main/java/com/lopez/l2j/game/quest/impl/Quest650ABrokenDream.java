package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.Clan;
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
 * Quest 650 - 650_ABrokenDream
 */
@Component
public class Quest650ABrokenDream extends Quest {

	public static final int bTU = 32054;
	public static final int bTV = 22027;
	public static final int bTW = 22028;
	public static final int bTX = 8514;

	public Quest650ABrokenDream(QuestManager questManager) {
	super(650, "650_ABrokenDream", "650_ABrokenDream");
		this.addStartNpc(32054);
		this.addKillId(22027);
		this.addKillId(22028);
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
            string2 = "ghost_of_railroadman_q0650_0103.htm";
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.setCond(1);
        } else if (event.equalsIgnoreCase("650_4")) {
            string2 = "ghost_of_railroadman_q0650_0205.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            qs.unset("cond");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = qs.getCond();
        String html = "noquest";
        if (n == 0) {
            QuestState questState2 = qs.getQuestState("117_OceanOfDistantStar");
            if (questState2 != null) {
                if (questState2.isCompleted()) {
                    if (pc.getLevel() < 39) {
                        qs.exitQuest(true);
                        html = "ghost_of_railroadman_q0650_0102.htm";
                    } else {
                        html = "ghost_of_railroadman_q0650_0101.htm";
                    }
                } else {
                    html = "ghost_of_railroadman_q0650_0104.htm";
                    qs.exitQuest(true);
                }
            } else {
                html = "ghost_of_railroadman_q0650_0104.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1) {
            html = "ghost_of_railroadman_q0650_0202.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        qs.rollAndGive(8514, 1, 1, 68.0);
        return null;
    
	}

}
