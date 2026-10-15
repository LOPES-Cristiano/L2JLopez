package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 503: Pursuit of Clan Ambition (Elevação de Clã para Nível 5).
 * Exige líder de clã de nível 4.
 * Concede o Seal of Aspiration (3870) / Scepter of Judgement (3838/3869) e 250.000 SP.
 */
@Component
public class Quest503PursuitOfClanAmbition extends Quest {

	public static final int QUEST_ID = 503;
	public static final String QUEST_NAME = "503_PursuitOfClanAmbition";

	// NPCs
	public static final int SIR_GUSTAF_ATHEBALDT = 30760;
	public static final int GUSTAF = 30758; // Compatibilidade com teste legado + Witch Athrea
	public static final int MARTIEN = 30645;
	public static final int WITCH_ATHREA = 30758;
	public static final int WITCH_KALIS = 30759;
	public static final int CORPSE_OF_FRITZ = 30761;
	public static final int CORPSE_OF_LUTZ = 30762;
	public static final int CORPSE_OF_KURTZ = 30763;
	public static final int KUSTO = 30512;
	public static final int BALTHAZAR = 30764;
	public static final int IMPERIAL_COFFER = 30765;
	public static final int CLEO = 30766;
	public static final int SIR_ERIC_RODEMAI = 30868;
	public static final int RODEMAI = 30867;

	// Monstros
	public static final int DRAKE = 20137;
	public static final int THUNDER_WYRM_HOLD = 20243;
	public static final int THUNDER_WYRM = 20282;
	public static final int DRAKE_HOLD = 20285;
	public static final int GIANT_SOLDIER = 20654;
	public static final int GIANT_SCOUT = 20656;
	public static final int GRAVE_GUARD = 20668;
	public static final int SPITEFUL_SOUL_LEADER = 20974;
	public static final int BLITZ_WYRM = 27178;
	public static final int GRAVE_KEYMASTER = 27179;
	public static final int IMPERIAL_SLAVE = 27180;
	public static final int IMPERIAL_GRAVEKEEPER = 27181;

	// Itens
	public static final int GUSTAF_INSTRUCTION = 3834;
	public static final int SCEPTER_OF_JUDGEMENT = 3838;
	public static final int RECIPE_POWER_STONE = 3838;
	public static final int MIST_DRAKES_EGG = 3839;
	public static final int BLITZ_WYRM_EGG = 3840;
	public static final int DRAKES_EGG = 3841;
	public static final int THUNDER_WYRM_EGG = 3842;
	public static final int BROOCH_OF_THE_MAGPIE = 3843;
	public static final int NEBULITE_CRYSTAL = 3844;
	public static final int BROKEN_POWER_STONE = 3845;
	public static final int TITAN_POWER_STONE = 3846;
	public static final int IMPERIAL_KEY = 3847;
	public static final int UNDYING_WILL = 3848;
	public static final int G_LET_MARTIEN = 3866;
	public static final int G_LET_BALTHAZAR = 3867;
	public static final int G_LET_RODEMAI = 3868;
	public static final int SCEPTER_JUDGEMENT = 3869;
	public static final int SEAL_OF_ASPIRATION = 3870;
	public static final int BLACK_ANVIL_COIN = 3871;

	public static final int[] EGG_LIST = {
			MIST_DRAKES_EGG, BLITZ_WYRM_EGG, DRAKES_EGG, THUNDER_WYRM_EGG
	};

	public Quest503PursuitOfClanAmbition() {
		this(null);
	}

