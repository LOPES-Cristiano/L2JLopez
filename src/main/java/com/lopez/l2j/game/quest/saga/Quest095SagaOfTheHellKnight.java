package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 95: Saga Of The Hell Knight
 * Transferencia para 3a Classe (ClassId 91 a partir de 6).
 */
@Component
public class Quest095SagaOfTheHellKnight extends SagaMasterQuest {

	public static final int QUEST_ID = 95;
	public static final String QUEST_NAME = "95_SagaOfTheHellKnight";

	private static final int[] NPC_IDS = {31582, 31623, 31297, 31297, 31599, 31646, 31647, 31653, 31654, 31655, 31656, 31297};
	private static final int[] ITEM_IDS = {7080, 7532, 7081, 7510, 7293, 7324, 7355, 7386, 7417, 7448, 7086, 0};
	private static final int[] MOB_IDS = {27258, 27244, 27263};
	private static final int[] X_COORDS = {162920, 47391, 47429};
	private static final int[] Y_COORDS = {-76504, -56929, -56923};
	private static final int[] Z_COORDS = {-3120, -2370, -2383};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest095SagaOfTheHellKnight(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Hell Knight", questManager,
				91, 6,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
