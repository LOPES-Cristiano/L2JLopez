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
 * Quest 363 - 363_SorrowfulSoundofFlute
 */
@Component
public class Quest363SorrowfulSoundofFlute extends Quest {

	public static final int NANARIN = 30956;
	public static final int BARBADO = 30959;
	public static final int POITAN = 30458;
	public static final int HOLVAS = 30058;
	public static final int MUSICAL_SCORE = 4420;
	public static final int EVENT_CLOTHES = 4318;
	public static final int NANARINS_FLUTE = 4319;
	public static final int SABRINS_BLACK_BEER = 4320;
	public static final int Musical_Score = 4420;

	public Quest363SorrowfulSoundofFlute(QuestManager questManager) {
	super(363, "363_SorrowfulSoundofFlute", "363_SorrowfulSoundofFlute");
		this.addStartNpc(30956);
		this.addTalkId(30956);
		this.addTalkId(30458);
		this.addTalkId(30058);
		this.addTalkId(30959);
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
        if (event.equalsIgnoreCase("30956_2.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            qs.takeItems(4318, -1L);
            qs.takeItems(4319, -1L);
            qs.takeItems(4320, -1L);
        } else if (event.equalsIgnoreCase("30956_4.htm")) {
            qs.giveItems(4319, 1L);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.setCond(3);
        } else if (event.equalsIgnoreCase("answer1")) {
            qs.giveItems(4318, 1L);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.setCond(3);
            string2 = "30956_6.htm";
        } else if (event.equalsIgnoreCase("answer2")) {
            qs.giveItems(4320, 1L);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.setCond(3);
            string2 = "30956_6.htm";
        } else if (event.equalsIgnoreCase("30956_7.htm")) {
            qs.giveItems(4420, 1L);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
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
        if (n == 30956) {
            if (n2 == 0) {
                if (pc.getLevel() < 15) {
                    html = "30956-00.htm";
                    qs.exitQuest(true);
                } else {
                    html = "30956_1.htm";
                }
            } else if (n2 == 1) {
                html = "30956_8.htm";
            } else if (n2 == 2) {
                html = "30956_3.htm";
            } else if (n2 == 3) {
                html = "30956_6.htm";
            } else if (n2 == 4) {
                html = "30956_5.htm";
            }
        } else if (n == 30959) {
            if (n2 == 3) {
                if (qs.getQuestItemsCount(4318) > 0L) {
                    qs.takeItems(4318, -1L);
                    html = "30959_2.htm";
                    qs.exitQuest(true);
                } else if (qs.getQuestItemsCount(4320) > 0L) {
                    qs.takeItems(4320, -1L);
                    html = "30959_2.htm";
                    qs.exitQuest(true);
                } else {
                    qs.takeItems(4319, -1L);
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "30959_1.htm";
                }
            } else if (n2 == 4) {
                html = "30959_3.htm";
            }
        } else if (n == 30058 && (n2 == 1 || n2 == 2)) {
            qs.setCond(2);
            html = (ThreadLocalRandom.current().nextDouble(100.0) < (60)) ? "30058_2.htm" : "30058_1.htm";
        } else if (n == 30458 && (n2 == 1 || n2 == 2)) {
            qs.setCond(2);
            html = (ThreadLocalRandom.current().nextDouble(100.0) < (60)) ? "30458_2.htm" : "30458_1.htm";
        }
        return html;
    
	}

}
