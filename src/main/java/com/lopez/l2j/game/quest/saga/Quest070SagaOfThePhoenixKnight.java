package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 70: Saga Of The Phoenix Knight
 * Transferencia para 3a Classe (ClassId 90 a partir de 5).
 */
@Component
public class Quest070SagaOfThePhoenixKnight extends SagaMasterQuest {

	public static final int QUEST_ID = 70;
	public static final String QUEST_NAME = "70_SagaOfThePhoenixKnight";

	private static final int[] NPC_IDS = {30849, 31624, 31277, 30849, 31631, 31646, 31647, 31650, 31654, 31655, 31657, 31277};
	private static final int[] ITEM_IDS = {7080, 7534, 7081, 7485, 7268, 7299, 7330, 7361, 7392, 7423, 7093, 6482};
	private static final int[] MOB_IDS = {27286, 27219, 27278};
	private static final int[] X_COORDS = {191046, 46087, 46066};
	private static final int[] Y_COORDS = {-40640, -36372, -36396};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest070SagaOfThePhoenixKnight(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Phoenix Knight", questManager,
				90, 5,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
