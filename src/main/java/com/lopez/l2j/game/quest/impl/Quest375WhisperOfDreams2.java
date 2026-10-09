package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 375: Whisper of Dreams, Part 2
 * Seer Manakia em Giran. Requer Mysterious Stone da Parte 1.
 * Karik e Cave Howler no Lair of Antharas para receitas A-Grade 100% de Robes (Tallum, Dark Crystal, Nightmare).
 */
@Component
public class Quest375WhisperOfDreams2 extends Quest {

	public static final int QUEST_ID = 375;
	public static final String QUEST_NAME = "375_WhisperOfDreams2";

	public static final int MANAKIA = 30515;
	public static final int MYSTERIOUS_STONE = 5887;

	// Aliases de compatibilidade para testes
	public static final int VAN_DIEM = MANAKIA;
	public static final int SEALED_STONE = MYSTERIOUS_STONE;

	public static final int KARIK = 20629;
	public static final int CAVE_HOWLER = 20624;

	public static final int KARIK_HORN = 5888;
	public static final int CAVE_HOWLER_SKULL = 5889;

	// Recipes A-Grade Robe 100%
	public static final int REC_ROBE_OF_SEAL_DC = 5348;
	public static final int REC_ROBE_OF_SEAL_TALLUM = 5350;
	public static final int REC_ROBE_OF_SEAL_NM = 5352;

	@Autowired
	public Quest375WhisperOfDreams2(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Whisper of Dreams, Part 2");
		addStartNpc(MANAKIA);
		addTalkId(MANAKIA);
		addKillId(KARIK, CAVE_HOWLER);
		registerQuestItems(KARIK_HORN, CAVE_HOWLER_SKULL);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event))) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "seer_manakia_q0375_03.htm";
		} else if ("reward".equalsIgnoreCase(event) || "reward_recipe".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(KARIK_HORN) >= 100 && qs.getQuestItemsCount(CAVE_HOWLER_SKULL) >= 100) {
				qs.takeItems(KARIK_HORN, -1);
				qs.takeItems(CAVE_HOWLER_SKULL, -1);
				int rec = switch (ThreadLocalRandom.current().nextInt(3)) {
					case 0 -> REC_ROBE_OF_SEAL_DC;
					case 1 -> REC_ROBE_OF_SEAL_TALLUM;
					default -> REC_ROBE_OF_SEAL_NM;
				};
				qs.giveItems(rec, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "seer_manakia_q0375_05.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		int cond = qs.getCond();
		if (cond == 0) {
			if (pc.level() >= 60 && qs.hasQuestItems(MYSTERIOUS_STONE)) {
				return "seer_manakia_q0375_01.htm";
			}
			qs.exitQuest(true);
			return "seer_manakia_q0375_02.htm";
		} else {
			if (qs.getQuestItemsCount(KARIK_HORN) >= 100 && qs.getQuestItemsCount(CAVE_HOWLER_SKULL) >= 100) {
				int rec = switch (ThreadLocalRandom.current().nextInt(3)) {
					case 0 -> REC_ROBE_OF_SEAL_DC;
					case 1 -> REC_ROBE_OF_SEAL_TALLUM;
					default -> REC_ROBE_OF_SEAL_NM;
				};
				qs.takeItems(KARIK_HORN, -1);
				qs.takeItems(CAVE_HOWLER_SKULL, -1);
				qs.giveItems(rec, 1);
				qs.playSound(QuestState.SOUND_FINISH);
				return "seer_manakia_q0375_05.htm";
			}
			return "seer_manakia_q0375_04.htm";
		}
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		int npcId = npc.getNpcId();
		if (npcId == KARIK && qs.getQuestItemsCount(KARIK_HORN) < 100) {
			qs.giveItems(KARIK_HORN, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == CAVE_HOWLER && qs.getQuestItemsCount(CAVE_HOWLER_SKULL) < 100) {
			qs.giveItems(CAVE_HOWLER_SKULL, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}
