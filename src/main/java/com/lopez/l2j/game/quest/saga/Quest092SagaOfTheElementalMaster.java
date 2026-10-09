package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 92: Saga Of The Elemental Master
 * Transferencia para 3a Classe (ClassId 104 a partir de 28).
 */
@Component
public class Quest092SagaOfTheElementalMaster extends SagaMasterQuest {

	public static final int QUEST_ID = 92;
	public static final String QUEST_NAME = "92_SagaOfTheElementalMaster";

	private static final int[] NPC_IDS = {30174, 31281, 31614, 31614, 31629, 31646, 31648, 31652, 31654, 31655, 31659, 31614};
	private static final int[] ITEM_IDS = {7080, 7605, 7081, 7507, 7290, 7321, 7352, 7383, 7414, 7445, 7111, 0};
	private static final int[] MOB_IDS = {27314, 27241, 27311};
	private static final int[] X_COORDS = {161719, 124376, 124355};
	private static final int[] Y_COORDS = {-92823, 82127, 82155};
	private static final int[] Z_COORDS = {-1893, -2796, -2803};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest092SagaOfTheElementalMaster(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Elemental Master", questManager,
				104, 28,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
