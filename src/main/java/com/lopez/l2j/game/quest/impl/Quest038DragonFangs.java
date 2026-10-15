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
 * Quest 38: Dragon Fangs
 * Guard Luis, Magister Iris e Magister Rohmer em Gludin / Gludio.
 */
@Component
public class Quest038DragonFangs extends Quest {

	public static final int QUEST_ID = 38;
	public static final String QUEST_NAME = "038_DragonFangs";

	public static final int START_NPC = 30386;
	public static final int LUIS = 30386;
	public static final int IRIS = 30034;
	public static final int ROHMER = 30344;

	public static final int LANGK_LIZARDMAN_LEADER = 20356;
	public static final int LANGK_LIZARDMAN_LIEUTENANT = 20357;
	public static final int LANGK_LIZARDMAN_SENTINEL = 21100;
	public static final int LANGK_LIZARDMAN_SHAMAN = 21101;

	public static final int FEATHER_ORNAMENT = 7173;
	public static final int TOOTH_OF_TOTEM = 7174;
	public static final int TOOTH_OF_DRAGON = 7175;
	public static final int LETTER_OF_IRIS = 7176;
	public static final int LETTER_OF_ROHMER = 7177;

	public static final int BONE_HELMET = 45;
	public static final int ASSAULT_BOOTS = 1125;
	public static final int BLUE_BUCKSKIN_BOOTS = 1123;

	@Autowired
	public Quest038DragonFangs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Dragon Fangs");
		addStartNpc(LUIS);
		addTalkId(LUIS, IRIS, ROHMER);
		addKillId(LANGK_LIZARDMAN_LEADER, LANGK_LIZARDMAN_LIEUTENANT, LANGK_LIZARDMAN_SENTINEL, LANGK_LIZARDMAN_SHAMAN);
		registerQuestItems(FEATHER_ORNAMENT, TOOTH_OF_TOTEM, TOOTH_OF_DRAGON, LETTER_OF_IRIS, LETTER_OF_ROHMER);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "30386-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30386-02.htm".equalsIgnoreCase(event) ? "30386-02.htm" : "guard_luis_q0038_0104.htm";
		} else if ("30386-04.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(FEATHER_ORNAMENT) >= 100) {
				qs.setCond(3);
				qs.takeItems(FEATHER_ORNAMENT, 100);
				qs.giveItems(TOOTH_OF_TOTEM, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30386-04.htm";
			}
			return "guard_luis_q0038_0202.htm";
		} else if ("30034-02a.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(TOOTH_OF_TOTEM)) {
				qs.setCond(4);
				qs.takeItems(TOOTH_OF_TOTEM, 1);
				qs.giveItems(LETTER_OF_IRIS, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30034-02a.htm";
			}
			return "iris_q0038_0302.htm";
		} else if ("30344-02a.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(LETTER_OF_IRIS)) {
				qs.setCond(5);
				qs.takeItems(LETTER_OF_IRIS, 1);
				qs.giveItems(LETTER_OF_ROHMER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30344-02a.htm";
			}
			return "magister_rohmer_q0038_0402.htm";
		} else if ("30034-04a.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(LETTER_OF_ROHMER)) {
				qs.setCond(6);
				qs.takeItems(LETTER_OF_ROHMER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30034-04a.htm";
			}
			return "iris_q0038_0502.htm";
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 2 && qs.getQuestItemsCount(FEATHER_ORNAMENT) >= 100) {
				qs.setCond(3);
				qs.takeItems(FEATHER_ORNAMENT, 100);
				qs.giveItems(TOOTH_OF_TOTEM, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "guard_luis_q0038_0201.htm";
			} else if (cond == 3 && qs.hasQuestItems(TOOTH_OF_TOTEM)) {
				qs.setCond(4);
				qs.takeItems(TOOTH_OF_TOTEM, 1);
				qs.giveItems(LETTER_OF_IRIS, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "iris_q0038_0301.htm";
			} else if (cond == 4 && qs.hasQuestItems(LETTER_OF_IRIS)) {
				qs.setCond(5);
				qs.takeItems(LETTER_OF_IRIS, 1);
				qs.giveItems(LETTER_OF_ROHMER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "magister_rohmer_q0038_0401.htm";
			} else if (cond == 5 && qs.hasQuestItems(LETTER_OF_ROHMER)) {
				qs.setCond(6);
				qs.takeItems(LETTER_OF_ROHMER, 1);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "iris_q0038_0501.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event) || "30034-06a.htm".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(TOOTH_OF_DRAGON) >= 50) {
				qs.takeItems(TOOTH_OF_DRAGON, -1);
				qs.giveItems(BONE_HELMET, 1);
				qs.giveItems(57, 5200);
				qs.addExpAndSp(435117, 23977);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitQuest(false);
				return "30034-06a.htm".equalsIgnoreCase(event) ? "30034-06a.htm" : "iris_q0038_0601.htm";
			}
			return "iris_q0038_0602.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		if (qs.isCompleted()) {
			return getAlreadyCompletedMsg();
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		switch (npcId) {
			case LUIS:
				if (cond == 0) {
					if (pc.level() >= 19) return "guard_luis_q0038_0101.htm";
					qs.exitQuest(true);
					return "guard_luis_q0038_0103.htm";
				} else if (cond == 1) {
					return "guard_luis_q0038_0106.htm";
				} else if (cond == 2 && qs.getQuestItemsCount(FEATHER_ORNAMENT) >= 100) {
					return "guard_luis_q0038_0105.htm";
				} else if (cond >= 3) {
					return "guard_luis_q0038_0203.htm";
				}
				break;
			case IRIS:
				if (cond == 3 && qs.hasQuestItems(TOOTH_OF_TOTEM)) {
					return "iris_q0038_0201.htm";
				} else if (cond == 4) {
					return "iris_q0038_0303.htm";
				} else if (cond == 5 && qs.hasQuestItems(LETTER_OF_ROHMER)) {
					return "iris_q0038_0401.htm";
				} else if (cond == 6) {
					return "iris_q0038_0504.htm";
				} else if (cond == 7 && qs.getQuestItemsCount(TOOTH_OF_DRAGON) >= 50) {
					return "iris_q0038_0503.htm";
				}
				break;
			case ROHMER:
				if (cond == 4 && qs.hasQuestItems(LETTER_OF_IRIS)) {
					return "magister_rohmer_q0038_0301.htm";
				} else if (cond >= 5) {
					return "magister_rohmer_q0038_0403.htm";
				}
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		int cond = qs.getCond();
		if (cond == 1 && (npcId == LANGK_LIZARDMAN_LIEUTENANT || npcId == LANGK_LIZARDMAN_SENTINEL)) {
			long count = qs.getQuestItemsCount(FEATHER_ORNAMENT);
			if (count < 100) {
				qs.giveItems(FEATHER_ORNAMENT, 1);
				if (count + 1 >= 100) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		} else if (cond == 6 && (npcId == LANGK_LIZARDMAN_LEADER || npcId == LANGK_LIZARDMAN_SHAMAN)) {
			long count = qs.getQuestItemsCount(TOOTH_OF_DRAGON);
			if (count < 50) {
				qs.giveItems(TOOTH_OF_DRAGON, 1);
				if (count + 1 >= 50) {
					qs.setCond(7);
					qs.playSound(QuestState.SOUND_MIDDLE);
				} else {
					qs.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}
}
