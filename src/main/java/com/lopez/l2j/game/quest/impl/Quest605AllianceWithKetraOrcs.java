package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 605: Alliance with Ketra Orcs
 *
 * Progressão épica de Aliança em 5 Estágios com os Ketra Orcs em Goddard.
 * Hierarch Wahkan (31371) troca insígnias de Varka Silenos por marcas de aliança (7211–7215).
 */
@Component
public class Quest605AllianceWithKetraOrcs extends Quest {

	public static final int QUEST_ID = 605;
	public static final String QUEST_NAME = "605_AllianceWithKetraOrcs";

	public static final int WAHKAN = 31371;

	// Marcas de Aliança Ketra (Estágios 1 a 5)
	public static final int MARK_KETRA_1 = 7211;
	public static final int MARK_KETRA_2 = 7212;
	public static final int MARK_KETRA_3 = 7213;
	public static final int MARK_KETRA_4 = 7214;
	public static final int MARK_KETRA_5 = 7215;

	// Insígnias Varka
	public static final int VARKA_BADGE_SOLDIER = 7216;
	public static final int VARKA_BADGE_CAPTAIN = 7217;
	public static final int VARKA_BADGE_OFFICER = 7217;
	public static final int VARKA_BADGE_GENERAL = 7218;

	// Totens especiais
	public static final int TOTEM_OF_VALOR = 7219;
	public static final int TOTEM_OF_WISDOM = 7220;

	// Marcas rivais Varka
	public static final int MARK_VARKA_1 = 7221;
	public static final int MARK_VARKA_2 = 7222;
	public static final int MARK_VARKA_3 = 7223;
	public static final int MARK_VARKA_4 = 7224;
	public static final int MARK_VARKA_5 = 7225;

	// Inimigos Silenos Nível 1
	private static final Set<Integer> VARKA_SOLDIERS = Set.of(
			21350, 21351, 21352, 21353, 21354, 21355
	);
	// Inimigos Silenos Nível 2
	private static final Set<Integer> VARKA_OFFICERS = Set.of(
			21356, 21357, 21358, 21359, 21360, 21361, 21362, 21363, 21364, 21369, 21370
	);
	// Inimigos Silenos Nível 3+
	private static final Set<Integer> VARKA_CAPTAINS = Set.of(
			21365, 21366, 21367, 21368, 21371, 21372, 21373, 21374, 21375
	);
	// Aliados Ketra (quebra de aliança ao matar)
	private static final Set<Integer> KETRA_ALLIES = Set.of(
			21324, 21325, 21327, 21328, 21329, 21331, 21332, 21334, 21335, 21336,
			21338, 21339, 21340, 21342, 21343, 21344, 21345, 21346, 21347, 21348, 21349,
			25299, 25302, 25305, 25306
	);

	public Quest605AllianceWithKetraOrcs() {
		this(null);
	}

	@Autowired
	public Quest605AllianceWithKetraOrcs(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Alliance with Ketra Orcs");
		addStartNpc(WAHKAN);
		addTalkId(WAHKAN);

		for (int mobId : VARKA_SOLDIERS) {
			addKillId(mobId);
		}
		for (int mobId : VARKA_OFFICERS) {
			addKillId(mobId);
		}
		for (int mobId : VARKA_CAPTAINS) {
			addKillId(mobId);
		}
		for (int mobId : KETRA_ALLIES) {
			addKillId(mobId);
		}

		registerQuestItems(VARKA_BADGE_SOLDIER, VARKA_BADGE_CAPTAIN, VARKA_BADGE_GENERAL);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState st = checkQuestState(player);
		if (st == null && player != null) {
			st = newQuestState(player);
		}
		if (st == null) {
			return null;
		}
		return onEvent(event, st);
	}

