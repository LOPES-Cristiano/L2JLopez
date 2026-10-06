package com.lopez.l2j.game.summon;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SummonItemServiceTest {

	private SummonItemsTable table;
	private SummonItemService service;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		table = new SummonItemsTable();
		// Adiciona itens invocadores para o teste
		table.add(new SummonItem(5560, 13006, 0)); // Christmas tree (static)
		table.add(new SummonItem(2375, 12077, 1)); // Wolf collar (pet)
		table.add(new SummonItem(4422, 12526, 1)); // Wind strider (pet)
		table.add(new SummonItem(8663, 12621, 2)); // Wyvern (mount)

		service = new SummonItemService(table, null, null, null, null);
		sentPackets = new ArrayList<>();
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	private ItemTemplate createItemTemplate(int id, String name, boolean stackable) {
		return new ItemTemplate(
				id, id, name, ItemTemplate.Kind.ETC, "summon",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				0, 10, stackable, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@Test
	@DisplayName("Identificacao de itens invocadores na tabela")
	void testSummonItemsTable() {
		assertTrue(service.isSummonItem(2375));
		assertTrue(service.isSummonItem(5560));
		assertFalse(service.isSummonItem(99999));

		var wolfOpt = service.getSummonItem(2375);
		assertTrue(wolfOpt.isPresent());
		assertEquals(12077, wolfOpt.get().npcId());
		assertTrue(wolfOpt.get().isPet());
	}

	@Test
	@DisplayName("Invocacao de objeto estatico consome item do inventario")
	void testSummonStaticObject() {
		PlayerCharacter player = createPlayer(1, "FestivePlayer");
		Inventory inv = new Inventory(1);
		player.inventory(inv);

		ItemInstance treeItem = new ItemInstance(10, createItemTemplate(5560, "Christmas Tree", true), 1, 3);
		inv.add(treeItem);

		boolean success = service.useSummonItem(player, treeItem, sentPackets::add);
		assertTrue(success, "Deve invocar arvore de natal com sucesso");
		assertEquals(2, treeItem.count(), "Deve consumir 1 arvore do inventario");
	}

	@Test
	@DisplayName("Invocacao de pet registra o mascote no jogador")
	void testSummonPetSuccess() {
		PlayerCharacter player = createPlayer(2, "WolfMaster");
		Inventory inv = new Inventory(2);
		player.inventory(inv);

		ItemInstance collar = new ItemInstance(20, createItemTemplate(2375, "Wolf Collar", false), 2, 1);
		inv.add(collar);

		assertFalse(player.hasPet());
		boolean success = service.useSummonItem(player, collar, sentPackets::add);
		assertTrue(success, "Deve invocar o lobo com sucesso");
		assertTrue(player.hasPet(), "Jogador agora deve possuir pet ativo");
		assertTrue(service.getPet(player.objectId()).isPresent());

		// Verifica pacotes enviados (gauge e mensagem de summon)
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SetupGauge));
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.SUMMON_A_PET));

		// Nao pode invocar outro pet enquanto ja possuir um ativo
		boolean secondPet = service.useSummonItem(player, collar, sentPackets::add);
		assertFalse(secondPet, "Nao pode invocar segundo pet");
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.YOU_ALREADY_HAVE_A_PET));

		// Recolhe o pet
		boolean unsummoned = service.unsummonPet(player, sentPackets::add);
		assertTrue(unsummoned);
		assertFalse(player.hasPet());
		assertFalse(service.getPet(player.objectId()).isPresent());
	}

	@Test
	@DisplayName("Montaria e desmontaria com item wyvern")
	void testMountAndDismount() {
		PlayerCharacter player = createPlayer(3, "DragonRider");
		Inventory inv = new Inventory(3);
		player.inventory(inv);

		ItemInstance mountItem = new ItemInstance(30, createItemTemplate(8663, "Wyvern Crystal", false), 3, 1);
		inv.add(mountItem);

		assertFalse(player.isMounted());
		boolean mounted = service.useSummonItem(player, mountItem, sentPackets::add);
		assertTrue(mounted);
		assertTrue(player.isMounted());
		assertEquals(12621, player.mountNpcId());

		// Desmontar
		boolean dismounted = service.dismount(player, sentPackets::add);
		assertTrue(dismounted);
		assertFalse(player.isMounted());
		assertEquals(0, player.mountNpcId());
	}
}
