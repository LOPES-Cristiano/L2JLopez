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
 * Quest 432 - 432_BirthdayPartySong
 */
@Component
public class Quest432BirthdayPartySong extends Quest {

	public static final int bHI = 31043;
	public static final int bHJ = 21103;
	public static final int bHK = 7541;
	public static final int bHL = 7061;

	public Quest432BirthdayPartySong(QuestManager questManager) {
	super(432, "432_BirthdayPartySong", "432_BirthdayPartySong");
		this.addStartNpc(bHI);
		this.addKillId(bHJ);
		this.addQuestItem(bHK);
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
        if (event.equalsIgnoreCase("muzyko_q0432_0104.htm")) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("muzyko_q0432_0201.htm")) {
            if (qs.getQuestItemsCount(bHK) == 50L) {
                qs.takeItems(bHK, -1L);
                qs.giveItems(bHL, 25L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            } else {
                string2 = "muzyko_q0432_0202.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == bHI) {
            if (n == 0) {
                if (pc.getLevel() >= 31) {
                    html = "muzyko_q0432_0101.htm";
                } else {
                    html = "muzyko_q0432_0103.htm";
                    qs.exitQuest(true);
                }
            } else if (n == 1) {
                html = "muzyko_q0432_0106.htm";
            } else if (n == 2 && qs.getQuestItemsCount(bHK) == 50L) {
                html = "muzyko_q0432_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        if (n == bHJ && qs.getCond() == 1 && qs.getQuestItemsCount(bHK) < 50L) {
            qs.giveItems(bHK, 1L);
            if (qs.getQuestItemsCount(bHK) == 50L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
