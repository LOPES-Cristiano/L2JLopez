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
 * Quest 510 - 510_AClansReputation
 */
@Component
public class Quest510AClansReputation extends Quest {

	public static final int bJw = 31331;
	public static final int bJx = 8767;
	public static final int bJy = 50;

	public Quest510AClansReputation(QuestManager questManager) {
	super(510, "510_AClansReputation", "510_AClansReputation");
		this.addStartNpc(31331);
		int n = 22215;
		while (n <= 22217) {
		this.addKillId(n++);
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

        int n = qs.getCond();
        String string2 = event;
        if (event.equals("31331-3.htm")) {
            if (n == 0) {
                qs.setCond(1);
                qs.setState(State.STARTED);
            }
        } else if (event.equals("31331-6.htm")) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        PlayerCharacter player = pc;
        Clan clan = player.getClan();
        if (player.getClan() == null || !player.isClanLeader()) {
            qs.exitQuest(true);
            html = "31331-0.htm";
        } else if (player.getClan().getLevel() < 5) {
            qs.exitQuest(true);
            html = "31331-0.htm";
        } else {
            int n = qs.getCond();
            int n2 = qs.getStateId();
            if (n2 == 1 && n == 0) {
                html = "31331-1.htm";
            } else if (n2 == 2 && n == 1) {
                long l = qs.getQuestItemsCount(8767);
                if (l == 0L) {
                    html = "31331-4.htm";
                } else if (l >= 1L) {
                    html = "31331-7.htm";
                    qs.takeItems(8767, -1L);
                    int n3 = 50 * (int) l;
                    int n4 = clan.incReputation(n3, true, "_510_AClansReputation");
                    // sendPacket
                }
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        if (!pc.isClanLeader()) {
            qs.exitQuest(true);
        } else if (qs.getState() == State.STARTED && (n = npc.getNpcId()) >= 22215 && n <= 22218) {
            qs.giveItems(8767, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
