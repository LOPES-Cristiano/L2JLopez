package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 218: Testimony of Life
 * 2º Passo de 2ª Classe para todas as classes da raça Elf (Elven Knight, Scout, Wizard, Oracle).
 */
@Component
public class Quest218TestimonyOfLife extends Quest {

	public static final int QUEST_ID = 218;
	public static final String QUEST_NAME = "218_TestimonyOfLife";

	// NPCs
	public static final int CARDIEN = 30460;
	public static final int ASTERIOS = 30154;
	public static final int THALIA = 30371;
	public static final int PUSHKIN = 30300;
	public static final int ARKENIA = 30419;
	public static final int ADONIUS = 30375;
	public static final int ISAEL = 30655;

	// Monstros
	public static final int HARPY = 20145;
	public static final int MARSH_STAKATO = 20157;
	public static final int WYRM = 20176;
	public static final int MARSH_SPIDER = 20233;
	public static final int GUARDIAN_BASILISK = 20550;
	public static final int LETO_LIZARDMAN_SHAMAN = 20581;
	public static final int LETO_LIZARDMAN_OVERLORD = 20582;
	public static final int ANT_RECRUIT = 20082;
	public static final int ANT_PATROL = 20084;
	public static final int ANT_GUARD = 20086;
	public static final int ANT_SOLDIER = 20087;
	public static final int ANT_WARRIOR_CAPTAIN = 20088;
	public static final int PURE_UNICORN = 27077;

	// Itens
	public static final int TALINS_SPEAR = 3026;
	public static final int MARK_OF_LIFE = 3140;
	public static final int CARDIENS_LETTER = 3141;
	public static final int CAMOMILE_CHARM = 3142;
	public static final int HIERARCHS_LETTER = 3143;
	public static final int MOONFLOWER_CHARM = 3144;
	public static final int GRAIL_DIAGRAM = 3145;
	public static final int THALIAS_LETTER1 = 3146;
	public static final int THALIAS_LETTER2 = 3147;
	public static final int THALIAS_INSTRUCTIONS = 3148;
	public static final int PUSHKINS_LIST = 3149;
	public static final int PURE_MITHRIL_CUP = 3150;
	public static final int ARKENIAS_CONTRACT = 3151;
	public static final int ARKENIAS_INSTRUCTIONS = 3152;
	public static final int ADONIUS_LIST = 3153;
	public static final int ANDARIEL_SCRIPTURE_COPY = 3154;
	public static final int STARDUST = 3155;
	public static final int ISAELS_INSTRUCTIONS = 3156;
	public static final int ISAELS_LETTER = 3157;
	public static final int GRAIL_OF_PURITY = 3158;
	public static final int TEARS_OF_UNICORN = 3159;
	public static final int WATER_OF_LIFE = 3160;
	public static final int PURE_MITHRIL_ORE = 3161;
	public static final int ANT_SOLDIER_ACID = 3162;
	public static final int WYRMS_TALON1 = 3163;
	public static final int SPIDER_ICHOR = 3164;
	public static final int HARPYS_DOWN = 3165;
	public static final int[] TALINS_PIECES = { 3166, 3167, 3168, 3169, 3170, 3171 };

	private static final Set<Integer> VALID_CLASSES = Set.of(19, 22, 26, 29);

