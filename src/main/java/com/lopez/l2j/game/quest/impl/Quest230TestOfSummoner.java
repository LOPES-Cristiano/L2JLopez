package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 230: Test of the Summoner
 * 3º Passo de 2ª Classe para Warlock, Elemental Summoner, Phantom Summoner.
 */
@Component
public class Quest230TestOfSummoner extends Quest {

	public static final int QUEST_ID = 230;
	public static final String QUEST_NAME = "230_TestOfSummoner";

	// NPCs
	public static final int GALATEA = 30634;
	public static final int LARA = 30063;
	public static final int ALMORS = 30635;
	public static final int CAMONIELL = 30636;
	public static final int BELTHUS = 30637;
	public static final int BASILLA = 30638;
	public static final int CELESTIEL = 30639;
	public static final int BRYNTHEA = 30640;

	// Itens
	public static final int MARK_OF_SUMMONER = 3336;
	public static final int GALATEAS_LETTER = 3352;
	public static final int BEGINNERS_ARCANA = 3353;
	public static final int ALMORS_ARCANA = 3354;
	public static final int CAMONIELL_ARCANA = 3355;
	public static final int BELTHUS_ARCANA = 3356;
	public static final int BASILLIA_ARCANA = 3357;
	public static final int CELESTIEL_ARCANA = 3358;
	public static final int BRYNTHEA_ARCANA = 3359;

	private static final Set<Integer> VALID_CLASSES = Set.of(11, 26, 39);

	@Autowired
	public Quest230TestOfSummoner(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Test of the Summoner");

		addStartNpc(GALATEA);
		addTalkId(GALATEA, LARA, ALMORS, CAMONIELL, BELTHUS, BASILLA, CELESTIEL, BRYNTHEA);

		registerQuestItems(GALATEAS_LETTER, BEGINNERS_ARCANA, ALMORS_ARCANA, CAMONIELL_ARCANA,
				BELTHUS_ARCANA, BASILLIA_ARCANA, CELESTIEL_ARCANA, BRYNTHEA_ARCANA);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event) || "30634-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(GALATEAS_LETTER, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30634-04.htm";
		} else if ("30634-09.htm".equalsIgnoreCase(event)) {
			if (hasAllArcanas(qs)) {
				qs.takeItems(ALMORS_ARCANA, -1);
				qs.takeItems(CAMONIELL_ARCANA, -1);
				qs.takeItems(BELTHUS_ARCANA, -1);
				qs.takeItems(BASILLIA_ARCANA, -1);
				qs.takeItems(CELESTIEL_ARCANA, -1);
				qs.takeItems(BRYNTHEA_ARCANA, -1);
				qs.giveItems(MARK_OF_SUMMONER, 1);
				qs.addExpAndSp(148409, 30000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	private boolean hasAllArcanas(QuestState qs) {
		return qs.hasQuestItems(ALMORS_ARCANA) && qs.hasQuestItems(CAMONIELL_ARCANA)
				&& qs.hasQuestItems(BELTHUS_ARCANA) && qs.hasQuestItems(BASILLIA_ARCANA)
				&& qs.hasQuestItems(CELESTIEL_ARCANA) && qs.hasQuestItems(BRYNTHEA_ARCANA);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == GALATEA) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 39 && VALID_CLASSES.contains(player.classId())) {
					return "30634-03.htm";
				} else {
					return "30634-01.htm";
				}
			} else if (cond == 1) {
				return "30634-05.htm";
			} else if (hasAllArcanas(qs)) {
				return "30634-08.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		return null;
	}
}
