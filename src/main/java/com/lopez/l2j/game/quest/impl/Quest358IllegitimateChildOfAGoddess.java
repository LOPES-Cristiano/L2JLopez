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
 * Quest 358 - 358_IllegitimateChildOfAGoddess
 */
@Component
public class Quest358IllegitimateChildOfAGoddess extends Quest {

	public static final int aGN = 30862;
	public static final int brp = 20672;
	public static final int brq = 20673;
	public static final int brr = 5868;
	public static final int brs = 6329;
	public static final int brt = 6331;
	public static final int bru = 6333;
	public static final int brv = 6335;
	public static final int brw = 6337;
	public static final int brx = 6339;
	public static final int bry = 5364;
	public static final int brz = 5366;

	public Quest358IllegitimateChildOfAGoddess(QuestManager questManager) {
	super(358, "358_IllegitimateChildOfAGoddess", "358_IllegitimateChildOfAGoddess");
		this.addStartNpc(30862);
		this.addKillId(20672, 20673);
		this.addQuestItem(5868);
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
        int n = getFirstStartNpc();
        if (n == 30862) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("illegitimate_child", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "grandmaster_oltlin_q0358_05.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "grandmaster_oltlin_q0358_04.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("illegitimate_child");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30862) break;
                if (pc.getLevel() < 63) {
                    html = "grandmaster_oltlin_q0358_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() < 63) break;
                html = "grandmaster_oltlin_q0358_02.htm";
                break;
            }
            case 2: {
                if (n2 != 30862) break;
                if (n == 1 && qs.getQuestItemsCount(5868) < 108L) {
                    html = "grandmaster_oltlin_q0358_06.htm";
                    break;
                }
                if (n != 1 || qs.getQuestItemsCount(5868) < 108L) break;
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 125) {
                    qs.giveItems(6331, 1L);
                } else if (n4 < 250) {
                    qs.giveItems(6337, 1L);
                } else if (n4 < 375) {
                    qs.giveItems(6329, 1L);
                } else if (n4 < 500) {
                    qs.giveItems(6335, 1L);
                } else if (n4 < 625) {
                    qs.giveItems(6333, 1L);
                } else if (n4 < 750) {
                    qs.giveItems(6339, 1L);
                } else if (n4 < 875) {
                    qs.giveItems(5366, 1L);
                } else {
                    qs.giveItems(5364, 1L);
                }
                qs.takeItems(5868, -1L);
                qs.unset("illegitimate_child");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                html = "grandmaster_oltlin_q0358_07.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("illegitimate_child");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 == 20672) {
                if (qs.getQuestItemsCount(5868) < 108L && ThreadLocalRandom.current().nextInt(100) < 71) {
                    qs.giveItems(5868, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                    if (qs.getQuestItemsCount(5868) >= 107L) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    } else {
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 20673 && qs.getQuestItemsCount(5868) < 108L && ThreadLocalRandom.current().nextInt(100) < 74) {
                qs.giveItems(5868, 1L);
                if (qs.getQuestItemsCount(5868) >= 107L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
