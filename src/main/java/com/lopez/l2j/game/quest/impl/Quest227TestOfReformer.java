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
 * Quest 227: Test of the Reformer
 * 3º Passo de 2ª Classe para Prophet, Shillien Elder.
 */
@Component
public class Quest227TestOfReformer extends Quest {

	public static final int QUEST_ID = 227;
	public static final String QUEST_NAME = "227_TestOfReformer";

	// NPCs
	public static final int PUPINA = 30118;
	public static final int SLA = 30666;
	public static final int KATARI = 30668;
	public static final int KAKAN = 30669;
	public static final int NYAKURI = 30670;
	public static final int RAMUS = 30667;

	// Itens
	public static final int MARK_OF_REFORMER = 2821;
	public static final int BOOK_OF_REFORM = 2822;
	public static final int LETTER_OF_INTRODUCTION = 2823;
	public static final int SLAS_LETTER = 2824;
	public static final int GREETINGS = 2825;
	public static final int OLMAHUMS_MONEY = 2826;
	public static final int KATARIS_LETTER = 2827;
	public static final int NYAKURIS_LETTER = 2828;
	public static final int UNDEAD_LIST = 2829;
	public static final int RAMUSS_LETTER = 2830;
	public static final int KAKANS_LETTER = 3037;

	private static final Set<Integer> VALID_CLASSES = Set.of(15, 42);

	@Autowired
	public Quest227TestOfReformer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Reformer");

		addStartNpc(PUPINA);
		addTalkId(PUPINA, SLA, KATARI, KAKAN, NYAKURI, RAMUS);

		registerQuestItems(BOOK_OF_REFORM, LETTER_OF_INTRODUCTION, SLAS_LETTER, GREETINGS,
				OLMAHUMS_MONEY, KATARIS_LETTER, NYAKURIS_LETTER, UNDEAD_LIST, RAMUSS_LETTER, KAKANS_LETTER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30118-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(BOOK_OF_REFORM, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30118-04.htm";
		} else if ("30118_1".equalsIgnoreCase(event)) {
			qs.takeItems(BOOK_OF_REFORM, -1);
			qs.giveItems(LETTER_OF_INTRODUCTION, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30118-06.htm";
		} else if ("30666_3".equalsIgnoreCase(event)) {
			qs.takeItems(LETTER_OF_INTRODUCTION, -1);
			qs.giveItems(SLAS_LETTER, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30666-04.htm";
		} else if ("30666-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RAMUSS_LETTER)) {
				qs.takeItems(RAMUSS_LETTER, -1);
				qs.giveItems(MARK_OF_REFORMER, 1);
				qs.addExpAndSp(164032, 17500);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
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

		if (npcId == PUPINA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30118-03.htm";
				} else {
					return "30118-01.htm";
				}
			} else if (cond == 1) {
				return "30118-05.htm";
			}
		} else if (npcId == SLA) {
			if (cond == 4) {
				return "30666-01.htm";
			} else if (cond >= 19 && qs.hasQuestItems(RAMUSS_LETTER)) {
				return "30666-06.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
