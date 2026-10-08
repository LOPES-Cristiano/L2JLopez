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
 * Quest 413: Path to Shillien Oracle (1ª Troca de Classe do Dark Mystic para Shillien Oracle).
 */
@Component
public class Quest413PathToShillienOracle extends Quest {

	public static final int QUEST_ID = 413;
	public static final String QUEST_NAME = "413_PathToShillienOracle";

	// NPCs
	public static final int SIDRA = 30330;
	public static final int ADONIUS = 30375;
	public static final int TALBOT = 30377;

	// Monstros
	public static final int ZOMBIE_SOLDIER = 20457;
	public static final int ZOMBIE_WARRIOR = 20458;
	public static final int SHIELD_SKELETON = 20514;
	public static final int SKELETON_LORD = 20515;
	public static final int DARK_SUCCUBUS = 20776;

	// Itens
	public static final int SIDRAS_LETTER1 = 1262;
	public static final int BLANK_SHEET1 = 1263;
	public static final int BLOODY_RUNE1 = 1264;
	public static final int GARMIEL_BOOK = 1265;
	public static final int PRAYER_OF_ADON = 1266;
	public static final int PENITENTS_MARK = 1267;
	public static final int ASHEN_BONES = 1268;
	public static final int ANDARIEL_BOOK = 1269;
	public static final int ORB_OF_ABYSS = 1270;

