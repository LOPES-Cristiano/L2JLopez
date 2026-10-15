package com.lopez.l2j.game.item;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.DropItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.GetItem;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de itens no chao do mundo (ItemsOnGroundManager e ItemsAutoDestroy do L2JDream).
 * Controla itens caidos de monstros/jogadores, animacao de queda e recolhimento, e expiracao automatica.
 */
@Service
public class GroundItemService {

	private static final Logger log = LoggerFactory.getLogger(GroundItemService.class);

	public record GroundItem(
			int objectId,
			int itemId,
			int count,
			int x,
			int y,
			int z,
			int dropperObjectId,
			long dropTimeMs,
			long expireTimeMs,
			boolean isHerb,
			ItemInstance itemInstance) {
	}

	private final Map<Integer, GroundItem> itemsByObjectId = new ConcurrentHashMap<>();
	private final GameWorld world;
	private final ObjectIdFactory idFactory;
	private final ItemTemplateTable templates;
	private final org.springframework.jdbc.core.simple.JdbcClient jdbc;

	public GroundItemService(GameWorld world, ObjectIdFactory idFactory, ItemTemplateTable templates) {
		this(world, idFactory, templates, null);
	}

	@Autowired
	public GroundItemService(GameWorld world, @Autowired(required = false) ObjectIdFactory idFactory,
			@Autowired(required = false) ItemTemplateTable templates,
			@Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		this.world = world;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x80000000);
		this.templates = templates;
		this.jdbc = jdbc;
		if (Config.SAVE_DROPPED_ITEM) {
			loadFromDb();
		}
	}

	public int size() {
		return itemsByObjectId.size();
	}

	public Collection<GroundItem> allGroundItems() {
		return Collections.unmodifiableCollection(itemsByObjectId.values());
	}

	public Optional<GroundItem> byObjectId(int objectId) {
		return Optional.ofNullable(itemsByObjectId.get(objectId));
	}

	public java.util.List<GroundItem> findAround(int x, int y, int radius) {
		long r2 = (long) radius * radius;
		java.util.List<GroundItem> list = new java.util.ArrayList<>();
		for (GroundItem gi : itemsByObjectId.values()) {
			long dx = gi.x() - x;
			long dy = gi.y() - y;
			if (dx * dx + dy * dy <= r2) {
				list.add(gi);
			}
		}
		return list;
	}

	/**
	 * Spawna um item caido no chao do mundo e transmite DropItem aos jogadores ao redor.
	 */
	public GroundItem dropItem(int dropperId, ItemInstance instance, int x, int y, int z) {
		if (instance == null) {
			return null;
		}
		if (dropperId > 0 && Config.DESTROY_PLAYER_INVENTORY_DROP) {
			log.debug("Item {} destruido imediatamente por Config.DESTROY_PLAYER_INVENTORY_DROP", instance.itemId());
			return null;
		}
		boolean isHerb = instance.template() != null && instance.template().name() != null
				&& instance.template().name().toLowerCase(java.util.Locale.ROOT).contains("herb");
		long now = System.currentTimeMillis();
		long expireMs = resolveExpireTimeMs(now, dropperId, instance.itemId(), instance.template(), isHerb);

		boolean stackable = instance.template() != null && instance.template().isStackable();
		if (Config.MULTIPLE_ITEM_DROP && stackable) {
			for (GroundItem existing : itemsByObjectId.values()) {
				if (existing.itemId() == instance.itemId()) {
					long dx = existing.x() - x;
					long dy = existing.y() - y;
					if (dx * dx + dy * dy <= 150 * 150) {
						int newCount = (int) Math.min(Integer.MAX_VALUE, (long) existing.count() + instance.count());
						if (existing.itemInstance() != null) {
							existing.itemInstance().count(newCount);
						}
						var merged = new GroundItem(existing.objectId(), existing.itemId(), newCount,
								existing.x(), existing.y(), existing.z(), existing.dropperObjectId(),
								existing.dropTimeMs(), expireMs, isHerb, existing.itemInstance());
						itemsByObjectId.put(existing.objectId(), merged);
						if (world != null) {
							var dropPkt = new DropItem(existing.dropperObjectId(), existing.objectId(), existing.itemId(),
									existing.x(), existing.y(), existing.z(), true, newCount);
							world.broadcastAround(existing.x(), existing.y(), GameWorld.VISIBILITY_RADIUS, dropPkt);
						}
						return merged;
					}
				}
			}
		}

		var gi = new GroundItem(instance.objectId(), instance.itemId(), instance.count(), x, y, z,
				dropperId, now, expireMs, isHerb, instance);

		itemsByObjectId.put(instance.objectId(), gi);

		if (world != null) {
			var dropPkt = new DropItem(dropperId, instance.objectId(), instance.itemId(), x, y, z, stackable, instance.count());
			world.broadcastAround(x, y, GameWorld.VISIBILITY_RADIUS, dropPkt);
			for (var p : world.findPlayersAround(x, y, GameWorld.VISIBILITY_RADIUS)) {
				p.addKnownObject(instance.objectId());
			}
		}
		return gi;
	}

	public GroundItem dropItem(int dropperId, int itemId, int count, int x, int y, int z) {
		if (dropperId > 0 && Config.DESTROY_PLAYER_INVENTORY_DROP) {
			log.debug("Item {} destruido imediatamente por Config.DESTROY_PLAYER_INVENTORY_DROP", itemId);
			return null;
		}
		var tpl = templates != null ? templates.get(itemId).orElse(null) : null;
		boolean stackable = tpl != null && tpl.isStackable();
		boolean isHerb = tpl != null && tpl.name() != null && tpl.name().toLowerCase(java.util.Locale.ROOT).contains("herb");
		long now = System.currentTimeMillis();
		long expireMs = resolveExpireTimeMs(now, dropperId, itemId, tpl, isHerb);

		if (Config.MULTIPLE_ITEM_DROP && stackable) {
			for (GroundItem existing : itemsByObjectId.values()) {
				if (existing.itemId() == itemId) {
					long dx = existing.x() - x;
					long dy = existing.y() - y;
					if (dx * dx + dy * dy <= 150 * 150) {
						int newCount = (int) Math.min(Integer.MAX_VALUE, (long) existing.count() + count);
						if (existing.itemInstance() != null) {
							existing.itemInstance().count(newCount);
						}
						var merged = new GroundItem(existing.objectId(), existing.itemId(), newCount,
								existing.x(), existing.y(), existing.z(), existing.dropperObjectId(),
								existing.dropTimeMs(), expireMs, isHerb, existing.itemInstance());
						itemsByObjectId.put(existing.objectId(), merged);
						if (world != null) {
							var dropPkt = new DropItem(existing.dropperObjectId(), existing.objectId(), existing.itemId(),
									existing.x(), existing.y(), existing.z(), true, newCount);
							world.broadcastAround(existing.x(), existing.y(), GameWorld.VISIBILITY_RADIUS, dropPkt);
						}
						return merged;
					}
				}
			}
		}

		int objectId = idFactory.nextId();
		ItemInstance inst = tpl != null ? new ItemInstance(objectId, tpl, 0, count) : null;
		var gi = new GroundItem(objectId, itemId, count, x, y, z, dropperId, now, expireMs, isHerb, inst);
		itemsByObjectId.put(objectId, gi);

		if (world != null) {
			var dropPkt = new DropItem(dropperId, objectId, itemId, x, y, z, stackable, count);
			world.broadcastAround(x, y, GameWorld.VISIBILITY_RADIUS, dropPkt);
			for (var p : world.findPlayersAround(x, y, GameWorld.VISIBILITY_RADIUS)) {
				p.addKnownObject(objectId);
			}
		}
		return gi;
	}

	/**
	 * Calcula o tempo de expiracao de um item no chao respeitando AUTO_DESTROY_HERB_TIME,
	 * LIST_OF_PROTECTED_ITEMS, DESTROY_PLAYER_DROPPED_ITEM e DESTROY_EQUIPABLE_ITEM.
	 */
	public static long resolveExpireTimeMs(long nowMs, int dropperId, int itemId, ItemTemplate tpl, boolean isHerb) {
		if (Config.LIST_OF_PROTECTED_ITEMS != null && Config.LIST_OF_PROTECTED_ITEMS.contains(itemId)) {
			return Long.MAX_VALUE;
		}
		if (isHerb) {
			int herbSec = Config.AUTO_DESTROY_HERB_TIME;
			return herbSec > 0 ? nowMs + (herbSec * 1000L) : Long.MAX_VALUE;
		}
		if (dropperId > 0 && !Config.DESTROY_PLAYER_DROPPED_ITEM) {
			return Long.MAX_VALUE;
		}
		boolean equipable = tpl != null && (tpl.isWeapon() || tpl.isArmor());
		if (equipable && !Config.DESTROY_EQUIPABLE_ITEM) {
			return Long.MAX_VALUE;
		}
		int lifetimeSec = Config.AUTO_DESTROY_DROPPED_ITEM_AFTER;
		return lifetimeSec > 0 ? nowMs + (lifetimeSec * 1000L) : Long.MAX_VALUE;
	}

	/**
	 * Verifica se um jogador tem permissao para descartar ou destruir um item do inventario.
	 */
	public boolean canPlayerDiscardOrDropItem(PlayerCharacter player, int itemId) {
		if (player != null && player.accessLevel() > 0) {
			return true;
		}
		if (!Config.ALLOW_DISCARD_ITEM) {
			return false;
		}
		if (Config.LIST_OF_NON_DROPPABLE_ITEMS != null && Config.LIST_OF_NON_DROPPABLE_ITEMS.contains(itemId)) {
			return false;
		}
		if (Config.LIST_OF_PET_ITEMS != null && Config.LIST_OF_PET_ITEMS.contains(itemId)) {
			return false;
		}
		return true;
	}

	public boolean isSaveDroppedItemsEnabled() {
		return Config.SAVE_DROPPED_ITEM;
	}

	public int getSaveDroppedItemIntervalMinutes() {
		return Math.max(0, Config.SAVE_DROPPED_ITEM_INTERVAL);
	}

	public boolean shouldEmptyDroppedItemTableAfterLoad() {
		return Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	}

	public boolean shouldClearDroppedItemTable() {
		return Config.CLEAR_DROPPED_ITEM_TABLE;
	}

	public void onServerStartupClearIfConfigured() {
		if (Config.CLEAR_DROPPED_ITEM_TABLE) {
			itemsByObjectId.clear();
			if (jdbc != null) {
				try {
					jdbc.sql("DELETE FROM items_on_ground").update();
				} catch (Exception ex) {
					log.warn("Erro ao limpar tabela items_on_ground: {}", ex.getMessage());
				}
			}
		}
	}

	public synchronized void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT object_id, item_id, count, enchant_level, x, y, z, drop_time, equipable
					FROM items_on_ground
					""").query().listOfRows();
			for (var row : rows) {
				int objectId = ((Number) row.get("object_id")).intValue();
				int itemId = ((Number) row.get("item_id")).intValue();
				int count = ((Number) row.get("count")).intValue();
				int x = ((Number) row.get("x")).intValue();
				int y = ((Number) row.get("y")).intValue();
				int z = ((Number) row.get("z")).intValue();
				long dropTime = row.get("drop_time") != null ? ((Number) row.get("drop_time")).longValue() : System.currentTimeMillis();

				var tpl = templates != null ? templates.get(itemId).orElse(null) : null;
				boolean isHerb = tpl != null && tpl.name() != null && tpl.name().toLowerCase(java.util.Locale.ROOT).contains("herb");
				long expireMs = resolveExpireTimeMs(dropTime, 0, itemId, tpl, isHerb);
				ItemInstance inst = tpl != null ? new ItemInstance(objectId, tpl, 0, count) : null;

				itemsByObjectId.put(objectId, new GroundItem(objectId, itemId, count, x, y, z, 0, dropTime, expireMs, isHerb, inst));
			}
			log.info("GroundItemService: {} itens no chao carregados de items_on_ground", itemsByObjectId.size());

			if (Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD) {
				jdbc.sql("DELETE FROM items_on_ground").update();
			}
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar items_on_ground do banco: {}", ex.getMessage());
		}
	}

	public synchronized void saveToDb() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM items_on_ground").update();
			for (GroundItem gi : itemsByObjectId.values()) {
				int equipable = (gi.itemInstance() != null && gi.itemInstance().template() != null
						&& (gi.itemInstance().template().isWeapon() || gi.itemInstance().template().isArmor())) ? 1 : 0;
				int enchant = gi.itemInstance() != null ? gi.itemInstance().enchant() : 0;
				jdbc.sql("""
						INSERT INTO items_on_ground (object_id, item_id, count, enchant_level, x, y, z, drop_time, equipable)
						VALUES (:obj, :item, :cnt, :ench, :x, :y, :z, :drop, :eq)
						""")
						.param("obj", gi.objectId())
						.param("item", gi.itemId())
						.param("cnt", gi.count())
						.param("ench", enchant)
						.param("x", gi.x())
						.param("y", gi.y())
						.param("z", gi.z())
						.param("drop", gi.dropTimeMs())
						.param("eq", equipable)
						.update();
			}
			log.info("GroundItemService: {} itens no chao persistidos em items_on_ground", itemsByObjectId.size());
		} catch (Exception ex) {
			log.warn("Erro ao salvar items_on_ground no banco: {}", ex.getMessage());
		}
	}

	/**
	 * Recolhe o item do chao e notifica jogadores proximos com GetItem e DeleteObject.
	 */
	public Optional<GroundItem> pickupItem(PlayerCharacter player, int itemObjectId) {
		GroundItem gi = itemsByObjectId.remove(itemObjectId);
		if (gi == null) {
			return Optional.empty();
		}

		if (world != null) {
			if (player != null) {
				var getPkt = new GetItem(player.objectId(), gi.objectId(), gi.x(), gi.y(), gi.z());
				world.broadcastAround(player.x(), player.y(), GameWorld.VISIBILITY_RADIUS, getPkt);
			}
			var delPkt = new DeleteObject(gi.objectId());
			world.broadcastAround(gi.x(), gi.y(), GameWorld.VISIBILITY_RADIUS, delPkt);
			for (var p : world.findPlayersAround(gi.x(), gi.y(), GameWorld.VISIBILITY_RADIUS)) {
				p.removeKnownObject(gi.objectId());
			}
		}
		return Optional.of(gi);
	}

	/**
	 * Remove itens expirados (Herbs apos AUTO_DESTROY_HERB_TIME, itens normais apos tempo configurado).
	 */
	public int cleanupExpiredItems() {
		long now = System.currentTimeMillis();
		java.util.List<GroundItem> expired = itemsByObjectId.values().stream()
				.filter(gi -> gi.expireTimeMs() <= now)
				.toList();

		for (GroundItem gi : expired) {
			itemsByObjectId.remove(gi.objectId());
			if (world != null) {
				world.broadcastAround(gi.x(), gi.y(), GameWorld.VISIBILITY_RADIUS, new DeleteObject(gi.objectId()));
				for (var p : world.findPlayersAround(gi.x(), gi.y(), GameWorld.VISIBILITY_RADIUS)) {
					p.removeKnownObject(gi.objectId());
				}
			}
		}
		if (!expired.isEmpty()) {
			log.debug("GroundItemService: {} itens expirados removidos do chao", expired.size());
		}
		return expired.size();
	}
}
