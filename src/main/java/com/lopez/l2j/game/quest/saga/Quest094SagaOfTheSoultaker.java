package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 94: Saga Of The Soultaker
 * Transferencia para 3a Classe (ClassId 95 a partir de 13).
 */
@Component
public class Quest094SagaOfTheSoultaker extends SagaMasterQuest {

	public static final int QUEST_ID = 94;
	public static final String QUEST_NAME = "94_SagaOfTheSoultaker";

	private static final int[] NPC_IDS = {30832, 31623, 31279, 31279, 31645, 31646, 31648, 31650, 31654, 31655, 31657, 31279};
	private static final int[] ITEM_IDS = {7080, 7533, 7081, 7509, 7292, 7323, 7354, 7385, 7416, 7447, 7085, 0};
	private static final int[] MOB_IDS = {27257, 27243, 27265};
	private static final int[] X_COORDS = {191046, 46066, 46087};
	private static final int[] Y_COORDS = {-40640, -36396, -36372};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest094SagaOfTheSoultaker(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Soultaker", questManager,
				95, 13,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
