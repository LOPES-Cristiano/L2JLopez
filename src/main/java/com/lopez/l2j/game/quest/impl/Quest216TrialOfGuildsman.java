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
	public static final int ANT = 20079;
	public static final int ANT_CAPTAIN = 20080;
	public static final int GRANITE_GOLEM = 20083;
	public static final int MANDRAGORA_SPROUT = 20154;
	public static final int MANDRAGORA_SAPLING = 20155;
	public static final int MANDRAGORA_BLOSSOM = 20156;
	public static final int SILENOS = 20168;
	public static final int STRAIN = 20200;
	public static final int GHOUL = 20201;
	public static final int DEAD_SEEKER = 20202;
	public static final int BREKA_ORC = 20267;
	public static final int BREKA_ORC_SHAMAN = 20269;
	public static final int BREKA_ORC_OVERLORD = 20270;
	public static final int BREKA_ORC_WARRIOR = 20271;

	// Itens
	public static final int RP_JOURNEYMAN_RING = 3024;
	public static final int RP_AMBER_BEAD = 3025;
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
	public static final int GRAY_BONE_POWDER = 3130;
	public static final int GRANITE_WHETSTONE = 3131;
	public static final int RED_PIGMENT = 3132;
	public static final int BRAIDED_YARN = 3133;
	public static final int JOURNEYMAN_GEM = 3134;
	public static final int PINTERS_INSTRUCTIONS = 3135;
	public static final int AMBER_BEAD = 3136;
	public static final int AMBER_LUMP = 3137;
	public static final int JOURNEYMAN_DECO_BEADS = 3138;
	public static final int JOURNEYMAN_RING = 3139;

	private static final Set<Integer> VALID_CLASSES = Set.of(54, 56); // Scavenger, Artisan

	@Autowired
	public Quest216TrialOfGuildsman(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Trial of the Guildsman");

		addStartNpc(VALKON);
		addTalkId(VALKON, ALLTRAN, NORMAN, PINTER, DUNING);

		addKillId(ANT, ANT_CAPTAIN, GRANITE_GOLEM, MANDRAGORA_SPROUT, MANDRAGORA_SAPLING, MANDRAGORA_BLOSSOM,
				SILENOS, STRAIN, GHOUL, DEAD_SEEKER, BREKA_ORC, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD, BREKA_ORC_WARRIOR);

		registerQuestItems(RP_JOURNEYMAN_RING, RP_AMBER_BEAD, VALKONS_RECOMMEND, MANDRAGORA_BERRY,
				ALLTRANS_INSTRUCTIONS, ALLTRANS_RECOMMEND1, ALLTRANS_RECOMMEND2, NORMANS_INSTRUCTIONS,
				NORMANS_RECEIPT, DUNINGS_INSTRUCTIONS, DUNINGS_KEY, NORMANS_LIST, GRAY_BONE_POWDER,
				GRANITE_WHETSTONE, RED_PIGMENT, BRAIDED_YARN, JOURNEYMAN_GEM, PINTERS_INSTRUCTIONS,
				AMBER_BEAD, AMBER_LUMP, JOURNEYMAN_DECO_BEADS, JOURNEYMAN_RING);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		PlayerCharacter player = qs.getPlayerCharacter();
		int cond = qs.getCond();

		if ("1".equalsIgnoreCase(event) || "30103-06.htm".equalsIgnoreCase(event)) {
			if ("30103-06.htm".equalsIgnoreCase(event) && qs.getQuestItemsCount(57) >= 2000) {
				qs.takeItems(57, 2000);
			}
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.giveItems(VALKONS_RECOMMEND, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "30103-06.htm";
		} else if ("30103-06c.htm".equalsIgnoreCase(event) || "30103-07c.htm".equalsIgnoreCase(event)) {
			if (cond < 3) {
				qs.setCond(3);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			return event;
		} else if ("30283_1".equalsIgnoreCase(event) || "30283-03.htm".equalsIgnoreCase(event)) {
			qs.takeItems(MANDRAGORA_BERRY, -1);
			qs.takeItems(VALKONS_RECOMMEND, -1);
			qs.giveItems(ALLTRANS_INSTRUCTIONS, 1);
			qs.giveItems(RP_JOURNEYMAN_RING, 1);
			qs.giveItems(ALLTRANS_RECOMMEND1, 1);
			qs.giveItems(ALLTRANS_RECOMMEND2, 1);
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "30283-03.htm";
		} else if ("30210_1".equalsIgnoreCase(event) || "30210-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ALLTRANS_RECOMMEND1, -1);
			qs.giveItems(NORMANS_INSTRUCTIONS, 1);
			qs.giveItems(NORMANS_RECEIPT, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30210-04.htm";
		} else if ("30210_2".equalsIgnoreCase(event) || "30210-10.htm".equalsIgnoreCase(event)) {
			qs.giveItems(NORMANS_LIST, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30210-10.htm";
		} else if ("30688_1".equalsIgnoreCase(event) || "30688-02.htm".equalsIgnoreCase(event)) {
			qs.takeItems(NORMANS_RECEIPT, -1);
			qs.giveItems(DUNINGS_INSTRUCTIONS, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			return "30688-02.htm";
		} else if ("30298_1".equalsIgnoreCase(event) || "30298-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(ALLTRANS_RECOMMEND2, -1);
			qs.giveItems(PINTERS_INSTRUCTIONS, 1);
			qs.playSound(QuestState.SOUND_ITEMGET);
			if (player != null && player.classId() == 56) {
				qs.giveItems(RP_AMBER_BEAD, 1);
				return "30298-05.htm";
			}
			return "30298-04.htm";
		} else if ("30103_3".equalsIgnoreCase(event) || "30103_4".equalsIgnoreCase(event)
				|| "30103-09a.htm".equalsIgnoreCase(event) || "30103-09b.htm".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(JOURNEYMAN_RING)) {
				qs.takeItems(JOURNEYMAN_RING, -1);
				qs.takeItems(ALLTRANS_INSTRUCTIONS, -1);
				qs.takeItems(RP_JOURNEYMAN_RING, -1);
				qs.giveItems(MARK_OF_GUILDSMAN, 1);
				qs.addExpAndSp(514739, 33384);
				qs.setCond(0);
				qs.playSound(QuestState.SOUND_FINISH);
				qs.exitCurrentQuest(false);
				return "30103-09b.htm".equalsIgnoreCase(event) ? "30103-09b.htm" : "30103-09a.htm";
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
				return "30103-06c.htm";
			} else if (cond < 5) {
				return "30103-07.htm";
			} else if (cond == 5) {
				return "30103-08.htm";
			} else if (cond == 6 && qs.hasQuestItems(JOURNEYMAN_RING)) {
				return qs.getQuestItemsCount(JOURNEYMAN_RING) >= 7 ? "30103-09.htm" : "30103-08.htm";
			}
		} else if (npcId == ALLTRAN) {
			if (cond < 4) {
				if (cond == 1) {
					qs.setCond(2);
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
				return "30283-01.htm";
			} else if (cond == 4) {
				return "30283-02.htm";
			} else if (cond == 5) {
				if (qs.getQuestItemsCount(JOURNEYMAN_RING) >= 7) {
					qs.setCond(6);
					qs.playSound(QuestState.SOUND_MIDDLE);
					return "30283-04.htm";
				}
				return "30283-05.htm";
			} else if (cond > 5) {
				return "30283-04.htm";
			}
		} else if (npcId == NORMAN) {
			if (cond == 5) {
				if (qs.hasQuestItems(ALLTRANS_RECOMMEND1)) {
					return "30210-01.htm";
				} else if (qs.hasQuestItems(NORMANS_RECEIPT)) {
					return "30210-05.htm";
				} else if (qs.hasQuestItems(DUNINGS_INSTRUCTIONS)) {
					return "30210-06.htm";
				} else if (qs.getQuestItemsCount(DUNINGS_KEY) >= 30) {
					qs.takeItems(DUNINGS_KEY, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30210-07.htm";
				} else if (qs.hasQuestItems(NORMANS_LIST)) {
					if (qs.getQuestItemsCount(GRAY_BONE_POWDER) >= 70
							&& qs.getQuestItemsCount(GRANITE_WHETSTONE) >= 70
							&& qs.getQuestItemsCount(RED_PIGMENT) >= 70
							&& qs.getQuestItemsCount(BRAIDED_YARN) >= 70) {
						qs.takeItems(NORMANS_INSTRUCTIONS, -1);
						qs.takeItems(NORMANS_LIST, -1);
						qs.takeItems(BRAIDED_YARN, -1);
						qs.takeItems(GRANITE_WHETSTONE, -1);
						qs.takeItems(GRAY_BONE_POWDER, -1);
						qs.takeItems(RED_PIGMENT, -1);
						qs.giveItems(JOURNEYMAN_GEM, 7);
						if (qs.getQuestItemsCount(JOURNEYMAN_DECO_BEADS) >= 7) {
							qs.setCond(6);
							qs.playSound(QuestState.SOUND_MIDDLE);
						} else {
							qs.playSound(QuestState.SOUND_ITEMGET);
						}
						return "30210-12.htm";
					}
					return "30210-11.htm";
				}
			}
		} else if (npcId == DUNING) {
			if (cond == 5) {
				if (qs.hasQuestItems(NORMANS_RECEIPT)) {
					return "30688-01.htm";
				} else if (qs.hasQuestItems(DUNINGS_INSTRUCTIONS)) {
					if (qs.getQuestItemsCount(DUNINGS_KEY) < 30) {
						return "30688-03.htm";
					}
					qs.takeItems(DUNINGS_INSTRUCTIONS, -1);
					qs.playSound(QuestState.SOUND_ITEMGET);
					return "30688-04.htm";
				} else {
					return "30688-05.htm";
				}
			}
		} else if (npcId == PINTER) {
			if (cond == 5) {
				if (qs.hasQuestItems(ALLTRANS_RECOMMEND2)) {
					return (player != null && player.level() < 36) ? "30298-01.htm" : "30298-02.htm";
				} else if (qs.hasQuestItems(PINTERS_INSTRUCTIONS)) {
					if (qs.getQuestItemsCount(AMBER_BEAD) < 70) {
						return "30298-06.htm";
					}
					qs.takeItems(AMBER_BEAD, -1);
					qs.takeItems(PINTERS_INSTRUCTIONS, -1);
					qs.giveItems(JOURNEYMAN_DECO_BEADS, 7);
					if (qs.getQuestItemsCount(JOURNEYMAN_GEM) >= 7) {
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
					} else {
						qs.playSound(QuestState.SOUND_ITEMGET);
					}
					return "30298-07.htm";
				}
			} else if (qs.hasQuestItems(JOURNEYMAN_DECO_BEADS)) {
				return "30298-08.htm";
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

		if ((cond == 1 || cond == 2 || cond == 3)
				&& (npcId == MANDRAGORA_SPROUT || npcId == MANDRAGORA_SAPLING || npcId == MANDRAGORA_BLOSSOM)) {
			if (!qs.hasQuestItems(MANDRAGORA_BERRY)) {
				qs.giveItems(MANDRAGORA_BERRY, 1);
				qs.setCond(4);
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if (npcId == BREKA_ORC || npcId == BREKA_ORC_WARRIOR || npcId == BREKA_ORC_OVERLORD || npcId == BREKA_ORC_SHAMAN) {
			if (qs.hasQuestItems(DUNINGS_INSTRUCTIONS) && qs.getQuestItemsCount(DUNINGS_KEY) < 30) {
				qs.giveItems(DUNINGS_KEY, 1);
				qs.playSound(qs.getQuestItemsCount(DUNINGS_KEY) >= 30 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == GHOUL || npcId == STRAIN) {
			if (qs.hasQuestItems(NORMANS_LIST) && qs.getQuestItemsCount(GRAY_BONE_POWDER) < 70) {
				qs.giveItems(GRAY_BONE_POWDER, 5);
				qs.playSound(qs.getQuestItemsCount(GRAY_BONE_POWDER) >= 70 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == GRANITE_GOLEM) {
			if (qs.hasQuestItems(NORMANS_LIST) && qs.getQuestItemsCount(GRANITE_WHETSTONE) < 70) {
				qs.giveItems(GRANITE_WHETSTONE, 7);
				qs.playSound(qs.getQuestItemsCount(GRANITE_WHETSTONE) >= 70 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == DEAD_SEEKER) {
			if (qs.hasQuestItems(NORMANS_LIST) && qs.getQuestItemsCount(RED_PIGMENT) < 70) {
				qs.giveItems(RED_PIGMENT, 7);
				qs.playSound(qs.getQuestItemsCount(RED_PIGMENT) >= 70 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == SILENOS) {
			if (qs.hasQuestItems(NORMANS_LIST) && qs.getQuestItemsCount(BRAIDED_YARN) < 70) {
				qs.giveItems(BRAIDED_YARN, 10);
				qs.playSound(qs.getQuestItemsCount(BRAIDED_YARN) >= 70 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == ANT || npcId == ANT_CAPTAIN) {
			if (qs.hasQuestItems(PINTERS_INSTRUCTIONS) && qs.getQuestItemsCount(AMBER_BEAD) < 70) {
				qs.giveItems(AMBER_BEAD, 5);
				if (qs.playerClassId() == 56) {
					qs.giveItems(AMBER_LUMP, 1);
				}
				qs.playSound(qs.getQuestItemsCount(AMBER_BEAD) >= 70 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
