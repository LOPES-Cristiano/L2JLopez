package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 82: Saga Of The Sagittarius
 * Transferencia para 3a Classe (ClassId 92 a partir de 9).
 */
@Component
public class Quest082SagaOfTheSagittarius extends SagaMasterQuest {

	public static final int QUEST_ID = 82;
	public static final String QUEST_NAME = "82_SagaOfTheSagittarius";

	private static final int[] NPC_IDS = {30702, 31627, 31604, 31640, 31633, 31646, 31647, 31650, 31654, 31655, 31657, 31641};
	private static final int[] ITEM_IDS = {7080, 7519, 7081, 7497, 7280, 7311, 7342, 7373, 7404, 7435, 7105, 0};
	private static final int[] MOB_IDS = {27296, 27231, 27305};
	private static final int[] X_COORDS = {191046, 46066, 46066};
	private static final int[] Y_COORDS = {-40640, -36396, -36396};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest082SagaOfTheSagittarius(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Sagittarius", questManager,
				92, 9,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
