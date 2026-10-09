package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 79: Saga Of The Adventurer
 * Transferencia para 3a Classe (ClassId 93 a partir de 8).
 */
@Component
public class Quest079SagaOfTheAdventurer extends SagaMasterQuest {

	public static final int QUEST_ID = 79;
	public static final String QUEST_NAME = "79_SagaOfTheAdventurer";

	private static final int[] NPC_IDS = {31603, 31584, 31579, 31615, 31619, 31646, 31647, 31651, 31654, 31655, 31658, 31616};
	private static final int[] ITEM_IDS = {7080, 7516, 7081, 7494, 7277, 7308, 7339, 7370, 7401, 7432, 7102, 0};
	private static final int[] MOB_IDS = {27299, 27228, 27302};
	private static final int[] X_COORDS = {119518, 181205, 181215};
	private static final int[] Y_COORDS = {-28658, 36676, 36676};
	private static final int[] Z_COORDS = {-3811, -4816, -4812};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest079SagaOfTheAdventurer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Adventurer", questManager,
				93, 8,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
