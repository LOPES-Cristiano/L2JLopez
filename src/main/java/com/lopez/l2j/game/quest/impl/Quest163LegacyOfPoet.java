package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 163 - Legacy Of Poet
 */
@Component
public class Quest163LegacyOfPoet extends Quest {



	public Quest163LegacyOfPoet(QuestManager questManager) {
		super(163, "163_LegacyOfPoet", "Legacy Of Poet");
		addStartNpc(30220);
		addKillId(20372, 20373);
		registerQuestItems(1038, 1039, 1040, 1041);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "sentinel_stardyen_q0315_07.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = qs.getStateId();
        PlayerCharacter player = pc;
        switch (n) {
            case 1: {
                if (pc.getRace() != Race.elf && pc.getRace() != Race.orc && pc.getRace() != Race.dwarf && pc.getRace() != Race.human) {
                    html = "sentinel_stardyen_q0315_00.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() >= 11) {
                    html = "sentinel_stardyen_q0315_03.htm";
                    break;
                }
                html = "sentinel_stardyen_q0315_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (qs.getQuestItemsCount((int)(1038)) == 1 && qs.getQuestItemsCount((int)(1039)) == 1 && qs.getQuestItemsCount((int)(1040)) == 1 && qs.getQuestItemsCount((int)(1041)) == 1) {
                    html = "sentinel_stardyen_q0315_09.htm";
                    qs.giveItems((int)(57), (int)(13890));
                    qs.takeItems((int)(1038), (int)(1));
                    qs.takeItems((int)(1039), (int)(1));
                    qs.takeItems((int)(1040), (int)(1));
                    qs.takeItems((int)(1041), (int)(1));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                    break;
                }
                html = "sentinel_stardyen_q0315_08.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        boolean bl;
        int n = npc.getNpcId();
        boolean bl2 = bl = qs.getQuestItemsCount((int)(1038)) + qs.getQuestItemsCount((int)(1039)) + qs.getQuestItemsCount((int)(1040)) + qs.getQuestItemsCount((int)(1041)) >= 3;
        if (n == 20372 || n == 20373 && qs.getCond() == 1) {
            if (ThreadLocalRandom.current().nextInt(10) == 0 && qs.getQuestItemsCount((int)(1038)) == 0) {
                qs.rollAndGive(1038, 1, 100.0);
                if (bl) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    qs.setCond(2);
                }
            }
            if (ThreadLocalRandom.current().nextInt(10) > 7 && qs.getQuestItemsCount((int)(1039)) == 0) {
                qs.rollAndGive(1039, 1, 100.0);
                if (bl) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    qs.setCond(2);
                }
            }
            if (ThreadLocalRandom.current().nextInt(10) > 7 && qs.getQuestItemsCount((int)(1040)) == 0) {
                qs.rollAndGive(1040, 1, 100.0);
                if (bl) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    qs.setCond(2);
                }
            }
            if (ThreadLocalRandom.current().nextInt(10) > 5 && qs.getQuestItemsCount((int)(1041)) == 0) {
                qs.rollAndGive(1041, 1, 100.0);
                if (bl) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    qs.setCond(2);
                }
            }
        }
        return null;
    
	}

}
