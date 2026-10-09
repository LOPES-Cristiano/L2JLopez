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
 * Quest 343 - 343_UndertheShadowoftheIvoryTower
 */
@Component
public class Quest343UndertheShadowoftheIvoryTower extends Quest {

	public static final int CEMA = 30834;
	public static final int ICARUS = 30835;
	public static final int MARSHA = 30934;
	public static final int TRUMPIN = 30935;
	public static final int[] MOBS = new int[]{20563, 20564, 20565, 20566};
	public static final int ORB = 4364;
	public static final int ECTOPLASM = 4365;
	public static final int[] AllowClass = new int[]{11, 12, 13, 14, 26, 27, 28, 39, 40, 41};
	public static final int CHANCE = 50;

	public Quest343UndertheShadowoftheIvoryTower(QuestManager questManager) {
	super(343, "343_UndertheShadowoftheIvoryTower", "343_UndertheShadowoftheIvoryTower");
		this.addStartNpc(30834);
		this.addTalkId(30834);
		this.addTalkId(30835);
		this.addTalkId(30934);
		this.addTalkId(30935);
		for (int n : this.MOBS) {
		this.addKillId(n);
		}
		this.addQuestItem(4364);
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
        int n = ThreadLocalRandom.current().nextInt(3);
        int n2 = ThreadLocalRandom.current().nextInt(2);
        long l = qs.getQuestItemsCount(4364);
        if (event.equalsIgnoreCase("30834-03.htm")) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if (event.equalsIgnoreCase("30834-08.htm")) {
            if (l > 0L) {
                qs.giveItems(57, l * 120L);
                qs.takeItems(4364, -1L);
            } else {
                string2 = "30834-08.htm";
            }
        } else if (event.equalsIgnoreCase("30834-09.htm")) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
        } else if (event.equalsIgnoreCase("30934-02.htm") || event.equalsIgnoreCase("30934-03.htm")) {
            if (l < 10L) {
                string2 = "noorbs.htm";
            } else if (event.equalsIgnoreCase("30934-03.htm")) {
                if (l >= 10L) {
                    qs.takeItems(4364, 10L);
                    qs.set("playing", "1");
                } else {
                    string2 = "noorbs.htm";
                }
            }
        } else if (event.equalsIgnoreCase("30934-04.htm")) {
            if (qs.getInt("playing") > 0) {
                if (n == 0) {
                    string2 = "30934-05.htm";
                    qs.giveItems(4364, 10L);
                } else if (n == 1) {
                    string2 = "30934-06.htm";
                } else {
                    string2 = "30934-04.htm";
                    qs.giveItems(4364, 20L);
                }
                qs.unset("playing");
            } else {
                string2 = "Player is cheating";
                qs.takeItems(4364, -1L);
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("30934-05.htm")) {
            if (qs.getInt("playing") > 0) {
                if (n == 0) {
                    string2 = "30934-04.htm";
                    qs.giveItems(4364, 20L);
                } else if (n == 1) {
                    string2 = "30934-05.htm";
                    qs.giveItems(4364, 10L);
                } else {
                    string2 = "30934-06.htm";
                }
                qs.unset("playing");
            } else {
                string2 = "Player is cheating";
                qs.takeItems(4364, -1L);
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("30934-06.htm")) {
            if (qs.getInt("playing") > 0) {
                if (n == 0) {
                    string2 = "30934-04.htm";
                    qs.giveItems(4364, 20L);
                } else if (n == 1) {
                    string2 = "30934-06.htm";
                } else {
                    string2 = "30934-05.htm";
                    qs.giveItems(4364, 10L);
                }
                qs.unset("playing");
            } else {
                string2 = "Player is cheating";
                qs.takeItems(4364, -1L);
                qs.exitQuest(true);
            }
        } else if (event.equalsIgnoreCase("30935-02.htm") || event.equalsIgnoreCase("30935-03.htm")) {
            qs.unset("toss");
            if (l < 10L) {
                string2 = "noorbs.htm";
            }
        } else if (event.equalsIgnoreCase("30935-05.htm")) {
            if (l >= 10L) {
                if (n2 == 0) {
                    int n3 = qs.getInt("toss");
                    if (n3 == 4) {
                        qs.unset("toss");
                        qs.giveItems(4364, 150L);
                        string2 = "30935-07.htm";
                    } else {
                        qs.set("toss", String.valueOf(n3 + 1));
                        string2 = "30935-04.htm";
                    }
                } else {
                    qs.unset("toss");
                    qs.takeItems(4364, 10L);
                }
            } else {
                string2 = "noorbs.htm";
            }
        } else if (event.equalsIgnoreCase("30935-06.htm")) {
            if (l >= 10L) {
                int n4 = qs.getInt("toss");
                qs.unset("toss");
                if (n4 == 1) {
                    qs.giveItems(4364, 10L);
                } else if (n4 == 2) {
                    qs.giveItems(4364, 30L);
                } else if (n4 == 3) {
                    qs.giveItems(4364, 70L);
                } else if (n4 == 4) {
                    qs.giveItems(4364, 150L);
                }
            } else {
                string2 = "noorbs.htm";
            }
        } else if (event.equalsIgnoreCase("30835-02.htm")) {
            if (qs.getQuestItemsCount(4365) > 0L) {
                qs.takeItems(4365, 1L);
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 <= 119) {
                    qs.giveItems(955, 1L);
                } else if (n5 <= 169) {
                    qs.giveItems(951, 1L);
                } else if (n5 <= 329) {
                    qs.giveItems(2511, ThreadLocalRandom.current().nextInt(200) + 401);
                } else if (n5 <= 559) {
                    qs.giveItems(2510, ThreadLocalRandom.current().nextInt(200) + 401);
                } else if (n5 <= 561) {
                    qs.giveItems(316, 1L);
                } else if (n5 <= 578) {
                    qs.giveItems(630, 1L);
                } else if (n5 <= 579) {
                    qs.giveItems(188, 1L);
                } else if (n5 <= 581) {
                    qs.giveItems(885, 1L);
                } else if (n5 <= 582) {
                    qs.giveItems(103, 1L);
                } else if (n5 <= 584) {
                    qs.giveItems(917, 1L);
                } else {
                    qs.giveItems(736, 1L);
                }
            } else {
                string2 = "30835-03.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc != null ? npc.getNpcId() : 0;
        String html = "noquest";
        int n2 = qs.getStateId();
        if (n == 30834) {
            if (n2 != 2) {
                for (int n3 : this.AllowClass) {
                    if (pc.classId() != n3 || pc.getLevel() < 40) continue;
                    html = "30834-01.htm";
                }
                if (!html.equals("30834-01.htm")) {
                    html = "30834-07.htm";
                    qs.exitQuest(true);
                }
            } else {
                html = qs.getQuestItemsCount(4364) > 0L ? "30834-06.htm" : "30834-05.htm";
            }
        } else if (n == 30835) {
            html = "30835-01.htm";
        } else if (n == 30934) {
            html = "30934-01.htm";
        } else if (n == 30935) {
            html = "30935-01.htm";
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if ((ThreadLocalRandom.current().nextDouble(100.0) < (50))) {
            qs.giveItems(4364, 1L);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
