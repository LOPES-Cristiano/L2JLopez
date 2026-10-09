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
 * Quest 225: Test of the Searcher
 * 3º Passo de 2ª Classe para Treasure Hunter, Plains Walker, Abyss Walker, Bounty Hunter.
 */
@Component
public class Quest225TestOfSearcher extends Quest {

	public static final int QUEST_ID = 225;
	public static final String QUEST_NAME = "225_TestOfSearcher";

	// NPCs
	public static final int LUTHER = 30690;
	public static final int ALEX = 30291;
	public static final int LEIRYNN = 30728;
	public static final int BORYA = 30729;
	public static final int JAX = 30730;
	public static final int TREE = 30627;
	public static final int CHEST = 30628;

	// Monstros
	public static final int DELU_LIZARDMAN = 20781;
	public static final int CHIEF_KALKIS = 27093;
	public static final int GIANT_FUNGUS = 20555;
	public static final int ROAD_RATMAN = 20551;
	public static final int HANGMAN_TREE = 20144;

	// Itens
	public static final int MARK_OF_SEARCHER = 2809;
	public static final int LUTHERS_LETTER = 2784;
	public static final int ALANKELLS_WARRANT = 2785;
	public static final int LEIRYNNS_ORDER1 = 2786;
	public static final int DELU_TOTEM = 2787;
	public static final int LEIRYNNS_ORDER2 = 2788;
	public static final int CHIEF_KALKIS_FANG = 2789;
	public static final int LEIRYNNS_REPORT = 2790;
	public static final int STRANGE_MAP = 2791;
	public static final int LAMBERTS_MAP = 2792;
	public static final int ALANKELLS_LETTER = 2793;
	public static final int ALANKELLS_ORDER = 2794;
	public static final int GOLD_BAR = 2807;
	public static final int ALANKELLS_RECOMMEND = 2808;

	private static final Set<Integer> VALID_CLASSES = Set.of(7, 22, 35, 54);

	@Autowired
	public Quest225TestOfSearcher(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Searcher");

		addStartNpc(LUTHER);
		addTalkId(LUTHER, ALEX, LEIRYNN, BORYA, JAX, TREE, CHEST);

		addKillId(DELU_LIZARDMAN, CHIEF_KALKIS, GIANT_FUNGUS, ROAD_RATMAN, HANGMAN_TREE);

		registerQuestItems(LUTHERS_LETTER, ALANKELLS_WARRANT, LEIRYNNS_ORDER1, DELU_TOTEM,
				LEIRYNNS_ORDER2, CHIEF_KALKIS_FANG, LEIRYNNS_REPORT, STRANGE_MAP, LAMBERTS_MAP,
				ALANKELLS_LETTER, ALANKELLS_ORDER, GOLD_BAR, ALANKELLS_RECOMMEND);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30690-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(LUTHERS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30291-07.htm".equalsIgnoreCase(event)) {
			qs.giveItems(ALANKELLS_LETTER, 1);
			qs.giveItems(ALANKELLS_ORDER, 1);
			qs.giveItems(LAMBERTS_MAP, 1);
			qs.takeItems(STRANGE_MAP, -1);
			qs.takeItems(LEIRYNNS_REPORT, -1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30690-07.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(ALANKELLS_RECOMMEND)) {
				qs.takeItems(ALANKELLS_RECOMMEND, -1);
				qs.giveItems(MARK_OF_SEARCHER, 1);
				qs.addExpAndSp(49000, 5700);
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

		if (npcId == LUTHER) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30690-03.htm";
				} else {
					return "30690-01.htm";
				}
			} else if (cond == 1) {
				return "30690-06.htm";
			} else if (cond == 19 && qs.hasQuestItems(ALANKELLS_RECOMMEND)) {
				return "30690-07.htm";
			}
		} else if (npcId == ALEX) {
			if (cond == 1) {
				qs.takeItems(LUTHERS_LETTER, -1);
				qs.giveItems(ALANKELLS_WARRANT, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30291-01.htm";
			} else if (cond == 18 && qs.getQuestItemsCount(GOLD_BAR) >= 20) {
				qs.takeItems(GOLD_BAR, -1);
				qs.giveItems(ALANKELLS_RECOMMEND, 1);
				qs.setCond(19);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30291-11.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
