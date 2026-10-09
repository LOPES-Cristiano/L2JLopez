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
 * Quest 155 - Find Sir Windawood
 */
@Component
public class Quest155FindSirWindawood extends Quest {

	public static final int OFFICIAL_LETTER = 1019;
	public static final int HASTE_POTION = 734;

	public Quest155FindSirWindawood(QuestManager questManager) {
		super(155, "155_FindSirWindawood", "Find Sir Windawood");
		addStartNpc(30042);
		addTalkId(30042);
		addTalkId(30311);
		registerQuestItems(OFFICIAL_LETTER);
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
        if (event.equals("30042-04.htm")) {
            qs.giveItems((int)(OFFICIAL_LETTER), (int)(1));
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc.getNpcId();
        String html = "noquest";
        int n2 = qs.getCond();
        if (n == 30042) {
            if (n2 == 0) {
                if (pc.level() >= 3) {
                    html = "30042-03.htm";
                    return html;
                }
                html = "30042-02.htm";
                qs.exitQuest(true);
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(OFFICIAL_LETTER)) == 1) {
                html = "30042-05.htm";
            }
        } else if (n == 30311 && n2 == 1 && qs.getQuestItemsCount((int)(OFFICIAL_LETTER)) == 1) {
            html = "30311-01.htm";
            qs.takeItems((int)(OFFICIAL_LETTER), (int)(-1));
            qs.giveItems((int)(HASTE_POTION), (int)(1));
            qs.setCond(0);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(false);
        }
        return html;
    
	}


}
