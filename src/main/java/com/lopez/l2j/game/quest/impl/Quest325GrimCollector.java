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
 * Quest 325 - 325_GrimCollector
 */
@Component
public class Quest325GrimCollector extends Quest {

	public static final int bfP = 30336;
	public static final int bfQ = 30342;
	public static final int bfR = 30434;
	public static final int bfS = 20026;
	public static final int bfT = 20029;
	public static final int bfU = 20035;
	public static final int bfV = 20042;
	public static final int bfW = 20045;
	public static final int bfX = 20457;
	public static final int bfY = 20458;
	public static final int bfZ = 20051;
	public static final int bga = 20514;
	public static final int bgb = 20515;
	public static final int bgc = 1350;
	public static final int bgd = 1351;
	public static final int bge = 1352;
	public static final int bgf = 1353;
	public static final int bgg = 1354;
	public static final int bgh = 1355;
	public static final int bgi = 1356;
	public static final int bgj = 1357;
	public static final int bgk = 1358;
	public static final int bgl = 1349;

	public Quest325GrimCollector(QuestManager questManager) {
		super(325, "325_GrimCollector", "325_GrimCollector");
		addStartNpc(30336);
		addTalkNpc(30342);
		addTalkNpc(30434);
		addKillId(20458);
		addKillId(20457);
		addKillId(20029);
		addKillId(20035);
		addKillId(20042);
		addKillId(20514);
		addKillId(20515);
		addKillId(20026);
		addKillId(20045);
		addKillId(20051);
		registerQuestItems(1350, 1351, 1352, 1353, 1354, 1355, 1356, 1357, 1358, 1349);
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
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.set("grim_collector", String.valueOf(1), true);
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound("QuestState.SOUND_ACCEPT");
            string2 = "guard_curtiz_q0325_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "samed_q0325_02.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "samed_q0325_03.htm";
            qs.giveItems(1349, 1L);
        } else if (event.equalsIgnoreCase("reply_3")) {
            string2 = "samed_q0325_06.htm";
            if (qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) + qs.getQuestItemsCount(1358) > 0L) {
                if (qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) + qs.getQuestItemsCount(1358) >= 10L) {
                    if (qs.getQuestItemsCount(1358) >= 1L) {
                        qs.giveItems(57, 2172L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                    } else {
                        qs.giveItems(57, 1629L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                    }
                } else if (qs.getQuestItemsCount(1358) >= 1L) {
                    qs.giveItems(57, 543L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                } else {
                    qs.giveItems(57, 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                }
            }
            qs.takeItems(1350, -1L);
            qs.takeItems(1351, -1L);
            qs.takeItems(1352, -1L);
            qs.takeItems(1353, -1L);
            qs.takeItems(1354, -1L);
            qs.takeItems(1355, -1L);
            qs.takeItems(1356, -1L);
            qs.takeItems(1357, -1L);
            qs.takeItems(1358, -1L);
            qs.takeItems(1349, -1L);
            qs.unset("grim_collector");
            qs.exitQuest(true);
            qs.playSound("QuestState.SOUND_FINISH");
        } else if (event.equalsIgnoreCase("reply_4")) {
            string2 = "samed_q0325_07.htm";
            if (qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) + qs.getQuestItemsCount(1358) > 0L) {
                if (qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) + qs.getQuestItemsCount(1358) >= 10L) {
                    if (qs.getQuestItemsCount(1358) >= 1L) {
                        qs.giveItems(57, 2172L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                    } else {
                        qs.giveItems(57, 1629L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                    }
                } else if (qs.getQuestItemsCount(1358) >= 1L) {
                    qs.giveItems(57, 543L + 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                } else {
                    qs.giveItems(57, 30L * qs.getQuestItemsCount(1350) + 20L * qs.getQuestItemsCount(1351) + 20L * qs.getQuestItemsCount(1352) + 100L * qs.getQuestItemsCount(1353) + 40L * qs.getQuestItemsCount(1354) + 14L * qs.getQuestItemsCount(1355) + 14L * qs.getQuestItemsCount(1356) + 14L * qs.getQuestItemsCount(1357) + 341L * qs.getQuestItemsCount(1358));
                }
            }
            qs.takeItems(1350, -1L);
            qs.takeItems(1351, -1L);
            qs.takeItems(1352, -1L);
            qs.takeItems(1353, -1L);
            qs.takeItems(1354, -1L);
            qs.takeItems(1355, -1L);
            qs.takeItems(1356, -1L);
            qs.takeItems(1357, -1L);
            qs.takeItems(1358, -1L);
        } else if (event.equalsIgnoreCase("reply_5")) {
            string2 = "samed_q0325_09.htm";
            if (qs.getQuestItemsCount(1358) > 0L) {
                qs.giveItems(57, 543L + 341L * qs.getQuestItemsCount(1358));
            }
            qs.takeItems(1358, -1L);
        } else if (event.equalsIgnoreCase("reply_6")) {
            if (qs.getQuestItemsCount(1355) > 0L && qs.getQuestItemsCount(1356) > 0L && qs.getQuestItemsCount(1353) > 0L && qs.getQuestItemsCount(1354) > 0L && qs.getQuestItemsCount(1357) > 0L) {
                if (ThreadLocalRandom.current().nextInt(5) < 4) {
                    string2 = "varsak_q0325_03.htm";
                    qs.takeItems(1355, 1L);
                    qs.takeItems(1353, 1L);
                    qs.takeItems(1356, 1L);
                    qs.takeItems(1354, 1L);
                    qs.takeItems(1357, 1L);
                    qs.giveItems(1358, 1L);
                } else {
                    qs.takeItems(1355, 1L);
                    qs.takeItems(1353, 1L);
                    qs.takeItems(1356, 1L);
                    qs.takeItems(1354, 1L);
                    qs.takeItems(1357, 1L);
                    string2 = "varsak_q0325_04.htm";
                }
            } else {
                string2 = "varsak_q0325_02.htm";
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
        int n3 = qs.getInt("grim_collector");
        switch (n2) {
            case 1: {
                if (n != 30336) break;
                if (pc.getLevel() >= 15) {
                    html = "guard_curtiz_q0325_02.htm";
                    break;
                }
                html = "guard_curtiz_q0325_01.htm";
                break;
            }
            case 2: {
                if (n == 30336) {
                    if (n3 == 1 && qs.getQuestItemsCount(1349) == 0L) {
                        html = "guard_curtiz_q0325_04.htm";
                        break;
                    }
                    if (n3 != 1 || qs.getQuestItemsCount(1349) != 1L) break;
                    html = "guard_curtiz_q0325_05.htm";
                    break;
                }
                if (n == 30342) {
                    if (n3 != 1 || qs.getQuestItemsCount(1349) != 1L) break;
                    html = "varsak_q0325_01.htm";
                    break;
                }
                if (n != 30434) break;
                if (n3 == 1 && qs.getQuestItemsCount(1349) == 0L) {
                    html = "samed_q0325_01.htm";
                    break;
                }
                if (n3 == 1 && qs.getQuestItemsCount(1349) == 1L && qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) + qs.getQuestItemsCount(1358) < 1L) {
                    html = "samed_q0325_04.htm";
                    break;
                }
                if (n3 == 1 && qs.getQuestItemsCount(1358) == 0L && qs.getQuestItemsCount(1349) == 1L && qs.getQuestItemsCount(1350) + qs.getQuestItemsCount(1355) + qs.getQuestItemsCount(1356) + qs.getQuestItemsCount(1351) + qs.getQuestItemsCount(1352) + qs.getQuestItemsCount(1353) + qs.getQuestItemsCount(1354) + qs.getQuestItemsCount(1357) > 0L) {
                    html = "samed_q0325_05.htm";
                    break;
                }
                if (n3 != 1 || qs.getQuestItemsCount(1349) != 1L || qs.getQuestItemsCount(1358) <= 0L) break;
                html = "samed_q0325_08.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = ThreadLocalRandom.current().nextInt(100);
        if (n == 20026 || n == 20029 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 30) {
                qs.rollAndGive(1350, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 50) {
                qs.rollAndGive(1351, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 75) {
                qs.rollAndGive(1352, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20035 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 5) {
                qs.rollAndGive(1353, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 15) {
                qs.rollAndGive(1354, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 29) {
                qs.rollAndGive(1355, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 79) {
                qs.rollAndGive(1357, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20042 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 6) {
                qs.rollAndGive(1353, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 19) {
                qs.rollAndGive(1354, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 69) {
                qs.rollAndGive(1356, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 86) {
                qs.rollAndGive(1357, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20045 || n == 20457 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 40) {
                qs.rollAndGive(1350, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 60) {
                qs.rollAndGive(1351, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 80) {
                qs.rollAndGive(1352, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20458 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 40) {
                qs.rollAndGive(1350, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 70) {
                qs.rollAndGive(1351, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 100) {
                qs.rollAndGive(1352, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20051 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 9) {
                qs.rollAndGive(1353, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 59) {
                qs.rollAndGive(1354, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 79) {
                qs.rollAndGive(1355, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 100) {
                qs.rollAndGive(1356, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20514 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 6) {
                qs.rollAndGive(1353, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 21) {
                qs.rollAndGive(1354, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 30) {
                qs.rollAndGive(1355, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 31) {
                qs.rollAndGive(1356, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 64) {
                qs.rollAndGive(1357, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        } else if (n == 20515 && qs.getQuestItemsCount(1349) > 0L) {
            if (n2 < 5) {
                qs.rollAndGive(1353, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 20) {
                qs.rollAndGive(1354, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 31) {
                qs.rollAndGive(1355, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 33) {
                qs.rollAndGive(1356, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else if (n2 < 69) {
                qs.rollAndGive(1357, 1, 100.0);
                qs.playSound("QuestState.SOUND_ITEMGET");
            }
        }
        return null;
    
	}

}
