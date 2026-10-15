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
 * Quest 108: Jumble, Tumble, Diamond Fuss
 */
@Component
public class Quest108JumbleTumbleDiamondFuss extends Quest {

	public static final int QUEST_ID = 108;
	public static final String QUEST_NAME = "108_JumbleTumbleDiamondFuss";
	public static final int START_NPC = 30523;

	public static final int GOUPH = 30523;
	public static final int REEP = 30516;
	public static final int MURDOC = 30521;
	public static final int AIRY = 30522;
	public static final int BRUNON = 30526;
	public static final int MARON = 30529;
	public static final int TOROCCO = 30555;

	public static final int GOUPH_CONTRACT = 1559;
	public static final int REEP_CONTRACT = 1560;
	public static final int ELVEN_WINE = 1561;
	public static final int BRUNON_DICE = 1562;
	public static final int BRUNON_CONTRACT = 1563;
	public static final int AQUAMARINE = 1564;
	public static final int CHRYSOBERYL = 1565;
	public static final int GEM_BOX = 1566;
	public static final int COAL_PIECE = 1567;
	public static final int BRUNON_LETTER = 1568;
	public static final int BERRY_TART = 1569;
	public static final int BAT_DIAGRAM = 1570;
	public static final int STAR_DIAMOND = 1571;

	public static final int SILVERSMITH_HAMMER_REWARD = 756;
	public static final int SILVERSMITH_HAMMER = 1511;

