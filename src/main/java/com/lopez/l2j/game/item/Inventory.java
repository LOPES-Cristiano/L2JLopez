package com.lopez.l2j.game.item;

import static com.lopez.l2j.game.item.ItemSlots.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Inventario + paperdoll de um personagem (porta de Inventory/PcInventory sem listeners de stats/skills, que
 * chegam com o sistema de combate). Os metodos de equipar devolvem os itens alterados para o chamador persistir
 * e montar o InventoryUpdate. Nao e thread-safe: acesso pela thread da conexao do dono.
 */
public final class Inventory {

	private final int ownerId;
	private final Map<Integer, ItemInstance> items = new LinkedHashMap<>();
	private final ItemInstance[] paperdoll = new ItemInstance[PAPERDOLL_SLOTS];

	public Inventory(int ownerId) {
		this.ownerId = ownerId;
	}

	public int ownerId() {
		return ownerId;
	}

	public Collection<ItemInstance> items() {
		return Collections.unmodifiableCollection(items.values());
	}

	public int size() {
		return items.size();
	}

	public Optional<ItemInstance> byObjectId(int objectId) {
		return Optional.ofNullable(items.get(objectId));
	}

	public Optional<ItemInstance> byItemId(int itemId) {
		return items.values().stream().filter(i -> i.itemId() == itemId).findFirst();
	}

	/** Adiciona sem empilhar (restauracao ou item ja resolvido pelo servico). */
	public void add(ItemInstance item) {
		item.ownerId(ownerId);
		items.put(item.objectId(), item);
	}

	/** Remove o item (desequipando se preciso); devolve os itens cujo estado mudou alem do removido. */
	public Set<ItemInstance> remove(ItemInstance item) {
		Set<ItemInstance> changed = unequip(item);
		changed.remove(item);
		items.remove(item.objectId());
		return changed;
	}

	public int getItemCount(int itemId) {
		return items.values().stream()
				.filter(i -> i.itemId() == itemId)
				.mapToInt(ItemInstance::count)
				.sum();
	}

	public synchronized boolean destroyItemByItemId(int itemId, int count) {
		if (count <= 0) {
			return true;
		}
		if (getItemCount(itemId) < count) {
			return false;
		}
		int remaining = count;
		var iterator = items.values().iterator();
		while (iterator.hasNext() && remaining > 0) {
			var it = iterator.next();
			if (it.itemId() == itemId) {
				if (it.count() <= remaining) {
					remaining -= it.count();
					it.count(0);
					iterator.remove();
				} else {
					it.count(it.count() - remaining);
					remaining = 0;
				}
			}
		}
		return remaining == 0;
	}

	public long adena() {
		return byItemId(ItemTemplate.ADENA_ID).map(ItemInstance::count).orElse(0);
	}

	public int currentLoad() {
		long load = 0;
		for (ItemInstance i : items.values()) {
			load += (long) i.template().weight() * i.count();
		}
		return (int) Math.min(Integer.MAX_VALUE, load);
	}

	public ItemInstance paperdoll(int slot) {
		return (slot >= 0 && slot < PAPERDOLL_SLOTS) ? paperdoll[slot] : null;
	}

	public Paperdoll paperdollView() {
		int[] o = new int[PAPERDOLL_SLOTS];
		int[] i = new int[PAPERDOLL_SLOTS];
		for (int s = 0; s < PAPERDOLL_SLOTS; s++) {
			if (paperdoll[s] != null) {
				o[s] = paperdoll[s].objectId();
				i[s] = paperdoll[s].template().displayId();
			}
		}
		return new Paperdoll(o, i);
	}

	public List<ItemInstance> equipped() {
		List<ItemInstance> out = new ArrayList<>();
		for (ItemInstance i : paperdoll) {
			if (i != null && !out.contains(i)) {
				out.add(i);
			}
		}
		return out;
	}

	public List<ItemInstance> equippedItems() {
		return equipped();
	}

