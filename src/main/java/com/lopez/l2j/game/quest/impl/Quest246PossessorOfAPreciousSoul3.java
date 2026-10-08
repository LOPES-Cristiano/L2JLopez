package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 246: Possessor of a Precious Soul - Part 3 (Derrota do Raid Boss Barakiel e obtenção da Staff of Goddess).
 */
@Component
public class Quest246PossessorOfAPreciousSoul3 extends Quest {

	public static final int QUEST_ID = 246;
	public static final String QUEST_NAME = "246_PossessorOfAPreciousSoul_3";

	// NPCs
	public static final int CARADINE = 31740;
	public static final int OSSIAN = 31741;
	public static final int LADD = 30721;

	// Monstros
	public static final int PILGRIM_OF_SPLENDOR = 21541;
	public static final int JUDGE_OF_SPLENDOR = 21544;
	public static final int BARAKIEL = 25325;

	// Itens
	public static final int WATERBINDER = 7591;
	public static final int EVERGREEN = 7592;
	public static final int RAIN_SONG = 7593;
	public static final int RELIC_BOX = 7594;
	public static final int CARADINE_LETTER_1 = 7678;
	public static final int CARADINE_LETTER_2 = 7679;

	@Autowired
	public Quest246PossessorOfAPreciousSoul3(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Possessor of a Precious Soul - Part 3");

		addStartNpc(CARADINE);
		addTalkId(CARADINE);
		addTalkId(OSSIAN);
		addTalkId(LADD);

		addKillId(PILGRIM_OF_SPLENDOR);
		addKillId(JUDGE_OF_SPLENDOR);
		addKillId(BARAKIEL);

		registerQuestItems(WATERBINDER, EVERGREEN, RAIN_SONG, RELIC_BOX);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31740-04.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.playSound("ItemSound.quest_accept");
			qs.takeItems(CARADINE_LETTER_1, -1);
			return event;
		} else if ("31741-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound("ItemSound.quest_middle");
			return event;
		} else if ("31741-05.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(WATERBINDER) && qs.hasQuestItems(EVERGREEN)) {
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				qs.takeItems(WATERBINDER, -1);
				qs.takeItems(EVERGREEN, -1);
				return event;
			}
		} else if ("31741-08.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RAIN_SONG)) {
				qs.setCond(6);
				qs.playSound("ItemSound.quest_middle");
				qs.takeItems(RAIN_SONG, -1);
				qs.giveItems(RELIC_BOX, 1);
				return event;
			}
		} else if ("30721-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RELIC_BOX)) {
				qs.takeItems(RELIC_BOX, -1);
				qs.giveItems(CARADINE_LETTER_2, 1);
				qs.addExpAndSp(719843, 0);
				qs.playSound("ItemSound.quest_finish");
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == CARADINE) {
			if (cond == 0) {
				if (qs.hasQuestItems(CARADINE_LETTER_1)) {
					return "31740-01.htm";
				}
				return "31740-02.htm";
			} else if (cond == 1) {
				return "31740-05.htm";
			}
		} else if (npcId == OSSIAN) {
			if (cond == 1) {
				return "31741-01.htm";
			} else if (cond == 2) {
				return "31741-03.htm";
			} else if (cond == 3 || (qs.hasQuestItems(WATERBINDER) && qs.hasQuestItems(EVERGREEN))) {
				return "31741-04.htm";
			} else if (cond == 4) {
				return "31741-06.htm";
			} else if (cond == 5 || qs.hasQuestItems(RAIN_SONG)) {
				return "31741-07.htm";
			} else if (cond == 6) {
				return "31741-09.htm";
			}
		} else if (npcId == LADD) {
			if (cond == 6 && qs.hasQuestItems(RELIC_BOX)) {
				return "30721-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 2) {
			if (npcId == PILGRIM_OF_SPLENDOR && !qs.hasQuestItems(WATERBINDER)) {
				if (ThreadLocalRandom.current().nextInt(100) < 50) {
					qs.giveItems(WATERBINDER, 1);
					if (qs.hasQuestItems(EVERGREEN)) {
						qs.setCond(3);
						qs.playSound("ItemSound.quest_middle");
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				}
			} else if (npcId == JUDGE_OF_SPLENDOR && !qs.hasQuestItems(EVERGREEN)) {
				if (ThreadLocalRandom.current().nextInt(100) < 50) {
					qs.giveItems(EVERGREEN, 1);
					if (qs.hasQuestItems(WATERBINDER)) {
						qs.setCond(3);
						qs.playSound("ItemSound.quest_middle");
					} else {
						qs.playSound("ItemSound.quest_itemget");
					}
				}
			}
		} else if (cond == 4 && npcId == BARAKIEL) {
			if (!qs.hasQuestItems(RAIN_SONG)) {
				qs.giveItems(RAIN_SONG, 1);
				qs.setCond(5);
				qs.playSound("ItemSound.quest_middle");
			}
		}

		return null;
	}
}
