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
 * Quest 651 - 651_RunawayYouth
 */
@Component
public class Quest651RunawayYouth extends Quest {

	public static final int bTY = 32014;
	public static final int bTZ = 31989;
	public static final int SOE = 736;
	private NpcInstance _npc = null;

	public Quest651RunawayYouth(QuestManager questManager) {
	super(651, "651_RunawayYouth", "651_RunawayYouth");
		this.addStartNpc(bTY);
		this.addTalkId(bTZ);
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
        if (event.equalsIgnoreCase("runaway_boy_ivan_q0651_03.htm")) {
            if (qs.getQuestItemsCount(SOE) > 0L) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.takeItems(SOE, 1L);
                string2 = "runaway_boy_ivan_q0651_04.htm";
                qs.startQuestTimer("ivan_timer", 20000L);
            }
        } else if (event.equalsIgnoreCase("runaway_boy_ivan_q0651_05.htm")) {
            qs.exitQuest(true);
            qs.playSound("ItemSound.quest_giveup");
        } else if (event.equalsIgnoreCase("ivan_timer")) {
            if (this._npc != null) {
                this._npc.deleteMe();
            }
            string2 = null;
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getCond();
        if (n == bTY) {
            this._npc = npc;
        }
        if (n == bTY && n2 == 0) {
            if (pc.getLevel() >= 26) {
                html = "runaway_boy_ivan_q0651_01.htm";
            } else {
                html = "runaway_boy_ivan_q0651_01a.htm";
                qs.exitQuest(true);
            }
        } else if (n == bTZ && n2 == 1) {
            html = "fisher_batidae_q0651_01.htm";
            qs.giveItems(57, Math.round(2883.0 * qs.getRateQuestsAdenaReward()));
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return html;
    
	}

}
