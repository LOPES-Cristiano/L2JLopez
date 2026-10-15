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
 * Quest 111: Elrokian Hunter's Proof
 */
@Component
public class Quest111ElrokianHuntersProof extends Quest {

	public static final int QUEST_ID = 111;
	public static final String QUEST_NAME = "111_ElrokianHuntersProof";
	public static final int START_NPC = 32113;

	public static final int MARQUEZ = 32113;
	public static final int MUSHIKA = 32114;
	public static final int ASAMAH = 32115;
	public static final int KIRIKASHIN = 32116;
	public static final int KARAKAWEI = 32117;

	public static final int FRAGMENT = 8768;
	public static final int EXPEDITION_LETTER = 8769;
	public static final int CLAW = 8770;
	public static final int BONE = 8771;
	public static final int SKIN = 8772;
	public static final int PRACTICE_TRAP = 8773;

	public static final int ELROKIAN_TRAP = 8763;
	public static final int TRAP_STONE = 8764;

	@Autowired
	public Quest111ElrokianHuntersProof(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Elrokian Hunter's Proof");
		addStartNpc(START_NPC);
		addTalkId(MARQUEZ, MUSHIKA, ASAMAH, KIRIKASHIN, KARAKAWEI);
		addKillId(22196, 22197, 22198, 22200, 22201, 22202, 22203, 22204, 22205,
				22208, 22209, 22210, 22211, 22212, 22213, 22218, 22219, 22220, 22221);
		registerQuestItems(FRAGMENT, EXPEDITION_LETTER, CLAW, BONE, SKIN, PRACTICE_TRAP);
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
				|| "32113-02.htm".equalsIgnoreCase(event)
				|| "32113-002.htm".equalsIgnoreCase(event)
				|| "marquez_q111_2.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "marquez_q0111_0104.htm";
		} else if ("32115-03.htm".equalsIgnoreCase(event)
				|| "32115-002.htm".equalsIgnoreCase(event)
				|| "asamah_q111_2.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32113-03.htm".equalsIgnoreCase(event)
				|| "32113-009.htm".equalsIgnoreCase(event)
				|| "marquez_q111_4.htm".equalsIgnoreCase(event)) {
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("32113-018.htm".equalsIgnoreCase(event)
				|| "marquez_q111_6.htm".equalsIgnoreCase(event)) {
			qs.takeItems(FRAGMENT, -1);
			if (!qs.hasQuestItems(EXPEDITION_LETTER)) {
				qs.giveItems(EXPEDITION_LETTER, 1);
			}
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32113-018.htm";
		} else if ("32116-003.htm".equalsIgnoreCase(event)
				|| "kirikachin_q111_2.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.playSound("EtcSound.elcroki_song_full");
			return "32116-003.htm";
		} else if ("32116-005.htm".equalsIgnoreCase(event)
				|| "kirikachin_q111_3.htm".equalsIgnoreCase(event)) {
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32116-005.htm";
		} else if ("32117-03.htm".equalsIgnoreCase(event)) {
			if (qs.getCond() < 8) {
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.takeItems(PRACTICE_TRAP, -1);
				qs.giveItems(ELROKIAN_TRAP, 1);
				qs.giveItems(TRAP_STONE, 100);
				qs.giveItems(57, 1022636);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
			}
			return event;
		} else if ("32115-04.htm".equalsIgnoreCase(event)
				|| "32115-004.htm".equalsIgnoreCase(event)
				|| "asamah_q111_4.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32115-004.htm";
		} else if ("32115-06.htm".equalsIgnoreCase(event)
				|| "32115-006.htm".equalsIgnoreCase(event)
				|| "asamah_q111_5.htm".equalsIgnoreCase(event)) {
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "32115-006.htm";
		} else if ("32116-007.htm".equalsIgnoreCase(event)
				|| "asamah_q111_8.htm".equalsIgnoreCase(event)) {
			qs.takeItems(PRACTICE_TRAP, -1);
			qs.giveItems(ELROKIAN_TRAP, 1);
			qs.giveItems(TRAP_STONE, 100);
			qs.giveItems(57, 1022636);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "32116-007.htm";
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

		if (npcId == MARQUEZ) {
			if (cond == 0) {
				if (pc.level() >= 75) {
					return "marquez_q0111_0101.htm";
				}
				qs.exitQuest(true);
				return "marquez_q0111_0103.htm";
			} else if (cond == 1) {
				return "marquez_q0111_0105.htm";
			} else if (cond == 2) {
				qs.giveItems(ELROKIAN_TRAP, 1);
				qs.giveItems(TRAP_STONE, 100);
				qs.giveItems(57, 102225);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "marquez_q0111_0301.htm";
			} else if (cond == 3) {
				return "32113-003.htm";
			} else if (cond == 4) {
				return "32113-009.htm";
			} else if (cond == 5) {
				qs.takeItems(FRAGMENT, -1);
				if (!qs.hasQuestItems(EXPEDITION_LETTER)) {
					qs.giveItems(EXPEDITION_LETTER, 1);
				}
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "32113-010.htm";
			}
		} else if (npcId == MUSHIKA) {
			if (cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return "32114-001.htm";
		} else if (npcId == ASAMAH) {
			if (cond == 2) {
				return "32115-001.htm";
			} else if (cond == 3) {
				return "32115-002.htm";
			} else if (cond == 8) {
				return "32115-003.htm";
			} else if (cond == 9) {
				return "32115-004.htm";
			} else if (cond == 10) {
				return "32115-006.htm";
			} else if (cond == 11) {
				qs.takeAllItems(CLAW, BONE, SKIN);
				qs.giveItems(PRACTICE_TRAP, 1);
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "32115-007.htm";
			} else if (cond == 12) {
				return "32115-007.htm";
			}
		} else if (npcId == KIRIKASHIN || npcId == KARAKAWEI) {
			if (npcId == KARAKAWEI && cond == 1) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "kirikachin_q0111_0201.htm";
			} else if (cond < 6) {
				return "32116-008.htm";
			} else if (cond == 6) {
				qs.takeItems(EXPEDITION_LETTER, -1);
				return "32116-001.htm";
			} else if (cond == 7) {
				return "32116-004.htm";
			} else if (cond == 12) {
				qs.takeItems(PRACTICE_TRAP, -1);
				qs.giveItems(ELROKIAN_TRAP, 1);
				qs.giveItems(TRAP_STONE, 100);
				qs.giveItems(57, 1022636);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "32116-006.htm";
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
		if (cond == 4) {
			if (npcId == 22196 || npcId == 22197 || npcId == 22198 || npcId == 22218) {
				if (qs.rollAndGive(FRAGMENT, 1, 1, 50, 25.0) && qs.getQuestItemsCount(FRAGMENT) >= 50) {
					qs.setCond(5);
				}
			}
		} else if (cond == 10) {
			if (npcId == 22200 || npcId == 22201 || npcId == 22202 || npcId == 22219) {
				qs.rollAndGive(CLAW, 1, 1, 10, 65.0);
			} else if (npcId == 22203 || npcId == 22204 || npcId == 22205 || npcId == 22220) {
				qs.rollAndGive(BONE, 1, 1, 10, 65.0);
			} else if (npcId == 22208 || npcId == 22209 || npcId == 22210
					|| npcId == 22211 || npcId == 22212 || npcId == 22213 || npcId == 22221) {
				qs.rollAndGive(SKIN, 1, 1, 10, 65.0);
			}
			if (qs.getQuestItemsCount(CLAW) >= 10
					&& qs.getQuestItemsCount(BONE) >= 10
					&& qs.getQuestItemsCount(SKIN) >= 10) {
				qs.setCond(11);
			}
		}
		return null;
	}
}
