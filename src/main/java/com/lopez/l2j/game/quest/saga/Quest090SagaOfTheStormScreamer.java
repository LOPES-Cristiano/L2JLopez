package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 90: Saga Of The Storm Screamer
 * Transferencia para 3a Classe (ClassId 110 a partir de 40).
 */
@Component
public class Quest090SagaOfTheStormScreamer extends SagaMasterQuest {

	public static final int QUEST_ID = 90;
	public static final String QUEST_NAME = "90_SagaOfTheStormScreamer";

	private static final int[] NPC_IDS = {30175, 31627, 31287, 31287, 31598, 31646, 31649, 31652, 31654, 31655, 31659, 31287};
	private static final int[] ITEM_IDS = {7080, 7531, 7081, 7505, 7288, 7319, 7350, 7381, 7412, 7443, 7084, 0};
	private static final int[] MOB_IDS = {27252, 27239, 27256};
	private static final int[] X_COORDS = {161719, 124376, 124355};
	private static final int[] Y_COORDS = {-92823, 82127, 82155};
	private static final int[] Z_COORDS = {-1893, -2796, -2803};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest090SagaOfTheStormScreamer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Storm Screamer", questManager,
				110, 40,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