	@Autowired
	public Quest108JumbleTumbleDiamondFuss(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Jumble, Tumble, Diamond Fuss");
		addStartNpc(START_NPC);
		addTalkId(GOUPH, REEP, MURDOC, AIRY, BRUNON, MARON, TOROCCO);
		addKillId(20323, 20324, 20480);
		registerQuestItems(GOUPH_CONTRACT, REEP_CONTRACT, ELVEN_WINE, BRUNON_DICE,
				BRUNON_CONTRACT, AQUAMARINE, CHRYSOBERYL, GEM_BOX, COAL_PIECE,
				BRUNON_LETTER, BERRY_TART, BAT_DIAGRAM, STAR_DIAMOND);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		if ("quest_accept".equalsIgnoreCase(event)
				|| "30523-03.htm".equalsIgnoreCase(event)
				|| "collector_gouph_q0108_03.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(GOUPH_CONTRACT)) {
				qs.giveItems(GOUPH_CONTRACT, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "collector_gouph_q0108_03.htm";
		} else if ("30555-02.htm".equalsIgnoreCase(event)
				|| "carrier_torocco_q0108_02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(REEP_CONTRACT, -1);
			qs.giveItems(ELVEN_WINE, 1);
			qs.setCond(3);
			return event;
		} else if ("30526-02.htm".equalsIgnoreCase(event)
				|| "blacksmith_bronp_q0108_02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BRUNON_DICE, -1);
			qs.giveItems(BRUNON_CONTRACT, 1);
			qs.setCond(5);
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) {
			return "noquest";
		}

		if (qs.isCompleted()) {
			return "completed";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == GOUPH) {
			if (cond == 0) {
				if (pc.race() != 4) {
					qs.exitQuest(true);
					return "collector_gouph_q0108_00.htm";
				}
				if (pc.level() >= 10) {
					return "collector_gouph_q0108_02.htm";
				}
				qs.exitQuest(true);
				return "collector_gouph_q0108_01.htm";
			} else if (cond == 1) {
				return "collector_gouph_q0108_04.htm";
			} else if (cond == 2) {
				if (!qs.hasQuestItems(REEP_CONTRACT)) {
					qs.giveItems(SILVERSMITH_HAMMER_REWARD, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "collector_gouph_q0108_07.htm";
				}
				return "collector_gouph_q0108_05.htm";
			} else if (cond > 2 && cond < 7) {
				return "collector_gouph_q0108_05.htm";
			} else if (cond == 7) {
				qs.takeItems(GEM_BOX, -1);
				qs.giveItems(COAL_PIECE, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collector_gouph_q0108_06.htm";
			} else if (cond > 7 && cond < 12) {
				return "collector_gouph_q0108_07.htm";
			} else if (cond == 12) {
				qs.takeItems(STAR_DIAMOND, -1);
				qs.giveItems(SILVERSMITH_HAMMER_REWARD, 1);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collector_gouph_q0108_08.htm";
			}
		} else if (npcId == REEP) {
			if (cond == 1) {
				qs.takeItems(GOUPH_CONTRACT, -1);
				qs.giveItems(REEP_CONTRACT, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_reep_q0108_01.htm";
			} else if (cond >= 2) {
				return "trader_reep_q0108_02.htm";
			}
		} else if (npcId == TOROCCO) {
			if (cond == 2) {
				return "carrier_torocco_q0108_01.htm";
			} else if (cond == 3) {
				return "carrier_torocco_q0108_03.htm";
			} else if (cond == 7) {
				return "carrier_torocco_q0108_04.htm";
			} else if (cond > 7) {
				return "carrier_torocco_q0108_05.htm";
			}
		} else if (npcId == MARON) {
			if (cond == 3) {
				qs.takeItems(ELVEN_WINE, -1);
				qs.giveItems(BRUNON_DICE, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "miner_maron_q0108_01.htm";
			} else if (cond == 4) {
				return "miner_maron_q0108_02.htm";
			} else if (cond > 4) {
				return "miner_maron_q0108_03.htm";
			}
		} else if (npcId == BRUNON) {
			if (cond == 1) {
				qs.takeItems(GOUPH_CONTRACT, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_bronp_q0108_02.htm";
			} else if (cond == 4) {
				return "blacksmith_bronp_q0108_01.htm";
			} else if (cond == 5) {
				return "blacksmith_bronp_q0108_03.htm";
			} else if (cond == 6) {
				qs.takeAllItems(BRUNON_CONTRACT, AQUAMARINE, CHRYSOBERYL);
				qs.giveItems(GEM_BOX, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_bronp_q0108_04.htm";
			} else if (cond == 7) {
				return "blacksmith_bronp_q0108_05.htm";
			} else if (cond == 8) {
				qs.takeItems(COAL_PIECE, -1);
				qs.giveItems(BRUNON_LETTER, 1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "blacksmith_bronp_q0108_06.htm";
			} else if (cond == 9) {
				return "blacksmith_bronp_q0108_07.htm";
			} else if (cond > 9) {
				return "blacksmith_bronp_q0108_08.htm";
			}
		} else if (npcId == MURDOC) {
			if (cond == 9) {
				qs.takeItems(BRUNON_LETTER, -1);
				qs.giveItems(BERRY_TART, 1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "warehouse_murphrin_q0108_01.htm";
			} else if (cond == 10) {
				return "warehouse_murphrin_q0108_02.htm";
			} else if (cond > 10) {
				return "warehouse_murphrin_q0108_03.htm";
			}
		} else if (npcId == AIRY) {
			if (cond == 10) {
				qs.takeItems(BERRY_TART, -1);
				qs.giveItems(BAT_DIAGRAM, 1);
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "warehouse_airy_q0108_01.htm";
			} else if (cond == 11) {
				return "warehouse_airy_q0108_02.htm";
			} else if (cond == 12) {
				return "warehouse_airy_q0108_03.htm";
			}
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		if ((npcId == 20323 || npcId == 20324) && cond == 5) {
			double chance = npcId == 20323 ? 80.0 : 60.0;
			qs.rollAndGive(AQUAMARINE, 1, 1, 10, chance);
			qs.rollAndGive(CHRYSOBERYL, 1, 1, 10, chance);
			if (qs.getQuestItemsCount(AQUAMARINE) >= 10 && qs.getQuestItemsCount(CHRYSOBERYL) >= 10) {
				qs.setCond(6);
			}
		} else if (npcId == 20480 && cond == 11) {
			if (qs.rollAndGive(STAR_DIAMOND, 1, 1, 1, 50.0)) {
				qs.takeItems(BAT_DIAGRAM, -1);
				qs.setCond(12);
			}
		}
		return null;
	}
}
