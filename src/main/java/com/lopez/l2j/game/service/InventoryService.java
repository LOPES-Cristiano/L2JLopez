package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.game.augmentation.Augmentation;
import com.lopez.l2j.game.augmentation.AugmentationRepository;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemRepository.StoredItem;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.item.Paperdoll;
import com.lopez.l2j.game.model.ObjectIdFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Regras de inventario com persistencia write-through: toda mudanca devolvida pelo {@link Inventory} e gravada
 * na hora (o legado acumulava e gravava no logout; aqui um crash nao perde itens).
 */
@Service
public class InventoryService {

	private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

	/** Resultado de UseItem/RequestUnEquipItem: itens alterados (para o InventoryUpdate). */
	public record EquipResult(ItemInstance item, boolean equipped, List<ItemInstance> changed) {
		public static final EquipResult NONE = new EquipResult(null, false, List.of());

		public boolean ok() {
			return item != null;
		}
	}

	/** Resultado de addItem: o item final e se ele e novo (InventoryUpdate add) ou modificado. */
	public record AddResult(ItemInstance item, boolean created) {
	}

	/** Resultado de consumeItem: o item alterado/removido e se foi totalmente removido. */
	public record ConsumeResult(ItemInstance item, boolean removed) {
	}

	private final ItemTemplateTable templates;
	private final ItemRepository repository;
	private final ObjectIdFactory ids;
	private final int startingAdena;
	private AugmentationRepository augmentationRepository;

	@Autowired
	public InventoryService(ItemTemplateTable templates, ItemRepository repository, ObjectIdFactory ids,
			ServerProperties properties, @Autowired(required = false) AugmentationRepository augmentationRepository) {
		this(templates, repository, ids, properties.game() == null ? 0 : properties.game().startingAdena());
		this.augmentationRepository = augmentationRepository;
	}

	public InventoryService(ItemTemplateTable templates, ItemRepository repository, ObjectIdFactory ids,
			ServerProperties properties) {
		this(templates, repository, ids, properties, null);
	}

	public InventoryService(ItemTemplateTable templates, ItemRepository repository, ObjectIdFactory ids,
			int startingAdena) {
		this.templates = templates;
		this.repository = repository;
		this.ids = ids;
		this.startingAdena = startingAdena;
	}

	public ItemTemplateTable templates() {
		return templates;
	}

	/** Inventory.restore: carrega itens e reequipa os que estavam no paperdoll. */
	public Inventory load(int ownerId) {
		Inventory inv = new Inventory(ownerId);
		List<ItemInstance> toEquip = new ArrayList<>();
		Map<Integer, Augmentation> augs = (augmentationRepository != null)
				? augmentationRepository.findByOwnerId(ownerId)
				: Map.of();
		for (StoredItem row : repository.findInventory(ownerId)) {
			var template = templates.get(row.itemId()).orElse(null);
			if (template == null) {
				log.warn("Item {} (objeto {}) do personagem {} sem template; ignorado", row.itemId(), row.objectId(),
						ownerId);
				continue;
			}
			ItemInstance item = new ItemInstance(row.objectId(), template, ownerId, row.count());
			item.enchant(row.enchant());
			item.customType1(row.customType1());
			item.customType2(row.customType2());
			item.mana(row.mana());
			if (!augs.isEmpty()) {
				var aug = augs.get(row.objectId());
				if (aug != null) {
					item.augmentation(aug);
				}
			}
			inv.add(item);
			if ("PAPERDOLL".equals(row.location())) {
				item.location(ItemInstance.Location.PAPERDOLL, row.locationData());
				toEquip.add(item);
			}
		}
		toEquip.sort(Comparator.comparingInt(ItemInstance::locationData));
		Set<ItemInstance> changed = new LinkedHashSet<>();
		for (ItemInstance item : toEquip) {
			item.location(ItemInstance.Location.INVENTORY, 0);
			changed.addAll(inv.equip(item));
		}
		// Normaliza linhas inconsistentes (ex.: dois itens no mesmo slot): o que nao reequipou volta ao inventario.
		for (ItemInstance item : toEquip) {
			if (!item.isEquipped()) {
				repository.update(item);
			}
		}
		return inv;
	}

	public Paperdoll paperdoll(int ownerId) {
		return Paperdoll.of(repository.findPaperdoll(ownerId));
	}

