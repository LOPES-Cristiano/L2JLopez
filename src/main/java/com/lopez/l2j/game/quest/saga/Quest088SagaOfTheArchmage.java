package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 88: Saga Of The Archmage
 * Transferencia para 3a Classe (ClassId 94 a partir de 12).
 */
@Component
public class Quest088SagaOfTheArchmage extends SagaMasterQuest {

	public static final int QUEST_ID = 88;
	public static final String QUEST_NAME = "88_SagaOfTheArchmage";

	private static final int[] NPC_IDS = {30176, 31627, 31282, 31282, 31590, 31646, 31647, 31650, 31654, 31655, 31657, 31282};
	private static final int[] ITEM_IDS = {7080, 7529, 7081, 7503, 7286, 7317, 7348, 7379, 7410, 7441, 7082, 0};
	private static final int[] MOB_IDS = {27250, 27237, 27254};
	private static final int[] X_COORDS = {191046, 46066, 46087};
	private static final int[] Y_COORDS = {-40640, -36396, -36372};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest088SagaOfTheArchmage(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Archmage", questManager,
				94, 12,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
