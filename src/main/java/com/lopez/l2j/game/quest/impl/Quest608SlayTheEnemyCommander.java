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
 * Quest 608 - 608_SlayTheEnemyCommander
 */
@Component
public class Quest608SlayTheEnemyCommander extends Quest {

	public static final int bLg = 31370;
	public static final int bLp = 25312;
	public static final int bLq = 7236;
	public static final int bLr = 7220;
	public static final int bLk = 7211;
	public static final int bLl = 7212;
	public static final int bLm = 7213;
	public static final int bLn = 7214;
	public static final int bLo = 7215;

	public Quest608SlayTheEnemyCommander(QuestManager questManager) {
	super(608, "608_SlayTheEnemyCommander", "608_SlayTheEnemyCommander");
		this.addStartNpc(31370);
		this.addKillId(25312);
		this.addQuestItem(7236);
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
            string2 = "elder_kadun_zu_ketra_q0608_0104.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("608_3")) {
            if (qs.getQuestItemsCount(7236) >= 1L) {
                string2 = "elder_kadun_zu_ketra_q0608_0201.htm";
                qs.takeItems(7236, -1L);
                qs.giveItems(7220, 1L);
                qs.addExpAndSp(0L, 10000L);
                qs.unset("cond");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                string2 = "elder_kadun_zu_ketra_q0608_0106.htm";
            }
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
            if (pc.getLevel() >= 75) {
                if (qs.getQuestItemsCount(7214) == 1L || qs.getQuestItemsCount(7215) == 1L) {
                    html = "elder_kadun_zu_ketra_q0608_0101.htm";
                } else {
                    html = "elder_kadun_zu_ketra_q0608_0102.htm";
                    qs.exitQuest(true);
                }
            } else {
                html = "elder_kadun_zu_ketra_q0608_0103.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1 && qs.getQuestItemsCount(7236) == 0L) {
            html = "elder_kadun_zu_ketra_q0608_0106.htm";
        } else if (n == 2 && qs.getQuestItemsCount(7236) >= 1L) {
            html = "elder_kadun_zu_ketra_q0608_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1) {
            qs.giveItems(7236, 1L);
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