	@Autowired
	public Quest503PursuitOfClanAmbition(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Pursuit of Clan Ambition");

		addStartNpc(SIR_GUSTAF_ATHEBALDT, GUSTAF);
		addTalkId(
				SIR_GUSTAF_ATHEBALDT,
				MARTIEN,
				WITCH_ATHREA,
				WITCH_KALIS,
				CORPSE_OF_FRITZ,
				CORPSE_OF_LUTZ,
				CORPSE_OF_KURTZ,
				KUSTO,
				BALTHAZAR,
				IMPERIAL_COFFER,
				CLEO,
				SIR_ERIC_RODEMAI,
				RODEMAI
		);

		addKillId(
				DRAKE,
				THUNDER_WYRM_HOLD,
				THUNDER_WYRM,
				DRAKE_HOLD,
				GIANT_SOLDIER,
				GIANT_SCOUT,
				GRAVE_GUARD,
				SPITEFUL_SOUL_LEADER,
				BLITZ_WYRM,
				GRAVE_KEYMASTER,
				IMPERIAL_SLAVE,
				IMPERIAL_GRAVEKEEPER
		);

		registerQuestItems(
				GUSTAF_INSTRUCTION,
				RECIPE_POWER_STONE,
				MIST_DRAKES_EGG,
				BLITZ_WYRM_EGG,
				DRAKES_EGG,
				THUNDER_WYRM_EGG,
				BROOCH_OF_THE_MAGPIE,
				NEBULITE_CRYSTAL,
				BROKEN_POWER_STONE,
				TITAN_POWER_STONE,
				IMPERIAL_KEY,
				UNDYING_WILL,
				G_LET_MARTIEN,
				G_LET_BALTHAZAR,
				G_LET_RODEMAI,
				SCEPTER_JUDGEMENT,
				BLACK_ANVIL_COIN
		);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private boolean checkEggs(QuestState qs) {
		int count = 0;
		for (int egg : EGG_LIST) {
			if (qs.getQuestItemsCount(egg) >= 10) {
				count++;
			}
		}
		return count >= 4;
	}

	private String exit503(boolean completed, QuestState qs) {
		if (completed) {
			qs.giveItems(SEAL_OF_ASPIRATION, 1);
			qs.addExpAndSp(0, 250_000);
			qs.unset("Fritz");
			qs.unset("Lutz");
			qs.unset("Kurtz");
			qs.unset("ImpGraveKeeper");
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
		} else {
			qs.exitQuest(true);
		}
		qs.takeItems(SCEPTER_JUDGEMENT, -1);
		return "Congratulations, you have finished the Pursuit of Clan Ambition";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return null;
		}
		return onEvent(event, qs);
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		// Fluxo legado (ClanAndEndgameQuestsTest)
		if ("quest_accept".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("legacy", 1);
			qs.giveItems(GUSTAF_INSTRUCTION, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "gustaf_started.htm";
		}

		if ("gustaf_finish".equalsIgnoreCase(event) && qs.getCond() == 2) {
			qs.takeItems(GUSTAF_INSTRUCTION, -1);
			qs.giveItems(SCEPTER_OF_JUDGEMENT, 1);
			qs.playSound(QuestState.SOUND_FINISH);
			qs.exitQuest(false);
			return "gustaf_concluded.htm";
		}

		// Fluxo oficial (Retail / Lucera2)
		String htmltext = event;
		if ("30760-08.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.giveItems(G_LET_MARTIEN, 1);
			qs.setCond(1);
			qs.set("Fritz", 1);
			qs.set("Lutz", 1);
			qs.set("Kurtz", 1);
			qs.set("ImpGraveKeeper", 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30760-12.htm".equalsIgnoreCase(event)) {
			qs.giveItems(G_LET_BALTHAZAR, 1);
			qs.setCond(4);
		} else if ("30760-16.htm".equalsIgnoreCase(event)) {
			qs.giveItems(G_LET_RODEMAI, 1);
			qs.setCond(7);
		} else if ("30760-20.htm".equalsIgnoreCase(event)) {
			exit503(true, qs);
		} else if ("30760-22.htm".equalsIgnoreCase(event)) {
			qs.setCond(13);
		} else if ("30760-23.htm".equalsIgnoreCase(event)) {
			exit503(true, qs);
		} else if ("30645-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(G_LET_MARTIEN, -1);
			qs.setCond(2);
		} else if ("30763-03.htm".equalsIgnoreCase(event) || "30763-02.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("Kurtz") == 1) {
				htmltext = "30763-02.htm";
				qs.giveItems(MIST_DRAKES_EGG, 6);
				qs.giveItems(BROOCH_OF_THE_MAGPIE, 1);
				qs.set("Kurtz", 2);
			}
		} else if ("30762-03.htm".equalsIgnoreCase(event) || "30762-02.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("Lutz") == 1) {
				htmltext = "30762-02.htm";
				qs.giveItems(MIST_DRAKES_EGG, 4);
				qs.giveItems(BLITZ_WYRM_EGG, 3);
				qs.set("Lutz", 2);
			}
			qs.addSpawn(BLITZ_WYRM);
			qs.addSpawn(BLITZ_WYRM);
		} else if ("30761-03.htm".equalsIgnoreCase(event) || "30761-02.htm".equalsIgnoreCase(event)) {
			if (qs.getInt("Fritz") == 1) {
				htmltext = "30761-02.htm";
				qs.giveItems(BLITZ_WYRM_EGG, 3);
				qs.set("Fritz", 2);
			}
			qs.addSpawn(BLITZ_WYRM);
			qs.addSpawn(BLITZ_WYRM);
		} else if ("30512-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BROOCH_OF_THE_MAGPIE, -1);
			qs.giveItems(BLACK_ANVIL_COIN, 1);
			qs.set("Kurtz", 3);
		} else if ("30764-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(G_LET_BALTHAZAR, -1);
			qs.setCond(5);
			qs.set("Kurtz", 3);
		} else if ("30764-05.htm".equalsIgnoreCase(event)) {
			qs.takeItems(G_LET_BALTHAZAR, -1);
			qs.setCond(5);
		} else if ("30764-06.htm".equalsIgnoreCase(event)) {
			qs.takeItems(BLACK_ANVIL_COIN, -1);
			qs.set("Kurtz", 4);
			qs.giveItems(RECIPE_POWER_STONE, 1);
		} else if ("30868-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(G_LET_RODEMAI, -1);
			qs.setCond(8);
		} else if ("30868-06a.htm".equalsIgnoreCase(event)) {
			qs.setCond(10);
		} else if ("30868-10.htm".equalsIgnoreCase(event)) {
			qs.setCond(12);
		} else if ("30766-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(9);
		} else if ("30766-08.htm".equalsIgnoreCase(event)) {
			qs.takeItems(SCEPTER_JUDGEMENT, -1);
			exit503(false, qs);
		}

		return htmltext;
	}

