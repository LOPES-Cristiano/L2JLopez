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
 * Quest 221: Testimony of Prosperity
 * 2º Passo de 2ª Classe para todas as classes da raça Dwarf (Bounty Hunter, Warsmith).
 */
@Component
public class Quest221TestimonyOfProsperity extends Quest {

	public static final int QUEST_ID = 221;
	public static final String QUEST_NAME = "221_TestimonyOfProsperity";

	// NPCs
	public static final int PARMAN = 30104;
	public static final int LOCKIRIN = 30531;
	public static final int NIKOLA = 30621;

	// Itens
	public static final int MARK_OF_PROSPERITY = 3238;
	public static final int RING_OF_TESTIMONY1 = 3239;
	public static final int RING_OF_TESTIMONY2 = 3240;
	public static final int OLD_ACCOUNT_BOOK = 3241;
	public static final int BLESSED_SEED = 3242;
	public static final int RECIPE_OF_EMILLY = 3243;
	public static final int LILITH_ELVEN_WAFER = 3244;
	public static final int MAPHR_TABLET_FRAGMENT = 3245;
	public static final int COLLECTION_LICENSE = 3246;
	public static final int PARMANS_LETTER = 3269;
	public static final int CLAY_DOUGH = 3270;
	public static final int PATTERN_OF_KEYHOLE = 3271;
	public static final int NIKOLAS_LIST = 3272;

	private static final Set<Integer> VALID_CLASSES = Set.of(54, 56);

	@Autowired
	public Quest221TestimonyOfProsperity(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Prosperity");

		addStartNpc(PARMAN);
		addTalkId(PARMAN, LOCKIRIN, NIKOLA);

		registerQuestItems(RING_OF_TESTIMONY1, RING_OF_TESTIMONY2, OLD_ACCOUNT_BOOK, BLESSED_SEED,
				RECIPE_OF_EMILLY, LILITH_ELVEN_WAFER, MAPHR_TABLET_FRAGMENT, COLLECTION_LICENSE,
				PARMANS_LETTER, CLAY_DOUGH, PATTERN_OF_KEYHOLE, NIKOLAS_LIST);

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

		if ("1".equalsIgnoreCase(event) || "30104-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(RING_OF_TESTIMONY1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30104-04.htm";
		} else if ("30104_1".equalsIgnoreCase(event)) {
			qs.takeItems(RING_OF_TESTIMONY1, -1);
			qs.giveItems(RING_OF_TESTIMONY2, 1);
			qs.giveItems(PARMANS_LETTER, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30104-08.htm";
		} else if ("30104-12.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MAPHR_TABLET_FRAGMENT)) {
				qs.takeItems(MAPHR_TABLET_FRAGMENT, -1);
				qs.takeItems(RING_OF_TESTIMONY2, -1);
				qs.giveItems(MARK_OF_PROSPERITY, 1);
				qs.addExpAndSp(113249, 7800);
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

		if (npcId == PARMAN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30104-03.htm";
				} else {
					return "30104-01.htm";
				}
			} else if (cond == 1) {
				return "30104-05.htm";
			} else if (cond == 2) {
				return "30104-06.htm";
			} else if (cond == 6 && qs.hasQuestItems(MAPHR_TABLET_FRAGMENT)) {
				return "30104-11.htm";
			}
		} else if (npcId == LOCKIRIN) {
			if (cond == 1) {
				return "30531-01.htm";
			}
		} else if (npcId == NIKOLA) {
			if (cond == 4) {
				qs.takeItems(PARMANS_LETTER, -1);
				qs.giveItems(CLAY_DOUGH, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30621-01.htm";
			} else if (cond == 5) {
				qs.takeItems(CLAY_DOUGH, -1);
				qs.giveItems(MAPHR_TABLET_FRAGMENT, 1);
				qs.setCond(6);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30621-04.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
