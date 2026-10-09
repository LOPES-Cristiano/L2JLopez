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
 * Quest 367 - 367_ElectrifyingRecharge
 */
@Component
public class Quest367ElectrifyingRecharge extends Quest {

	public static final int bst = 30673;
	public static final int bsu = 21035;
	public static final int bsv = 5875;
	public static final int bsw = 5876;
	public static final int bsx = 5877;
	public static final int bsy = 5878;
	public static final int bsz = 5879;
	public static final int bsA = 5880;
	public static final int bsB = 4553;
	public static final int bsC = 4554;
	public static final int bsD = 4555;
	public static final int bsE = 4556;
	public static final int bsF = 4557;
	public static final int bsG = 4558;
	public static final int bsH = 4559;
	public static final int bsI = 4560;
	public static final int bsJ = 4561;
	public static final int bsK = 4562;
	public static final int bsL = 4563;
	public static final int bsM = 4564;
	public static final int bsN = 4445;

	public Quest367ElectrifyingRecharge(QuestManager questManager) {
	super(367, "367_ElectrifyingRecharge", "367_ElectrifyingRecharge");
		this.addStartNpc(30673);
		this.addAttackId(21035);
		this.addQuestItem(5875, 5876, 5877, 5878, 5879, 5880);
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
        if (n == 30673) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("Buzz_Buzz_Charging_adventure", String.valueOf(1), true);
                qs.giveItems(5875, 1L);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "researcher_lorain_q0367_03.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=367&reply=1")) {
                string2 = "researcher_lorain_q0367_07.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=367&reply=2")) {
                qs.takeItems(5875, -1L);
                qs.takeItems(5876, -1L);
                qs.takeItems(5877, -1L);
                qs.takeItems(5878, -1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "researcher_lorain_q0367_08.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=367&reply=3")) {
                string2 = "researcher_lorain_q0367_09.htm";
            }
        }
        return string2;
    
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
                if (n != 30673) break;
                if (pc.getLevel() < 37 || pc.getLevel() > 47) {
                    html = "researcher_lorain_q0367_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                qs.exitQuest(true);
                html = "researcher_lorain_q0367_01.htm";
                break;
            }
            case 2: {
                if (n != 30673) break;
                if (qs.getQuestItemsCount(5879) == 0L && qs.getQuestItemsCount(5880) == 0L) {
                    html = "researcher_lorain_q0367_04.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5880) > 0L) {
                    qs.giveItems(5875, 1L);
                    qs.takeItems(5880, -1L);
                    html = "researcher_lorain_q0367_05.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5879) <= 0L) break;
                int n3 = ThreadLocalRandom.current().nextInt(14);
                if (n3 == 0) {
                    qs.giveItems(4553, 1L);
                } else if (n3 == 1) {
                    qs.giveItems(4554, 1L);
                } else if (n3 == 2) {
                    qs.giveItems(4555, 1L);
                } else if (n3 == 3) {
                    qs.giveItems(4556, 1L);
                } else if (n3 == 4) {
                    qs.giveItems(4557, 1L);
                } else if (n3 == 5) {
                    qs.giveItems(4558, 1L);
                } else if (n3 == 6) {
                    qs.giveItems(4559, 1L);
                } else if (n3 == 7) {
                    qs.giveItems(4560, 1L);
                } else if (n3 == 8) {
                    qs.giveItems(4561, 1L);
                } else if (n3 == 9) {
                    qs.giveItems(4562, 1L);
                } else if (n3 == 10) {
                    qs.giveItems(4563, 1L);
                } else if (n3 == 11) {
                    qs.giveItems(4564, 1L);
                } else {
                    qs.giveItems(4445, 1L);
                }
                qs.takeItems(5879, -1L);
                qs.giveItems(5875, 1L);
                html = "researcher_lorain_q0367_06.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onAttack(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getInt("Buzz_Buzz_Charging_adventure");
        if (n2 == 1 && n == 21035 && qs.getQuestItemsCount(5879) == 0L) {
            int n3 = ThreadLocalRandom.current().nextInt(37);
            if (n3 == 0) {
                if (qs.getQuestItemsCount(5875) > 0L) {
                    qs.giveItems(5876, 1L);
                    qs.takeItems(5875, -1L);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (qs.getQuestItemsCount(5876) > 0L) {
                    qs.giveItems(5877, 1L);
                    qs.takeItems(5876, -1L);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (qs.getQuestItemsCount(5877) > 0L) {
                    qs.giveItems(5878, 1L);
                    qs.takeItems(5877, -1L);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (qs.getQuestItemsCount(5878) > 0L) {
                    qs.giveItems(5879, 1L);
                    qs.takeItems(5878, -1L);
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
                // npc doCast
                qs.takeItems(5875, -1L);
                qs.takeItems(5876, -1L);
                qs.takeItems(5877, -1L);
                qs.takeItems(5878, -1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
        }
        return null;
    
	}

}
