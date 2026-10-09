package com.lopez.l2j.game.quest.impl;

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
 * Quest 316 - 316_DestroyPlaguebringers
 */
@Component
public class Quest316DestroyPlaguebringers extends Quest {

	public static final int bfx = 30155;
	public static final int bfy = 20040;
	public static final int bfz = 20047;
	public static final int bfA = 27020;
	public static final int bfB = 1042;
	public static final int bfC = 1043;

	public Quest316DestroyPlaguebringers(QuestManager questManager) {
		super(316, "316_DestroyPlaguebringers", "316_DestroyPlaguebringers");
		addStartNpc(30155);
		addKillId(20040);
		addKillId(20047);
		addKillId(27020);
		registerQuestItems(1042, 1043);
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
            string2 = "elliasin_q0316_04.htm";
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("reply_2")) {
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
            string2 = "elliasin_q0316_08.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            string2 = "elliasin_q0316_09.htm";
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
                if (n != 30155) break;
                if (pc.getRace() != Race.elf) {
                    html = "elliasin_q0316_00.htm";
                    break;
                }
                if (pc.getLevel() >= 18) {
                    html = "elliasin_q0316_03.htm";
                    break;
                }
                html = "elliasin_q0316_02.htm";
                break;
            }
            case 2: {
                if (n != 30155) break;
                if (qs.getQuestItemsCount(1042) >= 1L || qs.getQuestItemsCount(1043) >= 1L) {
                    html = "elliasin_q0316_07.htm";
                    if (qs.getQuestItemsCount(1042) + qs.getQuestItemsCount(1043) >= 10L) {
                        qs.giveItems(57, qs.getQuestItemsCount(1042) * 30L + qs.getQuestItemsCount(1043) * 10000L + 5000L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(1042) * 30L + qs.getQuestItemsCount(1043) * 10000L);
                    }
                    qs.takeItems(1042, -1L);
                    qs.takeItems(1043, -1L);
                    break;
                }
                html = "elliasin_q0316_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if ((n == 20040 || n == 20047) && ThreadLocalRandom.current().nextInt(10) > 5) {
            qs.rollAndGive(1042, 1, 100.0);
        } else if (n == 27020 && ThreadLocalRandom.current().nextInt(10) > 7) {
            qs.rollAndGive(1043, 1, 100.0);
        }
        return null;
    
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("cry");
        if (n == 0) {
            // npcSayCustomMessage
            qs.set("cry", 1);
        }
        return null;
    
	}

}
