package com.lopez.l2j.game.buffshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.lopez.l2j.game.buffshop.BuffShopService.BuffShopItem;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class BuffShopServiceTest {

	private BuffShopService service;
	private JdbcClient mockJdbc;
	private InventoryService mockInventories;
	private PlayerCharacter seller;
	private PlayerCharacter buyer;

	@BeforeEach
	void setUp() {
		mockJdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
		mockInventories = mock(InventoryService.class);
		service = new BuffShopService(mockJdbc);

		seller = createPlayer(101, "BuffProphet");
		buyer = createPlayer(102, "FighterBoy");
	}

	private PlayerCharacter createPlayer(int id, String name) {
		return new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	@Test
	@DisplayName("startShop configura o jogador para o modo loja privada de venda e registra buffs")
	void testStartShop() {
		var items = List.of(
				new BuffShopItem(1068, 1, 5000, "Might"),
				new BuffShopItem(1040, 1, 5000, "Shield")
		);

		boolean started = service.startShop(seller, "Prophet Buffs", items);

		assertThat(started).isTrue();
		assertThat(seller.sitting()).isTrue();
		assertThat(seller.privateStoreType()).isEqualTo(1);
		assertThat(seller.isBuffShop()).isTrue();
		assertThat(seller.storeTitle()).isEqualTo("Prophet Buffs");

		assertThat(service.isBuffShop(seller.objectId())).isTrue();
		var shopOpt = service.getShop(seller.objectId());
		assertThat(shopOpt).isPresent();
		assertThat(shopOpt.get().items()).hasSize(2);
		assertThat(shopOpt.get().items().get(1068).price()).isEqualTo(5000);
	}

	@Test
	@DisplayName("stopShop encerra a loja, desativa os flags e remove o vendedor")
	void testStopShop() {
		service.startShop(seller, "Buffs", List.of(new BuffShopItem(1068, 1, 5000, "Might")));
		assertThat(service.isBuffShop(seller.objectId())).isTrue();

		service.stopShop(seller);

		assertThat(service.isBuffShop(seller.objectId())).isFalse();
		assertThat(seller.privateStoreType()).isEqualTo(0);
		assertThat(seller.isBuffShop()).isFalse();
		assertThat(seller.sitting()).isFalse();
		assertThat(seller.storeTitle()).isNull();
	}

	@Test
	@DisplayName("purchaseBuffs falha se o comprador nao possui adena suficiente")
	void testPurchaseBuffsInsufficientAdena() {
		service.startShop(seller, "Expensive Buffs", List.of(new BuffShopItem(1068, 1, 50000, "Might")));

		// Buyer inventory is empty (0 adena)
		var result = service.purchaseBuffs(buyer, seller.objectId(), List.of(1068), seller, mockInventories, null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.message()).contains("Adena suficiente");
		assertThat(result.totalCost()).isZero();
	}

	@Test
	@DisplayName("purchaseBuffs processa cobranca de adena e lista buffs comprados com sucesso")
	void testPurchaseBuffsSuccess() {
		service.startShop(seller, "Best Buffs", List.of(
				new BuffShopItem(1068, 1, 10000, "Might"),
				new BuffShopItem(1040, 1, 15000, "Shield")
		));

		// Buyer has 50,000 adena
		var adenaTpl = com.lopez.l2j.game.item.ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true);
		buyer.inventory().add(new com.lopez.l2j.game.item.ItemInstance(1, adenaTpl, buyer.objectId(), 50000));

		var result = service.purchaseBuffs(buyer, seller.objectId(), List.of(1068, 1040), seller, mockInventories, null, null);

		assertThat(result.success()).isTrue();
		assertThat(result.totalCost()).isEqualTo(25000);
		assertThat(result.appliedSkills()).containsExactly(1068, 1040);

		verify(mockInventories).consumeItem(buyer.inventory(), 57, 25000, "BuffShopPurchase");
		verify(mockInventories).addItem(seller.inventory(), 57, 25000, "BuffShopRevenue");
	}

	@Test
	@DisplayName("Comprador nao pode comprar buffs da sua propria loja")
	void testSelfPurchaseBlocked() {
		service.startShop(seller, "Self Store", List.of(new BuffShopItem(1068, 1, 1000, "Might")));

		var result = service.purchaseBuffs(seller, seller.objectId(), List.of(1068), seller, mockInventories, null, null);

		assertThat(result.success()).isFalse();
		assertThat(result.message()).contains("propria loja");
	}
}
