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
 * Quest 219: Testimony of Fate
 * 2º Passo de 2ª Classe para todas as classes da raça Dark Elf (Palus Knight, Assassin, Dark Wizard, Shillien Oracle).
 */
@Component
public class Quest219TestimonyOfFate extends Quest {

	public static final int QUEST_ID = 219;
	public static final String QUEST_NAME = "219_TestimonyOfFate";

	// NPCs
	public static final int KAIRA = 30476;
	public static final int METHEUS = 30614;
	public static final int IXIA = 30463;
	public static final int ROA = 30114;
	public static final int THIFIEL = 30358;
	public static final int ARKENIA = 30419;

	// Monstros
	public static final int HANGMAN_TREE = 20144;
	public static final int MEDUSA = 20158;
	public static final int MARSH_STAKATO = 20157;
	public static final int DEAD_SEEKER = 20202;
	public static final int TYRANT = 20192;

	// Itens
	public static final int MARK_OF_FATE = 3172;
	public static final int KAIRAS_LETTER1 = 3173;
	public static final int METHEUS_FUNERAL_JAR = 3174;
	public static final int KASANDRAS_REMAINS = 3175;
	public static final int HERBALISM_TEXTBOOK = 3176;
	public static final int IXIAS_LIST = 3177;
	public static final int BELLADONNA = 3183;
	public static final int ALDERS_SKULL1 = 3184;
	public static final int ALDERS_SKULL2 = 3185;
	public static final int ALDERS_RECEIPT = 3186;
	public static final int REVELATIONS_MANUSCRIPT = 3187;
	public static final int KAIRAS_RECOMMEND = 3189;
	public static final int PALUS_CHARM = 3190;
	public static final int THIFIELS_LETTER = 3191;
	public static final int ARKENIAS_LETTER = 1246;

	private static final Set<Integer> VALID_CLASSES = Set.of(32, 35, 39, 42);

	@Autowired
	public Quest219TestimonyOfFate(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Fate");

		addStartNpc(KAIRA);
		addTalkId(KAIRA, METHEUS, IXIA, ROA, THIFIEL, ARKENIA);

		addKillId(HANGMAN_TREE, MEDUSA, MARSH_STAKATO, DEAD_SEEKER, TYRANT);

		registerQuestItems(KAIRAS_LETTER1, METHEUS_FUNERAL_JAR, KASANDRAS_REMAINS, HERBALISM_TEXTBOOK,
				IXIAS_LIST, BELLADONNA, ALDERS_SKULL1, ALDERS_SKULL2, ALDERS_RECEIPT,
				REVELATIONS_MANUSCRIPT, KAIRAS_RECOMMEND, PALUS_CHARM, THIFIELS_LETTER, ARKENIAS_LETTER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30476-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(KAIRAS_LETTER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30476-05.htm";
		} else if ("30614-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(KAIRAS_LETTER1, -1);
			qs.giveItems(METHEUS_FUNERAL_JAR, 1);
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30476_2".equalsIgnoreCase(event)) {
			qs.takeItems(REVELATIONS_MANUSCRIPT, -1);
			qs.giveItems(KAIRAS_RECOMMEND, 1);
			qs.setCond(15);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30476-12.htm";
		} else if ("30358-04.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ARKENIAS_LETTER)) {
				qs.takeItems(ARKENIAS_LETTER, -1);
				qs.takeItems(PALUS_CHARM, -1);
				qs.giveItems(MARK_OF_FATE, 1);
				qs.addExpAndSp(115000, 12000);
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

		if (npcId == KAIRA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30476-03.htm";
				} else {
					return "30476-02.htm";
				}
			} else if (cond == 1) {
				return "30476-06.htm";
			} else if (cond == 13 && qs.hasQuestItems(REVELATIONS_MANUSCRIPT)) {
				return "30476-11.htm";
			}
		} else if (npcId == METHEUS) {
			if (cond == 1) {
				return "30614-01.htm";
			} else if (cond == 3 && qs.hasQuestItems(KASANDRAS_REMAINS)) {
				qs.takeItems(KASANDRAS_REMAINS, -1);
				qs.giveItems(HERBALISM_TEXTBOOK, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30614-03.htm";
			}
		} else if (npcId == THIFIEL) {
			if (cond == 15 && qs.hasQuestItems(KAIRAS_RECOMMEND)) {
				qs.takeItems(KAIRAS_RECOMMEND, -1);
				qs.giveItems(PALUS_CHARM, 1);
				qs.giveItems(THIFIELS_LETTER, 1);
				qs.setCond(16);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30358-01.htm";
			} else if (cond == 18 && qs.hasQuestItems(ARKENIAS_LETTER)) {
				return "30358-03.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		if (qs.getCond() == 2 && npc.getNpcId() == HANGMAN_TREE && !qs.hasQuestItems(KASANDRAS_REMAINS)) {
			qs.takeItems(METHEUS_FUNERAL_JAR, -1);
			qs.giveItems(KASANDRAS_REMAINS, 1);
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}
