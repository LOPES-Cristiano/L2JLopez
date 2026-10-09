package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 81: Saga Of The Ghost Hunter
 * Transferencia para 3a Classe (ClassId 108 a partir de 36).
 */
@Component
public class Quest081SagaOfTheGhostHunter extends SagaMasterQuest {

	public static final int QUEST_ID = 81;
	public static final String QUEST_NAME = "81_SagaOfTheGhostHunter";

	private static final int[] NPC_IDS = {31603, 31624, 31286, 31615, 31617, 31646, 31649, 31653, 31654, 31655, 31656, 31616};
	private static final int[] ITEM_IDS = {7080, 7518, 7081, 7496, 7279, 7310, 7341, 7372, 7403, 7434, 7104, 0};
	private static final int[] MOB_IDS = {27301, 27230, 27304};
	private static final int[] X_COORDS = {162920, 47391, 47429};
	private static final int[] Y_COORDS = {-76504, -56929, -56923};
	private static final int[] Z_COORDS = {-3120, -2370, -2383};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest081SagaOfTheGhostHunter(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Ghost Hunter", questManager,
				108, 36,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
