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
 * Quest 365 - 365_DevilsLegacy
 */
@Component
public class Quest365DevilsLegacy extends Quest {

	public static final int bsk = 30095;
	public static final int blQ = 30092;
	public static final int[] MOBS = new int[]{20836, 29027, 20845, 21629, 21630, 29026};
	public static final int bsl = 25;
	public static final int bsm = 5070;
	public static final int bsn = 5873;

	public Quest365DevilsLegacy(QuestManager questManager) {
	super(365, "365_DevilsLegacy", "365_DevilsLegacy");
		this.addStartNpc(30095);
		this.addTalkId(30092);
		this.addKillId(this.MOBS);
		this.addQuestItem(5873);
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
        if (event.equalsIgnoreCase("30095-1.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30095-5.htm")) {
            long l = qs.getQuestItemsCount(5873);
            if (l > 0L) {
                long l2 = l * 5070L;
                qs.takeItems(5873, -1L);
                qs.giveItems(57, l2);
            } else {
                string2 = "You don't have required items";
            }
        } else if (event.equalsIgnoreCase("30095-6.htm")) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("30092_reward")) {
            if (qs.getQuestItemsCount(5873) < 1L) {
                string2 = "collob_q0365_03.htm";
            } else if (qs.getQuestItemsCount(57) < 600L) {
                string2 = "collob_q0365_04.htm";
            } else if (qs.getInt("cond") == 0) {
                string2 = "collob_q0365_05.htm";
            } else if (qs.getQuestItemsCount(5873) >= 1L && qs.getQuestItemsCount(57) >= 600L && qs.getInt("cond") == 1) {
                if (ThreadLocalRandom.current().nextInt(100) < 80) {
                    int n = ThreadLocalRandom.current().nextInt(100);
                    if (n < 1) {
                        qs.giveItems(995, 1L);
                    } else if (n < 4) {
                        qs.giveItems(956, 1L);
                    } else if (n < 36) {
                        qs.giveItems(1868, 1L);
                    } else if (n < 68) {
                        qs.giveItems(1884, 1L);
                    } else {
                        qs.giveItems(1872, 1L);
                    }
                    qs.takeItems(5873, 1L);
                    qs.takeItems(57, 600L);
                    string2 = "collob_q0365_06.htm";
                } else {
                    int n = ThreadLocalRandom.current().nextInt(1000);
                    if (n < 10) {
                        qs.giveItems(951, 1L);
                    } else if (n < 40) {
                        qs.giveItems(952, 1L);
                    } else if (n < 60) {
                        qs.giveItems(955, 1L);
                    } else if (n < 260) {
                        qs.giveItems(956, 1L);
                    } else if (n < 445) {
                        qs.giveItems(1879, 1L);
                    } else if (n < 630) {
                        qs.giveItems(1880, 1L);
                    } else if (n < 815) {
                        qs.giveItems(1882, 1L);
                    } else {
                        qs.giveItems(1881, 1L);
                    }
                    // skill effect
                    qs.takeItems(5873, 1L);
                    qs.takeItems(57, 600L);
                    string2 = "collob_q0365_07.htm";
                }
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
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 30095) {
            if (n == 0) {
                if (pc.getLevel() >= 39) {
                    html = "30095-0.htm";
                } else {
                    html = "30095-0a.htm";
                    qs.exitQuest(true);
                }
            } else if (n == 1) {
                html = qs.getQuestItemsCount(5873) == 0L ? "30095-2.htm" : "30095-4.htm";
            }
        }
        if (n2 == 30092) {
            if (n == 0) {
                html = "collob_q0365_02.htm";
            } else if (n == 1) {
                html = "collob_q0365_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if ((ThreadLocalRandom.current().nextDouble(100.0) < (25))) {
            qs.giveItems(5873, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
