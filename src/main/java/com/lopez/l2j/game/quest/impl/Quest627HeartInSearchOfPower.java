package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
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
 * Quest 627 - 627_HeartInSearchOfPower
 */
@Component
public class Quest627HeartInSearchOfPower extends Quest {

	public static final int bPR = 31518;
	public static final int bPS = 31519;
	public static final int bDX = 21520;
	public static final int bDZ = 21523;
	public static final int bPT = 21524;
	public static final int bPU = 21525;
	public static final int bEc = 21526;
	public static final int bPV = 21529;
	public static final int bDY = 21530;
	public static final int bPW = 21531;
	public static final int bPX = 21532;
	public static final int bPY = 21535;
	public static final int bDW = 21536;
	public static final int bDU = 21539;
	public static final int bDV = 21540;
	public static final int bPZ = 21658;
	public static final int bQa = 7170;
	public static final int bQb = 7171;
	public static final int bQc = 7172;
	public static final int bQd = 4041;
	public static final int boG = 4042;
	public static final int buT = 4043;
	public static final int buU = 4044;

	public Quest627HeartInSearchOfPower(QuestManager questManager) {
	super(627, "627_HeartInSearchOfPower", "627_HeartInSearchOfPower");
		this.addStartNpc(31518);
		this.addTalkId(31519);
		this.addQuestItem(7171, 7170, 7172);
		this.addKillId(21520, 21523, 21524, 21525, 21526, 21529, 21530, 21531, 21532, 21535, 21536, 21539, 21540, 21658);
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
        int n = qs.getInt("temptation_of_power_cookie");
        int n2 = getFirstStartNpc();
        if (n2 == 31518) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("temptation_of_power", String.valueOf(11), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "dark_necromancer_q0627_0104.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=1") && n == 1) {
                if (qs.getQuestItemsCount(7171) >= 300L) {
                    qs.setCond(3);
                    qs.set("temptation_of_power", String.valueOf(21), true);
                    qs.takeItems(7171, 300L);
                    qs.giveItems(7170, 1L, false);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "dark_necromancer_q0627_0201.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0202.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=3") && n == 3) {
                string2 = "dark_necromancer_q0627_0401.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=11") && n == 3) {
                if (qs.getQuestItemsCount(7172) >= 1L) {
                    qs.takeItems(7172, -1L);
                    qs.giveItems(57, 100000L, true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dark_necromancer_q0627_0402.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0403.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=12") && n == 3) {
                if (qs.getQuestItemsCount(7172) >= 1L) {
                    qs.takeItems(7172, -1L);
                    qs.giveItems(4043, 13L, true);
                    qs.giveItems(57, 6400L, true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dark_necromancer_q0627_0402.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0403.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=13") && n == 3) {
                if (qs.getQuestItemsCount(7172) >= 1L) {
                    qs.takeItems(7172, -1L);
                    qs.giveItems(4044, 13L, true);
                    qs.giveItems(57, 6400L, true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dark_necromancer_q0627_0402.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0403.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=14") && n == 3) {
                if (qs.getQuestItemsCount(7172) >= 1L) {
                    qs.takeItems(7172, -1L);
                    qs.giveItems(4042, 6L, true);
                    qs.giveItems(57, 13600L, true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dark_necromancer_q0627_0402.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0403.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=627&reply=15") && n == 3) {
                if (qs.getQuestItemsCount(7172) >= 1L) {
                    qs.takeItems(7172, -1L);
                    qs.giveItems(4041, 3L, true);
                    qs.giveItems(57, 17200L, true);
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    string2 = "dark_necromancer_q0627_0402.htm";
                } else {
                    string2 = "dark_necromancer_q0627_0403.htm";
                }
            }
        } else if (n2 == 31519 && event.equalsIgnoreCase("menu_select?ask=627&reply=1") && n == 2) {
            if (qs.getQuestItemsCount(7170) >= 1L) {
                qs.setCond(4);
                qs.set("temptation_of_power", String.valueOf(31), true);
                qs.takeItems(7170, 1L);
                qs.giveItems(7172, 1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "enfeux_q0627_0301.htm";
            } else {
                string2 = "enfeux_q0627_0302.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("temptation_of_power");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31518) break;
                if (pc.getLevel() >= 60) {
                    html = "dark_necromancer_q0627_0101.htm";
                    break;
                }
                qs.exitQuest(true);
                html = "dark_necromancer_q0627_0103.htm";
                break;
            }
            case 2: {
                if (n2 == 31518) {
                    if (n >= 11 && n <= 12) {
                        if (n == 12 && qs.getQuestItemsCount(7171) >= 300L) {
                            qs.set("temptation_of_power_cookie", String.valueOf(1), true);
                            html = "dark_necromancer_q0627_0105.htm";
                            break;
                        }
                        html = "dark_necromancer_q0627_0106.htm";
                        break;
                    }
                    if (n == 21) {
                        html = "dark_necromancer_q0627_0203.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(7172) < 1L || n != 31) break;
                    qs.set("temptation_of_power_cookie", String.valueOf(3), true);
                    html = "dark_necromancer_q0627_0301.htm";
                    break;
                }
                if (n2 != 31519) break;
                if (qs.getQuestItemsCount(7170) >= 1L && n == 21) {
                    qs.set("temptation_of_power_cookie", String.valueOf(2), true);
                    html = "enfeux_q0627_0201.htm";
                    break;
                }
                if (n != 31) break;
                html = "enfeux_q0627_0303.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 21520) {
            int n3 = ThreadLocalRandom.current().nextInt(1000);
            if (n3 < 661) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21523) {
            int n4 = ThreadLocalRandom.current().nextInt(1000);
            if (n4 < 668) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21524 || n2 == 21525) {
            int n5 = ThreadLocalRandom.current().nextInt(1000);
            if (n5 < 714) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21526) {
            int n6 = ThreadLocalRandom.current().nextInt(1000);
            if (n6 < 796) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21529) {
            int n7 = ThreadLocalRandom.current().nextInt(1000);
            if (n7 < 659) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21530) {
            int n8 = ThreadLocalRandom.current().nextInt(1000);
            if (n8 < 704) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21531 || n2 == 21658) {
            int n9 = ThreadLocalRandom.current().nextInt(1000);
            if (n9 < 791) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21532) {
            int n10 = ThreadLocalRandom.current().nextInt(1000);
            if (n10 < 820) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21535) {
            int n11 = ThreadLocalRandom.current().nextInt(1000);
            if (n11 < 827) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n2 == 21536) {
            int n12 = ThreadLocalRandom.current().nextInt(1000);
            if (n12 < 798) {
                if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                    if (qs.getQuestItemsCount(7171) <= 300L) {
                        qs.setCond(2);
                        qs.set("temptation_of_power", String.valueOf(12), true);
                        qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7171, 1L, true);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if ((n2 == 21539 || n2 == 21540) && (n = ThreadLocalRandom.current().nextInt(1000)) < 875) {
            if (qs.getQuestItemsCount(7171) + 1L >= 300L) {
                if (qs.getQuestItemsCount(7171) <= 300L) {
                    qs.setCond(2);
                    qs.set("temptation_of_power", String.valueOf(12), true);
                    qs.giveItems(7171, 300L - qs.getQuestItemsCount(7171));
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            } else {
                qs.giveItems(7171, 1L, true);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
