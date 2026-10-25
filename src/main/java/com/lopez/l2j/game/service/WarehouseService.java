package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.game.cursed.CursedWeaponsManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemInstance.Location;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de Armazem (Warehouse Pessoal, Clan Warehouse e Freight entre personagens da mesma conta).
 */
@Service
public class WarehouseService {

	private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);
	public static final int WAREHOUSE_FEE = 30; // 30 adena por item depositado (padrao retail)
	public static final int FREIGHT_FEE_PER_ITEM = 1000; // 1000 * count adena no saque/envio de Freight

	private final ItemRepository itemRepo;
	private final ItemTemplateTable templates;
	private final ObjectIdFactory ids;
	private final JdbcClient jdbc;
	private final AugmentationService augmentationService;

	private final Map<Integer, List<ItemInstance>> warehouseCache = new ConcurrentHashMap<>();
	private final Map<Integer, List<ItemInstance>> clanWarehouseCache = new ConcurrentHashMap<>();
	private final Map<Integer, List<ItemInstance>> freightCache = new ConcurrentHashMap<>();
	private final Map<Integer, String> charAccountMap = new ConcurrentHashMap<>();
	private final Map<Integer, String> charNameMap = new ConcurrentHashMap<>();

	public WarehouseService(ItemRepository itemRepo, ItemTemplateTable templates, ObjectIdFactory ids) {
		this(itemRepo, templates, ids, null, null);
	}

	@Autowired
	public WarehouseService(ItemRepository itemRepo,
							ItemTemplateTable templates,
							ObjectIdFactory ids,
							@Autowired(required = false) JdbcClient jdbc,
							@Autowired(required = false) AugmentationService augmentationService) {
		this.itemRepo = itemRepo;
		this.templates = templates;
		this.ids = ids;
		this.jdbc = jdbc;
		this.augmentationService = augmentationService;
	}

	public void registerAccountCharacter(String accountName, int charId, String charName) {
		if (accountName != null && !accountName.isBlank()) {
			charAccountMap.put(charId, accountName.trim().toLowerCase());
		}
		if (charName != null && !charName.isBlank()) {
			charNameMap.put(charId, charName);
		}
	}

	public Map<Integer, String> getAccountCharactersForFreight(String accountName, int excludeCharId) {
		Map<Integer, String> result = new LinkedHashMap<>();
		if (accountName == null || accountName.isBlank()) {
			return result;
		}
		String norm = accountName.trim().toLowerCase();
		for (var e : charAccountMap.entrySet()) {
			if (e.getKey() != excludeCharId && norm.equals(e.getValue())) {
				result.put(e.getKey(), charNameMap.getOrDefault(e.getKey(), "Char#" + e.getKey()));
			}
		}
		if (jdbc != null) {
			try {
				List<Map<String, Object>> rows = jdbc.sql(
						"SELECT obj_Id, char_name FROM characters WHERE LOWER(account_name) = :acc AND obj_Id <> :id")
						.param("acc", norm)
						.param("id", excludeCharId)
						.query().listOfRows();
				for (var row : rows) {
					int cid = ((Number) row.get("obj_Id")).intValue();
					String cname = String.valueOf(row.get("char_name"));
					result.put(cid, cname);
					charAccountMap.put(cid, norm);
					charNameMap.put(cid, cname);
				}
			} catch (Exception ex) {
				log.debug("getAccountCharactersForFreight query failed: {}", ex.getMessage());
			}
		}
		return result;
	}

	public boolean isSameAccount(int charId1, int charId2) {
		if (charId1 == charId2) {
			return true;
		}
		String acc1 = resolveAccountName(charId1);
		String acc2 = resolveAccountName(charId2);
		return acc1 != null && !acc1.isBlank() && acc1.equalsIgnoreCase(acc2);
	}

	public String resolveAccountName(int charId) {
		String cached = charAccountMap.get(charId);
		if (cached != null) {
			return cached;
		}
		if (jdbc != null) {
			try {
				String acc = jdbc.sql("SELECT account_name FROM characters WHERE obj_Id = :id")
						.param("id", charId)
						.query(String.class)
						.optional().orElse(null);
				if (acc != null) {
					charAccountMap.put(charId, acc.trim().toLowerCase());
					return acc.trim().toLowerCase();
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	public synchronized List<ItemInstance> listPrivate(int ownerId) {
		return getWarehouseItems(ownerId);
	}

	public synchronized List<ItemInstance> getWarehouseItems(int ownerId) {
		return warehouseCache.computeIfAbsent(ownerId, id -> {
			var stored = itemRepo.findWarehouse(id);
			List<ItemInstance> list = new ArrayList<>();
			for (var s : stored) {
				templates.get(s.itemId()).ifPresent(tpl -> {
					var item = new ItemInstance(s.objectId(), tpl, id, s.count());
					item.enchant(s.enchant());
					item.customType1(s.customType1());
					item.customType2(s.customType2());
					item.mana(s.mana());
					item.location(Location.WAREHOUSE, s.locationData());
					restoreItemAugmentation(item);
					list.add(item);
				});
			}
			return list;
		});
	}

	public synchronized List<ItemInstance> getClanWarehouseItems(int clanId) {
		return clanWarehouseCache.computeIfAbsent(clanId, id -> {
			List<ItemInstance> list = new ArrayList<>();
			if (jdbc != null) {
				try {
					List<Map<String, Object>> rows = jdbc.sql("""
							SELECT object_id, item_id, count, enchant_level, loc_data, custom_type1, custom_type2, mana_left
							FROM items WHERE owner_id = :id AND loc = 'CLANWH'
							""").param("id", id).query().listOfRows();
					for (var r : rows) {
						int itemId = ((Number) r.get("item_id")).intValue();
						templates.get(itemId).ifPresent(tpl -> {
							int objId = ((Number) r.get("object_id")).intValue();
							int cnt = ((Number) r.get("count")).intValue();
							var item = new ItemInstance(objId, tpl, id, cnt);
							item.enchant(((Number) r.get("enchant_level")).intValue());
							item.customType1(r.get("custom_type1") != null ? ((Number) r.get("custom_type1")).intValue() : 0);
							item.customType2(r.get("custom_type2") != null ? ((Number) r.get("custom_type2")).intValue() : 0);
							item.mana(r.get("mana_left") != null ? ((Number) r.get("mana_left")).intValue() : -1);
							item.location(Location.CLANWH, r.get("loc_data") != null ? ((Number) r.get("loc_data")).intValue() : 0);
							restoreItemAugmentation(item);
							list.add(item);
						});
					}
				} catch (Exception ex) {
					log.debug("getClanWarehouseItems query failed: {}", ex.getMessage());
				}
			}
			return list;
		});
	}

	public synchronized List<ItemInstance> getFreightItems(int targetCharId) {
		return freightCache.computeIfAbsent(targetCharId, id -> {
			List<ItemInstance> list = new ArrayList<>();
			if (jdbc != null) {
				try {
					List<Map<String, Object>> rows = jdbc.sql("""
							SELECT object_id, item_id, count, enchant_level, loc_data, custom_type1, custom_type2, mana_left
							FROM items WHERE owner_id = :id AND loc = 'FREIGHT'
							""").param("id", id).query().listOfRows();
					for (var r : rows) {
						int itemId = ((Number) r.get("item_id")).intValue();
						templates.get(itemId).ifPresent(tpl -> {
							int objId = ((Number) r.get("object_id")).intValue();
							int cnt = ((Number) r.get("count")).intValue();
							var item = new ItemInstance(objId, tpl, id, cnt);
							item.enchant(((Number) r.get("enchant_level")).intValue());
							item.customType1(r.get("custom_type1") != null ? ((Number) r.get("custom_type1")).intValue() : 0);
							item.customType2(r.get("custom_type2") != null ? ((Number) r.get("custom_type2")).intValue() : 0);
							item.mana(r.get("mana_left") != null ? ((Number) r.get("mana_left")).intValue() : -1);
							item.location(Location.FREIGHT, r.get("loc_data") != null ? ((Number) r.get("loc_data")).intValue() : 0);
							restoreItemAugmentation(item);
							list.add(item);
						});
					}
				} catch (Exception ex) {
					log.debug("getFreightItems query failed: {}", ex.getMessage());
				}
			}
			return list;
		});
	}

	private void restoreItemAugmentation(ItemInstance item) {
		AugmentationService augSvc = augmentationService != null ? augmentationService : AugmentationService.getInstance();
		if (augSvc != null && item != null) {
			try {
				augSvc.loadAugmentation(item.objectId()).ifPresent(rec ->
						item.augmentation(new com.lopez.l2j.game.augmentation.Augmentation(rec.attributes(), rec.skillId(), rec.skillLevel())));
			} catch (Exception ignored) {
			}
		}
	}

	private static boolean isDepositForbidden(ItemInstance item) {
		if (item == null || item.isEquipped()) {
			return true;
		}
		int id = item.itemId();
		if (id == CursedWeaponsManager.ZARICHE || id == CursedWeaponsManager.AKAMANAH) {
			return true;
		}
		return item.template() != null && item.template().isQuestItem();
	}

	public synchronized boolean depositItem(Inventory playerInv, int objectId, int count) {
		if (!Config.ALLOW_WAREHOUSE) {
			return false;
		}
		var itemOpt = playerInv.byObjectId(objectId);
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (isDepositForbidden(item) || count <= 0 || item.count() < count) {
			return false;
		}

		var whList = getWarehouseItems(item.ownerId());
		boolean isStackableExisting = item.template().stackable()
				&& whList.stream().anyMatch(i -> i.itemId() == item.itemId());
		if (!isStackableExisting && whList.size() >= Config.MAX_WAREHOUSE_SLOTS_FOR_OTHER) {
			return false;
		}

		// Checa e consome a taxa de 30 adena
		var adenaOpt = playerInv.byItemId(ItemTemplate.ADENA_ID);
		if (adenaOpt.isEmpty() || adenaOpt.get().count() < WAREHOUSE_FEE) {
			return false;
		}

		adenaOpt.get().count(adenaOpt.get().count() - WAREHOUSE_FEE);
		itemRepo.update(adenaOpt.get());

		if (item.template().stackable()) {
			var existing = whList.stream().filter(i -> i.itemId() == item.itemId()).findFirst().orElse(null);
			if (existing != null) {
				if ((long) existing.count() + count > Integer.MAX_VALUE) {
					return false;
				}
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
			item.count(item.count() - count);
			itemRepo.update(item);

			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var whItem = new ItemInstance(newId, item.template(), item.ownerId(), count);
			whItem.enchant(item.enchant());
			whItem.customType1(item.customType1());
			whItem.customType2(item.customType2());
			whItem.mana(item.mana());
			whItem.augmentation(item.augmentation());
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

		boolean stackableExisting = item.template().stackable() && playerInv.byItemId(item.itemId()).isPresent();
		int maxInvSlots = Config.MAX_INVENTORY_SLOTS_FOR_OTHER > 0 ? Config.MAX_INVENTORY_SLOTS_FOR_OTHER : 80;
		if (!stackableExisting && playerInv.size() >= maxInvSlots) {
			return false;
		}

		if (item.template().stackable()) {
			var existing = playerInv.byItemId(item.itemId()).orElse(null);
			if (existing != null) {
				if ((long) existing.count() + count > Integer.MAX_VALUE) {
					return false;
				}
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
			invItem.enchant(item.enchant());
			invItem.customType1(item.customType1());
			invItem.customType2(item.customType2());
			invItem.mana(item.mana());
			invItem.augmentation(item.augmentation());
			invItem.location(Location.INVENTORY, 0);
			playerInv.add(invItem);
			itemRepo.insert(invItem, "WithdrawWH");
		}
		return true;
	}

	public synchronized boolean depositClanItem(Inventory playerInv, int clanId, int objectId, int count) {
		if (!Config.ALLOW_WAREHOUSE || clanId <= 0) {
			return false;
		}
		var itemOpt = playerInv.byObjectId(objectId);
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (isDepositForbidden(item) || !item.template().isTradeable() || count <= 0 || item.count() < count) {
			return false;
		}
		var adenaOpt = playerInv.byItemId(ItemTemplate.ADENA_ID);
		if (adenaOpt.isEmpty() || adenaOpt.get().count() < WAREHOUSE_FEE) {
			return false;
		}
		adenaOpt.get().count(adenaOpt.get().count() - WAREHOUSE_FEE);
		itemRepo.update(adenaOpt.get());

		var clanWh = getClanWarehouseItems(clanId);
		if (item.template().stackable()) {
			var existing = clanWh.stream().filter(i -> i.itemId() == item.itemId()).findFirst().orElse(null);
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
			item.ownerId(clanId);
			item.location(Location.CLANWH, 0);
			clanWh.add(item);
			itemRepo.update(item);
		} else {
			item.count(item.count() - count);
			itemRepo.update(item);
			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var cItem = new ItemInstance(newId, item.template(), clanId, count);
			cItem.enchant(item.enchant());
			cItem.location(Location.CLANWH, 0);
			clanWh.add(cItem);
			itemRepo.insert(cItem, "DepositClanWH");
		}
		return true;
	}

	public synchronized boolean withdrawClanItem(Inventory playerInv, int clanId, boolean hasPrivilege, int objectId, int count) {
		if (!hasPrivilege || clanId <= 0) {
			return false;
		}
		var clanWh = getClanWarehouseItems(clanId);
		var itemOpt = clanWh.stream().filter(i -> i.objectId() == objectId).findFirst();
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
					clanWh.remove(item);
					itemRepo.delete(item.objectId());
				} else {
					item.count(item.count() - count);
					itemRepo.update(item);
				}
				return true;
			}
		}
		if (item.count() == count) {
			clanWh.remove(item);
			item.ownerId(playerInv.ownerId());
			item.location(Location.INVENTORY, 0);
			playerInv.add(item);
			itemRepo.update(item);
		} else {
			item.count(item.count() - count);
			itemRepo.update(item);
			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var invItem = new ItemInstance(newId, item.template(), playerInv.ownerId(), count);
			invItem.enchant(item.enchant());
			invItem.location(Location.INVENTORY, 0);
			playerInv.add(invItem);
			itemRepo.insert(invItem, "WithdrawClanWH");
		}
		return true;
	}

	/**
	 * Deposita item em Freight para outro personagem da mesma conta (accountName).
	 */
	public synchronized boolean depositFreightItem(Inventory senderInv, String senderAccount, int targetCharId,
			String targetAccount, int objectId, int count) {
		if (senderAccount != null && targetAccount != null && !senderAccount.equalsIgnoreCase(targetAccount)) {
			return false;
		}
		if (senderAccount != null) {
			registerAccountCharacter(senderAccount, senderInv.ownerId(), null);
		}
		if (targetAccount != null) {
			registerAccountCharacter(targetAccount, targetCharId, null);
		}
		return depositFreightItem(senderInv, targetCharId, objectId, count);
	}

	public synchronized boolean depositFreightItem(Inventory senderInv, int targetCharId, int objectId, int count) {
		if (!Config.ALLOW_FREIGHT || senderInv == null || targetCharId <= 0) {
			return false;
		}
		String acc1 = resolveAccountName(senderInv.ownerId());
		String acc2 = resolveAccountName(targetCharId);
		if (acc1 != null && acc2 != null && !acc1.equalsIgnoreCase(acc2)) {
			return false;
		}
		var itemOpt = senderInv.byObjectId(objectId);
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (isDepositForbidden(item) || item.isAugmented() || count <= 0 || item.count() < count) {
			return false;
		}

		var freightList = getFreightItems(targetCharId);
		boolean isStackableExisting = item.template().stackable()
				&& freightList.stream().anyMatch(i -> i.itemId() == item.itemId());
		int maxFreightSlots = Config.MAX_FREIGHT_SLOTS > 0 ? Config.MAX_FREIGHT_SLOTS : 20;
		if (!isStackableExisting && freightList.size() >= maxFreightSlots) {
			return false;
		}

		if (item.template().stackable()) {
			var existing = freightList.stream().filter(i -> i.itemId() == item.itemId()).findFirst().orElse(null);
			if (existing != null) {
				if ((long) existing.count() + count > Integer.MAX_VALUE) {
					return false;
				}
				existing.count(existing.count() + count);
				itemRepo.update(existing);

				if (item.count() == count) {
					senderInv.remove(item);
					itemRepo.delete(item.objectId());
				} else {
					item.count(item.count() - count);
					itemRepo.update(item);
				}
				return true;
			}
		}

		if (item.count() == count) {
			senderInv.remove(item);
			item.ownerId(targetCharId);
			item.location(Location.FREIGHT, senderInv.ownerId());
			freightList.add(item);
			itemRepo.update(item);
		} else {
			item.count(item.count() - count);
			itemRepo.update(item);

			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var fItem = new ItemInstance(newId, item.template(), targetCharId, count);
			fItem.enchant(item.enchant());
			fItem.customType1(item.customType1());
			fItem.customType2(item.customType2());
			fItem.mana(item.mana());
			fItem.location(Location.FREIGHT, senderInv.ownerId());
			freightList.add(fItem);
			itemRepo.insert(fItem, "DepositFreight");
		}
		return true;
	}

	/**
	 * Retira item do Freight cobrando a taxa de 1000 * count Adena.
	 */
	public synchronized boolean withdrawFreightItem(Inventory receiverInv, int charId, int objectId, int count) {
		if (!Config.ALLOW_FREIGHT || receiverInv == null || count <= 0) {
			return false;
		}
		var freightList = getFreightItems(charId);
		var itemOpt = freightList.stream().filter(i -> i.objectId() == objectId).findFirst();
		if (itemOpt.isEmpty()) {
			return false;
		}
		var item = itemOpt.get();
		if (item.count() < count) {
			return false;
		}

		long fee = (long) FREIGHT_FEE_PER_ITEM * count;
		var adenaOpt = receiverInv.byItemId(ItemTemplate.ADENA_ID);
		if (adenaOpt.isEmpty() || adenaOpt.get().count() < fee) {
			return false;
		}

		boolean stackableExisting = item.template().stackable() && receiverInv.byItemId(item.itemId()).isPresent();
		int maxInvSlots = Config.MAX_INVENTORY_SLOTS_FOR_OTHER > 0 ? Config.MAX_INVENTORY_SLOTS_FOR_OTHER : 80;
		if (!stackableExisting && receiverInv.size() >= maxInvSlots) {
			return false;
		}

		// Cobra a taxa de 1000 * count Adena
		int remainingAdena = (int) (adenaOpt.get().count() - fee);
		if (remainingAdena <= 0) {
			receiverInv.remove(adenaOpt.get());
			itemRepo.delete(adenaOpt.get().objectId());
		} else {
			adenaOpt.get().count(remainingAdena);
			itemRepo.update(adenaOpt.get());
		}

		if (item.template().stackable()) {
			var existing = receiverInv.byItemId(item.itemId()).orElse(null);
			if (existing != null) {
				if ((long) existing.count() + count > Integer.MAX_VALUE) {
					return false;
				}
				existing.count(existing.count() + count);
				itemRepo.update(existing);

				if (item.count() == count) {
					freightList.remove(item);
					itemRepo.delete(item.objectId());
				} else {
					item.count(item.count() - count);
					itemRepo.update(item);
				}
				return true;
			}
		}

		if (item.count() == count) {
			freightList.remove(item);
			item.ownerId(charId);
			item.location(Location.INVENTORY, 0);
			receiverInv.add(item);
			itemRepo.update(item);
		} else {
			item.count(item.count() - count);
			itemRepo.update(item);

			int newId = ids != null ? ids.nextId() : (int) System.currentTimeMillis();
			var invItem = new ItemInstance(newId, item.template(), charId, count);
			invItem.enchant(item.enchant());
			invItem.customType1(item.customType1());
			invItem.customType2(item.customType2());
			invItem.mana(item.mana());
			invItem.location(Location.INVENTORY, 0);
			receiverInv.add(invItem);
			itemRepo.insert(invItem, "WithdrawFreight");
		}
		return true;
	}
}
