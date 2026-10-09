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
	public static final int PURE_UNICORN = 27077;
	public static final int MARSH_STAKATO = 20157;
	public static final int MARSH_SPIDER = 20233;

	// Itens
	public static final int MARK_OF_LIFE = 3140;
	public static final int CARDIENS_LETTER = 3141;
	public static final int CAMOMILE_CHARM = 3142;
	public static final int HIERARCHS_LETTER = 3143;
	public static final int MOONFLOWER_CHARM = 3144;
	public static final int GRAIL_DIAGRAM = 3145;
	public static final int PURE_MITHRIL_CUP = 3150;
	public static final int THALIAS_LETTER1 = 3146;
	public static final int THALIAS_LETTER2 = 3147;
	public static final int STARDUST = 3155;
	public static final int GRAIL_OF_PURITY = 3158;
	public static final int TEARS_OF_UNICORN = 3159;
	public static final int WATER_OF_LIFE = 3160;

	private static final Set<Integer> VALID_CLASSES = Set.of(19, 22, 26, 29);

	@Autowired
	public Quest218TestimonyOfLife(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Life");

		addStartNpc(CARDIEN);
		addTalkId(CARDIEN, ASTERIOS, THALIA, PUSHKIN, ARKENIA, ADONIUS, ISAEL);

		addKillId(PURE_UNICORN, MARSH_STAKATO, MARSH_SPIDER);

		registerQuestItems(CARDIENS_LETTER, CAMOMILE_CHARM, HIERARCHS_LETTER, MOONFLOWER_CHARM,
				GRAIL_DIAGRAM, PURE_MITHRIL_CUP, THALIAS_LETTER1, THALIAS_LETTER2, STARDUST,
				GRAIL_OF_PURITY, TEARS_OF_UNICORN, WATER_OF_LIFE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("1".equalsIgnoreCase(event) || "30460-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(CARDIENS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30460-04.htm";
		} else if ("30154_6".equalsIgnoreCase(event)) {
			qs.takeItems(CARDIENS_LETTER, -1);
			qs.giveItems(MOONFLOWER_CHARM, 1);
			qs.giveItems(HIERARCHS_LETTER, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30154-07.htm";
		} else if ("30371_2".equalsIgnoreCase(event)) {
			qs.takeItems(HIERARCHS_LETTER, -1);
			qs.giveItems(GRAIL_DIAGRAM, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30371-03.htm";
		} else if ("30300_6".equalsIgnoreCase(event)) {
			qs.takeItems(GRAIL_DIAGRAM, -1);
			qs.giveItems(PURE_MITHRIL_CUP, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30300-07.htm";
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
			} else if (cond == 22 && qs.hasQuestItems(CAMOMILE_CHARM)) {
				return "30460-06.htm";
			}
		} else if (npcId == ASTERIOS) {
			if (cond == 1) {
				return "30154-01.htm";
			} else if (cond == 21 && qs.hasQuestItems(WATER_OF_LIFE)) {
				qs.takeItems(WATER_OF_LIFE, -1);
				qs.takeItems(MOONFLOWER_CHARM, -1);
				qs.giveItems(CAMOMILE_CHARM, 1);
				qs.setCond(22);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30154-09.htm";
			}
		} else if (npcId == THALIA) {
			if (cond == 2) {
				return "30371-01.htm";
			} else if (cond == 5 && qs.hasQuestItems(PURE_MITHRIL_CUP)) {
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-04.htm";
			} else if (cond == 20 && qs.hasQuestItems(TEARS_OF_UNICORN)) {
				qs.takeItems(TEARS_OF_UNICORN, -1);
				qs.takeItems(GRAIL_OF_PURITY, -1);
				qs.giveItems(WATER_OF_LIFE, 1);
				qs.setCond(21);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30371-13.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (npc.getNpcId() == PURE_UNICORN && !qs.hasQuestItems(TEARS_OF_UNICORN)) {
			qs.giveItems(TEARS_OF_UNICORN, 1);
			qs.setCond(20);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
