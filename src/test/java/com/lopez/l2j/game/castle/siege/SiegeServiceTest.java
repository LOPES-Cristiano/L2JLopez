package com.lopez.l2j.game.castle.siege;

import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiegeServiceTest {

	private CastleManager castleManager;
	private SiegeService siegeService;

	@BeforeEach
	void setUp() {
		castleManager = new CastleManager(null);
		siegeService = new SiegeService(castleManager, null, null);
	}

	@Test
	void testAllCastlesHaveSiegeRecords() {
		assertEquals(9, siegeService.allSieges().size());
		assertTrue(siegeService.getSiege(CastleManager.GLUDIO).isPresent());
		assertTrue(siegeService.getSiege(CastleManager.ADEN).isPresent());
	}

	@Test
	void testRegisterAttackerValidation() {
		Clan clanLowLvl = new Clan(101, "LowLevelClan", 1, "Leader1", 3);
		assertFalse(siegeService.registerAttacker(CastleManager.GLUDIO, clanLowLvl));

		Clan validClan = new Clan(102, "ValidClan", 2, "Leader2", 5);
		// Adiciona 15 membros
		for (int i = 1; i <= 15; i++) {
			validClan.addMember(new com.lopez.l2j.game.clan.ClanMember(i, "Member" + i, 60, 10, "", false, 0));
		}
		assertTrue(siegeService.registerAttacker(CastleManager.GLUDIO, validClan));
		assertTrue(siegeService.isAttacker(CastleManager.GLUDIO, 102));
	}

	@Test
	void testDefenderRegistrationAndApproval() {
		// Define dono do castelo
		castleManager.setOwner(CastleManager.DION, 200);
		// Reinicia os cercos com o dono
		siegeService = new SiegeService(castleManager, null, null);

		Clan defClan = new Clan(201, "DefenderClan", 3, "Leader3", 5);

		// Registra como defensor pendente
		assertTrue(siegeService.registerDefender(CastleManager.DION, defClan));

		// Outro clan tenta aprovar (falha)
		assertFalse(siegeService.approveDefender(CastleManager.DION, 201, 999));

		// Lorde aprova
		assertTrue(siegeService.approveDefender(CastleManager.DION, 201, 200));
		assertTrue(siegeService.isDefender(CastleManager.DION, 201));
	}

	@Test
	void testSiegeLifecycleAndMidVictory() {
		int castleId = CastleManager.GIRAN;
		castleManager.setOwner(castleId, 500);
		siegeService = new SiegeService(castleManager, null, null);

		Clan attacker = new Clan(600, "AttackingClan", 4, "Leader4", 5);
		for (int i = 1; i <= 15; i++) {
			attacker.addMember(new com.lopez.l2j.game.clan.ClanMember(i + 50, "Attacker" + i, 75, 10, "", false, 0));
		}
		assertTrue(siegeService.registerAttacker(castleId, attacker));

		// Inicia cerco
		assertTrue(siegeService.startSiege(castleId));
		assertEquals(SiegeStatus.IN_PROGRESS, siegeService.getSiege(castleId).orElseThrow().status());

		// Mid-Victory: atacante grava artefato
		assertTrue(siegeService.midVictory(castleId, 600));

		// 600 se torna dono e defensor, 500 vira atacante
		assertEquals(600, castleManager.getCastleById(castleId).orElseThrow().ownerClanId());
		assertTrue(siegeService.isDefender(castleId, 600));
		assertTrue(siegeService.isAttacker(castleId, 500));
		assertEquals(SiegeStatus.IN_PROGRESS, siegeService.getSiege(castleId).orElseThrow().status());

		// Fim do cerco confirma vitoria definitiva
		assertTrue(siegeService.endSiege(castleId, 600));
		assertEquals(SiegeStatus.FINISHED, siegeService.getSiege(castleId).orElseThrow().status());
	}
}
