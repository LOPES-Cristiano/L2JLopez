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
 * Quest 377 - 377_ExplorationOfGiantsCavePart2
 */
@Component
public class Quest377ExplorationOfGiantsCavePart2 extends Quest {

	public static final int bwk = 31147;
	public static final int bwU = 20654;
	public static final int bwV = 20656;
	public static final int bnT = 20657;
	public static final int bnU = 20658;
	public static final int bwW = 5950;
	public static final int bwX = 5951;
	public static final int bwY = 5952;
	public static final int bwZ = 5953;
	public static final int bxa = 5954;
	public static final int bxb = 5945;
	public static final int bxc = 5946;
	public static final int bxd = 5947;
	public static final int bxe = 5948;
	public static final int bxf = 5949;
	public static final int bxg = 5955;
	public static final int bxh = 5422;
	public static final int bxi = 5420;
	public static final int bxj = 5336;
	public static final int bxk = 5338;
	public static final int bxl = 5892;

	public Quest377ExplorationOfGiantsCavePart2(QuestManager questManager) {
	super(377, "377_ExplorationOfGiantsCavePart2", "377_ExplorationOfGiantsCavePart2");
		this.addKillId(20654, 20656, 20657, 20658);
		this.addStartNpc(31147);
		this.addTalkId(31147);
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
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "sobling_q0377_03.htm";
        } else if (event.equalsIgnoreCase("reply_1") && (qs.getQuestItemsCount(5950) >= 1L && qs.getQuestItemsCount(5951) >= 1L && qs.getQuestItemsCount(5952) >= 1L && qs.getQuestItemsCount(5953) >= 1L && qs.getQuestItemsCount(5954) >= 1L || qs.getQuestItemsCount(5945) >= 1L && qs.getQuestItemsCount(5946) >= 1L && qs.getQuestItemsCount(5947) >= 1L && qs.getQuestItemsCount(5948) >= 1L && qs.getQuestItemsCount(5949) >= 1L)) {
            if (qs.getQuestItemsCount(5950) > 0L && qs.getQuestItemsCount(5951) > 0L && qs.getQuestItemsCount(5952) > 0L && qs.getQuestItemsCount(5953) > 0L && qs.getQuestItemsCount(5954) > 0L) {
                qs.takeItems(5950, 1L);
                qs.takeItems(5951, 1L);
                qs.takeItems(5952, 1L);
                qs.takeItems(5953, 1L);
                qs.takeItems(5954, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5422, 1L);
                } else {
                    qs.giveItems(5420, 1L);
                }
            }
            if (qs.getQuestItemsCount(5945) > 0L && qs.getQuestItemsCount(5946) > 0L && qs.getQuestItemsCount(5947) > 0L && qs.getQuestItemsCount(5948) > 0L && qs.getQuestItemsCount(5949) > 0L) {
                qs.takeItems(5945, 1L);
                qs.takeItems(5946, 1L);
                qs.takeItems(5947, 1L);
                qs.takeItems(5948, 1L);
                qs.takeItems(5949, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5336, 1L);
                } else {
                    qs.giveItems(5338, 1L);
                }
            }
            string2 = "sobling_q0377_05.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "sobling_q0377_04.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "sobling_q0377_06.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            qs.exitQuest(true);
            qs.playSound(QuestState.SOUND_FINISH);
            string2 = "sobling_q0377_07.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        switch (n) {
            case 1: {
                if (pc.getLevel() >= 57 && qs.getQuestItemsCount(5892) > 0L) {
                    html = "sobling_q0377_01.htm";
                    break;
                }
                html = "sobling_q0377_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (!(qs.getCond() != 1 || qs.getQuestItemsCount(5950) >= 1L && qs.getQuestItemsCount(5951) >= 1L && qs.getQuestItemsCount(5952) >= 1L && qs.getQuestItemsCount(5953) >= 1L && qs.getQuestItemsCount(5954) >= 1L || qs.getQuestItemsCount(5945) >= 1L && qs.getQuestItemsCount(5946) >= 1L && qs.getQuestItemsCount(5947) >= 1L && qs.getQuestItemsCount(5948) >= 1L && qs.getQuestItemsCount(5949) >= 1L)) {
                    html = "sobling_q0377_04.htm";
                }
                if (qs.getCond() != 1 || (qs.getQuestItemsCount(5950) < 1L || qs.getQuestItemsCount(5951) < 1L || qs.getQuestItemsCount(5952) < 1L || qs.getQuestItemsCount(5953) < 1L || qs.getQuestItemsCount(5954) < 1L) && (qs.getQuestItemsCount(5945) < 1L || qs.getQuestItemsCount(5946) < 1L || qs.getQuestItemsCount(5947) < 1L || qs.getQuestItemsCount(5948) < 1L || qs.getQuestItemsCount(5949) < 1L)) break;
                if (qs.getQuestItemsCount(5950) > 0L && qs.getQuestItemsCount(5951) > 0L && qs.getQuestItemsCount(5952) > 0L && qs.getQuestItemsCount(5953) > 0L && qs.getQuestItemsCount(5954) > 0L) {
                    qs.takeItems(5950, 1L);
                    qs.takeItems(5951, 1L);
                    qs.takeItems(5952, 1L);
                    qs.takeItems(5953, 1L);
                    qs.takeItems(5954, 1L);
                    if (ThreadLocalRandom.current().nextInt(2) == 0) {
                        qs.giveItems(5422, 1L);
                    } else {
                        qs.giveItems(5420, 1L);
                    }
                }
                if (qs.getQuestItemsCount(5945) > 0L && qs.getQuestItemsCount(5946) > 0L && qs.getQuestItemsCount(5947) > 0L && qs.getQuestItemsCount(5948) > 0L && qs.getQuestItemsCount(5949) > 0L) {
                    qs.takeItems(5945, 1L);
                    qs.takeItems(5946, 1L);
                    qs.takeItems(5947, 1L);
                    qs.takeItems(5948, 1L);
                    qs.takeItems(5949, 1L);
                    if (ThreadLocalRandom.current().nextInt(2) == 0) {
                        qs.giveItems(5336, 1L);
                    } else {
                        qs.giveItems(5338, 1L);
                    }
                }
                html = "sobling_q0377_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20654 || n == 20656) {
            qs.rollAndGive(5955, 1, 18.0);
        } else if (n == 20657) {
            qs.rollAndGive(5955, 1, 14.0);
        } else if (n == 20658) {
            qs.rollAndGive(5955, 1, 12.0);
        }
        return null;
    
	}

}
