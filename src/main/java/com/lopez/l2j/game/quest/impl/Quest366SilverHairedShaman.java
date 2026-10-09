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
 * Quest 366 - 366_SilverHairedShaman
 */
@Component
public class Quest366SilverHairedShaman extends Quest {

	public static final int bso = 30111;
	public static final int bsp = 20986;
	public static final int bsq = 20987;
	public static final int bsr = 20988;
	public static final int bss = 5874;

	public Quest366SilverHairedShaman(QuestManager questManager) {
	super(366, "366_SilverHairedShaman", "366_SilverHairedShaman");
		this.addStartNpc(30111);
		this.addKillId(20986, 20987, 20988);
		this.addQuestItem(5874);
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
            qs.set("silver_haired_shaman", String.valueOf(1), true);
            string2 = "dieter_q0366_03.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("reply_1")) {
            qs.unset("silver_haired_shaman");
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "dieter_q0366_06.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "dieter_q0366_07.htm";
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
        int n3 = qs.getInt("silver_haired_shaman");
        switch (n2) {
            case 1: {
                if (n != 30111) break;
                if (pc.getLevel() >= 48) {
                    html = "dieter_q0366_01.htm";
                    break;
                }
                if (pc.getLevel() >= 48) break;
                html = "dieter_q0366_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n != 30111) break;
                if (n3 == 1 && qs.getQuestItemsCount(5874) < 1L) {
                    html = "dieter_q0366_04.htm";
                    break;
                }
                if (n3 != 1 || qs.getQuestItemsCount(5874) < 1L) break;
                qs.giveItems(57, qs.getQuestItemsCount(5874) * 500L + 29000L);
                qs.takeItems(5874, -1L);
                html = "dieter_q0366_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getCond();
        if (n == 1 && (ThreadLocalRandom.current().nextDouble(100.0) < (80))) {
            qs.rollAndGive(5874, 1, 100.0);
            qs.playSound(QuestState.SOUND_MIDDLE);
        }
        return null;
    
	}

}
