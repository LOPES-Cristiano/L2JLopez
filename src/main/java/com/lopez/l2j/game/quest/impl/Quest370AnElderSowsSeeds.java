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
 * Quest 370 - 370_AnElderSowsSeeds
 */
@Component
public class Quest370AnElderSowsSeeds extends Quest {

	public static final int btd = 30612;
	public static final int[] MOBS = new int[]{20082, 20084, 20086, 20089, 20090};
	public static final int bte = 5916;
	public static final int btf = 736;
	public static final int[] btg = new int[]{5917, 5918, 5919, 5920};

	public Quest370AnElderSowsSeeds(QuestManager questManager) {
	super(370, "370_AnElderSowsSeeds", "370_AnElderSowsSeeds");
		this.addStartNpc(btd);
		for (int n : MOBS) {
		this.addKillId(n);
		}
		this.addQuestItem(bte);
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
        if (event.equalsIgnoreCase("30612-1.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30612-6.htm")) {
            if (qs.getQuestItemsCount(btg[0]) > 0L && qs.getQuestItemsCount(btg[1]) > 0L && qs.getQuestItemsCount(btg[2]) > 0L && qs.getQuestItemsCount(btg[3]) > 0L) {
                long l = qs.getQuestItemsCount(btg[0]);
                for (int n : btg) {
                    l = Math.min(l, qs.getQuestItemsCount(n));
                }
                for (int n : btg) {
                    qs.takeItems(n, l);
                }
                qs.giveItems(57, 3600L * l);
                string2 = "30612-8.htm";
            } else {
                string2 = "30612-4.htm";
            }
        } else if (event.equalsIgnoreCase("30612-9.htm")) {
            qs.giveItems(btf, 1L);
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
        int n = qs.getCond();
        if (qs.getState() == State.CREATED) {
            if (pc.getLevel() < 28) {
                html = "30612-0a.htm";
                qs.exitQuest(true);
            } else {
                html = "30612-0.htm";
            }
        } else if (n == 1) {
            html = "30612-4.htm";
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
        if ((ThreadLocalRandom.current().nextDouble(100.0) < (Math.min((int)(15.0 * qs.getRateQuestsReward()), 100)))) {
            qs.giveItems(bte, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
