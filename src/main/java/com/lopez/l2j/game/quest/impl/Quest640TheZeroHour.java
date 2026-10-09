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
 * Quest 640 - 640_TheZeroHour
 */
@Component
public class Quest640TheZeroHour extends Quest {

	public static final int bRV = 22108;
	public static final int bRW = 22113;
	public static final int bRX = 22114;
	public static final int bRY = 22109;
	public static final int bRZ = 22110;
	public static final int bSa = 22118;
	public static final int bSb = 22119;
	public static final int bSc = 22105;
	public static final int bSd = 22116;
	public static final int bSe = 22107;
	public static final int bSf = 22117;
	public static final int bSg = 22111;
	public static final int bSh = 22121;
	public static final int bSi = 22115;
	public static final int bSj = 22106;
	public static final int boG = 4042;
	public static final int buT = 4043;
	public static final int buU = 4044;
	public static final int boM = 1887;
	public static final int bor = 1888;
	public static final int bSk = 1889;
	public static final int bSl = 5550;
	public static final int bSm = 1890;
	public static final int bSn = 1893;
	public static final int bQe = 31554;
	public static final int bSo = 8085;

	public Quest640TheZeroHour(QuestManager questManager) {
	super(640, "640_TheZeroHour", "640_TheZeroHour");
		this.addStartNpc(31554);
		this.addKillId(22108, 22113, 22114, 22109, 22110, 22118, 22119, 22105, 22116, 22107, 22117, 22111, 22121, 22115, 22106);
		this.addQuestItem(8085);
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

        int n = qs.getCond();
        String string2 = event;
        if (event.equalsIgnoreCase("quest_accept")) {
            if (pc.getLevel() >= 66) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "merc_kahmun_q0640_0103.htm";
            }
        } else if (event.equalsIgnoreCase("reply_1") && n == 1) {
            string2 = "merc_kahmun_q0640_0201.htm";
        } else if (event.equalsIgnoreCase("reply_3") && n == 1) {
            string2 = "merc_kahmun_q0640_0202.htm";
        } else if (event.equalsIgnoreCase("reply_4") && n == 1) {
            string2 = "merc_kahmun_q0640_0205.htm";
            qs.exitQuest(true);
            qs.playSound(QuestState.SOUND_FINISH);
        } else if (event.equalsIgnoreCase("reply_11") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 12L) {
                qs.takeItems(8085, 12L);
                qs.giveItems(4042, 1L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_12") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 6L) {
                qs.takeItems(8085, 6L);
                qs.giveItems(4043, 1L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_13") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 6L) {
                qs.takeItems(8085, 6L);
                qs.giveItems(4044, 1L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_14") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 81L) {
                qs.takeItems(8085, 81L);
                qs.giveItems(1887, 10L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_15") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 33L) {
                qs.takeItems(8085, 33L);
                qs.giveItems(1888, 5L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_16") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 30L) {
                qs.takeItems(8085, 30L);
                qs.giveItems(1889, 10L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_17") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 150L) {
                qs.takeItems(8085, 150L);
                qs.giveItems(5550, 10L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_18") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 131L) {
                qs.takeItems(8085, 131L);
                qs.giveItems(1890, 10L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        } else if (event.equalsIgnoreCase("reply_19") && n == 1) {
            if (qs.getQuestItemsCount(8085) >= 123L) {
                qs.takeItems(8085, 123L);
                qs.giveItems(1893, 5L);
                string2 = "merc_kahmun_q0640_0203.htm";
            } else {
                string2 = "merc_kahmun_q0640_0204.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        QuestState questState2 = qs.getQuestState("109_InSearchOfTheNest");
        if (pc.getLevel() >= 66 && n == 0) {
            html = questState2 != null && questState2.isCompleted() ? "merc_kahmun_q0640_0101.htm" : "merc_kahmun_q0640_0104.htm";
        } else if (pc.getLevel() < 66 && n == 0) {
            html = "merc_kahmun_q0640_0102.htm";
        } else if (n == 1) {
            html = qs.getQuestItemsCount(8085) == 0L ? "merc_kahmun_q0640_0106.htm" : "merc_kahmun_q0640_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = ThreadLocalRandom.current().nextInt(1000);
        if (n == 22108) {
            if (n2 < 77) {
                qs.rollAndGive(8085, 2, 100.0);
            } else {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22113) {
            if (n2 < 235) {
                qs.rollAndGive(8085, 2, 100.0);
            } else {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22114) {
            if (n2 < 829) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22109) {
            if (n2 < 63) {
                qs.rollAndGive(8085, 2, 100.0);
            } else {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22110) {
            if (n2 < 806) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22118) {
            if (n2 < 982) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22119 || n == 22105) {
            if (n2 < 727) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22116) {
            if (n2 < 702) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22107) {
            if (n2 < 773) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22117) {
            if (n2 < 723) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22111) {
            if (n2 < 776) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22121) {
            if (n2 < 704) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22115) {
            if (n2 < 682) {
                qs.rollAndGive(8085, 1, 100.0);
            }
        } else if (n == 22106 && n2 < 750) {
            qs.rollAndGive(8085, 1, 100.0);
        }
        return null;
    
	}

}
