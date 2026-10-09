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
 * Quest 215: Trial of the Pilgrim
 * 1º Passo de 2ª Classe para Bishop, Prophet, Elven Elder, Shillien Elder, Overlord, Warcryer.
 */
@Component
public class Quest215TrialOfPilgrim extends Quest {

	public static final int QUEST_ID = 215;
	public static final String QUEST_NAME = "215_TrialOfPilgrim";

	// NPCs
	public static final int SANTIAGO = 30648;
	public static final int MARTANKUS = 30649;
	public static final int GERALD = 30650;
	public static final int DORF = 30651;
	public static final int CASIAN = 30612;
	public static final int TANAPI = 30571;
	public static final int PRAGA = 30333;
	public static final int VDINIA = 30109;
	public static final int GAURI = 30550;
	public static final int ATUBA = 30280;
	public static final int MOODUS = 30551;
	public static final int URUHA = 30652;
	public static final int PETRON = 30362;

	// Monstros
	public static final int LAVA_SALAMANDER = 27116;
	public static final int NAHIR = 27117;
	public static final int BLACK_WILLOW = 27118;

	// Itens
	public static final int MARK_OF_PILGRIM = 2721;
	public static final int BOOK_OF_SAGE = 2722;
	public static final int VOUCHER_OF_TRIAL = 2723;
	public static final int SPIRIT_OF_FLAME = 2724;
	public static final int ESSENSE_OF_FLAME = 2725;
	public static final int BOOK_OF_GERALD = 2726;
	public static final int GREY_BADGE = 2727;
	public static final int PICTURE_OF_NAHIR = 2728;
	public static final int HAIR_OF_NAHIR = 2729;
	public static final int STATUE_OF_EINHASAD = 2730;
	public static final int BOOK_OF_DARKNESS = 2731;
	public static final int DEBRIS_OF_WILLOW = 2732;
	public static final int TAG_OF_RUMOR = 2733;

	private static final Set<Integer> VALID_CLASSES = Set.of(15, 29, 42, 50); // Cleric, Elven Oracle, Shillien Oracle, Orc Shaman

	@Autowired
	public Quest215TrialOfPilgrim(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Pilgrim");

		addStartNpc(SANTIAGO);
		addTalkId(SANTIAGO, MARTANKUS, GERALD, DORF, CASIAN, TANAPI, PRAGA, VDINIA, GAURI, ATUBA, MOODUS, URUHA, PETRON);

		addKillId(LAVA_SALAMANDER, NAHIR, BLACK_WILLOW);

		registerQuestItems(BOOK_OF_SAGE, VOUCHER_OF_TRIAL, SPIRIT_OF_FLAME, ESSENSE_OF_FLAME,
				BOOK_OF_GERALD, GREY_BADGE, PICTURE_OF_NAHIR, HAIR_OF_NAHIR, STATUE_OF_EINHASAD,
				BOOK_OF_DARKNESS, DEBRIS_OF_WILLOW, TAG_OF_RUMOR);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30648-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VOUCHER_OF_TRIAL, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30648-04.htm";
		} else if ("30649_1".equalsIgnoreCase(event)) {
			qs.giveItems(SPIRIT_OF_FLAME, 1);
			qs.takeItems(ESSENSE_OF_FLAME, -1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30649-04.htm";
		} else if ("30650_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(57) >= 100000) {
				qs.giveItems(BOOK_OF_GERALD, 1);
				qs.takeItems(57, 100000);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30650-02.htm";
			} else {
				return "30650-03.htm";
			}
		} else if ("30362_1".equalsIgnoreCase(event) || "30362_2".equalsIgnoreCase(event)) {
			qs.takeItems(BOOK_OF_DARKNESS, -1);
			qs.setCond(16);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30362-05.htm";
		} else if ("30652_1".equalsIgnoreCase(event)) {
			qs.giveItems(BOOK_OF_DARKNESS, 1);
			qs.takeItems(DEBRIS_OF_WILLOW, -1);
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30652-02.htm";
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

		if (npcId == SANTIAGO) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 35 && VALID_CLASSES.contains(player.classId())) {
					return "30648-03.htm";
				} else {
					return "30648-01.htm";
				}
			} else if (cond == 1) {
				return "30648-05.htm";
			} else if (cond == 17 && qs.hasQuestItems(BOOK_OF_SAGE)) {
				qs.takeItems(BOOK_OF_SAGE, -1);
				qs.giveItems(MARK_OF_PILGRIM, 1);
				qs.addExpAndSp(77107, 11000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30648-09.htm";
			}
		} else if (npcId == TANAPI) {
			if (cond == 1) {
				qs.takeItems(VOUCHER_OF_TRIAL, -1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30571-01.htm";
			} else if (cond == 5 && qs.hasQuestItems(SPIRIT_OF_FLAME)) {
				qs.takeItems(SPIRIT_OF_FLAME, -1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30571-04.htm";
			}
		} else if (npcId == MARTANKUS) {
			if (cond == 2) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30649-01.htm";
			} else if (cond == 4 && qs.hasQuestItems(ESSENSE_OF_FLAME)) {
				return "30649-03.htm";
			}
		} else if (npcId == GERALD) {
			if (cond == 6) {
				return "30650-01.htm";
			} else if (cond == 8) {
				return "30650-04.htm";
			}
		} else if (npcId == DORF) {
			if (cond == 8 && qs.hasQuestItems(BOOK_OF_GERALD)) {
				qs.takeItems(BOOK_OF_GERALD, -1);
				qs.giveItems(GREY_BADGE, 1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30651-01.htm";
			}
		} else if (npcId == CASIAN) {
			if (cond == 16) {
				qs.giveItems(BOOK_OF_SAGE, 1);
				qs.setCond(17);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30612-01.htm";
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

		if (cond == 3 && npcId == LAVA_SALAMANDER && !qs.hasQuestItems(ESSENSE_OF_FLAME)) {
			qs.giveItems(ESSENSE_OF_FLAME, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if (cond == 10 && npcId == NAHIR && !qs.hasQuestItems(HAIR_OF_NAHIR)) {
			qs.giveItems(HAIR_OF_NAHIR, 1);
			qs.setCond(11);
			qs.playSound(QuestState.SOUND_MIDDLE);
		} else if (cond == 14 && npcId == BLACK_WILLOW && !qs.hasQuestItems(DEBRIS_OF_WILLOW)) {
			qs.giveItems(DEBRIS_OF_WILLOW, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
