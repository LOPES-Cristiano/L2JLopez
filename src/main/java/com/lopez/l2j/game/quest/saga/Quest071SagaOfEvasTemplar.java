package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 71: Saga Of Evas Templar
 * Transferencia para 3a Classe (ClassId 99 a partir de 20).
 */
@Component
public class Quest071SagaOfEvasTemplar extends SagaMasterQuest {

	public static final int QUEST_ID = 71;
	public static final String QUEST_NAME = "71_SagaOfEvasTemplar";

	private static final int[] NPC_IDS = {30852, 31624, 31278, 30852, 31638, 31646, 31648, 31651, 31654, 31655, 31658, 31281};
	private static final int[] ITEM_IDS = {7080, 7535, 7081, 7486, 7269, 7300, 7331, 7362, 7393, 7424, 7094, 6482};
	private static final int[] MOB_IDS = {27287, 27220, 27279};
	private static final int[] X_COORDS = {119518, 181215, 181227};
	private static final int[] Y_COORDS = {-28658, 36676, 36703};
	private static final int[] Z_COORDS = {-3811, -4812, -4816};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest071SagaOfEvasTemplar(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of Evas Templar", questManager,
				99, 20,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
