package com.lopez.l2j.game.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repositorio de itens em memoria (guarda copias das linhas, como o banco). */
public class InMemoryItemRepository implements ItemRepository {

	public record Row(int ownerId, int objectId, int itemId, int count, int enchant, String loc, int locData) {
	}

	public final Map<Integer, Row> rows = new LinkedHashMap<>();

	@Override
	public synchronized List<StoredItem> findInventory(int ownerId) {
		List<StoredItem> out = new ArrayList<>();
		for (Row r : rows.values()) {
			if (r.ownerId() == ownerId && (r.loc().equals("INVENTORY") || r.loc().equals("PAPERDOLL"))) {
				out.add(new StoredItem(r.objectId(), r.itemId(), r.count(), r.enchant(), r.loc(), r.locData(), 0, 0,
						-1));
			}
		}
		return out;
	}

	@Override
	public synchronized List<Paperdoll.Entry> findPaperdoll(int ownerId) {
		return rows.values().stream().filter(r -> r.ownerId() == ownerId && r.loc().equals("PAPERDOLL"))
				.map(r -> new Paperdoll.Entry(r.locData(), r.objectId(), r.itemId())).toList();
	}

	@Override
	public synchronized void insert(ItemInstance item, String process) {
		if (rows.containsKey(item.objectId())) {
			throw new IllegalStateException("object_id duplicado " + item.objectId());
		}
		rows.put(item.objectId(), row(item));
	}

	@Override
	public synchronized void update(ItemInstance item) {
		rows.put(item.objectId(), row(item));
	}

	@Override
	public synchronized void delete(int objectId) {
		rows.remove(objectId);
	}

	@Override
	public synchronized void deleteByOwner(int ownerId) {
		rows.values().removeIf(r -> r.ownerId() == ownerId);
	}

	public synchronized List<Row> ofOwner(int ownerId) {
		return rows.values().stream().filter(r -> r.ownerId() == ownerId).toList();
	}

	private static Row row(ItemInstance i) {
		return new Row(i.ownerId(), i.objectId(), i.itemId(), i.count(), i.enchant(), i.location().name(),
				i.locationData());
	}
}
