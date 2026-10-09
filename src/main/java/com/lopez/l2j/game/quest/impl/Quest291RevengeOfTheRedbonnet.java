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
 * Quest 291 - 291_RevengeOfTheRedbonnet
 */
@Component
public class Quest291RevengeOfTheRedbonnet extends Quest {

	public static final int MaryseRedbonnet = 30553;
	public static final int BlackWolfPelt = 1482;
	public static final int ScrollOfEscape = 736;
	public static final int GrandmasPearl = 1502;
	public static final int GrandmasMirror = 1503;
	public static final int GrandmasNecklace = 1504;
	public static final int GrandmasHairpin = 1505;
	public static final int BlackWolf = 20317;

	public Quest291RevengeOfTheRedbonnet(QuestManager questManager) {
		super(291, "291_RevengeOfTheRedbonnet", "291_RevengeOfTheRedbonnet");
		addStartNpc(this.MaryseRedbonnet);
		addKillId(this.BlackWolf);
		registerQuestItems(this.BlackWolfPelt);
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
        if (event.equalsIgnoreCase("marife_redbonnet_q0291_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.getLevel() < 4) {
                html = "marife_redbonnet_q0291_01.htm";
                qs.exitQuest(true);
            } else {
                html = "marife_redbonnet_q0291_02.htm";
            }
        } else if (n == 1) {
            html = "marife_redbonnet_q0291_04.htm";
        } else if (n == 2 && qs.getQuestItemsCount(this.BlackWolfPelt) < 40L) {
            html = "marife_redbonnet_q0291_04.htm";
            qs.setCond(1);
        } else if (n == 2 && qs.getQuestItemsCount(this.BlackWolfPelt) >= 40L) {
            int n2 = ThreadLocalRandom.current().nextInt(100);
            qs.takeItems(this.BlackWolfPelt, -1L);
            if (n2 < 3) {
                qs.giveItems(this.GrandmasPearl, 1L);
            } else if (n2 < 21) {
                qs.giveItems(this.GrandmasMirror, 1L);
            } else if (n2 < 46) {
                qs.giveItems(this.GrandmasNecklace, 1L);
            } else {
                qs.giveItems(this.ScrollOfEscape, 1L);
                qs.giveItems(this.GrandmasHairpin, 1L);
            }
            html = "marife_redbonnet_q0291_05.htm";
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && qs.getQuestItemsCount(this.BlackWolfPelt) < 40L) {
            qs.giveItems(this.BlackWolfPelt, 1L);
            if (qs.getQuestItemsCount(this.BlackWolfPelt) < 40L) {
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else {
                qs.playSound("QuestState.SOUND_MIDDLE");
                qs.setCond(2);
                qs.setState(State.STARTED);
            }
        }
        return null;
    
	}

}
