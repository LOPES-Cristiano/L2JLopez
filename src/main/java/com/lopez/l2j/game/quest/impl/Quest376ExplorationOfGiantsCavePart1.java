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
 * Quest 376 - 376_ExplorationOfGiantsCavePart1
 */
@Component
public class Quest376ExplorationOfGiantsCavePart1 extends Quest {

	public static final int bwk = 31147;
	public static final int aZF = 30182;
	public static final int bwl = 20647;
	public static final int bwm = 20648;
	public static final int bwn = 20649;
	public static final int bwo = 20650;
	public static final int bwp = 5922;
	public static final int bwq = 5923;
	public static final int bwr = 5924;
	public static final int bws = 5925;
	public static final int bwt = 5926;
	public static final int bwu = 5927;
	public static final int bwv = 5928;
	public static final int bww = 5929;
	public static final int bwx = 5930;
	public static final int bwy = 5931;
	public static final int bwz = 5932;
	public static final int bwA = 5933;
	public static final int bwB = 5934;
	public static final int bwC = 5935;
	public static final int bwD = 5936;
	public static final int bwE = 5937;
	public static final int bwF = 5938;
	public static final int bwG = 5939;
	public static final int bwH = 5940;
	public static final int bwI = 5941;
	public static final int bwJ = 5891;
	public static final int bwK = 5890;
	public static final int bwL = 5944;
	public static final int bwM = 5354;
	public static final int bwN = 5346;
	public static final int bwO = 5416;
	public static final int bwP = 5418;
	public static final int bwQ = 5424;
	public static final int bwR = 5340;
	public static final int bwS = 5332;
	public static final int bwT = 5334;

