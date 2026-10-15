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
 * Quest 103: Spirit of Craftsman
 */
@Component
public class Quest103SpiritOfCraftsman extends Quest {

	public static final int QUEST_ID = 103;
	public static final String QUEST_NAME = "103_SpiritOfCraftsman";
	public static final int START_NPC = 30307;

	public static final int KARROD = 30307;
	public static final int CECKTINON = 30132;
	public static final int HARNE = 30144;

	public static final int KARROD_LETTER = 968;
	public static final int CECKTINON_VOUCHER_1 = 969;
	public static final int CECKTINON_VOUCHER_2 = 970;
	public static final int SOUL_CATCHER = 971;
	public static final int PRESERVING_OIL = 972;
	public static final int ZOMBIE_HEAD = 973;
	public static final int STEELBENDER_HEAD = 974;
	public static final int BONE_FRAGMENT = 1107;

	public static final int BLOOD_SABER = 747;
	public static final int BLOODSABER = 975;

	@Autowired
	public Quest103SpiritOfCraftsman(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Spirit of Craftsman");
		addStartNpc(START_NPC);
		addTalkId(KARROD, CECKTINON, HARNE);
		addKillId(20015, 20020, 20455, 20517, 20518);
		registerQuestItems(KARROD_LETTER, CECKTINON_VOUCHER_1, CECKTINON_VOUCHER_2,
				SOUL_CATCHER, PRESERVING_OIL, ZOMBIE_HEAD, STEELBENDER_HEAD, BONE_FRAGMENT);
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
				|| "30307-05.htm".equalsIgnoreCase(event)
				|| "blacksmith_karoyd_q0103_05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(KARROD_LETTER)) {
				qs.giveItems(KARROD_LETTER, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "blacksmith_karoyd_q0103_05.htm";
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

		if (npcId == KARROD) {
			if (cond == 0) {
				if (pc.race() != 2) {
					qs.exitQuest(true);
					return "blacksmith_karoyd_q0103_00.htm";
				}
				if (pc.level() >= 10) {
					return "blacksmith_karoyd_q0103_03.htm";
				}
				qs.exitQuest(true);
				return "blacksmith_karoyd_q0103_02.htm";
			} else if (cond == 1 || cond == 2) {
				return "blacksmith_karoyd_q0103_06.htm";
			} else if (cond == 3) {
				qs.takeItems(CECKTINON_VOUCHER_2, -1);
				qs.giveItems(BLOOD_SABER, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "blacksmith_karoyd_q0103_07.htm";
			} else if (cond > 3 && cond < 8) {
				return "blacksmith_karoyd_q0103_06.htm";
			} else if (cond == 8) {
				qs.takeItems(STEELBENDER_HEAD, -1);
				qs.giveItems(BLOOD_SABER, 1);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "blacksmith_karoyd_q0103_07.htm";
			}
		} else if (npcId == CECKTINON) {
			if (cond == 1) {
				if (qs.hasQuestItems(KARROD_LETTER)) {
					qs.takeItems(KARROD_LETTER, 1);
					qs.giveItems(CECKTINON_VOUCHER_1, 1);
				}
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "cecktinon_q0103_01.htm";
			} else if (cond >= 2 && cond < 5) {
				return "cecon_q0103_02.htm";
			} else if (cond == 5) {
				qs.takeItems(SOUL_CATCHER, 1);
				qs.giveItems(PRESERVING_OIL, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "cecon_q0103_03.htm";
			} else if (cond == 6) {
				return "cecon_q0103_04.htm";
			} else if (cond == 7) {
				qs.takeItems(ZOMBIE_HEAD, 1);
				qs.giveItems(STEELBENDER_HEAD, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "cecon_q0103_05.htm";
			} else if (cond == 8) {
				return "cecon_q0103_06.htm";
			}
		} else if (npcId == HARNE) {
			if (cond == 2) {
				if (qs.hasQuestItems(CECKTINON_VOUCHER_1)) {
					qs.takeItems(CECKTINON_VOUCHER_1, 1);
					qs.giveItems(CECKTINON_VOUCHER_2, 1);
				}
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "harne_q0103_01.htm";
			} else if (cond == 3) {
				return "harne_q0103_02.htm";
			} else if (cond == 4) {
				qs.takeItems(CECKTINON_VOUCHER_2, 1);
				qs.takeItems(BONE_FRAGMENT, -1);
				qs.giveItems(SOUL_CATCHER, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "harne_q0103_03.htm";
			} else if (cond >= 5) {
				return "harne_q0103_04.htm";
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
		if ((npcId == 20517 || npcId == 20518 || npcId == 20455) && cond == 3) {
			if (qs.rollAndGive(BONE_FRAGMENT, 1, 1, 10, 33.0) && qs.getQuestItemsCount(BONE_FRAGMENT) >= 10) {
				qs.setCond(4);
			}
		} else if ((npcId == 20015 || npcId == 20020) && cond == 6) {
			if (qs.rollAndGive(ZOMBIE_HEAD, 1, 1, 1, 33.0)) {
				qs.takeItems(PRESERVING_OIL, 1);
				qs.setCond(7);
			}
		}
		return null;
	}
}
