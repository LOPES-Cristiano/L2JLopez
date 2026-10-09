package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quest 373: Supplier of Reagents
 * Sistema de alquimia de Ivory Tower: mistura na urna, produção de reagentes, Pure Silver, True Gold e Hellfire Oil.
 */
@Component
public class Quest373SupplierOfReagents extends Quest {

	public static final int QUEST_ID = 373;
	public static final String QUEST_NAME = "373_SupplierOfReagents";

	// NPCs
	public static final int WESLEY = 30166;
	public static final int URN = 31149;

	// Monstros
	public static final int HAMES_ORC_SHAMAN = 20813;
	public static final int CRENDION = 20822;
	public static final int HALLATES_MAID = 20828;
	public static final int HALLATES_GUARDIAN = 21061;
	public static final int PLATINUM_TRIBE_SHAMAN = 21066;
	public static final int PLATINUM_GUARDIAN_SHAMAN = 21111;
	public static final int GUARDIAN_OF_HOLY_GRAIL = 21115;

	// Itens Base & Pouch
	public static final int MIXING_STONE = 5904;
	public static final int REAGENT_POUCH_1 = 6007;
	public static final int REAGENT_POUCH_2 = 6008;
	public static final int REAGENT_POUCH_3 = 6009;
	public static final int REAGENT_BOX = 6010;

	// Ingredientes
	public static final int WYRMS_BLOOD = 6011;
	public static final int LAVA_STONE = 6012;
	public static final int MOONSTONE_SHARD = 6013;
	public static final int ROTTEN_BONE = 6014;
	public static final int DEMONS_BLOOD = 6015;
	public static final int INFERNIUM_ORE = 6016;
	public static final int BLOOD_ROOT = 6017;
	public static final int VOLCANIC_ASH = 6018;
	public static final int QUICKSILVER = 6019;
	public static final int SULFUR = 6020;

	// Produtos
	public static final int DRACOPLASM = 6021;
	public static final int MAGMA_DUST = 6022;
	public static final int MOON_DUST = 6023;
	public static final int NECROPLASM = 6024;
	public static final int DEMONPLASM = 6025;
	public static final int INFERNO_DUST = 6026;
	public static final int DRACONIC_ESSENCE = 6027;
	public static final int FIRE_ESSENCE = 6028;
	public static final int LUNARGENT = 6029;
	public static final int MIDNIGHT_OIL = 6030;
	public static final int DEMONIC_ESSENCE = 6031;
	public static final int ABYSS_OIL = 6032;
	public static final int HELLFIRE_OIL = 6033;
	public static final int NIGHTMARE_OIL = 6034;

	// Subclasse / Mimir
	public static final int BLOOD_FIRE = 6318;
	public static final int MIMIRS_ELIXIR = 6319;
	public static final int PURE_SILVER = 6320;
	public static final int TRUE_GOLD = 6321;

	@Autowired
	public Quest373SupplierOfReagents(QuestManager questManager) {
		super(QUEST_ID, QUEST_NAME, "Supplier of Reagents");

		addStartNpc(WESLEY);
		addTalkId(WESLEY, URN);

		addKillId(HAMES_ORC_SHAMAN, CRENDION, HALLATES_MAID, HALLATES_GUARDIAN,
				PLATINUM_TRIBE_SHAMAN, PLATINUM_GUARDIAN_SHAMAN, GUARDIAN_OF_HOLY_GRAIL);

		registerQuestItems(MIXING_STONE);

		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		if (qs == null) {
			return null;
		}

		if ("30166-4.htm".equalsIgnoreCase(event)) {
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.giveItems(MIXING_STONE, 1);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return event;
		} else if ("30166-5.htm".equalsIgnoreCase(event)) {
			for (int id = 6007; id <= 6034; id++) {
				qs.takeItems(id, -1);
			}
			qs.takeItems(MIXING_STONE, -1);
			qs.exitCurrentQuest(true);
			qs.playSound(QuestState.SOUND_FINISH);
			return event;
		}

		// Urna Alquímica
		if ("Initial".equalsIgnoreCase(event)) {
			if (!qs.hasQuestItems(MIXING_STONE)) {
				return "NoMixing.htm";
			}
			QuestState mimir = qs.getPlayer() != null ? qs.getPlayer().getQuestState("235_MimirsElixir") : null;
			if (mimir != null && qs.hasQuestItems(PURE_SILVER) && qs.hasQuestItems(TRUE_GOLD) && qs.hasQuestItems(BLOOD_FIRE)) {
				return "3.htm";
			}
			return "2.htm";
		}

		// Síntese Mimir's Elixir
		if ("MimirElixirTemp1".equalsIgnoreCase(event)) {
			if (qs.hasQuestItems(PURE_SILVER) && qs.hasQuestItems(TRUE_GOLD) && qs.hasQuestItems(BLOOD_FIRE)) {
				qs.takeItems(PURE_SILVER, 1);
				qs.takeItems(TRUE_GOLD, 1);
				qs.takeItems(BLOOD_FIRE, 1);
				qs.giveItems(MIMIRS_ELIXIR, 1);
				QuestState mimir = qs.getPlayer() != null ? qs.getPlayer().getQuestState("235_MimirsElixir") : null;
				if (mimir != null) {
					mimir.setCond(8);
				}
				qs.playSound("SkillSound5.liquid_success_01");
				return "Ok.htm";
			}
			return "NoItem.htm";
		}

		// Fórmulas de Mistura
		if (event.startsWith("TempMoondust")) {
			return mixReagent(qs, MOONSTONE_SHARD, 10, VOLCANIC_ASH, 1, MOON_DUST, parseTemp(event));
		} else if (event.startsWith("TempLunargent")) {
			return mixReagent(qs, MOON_DUST, 10, QUICKSILVER, 1, LUNARGENT, parseTemp(event));
		} else if (event.startsWith("TempPureSilver")) {
			return mixReagent(qs, LUNARGENT, 1, QUICKSILVER, 1, PURE_SILVER, parseTemp(event));
		} else if (event.startsWith("TempTrueGold")) {
			return mixReagent(qs, MAGMA_DUST, 10, QUICKSILVER, 1, TRUE_GOLD, parseTemp(event));
		} else if (event.startsWith("TempDracoplasm")) {
			return mixReagent(qs, WYRMS_BLOOD, 10, BLOOD_ROOT, 1, DRACOPLASM, parseTemp(event));
		} else if (event.startsWith("TempDraconicEssence")) {
			return mixReagent(qs, DRACOPLASM, 10, QUICKSILVER, 1, DRACONIC_ESSENCE, parseTemp(event));
		} else if (event.startsWith("TempMagmaDust")) {
			return mixReagent(qs, LAVA_STONE, 10, VOLCANIC_ASH, 1, MAGMA_DUST, parseTemp(event));
		} else if (event.startsWith("TempFireEssence")) {
			return mixReagent(qs, MAGMA_DUST, 10, SULFUR, 1, FIRE_ESSENCE, parseTemp(event));
		} else if (event.startsWith("TempNecroplasm")) {
			return mixReagent(qs, ROTTEN_BONE, 10, BLOOD_ROOT, 1, NECROPLASM, parseTemp(event));
		} else if (event.startsWith("TempMidnightOil")) {
			return mixReagent(qs, NECROPLASM, 10, QUICKSILVER, 1, MIDNIGHT_OIL, parseTemp(event));
		} else if (event.startsWith("TempDemonplasm")) {
			return mixReagent(qs, DEMONS_BLOOD, 10, BLOOD_ROOT, 1, DEMONPLASM, parseTemp(event));
		} else if (event.startsWith("TempDemonicEssence")) {
			return mixReagent(qs, DEMONPLASM, 10, SULFUR, 1, DEMONIC_ESSENCE, parseTemp(event));
		} else if (event.startsWith("TempInfernoDust")) {
			return mixReagent(qs, INFERNIUM_ORE, 10, VOLCANIC_ASH, 1, INFERNO_DUST, parseTemp(event));
		} else if (event.startsWith("TempHellfireOil")) {
			return mixReagent(qs, FIRE_ESSENCE, 1, DEMONIC_ESSENCE, 1, HELLFIRE_OIL, parseTemp(event));
		}

		return event;
	}

