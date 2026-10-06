package com.lopez.l2j.game.pet;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PetServiceTest {

	private PetDataTable table;
	private PetService service;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		table = new PetDataTable();
		// Configura estatisticas basicas para teste manual
		// Wolf Lv 1 (expMax: 0, hpMax: 100, mpMax: 50, feedMax: 300)
		table.addStat(new PetStatTemplate("wolf", PetDataTable.WOLF_ID, 1, 0L, 100, 50,
				10, 10, 5, 5, 40, 35, 40, 140, 300, 333, 300, 2, 2, 50000, 2, 1, 0));
		// Wolf Lv 2 (expMax: 500L, hpMax: 120, mpMax: 60, feedMax: 300)
		table.addStat(new PetStatTemplate("wolf", PetDataTable.WOLF_ID, 2, 500L, 120, 60,
				15, 12, 6, 6, 41, 36, 40, 140, 300, 333, 300, 2, 2, 50000, 2, 1, 0));
		// Wolf Lv 55 (expMax: 50000L, hpMax: 1500, mpMax: 500, feedMax: 1000)
		table.addStat(new PetStatTemplate("wolf", PetDataTable.WOLF_ID, 55, 50000L, 1500, 500,
				100, 80, 50, 50, 60, 50, 40, 140, 300, 333, 1000, 5, 3, 50000, 5, 3, 0));

		// Great Wolf Lv 55
		table.addStat(new PetStatTemplate("great_wolf", PetDataTable.GREAT_WOLF_ID, 55, 50000L, 2500, 800,
				200, 150, 80, 80, 70, 60, 50, 160, 350, 333, 1500, 6, 4, 80000, 8, 5, 0));

		// Strider Lv 55 (Montavel)
		table.addStat(new PetStatTemplate("strider", PetDataTable.STRIDER_WIND_ID, 55, 50000L, 3000, 1000,
				150, 120, 60, 60, 55, 50, 40, 250, 300, 333, 1200, 4, 2, 70000, 6, 4, 0));

		service = new PetService(table, null, null);
		sentPackets = new ArrayList<>();
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 60, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	private ItemTemplate createItemTemplate(int id, String name) {
		return new ItemTemplate(
				id, id, name, ItemTemplate.Kind.ETC, "petfood",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				0, 10, true, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@Test
	@DisplayName("Carregamento do XML real pet_stats.xml")
	void testLoadRealPetStatsXml() {
		PetDataTable realTable = new PetDataTable();
		realTable.load();
		assertTrue(realTable.size() > 500, "Deve conter centenas de registros de stats de mascotes");

		var wolfLv1 = realTable.getStat(PetDataTable.WOLF_ID, 1);
		assertTrue(wolfLv1.isPresent());
		assertEquals(31, wolfLv1.get().hpMax());
		assertEquals(248, wolfLv1.get().feedMax());
	}

	@Test
	@DisplayName("Regras de alimentacao: consumir alimento correto aumenta nivel de fome")
	void testPetFeeding() {
		PlayerCharacter player = createPlayer(1, "MasterTamer");
		Inventory inv = new Inventory(1);
		player.inventory(inv);

		// Adiciona 5 Wolf Food (ID 2515)
		ItemInstance food = new ItemInstance(10, createItemTemplate(PetDataTable.FOOD_WOLF, "Wolf Food"), 1, 5);
		inv.add(food);

		int collarObjectId = 999;
		// Inicializa o pet
		PetDataRecord pet = service.createOrGetDefault(collarObjectId, PetDataTable.WOLF_ID, "Fenrir");
		assertEquals(300, pet.fed());

		// Simula pet com fome baixa
		service.savePet(pet.withFed(50));

		// Tenta alimentar com comida errada (ID 4038 Hatchling food) -> recusa
		boolean wrongFood = service.feed(player, collarObjectId, PetDataTable.WOLF_ID, PetDataTable.FOOD_HATCHLING, sentPackets::add);
		assertFalse(wrongFood);
		assertEquals(5, food.count(), "Nao deve consumir comida errada");

		// Alimenta com comida correta
		boolean rightFood = service.feed(player, collarObjectId, PetDataTable.WOLF_ID, PetDataTable.FOOD_WOLF, sentPackets::add);
		assertTrue(rightFood);
		assertEquals(4, food.count(), "Deve consumir 1 Wolf Food");

		// Fome aumentou de 50 para 50 + 60 = 110
		PetDataRecord updated = service.getPet(collarObjectId).orElseThrow();
		assertTrue(updated.fed() > 50);
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof CreatureSay cs && cs.text().contains("comeu com satisfacao")));
	}

	@Test
	@DisplayName("Ganho de experiencia e subida de nivel (Level Up)")
	void testAddExpAndLevelUp() {
		int collarObjectId = 1001;
		PetDataRecord pet = service.createOrGetDefault(collarObjectId, PetDataTable.WOLF_ID, "Wolfie");
		assertEquals(1, pet.level());
		assertEquals(0, pet.exp());

		// Concede 600 EXP (Lv 2 requer 500 EXP)
		boolean leveledUp = service.addExp(collarObjectId, PetDataTable.WOLF_ID, 600L, sentPackets::add);
		assertTrue(leveledUp, "Pet deve subir de nivel");

		PetDataRecord updated = service.getPet(collarObjectId).orElseThrow();
		assertEquals(2, updated.level());
		assertEquals(600L, updated.exp());
		assertEquals(120, updated.curHp()); // hpMax do Lv 2

		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof CreatureSay cs && cs.text().contains("alcancou o nivel 2")));
	}

	@Test
	@DisplayName("Montaria e desmontaria em mascotes compativeis")
	void testMountAndDismount() {
		PlayerCharacter player = createPlayer(2, "Rider");
		assertFalse(player.isMounted());

		// Lobo nao e montavel
		boolean mountWolf = service.mount(player, PetDataTable.WOLF_ID, sentPackets::add);
		assertFalse(mountWolf);
		assertFalse(player.isMounted());

		// Strider e montavel
		boolean mountStrider = service.mount(player, PetDataTable.STRIDER_WIND_ID, sentPackets::add);
		assertTrue(mountStrider);
		assertTrue(player.isMounted());
		assertEquals(PetDataTable.STRIDER_WIND_ID, player.mountNpcId());

		// Desmontar
		boolean dismounted = service.dismount(player, sentPackets::add);
		assertTrue(dismounted);
		assertFalse(player.isMounted());
		assertEquals(0, player.mountNpcId());
	}

	@Test
	@DisplayName("Evolucao de mascote (Wolf -> Great Wolf no Lv 55)")
	void testPetEvolution() {
		PlayerCharacter player = createPlayer(3, "BeastMaster");
		int collarObjectId = 1002;

		// Cria pet e ajusta para Lv 55
		PetDataRecord pet = service.createOrGetDefault(collarObjectId, PetDataTable.WOLF_ID, "Alpha");
		service.savePet(new PetDataRecord(collarObjectId, "Alpha", 55, 1500, 500, 60000L, 0, 1000, 0, 0, 0));

		var evolvedOpt = service.evolve(player, collarObjectId, PetDataTable.WOLF_ID, PetDataTable.GREAT_WOLF_ID, "Great Alpha");
		assertTrue(evolvedOpt.isPresent());

		PetDataRecord evolved = evolvedOpt.get();
		assertEquals("Great Alpha", evolved.name());
		assertEquals(2500, evolved.curHp()); // HP max do Great Wolf Lv 55
	}
}
