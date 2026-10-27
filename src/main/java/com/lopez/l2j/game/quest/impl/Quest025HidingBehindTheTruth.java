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
 * Quest 025: Hiding Behind the Truth
 * Benedict e Agripel em Rune, Maid of Lidia, Shadow Hardin, Broken Bookshelves,
 * Tombstone e Coffin.
 * Recompensa: Jóias B-grade (Earring of Blessing, Necklace of Blessing, Ring of
 * Blessing).
 */
@Component
public class Quest025HidingBehindTheTruth extends Quest {

	public static final int QUEST_ID = 25;
	public static final String QUEST_NAME = "025_HidingBehindTheTruth";

	public static final int BENEDICT = 31349;
	public static final int AGRIPEL = 31348;
	public static final int HARDIN = 31522;
	public static final int MAID = 31532;
	public static final int MAID_OF_LIDIA = 31532;
	public static final int BROKEN_BOOKSHELF_1 = 31533;
	public static final int BROKEN_BOOKSHELF_2 = 31534;
	public static final int BROKEN_BOOKSHELF_3 = 31535;
	public static final int TOMBSTONE = 31531;
	public static final int COFFIN = 31536;

	public static final int MAP_FOREST_OF_DEADMAN = 7063;
	public static final int CONTRACT = 7066;
	public static final int SUSPICIOUS_TOTEM = 7151;
	public static final int LIDIAS_DRESS = 7155;
	public static final int SUSPICIOUS_TOTEM_DOLL = 7156;
	public static final int GEMSTONE_KEY = 7157;
	public static final int TOTEM_DOLL = 7158;

	public static final int EARRING_OF_BLESSING = 874;
	public static final int NECKLACE_OF_BLESSING = 936;
	public static final int NECKLACE_OF_GRACE = 905;
	public static final int RING_OF_BLESSING = 905;

	public static final int TRIOLS_PAWN = 27218;
	public static final int PAWN_DESPAWN_DELAY = 20000;

