package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 75: Saga Of The Titan
 * Transferencia para 3a Classe (ClassId 113 a partir de 46).
 */
@Component
public class Quest075SagaOfTheTitan extends SagaMasterQuest {

	public static final int QUEST_ID = 75;
	public static final String QUEST_NAME = "75_SagaOfTheTitan";

	private static final int[] NPC_IDS = {31327, 31624, 31289, 31290, 31607, 31646, 31649, 31651, 31654, 31655, 31658, 31290};
	private static final int[] ITEM_IDS = {7080, 7539, 7081, 7490, 7273, 7304, 7335, 7366, 7397, 7428, 7098, 0};
	private static final int[] MOB_IDS = {27292, 27224, 27283};
	private static final int[] X_COORDS = {119518, 181215, 181227};
	private static final int[] Y_COORDS = {-28658, 36676, 36703};
	private static final int[] Z_COORDS = {-3811, -4812, -4816};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest075SagaOfTheTitan(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Titan", questManager,
				113, 46,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
