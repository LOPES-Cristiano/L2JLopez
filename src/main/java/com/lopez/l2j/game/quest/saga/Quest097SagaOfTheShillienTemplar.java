package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 97: Saga Of The Shillien Templar
 * Transferencia para 3a Classe (ClassId 106 a partir de 33).
 */
@Component
public class Quest097SagaOfTheShillienTemplar extends SagaMasterQuest {

	public static final int QUEST_ID = 97;
	public static final String QUEST_NAME = "97_SagaOfTheShillienTemplar";

	private static final int[] NPC_IDS = {31580, 31623, 31285, 31285, 31610, 31646, 31648, 31652, 31654, 31655, 31659, 31285};
	private static final int[] ITEM_IDS = {7080, 7526, 7081, 7512, 7295, 7326, 7357, 7388, 7419, 7450, 7091, 0};
	private static final int[] MOB_IDS = {27271, 27246, 27273};
	private static final int[] X_COORDS = {161719, 124355, 124376};
	private static final int[] Y_COORDS = {-92823, 82155, 82127};
	private static final int[] Z_COORDS = {-1893, -2803, -2796};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest097SagaOfTheShillienTemplar(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Shillien Templar", questManager,
				106, 33,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
