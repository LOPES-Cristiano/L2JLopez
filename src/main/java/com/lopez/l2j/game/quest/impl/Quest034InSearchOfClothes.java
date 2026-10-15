package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 34: In Search of Clothes
 * Armor Merchant Radia, Trader Varan e Magister Ralford.
 */
@Component
public class Quest034InSearchOfClothes extends Quest {

	public static final int QUEST_ID = 34;
	public static final String QUEST_NAME = "034_InSearchofClothes";

	public static final int START_NPC = 30088;
	public static final int RADIA = 30088;
	public static final int VARAN = 30294;
	public static final int RALFORD = 30165;

	public static final int TRISALIM_SPIDER = 20560;
	public static final int TRISALIM_TARANTULA = 20561;

	public static final int SUEDE = 1866;
	public static final int THREAD = 1868;
	public static final int SPINNERET = 7528;
	public static final int SPIDERSILK = 7161;
	public static final int MYSTERIOUS_CLOTH = 7076;

	@Autowired
	public Quest034InSearchOfClothes(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "In Search of Clothes");
		addStartNpc(RADIA);
		addTalkId(RADIA, VARAN, RALFORD);
		addKillId(TRISALIM_SPIDER, TRISALIM_TARANTULA);
		registerQuestItems(SPINNERET, SPIDERSILK);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "30088-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30088-1.htm".equalsIgnoreCase(event) ? "30088-1.htm" : "radia_q0034_0104.htm";
		} else if ("30294-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30294-1.htm";
		} else if ("30088-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30088-3.htm";
		} else if ("30165-1.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30165-1.htm";
		} else if ("30165-3.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.takeItems(SPINNERET, 10);
			qs.giveItems(SPIDERSILK, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30165-3.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_varanket_q0034_0201.htm";
			} else if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "radia_q0034_0301.htm";
			} else if (cond == 3) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "rapin_q0034_0401.htm";
			} else if (cond == 5 && qs.getQuestItemsCount(SPINNERET) >= 10) {
				qs.setCond(6);
				qs.takeItems(SPINNERET, 10);
				qs.giveItems(SPIDERSILK, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "rapin_q0034_0501.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event) || "30088-5.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SPIDERSILK) && qs.getQuestItemsCount(SUEDE) >= 3000 && qs.getQuestItemsCount(THREAD) >= 5000) {
				qs.takeItems(SPIDERSILK, 1);
				qs.takeItems(SUEDE, 3000);
				qs.takeItems(THREAD, 5000);
				qs.giveItems(MYSTERIOUS_CLOTH, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30088-5.htm".equalsIgnoreCase(event) ? "30088-5.htm" : "radia_q0034_0601.htm";
			}
			return "radia_q0034_0602.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case RADIA:
				if (cond == 0) {
					if (pc.level() >= 60) return "radia_q0034_0101.htm";
					qs.exitQuest(true);
					return "radia_q0034_0103.htm";
				} else if (cond == 1) {
					return "radia_q0034_0105.htm";
				} else if (cond == 2) {
					return "radia_q0034_0201.htm";
				} else if (cond >= 3 && cond <= 5) {
					return "radia_q0034_0302.htm";
				} else if (cond == 6) {
					if (qs.hasQuestItems(SPIDERSILK) && qs.getQuestItemsCount(SUEDE) >= 3000 && qs.getQuestItemsCount(THREAD) >= 5000) {
						return "radia_q0034_0501.htm";
					}
					return "radia_q0034_0502.htm";
				}
				break;
			case VARAN:
				if (cond == 1) {
					return "trader_varanket_q0034_0101.htm";
				} else if (cond >= 2) {
					return "trader_varanket_q0034_0202.htm";
				}
				break;
			case RALFORD:
				if (cond == 3) {
					return "rapin_q0034_0301.htm";
				} else if (cond == 4) {
					return "rapin_q0034_0403.htm";
				} else if (cond == 5 && qs.getQuestItemsCount(SPINNERET) >= 10) {
					return "rapin_q0034_0402.htm";
				} else if (cond >= 6) {
					return "rapin_q0034_0503.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs.getCond() == 4) {
			long count = qs.getQuestItemsCount(SPINNERET);
			if (count < 10) {
				qs.giveItems(SPINNERET, 1);
				if (count + 1 >= 10) {
					qs.setCond(5);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
