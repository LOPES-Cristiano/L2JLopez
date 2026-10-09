package com.lopez.l2j.game.quest.impl;

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
 * Quest 644 - 644_GraveRobberAnnihilation
 */
@Component
public class Quest644GraveRobberAnnihilation extends Quest {

	public static final int bTq = 32017;
	public static final int bTr = 22003;
	public static final int bTs = 22004;
	public static final int bTt = 22005;
	public static final int bTu = 22006;
	public static final int bHk = 22008;
	public static final int bTv = 8088;
	public static final int bTw = 1865;
	public static final int bqo = 1867;
	public static final int boJ = 1872;
	public static final int bqq = 1871;
	public static final int bqp = 1870;
	public static final int bTx = 1869;

	public Quest644GraveRobberAnnihilation(QuestManager questManager) {
	super(644, "644_GraveRobberAnnihilation", "644_GraveRobberAnnihilation");
		this.addStartNpc(32017);
		this.addKillId(22003, 22004, 22005, 22006, 22008);
		this.addQuestItem(8088);
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
        int n = qs.getInt("sweep_the_snatcher");
        int n2 = qs.getInt("sweep_the_snatcher_c");
        int n3 = pc != null && pc.getLastNpc() != null ? pc.getLastNpc().getNpcId() : 32017;
        if (n3 == 32017) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("sweep_the_snatcher", String.valueOf(11), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "karuda_q0644_0103.htm";
            } else if (event.equalsIgnoreCase("reply_3") && n2 == 1 && n >= 11) {
                string2 = "karuda_q0644_0201.htm";
            } else if (n >= 11 && qs.getQuestItemsCount(8088) >= 120L) {
                qs.takeItems(8088, 120L);
                if (event.equalsIgnoreCase("reply_11")) {
                    qs.giveItems(1865, 30L);
                } else if (event.equalsIgnoreCase("reply_12")) {
                    qs.giveItems(1867, 40L);
                } else if (event.equalsIgnoreCase("reply_13")) {
                    qs.giveItems(1872, 40L);
                } else if (event.equalsIgnoreCase("reply_14")) {
                    qs.giveItems(1871, 30L);
                } else if (event.equalsIgnoreCase("reply_15")) {
                    qs.giveItems(1870, 30L);
                } else if (event.equalsIgnoreCase("reply_16")) {
                    qs.giveItems(1869, 30L);
                }
                qs.unset("sweep_the_snatcher");
                qs.unset("sweep_the_snatcher_c");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "karuda_q0644_0202.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("sweep_the_snatcher");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 32017) break;
                if (pc.getLevel() >= 20) {
                    html = "karuda_q0644_0101.htm";
                    break;
                }
                html = "karuda_q0644_0102.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 32017 || n < 11 || n > 12) break;
                if (n == 12 && qs.getQuestItemsCount(8088) >= 120L) {
                    qs.set("sweep_the_snatcher_c", String.valueOf(1), true);
                    html = "karuda_q0644_0105.htm";
                    break;
                }
                html = "karuda_q0644_0106.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("sweep_the_snatcher");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 11) {
            int n3;
            if (n2 == 22003) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 714) {
                    if (qs.getQuestItemsCount(8088) + 1L >= 120L) {
                        if (qs.getQuestItemsCount(8088) < 120L) {
                            qs.giveItems(8088, 120L - qs.getQuestItemsCount(8088));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                        qs.setCond(2);
                        qs.set("sweep_the_snatcher", String.valueOf(12), true);
                    } else {
                        qs.giveItems(8088, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 22004) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 841) {
                    if (qs.getQuestItemsCount(8088) + 1L >= 120L) {
                        if (qs.getQuestItemsCount(8088) < 120L) {
                            qs.giveItems(8088, 120L - qs.getQuestItemsCount(8088));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                        qs.setCond(2);
                        qs.set("sweep_the_snatcher", String.valueOf(12), true);
                    } else {
                        qs.giveItems(8088, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 22005) {
                int n6 = ThreadLocalRandom.current().nextInt(1000);
                if (n6 < 746) {
                    if (qs.getQuestItemsCount(8088) + 1L >= 120L) {
                        if (qs.getQuestItemsCount(8088) < 120L) {
                            qs.giveItems(8088, 120L - qs.getQuestItemsCount(8088));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                        qs.setCond(2);
                        qs.set("sweep_the_snatcher", String.valueOf(12), true);
                    } else {
                        qs.giveItems(8088, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 22006) {
                int n7 = ThreadLocalRandom.current().nextInt(1000);
                if (n7 < 778) {
                    if (qs.getQuestItemsCount(8088) + 1L >= 120L) {
                        if (qs.getQuestItemsCount(8088) < 120L) {
                            qs.giveItems(8088, 120L - qs.getQuestItemsCount(8088));
                            qs.playSound(QuestState.SOUND_MIDDLE);
                        }
                        qs.setCond(2);
                        qs.set("sweep_the_snatcher", String.valueOf(12), true);
                    } else {
                        qs.giveItems(8088, 1L);
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 22008 && (n3 = ThreadLocalRandom.current().nextInt(1000)) < 810) {
                if (qs.getQuestItemsCount(8088) + 1L >= 120L) {
                    if (qs.getQuestItemsCount(8088) < 120L) {
                        qs.giveItems(8088, 120L - qs.getQuestItemsCount(8088));
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    }
                    qs.setCond(2);
                    qs.set("sweep_the_snatcher", String.valueOf(12), true);
                } else {
                    qs.giveItems(8088, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
