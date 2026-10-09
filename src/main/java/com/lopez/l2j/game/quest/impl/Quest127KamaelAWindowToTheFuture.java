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
 * Quest 127 - Kamael A Window To The Future
 */
@Component
public class Quest127KamaelAWindowToTheFuture extends Quest {



	public Quest127KamaelAWindowToTheFuture(QuestManager questManager) {
		super(127, "127_KamaelAWindowToTheFuture", "Kamael A Window To The Future");
		addStartNpc(31350);
		addTalkId(31288, 32092, 30113, 30187, 30862, 30756);
		registerQuestItems(8939, 8940, 8941, 8942, 8943, 8944);
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
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "falsepriest_dominic_q0127_05.htm";
            qs.giveItems((int)(8939), (int)(1));
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=1") && qs.getQuestItemsCount((int)(8939)) < 1) {
            string2 = "kai_q0127_04.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=1") && qs.getQuestItemsCount((int)(8939)) >= 1) {
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "kai_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=6")) {
            qs.setCond(3);
            qs.giveItems((int)(8940), (int)(1));
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "kai_q0127_11.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=7") && qs.getQuestItemsCount((int)(8940)) < 1) {
            string2 = "warehouse_chief_older_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=7") && qs.getQuestItemsCount((int)(8940)) >= 1) {
            qs.setCond(4);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "warehouse_chief_older_q0127_03.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=10")) {
            qs.setCond(5);
            qs.giveItems((int)(8941), (int)(1));
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "warehouse_chief_older_q0127_07.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=11") && qs.getQuestItemsCount((int)(8941)) < 1) {
            string2 = "high_prefect_aklan_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=11") && qs.getQuestItemsCount((int)(8941)) >= 1) {
            string2 = "high_prefect_aklan_q0127_03.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=13")) {
            qs.giveItems((int)(8944), (int)(1));
            qs.setCond(6);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "high_prefect_aklan_q0127_06.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=14") && qs.getQuestItemsCount((int)(8944)) < 1) {
            string2 = "grandmaster_oltlin_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=14") && qs.getQuestItemsCount((int)(8944)) >= 1) {
            string2 = "grandmaster_oltlin_q0127_03.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=16")) {
            qs.giveItems((int)(8943), (int)(1));
            qs.setCond(7);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "grandmaster_oltlin_q0127_06.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=17") && qs.getQuestItemsCount((int)(8943)) < 0) {
            string2 = "juria_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=17") && qs.getQuestItemsCount((int)(8943)) >= 1) {
            string2 = "juria_q0127_03.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=19")) {
            qs.giveItems((int)(8942), (int)(1));
            qs.setCond(8);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "juria_q0127_06.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=20") && (qs.getQuestItemsCount((int)(8940)) < 1 || qs.getQuestItemsCount((int)(8941)) < 1 || qs.getQuestItemsCount((int)(8942)) < 1 || qs.getQuestItemsCount((int)(8943)) < 1 || qs.getQuestItemsCount((int)(8944)) < 1 || qs.getQuestItemsCount((int)(8939)) < 1)) {
            string2 = "sir_kristof_rodemai_q0127_02.htm";
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=20") && qs.getQuestItemsCount((int)(8940)) >= 1 && qs.getQuestItemsCount((int)(8941)) >= 1 && qs.getQuestItemsCount((int)(8942)) >= 1 && qs.getQuestItemsCount((int)(8943)) >= 1 && qs.getQuestItemsCount((int)(8944)) >= 1 && qs.getQuestItemsCount((int)(8939)) >= 1) {
            string2 = "sir_kristof_rodemai_q0127_03.htm";
        } else if (event.equalsIgnoreCase("kamaelstory")) {
            string2 = "sir_kristof_rodemai_q0127_07.htm";
            
        } else if (event.equalsIgnoreCase("sir_kristof_rodemai_q0127_08")) {
            string2 = "sir_kristof_rodemai_q0127_08.htm";
            qs.setCond(9);
            qs.playSound(QuestState.SOUND_MIDDLE);
        } else if (event.equalsIgnoreCase("menu_select?ask=127&reply=23") && qs.getQuestItemsCount((int)(8940)) >= 1 && qs.getQuestItemsCount((int)(8941)) >= 1 && qs.getQuestItemsCount((int)(8942)) >= 1 && qs.getQuestItemsCount((int)(8943)) >= 1 && qs.getQuestItemsCount((int)(8944)) >= 1 && qs.getQuestItemsCount((int)(8939)) >= 1) {
            qs.takeItems((int)(8940), (int)(-1));
            qs.takeItems((int)(8941), (int)(-1));
            qs.takeItems((int)(8942), (int)(-1));
            qs.takeItems((int)(8943), (int)(-1));
            qs.takeItems((int)(8944), (int)(-1));
            qs.takeItems((int)(8939), (int)(-1));
            qs.giveItems((int)(57), (int)(159100));
            string2 = "falsepriest_dominic_q0127_09.htm";
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(false);
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = npc.getNpcId();
        int n2 = qs.getCond();
        if (n == 31350) {
            if (n2 == 0) {
                if (pc.level() >= 1) {
                    html = "falsepriest_dominic_q0127_01.htm";
                } else {
                    html = "falsepriest_dominic_q0127_02.htm";
                    qs.exitQuest(true);
                }
            } else if (n2 == 1) {
                html = "falsepriest_dominic_q0127_06.htm";
            } else if (n2 > 1 && n2 < 9) {
                html = "falsepriest_dominic_q0127_08.htm";
            } else if (n2 == 9) {
                html = "falsepriest_dominic_q0127_07.htm";
            }
        } else if (n == 30187) {
            if (n2 == 1) {
                html = "kai_q0127_01.htm";
            } else if (n2 == 2) {
                html = "kai_q0127_03.htm";
            } else if (n2 == 3) {
                html = "kai_q0127_11.htm";
            } else if (n2 > 3) {
                html = "kai_q0127_13.htm";
            }
        } else if (n == 32092) {
            if (n2 == 3) {
                html = "warehouse_chief_older_q0127_01.htm";
            } else if (n2 == 4) {
                html = "warehouse_chief_older_q0127_03.htm";
            } else if (n2 == 5) {
                html = "warehouse_chief_older_q0127_08.htm";
            } else if (n2 > 5) {
                html = "warehouse_chief_older_q0127_09.htm";
            }
        } else if (n == 31288) {
            if (n2 == 5) {
                html = "high_prefect_aklan_q0127_01.htm";
            } else if (n2 == 6) {
                html = "high_prefect_aklan_q0127_07.htm";
            } else if (n2 > 6) {
                html = "high_prefect_aklan_q0127_08.htm";
            }
        } else if (n == 30862) {
            if (n2 == 6) {
                html = "grandmaster_oltlin_q0127_01.htm";
            } else if (n2 == 7) {
                html = "grandmaster_oltlin_q0127_07.htm";
            } else if (n2 > 7) {
                html = "grandmaster_oltlin_q0127_08.htm";
            }
        } else if (n == 30113) {
            if (n2 == 7) {
                html = "juria_q0127_01.htm";
            } else if (n2 == 8) {
                html = "juria_q0127_07.htm";
            } else if (n2 > 8) {
                html = "juria_q0127_08.htm";
            }
        } else if (n == 30756) {
            if (n2 == 8) {
                html = "sir_kristof_rodemai_q0127_01.htm";
            } else if (n2 == 9) {
                html = "sir_kristof_rodemai_q0127_09.htm";
            }
        }
        return html;
    
	}


}
