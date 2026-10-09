package com.lopez.l2j.game.quest.saga;

import com.lopez.l2j.game.quest.QuestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 96: Saga Of The Spectral Dancer
 * Transferencia para 3a Classe (ClassId 107 a partir de 34).
 */
@Component
public class Quest096SagaOfTheSpectralDancer extends SagaMasterQuest {

	public static final int QUEST_ID = 96;
	public static final String QUEST_NAME = "96_SagaOfTheSpectralDancer";

	private static final int[] NPC_IDS = {31582, 31623, 31284, 31284, 31611, 31646, 31649, 31653, 31654, 31655, 31656, 31284};
	private static final int[] ITEM_IDS = {7080, 7527, 7081, 7511, 7294, 7325, 7356, 7387, 7418, 7449, 7092, 0};
	private static final int[] MOB_IDS = {27272, 27245, 27264};
	private static final int[] X_COORDS = {162920, 47429, 47391};
	private static final int[] Y_COORDS = {-76456, -56923, -56929};
	private static final int[] Z_COORDS = {-3120, -2383, -2370};
	private static final String[] TEXTS = new String[18];

	@Autowired
	public Quest096SagaOfTheSpectralDancer(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Saga Of The Spectral Dancer", questManager,
				107, 34,
				NPC_IDS, ITEM_IDS, MOB_IDS,
				X_COORDS, Y_COORDS, Z_COORDS, TEXTS);
	}
}
