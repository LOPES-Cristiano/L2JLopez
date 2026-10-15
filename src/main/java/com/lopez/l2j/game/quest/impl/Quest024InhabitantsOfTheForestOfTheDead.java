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
 * Quest 024: Inhabitants of the Forest of the Dead
 * Dorian, Tombstone, Maid of Lidia e Mysterious Wizard (Shadow Hardin) em Forest of the Dead.
 */
@Component
public class Quest024InhabitantsOfTheForestOfTheDead extends Quest {

	public static final int QUEST_ID = 24;
	public static final String QUEST_NAME = "024_InhabitantsOfTheForestOfTheDead";

	public static final int DORIAN = 31389;
	public static final int TOMBSTONE = 31531;
	public static final int MAID = 31532;
	public static final int MAID_OF_LIDIA = 31532;
	public static final int HARDIN = 31522;
	public static final int MYSTERIOUS_WIZARD = 31522;

	public static final int LIDIAS_LETTER = 7065;
	public static final int LIDIAS_HAIRPIN = 7148;
	public static final int SUSPICIOUS_TOTEM = 7151;
	public static final int FLOWER = 7152;
	public static final int SILVER_CROSS = 7153;
	public static final int BROKEN_SILVER_CROSS = 7154;
	public static final int SUSPICIOUS_TOTEM_DOLL = 7156;

	public static final int[] MOBS = {21557, 21558, 21560, 21563, 21564, 21565, 21566, 21567};

