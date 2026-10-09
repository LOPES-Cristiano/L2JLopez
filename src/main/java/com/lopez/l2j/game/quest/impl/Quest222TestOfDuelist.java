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
 * Quest 222: Test of the Duelist
 * 3º Passo de 2ª Classe para Gladiator.
 */
@Component
public class Quest222TestOfDuelist extends Quest {

	public static final int QUEST_ID = 222;
	public static final String QUEST_NAME = "222_TestOfDuelist";

	// NPCs
	public static final int KAIYAN = 30623;

	// Monstros
	public static final int PUNCHER = 20085;
	public static final int NOBLE_ANT = 20090;
	public static final int MARSH_STAKATO_DRONE = 20234;
	public static final int DEAD_SEEKER = 20202;
	public static final int BREKA_OVERLORD = 20270;
	public static final int FETTERED_SOUL = 20552;
	public static final int LETO_OVERLORD = 20582;
	public static final int ENCHANTED_MONSTER_EYE = 20564;
	public static final int TAMLIN_ORC = 20601;
	public static final int TAMLIN_ORC_ARCHER = 20602;

	// Monstros Finais
	public static final int EXCURO = 20214;
	public static final int KRATOR = 20217;
	public static final int GRANDIS = 20554;
	public static final int TIMAK_ORC_OVERLORD = 20588;
	public static final int LAKIN = 20604;

	// Itens
	public static final int ORDER_GLUDIO = 2763;
	public static final int ORDER_DION = 2764;
	public static final int ORDER_GIRAN = 2765;
	public static final int ORDER_OREN = 2766;
	public static final int ORDER_ADEN = 2767;
	public static final int FINAL_ORDER = 2778;
	public static final int MARK_OF_DUELIST = 2762;

	private static final Set<Integer> VALID_CLASSES = Set.of(1, 19, 32, 45, 47);

	@Autowired
	public Quest222TestOfDuelist(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Duelist");

		addStartNpc(KAIYAN);
		addTalkId(KAIYAN);

		addKillId(PUNCHER, NOBLE_ANT, MARSH_STAKATO_DRONE, DEAD_SEEKER, BREKA_OVERLORD,
				FETTERED_SOUL, LETO_OVERLORD, ENCHANTED_MONSTER_EYE, TAMLIN_ORC, TAMLIN_ORC_ARCHER,
				EXCURO, KRATOR, GRANDIS, TIMAK_ORC_OVERLORD, LAKIN);

		registerQuestItems(ORDER_GLUDIO, ORDER_DION, ORDER_GIRAN, ORDER_OREN, ORDER_ADEN, FINAL_ORDER);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30623-07.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.setState(State.STARTED);
			qs.giveItems(ORDER_GLUDIO, 1);
			qs.giveItems(ORDER_DION, 1);
			qs.giveItems(ORDER_GIRAN, 1);
			qs.giveItems(ORDER_OREN, 1);
			qs.giveItems(ORDER_ADEN, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30623-16.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ORDER_GLUDIO, -1);
			qs.takeItems(ORDER_DION, -1);
			qs.takeItems(ORDER_GIRAN, -1);
			qs.takeItems(ORDER_OREN, -1);
			qs.takeItems(ORDER_ADEN, -1);
			qs.giveItems(FINAL_ORDER, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30623-19.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(FINAL_ORDER)) {
				qs.takeItems(FINAL_ORDER, -1);
				qs.giveItems(MARK_OF_DUELIST, 1);
				qs.addExpAndSp(47215, 4000);
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
		int cond = qs.getCond();

		if (cond == 0) {
			if (qs.isCompleted()) {
				return "<html><body>This quest has already been completed.</body></html>";
			} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
				return "30623-03.htm";
			} else {
				return "30623-01.htm";
			}
		} else if (cond == 2) {
			return "30623-14.htm";
		} else if (cond == 3) {
			return "30623-15.htm";
		} else if (cond == 4) {
			return "30623-17.htm";
		} else if (cond == 5) {
			return "30623-18.htm";
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
