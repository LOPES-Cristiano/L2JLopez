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
 * Quest 663 - 663_SeductiveWhispers
 */
@Component
public class Quest663SeductiveWhispers extends Quest {

	public static final int bVb = 30846;
	public static final int bUw = 20674;
	public static final int bVc = 20678;
	public static final int bVd = 20954;
	public static final int bUy = 20955;
	public static final int bVe = 20956;
	public static final int bVf = 20957;
	public static final int bUz = 20958;
	public static final int bUA = 20959;
	public static final int bVg = 20960;
	public static final int bUB = 20961;
	public static final int bUC = 20962;
	public static final int bVh = 20963;
	public static final int bVi = 20974;
	public static final int bVj = 20975;
	public static final int bVk = 20976;
	public static final int bVl = 20996;
	public static final int bVm = 20997;
	public static final int bVn = 20998;
	public static final int bVo = 20999;
	public static final int bVp = 21000;
	public static final int bVq = 21001;
	public static final int bUH = 21002;
	public static final int bUJ = 21006;
	public static final int bVr = 21007;
	public static final int bUK = 21008;
	public static final int bVs = 21009;
	public static final int bUL = 21010;
	public static final int bVt = 8766;
	public static final int bUo = 955;
	public static final int bUY = 951;
	public static final int bUX = 947;
	public static final int bVu = 948;
	public static final int aGI = 729;
	public static final int bVv = 730;
	public static final int bVw = 4963;
	public static final int bVx = 4964;
	public static final int bVy = 4965;
	public static final int bVz = 4966;
	public static final int bVA = 4967;
	public static final int bVB = 4968;
	public static final int bVC = 4969;
	public static final int bVD = 4970;
	public static final int bVE = 4971;
	public static final int bVF = 4972;
	public static final int bVG = 5000;
	public static final int bVH = 5001;
	public static final int bVI = 5002;
	public static final int bVJ = 5003;
	public static final int bVK = 5004;
	public static final int bVL = 5005;
	public static final int bVM = 5006;
	public static final int bVN = 5007;
	public static final int boL = 4104;
	public static final int boD = 4105;
	public static final int boA = 4106;
	public static final int box = 4107;
	public static final int boN = 4108;
	public static final int boq = 4109;
	public static final int boK = 4110;
	public static final int bou = 4111;
	public static final int boz = 4112;
	public static final int boF = 4113;
	public static final int boy = 4114;
	public static final int boH = 4115;
	public static final int bVO = 4116;
	public static final int boE = 4117;
	public static final int boI = 4118;
	public static final int bos = 4119;
	public static final int bov = 4120;
	public static final int boB = 4121;

