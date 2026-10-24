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

	public void addItem(ItemInstance item) {
		add(item);
	}

	/** Remove o item (desequipando se preciso); devolve os itens cujo estado mudou alem do removido. */
	public Set<ItemInstance> remove(ItemInstance item) {
		Set<ItemInstance> changed = unequip(item);
		changed.remove(item);
		items.remove(item.objectId());
		return changed;
	}

	public Set<ItemInstance> remove(ItemInstance item, int count) {
		if (item == null) {
			return Collections.emptySet();
		}
		if (item.count() > count) {
			item.count(item.count() - count);
			return Set.of(item);
		}
		return remove(item);
	}

	public int getItemCount(int itemId) {
		return items.values().stream()
				.filter(i -> i.itemId() == itemId)
				.mapToInt(ItemInstance::count)
				.sum();
	}

	public int count(int itemId) {
		return getItemCount(itemId);
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

	public void addAdena(int count) {
		if (count <= 0) return;
		var opt = byItemId(ItemTemplate.ADENA_ID);
		if (opt.isPresent()) {
			opt.get().count(opt.get().count() + count);
		} else {
			ItemTemplate dummy = ItemTemplate.etc(ItemTemplate.ADENA_ID, ItemTemplate.ADENA_ID, "Adena", "asset", "asset", 0, "none", 0, false, false, false, false);
			add(new ItemInstance(0x40000000 + ItemTemplate.ADENA_ID, dummy, ownerId, count));
		}
	}

	public synchronized void addItem(int itemId, int count) {
		if (count <= 0) return;
		var opt = byItemId(itemId);
		if (opt.isPresent()) {
			opt.get().count(opt.get().count() + count);
		} else {
			ItemTemplate dummy = ItemTemplate.etc(itemId, itemId, "Item_" + itemId, "etcitem", "none", 0, "none", 0, false, false, true, false);
			int objId = 0x40000000 + (itemId * 31) + java.util.concurrent.ThreadLocalRandom.current().nextInt(10000);
			add(new ItemInstance(objId, dummy, ownerId, count));
		}
	}

	public int currentLoad() {
		long load = 0;
		for (ItemInstance i : items.values()) {
			if (i.itemId() == ItemTemplate.ADENA_ID || (i.template() != null && i.template().isQuestItem())) {
				continue;
			}
			if (i.template() != null) {
				load += (long) i.template().weight() * i.count();
			}
		}
		return (int) Math.min(Integer.MAX_VALUE, load);
	}

	public ItemInstance paperdoll(int slot) {
		return (slot >= 0 && slot < PAPERDOLL_SLOTS) ? paperdoll[slot] : null;
	}

	public Paperdoll paperdollView() {
		int[] o = new int[PAPERDOLL_SLOTS];
		int[] i = new int[PAPERDOLL_SLOTS];
		int[] e = new int[PAPERDOLL_SLOTS];
		for (int s = 0; s < PAPERDOLL_SLOTS; s++) {
			if (paperdoll[s] != null) {
				o[s] = paperdoll[s].objectId();
				i[s] = paperdoll[s].template().displayId();
				e[s] = paperdoll[s].enchant();
			}
		}
		if (paperdoll[HAIR] == null && paperdoll[HAIRALL] != null) {
			o[HAIR] = paperdoll[HAIRALL].objectId();
			i[HAIR] = paperdoll[HAIRALL].template().displayId();
			e[HAIR] = paperdoll[HAIRALL].enchant();
		}
		return new Paperdoll(o, i, e);
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
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
					set(CHEST, null, changed);
				}
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
				if (!item.template().isArrow()) {
					if (paperdoll[LRHAND] != null) {
						set(LRHAND, null, changed);
						set(RHAND, null, changed);
					}
					if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
						set(CHEST, null, changed);
					}
				}
				set(LHAND, item, changed);
			}
			case SLOT_R_HAND -> {
				if (paperdoll[LRHAND] != null) {
					set(LRHAND, null, changed);
					set(LHAND, null, changed);
				}
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
					set(CHEST, null, changed);
				}
				set(RHAND, item, changed);
			}
			case SLOT_LR_EAR, SLOT_L_EAR, SLOT_R_EAR -> set(pair(LEAR, REAR, item), item, changed);
			case SLOT_LR_FINGER, SLOT_L_FINGER, SLOT_R_FINGER -> set(pair(LFINGER, RFINGER, item), item, changed);
			case SLOT_NECK -> set(NECK, item, changed);
			case SLOT_FULL_ARMOR -> {
				set(LEGS, null, changed);
				set(CHEST, item, changed);
			}
			case SLOT_CHEST -> set(CHEST, item, changed);
			case SLOT_ALLDRESS -> {
				set(LEGS, null, changed);
				set(LHAND, null, changed);
				set(RHAND, null, changed);
				set(LRHAND, null, changed);
				set(HEAD, null, changed);
				set(FEET, null, changed);
				set(GLOVES, null, changed);
				set(CHEST, item, changed);
			}
			case SLOT_LEGS -> {
				if (paperdoll[CHEST] != null && (paperdoll[CHEST].template().bodyPart() == SLOT_FULL_ARMOR
						|| paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS)) {
					set(CHEST, null, changed);
				}
				set(LEGS, item, changed);
			}
			case SLOT_FEET -> {
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
					set(CHEST, null, changed);
				}
				set(FEET, item, changed);
			}
			case SLOT_GLOVES -> {
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
					set(CHEST, null, changed);
				}
				set(GLOVES, item, changed);
			}
			case SLOT_HEAD -> {
				if (paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
					set(CHEST, null, changed);
				}
				set(HEAD, item, changed);
			}
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

	/** Resolve o item equipado que ocupa a mascara de bodyPart informada (incluindo slots compostos como fullarmor/alldress/hairall/lrhand). */
	public ItemInstance resolveEquippedByBodyPart(int bodyPart) {
		int slot = paperdollIndex(bodyPart);
		if (slot < 0) {
			return null;
		}
		ItemInstance item = paperdoll[slot];
		if (item == null) {
			if (bodyPart == SLOT_LR_EAR) {
				item = paperdoll[REAR];
			} else if (bodyPart == SLOT_LR_FINGER) {
				item = paperdoll[RFINGER];
			} else if (bodyPart == SLOT_LEGS && paperdoll[CHEST] != null
					&& (paperdoll[CHEST].template().bodyPart() == SLOT_FULL_ARMOR
							|| paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS)) {
				item = paperdoll[CHEST];
			} else if ((bodyPart == SLOT_FEET || bodyPart == SLOT_GLOVES || bodyPart == SLOT_HEAD
					|| bodyPart == SLOT_R_HAND || bodyPart == SLOT_L_HAND || bodyPart == SLOT_LR_HAND)
					&& paperdoll[CHEST] != null && paperdoll[CHEST].template().bodyPart() == SLOT_ALLDRESS) {
				item = paperdoll[CHEST];
			} else if (bodyPart == SLOT_L_HAND && paperdoll[LRHAND] != null) {
				item = paperdoll[LRHAND];
			} else if ((bodyPart == SLOT_HAIR || bodyPart == SLOT_FACE) && paperdoll[HAIRALL] != null) {
				item = paperdoll[HAIRALL];
			}
		}
		return item;
	}

	/** RequestUnEquipItem: o cliente manda a mascara do body part. */
	public Set<ItemInstance> unequipBodyPart(int bodyPart) {
		ItemInstance item = resolveEquippedByBodyPart(bodyPart);
		if (item == null) {
			return new LinkedHashSet<>();
		}
		return unequip(item);
	}

	private int pair(int first, int second, ItemInstance newItem) {
		if (paperdoll[first] == null) {
			return first;
		}
		if (paperdoll[second] == null) {
			return second;
		}
		// Se ja possui o mesmo item equipado em um dos slots, substitui o outro slot
		if (paperdoll[second].itemId() == newItem.itemId()) {
			return first;
		}
		if (paperdoll[first].itemId() == newItem.itemId()) {
			return second;
		}
		return first;
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
