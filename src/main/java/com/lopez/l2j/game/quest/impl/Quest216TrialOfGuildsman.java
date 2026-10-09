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
 * Quest 216: Trial of the Guildsman
 * 1º Passo de 2ª Classe para Bounty Hunter e Warsmith.
 */
@Component
public class Quest216TrialOfGuildsman extends Quest {

	public static final int QUEST_ID = 216;
	public static final String QUEST_NAME = "216_TrialOfGuildsman";

	// NPCs
	public static final int VALKON = 30103;
	public static final int ALLTRAN = 30283;
	public static final int NORMAN = 30210;
	public static final int PINTER = 30298;
	public static final int DUNING = 30688;

	// Monstros
	public static final int MANDRAGORA_SPROUT = 20154;
	public static final int MANDRAGORA_SAPLING = 20155;
	public static final int MANDRAGORA_BLOSSOM = 20156;
	public static final int BREKA_ORC = 20267;
	public static final int BREKA_ORC_WARRIOR = 20271;

	// Itens
	public static final int MARK_OF_GUILDSMAN = 3119;
	public static final int VALKONS_RECOMMEND = 3120;
	public static final int MANDRAGORA_BERRY = 3121;
	public static final int ALLTRANS_INSTRUCTIONS = 3122;
	public static final int ALLTRANS_RECOMMEND1 = 3123;
	public static final int ALLTRANS_RECOMMEND2 = 3124;
	public static final int NORMANS_INSTRUCTIONS = 3125;
	public static final int NORMANS_RECEIPT = 3126;
	public static final int DUNINGS_INSTRUCTIONS = 3127;
	public static final int DUNINGS_KEY = 3128;
	public static final int NORMANS_LIST = 3129;
	public static final int JOURNEYMAN_RING = 3139;
	public static final int RP_JOURNEYMAN_RING = 3024;

	private static final Set<Integer> VALID_CLASSES = Set.of(54, 56); // Scavenger, Artisan

	@Autowired
	public Quest216TrialOfGuildsman(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Guildsman");

		addStartNpc(VALKON);
		addTalkId(VALKON, ALLTRAN, NORMAN, PINTER, DUNING);

		addKillId(MANDRAGORA_SPROUT, MANDRAGORA_SAPLING, MANDRAGORA_BLOSSOM, BREKA_ORC, BREKA_ORC_WARRIOR);

		registerQuestItems(VALKONS_RECOMMEND, MANDRAGORA_BERRY, ALLTRANS_INSTRUCTIONS, ALLTRANS_RECOMMEND1,
				ALLTRANS_RECOMMEND2, NORMANS_INSTRUCTIONS, NORMANS_RECEIPT, DUNINGS_INSTRUCTIONS,
				DUNINGS_KEY, NORMANS_LIST, JOURNEYMAN_RING, RP_JOURNEYMAN_RING);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		int cond = qs.getCond();

		if ("1".equalsIgnoreCase(event) || "30103-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VALKONS_RECOMMEND, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30103-06.htm";
		} else if ("30283_1".equalsIgnoreCase(event)) {
			qs.takeItems(VALKONS_RECOMMEND, -1);
			qs.giveItems(ALLTRANS_INSTRUCTIONS, 1);
			qs.giveItems(RP_JOURNEYMAN_RING, 1);
			qs.giveItems(ALLTRANS_RECOMMEND1, 1);
			qs.giveItems(ALLTRANS_RECOMMEND2, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30283-03.htm";
		} else if ("30103_3".equalsIgnoreCase(event) || "30103_4".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(JOURNEYMAN_RING)) {
				qs.takeItems(JOURNEYMAN_RING, -1);
				qs.takeItems(ALLTRANS_INSTRUCTIONS, -1);
				qs.takeItems(RP_JOURNEYMAN_RING, -1);
				qs.giveItems(MARK_OF_GUILDSMAN, 1);
				qs.addExpAndSp(514739, 33384);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30103-09a.htm";
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

		if (npcId == VALKON) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (player != null && player.level() >= 35 && VALID_CLASSES.contains(player.classId())) {
					return "30103-03.htm";
				} else {
					return "30103-01.htm";
				}
			} else if (cond == 1) {
				return "30103-07.htm";
			} else if (cond == 6 && qs.hasQuestItems(JOURNEYMAN_RING)) {
				return "30103-08.htm";
			}
		} else if (npcId == ALLTRAN) {
			if (cond == 1) {
				return "30283-01.htm";
			} else if (cond == 5) {
				if (qs.getQuestItemsCount(JOURNEYMAN_RING) >= 7) {
					qs.setCond(6);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30283-04.htm";
				}
				return "30283-05.htm";
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

		if (cond == 1 && (npcId == MANDRAGORA_SPROUT || npcId == MANDRAGORA_SAPLING || npcId == MANDRAGORA_BLOSSOM)) {
			if (!qs.hasQuestItems(MANDRAGORA_BERRY)) {
				qs.giveItems(MANDRAGORA_BERRY, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
