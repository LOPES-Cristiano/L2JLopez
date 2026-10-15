package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 501: Proof of Clan Alliance
 *
 * Elevação oficial de Clã para Nível 4.
 * Requer que o líder do clã obtenha o Alliance Manifesto (Item 3874) com Sir Kristof Rodemai e Bruxa Kalis,
 * provando a lealdade dos membros do clã através dos Symbols of Loyalty e Antidote Recipe.
 */
@Component
public class Quest501ProofOfClanAlliance extends Quest {

	public static final int QUEST_ID = 501;
	public static final String QUEST_NAME = "501_ProofOfClanAlliance";

	// NPCs
	public static final int SIR_KRISTOF_RODEMAI = 30756;
	public static final int STATUE_OF_OFFERING = 30757;
	public static final int WITCH_ATHREA = 30758;
	public static final int WITCH_KALIS = 30759;

	// Itens
	public static final int HERB_OF_HARIT = 3832;
	public static final int HERB_OF_VANOR = 3833;
	public static final int HERB_OF_OEL_MAHUM = 3834;
	public static final int BLOOD_OF_EVA = 3835;
	public static final int ATHREAS_COIN = 3836;
	public static final int SYMBOL_OF_LOYALTY = 3837;
	public static final int ANTIDOTE_RECIPE = 3872;
	public static final int VOUCHER_OF_FAITH = 3873;
	public static final int ALLIANCE_MANIFESTO = 3874;
	public static final int POTION_OF_RECOVERY = 3889;

	// Mobs de Ervas
	public static final int VANOR_SILENOS_SHAMAN = 20685;
	public static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	public static final int OEL_MAHUM_WITCH_DOCTOR = 20576;

	public static final int[] OEL_MAHUM_MOBS = {
			20576, 20577, 20578, 20579, 20580, 20581, 20582
	};

	public static final int[] ATHREA_BOXES = {
			27173, 27174, 27175, 27176, 27177, 27178
	};

	private static final Set<Integer> OEL_MAHUM_SET = Set.of(20576, 20577, 20578, 20579, 20580, 20581, 20582);
	private static final Set<Integer> BOX_SET = Set.of(27173, 27174, 27175, 27176, 27177, 27178);

	public Quest501ProofOfClanAlliance() {
		this(null);
	}

	@Autowired
	public Quest501ProofOfClanAlliance(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Proof of Clan Alliance");
		addStartNpc(SIR_KRISTOF_RODEMAI, STATUE_OF_OFFERING, WITCH_ATHREA);
		addTalkId(SIR_KRISTOF_RODEMAI, STATUE_OF_OFFERING, WITCH_ATHREA, WITCH_KALIS);
		addKillId(VANOR_SILENOS_SHAMAN, HARIT_LIZARDMAN_SHAMAN);
		addKillId(OEL_MAHUM_MOBS);
		addKillId(ATHREA_BOXES);

		registerQuestItems(
				SYMBOL_OF_LOYALTY,
				ANTIDOTE_RECIPE,
				HERB_OF_HARIT,
				HERB_OF_VANOR,
				HERB_OF_OEL_MAHUM,
				BLOOD_OF_EVA,
				ATHREAS_COIN,
				VOUCHER_OF_FAITH
		);

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

		if ("30756-07.htm".equalsIgnoreCase(event)
				|| "30756-03.htm".equalsIgnoreCase(event)
				|| "quest_accept".equalsIgnoreCase(event)
				|| "accept".equalsIgnoreCase(event)) {
			st.setState(State.STARTED);
			st.setCond(1);
			st.set("part", 1);
			st.playSound(QuestState.SOUND_ACCEPT);
			return "30756-03.htm".equalsIgnoreCase(event) ? "30756-03.htm" : "30756-07.htm";
		} else if ("30759-03.htm".equalsIgnoreCase(event)) {
			st.set("part", 2);
			st.setCond(2);
			st.set("dead_list", "");
			return "30759-03.htm";
		} else if ("30759-07.htm".equalsIgnoreCase(event)) {
			// Entrega 3 Symbols of Loyalty e recebe receita do antidoto
			st.takeItems(SYMBOL_OF_LOYALTY, -1);
			st.giveItems(ANTIDOTE_RECIPE, 1);
			st.set("part", 3);
			st.setCond(3);
			st.set("chest_count", 0);
			st.set("chest_game", 0);
			st.set("chest_try", 0);
			return "30759-07.htm";
		} else if ("30757-03.htm".equalsIgnoreCase(event)) {
			return "30757-03.htm";
		} else if ("30757-04.htm".equalsIgnoreCase(event)) {
			PlayerCharacter pc = st.playerChar();
			if (pc != null && ThreadLocalRandom.current().nextInt(100) < 50) {
				st.giveItems(SYMBOL_OF_LOYALTY, 1);
			} else {
				st.giveItems(SYMBOL_OF_LOYALTY, 1);
			}
			st.playSound(QuestState.SOUND_ACCEPT);
			return "30757-04.htm";
		} else if ("30757-05.htm".equalsIgnoreCase(event)) {
			// Sacrifício voluntário na Statue of Offering
			st.giveItems(SYMBOL_OF_LOYALTY, 1);
			return "30757-06.htm";
		} else if ("30758-03.htm".equalsIgnoreCase(event)) {
			startChestGame(st);
			return "30758-03.htm";
		} else if ("30758-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(57) < 10000) {
				return "30758-06.htm";
			}
			st.takeItems(57, 10000);
			startChestGame(st);
			return "30758-07.htm";
		} else if ("poison_timer".equalsIgnoreCase(event)) {
			st.exitQuest(true);
			return "30759-09.htm";
		} else if ("chest_timer".equalsIgnoreCase(event)) {
			if (st.getInt("chest_game") < 2) {
				stopChestGame(st);
			}
			return "";
		}

