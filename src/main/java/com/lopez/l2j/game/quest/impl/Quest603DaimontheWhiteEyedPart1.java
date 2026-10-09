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
 * Quest 603 - 603_DaimontheWhiteEyedPart1
 */
@Component
public class Quest603DaimontheWhiteEyedPart1 extends Quest {

	public static final int bJz = 31683;
	public static final int bJM = 31548;
	public static final int bJN = 31549;
	public static final int bJO = 31550;
	public static final int bJP = 31551;
	public static final int bJQ = 31552;
	public static final int bJJ = 21299;
	public static final int bJR = 21297;
	public static final int bJK = 21304;
	public static final int bJS = 7190;
	public static final int bJT = 7191;
	public static final int bJU = 7192;

	public Quest603DaimontheWhiteEyedPart1(QuestManager questManager) {
	super(603, "603_DaimontheWhiteEyedPart1", "603_DaimontheWhiteEyedPart1");
		this.addStartNpc(31683);
		this.addTalkId(31548, 31549, 31550, 31551, 31552);
		this.addKillId(21299, 21297, 21304);
		this.addQuestItem(7190);
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
        if (n == 31683) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("daemon_of_hundred", String.valueOf(11), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "eye_of_argos_q0603_0104.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                if (qs.getQuestItemsCount(7191) >= 5L) {
                    qs.setCond(7);
                    qs.set("daemon_of_hundred", String.valueOf(71), true);
                    qs.takeItems(7191, 5L);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "eye_of_argos_q0603_0701.htm";
                } else {
                    string2 = "eye_of_argos_q0603_0702.htm";
                }
            } else if (event.equalsIgnoreCase("reply_3")) {
                if (qs.getQuestItemsCount(7190) >= 200L) {
                    qs.takeItems(7190, -1L);
                    qs.giveItems(7192, 1L);
                    qs.unset("daemon_of_hundred");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(false);
                    string2 = "eye_of_argos_q0603_0801.htm";
                } else {
                    string2 = "eye_of_argos_q0603_0802.htm";
                }
            }
        } else if (n == 31548) {
            if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(2);
                qs.set("daemon_of_hundred", String.valueOf(21), true);
                qs.giveItems(7191, 1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "ancient_lithography1_q0603_0201.htm";
            }
        } else if (n == 31549) {
            if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(3);
                qs.set("daemon_of_hundred", String.valueOf(31), true);
                qs.giveItems(7191, 1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "ancient_lithography2_q0603_0301.htm";
            }
        } else if (n == 31550) {
            if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(4);
                qs.set("daemon_of_hundred", String.valueOf(41), true);
                qs.giveItems(7191, 1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "ancient_lithography3_q0603_0401.htm";
            }
        } else if (n == 31551) {
            if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(5);
                qs.set("daemon_of_hundred", String.valueOf(51), true);
                qs.giveItems(7191, 1L);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "ancient_lithography4_q0603_0501.htm";
            }
        } else if (n == 31552 && event.equalsIgnoreCase("reply_1")) {
            qs.setCond(6);
            qs.set("daemon_of_hundred", String.valueOf(61), true);
            qs.giveItems(7191, 1L);
            qs.playSound(QuestState.SOUND_MIDDLE);
            string2 = "ancient_lithography5_q0603_0601.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("daemon_of_hundred");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31683) break;
                if (pc.getLevel() >= 73) {
                    html = "eye_of_argos_q0603_0101.htm";
                    break;
                }
                html = "eye_of_argos_q0603_0103.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 31683) {
                    if (n == 11) {
                        html = "eye_of_argos_q0603_0105.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(7191) >= 1L && n == 61) {
                        html = "eye_of_argos_q0603_0601.htm";
                        break;
                    }
                    if (n > 72 || n < 71) break;
                    if (n == 72 && qs.getQuestItemsCount(7190) >= 200L) {
                        html = "eye_of_argos_q0603_0703.htm";
                        break;
                    }
                    html = "eye_of_argos_q0603_0704.htm";
                    break;
                }
                if (n2 == 31548) {
                    if (n == 11) {
                        html = "ancient_lithography1_q0603_0101.htm";
                        break;
                    }
                    if (n != 21) break;
                    html = "ancient_lithography1_q0603_0203.htm";
                    break;
                }
                if (n2 == 31549) {
                    if (qs.getQuestItemsCount(7191) >= 1L && n == 21) {
                        html = "ancient_lithography2_q0603_0201.htm";
                        break;
                    }
                    if (n != 31) break;
                    html = "ancient_lithography2_q0603_0303.htm";
                    break;
                }
                if (n2 == 31550) {
                    if (qs.getQuestItemsCount(7191) >= 1L && n == 31) {
                        html = "ancient_lithography3_q0603_0301.htm";
                        break;
                    }
                    if (n != 41) break;
                    html = "ancient_lithography3_q0603_0403.htm";
                    break;
                }
                if (n2 == 31551) {
                    if (qs.getQuestItemsCount(7191) >= 1L && n == 41) {
                        html = "ancient_lithography4_q0603_0401.htm";
                        break;
                    }
                    if (n != 51) break;
                    html = "ancient_lithography4_q0603_0503.htm";
                    break;
                }
                if (n2 != 31552) break;
                if (qs.getQuestItemsCount(7191) >= 1L && n == 51) {
                    html = "ancient_lithography5_q0603_0501.htm";
                    break;
                }
                if (n != 61) break;
                html = "ancient_lithography5_q0603_0603.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("daemon_of_hundred");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 71) {
            int n3;
            if (n2 == 21299) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 519) {
                    if (qs.getQuestItemsCount(7190) + 1L >= 200L) {
                        if (qs.getQuestItemsCount(7190) < 200L) {
                            qs.setCond(8);
                            qs.set("daemon_of_hundred", String.valueOf(72), true);
                            qs.giveItems(7190, 200L - qs.getQuestItemsCount(7190));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                    } else {
                        qs.giveItems(7190, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 21297) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 500) {
                    if (qs.getQuestItemsCount(7190) + 1L >= 200L) {
                        if (qs.getQuestItemsCount(7190) < 200L) {
                            qs.setCond(8);
                            qs.set("daemon_of_hundred", String.valueOf(72), true);
                            qs.giveItems(7190, 200L - qs.getQuestItemsCount(7190));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                    } else {
                        qs.giveItems(7190, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 21304 && (n3 = ThreadLocalRandom.current().nextInt(1000)) < 673) {
                if (qs.getQuestItemsCount(7190) + 1L >= 200L) {
                    if (qs.getQuestItemsCount(7190) < 200L) {
                        qs.setCond(8);
                        qs.set("daemon_of_hundred", String.valueOf(72), true);
                        qs.giveItems(7190, 200L - qs.getQuestItemsCount(7190));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                } else {
                    qs.giveItems(7190, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
