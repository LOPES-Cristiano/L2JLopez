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
 * Quest 351 - 351_BlackSwan
 */
@Component
public class Quest351BlackSwan extends Quest {

	public static final int bqe = 30916;
	public static final int bnG = 30969;
	public static final int bqf = 30897;
	public static final int bqg = 20784;
	public static final int bqh = 20785;
	public static final int bqi = 21639;
	public static final int bqj = 21640;
	public static final int bqk = 4296;
	public static final int bql = 4297;
	public static final int bqm = 4298;
	public static final int bqn = 4407;
	public static final int bqo = 1867;
	public static final int boJ = 1872;
	public static final int bqp = 1870;
	public static final int bqq = 1871;
	public static final int bqr = 1882;
	public static final int bot = 1879;
	public static final int boC = 1881;
	public static final int bow = 1874;
	public static final int bqs = 1875;
	public static final int bqt = 1894;
	public static final int bor = 1888;
	public static final int boM = 1887;
	public static final int bqu = 5220;

	public Quest351BlackSwan(QuestManager questManager) {
	super(351, "351_BlackSwan", "351_BlackSwan");
		this.addStartNpc(30916);
		this.addTalkId(30969, 30897);
		this.addKillId(20784, 20785, 21639, 21640);
		this.addQuestItem(4296, 4297, 4298, 4407);
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
        if (n == 30916) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("black_swan", String.valueOf(1), true);
                qs.giveItems(4296, 1L);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "captain_gosta_q0351_04.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "captain_gosta_q0351_03.htm";
            }
        } else if (n == 30969) {
            if (event.equalsIgnoreCase("reply_1")) {
                if (qs.getQuestItemsCount(4297) == 0L) {
                    string2 = "iason_haine_q0351_02.htm";
                } else if (qs.getQuestItemsCount(4297) >= 10L) {
                    qs.giveItems(57, 3880L + 20L * qs.getQuestItemsCount(4297));
                } else {
                    qs.giveItems(57, 20L * qs.getQuestItemsCount(4297));
                }
                qs.takeItems(4297, -1L);
                string2 = "iason_haine_q0351_03.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                if (qs.getQuestItemsCount(4298) == 0L) {
                    string2 = "iason_haine_q0351_04.htm";
                } else {
                    qs.setCond(2);
                    qs.giveItems(4407, qs.getQuestItemsCount(4298));
                    qs.giveItems(57, 3880L);
                    qs.takeItems(4298, -1L);
                    string2 = "iason_haine_q0351_05.htm";
                }
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = "iason_haine_q0351_06.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = qs.getQuestItemsCount(4298) == 0L && qs.getQuestItemsCount(4297) == 0L ? "iason_haine_q0351_07.htm" : "iason_haine_q0351_08.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                if (qs.getQuestItemsCount(4296) > 0L) {
                    qs.takeItems(4296, -1L);
                }
                qs.unset("black_swan");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "iason_haine_q0351_09.htm";
            }
        } else if (n == 30897) {
            if (event.equalsIgnoreCase("reply_1")) {
                if (qs.getQuestItemsCount(4407) > 0L) {
                    qs.giveItems(57, 700L);
                    qs.takeItems(4407, 1L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_2")) {
                if (qs.getQuestItemsCount(4407) >= 3L) {
                    qs.giveItems(1867, 20L);
                    qs.takeItems(4407, 3L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_3")) {
                if (qs.getQuestItemsCount(4407) >= 3L) {
                    qs.giveItems(1872, 20L);
                    qs.takeItems(4407, 3L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_4")) {
                if (qs.getQuestItemsCount(4407) >= 2L) {
                    qs.giveItems(1870, 10L);
                    qs.takeItems(4407, 2L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_5")) {
                if (qs.getQuestItemsCount(4407) >= 2L) {
                    qs.giveItems(1871, 10L);
                    qs.takeItems(4407, 2L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_6")) {
                if (qs.getQuestItemsCount(4407) >= 9L) {
                    qs.giveItems(1882, 10L);
                    qs.takeItems(4407, 9L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_7")) {
                if (qs.getQuestItemsCount(4407) >= 5L) {
                    qs.giveItems(1879, 6L);
                    qs.takeItems(4407, 5L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_8")) {
                if (qs.getQuestItemsCount(4407) >= 3L) {
                    qs.giveItems(1881, 2L);
                    qs.takeItems(4407, 3L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_9")) {
                if (qs.getQuestItemsCount(4407) >= 3L) {
                    qs.giveItems(1874, 1L);
                    qs.takeItems(4407, 3L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_10")) {
                if (qs.getQuestItemsCount(4407) >= 3L) {
                    qs.giveItems(1875, 1L);
                    qs.takeItems(4407, 3L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_11")) {
                if (qs.getQuestItemsCount(4407) >= 6L) {
                    qs.giveItems(1894, 1L);
                    qs.giveItems(57, 210L);
                    qs.takeItems(4407, 6L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_12")) {
                if (qs.getQuestItemsCount(4407) >= 7L) {
                    qs.giveItems(1888, 1L);
                    qs.giveItems(57, 280L);
                    qs.takeItems(4407, 7L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_13")) {
                if (qs.getQuestItemsCount(4407) >= 9L) {
                    qs.giveItems(1887, 1L);
                    qs.giveItems(57, 630L);
                    qs.takeItems(4407, 9L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_15")) {
                if (qs.getQuestItemsCount(4407) >= 5L) {
                    qs.giveItems(5220, 1L);
                    qs.takeItems(4407, 5L);
                    string2 = "head_blacksmith_roman_q0351_03.htm";
                } else {
                    string2 = "head_blacksmith_roman_q0351_04.htm";
                }
            } else if (event.equalsIgnoreCase("reply_14")) {
                string2 = "head_blacksmith_roman_q0351_05.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("black_swan");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30916) break;
                if (pc.getLevel() < 32 || pc.getLevel() > 36) {
                    html = "captain_gosta_q0351_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "captain_gosta_q0351_02.htm";
                break;
            }
            case 2: {
                if (n2 == 30916) {
                    if (n < 0) break;
                    html = "captain_gosta_q0351_05.htm";
                    break;
                }
                if (n2 == 30969) {
                    if (n != 1) break;
                    html = "iason_haine_q0351_01.htm";
                    break;
                }
                if (n2 != 30897) break;
                if (qs.getQuestItemsCount(4407) >= 1L) {
                    html = "head_blacksmith_roman_q0351_01.htm";
                    break;
                }
                if (qs.getQuestItemsCount(4407) != 0L) break;
                html = "head_blacksmith_roman_q0351_02.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getQuestItemsCount(4296) > 0L) {
            if (n == 20784 || n == 21639) {
                int n2 = ThreadLocalRandom.current().nextInt(100);
                if (n2 < 10) {
                    qs.giveItems(4297, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                    if (ThreadLocalRandom.current().nextInt(20) == 0) {
                        qs.giveItems(4298, 1L);
                    }
                } else if (n2 < 15) {
                    qs.giveItems(4297, 2L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                    if (ThreadLocalRandom.current().nextInt(20) == 0) {
                        qs.giveItems(4298, 1L);
                    }
                } else if (ThreadLocalRandom.current().nextInt(100) < 4) {
                    qs.giveItems(4298, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 20785 || n == 21640) {
                int n3 = ThreadLocalRandom.current().nextInt(20);
                if (n3 < 10) {
                    qs.giveItems(4297, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                    if (ThreadLocalRandom.current().nextInt(20) == 0) {
                        qs.giveItems(4298, 1L);
                    }
                } else if (n3 < 15) {
                    qs.giveItems(4297, 2L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                    if (ThreadLocalRandom.current().nextInt(20) == 0) {
                        qs.giveItems(4298, 1L);
                    }
                } else if (ThreadLocalRandom.current().nextInt(100) < 3) {
                    qs.giveItems(4298, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
