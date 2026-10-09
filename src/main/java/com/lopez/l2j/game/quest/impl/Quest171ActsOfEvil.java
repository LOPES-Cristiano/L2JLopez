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
 * Quest 171 - Acts Of Evil
 */
@Component
public class Quest171ActsOfEvil extends Quest {



	public Quest171ActsOfEvil(QuestManager questManager) {
		super(171, "171_ActsOfEvil", "Acts Of Evil");
		addStartNpc(30381);
		addTalkId(30207, 30420, 30437, 30425, 30617);
		addKillId(20062, 20438, 27190, 20496, 20497, 20498, 20499, 20066);
		registerQuestItems(4239, 4240, 4241, 4242, 4243, 4244, 4245, 4246, 4247, 4248, 4249);
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
        int n = 0;
        if (n == 30381) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "guard_alvah_q0171_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                qs.setCond(5);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "guard_alvah_q0171_07.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                qs.setCond(7);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "guard_alvah_q0171_12.htm";
            }
        } else if (n == 30437) {
            if (event.equalsIgnoreCase("reply_1")) {
                string2 = "trader_rolento_q0171_02.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "trader_rolento_q0171_03.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                qs.giveItems((int)(4247), (int)(1));
                qs.giveItems((int)(4248), (int)(1));
                qs.takeItems((int)(4245), (int)(qs.getQuestItemsCount((int)(4245))));
                qs.setCond(9);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "trader_rolento_q0171_04.htm";
            }
        } else if (n == 30617) {
            if (event.equalsIgnoreCase("reply_1")) {
                string2 = "turek_chief_burai_q0171_03.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "turek_chief_burai_q0171_04.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                qs.takeItems((int)(4246), (int)(qs.getQuestItemsCount((int)(4246))));
                qs.takeItems((int)(4247), (int)(qs.getQuestItemsCount((int)(4247))));
                qs.takeItems((int)(4248), (int)(qs.getQuestItemsCount((int)(4248))));
                qs.setCond(10);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "turek_chief_burai_q0171_05.htm";
            }
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "no-quest";
        int n = npc.getNpcId();
        int n2 = qs.getStateId();
        int n3 = qs.getCond();
        PlayerCharacter player = pc;
        switch (n2) {
            case 1: {
                if (n != 30381) break;
                if (pc.level() < 27) {
                    html = "guard_alvah_q0171_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.level() < 27) break;
                html = "guard_alvah_q0171_02.htm";
                break;
            }
            case 2: {
                if (n == 30381) {
                    if (n3 == 1) {
                        html = "guard_alvah_q0171_04.htm";
                        break;
                    }
                    if (n3 == 2 || n3 == 3) {
                        html = "guard_alvah_q0171_05.htm";
                        break;
                    }
                    if (n3 == 4) {
                        html = "guard_alvah_q0171_06.htm";
                        break;
                    }
                    if (n3 == 5 && (qs.getQuestItemsCount((int)(4241)) == 0 || qs.getQuestItemsCount((int)(4242)) == 0 || qs.getQuestItemsCount((int)(4243)) == 0 || qs.getQuestItemsCount((int)(4244)) == 0)) {
                        html = "guard_alvah_q0171_08.htm";
                        break;
                    }
                    if (n3 == 5 && qs.getQuestItemsCount((int)(4241)) >= 1 && qs.getQuestItemsCount((int)(4242)) >= 1 && qs.getQuestItemsCount((int)(4243)) >= 1 && qs.getQuestItemsCount((int)(4244)) >= 1) {
                        qs.takeItems((int)(4241), (int)(qs.getQuestItemsCount((int)(4241))));
                        qs.takeItems((int)(4242), (int)(qs.getQuestItemsCount((int)(4242))));
                        qs.takeItems((int)(4243), (int)(qs.getQuestItemsCount((int)(4243))));
                        qs.takeItems((int)(4244), (int)(qs.getQuestItemsCount((int)(4244))));
                        qs.setCond(6);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "guard_alvah_q0171_09.htm";
                        break;
                    }
                    if (n3 == 6 && (qs.getQuestItemsCount((int)(4245)) == 0 || qs.getQuestItemsCount((int)(4246)) == 0)) {
                        html = "guard_alvah_q0171_10.htm";
                        break;
                    }
                    if (n3 == 6 && qs.getQuestItemsCount((int)(4245)) >= 1 && qs.getQuestItemsCount((int)(4246)) >= 1) {
                        html = "guard_alvah_q0171_11.htm";
                        break;
                    }
                    if (n3 == 7) {
                        html = "guard_alvah_q0171_13.htm";
                        break;
                    }
                    if (n3 == 8) {
                        html = "guard_alvah_q0171_14.htm";
                        break;
                    }
                    if (n3 == 9) {
                        html = "guard_alvah_q0171_15.htm";
                        break;
                    }
                    if (n3 == 10) {
                        html = "guard_alvah_q0171_16.htm";
                        break;
                    }
                    if (n3 != 11) break;
                    qs.giveItems((int)(57), (int)(95000));
                    qs.playSound(QuestState.SOUND_FINISH);
                    html = "guard_alvah_q0171_17.htm";
                    qs.exitQuest(false);
                    break;
                }
                if (n == 30420) {
                    if (n3 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
                        html = "tweety_q0171_01.htm";
                        break;
                    }
                    if (n3 == 2 && qs.getQuestItemsCount((int)(4239)) >= 20) {
                        qs.giveItems((int)(4240), (int)(1));
                        qs.takeItems((int)(4239), (int)(qs.getQuestItemsCount((int)(4239))));
                        qs.setCond(3);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "tweety_q0171_02.htm";
                        break;
                    }
                    if (n3 == 3) {
                        html = "tweety_q0171_03.htm";
                        break;
                    }
                    if (n3 < 4) break;
                    html = "tweety_q0171_04.htm";
                    break;
                }
                if (n == 30207) {
                    if (n3 == 1) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "trader_arodin_q0171_01.htm";
                        break;
                    }
                    if (n3 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
                        html = "trader_arodin_q0171_02.htm";
                        break;
                    }
                    if (n3 == 2 && qs.getQuestItemsCount((int)(4239)) >= 20) {
                        html = "trader_arodin_q0171_03.htm";
                        break;
                    }
                    if (n3 == 3) {
                        qs.takeItems((int)(4240), (int)(qs.getQuestItemsCount((int)(4240))));
                        qs.setCond(4);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "trader_arodin_q0171_04.htm";
                        break;
                    }
                    if (n3 < 4) break;
                    html = "trader_arodin_q0171_05.htm";
                    break;
                }
                if (n == 30437) {
                    if (n3 == 8) {
                        html = "trader_rolento_q0171_02.htm";
                        break;
                    }
                    if (n3 == 9) {
                        html = "trader_rolento_q0171_05.htm";
                        break;
                    }
                    if (n3 < 10) break;
                    html = "trader_rolento_q0171_06.htm";
                    break;
                }
                if (n == 30425) {
                    if (n3 == 7) {
                        qs.setCond(8);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "neti_q0171_01.htm";
                        break;
                    }
                    if (n3 == 8) {
                        html = "neti_q0171_02.htm";
                        break;
                    }
                    if (n3 < 9) break;
                    html = "neti_q0171_03.htm";
                    break;
                }
                if (n != 30617) break;
                if (n3 < 9) {
                    html = "turek_chief_burai_q0171_01.htm";
                    break;
                }
                if (n3 == 9) {
                    html = "turek_chief_burai_q0171_02.htm";
                    break;
                }
                if (n3 == 10 && qs.getQuestItemsCount((int)(4249)) < 30) {
                    html = "turek_chief_burai_q0171_06.htm";
                    break;
                }
                if (n3 == 10 && qs.getQuestItemsCount((int)(4249)) >= 30) {
                    qs.giveItems((int)(57), (int)(8000));
                    qs.takeItems((int)(4249), (int)(qs.getQuestItemsCount((int)(4249))));
                    qs.setCond(11);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                    html = "turek_chief_burai_q0171_07.htm";
                    break;
                }
                if (n3 != 11) break;
                html = "turek_chief_burai_q0171_08.htm";
            }
        }
        return html;
    
	}


	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc.getNpcId();
        int n2 = qs.getCond();
        if (n == 20496 && n2 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
            NpcInstance npc2;
            if (ThreadLocalRandom.current().nextInt(100) < 53) {
                qs.rollAndGive(4239, 1, 100.0);
                if (qs.getQuestItemsCount((int)(4239)) >= 19) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            }
            if (qs.getQuestItemsCount((int)(4239)) == 5) {
                npc2 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
            if (qs.getQuestItemsCount((int)(4239)) >= 10 && ThreadLocalRandom.current().nextInt(100) <= 24) {
                npc2 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
        } else if (n == 20497 && n2 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
            NpcInstance npc3;
            if (ThreadLocalRandom.current().nextInt(100) < 55) {
                qs.rollAndGive(4239, 1, 100.0);
                if (qs.getQuestItemsCount((int)(4239)) >= 19) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            }
            if (qs.getQuestItemsCount((int)(4239)) == 5) {
                npc3 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
            if (qs.getQuestItemsCount((int)(4239)) >= 10 && ThreadLocalRandom.current().nextInt(100) <= 24) {
                npc3 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
        } else if (n == 20498 && n2 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
            NpcInstance npc4;
            if (ThreadLocalRandom.current().nextInt(100) < 51) {
                qs.rollAndGive(4239, 1, 100.0);
                if (qs.getQuestItemsCount((int)(4239)) >= 19) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            }
            if (qs.getQuestItemsCount((int)(4239)) == 5) {
                npc4 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
            if (qs.getQuestItemsCount((int)(4239)) >= 10 && ThreadLocalRandom.current().nextInt(100) <= 24) {
                npc4 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
        } else if (n == 20499 && n2 == 2 && qs.getQuestItemsCount((int)(4239)) < 20) {
            NpcInstance npc5;
            if (ThreadLocalRandom.current().nextInt(2) == 1) {
                qs.rollAndGive(4239, 1, 100.0);
                if (qs.getQuestItemsCount((int)(4239)) >= 19) {
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
            }
            if (qs.getQuestItemsCount((int)(4239)) == 5) {
                npc5 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
            if (qs.getQuestItemsCount((int)(4239)) >= 10 && ThreadLocalRandom.current().nextInt(100) <= 24) {
                npc5 = qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
            }
        } else if (n == 20062 && n2 == 5) {
            if (qs.getQuestItemsCount((int)(4241)) == 0) {
                qs.rollAndGive(4241, 1, 100.0);
            } else if (qs.getQuestItemsCount((int)(4241)) >= 1 && qs.getQuestItemsCount((int)(4242)) == 0) {
                if (ThreadLocalRandom.current().nextInt(100) <= 19) {
                    qs.rollAndGive(4242, 1, 100.0);
                }
            } else if (qs.getQuestItemsCount((int)(4241)) >= 1 && qs.getQuestItemsCount((int)(4242)) >= 1 && qs.getQuestItemsCount((int)(4243)) == 0) {
                if (ThreadLocalRandom.current().nextInt(100) <= 19) {
                    qs.rollAndGive(4243, 1, 100.0);
                }
            } else if (qs.getQuestItemsCount((int)(4241)) >= 1 && qs.getQuestItemsCount((int)(4242)) >= 1 && qs.getQuestItemsCount((int)(4243)) >= 1 && qs.getQuestItemsCount((int)(4244)) == 0 && ThreadLocalRandom.current().nextInt(100) <= 19) {
                qs.rollAndGive(4244, 1, 100.0);
            }
        } else if (n == 20438 && n2 == 6) {
            if (ThreadLocalRandom.current().nextInt(100) <= 9) {
                if (qs.getQuestItemsCount((int)(4245)) == 0) {
                    qs.rollAndGive(4245, 1, 100.0);
                }
                if (qs.getQuestItemsCount((int)(4246)) == 0) {
                    qs.rollAndGive(4246, 1, 100.0);
                }
            }
        } else if (n == 20066 && n2 == 10 && qs.getQuestItemsCount((int)(4249)) < 30 && ThreadLocalRandom.current().nextInt(100) <= 49) {
            qs.rollAndGive(4249, 1, 100.0);
            if (qs.getQuestItemsCount((int)(4249)) >= 29) {
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
        }
        return null;
    
	}

}
