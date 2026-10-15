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
 * Quest 611: Alliance with Varka Silenos
 *
 * Progressão de Aliança em 5 Estágios com os Varka Silenos em Goddard.
 * Naran Ashanuk (31378) troca insígnias de Ketra Orcs por marcas de aliança (7221–7225).
 */
@Component
public class Quest611AllianceWithVarkaSilenos extends Quest {

	public static final int QUEST_ID = 611;
	public static final String QUEST_NAME = "611_AllianceWithVarkaSilenos";

	public static final int NARAN_ASHANUK = 31378;

	// Marcas de Aliança Varka (Estágios 1 a 5)
	public static final int MARK_VARKA_1 = 7221;
	public static final int MARK_VARKA_2 = 7222;
	public static final int MARK_VARKA_3 = 7223;
	public static final int MARK_VARKA_4 = 7224;
	public static final int MARK_VARKA_5 = 7225;

	// Insígnias Ketra
	public static final int KETRA_BADGE_SOLDIER = 7226;
	public static final int KETRA_BADGE_OFFICER = 7227;
	public static final int KETRA_BADGE_CAPTAIN = 7228;

	// Penas especiais
	public static final int VALOR_FEATHER = 7229;
	public static final int WISDOM_FEATHER = 7230;

	// Marcas rivais Ketra
	public static final int MARK_KETRA_1 = 7211;
	public static final int MARK_KETRA_2 = 7212;
	public static final int MARK_KETRA_3 = 7213;
	public static final int MARK_KETRA_4 = 7214;
	public static final int MARK_KETRA_5 = 7215;

	// Inimigos Ketra Nível 1
	private static final Set<Integer> KETRA_SOLDIERS = Set.of(
			21324, 21325, 21326, 21327, 21328, 21329
	);
	// Inimigos Ketra Nível 2
	private static final Set<Integer> KETRA_OFFICERS = Set.of(
			21330, 21331, 21332, 21333, 21334, 21335, 21336, 21337, 21338, 21343, 21344
	);
	// Inimigos Ketra Nível 3+
	private static final Set<Integer> KETRA_CAPTAINS = Set.of(
			21339, 21340, 21341, 21342, 21345, 21346, 21347, 21348, 21349
	);

	public Quest611AllianceWithVarkaSilenos() {
		this(null);
	}

