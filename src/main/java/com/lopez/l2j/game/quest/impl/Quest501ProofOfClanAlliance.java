package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
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
	public static final int SYMBOL_OF_LOYALTY = 3837;
	public static final int ANTIDOTE_RECIPE = 3872;
	public static final int VOUCHER_OF_FAITH = 3873;
	public static final int ALLIANCE_MANIFESTO = 3874;
	public static final int POTION_OF_RECOVERY = 3889;

	// Mobs de Ervas
	public static final int VANOR_SILENOS_SHAMAN = 20685;
	public static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	public static final int OEL_MAHUM_WITCH_DOCTOR = 20576;

	public Quest501ProofOfClanAlliance() {
		super(QUEST_ID, QUEST_NAME, "Proof of Clan Alliance");
		addStartNpc(SIR_KRISTOF_RODEMAI);
		addTalkId(SIR_KRISTOF_RODEMAI, STATUE_OF_OFFERING, WITCH_ATHREA, WITCH_KALIS);
		addKillId(VANOR_SILENOS_SHAMAN, HARIT_LIZARDMAN_SHAMAN, OEL_MAHUM_WITCH_DOCTOR);
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState st = checkQuestState(player);
		if (st == null) {
			return null;
		}

		if ("30756-07.htm".equalsIgnoreCase(event)) {
			st.setState(State.STARTED);
			st.set("cond", 1);
			st.set("part", 1);
			st.playSound(QuestState.SOUND_ACCEPT);
			return "30756-07.htm";
		} else if ("30759-03.htm".equalsIgnoreCase(event)) {
			st.set("part", 2);
			st.set("cond", 2);
			return "30759-03.htm";
		} else if ("30759-07.htm".equalsIgnoreCase(event)) {
			// Entrega 3 Symbols of Loyalty e recebe receita do antidoto
			st.takeItems(SYMBOL_OF_LOYALTY, 3);
			st.giveItems(ANTIDOTE_RECIPE, 1);
			st.set("part", 3);
			st.set("cond", 3);
			return "30759-07.htm";
		} else if ("30757-05.htm".equalsIgnoreCase(event)) {
			// Sacrifício voluntário na Statue of Offering
			st.giveItems(SYMBOL_OF_LOYALTY, 1);
			return "30757-06.htm";
		}

		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState st = player.getQuestState(QUEST_NAME);
		if (st == null) {
			st = newQuestState(player);
		}

		PlayerCharacter c = player.activeChar();
		if (c == null) {
			return "noquest";
		}

		int npcId = npc.npcId();

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
				return "30756-10.htm"; // Em andamento
			} else if (st.isCompleted()) {
				return "30756-02.htm"; // Ja concluida
			}
		} else if (npcId == WITCH_KALIS) {
			if (st.isStarted()) {
				int part = st.getInt("part");
				if (part == 1) {
					return "30759-01.htm";
				} else if (part == 2) {
					long symbols = st.count(SYMBOL_OF_LOYALTY);
					if (symbols >= 3) {
						return "30759-06.htm";
					}
					return "30759-05.htm";
				} else if (part >= 3) {
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
						st.set("cond", 4);
						return "30759-08.htm"; // Entrega o Voucher of Faith
					}
					return "30759-10.htm"; // Procurando ingredientes do antidoto
				}
			}
		} else if (npcId == STATUE_OF_OFFERING) {
			if (st.isStarted() && st.getInt("part") == 2) {
				return "30757-01.htm";
			}
		}

		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState st = checkQuestState(player);
		if (st == null || !st.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int part = st.getInt("part");

		if (part >= 3) {
			if (npcId == HARIT_LIZARDMAN_SHAMAN && !st.hasQuestItems(HERB_OF_HARIT)) {
				st.giveItems(HERB_OF_HARIT, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else if (npcId == VANOR_SILENOS_SHAMAN && !st.hasQuestItems(HERB_OF_VANOR)) {
				st.giveItems(HERB_OF_VANOR, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else if (npcId == OEL_MAHUM_WITCH_DOCTOR && !st.hasQuestItems(HERB_OF_OEL_MAHUM)) {
				st.giveItems(HERB_OF_OEL_MAHUM, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}
