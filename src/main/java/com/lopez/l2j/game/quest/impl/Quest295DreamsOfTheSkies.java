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
 * Quest 295 - 295_DreamsOfTheSkies
 */
@Component
public class Quest295DreamsOfTheSkies extends Quest {

	public static final int FLOATING_STONE = 1492;
	public static final int RING_OF_FIREFLY = 1509;
	public static final int Arin = 30536;
	public static final int MagicalWeaver = 20153;

	public Quest295DreamsOfTheSkies(QuestManager questManager) {
		super(295, "295_DreamsOfTheSkies", "295_DreamsOfTheSkies");
		addStartNpc(Arin);
		addKillId(MagicalWeaver);
		registerQuestItems(FLOATING_STONE);
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
        if (event.equalsIgnoreCase("elder_arin_q0295_03.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n;
        String html = "noquest";
        int n2 = qs.getStateId();
        if (n2 == 1) {
            qs.setCond(0);
        }
        if ((n = qs.getCond()) == 0) {
            if (pc.getLevel() >= 11) {
                html = "elder_arin_q0295_02.htm";
                return html;
            }
            html = "elder_arin_q0295_01.htm";
            qs.exitQuest(true);
        } else if (n == 1 || qs.getQuestItemsCount(FLOATING_STONE) < 50L) {
            html = "elder_arin_q0295_04.htm";
        } else if (n == 2 && qs.getQuestItemsCount(FLOATING_STONE) == 50L) {
            qs.addExpAndSp(0L, 500L);
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
            if (qs.getQuestItemsCount(RING_OF_FIREFLY) < 1L) {
                html = "elder_arin_q0295_05.htm";
                qs.giveItems(RING_OF_FIREFLY, 1L);
            } else {
                html = "elder_arin_q0295_06.htm";
                qs.giveItems(57, 2400L);
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1 && qs.getQuestItemsCount(FLOATING_STONE) < 50L) {
            if ((ThreadLocalRandom.current().nextInt(100) < 25)) {
                qs.giveItems(FLOATING_STONE, 1L);
                if (qs.getQuestItemsCount(FLOATING_STONE) == 50L) {
                    qs.playSound("QuestState.SOUND_MIDDLE");
                    qs.setCond(2);
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            } else if (qs.getQuestItemsCount(FLOATING_STONE) >= 48L) {
                qs.giveItems(FLOATING_STONE, 50L - qs.getQuestItemsCount(FLOATING_STONE));
                qs.playSound("QuestState.SOUND_MIDDLE");
                qs.setCond(2);
            } else {
                qs.giveItems(FLOATING_STONE, 2L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        }
        return null;
    
	}

}
