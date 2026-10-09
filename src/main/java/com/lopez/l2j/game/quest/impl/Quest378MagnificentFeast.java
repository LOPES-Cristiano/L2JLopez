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
 * Quest 378 - 378_MagnificentFeast
 */
@Component
public class Quest378MagnificentFeast extends Quest {

	public static final int bxm = 30594;
	public static final int bxn = 5956;
	public static final int bxo = 5957;
	public static final int bxp = 5958;
	public static final int bxq = 4421;
	public static final int bxr = 5959;
	public static final int bxs = 1455;
	public static final int bxt = 1456;
	public static final int bxu = 1457;
	public static final Map<Integer, int[]> bxv = new HashMap<Integer, int[]>();

	public Quest378MagnificentFeast(QuestManager questManager) {
	super(378, "378_MagnificentFeast", "378_MagnificentFeast");
		this.addStartNpc(bxm);
		this.bxv.put(9, new int[]{847, 1, 5700});
		this.bxv.put(10, new int[]{846, 2, 0});
		this.bxv.put(12, new int[]{909, 1, 25400});
		this.bxv.put(17, new int[]{846, 2, 1200});
		this.bxv.put(18, new int[]{879, 1, 6900});
		this.bxv.put(20, new int[]{890, 2, 8500});
		this.bxv.put(33, new int[]{879, 1, 8100});
		this.bxv.put(34, new int[]{910, 1, 0});
		this.bxv.put(36, new int[]{910, 1, 0});
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
        int n = qs.getStateId();
        int n2 = qs.getCond();
        int n3 = qs.getInt("score");
        if (event.equalsIgnoreCase("quest_accept") && n == 1) {
            string2 = "warehouse_chief_ranspo_q0378_03.htm";
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("378_1") && n == 2) {
            if (n2 == 1 && qs.getQuestItemsCount(bxn) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_05.htm";
                qs.takeItems(bxn, 1L);
                qs.setCond(2);
                qs.set("score", String.valueOf(n3 + 1));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_08.htm";
            }
        } else if (event.equalsIgnoreCase("378_2") && n == 2) {
            if (n2 == 1 && qs.getQuestItemsCount(bxo) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_06.htm";
                qs.takeItems(bxo, 1L);
                qs.setCond(2);
                qs.set("score", String.valueOf(n3 + 2));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_08.htm";
            }
        } else if (event.equalsIgnoreCase("378_3") && n == 2) {
            if (n2 == 1 && qs.getQuestItemsCount(bxp) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_07.htm";
                qs.takeItems(bxp, 1L);
                qs.setCond(2);
                qs.set("score", String.valueOf(n3 + 4));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_08.htm";
            }
        } else if (event.equalsIgnoreCase("378_5") && n == 2) {
            if (n2 == 2 && qs.getQuestItemsCount(bxq) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_12.htm";
                qs.takeItems(bxq, 1L);
                qs.setCond(3);
            } else {
                string2 = "warehouse_chief_ranspo_q0378_10.htm";
            }
        } else if (event.equalsIgnoreCase("378_6") && n == 2) {
            if (n2 == 3 && qs.getQuestItemsCount(bxs) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_14.htm";
                qs.takeItems(bxs, 1L);
                qs.setCond(4);
                qs.set("score", String.valueOf(n3 + 8));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_17.htm";
            }
        } else if (event.equalsIgnoreCase("378_7") && n == 2) {
            if (n2 == 3 && qs.getQuestItemsCount(bxt) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_15.htm";
                qs.takeItems(bxt, 1L);
                qs.setCond(4);
                qs.set("score", String.valueOf(n3 + 16));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_17.htm";
            }
        } else if (event.equalsIgnoreCase("378_8") && n == 2) {
            if (n2 == 3 && qs.getQuestItemsCount(bxu) > 0L) {
                string2 = "warehouse_chief_ranspo_q0378_16.htm";
                qs.takeItems(bxu, 1L);
                qs.setCond(4);
                qs.set("score", String.valueOf(n3 + 32));
            } else {
                string2 = "warehouse_chief_ranspo_q0378_17.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        if (npc.getNpcId() != bxm) {
            return html;
        }
        int n = qs.getStateId();
        int n2 = qs.getCond();
        if (n == 1) {
            if (pc.getLevel() < 20) {
                html = "warehouse_chief_ranspo_q0378_01.htm";
                qs.exitQuest(true);
            } else {
                html = "warehouse_chief_ranspo_q0378_02.htm";
                qs.setCond(0);
            }
        } else if (n2 == 1 && n == 2) {
            html = "warehouse_chief_ranspo_q0378_04.htm";
        } else if (n2 == 2 && n == 2) {
            html = qs.getQuestItemsCount(bxq) > 0L ? "warehouse_chief_ranspo_q0378_11.htm" : "warehouse_chief_ranspo_q0378_10.htm";
        } else if (n2 == 3 && n == 2) {
            html = "warehouse_chief_ranspo_q0378_13.htm";
        } else if (n2 == 4 && n == 2) {
            int[] nArray = this.bxv.get(qs.getInt("score"));
            if (qs.getQuestItemsCount(bxr) > 0L && nArray != null) {
                html = "warehouse_chief_ranspo_q0378_20.htm";
                qs.takeItems(bxr, 1L);
                qs.giveItems(nArray[0], nArray[1]);
                if (nArray[2] > 0) {
                    qs.giveItems(57, nArray[2]);
                }
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                html = "warehouse_chief_ranspo_q0378_19.htm";
            }
        }
        return html;
    
	}

}
