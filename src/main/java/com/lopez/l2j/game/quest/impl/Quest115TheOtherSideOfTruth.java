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
 * Quest 115: The Other Side of Truth
 */
@Component
public class Quest115TheOtherSideOfTruth extends Quest {

	public static final int QUEST_ID = 115;
	public static final String QUEST_NAME = "115_TheOtherSideOfTruth";
	public static final int START_NPC = 32020;

	// NPCs
	public static final int RAFFORTY = 32020;
	public static final int MISA = 32018;
	public static final int SCULPTURE_1 = 32021;
	public static final int KIERRE = 32022;
	public static final int SCULPTURE_2 = 32077;
	public static final int SCULPTURE_3 = 32078;
	public static final int SCULPTURE_4 = 32079;

	// Items
	public static final int MISAS_LETTER = 8079;
	public static final int RAFFORTYS_LETTER = 8080;
	public static final int PIECE_OF_TABLET = 8081;
	public static final int REPORT_PIECE = 8082;

	@Autowired
	public Quest115TheOtherSideOfTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "The Other Side of Truth");
		addStartNpc(START_NPC);
		addTalkId(32020, 32018, 32021, 32022, 32077, 32078, 32079);
		registerQuestItems(8079, 8080, 8081, 8082);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "32020-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.set("beyond_the_turth", "1");
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "32020-02.htm".equalsIgnoreCase(event) ? "32020-02.htm" : "rafferty_q0115_0104.htm";
		} else if ("32020-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.set("beyond_the_turth", "3");
			qs.takeItems(MISAS_LETTER, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32020-05.htm";
		} else if ("32020-06.htm".equalsIgnoreCase(event) || "reply_2".equalsIgnoreCase(event) && cond == 2) {
			qs.takeItems(MISAS_LETTER, -1);
			qs.unset("beyond_the_turth");
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "32020-06.htm".equalsIgnoreCase(event) ? "32020-06.htm" : "repre_q0115_08.htm";
		} else if ("32020-08.htm".equalsIgnoreCase(event) || "reply_3".equalsIgnoreCase(event) || "reply_4".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.set("beyond_the_turth", "4");
			qs.playSound(QuestState.SOUND_MIDDLE);
			if ("reply_3".equalsIgnoreCase(event)) return "repre_q0115_11.htm";
			if ("reply_4".equalsIgnoreCase(event)) return "repre_q0115_12.htm";
			return "32020-08.htm";
		} else if ("reply_5".equalsIgnoreCase(event)) {
			qs.unset("beyond_the_turth");
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "repre_q0115_13.htm";
		} else if ("32020-12.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.set("beyond_the_turth", "5");
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32020-12.htm".equalsIgnoreCase(event) ? "32020-12.htm" : "repre_q0115_17.htm";
		} else if ("32018-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.set("beyond_the_turth", "7");
			qs.takeItems(RAFFORTYS_LETTER, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32018-04.htm";
		} else if ("32022-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.set("beyond_the_turth", "9");
			qs.giveItems(REPORT_PIECE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32022-02.htm";
		} else if ("32020-18.htm".equalsIgnoreCase(event) || "reply_8".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.set("beyond_the_turth", "10");
			qs.takeItems(REPORT_PIECE, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32020-18.htm".equalsIgnoreCase(event) ? "32020-18.htm" : "repre_q0115_23.htm";
		} else if ("reply_9".equalsIgnoreCase(event) || "reply_10".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(PIECE_OF_TABLET)) {
				qs.takeItems(PIECE_OF_TABLET, -1);
				qs.giveItems(57, 60044);
				qs.unset("beyond_the_turth");
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "reply_9".equalsIgnoreCase(event) ? "repre_q0115_25.htm" : "repre_q0115_26.htm";
			} else {
				qs.setCond(11);
				qs.set("beyond_the_turth", "11");
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "reply_9".equalsIgnoreCase(event) ? "repre_q0115_27.htm" : "repre_q0115_28.htm";
			}
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 2) {
				qs.setCond(3);
				qs.set("beyond_the_turth", "3");
				qs.takeItems(MISAS_LETTER, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "repre_q0115_07.htm";
			} else if (cond == 6) {
				qs.setCond(7);
				qs.set("beyond_the_turth", "7");
				qs.takeItems(RAFFORTYS_LETTER, -1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "misa_q0115_05.htm";
			} else if (cond == 7) {
				qs.giveItems(PIECE_OF_TABLET, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "ice_sculpture_q0115_03.htm";
			} else if (cond == 8) {
				qs.setCond(9);
				qs.set("beyond_the_turth", "9");
				qs.giveItems(REPORT_PIECE, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "keier_q0115_02.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == RAFFORTY) {
			if (cond == 0) {
				if (pc.level() >= 53) return "rafferty_q0115_0101.htm";
				qs.exitQuest(true);
				return "rafferty_q0115_0103.htm";
			} else if (cond == 1) {
				return "rafferty_q0115_0105.htm";
			} else if (cond == 2) {
				qs.takeItems(MISAS_LETTER, -1);
				qs.giveItems(57, 60044);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "rafferty_q0115_0202.htm";
			} else if (cond == 3) {
				return "repre_q0115_09.htm";
			} else if (cond == 4) {
				return "repre_q0115_16.htm";
			} else if (cond == 5) {
				qs.setCond(6);
				qs.set("beyond_the_turth", "6");
				qs.giveItems(RAFFORTYS_LETTER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "repre_q0115_18.htm";
			} else if (cond == 6) {
				if (!qs.hasQuestItems(RAFFORTYS_LETTER)) {
					qs.giveItems(RAFFORTYS_LETTER, 1);
					return "repre_q0115_20.htm";
				}
				return "repre_q0115_19.htm";
			} else if (cond == 7 || cond == 8) {
				return "repre_q0115_21.htm";
			} else if (cond == 9) {
				return "repre_q0115_22.htm";
			} else if (cond == 10) {
				return "repre_q0115_24.htm";
			} else if (cond == 11) {
				if (qs.hasQuestItems(PIECE_OF_TABLET)) {
					qs.takeItems(PIECE_OF_TABLET, -1);
					qs.giveItems(57, 60044);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "repre_q0115_30.htm";
				}
				return "repre_q0115_29.htm";
			} else if (cond == 12) {
				qs.takeItems(PIECE_OF_TABLET, -1);
				qs.giveItems(57, 60044);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "repre_q0115_30.htm";
			}
		} else if (npcId == MISA) {
			if (cond == 1) {
				qs.setCond(2);
				qs.set("beyond_the_turth", "2");
				qs.giveItems(MISAS_LETTER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "misa_q0115_0201.htm";
			} else if (cond == 2) {
				return "misa_q0115_03.htm";
			} else if (cond >= 3 && cond <= 5) {
				return "misa_q0115_01.htm";
			} else if (cond == 6) {
				return "misa_q0115_04.htm";
			} else if (cond >= 7) {
				return "misa_q0115_06.htm";
			}
		} else if (npcId == KIERRE) {
			if (cond == 8) {
				return "keier_q0115_01.htm";
			} else if (cond == 9) {
				if (!qs.hasQuestItems(REPORT_PIECE)) {
					qs.giveItems(REPORT_PIECE, 1);
					return "keier_q0115_04.htm";
				}
				return "keier_q0115_03.htm";
			} else if (cond >= 11) {
				return "keier_q0115_05.htm";
			}
		} else if (npcId == SCULPTURE_1 || npcId == SCULPTURE_2 || npcId == SCULPTURE_3 || npcId == SCULPTURE_4) {
			if (cond == 7) {
				qs.setCond(8);
				qs.set("beyond_the_turth", "8");
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "ice_sculpture_q0115_01.htm";
			} else if (cond == 8) {
				return "ice_sculpture_q0115_07.htm";
			} else if (cond == 11) {
				if (!qs.hasQuestItems(PIECE_OF_TABLET)) {
					qs.setCond(12);
					qs.giveItems(PIECE_OF_TABLET, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "ice_sculpture_q0115_08.htm";
				}
				return "ice_sculpture_q0115_09.htm";
			} else if (cond == 12) {
				return "ice_sculpture_q0115_09.htm";
			}
		}
		return "noquest";
	}
}
