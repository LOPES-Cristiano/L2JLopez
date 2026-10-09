package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 83: Saga Of The Moonlight Sentinel
 * Transferencia para 3a Classe (ClassId 102 a partir de 24).
 */
@Component
public class Quest083SagaOfTheMoonlightSentinel extends SagaMasterQuest {

	public static final int QUEST_ID = 83;
	public static final String QUEST_NAME = "83_SagaOfTheMoonlightSentinel";

	private static final int[] NPC_IDS = {30702, 31627, 31604, 31640, 31634, 31646, 31648, 31652, 31654, 31655, 31658, 31641};
	private static final int[] ITEM_IDS = {7080, 7520, 7081, 7498, 7281, 7312, 7343, 7374, 7405, 7436, 7106, 0};
	private static final int[] MOB_IDS = {27297, 27232, 27306};
	private static final int[] X_COORDS = {161719, 181227, 181215};
	private static final int[] Y_COORDS = {-92823, 36703, 36676};
	private static final int[] Z_COORDS = {-1893, -4816, -4812};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest083SagaOfTheMoonlightSentinel(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Moonlight Sentinel", questManager,
				102, 24,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
