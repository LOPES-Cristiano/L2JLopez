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
 * Quest 635 - 635_InTheDimensionalRift
 */
@Component
public class Quest635InTheDimensionalRift extends Quest {

	public static final int bQz = 7079;
	public static final int[][] bQA = new int[][]{new int[0], {-41572, 209731, -5087}, {42950, 143934, -5381}, {45256, 123906, -5411}, {46192, 170290, -4981}, {111273, 174015, -5437}, {-20221, -250795, -8160}, {-21726, 77385, -5171}, {140405, 79679, -5427}, {-52366, 79097, -4741}, {118311, 132797, -4829}, {172185, -17602, -4901}, {83000, 209213, -5439}, {-19500, 13508, -4901}, {113865, 84543, -6541}};

	public Quest635InTheDimensionalRift(QuestManager questManager) {
	super(635, "635_InTheDimensionalRift", "635_InTheDimensionalRift");
		int n;
		for (n = 31494; n < 31508; ++n) {
		this.addStartNpc(n);
		}
		for (n = 31095; n <= 31126; ++n) {
		if (n == 31111 || n == 31112 || n == 31113) continue;
		this.addStartNpc(n);
		}
		for (n = 31127; n <= 31141; ++n) {
		if (n == 31132 || n == 31133 || n == 31134 || n == 31135 || n == 31136) continue;
		this.addStartNpc(n);
		}
		n = 31488;
		while (n < 31494) {
		this.addTalkId(n++);
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
        int n = qs.getInt("id");
        String string3 = qs.get("loc");
        if (event.equals("5.htm")) {
            if (n > 0 || string3 != null) {
                if (pc.getLastNpc() != null && this.ac(pc.getLastNpc().getNpcId()) && !this.m(qs)) {
                    string2 = "Sorry...";
                    qs.exitQuest(true);
                    return string2;
                }
                qs.setState(State.STARTED);
                qs.setCond(1);
                pc.teleToLocation(-114790, -180576, -6781);
            } else {
                string2 = "What are you trying to do?";
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("6.htm")) {
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getInt("id");
        String string2 = qs.get("loc");
        if (this.ac(n) || this.ad(n) || this.ae(n)) {
            if (pc.getLevel() < 20) {
                qs.exitQuest(true);
                html = "1.htm";
            } else if (qs.getQuestItemsCount(7079) == 0L) {
                html = this.ad(n) || this.ae(n) ? "3.htm" : "3-ziggurat.htm";
            } else {
                qs.set("loc", pc.getX() + "," + pc.getY() + "," + pc.getZ());
                html = this.ad(n) ? "4.htm" : "4-ziggurat.htm";
            }
        } else if (n2 > 0) {
            int[] nArray = bQA[n2];
            pc.teleToLocation(nArray[0], nArray[1], nArray[2]);
            html = "7.htm";
            qs.exitQuest(true);
        } else if (string2 != null) {
            pc.teleToLocation(new int[]{0, 0, 0});
            html = "7.htm";
            qs.exitQuest(true);
        } else {
            html = "Where are you from?";
            qs.exitQuest(true);
        }
        return html;
    
	}

	private boolean m(QuestState qs) {
		int n = qs.playerChar() != null ? qs.playerChar().level() : 1;
		long n2 = n < 30 ? 2000 : (n < 40 ? 4500 : (n < 50 ? 8000 : (n < 60 ? 12500 : (n < 70 ? 18000 : 24500))));
		if (qs.getQuestItemsCount(57) < n2) {
			return false;
		}
		qs.takeItems(57, n2);
		return true;
	}

	private boolean ac(int n) {
		return n >= 31095 && n <= 31126;
	}

	private boolean ad(int n) {
		return n >= 31494 && n <= 31508;
	}

	private boolean ae(int n) {
		return n >= 31127 && n <= 31141;
	}

}
