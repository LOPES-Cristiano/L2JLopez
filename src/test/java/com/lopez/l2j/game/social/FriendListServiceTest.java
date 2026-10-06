package com.lopez.l2j.game.social;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class FriendListServiceTest {

	private FriendListService service;
	private JdbcClient jdbc;

	@BeforeEach
	void setUp() {
		jdbc = mock(JdbcClient.class);
		var statementSpec = mock(JdbcClient.StatementSpec.class);
		when(jdbc.sql(anyString())).thenReturn(statementSpec);
		when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(statementSpec);
		when(statementSpec.update()).thenReturn(1);

		service = new FriendListService(jdbc);
	}

	private PlayerCharacter createPlayer(int id, String name, int accessLevel) {
		PlayerCharacter c = new PlayerCharacter(id, "acc", name, 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", accessLevel, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		return c;
	}

	@Test
	void testAddAndRemoveBlock() {
		var actor = createPlayer(1001, "Alice", 0);

		// Add block
		boolean added = service.addBlock(actor, "Bob");
		assertTrue(added);
		assertTrue(actor.isBlocked("bob"));
		assertTrue(actor.isBlocked("BOB"));

		// Cannot block self
		assertFalse(service.addBlock(actor, "Alice"));

		// Remove block
		boolean removed = service.removeBlock(actor, "Bob");
		assertTrue(removed);
		assertFalse(actor.isBlocked("bob"));
	}

	@Test
	void testBlockAll() {
		var actor = createPlayer(1001, "Alice", 0);
		var sender = createPlayer(1002, "Charlie", 0);

		assertFalse(service.isBlocked(actor, sender));

		actor.setBlockingAll(true);
		assertTrue(service.isBlocked(actor, sender));

		actor.setBlockingAll(false);
		assertFalse(service.isBlocked(actor, sender));
	}

	@Test
	void testGmBypassesBlock() {
		var actor = createPlayer(1001, "Alice", 0);
		var gm = createPlayer(1002, "AdminGuy", 100);

		service.addBlock(actor, "AdminGuy");
		actor.setBlockingAll(true);

		// GM bypasses block always
		assertFalse(service.isBlocked(actor, gm));
	}

	@Test
	void testFriendshipMethodsDoNotThrow() {
		service.addFriendship(1001, "Alice", 1002, "Bob");
		service.removeFriendship(1001, 1002);
	}
}
