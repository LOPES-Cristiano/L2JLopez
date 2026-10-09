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
 * Quest 334 - 334_TheWishingPotion
 */
@Component
public class Quest334TheWishingPotion extends Quest {

	public static final int bbY = 30738;
	public static final int bjj = 30742;
	public static final int bjk = 30557;
	public static final int bjl = 30743;
	public static final int bjm = 20078;
	public static final int bjn = 20087;
	public static final int bjo = 20088;
	public static final int bjp = 20168;
	public static final int bjq = 20192;
	public static final int bjr = 20193;
	public static final int bjs = 20199;
	public static final int bjt = 20227;
	public static final int bju = 20248;
	public static final int bjv = 20249;
	public static final int bjw = 20250;
	public static final int bjx = 27135;
	public static final int bjy = 27136;
	public static final int bjz = 27138;
	public static final int bjA = 27139;
	public static final int bjB = 27153;
	public static final int bjC = 27154;
	public static final int bjD = 27155;
	public static final int bjE = 3684;
	public static final int bjF = 3685;
	public static final int bjG = 3686;
	public static final int bjH = 3687;
	public static final int bjI = 3688;
	public static final int bjJ = 3689;
	public static final int bjK = 3690;
	public static final int bjL = 3691;
	public static final int bjM = 931;
	public static final int bjN = 3467;
	public static final int bjO = 3468;
	public static final int bjP = 3469;
	public static final int bjQ = 3678;
	public static final int bjR = 3679;
	public static final int bjS = 3680;
	public static final int bjT = 3681;
	public static final int bjU = 3682;
	public static final int bjV = 3683;
	public static final int bjW = 1979;
	public static final int bjX = 1980;
	public static final int bjY = 2952;
	public static final int bjZ = 2953;
	public static final int bka = 4408;
	public static final int bkb = 4409;
	public static final int[] bkc = new int[]{3081, 3076, 3075, 3074, 4917, 3077, 3080, 3079, 3078, 4928, 4931, 4932, 5013, 3067, 3064, 3061, 3062, 3058, 4206, 3065, 3060, 3063, 4208, 3057, 3059, 3066, 4911, 4918, 3092, 3039, 4922, 3091, 3093, 3431};
	public static final int[] bkd = new int[]{3430, 3429, 3073, 3941, 3071, 3069, 3072, 4200, 3068, 3070, 4912, 3100, 3101, 3098, 3094, 3102, 4913, 3095, 3096, 3097, 3099, 3085, 3086, 3082, 4907, 3088, 4207, 3087, 3084, 3083, 4929, 4933, 4919, 3045};
	public static final int[] bke = new int[]{4923, 4201, 4914, 3942, 3090, 4909, 3089, 4930, 4934, 4920, 3041, 4924, 3114, 3105, 3110, 3104, 3113, 3103, 4204, 3108, 4926, 3112, 3107, 4205, 3109, 3111, 3106, 4925, 3117, 3115, 3118, 3116, 4927};
	public static int bkf = 0;
	public static int bkg = 0;
	public static int aFa = 0;
	public static int bkh = 0;
	public static int bki = 0;

