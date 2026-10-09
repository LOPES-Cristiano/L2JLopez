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
 * Quest 653 - 653_WildMaiden
 */
@Component
public class Quest653WildMaiden extends Quest {

	public static final int SUKI = 32013;
	public static final int GALIBREDO = 30181;
	public static final int SOE = 736;

	public Quest653WildMaiden(QuestManager questManager) {
	super(653, "653_WildMaiden", "653_WildMaiden");
		this.addStartNpc(32013);
		this.addTalkId(32013);
		this.addTalkId(30181);
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
        PlayerCharacter player = pc;
        if (event.equalsIgnoreCase("spring_girl_sooki_q0653_03.htm")) {
            if (qs.getQuestItemsCount(736) > 0L) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.takeItems(736, 1L);
                string2 = "spring_girl_sooki_q0653_04a.htm";
                NpcInstance npcInstance2 = this.c(32013, player);
                // broadcastPacket
                qs.startQuestTimer("suki_timer", 20000L);
            }
        } else if (event.equalsIgnoreCase("spring_girl_sooki_q0653_03.htm")) {
            qs.exitQuest(false);
            qs.playSound("ItemSound.quest_giveup");
        } else if (event.equalsIgnoreCase("suki_timer")) {
            NpcInstance npcInstance3 = this.c(32013, player);
            if (npcInstance3 != null) {
                npcInstance3.deleteMe();
            }
            string2 = null;
        }
        return string2;
    }

	private NpcInstance c(int n, PlayerCharacter player) {
		return null;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        if (n == 32013 && n2 == 1) {
            if (pc.getLevel() >= 36) {
                html = "spring_girl_sooki_q0653_01.htm";
            } else {
                html = "spring_girl_sooki_q0653_01a.htm";
                qs.exitQuest(false);
            }
        } else if (n == 30181 && qs.getCond() == 1) {
            html = "galicbredo_q0653_01.htm";
            qs.giveItems(57, 2553L);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(false);
        }
        return html;
    
	}

}
