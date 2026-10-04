package com.lopez.l2j.game.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.CharacterRepository.NewCharacter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/** Roda o repositorio contra a DDL real da migracao V50 (characters) no H2 em modo MariaDB. */
@SpringBootTest
@ActiveProfiles("test")
class JdbcCharacterRepositoryTest {

	@Autowired
	CharacterRepository repository;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void schema() throws Exception {
		jdbc.sql("DROP TABLE IF EXISTS characters").update();
		String ddl = new ClassPathResource("db/migration/V50__characters.sql").getContentAsString(StandardCharsets.UTF_8)
				.replaceAll("(?m)^--.*$", "");
		jdbc.sql(ddl.trim().replaceAll(";\\s*$", "")).update();
	}

	NewCharacter human(String account, String name) {
		return new NewCharacter(account, name, 0, 0, false, 1, 2, 3, 80, 30, 32, -71338, 258271, -3104, false);
	}

	@Test
	void createsListsAndPersistsState() {
		var a = repository.create(human("acc", "Alpha"));
		var b = repository.create(human("acc", "Beta"));
		repository.create(human("other", "Gamma"));
		assertTrue(a.objectId() >= JdbcCharacterRepository.FIRST_OBJECT_ID);
		assertEquals(a.objectId() + 1, b.objectId());

		var list = repository.findByAccount("acc");
		assertEquals(2, list.size());
		var loaded = list.get(0);
		assertEquals("Alpha", loaded.name());
		assertEquals(1, loaded.level());
		assertEquals(80, loaded.maxHp());
		assertEquals(80.0, loaded.currentHp());
		assertEquals(2, loaded.hairStyle());
		assertEquals(-71338, loaded.x());
		assertEquals("", loaded.title());

		loaded.moveTo(1, 2, 3);
		loaded.heading(500);
		repository.saveState(loaded, true);
		var again = repository.findByAccount("acc").get(0);
		assertEquals(1, again.x());
		assertEquals(500, again.heading());
		assertEquals(1, jdbc.sql("SELECT online FROM characters WHERE charId = :id").param("id", a.objectId())
				.query(Integer.class).single());
	}

	@Test
	void nameCheckIsCaseInsensitive() {
		repository.create(human("acc", "Alpha"));
		assertTrue(repository.nameExists("alpha"));
		assertFalse(repository.nameExists("Beta"));
	}

	@Test
	void deleteAndDeleteTime() {
		var a = repository.create(human("acc", "Alpha"));
		repository.updateDeleteTime(a.objectId(), 123L);
		assertEquals(123L, repository.findByAccount("acc").get(0).deleteTime());
		repository.delete(a.objectId());
		assertTrue(repository.findByAccount("acc").isEmpty());
	}
}
