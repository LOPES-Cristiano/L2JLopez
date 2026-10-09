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
 * Quest 275 - Black Winged Spies
 */
@Component
public class Quest275BlackWingedSpies extends Quest {

	public static final int aXM = 30567;
	public static final int bel = 20316;
	public static final int bem = 27043;
	public static final int ben = 1478;
	public static final int beo = 1479;
	public static final int bep = 10;

	public Quest275BlackWingedSpies(QuestManager questManager) {
		super(275, "275_BlackWingedSpies", "Black Winged Spies");
		addStartNpc(aXM);
		addKillId(bel);
		addKillId(bem);
		registerQuestItems(ben);
		registerQuestItems(beo);
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

        if (event.equalsIgnoreCase("neruga_chief_tantus_q0275_03.htm") && qs.getState() == State.CREATED) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        }
        return html;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        if (npc.getNpcId() != aXM) {
            return "noquest";
        }
        int n = qs.getStateId();
        if (n == 1) {
            if (pc.race() != 3) {
                qs.exitQuest(true);
                return "neruga_chief_tantus_q0275_00.htm";
            }
            if (pc.level() < 11) {
                qs.exitQuest(true);
                return "neruga_chief_tantus_q0275_01.htm";
            }
            qs.setCond(0);
            return "neruga_chief_tantus_q0275_02.htm";
        }
        if (n != 2) {
            return "noquest";
        }
        int n2 = qs.getCond();
        if (qs.getQuestItemsCount(ben) < 70) {
            if (n2 != 1) {
                qs.setCond(1);
            }
            return "neruga_chief_tantus_q0275_04.htm";
        }
        if (n2 == 2) {
            qs.giveItems(57, 4550);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            return "neruga_chief_tantus_q0275_05.htm";
        }
        return "noquest";
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        long l = qs.getQuestItemsCount(ben);
        if (n == bel && l < 70) {
            giveDarkwingBatFang(qs, 1);
        } else if (n == bem && l < 70 && qs.getQuestItemsCount(beo) > 0) {
            qs.takeItems(beo, -1);
            giveDarkwingBatFang(qs, 5);
        }
        return null;
	}

	private void giveDarkwingBatFang(QuestState qs, int count) {
		qs.giveItems(ben, count);
		if (qs.getQuestItemsCount(ben) >= 70) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}


    private static void g(QuestState questState) {
        if (questState.getQuestItemsCount(beo) > 0L) {
            return;
        }
        questState.giveItems(beo, 1L);
        questState.playSound("ItemSound.quest_itemget");
    }

    private static void give_Darkwing_Bat_Fang(QuestState questState, long l) {
        long l2 = questState.getQuestItemsCount(ben);
        if (l2 < 70L) {
            long l3 = l;
            if (l2 + l > 70L) {
                l3 = 70L - l2;
            }
            questState.giveItems(ben, l3);
            if (l2 + l3 >= 70L) {
                questState.setCond(2);
                questState.playSound("ItemSound.quest_middle");
            } else {
                questState.playSound("ItemSound.quest_itemget");
            }
        }
    }
}
