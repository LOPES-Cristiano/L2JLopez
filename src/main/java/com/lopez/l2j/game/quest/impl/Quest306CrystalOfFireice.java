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
 * Quest 306 - 306_CrystalOfFireice
 */
@Component
public class Quest306CrystalOfFireice extends Quest {

	public static final int bfo = 30004;
	public static final int bfp = 20109;
	public static final int bfq = 20110;
	public static final int bfr = 20112;
	public static final int bfs = 20113;
	public static final int bft = 20114;
	public static final int bfu = 20115;
	public static final int bfv = 1020;
	public static final int bfw = 1021;

	public Quest306CrystalOfFireice(QuestManager questManager) {
		super(306, "306_CrystalOfFireice", "306_CrystalOfFireice");
		addStartNpc(30004);
		addKillId(20113);
		addKillId(20114);
		addKillId(20112);
		addKillId(20115);
		addKillId(20110);
		addKillId(20109);
		registerQuestItems(1020, 1021);
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
            qs.setState(State.STARTED);
            qs.setCond(1);
            string2 = "katrine_q0306_04.htm";
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("reply_2")) {
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
            string2 = "katrine_q0306_08.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30004) break;
                if (pc.getLevel() >= 17) {
                    html = "katrine_q0306_03.htm";
                    break;
                }
                html = "katrine_q0306_02.htm";
                break;
            }
            case 2: {
                if (n != 30004) break;
                if (qs.getCond() == 1 && qs.getQuestItemsCount(1020) == 0L && qs.getQuestItemsCount(1021) == 0L) {
                    html = "katrine_q0306_05.htm";
                    break;
                }
                if (qs.getCond() != 1 || qs.getQuestItemsCount(1020) <= 0L && qs.getQuestItemsCount(1021) <= 0L) break;
                if (qs.getQuestItemsCount(1020) + qs.getQuestItemsCount(1021) >= 10L) {
                    qs.giveItems(57, 40L * qs.getQuestItemsCount(1020) + 40L * qs.getQuestItemsCount(1021) + 5000L);
                } else {
                    qs.giveItems(57, 40L * qs.getQuestItemsCount(1020) + 40L * qs.getQuestItemsCount(1021));
                }
                qs.takeItems(1020, -1L);
                qs.takeItems(1021, -1L);
                html = "katrine_q0306_07.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20109 && ThreadLocalRandom.current().nextInt(1000) < 925) {
            qs.rollAndGive(1020, 1, 100.0);
        } else if (n == 20110 && ThreadLocalRandom.current().nextInt(100) < 90) {
            qs.rollAndGive(1021, 1, 100.0);
        } else if (n == 20112 && ThreadLocalRandom.current().nextInt(100) < 90) {
            qs.rollAndGive(1020, 1, 100.0);
        } else if (n == 20113 && ThreadLocalRandom.current().nextInt(1000) < 925) {
            qs.rollAndGive(1021, 1, 100.0);
        } else if (n == 20114 && ThreadLocalRandom.current().nextInt(1000) < 925) {
            qs.rollAndGive(1020, 1, 100.0);
        } else if (n == 20115 && ThreadLocalRandom.current().nextInt(100) < 95) {
            qs.rollAndGive(1021, 1, 100.0);
        }
        return null;
    
	}

}
