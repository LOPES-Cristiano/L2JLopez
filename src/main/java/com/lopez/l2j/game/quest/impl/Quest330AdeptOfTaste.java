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
 * Quest 330 - 330_AdeptOfTaste
 */
@Component
public class Quest330AdeptOfTaste extends Quest {

	public static final int bgU = 30062;
	public static final int bgV = 30067;
	public static final int bgW = 30069;
	public static final int bgX = 30073;
	public static final int bgY = 30078;
	public static final int aLp = 30461;
	public static final int bgZ = 30469;
	public static final int bha = 20147;
	public static final int bhb = 20154;
	public static final int bhc = 20155;
	public static final int bhd = 20156;
	public static final int bhe = 20204;
	public static final int bhf = 20223;
	public static final int bhg = 20226;
	public static final int bhh = 20228;
	public static final int bhi = 20229;
	public static final int bhj = 20265;
	public static final int bhk = 20266;
	public static final int bhl = 1420;
	public static final int bhm = 1421;
	public static final int bhn = 1422;
	public static final int bho = 1423;
	public static final int bhp = 1424;
	public static final int bhq = 1425;
	public static final int bhr = 1426;
	public static final int bhs = 1427;
	public static final int bht = 1428;
	public static final int bhu = 1429;
	public static final int bhv = 1430;
	public static final int bhw = 1431;
	public static final int bhx = 1432;
	public static final int bhy = 1433;
	public static final int bhz = 1434;
	public static final int bhA = 1435;
	public static final int bhB = 1436;
	public static final int bhC = 1437;
	public static final int bhD = 1438;
	public static final int bhE = 1439;
	public static final int bhF = 1440;
	public static final int bhG = 1441;
	public static final int[] bhH = new int[]{1442, 1443, 1444, 1445, 1446};
	public static final int[] bhI = new int[]{1447, 1448, 1449, 1450, 1451};
	public static final int[] bhJ = new int[]{1424, 1429, 1433, 1437, 1441};
	public static final int[] bhK = new int[]{1425, 1430, 1438};
	public static final int[] bhL = new int[]{0, 0, 1455, 1456, 1457};
	public static final int[] bhM = new int[]{10000, 14870, 6490, 12220, 16540};

