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
 * Quest 333 - 333_BlackLionHunt
 */
@Component
public class Quest333BlackLionHunt extends Quest {

	public static final int bhU = 30735;
	public static final int bhV = 30736;
	public static final int bhW = 30471;
	public static final int aIc = 30130;
	public static final int bhX = 30531;
	public static final int bhY = 30737;
	public static final int bhZ = 20157;
	public static final int bia = 20160;
	public static final int bib = 20171;
	public static final int bic = 20197;
	public static final int bid = 20198;
	public static final int bie = 20200;
	public static final int bif = 20201;
	public static final int big = 20207;
	public static final int bih = 20208;
	public static final int bii = 20209;
	public static final int bij = 20210;
	public static final int bik = 20211;
	public static final int bil = 20230;
	public static final int bim = 20232;
	public static final int bin = 20234;
	public static final int bio = 20251;
	public static final int bip = 20252;
	public static final int biq = 20253;
	public static final int bir = 27151;
	public static final int bis = 27152;
	public static final int bgx = 1369;
	public static final int bit = 3675;
	public static final int biu = 3676;
	public static final int biv = 3677;
	public static final int biw = 3848;
	public static final int bix = 3849;
	public static final int biy = 3850;
	public static final int biz = 3851;
	public static final int biA = 3671;
	public static final int biB = 3672;
	public static final int biC = 3673;
	public static final int biD = 3674;
	public static final int biE = 3440;
	public static final int biF = 3441;
	public static final int biG = 3442;
	public static final int biH = 3443;
	public static final int bdp = 1061;
	public static final int biI = 1463;
	public static final int biJ = 2510;
	public static final int biK = 736;
	public static final int biL = 735;
	public static final int biM = 3444;
	public static final int biN = 3445;
	public static final int biO = 3446;
	public static final int biP = 3447;
	public static final int biQ = 3448;
	public static final int biR = 3449;
	public static final int biS = 3450;
	public static final int biT = 3451;
	public static final int biU = 3452;
	public static final int biV = 3453;
	public static final int biW = 3454;
	public static final int biX = 3455;
	public static final int biY = 3456;
	public static final int biZ = 3457;
	public static final int bja = 3458;
	public static final int bjb = 3459;
	public static final int bjc = 3460;
	public static final int bjd = 3461;
	public static final int bje = 3462;
	public static final int bjf = 3463;
	public static final int bjg = 3464;
	public static final int bjh = 3465;
	public static final int bji = 3466;

