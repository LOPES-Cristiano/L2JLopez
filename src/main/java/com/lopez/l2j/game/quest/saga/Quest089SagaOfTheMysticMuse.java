package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 89: Saga Of The Mystic Muse
 * Transferencia para 3a Classe (ClassId 103 a partir de 27).
 */
@Component
public class Quest089SagaOfTheMysticMuse extends SagaMasterQuest {

	public static final int QUEST_ID = 89;
	public static final String QUEST_NAME = "89_SagaOfTheMysticMuse";

	private static final int[] NPC_IDS = {30174, 31627, 31283, 31283, 31643, 31646, 31648, 31651, 31654, 31655, 31658, 31283};
	private static final int[] ITEM_IDS = {7080, 7530, 7081, 7504, 7287, 7318, 7349, 7380, 7411, 7442, 7083, 0};
	private static final int[] MOB_IDS = {27251, 27238, 27255};
	private static final int[] X_COORDS = {119518, 181227, 181215};
	private static final int[] Y_COORDS = {-28658, 36703, 36676};
	private static final int[] Z_COORDS = {-3811, -4816, -4812};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest089SagaOfTheMysticMuse(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Mystic Muse", questManager,
				103, 27,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
