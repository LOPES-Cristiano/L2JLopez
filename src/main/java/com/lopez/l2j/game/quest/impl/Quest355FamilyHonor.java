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
 * Quest 355 - 355_FamilyHonor
 */
@Component
public class Quest355FamilyHonor extends Quest {

	public static final int bqS = 30181;
	public static final int bqT = 30929;
	public static final int bqU = 20767;
	public static final int bqV = 20768;
	public static final int bqW = 20769;
	public static final int bqX = 20770;
	public static final int bqY = 4252;
	public static final int bqZ = 4350;
	public static final int bra = 4351;
	public static final int brb = 4352;
	public static final int brc = 4353;
	public static final int brd = 4354;

	public Quest355FamilyHonor(QuestManager questManager) {
	super(355, "355_FamilyHonor", "355_FamilyHonor");
		this.addStartNpc(30181);
		this.addTalkId(30929);
		this.addKillId(20767, 20768, 20769, 20770);
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
        if (n == 30181) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "galicbredo_q0355_04.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "galicbredo_q0355_03.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                if (qs.getQuestItemsCount(4252) < 1L) {
                    string2 = "galicbredo_q0355_07.htm";
                } else if (qs.getQuestItemsCount(4252) >= 100L) {
                    long l = 7800L + qs.getQuestItemsCount(4252) * 120L;
                    qs.takeItems(4252, -1L);
                    qs.giveItems(57, l);
                    string2 = "galicbredo_q0355_07b.htm";
                } else if (qs.getQuestItemsCount(4252) >= 1L && qs.getQuestItemsCount(4252) < 100L) {
                    qs.giveItems(57, qs.getQuestItemsCount(4252) * 120L + 2800L);
                    qs.takeItems(4252, -1L);
                    string2 = "galicbredo_q0355_07a.htm";
                }
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = "galicbredo_q0355_08.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                if (qs.getQuestItemsCount(4252) > 0L) {
                    qs.giveItems(57, qs.getQuestItemsCount(4252) * 120L);
                }
                qs.takeItems(4252, -1L);
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "galicbredo_q0355_09.htm";
            }
        } else if (n == 30929 && event.equalsIgnoreCase("reply_1")) {
            int n2 = ThreadLocalRandom.current().nextInt(100);
            if (qs.getQuestItemsCount(4350) < 1L) {
                string2 = "patrin_q0355_02.htm";
            } else if (qs.getQuestItemsCount(4350) >= 1L && n2 < 2) {
                qs.giveItems(4351, 1L);
                qs.takeItems(4350, 1L);
                string2 = "patrin_q0355_03.htm";
            } else if (qs.getQuestItemsCount(4350) >= 1L && n2 < 32) {
                qs.giveItems(4352, 1L);
                qs.takeItems(4350, 1L);
                string2 = "patrin_q0355_04.htm";
            } else if (qs.getQuestItemsCount(4350) >= 1L && n2 < 62) {
                qs.giveItems(4353, 1L);
                qs.takeItems(4350, 1L);
                string2 = "patrin_q0355_05.htm";
            } else if (qs.getQuestItemsCount(4350) >= 1L && n2 < 77) {
                qs.giveItems(4354, 1L);
                qs.takeItems(4350, 1L);
                string2 = "patrin_q0355_06.htm";
            } else {
                qs.takeItems(4350, 1L);
                string2 = "patrin_q0355_07.htm";
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
        switch (n2) {
            case 1: {
                if (n != 30181) break;
                if (pc.getLevel() < 36) {
                    qs.exitQuest(true);
                    html = "galicbredo_q0355_01.htm";
                    break;
                }
                html = "galicbredo_q0355_02.htm";
                break;
            }
            case 2: {
                if (n == 30181) {
                    if (qs.getQuestItemsCount(4350) < 1L) {
                        html = "galicbredo_q0355_05.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(4350) < 1L) break;
                    html = "galicbredo_q0355_06.htm";
                    break;
                }
                if (n != 30929 || n2 != 2) break;
                html = "patrin_q0355_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        if (n2 == 2) {
            if (n == 20767) {
                int n3 = ThreadLocalRandom.current().nextInt(1000);
                if (n3 < 560) {
                    qs.giveItems(4252, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n3 < 684) {
                    qs.giveItems(4350, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 20768) {
                int n4 = ThreadLocalRandom.current().nextInt(100);
                if (n4 < 53) {
                    qs.giveItems(4252, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n4 < 65) {
                    qs.giveItems(4350, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 20769) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 420) {
                    qs.giveItems(4252, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n5 < 516) {
                    qs.giveItems(4350, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 20770) {
                int n6 = ThreadLocalRandom.current().nextInt(100);
                if (n6 < 44) {
                    qs.giveItems(4252, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n6 < 56) {
                    qs.giveItems(4350, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
