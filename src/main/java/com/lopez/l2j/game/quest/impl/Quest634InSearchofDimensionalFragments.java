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
 * Quest 634 - 634_InSearchofDimensionalFragments
 */
@Component
public class Quest634InSearchofDimensionalFragments extends Quest {

	public static final int DIMENSION_FRAGMENT_ID = 7079;

	public Quest634InSearchofDimensionalFragments(QuestManager questManager) {
	super(634, "634_InSearchofDimensionalFragments", "634_InSearchofDimensionalFragments");
		int n;
		for (n = 31494; n < 31508; ++n) {
		this.addTalkId(n);
		this.addStartNpc(n);
		}
		n = 21208;
		while (n < 21256) {
		this.addKillId(n++);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            string2 = "dimension_keeperhtm";
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.setCond(1);
        } else if (event.equalsIgnoreCase("634_2")) {
            string2 = "dimension_keeperhtm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.getLevel() > 20) {
                html = "dimension_keeperhtm";
            } else {
                html = "dimension_keeperhtm";
                qs.exitQuest(true);
            }
        } else if (n == 2) {
            html = "dimension_keeperhtm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = (int)(1.6 + (double)((float)npc.getLevel() * 0.15f));
        qs.rollAndGive(this.DIMENSION_FRAGMENT_ID, n, 90.0);
        return null;
    
	}

}
