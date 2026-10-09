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
 * Quest 157 - Recover Smuggled
 */
@Component
public class Quest157RecoverSmuggled extends Quest {



	public Quest157RecoverSmuggled(QuestManager questManager) {
		super(157, "157_RecoverSmuggled", "Recover Smuggled");
		addStartNpc(30005);
		addTalkId(30005);
		addKillId(20121);
		registerQuestItems(1024);
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
        if (event.equals("quest_accept")) {
            qs.setCond(1);
            qs.set("recover_smuggled", String.valueOf(1), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "wilph_q0157_05.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = qs.getInt("recover_smuggled");
        int n2 = npc.getNpcId();
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30005) break;
                if (pc.level() >= 5) {
                    html = "wilph_q0157_03.htm";
                    break;
                }
                html = "wilph_q0157_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 30005 || n != 1) break;
                if (qs.getQuestItemsCount((int)(1024)) < 20) {
                    html = "wilph_q0157_06.htm";
                    break;
                }
                if (qs.getQuestItemsCount((int)(1024)) < 20) break;
                qs.takeItems((int)(1024), (int)(-1));
                qs.giveItems((int)(20), (int)(1));
                qs.playSound(QuestState.SOUND_FINISH);
                qs.unset("recover_smuggled");
                html = "wilph_q0157_07.htm";
                qs.exitQuest(false);
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        int n2 = qs.getInt("recover_smuggled");
        if (n == 20121 && n2 == 1 && qs.getQuestItemsCount((int)(1024)) < 20 && ThreadLocalRandom.current().nextInt(10) < 4) {
            qs.giveItems((int)(1024), (int)(1));
            if (qs.getQuestItemsCount((int)(1024)) >= 20) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
