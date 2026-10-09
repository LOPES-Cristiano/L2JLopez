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
 * Quest 156 - Millennium Love
 */
@Component
public class Quest156MillenniumLove extends Quest {

	public static final int LILITHS_LETTER = 1022;
	public static final int THEONS_DIARY = 1023;
	public static final int GR_COMP_PACKAGE_SS = 5250;
	public static final int GR_COMP_PACKAGE_SPS = 5256;

	public Quest156MillenniumLove(QuestManager questManager) {
		super(156, "156_MillenniumLove", "Millennium Love");
		addStartNpc(30368);
		addTalkId(30368);
		addTalkId(30368);
		addTalkId(30368);
		addTalkId(30369);
		registerQuestItems(LILITHS_LETTER, THEONS_DIARY);
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
        if (event.equals("30368-06.htm")) {
            qs.giveItems((int)(LILITHS_LETTER), (int)(1));
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("156_1")) {
            qs.takeItems((int)(LILITHS_LETTER), (int)(-1));
            if (qs.getQuestItemsCount((int)(THEONS_DIARY)) == 0) {
                qs.giveItems((int)(THEONS_DIARY), (int)(1));
                qs.setCond(2);
            }
            string2 = "30369-03.htm";
        } else if (event.equals("156_2")) {
            qs.takeItems((int)(LILITHS_LETTER), (int)(-1));
            qs.playSound(QuestState.SOUND_FINISH);
            string2 = "30369-04.htm";
            qs.exitQuest(false);
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
        if (n == 30368) {
            if (n2 == 0) {
                if (pc.level() >= 15) {
                    html = "30368-02.htm";
                } else {
                    html = "30368-05.htm";
                    qs.exitQuest(true);
                }
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(LILITHS_LETTER)) == 1) {
                html = "30368-07.htm";
            } else if (n2 == 2 && qs.getQuestItemsCount((int)(THEONS_DIARY)) == 1) {
                qs.takeItems((int)(THEONS_DIARY), (int)(-1));
                if (pc.isMage()) {
                    qs.giveItems((int)(GR_COMP_PACKAGE_SPS), (int)(1));
                } else {
                    qs.giveItems((int)(GR_COMP_PACKAGE_SS), (int)(1));
                }
                qs.playSound(QuestState.SOUND_FINISH);
                html = "30368-08.htm";
                qs.exitQuest(false);
            }
        } else if (n == 30369) {
            if (n2 == 1 && qs.getQuestItemsCount((int)(LILITHS_LETTER)) == 1) {
                html = "30369-02.htm";
            } else if (n2 == 2 && qs.getQuestItemsCount((int)(THEONS_DIARY)) == 1) {
                html = "30369-05.htm";
            }
        }
        return html;
    
	}


}