	@Override
	public String onEvent(String event, QuestState st) {
		if (st == null) {
			return null;
		}

		if ("31371-04.htm".equalsIgnoreCase(event)
				|| "31371-03a.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			if (st.hasQuestItems(MARK_VARKA_1) || st.hasQuestItems(MARK_VARKA_2)
					|| st.hasQuestItems(MARK_VARKA_3) || st.hasQuestItems(MARK_VARKA_4)
					|| st.hasQuestItems(MARK_VARKA_5)) {
				return "31371-02a.htm";
			}
			st.setState(State.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			for (int i = MARK_KETRA_1; i <= MARK_KETRA_5; i++) {
				if (st.hasQuestItems(i)) {
					st.setCond(i - 7209);
					return "31371-0" + (i - 7207) + ".htm";
				}
			}
			st.setCond(1);
			return "31371-04.htm";
		} else if ("31371-10-1.htm".equalsIgnoreCase(event) || "reply_1".equalsIgnoreCase(event)) {
			if (st.count(VARKA_BADGE_SOLDIER) >= 100) {
				st.takeItems(VARKA_BADGE_SOLDIER, -1);
				st.giveItems(MARK_KETRA_1, 1);
				st.setCond(2);
				st.playSound(QuestState.SOUND_MIDDLE);
				return "31371-10-1.htm";
			}
			return "31371-03b.htm";
		} else if ("31371-10-2.htm".equalsIgnoreCase(event) || "reply_2".equalsIgnoreCase(event)) {
			if (st.count(VARKA_BADGE_SOLDIER) >= 200 && st.count(VARKA_BADGE_CAPTAIN) >= 100) {
				st.takeItems(VARKA_BADGE_SOLDIER, -1);
				st.takeItems(VARKA_BADGE_CAPTAIN, -1);
				st.takeItems(MARK_KETRA_1, -1);
				st.giveItems(MARK_KETRA_2, 1);
				st.setCond(3);
				st.playSound(QuestState.SOUND_MIDDLE);
				return "31371-10-2.htm";
			}
			return "31371-12.htm";
		} else if ("31371-10-3.htm".equalsIgnoreCase(event) || "reply_3".equalsIgnoreCase(event)) {
			if (st.count(VARKA_BADGE_SOLDIER) >= 300 && st.count(VARKA_BADGE_CAPTAIN) >= 200
					&& st.count(VARKA_BADGE_GENERAL) >= 100) {
				st.takeItems(VARKA_BADGE_SOLDIER, -1);
				st.takeItems(VARKA_BADGE_CAPTAIN, -1);
				st.takeItems(VARKA_BADGE_GENERAL, -1);
				st.takeItems(MARK_KETRA_2, -1);
				st.giveItems(MARK_KETRA_3, 1);
				st.setCond(4);
				st.playSound(QuestState.SOUND_MIDDLE);
				return "31371-10-3.htm";
			}
			return "31371-15.htm";
		} else if ("31371-10-4.htm".equalsIgnoreCase(event) || "reply_4".equalsIgnoreCase(event)) {
			if (st.count(VARKA_BADGE_SOLDIER) >= 300 && st.count(VARKA_BADGE_CAPTAIN) >= 300
					&& st.count(VARKA_BADGE_GENERAL) >= 200 && st.hasQuestItems(TOTEM_OF_VALOR)) {
				st.takeItems(VARKA_BADGE_SOLDIER, -1);
				st.takeItems(VARKA_BADGE_CAPTAIN, -1);
				st.takeItems(VARKA_BADGE_GENERAL, -1);
				st.takeItems(TOTEM_OF_VALOR, -1);
				st.takeItems(MARK_KETRA_3, -1);
				st.giveItems(MARK_KETRA_4, 1);
				st.setCond(5);
				st.playSound(QuestState.SOUND_MIDDLE);
				return "31371-10-4.htm";
			}
			return "31371-21.htm";
		} else if ("reply_5".equalsIgnoreCase(event)) {
			return "herald_wakan_q0605_25.htm";
		} else if ("31371-20.htm".equalsIgnoreCase(event) || "reply_6".equalsIgnoreCase(event)) {
			st.takeItems(MARK_KETRA_1, -1);
			st.takeItems(MARK_KETRA_2, -1);
			st.takeItems(MARK_KETRA_3, -1);
			st.takeItems(MARK_KETRA_4, -1);
			st.takeItems(MARK_KETRA_5, -1);
			st.takeItems(VARKA_BADGE_SOLDIER, -1);
			st.takeItems(VARKA_BADGE_CAPTAIN, -1);
			st.takeItems(VARKA_BADGE_GENERAL, -1);
			st.takeItems(TOTEM_OF_VALOR, -1);
			st.takeItems(TOTEM_OF_WISDOM, -1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
			return "31371-20.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState st = player != null ? player.getQuestState(QUEST_NAME) : null;
		if (st == null && player != null) {
			st = newQuestState(player);
		}
		return onTalk(npc, st);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState st) {
		if (st == null) {
			return "noquest";
		}

		PlayerCharacter c = st.playerChar();
		if (c == null) {
			return "noquest";
		}

		if (st.isCreated()) {
			if (c.level() < 74) {
				return "31371-03.htm"; // Nível insuficiente
			}
			return "31371-01.htm"; // Início
		}

		if (st.isStarted()) {
			int cond = st.getCond();

			// Estágio 1 -> Requer 100 Varka Soldier Badges
			if (cond == 1) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 100);
					st.giveItems(MARK_KETRA_1, 1);
					st.setCond(2);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-05.htm"; // Promovido para Estágio 1!
				}
				return "31371-04a.htm"; // Em busca das 100 insígnias
			}

			// Estágio 2 -> Requer 200 Soldier + 100 Officer Badges
			if (cond == 2) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 200 && st.count(VARKA_BADGE_CAPTAIN) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 200);
					st.takeItems(VARKA_BADGE_CAPTAIN, 100);
					st.takeItems(MARK_KETRA_1, -1);
					st.giveItems(MARK_KETRA_2, 1);
					st.setCond(3);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-08.htm"; // Promovido para Estágio 2!
				}
				return "31371-06.htm";
			}

			// Estágio 3 -> Requer 300 Soldier + 200 Officer + 100 Captain Badges
			if (cond == 3) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 300 && st.count(VARKA_BADGE_CAPTAIN) >= 200
						&& st.count(VARKA_BADGE_GENERAL) >= 100) {
					st.takeItems(VARKA_BADGE_SOLDIER, 300);
					st.takeItems(VARKA_BADGE_CAPTAIN, 200);
					st.takeItems(VARKA_BADGE_GENERAL, 100);
					st.takeItems(MARK_KETRA_2, -1);
					st.giveItems(MARK_KETRA_3, 1);
					st.setCond(4);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-11.htm"; // Promovido para Estágio 3!
				}
				return "31371-09.htm";
			}

			// Estágio 4 -> Requer 300 Soldier + 300 Officer + 200 Captain + Totem of Valor
			if (cond == 4) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 300 && st.count(VARKA_BADGE_CAPTAIN) >= 300
						&& st.count(VARKA_BADGE_GENERAL) >= 200 && st.hasQuestItems(TOTEM_OF_VALOR)) {
					st.takeItems(VARKA_BADGE_SOLDIER, 300);
					st.takeItems(VARKA_BADGE_CAPTAIN, 300);
					st.takeItems(VARKA_BADGE_GENERAL, 200);
					st.takeItems(TOTEM_OF_VALOR, -1);
					st.takeItems(MARK_KETRA_3, -1);
					st.giveItems(MARK_KETRA_4, 1);
					st.setCond(5);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-14.htm";
				}
				if (st.hasQuestItems(MARK_KETRA_4)) {
					return "31371-14.htm";
				}
				return "31371-12.htm";
			}

			// Estágio 5 -> Requer 400 Soldier + 400 Officer + 200 Captain + Totem of Wisdom
			if (cond == 5) {
				if (st.count(VARKA_BADGE_SOLDIER) >= 400 && st.count(VARKA_BADGE_CAPTAIN) >= 400
						&& st.count(VARKA_BADGE_GENERAL) >= 200 && st.hasQuestItems(TOTEM_OF_WISDOM)) {
					st.takeItems(VARKA_BADGE_SOLDIER, 400);
					st.takeItems(VARKA_BADGE_CAPTAIN, 400);
					st.takeItems(VARKA_BADGE_GENERAL, 200);
					st.takeItems(TOTEM_OF_WISDOM, -1);
					st.takeItems(MARK_KETRA_4, -1);
					st.giveItems(MARK_KETRA_5, 1);
					st.setCond(6);
					st.playSound(QuestState.SOUND_FINISH);
					return "31371-10-5.htm";
				}
				return "31371-15.htm";
			}

			if (cond >= 6) {
				return "31371-08.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState st = checkQuestState(player);
		if (st == null) {
			return null;
		}
		return onKill(npc, st, isPet);
	}

	@Override
	public String onKill(NpcInstance npc, QuestState st, boolean isPet) {
		if (st == null || !st.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = st.getCond();

		if (KETRA_ALLIES.contains(npcId)) {
			st.takeItems(MARK_KETRA_1, -1);
			st.takeItems(MARK_KETRA_2, -1);
			st.takeItems(MARK_KETRA_3, -1);
			st.takeItems(MARK_KETRA_4, -1);
			st.takeItems(MARK_KETRA_5, -1);
			st.takeItems(VARKA_BADGE_SOLDIER, -1);
			st.takeItems(VARKA_BADGE_CAPTAIN, -1);
			st.takeItems(VARKA_BADGE_GENERAL, -1);
			st.exitQuest(true);
			return null;
		}

		if (cond >= 6) {
			return null;
		}

		if (VARKA_SOLDIERS.contains(npcId)) {
			int max = cond == 1 ? 100 : (cond == 2 ? 200 : (cond == 5 ? 400 : 300));
			if (st.count(VARKA_BADGE_SOLDIER) < max) {
				st.giveItems(VARKA_BADGE_SOLDIER, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (VARKA_OFFICERS.contains(npcId) && cond >= 2) {
			int max = cond == 2 ? 100 : (cond == 3 ? 200 : (cond == 4 ? 300 : 400));
			if (st.count(VARKA_BADGE_CAPTAIN) < max) {
				st.giveItems(VARKA_BADGE_CAPTAIN, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (VARKA_CAPTAINS.contains(npcId) && cond >= 3) {
			int max = cond == 3 ? 100 : 200;
			if (st.count(VARKA_BADGE_GENERAL) < max) {
				st.giveItems(VARKA_BADGE_GENERAL, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
