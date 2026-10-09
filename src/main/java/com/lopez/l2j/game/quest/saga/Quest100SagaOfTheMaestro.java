package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 100: Saga Of The Maestro
 * Transferencia para 3a Classe (ClassId 118 a partir de 57).
 */
@Component
public class Quest100SagaOfTheMaestro extends SagaMasterQuest {

	public static final int QUEST_ID = 100;
	public static final String QUEST_NAME = "100_SagaOfTheMaestro";

	private static final int[] NPC_IDS = {31592, 31273, 31597, 31597, 31596, 31646, 31648, 31653, 31654, 31655, 31656, 31597};
	private static final int[] ITEM_IDS = {7080, 7607, 7081, 7515, 7298, 7329, 7360, 7391, 7422, 7453, 7108, 0};
	private static final int[] MOB_IDS = {27260, 27249, 27308};
	private static final int[] X_COORDS = {162920, 47429, 47391};
	private static final int[] Y_COORDS = {-76504, -56923, -56929};
	private static final int[] Z_COORDS = {-3120, -2383, -2370};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest100SagaOfTheMaestro(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Maestro", questManager,
				118, 57,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
