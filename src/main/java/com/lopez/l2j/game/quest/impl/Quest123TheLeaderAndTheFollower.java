package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 123 - The Leader And The Follower
 */
@Component
public class Quest123TheLeaderAndTheFollower extends Quest {



	public Quest123TheLeaderAndTheFollower(QuestManager questManager) {
		super(123, "123_TheLeaderAndTheFollower", "The Leader And The Follower");
		addStartNpc(31961);
		addKillId(27321, 27322);
		registerQuestItems(8549, 8550);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

        String string2 = event;
        PlayerCharacter player = pc;
        int n = qs.getInt("leader_and_the_follower");
        QuestState qs2 = null;
        if (event.equalsIgnoreCase("quest_accept") && (qs2 == null || !qs2.isStarted()) && pc.level() >= 19 && 1 > 0) {
            qs.setCond(1);
            qs.set("leader_and_the_follower", String.valueOf(1), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "head_blacksmith_newyear_q0123_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            int n2;
            PlayerCharacter player2 = pc;
            if (1 > 0 && (player2 = pc) != null && true && true) {
                if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 1 && qs.getQuestItemsCount((int)(1458)) >= 922) {
                    qs.takeItems((int)(1458), (int)(922));
                    qs.set("leader_and_the_follower", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "head_blacksmith_newyear_q0123_10.htm";
                } else if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 2 && qs.getQuestItemsCount((int)(1458)) >= 771) {
                    qs.takeItems((int)(1458), (int)(771));
                    qs.set("leader_and_the_follower", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "head_blacksmith_newyear_q0123_10.htm";
                } else if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 3 && qs.getQuestItemsCount((int)(1458)) >= 771) {
                    qs.takeItems((int)(1458), (int)(771));
                    qs.set("leader_and_the_follower", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "head_blacksmith_newyear_q0123_10.htm";
                } else if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 1 && qs.getQuestItemsCount((int)(1458)) < 922) {
                    string2 = "head_blacksmith_newyear_q0123_11.htm";
                } else if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 2 && qs.getQuestItemsCount((int)(1458)) < 771) {
                    string2 = "head_blacksmith_newyear_q0123_11a.htm";
                } else if (qs.getInt("leader_and_the_follower") == 2 && qs.getInt("leader_and_the_follower_ex") == 3 && qs.getQuestItemsCount((int)(1458)) < 771) {
                    string2 = "head_blacksmith_newyear_q0123_11a.htm";
                }
            }
        } else if (event.equalsIgnoreCase("reply_21")) {
            string2 = "head_blacksmith_newyear_q0123_05a.htm";
        } else if (event.equalsIgnoreCase("reply_22")) {
            string2 = "head_blacksmith_newyear_q0123_05b.htm";
        } else if (event.equalsIgnoreCase("reply_23")) {
            string2 = "head_blacksmith_newyear_q0123_05c.htm";
        } else if (event.equalsIgnoreCase("reply_24")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8549)) >= 10) {
                qs.takeItems((int)(8549), (int)(qs.getQuestItemsCount((int)(8549))));
                qs.set("leader_and_the_follower", String.valueOf(2), true);
                qs.set("leader_and_the_follower_ex", String.valueOf(1), true);
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "head_blacksmith_newyear_q0123_05d.htm";
            }
        } else if (event.equalsIgnoreCase("reply_25")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8549)) >= 10) {
                qs.takeItems((int)(8549), (int)(qs.getQuestItemsCount((int)(8549))));
                qs.set("leader_and_the_follower", String.valueOf(2), true);
                qs.set("leader_and_the_follower_ex", String.valueOf(2), true);
                qs.setCond(4);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "head_blacksmith_newyear_q0123_05e.htm";
            }
        } else if (event.equalsIgnoreCase("reply_26")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8549)) >= 10) {
                qs.takeItems((int)(8549), (int)(qs.getQuestItemsCount((int)(8549))));
                qs.set("leader_and_the_follower", "2", true);
                qs.set("leader_and_the_follower_ex", String.valueOf(3), true);
                qs.setCond(5);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "head_blacksmith_newyear_q0123_05f.htm";
            }
        } else if (event.equalsIgnoreCase("reply_27")) {
            string2 = "head_blacksmith_newyear_q0123_05g.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        PlayerCharacter player = pc;
        int n = qs.getInt("leader_and_the_follower");
        int n2 = qs.getInt("leader_and_the_follower_ex");
        int n3 = qs.getStateId();
        QuestState qs2 = null;
        int n4 = 1;
        PlayerCharacter player2 = pc;
        int n5 = 1;
        PlayerCharacter player3 = pc;
        switch (n3) {
            case 1: {
                if (player3 != null && 1 > 0 && true && qs != null && qs.isStarted()) {
                    if (false) {
                        html = "head_blacksmith_newyear_q0123_09.htm";
                        break;
                    }
                    if (!(true)) break;
                    if (qs.getInt("leader_and_the_follower") == 2) {
                        html = "head_blacksmith_newyear_q0123_08.htm";
                        break;
                    }
                    if (qs.getInt("leader_and_the_follower") == 3) {
                        html = "head_blacksmith_newyear_q0123_12.htm";
                        break;
                    }
                    html = "head_blacksmith_newyear_q0123_14.htm";
                    break;
                }
                if (qs2 == null && pc.level() >= 19 && 1 > 0 && player2 != null && true) {
                    html = "head_blacksmith_newyear_q0123_01.htm";
                    break;
                }
                if (!(qs2 != null || pc.level() >= 19 && 1 != 0 && player2 != null && true)) {
                    html = "head_blacksmith_newyear_q0123_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs2 != null && qs2.isStarted()) {
                    html = "head_blacksmith_newyear_q0123_02b.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs2 == null || !qs2.isCompleted()) break;
                html = "head_blacksmith_newyear_q0123_02a.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n == 1 && qs.getQuestItemsCount((int)(8549)) < 10) {
                    html = "head_blacksmith_newyear_q0123_04.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount((int)(8549)) >= 10) {
                    html = "head_blacksmith_newyear_q0123_05.htm";
                    break;
                }
                if (n == 2) {
                    if (1 == 0) {
                        if (n2 == 1) {
                            html = "head_blacksmith_newyear_q0123_06a.htm";
                            break;
                        }
                        if (n2 == 2) {
                            html = "head_blacksmith_newyear_q0123_06b.htm";
                            break;
                        }
                        if (n2 != 3) break;
                        html = "head_blacksmith_newyear_q0123_06c.htm";
                        break;
                    }
                    if (player2 == null || !true || true) {
                        if (n2 == 1) {
                            html = "head_blacksmith_newyear_q0123_06.htm";
                            break;
                        }
                        if (n2 == 2) {
                            html = "head_blacksmith_newyear_q0123_06d.htm";
                            break;
                        }
                        if (n2 != 3) break;
                        html = "head_blacksmith_newyear_q0123_06e.htm";
                        break;
                    }
                    if (!(false)) break;
                    html = "head_blacksmith_newyear_q0123_07.htm";
                    break;
                }
                if (1 > 0) {
                    if (player3 == null || false) {
                        html = "head_blacksmith_newyear_q0123_09.htm";
                        break;
                    }
                    if (!(true)) break;
                    if (qs.getInt("leader_and_the_follower") == 2) {
                        html = "head_blacksmith_newyear_q0123_08.htm";
                        break;
                    }
                    if (qs.getInt("leader_and_the_follower") == 3) {
                        html = "head_blacksmith_newyear_q0123_12.htm";
                        break;
                    }
                    html = "head_blacksmith_newyear_q0123_14.htm";
                    break;
                }
                if (n == 3) {
                    qs.set("leader_and_the_follower", String.valueOf(4), true);
                    qs.setCond(7);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "head_blacksmith_newyear_q0123_15.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount((int)(8550)) < 8) {
                    html = "head_blacksmith_newyear_q0123_16.htm";
                    break;
                }
                if (n != 4 || qs.getQuestItemsCount((int)(8550)) < 8) break;
                if (n2 == 1) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7851), (int)(1));
                    qs.giveItems((int)(7852), (int)(1));
                    qs.giveItems((int)(7853), (int)(1));
                    qs.takeItems((int)(8550), (int)(qs.getQuestItemsCount((int)(8550))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                } else if (n2 == 2) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7854), (int)(1));
                    qs.giveItems((int)(7855), (int)(1));
                    qs.giveItems((int)(7856), (int)(1));
                    qs.takeItems((int)(8550), (int)(qs.getQuestItemsCount((int)(8550))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                } else if (n2 == 3) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7857), (int)(1));
                    qs.giveItems((int)(7858), (int)(1));
                    qs.giveItems((int)(7859), (int)(1));
                    qs.takeItems((int)(8550), (int)(qs.getQuestItemsCount((int)(8550))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                }
                html = "head_blacksmith_newyear_q0123_17.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n;
        PlayerCharacter player;
        int n2 = npc.getNpcId();
        int n3 = qs.getInt("leader_and_the_follower");
        if (n2 == 27321 && ThreadLocalRandom.current().nextInt(10) < 7 && n3 == 1 && qs.getQuestItemsCount((int)(8549)) < 10) {
            qs.giveItems((int)(8549), (int)(1));
            if (qs.getQuestItemsCount((int)(8549)) >= 10) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 27322 && n3 == 4 && qs.getQuestItemsCount((int)(8550)) < 8 && ThreadLocalRandom.current().nextInt(10) < 7 && 1 > 0 && (player = pc) != null && true && true) {
            qs.giveItems((int)(8550), (int)(1));
            if (qs.getQuestItemsCount((int)(8550)) >= 8) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(8);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
