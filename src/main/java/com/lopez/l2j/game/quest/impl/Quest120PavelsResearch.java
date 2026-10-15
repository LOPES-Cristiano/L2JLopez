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
 * Quest 120: Pavel's Research
 */
@Component
public class Quest120PavelsResearch extends Quest {

	public static final int QUEST_ID = 120;
	public static final String QUEST_NAME = "120_PavelsResearch";
	public static final int START_NPC = 32046;

	// NPCs
	public static final int YUMI = 32041;
	public static final int WEATHER_1 = 32042;
	public static final int WEATHER_2 = 32043;
	public static final int WEATHER_3 = 32044;
	public static final int BOOKSHELF = 32045;
	public static final int STONES = 32046;
	public static final int WENDY = 32047;

	// Items
	public static final int FLOWER_OF_PAVEL = 8290;
	public static final int HEART_OF_ATLANTA = 8291;
	public static final int WENDYS_NECKLACE = 8292;
	public static final int LOCKUP_RESEARCH_REPORT = 8058;
	public static final int RESEARCH_REPORT = 8059;
	public static final int KEY_OF_ENIGMA = 8060;
	public static final int EARRING_OF_BINDING = 854;

	@Autowired
	public Quest120PavelsResearch(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Pavel's Research");
		addStartNpc(32046);
		addStartNpc(32041);
		addTalkId(32041, 32042, 32043, 32044, 32045, 32046, 32047);
		registerQuestItems(8058, 8059, 8060, 8290, 8291, 8292);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "collecter_yumi_q0120_0104.htm";
		} else if ("32041-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32041-03.htm";
		} else if ("32041-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32041-04.htm";
		} else if ("32047-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32047-06.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 14) {
				qs.setCond(15);
				qs.giveItems(LOCKUP_RESEARCH_REPORT, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "drchaos_box_q0120_02.htm";
			}
			return "pavel_atlanta_q0120_05.htm";
		} else if ("reply_2".equalsIgnoreCase(event)) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_12.htm";
			} else if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collecter_yumi_q0120_03.htm";
			} else if (cond == 3 || cond == 4) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_06.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event)) {
			if (cond == 2) {
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collecter_yumi_q0120_05.htm";
			} else if (cond == 5) {
				qs.setCond(6);
				qs.giveItems(FLOWER_OF_PAVEL, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_16.htm";
			} else if (cond == 10) {
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller1_q0120_06.htm";
			} else if (cond == 17) {
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller2_q0120_06.htm";
			} else if (cond == 20) {
				qs.setCond(21);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller3_q0120_06.htm";
			}
		} else if ("reply_4".equalsIgnoreCase(event)) {
			return "chaos_secretary_wendy_q0120_09.htm";
		} else if ("reply_5".equalsIgnoreCase(event)) {
			if (cond == 6) {
				qs.setCond(7);
				qs.takeItems(FLOWER_OF_PAVEL, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_10.htm";
			} else if (cond == 7) {
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collecter_yumi_q0120_13.htm";
			} else if (cond == 9) {
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_25.htm";
			}
		} else if ("reply_6".equalsIgnoreCase(event)) {
			if (cond == 8) {
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_14.htm";
			} else if (cond == 12) {
				qs.setCond(13);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_32.htm";
			} else if (cond == 15) {
				qs.setCond(16);
				qs.giveItems(KEY_OF_ENIGMA, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collecter_yumi_q0120_17.htm";
			}
		} else if ("reply_7".equalsIgnoreCase(event)) {
			if (cond == 13) {
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_18.htm";
			} else if (cond == 16) {
				qs.setCond(17);
				qs.takeItems(KEY_OF_ENIGMA, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "collecter_yumi_q0120_23.htm";
			} else if (cond == 19) {
				qs.setCond(20);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_38.htm";
			}
		} else if ("reply_8".equalsIgnoreCase(event)) {
			if (cond == 22) {
				qs.setCond(23);
				qs.giveItems(HEART_OF_ATLANTA, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "pavel_atlanta_q0120_41.htm";
			} else if (cond == 23) {
				qs.setCond(24);
				qs.takeItems(HEART_OF_ATLANTA, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_26.htm";
			}
			return "collecter_yumi_q0120_30.htm";
		} else if ("reply_9".equalsIgnoreCase(event)) {
			return " chaos_secretary_wendy_q0120_29.htm".trim();
		} else if ("reply_10".equalsIgnoreCase(event)) {
			if (cond == 11) {
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller1_q0120_13a.htm";
			} else if (cond == 24) {
				qs.setCond(25);
				qs.giveItems(WENDYS_NECKLACE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "chaos_secretary_wendy_q0120_32.htm";
			} else if (cond == 25 || cond == 26) {
				qs.takeItems(WENDYS_NECKLACE, -1);
				qs.giveItems(EARRING_OF_BINDING, 1);
				qs.giveItems(57, 783720);
				qs.addExpAndSp(3447315, 272615);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collecter_yumi_q0120_33.htm";
			}
		} else if ("reply_12".equalsIgnoreCase(event)) {
			if (cond == 21) {
				qs.setCond(22);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller3_q0120_17.htm";
			}
		} else if ("reply_13".equalsIgnoreCase(event)) {
			if (cond == 18) {
				qs.setCond(19);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "weather_controller2_q0120_21.htm";
			}
		} else if ("reply_99".equalsIgnoreCase(event) || "reply_90".equalsIgnoreCase(event) || "reply_100".equalsIgnoreCase(event)) {
			return event;
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == STONES) {
			if (cond == 0) {
				if (pc.level() >= 50) return "pavel_atlanta_q0120_01.htm";
				qs.exitQuest(true);
				return "pavel_atlanta_q0120_03.htm";
			} else if (cond == 1) {
				return "pavel_atlanta_q0120_10.htm";
			} else if (cond >= 2 && cond <= 4) {
				return "pavel_atlanta_q0120_13.htm";
			} else if (cond == 5) {
				return "pavel_atlanta_q0120_14.htm";
			} else if (cond >= 6 && cond <= 8) {
				return "pavel_atlanta_q0120_16a.htm";
			} else if (cond == 9) {
				return "pavel_atlanta_q0120_17.htm";
			} else if (cond == 10 || cond == 11) {
				return "pavel_atlanta_q0120_28.htm";
			} else if (cond == 12) {
				return "pavel_atlanta_q0120_29.htm";
			} else if (cond >= 13 && cond <= 18) {
				return "pavel_atlanta_q0120_33.htm";
			} else if (cond == 19) {
				return "pavel_atlanta_q0120_34.htm";
			} else if (cond == 20 || cond == 21) {
				return "pavel_atlanta_q0120_39.htm";
			} else if (cond == 22) {
				return "pavel_atlanta_q0120_40.htm";
			} else if (cond >= 23) {
				return "pavel_atlanta_q0120_42.htm";
			}
		} else if (npcId == YUMI) {
			if (cond == 0) {
				if (pc.level() >= 50) return "collecter_yumi_q0120_0101.htm";
				qs.exitQuest(true);
				return "collecter_yumi_q0120_0103.htm";
			} else if (cond == 1) {
				return "collecter_yumi_q0120_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(57, 78336);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "collecter_yumi_q0120_0202.htm";
			} else if (cond == 3) {
				return "collecter_yumi_q0120_04.htm";
			} else if (cond == 4) {
				return "collecter_yumi_q0120_06.htm";
			} else if (cond == 7) {
				return "collecter_yumi_q0120_07.htm";
			} else if (cond >= 8 && cond <= 14) {
				return "collecter_yumi_q0120_14.htm";
			} else if (cond == 15) {
				return "collecter_yumi_q0120_15.htm";
			} else if (cond == 16) {
				if (qs.hasQuestItems(LOCKUP_RESEARCH_REPORT)) {
					qs.takeItems(LOCKUP_RESEARCH_REPORT, -1);
					qs.giveItems(RESEARCH_REPORT, 1);
				}
				return qs.hasQuestItems(RESEARCH_REPORT) ? "collecter_yumi_q0120_19.htm" : "collecter_yumi_q0120_18.htm";
			} else if (cond >= 17 && cond <= 24) {
				return "collecter_yumi_q0120_26.htm";
			} else if (cond == 25 || cond == 26) {
				qs.setCond(26);
				return "collecter_yumi_q0120_27.htm";
			}
		} else if (npcId == WENDY) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "wendy_q0120_0201.htm";
			} else if (cond >= 2 && cond <= 4) {
				return "chaos_secretary_wendy_q0120_01.htm";
			} else if (cond == 5) {
				return "chaos_secretary_wendy_q0120_07.htm";
			} else if (cond == 6) {
				return "chaos_secretary_wendy_q0120_08.htm";
			} else if (cond == 7) {
				return "chaos_secretary_wendy_q0120_11.htm";
			} else if (cond == 8) {
				return "chaos_secretary_wendy_q0120_11z.htm";
			} else if (cond >= 9 && cond <= 12) {
				return "chaos_secretary_wendy_q0120_15.htm";
			} else if (cond == 13) {
				return "chaos_secretary_wendy_q0120_16.htm";
			} else if (cond == 14) {
				return "chaos_secretary_wendy_q0120_19.htm";
			} else if (cond >= 15 && cond <= 22) {
				return "chaos_secretary_wendy_q0120_20.htm";
			} else if (cond == 23) {
				return "chaos_secretary_wendy_q0120_21.htm";
			} else if (cond == 24) {
				return "chaos_secretary_wendy_q0120_26.htm";
			} else if (cond >= 25) {
				return "chaos_secretary_wendy_q0120_33.htm";
			}
		} else if (npcId == BOOKSHELF) {
			if (cond == 14) return "drchaos_box_q0120_01.htm";
			if (cond >= 15) return "drchaos_box_q0120_03.htm";
		} else if (npcId == WEATHER_1) {
			if (cond == 10) return "weather_controller1_q0120_01.htm";
			if (cond == 11) return "weather_controller1_q0120_06.htm";
			if (cond >= 12) return "weather_controller1_q0120_13a.htm";
		} else if (npcId == WEATHER_2) {
			if (cond == 17) return "weather_controller2_q0120_01.htm";
			if (cond == 18) return "weather_controller2_q0120_06.htm";
			if (cond >= 19) return "weather_controller2_q0120_22.htm";
		} else if (npcId == WEATHER_3) {
			if (cond == 20) return "weather_controller3_q0120_01.htm";
			if (cond == 21) return "weather_controller3_q0120_08.htm";
			if (cond >= 22) return "weather_controller3_q0120_19.htm";
		}
		return "noquest";
	}
}
