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
 * Quest 220: Testimony of Glory
 * 2º Passo de 2ª Classe para todas as classes da raça Orc (Orc Raider, Orc Monk, Orc Shaman).
 */
@Component
public class Quest220TestimonyOfGlory extends Quest {

	public static final int QUEST_ID = 220;
	public static final String QUEST_NAME = "220_TestimonyOfGlory";

	// NPCs
	public static final int VOKIYAN = 30514;
	public static final int CHIANTA = 30642;
	public static final int TANAPI = 30571;
	public static final int KAKAI = 30565;

	// Monstros
	public static final int TYRANT = 20192;
	public static final int GUARDIAN_BASILISK = 20550;
	public static final int MANASHEN_GARGOYLE = 20563;

	// Itens
	public static final int MARK_OF_GLORY = 3203;
	public static final int VOKIYANS_ORDER1 = 3204;
	public static final int MANASHEN_SHARD = 3205;
	public static final int TYRANT_TALON = 3206;
	public static final int GUARDIAN_BASILISK_FANG = 3207;
	public static final int VOKIYANS_ORDER2 = 3208;
	public static final int NECKLACE_OF_AUTHORITY = 3209;
	public static final int CHIANTAS_ORDER1 = 3210;
	public static final int SCEPTER_BOX = 3220;
	public static final int SCEPTER_OF_TANTOS = 3236;
	public static final int RITUAL_BOX = 3237;

	private static final Set<Integer> VALID_CLASSES = Set.of(45, 47, 50);

	@Autowired
	public Quest220TestimonyOfGlory(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Testimony of Glory");

		addStartNpc(VOKIYAN);
		addTalkId(VOKIYAN, CHIANTA, TANAPI, KAKAI);

		addKillId(TYRANT, GUARDIAN_BASILISK, MANASHEN_GARGOYLE);

		registerQuestItems(VOKIYANS_ORDER1, MANASHEN_SHARD, TYRANT_TALON, GUARDIAN_BASILISK_FANG,
				VOKIYANS_ORDER2, NECKLACE_OF_AUTHORITY, CHIANTAS_ORDER1, SCEPTER_BOX, SCEPTER_OF_TANTOS, RITUAL_BOX);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30514-05.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VOKIYANS_ORDER1, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30642-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(VOKIYANS_ORDER2, -1);
			qs.giveItems(CHIANTAS_ORDER1, 1);
			qs.setCond(4);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30565-02.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(RITUAL_BOX)) {
				qs.takeItems(RITUAL_BOX, -1);
				qs.giveItems(MARK_OF_GLORY, 1);
				qs.addExpAndSp(91457, 13000);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return event;
			}
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == VOKIYAN) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 37 && VALID_CLASSES.contains(player.classId())) {
					return "30514-03.htm";
				} else {
					return "30514-02.htm";
				}
			} else if (cond == 1) {
				return "30514-06.htm";
			} else if (cond == 2 && qs.getQuestItemsCount(TYRANT_TALON) >= 10
					&& qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) >= 10
					&& qs.getQuestItemsCount(MANASHEN_SHARD) >= 10) {
				qs.takeItems(TYRANT_TALON, -1);
				qs.takeItems(GUARDIAN_BASILISK_FANG, -1);
				qs.takeItems(MANASHEN_SHARD, -1);
				qs.takeItems(VOKIYANS_ORDER1, -1);
				qs.giveItems(VOKIYANS_ORDER2, 1);
				qs.giveItems(NECKLACE_OF_AUTHORITY, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30514-08.htm";
			}
		} else if (npcId == CHIANTA) {
			if (cond == 3) {
				return "30642-01.htm";
			}
		} else if (npcId == KAKAI) {
			if (cond == 10 && qs.hasQuestItems(RITUAL_BOX)) {
				return "30565-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || qs.getState() != State.STARTED) {
			return null;
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (cond == 1) {
			if (npcId == TYRANT && qs.getQuestItemsCount(TYRANT_TALON) < 10) {
				qs.giveItems(TYRANT_TALON, 1);
			} else if (npcId == GUARDIAN_BASILISK && qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) < 10) {
				qs.giveItems(GUARDIAN_BASILISK_FANG, 1);
			} else if (npcId == MANASHEN_GARGOYLE && qs.getQuestItemsCount(MANASHEN_SHARD) < 10) {
				qs.giveItems(MANASHEN_SHARD, 1);
			}

			if (qs.getQuestItemsCount(TYRANT_TALON) >= 10
					&& qs.getQuestItemsCount(GUARDIAN_BASILISK_FANG) >= 10
					&& qs.getQuestItemsCount(MANASHEN_SHARD) >= 10) {
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
