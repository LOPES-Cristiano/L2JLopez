package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 77: Saga Of The Dominator
 * Transferencia para 3a Classe (ClassId 115 a partir de 51).
 */
@Component
public class Quest077SagaOfTheDominator extends SagaMasterQuest {

	public static final int QUEST_ID = 77;
	public static final String QUEST_NAME = "77_SagaOfTheDominator";

	private static final int[] NPC_IDS = {31336, 31624, 31371, 31290, 31636, 31646, 31648, 31653, 31654, 31655, 31656, 31290};
	private static final int[] ITEM_IDS = {7080, 7539, 7081, 7492, 7275, 7306, 7337, 7368, 7399, 7430, 7100, 0};
	private static final int[] MOB_IDS = {27294, 27226, 27262};
	private static final int[] X_COORDS = {162920, 47429, 47391};
	private static final int[] Y_COORDS = {-76504, -56923, -56929};
	private static final int[] Z_COORDS = {-3120, -2383, -2370};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest077SagaOfTheDominator(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Dominator", questManager,
				115, 51,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
