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
 * Quest 005: Miner's Favor
 * Bolter, Shari, Garita, Brunon e Reed na Dwarf Village.
 */
@Component
public class Quest005MinersFavor extends Quest {

	public static final int QUEST_ID = 5;
	public static final String QUEST_NAME = "005_MinersFavor";

	public static final int BOLTER = 30554;
	public static final int SHARI = 30517;
	public static final int GARITA = 30518;
	public static final int REED = 30520;
	public static final int BRUNON = 30526;

	public static final int BOLTERS_LIST = 1547;
	public static final int BOLTERS_SMELLY_SOCKS = 1548;
	public static final int MINERS_BOOTS = 1549;
	public static final int MINERS_PICK = 1550;
	public static final int BOOMBOOM_POWDER = 1551;
	public static final int REDSTONE_BEER = 1552;
	public static final int NECKLACE = 906;

	@Autowired
	public Quest005MinersFavor(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Miner's Favor");
		addStartNpc(BOLTER);
		addTalkId(BOLTER, SHARI, GARITA, REED, BRUNON);
		registerQuestItems(BOLTERS_LIST, BOLTERS_SMELLY_SOCKS, MINERS_BOOTS, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BOLTERS_LIST, 1);
			qs.giveItems(BOLTERS_SMELLY_SOCKS, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "miner_bolter_q0005_03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == BOLTER) {
			if (cond == 0) {
				if (pc.level() >= 2) return "miner_bolter_q0005_02.htm";
				qs.exitQuest(true);
				return "miner_bolter_q0005_01.htm";
			} else if (cond == 1) {
				return "miner_bolter_q0005_04.htm";
			} else if (cond == 2) {
				if (qs.hasQuestItems(MINERS_BOOTS) && qs.hasQuestItems(MINERS_PICK) && qs.hasQuestItems(BOOMBOOM_POWDER) && qs.hasQuestItems(REDSTONE_BEER)) {
					qs.takeItems(MINERS_BOOTS, -1);
					qs.takeItems(MINERS_PICK, -1);
					qs.takeItems(BOOMBOOM_POWDER, -1);
					qs.takeItems(REDSTONE_BEER, -1);
					qs.takeItems(BOLTERS_LIST, -1);
					qs.giveItems(NECKLACE, 1);
					qs.giveItems(57, 2466);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "miner_bolter_q0005_06.htm";
				}
				return "miner_bolter_q0005_05.htm";
			}
		} else if (npcId == SHARI) {
			if (cond == 1 && !qs.hasQuestItems(BOOMBOOM_POWDER)) {
				qs.giveItems(BOOMBOOM_POWDER, 1);
				checkAll(qs);
				return "trader_chali_q0005_01.htm";
			}
			return "trader_chali_q0005_02.htm";
		} else if (npcId == GARITA) {
			if (cond == 1 && !qs.hasQuestItems(MINERS_BOOTS)) {
				qs.giveItems(MINERS_BOOTS, 1);
				checkAll(qs);
				return "trader_garita_q0005_01.htm";
			}
			return "trader_garita_q0005_02.htm";
		} else if (npcId == REED) {
			if (cond == 1 && !qs.hasQuestItems(REDSTONE_BEER)) {
				qs.giveItems(REDSTONE_BEER, 1);
				checkAll(qs);
				return "warehouse_chief_reed_q0005_01.htm";
			}
			return "warehouse_chief_reed_q0005_02.htm";
		} else if (npcId == BRUNON) {
			if (cond == 1 && qs.hasQuestItems(BOLTERS_SMELLY_SOCKS)) {
				qs.takeItems(BOLTERS_SMELLY_SOCKS, -1);
				qs.giveItems(MINERS_PICK, 1);
				checkAll(qs);
				return "blacksmith_brunon_q0005_01.htm";
			}
			return "blacksmith_brunon_q0005_02.htm";
		}
		return "noquest";
	}

	private void checkAll(QuestState qs) {
		if (qs.hasQuestItems(MINERS_BOOTS) && qs.hasQuestItems(MINERS_PICK) && qs.hasQuestItems(BOOMBOOM_POWDER) && qs.hasQuestItems(REDSTONE_BEER)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else {
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}
}
