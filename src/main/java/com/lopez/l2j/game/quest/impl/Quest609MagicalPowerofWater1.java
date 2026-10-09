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
 * Quest 609 - 609_MagicalPowerofWater1
 */
@Component
public class Quest609MagicalPowerofWater1 extends Quest {

	public static final int WAHKAN = 31371;
	public static final int bLs = 31372;
	public static final int bLt = 31561;
	public static final int bLu = 7237;
	public static final int bLv = 7081;
	public static final int bLw = 7238;
	public static final int bLl = 7212;
	public static final int bLm = 7213;
	public static final int bLn = 7214;
	public static final int bLo = 7215;
	public static final int bLx = 1661;
	public static final int[] bLy = new int[20];

	public Quest609MagicalPowerofWater1(QuestManager questManager) {
	super(609, "609_MagicalPowerofWater1", "609_MagicalPowerofWater1");
		this.addStartNpc(31371);
		this.addTalkId(31372);
		this.addTalkId(31561);
		this.bLy[0] = 21350;
		this.bLy[1] = 21351;
		this.bLy[2] = 21353;
		this.bLy[3] = 21354;
		this.bLy[4] = 21355;
		this.bLy[5] = 21357;
		this.bLy[6] = 21358;
		this.bLy[7] = 21360;
		this.bLy[8] = 21361;
		this.bLy[9] = 21362;
		this.bLy[10] = 21364;
		this.bLy[11] = 21365;
		this.bLy[12] = 21366;
		this.bLy[13] = 21368;
		this.bLy[14] = 21369;
		this.bLy[15] = 21370;
		this.bLy[16] = 21371;
		this.bLy[17] = 21372;
		this.bLy[18] = 21373;
		this.bLy[19] = 21374;
		for (int n : this.bLy) {
		this.addAttackId(n);
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
        if (event.equals("quest_accept")) {
            string2 = "herald_wakan_q0609_02.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("609_1") && qs.getCond() == 2) {
            if (qs.getQuestItemsCount(1661) < 1L) {
                string2 = "udans_box_q0609_02.htm";
            } else if (qs.getInt("proval") == 1) {
                string2 = "udans_box_q0609_04.htm";
                qs.takeItems(1661, 1L);
            } else {
                qs.takeItems(1661, 1L);
                qs.giveItems(7237, 1L);
                string2 = "udans_box_q0609_03.htm";
                qs.setCond(3);
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
        int n3 = qs.getInt("proval");
        if (n == 31371) {
            if (n2 == 0) {
                if (pc.getLevel() >= 74) {
                    if (qs.getQuestItemsCount(7212) == 1L || qs.getQuestItemsCount(7213) == 1L || qs.getQuestItemsCount(7214) == 1L || qs.getQuestItemsCount(7215) == 1L) {
                        if (qs.getQuestItemsCount(7081) == 0L) {
                            html = "herald_wakan_q0609_01.htm";
                        } else {
                            html = "completed";
                            qs.exitQuest(true);
                        }
                    } else {
                        html = "herald_wakan_q0609_01a.htm";
                        qs.exitQuest(true);
                    }
                } else {
                    html = "herald_wakan_q0609_01b.htm";
                    qs.exitQuest(true);
                }
            } else if (n2 == 1) {
                html = "herald_wakan_q0609_03.htm";
            }
        } else if (n == 31372) {
            if (n2 == 1) {
                html = "shaman_asefa_q0609_01.htm";
                qs.setCond(2);
            } else if (n2 == 2 && n3 == 1) {
                html = "shaman_asefa_q0609_03.htm";
                // npc doCast
            } else if (n2 == 3 && qs.getQuestItemsCount(7237) >= 1L) {
                html = "shaman_asefa_q0609_04.htm";
                qs.takeItems(7237, qs.getQuestItemsCount(7237));
                qs.giveItems(7238, 1L);
                qs.giveItems(7081, 1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            }
        } else if (n == 31561 && n2 == 2) {
            html = "udans_box_q0609_01.htm";
        }
        return html;
    
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 2 && qs.getInt("proval") == 0) {
            // npc doCast
        }
        return null;
    
	}

}
