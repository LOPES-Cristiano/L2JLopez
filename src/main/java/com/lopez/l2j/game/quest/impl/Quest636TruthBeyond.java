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
 * Quest 636 - 636_TruthBeyond
 */
@Component
public class Quest636TruthBeyond extends Quest {

	public static final int bQB = 31329;
	public static final int bQC = 32010;
	public static final int bQD = 8067;
	public static final int bQE = 8064;
	public static final int bQF = 8065;

	public Quest636TruthBeyond(QuestManager questManager) {
	super(636, "636_TruthBeyond", "636_TruthBeyond");
		this.addStartNpc(31329);
		this.addTalkId(32010);
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
        if (n == 31329) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("truth_behind_door", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "priest_eliyah_q0636_05.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "priest_eliyah_q0636_04.htm";
            }
        } else if (n == 32010 && event.equalsIgnoreCase("reply_1")) {
            qs.giveItems(8064, 1L);
            qs.unset("truth_behind_door");
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "falsepriest_flauron_q0636_02.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("truth_behind_door");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31329) break;
                if (pc.getLevel() >= 73 && qs.getQuestItemsCount(8064) == 0L && qs.getQuestItemsCount(8065) == 0L && qs.getQuestItemsCount(8067) == 0L) {
                    html = "priest_eliyah_q0636_01.htm";
                    break;
                }
                if (pc.getLevel() >= 73 && (qs.getQuestItemsCount(8064) >= 1L || qs.getQuestItemsCount(8065) >= 1L || qs.getQuestItemsCount(8067) >= 1L)) {
                    html = "priest_eliyah_q0636_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() >= 73) break;
                html = "priest_eliyah_q0636_03.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 31329) {
                    if (n != 1) break;
                    html = "priest_eliyah_q0636_06.htm";
                    break;
                }
                if (n2 != 32010 || n != 1) break;
                html = "falsepriest_flauron_q0636_01.htm";
            }
        }
        return html;
    
	}

}
