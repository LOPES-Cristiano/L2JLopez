package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 76: Saga Of The Grand Khavatari
 * Transferencia para 3a Classe (ClassId 114 a partir de 48).
 */
@Component
public class Quest076SagaOfTheGrandKhavatari extends SagaMasterQuest {

	public static final int QUEST_ID = 76;
	public static final String QUEST_NAME = "76_SagaOfTheGrandKhavatari";

	private static final int[] NPC_IDS = {31339, 31624, 31589, 31290, 31637, 31646, 31647, 31652, 31654, 31655, 31659, 31290};
	private static final int[] ITEM_IDS = {7080, 7539, 7081, 7491, 7274, 7305, 7336, 7367, 7398, 7429, 7099, 0};
	private static final int[] MOB_IDS = {27293, 27226, 27284};
	private static final int[] X_COORDS = {161719, 124355, 124376};
	private static final int[] Y_COORDS = {-92823, 82155, 82127};
	private static final int[] Z_COORDS = {-1893, -2803, -2796};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest076SagaOfTheGrandKhavatari(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Grand Khavatari", questManager,
				114, 48,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
