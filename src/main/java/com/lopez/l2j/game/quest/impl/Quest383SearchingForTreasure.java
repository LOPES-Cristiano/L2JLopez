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
 * Quest 383 - 383_SearchingForTreasure
 */
@Component
public class Quest383SearchingForTreasure extends Quest {

	public static final int bxQ = 30890;
	public static final int bxR = 31148;
	public static final int bqR = 5915;
	public static final int bxS = 1661;
	public static final int bxT = 2450;
	public static final int bxU = 2451;
	public static final int azF = 956;
	public static final int bxV = 952;
	public static final int bxW = 4481;
	public static final int bxX = 4482;
	public static final int bxY = 4483;
	public static final int bxZ = 4484;
	public static final int bya = 4485;
	public static final int byb = 4486;
	public static final int byc = 4487;
	public static final int byd = 4488;
	public static final int bye = 4489;
	public static final int byf = 4490;
	public static final int byg = 4491;
	public static final int byh = 4492;
	public static final int byi = 1337;
	public static final int byj = 1338;
	public static final int byk = 1339;
	public static final int biP = 3447;
	public static final int biS = 3450;
	public static final int biV = 3453;
	public static final int biY = 3456;
	public static final int bka = 4408;
	public static final int bkb = 4409;
	public static final int byl = 4418;
	public static final int bym = 4419;

	public Quest383SearchingForTreasure(QuestManager questManager) {
	super(383, "383_SearchingForTreasure", "383_SearchingForTreasure");
		this.addStartNpc(30890);
		this.addTalkId(31148);
		this.addQuestItem(5915);
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
        int n = qs.getInt("treasure_hunt");
        int n2 = getFirstStartNpc();
        if (n2 == 30890) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("treasure_hunt", String.valueOf(1), true);
                qs.takeItems(5915, 1L);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "trader_espen_q0383_08.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "trader_espen_q0383_04.htm";
            } else if (event.equalsIgnoreCase("reply_2") && qs.getQuestItemsCount(5915) > 0L) {
                qs.giveItems(57, 1000L);
                qs.unset("treasure_hunt");
                qs.takeItems(5915, 1L);
                string2 = "trader_espen_q0383_05.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = qs.getQuestItemsCount(5915) > 0L ? "trader_espen_q0383_06.htm" : "trader_espen_q0383_07.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = "trader_espen_q0383_09.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                string2 = "trader_espen_q0383_10.htm";
            } else if (event.equalsIgnoreCase("reply_6")) {
                string2 = "trader_espen_q0383_11.htm";
            } else if (event.equalsIgnoreCase("reply_7") && n == 1) {
                qs.setCond(2);
                qs.set("treasure_hunt", String.valueOf(2), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "trader_espen_q0383_12.htm";
            }
        } else if (n2 == 31148 && event.equalsIgnoreCase("reply_1")) {
            if (qs.getQuestItemsCount(1661) == 0L) {
                string2 = "pirates_t_chest_q0383_02.htm";
            } else if (n == 2 && qs.getQuestItemsCount(1661) >= 1L) {
                qs.takeItems(1661, 1L);
                qs.unset("treasure_hunt");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "pirates_t_chest_q0383_03.htm";
                int n3 = 0;
                int n4 = ThreadLocalRandom.current().nextInt(100);
                if (n4 < 5) {
                    qs.giveItems(2450, 1L);
                } else if (n4 < 6) {
                    qs.giveItems(2451, 1L);
                } else if (n4 < 18) {
                    qs.giveItems(956, 1L);
                } else if (n4 < 28) {
                    qs.giveItems(952, 1L);
                } else {
                    n3 += 500;
                }
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 25) {
                    qs.giveItems(4481, 1L);
                } else if (n5 < 50) {
                    qs.giveItems(4482, 1L);
                } else if (n5 < 75) {
                    qs.giveItems(4483, 1L);
                } else if (n5 < 100) {
                    qs.giveItems(4484, 1L);
                } else if (n5 < 125) {
                    qs.giveItems(4485, 1L);
                } else if (n5 < 150) {
                    qs.giveItems(4486, 1L);
                } else if (n5 < 175) {
                    qs.giveItems(4487, 1L);
                } else if (n5 < 200) {
                    qs.giveItems(4488, 1L);
                } else if (n5 < 225) {
                    qs.giveItems(4489, 1L);
                } else if (n5 < 250) {
                    qs.giveItems(4490, 1L);
                } else if (n5 < 275) {
                    qs.giveItems(4491, 1L);
                } else if (n5 < 300) {
                    qs.giveItems(4492, 1L);
                } else {
                    n3 += 300;
                }
                int n6 = ThreadLocalRandom.current().nextInt(100);
                if (n6 < 4) {
                    qs.giveItems(1337, 1L);
                } else if (n6 < 8) {
                    qs.giveItems(1338, 2L);
                } else if (n6 < 12) {
                    qs.giveItems(1339, 2L);
                } else if (n6 < 16) {
                    qs.giveItems(3447, 2L);
                } else if (n6 < 20) {
                    qs.giveItems(3450, 1L);
                } else if (n6 < 25) {
                    qs.giveItems(3453, 1L);
                } else if (n6 < 27) {
                    qs.giveItems(3456, 1L);
                } else {
                    n3 += 500;
                }
                int n7 = ThreadLocalRandom.current().nextInt(100);
                if (n7 < 20) {
                    qs.giveItems(4408, 1L);
                } else if (n7 < 40) {
                    qs.giveItems(4409, 1L);
                } else if (n7 < 60) {
                    qs.giveItems(4418, 1L);
                } else if (n7 < 80) {
                    qs.giveItems(4419, 1L);
                } else {
                    n3 += 500;
                }
                qs.giveItems(57, n3);
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("treasure_hunt");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30890) break;
                if (pc.getLevel() < 42) {
                    html = "trader_espen_q0383_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() >= 42 && qs.getQuestItemsCount(5915) == 0L) {
                    html = "trader_espen_q0383_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() < 42 || qs.getQuestItemsCount(5915) <= 0L) break;
                html = "trader_espen_q0383_03.htm";
                break;
            }
            case 2: {
                if (n2 == 30890) {
                    if (n == 1) {
                        html = "trader_espen_q0383_13.htm";
                        break;
                    }
                    if (n != 2) break;
                    html = "trader_espen_q0383_14.htm";
                    break;
                }
                if (n2 != 31148 || n != 2) break;
                html = "pirates_t_chest_q0383_01.htm";
            }
        }
        return html;
    
	}

}