	@Autowired
	public Quest413PathToShillienOracle(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Path of the Shillien Oracle");

		addStartNpc(SIDRA);
		addTalkId(SIDRA);
		addTalkId(ADONIUS);
		addTalkId(TALBOT);

		addKillId(ZOMBIE_SOLDIER);
		addKillId(ZOMBIE_WARRIOR);
		addKillId(SHIELD_SKELETON);
		addKillId(SKELETON_LORD);
		addKillId(DARK_SUCCUBUS);

		registerQuestItems(SIDRAS_LETTER1, BLANK_SHEET1, BLOODY_RUNE1, GARMIEL_BOOK,
				PRAYER_OF_ADON, PENITENTS_MARK, ASHEN_BONES, ANDARIEL_BOOK);

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
			qs.giveItems(SIDRAS_LETTER1, 1);
			return "30330-06.htm";
		} else if ("413_1".equalsIgnoreCase(event)) {
			if (level >= 18 && classId == 0x26 && qs.getQuestItemsCount(ORB_OF_ABYSS) == 0) {
				return "30330-05.htm";
			} else if (classId != 0x26) {
				return classId == 0x2a ? "30330-02a.htm" : "30330-03.htm";
			} else if (level < 18 && classId == 0x26) {
				return "30330-02.htm";
			} else if (qs.getQuestItemsCount(ORB_OF_ABYSS) > 0) {
				return "30330-04.htm";
			}
		} else if ("30377_1".equalsIgnoreCase(event)) {
			qs.takeItems(SIDRAS_LETTER1, 1);
			qs.giveItems(BLANK_SHEET1, 5);
			qs.setCond(2);
			return "30377-02.htm";
		} else if ("30375_1".equalsIgnoreCase(event)) {
			return "30375-02.htm";
		} else if ("30375_2".equalsIgnoreCase(event)) {
			return "30375-03.htm";
		} else if ("30375_3".equalsIgnoreCase(event)) {
			qs.takeItems(PRAYER_OF_ADON, 1);
			qs.giveItems(PENITENTS_MARK, 1);
			qs.setCond(5);
			return "30375-04.htm";
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

		if (npcId == SIDRA) {
			if (cond == 0) {
				return "30330-01.htm";
			} else if (cond > 0) {
				if (qs.getQuestItemsCount(SIDRAS_LETTER1) == 1) {
					return "30330-07.htm";
				} else if (qs.getQuestItemsCount(BLANK_SHEET1) > 0 || qs.getQuestItemsCount(BLOODY_RUNE1) > 0) {
					return "30330-08.htm";
				} else if (qs.getQuestItemsCount(ANDARIEL_BOOK) == 1 && qs.getQuestItemsCount(GARMIEL_BOOK) == 1) {
					qs.rewardItems(57, 81900);
					qs.takeItems(ANDARIEL_BOOK, 1);
					qs.takeItems(GARMIEL_BOOK, 1);
					qs.giveItems(ORB_OF_ABYSS, 1);
					qs.addExpAndSp(295862, 19664);
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound("ItemSound.quest_finish");
					return "30330-10.htm";
				} else if (qs.getQuestItemsCount(ANDARIEL_BOOK) == 0
						&& (qs.getQuestItemsCount(PRAYER_OF_ADON) + qs.getQuestItemsCount(GARMIEL_BOOK)
								+ qs.getQuestItemsCount(PENITENTS_MARK) + qs.getQuestItemsCount(ASHEN_BONES) > 0)) {
					return "30330-09.htm";
				}
			}
		} else if (npcId == TALBOT && cond > 0) {
			if (qs.getQuestItemsCount(SIDRAS_LETTER1) == 1) {
				return "30377-01.htm";
			} else if (qs.getQuestItemsCount(BLANK_SHEET1) == 5 && qs.getQuestItemsCount(BLOODY_RUNE1) == 0) {
				return "30377-03.htm";
			} else if (qs.getQuestItemsCount(BLOODY_RUNE1) > 0 && qs.getQuestItemsCount(BLOODY_RUNE1) < 5) {
				return "30377-04.htm";
			} else if (qs.getQuestItemsCount(BLOODY_RUNE1) >= 5) {
				qs.takeItems(BLOODY_RUNE1, -1);
				qs.giveItems(GARMIEL_BOOK, 1);
				qs.giveItems(PRAYER_OF_ADON, 1);
				qs.setCond(4);
				return "30377-05.htm";
			} else if (qs.getQuestItemsCount(ANDARIEL_BOOK) == 1 && qs.getQuestItemsCount(GARMIEL_BOOK) == 1) {
				return "30377-07.htm";
			} else if (qs.getQuestItemsCount(PRAYER_OF_ADON) + qs.getQuestItemsCount(PENITENTS_MARK) + qs.getQuestItemsCount(ASHEN_BONES) > 0) {
				return "30377-06.htm";
			}
		} else if (npcId == ADONIUS && cond > 0) {
			if (qs.getQuestItemsCount(PRAYER_OF_ADON) == 1) {
				return "30375-01.htm";
			} else if (qs.getQuestItemsCount(PENITENTS_MARK) == 1 && qs.getQuestItemsCount(ASHEN_BONES) == 0 && qs.getQuestItemsCount(ANDARIEL_BOOK) == 0) {
				return "30375-05.htm";
			} else if (qs.getQuestItemsCount(PENITENTS_MARK) == 1 && qs.getQuestItemsCount(ASHEN_BONES) < 10) {
				return "30375-06.htm";
			} else if (qs.getQuestItemsCount(PENITENTS_MARK) == 1 && qs.getQuestItemsCount(ASHEN_BONES) >= 10) {
				qs.takeItems(ASHEN_BONES, -1);
				qs.takeItems(PENITENTS_MARK, -1);
				qs.giveItems(ANDARIEL_BOOK, 1);
				qs.setCond(7);
				return "30375-07.htm";
			} else if (qs.getQuestItemsCount(ANDARIEL_BOOK) == 1) {
				return "30375-08.htm";
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

		if (npcId == DARK_SUCCUBUS) {
			if (cond > 0 && qs.getQuestItemsCount(BLANK_SHEET1) > 0) {
				qs.giveItems(BLOODY_RUNE1, 1);
				qs.takeItems(BLANK_SHEET1, 1);
				if (qs.getQuestItemsCount(BLANK_SHEET1) == 0) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(3);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		} else if (npcId == SHIELD_SKELETON || npcId == SKELETON_LORD || npcId == ZOMBIE_SOLDIER || npcId == ZOMBIE_WARRIOR) {
			if (cond > 0 && qs.getQuestItemsCount(PENITENTS_MARK) == 1 && qs.getQuestItemsCount(ASHEN_BONES) < 10) {
				qs.giveItems(ASHEN_BONES, 1);
				if (qs.getQuestItemsCount(ASHEN_BONES) == 10) {
					qs.playSound("ItemSound.quest_middle");
					qs.setCond(6);
				} else {
					qs.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return null;
	}
}
