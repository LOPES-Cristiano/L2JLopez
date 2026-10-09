package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 87: Saga Of Evas Saint
 * Transferencia para 3a Classe (ClassId 105 a partir de 30).
 */
@Component
public class Quest087SagaOfEvasSaint extends SagaMasterQuest {

	public static final int QUEST_ID = 87;
	public static final String QUEST_NAME = "87_SagaOfEvasSaint";

	private static final int[] NPC_IDS = {30191, 31626, 31588, 31280, 31620, 31646, 31649, 31653, 31654, 31655, 31657, 31280};
	private static final int[] ITEM_IDS = {7080, 7524, 7081, 7502, 7285, 7316, 7347, 7378, 7409, 7440, 7088, 0};
	private static final int[] MOB_IDS = {27266, 27236, 27276};
	private static final int[] X_COORDS = {162920, 46087, 46066};
	private static final int[] Y_COORDS = {-76504, -36372, -36396};
	private static final int[] Z_COORDS = {-3120, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest087SagaOfEvasSaint(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of Evas Saint", questManager,
				105, 30,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
