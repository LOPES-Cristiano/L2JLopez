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
 * Quest 344 - 344_1000YearsEndofLamentation
 */
@Component
public class Quest3441000YearsEndofLamentation extends Quest {

	public static final int bng = 4269;
	public static final int bnh = 4270;
	public static final int bni = 4271;
	public static final int bnj = 4272;
	public static final int bnk = 4273;
	public static final int CHANCE = 36;
	public static final int bnl = 1000;
	public static final int GILMORE = 30754;
	public static final int bnm = 30756;
	public static final int ORVEN = 30857;
	public static final int bnn = 30623;
	public static final int bno = 30704;

	public Quest3441000YearsEndofLamentation(QuestManager questManager) {
	super(344, "344_1000YearsEndofLamentation", "344_1000YearsEndofLamentation");
		this.addStartNpc(30754);
		this.addTalkId(30756);
		this.addTalkId(30857);
		this.addTalkId(30704);
		this.addTalkId(30623);
		int n = 20236;
		while (n < 20241) {
		this.addKillId(n++);
		}
		this.addQuestItem(4269, 4270, 4271, 4272, 4273);
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
        long l = qs.getQuestItemsCount(4269);
        int n = qs.getCond();
        int n2 = pc.getLevel();
        if (event.equalsIgnoreCase("30754-04.htm")) {
            if (n2 >= 48 && n == 0) {
                qs.setState(State.STARTED);
                qs.setCond(1);
                qs.playSound(QuestState.SOUND_ACCEPT);
            } else {
                string2 = "noquest";
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("30754-08.htm")) {
            qs.exitQuest(true);
            qs.playSound(QuestState.SOUND_FINISH);
        } else if (event.equalsIgnoreCase("30754-06.htm") && n == 1) {
            if (l == 0L) {
                string2 = "30754-06a.htm";
            } else {
                if ((long)ThreadLocalRandom.current().nextInt((int)(1000.0 / qs.getRateQuestsAdenaReward())) >= l) {
                    qs.giveItems(57, l * 60L);
                } else {
                    string2 = "30754-10.htm";
                    qs.set("ok", "1");
                    qs.set("amount", this.str(l));
                }
                qs.takeItems(4269, -1L);
            }
        } else if (event.equalsIgnoreCase("30754-11.htm") && n == 1) {
            if (qs.getInt("ok") != 1) {
                string2 = "noquest";
            } else {
                int n3 = ThreadLocalRandom.current().nextInt(100);
                qs.setCond(2);
                qs.unset("ok");
                if (n3 < 25) {
                    string2 = "30754-12.htm";
                    qs.giveItems(4270, 1L);
                } else if (n3 < 50) {
                    string2 = "30754-13.htm";
                    qs.giveItems(4271, 1L);
                } else if (n3 < 75) {
                    string2 = "30754-14.htm";
                    qs.giveItems(4272, 1L);
                } else {
                    qs.giveItems(4273, 1L);
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
        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getStateId();
        int n3 = qs.getCond();
        long l = qs.getQuestItemsCount(4269);
        if (n2 == 1) {
            if (pc.getLevel() >= 48) {
                html = "30754-02.htm";
            } else {
                html = "30754-01.htm";
                qs.exitQuest(true);
            }
        } else if (n == 30754 && n3 == 1) {
            html = l > 0L ? "30754-05.htm" : "30754-09.htm";
        } else if (n3 == 2) {
            if (n == 30754) {
                html = "30754-15.htm";
            } else if (this.f(qs, n)) {
                html = this.str(n) + "-01.htm";
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
        } else if (n3 == 3) {
            if (n == 30754) {
                int n4 = qs.getInt("amount");
                int n5 = qs.getInt("mission");
                int n6 = 0;
                if (n5 == 1) {
                    n6 = 1500;
                } else if (n5 == 2) {
                    qs.giveItems(4044, 1L);
                } else if (n5 == 3) {
                    qs.giveItems(4043, 1L);
                } else if (n5 == 4) {
                    qs.giveItems(4042, 1L);
                }
                if (n4 > 0) {
                    qs.unset("amount");
                    qs.giveItems(57, n4 * 50 + n6, true);
                }
                html = "30754-16.htm";
                qs.setCond(1);
                qs.unset("mission");
            } else {
                html = this.str(n) + "-02.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getCond() == 1) {
            qs.rollAndGive(4269, 1, 36 + (npc.getNpcId() - 20234) * 2);
        }
        return null;
    
	}

private boolean f(QuestState qs, int n) {
        boolean bl = false;
        int n2 = ThreadLocalRandom.current().nextInt(100);
        if (n == 30857 && qs.getQuestItemsCount(4273) > 0L) {
            qs.set("mission", "1");
            qs.takeItems(4273, -1L);
            bl = true;
            if (n2 < 50) {
                qs.giveItems(1875, 19L);
            } else if (n2 < 70) {
                qs.giveItems(952, 5L);
            } else {
                qs.giveItems(2437, 1L);
            }
        } else if (n == 30704 && qs.getQuestItemsCount(4272) > 0L) {
            qs.set("mission", "2");
            qs.takeItems(4272, -1L);
            bl = true;
            if (n2 < 45) {
                qs.giveItems(1882, 70L);
            } else if (n2 < 95) {
                qs.giveItems(1881, 50L);
            } else {
                qs.giveItems(191, 1L);
            }
        } else if (n == 30623 && qs.getQuestItemsCount(4271) > 0L) {
            qs.set("mission", "3");
            qs.takeItems(4271, -1L);
            bl = true;
            if (n2 < 50) {
                qs.giveItems(1874, 25L);
            } else if (n2 < 75) {
                qs.giveItems(1887, 10L);
            } else if (n2 < 99) {
                qs.giveItems(951, 1L);
            } else {
                qs.giveItems(133, 1L);
            }
        } else if (n == 30756 && qs.getQuestItemsCount(4270) > 0L) {
            qs.set("mission", "4");
            qs.takeItems(4270, -1L);
            bl = true;
            if (n2 < 40) {
                qs.giveItems(1879, 55L);
            } else if (n2 < 90) {
                qs.giveItems(951, 1L);
            } else {
                qs.giveItems(885, 1L);
            }
        }
        return bl;
    }

}
