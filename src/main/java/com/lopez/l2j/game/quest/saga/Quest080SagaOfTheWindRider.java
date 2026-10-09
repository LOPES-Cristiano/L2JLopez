package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 80: Saga Of The Wind Rider
 * Transferencia para 3a Classe (ClassId 101 a partir de 23).
 */
@Component
public class Quest080SagaOfTheWindRider extends SagaMasterQuest {

	public static final int QUEST_ID = 80;
	public static final String QUEST_NAME = "80_SagaOfTheWindRider";

	private static final int[] NPC_IDS = {31603, 31624, 31284, 31615, 31612, 31646, 31648, 31652, 31654, 31655, 31659, 31616};
	private static final int[] ITEM_IDS = {7080, 7517, 7081, 7495, 7278, 7309, 7340, 7371, 7402, 7433, 7103, 0};
	private static final int[] MOB_IDS = {27300, 27229, 27303};
	private static final int[] X_COORDS = {161719, 124314, 124355};
	private static final int[] Y_COORDS = {-92823, 82155, 82155};
	private static final int[] Z_COORDS = {-1893, -2803, -2803};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest080SagaOfTheWindRider(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Wind Rider", questManager,
				101, 23,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
