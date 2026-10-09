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
 * Quest 659 - 659_IdRatherBeCollectingFairyBreath
 */
@Component
public class Quest659IdRatherBeCollectingFairyBreath extends Quest {

	public static final int GALATEA = 30634;
	public static final int[] MOBS = new int[]{20078, 21026, 21025, 21024, 21023};
	public static final int FAIRY_BREATH = 8286;

	public Quest659IdRatherBeCollectingFairyBreath(QuestManager questManager) {
	super(659, "659_IdRatherBeCollectingFairyBreath", "659_IdRatherBeCollectingFairyBreath");
		this.addStartNpc(30634);
		this.addTalkId(30634);
		this.addTalkId(30634);
		this.addTalkId(30634);
		for (int n : this.MOBS) {
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
        if (event.equalsIgnoreCase("high_summoner_galatea_q0659_0103.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("high_summoner_galatea_q0659_0203.htm")) {
            long l = qs.getQuestItemsCount(8286);
            if (l > 0L) {
                long l2 = 0L;
                l2 = l < 10L ? l * 50L : l * 50L + 5365L;
                qs.takeItems(8286, -1L);
                qs.giveItems(57, l2);
            }
        } else if (event.equalsIgnoreCase("high_summoner_galatea_q0659_0204.htm")) {
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getStateId();
        int n3 = 0;
        if (n2 != 1) {
            n3 = qs.getCond();
        }
        if (n == 30634) {
            if (pc.getLevel() < 26) {
                html = "high_summoner_galatea_q0659_0102.htm";
                qs.exitQuest(true);
            } else if (n3 == 0) {
                html = "high_summoner_galatea_q0659_0101.htm";
            } else if (n3 == 1) {
                html = qs.getQuestItemsCount(8286) == 0L ? "high_summoner_galatea_q0659_0105.htm" : "high_summoner_galatea_q0659_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getCond();
        if (n2 == 1) {
            for (int n3 : this.MOBS) {
                if (n != n3 || !(ThreadLocalRandom.current().nextDouble(100.0) < (30))) continue;
                qs.giveItems(8286, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
