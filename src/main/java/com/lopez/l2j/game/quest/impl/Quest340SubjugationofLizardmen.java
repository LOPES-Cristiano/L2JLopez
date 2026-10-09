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
 * Quest 340 - 340_SubjugationofLizardmen
 */
@Component
public class Quest340SubjugationofLizardmen extends Quest {

	public static final int bmN = 30385;
	public static final int bmO = 30037;
	public static final int bmP = 30375;
	public static final int bmQ = 30989;
	public static final int bmR = 20008;
	public static final int bmS = 20010;
	public static final int bmT = 20014;
	public static final int bmU = 20027;
	public static final int bmV = 20024;
	public static final int bmW = 25146;
	public static final int bmX = 4257;
	public static final int bmY = 4256;
	public static final int bmZ = 4255;

	public Quest340SubjugationofLizardmen(QuestManager questManager) {
	super(340, "340_SubjugationofLizardmen", "340_SubjugationofLizardmen");
		this.addStartNpc(bmN);
		this.addTalkId(bmO, bmP, bmQ);
		this.addQuestItem(bmX, bmY, bmZ);
		this.addKillId(bmR, bmS, bmT, bmU, bmV, bmW);
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
        int n2 = qs.getInt("subj_lizardmen");
        if (n == bmN) {
            if (event.equalsIgnoreCase("quest_accept")) {
                if (pc.getLevel() >= 17) {
                    qs.setCond(1);
                    qs.set("subj_lizardmen", String.valueOf(1), true);
                    qs.setState(State.STARTED);
                    qs.playSound(QuestState.SOUND_ACCEPT);
                    string2 = "guard_weisz_q0340_03.htm";
                } else {
                    string2 = "guard_weisz_q0340_02.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=340&reply=1")) {
                string2 = "guard_weisz_q0340_04.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=340&reply=2")) {
                qs.takeItems(bmZ, -1L);
                qs.set("subj_lizardmen", String.valueOf(2), true);
                qs.setCond(2);
                string2 = "guard_weisz_q0340_07.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=340&reply=3")) {
                string2 = "guard_weisz_q0340_08.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=340&reply=4")) {
                if (qs.getQuestItemsCount(bmZ) >= 30L) {
                    qs.giveItems(57, 4090L);
                    qs.takeItems(bmZ, -1L);
                    qs.set("subj_lizardmen", String.valueOf(1), true);
                    string2 = "guard_weisz_q0340_09.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=340&reply=5") && qs.getQuestItemsCount(bmZ) >= 30L) {
                qs.giveItems(57, 4090L);
                qs.takeItems(bmZ, -1L);
                qs.unset("subj_lizardmen");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(false);
                string2 = "guard_weisz_q0340_10.htm";
            }
        } else if (n == bmO) {
            if (event.equalsIgnoreCase("menu_select?ask=340&reply=1")) {
                qs.setCond(5);
                qs.set("subj_lizardmen", String.valueOf(5), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "levian_q0340_02.htm";
            }
        } else if (n == bmP) {
            if (event.equalsIgnoreCase("menu_select?ask=340&reply=1")) {
                qs.setCond(3);
                qs.set("subj_lizardmen", String.valueOf(3), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "priest_adonius_q0340_02.htm";
            }
        } else if (n == bmQ && event.equalsIgnoreCase("menu_select?ask=340&reply=1")) {
            if (n2 == 5) {
                qs.setCond(6);
                qs.set("subj_lizardmen", String.valueOf(6), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "chest_of_bifrons_q0340_02.htm";
                qs.giveItems(4258, 1L);
            } else {
                string2 = "chest_of_bifrons_q0340_03.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("subj_lizardmen");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != bmN) break;
                if (pc.getLevel() < 17 || pc.getLevel() > 22) {
                    html = "guard_weisz_q0340_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "guard_weisz_q0340_02.htm";
                break;
            }
            case 2: {
                if (n2 == bmN) {
                    if (n == 1 && qs.getQuestItemsCount(bmZ) < 30L) {
                        html = "guard_weisz_q0340_05.htm";
                    } else if (n == 1 && qs.getQuestItemsCount(bmZ) >= 30L) {
                        html = "guard_weisz_q0340_06.htm";
                    } else if (n == 2) {
                        html = "guard_weisz_q0340_11.htm";
                    } else if (n >= 3 && n < 7) {
                        html = "guard_weisz_q0340_12.htm";
                    } else if (n == 7) {
                        qs.giveItems(57, 14700L);
                        qs.unset("subj_lizardmen");
                        qs.playSound(QuestState.SOUND_FINISH);
                        qs.exitQuest(false);
                        html = "guard_weisz_q0340_13.htm";
                    }
                }
                if (n2 == bmO) {
                    if (n == 4) {
                        html = "levian_q0340_01.htm";
                    } else if (n == 5) {
                        html = "levian_q0340_03.htm";
                    } else if (n == 6) {
                        qs.takeItems(4258, -1L);
                        qs.set("subj_lizardmen", String.valueOf(7), true);
                        qs.setCond(7);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "levian_q0340_04.htm";
                    } else if (n == 7) {
                        html = "levian_q0340_05.htm";
                    }
                }
                if (n2 == bmP) {
                    if (n == 2) {
                        html = "priest_adonius_q0340_01.htm";
                        break;
                    }
                    if (n == 3 && (qs.getQuestItemsCount(bmY) == 0L || qs.getQuestItemsCount(bmX) == 0L)) {
                        html = "priest_adonius_q0340_03.htm";
                        break;
                    }
                    if (n == 3 && qs.getQuestItemsCount(bmY) >= 1L && qs.getQuestItemsCount(bmX) >= 1L) {
                        qs.takeItems(bmY, -1L);
                        qs.takeItems(bmX, -1L);
                        qs.setCond(4);
                        qs.set("subj_lizardmen", String.valueOf(4), true);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "priest_adonius_q0340_04.htm";
                        break;
                    }
                    if (n == 4) {
                        html = "priest_adonius_q0340_05.htm";
                        break;
                    }
                    if (n < 5) break;
                    html = "priest_adonius_q0340_06.htm";
                    break;
                }
                if (n2 != bmQ || n != 5) break;
                html = "chest_of_bifrons_q0340_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getInt("subj_lizardmen");
        if (qs.getState() != State.STARTED) {
            return null;
        }
        if (n == bmR || n == bmS || n == bmT) {
            if (n2 == 1 && qs.getQuestItemsCount(bmZ) < 30L && ThreadLocalRandom.current().nextInt(100) < 63) {
                qs.giveItems(bmZ, 1L);
                if (qs.getQuestItemsCount(bmZ) >= 30L) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else {
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n == bmU || n == bmV) {
            if (n2 == 3) {
                if (qs.getQuestItemsCount(bmY) == 0L) {
                    if (ThreadLocalRandom.current().nextInt(100) <= 18) {
                        qs.giveItems(bmY, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                } else if (qs.getQuestItemsCount(bmY) >= 1L && qs.getQuestItemsCount(bmX) == 0L && ThreadLocalRandom.current().nextInt(100) <= 18) {
                    qs.giveItems(bmX, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n == bmW) {
            qs.addSpawn(bmQ);
        }
        return null;
    
	}

}