	@Autowired
	public Quest025HidingBehindTheTruth(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Hiding Behind the Truth");
		addStartNpc(BENEDICT);
		addTalkId(BENEDICT, AGRIPEL, HARDIN, MAID_OF_LIDIA, BROKEN_BOOKSHELF_1, BROKEN_BOOKSHELF_2, BROKEN_BOOKSHELF_3,
				TOMBSTONE, COFFIN);
		addAttackId(TRIOLS_PAWN);
		addKillId(TRIOLS_PAWN);
		registerQuestItems(TOTEM_DOLL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if ("quest_accept".equalsIgnoreCase(event) || "31349-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31349-02.htm".equalsIgnoreCase(event) ? "31349-02.htm" : "falsepriest_benedict_q0025_03.htm";
		} else if ("reply_1".equalsIgnoreCase(event) || "31349-03.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(SUSPICIOUS_TOTEM_DOLL)) {
				return "31349-03.htm".equalsIgnoreCase(event) ? "31349-03.htm" : "falsepriest_benedict_q0025_04.htm";
			}
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "falsepriest_benedict_q0025_05.htm";
		} else if ("31349-06.htm".equalsIgnoreCase(event) || "31349-07.htm".equalsIgnoreCase(event)
				|| "31349-08.htm".equalsIgnoreCase(event) || "31349-09.htm".equalsIgnoreCase(event)
				|| "falsepriest_benedict_q0025_06.htm".equalsIgnoreCase(event)
				|| "falsepriest_benedict_q0025_07.htm".equalsIgnoreCase(event)
				|| "falsepriest_benedict_q0025_08.htm".equalsIgnoreCase(event)
				|| "falsepriest_benedict_q0025_09.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_3".equalsIgnoreCase(event) || "31349-10.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31349-10.htm".equalsIgnoreCase(event) ? "31349-10.htm" : "falsepriest_benedict_q0025_10.htm";
		} else if ("reply_4".equalsIgnoreCase(event) || "31348-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SUSPICIOUS_TOTEM_DOLL, -1);
			return "31348-02.htm".equalsIgnoreCase(event) ? "31348-02.htm" : "falsepriest_agripel_q0025_02.htm";
		} else if ("31348-03.htm".equalsIgnoreCase(event) || "31348-04.htm".equalsIgnoreCase(event)
				|| "31348-05.htm".equalsIgnoreCase(event) || "31348-06.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_04.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_05.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_06.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_07.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_6".equalsIgnoreCase(event) || "31348-07.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.giveItems(GEMSTONE_KEY, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31348-07.htm".equalsIgnoreCase(event) ? "31348-07.htm" : "falsepriest_agripel_q0025_08.htm";
		} else if ("reply_25".equalsIgnoreCase(event) || "31348-10.htm".equalsIgnoreCase(event)) {
			qs.takeItems(TOTEM_DOLL, -1);
			return "31348-10.htm".equalsIgnoreCase(event) ? "31348-10.htm" : "falsepriest_agripel_q0025_10.htm";
		} else if ("31348-11.htm".equalsIgnoreCase(event) || "31348-12.htm".equalsIgnoreCase(event)
				|| "31348-14.htm".equalsIgnoreCase(event) || "falsepriest_agripel_q0025_11.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_12.htm".equalsIgnoreCase(event)
				|| "falsepriest_agripel_q0025_14.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_21".equalsIgnoreCase(event) || "31348-13.htm".equalsIgnoreCase(event)) {
			qs.setCond(17);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31348-13.htm".equalsIgnoreCase(event) ? "31348-13.htm" : "falsepriest_agripel_q0025_13.htm";
		} else if ("reply_23".equalsIgnoreCase(event) || "31348-15.htm".equalsIgnoreCase(event)) {
			qs.setCond(17);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31348-15.htm".equalsIgnoreCase(event) ? "31348-15.htm" : "falsepriest_agripel_q0025_16.htm";
		} else if ("reply_24".equalsIgnoreCase(event) || "31348-16.htm".equalsIgnoreCase(event)) {
			qs.setCond(18);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31348-16.htm".equalsIgnoreCase(event) ? "31348-16.htm" : "falsepriest_agripel_q0025_17.htm";
		} else if ("reply_7".equalsIgnoreCase(event) || "31522-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(6);
			qs.giveItems(MAP_FOREST_OF_DEADMAN, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31522-04.htm".equalsIgnoreCase(event) ? "31522-04.htm" : "shadow_hardin_q0025_04.htm";
		} else if ("31522-07.htm".equalsIgnoreCase(event) || "31522-08.htm".equalsIgnoreCase(event)
				|| "31522-09.htm".equalsIgnoreCase(event) || "31522-10.htm".equalsIgnoreCase(event)
				|| "31522-11.htm".equalsIgnoreCase(event) || "shadow_hardin_q0025_07.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0025_08.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0025_09.htm".equalsIgnoreCase(event)
				|| "shadow_hardin_q0025_11.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_17".equalsIgnoreCase(event)) {
			return "shadow_hardin_q0025_10.htm";
		} else if ("reply_19".equalsIgnoreCase(event) || "31522-12.htm".equalsIgnoreCase(event)) {
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31522-12.htm".equalsIgnoreCase(event) ? "31522-12.htm" : "shadow_hardin_q0025_13.htm";
		} else if ("reply_23a".equalsIgnoreCase(event) || "31522-15.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MAP_FOREST_OF_DEADMAN, -1);
			qs.takeItems(CONTRACT, -1);
			qs.giveItems(EARRING_OF_BLESSING, 1);
			qs.giveItems(NECKLACE_OF_BLESSING, 1);
			qs.addExpAndSp(572277, 53750);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31522-15.htm".equalsIgnoreCase(event) ? "31522-15.htm" : "shadow_hardin_q0025_16.htm";
		} else if ("reply_8".equalsIgnoreCase(event) || "31533-02.htm".equalsIgnoreCase(event)) {
			return "31533-02.htm".equalsIgnoreCase(event) ? "31533-02.htm" : "broken_desk2_q0025_03.htm";
		} else if ("reply_8a".equalsIgnoreCase(event) || "31534-02.htm".equalsIgnoreCase(event)) {
			return "31534-02.htm".equalsIgnoreCase(event) ? "31534-02.htm" : "broken_desk3_q0025_03.htm";
		} else if ("reply_8b".equalsIgnoreCase(event) || "31535-02.htm".equalsIgnoreCase(event)) {
			return "31535-02.htm".equalsIgnoreCase(event) ? "31535-02.htm" : "broken_desk4_q0025_03.htm";
		} else if ("reply_9".equalsIgnoreCase(event) || "31535-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(TRIOLS_PAWN, 120000);
			return "31535-03.htm".equalsIgnoreCase(event) ? "31535-03.htm" : "broken_desk2_q0025_07.htm";
		} else if ("reply_9a".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(TRIOLS_PAWN, 120000);
			return "broken_desk3_q0025_07.htm";
		} else if ("reply_9b".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(TRIOLS_PAWN, 120000);
			return "broken_desk4_q0025_07.htm";
		} else if ("reply_10".equalsIgnoreCase(event) || "31535-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.takeItems(GEMSTONE_KEY, -1);
			qs.giveItems(CONTRACT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31535-05.htm".equalsIgnoreCase(event) ? "31535-05.htm" : "broken_desk2_q0025_11.htm";
		} else if ("reply_10a".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.takeItems(GEMSTONE_KEY, -1);
			qs.giveItems(CONTRACT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "broken_desk3_q0025_11.htm";
		} else if ("reply_10b".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.takeItems(GEMSTONE_KEY, -1);
			qs.giveItems(CONTRACT, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "broken_desk4_q0025_11.htm";
		} else if ("reply_11".equalsIgnoreCase(event) || "31532-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CONTRACT, -1);
			return "31532-02.htm".equalsIgnoreCase(event) ? "31532-02.htm" : "maid_of_ridia_q0025_02.htm";
		} else if ("31532-03.htm".equalsIgnoreCase(event) || "31532-04.htm".equalsIgnoreCase(event)
				|| "31532-05.htm".equalsIgnoreCase(event) || "maid_of_ridia_q0025_04.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_05.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_06.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_13".equalsIgnoreCase(event) || "31532-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(11);
			qs.playSound("SkillSound5.horror_01");
			return "31532-06.htm".equalsIgnoreCase(event) ? "31532-06.htm" : "maid_of_ridia_q0025_07.htm";
		} else if ("reply_14".equalsIgnoreCase(event) || "31532-09.htm".equalsIgnoreCase(event)) {
			return "31532-09.htm".equalsIgnoreCase(event) ? "31532-09.htm" : "maid_of_ridia_q0025_12.htm";
		} else if ("31532-10.htm".equalsIgnoreCase(event) || "31532-11.htm".equalsIgnoreCase(event)
				|| "31532-12.htm".equalsIgnoreCase(event) || "31532-13.htm".equalsIgnoreCase(event)
				|| "31532-14.htm".equalsIgnoreCase(event) || "31532-15.htm".equalsIgnoreCase(event)
				|| "31532-16.htm".equalsIgnoreCase(event) || "31532-17.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_14.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_15.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_16.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_19.htm".equalsIgnoreCase(event)
				|| "maid_of_ridia_q0025_20.htm".equalsIgnoreCase(event)) {
			return event;
		} else if ("reply_15".equalsIgnoreCase(event)) {
			return "maid_of_ridia_q0025_17.htm";
		} else if ("reply_16".equalsIgnoreCase(event) || "31532-18.htm".equalsIgnoreCase(event)) {
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "31532-18.htm".equalsIgnoreCase(event) ? "31532-18.htm" : "maid_of_ridia_q0025_21.htm";
		} else if ("reply_22".equalsIgnoreCase(event) || "31532-20.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MAP_FOREST_OF_DEADMAN, -1);
			qs.takeItems(CONTRACT, -1);
			qs.giveItems(EARRING_OF_BLESSING, 1);
			qs.giveItems(RING_OF_BLESSING, 2);
			qs.addExpAndSp(572277, 53750);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "31532-20.htm".equalsIgnoreCase(event) ? "31532-20.htm" : "maid_of_ridia_q0025_24.htm";
		} else if ("reply_1a".equalsIgnoreCase(event) || "31531-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(12);
			qs.playSound(QuestState.SOUND_MIDDLE);
			qs.addSpawn(COFFIN, 60104, -35820, -664, 120000);
			return "31531-02.htm".equalsIgnoreCase(event) ? "31531-02.htm" : "q_forest_stone2_q0025_02.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null)
			return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case BENEDICT:
				if (cond == 0) {
					if (pc.level() >= 66) {
						return "falsepriest_benedict_q0025_01.htm";
					}
					qs.exitQuest(true);
					return "falsepriest_benedict_q0025_02.htm";
				} else if (cond == 1) {
					return "falsepriest_benedict_q0025_03a.htm";
				} else if (cond == 2) {
					return "falsepriest_benedict_q0025_05.htm";
				} else if (cond == 3) {
					return "falsepriest_benedict_q0025_04.htm";
				} else if (cond >= 4) {
					return "falsepriest_benedict_q0025_11.htm";
				}
				break;
			case AGRIPEL:
				if (cond == 1) {
					qs.giveItems(CONTRACT, 1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "falsepriest_agripel_q0025_02.htm";
				} else if (cond == 2) {
					qs.takeItems(CONTRACT, -1);
					qs.giveItems(EARRING_OF_BLESSING, 1);
					qs.giveItems(NECKLACE_OF_GRACE, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "falsepriest_agripel_q0025_08.htm";
				} else if (cond == 4) {
					return "falsepriest_agripel_q0025_01.htm";
				} else if (cond == 5) {
					return "falsepriest_agripel_q0025_08a.htm";
				} else if (cond == 16 && qs.hasQuestItems(TOTEM_DOLL)) {
					return "falsepriest_agripel_q0025_09.htm";
				} else if (cond == 17) {
					return "falsepriest_agripel_q0025_15.htm";
				} else if (cond == 18) {
					return "falsepriest_agripel_q0025_18.htm";
				}
				break;
			case HARDIN:
				if (cond == 2) {
					qs.setCond(3);
					qs.giveItems(SUSPICIOUS_TOTEM_DOLL, 1);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "shadow_hardin_q0025_01.htm";
				} else if (cond == 3) {
					return "shadow_hardin_q0025_02.htm";
				} else if (cond == 5) {
					return "shadow_hardin_q0025_03.htm";
				} else if (cond >= 6 && cond <= 14) {
					return "shadow_hardin_q0025_05.htm";
				} else if (cond == 15) {
					return "shadow_hardin_q0025_06a.htm";
				} else if (cond == 16) {
					return "shadow_hardin_q0025_12.htm";
				} else if (cond == 17) {
					return "shadow_hardin_q0025_14.htm";
				} else if (cond == 18) {
					return "shadow_hardin_q0025_15.htm";
				}
				break;
			case BROKEN_BOOKSHELF_1:
				if (cond == 6) {
					return "broken_desk2_q0025_01.htm";
				} else if (cond == 7) {
					return "broken_desk2_q0025_05.htm";
				} else if (cond == 8 && qs.hasQuestItems(TOTEM_DOLL)) {
					return "broken_desk2_q0025_10.htm";
				} else if (cond >= 9) {
					return "broken_desk2_q0025_02.htm";
				}
				break;
			case BROKEN_BOOKSHELF_2:
				if (cond == 6) {
					return "broken_desk3_q0025_01.htm";
				} else if (cond == 7) {
					return "broken_desk3_q0025_05.htm";
				} else if (cond == 8 && qs.hasQuestItems(TOTEM_DOLL)) {
					return "broken_desk3_q0025_10.htm";
				} else if (cond >= 9) {
					return "broken_desk3_q0025_02.htm";
				}
				break;
			case BROKEN_BOOKSHELF_3:
				if (cond == 6) {
					return "broken_desk4_q0025_01.htm";
				} else if (cond == 7) {
					return "broken_desk4_q0025_05.htm";
				} else if (cond == 8 && qs.hasQuestItems(TOTEM_DOLL)) {
					return "broken_desk4_q0025_10.htm";
				} else if (cond >= 9) {
					return "broken_desk4_q0025_02.htm";
				}
				break;
			case MAID_OF_LIDIA:
				if (cond == 9 || cond == 10) {
					qs.setCond(10);
					return "maid_of_ridia_q0025_01.htm";
				} else if (cond == 11 || cond == 12) {
					return "maid_of_ridia_q0025_08.htm";
				} else if (cond == 13) {
					return "maid_of_ridia_q0025_09.htm";
				} else if (cond == 14 && qs.hasQuestItems(LIDIAS_DRESS)) {
					qs.takeItems(LIDIAS_DRESS, -1);
					return "maid_of_ridia_q0025_10.htm";
				} else if (cond == 15) {
					return "maid_of_ridia_q0025_22.htm";
				} else if (cond == 17) {
					return "maid_of_ridia_q0025_23.htm";
				} else if (cond == 18) {
					return "maid_of_ridia_q0025_25.htm";
				}
				break;
			case TOMBSTONE:
				if (cond == 11) {
					return "q_forest_stone2_q0025_01.htm";
				} else if (cond == 12) {
					qs.addSpawn(COFFIN, 60104, -35820, -664, 120000);
					return "q_forest_stone2_q0025_02.htm";
				} else if (cond >= 13) {
					return "q_forest_stone2_q0025_03.htm";
				}
				break;
			case COFFIN:
				if (cond == 12) {
					qs.giveItems(LIDIAS_DRESS, 1);
					qs.setCond(13);
					qs.setCond(14);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "q_forest_box1_q0025_01.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (npc.getNpcId() == TRIOLS_PAWN && qs.getCond() == 7 && !qs.hasQuestItems(TOTEM_DOLL)) {
			qs.giveItems(TOTEM_DOLL, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
