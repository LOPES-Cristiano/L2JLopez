package com.lopez.l2j.game.castle.mercenary;

import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MercenaryServiceTest {

	private CastleManager castleManager;
	private SiegeService siegeService;
	private MercenaryService mercenaryService;

	private PlayerCharacter dummyPlayer(int id, String name) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, 75, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	private ItemTemplate dummyTemplate(int itemId, String name) {
		return ItemTemplate.etc(itemId, itemId, name, "ticket", "none", 1, "none", 0, false, false, false, false);
	}

	@BeforeEach
	void setUp() {
		castleManager = new CastleManager(null);
		siegeService = new SiegeService(castleManager, null, null);
		mercenaryService = new MercenaryService(castleManager, siegeService, null);
	}

	@Test
	void testHireMercenarySuccess() {
		PlayerCharacter lord = dummyPlayer(100, "CastleLord");
		Clan clan = new Clan(1, "LordClan", 100, "CastleLord", 5);
		clan.castleId(CastleManager.GLUDIO);

		// Adiciona bilhete ao inventario
		int ticketId = 3960;
		lord.inventory().add(new ItemInstance(1, dummyTemplate(ticketId, "Merc Ticket"), lord.objectId(), 1));

		assertTrue(mercenaryService.hireMercenary(lord, clan, ticketId, 1000, 2000, -1000, 0));
		assertEquals(1, mercenaryService.getHiredMercenaries(CastleManager.GLUDIO).size());
		// Bilhete deve ter sido consumido
		assertTrue(lord.inventory().byItemId(ticketId).isEmpty());
	}

	@Test
	void testHireMercenaryFailsWithoutCastle() {
		PlayerCharacter player = dummyPlayer(101, "NoCastle");
		Clan clan = new Clan(2, "HomelessClan", 101, "NoCastle", 5);
		// clan.castleId() == 0

		int ticketId = 3960;
		player.inventory().add(new ItemInstance(1, dummyTemplate(ticketId, "Merc Ticket"), player.objectId(), 1));

		assertFalse(mercenaryService.hireMercenary(player, clan, ticketId, 1000, 2000, -1000, 0));
		assertEquals(0, mercenaryService.getHiredMercenaries(CastleManager.GLUDIO).size());
	}

	@Test
	void testHireMercenaryFailsDuringActiveSiege() {
		PlayerCharacter lord = dummyPlayer(100, "CastleLord");
		Clan clan = new Clan(1, "LordClan", 100, "CastleLord", 5);
		clan.castleId(CastleManager.GLUDIO);

		int ticketId = 3960;
		lord.inventory().add(new ItemInstance(1, dummyTemplate(ticketId, "Merc Ticket"), lord.objectId(), 1));

		// Inicia o cerco
		siegeService.startSiege(CastleManager.GLUDIO);

		assertFalse(mercenaryService.hireMercenary(lord, clan, ticketId, 1000, 2000, -1000, 0));
	}

	@Test
	void testClearCastleMercenaries() {
		PlayerCharacter lord = dummyPlayer(100, "CastleLord");
		Clan clan = new Clan(1, "LordClan", 100, "CastleLord", 5);
		clan.castleId(CastleManager.GLUDIO);

		int ticketId = 3960;
		lord.inventory().add(new ItemInstance(1, dummyTemplate(ticketId, "Merc Ticket"), lord.objectId(), 1));
		mercenaryService.hireMercenary(lord, clan, ticketId, 1000, 2000, -1000, 0);

		assertEquals(1, mercenaryService.getHiredMercenaries(CastleManager.GLUDIO).size());
		mercenaryService.clearCastleMercenaries(CastleManager.GLUDIO);
		assertEquals(0, mercenaryService.getHiredMercenaries(CastleManager.GLUDIO).size());
	}
}
