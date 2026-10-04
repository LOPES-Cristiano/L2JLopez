package com.lopez.l2j.game.item;

import static com.lopez.l2j.game.item.TestItems.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryTest {

	final ItemTemplateTable table = TestItems.table();
	final AtomicInteger ids = new AtomicInteger(1000);
	Inventory inv;

	@BeforeEach
	void setUp() {
		inv = new Inventory(1);
	}

	ItemInstance give(int itemId) {
		var item = new ItemInstance(ids.getAndIncrement(), table.get(itemId).orElseThrow(), 1, 1);
		inv.add(item);
		return item;
	}

	@Test
	void templateTypesFollowLegacyItemTable() {
		ItemTemplate dagger = table.get(DAGGER).orElseThrow();
		assertEquals(0, dagger.type1());
		assertEquals(0, dagger.type2());
		assertEquals(ItemSlots.SLOT_R_HAND, dagger.bodyPart());

		ItemTemplate shield = table.get(TEST_SHIELD).orElseThrow();
		assertEquals(ItemTemplate.TYPE1_SHIELD_ARMOR, shield.type1());
		assertEquals(ItemTemplate.TYPE2_SHIELD_ARMOR, shield.type2());

		ItemTemplate earring = table.get(APPRENTICE_EARRING).orElseThrow();
		assertEquals(ItemTemplate.TYPE2_ACCESSORY, earring.type2());
		assertEquals(ItemSlots.SLOT_LR_EAR, earring.bodyPart());

		ItemTemplate shirt = table.get(SQUIRE_SHIRT).orElseThrow();
		assertEquals(1, shirt.type1());
		assertEquals(1, shirt.type2());

		ItemTemplate adena = table.get(ADENA).orElseThrow();
		assertEquals(4, adena.type1());
		assertEquals(ItemTemplate.TYPE2_MONEY, adena.type2());
		assertTrue(adena.stackable());
		assertFalse(adena.isEquipable());

		ItemTemplate arrow = table.get(WOODEN_ARROW).orElseThrow();
		assertEquals(ItemSlots.SLOT_L_HAND, arrow.bodyPart());
		assertTrue(arrow.isArrow());

		assertFalse(table.get(TUTORIAL_GUIDE).orElseThrow().stackable());
		assertTrue(table.get(HEALING_POTION).orElseThrow().stackable());
	}

	@Test
	void petItemsAreNotPlayerEquipable() {
		var wolfArmor = ItemTemplate.armor(1, 1, "Wolf Armor", "wolf", "pet", 1, "none", 1, 0, 0, true, true, true,
				true);
		assertEquals(ItemTemplate.TYPE2_PET_WOLF, wolfArmor.type2());
		assertEquals(ItemSlots.SLOT_CHEST, wolfArmor.bodyPart());
		assertFalse(wolfArmor.isEquipable());
		var striderWeapon = ItemTemplate.weapon(2, 2, "Strider Fang", "strider", "pet", 1, "none", 1, 1, 1, 1, 0, 0,
				true, true, true, true);
		assertEquals(ItemTemplate.TYPE2_PET_STRIDER, striderWeapon.type2());
		assertFalse(striderWeapon.isEquipable());
	}

	@Test
	void twoHandedWeaponOccupiesBothHandsAndReleasesShield() {
		var shield = give(TEST_SHIELD);
		var sword = give(SQUIRE_SWORD);
		inv.equip(shield);
		inv.equip(sword);
		assertSame(sword, inv.paperdoll(ItemSlots.RHAND));
		assertSame(shield, inv.paperdoll(ItemSlots.LHAND));

		var brandish = give(BRANDISH);
		var changed = inv.equip(brandish);
		assertSame(brandish, inv.paperdoll(ItemSlots.RHAND));
		assertSame(brandish, inv.paperdoll(ItemSlots.LRHAND));
		assertNull(inv.paperdoll(ItemSlots.LHAND));
		assertTrue(changed.contains(shield) && changed.contains(sword) && changed.contains(brandish));
		assertFalse(shield.isEquipped());
		assertFalse(sword.isEquipped());
		assertEquals(ItemSlots.LRHAND, brandish.locationData(), "legado grava o slot LRHAND");

		inv.equip(shield);
		assertNull(inv.paperdoll(ItemSlots.RHAND), "escudo tira a arma de duas maos");
		assertNull(inv.paperdoll(ItemSlots.LRHAND));
		assertFalse(brandish.isEquipped());
	}

	@Test
	void bowPicksMatchingArrows() {
		var arrows = give(WOODEN_ARROW);
		var bow = give(BOW);
		inv.equip(bow);
		assertSame(arrows, inv.paperdoll(ItemSlots.LHAND));
		inv.unequip(bow);
		assertNull(inv.paperdoll(ItemSlots.LHAND), "flechas saem junto com o arco");
		assertFalse(arrows.isEquipped());
	}

	@Test
	void earringsAndRingsFillLeftThenRightThenReplaceLeft() {
		var e1 = give(APPRENTICE_EARRING);
		var e2 = give(APPRENTICE_EARRING);
		var e3 = give(APPRENTICE_EARRING);
		inv.equip(e1);
		inv.equip(e2);
		assertSame(e1, inv.paperdoll(ItemSlots.LEAR));
		assertSame(e2, inv.paperdoll(ItemSlots.REAR));
		inv.equip(e3);
		assertSame(e3, inv.paperdoll(ItemSlots.LEAR));
		assertFalse(e1.isEquipped());

		var r1 = give(RING_OF_KNOWLEDGE);
		inv.equip(r1);
		assertSame(r1, inv.paperdoll(ItemSlots.LFINGER));
	}

	@Test
	void fullArmorAndLegsAreExclusive() {
		var pants = give(SQUIRE_PANTS);
		var shirt = give(SQUIRE_SHIRT);
		inv.equip(pants);
		inv.equip(shirt);
		var robe = give(TEST_ROBE);
		inv.equip(robe);
		assertSame(robe, inv.paperdoll(ItemSlots.CHEST));
		assertNull(inv.paperdoll(ItemSlots.LEGS));
		inv.equip(pants);
		assertNull(inv.paperdoll(ItemSlots.CHEST), "calca tira a fullarmor");
		assertSame(pants, inv.paperdoll(ItemSlots.LEGS));
	}

	@Test
	void unequipByBodyPartMaskAndPaperdollView() {
		var sword = give(SQUIRE_SWORD);
		var neck = give(NECKLACE_OF_MAGIC);
		inv.equip(sword);
		inv.equip(neck);
		Paperdoll view = inv.paperdollView();
		assertEquals(SQUIRE_SWORD, view.itemId(ItemSlots.RHAND));
		assertEquals(sword.objectId(), view.objectId(ItemSlots.RHAND));
		assertEquals(NECKLACE_OF_MAGIC, view.itemId(ItemSlots.NECK));

		var changed = inv.unequipBodyPart(ItemSlots.SLOT_R_HAND);
		assertTrue(changed.contains(sword));
		assertFalse(sword.isEquipped());
		assertEquals(ItemInstance.Location.INVENTORY, sword.location());
		assertTrue(inv.unequipBodyPart(ItemSlots.SLOT_R_HAND).isEmpty());
		assertEquals(1, inv.equipped().size());
	}

	@Test
	void nonEquipableAndForeignItemsAreIgnored() {
		var guide = give(TUTORIAL_GUIDE);
		assertTrue(inv.equip(guide).isEmpty());
		var foreign = new ItemInstance(9, table.get(DAGGER).orElseThrow(), 2, 1);
		assertTrue(inv.equip(foreign).isEmpty(), "item fora do inventario");
	}

	@Test
	void loadIsWeightTimesCount() {
		var potions = give(HEALING_POTION);
		potions.count(10);
		give(DAGGER);
		assertEquals(5 * 10 + 1160, inv.currentLoad());
	}

	@Test
	void paperdollFromStoredRowsFillsRightHandForTwoHanded() {
		var p = Paperdoll.of(java.util.List.of(new Paperdoll.Entry(ItemSlots.LRHAND, 77, BRANDISH)));
		assertEquals(BRANDISH, p.itemId(ItemSlots.RHAND));
		assertEquals(77, p.objectId(ItemSlots.LRHAND));
	}

	@Test
	void bodyPartParsingCoversLegacyNames() {
		assertEquals(ItemSlots.SLOT_HAIRALL, ItemSlots.parseBodyPart("dhair"));
		assertEquals(ItemSlots.SLOT_UNDERWEAR, ItemSlots.parseBodyPart("shirt"));
		assertEquals(ItemSlots.SLOT_LR_FINGER, ItemSlots.parseBodyPart("rfinger,lfinger"));
		assertEquals(ItemSlots.SLOT_NONE, ItemSlots.parseBodyPart("???"));
		assertEquals(ItemSlots.RHAND, ItemSlots.paperdollIndex(ItemSlots.SLOT_LR_HAND));
		assertEquals(-1, ItemSlots.paperdollIndex(12345));
	}
}
