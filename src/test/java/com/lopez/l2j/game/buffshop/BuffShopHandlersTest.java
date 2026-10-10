package com.lopez.l2j.game.buffshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.lopez.l2j.game.buffshop.BuffShopService.BuffShopItem;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.handler.bypass.BypassBuffShopHandler;
import com.lopez.l2j.network.game.handler.voiced.VoicedBuffShopHandler;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

class BuffShopHandlersTest {

	private BuffShopService buffShopService;
	private VoicedBuffShopHandler voicedHandler;
	private BypassBuffShopHandler bypassHandler;

	private GameSession session;
	private GameSession.Context context;
	private PlayerCharacter seller;
	private PlayerCharacter buyer;
	private SkillTable skillTable;
	private SkillService skillService;
	private InventoryService inventoryService;
	private GameWorld world;

	@BeforeEach
	void setUp() {
		buffShopService = new BuffShopService(null);
		voicedHandler = new VoicedBuffShopHandler(buffShopService);
		bypassHandler = new BypassBuffShopHandler(buffShopService);

		seller = new PlayerCharacter(201, "acc1", "ProphetBob", 78, 0, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		buyer = new PlayerCharacter(202, "acc2", "KnightDave", 78, 0, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);

		session = mock(GameSession.class);
		context = mock(GameSession.Context.class);
		skillTable = mock(SkillTable.class);
		skillService = mock(SkillService.class);
		inventoryService = mock(InventoryService.class);
		world = mock(GameWorld.class);

		when(session.activeCharacter()).thenReturn(seller);
		when(session.context()).thenReturn(context);
		when(context.skillService()).thenReturn(skillService);
		when(skillService.table()).thenReturn(skillTable);
		when(context.inventories()).thenReturn(inventoryService);
		when(context.world()).thenReturn(world);

		var mightTpl = mock(SkillTemplate.class);
		when(mightTpl.id()).thenReturn(1068);
		when(mightTpl.level()).thenReturn(1);
		when(mightTpl.name()).thenReturn("Might");
		when(mightTpl.target()).thenReturn("TARGET_ONE");
		when(mightTpl.skillType()).thenReturn("BUFF");

		var shieldTpl = mock(SkillTemplate.class);
		when(shieldTpl.id()).thenReturn(1040);
		when(shieldTpl.level()).thenReturn(1);
		when(shieldTpl.name()).thenReturn("Shield");
		when(shieldTpl.target()).thenReturn("TARGET_ONE");
		when(shieldTpl.skillType()).thenReturn("BUFF");

		when(skillTable.get(1068, 1)).thenReturn(Optional.of(mightTpl));
		when(skillTable.get(1040, 1)).thenReturn(Optional.of(shieldTpl));

		seller.skills().put(1068, 1);
		seller.skills().put(1040, 1);
	}

	@Test
	@DisplayName("VoicedBuffShopHandler registra e reconhece .buffshop, .buffstore e .buybuff")
	void testVoicedCommandsRegistered() {
		var list = voicedHandler.getVoicedCommandList();
		assertThat(list).contains("buffshop", "buffstore", "buff_shop", "buff_store", "buybuff");
	}

	@Test
	@DisplayName("VoicedBuffShopHandler abre janela de gerenciamento ao executar .buffshop")
	void testVoicedBuffShopOpen() {
		boolean handled = voicedHandler.useVoicedCommand("buffshop", session, "");
		assertThat(handled).isTrue();

		ArgumentCaptor<GameServerPacket> captor = ArgumentCaptor.forClass(GameServerPacket.class);
		verify(session).send(captor.capture());
		assertThat(captor.getValue()).isInstanceOf(GameServerPacket.NpcHtmlMessage.class);
		var msg = (GameServerPacket.NpcHtmlMessage) captor.getValue();
		assertThat(msg.html()).contains("Buff Store Manager");
		assertThat(msg.html()).contains("Might");
	}

	@Test
	@DisplayName("VoicedBuffShopHandler abre janela de gerenciamento com .buffstore")
	void testVoicedBuffStoreOpen() {
		boolean handled = voicedHandler.useVoicedCommand("buffstore", session, "");
		assertThat(handled).isTrue();

		ArgumentCaptor<GameServerPacket> captor = ArgumentCaptor.forClass(GameServerPacket.class);
		verify(session).send(captor.capture());
		assertThat(captor.getValue()).isInstanceOf(GameServerPacket.NpcHtmlMessage.class);
	}

	@Test
	@DisplayName("BypassBuffShopHandler canHandle reconhece voiced_buffshop e buffshop_")
	void testBypassCanHandle() {
		assertThat(bypassHandler.canHandle("voiced_buffshop")).isTrue();
		assertThat(bypassHandler.canHandle("voiced_buffshop toggle 1068")).isTrue();
		assertThat(bypassHandler.canHandle("buffshop_start")).isTrue();
		assertThat(bypassHandler.canHandle("voiced_other")).isFalse();
	}

	@Test
	@DisplayName("BypassBuffShopHandler start inicia a loja com título e buffs configurados")
	void testBypassStartAndStop() {
		buffShopService.selectAllDraftBuffs(seller, skillTable);
		buffShopService.setDraftTitle(seller, "Buffs Top");

		boolean handledStart = bypassHandler.handleBypass("voiced_buffshop start", session);
		assertThat(handledStart).isTrue();
		assertThat(seller.isBuffShop()).isTrue();
		assertThat(seller.sitting()).isTrue();
		assertThat(seller.storeTitle()).isEqualTo("Buffs Top");

		boolean handledStop = bypassHandler.handleBypass("voiced_buffshop stop", session);
		assertThat(handledStop).isTrue();
		assertThat(seller.isBuffShop()).isFalse();
		assertThat(seller.sitting()).isFalse();
		assertThat(seller.storeTitle()).isNull();
	}

	@Test
	@DisplayName("BypassBuffShopHandler toggle alterna buffs no rascunho")
	void testBypassToggle() {
		buffShopService.selectAllDraftBuffs(seller, skillTable);
		assertThat(buffShopService.getDraftOrActiveItems(seller, skillTable)).containsKey(1068);

		bypassHandler.handleBypass("voiced_buffshop toggle 1068 1", session);
		assertThat(buffShopService.getDraftOrActiveItems(seller, skillTable)).doesNotContainKey(1068);

		bypassHandler.handleBypass("voiced_buffshop toggle 1068 1", session);
		assertThat(buffShopService.getDraftOrActiveItems(seller, skillTable)).containsKey(1068);
	}

	@Test
	@DisplayName("BypassBuffShopHandler setall altera preço de todos os buffs")
	void testBypassSetAllPrices() {
		buffShopService.selectAllDraftBuffs(seller, skillTable);
		bypassHandler.handleBypass("voiced_buffshop setall 50000 1", session);

		var draft = buffShopService.getDraftOrActiveItems(seller, skillTable);
		assertThat(draft.get(1068).price()).isEqualTo(50000);
		assertThat(draft.get(1040).price()).isEqualTo(50000);
	}

	@Test
	@DisplayName("BypassBuffShopHandler buy permite compra de buff por comprador")
	void testBypassBuy() {
		buffShopService.startShop(seller, "Store", List.of(new BuffShopItem(1068, 1, 5000, "Might")));

		GameSession buyerSession = mock(GameSession.class);
		when(buyerSession.activeCharacter()).thenReturn(buyer);
		when(buyerSession.context()).thenReturn(context);

		var adenaTpl = com.lopez.l2j.game.item.ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true);
		buyer.inventory().add(new com.lopez.l2j.game.item.ItemInstance(10, adenaTpl, buyer.objectId(), 50000));

		GameWorld.OnlinePlayer sellerPlayer = mock(GameWorld.OnlinePlayer.class);
		when(sellerPlayer.character()).thenReturn(seller);
		when(sellerPlayer.x()).thenReturn(0);
		when(sellerPlayer.y()).thenReturn(0);
		when(world.player(seller.objectId())).thenReturn(Optional.of(sellerPlayer));

		boolean ok = bypassHandler.handleBypass("voiced_buffshop buy " + seller.objectId() + " 1068 1", buyerSession);
		assertThat(ok).isTrue();

		verify(inventoryService).consumeItem(buyer.inventory(), 57, 5000, "BuffShopPurchase");
		verify(inventoryService).addItem(seller.inventory(), 57, 5000, "BuffShopRevenue");
	}