	public Quest333BlackLionHunt(QuestManager questManager) {
		super(333, "333_BlackLionHunt", "333_BlackLionHunt");
		addStartNpc(30735);
		addTalkNpc(30130);
		addTalkNpc(30736);
		addTalkNpc(30531);
		addTalkNpc(30471);
		addTalkNpc(30737);
		addKillId(20207);
		addKillId(27152);
		addKillId(20253);
		addKillId(20201);
		addKillId(20251);
		addKillId(20171);
		addKillId(27151);
		addKillId(20157);
		addKillId(20210);
		addKillId(20197);
		addKillId(20211);
		addKillId(20198);
		addKillId(20209);
		addKillId(20234);
		addKillId(20252);
		addKillId(20200);
		addKillId(20160);
		addKillId(20230);
		addKillId(20232);
		addKillId(20208);
		registerQuestItems(3675, 3676, 3677, 3848, 3849, 3850, 3851, 3671, 3672, 3673, 3674);
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
        int n = qs.getInt("hunt_of_blacklion");
        int n2 = getFirstStartNpc();
        if (n2 == 30735) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound("QuestState.SOUND_ACCEPT");
                string2 = "sophia_q0333_04.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                string2 = "sophia_q0333_05.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                string2 = "sophia_q0333_06.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=3")) {
                string2 = "sophia_q0333_07.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=4")) {
                string2 = "sophia_q0333_08.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=5")) {
                string2 = "sophia_q0333_09.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=6")) {
                if (qs.getQuestItemsCount(3671) == 0L) {
                    qs.giveItems(3671, 1L);
                    string2 = "sophia_q0333_10.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=7")) {
                if (qs.getQuestItemsCount(3672) == 0L) {
                    qs.giveItems(3672, 1L);
                    string2 = "sophia_q0333_11.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=8")) {
                if (qs.getQuestItemsCount(3673) == 0L) {
                    qs.giveItems(3673, 1L);
                    string2 = "sophia_q0333_12.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=9")) {
                if (qs.getQuestItemsCount(3674) == 0L) {
                    qs.giveItems(3674, 1L);
                    string2 = "sophia_q0333_13.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=10")) {
                if (qs.getQuestItemsCount(3675) < 10L) {
                    string2 = "sophia_q0333_16.htm";
                } else if (qs.getQuestItemsCount(3675) >= 10L && qs.getQuestItemsCount(3676) < 4L) {
                    qs.giveItems(3676, 1L);
                    int n3 = ThreadLocalRandom.current().nextInt(100);
                    if (n3 < 25) {
                        qs.giveItems(1061, 20L);
                    } else if (n3 < 50) {
                        if (!pc.isMage()) {
                            qs.giveItems(1463, 100L);
                        } else {
                            qs.giveItems(2510, 50L);
                        }
                    } else if (n3 < 75) {
                        qs.giveItems(736, 20L);
                    } else {
                        qs.giveItems(735, 3L);
                    }
                    qs.takeItems(3675, 10L);
                    string2 = "sophia_q0333_17a.htm";
                } else if (qs.getQuestItemsCount(3675) >= 10L && qs.getQuestItemsCount(3676) >= 4L && qs.getQuestItemsCount(3676) <= 7L) {
                    qs.giveItems(3676, 1L);
                    int n4 = ThreadLocalRandom.current().nextInt(100);
                    if (n4 < 25) {
                        qs.giveItems(1061, 25L);
                    } else if (n4 < 50) {
                        if (!pc.isMage()) {
                            qs.giveItems(1463, 200L);
                        } else {
                            qs.giveItems(2510, 100L);
                        }
                    } else if (n4 < 75) {
                        qs.giveItems(736, 20L);
                    } else {
                        qs.giveItems(735, 3L);
                    }
                    qs.takeItems(3675, 10L);
                    string2 = "sophia_q0333_18b.htm";
                } else if (qs.getQuestItemsCount(3675) >= 10L && qs.getQuestItemsCount(3676) >= 8L) {
                    int n5;
                    if (qs.getQuestItemsCount(3676) > 8L) {
                        qs.takeItems(3676, qs.getQuestItemsCount(3676) - 8L);
                    }
                    if ((n5 = ThreadLocalRandom.current().nextInt(100)) < 25) {
                        qs.giveItems(1061, 50L);
                    } else if (n5 < 50) {
                        if (!pc.isMage()) {
                            qs.giveItems(1463, 400L);
                        } else {
                            qs.giveItems(2510, 200L);
                        }
                    } else if (n5 < 75) {
                        qs.giveItems(736, 30L);
                    } else {
                        qs.giveItems(735, 4L);
                    }
                    qs.takeItems(3675, 10L);
                    string2 = "sophia_q0333_19b.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=11")) {
                qs.takeItems(3671, -1L);
                qs.takeItems(3672, -1L);
                qs.takeItems(3673, -1L);
                qs.takeItems(3674, -1L);
                string2 = "sophia_q0333_20.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=12")) {
                string2 = "sophia_q0333_21.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=13")) {
                string2 = "sophia_q0333_24a.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=14")) {
                string2 = "sophia_q0333_25b.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=15") && qs.getQuestItemsCount(1369) >= 1L) {
                qs.giveItems(57, 12400L);
                qs.takeItems(1369, -1L);
                string2 = "sophia_q0333_26.htm";
                qs.playSound("QuestState.SOUND_FINISH");
                qs.exitQuest(true);
            }
        } else if (n2 == 30737) {
            if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) >= 1L) {
                    if (qs.getQuestItemsCount(3440) >= 1L) {
                        qs.takeItems(3440, 1L);
                    } else if (qs.getQuestItemsCount(3441) >= 1L) {
                        qs.takeItems(3441, 1L);
                    } else if (qs.getQuestItemsCount(3442) >= 1L) {
                        qs.takeItems(3442, 1L);
                    } else if (qs.getQuestItemsCount(3443) >= 1L) {
                        qs.takeItems(3443, 1L);
                    }
                    if (qs.getQuestItemsCount(3677) < 80L) {
                        qs.giveItems(3677, 1L);
                    } else if (qs.getQuestItemsCount(3677) > 80L) {
                        qs.takeItems(3677, qs.getQuestItemsCount(3677) - 80L);
                    }
                    if (qs.getQuestItemsCount(3677) < 40L) {
                        qs.giveItems(57, 100L);
                        string2 = "morgan_q0333_03.htm";
                    } else if (qs.getQuestItemsCount(3677) >= 40L && qs.getQuestItemsCount(3677) < 80L) {
                        qs.giveItems(57, 200L);
                        string2 = "morgan_q0333_04.htm";
                    } else {
                        qs.giveItems(57, 300L);
                        string2 = "morgan_q0333_05.htm";
                    }
                } else {
                    string2 = "morgan_q0333_06.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                string2 = "morgan_q0333_07.htm";
            }
        } else if (n2 == 30736) {
            if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                int n6 = ThreadLocalRandom.current().nextInt(100);
                int n7 = ThreadLocalRandom.current().nextInt(100);
                if (qs.getQuestItemsCount(57) < 650L && qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) >= 1L) {
                    string2 = "redfoot_q0333_03.htm";
                } else if (qs.getQuestItemsCount(57) >= 650L && qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) >= 1L) {
                    if (n6 < 40) {
                        if (n7 < 33) {
                            qs.giveItems(3444, 1L);
                            string2 = "redfoot_q0333_04a.htm";
                        } else if (n7 < 66) {
                            qs.giveItems(3445, 1L);
                            string2 = "redfoot_q0333_04b.htm";
                        } else {
                            qs.giveItems(3446, 1L);
                            string2 = "redfoot_q0333_04c.htm";
                        }
                    } else if (n6 < 60) {
                        if (n7 < 33) {
                            qs.giveItems(3447, 1L);
                            string2 = "redfoot_q0333_04d.htm";
                        } else if (n7 < 66) {
                            qs.giveItems(3448, 1L);
                            string2 = "redfoot_q0333_04e.htm";
                        } else {
                            qs.giveItems(3449, 1L);
                            string2 = "redfoot_q0333_04f.htm";
                        }
                    } else if (n6 < 70) {
                        if (n7 < 33) {
                            qs.giveItems(3450, 1L);
                            string2 = "redfoot_q0333_04g.htm";
                        } else if (n7 < 66) {
                            qs.giveItems(3451, 1L);
                            string2 = "redfoot_q0333_04h.htm";
                        } else {
                            qs.giveItems(3452, 1L);
                            string2 = "redfoot_q0333_04i.htm";
                        }
                    } else if (n6 < 75) {
                        if (n7 < 33) {
                            qs.giveItems(3453, 1L);
                            string2 = "redfoot_q0333_04j.htm";
                        } else if (n7 < 66) {
                            qs.giveItems(3454, 1L);
                            string2 = "redfoot_q0333_04k.htm";
                        } else {
                            qs.giveItems(3455, 1L);
                            string2 = "redfoot_q0333_04l.htm";
                        }
                    } else if (n6 < 76) {
                        qs.giveItems(3456, 1L);
                        string2 = "redfoot_q0333_04m.htm";
                    } else if (ThreadLocalRandom.current().nextInt(100) < 50) {
                        if (n7 < 25) {
                            qs.giveItems(3457, 1L);
                        } else if (n7 < 50) {
                            qs.giveItems(3458, 1L);
                        } else if (n7 < 75) {
                            qs.giveItems(3459, 1L);
                        } else {
                            qs.giveItems(3460, 1L);
                        }
                        string2 = "redfoot_q0333_04n.htm";
                    } else {
                        if (n7 < 25) {
                            qs.giveItems(3462, 1L);
                        } else if (n7 < 50) {
                            qs.giveItems(3463, 1L);
                        } else if (n7 < 75) {
                            qs.giveItems(3464, 1L);
                        } else {
                            qs.giveItems(3465, 1L);
                        }
                        string2 = "redfoot_q0333_04o.htm";
                    }
                    qs.takeItems(57, 650L);
                    if (qs.getQuestItemsCount(3440) >= 1L) {
                        qs.takeItems(3440, 1L);
                    } else if (qs.getQuestItemsCount(3441) >= 1L) {
                        qs.takeItems(3441, 1L);
                    } else if (qs.getQuestItemsCount(3442) >= 1L) {
                        qs.takeItems(3442, 1L);
                    } else if (qs.getQuestItemsCount(3443) >= 1L) {
                        qs.takeItems(3443, 1L);
                    }
                } else if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) < 1L) {
                    string2 = "redfoot_q0333_05.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                string2 = "redfoot_q0333_06.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=3")) {
                int n8 = ThreadLocalRandom.current().nextInt(100);
                if (qs.getQuestItemsCount(57) < (long)(200 + n * 200)) {
                    string2 = "redfoot_q0333_07.htm";
                } else if (n * 100 > 200) {
                    string2 = "redfoot_q0333_08.htm";
                }
                string2 = n8 < 5 ? "redfoot_q0333_08a.htm" : (n8 < 10 ? "redfoot_q0333_08b.htm" : (n8 < 15 ? "redfoot_q0333_08c.htm" : (n8 < 20 ? "redfoot_q0333_08d.htm" : (n8 < 25 ? "redfoot_q0333_08e.htm" : (n8 < 30 ? "redfoot_q0333_08f.htm" : (n8 < 35 ? "redfoot_q0333_08g.htm" : (n8 < 40 ? "redfoot_q0333_08h.htm" : (n8 < 45 ? "redfoot_q0333_08i.htm" : (n8 < 50 ? "redfoot_q0333_08j.htm" : (n8 < 55 ? "redfoot_q0333_08k.htm" : (n8 < 60 ? "redfoot_q0333_08l.htm" : (n8 < 65 ? "redfoot_q0333_08m.htm" : (n8 < 70 ? "redfoot_q0333_08n.htm" : (n8 < 75 ? "redfoot_q0333_08o.htm" : (n8 < 80 ? "redfoot_q0333_08p.htm" : (n8 < 85 ? "redfoot_q0333_08q.htm" : (n8 < 90 ? "redfoot_q0333_08r.htm" : (n8 < 95 ? "redfoot_q0333_08s.htm" : "redfoot_q0333_08t.htm"))))))))))))))))));
                qs.takeItems(57, 200 + n * 200);
                qs.set("hunt_of_blacklion", String.valueOf(n + 1), true);
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=4")) {
                string2 = "redfoot_q0333_09.htm";
            }
        } else if (n2 == 30471) {
            if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                if (qs.getQuestItemsCount(3457) == 0L || qs.getQuestItemsCount(3458) == 0L || qs.getQuestItemsCount(3459) == 0L || qs.getQuestItemsCount(3460) == 0L) {
                    string2 = "blacksmith_rupio_q0333_03.htm";
                } else if (ThreadLocalRandom.current().nextInt(100) < 50) {
                    qs.giveItems(3461, 1L);
                    qs.takeItems(3457, 1L);
                    qs.takeItems(3458, 1L);
                    qs.takeItems(3459, 1L);
                    qs.takeItems(3460, 1L);
                    string2 = "blacksmith_rupio_q0333_04.htm";
                } else {
                    qs.takeItems(3457, 1L);
                    qs.takeItems(3458, 1L);
                    qs.takeItems(3459, 1L);
                    qs.takeItems(3460, 1L);
                    string2 = "blacksmith_rupio_q0333_05.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                if (qs.getQuestItemsCount(3462) == 0L || qs.getQuestItemsCount(3463) == 0L || qs.getQuestItemsCount(3464) == 0L || qs.getQuestItemsCount(3465) == 0L) {
                    string2 = "blacksmith_rupio_q0333_06.htm";
                } else if (ThreadLocalRandom.current().nextInt(100) < 50) {
                    qs.giveItems(3466, 1L);
                    qs.takeItems(3462, 1L);
                    qs.takeItems(3463, 1L);
                    qs.takeItems(3464, 1L);
                    qs.takeItems(3465, 1L);
                    string2 = "blacksmith_rupio_q0333_07.htm";
                } else {
                    qs.takeItems(3462, 1L);
                    qs.takeItems(3463, 1L);
                    qs.takeItems(3464, 1L);
                    qs.takeItems(3465, 1L);
                    string2 = "blacksmith_rupio_q0333_08.htm";
                }
            }
        } else if (n2 == 30130) {
            if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                if (qs.getQuestItemsCount(3461) >= 1L) {
                    qs.giveItems(57, 30000L);
                    qs.takeItems(3461, 1L);
                    string2 = "undres_q0333_04.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                string2 = "undres_q0333_05.htm";
            }
        } else if (n2 == 30531) {
            if (event.equalsIgnoreCase("menu_select?ask=333&reply=1")) {
                if (qs.getQuestItemsCount(3466) >= 1L) {
                    qs.giveItems(57, 30000L);
                    qs.takeItems(3466, 1L);
                    string2 = "first_elder_lockirin_q0333_04.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=333&reply=2")) {
                string2 = "first_elder_lockirin_q0333_05.htm";
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
                if (n != 30735) break;
                if (pc.getLevel() < 25 || pc.getLevel() > 39) {
                    html = "sophia_q0333_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs.getQuestItemsCount(1369) == 0L) {
                    html = "sophia_q0333_02.htm";
                    break;
                }
                html = "sophia_q0333_03.htm";
                break;
            }
            case 2: {
                if (n == 30735) {
                    if (qs.getQuestItemsCount(3671) + qs.getQuestItemsCount(3672) + qs.getQuestItemsCount(3673) + qs.getQuestItemsCount(3674) == 0L) {
                        html = "sophia_q0333_14.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3671) + qs.getQuestItemsCount(3672) + qs.getQuestItemsCount(3673) + qs.getQuestItemsCount(3674) == 1L && qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851) < 1L && qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) < 1L) {
                        html = "sophia_q0333_15.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3671) + qs.getQuestItemsCount(3672) + qs.getQuestItemsCount(3673) + qs.getQuestItemsCount(3674) == 1L && qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851) < 1L && qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) >= 1L) {
                        html = "sophia_q0333_15a.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3671) + qs.getQuestItemsCount(3672) + qs.getQuestItemsCount(3673) + qs.getQuestItemsCount(3674) == 1L && qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851) >= 1L && qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) == 0L) {
                        long l = qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851);
                        if (l >= 20L) {
                            if (l < 50L) {
                                qs.giveItems(3675, 1L);
                            } else if (l < 100L) {
                                qs.giveItems(3675, 2L);
                            } else {
                                qs.giveItems(3675, 3L);
                            }
                        }
                        qs.giveItems(57, qs.getQuestItemsCount(3848) * 35L + qs.getQuestItemsCount(3851) * 35L + qs.getQuestItemsCount(3850) * 35L + qs.getQuestItemsCount(3849) * 35L);
                        qs.takeItems(3848, -1L);
                        qs.takeItems(3849, -1L);
                        qs.takeItems(3850, -1L);
                        qs.takeItems(3851, -1L);
                        html = "sophia_q0333_22.htm";
                        qs.set("hunt_of_blacklion", String.valueOf(0), true);
                        break;
                    }
                    if (qs.getQuestItemsCount(3671) + qs.getQuestItemsCount(3672) + qs.getQuestItemsCount(3673) + qs.getQuestItemsCount(3674) != 1L || qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851) < 1L || qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) < 1L) break;
                    long l = qs.getQuestItemsCount(3848) + qs.getQuestItemsCount(3849) + qs.getQuestItemsCount(3850) + qs.getQuestItemsCount(3851);
                    if (l >= 20L) {
                        if (l < 50L) {
                            qs.giveItems(3675, 1L);
                        } else if (l < 100L) {
                            qs.giveItems(3675, 2L);
                        } else {
                            qs.giveItems(3675, 3L);
                        }
                    }
                    if (qs.getQuestItemsCount(3848) > 0L) {
                        qs.giveItems(57, qs.getQuestItemsCount(3848) * 35L);
                    }
                    if (qs.getQuestItemsCount(3849) > 0L) {
                        qs.giveItems(57, qs.getQuestItemsCount(3849) * 35L);
                    }
                    if (qs.getQuestItemsCount(3850) > 0L) {
                        qs.giveItems(57, qs.getQuestItemsCount(3850) * 35L);
                    }
                    if (qs.getQuestItemsCount(3851) > 0L) {
                        qs.giveItems(57, qs.getQuestItemsCount(3851) * 35L);
                    }
                    qs.takeItems(3848, -1L);
                    qs.takeItems(3849, -1L);
                    qs.takeItems(3850, -1L);
                    qs.takeItems(3851, -1L);
                    html = "sophia_q0333_23.htm";
                    qs.set("hunt_of_blacklion", String.valueOf(0), true);
                    break;
                }
                if (n == 30737) {
                    if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) == 0L) {
                        html = "morgan_q0333_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) < 1L) break;
                    html = "morgan_q0333_02.htm";
                    break;
                }
                if (n == 30736) {
                    if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) == 0L) {
                        html = "redfoot_q0333_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3440) + qs.getQuestItemsCount(3441) + qs.getQuestItemsCount(3442) + qs.getQuestItemsCount(3443) < 1L) break;
                    html = "redfoot_q0333_02.htm";
                    break;
                }
                if (n == 30471) {
                    if (qs.getQuestItemsCount(3457) + qs.getQuestItemsCount(3458) + qs.getQuestItemsCount(3459) + qs.getQuestItemsCount(3460) < 1L && qs.getQuestItemsCount(3462) + qs.getQuestItemsCount(3463) + qs.getQuestItemsCount(3464) + qs.getQuestItemsCount(3465) < 1L) {
                        html = "blacksmith_rupio_q0333_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3457) + qs.getQuestItemsCount(3458) + qs.getQuestItemsCount(3459) + qs.getQuestItemsCount(3460) < 1L && qs.getQuestItemsCount(3462) + qs.getQuestItemsCount(3463) + qs.getQuestItemsCount(3464) + qs.getQuestItemsCount(3465) < 1L) break;
                    html = "blacksmith_rupio_q0333_02.htm";
                    break;
                }
                if (n == 30130) {
                    if (qs.getQuestItemsCount(3457) + qs.getQuestItemsCount(3458) + qs.getQuestItemsCount(3459) + qs.getQuestItemsCount(3460) < 1L && qs.getQuestItemsCount(3461) == 0L) {
                        html = "undres_q0333_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3457) + qs.getQuestItemsCount(3458) + qs.getQuestItemsCount(3459) + qs.getQuestItemsCount(3460) >= 1L && qs.getQuestItemsCount(3461) == 0L) {
                        html = "undres_q0333_02.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3461) < 1L) break;
                    html = "undres_q0333_03.htm";
                    break;
                }
                if (n != 30531) break;
                if (qs.getQuestItemsCount(3462) + qs.getQuestItemsCount(3463) + qs.getQuestItemsCount(3464) + qs.getQuestItemsCount(3465) < 1L && qs.getQuestItemsCount(3466) == 0L) {
                    html = "first_elder_lockirin_q0333_01.htm";
                    break;
                }
                if (qs.getQuestItemsCount(3462) + qs.getQuestItemsCount(3463) + qs.getQuestItemsCount(3464) + qs.getQuestItemsCount(3465) >= 1L && qs.getQuestItemsCount(3466) == 0L) {
                    html = "first_elder_lockirin_q0333_02.htm";
                    break;
                }
                if (qs.getQuestItemsCount(3466) < 1L) break;
                html = "first_elder_lockirin_q0333_03.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20157) {
            if (qs.getQuestItemsCount(3674) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 55) {
                    qs.giveItems(3851, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 12) {
                    qs.giveItems(3443, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 2 && qs.getQuestItemsCount(3674) > 0L) {
                    qs.addSpawn(27152);
                }
            }
        } else if (n == 20160) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 11) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20171) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 60) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 8) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20197) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 60) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 9) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20198) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 12) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20200) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 13) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20201) {
            if (qs.getQuestItemsCount(3671) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3848, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 15) {
                    qs.giveItems(3440, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20207) {
            if (qs.getQuestItemsCount(3672) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3849, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 9) {
                    qs.giveItems(3441, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20208) {
            if (qs.getQuestItemsCount(3672) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3849, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 10) {
                    qs.giveItems(3441, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20209) {
            if (qs.getQuestItemsCount(3672) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3849, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 11) {
                    qs.giveItems(3441, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20210) {
            if (qs.getQuestItemsCount(3672) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3849, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 12) {
                    qs.giveItems(3441, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20211) {
            if (qs.getQuestItemsCount(3672) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3849, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 13) {
                    qs.giveItems(3441, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n == 20230) {
            if (qs.getQuestItemsCount(3674) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 60) {
                    qs.giveItems(3851, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 13) {
                    qs.giveItems(3443, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 2 && qs.getQuestItemsCount(3674) > 0L) {
                    qs.addSpawn(27152);
                }
            }
        } else if (n == 20232) {
            if (qs.getQuestItemsCount(3674) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 56) {
                    qs.giveItems(3851, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 14) {
                    qs.giveItems(3443, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 2 && qs.getQuestItemsCount(3674) > 0L) {
                    qs.addSpawn(27152);
                }
            }
        } else if (n == 20234) {
            if (qs.getQuestItemsCount(3674) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(100) < 60) {
                    qs.giveItems(3851, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 15) {
                    qs.giveItems(3443, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 2 && qs.getQuestItemsCount(3674) > 0L) {
                    qs.addSpawn(27152);
                }
            }
        } else if (n == 20251 || n == 20252) {
            if (qs.getQuestItemsCount(3673) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3850, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 14) {
                    qs.giveItems(3442, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 3 && qs.getQuestItemsCount(3673) > 0L) {
                    qs.addSpawn(27151);
                    qs.addSpawn(27151);
                }
            }
        } else if (n == 20253) {
            if (qs.getQuestItemsCount(3673) >= 1L) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(3850, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 15) {
                    qs.giveItems(3442, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                if (ThreadLocalRandom.current().nextInt(100) < 3 && qs.getQuestItemsCount(3673) > 0L) {
                    qs.addSpawn(27151);
                    qs.addSpawn(27151);
                }
            }
        } else if (n == 27151) {
            if (qs.getQuestItemsCount(3673) >= 1L) {
                qs.giveItems(3850, 4L);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 27152 && qs.getQuestItemsCount(3674) >= 1L) {
            qs.giveItems(3851, 8L);
            qs.playSound("QuestState.SOUND_ITEMGET");
        }
        return null;
    
	}

}
