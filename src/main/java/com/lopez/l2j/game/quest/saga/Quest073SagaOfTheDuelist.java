package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 73: Saga Of The Duelist
 * Transferencia para 3a Classe (ClassId 88 a partir de 2).
 */
@Component
public class Quest073SagaOfTheDuelist extends SagaMasterQuest {

	public static final int QUEST_ID = 73;
	public static final String QUEST_NAME = "73_SagaOfTheDuelist";

	private static final int[] NPC_IDS = {30849, 31624, 31226, 31331, 31639, 31646, 31647, 31653, 31654, 31655, 31656, 31277, 31537};
	private static final int[] ITEM_IDS = {7080, 7537, 7081, 7488, 7271, 7302, 7333, 7364, 7395, 7426, 7096, 7546};
	private static final int[] MOB_IDS = {27289, 27222, 27281};
	private static final int[] X_COORDS = {162920, 47429, 47391};
	private static final int[] Y_COORDS = {-76504, -56923, -56929};
	private static final int[] Z_COORDS = {-3120, -2383, -2370};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest073SagaOfTheDuelist(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Duelist", questManager,
				88, 2,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