	private int parseTemp(String event) {
		if (event.endsWith("1")) {
			return 1;
		}
		if (event.endsWith("2")) {
			return 2;
		}
		if (event.endsWith("3")) {
			return 3;
		}
		return 1;
	}

	private String mixReagent(QuestState qs, int item1, int count1, int item2, int count2, int resultItem, int temp) {
		if (qs.getQuestItemsCount(item1) < count1 || qs.getQuestItemsCount(item2) < count2) {
			return "NoItem.htm";
		}
		qs.takeItems(item1, count1);
		qs.takeItems(item2, count2);

		int chance = (temp == 1) ? 100 : (temp == 2 ? 45 : 15);
		int multiplier = (temp == 1) ? 1 : (temp == 2 ? 3 : 5);

		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			qs.giveItems(resultItem, multiplier);
			qs.playSound("SkillSound5.liquid_success_01");
			return "New.htm";
		} else {
			qs.playSound("SkillSound5.liquid_fail_01");
			return "New.htm";
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return "noquest";
		}

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == WESLEY) {
			if (cond == 0) {
				PlayerCharacter c = qs.getPlayerCharacter();
				if (c != null && c.level() >= 57) {
					return "30166-1.htm";
				} else {
					return "30166-2.htm";
				}
			} else {
				return "30166-3.htm";
			}
		} else if (npcId == URN) {
			if (qs.hasQuestItems(MIXING_STONE)) {
				return "urn.htm";
			} else {
				return "NoMixing.htm";
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
		int roll = ThreadLocalRandom.current().nextInt(100);

		if (npcId == HAMES_ORC_SHAMAN) {
			if (roll < 60) {
				qs.giveItems(QUICKSILVER, 1);
			} else {
				qs.giveItems(ROTTEN_BONE, 1);
			}
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == CRENDION) {
			if (roll < 40) {
				qs.giveItems(VOLCANIC_ASH, 1);
			} else {
				qs.giveItems(REAGENT_POUCH_1, 1);
			}
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALLATES_GUARDIAN) {
			if (roll < 70) {
				qs.giveItems(DEMONS_BLOOD, 1);
			} else if (roll < 90) {
				qs.giveItems(MOONSTONE_SHARD, 1);
			}
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == HALLATES_MAID) {
			if (roll < 70) {
				qs.giveItems(REAGENT_POUCH_2, 1);
			} else {
				qs.giveItems(QUICKSILVER, 1);
			}
			qs.playSound(QuestState.SOUND_ITEMGET);
		} else if (npcId == PLATINUM_TRIBE_SHAMAN) {
			if (roll < 40) {
				qs.giveItems(REAGENT_BOX, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == PLATINUM_GUARDIAN_SHAMAN) {
			if (roll < 50) {
				qs.giveItems(WYRMS_BLOOD, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			} else if (roll < 74) {
				qs.giveItems(LAVA_STONE, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if (npcId == GUARDIAN_OF_HOLY_GRAIL) {
			if (roll < 50) {
				qs.giveItems(REAGENT_POUCH_3, 1);
				qs.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}