	public boolean isEquipped(int itemId) {
		for (ItemInstance i : paperdoll) {
			if (i != null && i.itemId() == itemId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Equipa seguindo as regras de Inventory.equipItem do legado (duas maos libera escudo, fullarmor libera
	 * calca, brincos/aneis preenchem esquerda e depois direita...). Item precisa estar no inventario.
	 */
	public Set<ItemInstance> equip(ItemInstance item) {
		Set<ItemInstance> changed = new LinkedHashSet<>();
		if (!items.containsKey(item.objectId()) || !item.template().isEquipable()) {
			return changed;
		}
		switch (item.template().bodyPart()) {
			case SLOT_LR_HAND -> {
				set(LHAND, null, changed);
				set(RHAND, null, changed);
				set(RHAND, item, changed);
				set(LRHAND, item, changed);
				if ("bow".equals(item.template().subType())) {
					items.values().stream().filter(i -> i.template().isArrow()
							&& i.template().crystalType().equals(item.template().crystalType())).findFirst()
							.ifPresent(arrow -> set(LHAND, arrow, changed));
				}
			}
			case SLOT_L_HAND -> {
				if (!item.template().isArrow() && paperdoll[LRHAND] != null) {
					set(LRHAND, null, changed);
					set(RHAND, null, changed);
				}
				set(LHAND, item, changed);
			}
			case SLOT_R_HAND -> {
				if (paperdoll[LRHAND] != null) {
					set(LRHAND, null, changed);
					set(LHAND, null, changed);
				}
				set(RHAND, item, changed);
			}
			case SLOT_LR_EAR, SLOT_L_EAR, SLOT_R_EAR -> set(pair(LEAR, REAR), item, changed);
			case SLOT_LR_FINGER, SLOT_L_FINGER, SLOT_R_FINGER -> set(pair(LFINGER, RFINGER), item, changed);
			case SLOT_NECK -> set(NECK, item, changed);
			case SLOT_FULL_ARMOR -> {
				set(LEGS, null, changed);
				set(CHEST, item, changed);
			}
			case SLOT_CHEST, SLOT_ALLDRESS -> set(CHEST, item, changed);
			case SLOT_LEGS -> {
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_FULL_ARMOR) {
					set(CHEST, null, changed);
				}
				set(LEGS, item, changed);
			}
			case SLOT_FEET -> set(FEET, item, changed);
			case SLOT_GLOVES -> set(GLOVES, item, changed);
			case SLOT_HEAD -> set(HEAD, item, changed);
			case SLOT_HAIR -> {
				set(HAIRALL, null, changed);
				set(HAIR, item, changed);
			}
			case SLOT_FACE -> {
				set(HAIRALL, null, changed);
				set(FACE, item, changed);
			}
			case SLOT_HAIRALL -> {
				set(HAIR, null, changed);
				set(FACE, null, changed);
				set(HAIRALL, item, changed);
			}
			case SLOT_UNDERWEAR -> set(UNDER, item, changed);
			case SLOT_BACK -> set(BACK, item, changed);
			default -> {
				// mascara combinada sem slot (ex.: "chest,legs" nao existe no Interlude)
			}
		}
		return changed;
	}

	/** Tira o item de todos os slots que ele ocupa. */
	public Set<ItemInstance> unequip(ItemInstance item) {
		Set<ItemInstance> changed = new LinkedHashSet<>();
		for (int s = 0; s < PAPERDOLL_SLOTS; s++) {
			if (paperdoll[s] == item) {
				set(s, null, changed);
			}
		}
		if (item.template().bodyPart() == SLOT_LR_HAND && paperdoll[LHAND] != null
				&& paperdoll[LHAND].template().isArrow()) {
			set(LHAND, null, changed);
		}
		return changed;
	}

	/** RequestUnEquipItem: o cliente manda a mascara do body part. */
	public Set<ItemInstance> unequipBodyPart(int bodyPart) {
		int slot = paperdollIndex(bodyPart);
		if (slot < 0 || paperdoll[slot] == null) {
			return new LinkedHashSet<>();
		}
		return unequip(paperdoll[slot]);
	}

	private int pair(int first, int second) {
		if (paperdoll[first] == null) {
			return first;
		}
		return paperdoll[second] == null ? second : first;
	}

	private void set(int slot, ItemInstance item, Set<ItemInstance> changed) {
		ItemInstance old = paperdoll[slot];
		if (old == item) {
			return;
		}
		if (old != null) {
			paperdoll[slot] = null;
			if (!occupiesAnySlot(old)) {
				old.location(ItemInstance.Location.INVENTORY, 0);
			}
			changed.add(old);
		}
		if (item != null) {
			paperdoll[slot] = item;
			item.location(ItemInstance.Location.PAPERDOLL, slot);
			changed.add(item);
		}
	}

	private boolean occupiesAnySlot(ItemInstance item) {
		for (ItemInstance i : paperdoll) {
			if (i == item) {
				return true;
			}
		}
		return false;
	}
}
