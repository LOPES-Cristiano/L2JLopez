package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 234: Fate's Whisper (1º Passo Obrigatório para Subclasse no Lineage II Interlude).
 * Concede a lendária Star of Destiny (5011).
 */
@Component
public class Quest234FatesWhisper extends Quest {

	public static final int QUEST_ID = 234;
	public static final String QUEST_NAME = "234_FatesWhisper";

	// NPCs
	public static final int REORIN = 31002;
	public static final int CLIFF = 30182;
	public static final int FERRIS = 30847;
	public static final int ZENKIN = 30178;
	public static final int KASPAR = 30833;
	public static final int CABRIO_COFFER = 31027;
	public static final int KERNON_CHEST = 31028;
	public static final int GOLKONDA_CHEST = 31029;
	public static final int HALLATE_CHEST = 31030;

	// Monstros / Raid Bosses
	public static final int CABRIO = 25035;
	public static final int KERNON = 25054;
	public static final int GOLKONDA = 25126;
	public static final int HALLATE = 25220;
	public static final int BAIUM = 29020;

	// Itens
	public static final int PIPETTE_KNIFE = 4665;
	public static final int REIRIAS_SOUL_ORB = 4666;
	public static final int KERMONS_INFERNIUM_SCEPTER = 4667;
	public static final int GOLCONDAS_INFERNIUM_SCEPTER = 4668;
	public static final int HALLATES_INFERNIUM_SCEPTER = 4669;
	public static final int REORINS_HAMMER = 4670;
	public static final int REORINS_MOLD = 4671;
	public static final int INFERNIUM_VARNISH = 4672;
	public static final int RED_PIPETTE_KNIFE = 4673;
	public static final int STAR_OF_DESTINY = 5011;
	public static final int CRYSTAL_B = 1460;

	@Autowired
	public Quest234FatesWhisper(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Fate's Whisper");

		addStartNpc(REORIN);
		addTalkId(REORIN);
		addTalkId(CLIFF);
		addTalkId(FERRIS);
		addTalkId(ZENKIN);
		addTalkId(KASPAR);
		addTalkId(CABRIO_COFFER);
		addTalkId(KERNON_CHEST);
		addTalkId(GOLKONDA_CHEST);
		addTalkId(HALLATE_CHEST);

		addKillId(BAIUM);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}

		if ("1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "31002-03.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null) {
			qs = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == REORIN) {
			if (cond == 0) {
				if (c.level() >= 75) {
					return "31002-02.htm";
				} else {
					return "31002-01.htm";
				}
			} else if (cond == 1) {
				if (qs.getQuestItemsCount(REIRIAS_SOUL_ORB) > 0) {
					qs.takeItems(REIRIAS_SOUL_ORB, -1);
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-05.htm";
				}
				return "31002-04.htm";
			} else if (cond == 2) {
				if (qs.getQuestItemsCount(KERMONS_INFERNIUM_SCEPTER) > 0
						&& qs.getQuestItemsCount(GOLCONDAS_INFERNIUM_SCEPTER) > 0
						&& qs.getQuestItemsCount(HALLATES_INFERNIUM_SCEPTER) > 0) {
					qs.takeItems(KERMONS_INFERNIUM_SCEPTER, -1);
					qs.takeItems(GOLCONDAS_INFERNIUM_SCEPTER, -1);
					qs.takeItems(HALLATES_INFERNIUM_SCEPTER, -1);
					qs.setCond(3);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "31002-06.htm";
				}
				return "31002-05a.htm";
			} else if (cond >= 11) {
				// Passo final de entrega de arma B-grade + Cristais B
				if (qs.getQuestItemsCount(CRYSTAL_B) >= 984) {
					qs.takeItems(CRYSTAL_B, 984);
					qs.giveItems(STAR_OF_DESTINY, 1);
					qs.giveItems(79, 1); // Sword of Damascus (Low A-Grade representativo)
					qs.setCond(0);
					qs.exitQuest(false);
					qs.playSound(QuestState.SOUND_FINISH);
					return "31002-12.htm";
				}
				return "31002-11.htm";
			}
		} else if (npcId == CABRIO_COFFER && cond == 1) {
			if (qs.getQuestItemsCount(REIRIAS_SOUL_ORB) == 0) {
				qs.giveItems(REIRIAS_SOUL_ORB, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31027-01.htm";
			}
		} else if (npcId == KERNON_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(KERMONS_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(KERMONS_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31028-01.htm";
			}
		} else if (npcId == GOLKONDA_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(GOLCONDAS_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(GOLCONDAS_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31029-01.htm";
			}
		} else if (npcId == HALLATE_CHEST && cond == 2) {
			if (qs.getQuestItemsCount(HALLATES_INFERNIUM_SCEPTER) == 0) {
				qs.giveItems(HALLATES_INFERNIUM_SCEPTER, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				return "31030-01.htm";
			}
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		if (npc.npcId() == BAIUM && qs.getCond() == 7 && qs.getQuestItemsCount(PIPETTE_KNIFE) > 0) {
			qs.takeItems(PIPETTE_KNIFE, 1);
			qs.giveItems(RED_PIPETTE_KNIFE, 1);
			qs.setCond(8);
			qs.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}
