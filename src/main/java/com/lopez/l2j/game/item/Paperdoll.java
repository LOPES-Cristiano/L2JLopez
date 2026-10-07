package com.lopez.l2j.game.item;

import java.util.Arrays;

/** Foto imutavel do paperdoll (object id + item id + enchant por slot) usada pelos pacotes de aparencia. */
public record Paperdoll(int[] objectIds, int[] itemIds, int[] enchants) {

	public static final Paperdoll EMPTY = new Paperdoll(new int[ItemSlots.PAPERDOLL_SLOTS],
			new int[ItemSlots.PAPERDOLL_SLOTS], new int[ItemSlots.PAPERDOLL_SLOTS]);

	public Paperdoll {
		if (objectIds.length != ItemSlots.PAPERDOLL_SLOTS || itemIds.length != ItemSlots.PAPERDOLL_SLOTS) {
			throw new IllegalArgumentException("paperdoll precisa de " + ItemSlots.PAPERDOLL_SLOTS + " slots");
		}
		if (enchants == null) {
			enchants = new int[ItemSlots.PAPERDOLL_SLOTS];
		} else if (enchants.length != ItemSlots.PAPERDOLL_SLOTS) {
			throw new IllegalArgumentException("enchants precisa de " + ItemSlots.PAPERDOLL_SLOTS + " slots");
		}
		objectIds = objectIds.clone();
		itemIds = itemIds.clone();
		enchants = enchants.clone();
	}

	public Paperdoll(int[] objectIds, int[] itemIds) {
		this(objectIds, itemIds, new int[ItemSlots.PAPERDOLL_SLOTS]);
	}

	public int objectId(int slot) {
		return objectIds[slot];
	}

	public int itemId(int slot) {
		return itemIds[slot];
	}

	public int enchant(int slot) {
		return (slot >= 0 && slot < enchants.length) ? enchants[slot] : 0;
	}

	/** Linha persistida (loc=PAPERDOLL): armas de duas maos ficam gravadas no slot LRHAND e ocupam RHAND. */
	public record Entry(int slot, int objectId, int itemId, int enchant) {
		public Entry(int slot, int objectId, int itemId) {
			this(slot, objectId, itemId, 0);
		}
	}

	public static Paperdoll of(Iterable<Entry> entries) {
		int[] o = new int[ItemSlots.PAPERDOLL_SLOTS];
		int[] i = new int[ItemSlots.PAPERDOLL_SLOTS];
		int[] e = new int[ItemSlots.PAPERDOLL_SLOTS];
		for (Entry entry : entries) {
			if (entry.slot() < 0 || entry.slot() >= ItemSlots.PAPERDOLL_SLOTS) {
				continue;
			}
			o[entry.slot()] = entry.objectId();
			i[entry.slot()] = entry.itemId();
			e[entry.slot()] = entry.enchant();
			if (entry.slot() == ItemSlots.LRHAND && o[ItemSlots.RHAND] == 0) {
				o[ItemSlots.RHAND] = entry.objectId();
				i[ItemSlots.RHAND] = entry.itemId();
				e[ItemSlots.RHAND] = entry.enchant();
			}
		}
		return new Paperdoll(o, i, e);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Paperdoll p && Arrays.equals(objectIds, p.objectIds) && Arrays.equals(itemIds, p.itemIds) && Arrays.equals(enchants, p.enchants);
	}

	@Override
	public int hashCode() {
		return 31 * (31 * Arrays.hashCode(objectIds) + Arrays.hashCode(itemIds)) + Arrays.hashCode(enchants);
	}

	@Override
	public String toString() {
		return "Paperdoll" + Arrays.toString(itemIds);
	}
}
