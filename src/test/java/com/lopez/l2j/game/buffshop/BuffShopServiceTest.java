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

	@Test
	@DisplayName("getSkillIcon formata corretamente os ícones para Interlude sem alteração de system")
	void testGetSkillIcon() {
		assertThat(BuffShopService.getSkillIcon(1)).isEqualTo("icon.skill0001");
		assertThat(BuffShopService.getSkillIcon(395)).isEqualTo("icon.skill0395");
		assertThat(BuffShopService.getSkillIcon(1068)).isEqualTo("icon.skill1068");
		assertThat(BuffShopService.getSkillIcon(1388)).isEqualTo("icon.skill1388");
	}

	@Test
	@DisplayName("Controle de rascunho de buffs permite alternar, alterar preços e títulos")
	void testDraftManagement() {
		service.setDraftTitle(seller, "Minha Loja Top");
		assertThat(service.getDraftTitle(seller)).isEqualTo("Minha Loja Top");

		seller.skills().put(1068, 1);
		seller.skills().put(1040, 1);

		com.lopez.l2j.game.skill.SkillTable mockSkillTable = mock(com.lopez.l2j.game.skill.SkillTable.class);
		var mightTpl = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(mightTpl.name()).thenReturn("Might");
		when(mightTpl.level()).thenReturn(1);
		when(mightTpl.target()).thenReturn("TARGET_ONE");
		when(mightTpl.skillType()).thenReturn("BUFF");

		var shieldTpl = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(shieldTpl.name()).thenReturn("Shield");
		when(shieldTpl.level()).thenReturn(1);
		when(shieldTpl.target()).thenReturn("TARGET_ONE");
		when(shieldTpl.skillType()).thenReturn("BUFF");

		when(mockSkillTable.get(1068, 1)).thenReturn(java.util.Optional.of(mightTpl));
		when(mockSkillTable.get(1040, 1)).thenReturn(java.util.Optional.of(shieldTpl));

		var draft = service.getDraftOrActiveItems(seller, mockSkillTable);
		assertThat(draft).containsKey(1068);
		assertThat(draft).containsKey(1040);

		service.setDraftPrice(seller, 1068, 25000);
		assertThat(draft.get(1068).price()).isEqualTo(25000);

		service.setAllDraftPrices(seller, 75000, mockSkillTable);
		assertThat(draft.get(1068).price()).isEqualTo(75000);
		assertThat(draft.get(1040).price()).isEqualTo(75000);

		service.toggleDraftBuff(seller, 1068, mockSkillTable);
		assertThat(draft).doesNotContainKey(1068);

		service.toggleDraftBuff(seller, 1068, mockSkillTable);
		assertThat(draft).containsKey(1068);

		service.clearDraftBuffs(seller);
		assertThat(draft).isEmpty();
	}

	@Test
	@DisplayName("renderSellerManageHtml gera HTML com ícones de skill e controles de loja")
	void testRenderSellerManageHtml() {
		seller.skills().put(1068, 1);
		com.lopez.l2j.game.skill.SkillTable mockSkillTable = mock(com.lopez.l2j.game.skill.SkillTable.class);
		var mightTpl = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(mightTpl.name()).thenReturn("Might");
		when(mightTpl.level()).thenReturn(1);
		when(mightTpl.target()).thenReturn("TARGET_ONE");
		when(mightTpl.skillType()).thenReturn("BUFF");
		when(mockSkillTable.get(1068, 1)).thenReturn(java.util.Optional.of(mightTpl));

		String html = service.renderSellerManageHtml(seller, mockSkillTable, 1);
		assertThat(html).contains("Buff Store Manager");
		assertThat(html).contains("icon.skill1068");
		assertThat(html).contains("Might");
		assertThat(html).contains("INICIAR BUFF STORE");
	}

	@Test
	@DisplayName("renderBuyerShopHtml gera HTML com dados do vendedor e botões de compra")
	void testRenderBuyerShopHtml() {
		service.startShop(seller, "Buffs do Lopez", List.of(
				new BuffShopItem(1068, 1, 10000, "Might"),
				new BuffShopItem(1040, 1, 15000, "Shield")
		));

		String html = service.renderBuyerShopHtml(buyer, seller.objectId(), null, 1);
		assertThat(html).contains("Buffs do Lopez");
		assertThat(html).contains("BuffProphet");
		assertThat(html).contains("icon.skill1068");
		assertThat(html).contains("icon.skill1040");
		assertThat(html).contains("Comprar Todos");
	}

	@Test
	@DisplayName("getAvailableBuffSkills permite buffs transferíveis e bloqueia self skills de combate como Frenzy/Guts")
	void testAvailableBuffSkillsFilters() {
		com.lopez.l2j.game.skill.SkillTable mockSkillTable = mock(com.lopez.l2j.game.skill.SkillTable.class);

		// Might (1068) - buff transferivel
		var might = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(might.name()).thenReturn("Might");
		when(might.target()).thenReturn("TARGET_ONE");
		when(might.skillType()).thenReturn("BUFF");
		when(might.isBuff()).thenReturn(true);
		when(mockSkillTable.get(1068, 3)).thenReturn(java.util.Optional.of(might));

		// Frenzy (176) - self combat skill (deve ser excluido)
		var frenzy = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(frenzy.name()).thenReturn("Frenzy");
		when(frenzy.target()).thenReturn("TARGET_SELF");
		when(frenzy.skillType()).thenReturn("BUFF");
		when(mockSkillTable.get(176, 3)).thenReturn(java.util.Optional.of(frenzy));

		// Song of Earth (264) - buff de grupo transferivel
		var song = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(song.name()).thenReturn("Song of Earth");
		when(song.target()).thenReturn("TARGET_PARTY");
		when(song.skillType()).thenReturn("BUFF");
		when(song.isBuff()).thenReturn(true);
		when(mockSkillTable.get(264, 1)).thenReturn(java.util.Optional.of(song));

		seller.skills().put(1068, 3);
		seller.skills().put(176, 3);
		seller.skills().put(264, 1);

		var available = service.getAvailableBuffSkills(seller, mockSkillTable);
		var ids = available.stream().map(BuffShopItem::skillId).toList();

		assertThat(ids).contains(1068, 264);
		assertThat(ids).doesNotContain(176);
	}

	@Test
	@DisplayName("Vendedor AIO pode disponibilizar e vender todos os seus 90+ buffs sem limite artificial de 30")
	void testAllAioBuffsCanBeSoldWithout30Limit() {
		com.lopez.l2j.game.skill.SkillTable realTable = new com.lopez.l2j.game.skill.SkillTable(java.nio.file.Path.of("data", "xml", "stats", "skills"));
		com.lopez.l2j.game.service.AioService aioService = new com.lopez.l2j.game.service.AioService();
		aioService.rewardAioSkills(seller);

		var available = service.getAvailableBuffSkills(seller, realTable);
		assertThat(available.size()).isGreaterThanOrEqualTo(90);

		boolean started = service.startShop(seller, "AIO Super Buffs", available);
		assertThat(started).isTrue();

		var shopOpt = service.getShop(seller.objectId());
		assertThat(shopOpt).isPresent();
		assertThat(shopOpt.get().items()).hasSize(available.size());

		// Verifica que buffs criticos antes ausentes estao presentes
		var itemIds = shopOpt.get().items().keySet();
		assertThat(itemIds).contains(
				1062, // Berserker Spirit
				1003, // Pa'agrian Gift
				1005, // Blessings of Pa'agrio
				1282, // Pa'agrian Haste
				1357, // Prophecy of Wind
				4554  // Hot Springs Malaria
		);
	}

	@Test
	@DisplayName("Categorias de buffs categorizam corretamente dances, songs, chants e especiais")
	void testBuffCategories() {
		assertThat(BuffShopService.BuffCategory.getCategory(271, "Dance of the Warrior")).isEqualTo(BuffShopService.BuffCategory.DANCES);
		assertThat(BuffShopService.BuffCategory.getCategory(264, "Song of Earth")).isEqualTo(BuffShopService.BuffCategory.SONGS);
		assertThat(BuffShopService.BuffCategory.getCategory(1363, "Chant of Victory")).isEqualTo(BuffShopService.BuffCategory.CHANTS);
		assertThat(BuffShopService.BuffCategory.getCategory(1005, "Blessings of Pa'agrio")).isEqualTo(BuffShopService.BuffCategory.CHANTS);
		assertThat(BuffShopService.BuffCategory.getCategory(1357, "Prophecy of Wind")).isEqualTo(BuffShopService.BuffCategory.SPECIAL);
		assertThat(BuffShopService.BuffCategory.getCategory(4554, "Hot Springs Malaria")).isEqualTo(BuffShopService.BuffCategory.SPECIAL);
		assertThat(BuffShopService.BuffCategory.getCategory(1068, "Might")).isEqualTo(BuffShopService.BuffCategory.BUFFS);
	}

	@Test
	@DisplayName("Selecao e limpeza de draft por categoria")
	void testDraftCategorySelectionAndClearing() {
		seller.skills().put(1068, 1); // Might (BUFFS)
		seller.skills().put(271, 1);  // Dance of the Warrior (DANCES)

		com.lopez.l2j.game.skill.SkillTable mockSkillTable = mock(com.lopez.l2j.game.skill.SkillTable.class);
		var mightTpl = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(mightTpl.name()).thenReturn("Might");
		when(mightTpl.target()).thenReturn("TARGET_ONE");
		when(mightTpl.skillType()).thenReturn("BUFF");

		var danceTpl = mock(com.lopez.l2j.game.skill.SkillTemplate.class);
		when(danceTpl.name()).thenReturn("Dance of the Warrior");
		when(danceTpl.target()).thenReturn("TARGET_PARTY");
		when(danceTpl.skillType()).thenReturn("BUFF");

		when(mockSkillTable.get(1068, 1)).thenReturn(java.util.Optional.of(mightTpl));
		when(mockSkillTable.get(271, 1)).thenReturn(java.util.Optional.of(danceTpl));

		service.clearDraftBuffs(seller);
		assertThat(service.getDraftOrActiveItems(seller, mockSkillTable)).isEmpty();

		// Seleciona apenas categoria DANCES
		service.selectDraftCategory(seller, BuffShopService.BuffCategory.DANCES, mockSkillTable);
		var draft = service.getDraftOrActiveItems(seller, mockSkillTable);
		assertThat(draft).containsKey(271);
		assertThat(draft).doesNotContainKey(1068);

		// Limpa categoria DANCES
		service.clearDraftCategory(seller, BuffShopService.BuffCategory.DANCES, mockSkillTable);
		assertThat(service.getDraftOrActiveItems(seller, mockSkillTable)).isEmpty();
	}
}