	@Override
	public String onTalk(NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null && player != null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return "gustaf_noquest.htm";
		}
		return onTalk(npc, qs);
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "gustaf_noquest.htm";
		}

		PlayerCharacter character = qs.playerChar();
		if (character == null) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();
		boolean isLeader = character.isClanLeader();

		// Suporte ao fluxo legado (GUSTAF = 30758)
		if (npcId == GUSTAF) {
			if (qs.isCreated()) {
				if (isLeader && character.level() >= 55) {
					return "gustaf_intro.htm";
				} else {
					return "gustaf_not_eligible.htm";
				}
			}
			if (qs.isCompleted()) {
				return "gustaf_done.htm";
			}
			if (qs.getInt("legacy") == 1) {
				if (cond == 1) {
					return "gustaf_pending.htm";
				}
				if (cond == 2) {
					return "gustaf_ready_finish.htm";
				}
			}
			return "30758-01.htm";
		}

		if (npcId == SIR_GUSTAF_ATHEBALDT) {
			if (qs.isCreated()) {
				if (character.clanId() == 0) {
					qs.exitQuest(true);
					return "30760-01.htm";
				}
				if (!isLeader) {
					qs.exitQuest(true);
					return "30760-04t.htm";
				}
				if (qs.hasQuestItems(SEAL_OF_ASPIRATION)) {
					qs.exitQuest(true);
					return "30760-03.htm";
				}
				if (character.level() >= 50) {
					return "30760-04.htm";
				}
				qs.exitQuest(true);
				return "30760-02.htm";
			}
			if (qs.isCompleted()) {
				return "completed";
			}
			if (!isLeader) {
				if (cond == 3) {
					return "30760-11t.htm";
				} else if (cond == 4) {
					return "30760-15t.htm";
				} else if (cond == 12) {
					return "30760-19t.htm";
				} else if (cond == 13) {
					return "30760-24t.htm";
				}
				return "30760-04t.htm";
			}
			if (cond == 1) {
				return "30760-09.htm";
			} else if (cond == 2) {
				return "30760-10.htm";
			} else if (cond == 3) {
				return "30760-11.htm";
			} else if (cond == 4) {
				return "30760-13.htm";
			} else if (cond == 5) {
				return "30760-14.htm";
			} else if (cond == 6) {
				return "30760-15.htm";
			} else if (cond == 7) {
				return "30760-17.htm";
			} else if (cond == 12) {
				return "30760-19.htm";
			} else if (cond == 13) {
				return "30760-24.htm";
			}
			return "30760-18.htm";
		} else if (npcId == MARTIEN) {
			if (cond == 1) {
				if (qs.getInt("legacy") == 1 || qs.hasQuestItems(GUSTAF_INSTRUCTION)) {
					qs.setCond(2);
					return "martien_alliance_confirmed.htm";
				}
				return "30645-02.htm";
			} else if (cond == 2) {
				if (checkEggs(qs)) {
					qs.setCond(3);
					for (int egg : EGG_LIST) {
						qs.takeItems(egg, -1);
					}
					return "30645-05.htm";
				}
				return "30645-04.htm";
			} else if (cond == 3) {
				return "30645-07.htm";
			}
			return "30645-08.htm";
		} else if (npcId == CORPSE_OF_LUTZ) {
			if (cond == 2) {
				return "30762-01.htm";
			}
		} else if (npcId == CORPSE_OF_KURTZ) {
			if (cond == 2) {
				return "30763-01.htm";
			}
		} else if (npcId == CORPSE_OF_FRITZ) {
			if (cond == 2) {
				return "30761-01.htm";
			}
		} else if (npcId == KUSTO) {
			int kurtz = qs.getInt("Kurtz");
			if (kurtz == 1) {
				return "30512-01.htm";
			} else if (kurtz == 2) {
				return "30512-02.htm";
			}
			return "30512-04.htm";
		} else if (npcId == BALTHAZAR) {
			int kurtz = qs.getInt("Kurtz");
			if (cond == 4) {
				return kurtz > 2 ? "30764-04.htm" : "30764-02.htm";
			} else if (cond == 5) {
				if (qs.getQuestItemsCount(TITAN_POWER_STONE) >= 10 && qs.getQuestItemsCount(NEBULITE_CRYSTAL) >= 10) {
					qs.takeItems(TITAN_POWER_STONE, -1);
					qs.takeItems(NEBULITE_CRYSTAL, -1);
					qs.takeItems(BROOCH_OF_THE_MAGPIE, -1);
					qs.setCond(6);
					return "30764-08.htm";
				}
				return "30764-07.htm";
			} else if (cond == 6) {
				return "30764-09.htm";
			}
		} else if (npcId == SIR_ERIC_RODEMAI || npcId == RODEMAI) {
			if (cond == 7) {
				return "30868-02.htm";
			} else if (cond == 8) {
				return "30868-05.htm";
			} else if (cond == 9) {
				return "30868-06.htm";
			} else if (cond == 10) {
				return "30868-08.htm";
			} else if (cond == 11) {
				return "30868-09.htm";
			} else if (cond == 12) {
				return "30868-11.htm";
			}
		} else if (npcId == CLEO) {
			if (cond == 8) {
				return "30766-02.htm";
			} else if (cond == 9) {
				return "30766-05.htm";
			} else if (cond == 10) {
				return "30766-06.htm";
			} else if (cond == 11 || cond == 12 || cond == 13) {
				return "30766-07.htm";
			}
		} else if (npcId == IMPERIAL_COFFER) {
			if (cond == 10) {
				if (qs.getQuestItemsCount(IMPERIAL_KEY) < 6) {
					return "30765-03a.htm";
				} else if (qs.getInt("ImpGraveKeeper") == 3) {
					qs.setCond(11);
					qs.takeItems(IMPERIAL_KEY, 6);
					qs.giveItems(SCEPTER_JUDGEMENT, 1);
					return "30765-02.htm";
				}
				return "30765-03a.htm";
			}
			return "30765-01.htm";
		} else if (npcId == WITCH_KALIS) {
			return "30759-01.htm";
		}

		return "noquest";
	}

	private void giveItemUpTo(QuestState qs, int itemId, int limit) {
		long count = qs.getQuestItemsCount(itemId);
		if (count < limit) {
			qs.giveItems(itemId, 1);
			if (count + 1 >= limit) {
				qs.playSound(QuestState.SOUND_MIDDLE);
			} else {
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}

	@Override
	public String onKill(NpcInstance npc, GameSession player, boolean isPet) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			return null;
		}
		return onKill(npc, qs, isPet);
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs, boolean isPet) {
		if (qs == null || !qs.isStarted()) {
			return null;
		}

		int npcId = npc.npcId();
		int cond = qs.getCond();

		if (cond == 2) {
			switch (npcId) {
				case DRAKE -> {
					if (ThreadLocalRandom.current().nextInt(100) < 20) {
						giveItemUpTo(qs, DRAKES_EGG, 10);
					}
				}
				case DRAKE_HOLD -> {
					if (ThreadLocalRandom.current().nextInt(100) < 25) {
						giveItemUpTo(qs, DRAKES_EGG, 10);
					}
				}
				case THUNDER_WYRM_HOLD -> {
					if (ThreadLocalRandom.current().nextInt(100) < 15) {
						giveItemUpTo(qs, THUNDER_WYRM_EGG, 10);
					}
				}
				case THUNDER_WYRM -> {
					if (ThreadLocalRandom.current().nextInt(100) < 20) {
						giveItemUpTo(qs, THUNDER_WYRM_EGG, 10);
					}
				}
				case BLITZ_WYRM -> giveItemUpTo(qs, BLITZ_WYRM_EGG, 10);
			}
		} else if (cond == 5) {
			int chance = (npcId == GIANT_SOLDIER) ? 25 : (npcId == GIANT_SCOUT || npcId == SPITEFUL_SOUL_LEADER ? 35 : 0);
			if (chance > 0 && ThreadLocalRandom.current().nextInt(100) < chance) {
				int roll = ThreadLocalRandom.current().nextInt(3);
				if (roll == 0) {
					if (qs.getInt("Kurtz") >= 4) {
						giveItemUpTo(qs, BROKEN_POWER_STONE, 40);
					}
				} else if (roll == 1) {
					giveItemUpTo(qs, TITAN_POWER_STONE, 10);
				} else {
					giveItemUpTo(qs, NEBULITE_CRYSTAL, 10);
				}
			}
		} else if (cond == 10) {
			switch (npcId) {
				case GRAVE_GUARD -> {
					if (ThreadLocalRandom.current().nextInt(100) < 15) {
						qs.addSpawn(GRAVE_KEYMASTER);
					}
				}
				case GRAVE_KEYMASTER, IMPERIAL_SLAVE -> {
					if (ThreadLocalRandom.current().nextInt(100) < 80) {
						giveItemUpTo(qs, IMPERIAL_KEY, 6);
					}
				}
				case IMPERIAL_GRAVEKEEPER -> {
					qs.addSpawn(IMPERIAL_COFFER);
					qs.set("ImpGraveKeeper", 3);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}
