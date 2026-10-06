package com.lopez.l2j.game.fortress;

import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.fortress.siege.FortressSiegeService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FortressServiceTest {

	private FortressService fortressService;
	private FortressSiegeService fortressSiegeService;

	private PlayerCharacter dummyPlayer(int id, String name) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, 75, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	private ItemTemplate adenaTemplate() {
		return ItemTemplate.etc(ItemTemplate.ADENA_ID, ItemTemplate.ADENA_ID, "Adena", "none", "asset", 0, "none", 0, true, true, true, true);
	}

	@BeforeEach
	void setUp() {
		fortressService = new FortressService(null);
		fortressSiegeService = new FortressSiegeService(fortressService, null);
	}

	@Test
	void testAll21FortressesInitialized() {
		assertEquals(21, fortressService.size());
		assertTrue(fortressService.getFortressById(101).isPresent()); // Shanty
		assertEquals("Shanty", fortressService.getFortressById(101).orElseThrow().name());
		assertTrue(fortressService.getFortressById(121).isPresent()); // Monastic
	}

	@Test
	void testFortressOwnershipAndFunctions() {
		int fortId = 101;
		fortressService.setOwner(fortId, 50);
		assertEquals(50, fortressService.getFortressById(fortId).orElseThrow().ownerClanId());

		// Configura independencia
		fortressService.setFortState(fortId, FortressRecord.STATE_INDEPENDENT);
		assertEquals(FortressRecord.STATE_INDEPENDENT, fortressService.getFortressById(fortId).orElseThrow().state());

		// Funcao de Teleporte
		fortressService.setFunction(fortId, FortressService.FUNC_TELEPORT, 2, 1000, 1, 3600000L);
		var func = fortressService.getFunction(fortId, FortressService.FUNC_TELEPORT);
		assertTrue(func.isPresent());
		assertEquals(2, func.get().level());

		fortressService.removeFunction(fortId, FortressService.FUNC_TELEPORT);
		assertTrue(fortressService.getFunction(fortId, FortressService.FUNC_TELEPORT).isEmpty());
	}

	@Test
	void testFortressSiegeEngineLifecycle() {
		int fortId = 104; // Valley Fortress
		PlayerCharacter player = dummyPlayer(100, "SiegeLeader");
		Clan clan = new Clan(77, "ConquerorClan", 100, "SiegeLeader", 5);

		// Adiciona 500.000 Adena ao jogador
		player.inventory().add(new ItemInstance(1, adenaTemplate(), player.objectId(), 500_000));

		// Registra no cerco
		assertTrue(fortressSiegeService.registerAttacker(fortId, player, clan));
		// Taxa de 250.000 adenas deve ter sido descontada (restam 250.000)
		assertEquals(250_000, player.inventory().byItemId(ItemTemplate.ADENA_ID).orElseThrow().count());

		// Inicia batalha
		assertTrue(fortressSiegeService.startSiege(fortId));
		assertEquals(SiegeStatus.IN_PROGRESS, fortressSiegeService.getSiege(fortId).orElseThrow().status());

		// Tenta erguer a bandeira antes de desligar reatores e comandantes (falha)
		assertFalse(fortressSiegeService.raiseCombatFlag(fortId, 77));

		// Desliga os 3 reatores
		fortressSiegeService.disableReactor(fortId);
		fortressSiegeService.disableReactor(fortId);
		fortressSiegeService.disableReactor(fortId);

		// Derrota os 3 comandantes
		fortressSiegeService.defeatCommander(fortId);
		fortressSiegeService.defeatCommander(fortId);
		fortressSiegeService.defeatCommander(fortId);

		// Agora pode erguer a bandeira com sucesso!
		assertTrue(fortressSiegeService.raiseCombatFlag(fortId, 77));

		// O cerco termina e o cla 77 passa a ser o novo dono da fortaleza
		assertEquals(SiegeStatus.FINISHED, fortressSiegeService.getSiege(fortId).orElseThrow().status());
		assertEquals(77, fortressService.getFortressById(fortId).orElseThrow().ownerClanId());
	}
}
