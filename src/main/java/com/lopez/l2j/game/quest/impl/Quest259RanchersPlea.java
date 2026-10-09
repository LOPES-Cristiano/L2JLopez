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
 * Quest 259 - Ranchers Plea
 */
@Component
public class Quest259RanchersPlea extends Quest {

	public static final int bdo = 1495;
	public static final int bdp = 1061;
	public static final int bdq = 17;
	public static final int bdr = 1835;
	public static final int bds = 2509;
	public static final int bdt = 30497;
	public static final int bdu = 30405;
	public static final int bdv = 20103;
	public static final int bdw = 20106;
	public static final int bdx = 20108;

	public Quest259RanchersPlea(QuestManager questManager) {
		super(259, "259_RanchersPlea", "Ranchers Plea");
		addStartNpc(30497);
		addTalkId(30405);
		addKillId(20103, 20106, 20108);
		registerQuestItems(1495);
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

        String event2 = event;
        int n = getFirstStartNpc();
        if (n == 30497) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.playSound(QuestState.SOUND_ACCEPT);
                qs.setState(State.STARTED);
                event2 = "edmond_q0259_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                event2 = "edmond_q0259_06.htm";
                qs.exitQuest(true);
                qs.playSound(QuestState.SOUND_FINISH);
            } else if (event.equalsIgnoreCase("reply_2")) {
                event2 = "edmond_q0259_07.htm";
            }
        } else if (n == 30405) {
            if (event.equalsIgnoreCase("reply_1")) {
                event2 = "marius_q0259_03.htm";
            } else if (event.equalsIgnoreCase("reply_2") && qs.getQuestItemsCount(1495) >= 10) {
                event2 = "marius_q0259_04.htm";
                qs.giveItems(1061, 2);
                qs.takeItems(1495, 10);
            } else if (event.equalsIgnoreCase("reply_3") && qs.getQuestItemsCount(1495) >= 10) {
                event2 = "marius_q0259_05.htm";
                qs.giveItems(17, 250);
                qs.takeItems(1495, 10);
            } else if (event.equalsIgnoreCase("reply_4")) {
                if (qs.getQuestItemsCount(1495) >= 10) {
                    event2 = "marius_q0259_06.htm";
                } else if (qs.getQuestItemsCount(1495) < 10) {
                    event2 = "marius_q0259_07.htm";
                }
            } else if (event.equalsIgnoreCase("reply_5") && qs.getQuestItemsCount(1495) >= 10) {
                event2 = "marius_q0259_05a.htm";
                qs.giveItems(1835, 60);
                qs.takeItems(1495, 10);
            } else if (event.equalsIgnoreCase("reply_6") && qs.getQuestItemsCount(1495) >= 10) {
                event2 = "marius_q0259_05c.htm";
                qs.giveItems(2509, 30);
                qs.takeItems(1495, 10);
            }
        }
        return event2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        switch (n2) {
            case 1: {
                if (n != 30497) break;
                if (pc.level() >= 15) {
                    html = "edmond_q0259_02.htm";
                    break;
                }
                html = "edmond_q0259_01.htm";
                break;
            }
            case 2: {
                if (n == 30497) {
                    if (qs.getCond() == 1 && qs.getQuestItemsCount(1495) < 1) {
                        html = "edmond_q0259_04.htm";
                        break;
                    }
                    if (qs.getCond() != 1 || qs.getQuestItemsCount(1495) < 1) break;
                    html = "edmond_q0259_05.htm";
                    if (qs.getQuestItemsCount(1495) >= 10) {
                        qs.giveItems(57, qs.getQuestItemsCount(1495) * 25 + 250);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(1495) * 25);
                    }
                    qs.takeItems(1495, -1);
                    break;
                }
                if (n != 30405) break;
                if (qs.getCond() == 1 && qs.getQuestItemsCount(1495) < 10) {
                    html = "marius_q0259_01.htm";
                    break;
                }
                if (qs.getCond() != 1 || qs.getQuestItemsCount(1495) < 10) break;
                html = "marius_q0259_02.htm";
            }
        }
        return html;
    
	}



	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() > 0) {
            qs.rollAndGive(1495, 1, 100.0);
        }
        return null;
    
	}

}