	@Autowired
	public Quest218TestimonyOfLife(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Life");

		addStartNpc(CARDIEN);
		addTalkId(CARDIEN, ASTERIOS, THALIA, PUSHKIN, ARKENIA, ADONIUS, ISAEL);

		addKillId(HARPY, MARSH_STAKATO, WYRM, MARSH_SPIDER, GUARDIAN_BASILISK, LETO_LIZARDMAN_SHAMAN,
				LETO_LIZARDMAN_OVERLORD, ANT_RECRUIT, ANT_PATROL, ANT_GUARD, ANT_SOLDIER, ANT_WARRIOR_CAPTAIN, PURE_UNICORN);

		registerQuestItems(TALINS_SPEAR, CARDIENS_LETTER, CAMOMILE_CHARM, HIERARCHS_LETTER, MOONFLOWER_CHARM,
				GRAIL_DIAGRAM, THALIAS_LETTER1, THALIAS_LETTER2, THALIAS_INSTRUCTIONS, PUSHKINS_LIST,
				PURE_MITHRIL_CUP, ARKENIAS_CONTRACT, ARKENIAS_INSTRUCTIONS, ADONIUS_LIST, ANDARIEL_SCRIPTURE_COPY,
				STARDUST, ISAELS_INSTRUCTIONS, ISAELS_LETTER, GRAIL_OF_PURITY, TEARS_OF_UNICORN, WATER_OF_LIFE,
				PURE_MITHRIL_ORE, ANT_SOLDIER_ACID, WYRMS_TALON1, SPIDER_ICHOR, HARPYS_DOWN,
				3166, 3167, 3168, 3169, 3170, 3171);

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

		if ("1".equalsIgnoreCase(event) || "30460-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(CARDIENS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30460-04.htm";
		} else if ("30154_6".equalsIgnoreCase(event) || "30154-07.htm".equalsIgnoreCase(event)) {
			qs.takeItems(CARDIENS_LETTER, -1);
			qs.giveItems(MOONFLOWER_CHARM, 1);
			qs.giveItems(HIERARCHS_LETTER, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30154-07.htm";
		} else if ("30371_2".equalsIgnoreCase(event) || "30371-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(HIERARCHS_LETTER, -1);
			qs.giveItems(GRAIL_DIAGRAM, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30371-03.htm";
		} else if ("30371_3".equalsIgnoreCase(event) || "30371-11.htm".equalsIgnoreCase(event)) {
			qs.takeItems(STARDUST, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			if (player != null && player.level() < 38) {
				qs.giveItems(THALIAS_INSTRUCTIONS, 1);
				qs.setCond(13);
				return "30371-10.htm";
			}
			qs.giveItems(THALIAS_LETTER2, 1);
			qs.setCond(14);
			return "30371-11.htm";
		} else if ("30300_6".equalsIgnoreCase(event)) {
			qs.takeItems(GRAIL_DIAGRAM, -1);
			qs.giveItems(PURE_MITHRIL_CUP, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30300-07.htm";
		} else if ("30300-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(GRAIL_DIAGRAM, -1);
			qs.giveItems(PUSHKINS_LIST, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30300-06.htm";
		} else if ("30300_7".equalsIgnoreCase(event) || "30300-10.htm".equalsIgnoreCase(event)) {
			qs.takeItems(PUSHKINS_LIST, -1);
			qs.takeItems(ANT_SOLDIER_ACID, -1);
			qs.takeItems(PURE_MITHRIL_ORE, -1);
			qs.takeItems(WYRMS_TALON1, -1);
			qs.giveItems(PURE_MITHRIL_CUP, 1);
			qs.setCond(6);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30300-10.htm";
		} else if ("30419_3".equalsIgnoreCase(event) || "30419-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(THALIAS_LETTER1, -1);
			qs.giveItems(ARKENIAS_CONTRACT, 1);
			qs.giveItems(ARKENIAS_INSTRUCTIONS, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30419-04.htm";
		} else if ("30375_1".equalsIgnoreCase(event) || "30375-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ARKENIAS_INSTRUCTIONS, -1);
			qs.giveItems(ADONIUS_LIST, 1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30375-02.htm";
		} else if ("30655_1".equalsIgnoreCase(event) || "30655-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(THALIAS_LETTER2, -1);
			qs.giveItems(ISAELS_INSTRUCTIONS, 1);
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30655-02.htm";
		} else if ("30460-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(CAMOMILE_CHARM)) {
				qs.takeItems(CAMOMILE_CHARM, -1);
				qs.giveItems(MARK_OF_LIFE, 1);
				qs.addExpAndSp(104591, 11250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30460-07.htm";
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

		if (npcId == CARDIEN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30460-03.htm";
				} else {
					return "30460-02.htm";
				}
			} else if (cond == 1) {
				return "30460-05.htm";
			} else if (cond == 21 && qs.hasQuestItems(CAMOMILE_CHARM)) {
				qs.takeItems(CAMOMILE_CHARM, -1);
				qs.giveItems(MARK_OF_LIFE, 1);
				qs.addExpAndSp(104591, 11250);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30460-07.htm";
			} else if (cond == 22 && qs.hasQuestItems(CAMOMILE_CHARM)) {
				return "30460-06.htm";
			} else if (cond > 1) {
				return "30460-06.htm";
			}
		} else if (npcId == ASTERIOS) {
			if (cond == 1) {
				return "30154-01.htm";
			} else if (cond == 2) {
				return "30154-08.htm";
			} else if ((cond == 20 || cond == 21) && qs.hasQuestItems(WATER_OF_LIFE)) {
				qs.takeItems(WATER_OF_LIFE, -1);
				qs.takeItems(MOONFLOWER_CHARM, -1);
				qs.giveItems(CAMOMILE_CHARM, 1);
				qs.setCond(cond == 20 ? 21 : 22);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30154-09.htm";
			} else if (cond >= 21) {
				return "30154-10.htm";
			}
		} else if (npcId == PUSHKIN) {
			if (cond == 3) {
				return "30300-01.htm";
			} else if (cond == 4) {
				return "30300-07.htm";
			} else if (cond == 5) {
				return "30300-08.htm";
			} else if (cond == 6) {
				return "30300-11.htm";
			} else if (cond > 6) {
				return "30300-12.htm";
			}
		} else if (npcId == THALIA) {
			if (cond == 2) {
				return "30371-01.htm";
			} else if (cond == 3) {
				return "30371-04.htm";
			} else if (cond == 4) {
				return "30371-05.htm";
			} else if (cond == 5 && qs.hasQuestItems(PURE_MITHRIL_CUP)) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-04.htm";
			} else if (cond == 6) {
				qs.takeItems(PURE_MITHRIL_CUP, -1);
				qs.giveItems(THALIAS_LETTER1, 1);
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-06.htm";
			} else if (cond == 7) {
				return "30371-07.htm";
			} else if (cond > 7 && cond < 12) {
				return "30371-08.htm";
			} else if (cond == 12) {
				return "30371-09.htm";
			} else if (cond == 13) {
				if (player != null && player.level() < 38) {
					return "30371-12.htm";
				}
				qs.takeItems(THALIAS_INSTRUCTIONS, -1);
				qs.giveItems(THALIAS_LETTER2, 1);
				qs.setCond(14);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-13.htm";
			} else if (cond == 14) {
				return "30371-14.htm";
			} else if (cond == 15 || cond == 16) {
				return "30371-15.htm";
			} else if (cond == 17) {
				qs.takeItems(ISAELS_LETTER, -1);
				qs.giveItems(GRAIL_OF_PURITY, 1);
				qs.setCond(18);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-16.htm";
			} else if (cond == 18) {
				return "30371-17.htm";
			} else if ((cond == 19 || cond == 20) && qs.hasQuestItems(TEARS_OF_UNICORN)) {
				qs.takeItems(TEARS_OF_UNICORN, -1);
				qs.takeItems(GRAIL_OF_PURITY, -1);
				qs.giveItems(WATER_OF_LIFE, 1);
				qs.setCond(cond == 19 ? 20 : 21);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return cond == 19 ? "30371-18.htm" : "30371-13.htm";
			} else if (cond > 19) {
				return "30371-19.htm";
			}
		} else if (npcId == ADONIUS) {
			if (cond == 8) {
				return "30375-01.htm";
			} else if (cond == 9) {
				return "30375-03.htm";
			} else if (cond == 10) {
				qs.takeItems(ADONIUS_LIST, -1);
				qs.takeItems(HARPYS_DOWN, -1);
				qs.takeItems(SPIDER_ICHOR, -1);
				qs.giveItems(ANDARIEL_SCRIPTURE_COPY, 1);
				qs.setCond(11);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30375-04.htm";
			} else if (cond == 11) {
				return "30375-05.htm";
			} else if (cond > 11) {
				return "30375-06.htm";
			}
		} else if (npcId == ARKENIA) {
			if (cond == 7) {
				return "30419-01.htm";
			} else if (cond > 7 && cond < 11) {
				return "30419-05.htm";
			} else if (cond == 11) {
				qs.takeItems(ANDARIEL_SCRIPTURE_COPY, -1);
				qs.takeItems(ARKENIAS_CONTRACT, -1);
				qs.giveItems(STARDUST, 1);
				qs.setCond(12);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30419-06.htm";
			} else if (cond == 12) {
				return "30419-07.htm";
			} else if (cond > 12) {
				return "30419-08.htm";
			}
		} else if (npcId == ISAEL) {
			if (cond == 14) {
				return "30655-01.htm";
			} else if (cond == 15) {
				return "30655-03.htm";
			} else if (cond == 16) {
				boolean hasAll = true;
				for (int itemId : TALINS_PIECES) {
					if (!qs.hasQuestItems(itemId)) {
						hasAll = false;
						break;
					}
				}
				if (hasAll) {
					for (int itemId : TALINS_PIECES) {
						qs.takeItems(itemId, -1);
					}
					qs.takeItems(ISAELS_INSTRUCTIONS, -1);
					qs.giveItems(ISAELS_LETTER, 1);
					qs.giveItems(TALINS_SPEAR, 1);
					qs.setCond(17);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30655-04.htm";
				}
				return "30655-03.htm";
			} else if (cond == 17) {
				return "30655-05.htm";
			} else if (cond > 17) {
				return "30655-06.htm";
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

		if (cond == 4) {
			if (npcId == GUARDIAN_BASILISK && qs.getQuestItemsCount(PURE_MITHRIL_ORE) < 10) {
				qs.giveItems(PURE_MITHRIL_ORE, 1);
			} else if (npcId == WYRM && qs.getQuestItemsCount(WYRMS_TALON1) < 20) {
				qs.giveItems(WYRMS_TALON1, 1);
			} else if ((npcId == ANT_RECRUIT || npcId == ANT_PATROL || npcId == ANT_GUARD || npcId == ANT_SOLDIER
					|| npcId == ANT_WARRIOR_CAPTAIN) && qs.getQuestItemsCount(ANT_SOLDIER_ACID) < 20) {
				qs.giveItems(ANT_SOLDIER_ACID, 1);
			}

			if (qs.getQuestItemsCount(PURE_MITHRIL_ORE) >= 10
					&& qs.getQuestItemsCount(WYRMS_TALON1) >= 20
					&& qs.getQuestItemsCount(ANT_SOLDIER_ACID) >= 20) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 9) {
			if (npcId == MARSH_SPIDER && qs.getQuestItemsCount(SPIDER_ICHOR) < 20) {
				qs.giveItems(SPIDER_ICHOR, 1);
			} else if (npcId == HARPY && qs.getQuestItemsCount(HARPYS_DOWN) < 20) {
				qs.giveItems(HARPYS_DOWN, 1);
			}

			if (qs.getQuestItemsCount(SPIDER_ICHOR) >= 20 && qs.getQuestItemsCount(HARPYS_DOWN) >= 20) {
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (cond == 15 && (npcId == LETO_LIZARDMAN_SHAMAN || npcId == LETO_LIZARDMAN_OVERLORD)) {
			for (int itemId : TALINS_PIECES) {
				if (!qs.hasQuestItems(itemId)) {
					qs.giveItems(itemId, 1);
					break;
				}
			}
			boolean hasAll = true;
			for (int itemId : TALINS_PIECES) {
				if (!qs.hasQuestItems(itemId)) {
					hasAll = false;
					break;
				}
			}
			if (hasAll) {
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == PURE_UNICORN && !qs.hasQuestItems(TEARS_OF_UNICORN)) {
			qs.takeItems(GRAIL_OF_PURITY, -1);
			qs.takeItems(TALINS_SPEAR, -1);
			qs.giveItems(TEARS_OF_UNICORN, 1);
			qs.setCond(cond == 18 ? 19 : 20);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
