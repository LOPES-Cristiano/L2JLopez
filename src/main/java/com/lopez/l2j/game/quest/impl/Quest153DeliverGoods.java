package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
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
		addTalkId(30041, 30002, 30003, 30054);
		registerQuestItems(HEAVY_WOOD_BOX, CLOTH_BUNDLE, CLAY_POT, DELIVERY_LIST, JACKSONS_RECEIPT, SILVIAS_RECEIPT, RANTS_RECEIPT);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30041-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			if (qs.getQuestItemsCount(DELIVERY_LIST) == 0) {
				qs.giveItems(DELIVERY_LIST, 1);
			}
			if (qs.getQuestItemsCount(HEAVY_WOOD_BOX) == 0) {
				qs.giveItems(HEAVY_WOOD_BOX, 1);
			}
			if (qs.getQuestItemsCount(CLOTH_BUNDLE) == 0) {
				qs.giveItems(CLOTH_BUNDLE, 1);
			}
			if (qs.getQuestItemsCount(CLAY_POT) == 0) {
				qs.giveItems(CLAY_POT, 1);
			}
			return "30041-04.htm";
		}
		return event;
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
					return "30041-03.htm";
				}
				qs.exitQuest(true);
				return "30041-02.htm";
			} else if (n2 >= 1) {
				long receipts = qs.getQuestItemsCount(JACKSONS_RECEIPT) + qs.getQuestItemsCount(SILVIAS_RECEIPT) + qs.getQuestItemsCount(RANTS_RECEIPT);
				if (receipts < 3) {
					html = "30041-05.htm";
				} else {
					qs.giveItems(RING_OF_KNOWLEDGE, 2);
					qs.takeItems(DELIVERY_LIST, -1);
					qs.takeItems(JACKSONS_RECEIPT, -1);
					qs.takeItems(SILVIAS_RECEIPT, -1);
					qs.takeItems(RANTS_RECEIPT, -1);
					qs.addExpAndSp(600, 0);
					qs.playSound(QuestState.SOUND_FINISH);
					html = "30041-06.htm";
					qs.exitQuest(false);
				}
			}
		} else if (n == 30002) {
			if (n2 == 1 && qs.getQuestItemsCount(HEAVY_WOOD_BOX) == 1) {
				qs.takeItems(HEAVY_WOOD_BOX, -1);
				if (qs.getQuestItemsCount(JACKSONS_RECEIPT) == 0) {
					qs.giveItems(JACKSONS_RECEIPT, 1);
				}
				if (qs.hasQuestItems(JACKSONS_RECEIPT) && qs.hasQuestItems(SILVIAS_RECEIPT) && qs.hasQuestItems(RANTS_RECEIPT)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				html = "30002-01.htm";
			} else if (n2 >= 1 && qs.getQuestItemsCount(JACKSONS_RECEIPT) > 0) {
				html = "30002-02.htm";
			}
		} else if (n == 30003) {
			if (n2 == 1 && qs.getQuestItemsCount(CLOTH_BUNDLE) == 1) {
				qs.takeItems(CLOTH_BUNDLE, -1);
				if (qs.getQuestItemsCount(SILVIAS_RECEIPT) == 0) {
					qs.giveItems(SILVIAS_RECEIPT, 1);
					if (pc.isMage()) {
						qs.giveItems(2509, 3);
					} else {
						qs.giveItems(1835, 6);
					}
				}
				if (qs.hasQuestItems(JACKSONS_RECEIPT) && qs.hasQuestItems(SILVIAS_RECEIPT) && qs.hasQuestItems(RANTS_RECEIPT)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				html = "30003-01.htm";
			} else if (n2 >= 1 && qs.getQuestItemsCount(SILVIAS_RECEIPT) > 0) {
				html = "30003-02.htm";
			}
		} else if (n == 30054) {
			if (n2 == 1 && qs.getQuestItemsCount(CLAY_POT) == 1) {
				qs.takeItems(CLAY_POT, -1);
				if (qs.getQuestItemsCount(RANTS_RECEIPT) == 0) {
					qs.giveItems(RANTS_RECEIPT, 1);
				}
				if (qs.hasQuestItems(JACKSONS_RECEIPT) && qs.hasQuestItems(SILVIAS_RECEIPT) && qs.hasQuestItems(RANTS_RECEIPT)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
				html = "30054-01.htm";
			} else if (n2 >= 1 && qs.getQuestItemsCount(RANTS_RECEIPT) > 0) {
				html = "30054-02.htm";
			}
		}
		return html;
	}
}
