package com.lopez.l2j.game.extractable;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractableItemServiceTest {

	private ExtractableItemsTable table;
	private ExtractableItemService service;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		table = new ExtractableItemsTable();
		// Configura itens de teste
		// Item 10001: 100% de chance de extrair 57 (Adena) x 1000
		ExtractableProduct prod1 = new ExtractableProduct(100, List.of(new ExtractableProduct.ProductItem(57, 1000)));
		table.add(new ExtractableItem(10001, List.of(prod1)));

		// Item 10002: 0% de chance de extrair algo (vazio)
		ExtractableProduct prod2 = new ExtractableProduct(0, List.of(new ExtractableProduct.ProductItem(57, 500)));
		table.add(new ExtractableItem(10002, List.of(prod2)));

		// Item 10003: 100% de chance de extrair dois itens (1060 x 5 e 1061 x 2)
		ExtractableProduct prod3 = new ExtractableProduct(100, List.of(
				new ExtractableProduct.ProductItem(1060, 5),
				new ExtractableProduct.ProductItem(1061, 2)
		));
		table.add(new ExtractableItem(10003, List.of(prod3)));

		service = new ExtractableItemService(table, null);
		sentPackets = new ArrayList<>();
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	private ItemTemplate createItemTemplate(int id, String name, boolean stackable) {
		return new ItemTemplate(
				id, id, name, ItemTemplate.Kind.ETC, "extract",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				0, 10, stackable, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@Test
	@DisplayName("Identificacao de itens extraiveis na tabela")
	void testExtractableTable() {
		assertTrue(service.isExtractable(10001));
		assertTrue(service.isExtractable(10002));
		assertTrue(service.isExtractable(10003));
		assertFalse(service.isExtractable(99999));

		var exOpt = service.getExtractableItem(10001);
		assertTrue(exOpt.isPresent());
		assertEquals(1, exOpt.get().products().size());
		assertEquals(100, exOpt.get().products().get(0).chance());
	}

	@Test
	@DisplayName("Extracao com sucesso consome caixa e entrega recompensa")
	void testExtractSuccess() {
		PlayerCharacter player = createPlayer(1, "ChestOpener");
		Inventory inv = new Inventory(1);
		player.inventory(inv);

		ItemInstance chest = new ItemInstance(10, createItemTemplate(10001, "Mystery Box", true), 1, 5);
		inv.add(chest);

		boolean result = service.extract(player, chest, sentPackets::add);
		assertTrue(result);
		assertEquals(4, chest.count(), "Deve consumir 1 caixa");

		// Verifica notificacao de recebimento
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.YOU_PICKED_UP_S1_S2));
	}

	@Test
	@DisplayName("Extracao sem sucesso notifica que nao havia nada dentro")
	void testExtractNothingInside() {
		PlayerCharacter player = createPlayer(2, "UnluckyPlayer");
		Inventory inv = new Inventory(2);
		player.inventory(inv);

		ItemInstance emptyChest = new ItemInstance(20, createItemTemplate(10002, "Empty Box", true), 2, 1);
		inv.add(emptyChest);

		boolean result = service.extract(player, emptyChest, sentPackets::add);
		assertTrue(result);
		assertEquals(0, emptyChest.count(), "Deve consumir a caixa mesmo se vazia");

		// Verifica notificacao de nada dentro
		assertTrue(sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.NOTHING_INSIDE_THAT));
	}

	@Test
	@DisplayName("Extracao com multiplos itens de recompensa")
	void testExtractMultipleItems() {
		PlayerCharacter player = createPlayer(3, "LuckyPlayer");
		Inventory inv = new Inventory(3);
		player.inventory(inv);

		ItemInstance multiChest = new ItemInstance(30, createItemTemplate(10003, "Multi Box", true), 3, 1);
		inv.add(multiChest);

		boolean result = service.extract(player, multiChest, sentPackets::add);
		assertTrue(result);
		assertEquals(0, multiChest.count());

		// 2 mensagens de pickup enviadas
		long pickupCount = sentPackets.stream()
				.filter(p -> p instanceof SystemMessage sm && (sm.id() == SystemMessage.YOU_PICKED_UP_S1 || sm.id() == SystemMessage.YOU_PICKED_UP_S1_S2))
				.count();
		assertEquals(2, pickupCount);
	}

	@Test
	@DisplayName("Carregamento do XML real extractable_items.xml")
	void testXmlLoading() {
		ExtractableItemsTable xmlTable = new ExtractableItemsTable();
		xmlTable.load();
		// Deve ter centenas de itens extraiveis carregados do XML
		assertTrue(xmlTable.size() > 0, "Deve carregar itens do extractable_items.xml");
	}
}
