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
 * Quest 359 - 359_ForSleeplessDeadmen
 */
@Component
public class Quest359ForSleeplessDeadmen extends Quest {

	public static final int brA = 10;
	public static final int REQUIRED = 60;
	public static final int brB = 5869;
	public static final int brC = 6341;
	public static final int brD = 6342;
	public static final int brE = 6343;
	public static final int brF = 6344;
	public static final int brG = 6345;
	public static final int brH = 6346;
	public static final int brI = 5494;
	public static final int brJ = 5495;
	public static final int ORVEN = 30857;
	public static final int brK = 21006;
	public static final int brL = 21007;
	public static final int brM = 21008;
	public static final int brN = 21009;

	public Quest359ForSleeplessDeadmen(QuestManager questManager) {
	super(359, "359_ForSleeplessDeadmen", "359_ForSleeplessDeadmen");
		this.addStartNpc(30857);
		this.addKillId(21006);
		this.addKillId(21007);
		this.addKillId(21008);
		this.addKillId(21009);
		this.addQuestItem(5869);
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
        int n = qs.getCond();
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "highpriest_orven_q0359_06.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "highpriest_orven_q0359_05.htm";
        } else if (event.equalsIgnoreCase("reply_2") && n == 3) {
            qs.setCond(1);
            int n2 = ThreadLocalRandom.current().nextInt(100);
            int n3 = n2 <= 16 ? 6343 : (n2 <= 33 ? 6341 : (n2 <= 50 ? 6345 : (n2 <= 58 ? 6344 : (n2 <= 67 ? 6342 : (n2 <= 76 ? 6346 : (n2 <= 84 ? 5494 : 5495))))));
            qs.giveItems(n3, 4L, true);
            string2 = "highpriest_orven_q0359_10.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        int n2 = qs.getCond();
        if (n == 1) {
            if (pc.getLevel() < 60) {
                qs.exitQuest(true);
                html = "highpriest_orven_q0359_01.htm";
            } else {
                html = "highpriest_orven_q0359_02.htm";
            }
        } else if (n == 2) {
            if (n2 == 1 && qs.getQuestItemsCount(5869) < 60L) {
                html = "highpriest_orven_q0359_07.htm";
            } else if (n2 == 2 && qs.getQuestItemsCount(5869) < 60L) {
                html = "highpriest_orven_q0359_07.htm";
            } else if (n2 == 2 && qs.getQuestItemsCount(5869) >= 60L) {
                qs.takeItems(5869, 60L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(3);
                html = "highpriest_orven_q0359_08.htm";
            } else if (n2 == 3) {
                html = "highpriest_orven_q0359_09.htm";
            }
        } else {
            html = "highpriest_orven_q0359_05.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getQuestItemsCount(5869) < 60L && (ThreadLocalRandom.current().nextDouble(100.0) < (10))) {
            qs.giveItems(5869, 1L);
            if (qs.getQuestItemsCount(5869) >= 60L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(2);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
