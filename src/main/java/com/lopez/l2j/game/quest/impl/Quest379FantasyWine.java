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
 * Quest 379 - 379_FantasyWine
 */
@Component
public class Quest379FantasyWine extends Quest {

	public static final int HARLAN = 30074;
	public static final int Enku_Orc_Champion = 20291;
	public static final int Enku_Orc_Shaman = 20292;
	public static final int LEAF_OF_EUCALYPTUS = 5893;
	public static final int STONE_OF_CHILL = 5894;
	public static final int[] REWARD = new int[]{5956, 5957, 5958};

	public Quest379FantasyWine(QuestManager questManager) {
	super(379, "379_FantasyWine", "379_FantasyWine");
		this.addStartNpc(30074);
		this.addKillId(20291);
		this.addKillId(20292);
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
        if (event.equalsIgnoreCase("hitsran_q0379_06.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("reward")) {
            qs.takeItems(5893, -1L);
            qs.takeItems(5894, -1L);
            int n = ThreadLocalRandom.current().nextInt(100);
            if (n < 25) {
                qs.giveItems(this.REWARD[0], 1L);
                string2 = "hitsran_q0379_11.htm";
            } else if (n < 50) {
                qs.giveItems(this.REWARD[1], 1L);
                string2 = "hitsran_q0379_12.htm";
            } else {
                qs.giveItems(this.REWARD[2], 1L);
                string2 = "hitsran_q0379_13.htm";
            }
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("hitsran_q0379_05.htm")) {
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getStateId();
        int n3 = 0;
        if (n2 != 1) {
            n3 = qs.getCond();
        }
        if (n == 30074) {
            if (n3 == 0) {
                if (pc.getLevel() < 20) {
                    html = "hitsran_q0379_01.htm";
                    qs.exitQuest(true);
                } else {
                    html = "hitsran_q0379_02.htm";
                }
            } else if (n3 == 1) {
                html = qs.getQuestItemsCount(5893) < 80L && qs.getQuestItemsCount(5894) < 100L ? "hitsran_q0379_07.htm" : (qs.getQuestItemsCount(5893) == 80L && qs.getQuestItemsCount(5894) < 100L ? "hitsran_q0379_08.htm" : (qs.getQuestItemsCount(5893) < 80L && qs.getQuestItemsCount(5894) == 100L ? "hitsran_q0379_09.htm" : "hitsran_q0379_02.htm"));
            } else if (n3 == 2) {
                if (qs.getQuestItemsCount(5893) >= 80L && qs.getQuestItemsCount(5894) >= 100L) {
                    html = "hitsran_q0379_10.htm";
                } else {
                    qs.setCond(1);
                    if (qs.getQuestItemsCount(5893) < 80L && qs.getQuestItemsCount(5894) < 100L) {
                        html = "hitsran_q0379_07.htm";
                    } else if (qs.getQuestItemsCount(5893) >= 80L && qs.getQuestItemsCount(5894) < 100L) {
                        html = "hitsran_q0379_08.htm";
                    } else if (qs.getQuestItemsCount(5893) < 80L && qs.getQuestItemsCount(5894) >= 100L) {
                        html = "hitsran_q0379_09.htm";
                    }
                }
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getCond() == 1) {
            if (n == 20291 && qs.getQuestItemsCount(5893) < 80L) {
                qs.giveItems(5893, 1L);
            } else if (n == 20292 && qs.getQuestItemsCount(5894) < 100L) {
                qs.giveItems(5894, 1L);
            }
            if (qs.getQuestItemsCount(5893) >= 80L && qs.getQuestItemsCount(5894) >= 100L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
