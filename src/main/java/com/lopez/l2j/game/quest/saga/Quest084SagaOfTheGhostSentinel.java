package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 84: Saga Of The Ghost Sentinel
 * Transferencia para 3a Classe (ClassId 109 a partir de 37).
 */
@Component
public class Quest084SagaOfTheGhostSentinel extends SagaMasterQuest {

	public static final int QUEST_ID = 84;
	public static final String QUEST_NAME = "84_SagaOfTheGhostSentinel";

	private static final int[] NPC_IDS = {30702, 31587, 31604, 31640, 31635, 31646, 31649, 31652, 31654, 31655, 31659, 31641};
	private static final int[] ITEM_IDS = {7080, 7521, 7081, 7499, 7282, 7313, 7344, 7375, 7406, 7437, 7107, 0};
	private static final int[] MOB_IDS = {27298, 27233, 27307};
	private static final int[] X_COORDS = {161719, 124376, 124376};
	private static final int[] Y_COORDS = {-92823, 82127, 82127};
	private static final int[] Z_COORDS = {-1893, -2796, -2796};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest084SagaOfTheGhostSentinel(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Ghost Sentinel", questManager,
				109, 37,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
