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
 * Quest 615 - 615_MagicalPowerofFire1
 */
@Component
public class Quest615MagicalPowerofFire1 extends Quest {

	public static final int bMc = 31378;
	public static final int bMd = 31379;
	public static final int bMe = 31559;
	public static final int bMf = 7242;
	public static final int bLv = 7081;
	public static final int bMg = 7243;
	public static final int bLW = 7222;
	public static final int bLX = 7223;
	public static final int bLY = 7224;
	public static final int bLZ = 7225;
	public static final int bLx = 1661;
	public static final int[] bMh = new int[19];

	public Quest615MagicalPowerofFire1(QuestManager questManager) {
	super(615, "615_MagicalPowerofFire1", "615_MagicalPowerofFire1");
		this.addStartNpc(31378);
		this.addTalkId(31378);
		this.addTalkId(31379);
		this.addTalkId(31559);
		this.bMh[0] = 21324;
		this.bMh[1] = 21325;
		this.bMh[2] = 21327;
		this.bMh[3] = 21328;
		this.bMh[4] = 21329;
		this.bMh[5] = 21331;
		this.bMh[6] = 21332;
		this.bMh[7] = 21334;
		this.bMh[8] = 21335;
		this.bMh[9] = 21336;
		this.bMh[10] = 21338;
		this.bMh[11] = 21339;
		this.bMh[12] = 21340;
		this.bMh[13] = 21342;
		this.bMh[14] = 21343;
		this.bMh[15] = 21344;
		this.bMh[16] = 21345;
		this.bMh[17] = 21346;
		this.bMh[18] = 21347;
		for (int n : this.bMh) {
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
        if (event.equalsIgnoreCase("quest_accept")) {
            string2 = "herald_naran_q0615_02.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("615_1") && qs.getCond() == 2) {
            if (qs.getQuestItemsCount(1661) < 1L) {
                string2 = "asefas_box_q0615_02.htm";
            } else if (qs.getInt("proval") == 1) {
                string2 = "asefas_box_q0615_04.htm";
                qs.takeItems(1661, 1L);
            } else {
                qs.takeItems(1661, 1L);
                qs.giveItems(7242, 1L);
                string2 = "asefas_box_q0615_03.htm";
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
        switch (n) {
            case 31378: {
                if (n2 == 0) {
                    if (pc.getLevel() >= 74) {
                        if (qs.getQuestItemsCount(7222) == 1L || qs.getQuestItemsCount(7223) == 1L || qs.getQuestItemsCount(7224) == 1L || qs.getQuestItemsCount(7225) == 1L) {
                            if (qs.getQuestItemsCount(7081) == 0L) {
                                html = "herald_naran_q0615_01.htm";
                                break;
                            }
                            html = "completed";
                            qs.exitQuest(true);
                            break;
                        }
                        html = "herald_naran_q0615_01a.htm";
                        qs.exitQuest(true);
                        break;
                    }
                    html = "herald_naran_q0615_01b.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (n2 != 1) break;
                html = "herald_naran_q0615_03.htm";
                break;
            }
            case 31379: {
                if (n2 == 1) {
                    html = "shaman_udan_q0615_01.htm";
                    qs.setCond(2);
                    break;
                }
                if (n2 == 2 && n3 == 1) {
                    html = "shaman_udan_q0615_03.htm";
                    // npc doCast
                    break;
                }
                if (n2 != 3 || qs.getQuestItemsCount(7242) < 1L) break;
                html = "shaman_udan_q0615_04.htm";
                qs.takeItems(7242, qs.getQuestItemsCount(7242));
                qs.giveItems(7243, 1L);
                qs.giveItems(7081, 1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                break;
            }
            case 31559: {
                if (n2 != 2) break;
                html = "asefas_box_q0615_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getCond();
        int n2 = qs.getInt("proval");
        if (n == 2 && n2 == 0) {
            // npc doCast
        }
        return null;
    
	}

}
