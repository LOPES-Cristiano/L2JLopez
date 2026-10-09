package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 158 - Seed Of Evil
 */
@Component
public class Quest158SeedOfEvil extends Quest {

	public static final int CLAY_TABLET_ID = 1025;
	public static final int ENCHANT_ARMOR_D = 956;

	public Quest158SeedOfEvil(QuestManager questManager) {
		super(158, "158_SeedOfEvil", "Seed Of Evil");
		addStartNpc(30031);
		addKillId(27016);
		registerQuestItems(CLAY_TABLET_ID);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

        String string2 = event;
        if (event.equals("1")) {
            qs.set("id", "0");
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "30031-04.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc.getNpcId();
        String html = "noquest";
        int n2 = qs.getStateId();
        if (n2 == 1) {
            qs.setState(State.STARTED);
            qs.set("id", "0");
        }
        if (n == 30031 && qs.getCond() == 0) {
            if (qs.getCond() < 15) {
                if (pc.level() >= 21) {
                    html = "30031-03.htm";
                    return html;
                }
                html = "30031-02.htm";
                qs.exitQuest(true);
            } else {
                html = "30031-02.htm";
                qs.exitQuest(true);
            }
        } else if (n == 30031 && qs.getCond() == 0) {
            html = "completed";
        } else if (n == 30031 && qs.getCond() != 0 && qs.getQuestItemsCount((int)(CLAY_TABLET_ID)) == 0) {
            html = "30031-05.htm";
        } else if (n == 30031 && qs.getCond() != 0 && qs.getQuestItemsCount((int)(CLAY_TABLET_ID)) != 0) {
            qs.takeItems((int)(CLAY_TABLET_ID), (int)(qs.getQuestItemsCount((int)(CLAY_TABLET_ID))));
            qs.playSound(QuestState.SOUND_FINISH);
            qs.giveItems((int)(ENCHANT_ARMOR_D), (int)(1));
            html = "30031-06.htm";
            qs.exitQuest(false);
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount((int)(CLAY_TABLET_ID)) == 0) {
            qs.giveItems((int)(CLAY_TABLET_ID), (int)(1));
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.setCond(2);
        }
        return null;
    
	}

}
