package com.lopez.l2j.game.item;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcItemRepository implements ItemRepository {

	private final JdbcClient jdbc;

	JdbcItemRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<StoredItem> findInventory(int ownerId) {
		return jdbc.sql("""
				SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left
				FROM items WHERE owner_id = :owner AND loc IN ('INVENTORY', 'PAPERDOLL') ORDER BY object_id
				""")
				.param("owner", ownerId)
				.query((rs, i) -> new StoredItem(rs.getInt("object_id"), rs.getInt("item_id"), rs.getInt("count"),
						rs.getInt("enchant_level"), rs.getString("loc"), rs.getInt("loc_data"),
						rs.getInt("custom_type1"), rs.getInt("custom_type2"), rs.getInt("mana_left")))
				.list();
	}

	@Override
	public List<StoredItem> findWarehouse(int ownerId) {
		return jdbc.sql("""
				SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left
				FROM items WHERE owner_id = :owner AND loc = 'WAREHOUSE' ORDER BY object_id
				""")
				.param("owner", ownerId)
				.query((rs, i) -> new StoredItem(rs.getInt("object_id"), rs.getInt("item_id"), rs.getInt("count"),
						rs.getInt("enchant_level"), rs.getString("loc"), rs.getInt("loc_data"),
						rs.getInt("custom_type1"), rs.getInt("custom_type2"), rs.getInt("mana_left")))
				.list();
	}

	@Override
	public List<Paperdoll.Entry> findPaperdoll(int ownerId) {
		return jdbc.sql("SELECT object_id, item_id, loc_data FROM items WHERE owner_id = :owner AND loc = 'PAPERDOLL'")
				.param("owner", ownerId)
				.query((rs, i) -> new Paperdoll.Entry(rs.getInt("loc_data"), rs.getInt("object_id"),
						rs.getInt("item_id")))
				.list();
	}

	@Override
	public void insert(ItemInstance item, String process) {
		jdbc.sql("""
				INSERT INTO items (owner_id, object_id, item_id, count, enchant_level, loc, loc_data, time_of_use,
				  custom_type1, custom_type2, mana_left, process, creator_id, first_owner_id, creation_time)
				VALUES (:owner, :id, :itemId, :count, :enchant, :loc, :locData, 0, :ct1, :ct2, :mana, :process, 0,
				  :owner, :now)
				""")
				.param("owner", item.ownerId())
				.param("id", item.objectId())
				.param("itemId", item.itemId())
				.param("count", item.count())
				.param("enchant", item.enchant())
				.param("loc", item.location().name())
				.param("locData", item.locationData())
				.param("ct1", item.customType1())
				.param("ct2", item.customType2())
				.param("mana", item.mana())
				.param("process", process == null ? "" : process)
				.param("now", System.currentTimeMillis())
				.update();
	}

	@Override
	public void update(ItemInstance item) {
		jdbc.sql("""
				UPDATE items SET owner_id = :owner, count = :count, enchant_level = :enchant, loc = :loc,
				  loc_data = :locData, custom_type1 = :ct1, custom_type2 = :ct2, mana_left = :mana
				WHERE object_id = :id
				""")
				.param("owner", item.ownerId())
				.param("count", item.count())
				.param("enchant", item.enchant())
				.param("loc", item.location().name())
				.param("locData", item.locationData())
				.param("ct1", item.customType1())
				.param("ct2", item.customType2())
				.param("mana", item.mana())
				.param("id", item.objectId())
				.update();
	}

	@Override
	public void delete(int objectId) {
		jdbc.sql("DELETE FROM items WHERE object_id = :id").param("id", objectId).update();
	}

	@Override
	public void deleteByOwner(int ownerId) {
		jdbc.sql("DELETE FROM items WHERE owner_id = :owner").param("owner", ownerId).update();
	}
}
