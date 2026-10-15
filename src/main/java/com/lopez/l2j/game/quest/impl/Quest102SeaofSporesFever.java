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
 * Quest 102: Sea of Spores Fever
 */
@Component
public class Quest102SeaofSporesFever extends Quest {

	public static final int QUEST_ID = 102;
	public static final String QUEST_NAME = "102_SeaofSporesFever";
	public static final int START_NPC = 30284;

	public static final int ALBERIUS = 30284;
	public static final int COBENDELL = 30156;
	public static final int BERROS = 30217;
	public static final int VELTRESS = 30219;
	public static final int RAYEN = 30221;
	public static final int GARTRANDELL = 30285;

	public static final int ALBERIUS_LETTER = 964;
	public static final int EVERGREEN_AMULET = 965;
	public static final int DRYAD_TEARS = 966;
	public static final int ALBERIUS_LIST = 746;
	public static final int COBENDELL_MEDICINE_1 = 1130;
	public static final int COBENDELL_MEDICINE_2 = 1131;
	public static final int COBENDELL_MEDICINE_3 = 1132;
	public static final int COBENDELL_MEDICINE_4 = 1133;
	public static final int COBENDELL_MEDICINE_5 = 1134;

	public static final int SWORD_OF_SENTINEL = 743;
	public static final int STAFF_OF_SENTINEL = 744;

	public static final int DRYAD = 20013;
	public static final int DRYAD_ELDER = 20019;

	@Autowired
	public Quest102SeaofSporesFever(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Sea of Spores Fever");
		addStartNpc(START_NPC);
		addTalkId(ALBERIUS, COBENDELL, BERROS, VELTRESS, RAYEN, GARTRANDELL);
		addKillId(DRYAD, DRYAD_ELDER);
		registerQuestItems(ALBERIUS_LETTER, EVERGREEN_AMULET, DRYAD_TEARS,
				COBENDELL_MEDICINE_1, COBENDELL_MEDICINE_2, COBENDELL_MEDICINE_3,
				COBENDELL_MEDICINE_4, COBENDELL_MEDICINE_5, ALBERIUS_LIST);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private void checkSentinels(QuestState qs) {
		if (!qs.hasQuestItems(COBENDELL_MEDICINE_2)
				&& !qs.hasQuestItems(COBENDELL_MEDICINE_3)
				&& !qs.hasQuestItems(COBENDELL_MEDICINE_4)
				&& !qs.hasQuestItems(COBENDELL_MEDICINE_5)) {
			qs.setCond(6);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return event;
		}
		if ("quest_accept".equalsIgnoreCase(event)
				|| "alberryus_q0102_02.htm".equalsIgnoreCase(event)
				|| "30284-02.htm".equalsIgnoreCase(event)
				|| "alberryus_letter".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			if (!qs.hasQuestItems(ALBERIUS_LETTER)) {
				qs.giveItems(ALBERIUS_LETTER, 1);
			}
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event.endsWith(".htm") ? event : "alberryus_q0102_02.htm";
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

		if (npcId == ALBERIUS) {
			if (cond == 0) {
				if (pc.race() != 1) {
					qs.exitQuest(true);
					return "alberryus_q0102_00.htm";
				}
				if (pc.level() >= 12) {
					return "alberryus_q0102_07.htm";
				}
				qs.exitQuest(true);
				return "alberryus_q0102_08.htm";
			} else if (cond == 1) {
				return "alberryus_q0102_03.htm";
			} else if (cond == 2 || cond == 3) {
				return "alberryus_q0102_09.htm";
			} else if (cond == 4) {
				if (qs.hasQuestItems(COBENDELL_MEDICINE_1)) {
					qs.takeItems(COBENDELL_MEDICINE_1, 1);
					qs.giveItems(ALBERIUS_LIST, 1);
					qs.setCond(5);
					return "alberryus_q0102_04.htm";
				} else if (qs.hasQuestItems(DRYAD_TEARS)) {
					qs.takeItems(DRYAD_TEARS, -1);
					qs.giveItems(SWORD_OF_SENTINEL, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					qs.exitQuest(false);
					return "alberryus_q0102_05.htm";
				}
			} else if (cond == 5) {
				return "alberryus_q0102_05.htm";
			} else if (cond == 6 && qs.hasQuestItems(ALBERIUS_LIST)) {
				qs.takeItems(ALBERIUS_LIST, -1);
				qs.giveItems(SWORD_OF_SENTINEL, 1);
				qs.rewardItems(1835, 1000);
				qs.giveItems(1060, 100);
				for (int echo = 4412; echo <= 4416; echo++) {
					qs.giveItems(echo, 10);
				}
				qs.addExpAndSp(30202, 1339);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "alberryus_q0102_06.htm";
			}
		} else if (npcId == COBENDELL) {
			if (cond == 1 && qs.hasQuestItems(ALBERIUS_LETTER)) {
				qs.takeItems(ALBERIUS_LETTER, 1);
				qs.giveItems(EVERGREEN_AMULET, 1);
				qs.setCond(2);
				return "cob_q0102_03.htm";
			} else if (cond == 2) {
				return "cob_q0102_04.htm";
			} else if (cond == 3 && qs.getQuestItemsCount(DRYAD_TEARS) >= 10) {
				qs.takeItems(EVERGREEN_AMULET, 1);
				qs.takeItems(DRYAD_TEARS, -1);
				qs.giveItems(COBENDELL_MEDICINE_1, 1);
				qs.giveItems(COBENDELL_MEDICINE_2, 1);
				qs.giveItems(COBENDELL_MEDICINE_3, 1);
				qs.giveItems(COBENDELL_MEDICINE_4, 1);
				qs.giveItems(COBENDELL_MEDICINE_5, 1);
				qs.setCond(4);
				return "cob_q0102_05.htm";
			} else if (cond == 4) {
				return "cob_q0102_06.htm";
			} else if (cond >= 5) {
				return "cob_q0102_07.htm";
			}
		} else if (npcId == BERROS && cond == 5 && qs.hasQuestItems(COBENDELL_MEDICINE_2)) {
			qs.takeItems(COBENDELL_MEDICINE_2, 1);
			checkSentinels(qs);
			return "sentinel_berryos_q0102_01.htm";
		} else if (npcId == VELTRESS && cond == 5 && qs.hasQuestItems(COBENDELL_MEDICINE_3)) {
			qs.takeItems(COBENDELL_MEDICINE_3, 1);
			checkSentinels(qs);
			return "sentinel_veltress_q0102_01.htm";
		} else if (npcId == RAYEN && cond == 5 && qs.hasQuestItems(COBENDELL_MEDICINE_4)) {
			qs.takeItems(COBENDELL_MEDICINE_4, 1);
			checkSentinels(qs);
			return "sentinel_rayjien_q0102_01.htm";
		} else if (npcId == GARTRANDELL && cond == 5 && qs.hasQuestItems(COBENDELL_MEDICINE_5)) {
			qs.takeItems(COBENDELL_MEDICINE_5, 1);
			checkSentinels(qs);
			return "sentinel_gartrandell_q0102_01.htm";
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}
		int npcId = npc.getNpcId();
		if ((npcId == DRYAD || npcId == DRYAD_ELDER) && qs.getCond() == 2 && qs.hasQuestItems(EVERGREEN_AMULET)) {
			if (qs.rollAndGive(DRYAD_TEARS, 1, 1, 10, 33.0) && qs.getQuestItemsCount(DRYAD_TEARS) >= 10) {
				qs.setCond(3);
			}
		}
		return null;
	}
}
