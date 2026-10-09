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
 * Quest 362 - 362_BardsMandolin
 */
@Component
public class Quest362BardsMandolin extends Quest {

	public static final int brX = 30957;
	public static final int NANARIN = 30956;
	public static final int brY = 30958;
	public static final int brZ = 30837;
	public static final int bsa = 4316;
	public static final int bsb = 4317;
	public static final int bsc = 4410;

	public Quest362BardsMandolin(QuestManager questManager) {
	super(362, "362_BardsMandolin", "362_BardsMandolin");
		this.addStartNpc(brX);
		this.addTalkId(NANARIN);
		this.addTalkId(brY);
		this.addTalkId(brZ);
		this.addQuestItem(bsa);
		this.addQuestItem(bsb);
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
        int n = qs.getStateId();
        int n2 = qs.getCond();
        if (event.equalsIgnoreCase("30957_2.htm") && n == 1 && n2 == 0) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30957_5.htm") && n == 2 && n2 == 5) {
            qs.giveItems(57, 10000L);
            qs.giveItems(bsc, 1L);
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
        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getState() == State.CREATED) {
            if (n != brX) {
                return html;
            }
            qs.setCond(0);
        }
        int n2 = qs.getCond();
        if (n == brX) {
            if (n2 == 0) {
                html = "30957_1.htm";
            } else if (n2 == 3 && qs.getQuestItemsCount(bsa) > 0L && qs.getQuestItemsCount(bsb) == 0L) {
                html = "30957_3.htm";
                qs.setCond(4);
                qs.giveItems(bsb, 1L);
            } else if (n2 == 4 && qs.getQuestItemsCount(bsa) > 0L && qs.getQuestItemsCount(bsb) > 0L) {
                html = "30957_6.htm";
            } else if (n2 == 5) {
                html = "30957_4.htm";
            }
        } else if (n == brZ && n2 == 1) {
            html = "30837_1.htm";
            qs.setCond(2);
        } else if (n == brY && n2 == 2) {
            html = "30958_1.htm";
            qs.setCond(3);
            qs.giveItems(bsa, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        } else if (n == NANARIN && n2 == 4 && qs.getQuestItemsCount(bsa) > 0L && qs.getQuestItemsCount(bsb) > 0L) {
            html = "30956_1.htm";
            qs.takeItems(bsa, 1L);
            qs.takeItems(bsb, 1L);
            qs.setCond(5);
        }
        return html;
    
	}

}
