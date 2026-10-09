package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 72: Saga Of The Sword Muse
 * Transferencia para 3a Classe (ClassId 100 a partir de 21).
 */
@Component
public class Quest072SagaOfTheSwordMuse extends SagaMasterQuest {

	public static final int QUEST_ID = 72;
	public static final String QUEST_NAME = "72_SagaOfTheSwordMuse";

	private static final int[] NPC_IDS = {30853, 31624, 31583, 31537, 31618, 31646, 31649, 31652, 31654, 31655, 31659, 31281};
	private static final int[] ITEM_IDS = {7080, 7536, 7081, 7487, 7270, 7301, 7332, 7363, 7394, 7425, 7095, 6482};
	private static final int[] MOB_IDS = {27288, 27221, 27280};
	private static final int[] X_COORDS = {161719, 124355, 124376};
	private static final int[] Y_COORDS = {-92823, 82155, 82127};
	private static final int[] Z_COORDS = {-1893, -2803, -2796};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest072SagaOfTheSwordMuse(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Sword Muse", questManager,
				100, 21,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
