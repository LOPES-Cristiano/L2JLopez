package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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
 * Quest 660 - 660_AidingtheFloranVillage
 */
@Component
public class Quest660AidingtheFloranVillage extends Quest {

	public static final int bUj = 30608;
	public static final int bUk = 30291;
	public static final int bEu = 20781;
	public static final int bGv = 21102;
	public static final int bGe = 21103;
	public static final int bEt = 21104;
	public static final int bEr = 21105;
	public static final int bEn = 21106;
	public static final int bEs = 21107;
	public static final int bUl = 8074;
	public static final int bUm = 8075;
	public static final int bUn = 8076;
	public static final int azF = 956;
	public static final int bUo = 955;

	public Quest660AidingtheFloranVillage(QuestManager questManager) {
	super(660, "660_AidingtheFloranVillage", "660_AidingtheFloranVillage");
		this.addStartNpc(30608);
		this.addTalkId(30291);
		this.addKillId(20781, 21102, 21103, 21104, 21105, 21106, 21107);
		this.addQuestItem(8074, 8075, 8076);
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
        if (n == 30608) {
            if (event.equalsIgnoreCase("quest_accept")) {
                if (pc.getLevel() >= 30) {
                    qs.setCond(1);
                    qs.set("support_ploran_town", String.valueOf(1), true);
                    qs.setState(State.STARTED);
                    qs.playSound(QuestState.SOUND_ACCEPT);
                    string2 = "marya_q0660_06.htm";
                } else {
                    string2 = "marya_q0660_06a.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=20")) {
                string2 = "marya_q0660_02.htm";
            }
        } else if (n == 30291) {
            if (event.equalsIgnoreCase("menu_select?ask=660&reply=1")) {
                long l;
                long l2;
                long l3 = qs.getQuestItemsCount(8074);
                long l4 = l3 + (l2 = qs.getQuestItemsCount(8075)) + (l = qs.getQuestItemsCount(8076));
                if (l4 > 0L) {
                    qs.giveItems(57, l4 * 100L);
                    qs.takeItems(8074, -1L);
                    qs.takeItems(8075, -1L);
                    qs.takeItems(8076, -1L);
                    string2 = "alankell_q0660_06.htm";
                } else {
                    string2 = "alankell_q0660_08.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=2")) {
                string2 = "alankell_q0660_09.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=3")) {
                qs.takeItems(8074, -1L);
                qs.takeItems(8075, -1L);
                qs.takeItems(8076, -1L);
                qs.unset("support_ploran_town");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "alankell_q0660_08a.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=10")) {
                long l;
                long l5;
                long l6 = qs.getQuestItemsCount(8074);
                long l7 = l6 + (l5 = qs.getQuestItemsCount(8075)) + (l = qs.getQuestItemsCount(8076));
                if (l7 < 100L) {
                    string2 = "alankell_q0660_11.htm";
                } else {
                    long l8 = 100L;
                    if (l6 < l8) {
                        qs.takeItems(8074, l6);
                        l8 -= l6;
                    } else {
                        qs.takeItems(8074, l8);
                        l8 = 0L;
                    }
                    if (l5 < l8) {
                        qs.takeItems(8075, l5);
                        l8 -= l5;
                    } else {
                        qs.takeItems(8075, l8);
                        l8 = 0L;
                    }
                    if (l < l8) {
                        qs.takeItems(8076, l);
                        l8 -= l;
                    } else {
                        qs.takeItems(8076, l8);
                        l8 = 0L;
                    }
                    if (ThreadLocalRandom.current().nextInt(99) > 50) {
                        qs.giveItems(956, 1L);
                        qs.giveItems(57, 13000L);
                        string2 = "alankell_q0660_12.htm";
                    } else {
                        qs.giveItems(57, 1000L);
                        string2 = "alankell_q0660_13.htm";
                    }
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=14")) {
                long l;
                long l9;
                long l10 = qs.getQuestItemsCount(8074);
                long l11 = l10 + (l9 = qs.getQuestItemsCount(8075)) + (l = qs.getQuestItemsCount(8076));
                if (l11 < 200L) {
                    string2 = "alankell_q0660_15.htm";
                } else {
                    long l12 = 200L;
                    if (l10 < l12) {
                        qs.takeItems(8074, l10);
                        l12 -= l10;
                    } else {
                        qs.takeItems(8074, l12);
                        l12 = 0L;
                    }
                    if (l9 < l12) {
                        qs.takeItems(8075, l9);
                        l12 -= l9;
                    } else {
                        qs.takeItems(8075, l12);
                        l12 = 0L;
                    }
                    if (l < l12) {
                        qs.takeItems(8076, l);
                        l12 -= l;
                    } else {
                        qs.takeItems(8076, l12);
                        l12 = 0L;
                    }
                    if (ThreadLocalRandom.current().nextInt(100) >= 50) {
                        if (ThreadLocalRandom.current().nextInt(2) == 0) {
                            qs.giveItems(956, 1L);
                            qs.giveItems(57, 20000L);
                        } else {
                            qs.giveItems(955, 1L);
                        }
                        string2 = "alankell_q0660_16.htm";
                    } else {
                        qs.giveItems(57, 2000L);
                        string2 = "alankell_q0660_17.htm";
                    }
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=18")) {
                long l;
                long l13;
                long l14 = qs.getQuestItemsCount(8074);
                long l15 = l14 + (l13 = qs.getQuestItemsCount(8075)) + (l = qs.getQuestItemsCount(8076));
                if (l15 < 500L) {
                    string2 = "alankell_q0660_19.htm";
                } else {
                    long l16 = 500L;
                    if (l14 < l16) {
                        qs.takeItems(8074, l14);
                        l16 -= l14;
                    } else {
                        qs.takeItems(8074, l16);
                        l16 = 0L;
                    }
                    if (l13 < l16) {
                        qs.takeItems(8075, l13);
                        l16 -= l13;
                    } else {
                        qs.takeItems(8075, l16);
                        l16 = 0L;
                    }
                    if (l < l16) {
                        qs.takeItems(8076, l);
                        l16 -= l;
                    } else {
                        qs.takeItems(8076, l16);
                        l16 = 0L;
                    }
                    if (ThreadLocalRandom.current().nextInt(100) >= 50) {
                        qs.giveItems(955, 1L);
                        qs.giveItems(57, 45000L);
                        string2 = "alankell_q0660_20.htm";
                    } else {
                        qs.giveItems(57, 5000L);
                        string2 = "alankell_q0660_21.htm";
                    }
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=660&reply=20")) {
                long l;
                long l17;
                long l18 = qs.getQuestItemsCount(8074);
                long l19 = l18 + (l17 = qs.getQuestItemsCount(8075)) + (l = qs.getQuestItemsCount(8076));
                if (l19 <= 0L) {
                    string2 = "alankell_q0660_23.htm";
                } else {
                    long l20 = l19 * 100L;
                    qs.giveItems(57, l20);
                    string2 = "alankell_q0660_22.htm";
                }
                qs.takeItems(8074, -1L);
                qs.takeItems(8075, -1L);
                qs.takeItems(8076, -1L);
                qs.unset("support_ploran_town");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("support_ploran_town");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30608) break;
                if (pc.getLevel() < 30) {
                    html = "marya_q0660_04.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "marya_q0660_01.htm";
                break;
            }
            case 2: {
                if (n2 == 30608) {
                    if (n != 1) break;
                    html = "marya_q0660_05.htm";
                    break;
                }
                if (n2 != 30291) break;
                if (n == 1) {
                    qs.setCond(2);
                    qs.set("support_ploran_town", String.valueOf(2), true);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "alankell_q0660_04.htm";
                    break;
                }
                if (n != 2) break;
                html = "alankell_q0660_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("support_ploran_town");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 20781 || n2 == 21104) {
            if (n == 2 && ThreadLocalRandom.current().nextInt(100) < 65) {
                qs.giveItems(8076, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 21102) {
            if (n == 2 && ThreadLocalRandom.current().nextInt(100) < 50) {
                qs.giveItems(8074, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 21103) {
            if (n == 2 && ThreadLocalRandom.current().nextInt(100) < 52) {
                qs.giveItems(8075, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 21105) {
            if (n == 2 && ThreadLocalRandom.current().nextInt(100) < 75) {
                qs.giveItems(8076, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 21106) {
            if (n == 2 && ThreadLocalRandom.current().nextInt(100) < 63) {
                qs.giveItems(8074, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 21107 && n == 2) {
            if (ThreadLocalRandom.current().nextInt(100) < 33) {
                qs.giveItems(8076, 2L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else {
                qs.giveItems(8076, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
