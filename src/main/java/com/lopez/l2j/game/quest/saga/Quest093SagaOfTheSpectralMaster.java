package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 93: Saga Of The Spectral Master
 * Transferencia para 3a Classe (ClassId 111 a partir de 41).
 */
@Component
public class Quest093SagaOfTheSpectralMaster extends SagaMasterQuest {

	public static final int QUEST_ID = 93;
	public static final String QUEST_NAME = "93_SagaOfTheSpectralMaster";

	private static final int[] NPC_IDS = {30175, 31287, 31613, 30175, 31632, 31646, 31649, 31653, 31654, 31655, 31656, 31613};
	private static final int[] ITEM_IDS = {7080, 7606, 7081, 7508, 7291, 7322, 7353, 7384, 7415, 7446, 7112, 0};
	private static final int[] MOB_IDS = {27315, 27242, 27312};
	private static final int[] X_COORDS = {162920, 47429, 47391};
	private static final int[] Y_COORDS = {-76504, -56923, -56929};
	private static final int[] Z_COORDS = {-3120, -2383, -2370};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest093SagaOfTheSpectralMaster(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Spectral Master", questManager,
				111, 41,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
