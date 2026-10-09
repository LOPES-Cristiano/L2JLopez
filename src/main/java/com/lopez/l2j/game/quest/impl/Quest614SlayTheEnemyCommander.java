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
 * Quest 614 - 614_SlayTheEnemyCommander
 */
@Component
public class Quest614SlayTheEnemyCommander extends Quest {

	public static final int bLT = 31377;
	public static final int bLU = 25302;
	public static final int bLV = 7221;
	public static final int bLW = 7222;
	public static final int bLX = 7223;
	public static final int bLY = 7224;
	public static final int bLZ = 7225;
	public static final int bMa = 7241;
	public static final int bMb = 7230;

	public Quest614SlayTheEnemyCommander(QuestManager questManager) {
	super(614, "614_SlayTheEnemyCommander", "614_SlayTheEnemyCommander");
		this.addStartNpc(31377);
		this.addKillId(25302);
		this.addQuestItem(7241);
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
            string2 = "elder_ashas_barka_durai_q0614_0104.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("614_3")) {
            if (qs.getQuestItemsCount(7241) >= 1L) {
                string2 = "elder_ashas_barka_durai_q0614_0201.htm";
                qs.takeItems(7241, -1L);
                qs.giveItems(7230, 1L);
                qs.addExpAndSp(0L, 10000L);
                qs.unset("cond");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                string2 = "elder_ashas_barka_durai_q0614_0106.htm";
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
                if (qs.getQuestItemsCount(7224) == 1L || qs.getQuestItemsCount(7225) == 1L) {
                    html = "elder_ashas_barka_durai_q0614_0101.htm";
                } else {
                    html = "elder_ashas_barka_durai_q0614_0102.htm";
                    qs.exitQuest(true);
                }
            } else {
                html = "elder_ashas_barka_durai_q0614_0103.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1 && qs.getQuestItemsCount(7241) == 0L) {
            html = "elder_ashas_barka_durai_q0614_0106.htm";
        } else if (n == 2 && qs.getQuestItemsCount(7241) >= 1L) {
            html = "elder_ashas_barka_durai_q0614_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1) {
            qs.giveItems(7241, 1L);
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
