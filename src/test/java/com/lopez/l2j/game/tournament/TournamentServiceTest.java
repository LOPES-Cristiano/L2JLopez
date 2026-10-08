package com.lopez.l2j.game.tournament;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TournamentServiceTest {

	private TournamentService tournamentService;
	private InventoryService inventoryService;

	private PlayerCharacter p1, p2, p3, p4, p5, p6;

	@BeforeEach
	void setUp() {
		inventoryService = mock(InventoryService.class);
		tournamentService = new TournamentService(inventoryService);

		p1 = createMockPlayer(101, "Gladiator1", 1000, 2000, -100);
		p2 = createMockPlayer(102, "Mage1", 1000, 2000, -100);
		p3 = createMockPlayer(103, "Archer1", 1000, 2000, -100);

		p4 = createMockPlayer(201, "Gladiator2", 5000, 6000, -100);
		p5 = createMockPlayer(202, "Mage2", 5000, 6000, -100);
		p6 = createMockPlayer(203, "Archer2", 5000, 6000, -100);
	}

	private PlayerCharacter createMockPlayer(int id, String name, int x, int y, int z) {
		PlayerCharacter p = mock(PlayerCharacter.class);
		Inventory inv = mock(Inventory.class);
		when(p.objectId()).thenReturn(id);
		when(p.name()).thenReturn(name);
		when(p.x()).thenReturn(x);
		when(p.y()).thenReturn(y);
		when(p.z()).thenReturn(z);
		when(p.maxHp()).thenReturn(3000);
		when(p.maxMp()).thenReturn(1500);
		when(p.maxCp()).thenReturn(2000);
		when(p.isDead()).thenReturn(false);
		when(p.inventory()).thenReturn(inv);
		return p;
	}

	@Test
	void testSolo1x1RegistrationAndMatchLifecycle() {
		// Inscrição do Jogador 1
		boolean reg1 = tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(p1));
		assertTrue(reg1);
		assertTrue(tournamentService.isRegistered(p1.objectId()));
		assertFalse(tournamentService.isInMatch(p1.objectId()));

		// Tentar registrar novamente enquanto já inscrito deve falhar
		assertFalse(tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(p1)));

		// Inscrição do Jogador 2 -> Dispara matchmaking automático
		boolean reg2 = tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(p4));
		assertTrue(reg2);

		// Ambos agora estão em partida ativa
		assertTrue(tournamentService.isInMatch(p1.objectId()));
		assertTrue(tournamentService.isInMatch(p4.objectId()));
		assertFalse(tournamentService.isRegistered(p1.objectId()));

		var match = tournamentService.getPlayerMatch(p1.objectId());
		assertNotNull(match);
		assertEquals(p1.objectId(), match.teamRed().members().get(0).objectId());
		assertEquals(p4.objectId(), match.teamBlue().members().get(0).objectId());

		// Inicia combate
		tournamentService.startFight(match.matchId());
		assertEquals(TournamentService.TournamentState.FIGHTING, match.state());

		// Jogador 4 morre -> Jogador 1 (Team Red) vence
		tournamentService.onPlayerDeath(p4.objectId());

		// Vencedor recebe recompensa de Adena orgânica
		verify(inventoryService).addItem(eq(p1.inventory()), eq(57), eq(TournamentService.REWARD_ADENA_AMOUNT_1X1), eq("TournamentReward"));
		// Ambos retornam às coordenadas de origem
		verify(p1).teleport(1000, 2000, -100);
		verify(p4).teleport(5000, 6000, -100);

		assertFalse(tournamentService.isInMatch(p1.objectId()));
		assertFalse(tournamentService.isInMatch(p4.objectId()));
	}

	@Test
	void testTrio3x3RegistrationAndCombat() {
		List<PlayerCharacter> teamA = List.of(p1, p2, p3);
		List<PlayerCharacter> teamB = List.of(p4, p5, p6);

		assertTrue(tournamentService.register(TournamentService.TournamentMode.TRIO_3X3, teamA));
		assertTrue(tournamentService.register(TournamentService.TournamentMode.TRIO_3X3, teamB));

		var match = tournamentService.getPlayerMatch(p1.objectId());
		assertNotNull(match);
		assertEquals(TournamentService.TournamentMode.TRIO_3X3, match.mode());

		tournamentService.startFight(match.matchId());

		// Elimina 2 membros do time B, partida continua
		tournamentService.onPlayerDeath(p4.objectId());
		tournamentService.onPlayerDeath(p5.objectId());
		assertTrue(tournamentService.isInMatch(p1.objectId()));

		// Elimina o último membro do time B -> Fim de partida
		tournamentService.onPlayerDeath(p6.objectId());

		assertFalse(tournamentService.isInMatch(p1.objectId()));
		verify(inventoryService).addItem(eq(p1.inventory()), eq(57), eq(TournamentService.REWARD_ADENA_AMOUNT_3X3), eq("TournamentReward"));
		verify(inventoryService).addItem(eq(p2.inventory()), eq(57), eq(TournamentService.REWARD_ADENA_AMOUNT_3X3), eq("TournamentReward"));
		verify(inventoryService).addItem(eq(p3.inventory()), eq(57), eq(TournamentService.REWARD_ADENA_AMOUNT_3X3), eq("TournamentReward"));
	}

	@Test
	void testUnregisterFromQueue() {
		tournamentService.register(TournamentService.TournamentMode.SOLO_1X1, List.of(p1));
		assertTrue(tournamentService.isRegistered(p1.objectId()));

		assertTrue(tournamentService.unregister(p1.objectId()));
		assertFalse(tournamentService.isRegistered(p1.objectId()));
		assertFalse(tournamentService.unregister(p1.objectId()));
	}

	@Test
	void testBuildTournamentHtml() {
		String html = tournamentService.buildTournamentHtml(p1);
		assertTrue(html.contains(".tournament"));
		assertTrue(html.contains("tournament_join 1x1"));
		assertTrue(html.contains("tournament_join 3x3"));
		assertTrue(html.contains("tournament_join 5x5"));
	}
}
