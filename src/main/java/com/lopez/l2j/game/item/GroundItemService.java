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

	@Autowired
	public GroundItemService(GameWorld world, @Autowired(required = false) ObjectIdFactory idFactory,
			@Autowired(required = false) ItemTemplateTable templates) {
		this.world = world;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x80000000);
		this.templates = templates;
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
		boolean isHerb = instance.template() != null && instance.template().name() != null
				&& instance.template().name().toLowerCase(java.util.Locale.ROOT).contains("herb");
		int lifetimeSec = isHerb ? Config.getInt("HerbAutoDestroyTime", 15) : Config.getInt("AutoDestroyItemAfter", 600);
		long now = System.currentTimeMillis();
		long expireMs = lifetimeSec > 0 ? now + (lifetimeSec * 1000L) : Long.MAX_VALUE;

		boolean stackable = instance.template() != null && instance.template().isStackable();
		var gi = new GroundItem(instance.objectId(), instance.itemId(), instance.count(), x, y, z,
				dropperId, now, expireMs, isHerb, instance);

		itemsByObjectId.put(instance.objectId(), gi);

		if (world != null) {
			var dropPkt = new DropItem(dropperId, instance.objectId(), instance.itemId(), x, y, z, stackable, instance.count());
			world.broadcastAround(x, y, GameWorld.VISIBILITY_RADIUS, dropPkt);
		}
		return gi;
	}

	public GroundItem dropItem(int dropperId, int itemId, int count, int x, int y, int z) {
		int objectId = idFactory.nextId();
		var tpl = templates != null ? templates.get(itemId).orElse(null) : null;
		boolean stackable = tpl != null && tpl.isStackable();
		boolean isHerb = tpl != null && tpl.name() != null && tpl.name().toLowerCase(java.util.Locale.ROOT).contains("herb");
		int lifetimeSec = isHerb ? Config.getInt("HerbAutoDestroyTime", 15) : Config.getInt("AutoDestroyItemAfter", 600);
		long now = System.currentTimeMillis();
		long expireMs = lifetimeSec > 0 ? now + (lifetimeSec * 1000L) : Long.MAX_VALUE;

		ItemInstance inst = tpl != null ? new ItemInstance(objectId, tpl, 0, count) : null;
		var gi = new GroundItem(objectId, itemId, count, x, y, z, dropperId, now, expireMs, isHerb, inst);
		itemsByObjectId.put(objectId, gi);

		if (world != null) {
			var dropPkt = new DropItem(dropperId, objectId, itemId, x, y, z, stackable, count);
			world.broadcastAround(x, y, GameWorld.VISIBILITY_RADIUS, dropPkt);
		}
		return gi;
	}

	/**
	 * Recolhe o item do chao e notifica jogadores proximos com GetItem.
	 */
	public Optional<GroundItem> pickupItem(PlayerCharacter player, int itemObjectId) {
		GroundItem gi = itemsByObjectId.remove(itemObjectId);
		if (gi == null) {
			return Optional.empty();
		}

		if (world != null && player != null) {
			var getPkt = new GetItem(player.objectId(), gi.objectId(), gi.x(), gi.y(), gi.z());
			world.broadcastAround(player.x(), player.y(), GameWorld.VISIBILITY_RADIUS, getPkt);
		}
		return Optional.of(gi);
	}

	/**
	 * Remove itens expirados (Herbs apos 15s, itens normais apos tempo configurado).
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
			}
		}
		if (!expired.isEmpty()) {
			log.debug("GroundItemService: {} itens expirados removidos do chao", expired.size());
		}
		return expired.size();
	}
}
