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
 * Quest 606 - 606_WarwithVarkaSilenos
 */
@Component
public class Quest606WarwithVarkaSilenos extends Quest {

	public static final int bKB = 21371;
	public static final int bKv = 21365;
	public static final int bKt = 21369;
	public static final int bKj = 21350;
	public static final int bKm = 21354;
	public static final int bKq = 21360;
	public static final int bKw = 21366;
	public static final int bKx = 21368;
	public static final int bKo = 21357;
	public static final int bKl = 21353;
	public static final int bLc = 21364;
	public static final int bKs = 21362;
	public static final int bKn = 21355;
	public static final int bKp = 21358;
	public static final int bKy = 21373;
	public static final int bLd = 31370;
	public static final int bLe = 7233;
	public static final int bLf = 7186;

	public Quest606WarwithVarkaSilenos(QuestManager questManager) {
	super(606, "606_WarwithVarkaSilenos", "606_WarwithVarkaSilenos");
		this.addStartNpc(31370);
		this.addKillId(21350, 21353, 21354, 21355, 21357, 21358, 21360, 21362, 21364, 21365, 21366, 21368, 21369, 21371, 21373);
		this.addQuestItem(7233);
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
        if (event.equals("quest_accept")) {
            string2 = "elder_kadun_zu_ketra_q0606_0104.htm";
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equals("606_3")) {
            long l = qs.getQuestItemsCount(7233) / 5L;
            if (l > 0L) {
                string2 = "elder_kadun_zu_ketra_q0606_0202.htm";
                qs.takeItems(7233, l * 5L);
                qs.giveItems(7186, l);
            } else {
                string2 = "elder_kadun_zu_ketra_q0606_0203.htm";
            }
        } else if (event.equals("606_4")) {
            string2 = "elder_kadun_zu_ketra_q0606_0204.htm";
            qs.takeItems(7233, -1L);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getCond();
        if (n == 0) {
            if (pc.getLevel() >= 74) {
                html = "elder_kadun_zu_ketra_q0606_0101.htm";
            } else {
                html = "elder_kadun_zu_ketra_q0606_0103.htm";
                qs.exitQuest(true);
            }
        } else if (n == 1 && qs.getQuestItemsCount(7233) == 0L) {
            html = "elder_kadun_zu_ketra_q0606_0106.htm";
        } else if (n == 1 && qs.getQuestItemsCount(7233) > 0L) {
            html = "elder_kadun_zu_ketra_q0606_0105.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (qs.getCond() == 1) {
            int n2;
            if (n == 21371) {
                int n3 = ThreadLocalRandom.current().nextInt(1000);
                if (n3 < 713) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21365 || n == 21366 || n == 21368) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 568) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21369) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 664) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21350) {
                int n6 = ThreadLocalRandom.current().nextInt(1000);
                if (n6 < 500) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21354) {
                int n7 = ThreadLocalRandom.current().nextInt(1000);
                if (n7 < 522) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21360 || n == 21362) {
                int n8 = ThreadLocalRandom.current().nextInt(1000);
                if (n8 < 539) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21357 || n == 21358) {
                int n9 = ThreadLocalRandom.current().nextInt(1000);
                if (n9 < 529) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21353) {
                int n10 = ThreadLocalRandom.current().nextInt(1000);
                if (n10 < 510) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21364) {
                int n11 = ThreadLocalRandom.current().nextInt(1000);
                if (n11 < 558) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21355) {
                int n12 = ThreadLocalRandom.current().nextInt(1000);
                if (n12 < 519) {
                    qs.rollAndGive(7233, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n == 21373 && (n2 = ThreadLocalRandom.current().nextInt(1000)) < 738) {
                qs.rollAndGive(7233, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
