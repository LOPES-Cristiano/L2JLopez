package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 99: Saga Of The Fortune Seeker
 * Transferencia para 3a Classe (ClassId 117 a partir de 55).
 */
@Component
public class Quest099SagaOfTheFortuneSeeker extends SagaMasterQuest {

	public static final int QUEST_ID = 99;
	public static final String QUEST_NAME = "99_SagaOfTheFortuneSeeker";

	private static final int[] NPC_IDS = {31594, 31623, 31600, 31600, 31601, 31646, 31649, 31650, 31654, 31655, 31657, 31600};
	private static final int[] ITEM_IDS = {7080, 7608, 7081, 7514, 7297, 7328, 7359, 7390, 7421, 7452, 7109, 0};
	private static final int[] MOB_IDS = {27259, 27248, 27309};
	private static final int[] X_COORDS = {191046, 46066, 46087};
	private static final int[] Y_COORDS = {-40640, -36396, -36372};
	private static final int[] Z_COORDS = {-3042, -1685, -1685};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest099SagaOfTheFortuneSeeker(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Fortune Seeker", questManager,
				117, 55,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
