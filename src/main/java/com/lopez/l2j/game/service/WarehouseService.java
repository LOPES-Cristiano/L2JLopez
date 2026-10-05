package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemInstance.Location;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de Armazem (Warehouse) para deposito e retirada de itens particulares.
 */
@Service
public class WarehouseService {

	private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);
	public static final int WAREHOUSE_FEE = 30; // 30 adena por item depositado (padrao retail)

	private final ItemRepository itemRepo;
	private final ItemTemplateTable templates;
	private final ObjectIdFactory ids;
	private final Map<Integer, List<ItemInstance>> warehouseCache = new ConcurrentHashMap<>();

	public WarehouseService(ItemRepository itemRepo, ItemTemplateTable templates, ObjectIdFactory ids) {
		this.itemRepo = itemRepo;
		this.templates = templates;
		this.ids = ids;
	}

	public synchronized List<ItemInstance> getWarehouseItems(int ownerId) {
		return warehouseCache.computeIfAbsent(ownerId, id -> {
			var stored = itemRepo.findWarehouse(id);
			List<ItemInstance> list = new ArrayList<>();
			for (var s : stored) {
				templates.get(s.itemId()).ifPresent(tpl -> {
					var item = new ItemInstance(s.objectId(), tpl, id, s.count());
					item.enchant(s.enchant());
					item.location(Location.WAREHOUSE, s.locationData());
					list.add(item);
				});
			}
			return list;
		});
	}

	public synchronized boolean depositItem(Inventory playerInv, int objectId, int count) {
		var itemOpt = playerInv.byObjectId(objectId);
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (item.isEquipped() || count <= 0 || item.count() < count) {
			return false;
		}

		// Checa e consome a taxa de 30 adena (a nao ser que esteja depositando adena e o saldo cubra a taxa)
		var adenaOpt = playerInv.byItemId(ItemTemplate.ADENA_ID);
		if (adenaOpt.isEmpty() || adenaOpt.get().count() < WAREHOUSE_FEE) {
			return false;
		}

		adenaOpt.get().count(adenaOpt.get().count() - WAREHOUSE_FEE);
		itemRepo.update(adenaOpt.get());

		var whList = getWarehouseItems(item.ownerId());

		if (item.template().stackable()) {
			var existing = whList.stream().filter(i -> i.itemId() == item.itemId()).findFirst().orElse(null);
			if (existing != null) {
				existing.count(existing.count() + count);
				itemRepo.update(existing);

				if (item.count() == count) {
					playerInv.remove(item);
					itemRepo.delete(item.objectId());
				} else {
					item.count(item.count() - count);
					itemRepo.update(item);
				}
				return true;
			}
		}

		if (item.count() == count) {
			playerInv.remove(item);
			item.location(Location.WAREHOUSE, 0);
			whList.add(item);
			itemRepo.update(item);
		} else {
			// Divide pilha: o inventario fica com o restante, o warehouse ganha o novo com id novo
			item.count(item.count() - count);
			itemRepo.update(item);

			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var whItem = new ItemInstance(newId, item.template(), item.ownerId(), count);
			whItem.location(Location.WAREHOUSE, 0);
			whList.add(whItem);
			itemRepo.insert(whItem, "DepositWH");
		}
		return true;
	}

	public synchronized boolean withdrawItem(Inventory playerInv, int ownerId, int objectId, int count) {
		var whList = getWarehouseItems(ownerId);
		var itemOpt = whList.stream().filter(i -> i.objectId() == objectId).findFirst();
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (count <= 0 || item.count() < count) {
			return false;
		}

		if (item.template().stackable()) {
			var existing = playerInv.byItemId(item.itemId()).orElse(null);
			if (existing != null) {
				existing.count(existing.count() + count);
				itemRepo.update(existing);

				if (item.count() == count) {
					whList.remove(item);
					itemRepo.delete(item.objectId());
				} else {
					item.count(item.count() - count);
					itemRepo.update(item);
				}
				return true;
			}
		}

		if (item.count() == count) {
			whList.remove(item);
			item.location(Location.INVENTORY, 0);
			playerInv.add(item);
			itemRepo.update(item);
		} else {
			item.count(item.count() - count);
			itemRepo.update(item);

			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var invItem = new ItemInstance(newId, item.template(), ownerId, count);
			invItem.location(Location.INVENTORY, 0);
			playerInv.add(invItem);
			itemRepo.insert(invItem, "WithdrawWH");
		}
		return true;
	}
}