	@Autowired
	public Quest024InhabitantsOfTheForestOfTheDead(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Inhabitants of the Forest of the Dead");
		addStartNpc(DORIAN);
		addTalkId(DORIAN, TOMBSTONE, MAID_OF_LIDIA, MYSTERIOUS_WIZARD);
		addKillId(MOBS);
		registerQuestItems(SUSPICIOUS_TOTEM);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31389-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(FLOWER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31389-02.htm".equalsIgnoreCase(event) ? "31389-02.htm" : "day_dorian_q0024_03.htm";
		} else if ("reply_1a".equalsIgnoreCase(event) || "31531-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.takeItems(FLOWER, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31531-02.htm".equalsIgnoreCase(event) ? "31531-02.htm" : "q_forest_stone2_q0024_02.htm";
		} else if ("reply_1".equalsIgnoreCase(event) || "31389-05.htm".equalsIgnoreCase(event)
				|| "day_dorian_q0024_06.htm".equalsIgnoreCase(event) || "day_dorian_q0024_07.htm".equalsIgnoreCase(event)
				|| "31389-06.htm".equalsIgnoreCase(event) || "31389-07.htm".equalsIgnoreCase(event)
				|| "31389-08.htm".equalsIgnoreCase(event) || "31389-09.htm".equalsIgnoreCase(event)
				|| "31389-10.htm".equalsIgnoreCase(event) || "day_dorian_q0024_10.htm".equalsIgnoreCase(event)
				|| "day_dorian_q0024_11.htm".equalsIgnoreCase(event) || "day_dorian_q0024_12.htm".equalsIgnoreCase(event)) {
			return "reply_1".equalsIgnoreCase(event) ? "day_dorian_q0024_08.htm" : event;
		} else if ("reply_3".equalsIgnoreCase(event) || "31389-11.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.giveItems(SILVER_CROSS, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31389-11.htm".equalsIgnoreCase(event) ? "31389-11.htm" : "day_dorian_q0024_13.htm";
		} else if ("31389-14.htm".equalsIgnoreCase(event) || "31389-15.htm".equalsIgnoreCase(event)
				|| "31389-16.htm".equalsIgnoreCase(event) || "day_dorian_q0024_16.htm".equalsIgnoreCase(event)
				|| "day_dorian_q0024_17.htm".equalsIgnoreCase(event)) {
			if ("31389-15.htm".equalsIgnoreCase(event) || "day_dorian_q0024_16.htm".equalsIgnoreCase(event)) {
				qs.playSound("InterfaceSound.charstat_open_01");
			}
			return event;
		} else if ("reply_5".equalsIgnoreCase(event) || "31389-17.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.takeItems(BROKEN_SILVER_CROSS, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31389-17.htm".equalsIgnoreCase(event) ? "31389-17.htm" : "day_dorian_q0024_18.htm";
		} else if ("reply_6".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.takeItems(BROKEN_SILVER_CROSS, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "day_dorian_q0024_19.htm";
		} else if ("31532-02.htm".equalsIgnoreCase(event) || "31532-03.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0024_02.htm".equalsIgnoreCase(event) || "maid_of_ridia_q0024_03.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_7".equalsIgnoreCase(event) || "31532-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.giveItems(LIDIAS_LETTER, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31532-04.htm".equalsIgnoreCase(event) ? "31532-04.htm" : "maid_of_ridia_q0024_04.htm";
		} else if ("reply_8".equalsIgnoreCase(event) || "31532-06.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(LIDIAS_HAIRPIN)) {
				qs.takeItems(LIDIAS_LETTER, -1);
				qs.takeItems(LIDIAS_HAIRPIN, -1);
				return "31532-06.htm".equalsIgnoreCase(event) ? "31532-06.htm" : "maid_of_ridia_q0024_06.htm";
			}
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "maid_of_ridia_q0024_07.htm";
		} else if ("31532-07.htm".equalsIgnoreCase(event) || "31532-08.htm".equalsIgnoreCase(event)
				|| "31532-09.htm".equalsIgnoreCase(event) || "31532-11.htm".equalsIgnoreCase(event)
				|| "31532-12.htm".equalsIgnoreCase(event) || "31532-13.htm".equalsIgnoreCase(event)
				|| "31532-14.htm".equalsIgnoreCase(event) || "31532-15.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0024_09.htm".equalsIgnoreCase(event) || "maid_of_ridia_q0024_12.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0024_13.htm".equalsIgnoreCase(event) || "maid_of_ridia_q0024_15.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0024_16.htm".equalsIgnoreCase(event) || "maid_of_ridia_q0024_17.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0024_18.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_10".equalsIgnoreCase(event) || "31532-10.htm".equalsIgnoreCase(event)) {
			return "31532-10.htm".equalsIgnoreCase(event) ? "31532-10.htm" : "maid_of_ridia_q0024_10.htm";
		} else if ("reply_11".equalsIgnoreCase(event)) {
			return "maid_of_ridia_q0024_14.htm";
		} else if ("reply_12".equalsIgnoreCase(event) || "31532-16.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31532-16.htm".equalsIgnoreCase(event) ? "31532-16.htm" : "maid_of_ridia_q0024_19.htm";
		} else if ("31522-02.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_02.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_14".equalsIgnoreCase(event) || "31522-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SUSPICIOUS_TOTEM)) {
				qs.takeItems(SUSPICIOUS_TOTEM, 1);
				return "31522-03.htm".equalsIgnoreCase(event) ? "31522-03.htm" : "shadow_hardin_q0024_03.htm";
			}
			return "shadow_hardin_q0024_04.htm";
		} else if ("31522-04.htm".equalsIgnoreCase(event) || "31522-05.htm".equalsIgnoreCase(event)
				|| "31522-06.htm".equalsIgnoreCase(event) || "31522-09.htm".equalsIgnoreCase(event)
				|| "31522-10.htm".equalsIgnoreCase(event) || "31522-11.htm".equalsIgnoreCase(event)
				|| "31522-12.htm".equalsIgnoreCase(event) || "31522-13.htm".equalsIgnoreCase(event)
				|| "31522-14.htm".equalsIgnoreCase(event) || "31522-15.htm".equalsIgnoreCase(event)
				|| "31522-16.htm".equalsIgnoreCase(event) || "31522-18.htm".equalsIgnoreCase(event)
				|| "31522-19.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_05.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_06.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_07.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_10.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_11.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_12.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_13.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_14.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_15.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_16.htm".equalsIgnoreCase(event) || "shadow_hardin_q0024_19.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0024_20.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_16".equalsIgnoreCase(event) || "31522-07.htm".equalsIgnoreCase(event)) {
			qs.setCond(11);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31522-07.htm".equalsIgnoreCase(event) ? "31522-07.htm" : "shadow_hardin_q0024_08.htm";
		} else if ("reply_18".equalsIgnoreCase(event) || "31522-17.htm".equalsIgnoreCase(event)) {
			return "31522-17.htm".equalsIgnoreCase(event) ? "31522-17.htm" : "shadow_hardin_q0024_17.htm";
		} else if ("reply_19".equalsIgnoreCase(event) || "31522-20.htm".equalsIgnoreCase(event)) {
			qs.giveItems(SUSPICIOUS_TOTEM_DOLL, 1);
			qs.addExpAndSp(242105, 22529);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31522-20.htm".equalsIgnoreCase(event) ? "31522-20.htm" : "shadow_hardin_q0024_21.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			if (npc.getNpcId() == MYSTERIOUS_WIZARD) {
				return "shadow_hardin_q0024_22.htm";
			}
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case DORIAN:
				if (cond == 0) {
					if (pc.level() >= 65) {
						return "day_dorian_q0024_01.htm";
					}
					qs.exitQuest(true);
					return "day_dorian_q0024_02.htm";
				} else if (cond == 1) {
					return "day_dorian_q0024_04.htm";
				} else if (cond == 2) {
					return "day_dorian_q0024_05.htm";
				} else if (cond == 3) {
					return "day_dorian_q0024_14.htm";
				} else if (cond == 4 && qs.hasQuestItems(BROKEN_SILVER_CROSS)) {
					return "day_dorian_q0024_15.htm";
				} else if (cond == 5) {
					return "day_dorian_q0024_20.htm";
				} else if (cond == 7 && !qs.hasQuestItems(LIDIAS_HAIRPIN)) {
					qs.giveItems(LIDIAS_HAIRPIN, 1);
					qs.setCond(8);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "day_dorian_q0024_21.htm";
				} else if (cond >= 8) {
					return "day_dorian_q0024_22.htm";
				}
				break;
			case TOMBSTONE:
				if (cond == 1 && qs.hasQuestItems(FLOWER)) {
					qs.playSound("AmdSound.d_wind_loot_02");
					return "q_forest_stone2_q0024_01.htm";
				} else if (cond >= 2) {
					return "q_forest_stone2_q0024_03.htm";
				}
				break;
			case MAID_OF_LIDIA:
				if (cond == 5) {
					return "maid_of_ridia_q0024_01.htm";
				} else if (cond == 6) {
					return (qs.hasQuestItems(LIDIAS_LETTER) && qs.hasQuestItems(LIDIAS_HAIRPIN)) ? "maid_of_ridia_q0024_05.htm" : "maid_of_ridia_q0024_07.htm";
				} else if (cond == 7) {
					return "maid_of_ridia_q0024_07a.htm";
				} else if (cond == 8) {
					if (qs.hasQuestItems(LIDIAS_LETTER) && qs.hasQuestItems(LIDIAS_HAIRPIN)) {
						return "maid_of_ridia_q0024_08.htm";
					}
					return "maid_of_ridia_q0024_11.htm";
				} else if (cond == 9) {
					return "maid_of_ridia_q0024_14.htm";
				} else if (cond >= 10) {
					return "maid_of_ridia_q0024_20.htm";
				}
				break;
			case MYSTERIOUS_WIZARD:
				if (cond == 10 && qs.hasQuestItems(SUSPICIOUS_TOTEM)) {
					return "shadow_hardin_q0024_01.htm";
				} else if (cond == 11) {
					return "shadow_hardin_q0024_09.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int cond = qs.getCond();
		if ((cond == 3 || cond == 9) && !qs.hasQuestItems(SUSPICIOUS_TOTEM)) {
			qs.giveItems(SUSPICIOUS_TOTEM, 1);
			if (cond == 3 && qs.hasQuestItems(SILVER_CROSS)) {
				qs.takeItems(SILVER_CROSS, -1);
				qs.giveItems(BROKEN_SILVER_CROSS, 1);
				qs.setCond(4);
			} else {
				qs.setCond(10);
			}
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