		return event;
	}

	private void startChestGame(QuestState st) {
		st.set("chest_game", 1);
		st.set("chest_count", 0);
		st.set("chest_try", st.getInt("chest_try") + 1);
		if (st.getInt("part") < 4) {
			st.set("part", 4);
		}
		if (st.getCond() < 4) {
			st.setCond(4);
		}
		for (int i = 0; i < 3; i++) {
			for (int boxId : ATHREA_BOXES) {
				st.addSpawn(boxId, 102100, 103450, -3400, 60000);
			}
		}
	}

	private void stopChestGame(QuestState st) {
		st.set("chest_game", 0);
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

		int npcId = npc.npcId();
		int cond = st.getCond();
		int part = st.getInt("part");

		if (npcId == SIR_KRISTOF_RODEMAI) {
			if (st.isCreated()) {
				// Apenas líder de clã nível 3 pode iniciar
				if (c.clanId() == 0 || !c.clanLeader()) {
					return "30756-05.htm"; // Nao e lider de cla
				}
				if (st.hasQuestItems(ALLIANCE_MANIFESTO)) {
					return "30756-03.htm"; // Ja possui a prova
				}
				return "30756-04.htm"; // Pode aceitar a missao
			} else if (st.isStarted()) {
				if (st.hasQuestItems(VOUCHER_OF_FAITH)) {
					st.takeItems(VOUCHER_OF_FAITH, -1);
					st.giveItems(ALLIANCE_MANIFESTO, 1);
					st.addExpAndSp(0, 120_000);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
					return "30756-09.htm"; // Conclusao com sucesso!
				}
				if (cond == 1 || cond == 2) {
					return "30756-06.htm";
				}
				return "30756-10.htm"; // Em andamento
			} else if (st.isCompleted()) {
				return "30756-02.htm"; // Ja concluida
			}
		} else if (npcId == WITCH_KALIS) {
			if (st.isStarted()) {
				if (part == 1 || cond == 1) {
					return "30759-01.htm";
				} else if (part == 2 || cond == 2) {
					long symbols = st.count(SYMBOL_OF_LOYALTY);
					if (symbols >= 3) {
						return "30759-06.htm";
					}
					return "30759-05.htm";
				} else if (part >= 3 || cond >= 3) {
					if (st.hasQuestItems(HERB_OF_HARIT) && st.hasQuestItems(HERB_OF_VANOR)
							&& st.hasQuestItems(HERB_OF_OEL_MAHUM) && st.hasQuestItems(BLOOD_OF_EVA)) {
						st.takeItems(HERB_OF_HARIT, -1);
						st.takeItems(HERB_OF_VANOR, -1);
						st.takeItems(HERB_OF_OEL_MAHUM, -1);
						st.takeItems(BLOOD_OF_EVA, -1);
						st.takeItems(ANTIDOTE_RECIPE, -1);
						st.giveItems(VOUCHER_OF_FAITH, 1);
						st.giveItems(POTION_OF_RECOVERY, 1);
						st.set("part", 6);
						st.setCond(6);
						st.playSound(QuestState.SOUND_FINISH);
						return "30759-08.htm"; // Entrega o Voucher of Faith
					}
					if (st.hasQuestItems(VOUCHER_OF_FAITH)) {
						return "30759-08.htm";
					}
					return "30759-10.htm"; // Procurando ingredientes do antidoto
				}
			}
		} else if (npcId == STATUE_OF_OFFERING) {
			if (st.isStarted()) {
				if (st.count(SYMBOL_OF_LOYALTY) >= 3) {
					return "30757-03.htm";
				}
				if (c.level() < 40) {
					return "30757-02.htm";
				}
				if (part == 2 || cond == 2) {
					return "30757-01.htm";
				}
				return "30757-03.htm";
			}
		} else if (npcId == WITCH_ATHREA) {
			if (st.isStarted()) {
				int chestGame = st.getInt("chest_game");
				if (chestGame == 0) {
					if (st.getInt("chest_try") != 0) {
						return "30758-05.htm";
					}
					return "30758-01.htm";
				} else if (chestGame == 1) {
					return "30758-09.htm";
				} else if (chestGame == 2) {
					st.playSound(QuestState.SOUND_FINISH);
					st.giveItems(BLOOD_OF_EVA, 1);
					stopChestGame(st);
					st.set("chest_game", 3);
					st.set("part", 5);
					if (st.hasQuestItems(HERB_OF_HARIT) && st.hasQuestItems(HERB_OF_VANOR)
							&& st.hasQuestItems(HERB_OF_OEL_MAHUM)) {
						st.setCond(5);
					} else {
						st.setCond(4);
					}
					return "30758-08.htm";
				} else if (chestGame == 3) {
					return "30758-08.htm";
				}
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
		int part = st.getInt("part");
		int cond = st.getCond();

		if (part >= 3 || cond >= 3) {
			if (npcId == HARIT_LIZARDMAN_SHAMAN && !st.hasQuestItems(HERB_OF_HARIT)) {
				st.giveItems(HERB_OF_HARIT, 1);
				checkAllIngredients(st);
			} else if (npcId == VANOR_SILENOS_SHAMAN && !st.hasQuestItems(HERB_OF_VANOR)) {
				st.giveItems(HERB_OF_VANOR, 1);
				checkAllIngredients(st);
			} else if (OEL_MAHUM_SET.contains(npcId) && !st.hasQuestItems(HERB_OF_OEL_MAHUM)) {
				st.giveItems(HERB_OF_OEL_MAHUM, 1);
				checkAllIngredients(st);
			} else if (BOX_SET.contains(npcId)) {
				int chestCount = st.getInt("chest_count") + 1;
				st.set("chest_count", chestCount);
				if (chestCount >= 4) {
					stopChestGame(st);
					st.set("chest_game", 2);
					st.set("part", 4);
					if (st.getCond() < 4) {
						st.setCond(4);
					}
					st.playSound(QuestState.SOUND_MIDDLE);
				} else {
					st.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		return null;
	}

	private void checkAllIngredients(QuestState st) {
		if (st.hasQuestItems(HERB_OF_HARIT) && st.hasQuestItems(HERB_OF_VANOR)
				&& st.hasQuestItems(HERB_OF_OEL_MAHUM) && st.hasQuestItems(BLOOD_OF_EVA)) {
			st.set("part", 5);
			st.setCond(5);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else {
			st.playSound(QuestState.SOUND_ITEMGET);
		}
	}
}
