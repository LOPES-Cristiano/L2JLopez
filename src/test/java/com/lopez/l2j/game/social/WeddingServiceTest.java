package com.lopez.l2j.game.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class WeddingServiceTest {

	private WeddingService service;
	private JdbcClient jdbc;

	@BeforeEach
	void setUp() {
		jdbc = mock(JdbcClient.class);
		var statementSpec = mock(JdbcClient.StatementSpec.class);
		when(jdbc.sql(anyString())).thenReturn(statementSpec);
		when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(statementSpec);
		when(statementSpec.update()).thenReturn(1);

		var mappedQuerySpec = mock(JdbcClient.MappedQuerySpec.class);
		when(statementSpec.query(org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<WeddingService.Couple>>any())).thenReturn(mappedQuerySpec);
		when(mappedQuerySpec.list()).thenReturn(List.of());

		service = new WeddingService(jdbc);
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	@Test
	void testEngageAndMarryAndDivorce() {
		var romeo = createPlayer(101, "Romeo");
		var juliet = createPlayer(102, "Juliet");

		assertFalse(service.isMarried(101));
		assertFalse(service.isMarried(102));

		var couple = service.engage(romeo, juliet);
		assertNotNull(couple);
		assertEquals(102, service.getPartnerId(101));
		assertEquals(101, service.getPartnerId(102));
		assertFalse(couple.married());

		// Duplicate engagement should fail
		assertThrows(IllegalStateException.class, () -> service.engage(romeo, juliet));

		// Marry
		boolean marriedOk = service.marry(couple.id());
		assertTrue(marriedOk);
		assertTrue(service.isMarried(101));
		assertTrue(service.isMarried(102));

		// Teleport conditions
		assertTrue(service.canTeleportToPartner(romeo, juliet));
		romeo.karma(500);
		assertFalse(service.canTeleportToPartner(romeo, juliet));
		romeo.karma(0);
		assertTrue(service.canTeleportToPartner(romeo, juliet));

		// Divorce
		service.divorce(couple.id());
		assertFalse(service.isMarried(101));
		assertFalse(service.isMarried(102));
		assertEquals(0, service.getPartnerId(101));
	}
}
