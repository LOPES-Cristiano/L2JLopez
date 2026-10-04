package com.lopez.l2j.game.item;

import java.util.Arrays;

/** Foto imutavel do paperdoll (object id + item id por slot) usada pelos pacotes de aparencia. */
public record Paperdoll(int[] objectIds, int[] itemIds) {

	public static final Paperdoll EMPTY = new Paperdoll(new int[ItemSlots.PAPERDOLL_SLOTS],
			new int[ItemSlots.PAPERDOLL_SLOTS]);

	public Paperdoll {
		if (objectIds.length != ItemSlots.PAPERDOLL_SLOTS || itemIds.length != ItemSlots.PAPERDOLL_SLOTS) {
			throw new IllegalArgumentException("paperdoll precisa de " + ItemSlots.PAPERDOLL_SLOTS + " slots");
		}
		objectIds = objectIds.clone();
		itemIds = itemIds.clone();
	}

	public int objectId(int slot) {
		return objectIds[slot];
	}

	public int itemId(int slot) {
		return itemIds[slot];
	}

	/** Linha persistida (loc=PAPERDOLL): armas de duas maos ficam gravadas no slot LRHAND e ocupam RHAND. */
	public record Entry(int slot, int objectId, int itemId) {
	}

	public static Paperdoll of(Iterable<Entry> entries) {
		int[] o = new int[ItemSlots.PAPERDOLL_SLOTS];
		int[] i = new int[ItemSlots.PAPERDOLL_SLOTS];
		for (Entry e : entries) {
			if (e.slot() < 0 || e.slot() >= ItemSlots.PAPERDOLL_SLOTS) {
				continue;
			}
			o[e.slot()] = e.objectId();
			i[e.slot()] = e.itemId();
			if (e.slot() == ItemSlots.LRHAND && o[ItemSlots.RHAND] == 0) {
				o[ItemSlots.RHAND] = e.objectId();
				i[ItemSlots.RHAND] = e.itemId();
			}
		}
		return new Paperdoll(o, i);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Paperdoll p && Arrays.equals(objectIds, p.objectIds) && Arrays.equals(itemIds, p.itemIds);
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(objectIds) + Arrays.hashCode(itemIds);
	}

	@Override
	public String toString() {
		return "Paperdoll" + Arrays.toString(itemIds);
	}
}
