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
 * Quest 610 - 610_MagicalPowerofWater2
 */
@Component
public class Quest610MagicalPowerofWater2 extends Quest {

	public static final int bLs = 31372;
	public static final int bLz = 31560;
	public static final int bLw = 7238;
	public static final int ICE_HEART_OF_ASHUTAR = 7239;
	public static final int bLA = 4589;
	public static final int bLB = 4594;
	public static final int bLC = 25316;
	private NpcInstance bLD = null;

	public Quest610MagicalPowerofWater2(QuestManager questManager) {
	super(610, "610_MagicalPowerofWater2", "610_MagicalPowerofWater2");
		this.addStartNpc(31372);
		this.addTalkId(31560);
		this.addKillId(25316);
		this.addQuestItem(this.ICE_HEART_OF_ASHUTAR);
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
            string2 = "shaman_asefa_q0610_0104.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("610_1")) {
            if (Math.max(0L, 0L) + 10800000L > System.currentTimeMillis()) {
                string2 = "totem_of_barka_q0610_0204.htm";
            } else if (qs.getQuestItemsCount(7238) >= 1L && (this.bLD == null || this.bLD.isDead())) {
                qs.takeItems(7238, 1L);
                this.bLD = qs.addSpawn(25316, 104825, -36926, -1136);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                string2 = "totem_of_barka_q0610_0203.htm";
            }
        } else if (event.equalsIgnoreCase("610_3")) {
            if (qs.getQuestItemsCount(this.ICE_HEART_OF_ASHUTAR) >= 1L) {
                qs.takeItems(this.ICE_HEART_OF_ASHUTAR, -1L);
                qs.addExpAndSp(10000L, 0L);
                qs.giveItems(ThreadLocalRandom.current().nextInt(4589, 4594 + 1), 5L, true);
                qs.playSound(QuestState.SOUND_FINISH);
                string2 = "shaman_asefa_q0610_0301.htm";
                qs.exitQuest(true);
            } else {
                string2 = "shaman_asefa_q0610_0302.htm";
            }
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
        if (n == 31372) {
            if (n2 == 0) {
                if (pc.getLevel() >= 75) {
                    if (qs.getQuestItemsCount(7238) >= 1L) {
                        html = "shaman_asefa_q0610_0101.htm";
                    } else {
                        html = "shaman_asefa_q0610_0102.htm";
                        qs.exitQuest(true);
                    }
                } else {
                    html = "shaman_asefa_q0610_0103.htm";
                    qs.exitQuest(true);
                }
            } else if (n2 == 1) {
                html = "shaman_asefa_q0610_0105.htm";
            } else if (n2 == 2) {
                html = "shaman_asefa_q0610_0202.htm";
            } else if (n2 == 3 && qs.getQuestItemsCount(this.ICE_HEART_OF_ASHUTAR) >= 1L) {
                html = "shaman_asefa_q0610_0201.htm";
            }
        } else if (n == 31560) {
            if (Math.max(0L, 0L) + 10800000L > System.currentTimeMillis()) {
                html = "totem_of_barka_q0610_0204.htm";
            } else if (this.bLD != null && !this.bLD.isDead()) {
                html = "totem_of_barka_q0610_0202.htm";
            } else if (n2 == 1) {
                html = "totem_of_barka_q0610_0101.htm";
            } else if (n2 == 2) {
                if (this.bLD == null || this.bLD.isDead()) {
                    this.bLD = qs.addSpawn(25316, 104825, -36926, -1136);
                    html = "totem_of_barka_q0610_0204.htm";
                } else {
                    html = "<html><body>Already in spawn.</body></html>";
                }
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount(this.ICE_HEART_OF_ASHUTAR) == 0L && (npc == null || npc.getNpcId() == 25316)) {
            qs.giveItems(this.ICE_HEART_OF_ASHUTAR, 1L);
            qs.setCond(3);
            if (this.bLD != null) {
                this.bLD.deleteMe();
            }
            this.bLD = null;
        }
        return null;
    
	}

}
