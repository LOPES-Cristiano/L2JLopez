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
 * Quest 639 - 639_GuardiansOfTheHolyGrail
 */
@Component
public class Quest639GuardiansOfTheHolyGrail extends Quest {

	public static final int aAa = 31350;
	public static final int bRB = 32008;
	public static final int bRC = 32028;
	public static final int bRD = 8069;
	public static final int bRE = 8070;
	public static final int bRF = 8071;
	public static final int bRG = 8056;
	public static final int bQJ = 960;
	public static final int bQK = 959;
	public static final int bRH = 22123;
	public static final int bRI = 22122;
	public static final int bRJ = 22128;
	public static final int bRK = 22135;
	public static final int bRL = 22132;
	public static final int bRM = 22131;
	public static final int bRN = 22129;
	public static final int bRO = 22133;
	public static final int bRP = 22134;
	public static final int bRQ = 22127;
	public static final int bRR = 22126;
	public static final int bRS = 22124;
	public static final int bRT = 22125;
	public static final int bRU = 22130;

	public Quest639GuardiansOfTheHolyGrail(QuestManager questManager) {
	super(639, "639_GuardiansOfTheHolyGrail", "639_GuardiansOfTheHolyGrail");
		this.addStartNpc(31350);
		this.addTalkId(32008, 32028);
		this.addQuestItem(8069);
		this.addKillId(22123, 22122, 22128, 22135, 22132, 22131, 22129, 22133, 22134, 22127, 22126, 22124, 22125, 22130);
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
        if (event.equals("falsepriest_dominic_q0639_04.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("falsepriest_dominic_q0639_09.htm")) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equals("falsepriest_dominic_q0639_08.htm")) {
            long count = qs.getQuestItemsCount(8069);
            qs.takeAllItems(8069);
            qs.giveItems(57, count * 1625L);
        } else if (event.equals("falsepriest_gremory_q0639_05.htm")) {
            qs.setCond(2);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.giveItems(8070, 1L, false);
        } else if (event.equals("holy_grail_q0639_02.htm")) {
            qs.setCond(3);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.takeItems(8070, -1L);
            qs.giveItems(8071, 1L);
        } else if (event.equals("falsepriest_gremory_q0639_09.htm")) {
            qs.setCond(4);
            qs.playSound(QuestState.SOUND_MIDDLE);
            qs.takeItems(8071, -1L);
        } else if (event.equals("falsepriest_gremory_q0639_11.htm")) {
            qs.takeItems(8069, 4000L);
            qs.giveItems(959, 1L, true);
        } else if (event.equals("falsepriest_gremory_q0639_13.htm")) {
            qs.takeItems(8069, 400L);
            qs.giveItems(960, 1L, true);
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
        int n3 = qs.getCond();
        if (n == 31350) {
            if (n2 == 1) {
                html = pc.getLevel() >= 73 ? "falsepriest_dominic_q0639_01.htm" : "falsepriest_dominic_q0639_02.htm";
                qs.exitQuest(true);
            } else {
                html = qs.getQuestItemsCount(8069) >= 1L ? "falsepriest_dominic_q0639_05.htm" : "falsepriest_dominic_q0639_06.htm";
            }
        } else if (n == 32008) {
            if (n3 == 1) {
                html = "falsepriest_gremory_q0639_01.htm";
            } else if (n3 == 2) {
                html = "falsepriest_gremory_q0639_06.htm";
            } else if (n3 == 3) {
                html = "falsepriest_gremory_q0639_08.htm";
            } else if (n3 == 4 && qs.getQuestItemsCount(8069) < 400L) {
                html = "falsepriest_gremory_q0639_09.htm";
            } else if (n3 == 4 && qs.getQuestItemsCount(8069) >= 4000L) {
                html = "falsepriest_gremory_q0639_10.htm";
            } else if (n3 == 4 && qs.getQuestItemsCount(8069) >= 400L && qs.getQuestItemsCount(8069) < 4000L) {
                html = "falsepriest_gremory_q0639_14.htm";
            }
        } else if (n == 32028 && n3 == 2) {
            html = "holy_grail_q0639_01.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 22123 && ThreadLocalRandom.current().nextInt(100) < 75) {
            qs.rollAndGive(8069, 1, 100.0);
        } else if (n == 22122 && ThreadLocalRandom.current().nextInt(100) < 76) {
            qs.rollAndGive(8069, 1, 100.0);
        } else if (n == 22128 && ThreadLocalRandom.current().nextInt(100) < 17) {
            qs.rollAndGive(8069, 1, 100.0);
        } else if (n == 22130) {
            if (ThreadLocalRandom.current().nextInt(100) < 17) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 85) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22135) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 93) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22132) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 58) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22131) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 92) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22129) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 59) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22133) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 23) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22134) {
            if (ThreadLocalRandom.current().nextInt(30) < 1) {
                qs.rollAndGive(8056, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(100) < 58) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if (n == 22127 || n == 22125) {
            if (ThreadLocalRandom.current().nextInt(100) < 58) {
                qs.rollAndGive(8069, 1, 100.0);
            }
        } else if ((n == 22126 || n == 22124) && ThreadLocalRandom.current().nextInt(100) < 59) {
            qs.rollAndGive(8069, 1, 100.0);
        }
        return null;
    
	}

}
