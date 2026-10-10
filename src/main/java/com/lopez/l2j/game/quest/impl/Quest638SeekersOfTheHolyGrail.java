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
 * Quest 638 - 638_SeekersOfTheHolyGrail
 */
@Component
public class Quest638SeekersOfTheHolyGrail extends Quest {

	public static final int aAb = 31328;
	public static final int bQI = 8068;
	public static final int bQJ = 960;
	public static final int bQK = 959;
	public static final int bQL = 22176;
	public static final int bQM = 22146;
	public static final int bQN = 22151;
	public static final int bQO = 22138;
	public static final int bQP = 22141;
	public static final int bQQ = 22175;
	public static final int bQR = 22155;
	public static final int bQS = 22159;
	public static final int bQT = 22163;
	public static final int bQU = 22167;
	public static final int bQV = 22171;
	public static final int bQW = 22143;
	public static final int bQX = 22137;
	public static final int bQY = 22194;
	public static final int bQZ = 22164;
	public static final int bRa = 22156;
	public static final int bRb = 22166;
	public static final int bRc = 22173;
	public static final int bRd = 22170;
	public static final int bRe = 22157;
	public static final int bRf = 22160;
	public static final int bRg = 22165;
	public static final int bRh = 22168;
	public static final int bRi = 22174;
	public static final int bRj = 22158;
	public static final int bRk = 22162;
	public static final int bRl = 22149;
	public static final int bRm = 22147;
	public static final int bRn = 22154;
	public static final int bRo = 22161;
	public static final int bRp = 22169;
	public static final int bRq = 22172;
	public static final int bRr = 22145;
	public static final int bRs = 22152;
	public static final int bRt = 22153;
	public static final int bRu = 22136;
	public static final int bRv = 22150;
	public static final int bRw = 22148;
	public static final int bRx = 22142;
	public static final int bRy = 22144;
	public static final int bRz = 22139;
	public static final int bRA = 22140;

	public Quest638SeekersOfTheHolyGrail(QuestManager questManager) {
	super(638, "638_SeekersOfTheHolyGrail", "638_SeekersOfTheHolyGrail");
		this.addStartNpc(31328);
		this.addQuestItem(8068);
		this.addKillId(22176, 22146, 22151, 22138, 22141, 22175, 22155, 22159, 22163, 22167, 22171, 22143, 22137, 22194, 22164, 22156, 22166, 22173, 22170, 22157, 22160, 22165, 22168, 22174, 22158, 22162, 22149, 22147, 22154, 22161, 22169, 22172, 22145, 22152, 22153, 22136, 22150, 22148, 22142, 22144, 22139, 22140);
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
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "highpriest_innocentin_q0638_03.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "highpriest_innocentin_q0638_06.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            if (qs.getCond() == 1 && qs.getQuestItemsCount(8068) >= 2000L) {
                if (ThreadLocalRandom.current().nextInt(100) < 80) {
                    if (ThreadLocalRandom.current().nextInt(2) == 0) {
                        qs.giveItems(960, 1L, true);
                    } else {
                        qs.giveItems(959, 1L, true);
                    }
                    qs.takeItems(8068, 2000L);
                    string2 = "highpriest_innocentin_q0638_07.htm";
                } else {
                    qs.giveItems(57, 3576000L);
                    qs.takeItems(8068, 2000L);
                    string2 = "highpriest_innocentin_q0638_08.htm";
                }
            }
        } else if (event.equalsIgnoreCase("reply_3")) {
            qs.giveItems(57, 1700L * qs.getQuestItemsCount(8068));
            qs.takeItems(8068, -1L);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "highpriest_innocentin_q0638_09.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getStateId();
        if (npc.getNpcId() == 31328) {
            if (n == 1) {
                html = pc.getLevel() >= 73 ? "highpriest_innocentin_q0638_01.htm" : "highpriest_innocentin_q0638_02.htm";
            } else if (qs.getQuestItemsCount(8068) >= 2000L && qs.getCond() == 1) {
                html = "highpriest_innocentin_q0638_04.htm";
            } else if (qs.getQuestItemsCount(8068) < 2000L && qs.getCond() == 1) {
                html = "highpriest_innocentin_q0638_05.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 22176 && ThreadLocalRandom.current().nextInt(100) < 6) {
            qs.rollAndGive(8068, 1, 100.0);
        } else if (n == 22146) {
            if (ThreadLocalRandom.current().nextInt(100) < 54) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(10) < 1) {
                qs.giveItems(8275, 1);
            }
        } else if (n == 22151) {
            if (ThreadLocalRandom.current().nextInt(100) < 62) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            if (ThreadLocalRandom.current().nextInt(10) < 1) {
                qs.giveItems(8275, 1);
            }
        } else if (n == 22149) {
            if (ThreadLocalRandom.current().nextInt(100) < 54) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            qs.giveItems(8273, 6);
        } else if (n == 22143) {
            if (ThreadLocalRandom.current().nextInt(100) < 62) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            qs.giveItems(8274, 1);
        } else if (n == 22142) {
            if (ThreadLocalRandom.current().nextInt(100) < 54) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            qs.giveItems(8274, 1);
        } else if (n == 22140) {
            if (ThreadLocalRandom.current().nextInt(100) < 54) {
                qs.rollAndGive(8068, 1, 100.0);
            }
            qs.giveItems(8273, 1);
        } else if (n == 22141 || n == 22147 || n == 22152 || n == 22136) {
            if (ThreadLocalRandom.current().nextInt(100) < 55) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22175) {
            if (ThreadLocalRandom.current().nextInt(100) < 3) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22155 || n == 22159 || n == 22163 || n == 22167) {
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22171 && ThreadLocalRandom.current().nextInt(100) < 87) {
            qs.rollAndGive(8068, 1, 100.0);
        } else if (n == 22137 || n == 22194 || n == 22138) {
            if (ThreadLocalRandom.current().nextInt(100) < 6) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22156 || n == 22164 || n == 22170 || n == 22160 || n == 22174 || n == 22158 || n == 22162) {
            if (ThreadLocalRandom.current().nextInt(100) < 67) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22166 || n == 22173 || n == 22157 || n == 22165 || n == 22168) {
            if (ThreadLocalRandom.current().nextInt(100) < 66) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22154 || n == 22145 || n == 22153) {
            if (ThreadLocalRandom.current().nextInt(100) < 53) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22161 || n == 22169 || n == 22172) {
            if (ThreadLocalRandom.current().nextInt(100) < 78) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if (n == 22150 || n == 22148) {
            if (ThreadLocalRandom.current().nextInt(100) < 46) {
                qs.rollAndGive(8068, 1, 100.0);
            }
        } else if ((n == 22144 || n == 22139) && ThreadLocalRandom.current().nextInt(100) < 54) {
            qs.rollAndGive(8068, 1, 100.0);
        }
        return null;
    
	}

}
