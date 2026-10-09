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
 * Quest 299 - 299_GatherIngredientsforPie
 */
@Component
public class Quest299GatherIngredientsforPie extends Quest {

	public static final int aQK = 30620;
	public static final int Lara = 30063;
	public static final int aQJ = 30466;
	public static final int bfb = 20934;
	public static final int bfc = 20935;
	public static final int bfd = 1865;
	public static final int bfe = 7136;
	public static final int bff = 7137;
	public static final int bfg = 7138;
	public static final int bfh = 55;
	public static final int bfi = 70;

	public Quest299GatherIngredientsforPie(QuestManager questManager) {
		super(299, "299_GatherIngredientsforPie", "299_GatherIngredientsforPie");
		addStartNpc(30620);
		addTalkNpc(30063);
		addTalkNpc(30466);
		addKillId(20934);
		addKillId(20935);
		registerQuestItems(7136, 7137, 7138);
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

        int n = qs.getStateId();
        int n2 = qs.getCond();
        if (event.equalsIgnoreCase("emilly_q0299_0104.htm") && n == 1) {
            qs.setState(State.STARTED);
            qs.setCond(1);
            qs.playSound("QuestState.SOUND_ACCEPT");
        } else if (event.equalsIgnoreCase("emilly_q0299_0201.htm") && n == 2) {
            if (qs.getQuestItemsCount(7138) < 100L) {
                return "emilly_q0299_0202.htm";
            }
            qs.takeItems(7138, -1L);
            qs.setCond(3);
        } else if (event.equalsIgnoreCase("lars_q0299_0301.htm") && n == 2 && n2 == 3) {
            qs.giveItems(7137, 1L);
            qs.setCond(4);
        } else if (event.equalsIgnoreCase("emilly_q0299_0401.htm") && n == 2) {
            if (qs.getQuestItemsCount(7137) < 1L) {
                return "emilly_q0299_0402.htm";
            }
            qs.takeItems(7137, -1L);
            qs.setCond(5);
        } else if (event.equalsIgnoreCase("guard_bright_q0299_0501.htm") && n == 2 && n2 == 5) {
            qs.giveItems(7136, 1L);
            qs.setCond(6);
        } else if (event.equalsIgnoreCase("emilly_q0299_0601.htm") && n == 2) {
            if (qs.getQuestItemsCount(7136) < 1L) {
                return "emilly_q0299_0602.htm";
            }
            int n3 = ThreadLocalRandom.current().nextInt(1000);
            if (n3 < 400) {
                qs.giveItems(57, 25000L);
            } else if (n3 < 550) {
                qs.giveItems(1865, 50L);
            } else if (n3 < 700) {
                qs.giveItems(1870, 50L);
            } else if (n3 < 850) {
                qs.giveItems(1869, 50L);
            } else if (n3 < 1000) {
                qs.giveItems(1871, 50L);
            }
            qs.takeItems(7136, -1L);
            qs.playSound("QuestState.SOUND_FINISH");
            qs.exitQuest(true);
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 != 30620) {
                return "noquest";
            }
            if (pc.getLevel() >= 34) {
                qs.setCond(0);
                return "emilly_q0299_0101.htm";
            }
            qs.exitQuest(true);
            return "emilly_q0299_0102.htm";
        }
        int n3 = qs.getCond();
        if (n2 == 30620 && n == 2) {
            if (n3 == 1 && qs.getQuestItemsCount(7138) <= 99L) {
                return "emilly_q0299_0106.htm";
            }
            if (n3 == 2 && qs.getQuestItemsCount(7138) >= 100L) {
                return "emilly_q0299_0105.htm";
            }
            if (n3 == 3 && qs.getQuestItemsCount(7137) == 0L) {
                return "emilly_q0299_0203.htm";
            }
            if (n3 == 4 && qs.getQuestItemsCount(7137) == 1L) {
                return "emilly_q0299_0301.htm";
            }
            if (n3 == 5 && qs.getQuestItemsCount(7136) == 0L) {
                return "emilly_q0299_0403.htm";
            }
            if (n3 == 6 && qs.getQuestItemsCount(7136) == 1L) {
                return "emilly_q0299_0501.htm";
            }
        }
        if (n2 == 30063 && n == 2 && n3 == 3) {
            return "lars_q0299_0201.htm";
        }
        if (n2 == 30063 && n == 2 && n3 == 4) {
            return "lars_q0299_0302.htm";
        }
        if (n2 == 30466 && n == 2 && n3 == 5) {
            return "guard_bright_q0299_0401.htm";
        }
        if (n2 == 30466 && n == 2 && n3 == 5) {
            return "guard_bright_q0299_0502.htm";
        }
        return "noquest";
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED || qs.getCond() != 1 || qs.getQuestItemsCount(7138) >= 100L) {
            return null;
        }
        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20934 && (ThreadLocalRandom.current().nextInt(100) < 55) || n == 20935 && (ThreadLocalRandom.current().nextInt(100) < 70)) {
            qs.giveItems(7138, 1L);
            if (qs.getQuestItemsCount(7138) < 100L) {
                qs.playSound("QuestState.SOUND_ITEMGET");
            } else {
                qs.setCond(2);
                qs.playSound("QuestState.SOUND_MIDDLE");
            }
        }
        return null;
    
	}

}
