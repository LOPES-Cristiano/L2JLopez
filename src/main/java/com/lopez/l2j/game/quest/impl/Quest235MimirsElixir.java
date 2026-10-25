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
 * Quest 235: Mimir's Elixir (2º Passo Obrigatório para Subclasse no Lineage II Interlude).
 * Concede a lendária Mimir's Elixir (6319) e desbloqueia a adição de Subclasses nos Grand Masters.
 */
@Component
public class Quest235MimirsElixir extends Quest {

	public static final int QUEST_ID = 235;
	public static final String QUEST_NAME = "235_MimirsElixir";

	// NPCs
	public static final int LADD = 30721;
	public static final int JOAN = 30718;
	public static final int MIXING_URN = 31149;

	// Monstros
	public static final int CHIMERA_PIECE = 20965;
	public static final int BLOODY_GUARDIAN = 21090;

	// Itens
	public static final int STAR_OF_DESTINY = 5011;
	public static final int MAGISTER_MIXING_STONE = 5905;
	public static final int BLOOD_FIRE = 6318;
	public static final int MIMIRS_ELIXIR = 6319;
	public static final int PURE_SILVER = 6320;
	public static final int TRUE_GOLD = 6321;
	public static final int SAGES_STONE = 6322;
	public static final int ENCHANT_WEAPON_A = 729;

	@Autowired
	public Quest235MimirsElixir(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Mimir's Elixir");

		addStartNpc(LADD);
		addTalkId(LADD, JOAN, MIXING_URN);

		addKillId(CHIMERA_PIECE, BLOODY_GUARDIAN);

		registerQuestItems(MAGISTER_MIXING_STONE, BLOOD_FIRE, PURE_SILVER, TRUE_GOLD, SAGES_STONE);

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

		if ("1".equalsIgnoreCase(event) || "30721-06.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30721-06.htm".equalsIgnoreCase(event) ? "30721-06.htm" : "30721-02.htm";
		} else if ("30721-12.htm".equalsIgnoreCase(event) && qs.hasQuestItems(TRUE_GOLD)) {
			qs.setCond(6);
			qs.giveItems(MAGISTER_MIXING_STONE, 1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30721-12.htm";
		} else if (("30721-15.htm".equalsIgnoreCase(event) || "30721-16.htm".equalsIgnoreCase(event)) && qs.hasQuestItems(MIMIRS_ELIXIR)) {
			qs.takeItems(MAGISTER_MIXING_STONE, -1);
			qs.takeItems(MIMIRS_ELIXIR, -1);
			qs.takeItems(STAR_OF_DESTINY, -1);
			qs.giveItems(ENCHANT_WEAPON_A, 1);
			qs.setCond(0);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return event;
		} else if ("30718-03.htm".equalsIgnoreCase(event)) {
			qs.setCond(3);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30718-03.htm";
		} else if ("31149-02.htm".equalsIgnoreCase(event)) {
			return qs.hasQuestItems(MAGISTER_MIXING_STONE) ? "31149-02.htm" : "31149-havent.htm";
		} else if ("31149-03.htm".equalsIgnoreCase(event)) {
			return (qs.hasQuestItems(MAGISTER_MIXING_STONE) && qs.hasQuestItems(PURE_SILVER)) ? "31149-03.htm" : "31149-havent.htm";
		} else if ("31149-05.htm".equalsIgnoreCase(event)) {
			return (qs.hasQuestItems(MAGISTER_MIXING_STONE) && qs.hasQuestItems(PURE_SILVER) && qs.hasQuestItems(TRUE_GOLD)) ? "31149-05.htm" : "31149-havent.htm";
		} else if ("31149-07.htm".equalsIgnoreCase(event)) {
			return (qs.hasQuestItems(MAGISTER_MIXING_STONE) && qs.hasQuestItems(PURE_SILVER)
					&& qs.hasQuestItems(TRUE_GOLD) && qs.hasQuestItems(BLOOD_FIRE)) ? "31149-07.htm" : "31149-havent.htm";
		} else if ("31149-success.htm".equalsIgnoreCase(event) || "31149-11.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(MAGISTER_MIXING_STONE) && qs.hasQuestItems(PURE_SILVER)
					&& qs.hasQuestItems(TRUE_GOLD) && qs.hasQuestItems(BLOOD_FIRE)) {
				qs.takeItems(PURE_SILVER, -1);
				qs.takeItems(TRUE_GOLD, -1);
				qs.takeItems(BLOOD_FIRE, -1);
				qs.giveItems(MIMIRS_ELIXIR, 1);
				qs.setCond(8);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return event;
			}
			return "31149-havent.htm";
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

		if (npcId == LADD) {
			if (cond == 0) {
				if (qs.isCompleted()) {
					return "<html><body>This quest has already been completed.</body></html>";
				} else if (c.level() >= 75 && qs.getQuestItemsCount(STAR_OF_DESTINY) > 0) {
					return "30721-01.htm";
				} else {
					return "30721-01a.htm";
				}
			} else if (cond == 1) {
				if (qs.hasQuestItems(PURE_SILVER)) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30721-08.htm";
				}
				return "30721-07.htm";
			} else if (cond >= 2 && cond <= 4) {
				return "30721-10.htm";
			} else if (cond == 5 && qs.hasQuestItems(TRUE_GOLD)) {
				return "30721-11.htm";
			} else if (cond == 6 || cond == 7) {
				return "30721-13.htm";
			} else if (cond == 8) {
				if (qs.hasQuestItems(PURE_SILVER) && qs.hasQuestItems(TRUE_GOLD) && qs.hasQuestItems(BLOOD_FIRE)) {
					qs.takeItems(PURE_SILVER, -1);
					qs.takeItems(TRUE_GOLD, -1);
					qs.takeItems(BLOOD_FIRE, -1);
					qs.takeItems(MAGISTER_MIXING_STONE, -1);
					qs.takeItems(STAR_OF_DESTINY, -1);
					qs.setCond(0);
					qs.exitQuest(false);
					if (!qs.hasQuestItems(MIMIRS_ELIXIR)) {
						qs.giveItems(MIMIRS_ELIXIR, 1);
					}
					qs.giveItems(ENCHANT_WEAPON_A, 1);
					qs.playSound(QuestState.SOUND_FINISH);
					return "30721-07.htm";
				} else if (qs.hasQuestItems(MIMIRS_ELIXIR)) {
					return "30721-14.htm";
				}
			}
		} else if (npcId == JOAN) {
			if (cond == 2) {
				return "30718-01.htm";
			} else if (cond == 3) {
				return "30718-04.htm";
			} else if (cond == 4 && qs.hasQuestItems(SAGES_STONE)) {
				qs.takeItems(SAGES_STONE, -1);
				qs.giveItems(TRUE_GOLD, 1);
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "30718-05.htm";
			} else if (cond > 4) {
				return "30718-06.htm";
			}
		} else if (npcId == MIXING_URN) {
			return "31149-01.htm";
		}
		return "<html><body>I have nothing to say to you.</body></html>";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = player.getQuestState(QUEST_NAME);
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (npcId == CHIMERA_PIECE && cond == 3) {
			if (qs.getQuestItemsCount(SAGES_STONE) == 0) {
				qs.giveItems(SAGES_STONE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				qs.setCond(4);
			}
		} else if (npcId == BLOODY_GUARDIAN && cond == 6) {
			if (qs.getQuestItemsCount(BLOOD_FIRE) == 0) {
				qs.giveItems(BLOOD_FIRE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
				qs.setCond(7);
			}
		}
		return null;
	}
}
