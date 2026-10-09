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
 * Quest 360 - 360_PlunderTheirSupplies
 */
@Component
public class Quest360PlunderTheirSupplies extends Quest {

	public static final int brO = 30873;
	public static final int brP = 20666;
	public static final int brQ = 20669;
	public static final int brR = 5872;
	public static final int brS = 5871;
	public static final int brT = 5870;
	public static final int brU = 50;
	public static final int brV = 65;
	public static final int brW = 5;

	public Quest360PlunderTheirSupplies(QuestManager questManager) {
	super(360, "360_PlunderTheirSupplies", "360_PlunderTheirSupplies");
		this.addStartNpc(30873);
		this.addKillId(20666);
		this.addKillId(20669);
		this.addQuestItem(5872);
		this.addQuestItem(5871);
		this.addQuestItem(5870);
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
        if (event.equalsIgnoreCase("guard_coleman_q0360_04.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("guard_coleman_q0360_10.htm")) {
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
        long l = qs.getQuestItemsCount(5870);
        long l2 = qs.getQuestItemsCount(5872);
        if (n != 2) {
            html = pc.getLevel() >= 52 ? "guard_coleman_q0360_02.htm" : "guard_coleman_q0360_01.htm";
        } else if (l > 0L || l2 > 0L) {
            long l3 = 6000L + l2 * 100L + l * 6000L;
            qs.takeItems(5872, -1L);
            qs.takeItems(5870, -1L);
            qs.giveItems(57, l3);
            html = "guard_coleman_q0360_08.htm";
        } else {
            html = "guard_coleman_q0360_05.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20666 && (ThreadLocalRandom.current().nextDouble(100.0) < (50)) || n == 20669 && (ThreadLocalRandom.current().nextDouble(100.0) < (65))) {
            qs.giveItems(5872, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        if ((ThreadLocalRandom.current().nextDouble(100.0) < (5))) {
            if (qs.getQuestItemsCount(5871) < 4L) {
                qs.giveItems(5871, 1L);
            } else {
                qs.takeItems(5871, -1L);
                qs.giveItems(5870, 1L);
            }
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
