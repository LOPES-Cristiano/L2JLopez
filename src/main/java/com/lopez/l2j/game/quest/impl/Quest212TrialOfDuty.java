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
 * Quest 212: Trial of Duty (1º Passo de 2ª Classe para Paladin, Dark Avenger, Temple Knight, Shillien Knight).
 */
@Component
public class Quest212TrialOfDuty extends Quest {

	public static final int QUEST_ID = 212;
	public static final String QUEST_NAME = "212_TrialOfDuty";

	// NPCs
	public static final int HANNAVALT = 30109;
	public static final int DUSTIN = 30116;
	public static final int SIR_COLLIN_WINDAWOOD = 30311;
	public static final int SIR_ARON_TANFORD = 30653;
	public static final int SIR_KIEL_NIGHTHAWK = 30654;
	public static final int ISAEL_SILVERSHADOW = 30655;
	public static final int SPIRIT_OF_SIR_TALIANUS = 30656;

	// Monstros
	public static final int SKELETON_MARAUDER = 20190;
	public static final int SKELETON_RAIDER = 20191;
	public static final int SPIRIT_OF_SIR_HEROD = 27119;
	public static final int STRAIN = 20200;
	public static final int GHOUL = 20201;
	public static final int MEDUSA = 20144;
	public static final int ANT_RECRUIT = 20270;

	// Itens
	public static final int MARK_OF_DUTY = 2633;
	public static final int LETTER_OF_DUSTIN = 2634;
	public static final int KNIGHTS_TEAR = 2635;
	public static final int MIRROR_OF_ORPIC = 2636;
	public static final int TEAR_OF_CONFESSION = 2637;
	public static final int REPORT_PIECE = 2638;
	public static final int TALIANUSS_REPORT = 2639;
	public static final int TEAR_OF_LOYALTY = 2640;
	public static final int MILITIA_WEAPON_PIECE = 2641;
	public static final int ATHEBALTS_SKULL = 2642;
	public static final int ATHEBALTS_RIBS = 2643;
	public static final int ATHEBALTS_SHIN = 2644;
	public static final int ATHEBALTS_ARM = 2645;
	public static final int LETTER_OF_WINDAWOOD = 2646;
	public static final int OLD_KNIGHT_SWORD = 3027;