	public Quest663SeductiveWhispers(QuestManager questManager) {
	super(663, "663_SeductiveWhispers", "663_SeductiveWhispers");
		this.addStartNpc(30846);
		this.addKillId(20674, 20678, 20954, 20955, 20956, 20957, 20958, 20959, 20960, 20961, 20962, 20963, 20974, 20975, 20976, 20996, 20997, 20998, 20999, 21000, 21001, 21002, 21006, 21007, 21008, 21009, 21010);
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

        String string2 = "no-quest";
        int n = qs.getInt("whispers_of_temptation");
        int n2 = qs.getInt("whispers_of_temptation_ex");
        int n3 = 30846;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        if (n3 == 30846) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("whispers_of_temptation", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "blacksmith_wilbert_q0663_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "blacksmith_wilbert_q0663_01a.htm";
            } else if (event.equalsIgnoreCase("reply_4") && n % 10 <= 4) {
                if (n / 10 < 1) {
                    if (qs.getQuestItemsCount(8766) >= 50L) {
                        qs.takeItems(8766, 50L);
                        qs.set("whispers_of_temptation", String.valueOf(5), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                        string2 = "blacksmith_wilbert_q0663_09.htm";
                    } else {
                        string2 = "blacksmith_wilbert_q0663_10.htm";
                    }
                } else {
                    n4 = n / 10;
                    n5 = n4 * 10 + 5;
                    qs.set("whispers_of_temptation", String.valueOf(n5), true);
                    qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                    string2 = "blacksmith_wilbert_q0663_09a.htm";
                }
            } else if (event.equalsIgnoreCase("reply_5") && n % 10 == 5 && n / 1000 == 0) {
                n4 = n2;
                if (n4 < 0) {
                    n4 = 0;
                }
                n5 = n4 % 10;
                n6 = (n4 - n5) / 10;
                int n10 = ThreadLocalRandom.current().nextInt(2) + 1;
                int n11 = ThreadLocalRandom.current().nextInt(5) + 1;
                n9 = n / 10;
                int n12 = n10 * 10 + n11;
                if (n10 == n6) {
                    n7 = n11 + n5;
                    if (n7 % 5 == 0 && n7 != 10) {
                        if (n % 100 / 10 >= 7) {
                            string2 = "blacksmith_wilbert_q0663_14.htm";
                            string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                            string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                            string2 = string2.replace("<?name?>", pc.getName());
                            qs.set("whispers_of_temptation", String.valueOf(4), true);
                            qs.giveItems(57, 2384000L);
                            qs.giveItems(729, 1L);
                            qs.giveItems(730, 2L);
                        } else {
                            string2 = "blacksmith_wilbert_q0663_13.htm";
                            string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                            string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                            string2 = string2.replace("<?name?>", pc.getName());
                            string2 = string2.replace("<?wincount?>", String.valueOf(n9 + 1));
                            n8 = n / 10 * 10 + 7;
                            qs.set("whispers_of_temptation", String.valueOf(n8), true);
                        }
                    } else {
                        string2 = "blacksmith_wilbert_q0663_12.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation_ex", String.valueOf(n12), true);
                        n8 = n / 10 * 10 + 6;
                        qs.set("whispers_of_temptation", String.valueOf(n8), true);
                    }
                } else if (n10 != n6) {
                    if (n11 == 5 || n5 == 5) {
                        if (n % 100 / 10 >= 7) {
                            string2 = "blacksmith_wilbert_q0663_14.htm";
                            string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                            string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                            string2 = string2.replace("<?name?>", pc.getName());
                            qs.giveItems(57, 2384000L);
                            qs.giveItems(729, 1L);
                            qs.giveItems(730, 2L);
                            qs.set("whispers_of_temptation", String.valueOf(4), true);
                        } else {
                            string2 = "blacksmith_wilbert_q0663_13.htm";
                            string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                            string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                            string2 = string2.replace("<?name?>", pc.getName());
                            string2 = string2.replace("<?wincount?>", String.valueOf(n9 + 1));
                            n8 = n / 10 * 10 + 7;
                            qs.set("whispers_of_temptation", String.valueOf(n8), true);
                        }
                    } else {
                        string2 = "blacksmith_wilbert_q0663_12.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n12));
                        string2 = string2.replace("<?name?>", pc.getName());
                        n12 = n10 * 10 + n11;
                        qs.set("whispers_of_temptation_ex", String.valueOf(n12), true);
                        n8 = n / 10 * 10 + 6;
                        qs.set("whispers_of_temptation", String.valueOf(n8), true);
                    }
                }
            } else if (event.equalsIgnoreCase("reply_6") && n % 10 == 6 && n / 1000 == 0) {
                n4 = n2;
                if (n4 < 0) {
                    n4 = 0;
                }
                n5 = n4 % 10;
                n6 = (n4 - n5) / 10;
                int n13 = ThreadLocalRandom.current().nextInt(2) + 1;
                int n14 = ThreadLocalRandom.current().nextInt(5) + 1;
                int n15 = n13 * 10 + n14;
                if (n13 == n6) {
                    n7 = n14 + n5;
                    if (n7 % 5 == 0 && n7 != 10) {
                        string2 = "blacksmith_wilbert_q0663_19.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n15));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_18.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n15));
                        string2 = string2.replace("<?name?>", pc.getName());
                        n15 = n13 * 10 + n14;
                        qs.set("whispers_of_temptation_ex", String.valueOf(n15), true);
                        n8 = n / 10 * 10 + 5;
                        qs.set("whispers_of_temptation", String.valueOf(n8), true);
                    }
                } else if (n13 != n6) {
                    n7 = n13 + n5;
                    n8 = 66310 + n6;
                    n9 = 66310 + n13;
                    if (n14 == 5 || n5 == 5) {
                        string2 = "blacksmith_wilbert_q0663_19.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n15));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_18.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n15));
                        string2 = string2.replace("<?name?>", pc.getName());
                        n15 = n13 * 10 + n14;
                        qs.set("whispers_of_temptation_ex", String.valueOf(n15), true);
                        n8 = n / 10 * 10 + 5;
                        qs.set("whispers_of_temptation", String.valueOf(n8), true);
                    }
                }
            } else if (event.equalsIgnoreCase("reply_8") && n % 10 == 7 && n / 1000 == 0) {
                n4 = n / 10;
                n5 = (n4 + 1) * 10 + 4;
                qs.set("whispers_of_temptation", String.valueOf(n5), true);
                qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                string2 = "blacksmith_wilbert_q0663_20.htm";
            } else if (event.equalsIgnoreCase("reply_9") && n % 10 == 7 && n / 1000 == 0) {
                n4 = n / 10;
                if (n4 == 0) {
                    qs.giveItems(57, 40000L);
                } else if (n4 == 1) {
                    qs.giveItems(57, 80000L);
                } else if (n4 == 2) {
                    qs.giveItems(57, 110000L);
                    qs.giveItems(955, 1L);
                } else if (n4 == 3) {
                    qs.giveItems(57, 199000L);
                    qs.giveItems(951, 1L);
                } else if (n4 == 4) {
                    qs.giveItems(57, 388000L);
                    n5 = ThreadLocalRandom.current().nextInt(18) + 1;
                    if (n5 == 1) {
                        qs.giveItems(4963, 1L);
                    } else if (n5 == 2) {
                        qs.giveItems(4964, 1L);
                    } else if (n5 == 3) {
                        qs.giveItems(4965, 1L);
                    } else if (n5 == 4) {
                        qs.giveItems(4966, 1L);
                    } else if (n5 == 5) {
                        qs.giveItems(4967, 1L);
                    } else if (n5 == 6) {
                        qs.giveItems(4968, 1L);
                    } else if (n5 == 7) {
                        qs.giveItems(4969, 1L);
                    } else if (n5 == 8) {
                        qs.giveItems(4970, 1L);
                    } else if (n5 == 9) {
                        qs.giveItems(4971, 1L);
                    } else if (n5 == 10) {
                        qs.giveItems(4972, 1L);
                    } else if (n5 == 11) {
                        qs.giveItems(5000, 1L);
                    } else if (n5 == 12) {
                        qs.giveItems(5001, 1L);
                    } else if (n5 == 13) {
                        qs.giveItems(5002, 1L);
                    } else if (n5 == 14) {
                        qs.giveItems(5003, 1L);
                    } else if (n5 == 15) {
                        qs.giveItems(5004, 1L);
                    } else if (n5 == 16) {
                        qs.giveItems(5005, 1L);
                    } else if (n5 == 17) {
                        qs.giveItems(5006, 1L);
                    } else if (n5 == 18) {
                        qs.giveItems(5007, 1L);
                    }
                } else if (n4 == 5) {
                    qs.giveItems(57, 675000L);
                    n5 = ThreadLocalRandom.current().nextInt(18) + 1;
                    if (n5 == 1) {
                        qs.giveItems(4104, 12L);
                    } else if (n5 == 2) {
                        qs.giveItems(4113, 12L);
                    } else if (n5 == 3) {
                        qs.giveItems(4112, 12L);
                    } else if (n5 == 4) {
                        qs.giveItems(4108, 12L);
                    } else if (n5 == 5) {
                        qs.giveItems(4111, 12L);
                    } else if (n5 == 6) {
                        qs.giveItems(4106, 12L);
                    } else if (n5 == 7) {
                        qs.giveItems(4109, 12L);
                    } else if (n5 == 8) {
                        qs.giveItems(4107, 12L);
                    } else if (n5 == 9) {
                        qs.giveItems(4105, 12L);
                    } else if (n5 == 10) {
                        qs.giveItems(4110, 12L);
                    } else if (n5 == 11) {
                        qs.giveItems(4114, 13L);
                    } else if (n5 == 12) {
                        qs.giveItems(4115, 13L);
                    } else if (n5 == 13) {
                        qs.giveItems(4120, 13L);
                    } else if (n5 == 14) {
                        qs.giveItems(4118, 13L);
                    } else if (n5 == 15) {
                        qs.giveItems(4116, 13L);
                    } else if (n5 == 16) {
                        qs.giveItems(4117, 13L);
                    } else if (n5 == 17) {
                        qs.giveItems(4119, 13L);
                    } else if (n5 == 18) {
                        qs.giveItems(4121, 13L);
                    }
                } else if (n4 == 6) {
                    qs.giveItems(57, 1284000L);
                    qs.giveItems(947, 2L);
                    qs.giveItems(948, 2L);
                }
                qs.set("whispers_of_temptation", String.valueOf(1), true);
                qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                string2 = "blacksmith_wilbert_q0663_21.htm";
            } else if (event.equalsIgnoreCase("reply_10") && n == 1 && n / 1000 == 0) {
                string2 = "blacksmith_wilbert_q0663_21a.htm";
            } else if (event.equalsIgnoreCase("reply_14") && n % 10 == 1) {
                if (qs.getQuestItemsCount(8766) >= 1L) {
                    qs.set("whispers_of_temptation", String.valueOf(1005), true);
                    qs.takeItems(8766, 1L);
                    string2 = "blacksmith_wilbert_q0663_22.htm";
                } else {
                    string2 = "blacksmith_wilbert_q0663_22a.htm";
                }
            } else if (event.equalsIgnoreCase("reply_15") && n == 1005) {
                n4 = n2;
                if (n4 < 0) {
                    n4 = 0;
                }
                n5 = n4 % 10;
                n6 = (n4 - n5) / 10;
                int n16 = ThreadLocalRandom.current().nextInt(2) + 1;
                int n17 = ThreadLocalRandom.current().nextInt(5) + 1;
                int n18 = n16 * 10 + n17;
                if (n16 == n6) {
                    n7 = n17 + n5;
                    n8 = 66310 + n6;
                    n9 = 66310 + n16;
                    if (n7 % 5 == 0 && n7 != 10) {
                        string2 = "blacksmith_wilbert_q0663_25.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n18));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                        qs.giveItems(57, 800L);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_24.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n18));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation_ex", String.valueOf(n18), true);
                        qs.set("whispers_of_temptation", String.valueOf(1006), true);
                    }
                } else if (n16 != n6) {
                    if (n17 == 5 || n5 == 5) {
                        string2 = "blacksmith_wilbert_q0663_25.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n18));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                        qs.giveItems(57, 800L);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_24.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n18));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation_ex", String.valueOf(n18), true);
                        qs.set("whispers_of_temptation", String.valueOf(1006), true);
                    }
                }
            } else if (event.equalsIgnoreCase("reply_16") && n == 1006) {
                n4 = n2;
                if (n4 < 0) {
                    n4 = 0;
                }
                n5 = n4 % 10;
                n6 = (n4 - n5) / 10;
                int n19 = ThreadLocalRandom.current().nextInt(2) + 1;
                int n20 = ThreadLocalRandom.current().nextInt(5) + 1;
                int n21 = n19 * 10 + n20;
                if (n19 == n6) {
                    n7 = n20 + n5;
                    if (n7 % 5 == 0 && n7 != 10) {
                        string2 = "blacksmith_wilbert_q0663_29.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n21));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_28.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n21));
                        string2 = string2.replace("<?name?>", pc.getName());
                        n21 = n19 * 10 + n20;
                        qs.set("whispers_of_temptation_ex", String.valueOf(n21), true);
                        qs.set("whispers_of_temptation", String.valueOf(1005), true);
                    }
                } else if (n19 != n6) {
                    n7 = n19 + n5;
                    n8 = 66310 + n6;
                    n9 = 66310 + n19;
                    if (n20 == 5 || n5 == 5) {
                        string2 = "blacksmith_wilbert_q0663_29.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n21));
                        string2 = string2.replace("<?name?>", pc.getName());
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.set("whispers_of_temptation_ex", String.valueOf(0), true);
                    } else {
                        string2 = "blacksmith_wilbert_q0663_28.htm";
                        string2 = string2.replace("<?card1pic?>", String.valueOf(n4));
                        string2 = string2.replace("<?card2pic?>", String.valueOf(n21));
                        string2 = string2.replace("<?name?>", pc.getName());
                        n21 = n19 * 10 + n20;
                        qs.set("whispers_of_temptation_ex", String.valueOf(n21), true);
                        qs.set("whispers_of_temptation", String.valueOf(1005), true);
                    }
                }
            } else if (event.equalsIgnoreCase("reply_20")) {
                qs.unset("whispers_of_temptation");
                qs.unset("whispers_of_temptation_ex");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "blacksmith_wilbert_q0663_30.htm";
            } else if (event.equalsIgnoreCase("reply_21")) {
                string2 = "blacksmith_wilbert_q0663_31.htm";
            } else if (event.equalsIgnoreCase("reply_22")) {
                string2 = "blacksmith_wilbert_q0663_32.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("whispers_of_temptation");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30846) break;
                if (pc.getLevel() < 50) {
                    html = "blacksmith_wilbert_q0663_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "blacksmith_wilbert_q0663_01.htm";
                break;
            }
            case 2: {
                if (n2 != 30846) break;
                if (n < 4 && n >= 1 && qs.getQuestItemsCount(8766) == 0L) {
                    html = "blacksmith_wilbert_q0663_04.htm";
                    break;
                }
                if (n < 4 && n >= 1 && qs.getQuestItemsCount(8766) > 0L) {
                    html = "blacksmith_wilbert_q0663_05.htm";
                    break;
                }
                if (n % 10 == 4 && n / 1000 == 0) {
                    html = "blacksmith_wilbert_q0663_05a.htm";
                    break;
                }
                if (n % 10 == 5 && n / 1000 == 0) {
                    html = "blacksmith_wilbert_q0663_11.htm";
                    break;
                }
                if (n % 10 == 6 && n / 1000 == 0) {
                    html = "blacksmith_wilbert_q0663_15.htm";
                    break;
                }
                if (n % 10 == 7 && n / 1000 == 0) {
                    int n4 = n % 100;
                    if (n4 / 10 >= 7) {
                        qs.set("whispers_of_temptation", String.valueOf(1), true);
                        qs.giveItems(57, 2384000L);
                        qs.giveItems(729, 1L);
                        qs.giveItems(730, 2L);
                        html = "blacksmith_wilbert_q0663_17.htm";
                        break;
                    }
                    int n5 = n / 10;
                    html = "blacksmith_wilbert_q0663_16.htm";
                    html = html.replace("<?wincount?>", String.valueOf(n5 + 1));
                    break;
                }
                if (n == 1005) {
                    html = "blacksmith_wilbert_q0663_23.htm";
                    break;
                }
                if (n != 1006) break;
                html = "blacksmith_wilbert_q0663_26.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        int n2 = qs.getInt("whispers_of_temptation");
        int n3 = npc.getNpcId();
        if (n3 == 20674) {
            int n4;
            if (n2 >= 1 && n2 <= 4 && (n4 = ThreadLocalRandom.current().nextInt(1000)) < 807) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20678 || n3 == 20960) {
            int n5;
            if (n2 >= 1 && n2 <= 4 && (n5 = ThreadLocalRandom.current().nextInt(1000)) < 372) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20954) {
            int n6;
            if (n2 >= 1 && n2 <= 4 && (n6 = ThreadLocalRandom.current().nextInt(1000)) < 460) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20955) {
            int n7;
            if (n2 >= 1 && n2 <= 4 && (n7 = ThreadLocalRandom.current().nextInt(1000)) < 537) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20956 || n3 == 21007) {
            int n8;
            if (n2 >= 1 && n2 <= 4 && (n8 = ThreadLocalRandom.current().nextInt(1000)) < 540) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20957) {
            int n9;
            if (n2 >= 1 && n2 <= 4 && (n9 = ThreadLocalRandom.current().nextInt(1000)) < 565) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20958) {
            int n10;
            if (n2 >= 1 && n2 <= 4 && (n10 = ThreadLocalRandom.current().nextInt(1000)) < 425) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20959) {
            int n11;
            if (n2 >= 1 && n2 <= 4 && (n11 = ThreadLocalRandom.current().nextInt(1000)) < 682) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20961) {
            int n12;
            if (n2 >= 1 && n2 <= 4 && (n12 = ThreadLocalRandom.current().nextInt(1000)) < 547) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20962) {
            int n13;
            if (n2 >= 1 && n2 <= 4 && (n13 = ThreadLocalRandom.current().nextInt(1000)) < 522) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20963) {
            int n14;
            if (n2 >= 1 && n2 <= 4 && (n14 = ThreadLocalRandom.current().nextInt(1000)) < 498) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20974) {
            if (n2 >= 1 && n2 <= 4) {
                int n15 = ThreadLocalRandom.current().nextInt(1000);
                if (n15 < 100) {
                    qs.giveItems(8766, 2L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else {
                    qs.giveItems(8766, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        } else if (n3 == 20975) {
            int n16;
            if (n2 >= 1 && n2 <= 4 && (n16 = ThreadLocalRandom.current().nextInt(1000)) < 975) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20976) {
            int n17;
            if (n2 >= 1 && n2 <= 4 && (n17 = ThreadLocalRandom.current().nextInt(1000)) < 825) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20996) {
            int n18;
            if (n2 >= 1 && n2 <= 4 && (n18 = ThreadLocalRandom.current().nextInt(1000)) < 385) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20997) {
            int n19;
            if (n2 >= 1 && n2 <= 4 && (n19 = ThreadLocalRandom.current().nextInt(1000)) < 342) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20998) {
            int n20;
            if (n2 >= 1 && n2 <= 4 && (n20 = ThreadLocalRandom.current().nextInt(1000)) < 377) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 20999) {
            int n21;
            if (n2 >= 1 && n2 <= 4 && (n21 = ThreadLocalRandom.current().nextInt(1000)) < 450) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21000) {
            int n22;
            if (n2 >= 1 && n2 <= 4 && (n22 = ThreadLocalRandom.current().nextInt(1000)) < 395) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21001) {
            int n23;
            if (n2 >= 1 && n2 <= 4 && (n23 = ThreadLocalRandom.current().nextInt(1000)) < 535) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21002) {
            int n24;
            if (n2 >= 1 && n2 <= 4 && (n24 = ThreadLocalRandom.current().nextInt(1000)) < 472) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21006) {
            int n25;
            if (n2 >= 1 && n2 <= 4 && (n25 = ThreadLocalRandom.current().nextInt(1000)) < 502) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21008) {
            int n26;
            if (n2 >= 1 && n2 <= 4 && (n26 = ThreadLocalRandom.current().nextInt(1000)) < 692) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21009) {
            int n27;
            if (n2 >= 1 && n2 <= 4 && (n27 = ThreadLocalRandom.current().nextInt(1000)) < 740) {
                qs.giveItems(8766, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n3 == 21010 && n2 >= 1 && n2 <= 4 && (n = ThreadLocalRandom.current().nextInt(1000)) < 595) {
            qs.giveItems(8766, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