	/** CharacterCreate: itens de char_creation_items (equipando os marcados) + StartingAdena. */
	public Inventory giveStarterItems(int ownerId, int classId) {
		Inventory inv = new Inventory(ownerId);
		for (var row : templates.creationItems(classId)) {
			AddResult added = addItem(inv, row.itemId(), row.amount(), "Init");
			if (added != null && row.equipped() && added.item().template().isEquipable()) {
				persist(inv.equip(added.item()));
			}
		}
		int adena = startingAdena > 0 ? startingAdena : Config.STARTING_ADENA;
		if (adena > 0) {
			addItem(inv, ItemTemplate.ADENA_ID, adena, "Init");
		}
		if (Config.STARTING_AA > 0) {
			addItem(inv, 5575, Config.STARTING_AA, "Init");
		}
		if (Config.CUSTOM_STARTER_ITEMS_ENABLED) {
			boolean isMage = (classId >= 10 && classId <= 17) || (classId >= 25 && classId <= 30)
					|| (classId >= 38 && classId <= 43) || (classId >= 49 && classId <= 52);
			String customItems = isMage ? Config.STARTING_CUSTOM_ITEMS_MAGE : Config.STARTING_CUSTOM_ITEMS_FIGHTER;
			if (customItems != null && !customItems.isBlank()) {
				for (String entry : customItems.split(";")) {
					if (entry.isBlank()) continue;
					String[] parts = entry.split(",");
					if (parts.length == 2) {
						try {
							int itemId = Integer.parseInt(parts[0].trim());
							int count = Integer.parseInt(parts[1].trim());
							addItem(inv, itemId, count, "CustomStarter");
						} catch (NumberFormatException ignored) {}
					}
				}
			}
		}
		return inv;
	}

	/** Adiciona (empilhando se stackable). Devolve null se o item nao existe. */
	public AddResult addItem(Inventory inv, int itemId, int count, String process) {
		var template = templates.get(itemId).orElse(null);
		if (template == null || count <= 0) {
			return null;
		}
		if (template.stackable()) {
			var existing = inv.byItemId(itemId);
			if (existing.isPresent()) {
				ItemInstance item = existing.get();
				item.count((int) Math.min(Integer.MAX_VALUE, (long) item.count() + count));
				repository.update(item);
				return new AddResult(item, false);
			}
			ItemInstance item = new ItemInstance(ids.nextId(), template, inv.ownerId(), count);
			inv.add(item);
			repository.insert(item, process);
			return new AddResult(item, true);
		}
		ItemInstance first = null;
		for (int i = 0; i < count; i++) {
			ItemInstance item = new ItemInstance(ids.nextId(), template, inv.ownerId(), 1);
			inv.add(item);
			repository.insert(item, process);
			if (first == null) {
				first = item;
			}
		}
		return new AddResult(first, true);
	}

	/** Consome ou remove uma quantidade de item/adena. Devolve null se saldo insuficiente ou contagem invalida. */
	public ConsumeResult consumeItem(Inventory inv, int itemId, int count, String process) {
		if (count <= 0) {
			return null;
		}
		var opt = inv.byItemId(itemId);
		if (opt.isEmpty() || opt.get().count() < count) {
			return null;
		}
		ItemInstance item = opt.get();
		if (item.count() == count) {
			inv.remove(item);
			repository.delete(item.objectId());
			return new ConsumeResult(item, true);
		} else {
			item.count(item.count() - count);
			repository.update(item);
			return new ConsumeResult(item, false);
		}
	}

	/** Destroi um item especifico pelo objectId. */
	public ConsumeResult destroyItem(Inventory inv, int objectId, int count, String process) {
		if (count <= 0) {
			return null;
		}
		var opt = inv.byObjectId(objectId);
		if (opt.isEmpty() || opt.get().count() < count) {
			return null;
		}
		ItemInstance item = opt.get();
		if (item.count() == count) {
			inv.remove(item);
			repository.delete(item.objectId());
			if (augmentationRepository != null) {
				augmentationRepository.delete(item.objectId());
			}
			return new ConsumeResult(item, true);
		} else {
			item.count(item.count() - count);
			repository.update(item);
			return new ConsumeResult(item, false);
		}
	}

	/** Persiste as alteracoes de um item (ex: enchant). */
	public void saveItem(ItemInstance item) {
		repository.update(item);
	}

	/** UseItem para equipaveis: alterna equipar/desequipar. */
	public EquipResult toggleEquip(Inventory inv, int objectId) {
		ItemInstance item = inv.byObjectId(objectId).orElse(null);
		if (item == null || !item.template().isEquipable()) {
			return EquipResult.NONE;
		}
		boolean wasEquipped = item.isEquipped();
		Set<ItemInstance> changed = wasEquipped ? inv.unequip(item) : inv.equip(item);
		persist(changed);
		return new EquipResult(item, !wasEquipped, List.copyOf(changed));
	}

	/** RequestUnEquipItem (mascara de body part). */
	public EquipResult unequipBodyPart(Inventory inv, int bodyPart) {
		int slot = com.lopez.l2j.game.item.ItemSlots.paperdollIndex(bodyPart);
		ItemInstance item = slot < 0 ? null : inv.paperdoll(slot);
		if (item == null) {
			return EquipResult.NONE;
		}
		Set<ItemInstance> changed = inv.unequip(item);
		persist(changed);
		return new EquipResult(item, false, List.copyOf(changed));
	}

	public void deleteAll(int ownerId) {
		repository.deleteByOwner(ownerId);
	}

	private void persist(Collection<ItemInstance> changed) {
		changed.forEach(repository::update);
	}
}