	public Quest376ExplorationOfGiantsCavePart1(QuestManager questManager) {
	super(376, "376_ExplorationOfGiantsCavePart1", "376_ExplorationOfGiantsCavePart1");
		this.addStartNpc(31147);
		this.addTalkId(31147, 30182);
		this.addKillId(20647, 20648, 20649, 20650);
		this.addQuestItem(5891, 5890);
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
            qs.giveItems(5891, 1L);
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "sobling_q0376_03.htm";
        } else if (event.equalsIgnoreCase("reply_1") && qs.getQuestItemsCount(5922) >= 1L && qs.getQuestItemsCount(5923) >= 1L && qs.getQuestItemsCount(5924) >= 1L && qs.getQuestItemsCount(5925) >= 1L && qs.getQuestItemsCount(5926) >= 1L || qs.getQuestItemsCount(5927) >= 1L && qs.getQuestItemsCount(5928) >= 1L && qs.getQuestItemsCount(5929) >= 1L && qs.getQuestItemsCount(5930) >= 1L && qs.getQuestItemsCount(5931) >= 1L || qs.getQuestItemsCount(5932) >= 1L && qs.getQuestItemsCount(5933) >= 1L && qs.getQuestItemsCount(5934) >= 1L && qs.getQuestItemsCount(5935) >= 1L && qs.getQuestItemsCount(5936) >= 1L || qs.getQuestItemsCount(5937) >= 1L && qs.getQuestItemsCount(5938) >= 1L && qs.getQuestItemsCount(5939) >= 1L && qs.getQuestItemsCount(5940) >= 1L && qs.getQuestItemsCount(5941) >= 1L) {
            if (qs.getQuestItemsCount(5922) > 0L && qs.getQuestItemsCount(5923) > 0L && qs.getQuestItemsCount(5924) > 0L && qs.getQuestItemsCount(5925) > 0L && qs.getQuestItemsCount(5926) > 0L) {
                qs.takeItems(5922, 1L);
                qs.takeItems(5923, 1L);
                qs.takeItems(5924, 1L);
                qs.takeItems(5925, 1L);
                qs.takeItems(5926, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5416, 1L);
                } else {
                    qs.giveItems(5418, 1L);
                }
            }
            if (qs.getQuestItemsCount(5927) > 0L && qs.getQuestItemsCount(5928) > 0L && qs.getQuestItemsCount(5929) > 0L && qs.getQuestItemsCount(5930) > 0L && qs.getQuestItemsCount(5931) > 0L) {
                qs.takeItems(5927, 1L);
                qs.takeItems(5928, 1L);
                qs.takeItems(5929, 1L);
                qs.takeItems(5930, 1L);
                qs.takeItems(5931, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5424, 1L);
                } else {
                    qs.giveItems(5340, 1L);
                }
            }
            if (qs.getQuestItemsCount(5932) > 0L && qs.getQuestItemsCount(5933) > 0L && qs.getQuestItemsCount(5934) > 0L && qs.getQuestItemsCount(5935) > 0L && qs.getQuestItemsCount(5936) > 0L) {
                qs.takeItems(5932, 1L);
                qs.takeItems(5933, 1L);
                qs.takeItems(5934, 1L);
                qs.takeItems(5935, 1L);
                qs.takeItems(5936, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5332, 1L);
                } else {
                    qs.giveItems(5334, 1L);
                }
            }
            if (qs.getQuestItemsCount(5937) > 0L && qs.getQuestItemsCount(5938) > 0L && qs.getQuestItemsCount(5939) > 0L && qs.getQuestItemsCount(5940) > 0L && qs.getQuestItemsCount(5941) > 0L) {
                qs.takeItems(5937, 1L);
                qs.takeItems(5938, 1L);
                qs.takeItems(5939, 1L);
                qs.takeItems(5940, 1L);
                qs.takeItems(5941, 1L);
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    qs.giveItems(5354, 1L);
                } else {
                    qs.giveItems(5346, 1L);
                }
            }
            string2 = "sobling_q0376_05.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "sobling_q0376_04.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            string2 = "sobling_q0376_06.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            qs.takeItems(5891, 1L);
            qs.exitQuest(true);
            qs.playSound(QuestState.SOUND_FINISH);
            string2 = "sobling_q0376_07.htm";
        }
        if (event.equalsIgnoreCase("reply_4")) {
            qs.giveItems(5892, 1L);
            qs.takeItems(5890, 1L);
            qs.setCond(3);
            string2 = "cliff_q0376_02.htm";
            qs.playSound(QuestState.SOUND_MIDDLE);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        switch (n) {
            case 1: {
                if (pc.getLevel() >= 51) {
                    html = "sobling_q0376_01.htm";
                    break;
                }
                html = "sobling_q0376_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 31147) {
                    if (!(qs.getCond() < 1 || qs.getQuestItemsCount(5890) != 0L || qs.getQuestItemsCount(5922) >= 1L && qs.getQuestItemsCount(5923) >= 1L && qs.getQuestItemsCount(5924) >= 1L && qs.getQuestItemsCount(5925) >= 1L && qs.getQuestItemsCount(5926) >= 1L || qs.getQuestItemsCount(5927) >= 1L && qs.getQuestItemsCount(5928) >= 1L && qs.getQuestItemsCount(5929) >= 1L && qs.getQuestItemsCount(5930) >= 1L && qs.getQuestItemsCount(5931) >= 1L || qs.getQuestItemsCount(5932) >= 1L && qs.getQuestItemsCount(5933) >= 1L && qs.getQuestItemsCount(5934) >= 1L && qs.getQuestItemsCount(5935) >= 1L && qs.getQuestItemsCount(5936) >= 1L || qs.getQuestItemsCount(5937) >= 1L && qs.getQuestItemsCount(5938) >= 1L && qs.getQuestItemsCount(5939) >= 1L && qs.getQuestItemsCount(5940) >= 1L && qs.getQuestItemsCount(5941) >= 1L)) {
                        html = "sobling_q0376_04.htm";
                        break;
                    }
                    if (qs.getCond() >= 1 && qs.getQuestItemsCount(5890) == 0L && (qs.getQuestItemsCount(5922) >= 1L && qs.getQuestItemsCount(5923) >= 1L && qs.getQuestItemsCount(5924) >= 1L && qs.getQuestItemsCount(5925) >= 1L && qs.getQuestItemsCount(5926) >= 1L || qs.getQuestItemsCount(5927) >= 1L && qs.getQuestItemsCount(5928) >= 1L && qs.getQuestItemsCount(5929) >= 1L && qs.getQuestItemsCount(5930) >= 1L && qs.getQuestItemsCount(5931) >= 1L || qs.getQuestItemsCount(5932) >= 1L && qs.getQuestItemsCount(5933) >= 1L && qs.getQuestItemsCount(5934) >= 1L && qs.getQuestItemsCount(5935) >= 1L && qs.getQuestItemsCount(5936) >= 1L || qs.getQuestItemsCount(5937) >= 1L && qs.getQuestItemsCount(5938) >= 1L && qs.getQuestItemsCount(5939) >= 1L && qs.getQuestItemsCount(5940) >= 1L && qs.getQuestItemsCount(5941) >= 1L)) {
                        if (qs.getQuestItemsCount(5922) > 0L && qs.getQuestItemsCount(5923) > 0L && qs.getQuestItemsCount(5924) > 0L && qs.getQuestItemsCount(5925) > 0L && qs.getQuestItemsCount(5926) > 0L) {
                            qs.takeItems(5922, 1L);
                            qs.takeItems(5923, 1L);
                            qs.takeItems(5924, 1L);
                            qs.takeItems(5925, 1L);
                            qs.takeItems(5926, 1L);
                            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                                qs.giveItems(5416, 1L);
                            } else {
                                qs.giveItems(5418, 1L);
                            }
                        }
                        if (qs.getQuestItemsCount(5927) > 0L && qs.getQuestItemsCount(5928) > 0L && qs.getQuestItemsCount(5929) > 0L && qs.getQuestItemsCount(5930) > 0L && qs.getQuestItemsCount(5931) > 0L) {
                            qs.takeItems(5927, 1L);
                            qs.takeItems(5928, 1L);
                            qs.takeItems(5929, 1L);
                            qs.takeItems(5930, 1L);
                            qs.takeItems(5931, 1L);
                            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                                qs.giveItems(5424, 1L);
                            } else {
                                qs.giveItems(5340, 1L);
                            }
                        }
                        if (qs.getQuestItemsCount(5932) > 0L && qs.getQuestItemsCount(5933) > 0L && qs.getQuestItemsCount(5934) > 0L && qs.getQuestItemsCount(5935) > 0L && qs.getQuestItemsCount(5936) > 0L) {
                            qs.takeItems(5932, 1L);
                            qs.takeItems(5933, 1L);
                            qs.takeItems(5934, 1L);
                            qs.takeItems(5935, 1L);
                            qs.takeItems(5936, 1L);
                            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                                qs.giveItems(5332, 1L);
                            } else {
                                qs.giveItems(5334, 1L);
                            }
                        }
                        if (qs.getQuestItemsCount(5937) > 0L && qs.getQuestItemsCount(5938) > 0L && qs.getQuestItemsCount(5939) > 0L && qs.getQuestItemsCount(5940) > 0L && qs.getQuestItemsCount(5941) > 0L) {
                            qs.takeItems(5937, 1L);
                            qs.takeItems(5938, 1L);
                            qs.takeItems(5939, 1L);
                            qs.takeItems(5940, 1L);
                            qs.takeItems(5941, 1L);
                            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                                qs.giveItems(5354, 1L);
                            } else {
                                qs.giveItems(5346, 1L);
                            }
                        }
                        html = "sobling_q0376_05.htm";
                        break;
                    }
                    if (qs.getCond() == 1 && qs.getQuestItemsCount(5890) == 1L) {
                        qs.setCond(2);
                        html = "sobling_q0376_08.htm";
                        break;
                    }
                    if (qs.getCond() != 2 || qs.getQuestItemsCount(5890) != 1L) break;
                    html = "sobling_q0376_09.htm";
                    break;
                }
                if (n2 != 30182) break;
                if (qs.getCond() == 2 && qs.getQuestItemsCount(5890) >= 1L) {
                    html = "cliff_q0376_01.htm";
                    break;
                }
                if (qs.getCond() != 2 || qs.getQuestItemsCount(5892) < 1L) break;
                html = "cliff_q0376_03.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20647) {
            if (qs.getQuestItemsCount(5890) == 0L && qs.getQuestItemsCount(5892) == 0L) {
                qs.rollAndGive(5890, 1, 0.2);
            }
            qs.rollAndGive(5944, 1, 2.6);
        } else if (n == 20648) {
            if (qs.getQuestItemsCount(5890) == 0L && qs.getQuestItemsCount(5892) == 0L) {
                qs.rollAndGive(5890, 1, 0.2);
            }
            qs.rollAndGive(5944, 1, 2.6);
        } else if (n == 20649) {
            if (qs.getQuestItemsCount(5890) == 0L && qs.getQuestItemsCount(5892) == 0L) {
                qs.rollAndGive(5890, 1, 0.2);
            }
            qs.rollAndGive(5944, 1, 2.6);
        } else if (n == 20650) {
            if (qs.getQuestItemsCount(5890) == 0L && qs.getQuestItemsCount(5892) == 0L) {
                qs.rollAndGive(5890, 1, 0.2);
            }
            qs.rollAndGive(5944, 1, 2.6);
        }
        return null;
    
	}

}
