package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 91: Saga Of The Arcana Lord
 * Transferencia para 3a Classe (ClassId 96 a partir de 14).
 */
@Component
public class Quest091SagaOfTheArcanaLord extends SagaMasterQuest {

	public static final int QUEST_ID = 91;
	public static final String QUEST_NAME = "91_SagaOfTheArcanaLord";

	private static final int[] NPC_IDS = {31605, 31622, 31585, 31608, 31586, 31646, 31647, 31651, 31654, 31655, 31658, 31608};
	private static final int[] ITEM_IDS = {7080, 7604, 7081, 7506, 7289, 7320, 7351, 7382, 7413, 7444, 7110, 0};
	private static final int[] MOB_IDS = {27313, 27240, 27310};
	private static final int[] X_COORDS = {119518, 181215, 181227};
	private static final int[] Y_COORDS = {-28658, 36676, 36703};
	private static final int[] Z_COORDS = {-3811, -4812, -4816};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest091SagaOfTheArcanaLord(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Arcana Lord", questManager,
				96, 14,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
