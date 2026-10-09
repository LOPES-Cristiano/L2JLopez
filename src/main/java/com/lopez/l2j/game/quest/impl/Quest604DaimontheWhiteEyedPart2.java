package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 604 - 604_DaimontheWhiteEyedPart2
 */
@Component
public class Quest604DaimontheWhiteEyedPart2 extends Quest {

	public static final int bJz = 31683;
	public static final int bJV = 31541;
	public static final int bJq = 25290;
	public static final int bJU = 7192;
	public static final int bJW = 7193;
	public static final int bJX = 7194;
	public static final int bJY = 4595;
	public static final int bJZ = 4596;
	public static final int bKa = 4597;
	public static final int bKb = 4598;
	public static final int bKc = 4599;
	public static final int bKd = 4600;

	public Quest604DaimontheWhiteEyedPart2(QuestManager questManager) {
	super(604, "604_DaimontheWhiteEyedPart2", "604_DaimontheWhiteEyedPart2");
		this.addStartNpc(31683);
		this.addTalkId(31541);
		this.addKillId(25290);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return null;
		}

		if ("60401".equalsIgnoreCase(event)) {
			// npcSay
			qs.set("spawned_daemon_of_hundred_eyes", String.valueOf(0), true);
			if (npc != null) {
				npc.deleteMe();
			}
			return null;
		}

		String string2 = event;
		int n = qs.getInt("spawned_daemon_of_hundred_eyes");
		int n2 = qs.getInt("daemon_of_hundred_eyes_second_cookie");
		int n3 = npc != null ? npc.getNpcId() : 0;
		if (n3 == 31683) {
			if (event.equalsIgnoreCase("quest_accept")) {
				qs.setCond(1);
				qs.set("daemon_of_hundred_eyes_second", String.valueOf(11), true);
				qs.set("spawned_daemon_of_hundred_eyes", String.valueOf(0), true);
				qs.takeItems(7192, -1L);
				qs.giveItems(7193, 1L);
				qs.setState(State.STARTED);
				qs.playSound(QuestState.SOUND_ACCEPT);
				string2 = "eye_of_argos_q0604_0104.htm";
			} else if (event.equalsIgnoreCase("reply_3") && n2 == 2) {
				if (qs.getQuestItemsCount(7194) >= 1L) {
					int n4 = ThreadLocalRandom.current().nextInt(1000);
					qs.takeItems(7194, 1L);
					if (n4 < 167) {
						qs.giveItems(4595, 5L);
					} else if (n4 < 334) {
						qs.giveItems(4596, 5L);
					} else if (n4 < 501) {
						qs.giveItems(4597, 5L);
					} else if (n4 < 668) {
						qs.giveItems(4598, 5L);
					} else if (n4 < 835) {
						qs.giveItems(4599, 5L);
					} else if (n4 < 1000) {
						qs.giveItems(4600, 5L);
					}
					qs.unset("daemon_of_hundred_eyes_second");
					qs.unset("daemon_of_hundred_eyes_second_cookie");
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(true);
					string2 = "eye_of_argos_q0604_0301.htm";
				} else {
					string2 = "eye_of_argos_q0604_0302.htm";
				}
			}
		} else if (n3 == 31541) {
			if (event.equalsIgnoreCase("reply_1") && n2 == 1) {
				if (qs.getQuestItemsCount(7193) >= 1L) {
					if (n == 0) {
						qs.setCond(2);
						qs.set("daemon_of_hundred_eyes_second", String.valueOf(21), true);
						qs.takeItems(7193, 1L);
						string2 = "daimons_altar_q0604_0201.htm";
						qs.set("spawned_daemon_of_hundred_eyes", String.valueOf(1), true);
						NpcInstance npcInstance2 = qs.addSpawn(25290, 186320, -43904, -3175);
						// npcSay
						qs.startQuestTimer("60401", 1200000L, npcInstance2);
						qs.playSound(QuestState.SOUND_MIDDLE);
						string2 = "daimons_altar_q0604_0201.htm";
					} else {
						string2 = "daimons_altar_q0604_0202.htm";
					}
				} else {
					string2 = "daimons_altar_q0604_0203.htm";
				}
			}
		}
		return string2;
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		return onAdvEvent(event, null, qs != null ? qs.getPlayer() : null);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("daemon_of_hundred_eyes_second");
        int n2 = qs.getInt("spawned_daemon_of_hundred_eyes");
        int n3 = npc.getNpcId();
        int n4 = qs.getStateId();
        switch (n4) {
            case 1: {
                if (n3 != 31683) break;
                if (pc.getLevel() >= 73) {
                    if (qs.getQuestItemsCount(7192) >= 1L) {
                        html = "eye_of_argos_q0604_0101.htm";
                        break;
                    }
                    html = "eye_of_argos_q0604_0102.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "eye_of_argos_q0604_0103.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n3 == 31683) {
                    if (n == 11) {
                        html = "eye_of_argos_q0604_0105.htm";
                        break;
                    }
                    if (n < 22) break;
                    if (qs.getQuestItemsCount(7194) >= 1L) {
                        qs.set("daemon_of_hundred_eyes_second_cookie", String.valueOf(2), true);
                        html = "eye_of_argos_q0604_0201.htm";
                        break;
                    }
                    html = "eye_of_argos_q0604_0202.htm";
                    break;
                }
                if (n3 != 31541) break;
                if (qs.getQuestItemsCount(7193) >= 1L && n == 11) {
                    qs.set("daemon_of_hundred_eyes_second_cookie", String.valueOf(1), true);
                    html = "daimons_altar_q0604_0101.htm";
                    break;
                }
                if (n == 21) {
                    if (n2 == 0) {
                        qs.set("spawned_daemon_of_hundred_eyes", String.valueOf(1), true);
                        NpcInstance npcInstance2 = qs.addSpawn(25290, 186320, -43904, -3175);
                        // npcSay
                        qs.startQuestTimer("60401", 1200000L, npcInstance2);
                        html = "daimons_altar_q0604_0201.htm";
                        break;
                    }
                    html = "daimons_altar_q0604_0202.htm";
                    break;
                }
                if (n < 22) break;
                html = "daimons_altar_q0604_0204.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        int n2 = qs.getInt("daemon_of_hundred_eyes_second");
        int n3 = npc.getNpcId();
        if (n2 >= 11 && n2 <= 21 && n3 == 25290 && (n = ThreadLocalRandom.current().nextInt(1000)) < 1000) {
            if (qs.getQuestItemsCount(7194) + 1L >= 1L) {
                qs.setCond(3);
                qs.set("daemon_of_hundred_eyes_second", String.valueOf(22), true);
                qs.set("spawned_daemon_of_hundred_eyes", String.valueOf(0), true);
                qs.giveItems(7194, 1L - qs.getQuestItemsCount(7194));
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.giveItems(7194, 1L);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
