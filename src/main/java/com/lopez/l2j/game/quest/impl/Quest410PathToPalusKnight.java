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
 * Quest 410: Path to Palus Knight (1ª Troca de Classe do Dark Fighter para Palus Knight).
 */
@Component
public class Quest410PathToPalusKnight extends Quest {

	public static final int QUEST_ID = 410;
	public static final String QUEST_NAME = "410_PathToPalusKnight";

	// NPCs
	public static final int VIRGIL = 30329;
	public static final int KALINTA = 30422;

	// Monstros
	public static final int POISON_SPIDER = 20038;
	public static final int ARACHNID_TRACKER = 20043;
	public static final int LYCANTHROPE = 20049;

	// Itens
	public static final int PALLUS_TALISMAN = 1237;
	public static final int LYCANTHROPE_SKULL = 1238;
	public static final int VIRGILS_LETTER = 1239;
	public static final int MORTE_TALISMAN = 1240;
	public static final int PREDATOR_CARAPACE = 1241;
	public static final int TRIMDEN_SILK = 1242;
	public static final int COFFIN_ETERNAL_REST = 1243;
	public static final int GAZE_OF_ABYSS = 1244;

	@Autowired
	public Quest410PathToPalusKnight(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Palus Knight");

		addStartNpc(VIRGIL);
		addTalkId(VIRGIL);
		addTalkId(KALINTA);

		addKillId(POISON_SPIDER);
		addKillId(ARACHNID_TRACKER);
		addKillId(LYCANTHROPE);

		registerQuestItems(PALLUS_TALISMAN, LYCANTHROPE_SKULL, VIRGILS_LETTER,
				MORTE_TALISMAN, PREDATOR_CARAPACE, TRIMDEN_SILK, COFFIN_ETERNAL_REST);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int level = player != null ? player.getLevel() : 0;
		int classId = player != null ? player.getClassId() : -1;

		if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			qs.giveItems(PALLUS_TALISMAN, 1);
			return "30329-06.htm";
		} else if ("410_1".equalsIgnoreCase(event)) {
			if (level >= 18 && classId == 0x1f && qs.getQuestItemsCount(GAZE_OF_ABYSS) == 0) {
				return "30329-05.htm";
			} else if (classId != 0x1f) {
				return classId == 0x20 ? "30329-02a.htm" : "30329-03.htm";
			} else if (level < 18 && classId == 0x1f) {
				return "30329-02.htm";
			} else if (qs.getQuestItemsCount(GAZE_OF_ABYSS) > 0) {
				return "30329-04.htm";
			}
		} else if ("30329_2".equalsIgnoreCase(event)) {
			qs.takeItems(PALLUS_TALISMAN, 1);
			qs.takeItems(LYCANTHROPE_SKULL, -1);
			qs.giveItems(VIRGILS_LETTER, 1);
			qs.setCond(3);
			qs.playSound("ItemSound.quest_middle");
			return "30329-10.htm";
		} else if ("30422_1".equalsIgnoreCase(event)) {
			qs.takeItems(VIRGILS_LETTER, 1);
			qs.giveItems(MORTE_TALISMAN, 1);
			qs.setCond(4);
			qs.playSound("ItemSound.quest_middle");
			return "30422-02.htm";
		} else if ("30422_2".equalsIgnoreCase(event)) {
			qs.takeItems(MORTE_TALISMAN, 1);
			qs.takeItems(TRIMDEN_SILK, -1);
			qs.takeItems(PREDATOR_CARAPACE, -1);
			qs.giveItems(COFFIN_ETERNAL_REST, 1);
			qs.setCond(6);
			qs.playSound("ItemSound.quest_middle");
			return "30422-06.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == VIRGIL) {
			if (cond == 0) {
				return "30329-01.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(PALLUS_TALISMAN) == 1 && qs.getQuestItemsCount(LYCANTHROPE_SKULL) == 0) {
					return "30329-07.htm";
				} else if (qs.getQuestItemsCount(PALLUS_TALISMAN) == 1 && qs.getQuestItemsCount(LYCANTHROPE_SKULL) > 0 && qs.getQuestItemsCount(LYCANTHROPE_SKULL) < 13) {
					return "30329-08.htm";
				} else if (qs.getQuestItemsCount(PALLUS_TALISMAN) == 1 && qs.getQuestItemsCount(LYCANTHROPE_SKULL) >= 13) {
					return "30329-09.htm";
				} else if (qs.getQuestItemsCount(COFFIN_ETERNAL_REST) == 1) {
					qs.rewardItems(57, 81900);
					qs.takeItems(COFFIN_ETERNAL_REST, 1);
					qs.giveItems(GAZE_OF_ABYSS, 1);
					qs.addExpAndSp(295862, 19804);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30329-11.htm";
				} else if (qs.getQuestItemsCount(MORTE_TALISMAN) > 0 || qs.getQuestItemsCount(VIRGILS_LETTER) > 0) {
					return "30329-12.htm";
				}
			}
		} else if (npcId == KALINTA && cond > 0) {
			if (qs.getQuestItemsCount(VIRGILS_LETTER) > 0) {
				return "30422-01.htm";
			} else if (qs.getQuestItemsCount(MORTE_TALISMAN) > 0) {
				long silk = qs.getQuestItemsCount(TRIMDEN_SILK);
				long carapace = qs.getQuestItemsCount(PREDATOR_CARAPACE);
				if (silk == 0 && carapace == 0) {
					return "30422-03.htm";
				} else if (silk >= 5 && carapace > 0) {
					return "30422-05.htm";
				} else {
					return "30422-04.htm";
				}
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

		if (npcId == LYCANTHROPE) {
			if (cond > 0 && qs.getQuestItemsCount(PALLUS_TALISMAN) == 1 && qs.getQuestItemsCount(LYCANTHROPE_SKULL) < 13) {
				qs.giveItems(LYCANTHROPE_SKULL, 1);
				if (qs.getQuestItemsCount(LYCANTHROPE_SKULL) == 13) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(2);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == POISON_SPIDER) {
			if (cond > 0 && qs.getQuestItemsCount(MORTE_TALISMAN) == 1 && qs.getQuestItemsCount(PREDATOR_CARAPACE) < 1) {
				qs.giveItems(PREDATOR_CARAPACE, 1);
				qs.playSound("ItemSound.quest_middle");
				if (qs.getQuestItemsCount(TRIMDEN_SILK) >= 5) {
					qs.setCond(5);
				}
			}
		} else if (npcId == ARACHNID_TRACKER) {
			if (cond > 0 && qs.getQuestItemsCount(MORTE_TALISMAN) == 1 && qs.getQuestItemsCount(TRIMDEN_SILK) < 5) {
				qs.giveItems(TRIMDEN_SILK, 1);
				if (qs.getQuestItemsCount(TRIMDEN_SILK) == 5) {
					qs.playSound("ItemSound.quest_middle");
					if (qs.getQuestItemsCount(PREDATOR_CARAPACE) > 0) {
						qs.setCond(5);
					}
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return null;
	}
}