	public Quest334TheWishingPotion(QuestManager questManager) {
		super(334, "334_TheWishingPotion", "334_TheWishingPotion");
		addStartNpc(30738);
		addTalkNpc(30743);
		addTalkNpc(30742);
		addTalkNpc(30557);
		addKillId(20248);
		addKillId(20249);
		addKillId(27136);
		addKillId(20087);
		addKillId(27138);
		addKillId(27155);
		addKillId(20078);
		addKillId(27153);
		addKillId(20193);
		addKillId(20250);
		addKillId(27139);
		addKillId(20168);
		addKillId(27154);
		addKillId(20088);
		addKillId(20199);
		addKillId(27135);
		addKillId(20192);
		addKillId(20227);
		registerQuestItems(3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691);
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
		NpcInstance npc = null;

        String string2 = event;
        NpcInstance npcInstance2 = null;
        int n = getFirstStartNpc();
        if (n == 30738) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("wish_potion", String.valueOf(1), true);
                if (qs.getQuestItemsCount(3678) == 0L) {
                    qs.giveItems(3678, 1L);
                }
                qs.setState(State.STARTED);
                qs.playSound("QuestState.SOUND_ACCEPT");
                string2 = "alchemist_matild_q0334_04.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "alchemist_matild_q0334_03.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                qs.setCond(3);
                qs.set("wish_potion", String.valueOf(2), true);
                qs.takeItems(3679, -1L);
                qs.takeItems(3678, -1L);
                qs.giveItems(3680, 1L);
                qs.giveItems(3681, 1L);
                qs.playSound("QuestState.SOUND_MIDDLE");
                string2 = "alchemist_matild_q0334_07.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = "alchemist_matild_q0334_10.htm";
            } else if (event.equalsIgnoreCase("reply_4") && qs.getQuestItemsCount(3684) > 0L && qs.getQuestItemsCount(3686) > 0L && qs.getQuestItemsCount(3687) > 0L && qs.getQuestItemsCount(3688) > 0L && qs.getQuestItemsCount(3689) > 0L && qs.getQuestItemsCount(3690) > 0L && qs.getQuestItemsCount(3691) > 0L && qs.getQuestItemsCount(3685) > 0L && qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L) {
                qs.setCond(5);
                qs.set("wish_potion", String.valueOf(2), true);
                qs.giveItems(3467, 1L);
                if (qs.getQuestItemsCount(3682) == 0L) {
                    qs.giveItems(3682, 1L);
                }
                qs.takeItems(3684, 1L);
                qs.takeItems(3686, 1L);
                qs.takeItems(3687, 1L);
                qs.takeItems(3688, 1L);
                qs.takeItems(3689, 1L);
                qs.takeItems(3690, 1L);
                qs.takeItems(3691, 1L);
                qs.takeItems(3685, 1L);
                qs.takeItems(3680, -1L);
                qs.takeItems(3681, -1L);
                qs.playSound("QuestState.SOUND_ITEMGET");
                string2 = "alchemist_matild_q0334_11.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                string2 = qs.getQuestItemsCount(3467) > 0L ? "alchemist_matild_q0334_13.htm" : "alchemist_matild_q0334_14.htm";
            } else if (event.equalsIgnoreCase("reply_6")) {
                if (qs.getQuestItemsCount(3467) > 0L) {
                    string2 = "alchemist_matild_q0334_15a.htm";
                } else {
                    string2 = "alchemist_matild_q0334_15.htm";
                    qs.giveItems(3680, 1L);
                    qs.giveItems(3681, 1L);
                }
            } else if (event.equalsIgnoreCase("reply_7")) {
                if (qs.getQuestItemsCount(3467) > 0L) {
                    if (this.bkf == 0) {
                        string2 = "alchemist_matild_q0334_16.htm";
                        qs.takeItems(3467, 1L);
                        this.aFa = 1;
                        this.bkg = 1;
                        qs.startQuestTimer("2336008", 3000L, npcInstance2);
                    } else {
                        string2 = "alchemist_matild_q0334_20.htm";
                    }
                } else {
                    string2 = "alchemist_matild_q0334_14.htm";
                }
            } else if (event.equalsIgnoreCase("reply_8")) {
                if (qs.getQuestItemsCount(3467) > 0L) {
                    if (this.bkf == 0) {
                        string2 = "alchemist_matild_q0334_17.htm";
                        qs.takeItems(3467, 1L);
                        this.aFa = 2;
                        this.bkg = 2;
                        qs.startQuestTimer("2336008", 3000L, npcInstance2);
                    } else {
                        string2 = "alchemist_matild_q0334_20.htm";
                    }
                } else {
                    string2 = "alchemist_matild_q0334_14.htm";
                }
            } else if (event.equalsIgnoreCase("reply_9")) {
                if (qs.getQuestItemsCount(3467) > 0L) {
                    if (this.bkf == 0) {
                        string2 = "alchemist_matild_q0334_18.htm";
                        qs.takeItems(3467, 1L);
                        this.aFa = 3;
                        this.bkg = 3;
                        qs.startQuestTimer("2336008", 3000L, npcInstance2);
                    } else {
                        string2 = "alchemist_matild_q0334_20.htm";
                    }
                } else {
                    string2 = "alchemist_matild_q0334_14.htm";
                }
            } else if (event.equalsIgnoreCase("reply_10")) {
                if (qs.getQuestItemsCount(3467) > 0L) {
                    if (this.bkf == 0) {
                        string2 = "alchemist_matild_q0334_18.htm";
                        qs.takeItems(3467, 1L);
                        this.aFa = 4;
                        this.bkg = 4;
                        qs.startQuestTimer("2336008", 3000L, npcInstance2);
                    } else {
                        string2 = "alchemist_matild_q0334_20.htm";
                    }
                } else {
                    string2 = "alchemist_matild_q0334_14.htm";
                }
            } else {
                if (event.equalsIgnoreCase("2336008")) {
                    // npcSayCustomMessage
                    qs.startQuestTimer("2336009", 4000L, npcInstance2);
                    return null;
                }
                if (event.equalsIgnoreCase("2336009")) {
                    // npcSayCustomMessage
                    qs.startQuestTimer("2336010", 4000L, npcInstance2);
                    return null;
                }
                if (event.equalsIgnoreCase("2336010")) {
                    int n2 = 0;
                    // npcSayCustomMessage
                    if (this.aFa == 1) {
                        n2 = ThreadLocalRandom.current().nextInt(2);
                    } else if (this.aFa == 3 || this.aFa == 4 || this.aFa == 2) {
                        n2 = ThreadLocalRandom.current().nextInt(3);
                    }
                    switch (n2) {
                        case 0: {
                            if (this.aFa == 1) {
                                NpcInstance npcInstance3 = qs.addSpawn(30742);
                                // npcSayCustomMessage
                                qs.startQuestTimer("2336001", 120000L, npcInstance3);
                                this.bkf = 1;
                                break;
                            }
                            if (this.aFa == 2) {
                                NpcInstance npcInstance4 = qs.addSpawn(27135, 120000);
                                NpcInstance npcInstance5 = qs.addSpawn(27135, 120000);
                                NpcInstance npcInstance6 = qs.addSpawn(27135, 120000);
                                // npcSayCustomMessage
                                // npcSayCustomMessage
                                // npcSayCustomMessage
                                qs.startQuestTimer("2336002", 120000L, npcInstance2);
                                this.bkf = 1;
                                break;
                            }
                            if (this.aFa == 3) {
                                qs.playSound("QuestState.SOUND_ITEMGET");
                                qs.giveItems(3469, 1L);
                                break;
                            }
                            if (this.aFa != 4) break;
                            NpcInstance npcInstance7 = qs.addSpawn(30743);
                            // npcSayCustomMessage
                            qs.startQuestTimer("2336007", 120000L, npcInstance7);
                            this.bkf = 1;
                            break;
                        }
                        case 1: {
                            if (this.aFa == 1) {
                                NpcInstance npcInstance8 = qs.addSpawn(27136, 200000);
                                NpcInstance npcInstance9 = qs.addSpawn(27136, 200000);
                                NpcInstance npcInstance10 = qs.addSpawn(27136, 200000);
                                // npcSayCustomMessage
                                // npcSayCustomMessage
                                // npcSayCustomMessage
                                qs.startQuestTimer("2336003", 200000L, npcInstance2);
                                this.bkf = 1;
                                break;
                            }
                            if (this.aFa == 2) {
                                qs.playSound("QuestState.SOUND_ITEMGET");
                                qs.giveItems(57, 10000L);
                                break;
                            }
                            if (this.aFa == 3) {
                                NpcInstance npcInstance11 = qs.addSpawn(27153);
                                // npcSayCustomMessage
                                qs.startQuestTimer("2336004", 200000L, npcInstance11);
                                this.bkf = 1;
                                break;
                            }
                            if (this.aFa != 4) break;
                            NpcInstance npcInstance12 = qs.addSpawn(30743);
                            // npcSayCustomMessage
                            qs.startQuestTimer("2336007", 120000L, npcInstance12);
                            this.bkf = 1;
                            break;
                        }
                        case 2: {
                            if (this.aFa == 2) {
                                qs.playSound("QuestState.SOUND_ITEMGET");
                                qs.giveItems(57, 10000L);
                                break;
                            }
                            if (this.aFa == 3) {
                                qs.playSound("QuestState.SOUND_ITEMGET");
                                qs.giveItems(3468, 1L);
                                break;
                            }
                            if (this.aFa != 4) break;
                            NpcInstance npcInstance13 = qs.addSpawn(30743);
                            // npcSayCustomMessage
                            qs.startQuestTimer("2336007", 120000L, npcInstance13);
                            this.bkf = 1;
                        }
                    }
                    return null;
                }
                if (event.equalsIgnoreCase("2336003")) {
                    this.bkf = 0;
                    return null;
                }
                if (event.equalsIgnoreCase("2336002")) {
                    this.bkf = 0;
                    return null;
                }
            }
        } else {
            if (event.equalsIgnoreCase("2336001")) {
                if (npc != null) {
                    this.bkf = 0;
                }
                npc.deleteMe();
                return null;
            }
            if (event.equalsIgnoreCase("2336007")) {
                if (npc != null) {
                    this.bkf = 0;
                }
                npc.deleteMe();
                return null;
            }
            if (event.equalsIgnoreCase("2336004")) {
                if (npc != null) {
                    this.bkf = 0;
                }
                npc.deleteMe();
                return null;
            }
            if (event.equalsIgnoreCase("2336107")) {
                if (npc != null) {
                    // npcSayCustomMessage
                    // npcSayCustomMessage
                }
                this.bkf = 0;
                npc.deleteMe();
                return null;
            }
            if (event.equalsIgnoreCase("2336005")) {
                if (npc != null) {
                    // npcSayCustomMessage
                }
                this.bkf = 0;
                npc.deleteMe();
                return null;
            }
            if (event.equalsIgnoreCase("2336006")) {
                if (npc != null) {
                    // npcSayCustomMessage
                }
                this.bkf = 0;
                npc.deleteMe();
                return null;
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
                if (n != 30738) break;
                if (pc.getLevel() < 30) {
                    html = "alchemist_matild_q0334_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "alchemist_matild_q0334_02.htm";
                break;
            }
            case 2: {
                if (n == 30738) {
                    if (qs.getQuestItemsCount(3679) == 0L && qs.getQuestItemsCount(3678) == 1L) {
                        html = "alchemist_matild_q0334_05.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3679) == 1L && qs.getQuestItemsCount(3678) == 1L) {
                        html = "alchemist_matild_q0334_06.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3680) == 1L && qs.getQuestItemsCount(3681) == 1L && (qs.getQuestItemsCount(3684) == 0L || qs.getQuestItemsCount(3685) > 0L && qs.getQuestItemsCount(3686) == 0L || qs.getQuestItemsCount(3687) == 0L || qs.getQuestItemsCount(3688) == 0L || qs.getQuestItemsCount(3689) == 0L || qs.getQuestItemsCount(3690) == 0L || qs.getQuestItemsCount(3691) == 0L)) {
                        html = "alchemist_matild_q0334_08.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3680) == 1L && qs.getQuestItemsCount(3681) == 1L && qs.getQuestItemsCount(3684) > 0L && qs.getQuestItemsCount(3685) > 0L && qs.getQuestItemsCount(3686) > 0L && qs.getQuestItemsCount(3687) > 0L && qs.getQuestItemsCount(3688) > 0L && qs.getQuestItemsCount(3689) > 0L && qs.getQuestItemsCount(3690) > 0L && qs.getQuestItemsCount(3691) > 0L) {
                        html = "alchemist_matild_q0334_09.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(3682) != 1L || qs.getQuestItemsCount(3680) != 0L || qs.getQuestItemsCount(3681) != 0L || qs.getQuestItemsCount(3684) != 0L && (qs.getQuestItemsCount(3685) <= 0L || qs.getQuestItemsCount(3686) != 0L) && qs.getQuestItemsCount(3687) != 0L && qs.getQuestItemsCount(3688) != 0L && qs.getQuestItemsCount(3689) != 0L && qs.getQuestItemsCount(3690) != 0L && qs.getQuestItemsCount(3691) != 0L) break;
                    html = "alchemist_matild_q0334_12.htm";
                    break;
                }
                if (n == 30742) {
                    if (this.bkg != 1) break;
                    if (ThreadLocalRandom.current().nextInt(100) < 4) {
                        html = "fairy_rupina_q0334_01.htm";
                        qs.giveItems(931, 1L);
                        this.bkg = 0;
                        if (qs.isRunningQuestTimer("2336001")) {
                            qs.cancelQuestTimer("2336001");
                        }
                        this.bkf = 0;
                        npc.deleteMe();
                        break;
                    }
                    html = "fairy_rupina_q0334_02.htm";
                    int n3 = ThreadLocalRandom.current().nextInt(4);
                    if (n3 == 0) {
                        qs.giveItems(1979, 1L);
                    } else if (n3 == 1) {
                        qs.giveItems(1980, 1L);
                    } else if (n3 == 2) {
                        qs.giveItems(2952, 1L);
                    } else if (n3 == 3) {
                        qs.giveItems(2953, 1L);
                    }
                    this.bkg = 0;
                    if (qs.isRunningQuestTimer("2336001")) {
                        qs.cancelQuestTimer("2336001");
                    }
                    this.bkf = 0;
                    npc.deleteMe();
                    break;
                }
                if (n == 30557) {
                    if (qs.getQuestItemsCount(3683) < 1L) break;
                    qs.giveItems(57, 500000L);
                    qs.takeItems(3683, 1L);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                    html = "torai_q0334_01.htm";
                    break;
                }
                if (n != 30743 || this.bkg != 4) break;
                int n4 = ThreadLocalRandom.current().nextInt(10);
                if (n4 < 2) {
                    html = "wisdom_chest_q0334_01.htm";
                } else if (n4 >= 2 && n4 < 4) {
                    html = "wisdom_chest_q0334_02.htm";
                } else if (n4 >= 4 && n4 < 6) {
                    html = "wisdom_chest_q0334_03.htm";
                } else if (n4 == 6) {
                    html = "wisdom_chest_q0334_04.htm";
                } else if (n4 >= 7 && n4 < 9) {
                    html = "wisdom_chest_q0334_05.htm";
                } else if (n4 == 9) {
                    html = "wisdom_chest_q0334_06.htm";
                }
                int n5 = bkc[ThreadLocalRandom.current().nextInt(bkc.length)];
                qs.giveItems(n5, 1L);
                int n6 = bkd[ThreadLocalRandom.current().nextInt(bkd.length)];
                qs.giveItems(n6, 1L);
                int n7 = bke[ThreadLocalRandom.current().nextInt(bke.length)];
                qs.giveItems(n7, 1L);
                if (ThreadLocalRandom.current().nextInt(3) == 0) {
                    qs.giveItems(3943, 1L);
                }
                qs.giveItems(4408, 1L);
                qs.giveItems(4409, 1L);
                this.bkg = 0;
                this.bkf = 0;
                if (qs.isRunningQuestTimer("2336007")) {
                    qs.cancelQuestTimer("2336007");
                }
                npc.deleteMe();
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("wish_potion");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 20078) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3685) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3685, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20087 || n2 == 20088) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3689) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3689, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20168) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3688) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3688, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20192 || n2 == 20193) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3690) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3690, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20199) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3684) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3684, 1L);
                if (qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20227) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3687) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3687, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20248 || n2 == 20249) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3691) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3691, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3686) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 20250) {
            if (qs.getQuestItemsCount(3680) > 0L && qs.getQuestItemsCount(3681) > 0L && qs.getQuestItemsCount(3686) == 0L && ThreadLocalRandom.current().nextInt(10) == 0) {
                qs.giveItems(3686, 1L);
                if (qs.getQuestItemsCount(3684) >= 1L && qs.getQuestItemsCount(3685) >= 1L && qs.getQuestItemsCount(3687) >= 1L && qs.getQuestItemsCount(3688) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3689) >= 1L && qs.getQuestItemsCount(3690) >= 1L && qs.getQuestItemsCount(3691) >= 1L) {
                    qs.setCond(4);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                } else {
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
            }
        } else if (n2 == 27135) {
            if (n == 2 && this.bkg == 2) {
                if (ThreadLocalRandom.current().nextInt(1000) < 33) {
                    int n3 = ThreadLocalRandom.current().nextInt(1000);
                    if (n3 == 0) {
                        qs.giveItems(57, 100000000L);
                    } else {
                        qs.giveItems(57, 900000L);
                    }
                    qs.playSound("QuestState.SOUND_ITEMGET");
                }
                ++this.bki;
                if (this.bki == 3) {
                    this.bkg = 0;
                    this.bkf = 0;
                    this.bki = 0;
                    if (qs.isRunningQuestTimer("2336002")) {
                        qs.cancelQuestTimer("2336002");
                    }
                }
            }
        } else if (n2 == 27136) {
            if (n == 2 && this.bkg == 1 && qs.getQuestItemsCount(3683) == 0L) {
                if (ThreadLocalRandom.current().nextInt(1000) < 28) {
                    qs.giveItems(3683, 1L);
                    qs.playSound("QuestState.SOUND_ITEMGET");
                    this.bkg = 0;
                    this.bkf = 0;
                    this.bkh = 0;
                    if (qs.isRunningQuestTimer("2336003")) {
                        qs.cancelQuestTimer("2336003");
                    }
                }
                ++this.bkh;
                if (this.bkh == 3) {
                    this.bkg = 0;
                    this.bkf = 0;
                    this.bkh = 0;
                    if (qs.isRunningQuestTimer("2336003")) {
                        qs.cancelQuestTimer("2336003");
                    }
                }
            }
        } else if (n2 == 27138) {
            if (n == 2 && this.bkg == 3) {
                qs.giveItems(57, 1406956L);
                qs.playSound("QuestState.SOUND_ITEMGET");
                this.bkg = 0;
                this.bkf = 0;
                if (qs.isRunningQuestTimer("2336107")) {
                    qs.cancelQuestTimer("2336107");
                }
            }
        } else if (n2 == 27139) {
            if (n == 1 && qs.getQuestItemsCount(3679) == 0L) {
                qs.setCond(2);
                qs.giveItems(3679, 1L);
                qs.playSound("QuestState.SOUND_MIDDLE");
            }
        } else if (n2 == 27153) {
            if (n == 2 && this.bkg == 3) {
                // npcSayCustomMessage
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    NpcInstance npcInstance2 = qs.addSpawn(27154);
                    // npcSayCustomMessage
                    qs.startQuestTimer("2336005", 200000L, npcInstance2);
                } else {
                    int n4 = ThreadLocalRandom.current().nextInt(4);
                    if (n4 == 0) {
                        qs.giveItems(1979, 1L);
                    } else if (n4 == 1) {
                        qs.giveItems(1980, 1L);
                    } else if (n4 == 2) {
                        qs.giveItems(2952, 1L);
                    } else if (n4 == 3) {
                        qs.giveItems(2953, 1L);
                    }
                    this.bkf = 0;
                    this.bkg = 0;
                    if (qs.isRunningQuestTimer("2336004")) {
                        qs.cancelQuestTimer("2336004");
                    }
                }
            }
        } else if (n2 == 27154) {
            if (n == 2 && this.bkg == 3) {
                // npcSayCustomMessage
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    NpcInstance npcInstance3 = qs.addSpawn(27155);
                    // npcSayCustomMessage
                    qs.startQuestTimer("2336006", 200000L, npcInstance3);
                } else {
                    int n5 = ThreadLocalRandom.current().nextInt(4);
                    if (n5 == 0) {
                        qs.giveItems(1979, 1L);
                    } else if (n5 == 1) {
                        qs.giveItems(1980, 1L);
                    } else if (n5 == 2) {
                        qs.giveItems(2952, 1L);
                    } else if (n5 == 3) {
                        qs.giveItems(2953, 1L);
                    }
                    this.bkf = 0;
                    this.bkg = 0;
                    if (qs.isRunningQuestTimer("2336005")) {
                        qs.cancelQuestTimer("2336005");
                    }
                }
            }
        } else if (n2 == 27155 && n == 2 && this.bkg == 3) {
            // npcSayCustomMessage
            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                NpcInstance npcInstance4 = qs.addSpawn(27138);
                // npcSayCustomMessage
                qs.startQuestTimer("2336107", 600000L, npcInstance4);
            } else {
                int n6 = ThreadLocalRandom.current().nextInt(4);
                if (n6 == 0) {
                    qs.giveItems(1979, 1L);
                } else if (n6 == 1) {
                    qs.giveItems(1980, 1L);
                } else if (n6 == 2) {
                    qs.giveItems(2952, 1L);
                } else if (n6 == 3) {
                    qs.giveItems(2953, 1L);
                }
                this.bkf = 0;
                this.bkg = 0;
                if (qs.isRunningQuestTimer("2336006")) {
                    qs.cancelQuestTimer("2336006");
                }
            }
        }
        return null;
    
	}

}