	@Test
	@DisplayName("BypassBuffShopHandler suporta navegacao por abas de categorias no vendedor e comprador")
	void testBypassCategoryNavigation() {
		buffShopService.startShop(seller, "Store", List.of(new BuffShopItem(1068, 1, 5000, "Might")));

		boolean sellerPage = bypassHandler.handleBypass("voiced_buffshop page dances 1", session);
		assertThat(sellerPage).isTrue();

		GameSession buyerSession = mock(GameSession.class);
		when(buyerSession.activeCharacter()).thenReturn(buyer);
		when(buyerSession.context()).thenReturn(context);

		boolean buyerPage = bypassHandler.handleBypass("voiced_buffshop buyer_page " + seller.objectId() + " buffs 1", buyerSession);
		assertThat(buyerPage).isTrue();
	}

	@Test
	@DisplayName("BypassBuffShopHandler buycat compra todos os buffs de uma categoria especifica")
	void testBypassBuyCat() {
		buffShopService.startShop(seller, "Store", List.of(
				new BuffShopItem(1068, 1, 5000, "Might"),
				new BuffShopItem(1040, 1, 5000, "Shield")
		));

		GameSession buyerSession = mock(GameSession.class);
		when(buyerSession.activeCharacter()).thenReturn(buyer);
		when(buyerSession.context()).thenReturn(context);

		var adenaTpl = com.lopez.l2j.game.item.ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true);
		buyer.inventory().add(new com.lopez.l2j.game.item.ItemInstance(10, adenaTpl, buyer.objectId(), 50000));

		GameWorld.OnlinePlayer sellerPlayer = mock(GameWorld.OnlinePlayer.class);
		when(sellerPlayer.character()).thenReturn(seller);
		when(sellerPlayer.x()).thenReturn(0);
		when(sellerPlayer.y()).thenReturn(0);
		when(world.player(seller.objectId())).thenReturn(Optional.of(sellerPlayer));

		boolean ok = bypassHandler.handleBypass("voiced_buffshop buycat " + seller.objectId() + " buffs 1", buyerSession);
		assertThat(ok).isTrue();

		verify(inventoryService).consumeItem(buyer.inventory(), 57, 10000, "BuffShopPurchase");
		verify(inventoryService).addItem(seller.inventory(), 57, 10000, "BuffShopRevenue");
	}
}
