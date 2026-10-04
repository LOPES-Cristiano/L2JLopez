package com.lopez.l2j.game.service;

import static com.lopez.l2j.game.item.TestItems.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.ObjectIdFactory;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

	final InMemoryItemRepository repo = new InMemoryItemRepository();
	final InventoryService service = new InventoryService(TestItems.table(), repo,
			ObjectIdFactory.sequential(0x20000000), 10_000_000);

	@Test
	void starterItemsFollowCharCreationItems() {
		var inv = service.giveStarterItems(7, 0);
		// guia (-1) + dagger + shirt + pants + sword + adena; item inexistente ignorado
		assertEquals(6, inv.size());
		assertEquals(10_000_000, inv.adena());
		assertEquals(SQUIRE_SWORD, inv.paperdoll(ItemSlots.RHAND).itemId());
		assertEquals(SQUIRE_SHIRT, inv.paperdoll(ItemSlots.CHEST).itemId());
		assertEquals(SQUIRE_PANTS, inv.paperdoll(ItemSlots.LEGS).itemId());
		assertFalse(inv.byItemId(DAGGER).orElseThrow().isEquipped(), "dagger vem com equipped=false");
		assertEquals(6, repo.ofOwner(7).size());
		assertEquals(3, repo.ofOwner(7).stream().filter(r -> r.loc().equals("PAPERDOLL")).count());

		var mage = service.giveStarterItems(8, 10);
		assertEquals(APPRENTICE_WAND, mage.paperdoll(ItemSlots.RHAND).itemId());
		assertEquals(APPRENTICE_TUNIC, mage.paperdoll(ItemSlots.CHEST).itemId());
	}

	@Test
	void loadRestoresEquipmentAndPaperdoll() {
		service.giveStarterItems(7, 0);
		var inv = service.load(7);
		assertEquals(6, inv.size());
		assertEquals(SQUIRE_SWORD, inv.paperdoll(ItemSlots.RHAND).itemId());
		assertEquals(3, inv.equipped().size());
		var paperdoll = service.paperdoll(7);
		assertEquals(SQUIRE_SHIRT, paperdoll.itemId(ItemSlots.CHEST));
		assertEquals(0, service.paperdoll(999).itemId(ItemSlots.CHEST));
	}

	@Test
	void stackablesMergeAndNonStackablesSplit() {
		var inv = service.giveStarterItems(7, 0);
		var first = service.addItem(inv, HEALING_POTION, 5, "test");
		assertTrue(first.created());
		var second = service.addItem(inv, HEALING_POTION, 3, "test");
		assertFalse(second.created());
		assertEquals(8, second.item().count());
		assertEquals(8, repo.rows.get(second.item().objectId()).count());

		int before = inv.size();
		service.addItem(inv, DAGGER, 2, "test");
		assertEquals(before + 2, inv.size());
		assertNull(service.addItem(inv, 424242, 1, "test"));
	}

	@Test
	void toggleEquipPersistsChanges() {
		var inv = service.giveStarterItems(7, 0);
		var dagger = inv.byItemId(DAGGER).orElseThrow();
		var sword = inv.byItemId(SQUIRE_SWORD).orElseThrow();

		var r = service.toggleEquip(inv, dagger.objectId());
		assertTrue(r.ok() && r.equipped());
		assertTrue(r.changed().contains(sword), "espada saiu da mao");
		assertEquals("PAPERDOLL", repo.rows.get(dagger.objectId()).loc());
		assertEquals("INVENTORY", repo.rows.get(sword.objectId()).loc());

		var off = service.toggleEquip(inv, dagger.objectId());
		assertFalse(off.equipped());
		assertEquals("INVENTORY", repo.rows.get(dagger.objectId()).loc());

		assertFalse(service.toggleEquip(inv, inv.byItemId(TUTORIAL_GUIDE).orElseThrow().objectId()).ok());
		assertFalse(service.unequipBodyPart(inv, ItemSlots.SLOT_R_HAND).ok(), "mao ja vazia");
		assertTrue(service.unequipBodyPart(inv, ItemSlots.SLOT_CHEST).ok());
	}

	@Test
	void deleteAllRemovesOwnerRows() {
		service.giveStarterItems(7, 0);
		service.giveStarterItems(8, 10);
		service.deleteAll(7);
		assertTrue(repo.ofOwner(7).isEmpty());
		assertFalse(repo.ofOwner(8).isEmpty());
	}
}
