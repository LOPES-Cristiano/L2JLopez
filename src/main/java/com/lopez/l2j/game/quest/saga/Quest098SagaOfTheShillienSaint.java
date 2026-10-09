package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 98: Saga Of The Shillien Saint
 * Transferencia para 3a Classe (ClassId 112 a partir de 43).
 */
@Component
public class Quest098SagaOfTheShillienSaint extends SagaMasterQuest {

	public static final int QUEST_ID = 98;
	public static final String QUEST_NAME = "98_SagaOfTheShillienSaint";

	private static final int[] NPC_IDS = {31581, 31626, 31588, 31287, 31621, 31646, 31647, 31651, 31654, 31655, 31658, 31287};
	private static final int[] ITEM_IDS = {7080, 7525, 7081, 7513, 7296, 7327, 7358, 7389, 7420, 7451, 7090, 0};
	private static final int[] MOB_IDS = {27270, 27247, 27277};
	private static final int[] X_COORDS = {119518, 181215, 181227};
	private static final int[] Y_COORDS = {-28658, 36676, 36703};
	private static final int[] Z_COORDS = {-3811, -4812, -4816};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest098SagaOfTheShillienSaint(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Shillien Saint", questManager,
				112, 43,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
