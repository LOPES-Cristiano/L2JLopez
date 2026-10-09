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
 * Quest 369 - 369_CollectorOfJewels
 */
@Component
public class Quest369CollectorOfJewels extends Quest {

	public static final int bsU = 30376;
	public static final int bsV = 20609;
	public static final int bsW = 20612;
	public static final int bsX = 20616;
	public static final int bsY = 20619;
	public static final int bsZ = 20747;
	public static final int bta = 20749;
	public static final int btb = 5882;
	public static final int btc = 5883;

	public Quest369CollectorOfJewels(QuestManager questManager) {
	super(369, "369_CollectorOfJewels", "369_CollectorOfJewels");
		this.addStartNpc(bsU);
		this.addKillId(20609, 20612, 20616, 20619, 20747, 20749);
		this.addQuestItem(btb, btc);
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
        int n = getFirstStartNpc();
        if (n == bsU) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("man_collect_element", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "magister_nell_q0369_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(3);
                qs.set("man_collect_element", String.valueOf(3), true);
                string2 = "magister_nell_q0369_07.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                qs.takeItems(btb, -1L);
                qs.takeItems(btc, -1L);
                qs.unset("man_collect_element");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "magister_nell_q0369_08.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("man_collect_element");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != bsU) break;
                if (pc.getLevel() < 25) {
                    html = "magister_nell_q0369_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "magister_nell_q0369_02.htm";
                break;
            }
            case 2: {
                if (n2 != bsU) break;
                if ((qs.getQuestItemsCount(btc) < 50L || qs.getQuestItemsCount(btb) < 50L) && n == 1) {
                    html = "magister_nell_q0369_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(btc) >= 50L && qs.getQuestItemsCount(btb) >= 50L && n == 1) {
                    qs.giveItems(57, 12500L);
                    qs.takeItems(btb, -1L);
                    qs.takeItems(btc, -1L);
                    qs.set("man_collect_element", String.valueOf(2), true);
                    html = "magister_nell_q0369_05.htm";
                    break;
                }
                if (n == 2) {
                    html = "magister_nell_q0369_09.htm";
                    break;
                }
                if (n == 3 && (qs.getQuestItemsCount(btc) < 200L || qs.getQuestItemsCount(btb) < 200L)) {
                    html = "magister_nell_q0369_10.htm";
                    break;
                }
                if (n != 3 || qs.getQuestItemsCount(btc) < 200L || qs.getQuestItemsCount(btb) < 200L) break;
                qs.giveItems(57, 76000L);
                qs.takeItems(btb, -1L);
                qs.takeItems(btc, -1L);
                qs.unset("man_collect_element");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                html = "magister_nell_q0369_11.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("man_collect_element");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 20609) {
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                qs.giveItems(btb, 1L);
                if (n == 1 && qs.getQuestItemsCount(btc) >= 50L && qs.getQuestItemsCount(btb) >= 49L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 200L && qs.getQuestItemsCount(btb) >= 199L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 20612) {
            if (ThreadLocalRandom.current().nextInt(100) < 91) {
                qs.giveItems(btb, 1L);
                if (n == 1 && qs.getQuestItemsCount(btc) >= 50L && qs.getQuestItemsCount(btb) >= 49L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 200L && qs.getQuestItemsCount(btb) >= 199L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 20616) {
            if (ThreadLocalRandom.current().nextInt(100) < 80) {
                qs.giveItems(btb, 1L);
                if (n == 1 && qs.getQuestItemsCount(btc) >= 50L && qs.getQuestItemsCount(btb) >= 49L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 200L && qs.getQuestItemsCount(btb) >= 199L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 20619) {
            if (ThreadLocalRandom.current().nextInt(100) < 87) {
                qs.giveItems(btb, 1L);
                if (n == 1 && qs.getQuestItemsCount(btc) >= 50L && qs.getQuestItemsCount(btb) >= 49L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 200L && qs.getQuestItemsCount(btb) >= 199L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 20747 || n2 == 20749) {
            if (ThreadLocalRandom.current().nextInt(100) < 2) {
                if (n == 1 && qs.getQuestItemsCount(btc) >= 49L) {
                    qs.giveItems(btc, 1L);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 199L) {
                    qs.giveItems(btc, 1L);
                } else {
                    qs.giveItems(btc, 2L);
                }
                if (n == 1 && qs.getQuestItemsCount(btc) >= 49L && qs.getQuestItemsCount(btb) >= 50L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 199L && qs.getQuestItemsCount(btb) >= 200L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else {
                qs.giveItems(btc, 1L);
                if (n == 1 && qs.getQuestItemsCount(btc) >= 49L && qs.getQuestItemsCount(btb) >= 50L) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 3 && qs.getQuestItemsCount(btc) >= 199L && qs.getQuestItemsCount(btb) >= 200L) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
