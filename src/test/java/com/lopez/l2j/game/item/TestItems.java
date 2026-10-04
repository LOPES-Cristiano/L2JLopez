package com.lopez.l2j.game.item;

import com.lopez.l2j.game.item.ItemTemplateTable.CreationItem;
import java.util.List;

/** Templates reais (valores das tabelas do legado) para testes sem banco. */
public final class TestItems {

	private TestItems() {
	}

	public static final int DAGGER = 10;
	public static final int APPRENTICE_WAND = 6;
	public static final int SQUIRE_SWORD = 2369;
	public static final int BOW = 14;
	public static final int BRANDISH = 1333;
	public static final int SQUIRE_SHIRT = 1146;
	public static final int SQUIRE_PANTS = 1147;
	public static final int APPRENTICE_TUNIC = 425;
	public static final int APPRENTICE_STOCKINGS = 461;
	public static final int APPRENTICE_EARRING = 112;
	public static final int RING_OF_KNOWLEDGE = 875;
	public static final int NECKLACE_OF_MAGIC = 118;
	public static final int WOODEN_ARROW = 17;
	public static final int ADENA = 57;
	public static final int TUTORIAL_GUIDE = 5588;
	public static final int HEALING_POTION = 1060;
	/** Sinteticos: nao precisamos do id real, so do body part. */
	public static final int TEST_ROBE = 90001;
	public static final int TEST_SHIELD = 90002;

	public static List<ItemTemplate> templates() {
		return List.of(
				ItemTemplate.weapon(DAGGER, DAGGER, "Dagger", "rhand", "dagger", 1160, "none", 5, 5, 433, 12, 0, 0,
						true, true, true, true),
				ItemTemplate.weapon(APPRENTICE_WAND, APPRENTICE_WAND, "Apprentice's Wand", "rhand", "blunt", 1350,
						"none", 5, 7, 379, 4, 0, 0, true, true, true, true),
				ItemTemplate.weapon(SQUIRE_SWORD, SQUIRE_SWORD, "Squire's Sword", "rhand", "sword", 1600, "none", 6, 5,
						379, 8, 0, 0, true, true, true, true),
				ItemTemplate.weapon(BOW, BOW, "Bow", "lrhand", "bow", 1930, "none", 23, 9, 293, 12, 0, 0, true, true,
						true, true),
				ItemTemplate.weapon(BRANDISH, BRANDISH, "Brandish", "lrhand", "bigsword", 2250, "none", 21, 12, 325, 8,
						0, 0, true, true, true, true),
				ItemTemplate.weapon(TEST_SHIELD, TEST_SHIELD, "Test Shield", "lhand", "none", 1000, "none", 0, 0, 0, 0,
						50, 0, true, true, true, true),
				ItemTemplate.armor(SQUIRE_SHIRT, SQUIRE_SHIRT, "Squire's Shirt", "chest", "light", 3301, "none", 33, 0,
						0, true, true, true, true),
				ItemTemplate.armor(SQUIRE_PANTS, SQUIRE_PANTS, "Squire's Pants", "legs", "light", 1750, "none", 20, 0,
						0, true, true, true, true),
				ItemTemplate.armor(APPRENTICE_TUNIC, APPRENTICE_TUNIC, "Apprentice's Tunic", "chest", "magic", 2150,
						"none", 17, 0, 0, true, true, true, true),
				ItemTemplate.armor(APPRENTICE_STOCKINGS, APPRENTICE_STOCKINGS, "Apprentice's Stockings", "legs",
						"magic", 1100, "none", 10, 0, 0, true, true, true, true),
				ItemTemplate.armor(APPRENTICE_EARRING, APPRENTICE_EARRING, "Apprentice's Earring", "rear,lear", "none",
						150, "none", 0, 11, 0, true, true, true, true),
				ItemTemplate.armor(RING_OF_KNOWLEDGE, RING_OF_KNOWLEDGE, "Ring of Knowledge", "rfinger,lfinger",
						"none", 150, "none", 0, 9, 0, true, true, true, true),
				ItemTemplate.armor(NECKLACE_OF_MAGIC, NECKLACE_OF_MAGIC, "Necklace of Magic", "neck", "none", 150,
						"none", 0, 15, 0, true, true, true, true),
				ItemTemplate.armor(TEST_ROBE, TEST_ROBE, "Test Robe", "fullarmor", "magic", 3000, "none", 40, 0, 0,
						true, true, true, true),
				ItemTemplate.etc(WOODEN_ARROW, WOODEN_ARROW, "Wooden Arrow", "arrow", "stackable", 6, "none", 0, true,
						true, true, true),
				ItemTemplate.etc(ADENA, ADENA, "Adena", "none", "asset", 0, "none", 0, true, true, true, true),
				ItemTemplate.etc(TUTORIAL_GUIDE, TUTORIAL_GUIDE, "Tutorial Guide", "none", "normal", 10, "none", 0,
						true, true, true, true),
				ItemTemplate.etc(HEALING_POTION, HEALING_POTION, "Lesser Healing Potion", "potion", "stackable", 5,
						"none", 0, true, true, true, true));
	}

	/** Recorte de char_creation_items (guerreiro humano e mago humano). */
	public static List<CreationItem> creation() {
		return List.of(new CreationItem(-1, TUTORIAL_GUIDE, 1, false), new CreationItem(0, DAGGER, 1, false),
				new CreationItem(0, SQUIRE_SHIRT, 1, true), new CreationItem(0, SQUIRE_PANTS, 1, true),
				new CreationItem(0, SQUIRE_SWORD, 1, true), new CreationItem(10, APPRENTICE_WAND, 1, true),
				new CreationItem(10, APPRENTICE_TUNIC, 1, true), new CreationItem(10, APPRENTICE_STOCKINGS, 1, true),
				new CreationItem(0, 99999, 1, true)); // item inexistente: ignorado como no legado
	}

	public static ItemTemplateTable table() {
		return ItemTemplateTable.of(templates(), creation());
	}
}