	@Autowired
	public Quest212TrialOfDuty(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of Duty");

		addStartNpc(HANNAVALT);
		addTalkId(HANNAVALT);
		addTalkId(DUSTIN);
		addTalkId(SIR_COLLIN_WINDAWOOD);
		addTalkId(SIR_ARON_TANFORD);
		addTalkId(SIR_KIEL_NIGHTHAWK);
		addTalkId(ISAEL_SILVERSHADOW);
		addTalkId(SPIRIT_OF_SIR_TALIANUS);

		addKillId(SKELETON_MARAUDER);
		addKillId(SKELETON_RAIDER);
		addKillId(SPIRIT_OF_SIR_HEROD);
		addKillId(STRAIN);
		addKillId(GHOUL);
		addKillId(MEDUSA);
		addKillId(ANT_RECRUIT);
		for (int mob = 20577; mob <= 20582; mob++) {
			addKillId(mob);
		}

		registerQuestItems(LETTER_OF_DUSTIN, KNIGHTS_TEAR, OLD_KNIGHT_SWORD, MIRROR_OF_ORPIC,
				TEAR_OF_CONFESSION, REPORT_PIECE, TALIANUSS_REPORT, TEAR_OF_LOYALTY,
				MILITIA_WEAPON_PIECE, ATHEBALTS_SKULL, ATHEBALTS_RIBS, ATHEBALTS_SHIN,
				ATHEBALTS_ARM, LETTER_OF_WINDAWOOD);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.playSound("ItemSound.quest_accept");
			qs.setCond(1);
			return "30109-04.htm";
		} else if ("30116_1".equalsIgnoreCase(event)) {
			return "30116-02.htm";
		} else if ("30116_2".equalsIgnoreCase(event)) {
			return "30116-03.htm";
		} else if ("30116_3".equalsIgnoreCase(event)) {
			return "30116-04.htm";
		} else if ("30116_4".equalsIgnoreCase(event)) {
			qs.takeItems(TEAR_OF_LOYALTY, -1);
			qs.setCond(14);
			return "30116-05.htm";
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

		if (qs.getQuestItemsCount(MARK_OF_DUTY) > 0) {
			return "completed";
		}

		if (npcId == HANNAVALT) {
			if (cond == 0) {
				int cId = player.getClassId();
				if (cId == 4 || cId == 19 || cId == 32) {
					if (player.getLevel() >= 35) {
						return "30109-03.htm";
					}
					return "30109-01.htm";
				}
				return "30109-02.htm";
			} else if (cond == 18 && qs.getQuestItemsCount(LETTER_OF_DUSTIN) > 0) {
				qs.takeItems(LETTER_OF_DUSTIN, -1);
				qs.giveItems(MARK_OF_DUTY, 1);
				qs.rewardItems(7562, 8); // Dimensional Diamond
				qs.addExpAndSp(79832, 37500);
				qs.playSound("ItemSound.quest_finish");
				qs.exitCurrentQuest(false);
				return "30109-05.htm";
			} else if (cond == 1) {
				return "30109-04.htm";
			}
		} else if (npcId == SIR_ARON_TANFORD) {
			if (cond == 1) {
				if (qs.getQuestItemsCount(OLD_KNIGHT_SWORD) == 0) {
					qs.giveItems(OLD_KNIGHT_SWORD, 1);
				}
				qs.setCond(2);
				qs.playSound("ItemSound.quest_middle");
				return "30653-01.htm";
			} else if (cond == 2 && qs.getQuestItemsCount(KNIGHTS_TEAR) == 0) {
				return "30653-02.htm";
			} else if (cond == 3 && qs.getQuestItemsCount(KNIGHTS_TEAR) > 0) {
				qs.takeItems(KNIGHTS_TEAR, -1);
				qs.takeItems(OLD_KNIGHT_SWORD, -1);
				qs.setCond(4);
				qs.playSound("ItemSound.quest_middle");
				return "30653-03.htm";
			} else if (cond == 4) {
				return "30653-04.htm";
			}
		} else if (npcId == SIR_KIEL_NIGHTHAWK) {
			if (cond == 4) {
				qs.setCond(5);
				qs.playSound("ItemSound.quest_middle");
				return "30654-01.htm";
			} else if (cond == 5 && qs.getQuestItemsCount(TALIANUSS_REPORT) == 0) {
				return "30654-02.htm";
			} else if (cond == 6 && qs.getQuestItemsCount(TALIANUSS_REPORT) > 0) {
				qs.giveItems(MIRROR_OF_ORPIC, 1);
				qs.setCond(7);
				qs.playSound("ItemSound.quest_middle");
				return "30654-03.htm";
			} else if (cond == 7 && qs.getQuestItemsCount(MIRROR_OF_ORPIC) > 0) {
				return "30654-04.htm";
			} else if (qs.getQuestItemsCount(TEAR_OF_CONFESSION) > 0) {
				qs.takeItems(TEAR_OF_CONFESSION, -1);
				qs.setCond(10);
				qs.playSound("ItemSound.quest_middle");
				return "30654-05.htm";
			} else if (cond == 10) {
				return "30654-06.htm";
			}
		} else if (npcId == SPIRIT_OF_SIR_TALIANUS) {
			if (cond == 8 && qs.getQuestItemsCount(MIRROR_OF_ORPIC) > 0) {
				qs.takeItems(MIRROR_OF_ORPIC, -1);
				qs.takeItems(TALIANUSS_REPORT, -1);
				qs.giveItems(TEAR_OF_CONFESSION, 1);
				qs.setCond(9);
				qs.playSound("ItemSound.quest_middle");
				return "30656-01.htm";
			}
		} else if (npcId == ISAEL_SILVERSHADOW) {
			if (cond == 10) {
				if (player.getLevel() >= 36) {
					qs.setCond(11);
					qs.playSound("ItemSound.quest_middle");
					return "30655-02.htm";
				}
				return "30655-01.htm";
			} else if (cond == 11) {
				return "30655-03.htm";
			} else if (cond == 12 && qs.getQuestItemsCount(MILITIA_WEAPON_PIECE) >= 20) {
				qs.takeItems(MILITIA_WEAPON_PIECE, -1);
				qs.giveItems(TEAR_OF_LOYALTY, 1);
				qs.setCond(13);
				qs.playSound("ItemSound.quest_middle");
				return "30655-04.htm";
			} else if (cond == 13) {
				return "30655-05.htm";
			}
		} else if (npcId == DUSTIN) {
			if (cond == 13 && qs.getQuestItemsCount(TEAR_OF_LOYALTY) > 0) {
				return "30116-01.htm";
			} else if (cond == 14) {
				return "30116-06.htm";
			} else if (cond == 15) {
				qs.takeItems(ATHEBALTS_SKULL, -1);
				qs.takeItems(ATHEBALTS_RIBS, -1);
				qs.takeItems(ATHEBALTS_SHIN, -1);
				qs.takeItems(ATHEBALTS_ARM, -1);
				qs.giveItems(ATHEBALTS_SKULL, 1);
				qs.setCond(16);
				qs.playSound("ItemSound.quest_middle");
				return "30116-07.htm";
			} else if (cond == 16) {
				return "30116-09.htm";
			} else if (cond == 17 && qs.getQuestItemsCount(LETTER_OF_WINDAWOOD) > 0) {
				qs.takeItems(LETTER_OF_WINDAWOOD, -1);
				qs.giveItems(LETTER_OF_DUSTIN, 1);
				qs.setCond(18);
				qs.playSound("ItemSound.quest_middle");
				return "30116-08.htm";
			} else if (cond == 18) {
				return "30116-10.htm";
			}
		} else if (npcId == SIR_COLLIN_WINDAWOOD) {
			if (cond == 16) {
				qs.takeItems(ATHEBALTS_SKULL, -1);
				qs.giveItems(LETTER_OF_WINDAWOOD, 1);
				qs.setCond(17);
				qs.playSound("ItemSound.quest_middle");
				return "30311-01.htm";
			} else if (cond == 17) {
				return "30311-02.htm";
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

		if (cond == 2 && (npcId == SKELETON_MARAUDER || npcId == SKELETON_RAIDER)) {
			if (ThreadLocalRandom.current().nextInt(100) < 30) {
				qs.giveItems(KNIGHTS_TEAR, 1);
				qs.setCond(3);
				qs.playSound("ItemSound.quest_middle");
			}
		} else if (cond == 5 && (npcId == STRAIN || npcId == GHOUL)) {
			if (qs.getQuestItemsCount(REPORT_PIECE) < 10) {
				qs.giveItems(REPORT_PIECE, 1);
				if (qs.getQuestItemsCount(REPORT_PIECE) >= 10) {
					qs.takeItems(REPORT_PIECE, -1);
					qs.giveItems(TALIANUSS_REPORT, 1);
					qs.setCond(6);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 7 && npcId == MEDUSA) {
			qs.setCond(8);
			qs.playSound("ItemSound.quest_middle");
		} else if (cond == 11 && npcId >= 20577 && npcId <= 20582) {
			if (qs.getQuestItemsCount(MILITIA_WEAPON_PIECE) < 20) {
				qs.giveItems(MILITIA_WEAPON_PIECE, 1);
				if (qs.getQuestItemsCount(MILITIA_WEAPON_PIECE) >= 20) {
					qs.setCond(12);
					qs.playSound("ItemSound.quest_middle");
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (cond == 14 && npcId == ANT_RECRUIT) {
			if (qs.getQuestItemsCount(ATHEBALTS_SKULL) == 0) {
				qs.giveItems(ATHEBALTS_SKULL, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (qs.getQuestItemsCount(ATHEBALTS_RIBS) == 0) {
				qs.giveItems(ATHEBALTS_RIBS, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (qs.getQuestItemsCount(ATHEBALTS_SHIN) == 0) {
				qs.giveItems(ATHEBALTS_SHIN, 1);
				qs.playSound("ItemSound.quest_itemget");
			} else if (qs.getQuestItemsCount(ATHEBALTS_ARM) == 0) {
				qs.giveItems(ATHEBALTS_ARM, 1);
				qs.setCond(15);
				qs.playSound("ItemSound.quest_middle");
			}
		}

		return null;
	}
}
