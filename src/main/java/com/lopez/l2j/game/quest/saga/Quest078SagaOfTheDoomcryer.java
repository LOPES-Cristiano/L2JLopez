package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 78: Saga Of The Doomcryer
 * Transferencia para 3a Classe (ClassId 116 a partir de 52).
 */
@Component
public class Quest078SagaOfTheDoomcryer extends SagaMasterQuest {

	public static final int QUEST_ID = 78;
	public static final String QUEST_NAME = "78_SagaOfTheDoomcryer";

	private static final int[] NPC_IDS = {31336, 31624, 31589, 31290, 31642, 31646, 31649, 31650, 31654, 31655, 31657, 31290};
	private static final int[] ITEM_IDS = {7080, 7539, 7081, 7493, 7276, 7307, 7338, 7369, 7400, 7431, 7101, 0};
	private static final int[] MOB_IDS = {27295, 27227, 27285};
	private static final int[] X_COORDS = {191046, 46087, 46066};
	private static final int[] Y_COORDS = {-40640, -36372, -36396};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest078SagaOfTheDoomcryer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Doomcryer", questManager,
				116, 52,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
