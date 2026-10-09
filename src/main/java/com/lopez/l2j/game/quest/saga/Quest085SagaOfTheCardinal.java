package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 85: Saga Of The Cardinal
 * Transferencia para 3a Classe (ClassId 97 a partir de 16).
 */
@Component
public class Quest085SagaOfTheCardinal extends SagaMasterQuest {

	public static final int QUEST_ID = 85;
	public static final String QUEST_NAME = "85_SagaOfTheCardinal";

	private static final int[] NPC_IDS = {30191, 31626, 31588, 31280, 31644, 31646, 31647, 31651, 31654, 31655, 31658, 31280};
	private static final int[] ITEM_IDS = {7080, 7522, 7081, 7500, 7283, 7314, 7345, 7376, 7407, 7438, 7087, 0};
	private static final int[] MOB_IDS = {27267, 27234, 27274};
	private static final int[] X_COORDS = {119518, 181215, 181227};
	private static final int[] Y_COORDS = {-28658, 36676, 36703};
	private static final int[] Z_COORDS = {-3811, -4812, -4816};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest085SagaOfTheCardinal(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Cardinal", questManager,
				97, 16,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
