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
 * Quest 612 - 612_WarwithKetraOrcs
 */
@Component
public class Quest612WarwithKetraOrcs extends Quest {

	public static final int bLP = 31377;
	public static final int bKN = 21324;
	public static final int bKW = 21327;
	public static final int bKT = 21328;
	public static final int bKV = 21329;
	public static final int bKX = 21331;
	public static final int bKL = 21332;
	public static final int bKP = 21334;
	public static final int bKM = 21336;
	public static final int bKU = 21338;
	public static final int bKQ = 21339;
	public static final int bKS = 21340;
	public static final int bKR = 21342;
	public static final int bKO = 21343;
	public static final int bKK = 21345;
	public static final int bKY = 21347;
	public static final int bLQ = 7234;
	public static final int bLR = 7187;

	public Quest612WarwithKetraOrcs(QuestManager questManager) {
	super(612, "612_WarwithKetraOrcs", "612_WarwithKetraOrcs");
		this.addStartNpc(31377);
		this.addKillId(21324, 21327, 21328, 21329, 21331, 21332, 21334, 21336, 21338, 21339, 21340, 21342, 21343, 21345, 21347);
		this.addQuestItem(7234);
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
            qs.set("war_with_ketra_orcs", String.valueOf(11), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "elder_ashas_barka_durai_q0612_0104.htm";
        } else if (event.equalsIgnoreCase("reply_1")) {
            string2 = "elder_ashas_barka_durai_q0612_0201.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            if (qs.getQuestItemsCount(7234) >= 100L) {
                qs.takeItems(7234, 100L);
                qs.giveItems(7187, 20L);
                string2 = "elder_ashas_barka_durai_q0612_0202.htm";
            } else {
                string2 = "elder_ashas_barka_durai_q0612_0203.htm";
            }
        } else if (event.equalsIgnoreCase("reply_4")) {
            qs.takeItems(7234, -1L);
            qs.unset("war_with_ketra_orcs");
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "elder_ashas_barka_durai_q0612_0204.htm";
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("war_with_ketra_orcs");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31377) break;
                if (pc.getLevel() >= 74) {
                    html = "elder_ashas_barka_durai_q0612_0101.htm";
                    break;
                }
                html = "elder_ashas_barka_durai_q0612_0103.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 31377 || n != 11) break;
                html = qs.getQuestItemsCount(7234) == 0L ? "elder_ashas_barka_durai_q0612_0106.htm" : "elder_ashas_barka_durai_q0612_0105.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("war_with_ketra_orcs");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 11) {
            int n3;
            if (n2 == 21324) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 500) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21327) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 510) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21328) {
                int n6 = ThreadLocalRandom.current().nextInt(1000);
                if (n6 < 522) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21329) {
                int n7 = ThreadLocalRandom.current().nextInt(1000);
                if (n7 < 519) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21331 || n2 == 21332) {
                int n8 = ThreadLocalRandom.current().nextInt(1000);
                if (n8 < 529) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21334) {
                int n9 = ThreadLocalRandom.current().nextInt(1000);
                if (n9 < 539) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21336) {
                int n10 = ThreadLocalRandom.current().nextInt(1000);
                if (n10 < 548) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21338) {
                int n11 = ThreadLocalRandom.current().nextInt(1000);
                if (n11 < 558) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21339 || n2 == 21340) {
                int n12 = ThreadLocalRandom.current().nextInt(1000);
                if (n12 < 568) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21342) {
                int n13 = ThreadLocalRandom.current().nextInt(1000);
                if (n13 < 578) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21343) {
                int n14 = ThreadLocalRandom.current().nextInt(1000);
                if (n14 < 664) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21345) {
                int n15 = ThreadLocalRandom.current().nextInt(1000);
                if (n15 < 713) {
                    qs.rollAndGive(7234, 1, 100.0);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == 21347 && (n3 = ThreadLocalRandom.current().nextInt(1000)) < 738) {
                qs.rollAndGive(7234, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
