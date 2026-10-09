package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.sevensigns.SevenSignsManager;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShutdownServiceTest {

	private GameWorld world;
	private CharacterService characters;
	private SevenSignsManager sevenSigns;
	private CastleManager castles;
	private ShutdownService shutdownService;

	@BeforeEach
	void setUp() {
		world = mock(GameWorld.class);
		characters = mock(CharacterService.class);
		sevenSigns = mock(SevenSignsManager.class);
		castles = mock(CastleManager.class);

		shutdownService = new ShutdownService(world, characters, sevenSigns, castles);
	}

	@Test
	@DisplayName("Deve agendar restart com contagem regressiva e permitir abortar")
	void testStartRestartAndAbort() {
		assertFalse(shutdownService.isShutdownInProgress());
		assertEquals(ShutdownService.Mode.NONE, shutdownService.getMode());

		shutdownService.startShutdown(120, ShutdownService.Mode.RESTART, "Admin");

		assertTrue(shutdownService.isShutdownInProgress());
		assertEquals(ShutdownService.Mode.RESTART, shutdownService.getMode());
		assertTrue(shutdownService.getSecondsRemaining() > 0);

		boolean aborted = shutdownService.abort("Admin");
		assertTrue(aborted);
		assertFalse(shutdownService.isShutdownInProgress());
		assertEquals(ShutdownService.Mode.ABORT, shutdownService.getMode());
		assertEquals(0, shutdownService.getSecondsRemaining());
	}

	@Test
	@DisplayName("Abortar quando nao ha shutdown em andamento deve retornar false")
	void testAbortWhenNoShutdown() {
		assertFalse(shutdownService.abort("Admin"));
	}

	@Test
	@DisplayName("Alternar modo OnlyGM deve atualizar status e configuracao")
	void testOnlyGmToggle() {
		shutdownService.setOnlyGm(true);
		assertTrue(shutdownService.isOnlyGm());
		assertTrue(Config.SERVER_GM_ONLY);

		shutdownService.setOnlyGm(false);
		assertFalse(shutdownService.isOnlyGm());
		assertFalse(Config.SERVER_GM_ONLY);
	}

	@Test
	@DisplayName("KickAll deve desconectar jogadores nao-GM e salvar seus dados")
	void testKickAll() {
		GameSession session1 = mock(GameSession.class);
		PlayerCharacter player1 = mock(PlayerCharacter.class);
		when(session1.character()).thenReturn(player1);
		when(player1.isGm()).thenReturn(false);

		GameSession gmSession = mock(GameSession.class);
		PlayerCharacter gm = mock(PlayerCharacter.class);
		when(gmSession.character()).thenReturn(gm);
		when(gm.isGm()).thenReturn(true);

		when(world.allPlayers()).thenReturn(List.of(session1, gmSession));

		int kicked = shutdownService.kickAll();

		assertEquals(1, kicked);
		verify(characters, times(1)).save(player1, true);
		verify(session1, times(1)).kick();
		verify(gmSession, never()).kick();
	}
}
