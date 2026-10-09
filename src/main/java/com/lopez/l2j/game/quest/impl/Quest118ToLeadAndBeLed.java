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
 * Quest 118 - To Lead And Be Led
 */
@Component
public class Quest118ToLeadAndBeLed extends Quest {



	public Quest118ToLeadAndBeLed(QuestManager questManager) {
		super(118, "118_ToLeadAndBeLed", "To Lead And Be Led");
		addStartNpc(30298);
		addKillId(20927, 20919, 20921, 20920);
		registerQuestItems(8062, 8063);
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
        int n = qs.getInt("to_lead_and_be_led");
        QuestState qs2 = null;
        if (event.equalsIgnoreCase("quest_accept") && (qs2 == null || !qs2.isStarted()) && pc.level() >= 19 && 1 > 0) {
            qs.setCond(1);
            qs.set("to_lead_and_be_led", String.valueOf(1), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "blacksmith_pinter_q0118_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            int n2;
            PlayerCharacter player2 = pc;
            if (1 > 0 && (player2 = pc) != null && true && true) {
                if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 1 && qs.getQuestItemsCount((int)(1458)) >= 922) {
                    qs.takeItems((int)(1458), (int)(922));
                    qs.set("to_lead_and_be_led", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "blacksmith_pinter_q0118_10.htm";
                } else if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 2 && qs.getQuestItemsCount((int)(1458)) >= 771) {
                    qs.takeItems((int)(1458), (int)(771));
                    qs.set("to_lead_and_be_led", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "blacksmith_pinter_q0118_10.htm";
                } else if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 3 && qs.getQuestItemsCount((int)(1458)) >= 771) {
                    qs.takeItems((int)(1458), (int)(771));
                    qs.set("to_lead_and_be_led", String.valueOf(3), true);
                    qs.setCond(6);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    string2 = "blacksmith_pinter_q0118_10.htm";
                } else if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 1 && qs.getQuestItemsCount((int)(1458)) < 922) {
                    string2 = "blacksmith_pinter_q0118_11.htm";
                } else if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 2 && qs.getQuestItemsCount((int)(1458)) < 771) {
                    string2 = "blacksmith_pinter_q0118_11a.htm";
                } else if (qs.getInt("to_lead_and_be_led") == 2 && qs.getInt("to_lead_and_be_led_ex") == 3 && qs.getQuestItemsCount((int)(1458)) < 771) {
                    string2 = "blacksmith_pinter_q0118_11a.htm";
                }
            }
        } else if (event.equalsIgnoreCase("reply_21")) {
            string2 = "blacksmith_pinter_q0118_05a.htm";
        } else if (event.equalsIgnoreCase("reply_22")) {
            string2 = "blacksmith_pinter_q0118_05b.htm";
        } else if (event.equalsIgnoreCase("reply_23")) {
            string2 = "blacksmith_pinter_q0118_05c.htm";
        } else if (event.equalsIgnoreCase("reply_24")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8062)) >= 10) {
                qs.takeItems((int)(8062), (int)(qs.getQuestItemsCount((int)(8062))));
                qs.set("to_lead_and_be_led", String.valueOf(2), true);
                qs.set("to_lead_and_be_led_ex", String.valueOf(1), true);
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "blacksmith_pinter_q0118_05d.htm";
            }
        } else if (event.equalsIgnoreCase("reply_25")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8062)) >= 10) {
                qs.takeItems((int)(8062), (int)(qs.getQuestItemsCount((int)(8062))));
                qs.set("to_lead_and_be_led", String.valueOf(2), true);
                qs.set("to_lead_and_be_led_ex", String.valueOf(2), true);
                qs.setCond(4);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "blacksmith_pinter_q0118_05e.htm";
            }
        } else if (event.equalsIgnoreCase("reply_26")) {
            if (n == 1 && qs.getQuestItemsCount((int)(8062)) >= 10) {
                qs.takeItems((int)(8062), (int)(qs.getQuestItemsCount((int)(8062))));
                qs.set("to_lead_and_be_led", String.valueOf(2), true);
                qs.set("to_lead_and_be_led_ex", String.valueOf(3), true);
                qs.setCond(5);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "blacksmith_pinter_q0118_05f.htm";
            }
        } else if (event.equalsIgnoreCase("reply_27")) {
            string2 = "blacksmith_pinter_q0118_05g.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        PlayerCharacter player = pc;
        int n = qs.getInt("to_lead_and_be_led");
        int n2 = qs.getInt("to_lead_and_be_led_ex");
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
                        html = "blacksmith_pinter_q0118_09.htm";
                        break;
                    }
                    if (!(true)) break;
                    if (qs.getInt("to_lead_and_be_led") == 2) {
                        html = "blacksmith_pinter_q0118_08.htm";
                        break;
                    }
                    if (qs.getInt("to_lead_and_be_led") == 3) {
                        html = "blacksmith_pinter_q0118_12.htm";
                        break;
                    }
                    html = "blacksmith_pinter_q0118_4.htm";
                    break;
                }
                if (qs2 == null && pc.level() >= 19 && 1 > 0 && player2 != null && true) {
                    html = "blacksmith_pinter_q0118_01.htm";
                    break;
                }
                if (!(qs2 != null || pc.level() >= 19 && 1 != 0 && player2 != null && true)) {
                    html = "blacksmith_pinter_q0118_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs2 != null && qs2.isStarted()) {
                    html = "blacksmith_pinter_q0118_02b.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (qs2 == null || !qs2.isCompleted()) break;
                html = "blacksmith_pinter_q0118_02a.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n == 1 && qs.getQuestItemsCount((int)(8062)) < 10) {
                    html = "blacksmith_pinter_q0118_04.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount((int)(8062)) >= 10) {
                    html = "blacksmith_pinter_q0118_05.htm";
                    break;
                }
                if (n == 2) {
                    if (1 == 0) {
                        if (n2 == 1) {
                            html = "blacksmith_pinter_q0118_06a.htm";
                            break;
                        }
                        if (n2 == 2) {
                            html = "blacksmith_pinter_q0118_06b.htm";
                            break;
                        }
                        if (n2 != 3) break;
                        html = "blacksmith_pinter_q0118_06c.htm";
                        break;
                    }
                    if (player2 == null || !true || true) {
                        if (n2 == 1) {
                            html = "blacksmith_pinter_q0118_06.htm";
                            break;
                        }
                        if (n2 == 2) {
                            html = "blacksmith_pinter_q0118_06d.htm";
                            break;
                        }
                        if (n2 != 3) break;
                        html = "blacksmith_pinter_q0118_06e.htm";
                        break;
                    }
                    if (!(false)) break;
                    html = "blacksmith_pinter_q0118_07.htm";
                    break;
                }
                if (1 > 0) {
                    if (player3 == null || false) {
                        html = "blacksmith_pinter_q0118_09.htm";
                        break;
                    }
                    if (!(true)) break;
                    if (qs.getInt("to_lead_and_be_led") == 2) {
                        html = "blacksmith_pinter_q0118_08.htm";
                        break;
                    }
                    if (qs.getInt("to_lead_and_be_led") == 3) {
                        html = "blacksmith_pinter_q0118_12.htm";
                        break;
                    }
                    html = "blacksmith_pinter_q0118_14.htm";
                    break;
                }
                if (n == 3) {
                    qs.set("to_lead_and_be_led", String.valueOf(4), true);
                    qs.setCond(7);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "blacksmith_pinter_q0118_15.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount((int)(8063)) < 8) {
                    html = "blacksmith_pinter_q0118_16.htm";
                    break;
                }
                if (n != 4 || qs.getQuestItemsCount((int)(8063)) < 8) break;
                if (n2 == 1) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7851), (int)(1));
                    qs.giveItems((int)(7852), (int)(1));
                    qs.giveItems((int)(7853), (int)(1));
                    qs.takeItems((int)(8063), (int)(qs.getQuestItemsCount((int)(8063))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                } else if (n2 == 2) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7854), (int)(1));
                    qs.giveItems((int)(7855), (int)(1));
                    qs.giveItems((int)(7856), (int)(1));
                    qs.takeItems((int)(8063), (int)(qs.getQuestItemsCount((int)(8063))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                } else if (n2 == 3) {
                    qs.giveItems((int)(7850), (int)(1));
                    qs.giveItems((int)(7857), (int)(1));
                    qs.giveItems((int)(7858), (int)(1));
                    qs.giveItems((int)(7859), (int)(1));
                    qs.takeItems((int)(8063), (int)(qs.getQuestItemsCount((int)(8063))));
                    qs.exitQuest(false);
                    qs.playSound(QuestState.SOUND_FINISH);
                }
                html = "blacksmith_pinter_q0118_17.htm";
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
        int n3 = qs.getInt("to_lead_and_be_led");
        if ((n2 == 20919 || n2 == 20921 || n2 == 20920) && ThreadLocalRandom.current().nextInt(10) < 7 && n3 == 1 && qs.getQuestItemsCount((int)(8062)) < 10) {
            qs.giveItems((int)(8062), (int)(1));
            if (qs.getQuestItemsCount((int)(8062)) >= 10) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 20927 && n3 == 4 && qs.getQuestItemsCount((int)(8063)) < 8 && ThreadLocalRandom.current().nextInt(10) < 7 && 1 > 0 && (player = pc) != null && true && true) {
            qs.giveItems((int)(8063), (int)(1));
            if (qs.getQuestItemsCount((int)(8063)) >= 7) {
                qs.playSound(QuestState.SOUND_MIDDLE);
                qs.setCond(8);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