	@Autowired
	public Quest611AllianceWithVarkaSilenos(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Alliance with Varka Silenos");

		addStartNpc(NARAN_ASHANUK);
		addTalkId(NARAN_ASHANUK);

		for (int mobId : KETRA_SOLDIERS) {
			addKillId(mobId);
		}
		for (int mobId : KETRA_OFFICERS) {
			addKillId(mobId);
		}
		for (int mobId : KETRA_CAPTAINS) {
			addKillId(mobId);
		}

		registerQuestItems(KETRA_BADGE_SOLDIER, KETRA_BADGE_OFFICER, KETRA_BADGE_CAPTAIN);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("31378-04.htm".equalsIgnoreCase(event)
				|| "31378-03a.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MARK_KETRA_1) || qs.hasQuestItems(MARK_KETRA_2)
					|| qs.hasQuestItems(MARK_KETRA_3) || qs.hasQuestItems(MARK_KETRA_4)
					|| qs.hasQuestItems(MARK_KETRA_5)) {
				return "31378-02a.htm";
			}
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			for (int i = MARK_VARKA_1; i <= MARK_VARKA_5; i++) {
				if (qs.hasQuestItems(i)) {
					qs.setCond(i - 7219);
					return "31378-0" + (i - 7217) + ".htm";
				}
			}
			qs.setCond(1);
			return "31378-04.htm";
		} else if ("31378-10-1.htm".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 100) {
				qs.takeItems(KETRA_BADGE_SOLDIER, -1);
				qs.giveItems(MARK_VARKA_1, 1);
				qs.setCond(2);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31378-10-1.htm";
			}
			return "31378-03b.htm";
		} else if ("31378-10-2.htm".equalsIgnoreCase(event) || "reply_2".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 200
					&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 100) {
				qs.takeItems(KETRA_BADGE_SOLDIER, -1);
				qs.takeItems(KETRA_BADGE_OFFICER, -1);
				qs.takeItems(MARK_VARKA_1, -1);
				qs.giveItems(MARK_VARKA_2, 1);
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31378-10-2.htm";
			}
			return "31378-12.htm";
		} else if ("31378-10-3.htm".equalsIgnoreCase(event) || "reply_3".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 300
					&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 200
					&& qs.getQuestItemsCount(KETRA_BADGE_CAPTAIN) >= 100) {
				qs.takeItems(KETRA_BADGE_SOLDIER, -1);
				qs.takeItems(KETRA_BADGE_OFFICER, -1);
				qs.takeItems(KETRA_BADGE_CAPTAIN, -1);
				qs.takeItems(MARK_VARKA_2, -1);
				qs.giveItems(MARK_VARKA_3, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31378-10-3.htm";
			}
			return "31378-15.htm";
		} else if ("31378-10-4.htm".equalsIgnoreCase(event) || "reply_4".equalsIgnoreCase(event)) {
			if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 300
					&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 300
					&& qs.getQuestItemsCount(KETRA_BADGE_CAPTAIN) >= 200
					&& qs.hasQuestItems(VALOR_FEATHER)) {
				qs.takeItems(KETRA_BADGE_SOLDIER, -1);
				qs.takeItems(KETRA_BADGE_OFFICER, -1);
				qs.takeItems(KETRA_BADGE_CAPTAIN, -1);
				qs.takeItems(VALOR_FEATHER, -1);
				qs.takeItems(MARK_VARKA_3, -1);
				qs.giveItems(MARK_VARKA_4, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "31378-10-4.htm";
			}
			return "31378-21.htm";
		} else if ("31378-20.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			qs.takeItems(MARK_VARKA_1, -1);
			qs.takeItems(MARK_VARKA_2, -1);
			qs.takeItems(MARK_VARKA_3, -1);
			qs.takeItems(MARK_VARKA_4, -1);
			qs.takeItems(MARK_VARKA_5, -1);
			qs.takeItems(KETRA_BADGE_SOLDIER, -1);
			qs.takeItems(KETRA_BADGE_OFFICER, -1);
			qs.takeItems(KETRA_BADGE_CAPTAIN, -1);
			qs.takeItems(VALOR_FEATHER, -1);
			qs.takeItems(WISDOM_FEATHER, -1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(true);
			return "31378-20.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int cond = qs.getCond();

		if (qs.getState() == State.CREATED) {
			if (player != null && player.getLevel() < 74) {
				return "31378-03a.htm";
			}
			return "31378-01.htm";
		}

		if (qs.getState() == State.STARTED) {
			// Estágio 1 -> Requer 100 Ketra Soldier Badges
			if (cond == 1) {
				if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 100) {
					qs.takeItems(KETRA_BADGE_SOLDIER, 100);
					qs.giveItems(MARK_VARKA_1, 1);
					qs.setCond(2);
					qs.playSound("ItemSound.quest_finish");
					return "31378-05.htm";
				}
				return "31378-04a.htm";
			}

			// Estágio 2 -> Requer 200 Soldier + 100 Officer Badges
			if (cond == 2) {
				if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 200 && qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 100) {
					qs.takeItems(KETRA_BADGE_SOLDIER, 200);
					qs.takeItems(KETRA_BADGE_OFFICER, 100);
					qs.takeItems(MARK_VARKA_1, -1);
					qs.giveItems(MARK_VARKA_2, 1);
					qs.setCond(3);
					qs.playSound("ItemSound.quest_finish");
					return "31378-06.htm";
				}
				return "31378-05a.htm";
			}

			// Estágio 3 -> Requer 300 Soldier + 200 Officer + 100 Captain Badges
			if (cond == 3) {
				if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 300
						&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 200
						&& qs.getQuestItemsCount(KETRA_BADGE_CAPTAIN) >= 100) {
					qs.takeItems(KETRA_BADGE_SOLDIER, 300);
					qs.takeItems(KETRA_BADGE_OFFICER, 200);
					qs.takeItems(KETRA_BADGE_CAPTAIN, 100);
					qs.takeItems(MARK_VARKA_2, -1);
					qs.giveItems(MARK_VARKA_3, 1);
					qs.setCond(4);
					qs.playSound("ItemSound.quest_finish");
					return "31378-07.htm";
				}
				return "31378-06a.htm";
			}

			// Estágio 4 -> Requer 300 Soldier + 300 Officer + 200 Captain + Feather of Valor
			if (cond == 4) {
				if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 300
						&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 300
						&& qs.getQuestItemsCount(KETRA_BADGE_CAPTAIN) >= 200
						&& qs.getQuestItemsCount(VALOR_FEATHER) >= 1) {
					qs.takeItems(KETRA_BADGE_SOLDIER, 300);
					qs.takeItems(KETRA_BADGE_OFFICER, 300);
					qs.takeItems(KETRA_BADGE_CAPTAIN, 200);
					qs.takeItems(VALOR_FEATHER, 1);
					qs.takeItems(MARK_VARKA_3, -1);
					qs.giveItems(MARK_VARKA_4, 1);
					qs.setCond(5);
					qs.playSound("ItemSound.quest_finish");
					return "31378-08.htm";
				}
				return "31378-07a.htm";
			}

			// Estágio 5 -> Requer 400 Soldier + 400 Officer + 200 Captain + Feather of Wisdom
			if (cond == 5) {
				if (qs.getQuestItemsCount(KETRA_BADGE_SOLDIER) >= 400
						&& qs.getQuestItemsCount(KETRA_BADGE_OFFICER) >= 400
						&& qs.getQuestItemsCount(KETRA_BADGE_CAPTAIN) >= 200
						&& qs.getQuestItemsCount(WISDOM_FEATHER) >= 1) {
					qs.takeItems(KETRA_BADGE_SOLDIER, 400);
					qs.takeItems(KETRA_BADGE_OFFICER, 400);
					qs.takeItems(KETRA_BADGE_CAPTAIN, 200);
					qs.takeItems(WISDOM_FEATHER, 1);
					qs.takeItems(MARK_VARKA_4, -1);
					qs.giveItems(MARK_VARKA_5, 1);
					qs.setCond(6);
					qs.playSound("ItemSound.quest_finish");
					return "31378-09.htm";
				}
				return "31378-08a.htm";
			}

			if (cond == 6) {
				return "31378-10-5.htm";
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

		if (cond >= 1 && KETRA_SOLDIERS.contains(npcId)) {
			qs.giveItems(KETRA_BADGE_SOLDIER, 1);
			qs.playSound("ItemSound.quest_itemget");
		} else if (cond >= 2 && KETRA_OFFICERS.contains(npcId)) {
			qs.giveItems(KETRA_BADGE_OFFICER, 1);
			qs.playSound("ItemSound.quest_itemget");
		} else if (cond >= 3 && KETRA_CAPTAINS.contains(npcId)) {
			qs.giveItems(KETRA_BADGE_CAPTAIN, 1);
			qs.playSound("ItemSound.quest_itemget");
		}

		return null;
	}
}
