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
 * Quest 652 - 652_AnAgedExAdventurer
 */
@Component
public class Quest652AnAgedExAdventurer extends Quest {

	public static final int bUa = 32012;
	public static final int bUb = 30180;
	public static final int bUc = 1464;
	public static final int bUd = 956;

	public Quest652AnAgedExAdventurer(QuestManager questManager) {
	super(652, "652_AnAgedExAdventurer", "652_AnAgedExAdventurer");
		this.addStartNpc(32012);
		this.addTalkId(30180);
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
        if (event.equalsIgnoreCase("retired_oldman_tantan_q0652_03.htm") && qs.getQuestItemsCount(1464) >= 100L) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.takeItems(1464, 100L);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "retired_oldman_tantan_q0652_04.htm";
        } else {
            string2 = "retired_oldman_tantan_q0652_03.htm";
            qs.exitQuest(true);
            qs.playSound("ItemSound.quest_giveup");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getCond();
        if (n == 32012) {
            if (n2 == 0) {
                if (pc.getLevel() < 46) {
                    html = "retired_oldman_tantan_q0652_01a.htm";
                    qs.exitQuest(true);
                } else {
                    html = "retired_oldman_tantan_q0652_01.htm";
                }
            }
        } else if (n == 30180 && n2 == 1) {
            html = "sara_q0652_01.htm";
            qs.giveItems(57, 5026L, true);
            if ((ThreadLocalRandom.current().nextDouble(100.0) < (50))) {
                qs.giveItems(956, 1L, false);
            }
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return html;
    
	}

}
