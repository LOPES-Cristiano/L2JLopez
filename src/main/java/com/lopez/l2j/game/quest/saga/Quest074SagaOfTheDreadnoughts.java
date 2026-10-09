package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 74: Saga Of The Dreadnoughts
 * Transferencia para 3a Classe (ClassId 89 a partir de 3).
 */
@Component
public class Quest074SagaOfTheDreadnoughts extends SagaMasterQuest {

	public static final int QUEST_ID = 74;
	public static final String QUEST_NAME = "74_SagaOfTheDreadnoughts";

	private static final int[] NPC_IDS = {30850, 31624, 31298, 31276, 31595, 31646, 31648, 31650, 31654, 31655, 31657, 31522};
	private static final int[] ITEM_IDS = {7080, 7538, 7081, 7489, 7272, 7303, 7334, 7365, 7396, 7427, 7097, 6480};
	private static final int[] MOB_IDS = {27290, 27223, 27282};
	private static final int[] X_COORDS = {191046, 46087, 46066};
	private static final int[] Y_COORDS = {-40640, -36372, -36396};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest074SagaOfTheDreadnoughts(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Dreadnoughts", questManager,
				89, 3,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
