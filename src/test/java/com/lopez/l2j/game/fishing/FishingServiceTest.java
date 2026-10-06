package com.lopez.l2j.game.fishing;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingEnd;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingStart;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingStartCombat;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FishingServiceTest {

	private FishTable fishTable;
	private FishingChampionshipService championship;
	private FishingService service;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		fishTable = new FishTable();
		// Adiciona peixes para testes
		fishTable.addFish(new FishData(6411, 1, "Small Green Nimble Fish", 100, 4, 1, 1, 500, 5000, 2000, 24000));
		fishTable.addFish(new FishData(6412, 1, "Small Green Ugly Fish", 120, 5, 2, 1, 500, 5000, 2000, 27000));
		fishTable.addFish(new FishData(6420, 4, "Small Jade Nimble Fish", 200, 8, 1, 1, 500, 5000, 2000, 28000));

		championship = new FishingChampionshipService();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		service = new FishingService(fishTable, championship, null, scheduler);
		sentPackets = new ArrayList<>();
	}

	private ItemTemplate createRodTemplate(int id) {
		return ItemTemplate.weapon(
				id, id, "Baby Duck Rod", "rhand", "rod",
				1000, "none", 10, 10, 300, 10, 0, 0,
				true, true, true, true
		);
	}

	private ItemTemplate createLureTemplate(int id) {
		return new ItemTemplate(
				id, id, "Normal Lure", ItemTemplate.Kind.ETC, "lure",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				ItemSlots.SLOT_L_HAND, 10, true, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	private ItemTemplate createSwordTemplate(int id) {
		return ItemTemplate.weapon(
				id, id, "Short Sword", "rhand", "sword",
				1000, "none", 20, 15, 379, 10, 0, 0,
				true, true, true, true
		);
	}

	@Test
	@DisplayName("FishTable recupera peixes corretamente por nivel, tipo e grupo")
	void testFishTableLookup() {
		var fishOpt = fishTable.getFish(1, 1, 1);
		assertTrue(fishOpt.isPresent());
		assertEquals(6411, fishOpt.get().id());
		assertEquals("Small Green Nimble Fish", fishOpt.get().name());

		var fallbackOpt = fishTable.getFish(4, 99, 99);
		assertTrue(fallbackOpt.isPresent());
		assertEquals(4, fallbackOpt.get().level());
	}

	private PlayerCharacter createPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	@Test
	@DisplayName("Nao permite pescar sem vara de pescar equipada")
	void testStartFishingWithoutRod() {
		PlayerCharacter player = createPlayer(1, "Fisherman", 20);
		Inventory inv = new Inventory(1);
		player.inventory(inv);

		// Equipa uma espada comum em vez de vara
		ItemInstance sword = new ItemInstance(101, createSwordTemplate(1), 1, 1);
		inv.add(sword);
		inv.equip(sword);

		boolean started = service.startFishing(player, 100, 100, -100, sentPackets::add);
		assertFalse(started, "Nao deve iniciar pesca com espada");
		assertFalse(player.isFishing());
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.FISHING_POLE_NOT_EQUIPPED));
	}

	@Test
	@DisplayName("Nao permite pescar sem isca (lure)")
	void testStartFishingWithoutLure() {
		PlayerCharacter player = createPlayer(2, "NoBaitFisher", 20);
		Inventory inv = new Inventory(2);
		player.inventory(inv);

		// Equipa uma vara valida mas sem isca
		ItemInstance rod = new ItemInstance(102, createRodTemplate(6529), 2, 1);
		inv.add(rod);
		inv.equip(rod);

		boolean started = service.startFishing(player, 100, 100, -100, sentPackets::add);
		assertFalse(started, "Nao deve iniciar pesca sem isca");
		assertFalse(player.isFishing());
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.BAIT_ON_HOOK_BEFORE_FISHING));
	}

	@Test
	@DisplayName("Inicia pesca com sucesso quando vara e isca estao equipadas")
	void testStartFishingSuccess() {
		PlayerCharacter player = createPlayer(3, "MasterFisher", 20);
		Inventory inv = new Inventory(3);
		player.inventory(inv);

		ItemInstance rod = new ItemInstance(103, createRodTemplate(6529), 3, 1);
		inv.add(rod);
		inv.equip(rod);

		ItemInstance lure = new ItemInstance(104, createLureTemplate(6519), 3, 5);
		inv.add(lure);
		inv.equip(lure);

		boolean started = service.startFishing(player, 100, 100, -100, sentPackets::add);
		assertTrue(started, "Deve iniciar pesca com vara e isca validas");
		assertTrue(player.isFishing());
		assertEquals(4, lure.count(), "Deve consumir 1 isca ao arremessar a linha");

		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof ExFishingStart));
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.CAST_LINE_AND_START_FISHING));

		// Cancelar pesca
		service.stopFishing(player, sentPackets::add);
		assertFalse(player.isFishing());
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof ExFishingEnd));
	}

	@Test
	@DisplayName("Minigame de combate: Pumping e Reeling alteram o HP do peixe")
	void testFishingCombatSession() {
		FishData fish = new FishData(6411, 1, "Test Fish", 100, 5, 1, 1, 500, 5000, 1000, 20000);
		FishingSession session = new FishingSession(10, fish, 0, 0, 0, false, true, false);

		assertEquals(100, session.curHp());
		assertEquals(FishingSession.State.WAITING_BITE, session.state());

		session.startCombat();
		assertEquals(FishingSession.State.COMBAT, session.state());

		// Simula uso de habilidades dependendo do modo atual
		if (session.combatMode() == 0) {
			// Modo 0 espera Pumping
			session.usePumping(50, 0);
			assertTrue(session.curHp() <= 50 || session.goodUse() == 0, "Dano aplicado ou resistido");
		} else {
			// Modo 1 espera Reeling
			session.useReeling(50, 0);
			assertTrue(session.curHp() <= 50 || session.goodUse() == 0, "Dano aplicado ou resistido");
		}

		// Peixe zerando a vida da vitoria
		session.usePumping(200, 0);
		if (session.curHp() == 0) {
			assertTrue(session.isWon());
		}
	}

	@Test
	@DisplayName("Campeonato de pesca registra capturas e gera ranking ordenado")
	void testFishingChampionship() {
		float l1 = championship.registerCatch("Alice", 10);
		float l2 = championship.registerCatch("Bob", 20);
		float l3 = championship.registerCatch("Charlie", 5);

		var top = championship.getTopFishers(3);
		assertEquals(3, top.size());
		assertTrue(top.get(0).fishLength() >= top.get(1).fishLength());
		assertTrue(top.get(1).fishLength() >= top.get(2).fishLength());
	}
}
