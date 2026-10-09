package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 153 - Deliver Goods
 */
@Component
public class Quest153DeliverGoods extends Quest {

	public static final int DELIVERY_LIST = 1012;
	public static final int HEAVY_WOOD_BOX = 1013;
	public static final int CLOTH_BUNDLE = 1014;
	public static final int CLAY_POT = 1015;
	public static final int JACKSONS_RECEIPT = 1016;
	public static final int SILVIAS_RECEIPT = 1017;
	public static final int RANTS_RECEIPT = 1018;
	public static final int RING_OF_KNOWLEDGE = 875;

	public Quest153DeliverGoods(QuestManager questManager) {
		super(153, "153_DeliverGoods", "Deliver Goods");
		addStartNpc(30041);
		addTalkId(30002);
		addTalkId(30003);
		addTalkId(30054);
		registerQuestItems(HEAVY_WOOD_BOX, CLOTH_BUNDLE, CLAY_POT, DELIVERY_LIST, JACKSONS_RECEIPT, SILVIAS_RECEIPT, RANTS_RECEIPT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}

        String string2 = event;
        if (event.equals("30041-04.htm")) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            if (qs.getQuestItemsCount((int)(DELIVERY_LIST)) == 0) {
                qs.giveItems((int)(DELIVERY_LIST), (int)(1));
            }
            if (qs.getQuestItemsCount((int)(HEAVY_WOOD_BOX)) == 0) {
                qs.giveItems((int)(HEAVY_WOOD_BOX), (int)(1));
            }
            if (qs.getQuestItemsCount((int)(CLOTH_BUNDLE)) == 0) {
                qs.giveItems((int)(CLOTH_BUNDLE), (int)(1));
            }
            if (qs.getQuestItemsCount((int)(CLAY_POT)) == 0) {
                qs.giveItems((int)(CLAY_POT), (int)(1));
            }
            string2 = "30041-04.htm";
        }
        return string2;
    
	}


	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = npc.getNpcId();
        String html = "noquest";
        int n2 = qs.getCond();
        if (n == 30041) {
            if (n2 == 0) {
                if (pc.level() >= 2) {
                    html = "30041-03.htm";
                    return html;
                }
                html = "30041-02.htm";
                qs.exitQuest(true);
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(JACKSONS_RECEIPT)) + qs.getQuestItemsCount((int)(SILVIAS_RECEIPT)) + qs.getQuestItemsCount((int)(RANTS_RECEIPT)) == 0) {
                html = "30041-05.htm";
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(JACKSONS_RECEIPT)) + qs.getQuestItemsCount((int)(SILVIAS_RECEIPT)) + qs.getQuestItemsCount((int)(RANTS_RECEIPT)) == 3) {
                qs.giveItems((int)(RING_OF_KNOWLEDGE), (int)(1));
                qs.takeItems((int)(DELIVERY_LIST), (int)(-1));
                qs.takeItems((int)(JACKSONS_RECEIPT), (int)(-1));
                qs.takeItems((int)(SILVIAS_RECEIPT), (int)(-1));
                qs.takeItems((int)(RANTS_RECEIPT), (int)(-1));
                qs.addExpAndSp(600, 0);
                qs.playSound(QuestState.SOUND_FINISH);
                html = "30041-06.htm";
                qs.exitQuest(false);
            }
        } else if (n == 30002) {
            if (n2 == 1 && qs.getQuestItemsCount((int)(HEAVY_WOOD_BOX)) == 1) {
                qs.takeItems((int)(HEAVY_WOOD_BOX), (int)(-1));
                if (qs.getQuestItemsCount((int)(JACKSONS_RECEIPT)) == 0) {
                    qs.giveItems((int)(JACKSONS_RECEIPT), (int)(1));
                }
                html = "30002-01.htm";
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(JACKSONS_RECEIPT)) > 0) {
                html = "30002-02.htm";
            }
        } else if (n == 30003) {
            if (n2 == 1 && qs.getQuestItemsCount((int)(CLOTH_BUNDLE)) == 1) {
                qs.takeItems((int)(CLOTH_BUNDLE), (int)(-1));
                if (qs.getQuestItemsCount((int)(SILVIAS_RECEIPT)) == 0) {
                    qs.giveItems((int)(SILVIAS_RECEIPT), (int)(1));
                    if (pc.isMage()) {
                        qs.giveItems((int)(2509), (int)(3));
                    } else {
                        qs.giveItems((int)(1835), (int)(6));
                    }
                }
                html = "30003-01.htm";
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(SILVIAS_RECEIPT)) > 0) {
                html = "30003-02.htm";
            }
        } else if (n == 30054) {
            if (n2 == 1 && qs.getQuestItemsCount((int)(CLAY_POT)) == 1) {
                qs.takeItems((int)(CLAY_POT), (int)(-1));
                if (qs.getQuestItemsCount((int)(RANTS_RECEIPT)) == 0) {
                    qs.giveItems((int)(RANTS_RECEIPT), (int)(1));
                }
                html = "30054-01.htm";
            } else if (n2 == 1 && qs.getQuestItemsCount((int)(RANTS_RECEIPT)) > 0) {
                html = "30054-02.htm";
            }
        }
        return html;
    
	}


}
