package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/** Repositorio e catalogo de itens contra a DDL real das migracoes (items, etcitem, armor, weapon...) no H2. */
@SpringBootTest
@ActiveProfiles("test")
class JdbcItemPersistenceTest {

	@Autowired
	JdbcClient jdbc;

	@Autowired
	ItemRepository repository;

	void runMigration(String file, String table) throws Exception {
		jdbc.sql("DROP TABLE IF EXISTS " + table).update();
		String ddl = new ClassPathResource("db/migration/" + file).getContentAsString(StandardCharsets.UTF_8)
				.replaceAll("(?m)^--.*$", "");
		jdbc.sql(ddl.trim().replaceAll(";\\s*$", "")).update();
	}

	@BeforeEach
	void schema() throws Exception {
		runMigration("V90__items.sql", "items");
		runMigration("V72__etcitem.sql", "etcitem");
		runMigration("V5__armor.sql", "armor");
		runMigration("V122__weapon.sql", "weapon");
		runMigration("V26__char_creation_items.sql", "char_creation_items");
	}

	@Test
	void insertUpdateFindAndDelete() {
		var t = TestItems.table();
		var sword = new ItemInstance(0x10000010, t.get(TestItems.BRANDISH).orElseThrow(), 5, 1);
		var adena = new ItemInstance(0x10000011, t.get(TestItems.ADENA).orElseThrow(), 5, 1000);
		repository.insert(sword, "test");
		repository.insert(adena, "test");
		sword.location(ItemInstance.Location.PAPERDOLL, ItemSlots.LRHAND);
		sword.enchant(3);
		repository.update(sword);

		var rows = repository.findInventory(5);
		assertEquals(2, rows.size());
		var stored = rows.get(0);
		assertEquals(TestItems.BRANDISH, stored.itemId());
		assertEquals("PAPERDOLL", stored.location());
		assertEquals(ItemSlots.LRHAND, stored.locationData());
		assertEquals(3, stored.enchant());
		assertEquals(-1, stored.mana());
		assertEquals(1000, rows.get(1).count());

		var paperdoll = Paperdoll.of(repository.findPaperdoll(5));
		assertEquals(TestItems.BRANDISH, paperdoll.itemId(ItemSlots.RHAND));
		assertEquals(5, jdbc.sql("SELECT first_owner_id FROM items WHERE object_id = :id")
				.param("id", sword.objectId()).query(Integer.class).single());

		repository.delete(adena.objectId());
		assertEquals(1, repository.findInventory(5).size());
		repository.deleteByOwner(5);
		assertTrue(repository.findInventory(5).isEmpty());
	}

	@Test
	void templateTableLoadsLegacyRows() {
		jdbc.sql("""
				INSERT INTO weapon (item_id, name, bodypart, crystallizable, weight, soulshots, spiritshots, material,
				  crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify, shield_def,
				  shield_def_rate, atk_speed, mp_consume, m_dam, duration, price, crystal_count, sellable, dropable,
				  destroyable, tradeable)
				VALUES (10, 'Dagger', 'rhand', 'false', 1160, 1, 1, 'steel', 'none', 5, 10, 'dagger', 12, -3.0, 0, 0,
				  0, 433, 0, 5, -1, 0, 0, 'true', 'true', 'true', 'true')
				""").update();
		jdbc.sql("""
				INSERT INTO armor (item_id, name, bodypart, crystallizable, armor_type, weight, material, crystal_type,
				  p_def, m_def, price) VALUES (112, 'Apprentice''s Earring', 'rear,lear', 'false', 'none', 150, 'silver',
				  'none', 0, 11, 0)
				""").update();
		jdbc.sql("""
				INSERT INTO etcitem (item_id, name, crystallizable, item_type, weight, consume_type, material,
				  crystal_type, duration, price, crystal_count, sellable, dropable, destroyable, tradeable)
				VALUES (57, 'Adena', 'false', 'none', 0, 'asset', 'gold', 'none', -1, 0, 0, 'true', 'true', 'true',
				  'true')
				""").update();
		jdbc.sql("INSERT INTO char_creation_items VALUES (0, 10, 1, 'false'), (-1, 57, 5, 'false'), (0, 999, 1, 'true')")
				.update();

		var table = new JdbcItemTemplateTable(jdbc);
		assertEquals(3, table.size());
		var dagger = table.get(10).orElseThrow();
		assertEquals(ItemTemplate.Kind.WEAPON, dagger.kind());
		assertEquals(433, dagger.atkSpeed());
		assertEquals(ItemTemplate.TYPE2_ACCESSORY, table.get(112).orElseThrow().type2());
		assertTrue(table.get(57).orElseThrow().stackable());
		assertFalse(table.get(1).isPresent());
		var creation = table.creationItems(0);
		assertEquals(2, creation.size(), "999 nao existe e e ignorado");
		assertEquals(57, creation.get(0).itemId(), "classId -1 vem primeiro");
	}
}
