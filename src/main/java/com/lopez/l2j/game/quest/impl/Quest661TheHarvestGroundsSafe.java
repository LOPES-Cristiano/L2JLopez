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
 * Quest 661 - 661_TheHarvestGroundsSafe
 */
@Component
public class Quest661TheHarvestGroundsSafe extends Quest {

	public static final int aIm = 30210;
	public static final int bUp = 21095;
	public static final int bUq = 21096;
	public static final int bUr = 21097;
	public static final int bUs = 8283;
	public static final int bUt = 8284;
	public static final int bUu = 8285;

	public Quest661TheHarvestGroundsSafe(QuestManager questManager) {
	super(661, "661_TheHarvestGroundsSafe", "661_TheHarvestGroundsSafe");
		this.addStartNpc(30210);
		this.addKillId(21095, 21096, 21097);
		this.addQuestItem(8283, 8284, 8285);
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
        int n = qs.getInt("clear_gathering_site_cookie");
        int n2 = getFirstStartNpc();
        if (n2 == 30210) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("clear_gathering_site", String.valueOf(11), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "warehouse_keeper_norman_q0661_0103.htm";
            } else if (event.equalsIgnoreCase("reply_1") && n == 1) {
                string2 = "warehouse_keeper_norman_q0661_0201.htm";
            } else if (event.equalsIgnoreCase("reply_3") && n == 1) {
                if (qs.getQuestItemsCount(8283) == 0L && qs.getQuestItemsCount(8284) == 0L && qs.getQuestItemsCount(8285) == 0L) {
                    string2 = "warehouse_keeper_norman_q0661_0202.htm";
                }
                if (qs.getQuestItemsCount(8283) + qs.getQuestItemsCount(8284) + qs.getQuestItemsCount(8285) >= 10L) {
                    qs.giveItems(57, 5773L + 57L * qs.getQuestItemsCount(8283) + 56L * qs.getQuestItemsCount(8284) + 60L * qs.getQuestItemsCount(8285));
                } else {
                    qs.giveItems(57, 57L * qs.getQuestItemsCount(8283) + 56L * qs.getQuestItemsCount(8284) + 60L * qs.getQuestItemsCount(8285));
                }
                string2 = "warehouse_keeper_norman_q0661_0203.htm";
                qs.takeItems(8283, -1L);
                qs.takeItems(8284, -1L);
                qs.takeItems(8285, -1L);
            } else if (event.equalsIgnoreCase("reply_4") && n == 1) {
                string2 = "warehouse_keeper_norman_q0661_0204.htm";
                qs.unset("clear_gathering_site_cookie");
                qs.unset("clear_gathering_site");
                qs.exitQuest(true);
                qs.playSound(QuestState.SOUND_FINISH);
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("clear_gathering_site");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30210) break;
                if (pc.getLevel() < 21) {
                    html = "warehouse_keeper_norman_q0661_0102.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "warehouse_keeper_norman_q0661_0101.htm";
                break;
            }
            case 2: {
                if (n2 != 30210 || n != 11) break;
                if (qs.getQuestItemsCount(8283) == 0L && qs.getQuestItemsCount(8284) == 0L && qs.getQuestItemsCount(8285) == 0L) {
                    html = "warehouse_keeper_norman_q0661_0106.htm";
                    break;
                }
                qs.set("clear_gathering_site_cookie", String.valueOf(1), true);
                html = "warehouse_keeper_norman_q0661_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("clear_gathering_site");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 11) {
            int n3;
            if (n2 == 21095) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 508) {
                    qs.giveItems(8283, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21096) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 500) {
                    qs.giveItems(8284, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21097 && (n3 = ThreadLocalRandom.current().nextInt(1000)) < 516) {
                qs.giveItems(8285, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