	public Quest330AdeptOfTaste(QuestManager questManager) {
		super(330, "330_AdeptOfTaste", "330_AdeptOfTaste");
		addStartNpc(30469);
		addTalkNpc(30062);
		addTalkNpc(30461);
		addTalkNpc(30078);
		addTalkNpc(30069);
		addTalkNpc(30067);
		addTalkNpc(30073);
		addKillId(20228);
		addKillId(20155);
		addKillId(20204);
		addKillId(20154);
		addKillId(20229);
		addKillId(20156);
		addKillId(20223);
		addKillId(20266);
		addKillId(20147);
		addKillId(20265);
		addKillId(20226);
		registerQuestItems(1420, 1421, 1422, 1423, 1426, 1427, 1428, 1431, 1432, 1434, 1435, 1436, 1439, 1440);
		registerQuestItems(bhJ);
		registerQuestItems(bhK);
		registerQuestItems(bhH);
		registerQuestItems(bhI);
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

        int n = qs.getStateId();
        if (event.equalsIgnoreCase("30469_03.htm") && n == 1) {
            if (qs.getQuestItemsCount(1420) == 0L) {
                qs.giveItems(1420, 1L);
            }
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("30062_05.htm") && n == 2) {
            if (qs.getQuestItemsCount(1423) + qs.getQuestItemsCount(1422) < 40L) {
                return null;
            }
            rewardIngredient(qs, 1421, 1422, 1423, 1424);
        } else if (event.equalsIgnoreCase("30067_05.htm") && n == 2) {
            if (qs.getQuestItemsCount(1436) + qs.getQuestItemsCount(1435) < 20L) {
                return null;
            }
            rewardIngredient(qs, 1434, 1435, 1436, 1437);
        } else if (event.equalsIgnoreCase("30073_05.htm") && n == 2) {
            if (qs.getQuestItemsCount(1427) < 20L) {
                return null;
            }
            rewardIngredient(qs, 1426, 1427, 1428, 1429);
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        boolean bl;
        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 != 30469) {
                return "noquest";
            }
            if (pc.getLevel() < 24) {
                qs.exitQuest(true);
                return "30469_01.htm";
            }
            qs.setCond(0);
            return "30469_02.htm";
        }
        if (n != 2) {
            return "noquest";
        }
        long l = qs.getQuestItemsCount(bhJ);
        long l2 = qs.getQuestItemsCount(bhK);
        long l3 = l + l2;
        boolean bl2 = bl = qs.getQuestItemsCount(1420) > 0L;
        if (n2 == 30469) {
            if (bl) {
                if (l3 < 5L) {
                    return "30469_04.htm";
                }
                qs.takeAllItems(1420);
                qs.takeAllItems(bhJ);
                qs.takeAllItems(bhK);
                if (l2 > 3L) {
                    l2 = 3L;
                }
                qs.playSound((l2 += (long)ThreadLocalRandom.current().nextInt(0, 1 + 1)) == 4L ? "QuestState.QuestState.SOUND_JACKPOT" : "QuestState.SOUND_MIDDLE");
                qs.giveItems(bhH[(int)l2], 1L);
                return "30469_05t" + ++l2 + ".htm";
            }
            if (l3 == 0L) {
                long l4 = qs.getQuestItemsCount(bhH);
                long l5 = qs.getQuestItemsCount(bhI);
                if (l4 > 0L && l5 == 0L) {
                    return "30469_06.htm";
                }
                if (l4 == 0L && l5 > 0L) {
                    for (int i = bhI.length; i > 0; --i) {
                        if (qs.getQuestItemsCount(bhI[i - 1]) <= 0L) continue;
                        qs.takeAllItems(bhI);
                        if (bhM[i - 1] > 0) {
                            qs.giveItems(57, bhM[i - 1]);
                        }
                        if (bhL[i - 1] > 0) {
                            qs.giveItems(bhL[i - 1], 1L);
                        }
                        qs.playSound("QuestState.SOUND_FINISH");
                        qs.exitQuest(true);
                        return "30469_06t" + i + ".htm";
                    }
                }
            }
        }
        if (n2 == 30461) {
            if (bl) {
                return "30461_01.htm";
            }
            if (l3 == 0L) {
                if (qs.getQuestItemsCount(bhI) > 0L) {
                    return "30461_04.htm";
                }
                for (int i = bhH.length; i > 0; --i) {
                    if (qs.getQuestItemsCount(bhH[i - 1]) <= 0L) continue;
                    qs.takeAllItems(bhH);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                    qs.giveItems(bhI[i - 1], 1L);
                    return "30461_02t" + i + ".htm";
                }
            }
        }
        if (!bl || l3 >= 5L) {
            return "noquest";
        }
        if (n2 == 30062) {
            boolean bl3;
            boolean bl4 = bl3 = qs.getQuestItemsCount(1424) > 0L || qs.getQuestItemsCount(1425) > 0L;
            if (qs.getQuestItemsCount(1421) > 0L) {
                if (!bl3) {
                    long l6 = qs.getQuestItemsCount(1423);
                    if (l6 >= 40L) {
                        rewardIngredient(qs, 1421, 1422, 1423, 1425);
                        return "30062_06.htm";
                    }
                    return (l6 += qs.getQuestItemsCount(1422)) < 40L ? "30062_02.htm" : "30062_03.htm";
                }
            } else {
                if (bl3) {
                    return "30062_07.htm";
                }
                qs.giveItems(1421, 1L);
                return "30062_01.htm";
            }
        }
        if (n2 == 30067) {
            boolean bl5;
            boolean bl6 = bl5 = qs.getQuestItemsCount(1437) > 0L || qs.getQuestItemsCount(1438) > 0L;
            if (qs.getQuestItemsCount(1434) > 0L) {
                if (!bl5) {
                    long l7 = qs.getQuestItemsCount(1436);
                    if (l7 >= 20L) {
                        rewardIngredient(qs, 1434, 1435, 1436, 1438);
                        return "30067_06.htm";
                    }
                    return (l7 += qs.getQuestItemsCount(1435)) < 20L ? "30067_02.htm" : "30067_03.htm";
                }
            } else if (bl5) {
                return "30067_07.htm";
            }
            qs.giveItems(1434, 1L);
            return "30067_01.htm";
        }
        if (n2 == 30069) {
            boolean bl7;
            boolean bl8 = bl7 = qs.getQuestItemsCount(1441) > 0L;
            if (qs.getQuestItemsCount(1439) > 0L) {
                if (!bl7) {
                    if (qs.getQuestItemsCount(1440) < 30L) {
                        return "30069_02.htm";
                    }
                    qs.takeItems(1439, -1L);
                    qs.takeItems(1440, -1L);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                    qs.giveItems(1441, 1L);
                    return "30069_03.htm";
                }
            } else {
                if (bl7) {
                    return "30069_04.htm";
                }
                qs.giveItems(1439, 1L);
                return "30069_01.htm";
            }
        }
        if (n2 == 30073) {
            boolean bl9;
            boolean bl10 = bl9 = qs.getQuestItemsCount(1429) > 0L || qs.getQuestItemsCount(1430) > 0L;
            if (qs.getQuestItemsCount(1426) > 0L) {
                if (!bl9) {
                    if (qs.getQuestItemsCount(1427) < 20L) {
                        return "30073_02.htm";
                    }
                    if (qs.getQuestItemsCount(1428) < 10L) {
                        return "30073_03.htm";
                    }
                    rewardIngredient(qs, 1426, 1427, 1428, 1430);
                    return "30073_06.htm";
                }
            } else {
                if (bl9) {
                    return "30073_07.htm";
                }
                qs.giveItems(1426, 1L);
                return "30073_01.htm";
            }
        }
        if (n2 == 30078) {
            boolean bl11;
            boolean bl12 = bl11 = qs.getQuestItemsCount(1433) > 0L;
            if (qs.getQuestItemsCount(1431) > 0L) {
                if (!bl11) {
                    if (qs.getQuestItemsCount(1432) < 30L) {
                        return "30078_02.htm";
                    }
                    qs.takeItems(1431, -1L);
                    qs.takeItems(1432, -1L);
                    qs.playSound("QuestState.SOUND_MIDDLE");
                    qs.giveItems(1433, 1L);
                    return "30078_03.htm";
                }
            } else {
                if (bl11) {
                    return "30078_04.htm";
                }
                qs.giveItems(1431, 1L);
                return "30078_01.htm";
            }
        }
        return "noquest";
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        boolean bl;
        if (qs.getState() != State.STARTED) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        long l = qs.getQuestItemsCount(bhJ);
        long l2 = qs.getQuestItemsCount(bhK);
        long l3 = l + l2;
        boolean bl2 = bl = qs.getQuestItemsCount(1420) > 0L;
        if (!bl || l3 >= 5L) {
            return null;
        }
        if (n == 20147 && qs.getQuestItemsCount(1431) > 0L) {
            qs.rollAndGive(1432, 1, 1, 30, 100.0);
        } else if (n == 20154 && qs.getQuestItemsCount(1421) > 0L) {
            a(qs, 70, 77);
        } else if (n == 20155 && qs.getQuestItemsCount(1421) > 0L) {
            a(qs, 77, 85);
        } else if (n == 20156 && qs.getQuestItemsCount(1421) > 0L) {
            a(qs, 87, 96);
        } else if (n == 20223 && qs.getQuestItemsCount(1421) > 0L) {
            a(qs, 70, 77);
        } else if (n == 20204 && qs.getQuestItemsCount(1426) > 0L) {
            b(qs, 80, 95);
        } else if (n == 20229 && qs.getQuestItemsCount(1426) > 0L) {
            b(qs, 92, 100);
        } else if (n == 20226 && qs.getQuestItemsCount(1434) > 0L) {
            c(qs, 87, 96);
        } else if (n == 20228 && qs.getQuestItemsCount(1434) > 0L) {
            c(qs, 90, 100);
        } else if (n == 20265 && qs.getQuestItemsCount(1439) > 0L) {
            qs.rollAndGive(1440, 1, 3, 30, 97.0);
        } else if (n == 20266 && qs.getQuestItemsCount(1439) > 0L) {
            qs.rollAndGive(1440, 1, 2, 30, 100.0);
        }
        return null;
	}

	private static void a(QuestState qs, int c1, int c2) {
		int roll = ThreadLocalRandom.current().nextInt(100);
		if (roll < c1) {
			qs.rollAndGive(1422, 1, 1, 40, 100.0);
		} else if (roll < c2) {
			qs.rollAndGive(1423, 1, 1, 40, 100.0);
		}
	}

	private static void b(QuestState qs, int c1, int c2) {
		int roll = ThreadLocalRandom.current().nextInt(100);
		if (roll < c1) {
			qs.rollAndGive(1427, 1, 1, 20, 100.0);
		} else if (roll < c2) {
			qs.rollAndGive(1428, 1, 1, 10, 100.0);
		}
	}

	private static void c(QuestState qs, int c1, int c2) {
		int roll = ThreadLocalRandom.current().nextInt(100);
		if (roll < c1) {
			qs.rollAndGive(1435, 1, 1, 20, 100.0);
		} else if (roll < c2) {
			qs.rollAndGive(1436, 1, 1, 20, 100.0);
		}
	}

	private static void rewardIngredient(QuestState qs, int orderId, int item1, int item2, int rewardId) {
		qs.takeItems(orderId, -1);
		qs.takeItems(item1, -1);
		qs.takeItems(item2, -1);
		qs.playSound(QuestState.SOUND_MIDDLE);
		qs.giveItems(rewardId, 1);
	}

}
