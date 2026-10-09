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
 * Quest 226: Test of the Healer
 * 3º Passo de 2ª Classe para Bishop, Elven Elder, Shillien Elder.
 */
@Component
public class Quest226TestOfHealer extends Quest {

	public static final int QUEST_ID = 226;
	public static final String QUEST_NAME = "226_TestOfHealer";

	// NPCs
	public static final int BANDELLOS = 30473;
	public static final int PERRIN = 30428;
	public static final int ALLANA = 30424;
	public static final int GUPU = 30658;
	public static final int WINDY = 30660;
	public static final int SORIUS = 30327;
	public static final int KRISTINA = 30665;

	// Monstros
	public static final int TATOMA = 27134;

	// Itens
	public static final int REPORT_OF_PERRIN = 2810;
	public static final int CRISTINAS_LETTER = 2811;
	public static final int PICTURE_OF_WINDY = 2812;
	public static final int GOLDEN_STATUE = 2813;
	public static final int WINDYS_PEBBLES = 2814;
	public static final int ORDER_OF_SORIUS = 2815;
	public static final int MARK_OF_HEALER = 2820;

	private static final Set<Integer> VALID_CLASSES = Set.of(4, 15, 19, 29, 42);

	@Autowired
	public Quest226TestOfHealer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Healer");

		addStartNpc(BANDELLOS);
		addTalkId(BANDELLOS, PERRIN, ALLANA, GUPU, WINDY, SORIUS, KRISTINA);

		addKillId(TATOMA);

		registerQuestItems(REPORT_OF_PERRIN, CRISTINAS_LETTER, PICTURE_OF_WINDY, GOLDEN_STATUE,
				WINDYS_PEBBLES, ORDER_OF_SORIUS);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30473-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(REPORT_OF_PERRIN, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30473-04.htm";
		} else if ("30473_2".equalsIgnoreCase(event)) {
			qs.giveItems(MARK_OF_HEALER, 1);
			qs.addExpAndSp(118304, 26250);
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitCurrentQuest(false);
			return "30473-09.htm";
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

		if (npcId == BANDELLOS) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30473-03.htm";
				} else {
					return "30473-01.htm";
				}
			} else if (cond == 1) {
				return "30473-05.htm";
			} else if (cond >= 20) {
				return "30473-08.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
