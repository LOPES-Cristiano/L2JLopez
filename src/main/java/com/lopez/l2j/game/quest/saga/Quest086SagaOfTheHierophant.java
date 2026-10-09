package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 86: Saga Of The Hierophant
 * Transferencia para 3a Classe (ClassId 98 a partir de 17).
 */
@Component
public class Quest086SagaOfTheHierophant extends SagaMasterQuest {

	public static final int QUEST_ID = 86;
	public static final String QUEST_NAME = "86_SagaOfTheHierophant";

	private static final int[] NPC_IDS = {30191, 31626, 31588, 31280, 31591, 31646, 31648, 31652, 31654, 31655, 31659, 31280};
	private static final int[] ITEM_IDS = {7080, 7523, 7081, 7501, 7284, 7315, 7346, 7377, 7408, 7439, 7089, 0};
	private static final int[] MOB_IDS = {27269, 27235, 27275};
	private static final int[] X_COORDS = {161719, 124355, 124376};
	private static final int[] Y_COORDS = {-92823, 82155, 82127};
	private static final int[] Z_COORDS = {-1893, -2803, -2796};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest086SagaOfTheHierophant(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Hierophant", questManager,
				98, 17,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
